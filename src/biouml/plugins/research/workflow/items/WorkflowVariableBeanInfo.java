package biouml.plugins.research.workflow.items;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class WorkflowVariableBeanInfo extends WorkflowItemBeanInfo
{
    public WorkflowVariableBeanInfo(Class type, String name)
    {
        super( type, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName( getResourceString("CN_VARIABLE") );
        beanDescriptor.setShortDescription( getResourceString("CD_VARIABLE") );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        PropertyDescriptorEx pde = new PropertyDescriptorEx("type", beanClass);
        add(pde, getResourceString("PN_TYPE"), getResourceString("PD_TYPE"));
        pde = new PropertyDescriptorEx("value", beanClass, "getValueString", null);
        add(pde, getResourceString("PN_VALUE"), getResourceString("PD_VALUE"));
    }
}
