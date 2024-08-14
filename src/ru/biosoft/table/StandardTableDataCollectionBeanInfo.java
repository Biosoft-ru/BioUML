package ru.biosoft.table;

public class StandardTableDataCollectionBeanInfo extends TableDataCollectionBeanInfo
{
    public StandardTableDataCollectionBeanInfo()
    {
        this(StandardTableDataCollection.class);
    }
    
    protected StandardTableDataCollectionBeanInfo(Class<? extends TableDataCollection> c)
    {
        super(c);
    }
}
