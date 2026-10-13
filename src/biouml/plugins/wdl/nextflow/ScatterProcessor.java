package biouml.plugins.wdl.nextflow;

import java.util.ArrayList;
import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.parser.VariableRenamer;
import one.util.streamex.StreamEx;


public class ScatterProcessor extends NextFlowVelocityHelper
{
    private int conditionalContextIndex = 0;
    private int scatterContextIndex = 0;


    public ScatterProcessor(Diagram diagram)
    {
        super( diagram );
    }


    public String printCycle(Compartment cycle)
    {
        StringBuilder sb = new StringBuilder();

        List<Node> contextNodes = new ArrayList<>();

        Node cycleVariable = WorkflowUtil.getCycleVariableNode( cycle );

        contextNodes.add( cycleVariable );

        conditionalContextIndex = 0;
        scatterContextIndex = 0;

        sb.append( "\n context = createScatter(" );
        sb.append( getCycleName( cycle ) );
        sb.append( ")" );

        printCycleContent( cycle, contextNodes, sb, "context" );

        return sb.toString();
    }


    private void printCycleContent(Compartment cycle, List<Node> contextNodes, StringBuilder sb, String contextName)
    {
        List<Node> nodes = WorkflowUtil.orderCallsScatters( cycle );

        for( Node node : nodes )
        {
            if( isExpression( node ) )
            {
                printExpressionInCycle( node, contextNodes, sb, contextName );
            }
            else if( isCall( node ) )
            {
                printCallInCycle( (Compartment)node, contextNodes, sb, new ArrayList<>(), contextName );
            }
            else if( isConditional( node ) )
            {
                printConditionalInCycle( (Compartment)node, contextNodes, sb, new ArrayList<>(), contextName );
            }
            else if( WorkflowUtil.isCycle( node ) )
            {
                printNestedCycle( (Compartment)node, contextNodes, sb, contextName );
            }
        }
    }


    private void printNestedCycle(Compartment cycle, List<Node> contextNodes, StringBuilder sb, String contextName)
    {
        Node cycleVariable = WorkflowUtil.getCycleVariableNode( cycle );

        sb.append( "\n " );
        sb.append( contextName );
        sb.append( " = addScatter(" );
        sb.append( contextName );
        sb.append( ", " );
        sb.append( getCycleName( cycle ) );
        sb.append( ")" );

        contextNodes.add( cycleVariable );

        printCycleContent( cycle, contextNodes, sb, contextName );
    }


    private void printConditionalNestedCycle(Compartment cycle, List<Node> contextNodes, StringBuilder sb, String conditionalContextName)
    {
        Node cycleVariable = WorkflowUtil.getCycleVariableNode( cycle );

        scatterContextIndex++;

        String scatterContextName;

        if( scatterContextIndex == 1 )
        {
            scatterContextName = "scatter_context";
        }
        else
        {
            scatterContextName = "scatter_" + scatterContextIndex + "_context";
        }

        sb.append( "\n " );
        sb.append( scatterContextName );
        sb.append( " = addScatter(" );
        sb.append( conditionalContextName );
        sb.append( ", " );
        sb.append( getCycleName( cycle ) );
        sb.append( ")" );

        contextNodes.add( cycleVariable );

        printCycleContent( cycle, contextNodes, sb, scatterContextName );
    }


    public void printExpressionInCycle(Node node, List<Node> contextNodes, StringBuilder sb)
    {
        printExpressionInCycle( node, contextNodes, sb, "context" );
    }


    private void printExpressionInCycle(Node node, List<Node> contextNodes, StringBuilder sb, String contextName)
    {
        String name = getName( node );

        sb.append( "\n " );


        if( name.endsWith( "_collected" ) )
        {
            sb.append( name );
            sb.append( " = " );
            sb.append( getExpression( node ) );

            return;
        }


        if( name.endsWith( "_wrapped" ) )
        {
            printWrapperInCycle( node, contextNodes, sb, null, contextName );

            return;
        }


        String expression = prepareExpression( getCallEmit( node ), contextNodes );

        sb.append( name );
        sb.append( " = " );

        sb.append( printContextMap( expression, contextNodes, contextName ) );


        sb.append( "\n " );
        sb.append( contextName );
        sb.append( " = addValue(" );
        sb.append( contextName );
        sb.append( ", " );
        sb.append( name );
        sb.append( ")" );


        contextNodes.add( node );
    }


    private void printConditionalCollectedExpression(Node node, List<Node> parentContextNodes, StringBuilder sb, String parentContextName,
            String conditionalContextName)
    {
        String name = getName( node );

        /*
         * Temporary channel name.
         *
         * Important:
         * this is NOT the same as the closure argument.
         */
        String collectedValueName = name + "_value";


        sb.append( "\n " );
        sb.append( collectedValueName );
        sb.append( " = " );
        sb.append( getExpression( node ) );


        /*
         * Restore missing rows into parent context.
         */
        sb.append( "\n " );
        sb.append( parentContextName );
        sb.append( " = addConditionalValue(" );
        sb.append( parentContextName );
        sb.append( ", " );
        sb.append( collectedValueName );
        sb.append( ", " );
        sb.append( conditionalContextName );
        sb.append( ")" );


        /*
         * From now on result_collected is represented
         * inside parent context.
         */
        parentContextNodes.add( node );


        /*
         * Closure argument is:
         *
         * result_collected_v
         *
         * not:
         *
         * result_collected_value
         */
        sb.append( "\n " );
        sb.append( name );
        sb.append( " = " );

        sb.append( printContextMap( getContextArgumentName( node ), parentContextNodes, parentContextName ) );

        sb.append( ".toList()" );
    }


    private void printWrapperInCycle(Node node, List<Node> contextNodes, StringBuilder sb, String condition)
    {
        printWrapperInCycle( node, contextNodes, sb, condition, "context" );
    }


    private void printWrapperInCycle(Node node, List<Node> contextNodes, StringBuilder sb, String condition, String contextName)
    {
        String name = getName( node );

        String expression = WorkflowUtil.getExpression( node );

        Node source = WorkflowUtil.getSources( node ).toList().get( 0 );

        String sourceValueName = getContextArgumentName( source );

        expression = renameVariable( expression, "x1", sourceValueName );

        if( condition != null )
        {
            expression = sourceValueName + " != \"NO_VALUE\" ? " + expression + " : \"NO_VALUE\"";
        }

        sb.append( name );
        sb.append( " = " );

        sb.append( printContextMap( expression, contextNodes, contextName ) );

        sb.append( "\n " );
        sb.append( contextName );
        sb.append( " = addValue(" );
        sb.append( contextName );
        sb.append( ", " );
        sb.append( name );
        sb.append( ")" );

        contextNodes.add( node );
    }


    public void printCallInCycle(Compartment call, List<Node> contextNodes, StringBuilder sb)
    {
        printCallInCycle( call, contextNodes, sb, new ArrayList<>(), "context" );
    }


    private void printCallInCycle(Compartment call, List<Node> contextNodes, StringBuilder sb, List<Compartment> conditionals,
            String parentContextName)
    {
        List<Node> inputs = WorkflowUtil.getOrderedInputs( call );

        List<String> inputNames = new ArrayList<>();

        List<String> inputDeclarations = new ArrayList<>();

        sb.append( "\n" );


        String contextName = parentContextName;

        String condition = null;


        if( !conditionals.isEmpty() )
        {
            String originalCondition = StreamEx.of( conditionals ).map( c -> findCondition( c ) ).joining( "&&" );

            condition = prepareExpression( originalCondition, contextNodes );

            contextName = getCallName( call ) + "_context";


            sb.append( " " );
            sb.append( contextName );
            sb.append( " = condition(" );
            sb.append( parentContextName );
            sb.append( ") { " );
            sb.append( getContextArguments( contextNodes ) );
            sb.append( " -> " );
            sb.append( condition );
            sb.append( " }\n" );
        }


        for( Node input : inputs )
        {
            List<Node> cycledSources = getCycledSources( input );

            if( cycledSources.isEmpty() && conditionals.isEmpty() )
            {
                inputNames.add( getCallInputName( input ) );
            }
            else
            {
                String inputName = createInputName( input );

                inputDeclarations.add( inputName + " = " + printInputInCycle( input, contextNodes, contextName ) );

                inputNames.add( inputName );
            }
        }


        if( !inputDeclarations.isEmpty() )
        {
            sb.append( StreamEx.of( inputDeclarations ).joining( "\n ", " ", "" ) );

            sb.append( "\n" );
        }


        String resultName = WorkflowUtil.getResultName( call );

        sb.append( " " );

        if( resultName != null )
        {
            sb.append( resultName );
            sb.append( " = " );
        }

        sb.append( getCallName( call ) );

        sb.append( "( " );

        sb.append( StreamEx.of( inputNames ).joining( ", " ) );

        sb.append( " )" );


        addCallOutputsToContext( call, contextNodes, sb, condition == null ? null : contextName, parentContextName );
    }


    private String printInputInCycle(Node input, List<Node> contextNodes)
    {
        return printInputInCycle( input, contextNodes, "context" );
    }


    private String printInputInCycle(Node input, List<Node> contextNodes, String contextName)
    {
        String expression = prepareExpression( getCallEmit( input ), contextNodes );

        return printContextMap( expression, contextNodes, contextName );
    }


    private String printContextMap(String expression, List<Node> contextNodes)
    {
        return printContextMap( expression, contextNodes, "context" );
    }


    private String printContextMap(String expression, List<Node> contextNodes, String contextName)
    {
        return "calcInScatter(" + contextName + ") { " + getContextArguments( contextNodes ) + " -> " + expression + " }";
    }


    private String createInputName(Node input)
    {
        return getCallName( input.getCompartment() ) + "_input_" + getName( input );
    }


    private void addCallOutputsToContext(Compartment call, List<Node> contextNodes, StringBuilder sb, String conditionalContext,
            String parentContextName)
    {
        List<Node> outputs = WorkflowUtil.getOutputs( call );

        if( outputs.isEmpty() )
            return;


        if( outputs.size() == 1 )
        {
            String outputChannel = getCallOutputChannel( call, outputs.get( 0 ) );

            if( conditionalContext == null )
            {
                sb.append( "\n " );
                sb.append( parentContextName );
                sb.append( " = addValue(" );
                sb.append( parentContextName );
                sb.append( ", " );
                sb.append( outputChannel );
                sb.append( ")" );
            }
            else
            {
                sb.append( "\n " );
                sb.append( parentContextName );
                sb.append( " = addConditionalValue(" );
                sb.append( parentContextName );
                sb.append( ", " );
                sb.append( outputChannel );
                sb.append( ", " );
                sb.append( conditionalContext );
                sb.append( ")" );
            }
        }
        else
        {
            String outputChannels = StreamEx.of( outputs ).map( output -> getCallOutputChannel( call, output ) ).joining( ", " );

            if( conditionalContext == null )
            {
                sb.append( "\n " );
                sb.append( parentContextName );
                sb.append( " = addValues(" );
                sb.append( parentContextName );
                sb.append( ", [" );
                sb.append( outputChannels );
                sb.append( "])" );
            }
            else
            {
                sb.append( "\n " );
                sb.append( parentContextName );
                sb.append( " = addConditionalValues(" );
                sb.append( parentContextName );
                sb.append( ", [" );
                sb.append( outputChannels );
                sb.append( "], " );
                sb.append( conditionalContext );
                sb.append( ")" );
            }
        }


        /*
         * Outputs are now represented inside parent context.
         */
        contextNodes.addAll( outputs );


        /*
         * Restore standalone output channels.
         *
         * Example:
         *
         * make_file_deep_outfile =
         *     calcInScatter(context) {
         *         i_v,make_file_deep_outfile_v ->
         *             make_file_deep_outfile_v
         *     }
         *
         * This keeps old generated expressions such as
         *
         * make_file_deep_outfile.toList()
         *
         * working.
         */
        for( Node output : outputs )
        {
            String outputName = getCallName( call ) + "_" + getName( output );

            sb.append( "\n " );
            sb.append( outputName );
            sb.append( " = " );

            sb.append( printContextMap( getContextArgumentName( output ), contextNodes, parentContextName ) );
        }
    }


    private String getCallOutputChannel(Compartment call, Node output)
    {
        String resultName = WorkflowUtil.getResultName( call );

        if( resultName != null )
        {
            return resultName + "." + getName( output );
        }

        return getCallName( call ) + ".out." + getName( output );
    }


    public void printConditionalInCycle(Compartment conditional, List<Node> contextNodes, StringBuilder sb)
    {
        printConditionalInCycle( conditional, contextNodes, sb, new ArrayList<>(), "context" );
    }


    private void printConditionalInCycle(Compartment conditional, List<Node> contextNodes, StringBuilder sb,
            List<Compartment> parentConditionals, String parentContextName)
    {
        List<Compartment> conditionals = new ArrayList<>( parentConditionals );

        conditionals.add( conditional );


        List<Node> nodes = WorkflowUtil.orderCallsScatters( conditional );


        boolean hasNestedCycle = false;

        for( Node node : nodes )
        {
            if( WorkflowUtil.isCycle( node ) )
            {
                hasNestedCycle = true;

                break;
            }
        }


        if( hasNestedCycle )
        {
            printConditionalWithNestedCycle( conditional, contextNodes, sb, conditionals, parentContextName, nodes );

            return;
        }


        /*
         * Old behavior for normal conditional.
         */
        for( Node node : nodes )
        {
            if( isExpression( node ) )
            {
                String condition = StreamEx.of( conditionals ).map( c -> findCondition( c ) ).joining( "&&" );

                condition = prepareExpression( condition, contextNodes );


                String name = getName( node );


                if( name.endsWith( "_wrapped" ) )
                {
                    sb.append( "\n " );

                    printWrapperInCycle( node, contextNodes, sb, condition, parentContextName );

                    continue;
                }


                String expression = prepareExpression( getExpression( node ), contextNodes );


                String conditionalExpression = "(" + condition + ") ? " + expression + " : \"NO_VALUE\"";


                sb.append( "\n " );

                sb.append( name );
                sb.append( " = " );

                sb.append( printContextMap( conditionalExpression, contextNodes, parentContextName ) );


                sb.append( "\n " );
                sb.append( parentContextName );
                sb.append( " = addValue(" );
                sb.append( parentContextName );
                sb.append( ", " );
                sb.append( name );
                sb.append( ")" );


                contextNodes.add( node );
            }
            else if( isCall( node ) )
            {
                printCallInCycle( (Compartment)node, contextNodes, sb, conditionals, parentContextName );
            }
            else if( isConditional( node ) )
            {
                printConditionalInCycle( (Compartment)node, contextNodes, sb, conditionals, parentContextName );
            }
        }
    }


    private void printConditionalWithNestedCycle(Compartment conditional, List<Node> parentContextNodes, StringBuilder sb,
            List<Compartment> conditionals, String parentContextName, List<Node> nodes)
    {
        String originalCondition = StreamEx.of( conditionals ).map( c -> findCondition( c ) ).joining( "&&" );


        String condition = prepareExpression( originalCondition, parentContextNodes );


        String conditionalContextName = "if_" + ( ++conditionalContextIndex ) + "_context";


        sb.append( "\n " );
        sb.append( conditionalContextName );
        sb.append( " = condition(" );
        sb.append( parentContextName );
        sb.append( ") { " );
        sb.append( getContextArguments( parentContextNodes ) );
        sb.append( " -> " );
        sb.append( condition );
        sb.append( " }" );


        List<Node> conditionalContextNodes = new ArrayList<>( parentContextNodes );


        List<String> cycleNames = new ArrayList<>();


        for( Node node : nodes )
        {
            if( WorkflowUtil.isCycle( node ) )
            {
                cycleNames.add( getCycleName( (Compartment)node ) );
            }
        }


        for( Node node : nodes )
        {
            if( isExpression( node ) )
            {
                String name = getName( node );


                /*
                 * collect node created earlier by createCollect()
                 */
                if( name.endsWith( "_collected" ) )
                {
                    printConditionalCollectedExpression( node, parentContextNodes, sb, parentContextName, conditionalContextName );

                    continue;
                }


                if( name.endsWith( "_wrapped" ) )
                {
                    sb.append( "\n " );

                    printWrapperInCycle( node, conditionalContextNodes, sb, null, conditionalContextName );

                    continue;
                }


                String expression = prepareExpression( getExpression( node ), conditionalContextNodes );


                /*
                 * Array used as nested scatter collection.
                 *
                 * Example:
                 *
                 * j_array =
                 *     calcInScatter(if_1_context) {
                 *         i_v -> inner_values
                 *     }
                 *
                 * It is NOT appended to if_1_context.
                 */
                if( cycleNames.contains( name ) )
                {
                    sb.append( "\n " );

                    sb.append( name );
                    sb.append( " = " );

                    sb.append( printContextMap( expression, conditionalContextNodes, conditionalContextName ) );

                    continue;
                }


                sb.append( "\n " );

                sb.append( name );
                sb.append( " = " );

                sb.append( printContextMap( expression, conditionalContextNodes, conditionalContextName ) );


                sb.append( "\n " );
                sb.append( conditionalContextName );
                sb.append( " = addValue(" );
                sb.append( conditionalContextName );
                sb.append( ", " );
                sb.append( name );
                sb.append( ")" );


                conditionalContextNodes.add( node );
            }
            else if( WorkflowUtil.isCycle( node ) )
            {
                List<Node> scatterContextNodes = new ArrayList<>( conditionalContextNodes );


                printConditionalNestedCycle( (Compartment)node, scatterContextNodes, sb, conditionalContextName );
            }
            else if( isCall( node ) )
            {
                printCallInCycle( (Compartment)node, conditionalContextNodes, sb, new ArrayList<>(), conditionalContextName );
            }
            else if( isConditional( node ) )
            {
                printConditionalInCycle( (Compartment)node, conditionalContextNodes, sb, conditionals, conditionalContextName );
            }
        }
    }


    private String prepareExpression(String expression, List<Node> contextNodes)
    {
        for( Node contextNode : contextNodes )
        {
            String oldName = getName( contextNode );

            String newName = getContextArgumentName( contextNode );


            if( isCall( contextNode.getCompartment() ) )
            {
                String callName = getCallName( contextNode.getCompartment() );

                expression = expression.replace( callName + "." + oldName, oldName );
            }


            expression = renameVariable( expression, oldName, newName );
        }


        return expression;
    }


    private String getContextArgumentName(Node node)
    {
        String name = getName( node );

        Compartment parent = node.getCompartment();

        if( WorkflowUtil.isCall( parent ) )
        {
            return getCallName( parent ) + "_" + name + "_v";
        }

        return name + "_v";
    }

    private String getContextArguments(List<Node> contextNodes)
    {
        return StreamEx.of( contextNodes ).map( this::getContextArgumentName ).joining( "," );
    }


    private String renameVariable(String expression, String oldName, String newName)
    {
        try
        {
            return new VariableRenamer().rename( expression, oldName, newName );
        }
        catch( Exception ex )
        {
            return "";
        }
    }


    private static List<Node> getCycledSources(Node expression)
    {
        List<Node> result = new ArrayList<>();


        for( Node source : WorkflowUtil.getSources( expression ) )
        {
            if( isArrayVariable( source ) )
            {
                result.add( source );
            }
        }


        return result;
    }


    private static boolean isArrayVariable(Node node)
    {
        if( WorkflowUtil.isCycleVariable( node ) )
        {
            return true;
        }


        Compartment parent = node.getCompartment();


        if( WorkflowUtil.isCall( parent ) && isInsideCycle( parent ) )
        {
            return true;
        }


        for( Node source : WorkflowUtil.getSources( node ) )
        {
            if( isArrayVariable( source ) )
            {
                return true;
            }
        }


        return false;
    }
}