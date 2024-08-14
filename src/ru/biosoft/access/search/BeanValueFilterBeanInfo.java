package ru.biosoft.access.search;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.IndexedPropertyDescriptorEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/** @todo Implement */
public class BeanValueFilterBeanInfo extends BeanInfoEx
{
    public BeanValueFilterBeanInfo()
    {
        super( BeanValueFilter.class, MessageBundle.class.getName() );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx("enabled",beanClass),
             getResourceString("PN_BEAN_VALUE_FILTER_FILTER_ENABLED"),
             getResourceString("PD_BEAN_VALUE_FILTER_FILTER_ENABLED") );
        IndexedPropertyDescriptorEx pde = new IndexedPropertyDescriptorEx("filter", beanClass);
        pde.setChildDisplayName(beanClass.getMethod("getItemDisplayName", new Class[] {Integer.class, Object.class }));
        add(pde,
            getResourceString("PN_BEAN_VALUE_FILTER_FILTER"),
            getResourceString("PD_BEAN_VALUE_FILTER_FILTER"));
        setSubstituteByChild( true );
    }
}
