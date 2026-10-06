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

        sb.append( "\n context = createScatter(" );
        sb.append( getCycleName( cycle ) );
        sb.append( ")" );
        printCycleContent( cycle, contextNodes, sb );
        return sb.toString();
    }

    private void printCycleContent(Compartment cycle, List<Node> contextNodes, StringBuilder sb)
    {
        List<Node> nodes = WorkflowUtil.orderCallsScatters( cycle );

        for( Node node : nodes )
        {
            if( isExpression( node ) )
            {
                printExpressionInCycle( node, contextNodes, sb );
            }
            else if( isCall( node ) )
            {
                printCallInCycle( (Compartment)node, contextNodes, sb );
            }
            else if( isConditional( node ) )
            {
                printConditionalInCycle( (Compartment)node, contextNodes, sb );
            }
            else if( WorkflowUtil.isCycle( node ) )
            {
                printNestedCycle( (Compartment)node, contextNodes, sb );
            }
        }
    }

    private void printNestedCycle(Compartment cycle, List<Node> contextNodes, StringBuilder sb)
    {
        Node cycleVariable = WorkflowUtil.getCycleVariableNode( cycle );

        sb.append( "\n context = addScatter(context, " );
        sb.append( getCycleName( cycle ) );
        sb.append( ")" );

        contextNodes.add( cycleVariable );

        printCycleContent( cycle, contextNodes, sb );
    }

    public void printExpressionInCycle(Node node, List<Node> contextNodes, StringBuilder sb)
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
            printWrapperInCycle( node, contextNodes, sb, null );
            return;
        }

        String expression = prepareExpression( getCallEmit( node ), contextNodes );

        sb.append( name );
        sb.append( " = " );
        sb.append( printContextMap( expression, contextNodes ) );

        sb.append( "\n context = addValue(context, " );
        sb.append( name );
        sb.append( ")" );

        contextNodes.add( node );
    }

    private void printWrapperInCycle(Node node, List<Node> contextNodes, StringBuilder sb, String condition)
    {
        String name = getName( node );
        String expression = WorkflowUtil.getExpression( node );
        Node source = WorkflowUtil.getSources( node ).toList().get( 0 );
        String sourceValueName = getContextArgumentName( source );
        expression = renameVariable( expression, "x1", sourceValueName );

        if( condition != null )
            expression = sourceValueName + " != \"NO_VALUE\" ? " + expression + " : \"NO_VALUE\"";

        sb.append( name );
        sb.append( " = " );
        sb.append( printContextMap( expression, contextNodes ) );

        sb.append( "\n context = addValue(context, " );
        sb.append( name );
        sb.append( ")" );

        contextNodes.add( node );
    }

    public void printCallInCycle(Compartment call, List<Node> contextNodes, StringBuilder sb)
    {
        printCallInCycle( call, contextNodes, sb, new ArrayList<>() );
    }

    private void printCallInCycle(Compartment call, List<Node> contextNodes, StringBuilder sb, List<Compartment> conditionals)
    {
        List<Node> inputs = WorkflowUtil.getOrderedInputs( call );
        List<String> inputNames = new ArrayList<>();
        List<String> inputDeclarations = new ArrayList<>();

        sb.append( "\n" );

        String contextName = "context";
        String condition = null;

        if( !conditionals.isEmpty() )
        {
            String originalCondition = StreamEx.of( conditionals ).map( c -> findCondition( c ) ).joining( "&&" );

            condition = prepareExpression( originalCondition, contextNodes );
            contextName = getCallName( call ) + "_context";

            sb.append( " " );
            sb.append( contextName );
            sb.append( " = condition(context) { " );
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

        addCallOutputsToContext( call, contextNodes, sb, condition == null ? null : contextName );
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

    private void addCallOutputsToContext(Compartment call, List<Node> contextNodes, StringBuilder sb, String conditionalContext)
    {
        List<Node> outputs = WorkflowUtil.getOutputs( call );

        if( outputs.isEmpty() )
            return;

        if( outputs.size() == 1 )
        {
            String outputChannel = getCallOutputChannel( call, outputs.get( 0 ) );

            if( conditionalContext == null )
                sb.append( "\n context = addValue(context, " + outputChannel + ")" );
            else
                sb.append( "\n context = addConditionalValue(context, " + outputChannel + ", " + conditionalContext + ")" );
        }
        else
        {
            String outputChannels = StreamEx.of( outputs ).map( output -> getCallOutputChannel( call, output ) ).joining( ", " );

            if( conditionalContext == null )
                sb.append( "\n context = addValues(context, [" + outputChannels + "])" );
            else
                sb.append( "\n context = addConditionalValues(context, [" + outputChannels + "], " + conditionalContext + ")" );
        }

        contextNodes.addAll( outputs );

        if( conditionalContext != null )
        {
            for( Node output : outputs )
            {
                String outputName = getCallName( call ) + "_" + getName( output );

                sb.append( "\n " );
                sb.append( outputName );
                sb.append( " = " );
                sb.append( printContextMap( getContextArgumentName( output ), contextNodes ) );
            }
        }
    }

    private String getCallOutputChannel(Compartment call, Node output)
    {
        String resultName = WorkflowUtil.getResultName( call );

        if( resultName != null )
            return resultName + "." + getName( output );

        return getCallName( call ) + ".out." + getName( output );
    }

    public void printConditionalInCycle(Compartment conditional, List<Node> contextNodes, StringBuilder sb)
    {
        printConditionalInCycle( conditional, contextNodes, sb, new ArrayList<>() );
    }

    private void printConditionalInCycle(Compartment conditional, List<Node> contextNodes, StringBuilder sb,
            List<Compartment> parentConditionals)
    {
        List<Compartment> conditionals = new ArrayList<>( parentConditionals );

        conditionals.add( conditional );

        for( Node node : WorkflowUtil.orderCallsScatters( conditional ) )
        {
            if( isExpression( node ) )
            {
                String condition = StreamEx.of( conditionals ).map( c -> findCondition( c ) ).joining( "&&" );

                condition = prepareExpression( condition, contextNodes );

                String name = getName( node );

                if( name.endsWith( "_wrapped" ) )
                {
                    sb.append( "\n " );
                    printWrapperInCycle( node, contextNodes, sb, condition );
                    continue;
                }

                String expression = prepareExpression( getExpression( node ), contextNodes );

                String conditionalExpression = "(" + condition + ") ? " + expression + " : \"NO_VALUE\"";

                sb.append( "\n " );
                sb.append( name );
                sb.append( " = " );
                sb.append( printContextMap( conditionalExpression, contextNodes ) );

                sb.append( "\n context = addValue(context, " );
                sb.append( name );
                sb.append( ")" );

                contextNodes.add( node );
            }
            else if( isCall( node ) )
            {
                printCallInCycle( (Compartment)node, contextNodes, sb, conditionals );
            }
            else if( isConditional( node ) )
            {
                printConditionalInCycle( (Compartment)node, contextNodes, sb, conditionals );
            }
            else if( WorkflowUtil.isCycle( node ) )
            {
                printNestedCycle( (Compartment)node, contextNodes, sb );
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

                expression = expression.replace( callName + "." + oldName, newName );
            }
            else
            {
                expression = renameVariable( expression, oldName, newName );
            }
        }

        return expression;
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
            return expression.replace( oldName, newName ); //TODO: make parser more robust
        }
    }

    private static List<Node> getCycledSources(Node expression)
    {
        List<Node> result = new ArrayList<>();

        for( Node source : WorkflowUtil.getSources( expression ) )
        {
            if( isArrayVariable( source ) )
                result.add( source );
        }

        return result;
    }

    private static boolean isArrayVariable(Node node)
    {
        if( WorkflowUtil.isCycleVariable( node ) )
            return true;

        Compartment parent = node.getCompartment();

        if( WorkflowUtil.isCall( parent ) && isInsideCycle( parent ) )
            return true;

        for( Node source : WorkflowUtil.getSources( node ) )
        {
            if( isArrayVariable( source ) )
                return true;
        }

        return false;
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
}