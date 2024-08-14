package ru.biosoft.access.search._test;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

public class SimpleBean extends DataElementSupport
{
    public SimpleBean( DataCollection origin, String name, int id )
    {
        super( name,origin );
        this.id = id;
    }

    private int id;
    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }
}