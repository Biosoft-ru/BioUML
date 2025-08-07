package biouml.plugins.wdl;

import java.util.List;

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
        orderedCalls = WDLUtil.orderCallsScatters( diagram );
    }

    public List<Node> orderCalls(Compartment compartment)
    {
        return WDLUtil.orderCallsScatters( compartment );
    }

    public String getName()
    {
        return diagram.getName();
    }
    
    public String getMainWorkflowName()
    {
        return WDLConstants.MAIN_WORKFLOW;
    }
}
