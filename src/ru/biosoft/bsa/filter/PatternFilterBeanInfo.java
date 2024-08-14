package ru.biosoft.bsa.filter;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class PatternFilterBeanInfo extends FilterBeanInfo
{
    public PatternFilterBeanInfo(Class c, String key)
    {
        super(c, key, "pattern");
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        addHidden(new PropertyDescriptorEx("pattern", beanClass));
    }
}


