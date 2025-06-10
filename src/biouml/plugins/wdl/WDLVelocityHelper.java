package biouml.plugins.wdl;

import java.util.List;
import java.util.Map;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;

public class WDLVelocityHelper
{
    private Diagram diagram;

    public WDLVelocityHelper(Diagram diagram)
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

    public List<Node> getTasks()
    {
        return WDLUtil.getTasks( diagram );
    }

    public List<Node> getCalls()
    {
        return WDLUtil.getCalls( diagram );
    }

    public List<Node> getInputs(Compartment c)
    {
        return WDLUtil.getInputs( c );
    }

    public List<Node> getOutputs(Compartment c)
    {
        return WDLUtil.getOutputs( c );
    }

    public String getCommand(Compartment c)
    {
        return WDLUtil.getCommand( c );
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
        return WDLUtil.getExpression( n );
    }

    public String getType(Node n)
    {
        return WDLUtil.getType( n );
    }

    public String getName(Node n)
    {
        return WDLUtil.getName( n );
    }

    public String getDeclaration(Node n)
    {
        if (n == null)
            return "??";
        if( getExpression( n ) != null && !getExpression( n ).isEmpty())
            return getType( n ) + " " + getName( n ) + " = " + getExpression( n );
        return getType( n ) + " " + getName( n );
    }

    public String getVersion()
    {
        return WDLUtil.getVersion( diagram );
    }
}
