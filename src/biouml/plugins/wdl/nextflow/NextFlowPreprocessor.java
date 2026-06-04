package biouml.plugins.wdl.nextflow;

import java.awt.Point;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.diagram.ExpressionProperties;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.model.ExpressionInfo;
import biouml.plugins.wdl.parser.AstDeclaration;
import biouml.standard.type.Stub;
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
        processSameTaskCall( result );
        processConditionals(result);

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

            Object beforeCommand = WorkflowUtil.getBeforeCommand( task );
            if( beforeCommand instanceof ExpressionInfo[] )
            {
                for( ExpressionInfo info : (ExpressionInfo[])beforeCommand )
                {
                    String expression = null;
                    if( info.getAST() != null )
                    {
                        AstDeclaration declaration = info.getAST();
                        expression = new WDLNextflowFormatter().format( declaration.getExpression() );
                    }
                    if( expression == null )
                        expression = info.getExpression();
                    expression = procesRegexes( expression );
                    info.setExpression( expression );
                }
            }

            String command = WorkflowUtil.getCommand( task );
            ConversionResult converted = processIf( command );
            for( ExpressionInfo dec : converted.declarations )
                WorkflowUtil.addBeforeCommand( task, dec );

            command = converted.convertedCommand;
            command = dedent( command );
            Set<String> seps = findSeps( command );
            for( String sep : seps )
            {
                String name = getSepName( sep );
                ExpressionInfo dec = new ExpressionInfo( "String", name + "_str", name + ".join(' ')" );
                WorkflowUtil.addBeforeCommand( task, dec );
                command = command.replace( sep, "~{" + name + "_str}" );
            }
            command = this.processWDLFunctions( command );
            Set<String> variables = StreamEx.of( WorkflowUtil.getInputs( task ) ).map( input -> WorkflowUtil.getName( input ) ).toSet();
            variables.addAll(
                    StreamEx.of( WorkflowUtil.getBeforeCommandExpressions( task ) ).map( expression -> expression.getName() ).toSet() );
            command = procesRegexes( command );
            command = processVariables( command, variables );

            WorkflowUtil.setCommand( task, command );
        }

        for( Node node : result.recursiveStream().select( Node.class ) )
        {
            String expression = null;
            ExpressionInfo info = WorkflowUtil.getExpressionInfo( node );
            if( info != null && info.getAST() != null )
            {
                AstDeclaration declaration = info.getAST();
                expression = new WDLNextflowFormatter().format( declaration.getExpression() );
            }

            if( expression == null )
                expression = WorkflowUtil.getExpression( node );

            if( expression != null && !expression.isEmpty() )
            {
                //                expression =processCallName(node, expression);
                Set<String> seps = findSeps( expression );
                for( String sep : seps )
                {
                    String name = getSepName( sep );
                    ExpressionInfo dec = new ExpressionInfo( "String", name + "_str", name + ".join(' ')" );
                    ExpressionProperties properties = new ExpressionProperties();
                    properties.setVariable( dec.getName() );
                    properties.setType( dec.getType() );
                    properties.setRhs( dec.getExpression() );
                    DiagramElementGroup deg = properties.createElements( result, new Point( 0, 0 ), null );
                    Node newNode = (Node)deg.getElement();
                    Set<String> arguments = new HashSet<>();
                    arguments.add( name );
                    WorkflowUtil.setArguments( newNode, arguments );
                    result.put( newNode );
                    Set<Node> argNodes = WorkflowUtil.findExpressionNodes( result, name );
                    for( Node argNode : StreamEx.of( argNodes ) )
                    {
                        Edge edge1 = new Edge(
                                new Stub( null, argNode.getName() + " interact " + newNode.getName(), WDLConstants.LINK_TYPE ), argNode,
                                newNode );
                        result.put( edge1 );
                    }
                    Edge edge2 = new Edge( new Stub( null, newNode.getName() + " interact " + node.getName(), WDLConstants.LINK_TYPE ),
                            newNode, node );
                    result.put( edge2 );
                    expression = expression.replace( sep, "~{" + name + "_str}" );
                }
                //                expression = procesStruct(expression, structs);
                expression = processArrayElements( result, expression );
                expression = removeGlobs( expression );
                expression = processTernary( expression );
                expression = processMap( expression );
                expression = processPair( expression );
                expression = processObject( expression );
                expression = processWDLFunctions( expression );
                expression = procesRegexes( expression );
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
        if( sep.contains( "\"" ) )
            return sep.substring( sep.lastIndexOf( "\"" ) + 1, sep.length() - 1 ).trim();
        else if( sep.contains( "\'" ) )
            return sep.substring( sep.lastIndexOf( "\'" ) + 1, sep.length() - 1 ).trim();
        return null;
    }

    public static Set<String> findSeps(String input)
    {
        Set<String> result = new HashSet<>();
        String regex = "[~$]\\{sep=([\"'])(.*?)\\1\\s+([a-zA-Z_][a-zA-Z0-9_]*)}";
        //        String regex = "~\\{sep=([\"'])(.*?)\\1\\s+([a-zA-Z_][a-zA-Z0-9_]*)}";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( input );
        while( matcher.find() )
        {
            result.add( matcher.group() );
        }
        return result;
    }

    public static String procesRegexes(String content)
    {
        Pattern pattern = Pattern.compile( "\"([^\"]*\\$)\"" );
        Matcher matcher = pattern.matcher( content );

        return matcher.replaceAll( "'$1'" );
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
    
    public static void processConditionals(Diagram diagram)
    {
        for (Compartment cycle: diagram.recursiveStream().select( Compartment.class ).filter( c->WorkflowUtil.isConditional( c ) ))
        {
            for( Node node : cycle.getNodes() )
            {
                if( WorkflowUtil.isExpression( node ) )
                {
                    String name = WorkflowUtil.getName( node );
                    if( name != null )
                    {
                        String nodeName = DefaultSemanticController.generateUniqueName( diagram, node.getName() );
                        Node clone = node.clone( cycle.getCompartment(), nodeName );
                        WorkflowUtil.setExpressionInfo( clone, null );
                        WorkflowUtil.setExpression( clone, "null" );
                        cycle.getCompartment().put( clone );
                        
                        Edge edge1 = new Edge(
                                new Stub( null, clone.getName() + " interact " + node.getName(), WDLConstants.LINK_TYPE ), clone,
                                node );
                        cycle.getCompartment().put( edge1 );
                    }
                }
            }
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
            if( variable.contains( "(" ) )//this is function
            {
                if( !StreamEx.of( NextFlowVelocityHelper.getWDLFunctions() ).toSet()
                        .contains( variable.substring( 0, variable.indexOf( "_wdl(" ) ) ) )
                {
                    command = command.replace( "${" + variable + "}", "\\${" + variable + "}" );
                }
            }
            else if( !candidateVariables.contains( variable ) )
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
        for( String function : NextFlowVelocityHelper.getWDLFunctions() )
        {
            if( s.contains( function + "(" ) )
                s = s.replace( function + "(", function + "_wdl(" );
        }
        //        if( s.contains( "transpose(" ) )
        //            s = s.replace( "transpose(", "transpose_wdl(" );
        //        if( s.contains( "cross(" ) )
        //            s = s.replace( "cross(", "cross_wdl(" );
        //        if( s.contains( "flatten(" ) )
        //            s = s.replace( "cross(", "flatten_wdl(" );
        //        if( s.contains( "write_map(" ) )
        //            s = s.replace( "write_map(", "write_map_wdl(" );
        return s;
    }

    public String dedent(String text)
    {
        if( text == null || text.isEmpty() )
            return text;

        String[] lines = text.split( "\\R", -1 );

        int minIndent = Integer.MAX_VALUE;

        // Find minimal indentation among non-empty lines
        for( String line : lines )
        {
            if( line.trim().isEmpty() )
                continue;

            int indent = 0;
            while( indent < line.length() && ( line.charAt( indent ) == ' ' || line.charAt( indent ) == '\t' ) )
            {
                indent++;
            }

            minIndent = Math.min( minIndent, indent );
        }

        if( minIndent == Integer.MAX_VALUE || minIndent == 0 )
            return text;

        // Remove common indentation
        StringBuilder result = new StringBuilder();

        for( int i = 0; i < lines.length; i++ )
        {
            String line = lines[i];

            if( line.trim().isEmpty() )
            {
                result.append( line );
            }
            else
            {
                result.append( line.substring( Math.min( minIndent, line.length() ) ) );
            }

            if( i < lines.length - 1 )
                result.append( '\n' );
        }

        return result.toString();
    }

    private void processSameTaskCall(Diagram diagram)
    {
        List<Compartment> calls = StreamEx.of( WorkflowUtil.getWorkflows( diagram ) ).prepend( diagram ).toFlatList( w -> WorkflowUtil.getCalls( w ) );


        //process aliases for NOT imported calls
        for (Compartment call: calls)
        {
            String alias = WorkflowUtil.getAlias( call );
            String taskName = WorkflowUtil.getTaskRef( call );

            if( WorkflowUtil.findTask( taskName, diagram ) != null && !alias.equals( taskName ) )
            {
                WorkflowUtil.setAlias( call, taskName );
                WorkflowUtil.setResultName( call, alias );
            }
        }
        
        Set<String> repeatedTasks = new HashSet<>();
        for (Compartment call: calls)
        {
            String taskRef = WorkflowUtil.getTaskRef( call );
            if (repeatedTasks.contains( taskRef ))
            {
                Compartment task = WorkflowUtil.findTask( taskRef, diagram );
                if( task != null )
                {
                    Compartment copy = copyTask( task, DefaultSemanticController.generateUniqueName( diagram, taskRef ) );
                    String newTaskName = WorkflowUtil.getName( copy );
                    WorkflowUtil.setTaskRef( copy, newTaskName );
                    WorkflowUtil.setAlias( call, newTaskName );
                }
            }
            repeatedTasks.add(taskRef);//.computeIfAbsent( taskRef, k -> new HashSet<Compartment>() ).add( call );
        }


    }

    private Compartment copyTask(Compartment c, String name)
    {
        Compartment c2 = c.clone( c.getCompartment(), name );
        WorkflowUtil.setName( c2, name );
        c.getCompartment().put( c2 );
        return c2;
    }
}