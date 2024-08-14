package biouml.plugins.research.workflow;

import com.developmentontheedge.beans.BeanInfoEx;

public class WorkflowDiagramTypeBeanInfo extends BeanInfoEx
{

    public WorkflowDiagramTypeBeanInfo()
    {
        super(WorkflowDiagramType.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName     (getResourceString("CN_WORKFLOW_DIAGRAM"));
        beanDescriptor.setShortDescription(getResourceString("CD_WORKFLOW_DIAGRAM"));
    }

}
