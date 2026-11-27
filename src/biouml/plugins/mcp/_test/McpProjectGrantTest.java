package biouml.plugins.mcp._test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import biouml.plugins.mcp.support.McpProjectSupport;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SecurityProvider;
import ru.biosoft.access.security.UserPermissions;

/**
 * Covers the grant branch of {@code McpProjectSupport.create}: after the project group is created in
 * the security provider, {@code grantProjectToUserSessions} must give every session of the user
 * durable access to the new project — including sessions whose stored credentials no longer
 * re-authorize (expired token, changed password, sudo login), and without any provider round-trip
 * that could stall project creation.
 */
public class McpProjectGrantTest extends AbstractBioUMLTest
{
    /**
     * Snapshot-semantics provider (like RemoteSecurityProvider) whose authorization can be switched
     * to failing, emulating sessions with stale credentials.
     */
    public static class FailableSnapshotSecurityProvider implements SecurityProvider
    {
        static final Set<String> grantedPaths = ConcurrentHashMap.newKeySet();
        /** When true, any (re-)authorization fails, like BioStore rejecting a stale credential. */
        static volatile boolean failAuthorization = false;
        /** Counts authorization attempts after the flag is set — must stay 0 during the grant. */
        static volatile int authorizationsWhileFailing = 0;

        @Override
        public void init(Properties properties)
        {
        }

        @Override
        public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken)
                throws SecurityException
        {
            if( failAuthorization )
            {
                authorizationsWhileFailing++;
                throw new SecurityException( "BioStore denied session renewal authorization" );
            }
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
        FailableSnapshotSecurityProvider.grantedPaths.clear();
        FailableSnapshotSecurityProvider.failAuthorization = false;
        FailableSnapshotSecurityProvider.authorizationsWhileFailing = 0;
        Properties properties = new Properties();
        properties.put( SecurityManager.PROVIDER_CLASS_PROPERTY, FailableSnapshotSecurityProvider.class.getName() );
        SecurityManager.initSecurityManager( properties );
    }

    @Override
    protected void tearDown() throws Exception
    {
        FailableSnapshotSecurityProvider.failAuthorization = false;
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
     * The create() grant branch: a web session logged in before the project existed — with
     * credentials that no longer re-authorize — must still get durable write access, and the grant
     * must not attempt any re-authorization.
     */
    public void testGrantReachesSessionWithFailingCredentialsWithoutReauthorization() throws Exception
    {
        String user = "mcpGrantUser1";
        DataElementPath project = DataElementPath.create( "users", user, "Projects", "NewProject" );

        // Web session logs in before the project exists.
        login( "mcp-grant-web", user );
        assertFalse( SecurityManager.getPermissions( project.toString() ).isWriteAllowed() );

        // MCP session of the same user; then the project group grant lands in the provider...
        login( "mcp-grant-mcp", user );
        FailableSnapshotSecurityProvider.grantedPaths.add( project.toString() );

        // ...while every stored credential goes stale (expired token, changed password, ...).
        FailableSnapshotSecurityProvider.failAuthorization = true;

        // The create() grant branch must not depend on re-authorizing any session.
        McpProjectSupport.grantProjectToUserSessions( user, project );
        assertEquals( "the grant must not attempt re-authorization", 0,
                FailableSnapshotSecurityProvider.authorizationsWhileFailing );

        // Both sessions get write (not admin) access that outlives the one-hour window.
        Permission mcpPermission = SecurityManager.getPermissions( project.toString() );
        assertTrue( mcpPermission.isWriteAllowed() );
        assertFalse( "project group grant is not admin", mcpPermission.isAdminAllowed() );

        switchTo( "mcp-grant-web" );
        Permission webPermission = SecurityManager.getPermissions( project.toString() );
        assertTrue( "session with failing credentials must still get access", webPermission.isWriteAllowed() );
        assertTrue( "grant must outlive the one-hour injection window",
                webPermission.getExpirationTime() > System.currentTimeMillis() + UserPermissions.TIMEOUT );
        assertEquals( "grant adopts the reading session's id", "mcp-grant-web", webPermission.getSessionId() );
    }

    /**
     * Deleting and recreating a project under the same name must leave the sessions with a working
     * grant for the (same) path — the injected permission is path-based and idempotent.
     */
    public void testGrantIsIdempotentForRecreatedProject() throws Exception
    {
        String user = "mcpGrantUser2";
        DataElementPath project = DataElementPath.create( "users", user, "Projects", "ReProject" );

        login( "mcp-grant-web-2", user );
        login( "mcp-grant-mcp-2", user );

        FailableSnapshotSecurityProvider.grantedPaths.add( project.toString() );
        McpProjectSupport.grantProjectToUserSessions( user, project );
        // Recreate: same path granted again (as after a delete + create with the same name).
        McpProjectSupport.grantProjectToUserSessions( user, project );

        assertTrue( SecurityManager.getPermissions( project.toString() ).isWriteAllowed() );
        switchTo( "mcp-grant-web-2" );
        Permission webPermission = SecurityManager.getPermissions( project.toString() );
        assertTrue( webPermission.isWriteAllowed() );
        assertTrue( webPermission.getExpirationTime() > System.currentTimeMillis() + UserPermissions.TIMEOUT );
    }
}
