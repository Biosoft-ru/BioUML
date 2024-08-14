package ru.biosoft.access.security;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for session caches
 */
public class SessionCacheManager
{
    protected static final Logger log = Logger.getLogger(SessionCacheManager.class.getName());

    private static final Map<String, SessionCache> sessionCacheMap = new ConcurrentHashMap<>();
    
    static
    {
        addSessionCache("system");
    }
    
    /**
     * Returns SessionCache for current session
     */
    public static SessionCache getSessionCache()
    {
        SessionCache cache = sessionCacheMap.get(SecurityManager.getSession());
        if( cache == null )
        {
           log.log( Level.SEVERE, "Session cache not found: SecurityManager.getSession() = " + SecurityManager.getSession() + "\nsessionCacheMap = " + sessionCacheMap );
        }
        return cache;
    }

    /**
     * Returns SessionCache by session ID
     */
    public static SessionCache getSessionCache(String sessionID)
    {
        return sessionCacheMap.get(sessionID);
    }

    /**
     * Add cache for session. Is used at the beginning of request.
     */
    public static synchronized SessionCache addSessionCache(String sessionID)
    {
        SessionCache cache = new SessionCache();
        sessionCacheMap.put(sessionID, cache);
        return cache;
    }

    /**
     * Remove cache for session. Is used at the end of request
     */
    public static synchronized void removeSessionCache(String sessionID)
    {
        sessionCacheMap.remove(sessionID);
    }
}
