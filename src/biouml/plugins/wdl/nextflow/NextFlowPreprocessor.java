package biouml.plugins.wdl.nextflow;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.ExpressionProperties;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.model.ExpressionInfo;
import biouml.plugins.wdl.parser.AstExpression;
import biouml.plugins.wdl.parser.AstFunction;
import biouml.plugins.wdl.parser.AstRegularFormulaElement;
import biouml.plugins.wdl.parser.AstSymbol;
import biouml.plugins.wdl.parser.AstText;
import biouml.plugins.wdl.parser.ExpressionFormatter;
import biouml.plugins.wdl.parser.ExpressionParser;
import biouml.plugins.wdl.parser.ParserUtil;
import biouml.plugins.wdl.parser.SimpleNode;
import biouml.plugins.wdl.parser.Token;
import biouml.plugins.wdl.parser.WDLParserTreeConstants;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;

public class NextFlowPreprocessor
{
    private static Set<String> fileGenerators = Set.of( "write_lines" );

    private String versionWDL = "1.2";
    private String publishDir = "";

    public void NextFlowPreprocessor()
    {

    }

    public void setPublishDir(String publishDir)
    {
        this.publishDir = publishDir;
    }

    private boolean isFunctionExpression(biouml.plugins.wdl.parser.Node node, String name)
    {
        return node instanceof AstExpression && node.jjtGetNumChildren() == 1 && isFunction( node.jjtGetChild( 0 ), name );
    }

    public static boolean isFunction(biouml.plugins.wdl.parser.Node node, String name)
    {
        return node instanceof AstFunction && ( (AstFunction)node ).toString().equals( name );
    }

    public Diagram preprocess(Diagram diagram) throws Exception
    {
        Diagram result = diagram.clone( diagram.getOrigin(), diagram.getName() );
        versionWDL = diagram.getAttributes().getValueAsString( WDLConstants.WDL_VERSION_ATTR );
        result.getAttributes().add( new DynamicProperty( WDLConstants.WDL_VERSION_ATTR, String.class, versionWDL ) );
        processSameTaskCall( result );
        processConditionals( result );
        wrapProcesses( result );
        processFileGenerators( result );
        processEmptyOutput( result );

        for( Compartment task : WorkflowUtil.getTasks( result ) )
        {
            if( publishDir.isEmpty() )
                WorkflowUtil.setRuntimeProperty( task, "publishDir", "\"" + task.getName() + "\", mode: 'link', overwrite: true" );
            else
                WorkflowUtil.setRuntimeProperty( task, "publishDir",
                        "\"" + publishDir + "/" + task.getName() + "\", mode: 'link', overwrite: true" );

            for( Node input : WorkflowUtil.getInputs( task ) )
            {
                String expression = WorkflowUtil.getExpression( input );
                if( expression != null && !expression.isEmpty() )
                {
                    String name = WorkflowUtil.getName( input );
                    WorkflowUtil.addBeforeCommand( task,
                            new ExpressionInfo( WorkflowUtil.getType( input ), name, "getDefault(" + name + ", " + expression + ")" ) );
                }
            }

            boolean hasStdout = false;
            boolean hasStdErr = false;
            for( Node output : WorkflowUtil.getOutputs( task ) )
            {
                String expression = WorkflowUtil.parseExpression( output, "Nextflow" );
                if( expression != null && !expression.isEmpty() )
                {
                    if( expression.contains( "stdout()" ) )
                    {
                        hasStdout = true;
                        ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );
                        AstExpression astExpression = info.getAST();
                        AstExpression newExpression = new AstExpression( WDLParserTreeConstants.JJTEXPRESSION );
                        for( int i = 0; i < astExpression.jjtGetNumChildren(); i++ )
                        {
                            biouml.plugins.wdl.parser.Node node = astExpression.jjtGetChild( i );
                            if( isFunctionExpression( node, "stdout" ) || isFunction( node, "stdout" ) )
                            {
                                AstText astText = new AstText( WDLParserTreeConstants.JJTTEXT );
                                astText.setText( "stdout.txt" );
                                newExpression.jjtAddChild( astText, i );
                            }
                            else
                                newExpression.jjtAddChild( node, i );
                        }
                        WorkflowUtil.setExpression( output, newExpression.toString() );
                        info.setAST( newExpression );
                    }
                    if( expression.contains( "stderr()" ) )
                    {
                        hasStdErr = true;
                        ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );
                        AstExpression astExpression = info.getAST();
                        AstExpression newExpression = new AstExpression( WDLParserTreeConstants.JJTEXPRESSION );
                        for( int i = 0; i < astExpression.jjtGetNumChildren(); i++ )
                        {
                            biouml.plugins.wdl.parser.Node node = astExpression.jjtGetChild( i );
                            if( isFunctionExpression( node, "stderr" ) || isFunction( node, "stderr" ) )
                            {
                                AstText astText = new AstText( WDLParserTreeConstants.JJTTEXT );
                                astText.setText( "stderr.txt" );
                                newExpression.jjtAddChild( astText, i );
                            }
                            else
                                newExpression.jjtAddChild( node, i );
                        }
                        WorkflowUtil.setExpression( output, newExpression.toString() );
                        info.setAST( newExpression );
                    }
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
                        expression = new WDLNextflowFormatter().format( info.getAST() );
                    }
                    if( expression == null )
                        expression = info.getExpression();
                    expression = processExpression( expression, diagram, getVariables( task ) );
                    info.setExpression( expression );
                }
            }

            String command = WorkflowUtil.getCommand( task );
            ConversionResult converted = processIf( command );
            for( ExpressionInfo dec : converted.declarations )
                WorkflowUtil.addBeforeCommand( task, dec );

            command = converted.convertedCommand;
            command = dedent( command );

            if( hasStdout )
                command = processEcho( command );
            if( hasStdErr )
                command = processStdErr( command );
            Set<String> seps = findSeps( command );
            for( String sep : seps )
            {
                String name = getSepName( sep );
                String del = getSepDelimiter( sep );
                ExpressionInfo dec = new ExpressionInfo( "String", name + "_str", "sep_wdl ( '" + del + "', " + name + ")" );
                WorkflowUtil.addBeforeCommand( task, dec );
                command = command.replace( sep, "~{" + name + "_str}" );
            }
            command = this.processWDLFunctions( command, true );
            command = doubleBackslashesOutsidePlaceholders( command );
            command = procesRegexes( command );
            command = escapeSingleBuck( command );
            command = processVariables( command, getVariables( task ), true );
            command = removeEscape( command );

            WorkflowUtil.setCommand( task, command );
        }

        for( Node node : result.recursiveStream().select( Node.class ) )
        {
            String expression = null;
            ExpressionInfo info = WorkflowUtil.getExpressionInfo( node );
            if( info != null && info.getAST() != null )
            {
                expression = new WDLNextflowFormatter().format( info.getAST() );
            }

            if( expression == null )
                expression = WorkflowUtil.getExpression( node );

            if( expression != null && !expression.isEmpty() )
            {
                //                expression =processCallName(node, expression);
                Set<String> seps = findSeps( expression );
                for( String sep : seps )
                {
                    if( versionWDL.equals( "1.0" ) )
                    {
                        String name = getSepName( sep );
                        expression = expression.replace( "\"" + sep + "\"", "stringify_wdl(" + name + ")" );
                    }
                    else
                    {
                        String name = getSepName( sep );
                        String del = getSepDelimiter( sep );
                        ExpressionInfo dec = new ExpressionInfo( "String", name + "_str", name + ".join('" + del + "')" );
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
                }
                Map<String, String> variables = new HashMap<>();
                if( WorkflowUtil.isCall( node.getCompartment() ) || WorkflowUtil.isTask( node.getCompartment() ) )
                    variables = getVariables( node.getCompartment() );
                expression = processExpression( expression, diagram, variables );
                WorkflowUtil.setExpression( node, expression );
            }
        }

        return result;
    }

    private Map<String, String> getVariables(Compartment task)
    {
        Map<String, String> variables = StreamEx.of( WorkflowUtil.getInputs( task ) ).toMap( input -> WorkflowUtil.getName( input ),
                input -> WorkflowUtil.getType( input ) );
        variables.putAll( StreamEx.of( WorkflowUtil.getBeforeCommandExpressions( task ) ).toMap( expression -> expression.getName(),
                expression -> expression.getType() ) );
        return variables;
    }

    private String processExpression(String expression, Diagram diagram, Map<String, String> variables)
    {
        //                expression = procesStruct(expression, structs);
        expression = processArrayElements( diagram, expression );
        expression = removeGlobs( expression );
        expression = processTernary( expression );
        expression = processMap( expression );
        expression = processPair( expression );
        expression = processObject( expression );
        expression = processWDLFunctions( expression, false );
        expression = procesRegexes( expression );
        expression = processVariables( expression, variables, false );
        return expression;
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

    public static String getSepDelimiter(String sep)
    {
        if( sep.contains( "\"" ) )
        {
            String del = sep.substring( sep.indexOf( "\"" ) + 1 );
            return del.substring( 0, del.indexOf( "\"" ) );
        }
        else if( sep.contains( "\'" ) )
        {
            String del = sep.substring( sep.indexOf( "\'" ) + 1 );
            return del.substring( 0, del.indexOf( "\'" ) );
        }
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
        String regex = "glob\\((['\"])(.*?)\\1\\)";
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
        for( Compartment cycle : diagram.recursiveStream().select( Compartment.class ).filter( c -> WorkflowUtil.isConditional( c ) ) )
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

                        Edge edge1 = new Edge( new Stub( null, clone.getName() + " interact " + node.getName(), WDLConstants.LINK_TYPE ),
                                clone, node );
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
            //            String value = input.getString( key );
            //            if (value.startsWith( "tests/" ))
            //                value = value.replace( "tests/", "results/" );
            result.put( newKey, input.get( key ) );
        }

        return result.toString( 2 );
    }

    private String escapeSingleBuck(String str)
    {
        return str.replaceAll( "\\$(?!\\{)", "\\\\\\$" );
    }

    private String removeEscape(String str)
    {
        return str.replace( "^\\#", "^#" );
    }

    private String processVariables(String command, Map<String, String> candidateVariables, boolean inCommand)
    {
        Set<String> replaced = new HashSet<>();
        while( true )
        {
            List<String> buckVariables = extractVariables( command, "$" );
            buckVariables.removeAll( replaced );
            if( buckVariables.isEmpty() )
                break;
            //        for( String variable : buckVariables )
            //        {
            String variable = buckVariables.get( 0 );
            String original = variable;
            if( variable.contains( "(" ) )//this is function
            {
                if( original.contains( "_wdl(" ) )
                    original = original.substring( 0, original.indexOf( "_wdl(" ) );
                else if( original.contains( "_bash(" ) )
                    original = original.substring( 0, original.indexOf( "_bash(" ) );
                if( !StreamEx.of( NextFlowVelocityHelper.getWDLFunctions() ).toSet().contains( original ) )
                {
                    command = command.replace( "${" + variable + "}", "\\${" + variable + "}" );
                }
            }
            else if( !candidateVariables.containsKey( variable ) )
            {
                if( inCommand )
                    command = command.replace( "${" + variable + "}", "\\${" + variable + "}" );
                else
                {
                    command = command.replace( "\"${" + variable + "}\"", variable );
                    command = command.replace( "'${" + variable + "}'", variable );
                    command = command.replace( "${" + variable + "}", variable );
                }
            }


            replaced.add( variable );
            //        }
        }

        replaced.clear();
        while( true )
        {
            List<String> tildaVariables = extractVariables( command, "~" );
            tildaVariables.removeAll( replaced );
            if( tildaVariables.isEmpty() )
                break;

            String variable = tildaVariables.get( 0 );
            //        for( String variable : tildaVariables )

            if( candidateVariables.containsKey( variable ) )
            {
                String type = candidateVariables.get( variable );
                if( type.equals( "Directory" ) )
                {
                    command = command.replace( "~{" + variable + "}", "${" + variable + "}/" );
                }
                else
                {
                    command = command.replace( "~{" + variable + "}", "${" + variable + "}" );
                }
            }
            else
            {
                if( inCommand )
                {
                    command = command.replace( "'~{" + variable + "}'", "~{" + variable + "}" );
                    command = command.replace( "~{" + variable + "}", "${" + variable + "}" );
                }
                else
                {

                    command = command.replace( "\"~{" + variable + "}\"", variable );
                    command = command.replace( "'~{" + variable + "}'", variable );
                    command = command.replace( "~{" + variable + "}", variable );
                }
            }
            replaced.add( variable );
            //        }
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

    private String processStdErr(String text)
    {
        return "{" + System.lineSeparator() + text + System.lineSeparator() + "}  2> stderr.txt";
    }

    private String processEcho(String text)
    {
        return "{" + System.lineSeparator() + text + System.lineSeparator() + "} > stdout.txt";
    }

    private String processWDLFunctions(String s, boolean inCommand)
    {
        if( s == null )
            return null;
        for( String function : NextFlowVelocityHelper.getWDLFunctions() )
        {
            String replacement = NextFlowVelocityHelper.toNextflowFunction( function, inCommand );
            if( s.contains( function + "(" ) )
                s = s.replace( function + "(", replacement + "(" );
        }
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
        List<Compartment> calls = StreamEx.of( WorkflowUtil.getWorkflows( diagram ) ).prepend( diagram )
                .toFlatList( w -> WorkflowUtil.getCalls( w ) );


        //process aliases for NOT imported calls
        for( Compartment call : calls )
        {
            String alias = WorkflowUtil.getAlias( call );
            String taskName = WorkflowUtil.getTaskRef( call );

            if( WorkflowUtil.findTask( taskName, diagram ) != null && alias != null )
            {
                WorkflowUtil.setAlias( call, taskName );
                WorkflowUtil.setResultName( call, alias );
            }
        }

        Set<String> repeatedTasks = new HashSet<>();
        for( Compartment call : calls )
        {
            String taskRef = WorkflowUtil.getTaskRef( call );
            if( repeatedTasks.contains( taskRef ) )
            {
                Compartment task = WorkflowUtil.findTask( taskRef, diagram );
                if( task != null )
                {
                    Compartment copy = copyTask( task, DefaultSemanticController.generateUniqueName( diagram, taskRef ) );
                    String newTaskName = WorkflowUtil.getName( copy );
                    WorkflowUtil.setTaskRef( call, newTaskName );
                    WorkflowUtil.setAlias( call, newTaskName );
                }
            }
            repeatedTasks.add( taskRef );
        }
    }

    private Compartment copyTask(Compartment c, String name)
    {
        try
        {
            Compartment c2 = c.clone( c.getCompartment(), name );

            for( Node n : c.getNodes() )
            {
                String nodeName = WorkflowUtil.getName( n );
                Node copyNode = WorkflowUtil.findNode( c, nodeName );
                ExpressionInfo info = WorkflowUtil.getExpressionInfo( n );
                WorkflowUtil.setExpressionInfo( copyNode, info.clone() );
            }

            WorkflowUtil.setName( c2, name );
            c.getCompartment().put( c2 );
            return c2;
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * In output block
     * @param diagram
     * @throws Exception
     */
    private static void processFileGenerators(Diagram diagram) throws Exception
    {
        for( Compartment c : WorkflowUtil.getTasks( diagram ) )
        {
            List<Node> funNeedsWrapper = findFileGenerators( c );
            for( Node output : funNeedsWrapper )
            {
                String outName = "\'" + WorkflowUtil.getName( output ) + "\'";
                ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );
                String expression = info.getExpression();
                expression = "cp ${" + expression + "} " + WorkflowUtil.getName( output );
                String command = WorkflowUtil.getCommand( c );
                if( command.isEmpty() )
                    command = expression;
                else
                    command = command + "\n" + expression;
                WorkflowUtil.setCommand( c, command );
                info.setExpression( outName );
                info.setAST( new ExpressionParser().parseExpression( outName ) );
            }
        }
    }

    private static void wrapProcesses(Diagram diagram) throws Exception
    {
        for( Compartment c : WorkflowUtil.getAllCalls( diagram ) )
        {
            List<Node> funNeedsWrapper = needsWrapper( c );
            if( funNeedsWrapper != null )
            {
                for( Node node : funNeedsWrapper )
                {
                    Compartment task = WorkflowUtil.findTask( WorkflowUtil.getTaskRef( c ), diagram );
                    Node taskOutput = WorkflowUtil.findOutput( WorkflowUtil.getName( node ), task );

                    String type = WorkflowUtil.getType( node );
                    if( !type.equals( "File?" ) )
                    {
                        WorkflowUtil.setType( taskOutput, "File" );
                        WorkflowUtil.setType( node, "File" );
                    }

                    ExpressionInfo info = WorkflowUtil.getExpressionInfo( node );
                    String funName = findNeedWrapper( taskOutput ).toString();
                    List<String> newOutputs = replaceFunction( funName, info.getAST(), diagram, c, node );
                    info.setExpression( newOutputs.get( 0 ) );
                    info.setAST( new ExpressionParser().parseExpression( newOutputs.get( 0 ) ) );
                    WorkflowUtil.setExpressionInfo( taskOutput, info.clone() );
                }
            }
        }
    }

    private static void findArguments(biouml.plugins.wdl.parser.Node expression, List<biouml.plugins.wdl.parser.Node> arguments)
            throws Exception
    {
        if( expression instanceof AstText
                || ( expression instanceof AstRegularFormulaElement && ( (AstRegularFormulaElement)expression ).isVariable )
                || ( expression instanceof AstFunction && ( ( (AstFunction)expression ).toString().equals( "stdout" )
                        || ( (AstFunction)expression ).toString().equals( "stderr" ) ) ) )
            arguments.add( expression );
        for( int j = 0; j < expression.jjtGetNumChildren(); j++ )
        {
            biouml.plugins.wdl.parser.Node child = expression.jjtGetChild( j );
            findArguments( child, arguments );
        }
    }

    public static void findFunction(biouml.plugins.wdl.parser.Node expression, String name, List<AstFunction> result)
    {
        if( isFunction( expression, name ) )
        {

        }
        for( int j = 0; j < expression.jjtGetNumChildren(); j++ )
        {
            biouml.plugins.wdl.parser.Node child = expression.jjtGetChild( j );
            findFunction( child, name, result );
        }
    }

    private static List<String> replaceFunction(String funName, biouml.plugins.wdl.parser.Node expression, Diagram diagram,
            Compartment call, Node from) throws Exception
    {
        List<biouml.plugins.wdl.parser.Node> arguments = new ArrayList<>();

        //        biouml.plugins.wdl.parser.Node result = expression.getClass().getConstructor( int.class ).newInstance( expression.getId() );

        findArguments( expression, arguments );

        Map<String, Set<String>> skipArguments = new HashMap<>();
        skipArguments.put( "size", Set.of( "B", "K", "M", "G", "T", "Ki", "Mi", "Gi", "Ti" ) );


        Set<String> toSkip = skipArguments.containsKey( funName ) ? skipArguments.get( funName ) : new HashSet<>();

        int i = 1;
        for( biouml.plugins.wdl.parser.Node argument : arguments )
        {
            if( toSkip.contains( argument.toString() ) )
                continue;
            String name = "x" + i;

            if( argument instanceof AstText && ! ( argument.jjtGetParent().jjtGetParent() instanceof AstFunction ) )
            {
                continue;
            }
            AstSymbol replacement = new AstSymbol( WDLParserTreeConstants.JJTSYMBOL );
            replacement.jjtSetFirstToken( new Token( WDLParserTreeConstants.JJTSYMBOL, name ) );
            ParserUtil.replaceChild( (SimpleNode)argument.jjtGetParent(), argument, replacement );
            i++;
        }

        List<String> result = new ArrayList<>();
        for( biouml.plugins.wdl.parser.Node argument : arguments )
        {
            AstExpression parentExpression = new AstExpression( WDLParserTreeConstants.JJTEXPRESSION );
            parentExpression.jjtAddChild( argument, 0 );
            result.add( new ExpressionFormatter().format( parentExpression ) );
        }

        String rightHandSide = new WDLNextflowFormatter().format( (SimpleNode)expression );
        createWrapper( diagram, call, arguments, rightHandSide, from );
        return result;
    }

    private static void createWrapper(Diagram diagram, Compartment call, List<biouml.plugins.wdl.parser.Node> arguments, String expression,
            Node from) throws Exception
    {
        String inputName = WorkflowUtil.getCallName( call ) + "." + WorkflowUtil.getName( from );
        String resultName = WorkflowUtil.getResultName( call );
        if( resultName != null )
            inputName = resultName + "." + WorkflowUtil.getName( from );

        String fullExpression = null;
        if( arguments.size() == 1 )
            fullExpression = inputName + ".map { x1 -> " + expression + " }";
        else
            fullExpression = inputName + ".map { x1 -> " + expression + " }"; //TODO

        ExpressionProperties properties = new ExpressionProperties();
        String wrappedName = WorkflowUtil.getCallName( call ) + "_" + WorkflowUtil.getName( from ) + "_wrapped";
        properties.setRhs( fullExpression );
        properties.setName( wrappedName );
        properties.setVariable( wrappedName );
        DiagramElementGroup group = properties.createElements( call.getCompartment(), new Point(), null );
        Node expressionNode = (Node)group.getElement();
        call.getCompartment().put( expressionNode );

        for( Node node : from.edges().filter( e -> e.getInput().equals( from ) ).map( e -> e.getOtherEnd( from ) ) )
        {
            createLink( expressionNode, node );
            String nextExpression = WorkflowUtil.getExpression( node );
            nextExpression = nextExpression.replace( inputName, wrappedName );
            //           String name = WorkflowUtil.getName( from );
            WorkflowUtil.setExpression( node, nextExpression );
            AstExpression dec = new ExpressionParser().parseExpression( nextExpression );
            WorkflowUtil.getExpressionInfo( node ).setAST( dec );
        }

        for( Edge e : from.edges().toList() )
        {
            e.getOrigin().remove( e.getName() );
            from.removeEdge( e );
            e.getOtherEnd( from ).removeEdge( e );
        }

        createLink( from, expressionNode );
    }

    private static void processEmptyOutput(Diagram diagram)
    {
        String version = diagram.getAttributes().getValueAsString( WDLConstants.WDL_VERSION_ATTR );
        boolean isWDL10 = version.equals( "1.0" ) || version.equals( "development" );
        List<Node> globalOutputs = WorkflowUtil.getExternalOutputs( diagram );
        if( globalOutputs.isEmpty() && isWDL10 )
        {
            diagram.getAttributes().add( new DynamicProperty( "autoOutputs", Boolean.class, true ) );
            DiagramGenerator.addOutputs( diagram );
        }
    }

    private static AstFunction findFileGeneratorFunctions(Node output)
    {
        ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );

        AstExpression expression = info.getAST();

        for( biouml.plugins.wdl.parser.Node node : expression.getChildren() )
        {
            if( node instanceof AstFunction )
            {
                String name = ( (AstFunction)node ).toString();
                if( fileGenerators.contains( name ) )
                {
                    return (AstFunction)node;
                }
            }
        }
        return null;
    }

    private static AstFunction findNeedWrapper(Node output)
    {
        ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );

        AstExpression expression = info.getAST();

        for( biouml.plugins.wdl.parser.Node node : expression.getChildren() )
        {
            if( node instanceof AstFunction )
            {

                String name = ( (AstFunction)node ).toString();
                if( name.equals( "glob" ) ) //exclusion
                    continue;
                return (AstFunction)node;
            }
        }
        return null;
    }

    private static List<Node> findFileGenerators(Compartment compartment)
    {
        List<Node> result = new ArrayList<>();
        for( Node output : WorkflowUtil.getOutputs( compartment ) )
        {
            if( !WorkflowUtil.getType( output ).equals( "File" ) )
                continue;

            if( findFileGeneratorFunctions( output ) != null )
                result.add( output );

        }
        return result;
    }

    private static List<Node> needsWrapper(Compartment compartment)
    {
        List<Node> result = new ArrayList<>();
        for( Node output : WorkflowUtil.getOutputs( compartment ) )
        {
            if( WorkflowUtil.getType( output ).equals( "File" ) )
                continue;

            if( findNeedWrapper( output ) != null )
                result.add( output );

        }
        return result;
    }


    private static Edge createLink(Node from, Node to)
    {
        Edge edge = new Edge( new Stub( null, from.getName() + " interact " + to.getName(), WDLConstants.LINK_TYPE ), from, to );
        Node.findCommonOrigin( from, to ).put( edge );
        return edge;
    }

    public static String doubleBackslashesOutsidePlaceholders(String input)
    {
        if( input == null || input.isEmpty() )
            return input;

        StringBuilder result = new StringBuilder( input.length() * 2 );

        int index = 0;

        while( index < input.length() )
        {
            if( isPlaceholderStart( input, index ) )
            {
                int end = findPlaceholderEnd( input, index );

                if( end < 0 )
                {
                    result.append( input, index, input.length() );
                    break;
                }

                result.append( input, index, end + 1 );
                index = end + 1;
                continue;
            }

            char current = input.charAt( index );

            if( current == '\\' )
            {
                result.append( "\\\\" );
            }
            else
            {
                result.append( current );
            }

            index++;
        }

        return result.toString();
    }

    private static boolean isPlaceholderStart(String text, int index)
    {
        if( index + 1 >= text.length() )
        {
            return false;
        }

        char first = text.charAt( index );
        char second = text.charAt( index + 1 );

        return ( first == '$' || first == '~' ) && second == '{';
    }

    private static int findPlaceholderEnd(String text, int start)
    {
        int depth = 1;

        boolean insideSingleQuotedString = false;
        boolean insideDoubleQuotedString = false;
        boolean escaped = false;

        for( int index = start + 2; index < text.length(); index++ )
        {
            char current = text.charAt( index );

            if( insideSingleQuotedString || insideDoubleQuotedString )
            {
                if( escaped )
                {
                    escaped = false;
                    continue;
                }

                if( current == '\\' )
                {
                    escaped = true;
                    continue;
                }

                if( insideSingleQuotedString && current == '\'' )
                {
                    insideSingleQuotedString = false;
                }
                else if( insideDoubleQuotedString && current == '"' )
                {
                    insideDoubleQuotedString = false;
                }

                continue;
            }

            if( current == '\'' )
            {
                insideSingleQuotedString = true;
                continue;
            }

            if( current == '"' )
            {
                insideDoubleQuotedString = true;
                continue;
            }

            if( current == '{' )
            {
                depth++;
            }
            else if( current == '}' )
            {
                depth--;

                if( depth == 0 )
                {
                    return index;
                }
            }
        }

        return -1;
    }

    public static void main(String[] args)
    {
        String command = "prokka --outdir \"prokka_annotation\" " + "--prefix \"$(basename \"${contigs}\" | sed 's/\\..*//')\" "
                + "${contigs}\n" + "find $(pwd)/prokka_annotation -type f > outputs.txt";

        String converted = doubleBackslashesOutsidePlaceholders( command );

        System.out.println( "Before:" );
        System.out.println( command );

        System.out.println( "After:" );
        System.out.println( converted );

        System.out.println( "Same object value: " + command.equals( converted ) );
    }
}