package biouml.plugins.wdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;

public class WorkflowVelocityHelper
{
    protected Diagram diagram;
    protected List<Node> orderedCalls;
    
    public WorkflowVelocityHelper(Diagram diagram)
    {
        this.diagram = diagram;
        orderedCalls = WorkflowUtil.orderCallsScatters( diagram );
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
        return WorkflowUtil.getName( n );
    }
    
    /**
     * @return  list of all nodes corresponding to workflow structs
     */
    public List<Node> getStructs()
    {
        return WorkflowUtil.getStructs( diagram );
    }
    
    /**
     * @return  list of all compartments corresponding to workflow tasks
     */
    public List<Compartment> getTasks()
    {
        return WorkflowUtil.getTasks( diagram );
    }

    /**
     * @return  list of all compartments corresponding to workflow cycles
     */
    public List<Compartment> getScatters(Compartment c)
    {
        return WorkflowUtil.getCycles( c );
    }

    /**
     * @return  list of all compartments corresponding to workflow calls
     */
    public List<Compartment> getCalls(Compartment c)
    {
        return WorkflowUtil.getCalls( c );
    }

    /**
     * @return formula for given node if there is any
     */
    public String getExpression(Node n)
    {
        return WorkflowUtil.getExpression( n );
    }

    /**
     * @return source node from which its formula depend
     */
    public static List<Node> getSources(Node node)
    {
        return WorkflowUtil.getSources( node ).toList();
    }
    
    /**
     * @return returns data type of workflow element associated with node (e.g. File, String,... )
     */
    public String getType(Node n)
    {
        return WorkflowUtil.getType( n );
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
        return WorkflowUtil.getRequirements( c );
    }

    public Map<String, String> getHints(Compartment c)
    {
        return WorkflowUtil.getHints( c );
    }

    public Map<String, String> getRuntime(Compartment c)
    {
        return WorkflowUtil.getRuntime( c );
    }
    
    public Map<String, String> getMeta(Compartment c)
    {
        return WorkflowUtil.getMeta( c );
    }
    
    public Map<String, String> getParametersMeta(Compartment c)
    {
        return WorkflowUtil.getParameterMeta( c );
    }
    
    public String getCommand(Compartment c)
    {
        return WorkflowUtil.getCommand( c );
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
        return WorkflowUtil.getOrderedInputs( c );
    }

    public List<Node> getOutputs(Compartment c)
    {
        return WorkflowUtil.getOutputs( c );
    }
    
    public List<Node> getExternalParameters()
    {
        return WorkflowUtil.getExternalParameters( diagram );
    }

    public List<Node> getExternalOutputs()
    {
        return WorkflowUtil.getExternalOutputs( diagram );
    }
    
    public String getTaskRef(Compartment c)
    {
        return WorkflowUtil.getTaskRef( c );
    }
    
    public String getCallName(Compartment call)
    {
        return WorkflowUtil.getCallName( call );
    }
    
    public String findCondition(Compartment conditional)
    {
        return WorkflowUtil.findCondition( conditional );
    }
    
    public String getImportedDiagram(Compartment call)
    {
        return WorkflowUtil.getDiagramRef( call );
    }

    public String getCycleVariable(Compartment c)
    {
        return WorkflowUtil.getCycleVariable( c );
    }

    public String getCycleName(Compartment c)
    {
        return WorkflowUtil.getCycleName( c );
    }

    public boolean isCall(Node node)
    {
        return WorkflowUtil.isCall( node );
    }
    
    public boolean isConditional(Node node)
    {
        return WorkflowUtil.isConditional( node );
    }

    public boolean isCycle(Node node)
    {
        return WorkflowUtil.isCycle( node );
    }
    
    public boolean isInsideCycle(Node call)
    {
        return ! ( call instanceof Diagram ) && WorkflowUtil.isCycle( call.getCompartment() );
    }
    
    public List<Compartment> getCycles(Node node)
    {
        List<Compartment> result = new ArrayList<>();
        Compartment c = node.getCompartment();
        while( ! ( c instanceof Diagram ) )
        {
            if( isCycle( c ) )
                result.add( c );
            c = c.getCompartment();
        }
        return result;
    }
    
    public Compartment getClosestCycle(Node node)
    {
        Compartment c = node.getCompartment();
        while( ! ( c instanceof Diagram ) )
        {
            if( isCycle( c ) )
                return c;
            c = c.getCompartment();
        }
        return null;
    }

    public static boolean isExpression(Node node)
    {
        return WorkflowUtil.isExpression( node ) && !WorkflowUtil.isExternalOutput( node );
    }

    public List<Node> orderCalls(Compartment compartment)
    {
        return WorkflowUtil.orderCallsScatters( compartment );
    }

    public String getAlias(Compartment call)
    {
        return WorkflowUtil.getAlias( call );
    }

    public String getExternalDiagramAlias(Compartment call)
    {
        return WorkflowUtil.getExternalDiagramAlias( call );
    }

    public Object getBeforeCommand(Compartment task)
    {
        return WorkflowUtil.getBeforeCommand( task );
    }

    public ImportProperties[] getImports()
    {
        return WorkflowUtil.getImports( diagram );
    }  

    public Declaration[] getStructMembers(Node node)
    {
        return WorkflowUtil.getStructMembers( node );
    }
}
