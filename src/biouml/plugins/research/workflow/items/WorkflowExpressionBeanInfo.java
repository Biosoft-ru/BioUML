package biouml.plugins.research.workflow.items;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class WorkflowExpressionBeanInfo extends WorkflowVariableBeanInfo
{
    public WorkflowExpressionBeanInfo()
    {
        this(WorkflowExpression.class, "EXPRESSION");
    }

    public WorkflowExpressionBeanInfo(Class type, String name)
    {
        super(type, name);
        beanDescriptor.setDisplayName( getResourceString("CN_EXPRESSION") );
        beanDescriptor.setShortDescription( getResourceString("CD_EXPRESSION") );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        PropertyDescriptorEx pde = new PropertyDescriptorEx("expression", beanClass);
        pde.setPropertyEditorClass(ExpressionEditor.class);
        add(pde, getResourceString("PN_EXPRESSION"), getResourceString("PD_EXPRESSION"));
    }
}
