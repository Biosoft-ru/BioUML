package biouml.plugins.wdl;

import java.util.List;
import java.util.Map;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;

public class NextFlowVelocityHelper
{
    private Diagram diagram;

    public NextFlowVelocityHelper(Diagram diagram)
    {
        this.diagram = diagram;
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
        return node.edges().map(e->e.getOtherEnd( node )).findAny( n->WDLUtil.isCall( n )).orElse( null );
    }

    public List<Compartment> getTasks()
    {
        return WDLUtil.getTasks( diagram );
    }

    public List<Compartment> getCalls(Compartment compartment)
    {
        return WDLUtil.getCalls( compartment );
    }
    
//    public int getCallIndex(String name)
//    {
//        List<Node> calls = getCalls();
//        for( int i = 0; i < calls.size(); i++ )
//        {
//            if( calls.get( i ).getName().equals( name ) )
//                return i;
//        }
//        return -1;
//    }

    public List<Node> getInputs(Compartment c)
    {
        return WDLUtil.getInputs( c );
    }

    /**
     * returns node which is connected with this input node of a call
     */
    public Node getSource(Node node)
    {
        return node.edges().map( e->e.getOtherEnd( node ) ).findAny().orElse(null);
    }
    
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
        String expression =  WDLUtil.getExpression( n );
        if (expression == null)
            return null;
        return expression.replace( "~{", "${" );
    }

    public String getType(Node n)
    {
        return getNextFlowType(WDLUtil.getType( n ));
    }

    public String getName(Node n)
    {
        String name = WDLUtil.getName( n );
        if( WDLUtil.isExternalParameter( n ) )
            return "params." + name;
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
    
    private static String getNextFlowType(String wdlType)
    {
        switch (wdlType)
        {
            case "File":
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
        result.append( getName(n) );
                
        if( getExpression( n ) != null && !getExpression( n ).isEmpty() )
            result.append( " = " + getExpression( n ));
        
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
        if (cycleVarNode == null)
            return null;
        Node source = getSource(cycleVarNode);
        if (source == null)
            return null;
        return getName(source);
    }
    
    public String getCycleVariable(Compartment c)
    {
        return WDLUtil.getCycleVariable( c );
    }
    
    public String createChannelName(String input)
    {
        return input.replace( ".", "_" )+"_ch";
    }
}
