package ru.biosoft.util.serialization.common.data.factories;

import ru.biosoft.util.serialization.CustomTypeFactory;

public class SqlDateCustomTypeFactory implements CustomTypeFactory
{
    long time;

    @Override
    public Object getFactoryInstance( Object o )
    {
        time = ( ( java.sql.Date )o ).getTime();
        return this;
    }

    @Override
    public Object getOriginObject()
    {
        return new java.sql.Date( time );
    }
}
