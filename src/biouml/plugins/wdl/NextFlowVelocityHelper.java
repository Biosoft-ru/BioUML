package biouml.plugins.wdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;

public class NextFlowVelocityHelper
{
    private Diagram diagram;
    private List<Compartment> orderedCalls;

    public NextFlowVelocityHelper(Diagram diagram)
    {
        this.diagram = diagram;
        orderedCalls = WDLUtil.orderCallsScatters( diagram );
    }

    public List<Compartment> orderCalls(Compartment compartment)
    {
        return WDLUtil.orderCallsScatters( compartment );
    }

    public String getName()
    {
        return diagram.getName();
    }

    public List<Node> getExternalParameters()
    {
        return WDLUtil.getExternalParameters( diagram );
    }

    public List<Node> getExternalOutputs()
    {
        return WDLUtil.getExternalOutputs( diagram );
    }

    public Node getCallByOutput(Node node)
    {
        return node.edges().map( e -> e.getOtherEnd( node ) ).findAny( n -> WDLUtil.isCall( n ) ).orElse( null );
    }

    public List<Compartment> getTasks()
    {
        return WDLUtil.getTasks( diagram );
    }



    public List<Node> getInputs(Compartment c)
    {
        return WDLUtil.getInputs( c );
    }

    /**
     * returns node which is connected with this input node of a call
     */
    //    public Node getSource(Node node)
    //    {
    //        return node.edges().filter(e->e.getOutput().equals( node )).map( e -> e.getInput() ).findAny().orElse( null );
    //    }

    public List<Node> getOutputs(Compartment c)
    {
        return WDLUtil.getOutputs( c );
    }

    public String getCommand(Compartment c)
    {
        String command = WDLUtil.getCommand( c );
        return command.replace( "~{", "${" );
    }

    public Map<String, String> getRequirements(Compartment c)
    {
        return WDLUtil.getRequirements( c );
    }

    public Map<String, String> getHints(Compartment c)
    {
        return WDLUtil.getHints( c );
    }

    public Map<String, String> getRuntime(Compartment c)
    {
        return WDLUtil.getRuntime( c );
    }

    public String getExpression(Node n)
    {
        String expression = WDLUtil.getExpression( n );
        if( expression == null )
            return null;
        expression = expression.replace( "~{", "${" );
        return expression;
    }

    public String getType(Node n)
    {
        return getNextFlowType( WDLUtil.getType( n ) );
    }

    public String getName(Node n)
    {
        String name = WDLUtil.getName( n );
        return name;
    }

    public String getDeclaration(Node n)
    {
        if( n == null )
            return "??";
        if( getExpression( n ) != null && !getExpression( n ).isEmpty() )
            return getType( n ) + " " + getName( n ) + " = " + getExpression( n );
        return getType( n ) + " " + getName( n );
    }

    public String getShortDeclaration(Node n)
    {
        if( n == null )
            return "??";
        return getType( n ) + " " + getName( n );
    }

    private static String getNextFlowType(String wdlType)
    {
        switch( wdlType )
        {
            case "File":
            case "Array[File]":
                return "path";
            default:
                return "val";
        }
    }

    public String getExternalInput(Node n)
    {
        if( n == null )
            return "??";

        StringBuilder result = new StringBuilder();

        result.append( "params." );
        result.append( getName( n ) );
        //        String expression = getExpression( n );
        //        if( expression != null && !expression.isEmpty() )
        //        {
        //            String type = getType( n );
        //            if( type.equals( "path" ) )
        //                expression = "file(" + expression + ")";
        //            result.append( " = " + expression );
        //        }
        return result.toString();
    }

    public String getVersion()
    {
        return WDLUtil.getVersion( diagram );
    }

    public String getTaskRef(Compartment c)
    {
        return WDLUtil.getTaskRef( c );
    }

    public List<Compartment> getScatters(Compartment c)
    {
        return WDLUtil.getCycles( c );
    }

    public String getCycleName(Compartment c)
    {
        Node cycleVarNode = WDLUtil.getCycleVariableNode( c );
        if( cycleVarNode == null )
            return null;
        Node source = WDLUtil.getSource( cycleVarNode );
        if( source == null )
            return null;
        return getName( source );
    }

    public String getCycleVariable(Compartment c)
    {
        return WDLUtil.getCycleVariable( c );
    }

    public String createChannelName(String input)
    {
        return input.replace( ".", "_" ) + "_ch";
    }

    public List<Compartment> getCallsScatters(Compartment compartment)
    {
        List<Compartment> result = new ArrayList<>();
        for( Compartment c : orderedCalls )
        {
            if( c.getParent().equals( compartment ) )
                result.add( c );
        }
        return result;
    }

    public String getInputName(Node n)
    {
        Node source = WDLUtil.getSource( n );
        if( source != null )
            n = source;
        String name = WDLUtil.getName( n );
        String type = WDLUtil.getType( n );
        if( WDLUtil.isExternalParameter( n ) )
        {
            String result = "params." + name;
            if( "File".equals( type ) )
                result = "channel.fromPath(" + result + ")";
            return result;
        }
        if( WDLUtil.isCall( n.getCompartment() ) )
            return getResultName( n.getCompartment() ) + "." + name;
        return name;
    }

    public String getResultName(Compartment c)
    {
        return "result_" + WDLUtil.getCallName( c );
    }

    public String createChannelFromArrray(Compartment cycle)
    {
        String cycleVar = getCycleVariable( cycle );
        Node arrayNode = WDLUtil.getCycleNode( cycle );
        String cycleName = getCycleName( cycle );
        String type = WDLUtil.getType( arrayNode );
        //        if( type.equals( "Array[File]" ) || type.equals( "Directory" ) )
        //        {
        //            return "Channel.fromPath(" + cycleName + ").set{" + cycleVar + " }";
        //        }
        return "Channel.from(" + cycleName + ").set{" + cycleVar + " }";
    }

    public String getContainer(Compartment process)
    {
        DynamicProperty dp = process.getAttributes().getProperty( WDLConstants.RUNTIME_ATTR );
        if( dp == null || ! ( dp.getValue() instanceof String[] ) )
            return null;
        String[] options = (String[])dp.getValue();
        for( String option : options )
        {
            String[] parts = option.split( "#" );
            if( parts[0].equals( "docker" ) )
                return parts[1];
        }
        return null;
    }

    public boolean shouldCollect(Compartment producer, Compartment consumer)
    {
        return true;
    }

    public boolean isCall(Node node)
    {
        return WDLUtil.isCall( node );
    }

    public boolean isCycle(Node node)
    {
        return WDLUtil.isCycleVariable( node );
    }

    public String getFunctions()
    {
        return "basename";
    }
}