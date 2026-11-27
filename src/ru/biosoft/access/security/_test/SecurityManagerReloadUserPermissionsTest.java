package ru.biosoft.access.security._test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SecurityProvider;
import ru.biosoft.access.security.UserPermissions;

/**
 * Regression test for the MCP project-creation permission loss.
 *
 * <p>Scenario (as reported): a project created through an MCP session was immediately visible in a
 * parallel web UI session, but about an hour later the project became inaccessible there and only a
 * re-login brought it back.
 *
 * <p>Mechanism: {@code McpProjectSupport.create} granted the new project to the user's other sessions
 * by injecting an in-memory {@link Permission} with {@code UserPermissions.TIMEOUT} (one hour)
 * expiration via {@code SecurityManager.addUserPermission}. When that permission expired,
 * {@code SecurityManager.getUserPermission} evicted it and re-authorized through
 * {@code RemoteSecurityProvider.getPermissions}, which only consults the session's login-time
 * permission snapshot — the new project is not in it — so the session got 0 permissions, cached for
 * a year ({@code SecurityManager.timeLimit}). Re-login rebuilt the snapshot from BioStore (which has
 * the project group grant), which is why it "fixed" the project.
 *
 * <p>The fix is {@code SecurityManager.reloadUserPermissions(user)}: re-authorize every session of
 * the user against the security provider, so the new grant enters each session's snapshot with the
 * normal long expiration. The test provider below mimics {@code RemoteSecurityProvider}: the login
 * response seeds the session snapshot from the currently granted paths, and per-path authorization
 * consults only that snapshot.
 *
 * <p>Sessions are static JVM state, so every test uses its own user name and only counts that
 * user's sessions.
 */
public class SecurityManagerReloadUserPermissionsTest extends AbstractBioUMLTest
{
    private static String projectPath(String user)
    {
        return "users/" + user + "/Projects/NewProject";
    }

    /**
     * Mimics RemoteSecurityProvider: {@link #authorize} seeds the session snapshot from the paths
     * granted at that moment; {@link #getPermissions} consults only the session snapshot, never the
     * live grant set.
     */
    public static class SnapshotSecurityProvider implements SecurityProvider
    {
        /** Paths granted "in the security database"; mutable so the test can grant a project later. */
        static final Set<String> grantedPaths = ConcurrentHashMap.newKeySet();
        /** How many times each user was (re-)authorized. */
        static final Map<String, Integer> authorizeCount = new ConcurrentHashMap<>();

        @Override
        public void init(Properties properties)
        {
        }

        @Override
        public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken)
                throws SecurityException
        {
            authorizeCount.merge( username, 1, Integer::sum );
            UserPermissions result = new UserPermissions( username, password );
            long expiration = System.currentTimeMillis() + 365L * 24 * 3600 * 1000;
            for( String path : grantedPaths )
            {
                if( path.startsWith( "users/" + username + "/" ) )
                    result.getDbToPermission().put( path,
                            new Permission( Permission.INFO | Permission.READ | Permission.WRITE | Permission.DELETE,
                                    username, "", expiration ) );
            }
            return result;
        }

        @Override
        public int getPermissions(String dataCollectionName, UserPermissions userInfo)
        {
            // Snapshot-only lookup, exactly like RemoteSecurityProvider.getPermissions.
            Permission permission = userInfo.getDbToPermission().get( dataCollectionName );
            return permission == null ? 0 : permission.getPermissions();
        }

        @Override
        public int getGuestPermissions(String dataCollectionName)
        {
            return 0;
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
        public List<String> getGroups()
        {
            return Collections.emptyList();
        }

        @Override
        public List<String> getGroupUsers(String group)
        {
            return Collections.emptyList();
        }

        @Override
        public boolean checkUse(String username, String serviceName)
        {
            return true;
        }

        @Override
        public boolean isOnAllowedPath(File file)
        {
            return false;
        }

        @Override
        public boolean removeProject(String groupName, String username, String password, String jwToken)
        {
            return false;
        }
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        SnapshotSecurityProvider.grantedPaths.clear();
        SnapshotSecurityProvider.authorizeCount.clear();
        Properties properties = new Properties();
        properties.put( SecurityManager.PROVIDER_CLASS_PROPERTY, SnapshotSecurityProvider.class.getName() );
        SecurityManager.initSecurityManager( properties );
    }

    @Override
    protected void tearDown() throws Exception
    {
        SecurityManager.removeThreadFromSessionRecord();
        Properties properties = new Properties();
        properties.put( SecurityManager.PROVIDER_CLASS_PROPERTY,
                ru.biosoft.access.security.TestSecurityProvider.class.getName() );
        SecurityManager.initSecurityManager( properties );
        SecurityManager.invalidatePermissions();
        super.tearDown();
    }

    private static void login(String sessionId, String userName) throws Exception
    {
        SecurityManager.removeThreadFromSessionRecord();
        SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
        SecurityManager.commonLogin( userName, "pass", null, null );
    }

    private static void switchTo(String sessionId)
    {
        SecurityManager.removeThreadFromSessionRecord();
        SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
    }

    /**
     * The reported bug, fixed: a web session logged in before the project existed; the project is
     * then created through a parallel (MCP) session. After {@code reloadUserPermissions} both
     * sessions resolve the project from their refreshed provider snapshot — with the provider's
     * normal long expiration, not a one-hour injected permission that would later strand them.
     */
    public void testReloadMakesNewProjectVisibleToAllUserSessions() throws Exception
    {
        String user = "reloadUser1";
        String project = projectPath( user );
        String existing = "users/" + user + "/Projects/OldProject";
        SnapshotSecurityProvider.grantedPaths.add( existing );

        // Web session logs in before the new project exists: its snapshot has only the old project.
        login( "reload-web-1", user );
        assertTrue( SecurityManager.getPermissions( existing ).isWriteAllowed() );
        assertFalse( SecurityManager.getPermissions( project ).isWriteAllowed() );

        // MCP session of the same user creates the project: the grant appears in the provider...
        login( "reload-mcp-1", user );
        SnapshotSecurityProvider.grantedPaths.add( project );

        // ...and the fix re-authorizes every session of the user against the provider.
        assertEquals( 2, SecurityManager.reloadUserPermissions( user ) );

        // Both sessions now see the project...
        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );
        switchTo( "reload-web-1" );
        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );

        // ...with the provider's long expiration, so nothing expires an hour later.
        assertTrue( "reloaded permission must outlive the one-hour injection window",
                SecurityManager.getPermissions( project ).getExpirationTime()
                        > System.currentTimeMillis() + UserPermissions.TIMEOUT );
    }

    /**
     * Without the reload, the old injection is a time bomb: once the injected permission expires,
     * re-authorization consults the login-time snapshot, finds nothing, and the session is locked
     * out (the reported symptom). Re-authorizing the user's sessions — what a re-login does —
     * recovers the project.
     */
    public void testExpiredInjectionWithoutReloadLocksSessionOut() throws Exception
    {
        String user = "reloadUser2";
        String project = projectPath( user );

        login( "reload-web-2", user );
        SnapshotSecurityProvider.grantedPaths.add( project );

        // Old MCP behavior: inject a permission that is already expired (as it would be after an hour).
        SecurityManager.addUserPermission( user, project,
                new Permission( Permission.INFO | Permission.READ | Permission.WRITE | Permission.DELETE,
                        user, "reload-mcp-2", System.currentTimeMillis() - 1000 ) );

        assertFalse( "expired injection without reload must reproduce the lock-out",
                SecurityManager.getPermissions( project ).isWriteAllowed() );

        // Re-authorizing the sessions (what reloadUserPermissions and a re-login do) recovers it.
        assertEquals( 1, SecurityManager.reloadUserPermissions( user ) );
        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );
    }

    /**
     * Only the target user's sessions are re-authorized; sessions of other users are untouched.
     */
    public void testReloadDoesNotTouchOtherUsers() throws Exception
    {
        String user = "reloadUser3";
        String other = "reloadOther3";

        login( "reload-other-3", other );
        int otherAuthorizations = SnapshotSecurityProvider.authorizeCount.getOrDefault( other, 0 ).intValue();

        login( "reload-web-3", user );
        assertEquals( 1, SecurityManager.reloadUserPermissions( user ) );

        assertEquals( "other user's session must not be re-authorized", otherAuthorizations,
                SnapshotSecurityProvider.authorizeCount.getOrDefault( other, 0 ).intValue() );
    }

    /**
     * Aliased sessions (e.g. an MCP session copied from a web login) share one UserPermissions
     * instance; it is reloaded once, and every aliased session id sees the result.
     */
    public void testAliasedSessionsShareSingleReload() throws Exception
    {
        String user = "reloadUser4";
        String project = projectPath( user );

        login( "reload-alias-src", user );
        // Copy the session like commonLoginViaOtherSession does: the new session id maps to the
        // same UserPermissions instance.
        switchTo( "reload-alias-2" );
        SecurityManager.commonLoginViaOtherSession( "reload-alias-src", null, false );

        SnapshotSecurityProvider.grantedPaths.add( project );
        int before = SnapshotSecurityProvider.authorizeCount.getOrDefault( user, 0 ).intValue();
        assertEquals( "shared UserPermissions must be reloaded once", 1,
                SecurityManager.reloadUserPermissions( user ) );
        assertEquals( before + 1, SnapshotSecurityProvider.authorizeCount.get( user ).intValue() );

        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );
        switchTo( "reload-alias-src" );
        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );
    }
}
