package ru.biosoft.util;

import java.util.Map;
import java.util.TreeMap;

import com.developmentontheedge.beans.model.Property;

import one.util.streamex.StreamEx;

/**
 * Hierarchical list of bean properties
 * @author lan
 *
 */
public class FieldMap
{
    public static final FieldMap ALL = new FieldMap();
    
    private final Map<String, FieldMap> fields = new TreeMap<>();
    
    private FieldMap()
    {
    }
    
    /**
     * Read FieldMap from semicolon-separated string
     * @param fieldsStr
     */
    public FieldMap(String fieldsStr)
    {
        if( fieldsStr == null )
            return;
        for( String fieldName : StreamEx.split(fieldsStr, ';').map( String::trim ).remove( String::isEmpty ) )
        {
            StreamEx.split(fieldName, '/').foldLeft( this.fields,
                    (map, part) -> map.computeIfAbsent( part, k -> new FieldMap() ).fields );
        }
    }
    
    public FieldMap get(String name)
    {
        FieldMap fieldMap = fields.get(name);
        return fieldMap == null ? ALL : fieldMap;
    }
    
    public FieldMap get(Property property)
    {
        return get(property.getName());
    }
    
    public boolean contains(String name)
    {
        return fields.isEmpty() || fields.containsKey(name);
    }
    
    @Override
    public String toString()
    {
        return fields.toString();
    }
}