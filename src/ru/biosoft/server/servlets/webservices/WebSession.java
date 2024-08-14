package ru.biosoft.server.servlets.webservices;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.access.security.UserPermissions;

/**
 * Session wrapper for BioUML web
 */
public class WebSession
{
    private static final String WEB_SESSION_OBJECT_KEY = "webSessionObject";
    private static final String WEB_SESSION_REFRESH_TREE_PATH_KEY = "refreshTreePath";
    public static final String CURRENT_USER_NAME = "currentUser";

    protected static final Logger log = Logger.getLogger(WebSession.class.getName());

    protected Object session;

    public static WebSession getCurrentSession()
    {
        Object webSessionObject = SessionCacheManager.getSessionCache().getObject(WEB_SESSION_OBJECT_KEY);
        if(!(webSessionObject instanceof WebSession))
        {
            log.log(Level.SEVERE, "Unknown session requested", new Exception());
            return null;
        }
        return (WebSession)webSessionObject;
    }

    public static WebSession getSession( Object session )
    {
        synchronized(session)
        {
            String sessionId;
            try
            {
                Method getIdMethod = session.getClass().getMethod("getId", new Class[] {});
                sessionId = (String)getIdMethod.invoke(session, new Object[] {});
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get ID of session");
                return null;
            }
            SessionCache sessionCache = SessionCacheManager.getSessionCache(sessionId);
            if(sessionCache == null)
            {
                sessionCache = SessionCacheManager.addSessionCache(sessionId);
            }
            Object webSessionObject = sessionCache.getObject(WEB_SESSION_OBJECT_KEY);
            SecurityManager.addThreadToSessionRecord(Thread.currentThread(), sessionId);
            if(webSessionObject instanceof WebSession)
            {
                WebSession webSession = (WebSession)webSessionObject;
                webSession.session = session;
                return webSession;
            }
            return new WebSession(session);
        }
    }

    public static WebSession findSession( String sessionId )
    {
        SessionCache sessionCache = SessionCacheManager.getSessionCache( sessionId );
        if( sessionCache != null && sessionCache.getObject(WEB_SESSION_OBJECT_KEY) != null )
        {
            return ( WebSession )sessionCache.getObject( WEB_SESSION_OBJECT_KEY );
        }
        return null;
    }        

    public WebSession(Object session)
    {
        this.session = session;
        updateInCache();
    }

    public void updateInCache()
    {
        SessionCacheManager.getSessionCache(getSessionId()).addObject(WEB_SESSION_OBJECT_KEY, this, true);
    }

    /**
     * Marks thread as belonging to current session
     * @param t - thread to mark
     */
    public static void addThread(Thread t)
    {
        WebSession session = getCurrentSession();
        if(session != null)
        {
            SecurityManager.addThreadToSessionRecord(t, session.getSessionId());
        }
    }

    /**
     * Get session ID
     */
    public String getSessionId()
    {
        try
        {
            Method getIdMethod = session.getClass().getMethod("getId", new Class[] {});
            return (String)getIdMethod.invoke(session, new Object[] {});
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get ID of session");
            return null;
        }
    }

    public String getUserName()
    {
        Object nameObj = getValue(WebSession.CURRENT_USER_NAME);
        return nameObj == null ? "?" : nameObj.toString();
    }

    /**
     * Get object from session by key
     */
    public Object getValue(String key)
    {
        try
        {
            Method getValueMethod = session.getClass().getMethod("getValue", String.class);
            Object result = getValueMethod.invoke(session, key);
            return result;
        }
        catch( Exception e )
        {
            if(e instanceof InvocationTargetException && e.getCause() instanceof IllegalStateException)
                throw new BiosoftInvalidSessionException( toString() );
            throw ExceptionRegistry.translateException( e );
        }
    }

    /**
     * Put object to session
     */
    public void putValue(String key, Object value)
    {
        try
        {
            Method putValueMethod = session.getClass().getMethod("putValue", String.class, Object.class);
            putValueMethod.invoke(session, key, value);
        }
        catch( Exception e )
        {
            if(e instanceof InvocationTargetException && e.getCause() instanceof IllegalStateException)
                throw new BiosoftInvalidSessionException( toString() );
            throw ExceptionRegistry.translateException( e );
        }

    }

    /**
     * Remove object from session
     */
    public boolean removeValue(String key)
    {
        try
        {
            Method removeValueMethod = session.getClass().getMethod( "removeValue", String.class );
            removeValueMethod.invoke( session, key );
            return true;
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
            return false;
        }
    }

    public void updateActivity(Map<String, String> arguments)
    {
        SessionCacheManager.getSessionCache(getSessionId()).addObject("lastActivity", System.currentTimeMillis(), true);
        String dc = arguments.get("dc");
        if(dc == null)
            dc = arguments.get("de");
        if(dc != null)
            SessionCacheManager.getSessionCache(getSessionId()).addObject("lastPath", dc, true);
    }

    public synchronized Set<ru.biosoft.access.core.DataElementPath> popRefreshPaths()
    {
        Set<ru.biosoft.access.core.DataElementPath> paths = getRefreshPaths();
        Set<ru.biosoft.access.core.DataElementPath> result = new TreeSet<>( paths );
        paths.clear();
        return result;
    }

    public synchronized void pushRefreshPath(DataElementPath path)
    {
        try
        {
            getRefreshPaths().add( path );
        }
        catch( BiosoftInvalidSessionException e )
        {
            //Ignore refresh paths for invalidated sessions
        }
    }

    private Set<ru.biosoft.access.core.DataElementPath> getRefreshPaths()
    {
        Set<ru.biosoft.access.core.DataElementPath> paths = (DataElementPathSet)getValue( WEB_SESSION_REFRESH_TREE_PATH_KEY );
        if(paths == null)
            putValue( WEB_SESSION_REFRESH_TREE_PATH_KEY, paths = new DataElementPathSet() );
        return paths;
    }


    public static class SessionInfo
    {
        long lastActivity;
        String userName;
        String sessionId;
        String lastPath;
        /**
         * @return the lastActivity
         */
        public long getLastActivity()
        {
            return lastActivity;
        }
        /**
         * @return the userName
         */
        public String getUserName()
        {
            return userName;
        }
        /**
         * @return the sessionId
         */
        public String getSessionId()
        {
            return sessionId;
        }
        /**
         * @return the lastPath
         */
        public String getLastPath()
        {
            return lastPath;
        }
    }

    @Override
    public String toString()
    {
        String sessionId = getSessionId();
        if(sessionId == null)
            return "WebSession(invalid)";
        SessionCache cache = SessionCacheManager.getSessionCache(sessionId);
        UserPermissions currentUserPermission = SecurityManager.getCurrentUserPermission();
        String user = currentUserPermission == null ? null : currentUserPermission.getUser();
        if(user == null) user = "(not-logged-in)";
        String lastPath = cache == null ? null : (String)cache.getObject("lastPath");
        return sessionId+" (user: "+user+(lastPath==null?"":"; lastPath: "+lastPath)+")";
    }

    public static List<SessionInfo> getSessions()
    {
        Map<String,SessionInfo> sessions = new HashMap<>();
        for(String sessionId: SecurityManager.getSessionIds())
        {
            SessionCache cache = SessionCacheManager.getSessionCache(sessionId);
            if(cache == null) continue;
            SessionInfo sessionInfo = new SessionInfo();
            Object value = cache.getObject("lastActivity");
            sessionInfo.lastActivity = value instanceof Long?(Long)value:-1;
            String user = SecurityManager.getSessionPermission(sessionId).getUser();
            sessionInfo.userName = user == null? "(not-logged-in)" : user;
            value = cache.getObject("lastPath");
            sessionInfo.lastPath = value instanceof String?(String)value:"";
            sessionInfo.sessionId = sessionId;
            sessions.put(sessionId, sessionInfo);
        }
        return StreamEx.ofValues(sessions).sorted(Comparator.comparingLong( info -> info.lastActivity)).toList();
    }

    public void invalidate()
    {
        //log.info("Session "+getSessionId()+" invalidated!", new Throwable());
        try
        {
            session.getClass().getMethod("invalidate").invoke(session);
        }
        catch( Exception e )
        {
        }
    }

    public synchronized Map<String, Object> getImagesMap()
    {
        Object tablesObj = getValue(WebServicesServlet.IMAGES);
        Map<String, Object> images;
        if( tablesObj == null || ! ( tablesObj instanceof Map ) )
        {
            images = new ConcurrentHashMap<>();
            putValue(WebServicesServlet.IMAGES, images);
        }
        else
        {
            images = (Map)tablesObj;
        }
        return images;
    }

    /**
     * Puts image to session
     */
    public void putImage(String completeName, BufferedImage image)
    {
        Map<String, Object> images = getImagesMap();
        images.put(completeName, image);
    }
}
