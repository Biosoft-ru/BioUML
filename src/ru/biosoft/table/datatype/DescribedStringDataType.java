package ru.biosoft.table.datatype;

import ru.biosoft.table.DescribedString;

public class DescribedStringDataType extends DataType
{
    public DescribedStringDataType()
    {
        super( DescribedString.class, "DescribedString", new DescribedString( "", "" ) );
    }
    @Override
    public Object convertValue(Object value)
    {
        if( value == null || value instanceof DescribedString )
            return value;
        return new DescribedString(value.toString());
    }
}