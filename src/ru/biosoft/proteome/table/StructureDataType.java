package ru.biosoft.proteome.table;

import ru.biosoft.table.datatype.DataType;

public class StructureDataType extends DataType
{
    public StructureDataType()
    {
        super( Structure3D.class, "Structure", null );
    }
    @Override
    public Object convertValue(Object value)
    {
        if( value instanceof Structure3D )
            return value;
        if(value == null)
            return null;
        return new Structure3D(value.toString());
    }
}