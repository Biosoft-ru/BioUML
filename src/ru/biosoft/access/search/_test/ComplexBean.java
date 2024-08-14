package ru.biosoft.access.search._test;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

public class ComplexBean extends DataElementSupport
{
    public ComplexBean( DataCollection origin, String name )
    {
        super( name,origin );
    }

    private int id;
    public int getId()
    {
        return id;
    }
    public void setId( int id )
    {
        this.id = id;
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

    private SimpleBean nestedBean;
    public SimpleBean getBean()
    {
        return nestedBean;
    }
    public void setBean( SimpleBean bean )
    {
        this.nestedBean = bean;
    }
}