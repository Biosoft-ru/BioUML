package ru.biosoft.bsa.filter;

import ru.biosoft.access.core.filter.MutableFilter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.GridLayout2;

public class FilterBeanInfo extends BeanInfoEx
{
    public FilterBeanInfo(Class<? extends MutableFilter<?>> c, String key, String property)
    {
        super(c, MessageBundle.class.getName());
        beanDescriptor.setDisplayName     (getResourceString("PN_" + key));
        beanDescriptor.setShortDescription(getResourceString("PD_" + key));
        setCompositeEditor("enabled;" + property, new GridLayout2());
    }

    @Override
    public void initProperties() throws Exception
    {
        addHidden(new PropertyDescriptorEx("enabled", beanClass));
    }
}


