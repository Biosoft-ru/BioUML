package ru.biosoft.server.servlets.support;

import static ru.biosoft.server.servlets.support.SupportServlet.ProjectDeletionStatus.COMPLETE;
import static ru.biosoft.server.servlets.support.SupportServlet.ProjectDeletionStatus.INCOMPLETE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.ProjectStorage;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SQLSecurityProvider;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SecurityProvider;
import ru.biosoft.access.security.SecurityProvider.ProjectGroupStatus;
import ru.biosoft.access.security.TestSecurityProvider;
import ru.biosoft.access.security.UserPermissions;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

public class ProjectDeletionLifecycleTest extends AbstractBioUMLTest
{
    private static final String PROJECT = "Delete fixture";
    private static final String DATABASE = "fixture_schema";
    private Path testRoot;
    private Path projects;
    private Path project;
    private Path backups;
    private Preferences oldPreferences;
    private Preferences globals;
    private LocalRepository repository;
    private ProjectDeletion deletion;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        oldPreferences = Application.getPreferences();
        testRoot = TempFiles.dir( "project-deletion-lifecycle" ).toPath();
        projects = Files.createDirectory( testRoot.resolve( "projects" ) );
        project = Files.createDirectory( projects.resolve( PROJECT ) );
        backups = Files.createDirectory( testRoot.resolve( "backups" ) );
        Files.writeString( project.resolve( "sequence.fasta" ), ">fixture\nATCG\n" );
        storeConfig( PROJECT, project, null );

        repository = new LocalRepository( null, repositoryProperties( "deletionTestProjects", projects ) );
        CollectionFactory.registerRoot( repository );
        Preferences preferences = new Preferences();
        globals = new Preferences();
        globals.addValue( "UserProjectsPath", repository.getCompletePath().toString() );
        globals.addValue( "BackupFolder", backups.toString() );
        preferences.addValue( "Global", globals );
        Application.setPreferences( preferences );
        deletion = new ProjectDeletion( projects, PROJECT );
    }

    @Override
    protected void tearDown() throws Exception
    {
        try
        {
            Application.setPreferences( oldPreferences );
            if( repository != null )
            {
                CollectionFactory.unregisterRoot( repository );
                repository.close();
            }
        }
        finally
        {
            if( testRoot != null )
                ApplicationUtils.removeDir( testRoot.toFile() );
            super.tearDown();
        }
    }

    public void testInterruptedBackupAndFailedDumpAreRetriedWithFreshFiles() throws Exception
    {
        addDatabaseMetadata();
        AtomicInteger drops = new AtomicInteger();
        assertEquals( INCOMPLETE, delete( (db, output) -> { throw new InterruptedException( "fixture interruption" ); },
                db -> drops.incrementAndGet() ).status() );
        Path interrupted = deletion.readPending().backupAttempt();
        assertNotNull( interrupted );
        assertNoBackupStaging();
        assertFalse( deletion.readPending().backupComplete() );
        assertTrue( Files.exists( project.resolve( "default.config" ) ) );

        assertEquals( INCOMPLETE, delete( (db, output) -> {
            Files.writeString( output, "partial SQL" );
            throw new IOException( "fixture dump exited nonzero" );
        }, db -> drops.incrementAndGet() ).status() );
        assertFalse( deletion.readPending().backupComplete() );
        assertFalse( interrupted.equals( deletion.readPending().backupAttempt() ) );
        assertNoBackupStaging();
        assertEquals( 0, drops.get() );
        assertTrue( Files.exists( project.resolve( "sequence.fasta" ) ) );

        Path abandoned = backups.resolve( PROJECT + "-" + UUID.randomUUID() );
        Path staging = Files.createDirectory( abandoned.resolveSibling( abandoned.getFileName() + ".partial" ) );
        Files.writeString( staging.resolve( "abandoned" ), "partial backup from an interrupted server" );
        deletion.save( deletion.readPending().withBackup( null, false, abandoned ) );

        Files.writeString( project.resolve( "after-failure.txt" ), "latest contents" );
        assertEquals( COMPLETE, delete( this::writeDump, db -> drops.incrementAndGet() ).status() );
        assertEquals( 1, drops.get() );
        assertTrue( Files.notExists( staging ) );
        assertTrue( Files.notExists( project ) );
        try( var paths = Files.list( backups ) )
        {
            Path complete = paths.filter( path -> !path.getFileName().toString().endsWith( ".partial" ) ).findFirst().orElseThrow();
            assertEquals( "latest contents", Files.readString( complete.resolve( "project/after-failure.txt" ) ) );
            assertEquals( "fixture SQL", Files.readString( complete.resolve( "_dump.sql" ) ) );
        }
    }

    public void testFailedDatabaseDropReservesNameAndRetryUsesCompletedBackup() throws Exception
    {
        addDatabaseMetadata();
        AtomicInteger dumps = new AtomicInteger();
        ProjectBackup.DatabaseDump dump = (db, output) -> {
            dumps.incrementAndGet();
            writeDump( db, output );
        };
        assertEquals( INCOMPLETE, delete( dump, db -> { throw new IOException( "fixture drop failed" ); } ).status() );
        assertTrue( Files.notExists( project ) );
        assertTrue( deletion.readPending().backupComplete() );

        assertCreationBlocked( () -> SupportServlet.createFileBasedProject( PROJECT, repository, null ) );
        assertCreationBlocked( () -> SupportServlet.createSQLBasedProject( PROJECT, repository ) );
        assertCreationBlocked( () -> repository.createDataCollection( PROJECT, repositoryProperties( PROJECT, project ), PROJECT, null, null ) );
        assertTrue( Files.notExists( project ) );

        AtomicInteger drops = new AtomicInteger();
        assertEquals( COMPLETE, delete( dump, db -> {
            assertEquals( DATABASE, db );
            drops.incrementAndGet();
        } ).status() );
        assertEquals( 1, dumps.get() );
        assertEquals( 1, drops.get() );
        assertNull( deletion.readPending() );
        assertNotNull( SupportServlet.createFileBasedProject( PROJECT, repository, null ) );
    }

    public void testRetryKeepsOriginalBackupAfterRepositoryRediscoversOrphan() throws Exception
    {
        ProjectDeletion.Pending state = deletion.begin( project, DATABASE );
        state = ProjectBackup.ensureComplete( deletion, state, this::writeDump );
        Path backup = state.backup();
        repository.remove( PROJECT );
        Files.move( project.resolve( "sequence.fasta" ), testRoot.resolve( "removed-sequence.fasta" ) );

        CollectionFactory.unregisterRoot( repository );
        repository.close();
        repository = new LocalRepository( null, repositoryProperties( "deletionTestProjects", projects ) );
        CollectionFactory.registerRoot( repository );
        assertTrue( repository.contains( PROJECT ) );

        AtomicInteger drops = new AtomicInteger();
        Files.writeString( project.resolve( "changed.txt" ), "new contents" );
        assertEquals( COMPLETE, delete( (db, output) -> {
            assertEquals( DATABASE, db );
            Files.writeString( output, "remaining SQL tables" );
        }, db -> drops.incrementAndGet() ).status() );
        try( var archives = Files.list( backups ) )
        {
            Path latest = archives.filter( path -> !path.equals( backup ) ).findFirst().orElseThrow();
            assertEquals( "new contents", Files.readString( latest.resolve( "project/changed.txt" ) ) );
            assertEquals( "remaining SQL tables", Files.readString( latest.resolve( "_dump.sql" ) ) );
        }
        assertEquals( "fixture SQL", Files.readString( backup.resolve( "_dump.sql" ) ) );
        assertEquals( 1, drops.get() );
        assertTrue( Files.exists( backup.resolve( "project/sequence.fasta" ) ) );
        assertTrue( Files.notExists( project ) );
    }

    public void testVetoPreservesFilesAndLinksAndRetryBacksUpAgain() throws Exception
    {
        Path external = Files.writeString( testRoot.resolve( "external" ), "external" );
        Path link = Files.createSymbolicLink( project.resolve( "link" ), external );
        DataCollectionListenerSupport veto = new DataCollectionListenerSupport()
        {
            @Override
            public void elementWillRemove(DataCollectionEvent event) throws DataCollectionVetoException
            {
                throw new DataCollectionVetoException();
            }
        };
        repository.addDataCollectionListener( veto );
        assertEquals( INCOMPLETE, delete( this::writeDump, db -> fail() ).status() );
        assertTrue( repository.contains( PROJECT ) );
        assertTrue( Files.isSymbolicLink( link ) );
        assertTrue( Files.exists( project.resolve( "sequence.fasta" ) ) );
        assertTrue( deletion.readPending().backupComplete() );
        repository.removeDataCollectionListener( veto );

        assertEquals( COMPLETE, delete( this::writeDump, db -> fail() ).status() );
        assertTrue( Files.notExists( project ) );
        assertEquals( "external", Files.readString( external ) );
    }

    public void testRetryBacksUpChangesToConfiglessProject() throws Exception
    {
        String name = "File based fixture";
        assertNotNull( SupportServlet.createFileBasedProject( name, repository, null ) );
        ProjectDeletion fileDeletion = new ProjectDeletion( projects, name );
        Path directory = fileDeletion.directory();
        assertTrue( Files.notExists( directory.resolve( "default.config" ) ) );
        ProjectDeletion.Pending original = ProjectBackup.ensureComplete( fileDeletion, fileDeletion.begin( directory, null ), this::writeDump );

        Files.writeString( directory.resolve( "new.txt" ), "new contents" );
        assertEquals( COMPLETE, SupportServlet.deleteProjectResult( name, repository, this::writeDump, db -> fail() ).status() );
        assertTrue( Files.notExists( original.backup().resolve( "project/new.txt" ) ) );
        assertArchived( "new.txt", "new contents" );
    }

    public void testBackupEnabledAfterVeto() throws Exception
    {
        globals.setValue( "BackupFolder", "BackupFolder" );
        DataCollectionListenerSupport veto = new DataCollectionListenerSupport()
        {
            @Override
            public void elementWillRemove(DataCollectionEvent event) throws DataCollectionVetoException
            {
                throw new DataCollectionVetoException();
            }
        };
        repository.addDataCollectionListener( veto );
        assertEquals( INCOMPLETE, delete( this::writeDump, db -> fail() ).status() );
        assertTrue( deletion.readPending().backupComplete() );
        assertNull( deletion.readPending().backup() );
        repository.removeDataCollectionListener( veto );

        globals.setValue( "BackupFolder", backups.toString() );
        assertEquals( COMPLETE, delete( this::writeDump, db -> fail() ).status() );
        assertArchived( "sequence.fasta", ">fixture\nATCG\n" );
    }

    public void testDumpConnectionOptions() throws Exception
    {
        assertEquals( List.of( "--host=localhost" ), ProjectBackup.dumpConnectionOptions( "jdbc:mysql://localhost/schema" ) );
        assertEquals( List.of( "--protocol=TCP", "--host=db_server", "--port=3307" ),
                ProjectBackup.dumpConnectionOptions( "jdbc:mysql://db_server:3307/schema?useSSL=false" ) );
        assertEquals( List.of( "--protocol=TCP", "--host=localhost", "--port=3307" ),
                ProjectBackup.dumpConnectionOptions( "jdbc:mysql://localhost:3307/schema" ) );
        assertEquals( List.of( "--protocol=TCP", "--host=::1", "--port=3306" ),
                ProjectBackup.dumpConnectionOptions( "jdbc:mariadb://[::1]/schema" ) );
        for( String invalid : List.of( "jdbc:mysql://user@localhost/schema", "jdbc:mysql://localhost:65536/schema",
                "jdbc:mysql://server,backup/schema" ) )
        {
            try
            {
                ProjectBackup.dumpConnectionOptions( invalid );
                fail( "Unsupported database authority: " + invalid );
            }
            catch( IOException expected )
            {
                assertTrue( expected.getMessage().contains( "supported database server" ) );
            }
        }
    }

    public void testLinkedDataIsRejectedBeforeAnyChanges() throws Exception
    {
        Path external = Files.createDirectory( testRoot.resolve( "external-data" ) );
        Path link = Files.createSymbolicLink( project.resolve( "Data" ), external );
        assertEquals( INCOMPLETE, delete( this::writeDump, db -> fail() ).status() );
        assertNull( deletion.readPending() );
        assertTrue( Files.isSymbolicLink( link ) );
        assertTrue( Files.exists( project.resolve( "default.config" ) ) );
    }

    public void testIncompleteDeletionKeepsBiostoreGroup() throws Exception
    {
        Files.createSymbolicLink( project.resolve( "Data" ), testRoot );
        var providerField = SecurityManager.class.getDeclaredField( "securityProvider" );
        providerField.setAccessible( true );
        Object previous = providerField.get( null );
        AtomicInteger groupRemovals = new AtomicInteger();
        TestSecurityProvider provider = new TestSecurityProvider()
        {
            @Override
            public boolean removeProject(String groupName, String username, String password, String jwToken)
            {
                groupRemovals.incrementAndGet();
                return true;
            }
        };
        providerField.set( null, provider );
        try
        {
            assertTrue( new FixtureServlet().delete( false, false ).toString().contains( "was not removed completely" ) );
            assertEquals( 0, groupRemovals.get() );
        }
        finally
        {
            providerField.set( null, previous );
        }
    }

    public void testWebResumesFailedBiostoreCleanup() throws Exception
    {
        var providerField = SecurityManager.class.getDeclaredField( "securityProvider" );
        providerField.setAccessible( true );
        Object previous = providerField.get( null );
        AtomicInteger groupRemovals = new AtomicInteger();
        providerField.set( null, new TestSecurityProvider()
        {
            @Override
            public boolean removeProject(String groupName, String username, String password, String jwToken)
            {
                return groupRemovals.incrementAndGet() > 1;
            }
        } );
        try
        {
            FixtureServlet servlet = new FixtureServlet();
            assertTrue( servlet.delete( false, false ).toString().contains( "was not removed completely" ) );
            assertTrue( Files.notExists( project ) );
            assertTrue( deletion.readPending().groupPending() );
            assertTrue( deletion.readPending().groupPending() );
            servlet.delete( true, false );
            assertEquals( 2, groupRemovals.get() );
            assertNull( deletion.readPending() );
        }
        finally
        {
            providerField.set( null, previous );
        }
    }

    public void testVerifiedMissingGroupDoesNotReserveName() throws Exception
    {
        withSecurityProvider( new TestSecurityProvider()
        {
            @Override
            public boolean removeProject(String groupName, String username, String password, String jwToken)
            {
                return false;
            }

            @Override
            public ProjectGroupStatus getProjectGroupStatus(String groupName)
            {
                return ProjectGroupStatus.ABSENT;
            }
        }, () -> {
            JSONObject result = new FixtureServlet().delete( false, false );
            assertEquals( result.toString(), "ok", result.getString( "type" ) );
            assertNull( deletion.readPending() );
            assertNotNull( SupportServlet.createFileBasedProject( PROJECT, repository, null ) );
        } );
    }

    public void testOriginalRequesterCanFinishAfterGroupDisappears() throws Exception
    {
        String projectPath = repository.getCompletePath().getChildPath( PROJECT ).toString();
        AtomicInteger groupRemovals = new AtomicInteger();
        AtomicInteger statusChecks = new AtomicInteger();
        TestSecurityProvider provider = new TestSecurityProvider()
        {
            @Override
            public int getPermissions(String path, UserPermissions user)
            {
                return path.equals( projectPath ) && groupRemovals.get() == 0 ? Permission.DELETE | Permission.READ : Permission.READ;
            }

            @Override
            public boolean removeProject(String groupName, String username, String password, String jwToken)
            {
                assertEquals( "The provider must only remove the group once", 1, groupRemovals.incrementAndGet() );
                throw new IllegalStateException( "Fixture interrupted after removing group" );
            }

            @Override
            public ProjectGroupStatus getProjectGroupStatus(String groupName)
            {
                // The first request lost its reply after the provider removed the group.
                return statusChecks.incrementAndGet() == 1 ? ProjectGroupStatus.UNKNOWN : ProjectGroupStatus.ABSENT;
            }
        };
        withSecurityProvider( provider, () -> {
            FixtureServlet servlet = new FixtureServlet();
            String requester = SecurityManager.getSessionUser();
            assertTrue( SecurityManager.getPermissions( projectPath ).isDeleteAllowed() );
            assertEquals( "error", servlet.delete( false, false ).getString( "type" ) );
            assertEquals( 1, groupRemovals.get() );
            assertTrue( deletion.readPending().localComplete() );
            assertEquals( requester, deletion.readPending().groupRequestedBy() );
            assertTrue( Files.notExists( project ) );

            SecurityManager.commonLogin( "other-user", "", null, null );
            assertTrue( servlet.delete( true, false ).getString( "message" ).contains( "Delete access denied" ) );
            assertNotNull( deletion.readPending() );
            SecurityManager.commonLogin( requester, "", null, null );
            assertNull( SecurityManager.getSessionAdministratorPermission() );
            assertFalse( SecurityManager.getPermissions( projectPath ).isDeleteAllowed() );
            JSONObject result = servlet.delete( true, false );
            assertEquals( result.toString(), "ok", result.getString( "type" ) );
            assertNull( deletion.readPending() );
            assertEquals( 1, groupRemovals.get() );
        } );
    }

    public void testAdminRecoveryAfterGroupPermissionIsLost() throws Exception
    {
        String projectPath = repository.getCompletePath().getChildPath( PROJECT ).toString();
        var status = new java.util.concurrent.atomic.AtomicReference<>( ProjectGroupStatus.UNKNOWN );
        TestSecurityProvider provider = new TestSecurityProvider()
        {
            @Override
            public int getPermissions(String path, UserPermissions user)
            {
                return "/".equals( path ) && "server-admin".equals( user.getUser() ) ? Permission.ADMIN : Permission.READ;
            }

            @Override
            public ProjectGroupStatus getProjectGroupStatus(String groupName)
            {
                return status.get();
            }
        };
        withSecurityProvider( provider, () -> {
            FixtureServlet servlet = new FixtureServlet();
            SecurityManager.commonLogin( "server-admin", "", null, null );
            assertFalse( SecurityManager.getPermissions( projectPath ).isDeleteAllowed() );
            assertTrue( servlet.recover( true ).getString( "message" ).contains( "No pending BioStore cleanup" ) );

            // A record from before the local-completion checkpoint was introduced.
            ProjectDeletion.Pending pending = deletion.begin( project, null ).withGroupPending( null );
            pending = ProjectBackup.ensureComplete( deletion, pending, this::writeDump );
            assertTrue( servlet.recover( true ).getString( "message" ).contains( "still has a repository entry or files" ) );
            repository.remove( PROJECT );
            deletion.removeDirectory( pending );
            SecurityManager.commonLogin( "ordinary-user", "", null, null );
            try
            {
                servlet.recover( true );
                fail( "Recovery must require authenticated root ADMIN" );
            }
            catch( SecurityException expected )
            {
                assertTrue( expected.getMessage().contains( "Server administrator" ) );
            }

            SecurityManager.commonLogin( "server-admin", "", null, null );
            assertTrue( servlet.recover( false ).getString( "message" ).contains( "cannot verify group absence" ) );
            status.set( ProjectGroupStatus.PRESENT );
            assertTrue( servlet.recover( true ).getString( "message" ).contains( "group still exists" ) );
            assertNotNull( deletion.readPending() );
            status.set( ProjectGroupStatus.UNKNOWN );
            JSONObject result = servlet.recover( true );
            assertEquals( result.toString(), "ok", result.getString( "type" ) );
            assertNull( deletion.readPending() );
            assertNotNull( SupportServlet.createFileBasedProject( PROJECT, repository, null ) );
        } );
    }

    public void testWebResponseIncludesSafeProviderReason() throws Exception
    {
        withSecurityProvider( new TestSecurityProvider()
        {
            @Override
            public boolean removeProject(String groupName, String username, String password, String jwToken)
            {
                throw new IllegalStateException( "Fixture provider unavailable <message> " + password );
            }
        }, () -> {
            JSONObject result = new FixtureServlet().delete( false, false, "fixture-credential" );
            String message = result.getString( "message" );
            assertTrue( message, message.contains( "Fixture provider unavailable &lt;message&gt; [redacted]" ) );
            assertFalse( message.contains( "fixture-credential" ) );
            assertTrue( deletion.readPending().groupPending() );
        } );
    }

    public void testPendingDeletionBlocksWebAndMcpGroupCreation() throws Exception
    {
        AtomicInteger groupCreations = new AtomicInteger();
        withSecurityProvider( new TestSecurityProvider()
        {
            @Override
            public void createGroup(String username, String groupName, boolean reuse)
            {
                groupCreations.incrementAndGet();
            }
        }, () -> {
            ProjectDeletion.Pending pending = deletion.begin( project, null ).withGroupPending( "fixture" );
            pending = ProjectBackup.ensureComplete( deletion, pending, this::writeDump );
            repository.remove( PROJECT );
            deletion.removeDirectory( pending );
            deletion.save( pending.withLocalComplete() );
            FixtureServlet servlet = new FixtureServlet();
            assertCreationBlocked( servlet::create );
            assertEquals( 0, groupCreations.get() );
            assertTrue( servlet.delete( true, true ).getString( "message" ).contains( "needs BioStore cleanup" ) );
            assertNotNull( deletion.readPending() );
        } );
    }

    public void testWebRetryRequiresPendingDeletion() throws Exception
    {
        FixtureServlet servlet = new FixtureServlet();
        assertTrue( servlet.delete( true, true ).toString().contains( "No pending deletion" ) );
        assertTrue( Files.exists( project.resolve( "sequence.fasta" ) ) );

        globals.setValue( "BackupFolder", "BackupFolder" );
        ProjectDeletion.Pending state = deletion.begin( project, null );
        state = ProjectBackup.ensureComplete( deletion, state, this::writeDump );
        repository.remove( PROJECT );
        deletion.removeDirectory( state );
        servlet.delete( true, true );
        assertNull( deletion.readPending() );
    }

    public void testSqlAdministratorDeletesGroupAndCanRecreateIt() throws Exception
    {
        withSqlGroups( connection -> {
            SQLSecurityProvider provider = new SQLSecurityProvider();
            assertTrue( provider.removeProject( PROJECT, "member", "", null ) );
            assertEquals( List.of( "fixture" ), SqlUtil.queryStrings( connection, "SELECT `user` FROM user_groups" ) );
            assertEquals( 1, SqlUtil.queryInt( connection, "SELECT COUNT(*) FROM group_permissions" ) );
            assertEquals( ProjectGroupStatus.PRESENT, provider.getProjectGroupStatus( PROJECT ) );

            assertTrue( provider.removeProject( PROJECT, "fixture", "", null ) );
            assertEquals( ProjectGroupStatus.ABSENT, provider.getProjectGroupStatus( PROJECT ) );
            assertEquals( 0, SqlUtil.queryInt( connection, "SELECT COUNT(*) FROM user_groups" ) );
            assertEquals( 0, SqlUtil.queryInt( connection, "SELECT COUNT(*) FROM group_permissions" ) );
            assertTrue( provider.removeProject( PROJECT, "fixture", "", null ) );

            provider.createGroup( "fixture", PROJECT, false );
            assertEquals( ProjectGroupStatus.PRESENT, provider.getProjectGroupStatus( PROJECT ) );
            assertEquals( List.of( "fixture" ), SqlUtil.queryStrings( connection, "SELECT `user` FROM user_groups" ) );
        } );
    }

    public void testSqlGroupRemovalRollsBackWhenFinalDeleteFails() throws Exception
    {
        withSqlGroups( connection -> {
            SqlUtil.execute( connection, "CREATE TABLE group_reference (group_id INT, FOREIGN KEY (group_id) REFERENCES `groups`(id))" );
            SqlUtil.execute( connection, "INSERT INTO group_reference VALUES (1)" );
            SQLSecurityProvider provider = new SQLSecurityProvider();

            assertFalse( provider.removeProject( PROJECT, "fixture", "", null ) );
            assertEquals( ProjectGroupStatus.PRESENT, provider.getProjectGroupStatus( PROJECT ) );
            assertEquals( 2, SqlUtil.queryInt( connection, "SELECT COUNT(*) FROM user_groups" ) );
            assertEquals( 1, SqlUtil.queryInt( connection, "SELECT COUNT(*) FROM group_permissions" ) );
        } );
    }

    public void testSqlRequesterCanRetryAfterGroupRemovalWasInterrupted() throws Exception
    {
        withSqlGroups( connection -> {
            SQLSecurityProvider provider = new SQLSecurityProvider()
            {
                @Override
                public boolean removeProject(String groupName, String username, String password, String jwToken)
                {
                    assertTrue( super.removeProject( groupName, username, password, jwToken ) );
                    throw new InterruptedGroupRemoval();
                }
            };
            withSecurityProvider( provider, () -> {
                FixtureServlet servlet = new FixtureServlet();
                String projectPath = repository.getCompletePath().getChildPath( PROJECT ).toString();
                // Grant the current session delete access. A fresh SQL login after interruption has no such grant.
                SecurityManager.addUserPermission( "fixture", projectPath, new Permission( Permission.DELETE | Permission.READ,
                        "fixture", SecurityManager.getSession(), Long.MAX_VALUE ) );
                try
                {
                    servlet.delete( false, false );
                    fail( "Expected interruption after SQL group removal" );
                }
                catch( InterruptedGroupRemoval expected )
                {
                    assertEquals( ProjectGroupStatus.ABSENT, provider.getProjectGroupStatus( PROJECT ) );
                    assertTrue( deletion.readPending().localComplete() );
                    assertTrue( Files.notExists( project ) );
                }

                SecurityManager.commonLogin( "fixture", "", null, null );
                assertFalse( SecurityManager.getPermissions( projectPath ).isDeleteAllowed() );
                JSONObject retry = servlet.delete( true, false );
                assertEquals( retry.toString(), "ok", retry.getString( "type" ) );
                assertNull( deletion.readPending() );
            } );
        } );
    }

    private static class InterruptedGroupRemoval extends Error
    {
    }

    @FunctionalInterface
    private interface SqlFixtureAction
    {
        void run(Connection connection) throws Exception;
    }

    private void withSqlGroups(SqlFixtureAction test) throws Exception
    {
        var propertiesField = GlobalDatabaseManager.class.getDeclaredField( "properties" );
        propertiesField.setAccessible( true );
        Object previous = propertiesField.get( null );
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER";
        Properties properties = new Properties();
        properties.setProperty( SqlDataCollection.JDBC_URL_PROPERTY, url );
        properties.setProperty( SqlDataCollection.JDBC_USER_PROPERTY, "sa" );
        properties.setProperty( SqlDataCollection.JDBC_PASSWORD_PROPERTY, "" );
        try( Connection connection = DriverManager.getConnection( url, "sa", "" ) )
        {
            GlobalDatabaseManager.initDatabaseManager( properties );
            SqlUtil.execute( connection, "CREATE TABLE `groups` (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) UNIQUE,"
                    + " language VARCHAR(2), description VARCHAR(255))" );
            SqlUtil.execute( connection, "CREATE TABLE user_groups (`user` VARCHAR(255), groupID INT, role VARCHAR(255))" );
            SqlUtil.execute( connection, "CREATE TABLE group_permissions (groupID INT, module VARCHAR(255), permission INT)" );
            SqlUtil.execute( connection, "CREATE TABLE permissions (user VARCHAR(255), module VARCHAR(255), permission INT)" );
            SqlUtil.execute( connection, "CREATE TABLE users (user_name VARCHAR(255), user_pass VARCHAR(255), expirationDate BIGINT, trial INT)" );
            SqlUtil.execute( connection, "CREATE TABLE user_roles (user_name VARCHAR(255), role_name VARCHAR(255))" );
            SqlUtil.execute( connection, "INSERT INTO `groups`(name) VALUES (" + SqlUtil.quoteString( PROJECT ) + ")" );
            SqlUtil.execute( connection, "INSERT INTO users VALUES ('fixture', '', 0, 0), ('member', '', 0, 0)" );
            SqlUtil.execute( connection, "INSERT INTO user_groups VALUES ('fixture', 1, 'Administrator'), ('member', 1, 'Member')" );
            SqlUtil.execute( connection, "INSERT INTO group_permissions VALUES (1, 'fixture', 15)" );
            test.run( connection );
        }
        finally
        {
            SqlConnectionPool.closeMyConnections();
            propertiesField.set( null, previous );
        }
    }

    private void withSecurityProvider(SecurityProvider provider, LocalRepository.RemovalAction test) throws Exception
    {
        var providerField = SecurityManager.class.getDeclaredField( "securityProvider" );
        providerField.setAccessible( true );
        Object previous = providerField.get( null );
        String previousSession = SecurityManager.getSession();
        provider.init( new Properties() );
        providerField.set( null, provider );
        SecurityManager.addThreadToSessionRecord( Thread.currentThread(), "deletion-" + UUID.randomUUID() );
        try
        {
            SecurityManager.commonLogin( "fixture", "", null, null );
            test.run();
        }
        finally
        {
            SecurityManager.commonLogout();
            SecurityManager.removeThreadFromSessionRecord();
            if( !SecurityManager.SYSTEM_SESSION.equals( previousSession ) )
                SecurityManager.addThreadToSessionRecord( Thread.currentThread(), previousSession );
            providerField.set( null, previous );
        }
    }

    private static class FixtureServlet extends SupportServlet
    {
        @Override
        protected void login(Map params)
        {
        }

        JSONObject delete(boolean retryOnly, boolean skipBiostore) throws Exception
        {
            return delete( retryOnly, skipBiostore, "" );
        }

        JSONObject delete(boolean retryOnly, boolean skipBiostore, String password) throws Exception
        {
            return deleteProjects( Map.of( "user", new String[]{"fixture"}, "pass", new String[]{password},
                    "projects", new String[]{"[\"" + PROJECT + "\"]"}, "retryOnly", new String[]{Boolean.toString( retryOnly )},
                    "skipBiostore", new String[]{Boolean.toString( skipBiostore )} ) );
        }

        JSONObject create() throws Exception
        {
            return createProjectWithPermissions( Map.of( "project", new String[]{PROJECT}, "projectType", new String[]{"FILE"} ) );
        }

        JSONObject recover(boolean confirm) throws Exception
        {
            return recoverProjectDeletion( Map.of( "project", new String[]{PROJECT},
                    "confirmGroupHandled", new String[]{Boolean.toString( confirm )} ) );
        }
    }

    private SupportServlet.ProjectDeletionResult delete(ProjectBackup.DatabaseDump dump, SupportServlet.DropProjectDatabase drop)
            throws Exception
    {
        return SupportServlet.deleteProjectResult( PROJECT, repository, dump, drop );
    }

    private void assertNoBackupStaging() throws IOException
    {
        try( var paths = Files.list( backups ) )
        {
            assertFalse( paths.anyMatch( path -> path.getFileName().toString().endsWith( ".partial" ) ) );
        }
    }

    private void assertArchived(String name, String contents) throws IOException
    {
        try( var paths = Files.list( backups ) )
        {
            Path archived = paths.map( path -> path.resolve( "project" ).resolve( name ) )
                    .filter( Files::isRegularFile ).findFirst().orElseThrow();
            assertEquals( contents, Files.readString( archived ) );
        }
    }

    private void addDatabaseMetadata() throws Exception
    {
        Path data = Files.createDirectory( project.resolve( "Data" ) );
        storeConfig( "Data", data, "jdbc:mysql://localhost/" + DATABASE );
    }

    private void writeDump(String database, Path output) throws IOException
    {
        assertEquals( DATABASE, database );
        Files.writeString( output, "fixture SQL" );
    }

    private static void assertCreationBlocked(LocalRepository.RemovalAction create) throws Exception
    {
        try
        {
            create.run();
            fail( "Creation must wait until the previous deletion is complete" );
        }
        catch( IOException e )
        {
            assertTrue( e.getMessage(), e.getMessage().contains( "Finish deleting project" ) );
        }
    }

    private static void storeConfig(String name, Path path, String jdbcUrl) throws Exception
    {
        Properties properties = repositoryProperties( name, path );
        if( jdbcUrl != null )
            properties.setProperty( SqlDataCollection.JDBC_URL_PROPERTY, jdbcUrl );
        ExProperties.store( properties, path.resolve( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ).toFile() );
    }

    private static Properties repositoryProperties(String name, Path path)
    {
        Properties result = new Properties();
        result.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        result.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        result.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, path.toString() );
        return result;
    }
}
