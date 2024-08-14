package ru.biosoft.access.search._test;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

public class ArrayBean extends DataElementSupport
{
    public ArrayBean( DataCollection origin, String name )
    {
        super( name,origin );
    }

    private String[] array;
    public String[] getArray()
    {
        return array;
    }
    public void setArray(String[] array)
    {
        this.array = array;
    }
}