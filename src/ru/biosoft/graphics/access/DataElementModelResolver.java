package ru.biosoft.graphics.access;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.View.ModelResolver;

public class DataElementModelResolver implements ModelResolver
{
    @Override
    public String toString(Object model)
    {
        if( model instanceof DataElement )
            return DataElementPath.create( (DataElement)model ).toString();
        return null;
    }

    @Override
    public Object fromString(String name)
    {
        return DataElementPath.create( name ).optDataElement();
    }

}
