package ru.biosoft.access.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.biosoft.util.HashMapWeakValues;

/**
 * Cache for saving copies of objects for session
 */
public class SessionCache
{
    /**
     * Map for asked(not changed) objects.
     */
    protected Map<String, Object> readObjects = new HashMapWeakValues();
    /**
     * Map for changed objects
     */
    protected Map<String, Object> changedObjects = new ConcurrentHashMap<>();

    /**
     * Add object to cache
     */
    public void addObject(String key, Object object, boolean isChanged)
    {
        if( isChanged )
        {
            changedObjects.put(key, object);
        }
        else
        {
            readObjects.put(key, object);
        }
    }

    /**
     * Move object to changed objects map
     */
    public void setObjectChanged(String key, Object object)
    {
        readObjects.remove(key);
        changedObjects.put(key, object);
    }

    /**
     * Get object from cache. Returns null if object not found in cache.
     */
    public Object getObject(String key)
    {
        Object result = changedObjects.get(key);
        if( result == null )
        {
            result = readObjects.get(key);
        }
        return result;
    }

    /**
     * Remove object from cache.
     */
    public void removeObject(String key)
    {
        changedObjects.remove(key);
        readObjects.remove(key);
    }
}
