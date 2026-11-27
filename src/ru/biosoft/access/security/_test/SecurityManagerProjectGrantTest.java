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
 * <p>Mechanism: the new project was granted to the user's other sessions by injecting an in-memory
 * {@link Permission} with {@code UserPermissions.TIMEOUT} (one hour) expiration via
 * {@code SecurityManager.addUserPermission}. When that permission expired,
 * {@code SecurityManager.getUserPermission} evicted it and re-authorized through
 * {@code RemoteSecurityProvider.getPermissions}, which only consults the session's login-time
 * permission snapshot — the new project is not in it — so the session got 0 permissions, cached for
 * a year ({@code SecurityManager.timeLimit}). Re-login rebuilt the snapshot from BioStore (which has
 * the project group grant), which is why it "fixed" the project.
 *
 * <p>The fix mirrors the provider-persistent grant (the project group) into every session of the
 * user with the same long expiration ({@code SecurityManager.timeLimit}) and an empty session id,
 * so nothing expires an hour later and each session adopts its own id. These tests exercise that
 * grant semantics at the {@code SecurityManager} level; the MCP-side caller is covered by
 * {@code biouml.plugins.mcp._test.McpProjectGrantTest}.
 *
 * <p>Sessions are static JVM state, so every test uses its own user name and only inspects that
 * user's sessions.
 */
public class SecurityManagerProjectGrantTest extends AbstractBioUMLTest
{
    private static final int PROJECT_PERMISSION = Permission.INFO | Permission.READ | Permission.WRITE | Permission.DELETE;

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

        @Override
        public void init(Properties properties)
        {
        }

        @Override
        public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken)
                throws SecurityException
        {
            UserPermissions result = new UserPermissions( username, password );
            long expiration = System.currentTimeMillis() + 365L * 24 * 3600 * 1000;
            for( String path : grantedPaths )
            {
                if( path.startsWith( "users/" + username + "/" ) )
                    result.getDbToPermission().put( path,
                            new Permission( PROJECT_PERMISSION, username, "", expiration ) );
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

    /** The grant the fixed MCP project creation injects: long-lived, empty session id. */
    private static Permission longLivedGrant(String user)
    {
        return new Permission( PROJECT_PERMISSION, user, "", System.currentTimeMillis() + SecurityManager.timeLimit );
    }

    /**
     * The fix: a long-lived grant reaches every session of the user, outlives the one-hour window,
     * adopts each reading session's own id, and heals a session that had already cached a denial
     * for the project path.
     */
    public void testLongLivedGrantReachesAllSessionsAndOutlivesInjectionWindow() throws Exception
    {
        String user = "grantUser1";
        String project = projectPath( user );

        // Web session logs in before the project exists — and even hits the path once, caching a
        // denial for a year (the poisoned state a locked-out session ends up in).
        login( "grant-web-1", user );
        assertFalse( SecurityManager.getPermissions( project ).isWriteAllowed() );

        // MCP session of the same user creates the project and mirrors the persistent grant.
        login( "grant-mcp-1", user );
        SnapshotSecurityProvider.grantedPaths.add( project );
        SecurityManager.addUserPermission( user, project, longLivedGrant( user ) );

        // Both sessions see the project; the previously cached denial is overwritten.
        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );
        switchTo( "grant-web-1" );
        Permission webPermission = SecurityManager.getPermissions( project );
        assertTrue( "grant must heal a session with a cached denial", webPermission.isWriteAllowed() );

        // Nothing expires an hour later...
        assertTrue( "grant must outlive the one-hour injection window",
                webPermission.getExpirationTime() > System.currentTimeMillis() + UserPermissions.TIMEOUT );

        // ...and the empty session id adopts the reading session, not the granting one.
        assertEquals( "grant-web-1", webPermission.getSessionId() );
        switchTo( "grant-mcp-1" );
        assertEquals( "grant-mcp-1", SecurityManager.getPermissions( project ).getSessionId() );
    }

    /**
     * The reported bug's mechanism: a short-lived injection (the old behavior) locks the session
     * out when it expires, because re-authorization consults the login-time snapshot. This is why
     * the grant must be long-lived; the test documents the failure mode.
     */
    public void testExpiredShortInjectionLocksSessionOut() throws Exception
    {
        String user = "grantUser2";
        String project = projectPath( user );

        login( "grant-web-2", user );
        SnapshotSecurityProvider.grantedPaths.add( project );

        // Old MCP behavior: inject a permission that is already expired (as it would be after an hour).
        SecurityManager.addUserPermission( user, project,
                new Permission( PROJECT_PERMISSION, user, "grant-mcp-2", System.currentTimeMillis() - 1000 ) );

        assertFalse( "expired short-lived injection must reproduce the lock-out",
                SecurityManager.getPermissions( project ).isWriteAllowed() );

        // The long-lived grant recovers even this poisoned session.
        SecurityManager.addUserPermission( user, project, longLivedGrant( user ) );
        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );
    }

    /**
     * The grant touches only the target user's sessions.
     */
    public void testGrantDoesNotTouchOtherUsers() throws Exception
    {
        String user = "grantUser3";
        String other = "grantOther3";
        String project = projectPath( user );

        login( "grant-other-3", other );
        login( "grant-web-3", user );
        SecurityManager.addUserPermission( user, project, longLivedGrant( user ) );

        switchTo( "grant-other-3" );
        assertFalse( "other user must not gain access", SecurityManager.getPermissions( project ).isWriteAllowed() );
        switchTo( "grant-web-3" );
        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );
    }

    /**
     * Aliased sessions (e.g. an MCP session copied from a web login) share one UserPermissions
     * instance; the grant reaches every alias, and a dead session among them breaks nothing.
     */
    public void testAliasedAndDeadSessions() throws Exception
    {
        String user = "grantUser4";
        String project = projectPath( user );

        login( "grant-alias-src", user );
        // Copy the session like commonLoginViaOtherSession does: the new session id maps to the
        // same UserPermissions instance.
        switchTo( "grant-alias-2" );
        SecurityManager.commonLoginViaOtherSession( "grant-alias-src", null, false );

        // A second, unrelated session of the same user, marked dead (logged out, not yet cleaned).
        login( "grant-dead-4", user );
        SecurityManager.getCurrentUserPermission().setDead( true );

        SecurityManager.addUserPermission( user, project, longLivedGrant( user ) );

        switchTo( "grant-alias-src" );
        assertTrue( SecurityManager.getPermissions( project ).isWriteAllowed() );
        switchTo( "grant-alias-2" );
        assertTrue( "aliased session must see the grant", SecurityManager.getPermissions( project ).isWriteAllowed() );
    }
}
