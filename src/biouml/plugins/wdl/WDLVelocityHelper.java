package biouml.plugins.wdl;

import java.util.List;
import java.util.Map;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WDLUtil.ImportProperties;

public class WDLVelocityHelper extends WorkflowVelocityHelper
{
    public WDLVelocityHelper(Diagram diagram)
    {
        super(diagram);
    }

    public List<Node> getExternalParameters()
    {
        return WDLUtil.getExternalParameters( diagram );
    }

    public List<Node> getExternalOutputs()
    {
        return WDLUtil.getExternalOutputs( diagram );
    }

    public List<Compartment> getTasks()
    {
        return WDLUtil.getTasks( diagram );
    }

    public List<Compartment> getScatters(Compartment c)
    {
        return WDLUtil.getCycles( c );
    }

    public List<Compartment> getCalls(Compartment c)
    {
        return WDLUtil.getCalls( c );
    }

    public List<Node> getOrderedInputs(Compartment c)
    {
        return WDLUtil.getOrderedInputs( c );
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
    
    public Map<String, String> getMeta(Compartment c)
    {
        return WDLUtil.getMeta( c );
    }
    
    public Map<String, String> getParametersMeta(Compartment c)
    {
        return WDLUtil.getParameterMeta( c );
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

    public String getVersion()
    {
        return WDLUtil.getVersion( diagram );
    }

    public String getTaskRef(Compartment c)
    {
        return WDLUtil.getTaskRef( c );
    }

    public String getImportedDiagram(Compartment call)
    {
        return WDLUtil.getDiagramRef( call );
    }

    public String getCycleVariable(Compartment c)
    {
        return WDLUtil.getCycleVariable( c );
    }

    public String getCycleName(Compartment c)
    {
        return WDLUtil.getCycleName( c );
    }

    public String getCallInput(Node inputNode)
    {
        String name = getName( inputNode );
        String expression = getExpression( inputNode );
        if( expression == null )
            return name;
        return name + " = " + expression;
    }

    public ImportProperties[] getImports()
    {
        return WDLUtil.getImports( diagram );
    }

    public boolean isCall(Node node)
    {
        return WDLUtil.isCall( node );
    }

    public boolean isCycle(Node node)
    {
        return WDLUtil.isCycle( node );
    }

    public static boolean isExpression(Node node)
    {
        return WDLUtil.isExpression( node ) && !WDLUtil.isExternalOutput(node);
    }

    public List<Node> orderCalls(Compartment compartment)
    {
        return WDLUtil.orderCallsScatters( compartment );
    }

    public String getAlias(Compartment call)
    {
        return WDLUtil.getAlias( call );
    }

    public String getExternalDiagramAlias(Compartment call)
    {
        return WDLUtil.getExternalDiagramAlias( call );
    }

    public Object getBeforeCommand(Compartment task)
    {
        return WDLUtil.getBeforeCommand( task );
    }
}