package ru.biosoft.server.servlets.webservices;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.security.SecurityManager;

public class SystemSession
{
    private Map<String, Object> map = new HashMap<>();
    
    public String getId()
    {
        return SecurityManager.SYSTEM_SESSION;
    }
    
    public synchronized Object getValue(String key)
    {
        return map.get(key);
    }
    
    public synchronized void putValue(String key, Object value)
    {
        map.put(key, value);
    }
}