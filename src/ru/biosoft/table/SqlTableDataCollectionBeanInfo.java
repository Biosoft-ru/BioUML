package ru.biosoft.table;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SqlTableDataCollectionBeanInfo extends TableDataCollectionBeanInfo
{
    public SqlTableDataCollectionBeanInfo()
    {
        super(SqlTableDataCollection.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        
        PropertyDescriptorEx pde = new PropertyDescriptorEx("sqlTable", beanClass, "getTableId", null);
        add(pde);
    }
}
