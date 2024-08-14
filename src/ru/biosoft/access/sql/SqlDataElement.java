package ru.biosoft.access.sql;

import ru.biosoft.access.core.DataElement;

/**
 * Common interface for all items based on Sql table
 */
public interface SqlDataElement extends DataElement, SqlConnectionHolder
{
    public static final String RELATED_TABLE_PROPERTY = "relatedTable";

    public String getTableId();
    
    public String[] getUsedTables();
}
