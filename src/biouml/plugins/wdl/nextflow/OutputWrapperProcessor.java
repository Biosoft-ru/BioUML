package biouml.plugins.wdl.nextflow;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.diagram.ExpressionProperties;
import biouml.plugins.wdl.diagram.OutputProperties;
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

/**
 * Moves task output expressions requiring post-processing into an
 * expression node after a call.
 *
 * Each distinct run-time argument becomes a separate task + call
 * output; literals/operators/parentheses are not automatically
 * arguments.
 */
public class OutputWrapperProcessor
{
    private final Map<String, Diagram> allDiagrams;
    private static final Pattern IDENTIFIER = Pattern.compile( "[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*" );
    private static final Set<String> SIZE_UNITS = Set.of( "B", "K", "M", "G", "T", "Ki", "Mi", "Gi", "Ti" );
    private static final Pattern GLOB_CALL = Pattern.compile( "\\bglob(?:_wdl)?\\s*\\(" );

    private static class Argument
    {
        final String sourceExpression;
        final String variable;
        final String type;

        String outputName;

        Argument(String sourceExpression, String variable, String type)
        {
            this.sourceExpression = sourceExpression;
            this.variable = variable;
            this.type = type;
        }
    }

    public OutputWrapperProcessor(Map<String, Diagram> allDiagrams)
    {
        this.allDiagrams = allDiagrams;
    }

    public void process(Diagram diagram) throws Exception
    {
        for( Compartment call : WorkflowUtil.getAllCalls( diagram ) )
        {
            Compartment callee = WorkflowUtil.findCallee( call, allDiagrams );

            if( callee == null || callee instanceof Diagram )
                continue;

            // Snapshot: we add outputs to the call while processing.
            List<Node> originalOutputs = new ArrayList<>( WorkflowUtil.getOutputs( call ) );

            for( Node from : originalOutputs )
            {
                if( "File".equals( WorkflowUtil.getType( from ) ) )
                    continue;

                AstFunction function = findNeedWrapper( from );

                if( function != null )
                    wrap( diagram, call, from, function );
            }
        }
    }

    private void wrap(Diagram diagram, Compartment call, Node from, AstFunction function) throws Exception
    {
        Compartment task = WorkflowUtil.findCallee( call, allDiagrams );
        if( task == null )
            throw new IllegalStateException( "Cannot find callee: " + WorkflowUtil.getCallName( call ) );

        String name = WorkflowUtil.getName( from );

        // Preserve original WDL output type before replacing the output
        // with a technical Nextflow output.
        String originalType = WorkflowUtil.getType( from );
        Node taskOutput = findTaskOutput( task, name );

        if( taskOutput == null )
            throw new IllegalStateException( "Cannot find task output: " + name );

        ExpressionInfo fromInfo = WorkflowUtil.getExpressionInfo( from );
        AstExpression ast = fromInfo == null ? null : fromInfo.getAST();
        if( ast == null )
            return;

        /*
         * A dynamic/nested glob must be evaluated after the task,
         * in its work directory.
         *
         * Reserve x1 for a small file staged from that directory.
         * This is also needed when the WDL expression contains no
         * variables at all (globRecursion).
         */
        boolean hasGlob = containsFunction( ast, "glob" );
        LinkedHashMap<String, Argument> unique = new LinkedHashMap<>();
        if( hasGlob )
        {
            String anchor = ".biouml_glob_context_" + name.replaceAll( "[^A-Za-z0-9_-]", "_" );
            String anchorExpression = "\"" + anchor + "\"";
            unique.put( anchorExpression, new Argument( anchorExpression, "x1", "File" ) );
            ensureGlobAnchor( task, anchor );
        }

        collectAndReplace( ast, task, unique, function.toString() );
        if( unique.isEmpty() )
            return;

        String expression = new WDLNextflowFormatter().format( ast );

        if( hasGlob )
        {
            expression = addGlobBases( expression, "x1.parent" );
            /*
             * WDL optional File outputs may refer to a file that does
             * not exist.
             *
             * For File?, normalize the evaluated output:
             *   existing file -> absolute Path
             *   missing file  -> NO_VALUE
             *
             * This must happen before the generated workflow evaluates
             * defined_wdl() or read_string_wdl().
             */
            if( "File?".equals( originalType ) )
                expression = "optFile(" + expression + ", x1.parent)";
        }

        List<Argument> args = new ArrayList<>( unique.values() );
        List<Node> sources = new ArrayList<>();
        List<String> channels = new ArrayList<>();

        for( int i = 0; i < args.size(); i++ )
        {
            Argument arg = args.get( i );
            String outputName = ( i == 0 ) ? name : chooseOutputName( task, call, name + "_arg" + ( i + 1 ), arg.sourceExpression );
            arg.outputName = outputName;
            Node existingTaskOutput = findTaskOutput( task, outputName );
            Node taskArg = ( i == 0 ) ? taskOutput
                    : existingTaskOutput != null ? existingTaskOutput : createOutput( task, outputName, arg.type, arg.sourceExpression );
            Node callArg = ( i == 0 ) ? from : createOutput( call, outputName, arg.type, arg.sourceExpression );
            setOutput( taskArg, outputName, arg.type, arg.sourceExpression );
            setOutput( callArg, outputName, arg.type, arg.sourceExpression );
            sources.add( callArg );
            String resultName = WorkflowUtil.getResultName( call );
            channels.add( resultName != null ? resultName + "." + outputName : WorkflowUtil.getCallName( call ) + "." + outputName );
        }

        String fullExpression;

        if( WorkflowUtil.isInCycle( call ) )
        {
            /*
             * ScatterProcessor uses the call outputs already
             * available in context.
             */
            fullExpression = expression;
        }
        else if( args.size() == 1 )
        {
            fullExpression = channels.get( 0 ) + ".map { x1 -> " + expression + " }";
        }
        else
        {
            StringBuilder body = new StringBuilder();
            for( int i = 0; i < args.size(); i++ )
            {
                body.append( "def x" ).append( i + 1 ).append( " = values[" ).append( i ).append( "]; " );
            }
            fullExpression = "mergeAll([" + String.join( ", ", channels ) + "]).map { values -> " + body + expression + " }";
        }

        String wrappedName = WorkflowUtil.getCallName( call ) + "_" + name + "_wrapped";
        ExpressionProperties properties = new ExpressionProperties();
        properties.setName( wrappedName );
        properties.setVariable( wrappedName );
        properties.setRhs( fullExpression );
        DiagramElementGroup group = properties.createElements( call.getCompartment(), new Point(), null );
        Node wrapper = (Node)group.getElement();
        call.getCompartment().put( wrapper );

        /*
         * Store source order for ScatterProcessor.
         * Edge iteration order is not reliable.
         */
        String argNames = String.join( ",", args.stream().map( arg -> arg.outputName ).toArray( String[]::new ) );
        wrapper.getAttributes().add( new DynamicProperty( "wrappedSources", String.class, argNames ) );
        String oldName = WorkflowUtil.getCallName( call ) + "." + name;
        String alias = WorkflowUtil.getResultName( call );

        /*
         * Redirect only outgoing links, preserving the existing
         * diagram transformation.
         */
        List<Edge> edges = from.edges().toList();
        for( Edge edge : edges )
        {
            if( !edge.getInput().equals( from ) )
                continue;

            Node target = edge.getOtherEnd( from );
            createLink( wrapper, target );
            String oldExpression = WorkflowUtil.getExpression( target );

            if( oldExpression != null )
            {
                String updated = oldExpression.replace( oldName, wrappedName );
                if( alias != null )
                    updated = updated.replace( alias + "." + name, wrappedName );

                WorkflowUtil.setExpression( target, updated );
                ExpressionInfo info = WorkflowUtil.getExpressionInfo( target );
                if( info != null )
                {
                    info.setExpression( updated );
                    // AST would otherwise override the new expression.
                    info.setAST( null );
                }
            }
        }

        for( Edge edge : edges )
        {
            edge.getOrigin().remove( edge.getName() );
            from.removeEdge( edge );
            edge.getOtherEnd( from ).removeEdge( edge );
        }

        for( Node source : sources )
            createLink( source, wrapper );
    }

    private static boolean containsFunction(biouml.plugins.wdl.parser.Node node, String name)
    {
        if( node instanceof AstFunction && name.equals( node.toString() ) )
            return true;

        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            if( containsFunction( node.jjtGetChild( i ), name ) )
                return true;
        }

        return false;
    }

    private static void ensureGlobAnchor(Compartment task, String anchor)
    {
        String command = WorkflowUtil.getCommand( task );
        String marker = "touch '" + anchor + "'";
        if( command == null )
            command = "";

        if( !command.contains( marker ) )
        {
            WorkflowUtil.setCommand( task, command + "\n" + marker );
        }
    }

    /**
     * Add the work directory to EVERY glob call,
     * including nested glob calls.
     *
     * Work from right to left, so inserting arguments
     * does not invalidate offsets.
     *
     * x1 is the anchor file emitted by the task.
     */
    private static String addGlobBases(String expression, String workDir)
    {
        Matcher matcher = GLOB_CALL.matcher( expression );
        List<Integer> starts = new ArrayList<>();
        while( matcher.find() )
        {
            if( !isInsideQuotedString( expression, matcher.start() ) )
                starts.add( matcher.start() );
        }

        String result = expression;
        for( int i = starts.size() - 1; i >= 0; i-- )
        {
            int start = starts.get( i );
            int open = result.indexOf( '(', start );
            if( open < 0 )
                continue;
            int close = findClosingParenthesis( result, open );
            if( close < 0 )
                throw new IllegalArgumentException( "Unbalanced glob expression: " + result );

            if( !hasTopLevelComma( result, open + 1, close ) )
                result = result.substring( 0, close ) + ", " + workDir + result.substring( close );
        }
        return result;
    }

    private static boolean isInsideQuotedString(String s, int position)
    {
        char quote = 0;
        boolean escape = false;

        for( int i = 0; i < position; i++ )
        {
            char c = s.charAt( i );

            if( escape )
            {
                escape = false;
                continue;
            }

            if( quote != 0 && c == '\\' )
            {
                escape = true;
                continue;
            }

            if( quote != 0 )
            {
                if( c == quote )
                    quote = 0;
            }
            else if( c == '\'' || c == '"' )
            {
                quote = c;
            }
        }

        return quote != 0;
    }

    private static int findClosingParenthesis(String s, int open)
    {
        int depth = 0;
        char quote = 0;
        boolean escape = false;

        for( int i = open; i < s.length(); i++ )
        {
            char c = s.charAt( i );

            if( escape )
            {
                escape = false;
                continue;
            }

            if( quote != 0 )
            {
                if( c == '\\' )
                    escape = true;
                else if( c == quote )
                    quote = 0;

                continue;
            }

            if( c == '\'' || c == '"' )
            {
                quote = c;
            }
            else if( c == '(' )
            {
                depth++;
            }
            else if( c == ')' && --depth == 0 )
            {
                return i;
            }
        }

        return -1;
    }

    private static boolean hasTopLevelComma(String s, int begin, int end)
    {
        int depth = 0;
        char quote = 0;
        boolean escape = false;

        for( int i = begin; i < end; i++ )
        {
            char c = s.charAt( i );

            if( escape )
            {
                escape = false;
                continue;
            }

            if( quote != 0 )
            {
                if( c == '\\' )
                    escape = true;
                else if( c == quote )
                    quote = 0;

                continue;
            }

            if( c == '\'' || c == '"' )
            {
                quote = c;
            }
            else if( c == '(' || c == '[' || c == '{' )
            {
                depth++;
            }
            else if( c == ')' || c == ']' || c == '}' )
            {
                depth--;
            }
            else if( c == ',' && depth == 0 )
            {
                return true;
            }
        }

        return false;
    }

    private static Node findTaskOutput(Compartment task, String name)
    {
        return task instanceof Diagram ? WorkflowUtil.findExternalOutput( name, task ) : WorkflowUtil.findOutput( name, task );
    }

    private static String chooseOutputName(Compartment task, Compartment call, String candidate, String expression)
    {
        String name = candidate;
        int suffix = 2;

        while( true )
        {
            Node existing = findTaskOutput( task, name );
            boolean sameTaskOutput = existing != null && expression.equals( WorkflowUtil.getExpression( existing ) );
            if( ( existing == null || sameTaskOutput ) && WorkflowUtil.findOutput( name, call ) == null )
                return name;
            name = candidate + "_" + suffix++;
        }
    }

    private static Node createOutput(Compartment parent, String name, String type, String rhs) throws Exception
    {
        OutputProperties properties = new OutputProperties();
        properties.setName( name );
        properties.setVariable( name );
        properties.setRhs( rhs );
        DiagramElementGroup group = properties.createElements( parent, new Point(), null );
        Node node = (Node)group.getElement();
        parent.put( node );
        return node;
    }

    private static void setOutput(Node output, String name, String type, String rhs) throws Exception
    {
        WorkflowUtil.setType( output, type );
        WorkflowUtil.setExpression( output, rhs );
        ExpressionInfo info = new ExpressionInfo( type, name, rhs );
        info.setAST( new ExpressionParser().parseExpression( rhs ) );
        WorkflowUtil.setExpressionInfo( output, info );
    }

    private void collectAndReplace(biouml.plugins.wdl.parser.Node node, Compartment task, Map<String, Argument> args, String outerFunction)
            throws Exception
    {
        if( isStdStream( node ) )
        {
            replaceArgument( node, task, args, outerFunction );
            return;
        }

        if( node instanceof AstRegularFormulaElement && ( (AstRegularFormulaElement)node ).isVariable )
        {
            String name = node.toString().trim();
            if( !SIZE_UNITS.contains( name ) || !"size".equals( outerFunction ) )
            {
                if( IDENTIFIER.matcher( name ).matches() && !Set.of( "true", "false", "null", "None" ).contains( name ) )
                {
                    replaceArgument( node, task, args, outerFunction );
                }
            }
            return;
        }

        if( node instanceof AstText && isDirectFileArgument( node ) )
        {
            /*
             * Literal file paths passed to read_*() / size()
             * need staging.
             *
             * Other literals (including size units, delimiters
             * and regexes) stay in the wrapper.
             */
            String value = node.toString().trim();

            if( !value.isEmpty() && !Set.of( "(", ")", ",", "[", "]", "{", "}" ).contains( value ) )
            {
                replaceArgument( node, task, args, outerFunction );
            }
            return;
        }

        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            collectAndReplace( node.jjtGetChild( i ), task, args, outerFunction );
        }
    }

    private static boolean isDirectFileArgument(biouml.plugins.wdl.parser.Node node)
    {
        if( ! ( node.jjtGetParent() instanceof AstExpression ) )
            return false;

        biouml.plugins.wdl.parser.Node arg = node.jjtGetParent();
        if( arg.jjtGetNumChildren() != 1 || ! ( arg.jjtGetParent() instanceof AstFunction ) )
            return false;

        AstFunction function = (AstFunction)arg.jjtGetParent();
        String name = function.toString();

        if( !name.startsWith( "read_" ) && !"size".equals( name ) )
            return false;
        //Function syntax: '(' at child 0, first argument at child 1.
        return function.jjtGetNumChildren() > 1 && function.jjtGetChild( 1 ) == arg;
    }

    private static boolean isStdStream(biouml.plugins.wdl.parser.Node node)
    {
        return node instanceof AstFunction && ( "stdout".equals( node.toString() ) || "stderr".equals( node.toString() ) );
    }

    private static void replaceArgument(biouml.plugins.wdl.parser.Node node, Compartment task, Map<String, Argument> args,
            String outerFunction) throws Exception
    {
        if( ! ( node.jjtGetParent() instanceof SimpleNode ) )
            throw new IllegalStateException( "Missing AST parent for " + node );
        /*
         * formatSource() temporarily wraps this AST node;
         * keep its original parent.
         */
        SimpleNode parent = (SimpleNode)node.jjtGetParent();
        boolean directFileArgument = isDirectFileArgument( node );
        String source = formatSource( node );
        Argument arg = args.get( source );
        if( arg == null )
        {
            String type = directFileArgument ? "File" : inferType( task, source, outerFunction );
            arg = new Argument( source, "x" + ( args.size() + 1 ), type );
            args.put( source, arg );
        }
        AstSymbol replacement = new AstSymbol( WDLParserTreeConstants.JJTSYMBOL );
        replacement.jjtSetFirstToken( new Token( WDLParserTreeConstants.JJTSYMBOL, arg.variable ) );
        ParserUtil.replaceChild( parent, node, replacement );
    }

    private static String formatSource(biouml.plugins.wdl.parser.Node node)
    {
        if( isStdStream( node ) )
            return node.toString() + "()";

        AstExpression fragment = new AstExpression( WDLParserTreeConstants.JJTEXPRESSION );
        fragment.jjtAddChild( node, 0 );
        return new ExpressionFormatter().format( fragment );
    }

    private static String inferType(Compartment task, String source, String outerFunction)
    {
        if( "stdout()".equals( source ) || "stderr()".equals( source ) )
        {
            return "File";
        }

        for( Node input : WorkflowUtil.getInputs( task ) )
        {
            if( source.equals( WorkflowUtil.getName( input ) ) )
                return WorkflowUtil.getType( input );
        }

        for( ExpressionInfo info : WorkflowUtil.getBeforeCommandExpressions( task ) )
        {
            if( source.equals( info.getName() ) )
                return info.getType();
        }

        for( Node output : WorkflowUtil.getOutputs( task ) )
        {
            if( source.equals( WorkflowUtil.getName( output ) ) )
                return WorkflowUtil.getType( output );
        }

        if( source.startsWith( "\"" ) || source.startsWith( "'" ) )
        {
            return outerFunction.startsWith( "read_" ) || "size".equals( outerFunction ) ? "File" : "String";
        }

        if( source.matches( "-?\\d+" ) )
            return "Int";

        if( source.matches( "-?\\d+\\.\\d+" ) )
            return "Float";

        throw new IllegalArgumentException( "Cannot determine type of wrapper argument: " + source );
    }

    private static AstFunction findNeedWrapper(Node output)
    {
        ExpressionInfo info = WorkflowUtil.getExpressionInfo( output );
        AstExpression expression = info == null ? null : info.getAST();

        if( expression == null )
            return null;
        /*
         * Only a bare glob("literal") is translated
         * to a Nextflow path pattern.
         */
        biouml.plugins.wdl.parser.Node[] children = expression.getChildren();
        if( children.length == 1 && children[0] instanceof AstFunction )
        {
            AstFunction function = (AstFunction)children[0];

            if( "glob".equals( function.toString() ) && hasOneTextArgument( function ) )
                return null;
        }

        for( biouml.plugins.wdl.parser.Node node : children )
        {
            if( node instanceof AstFunction )
                return (AstFunction)node;
        }
        return null;
    }

    private static boolean hasOneTextArgument(AstFunction func)
    {
        biouml.plugins.wdl.parser.Node[] children = func.getChildren();
        return children.length == 3 && children[1] instanceof AstExpression && children[1].jjtGetNumChildren() == 1
                && children[1].jjtGetChild( 0 ) instanceof AstText;
    }

    private static Edge createLink(Node from, Node to)
    {
        Edge edge = new Edge( new Stub( null, from.getName() + " interact " + to.getName(), WDLConstants.LINK_TYPE ), from, to );
        Node.findCommonOrigin( from, to ).put( edge );
        return edge;
    }
}