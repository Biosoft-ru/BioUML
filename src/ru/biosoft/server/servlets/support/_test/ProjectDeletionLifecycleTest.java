package ru.biosoft.server.servlets.support;

import static ru.biosoft.server.servlets.support.SupportServlet.ProjectDeletionStatus.COMPLETE;
import static ru.biosoft.server.servlets.support.SupportServlet.ProjectDeletionStatus.INCOMPLETE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Preferences;

import biouml.plugins.mcp.support.McpProjectSupport;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.ProjectStorage;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.TestSecurityProvider;
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
        Path interrupted = deletion.readPending().backup();
        assertFalse( deletion.readPending().backupComplete() );
        assertTrue( Files.exists( project.resolve( "default.config" ) ) );

        assertEquals( INCOMPLETE, delete( (db, output) -> {
            Files.writeString( output, "partial SQL" );
            throw new IOException( "fixture dump exited nonzero" );
        }, db -> drops.incrementAndGet() ).status() );
        assertFalse( deletion.readPending().backupComplete() );
        assertFalse( interrupted.equals( deletion.readPending().backup() ) );
        assertEquals( 0, drops.get() );
        assertTrue( Files.exists( project.resolve( "sequence.fasta" ) ) );

        Files.writeString( project.resolve( "after-failure.txt" ), "latest contents" );
        assertEquals( COMPLETE, delete( this::writeDump, db -> drops.incrementAndGet() ).status() );
        assertEquals( 1, drops.get() );
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
        assertEquals( COMPLETE, delete( (db, output) -> fail( "Keep the original backup" ), db -> drops.incrementAndGet() ).status() );
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
        assertFalse( deletion.readPending().backupComplete() );
        repository.removeDataCollectionListener( veto );

        assertEquals( COMPLETE, delete( this::writeDump, db -> fail() ).status() );
        assertTrue( Files.notExists( project ) );
        assertEquals( "external", Files.readString( external ) );
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

    public void testMcpResumesWhenProjectIsMissingFromRepository() throws Exception
    {
        globals.setValue( "BackupFolder", "BackupFolder" );
        ProjectDeletion.Pending state = deletion.begin( project, null );
        state = ProjectBackup.ensureComplete( deletion, state, this::writeDump );
        repository.remove( PROJECT );
        deletion.removeDirectory( state );
        assertFalse( repository.contains( PROJECT ) );
        assertTrue( Files.exists( ProjectStorage.deletionRecord( projects.toRealPath(), PROJECT ) ) );

        assertNull( McpProjectSupport.delete( PROJECT ).getError() );
        assertNull( deletion.readPending() );
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

    private static class FixtureServlet extends SupportServlet
    {
        @Override
        protected void login(Map params)
        {
        }

        JSONObject delete(boolean retryOnly, boolean skipBiostore) throws Exception
        {
            return deleteProjects( Map.of( "user", new String[]{"fixture"}, "pass", new String[]{""},
                    "projects", new String[]{"[\"" + PROJECT + "\"]"}, "retryOnly", new String[]{Boolean.toString( retryOnly )},
                    "skipBiostore", new String[]{Boolean.toString( skipBiostore )} ) );
        }
    }

    private SupportServlet.ProjectDeletionResult delete(ProjectBackup.DatabaseDump dump, SupportServlet.DropProjectDatabase drop)
            throws Exception
    {
        return SupportServlet.deleteProjectResult( PROJECT, repository, dump, drop );
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
