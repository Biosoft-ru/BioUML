/**
 * 
 */
package biouml.plugins.research;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class JournalInfoWrapperBeanInfo extends BeanInfoEx
{
    public JournalInfoWrapperBeanInfo()
    {
        super(JournalInfoWrapper.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde;
        
        pde = new PropertyDescriptorEx("type", beanClass.getMethod("getType"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_TYPE"), getResourceString("PD_TYPE"));

        pde = new PropertyDescriptorEx("source", beanClass.getMethod("getSource"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_SOURCE"), getResourceString("PD_SOURCE"));

        pde = new PropertyDescriptorEx("endTime", beanClass.getMethod("getEndTimeStr"), null);
        pde.setReadOnly(true);
        add(pde, getResourceString("PN_TIME"), getResourceString("PD_TIME"));
    }
}