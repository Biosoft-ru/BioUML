package biouml.plugins.wdl.nextflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.model.ExpressionInfo;
import one.util.streamex.StreamEx;

public class NextFlowPreprocessor
{
    private WorkflowSettings settings;
    public void NextFlowPreprocessor()
    {

    }

    public void setExportPath(WorkflowSettings settings)
    {
        this.settings = settings;
    }

    public Diagram preprocess(Diagram diagram) throws Exception
    {
        Diagram result = diagram.clone( diagram.getOrigin(), diagram.getName() );

        for( Compartment task : WorkflowUtil.getTasks( result ) )
        {
            for( Node input : WorkflowUtil.getInputs( task ) )
            {
                String expression = WorkflowUtil.getExpression( input );
                if( expression != null && !expression.isEmpty() )
                {
                    String name = WorkflowUtil.getName( input );
                    WorkflowUtil.addBeforeCommand( task,
                            new ExpressionInfo( WorkflowUtil.getType( input ), name, "getDefault(" + name + ", " + expression + ")" ) );
                }
                //                WorkflowUtil.setExpression( input, processWDLFunctions( expression ));
            }

            for( Node output : WorkflowUtil.getOutputs( task ) )
            {
                String expression = WorkflowUtil.getExpression( output );
                if( expression != null && !expression.isEmpty() )
                {
                    if( expression.contains( "stdout()" ) )
                    {
                        WorkflowUtil.setExpression( output, expression.replace( "stdout()", "stdout" ) );
                    }
                    //                    WorkflowUtil.setExpression( output, processWDLFunctions( expression ));
                }
            }

            String command = WorkflowUtil.getCommand( task );
            ConversionResult converted = processIf( command );
            for( ExpressionInfo dec : converted.declarations )
                WorkflowUtil.addBeforeCommand( task, dec );

            command = converted.convertedCommand;
            Set<String> seps = findSeps( command );
            for( String sep : seps )
            {
                String name = getSepName( sep );
                ExpressionInfo dec = new ExpressionInfo( "String", name + "_str", name + ".join(' ')" );
                WorkflowUtil.addBeforeCommand( task, dec );
                command = command.replace( sep, "~{" + name + "_str}" );
            }
            command = this.processWDLFunctions( command );
            command = processVariables( command,
                    StreamEx.of( WorkflowUtil.getInputs( task ) ).map( input -> WorkflowUtil.getName( input ) ).toSet() );
            WorkflowUtil.setCommand( task, command );
        }

        for( Node node : result.recursiveStream().select( Node.class ) )
        {
            String expression = WorkflowUtil.getExpression( node );

            if( expression != null && !expression.isEmpty() )
            {
                //                expression =processCallName(node, expression);
                expression = processArrayElements( result, expression );
                expression = removeGlobs( expression );
                expression = processTernary( expression );
                expression = processMap( expression );
                expression = processPair( expression );
                expression = processObject( expression );
                expression = processWDLFunctions( expression );
                WorkflowUtil.setExpression( node, expression );
            }
        }
        return result;
    }


    public String preprocess(String s) throws Exception
    {
        return s;
    }

    public static String getSepName(String sep)
    {
        return sep.substring( 9, sep.length() - 1 ).trim();
    }

    public static Set<String> findSeps(String input)
    {
        Set<String> result = new HashSet<>();
        String regex = "~\\{sep=([\"'])\\s*\\1 ([a-zA-Z_][a-zA-Z0-9_]*)}";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( input );
        while( matcher.find() )
        {
            result.add( matcher.group() );
        }
        return result;
    }

    public static String processArrayElements(Diagram diagram, String input)
    {
        String regex = "^([a-zA-Z_][a-zA-Z0-9_\\.]*)\\[\\s*([a-zA-Z0-9_\\.]+)\\s*\\]$";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( input );
        if( matcher.matches() )
        {
            String arrayName = matcher.group( 1 );
            String index = matcher.group( 2 );

            Node arrayIndex = diagram.recursiveStream().select( Node.class )
                    .filter( n -> WorkflowUtil.isCycleVariable( n ) && WorkflowUtil.getName( n ).equals( index ) ).findAny().orElse( null );
            if( arrayIndex != null )
                return input;
            String nextFlowStyle = "get(" + arrayName + ", " + index + ")";
            input = input.replace( matcher.group(), nextFlowStyle );
        }
        return input;
    }

    public static String removeGlobs(String input)
    {
        String regex = "glob\\((['\"])([a-zA-Z0-9./*_]+)\\1\\)";
        return input.replaceAll( regex, "\"$2\"" );
    }

    public static String processPair(String pair)
    {
        if( pair.startsWith( "(" ) && pair.endsWith( ")" ) && pair.contains( "," ) )
        {
            pair = "[" + pair.substring( 1, pair.lastIndexOf( ")" ) ) + "]";
        }
        return pair;
    }

    public static String processMap(String map)
    {
        String content = map.trim();
        if( content.startsWith( "{" ) && content.endsWith( "}" ) && content.contains( ":" ) )
        {
            return map.replace( "{", "[" ).replace( "}", "]" );
        }
        else if( content.startsWith( "[" ) && content.endsWith( "]" ) && content.contains( ":" ) )
        {
            return map.replace( "{", "[" ).replace( "}", "]" );
        }
        return map;
    }

    public static String processObject(String obj)
    {
        String content = obj.trim();
        if( content.startsWith( "object" ) && content.endsWith( "}" ) )
        {
            return obj.replace( "{", "[" ).replace( "}", "]" ).replace( "object", "" );
        }
        return obj;
    }

    public static String processTernary(String ternary)
    {
        String patternString = "if\\s+(.+?)\\s+then\\s+(.+?)\\s+else\\s+(.+)";

        Pattern pattern = Pattern.compile( patternString );
        Matcher matcher = pattern.matcher( ternary );

        if( matcher.matches() )
        {
            String condition = matcher.group( 1 ).trim();
            String truePart = matcher.group( 2 ).trim();
            String falsePart = matcher.group( 3 ).trim();
            return condition + " ? " + truePart + " : " + falsePart;
        }
        return ternary;
    }

    public static class ConversionResult
    {
        public List<ExpressionInfo> declarations;
        public String convertedCommand;

        public ConversionResult(List<ExpressionInfo> declarations, String command)
        {
            this.declarations = declarations;
            this.convertedCommand = command;
        }
    }

    /**
     * Matches: ~{if defined(var) then value1 else value2}
     */
    public static ConversionResult processIf(String wdlCommand)
    {
        List<ExpressionInfo> declarations = new ArrayList<>();
        String result = wdlCommand;
        int counter = 0;

        Pattern pattern = Pattern.compile( "~\\{if (.+?) then (.+?) else (.+?)\\}" );

        Matcher matcher = pattern.matcher( result );
        StringBuffer sb = new StringBuffer();

        while( matcher.find() )
        {
            String variable = matcher.group( 1 ).trim();
            String thenValue = cleanValue( matcher.group( 2 ).trim() );
            String elseValue = cleanValue( matcher.group( 3 ).trim() );
            String varName = "var_" + counter++;

            // Generate: def var_0 = isDefined(x) ? "value1" : "value2"
            String expression = "defined(" + variable + ") ? " + "\"" + thenValue + "\"" + " : " + "\"" + elseValue + "\"";
            declarations.add( new ExpressionInfo( "String", varName, expression ) );
            matcher.appendReplacement( sb, "\\${" + varName + "}" );
        }
        matcher.appendTail( sb );
        result = sb.toString();

        // Handle ~{default="X" var} -> ${var ?: 'X'}
        result = result.replaceAll( "~\\{\\s*default\\s*=\\s*\"([^\"]*)\"\\s+([^}]+)\\s*\\}", "\\${$2 ?: '$1'}" );
        return new ConversionResult( declarations, result );
    }

    /**
     * Clean WDL value: remove quotes and handle string concatenation
     */
    private static String cleanValue(String value)
    {
        return value.replace( "\"", "" ).replace( "'", "" ).replace( " + ", "" ).replace( "+", "" );
    }

    public static String processJson(String json)
    {
        JSONObject input = new JSONObject( json );
        JSONObject result = new JSONObject();

        for( String key : input.keySet() )
        {
            int dotIndex = key.indexOf( '.' );
            String newKey = dotIndex >= 0 ? key.substring( dotIndex + 1 ) : key;

            result.put( newKey, input.get( key ) );
        }

        return result.toString( 2 );
    }

    private String processVariables(String command, Set<String> candidateVariables)
    {
        List<String> buckVariables = extractVariables( command, "$" );
        for( String variable : buckVariables )
        {
            if( !candidateVariables.contains( variable ) )
            {
                command = command.replace( "${" + variable + "}", "\\${" + variable + "}" );
            }
        }
        List<String> tildaVariables = extractVariables( command, "~" );
        for( String variable : tildaVariables )
        {
            command = command.replace( "~{" + variable + "}", "${" + variable + "}" );
        }
        return command;
    }

    public static List<String> extractVariables(String text, String prefix)
    {
        List<String> result = new ArrayList<>();

        Pattern pattern = Pattern.compile( "\\" + prefix + "\\{([^}]*)\\}" );
        Matcher matcher = pattern.matcher( text );

        while( matcher.find() )
        {
            result.add( matcher.group( 1 ) );
        }

        return result;
    }

    private String processWDLFunctions(String s)
    {
        if( s == null )
            return null;
        if( s.contains( "transpose(" ) )
            s = s.replace( "transpose(", "transpose_wdl(" );
        if( s.contains( "cross(" ) )
            s = s.replace( "cross(", "cross_wdl(" );
        if( s.contains( "flatten(" ) )
            s = s.replace( "cross(", "flatten_wdl(" );
        return s;
    }
}