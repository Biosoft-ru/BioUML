package ru.biosoft.bsa.access;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.table.datatype.DataType;

public class GenomeBrowserDataType extends DataType
{
    public GenomeBrowserDataType()
    {
        super( Project.class, "GenomeBrowser", null );
    }
    @Override
    public Object convertValue(Object value)
    {
        if( value == null || value instanceof Project )
            return value;
        try
        {
            return CollectionFactory.getDataElement(value.toString());
        }
        catch( Exception e )
        {
            return null;
        }
    }
}