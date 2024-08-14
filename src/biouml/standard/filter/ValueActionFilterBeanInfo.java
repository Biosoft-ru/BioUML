package biouml.standard.filter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.IndexedPropertyDescriptorEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ValueActionFilterBeanInfo extends BeanInfoEx
{
    protected ValueActionFilterBeanInfo(Class<?> c)
    {
        super(c, MessageBundle.class.getName());
    }

    protected void initProperties(String key) throws Exception
    {
        addHidden(new PropertyDescriptorEx("enabled", beanClass));

        IndexedPropertyDescriptorEx pde = new IndexedPropertyDescriptorEx("valueAction", beanClass);
        pde.setChildDisplayName(beanClass.getMethod("getItemDisplayName", new Class[] {Integer.class, Object.class }));
        pde.setCompositeEditor("../enabled", new java.awt.BorderLayout());
        add(pde,
            getResourceString("PN_" + key),
            getResourceString("PD_" + key));
    }
}

