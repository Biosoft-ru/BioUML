package ru.biosoft.bsa.filter;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SiteIDFilterBeanInfo extends FilterBeanInfo
{
    public SiteIDFilterBeanInfo()
    {
        super(SiteIDFilter.class, "SITEID_FILTER", "pattern");
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        addHidden(new PropertyDescriptorEx("pattern", beanClass));
    }
}


