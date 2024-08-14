package ru.biosoft.table;

public class FileTableDataCollectionBeanInfo extends StandardTableDataCollectionBeanInfo
{
    public FileTableDataCollectionBeanInfo()
    {
        this(FileTableDataCollection.class);
    }

    protected FileTableDataCollectionBeanInfo(Class<? extends TableDataCollection> c)
    {
        super(c);
    }
}
