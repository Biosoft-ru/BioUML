package ru.biosoft.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Class which allows you to reuse equal objects
 * @author lan
 */
public class ObjectCache<T>
{
    private Map<T, T> map = new HashMap<>();
    
    /**
     * Return either the object passed or object which equals to the object passed (and was passed before)
     */
    public synchronized T get(T object)
    {
        T oldObject = map.get(object);
        if(oldObject != null) return oldObject;
        map.put(object, object);
        return object;
    }
    
    @Override
    public String toString()
    {
        return map.keySet().toString();
    }
}
