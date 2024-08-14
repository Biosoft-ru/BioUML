package ru.biosoft.table.datatype;

import ru.biosoft.table.StringSet;

public class SetDataType extends DataType
{
    public SetDataType()
    {
        super( StringSet.class, "Set", null );
    }
    @Override
    public Object convertValue(Object value)
    {
        if( value instanceof StringSet )
            return value;
        if (value == null )
            return new StringSet();
        return new StringSet(value.toString());
    }
}