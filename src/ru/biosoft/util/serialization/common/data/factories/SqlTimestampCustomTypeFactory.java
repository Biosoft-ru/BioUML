package ru.biosoft.util.serialization.common.data.factories;


import java.util.Date;

import ru.biosoft.util.serialization.CustomTypeFactory;
import ru.biosoft.util.serialization.utils.TimeUtils;

public class SqlTimestampCustomTypeFactory implements CustomTypeFactory
{
    String time;

    @Override
    public Object getFactoryInstance( Object o )
    {
        time = TimeUtils.formatAsSQLDateTime( ( java.sql.Timestamp )o );
        return this;
    }

    @Override
    public Object getOriginObject()
    {
        long t = 0;
        try
        {
            Date d = TimeUtils.getDateTimeFromSQLString( time );
            t = d.getTime();
        }
        catch( Exception e )
        {
        }
        return new java.sql.Timestamp( t );
    }
}
