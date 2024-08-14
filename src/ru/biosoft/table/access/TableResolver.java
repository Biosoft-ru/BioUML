package ru.biosoft.table.access;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Interface for table resolver: build a table upon some data element
 */
public abstract class TableResolver
{
    /**
     * @param de
     * @return table if resolved
     * null if detected that this resolver is not suitable
     * @throws Exception if resolver is suitable, but table cannot be returned 
     */
    public abstract DataCollection<?> getTable(DataElement de) throws Exception;
    
    public int accept(DataElement de) throws Exception {
        return 0;
    }
    
    public String getRowId(DataElement de, String elementName)
    {
        return elementName;
    }
    
    public boolean isTableEditable()
    {
        return false;
    }
}