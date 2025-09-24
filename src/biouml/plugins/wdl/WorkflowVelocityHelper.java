package biouml.plugins.wdl;

import java.util.List;
import java.util.Map;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WDLUtil.ImportProperties;
import biouml.plugins.wdl.diagram.WDLConstants;

public class WorkflowVelocityHelper
{
    protected Diagram diagram;
    protected List<Node> orderedCalls;
    
    public WorkflowVelocityHelper(Diagram diagram)
    {
        this.diagram = diagram;
        orderedCalls = WDLUtil.orderCallsScatters( diagram );
    }

    /**
     * @return diagram name
     */
    public String getName()
    {
        return diagram.getName();
    }
    
    /**
     * @return name of the workflow element associated with this node (not necessarily the same as the name of the node!)
     */
    public String getName(Node n)
    {
        return WDLUtil.getName( n );
    }
    
    /**
     * @return  list of all compartments corresponding to workflow tasks
     */
    public List<Compartment> getTasks()
    {
        return WDLUtil.getTasks( diagram );
    }

    /**
     * @return  list of all compartments corresponding to workflow cycles
     */
    public List<Compartment> getScatters(Compartment c)
    {
        return WDLUtil.getCycles( c );
    }

    /**
     * @return  list of all compartments corresponding to workflow calls
     */
    public List<Compartment> getCalls(Compartment c)
    {
        return WDLUtil.getCalls( c );
    }

    /**
     * @return formula for given node if there is any
     */
    public String getExpression(Node n)
    {
        return WDLUtil.getExpression( n );
    }

    /**
     * @return source node from which its formula depends
     */
    public static Node getSource(Node node)
    {
        return WDLUtil.getSource( node );
    }
    
    /**
     * @return returns data type of workflow element associated with node (e.g. File, String,... )
     */
    public String getType(Node n)
    {
        return WDLUtil.getType( n );
    }

    /**
     * @return main workflow name in the diagram
     */
    public String getMainWorkflowName()
    {
        return WDLConstants.MAIN_WORKFLOW;
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
    
    public String getCommand(Compartment c)
    {
        return WDLUtil.getCommand( c );
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

    public List<Node> getOrderedInputs(Compartment c)
    {
        return WDLUtil.getOrderedInputs( c );
    }

    public List<Node> getOutputs(Compartment c)
    {
        return WDLUtil.getOutputs( c );
    }
    
    public List<Node> getExternalParameters()
    {
        return WDLUtil.getExternalParameters( diagram );
    }

    public List<Node> getExternalOutputs()
    {
        return WDLUtil.getExternalOutputs( diagram );
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

    public boolean isCall(Node node)
    {
        return WDLUtil.isCall( node );
    }

    public boolean isCycle(Node node)
    {
        return WDLUtil.isCycle( node );
    }
    
    public boolean isInsideCycle(Compartment call)
    {
        return ! ( call instanceof Diagram ) && WDLUtil.isCycle( call.getCompartment() );
    }

    public static boolean isExpression(Node node)
    {
        return WDLUtil.isExpression( node ) && !WDLUtil.isExternalOutput( node );
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

    public ImportProperties[] getImports()
    {
        return WDLUtil.getImports( diagram );
    }
}
