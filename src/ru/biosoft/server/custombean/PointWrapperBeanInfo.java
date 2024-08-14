package ru.biosoft.server.custombean;

import com.developmentontheedge.beans.BeanInfoEx;

public class PointWrapperBeanInfo extends BeanInfoEx
{
    public PointWrapperBeanInfo()
    {
        super( PointWrapper.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("x");
        add("y");
    }
}
