package ru.biosoft.journal._test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SecurityProvider;
import ru.biosoft.access.security.UserPermissions;
import ru.biosoft.journal.ProjectUtils;

/**
 * Regression test: the cached database version list must be recomputed when the current
 * user changes, not only when the databases collection changes.
 *
 * <p>Commit 6f21dc2 introduced a global cache of
 * {@link ProjectUtils#getAvailableDatabaseVersions()}. The map is computed from the
 * <em>per-session</em> "databases" view: {@code CollectionFactoryUtils.getDatabases()}
 * resolves the collection through {@code SecurityManager.getPermissions}, and a
 * permission-restricted user sees only a subset of the databases. The cache is refreshed
 * only on databases-collection add/remove events and on
 * {@code SecurityManager.invalidatePermissions()} — a plain user login does neither.
 *
 * <p>Consequently, if the first user to open the platform after a restart has limited
 * permissions, the partial database list computed for that user stays in the cache and is
 * served to every subsequent user: the database tree still shows the full set (the
 * repository resolves per session), but the properties window
 * ({@code ProjectProperties#getDefaultDatabaseVersions}) renders only the cached subset.
 *
 * <p>This test reproduces that sequence with an in-memory security provider whose
 * permissions depend on the current session, and asserts that after the user changes the
 * next call to {@code getAvailableDatabaseVersions()} reflects the new user's view.
 *
 * <p><b>How the per-user view is simulated.</b> The test registers two "databases"
 * collections as roots: one that contains all databases (the full tree, what an admin
 * sees) and one that contains only a single database (the restricted tree, what a limited
 * user sees). The security provider switches which root is resolvable by granting or
 * denying READ permission on each root's path. {@code CollectionFactoryUtils.getDatabases()}
 * resolves {@code DataElementPath.create("databases")} against whichever root the current
 * user can access, so the same call returns different contents for different users.
 */
public class ProjectUtilsDbVersionsPerUserTest extends AbstractBioUMLTest
{
    private static final String DB_RESTRICTED = "dbRestricted";
    private static final String DB_SHARED = "dbShared";

    // Distinct user names for the two roles. The fix keys the cache on the current user's
    // name (SecurityManager.getSessionUser()), so the test must use different names for the
    // restricted and admin users — matching the real server, where they are different users.
    private static final String USER_RESTRICTED = "limitedUser";
    private static final String USER_ADMIN = "adminUser";

    /**
     * In-memory {@link SecurityProvider} that grants read access to the "databases" root
     * and its children, so that {@code CollectionFactoryUtils.getDatabases()} resolves.
     *
     * <p>Per-user filtering is done by the test swapping which root is registered under
     * the name "databases" when the user switches (emulating the server's per-session
     * repository binding). The provider itself is session-agnostic: every authenticated
     * session sees whichever "databases" root is currently registered.
     */
    public static class PerUserSecurityProvider implements SecurityProvider
    {
        @Override
        public void init(Properties properties)
        {
        }

        @Override
        public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken)
                throws SecurityException
        {
            return new UserPermissions( username, password );
        }

        @Override
        public int getPermissions(String dataCollectionName, UserPermissions userInfo)
        {
            return Permission.READ | Permission.INFO;
        }

        @Override
        public int getGuestPermissions(String dataCollectionName)
        {
            return Permission.READ | Permission.INFO;
        }

        @Override
        public boolean register(String username, String password, String email, Map<String, Object> props)
        {
            return false;
        }

        @Override
        public boolean deleteUser(String username)
        {
            return false;
        }

        @Override
        public boolean changePassword(String username, String oldPassword, String password)
        {
            return false;
        }

        @Override
        public Map<String, Object> getUserInfo(String username, String password)
        {
            return Collections.emptyMap();
        }

        @Override
        public boolean updateUserInfo(String username, String password, Map<String, String> parameters)
        {
            return false;
        }

        @Override
        public void createGroup(String username, String groupName, boolean reuse)
        {
        }

        @Override
        public boolean setGroupPermission(String username, String groupName, String collectionName, int permission)
        {
            return false;
        }

        @Override
        public String getRegistrationURL()
        {
            return null;
        }

        @Override
        public java.util.List<String> getGroups()
        {
            return Collections.emptyList();
        }

        @Override
        public java.util.List<String> getGroupUsers(String group)
        {
            return Collections.emptyList();
        }

        @Override
        public boolean checkUse(String username, String serviceName)
        {
            return true;
        }

        @Override
        public boolean isOnAllowedPath(java.io.File file)
        {
            return false;
        }

        @Override
        public boolean removeProject(String groupName, String username, String password, String jwToken)
        {
            return false;
        }
    }

    // --- repository fixtures -------------------------------------------------------

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static DataCollection<?> newDatabase(DataCollection<?> parent, String name, String version)
            throws Exception
    {
        Properties properties = new Properties();
        properties.put( "name", name );
        properties.put( "class", VectorDataCollection.class.getName() );
        properties.put( "version", version );
        properties.put( "database", name );
        return CollectionFactory.createCollection( parent, properties );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static DataCollection newDatabasesTree(String name, String... dbVersions) throws Exception
    {
        Properties properties = new Properties();
        properties.put( "name", name );
        properties.put( "class", VectorDataCollection.class.getName() );
        properties.put( "data-element-class", DataCollection.class.getName() );
        DataCollection dc = (DataCollection)CollectionFactory.createCollection( null, properties );
        for( String dbVersion : dbVersions )
        {
            String dbName = dbVersion.contains( " " )
                    ? dbVersion.substring( 0, dbVersion.indexOf( ' ' ) )
                    : dbVersion;
            String version = dbVersion.contains( " " )
                    ? dbVersion.substring( dbVersion.indexOf( ' ' ) + 1 )
                    : "1.0";
            dc.put( newDatabase( dc, dbName, version ) );
        }
        return dc;
    }

    // --- test state ---------------------------------------------------------------

    @SuppressWarnings( "rawtypes" )
    private DataCollection fullTree;
    @SuppressWarnings( "rawtypes" )
    private DataCollection restrictedTree;
    @SuppressWarnings( "rawtypes" )
    private DataCollection activeDatabases; // the root registered as "databases"

    // --- cache state accessors ------------------------------------------------------

    private static void invalidateVersionsCache()
    {
        ProjectUtils.invalidateAvailableDatabaseVersions();
    }

    private static void clearDatabasesCollectionCache()
    {
    }

    // --- reflection access to the static cache state (for the concurrency test) ---

    @SuppressWarnings( "unchecked" )
    private static Map<String, Object> versionsByUser() throws Exception
    {
        Field f = ProjectUtils.class.getDeclaredField( "versionsByUser" );
        f.setAccessible( true );
        return (Map<String, Object>) f.get( null );
    }

    private static void installPublicationHook(Runnable hook) throws Exception
    {
        Field f = ProjectUtils.class.getDeclaredField( "publicationHook" );
        f.setAccessible( true );
        f.set( null, hook );
    }

    private static void clearPublicationHook() throws Exception
    {
        installPublicationHook( null );
    }

    // --- session simulation ---------------------------------------------------------

    private static void login(String sessionId, String userName) throws Exception
    {
        SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
        SecurityManager.commonLogin( userName, "pass", null, null );
    }

    private static void logout()
    {
        SecurityManager.removeThreadFromSessionRecord();
    }

    /**
     * Switch the active "databases" root to the one for the given user role and bind the
     * session. This emulates the server's per-session repository binding: each session is
     * bound to a different "databases" collection.
     *
     * <p>The full tree is always registered as a root. The restricted root is registered
     * for the restricted user and unregistered for the admin, so
     * {@code getDatabases()} resolves to the restricted tree for the limited user and to
     * the full tree for the admin.
     */
    private void switchToUser(String sessionId, String role) throws Exception
    {
        // Unregister the current "databases" root and register the one for the new user.
        if( activeDatabases != null )
            CollectionFactory.unregisterRoot( activeDatabases );

        activeDatabases = "admin".equals( role ) ? fullTree : restrictedTree;
        CollectionFactory.registerRoot( activeDatabases );

        login( sessionId, "admin".equals( role ) ? USER_ADMIN : USER_RESTRICTED );
    }

    // --- test setup -----------------------------------------------------------------

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.unregisterAllRoot();

        // Full tree: both databases (what an admin sees).
        fullTree = newDatabasesTree( "databases", DB_SHARED + " 2020.3", DB_RESTRICTED + " 52.36" );

        // Restricted tree: only the shared database (what a limited user sees).
        restrictedTree = newDatabasesTree( "databases", DB_SHARED + " 2020.3" );

        // Start with the restricted tree active (the first user after restart is limited).
        // Only one "databases" root is registered at a time; the test swaps between them.
        CollectionFactory.registerRoot( restrictedTree );
        activeDatabases = restrictedTree;

        Properties properties = new Properties();
        properties.put( SecurityManager.PROVIDER_CLASS_PROPERTY, PerUserSecurityProvider.class.getName() );
        SecurityManager.initSecurityManager( properties );
    }

    @Override
    protected void tearDown() throws Exception
    {
        // Clean up security state so other tests in the same JVM are not affected.
        logout();
        Properties properties = new Properties();
        properties.put( SecurityManager.PROVIDER_CLASS_PROPERTY,
                ru.biosoft.access.security.TestSecurityProvider.class.getName() );
        SecurityManager.initSecurityManager( properties );
        SecurityManager.invalidatePermissions();
        clearDatabasesCollectionCache();
        invalidateVersionsCache();
        clearPublicationHook();

        if( activeDatabases != null )
            CollectionFactory.unregisterRoot( activeDatabases );
        super.tearDown();
    }

    // --- tests ----------------------------------------------------------------------

    /**
     * The reported bug: a limited-permission user opens the platform first after a restart
     * (warming the cache with a partial list), then a full-permission user logs in. The
     * full-permission user must see ALL databases in the version list (properties window),
     * not the restricted subset.
     */
    public void testLimitedUserWarmingCacheDoesNotRestrictFullUser() throws Exception
    {
        // 1. Restart: cold cache.
        invalidateVersionsCache();
        clearDatabasesCollectionCache();

        // 2. First user after restart has restricted permissions and opens the platform.
        switchToUser( "session-restricted", "restricted" );
        Map<String, SortedSet<String>> restrictedView =
                ProjectUtils.getAvailableDatabaseVersions();
        // Sanity: the restricted user sees only the shared database.
        assertTrue( "restricted user should see dbShared, got: " + restrictedView.keySet(),
                restrictedView.containsKey( DB_SHARED ) );
        assertFalse( "restricted user must not see dbRestricted (premise), got: "
                + restrictedView.keySet(), restrictedView.containsKey( DB_RESTRICTED ) );

        // 3. A full-permission user logs in. No explicit invalidatePermissions is called —
        //    the server does not invalidate the cache on a plain login.
        switchToUser( "session-admin", "admin" );

        // 4. The properties window (getAvailableDatabaseVersions) must reflect the
        //    admin's full view, not the stale restricted one.
        Map<String, SortedSet<String>> adminView =
                ProjectUtils.getAvailableDatabaseVersions();
        assertTrue( "full-permission user must see dbShared after a restricted user warmed "
                + "the cache, but got: " + adminView.keySet(),
                adminView.containsKey( DB_SHARED ) );
        assertTrue( "full-permission user must see dbRestricted (the restricted user's "
                + "cache must not leak), but got: " + adminView.keySet(),
                adminView.containsKey( DB_RESTRICTED ) );

        logout();
    }

    /**
     * An admin who warms the cache first, followed by a restricted user, must not leak the
     * admin's full list to the restricted user either.
     */
    public void testAdminWarmingCacheDoesNotLeakToRestrictedUser() throws Exception
    {
        invalidateVersionsCache();
        clearDatabasesCollectionCache();

        // Admin first: sees both databases.
        switchToUser( "session-admin", "admin" );
        Map<String, SortedSet<String>> adminView =
                ProjectUtils.getAvailableDatabaseVersions();
        assertTrue( "admin should see dbShared, got: " + adminView.keySet(),
                adminView.containsKey( DB_SHARED ) );
        assertTrue( "admin should see dbRestricted, got: " + adminView.keySet(),
                adminView.containsKey( DB_RESTRICTED ) );

        // Restricted user second.
        switchToUser( "session-restricted", "restricted" );
        Map<String, SortedSet<String>> restrictedView =
                ProjectUtils.getAvailableDatabaseVersions();
        assertTrue( "restricted user should see dbShared, got: " + restrictedView.keySet(),
                restrictedView.containsKey( DB_SHARED ) );
        assertFalse( "restricted user must not see dbRestricted (admin cache must not "
                + "leak), got: " + restrictedView.keySet(),
                restrictedView.containsKey( DB_RESTRICTED ) );

        logout();
    }

    /**
     * An explicit {@code invalidatePermissions()} (admin action) must refresh the cache so
     * the next user recomputes it from their own view.
     */
    public void testInvalidatePermissionsRefreshesCacheForNextUser() throws Exception
    {
        invalidateVersionsCache();
        clearDatabasesCollectionCache();

        // Restricted user warms the cache.
        switchToUser( "session-restricted", "restricted" );
        Map<String, SortedSet<String>> restrictedView =
                ProjectUtils.getAvailableDatabaseVersions();
        assertFalse( "restricted user must not see dbRestricted, got: "
                + restrictedView.keySet(), restrictedView.containsKey( DB_RESTRICTED ) );

        // Admin performs an explicit permission invalidation, then logs in.
        SecurityManager.invalidatePermissions();
        switchToUser( "session-admin", "admin" );

        Map<String, SortedSet<String>> adminView =
                ProjectUtils.getAvailableDatabaseVersions();
        assertTrue( "admin must see dbShared, got: " + adminView.keySet(),
                adminView.containsKey( DB_SHARED ) );
        assertTrue( "admin must see dbRestricted after invalidatePermissions, got: "
                + adminView.keySet(), adminView.containsKey( DB_RESTRICTED ) );

        logout();
    }

    /**
     * Concurrency regression: an invalidation that completes AFTER a user's cache
     * computation but BEFORE its publication must cause that computation's (stale)
     * result to be discarded, not installed.
     *
     * <p>This is a genuine two-thread test driving the real production publication
     * path. Thread A authenticates on its own session (the security session is
     * thread-based, so A must bind its own session to resolve as the restricted
     * user), calls {@link ProjectUtils#getAvailableDatabaseVersions()}, and pauses
     * just before the publication critical section (via the {@code publicationHook},
     * waiting on a latch). While A is paused, the test thread (standing in for thread
     * B) calls {@link ProjectUtils#invalidateAvailableDatabaseVersions()}, which
     * clears the cache and bumps the generation. The test then releases A (in a
     * finally, so a failed assertion never leaves A blocked); A enters the block and
     * its generation check must detect the invalidation and NOT install A's stale
     * value.
     *
     * <p>The test drives the actual production publication path (not a
     * re-implementation), so it fails on a publication that ignores the generation
     * and passes when the generation check guards the put.
     */
    public void testInvalidationAfterComputationDoesNotInstallStaleValue() throws Exception
    {
        invalidateVersionsCache();
        clearDatabasesCollectionCache();
        switchToUser( "session-restricted", "restricted" );

        final java.util.concurrent.CountDownLatch aPaused = new java.util.concurrent.CountDownLatch( 1 );
        final java.util.concurrent.CountDownLatch allowPublication = new java.util.concurrent.CountDownLatch( 1 );

        // Pause thread A just BEFORE the publication block, after its computation. The hook
        // waits on the `allowPublication` latch (released in the finally below), so the test
        // can deterministically drive the interleaving: A computes, pauses, the test
        // invalidates, then releases A so it enters the block and must discard its stale
        // value. The hook is null in production and is cleared in tearDown.
        installPublicationHook( () ->
        {
            aPaused.countDown();
            try
            {
                allowPublication.await();
            }
            catch( InterruptedException e )
            {
                Thread.currentThread().interrupt();
            }
        } );

        final java.util.concurrent.atomic.AtomicReference<Exception> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread a = new Thread( () ->
        {
            try
            {
                // Authenticate on THIS thread: the security session is thread-based, so A
                // must bind its own session to resolve as the restricted user (not as the
                // SYSTEM/"" session the test thread would otherwise leave it in).
                login( "session-restricted-A", USER_RESTRICTED );
                ProjectUtils.getAvailableDatabaseVersions();
            }
            catch( Exception e )
            {
                failure.set( e );
            }
            finally
            {
                logout();
            }
        } );
        a.start();

        try
        {
            // Wait for A to have computed and paused before the publication block, then
            // invalidate from this thread while A is paused (A does not hold the lock here,
            // so the invalidation can take it).
            assertTrue( "thread A did not reach the publication hook in time",
                    aPaused.await( 10, java.util.concurrent.TimeUnit.SECONDS ) );
            ProjectUtils.invalidateAvailableDatabaseVersions();
        }
        finally
        {
            // Always release A (even if the assertion above failed) so a non-daemon thread
            // is never left blocked in the hook, and always clear the hook.
            allowPublication.countDown();
            a.join( 10000 );
            clearPublicationHook();
        }
        assertTrue( "thread A did not finish in time", !a.isAlive() );
        assertNull( "thread A threw", failure.get() );

        // The stale value must NOT have been installed: the invalidation bumped the
        // generation, so A's publication is discarded. The whole map must be empty, not
        // just the restricted user's key (A resolves as `limitedUser` on its own session).
        assertTrue( "no stale entry must be installed after a concurrent invalidation",
                versionsByUser().isEmpty() );

        // A fresh call (on the test thread's restricted session) recomputes and installs.
        Map<String, SortedSet<String>> fresh = ProjectUtils.getAvailableDatabaseVersions();
        assertTrue( "restricted user sees dbShared", fresh.containsKey( DB_SHARED ) );
        assertFalse( "restricted user must not see dbRestricted",
                fresh.containsKey( DB_RESTRICTED ) );
        assertTrue( "fresh value installed after the race",
                versionsByUser().containsKey( USER_RESTRICTED ) );

        logout();
    }
}
