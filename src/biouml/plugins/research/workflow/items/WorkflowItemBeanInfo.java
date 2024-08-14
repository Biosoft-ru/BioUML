package biouml.plugins.research.workflow.items;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class WorkflowItemBeanInfo extends BeanInfoEx
{
    public WorkflowItemBeanInfo(Class type, String name)
    {
        super( type, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName( getResourceString("CN_ITEM") );
        beanDescriptor.setShortDescription( getResourceString("CD_ITEM") );
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass);
        pde.setReadOnly(beanClass.getMethod("isNameReadonly"));
        add(pde, getResourceString("PN_NAME"), getResourceString("PD_NAME"));
    }
}
