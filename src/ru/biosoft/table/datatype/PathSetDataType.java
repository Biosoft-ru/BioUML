package ru.biosoft.table.datatype;

import ru.biosoft.access.core.DataElementPathSet;

public class PathSetDataType extends DataType
{
    public PathSetDataType()
    {
        super( DataElementPathSet.class, "PathSet", null );
    }
    @Override
    public Object convertValue(Object value)
    {
        if( value == null || value instanceof DataElementPathSet )
            return value;
        return new DataElementPathSet(value.toString());
    }
}