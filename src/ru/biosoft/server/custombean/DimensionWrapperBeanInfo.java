package ru.biosoft.server.custombean;

import com.developmentontheedge.beans.BeanInfoEx;

public class DimensionWrapperBeanInfo extends BeanInfoEx
{
    public DimensionWrapperBeanInfo()
    {
        super( DimensionWrapper.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("width");
        add("height");
    }
}
