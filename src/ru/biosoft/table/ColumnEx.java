package ru.biosoft.table;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.swing.table.ColumnWithSort;

/**
 * Column with attached meta-information
 * @author lan
 */
public class ColumnEx extends ColumnWithSort
{
    private Map<String, String> values;
    
    private Map<String, String> getValuesMap()
    {
        if(values == null) values = new HashMap<>();
        return values;
    }
    
    public Set<String> getKeys()
    {
        return values == null?Collections.<String>emptySet():getValuesMap().keySet();
    }
    
    public static final String DISPLAY_TITLE = "displayTitle";
    
    public String getValue(String key)
    {
        return getValuesMap().get(key);
    }
    
    public void setValue(String key, String value)
    {
        getValuesMap().put(key, value);
    }
    
    public void removeValue(String key)
    {
        getValuesMap().remove(key);
    }

    /**
     * @param parent
     * @param columnKey
     * @param enabled
     * @param sorting
     */
    public ColumnEx(Option parent, String columnKey, boolean enabled, int sorting)
    {
        super(parent, columnKey, enabled, sorting);
    }

    /**
     * @param parent
     * @param columnKey
     * @param enabled
     */
    public ColumnEx(Option parent, String columnKey, boolean enabled)
    {
        super(parent, columnKey, enabled);
    }

    /**
     * @param parent
     * @param columnKey
     * @param sorting
     */
    public ColumnEx(Option parent, String columnKey, int sorting)
    {
        super(parent, columnKey, sorting);
    }

    /**
     * @param parent
     * @param columnKey
     * @param name
     * @param enabled
     * @param sorting
     */
    public ColumnEx(Option parent, String columnKey, String name, boolean enabled, int sorting)
    {
        super(parent, columnKey, name, enabled, sorting);
    }

    /**
     * @param parent
     * @param columnKey
     * @param name
     * @param enabled
     */
    public ColumnEx(Option parent, String columnKey, String name, boolean enabled)
    {
        super(parent, columnKey, name, enabled);
    }

    /**
     * @param parent
     * @param columnKey
     * @param name
     * @param sorting
     */
    public ColumnEx(Option parent, String columnKey, String name, int sorting)
    {
        super(parent, columnKey, name, sorting);
    }

    /**
     * @param parent
     * @param columnKey
     * @param name
     */
    public ColumnEx(Option parent, String columnKey, String name)
    {
        super(parent, columnKey, name);
    }

    /**
     * @param parent
     * @param columnKey
     */
    public ColumnEx(Option parent, String columnKey)
    {
        super(parent, columnKey);
    }
    
    public void copyMetaData(ColumnEx source)
    {
        getValuesMap().clear();
        for(String key: source.getKeys())
        {
            setValue(key, source.getValue(key));
        }
    }
}
