package ru.biosoft.table.datatype;

import ru.biosoft.access.core.DataElementPath;

public class PathDataType extends DataType
{
    public PathDataType()
    {
        super( DataElementPath.class, "Path", DataElementPath.EMPTY_PATH  );
    }
    @Override
    public Object convertValue(Object value)
    {
        if( value == null || value instanceof DataElementPath )
            return value;
        return DataElementPath.create(value.toString());
    }
}