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
import biouml.plugins.wdl.parser.AstText;
import biouml.plugins.wdl.parser.ExpressionParser;
import biouml.plugins.wdl.parser.GlobProcessor;
import biouml.plugins.wdl.parser.WDLParserTreeConstants;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;

public class NextFlowPreprocessor
{
    private static Set<String> fileGenerators = Set.of( "write_lines", "write_tsv", "write_map", "write_object", "write_objects",
            "write_json" );

    private String versionWDL = "1.2";
    private String publishDir = "";
    private Map<String, Diagram> allDiagrams = null;

    public void NextFlowPreprocessor()
    {

    }

    public void setAllDiagrams(Map<String, Diagram> diagrams)
    {
        this.allDiagrams = diagrams;
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

        wrapProcesses( result );
        processGlob( result );
        processSameTaskCall( result );
        processWorkflowOutputs( result );
        processConditionals( result );
        processFileGenerators( result );
        processEmptyOutput( result );
        processCollect( result );

        for( Compartment task : WorkflowUtil.getTasks( result ) )
        {
            if( publishDir.isEmpty() )
                WorkflowUtil.setRuntimeProperty( task, "publishDir", "\"" + task.getName() + "\"" );
            else
                WorkflowUtil.setRuntimeProperty( task, "publishDir", "\"" + publishDir + "/" + task.getName() + "\"" );

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
                hasStdout = hasStdout || processStdout( output );
                hasStdErr = hasStdErr || processStderr( output );
            }
            processBeforeCommand( diagram, task );
            processCommand( task, hasStdout, hasStdErr );
        }

        for( Node node : result.recursiveStream().select( Node.class ) )
        {
            String expression = null;
            ExpressionInfo info = WorkflowUtil.getExpressionInfo( node );
            if( info != null && info.getAST() != null )
                expression = new WDLNextflowFormatter().format( info.getAST() );

            if( expression == null )
                expression = WorkflowUtil.getExpression( node );

            if( expression == null || expression.isEmpty() )
                continue;
            Map<String, String> variables = new HashMap<>();
            if( WorkflowUtil.isCall( node.getCompartment() ) || WorkflowUtil.isTask( node.getCompartment() ) )
                variables = getVariables( node.getCompartment() );
            else
                variables = getVariables( WorkflowUtil.getEnclosingdWorkflow( node ) );
            expression = processSep( expression, node );
            expression = processExpression( expression, diagram, variables );
            WorkflowUtil.setExpression( node, expression );
        }
        return result;
    }

    private void processCommand(Compartment task, boolean hasStdout, boolean hasStdErr)
    {
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

        command = processSepCommand( command, task );
        command = processBooleanCommand( command, task );
        command = processDefaultCommand( command, task );
        command = processWDLFunctions( command, true );
        command = doubleBackslashesOutsidePlaceholders( command );
        command = procesRegexes( command );
        command = escapeSingleBuck( command );
        command = processVariables( command, getVariables( task ), true );
        command = removeEscape( command );
        WorkflowUtil.setCommand( task, command );
    }

    private void processBeforeCommand(Diagram diagram, Compartment task)
    {
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
                if( expression != null )
                {
                    expression = processExpression( expression, diagram, getVariables( task ) );
                    info.setExpression( expression );
                }
            }
        }
    }

    private boolean processStdout(Node output)
    {
        boolean hasStdout = false;
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
        }
        return hasStdout;
    }

    private boolean processStderr(Node output)
    {
        boolean hasStdErr = false;
        String expression = WorkflowUtil.parseExpression( output, "Nextflow" );
        if( expression != null && !expression.isEmpty() )
        {
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
        return hasStdErr;
    }

    private String processSep(String expression, Node node) throws Exception
    {
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

                Diagram diagram = Diagram.getDiagram( node );
                DiagramElementGroup deg = properties.createElements( diagram, new Point( 0, 0 ), null );
                Node newNode = (Node)deg.getElement();
                Set<String> arguments = new HashSet<>();
                arguments.add( name );
                WorkflowUtil.setArguments( newNode, arguments );
                diagram.put( newNode );
                Set<Node> argNodes = WorkflowUtil.findExpressionNodes( diagram, name );
                for( Node argNode : StreamEx.of( argNodes ) )
                {
                    Edge edge1 = new Edge( new Stub( null, argNode.getName() + " interact " + newNode.getName(), WDLConstants.LINK_TYPE ),
                            argNode, newNode );
                    diagram.put( edge1 );
                }
                Edge edge2 = new Edge( new Stub( null, newNode.getName() + " interact " + node.getName(), WDLConstants.LINK_TYPE ), newNode,
                        node );
                diagram.put( edge2 );
                expression = expression.replace( sep, "~{" + name + "_str}" );
            }
        }
        return expression;
    }

    private String processSepCommand(String command, Compartment task)
    {
        Set<String> seps = findSeps( command );
        Set<String> newNames = new HashSet<>();
        for( String sep : seps )
        {
            String name = getSepName( sep );
            String del = getSepDelimiter( sep );
            String newName = name + "_joined";
            int i = 1;
            while( newNames.contains( newName ) )
            {
                newName = name + "_joined_" + i;
                i++;
            }
            ExpressionInfo dec = new ExpressionInfo( "String", newName, "sep_wdl ( '" + del + "', " + name + ")" );
            WorkflowUtil.addBeforeCommand( task, dec );
            command = command.replace( sep, "~{" + newName + "}" );
            newNames.add( newName );
        }
        return command;
    }

    private String processBooleanCommand(String command, Compartment task)
    {
        Pattern pattern = Pattern
                .compile( "~\\{\\s*true\\s*=\\s*(\"[^\"]*\"|'[^']*')" + "\\s+false\\s*=\\s*(\"[^\"]*\"|'[^']*')" + "\\s+([^}]+)\\}" );

        Matcher matcher = pattern.matcher( command );
        StringBuffer result = new StringBuffer();
        int index = 1;
        while( matcher.find() )
        {
            String trueValue = matcher.group( 1 );
            String falseValue = matcher.group( 2 );
            String condition = matcher.group( 3 ).trim();
            String newName = "bool_" + index++;
            String expression = "(" + condition + ") ? " + trueValue + " : " + falseValue;
            ExpressionInfo dec = new ExpressionInfo( "String", newName, expression );
            WorkflowUtil.addBeforeCommand( task, dec );
            matcher.appendReplacement( result, Matcher.quoteReplacement( "~{" + newName + "}" ) );
        }
        matcher.appendTail( result );
        return result.toString();
    }

    private String processDefaultCommand(String command, Compartment task)
    {
        Pattern pattern = Pattern.compile( "~\\{\\s*default\\s*=\\s*(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')" + "\\s+([^}]+)\\}" );
        Matcher matcher = pattern.matcher( command );
        StringBuffer result = new StringBuffer();
        int index = 1;
        while( matcher.find() )
        {
            String defaultValue = matcher.group( 1 );
            String expression = matcher.group( 2 ).trim();
            String newName = "default_command_" + index++;
            ExpressionInfo dec = new ExpressionInfo( "String", newName, "select_first_wdl([" + expression + ", " + defaultValue + "])" );
            WorkflowUtil.addBeforeCommand( task, dec );
            matcher.appendReplacement( result, Matcher.quoteReplacement( "~{" + newName + "}" ) );
        }
        matcher.appendTail( result );
        return result.toString();
    }

    private Map<String, String> getVariables(Compartment task)
    {
        Map<String, String> variables = StreamEx.of( WorkflowUtil.getInputs( task ) ).toMap( input -> WorkflowUtil.getName( input ),
                input -> WorkflowUtil.getType( input ) );
        variables.putAll( StreamEx.of( WorkflowUtil.getBeforeCommandExpressions( task ) ).toMap( expression -> expression.getName(),
                expression -> expression.getType() ) );

        List<Node> expressions = task.recursiveStream().select( Node.class )
                .filter( n -> WorkflowUtil.isExpression( n ) || WorkflowUtil.isCycleVariable( n ) ).toList();
        for( Node expression : expressions )
        {
            String name = WorkflowUtil.getName( expression );
            if( name != null )
                variables.put( name, WorkflowUtil.getType( expression ) );
        }
        return variables;
    }

    private String processExpression(String expression, Diagram diagram, Map<String, String> variables)
    {
        expression = removeGlobs( expression );
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

    private static final Pattern SIMPLE_LITERAL_GLOB = Pattern
            .compile( "^\\s*glob\\s*\\(\\s*(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')\\s*\\)\\s*$", Pattern.DOTALL );

    private static boolean isSimpleLiteralGlob(String expression)
    {
        return expression != null && SIMPLE_LITERAL_GLOB.matcher( expression ).matches();
    }

    public static String removeGlobs(String input)
    {
        if( input == null )
            return null;
        Matcher matcher = SIMPLE_LITERAL_GLOB.matcher( input );
        // Only a whole, literal glob can be lowered into a Nextflow path pattern.
        // Replacing inner glob() calls breaks e.g. glob(read_lines(glob(...)[0])).
        return matcher.matches() ? matcher.group( 1 ) : input;
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
        for( Compartment conditional : diagram.recursiveStream().select( Compartment.class )
                .filter( c -> WorkflowUtil.isConditional( c ) ) )
        {
            if( WorkflowUtil.isInCycle( conditional ) )
                continue;
            for( Node node : conditional.getNodes() )
            {
                if( WorkflowUtil.isExpression( node ) )
                {
                    String name = WorkflowUtil.getName( node );
                    if( name != null )
                    {
                        String nodeName = DefaultSemanticController.generateUniqueName( diagram, node.getName() );
                        Node clone = node.clone( conditional.getCompartment(), nodeName );
                        WorkflowUtil.setExpressionInfo( clone, null );
                        WorkflowUtil.setExpression( clone, "null" );
                        conditional.getCompartment().put( clone );

                        Edge edge1 = new Edge( new Stub( null, clone.getName() + " interact " + node.getName(), WDLConstants.LINK_TYPE ),
                                clone, node );

                        conditional.getCompartment().put( edge1 );
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
            else if( !candidateVariables.containsKey( variable ) && !variable.contains( "?" ) )
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
        }

        replaced.clear();
        while( true )
        {
            List<String> tildaVariables = extractVariables( command, "~" );
            tildaVariables.removeAll( replaced );
            if( tildaVariables.isEmpty() )
                break;

            String variable = tildaVariables.get( 0 );
            String original = variable;
            if( variable.contains( "(" ) )//this is function
            {
                if( original.contains( "_wdl(" ) )
                    original = original.substring( 0, original.indexOf( "_wdl(" ) );
                else if( original.contains( "_bash(" ) )
                    original = original.substring( 0, original.indexOf( "_bash(" ) );
                if( StreamEx.of( NextFlowVelocityHelper.getWDLFunctions() ).toSet().contains( original ) )
                {
                    command = command.replace( "~{" + variable + "}", "${" + variable + "}" );
                }
            }
            else if( candidateVariables.containsKey( variable ) )
            {
                String type = candidateVariables.get( variable );
                if( "Directory".equals( type ) )
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
                .toFlatList( w -> WorkflowUtil.getAllCalls( w ) );


        //process aliases for NOT imported calls
        for( Compartment call : calls )
        {
            String alias = WorkflowUtil.getAlias( call );
            String taskName = WorkflowUtil.getTaskRef( call );

            if( WorkflowUtil.findTask( taskName, diagram ) != null && alias != null )
            {
                //                WorkflowUtil.setAlias( call, taskName );
                WorkflowUtil.setResultName( call, alias );
            }
        }

        for( Compartment call : calls )
        {
            String taskRef = WorkflowUtil.getTaskRef( call );
            if( WorkflowUtil.getAlias( call ) != null && !taskRef.equals( WorkflowUtil.getAlias( call ) ) )
            {
                Compartment task = WorkflowUtil.findTask( taskRef, diagram );
                if( task != null )
                {
                    Compartment copy = copyTask( task, WorkflowUtil.getAlias( call ) );//DefaultSemanticController.generateUniqueName( diagram, taskRef ) );
                    String newTaskName = WorkflowUtil.getName( copy );
                    WorkflowUtil.setTaskRef( call, newTaskName );
                }
            }
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

    private static void processWorkflowOutputs(Diagram diagram)
    {
        List<Compartment> workflows = WorkflowUtil.getWorkflows( diagram );
        workflows.add( diagram );
        for( Compartment workflow : workflows )
        {
            for( Node output : WorkflowUtil.getExternalOutputs( workflow ) )
            {
                ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );
                if( info.getExpression() != null )
                {
                    Node expressionNode = DiagramGenerator.createExpressionNode( diagram, info.clone() );
                    for( Edge edge : output.getEdges() )
                    {
                        if( edge.getOutput().equals( output ) )
                            edge.setOutput( expressionNode );
                        else
                            edge.setInput( expressionNode );
                        output.removeEdge( edge );
                        expressionNode.addEdge( edge );
                    }

                    DiagramGenerator.createLink( expressionNode, output );
                    info.setAST( null );
                    info.setExpression( null );
                    WorkflowUtil.setExpression( output, null );
                }
            }
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
                String expression = null;
                ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );
                if( info != null && info.getAST() != null )
                    expression = new WDLNextflowFormatter().format( info.getAST() );
                else
                    expression = info.getExpression();
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

    private void wrapProcesses(Diagram diagram) throws Exception
    {
        new OutputWrapperProcessor( allDiagrams ).process( diagram );
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

    private static void processGlob(Diagram result)
    {
        for( Node output : result.recursiveStream().select( Node.class ).filter( n -> WorkflowUtil.isExpression( n ) ) )
        {
            try
            {
                String expression = WorkflowUtil.getExpression( output );
                if( expression == null )
                    continue;
                // GlobProcessor rewrites simple glob patterns into file patterns.
                // Never run it on a compound expression: nested glob() calls
                // must survive until the post-task OutputWrapperProcessor wrapper.
                if( expression.contains( "glob_wdl(" ) || ( expression.contains( "glob(" ) && !isSimpleLiteralGlob( expression ) ) )
                    continue;
                expression = new GlobProcessor().process( expression );
                WorkflowUtil.setExpression( output, expression );
                ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );
                if( info != null )
                    info.setExpression( expression );
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }

        }
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

    public void processCollect(Diagram diagram) throws Exception
    {
        List<Edge> edges = diagram.recursiveStream().select( Edge.class ).toList();
        for( Edge e : edges )
        {
            createCollect( e );
        }
    }

    public static List<Compartment> shouldBeCollected(Node source, Node target)
    {
        List<Compartment> result = new ArrayList<>();
        boolean hasCycle = false;
        Compartment parent = source.getCompartment();
        while( !WorkflowUtil.isInside( target, parent ) )
        {
            if( WorkflowUtil.isCycle( parent ) )
                hasCycle = true;
            result.add( parent );
            parent = parent.getCompartment();
        }
        if( !hasCycle )
            return List.of();
        return result;
    }

    public void createCollect(Edge link) throws Exception
    {
        Node source = link.getInput();
        Node target = link.getOutput();

        List<Compartment> compartments = shouldBeCollected( source, target );

        if( compartments.isEmpty() )
            return;

        List<Compartment> cycles = new ArrayList<>();
        List<Compartment> conditionals = new ArrayList<>();

        for( Compartment compartment : compartments )
        {
            if( WorkflowUtil.isCycle( compartment ) )
            {
                cycles.add( compartment );
            }
            else if( WorkflowUtil.isConditional( compartment ) )
            {
                conditionals.add( compartment );
            }
        }

        if( cycles.isEmpty() )
            return;


        ExpressionInfo expressionInfo = new ExpressionInfo();


        String expression;

        String fullName = WorkflowUtil.getQualifiedName( source );

        String collectedName = fullName.replace( ".", "_" ) + "_collected";

        String collectSource = fullName;


        /*
         * Conditional call output already has a channel
         * reconstructed from context.
         */
        if( isConditionalCallOutputInsideScatter( source ) )
        {
            collectSource = fullName.replace( ".", "_" );
        }


        /*
         * One scatter:
         *
         * scatter(i) {
         *     ...
         * }
         *
         * result.toList()
         */
        if( cycles.size() == 1 )
        {
            expression = collectSource + ".toList()";
        }
        else
        {
            /*
             * Outer scatter is NOT passed to collectScatter.
             *
             * Example:
             *
             * scatter(i) {
             *     scatter(j) {
             *         result
             *     }
             * }
             *
             * ->
             *
             * collectScatter(
             *     result,
             *     [
             *         j_array
             *     ]
             * )
             */
            List<Compartment> nestedCycles = StreamEx.ofReversed( cycles ).skip( 1 ).toList();

            StringBuilder sb = new StringBuilder();
            sb.append( "collectScatter(\n" );
            sb.append( "    " );
            sb.append( collectSource );
            sb.append( ",\n" );
            sb.append( "    [\n" );
            sb.append( StreamEx.of( nestedCycles ).map( cycle -> "        " + WorkflowUtil.getCycleName( cycle ) ).joining( ",\n" ) );
            sb.append( "\n" );
            sb.append( "    ]\n" );
            sb.append( ")" );
            expression = sb.toString();
        }


        expressionInfo.setExpression( expression );
        expressionInfo.setName( collectedName );


        /*
         * Normally collect expression is created near target.
         *
         * But if source leaves a conditional and there is a nested
         * scatter inside that conditional, the collected expression
         * must remain INSIDE the conditional.
         *
         * Otherwise ScatterProcessor will never see it while
         * processing:
         *
         *     if (...) {
         *         scatter (...) {
         *             ...
         *         }
         *
         *         result_collected = ...
         *     }
         *
         * and therefore cannot call addConditionalValue().
         */
        Compartment parent;

        if( !conditionals.isEmpty() && cycles.size() > 1 )
        {
            /*
             * The conditional crossed by this link becomes
             * the owner of result_collected.
             *
             * For the current test this is the compartment
             * corresponding to:
             *
             *     if (i == 2)
             */
            parent = conditionals.get( conditionals.size() - 1 );
        }
        else
        {
            parent = target.getCompartment();

            if( WorkflowUtil.isCall( parent ) )
            {
                parent = parent.getCompartment();
            }
        }

        Node collectNode = DiagramGenerator.createExpressionNode( parent, expressionInfo );

        /*
         * Replace:
         *
         * source -----------------> target
         *
         * with:
         *
         * source -> collectNode -> target
         */
        source.removeEdge( link );
        target.removeEdge( link );
        link.getCompartment().remove( link.getName() );
        createLink( source, collectNode );
        createLink( collectNode, target );
        /*
         * Replace original source reference in target expression.
         */
        ExpressionInfo targetExpression = WorkflowUtil.getExpressionInfo( target );
        if( targetExpression != null )
        {
            String newExpression = targetExpression.getExpression().replace( fullName, collectedName );

            targetExpression.setExpression( newExpression );

            WorkflowUtil.setExpression( target, newExpression );
        }
    }

    private static boolean isConditionalCallOutputInsideScatter(Node source)
    {
        Compartment parent = source.getCompartment();

        if( !WorkflowUtil.isCall( parent ) )
            return false;

        boolean conditionalFound = false;
        boolean cycleFound = false;
        parent = parent.getCompartment();

        while( parent != null && ! ( parent instanceof Diagram ) )
        {
            if( WorkflowUtil.isConditional( parent ) )
                conditionalFound = true;

            if( WorkflowUtil.isCycle( parent ) )
                cycleFound = true;

            if( conditionalFound && cycleFound )
                return true;

            parent = parent.getCompartment();
        }
        return false;
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