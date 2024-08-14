package ru.biosoft.access.security;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.eclipse.core.runtime.Platform;

import one.util.streamex.EntryStream;
import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.exception.ProductNotAvailableException;
import ru.biosoft.access.users.FilteredUsersRepository;
import ru.biosoft.access.users.UsersRepository;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.ApplicationUtils;

/**
 * Facade for authorization system
 */
public class SecurityManager
{
    private static final String NODE_SESSION_PREFIX = "node_";
    public static final @Nonnull String SYSTEM_SESSION = "system";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SESSION_ID = "sessionId";

    public static final String PROVIDER_CLASS_PROPERTY = "securityProvider";

    // If present, such session ID is considered to belong to administrator
    // Use carefully!
    public static final String ADMIN_SESSION_PROPERTY = "adminSession";

    protected static final Logger log = Logger.getLogger(SecurityManager.class.getName());

    //public static final long timeLimit = 1000 * 3600 * 24;//one day
    public static final long timeLimit = 1000L * 3600 * 24 * 365;//one year

    private static final Permission PRIVILEGED_PERMISSION = new Permission(Permission.ALL, "", "", 0);

    private static final Map<String, UserPermissions> sessionToPermission = new ConcurrentHashMap<>();
    private static final Map<Thread, String> threadToSession = Collections.synchronizedMap(new WeakHashMap<Thread, String>());

    private static final ThreadLocal<Integer> privilegedThread = new ThreadLocal<>();

    private static final Map<String, Integer> guestPermissionsMap = new WeakHashMap<>();

    private static SecurityProvider securityProvider = null;

    private static String adminSession = null;

    private static boolean isThreadPrivileged()
    {
        Integer privileged = privilegedThread.get();
        return privileged != null && privileged > 0;
    }

    private static void setThreadPrivileged(boolean privileged)
    {
        Integer oldValue = privilegedThread.get();
        if(oldValue == null)
            oldValue = 0;
        if(privileged)
            privilegedThread.set(oldValue+1);
        else
            privilegedThread.set(oldValue-1);
    }

    /**
     * Initialization. Should be call when application starts.
     */
    public static void initSecurityManager(Properties properties)
    {
        String providerClassName = properties.getProperty(PROVIDER_CLASS_PROPERTY);
        try
        {
            Class<? extends SecurityProvider> providerClass = providerClassName == null ? SQLSecurityProvider.class
                    : ClassLoading.loadSubClass( providerClassName, SecurityProvider.class );
            securityProvider = providerClass.newInstance();
            securityProvider.init(properties);
            adminSession = properties.getProperty(ADMIN_SESSION_PROPERTY);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot init security provider: " + ExceptionRegistry.log(e));
        }
    }

    /**
     * Get current {@link SecurityProvider}
     * @return
     */
    public static SecurityProvider getSecurityProvider()
    {
        return securityProvider;
    }

    public static Permission getPermissions(DataElementPath dataCollectionPath)
    {
        return getPermissions(dataCollectionPath.toString(), true);
    }

    public static Permission getPermissions(String dataCollectionPath)
    {
        return getPermissions(dataCollectionPath, true);
    }

    /**
     * Get permissions by collection name and properties if necessary
     * @param create create guest permissions automatically if not logged in
     */
    protected static synchronized Permission getPermissions(String dataCollectionName, boolean create)
    {
        if(securityProvider == null || Thread.currentThread().getName().equals("Finalizer") || isThreadPrivileged())    // Bypass security check for Finalizer
        {
            return PRIVILEGED_PERMISSION;
        }
        String sessionId = getSession();
        if(adminSession != null && adminSession.equals(sessionId))
            return PRIVILEGED_PERMISSION;
        long currentTime = System.currentTimeMillis();
        if( !sessionId.equals(SYSTEM_SESSION) )
        {
            UserPermissions ps = sessionToPermission.get(sessionId);
            if( ps == null )
            {
                if(!create)
                    return null;
                ps = new UserPermissions("", "");
                sessionToPermission.put(sessionId, ps);
            }
            ps.updateExpirationTime();
            Permission permission = null;

            Hashtable<String, Permission> dbToPermission = ps.getDbToPermission();
            synchronized( dbToPermission )
            {
                if( dbToPermission.containsKey(dataCollectionName) )
                {
                    Permission p = dbToPermission.get(dataCollectionName);
                    if( p.getExpirationTime() > currentTime )
                    {
                        permission = p;
                    }
                    else
                    {
                        dbToPermission.remove(dataCollectionName);
                    }
                }

                if( permission == null )
                {
                    permission = authorize(dataCollectionName, ps, sessionId);
                    dbToPermission.put(dataCollectionName, permission);
                }
            }
            if(permission.getSessionId() == null || permission.getSessionId().isEmpty())
            {
                permission = new Permission(permission.getPermissions(), permission.getUserName(), sessionId, permission.getExpirationTime());
            }
            return permission;
        }
        if(!create)
            return null;

        Integer guestPermissions = guestPermissionsMap.get(dataCollectionName);
        if( guestPermissions == null )
        {
            guestPermissions = securityProvider.getGuestPermissions(dataCollectionName);
            guestPermissionsMap.put(dataCollectionName, guestPermissions);
        }
        return new Permission(guestPermissions, "", "", currentTime + timeLimit);
    }

    private static Permission authorize(String dataCollectionName, UserPermissions userInfo, String sessionId)
    {
        long currentTime = System.currentTimeMillis();
        int permissionFlags = securityProvider.getPermissions(dataCollectionName, userInfo);
        Permission permission = new Permission(permissionFlags, userInfo.getUser(), sessionId, currentTime + SecurityManager.timeLimit);

        if(isRecursiveDeleteAllowed( dataCollectionName, permission, userInfo ))
        {
            permissionFlags |= Permission.DELETE;
            permission = new Permission(permissionFlags, userInfo.getUser(), sessionId, currentTime + SecurityManager.timeLimit);
        }

        return permission;
    }

    private static boolean isRecursiveDeleteAllowed(String dataCollectionName, Permission permission, UserPermissions userInfo)
    {
        if(permission.isAdminAllowed())
            return true;
        if(!permission.isWriteAllowed())
            return false;
        //Check that all childs have delete permissions
        DataElementPath dataCollectionPath = DataElementPath.create( dataCollectionName );
        boolean recursiveDeleteAllowed = EntryStream.of( userInfo.getDbToPermission() )
            .mapKeys( DataElementPath::create )
            .filterKeys( path->path.isDescendantOf( dataCollectionPath ) )
                .values().map( Permission::isDeleteAllowed ).reduce( Boolean::logicalAnd ).orElse( false );
        return recursiveDeleteAllowed;
    }

    /**
     * Login to data collection
     */
    public static synchronized Permission login(String dataCollectionName, String username, String password)
    {
        long currentTime = System.currentTimeMillis();
        //this is the best place to clean all expired records in sessionToPermission map
        cleanSessionToPermissionMap(currentTime);

        String sessionId = getSession();
        if( !sessionId.equals(SYSTEM_SESSION) )
        {
            UserPermissions ps = new UserPermissions(username, password);
            sessionToPermission.put(sessionId, ps);
            SessionCacheManager.addSessionCache(sessionId);

            Permission permission = authorize(dataCollectionName, ps, sessionId);
            Hashtable<String, Permission> dbToPermission = ps.getDbToPermission();
            synchronized( dbToPermission )
            {
                dbToPermission.put(dataCollectionName, permission);
            }
            return permission;
        }
        return null;
    }

    /**
     * Login to server. Create PermissionStructure without data collection login.
     * @param remoteAddress TODO
     */
    public static Permission commonLogin(String username, String password, String remoteAddress, String jwToken) throws SecurityException
    {
        long currentTime = System.currentTimeMillis();
        boolean userExists = false;
        String sessionId = null;
        UserPermissions ps = securityProvider.authorize( username, password, remoteAddress, jwToken );
        if( ps != null )
        {
            userExists = true;
            //this is the best place to clean all expired records in sessionToPermission map
            cleanSessionToPermissionMap(currentTime);

            synchronized(SecurityManager.class)
            {
                sessionId = getSession();
                if( !sessionId.equals(SYSTEM_SESSION) )
                {
                    sessionToPermission.put(sessionId, ps);
                    SessionCacheManager.addSessionCache(sessionId);
                }
            }
        }
        if( !userExists )
        {
            throw new SecurityException("Invalid user name or password");
        }
        return new Permission(0, username, sessionId, currentTime + timeLimit);
    }

    public static Permission commonLoginViaOtherSession( 
         String otherSessionId, String remoteAddress, boolean bRemoveOther ) throws SecurityException
    {
        long currentTime = System.currentTimeMillis();
        UserPermissions ps = sessionToPermission.get( otherSessionId );
        String sessionId = getSession();
        sessionToPermission.put( sessionId, ps );
        SessionCacheManager.addSessionCache( sessionId );
        if( bRemoveOther && !sessionId.equals( otherSessionId ) )
        {
            sessionToPermission.remove( otherSessionId );
            SessionCacheManager.removeSessionCache( otherSessionId );
        } 
        //this is the best place to clean all expired records in sessionToPermission map
        cleanSessionToPermissionMap(currentTime);
        return new Permission(0, ps.getUser(), sessionId, currentTime + timeLimit);
    } 

    /**
     * Anonymously login to server. Create default PermissionStructure without data collection login.
     */
    public synchronized static void anonymousLogin()
    {
        String sessionId = getSession();
        if( !sessionId.equals(SYSTEM_SESSION) )
        {
            if( sessionToPermission.containsKey(sessionId) )
            {
                sessionToPermission.remove(sessionId);
            }
            UserPermissions ps;
            if(securityProvider == null)
            {
                ps = new UserPermissions("", "");
                ps.getDbToPermission().put("/", PRIVILEGED_PERMISSION);
            } else
            {
                ps = securityProvider.authorize( "", "", null, null );
                if(ps == null)
                    throw new SecurityException("Anonymous login is not allowed");
            }
            sessionToPermission.put(sessionId, ps);
            SessionCacheManager.addSessionCache(sessionId);
        }
    }

    public static void commonLogout()
    {
        String sessionId = getSession();
        if( !sessionId.equals(SYSTEM_SESSION) )
        {
            UserPermissions userPermissions = sessionToPermission.get(sessionId);
            if( userPermissions != null )
            {
                userPermissions.setDead(true);
                removeThreadFromSessionRecord();
                cleanSessionToPermissionMap(System.currentTimeMillis());
            }
        }
    }

    /**
     * Remove expired records
     */
    protected static synchronized void cleanSessionToPermissionMap(long currentTime)
    {
        List<String> toDelete = new ArrayList<>();
        for(Entry<String, UserPermissions> entry: sessionToPermission.entrySet())
        {
            String sessionId = entry.getKey();
            UserPermissions ps = entry.getValue();

            if( ps.isDead() && !threadToSession.containsValue(sessionId) )
            {
                toDelete.add(sessionId);
                continue;
            }
            Hashtable<String, Permission> dbToPermission = ps.getDbToPermission();
            synchronized( dbToPermission )
            {
                List<String> permissionsToDelete = new ArrayList<>();
                for( Map.Entry<String, Permission> dbToPermissionEntry : dbToPermission.entrySet() )
                {
                    Permission p = dbToPermissionEntry.getValue();
                    if( p.getExpirationTime() < currentTime )
                    {
                        permissionsToDelete.add(dbToPermissionEntry.getKey());
                    }
                }
                for( String dcName : permissionsToDelete )
                {
                    dbToPermission.remove(dcName);
                }
                if( dbToPermission.size() == 0 && ps.isExpired() && !threadToSession.containsValue( sessionId ) )
                {
                    toDelete.add(sessionId);
                }
            }
        }

        for( String sid : toDelete )
        {
            sessionToPermission.remove(sid);
            SessionCacheManager.removeSessionCache(sid);
        }
    }      

    /**
     * Add record to threadToSession map. This method should be called at the beginning of server request
     */
    public static void addThreadToSessionRecord(Thread thread, String sessionId)
    {
        if( sessionId != null )
        {
            threadToSession.put(thread, sessionId);
        }
    }

    public static void removeThreadFromSessionRecord()
    {
        threadToSession.remove(Thread.currentThread());
    }

    /**
     * Adds specified thread to the same session as current thread
     * @param t
     */
    public static synchronized void addThread(Thread t)
    {
        addThreadToSessionRecord(t, getSession());
    }

    /**
     * Get current session ID
     */
    public static @Nonnull String getSession()
    {
        String session = threadToSession.get(Thread.currentThread());
        if( session == null )
            return SYSTEM_SESSION;
        if( session.startsWith(NODE_SESSION_PREFIX) )
            return session.substring(NODE_SESSION_PREFIX.length());
        return session;
    }

    /**
     * Generate new session ID
     */
    public static String generateSessionId()
    {
        synchronized(threadToSession)
        {
            String newSessionId = UUID.randomUUID().toString();
            while( threadToSession.containsValue(newSessionId) )
            {
                newSessionId = UUID.randomUUID().toString();
            }
            return newSessionId;
        }
    }

    public static String createNodeSessionId()
    {
        if(isNode())
            return getSession();
        return NODE_SESSION_PREFIX+getSession();
    }

    public static UserPermissions getCurrentUserPermission()
    {
        String sessionId = getSession();
        if( !sessionId.equals(SYSTEM_SESSION) )
        {
            UserPermissions userPermissions = sessionToPermission.get(sessionId);
            if( userPermissions != null )
                userPermissions.updateExpirationTime();
            return userPermissions;
        }
        return null;
    }

    /**
     * @return maximum memory (in bytes) allowed for single process of current user
     * TODO request memory limits from backend security provider
     */
    public static long getMaximumMemoryPerProcess()
    {
        return Runtime.getRuntime().maxMemory() / ( isAdmin() ? 1 : getSessionUser() == null ? 10 : 5 );
    }

    /**
     * @return maximum number of background threads allowed for current user
     * TODO request thread number limits from backend security provider
     */
    public static int getMaximumThreadsNumber()
    {
        return ApplicationUtils.getPreferredThreadsNumber();
    }

    /**
     * Returns permissions for given session (for ADMIN only)
     */
    public static UserPermissions getSessionPermission(String sessionId)
    {
        if( !authorize("", getCurrentUserPermission(), getSession()).isAllowed(
                Permission.ADMIN) )
            throw new SecurityException("Access denied");
        if( sessionId != null )
        {
            return sessionToPermission.get(sessionId);
        }
        return null;
    }

    /**
     * Returns user name for given session
     */
    public static String getSessionUser()
    {
        String sessionId = getSession();
        UserPermissions userPermissions = sessionToPermission.get(sessionId);
        if( userPermissions != null )
            return userPermissions.getUser();
        return null;
    }

    /**
     * Returns user name for given session
     */
    public static List<String> getUsersForElement(Object object, String key)
    {
        return usersForElement(object, key).values().map(UserPermissions::getUser).toList();
    }

    private static EntryStream<SessionCache, UserPermissions> usersForElement(Object object, String key) {
        return EntryStream.of(sessionToPermission).mapKeys(SessionCacheManager::getSessionCache).nonNullKeys()
                .filterKeys(cache -> cache.getObject(key) == object);
    }

    public static void removeObjectFromUserCaches(Object object, String key)
    {
        for(SessionCache cache : usersForElement(object, key).keys())
        {
            cache.removeObject(key);
        }
    }

    public static boolean checkSessionPosessElement(String session, Object object, String key)
    {
        SessionCache sessionCache = SessionCacheManager.getSessionCache(session);
        if(sessionCache == null) return false;
        return sessionCache.getObject(key) == object;
    }

    /**
     * Returns list of active sessions (for ADMIN only)
     */
    public static Set<String> getSessionIds()
    {
        if( !isAdmin() )
            throw new SecurityException("Access denied");
        return sessionToPermission.keySet();
    }

    public static Set<String> getSessionIdsForUser(String user)
    {
        if(!isAdmin())
            throw new SecurityException("Access denied");
        return doGetSessionIdsFoUser( user );
    }

    public static Set<String> getSessionIdsForCurrentUser()
    {
        String user = getSessionUser();
        if(user == null)
            return Collections.emptySet();
        return doGetSessionIdsFoUser( user );
    }

    private static Set<String> doGetSessionIdsFoUser(String user)
    {
        Set<String> result = new HashSet<>();
        sessionToPermission.forEach( (session, perm) -> {
            if( user.equals( perm.getUser() ) )
                result.add( session );
        } );
        return result;
    }

    public static Object runPrivileged(PrivilegedAction action) throws Exception
    {
        StackTraceElement callerElement = (new Exception()).getStackTrace()[1];
        if(callerElement.isNativeMethod())// Called via reflection
        {
            throw new SecurityException("Access denied");
        }
        setThreadPrivileged(true);
        try
        {
            return BiosoftSecurityManager.escapeFromSandbox( action );
        }
        finally
        {
            setThreadPrivileged(false);
        }
    }

    public static boolean isSessionInvalid(String sessionId)
    {
        if( sessionToPermission.containsKey(sessionId) )
            return false;
        return true;
    }

    public static boolean isSessionDead(String sessionId)
    {
        if(isTestMode() || securityProvider == null) return false;
        UserPermissions userPermissions = sessionToPermission.get(sessionId);
        if( userPermissions == null )
            return true;
        return userPermissions.isDead();
    }

    /**
     * @return true if currently logged in user is server administrator
     */
    public static boolean isAdmin()
    {
        try
        {
            if(isTestMode()) return true;
            Permission permission = SecurityManager.getPermissions("/", false);
            return permission != null && permission.isAllowed(Permission.ADMIN);
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE,  "Cannot check privileges", e );
            return false;
        }
    }

    public static boolean isNode()
    {
        String sessionId = threadToSession.get(Thread.currentThread());
        return sessionId != null && sessionId.startsWith(NODE_SESSION_PREFIX);
    }

    private static volatile Boolean testMode = null;

    public static boolean isTestMode()
    {
        if(testMode == null)
        {
            testMode = Platform.getBundle("ru.biosoft.access") == null;
        }
        return testMode;
    }

    /**
     * Mark current permissions as invalid and reload it from database
     */
    public static void invalidatePermissions()
    {
        //clear guest permissions map
        guestPermissionsMap.clear();

        //reload user permissions
        cleanSessionToPermissionMap(System.currentTimeMillis());
        for( Map.Entry<String, UserPermissions> entry : sessionToPermission.entrySet() )
        {
            UserPermissions permissions = entry.getValue();
            Hashtable<String, Permission> dbToPermission = permissions.getDbToPermission();
            for( String dbName : dbToPermission.keySet() )
            {
                Permission permission = authorize(dbName, permissions, entry.getKey());
                dbToPermission.put(dbName, permission);
            }
        }

        //reinit users tree
        DataCollection<?> usersDC = CollectionFactory.getDataCollection("users");
        if( usersDC instanceof FilteredUsersRepository )
        {
            DataCollection<?> primary = ( (FilteredUsersRepository)usersDC ).getPrimaryCollection();
            if( primary instanceof UsersRepository )
            {
                ( (UsersRepository)primary ).reinit();
            }
        }
    }

    //reload permissions of current user for a given path
    public static void invalidatePermissions(String path)
    {
        cleanSessionToPermissionMap(System.currentTimeMillis());

        String sessionId = getSession();
        UserPermissions userPermissions = sessionToPermission.get(sessionId);
        if(userPermissions == null)
            return;
        Hashtable<String, Permission> dbToPermission = userPermissions.getDbToPermission();
        Permission permission = authorize(path, userPermissions, sessionId);
        dbToPermission.put(path, permission);
    }

    public static boolean isProductAvailable(String productName)
    {
        if( productName == null || productName.isEmpty() || isTestMode() || securityProvider == null )
            return true;
        UserPermissions permission = getCurrentUserPermission();
        return permission != null && permission.isProductAvailable(productName);
    }

    public static void checkProductAvailable(String productName) throws ProductNotAvailableException
    {
        if(!isAdmin() && !isProductAvailable(productName))
            throw new ProductNotAvailableException(productName);
    }

    public static boolean isExperimentalFeatureHidden()
    {
        return !isProductAvailable("Experimental");
    }

    public static Method isExperimentalFeatureHiddenMethod()
    {
        try
        {
            return SecurityManager.class.getMethod("isExperimentalFeatureHidden");
        }
        catch( NoSuchMethodException e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    public static void check(DataElementPath dataCollectionPath, String method) throws RepositoryAccessDeniedException
    {
        Permission permission = getPermissions(dataCollectionPath);
        if( !permission.isMethodAllowed(method) )
        {
            throw new RepositoryAccessDeniedException( dataCollectionPath, getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
    }
}
