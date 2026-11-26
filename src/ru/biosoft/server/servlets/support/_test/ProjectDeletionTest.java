package ru.biosoft.server.servlets.support._test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.LocalRepository;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.server.servlets.support.SupportServlet;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

public class ProjectDeletionTest extends AbstractBioUMLTest
{
    private static final String PROJECT = "Project with untracked files";
    private Path testRoot;
    private Path project;
    private LocalRepository repository;
    private boolean replaceDirectoryAfterRepositoryRemoval;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        testRoot = TempFiles.dir( "project-deletion" ).toPath();
        Path projects = Files.createDirectory( testRoot.resolve( "projects" ) );
        project = Files.createDirectory( projects.resolve( PROJECT ) );
        Files.writeString( Files.createDirectories( project.resolve( "Data/nested" ) ).resolve( "sequence.fasta" ), ">example\nATCG\n" );
        ExProperties.store( repositoryProperties( PROJECT, project ),
                project.resolve( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ).toFile() );
        repository = openRepository( projects );
        assertTrue( repository.contains( PROJECT ) );
    }

    private LocalRepository openRepository(Path projects) throws Exception
    {
        return new LocalRepository( null, repositoryProperties( "projects", projects ) )
        {
            @Override
            protected void doRemove(String name) throws Exception
            {
                super.doRemove( name );
                if( replaceDirectoryAfterRepositoryRemoval )
                {
                    Files.move( project, testRoot.resolve( "original-project" ) );
                    Files.writeString( project, "replacement must survive" );
                }
            }
        };
    }

    @Override
    protected void tearDown() throws Exception
    {
        try
        {
            if( repository != null )
                repository.close();
        }
        finally
        {
            if( testRoot != null )
                ApplicationUtils.removeDir( testRoot.toFile() );
            super.tearDown();
        }
    }

    public void testDeletesUntrackedFilesAndNestedDirectories() throws Exception
    {
        Path outside = Files.writeString( testRoot.resolve( "outside.txt" ), "outside the project" );
        Files.createSymbolicLink( project.resolve( "Data/nested/link" ), outside );
        assertNull( SupportServlet.deleteProject( PROJECT, repository ) );
        assertTrue( Files.notExists( project ) );
        assertTrue( Files.exists( outside ) );
        assertFalse( repository.contains( PROJECT ) );
    }

    public void testDoesNotFollowLinkedCollectionDuringRepositoryRemoval() throws Exception
    {
        Properties properties = repositoryProperties( PROJECT, project );
        properties.setProperty( DataCollectionConfigConstants.REMOVE_CHILDREN, "true" );
        ExProperties.store( properties, project.resolve( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ).toFile() );

        Path outside = Files.createDirectory( testRoot.resolve( "external" ) );
        Path outsideConfig = outside.resolve( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE );
        ExProperties.store( repositoryProperties( "linked", outside ), outsideConfig.toFile() );
        Files.createSymbolicLink( project.resolve( "linked" ), outside );

        assertNull( SupportServlet.deleteProject( PROJECT, repository ) );
        assertTrue( Files.notExists( project ) );
        assertTrue( Files.exists( outsideConfig ) );
    }

    public void testRetryAfterRepositoryMetadataWasRemoved() throws Exception
    {
        replaceDirectoryAfterRepositoryRemoval = true;
        String error = SupportServlet.deleteProject( PROJECT, repository );
        assertNotNull( error );
        assertTrue( error, error.startsWith( "Cannot completely delete project" ) );
        assertEquals( "replacement must survive", Files.readString( project ) );
        Path original = testRoot.resolve( "original-project" );
        assertTrue( Files.notExists( original.resolve( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) ) );
        assertTrue( Files.exists( original.resolve( "Data/nested/sequence.fasta" ) ) );

        Files.move( project, testRoot.resolve( "replacement" ) );
        Files.move( original, project );
        replaceDirectoryAfterRepositoryRemoval = false;
        repository.close();
        repository = openRepository( project.getParent() );
        assertNull( SupportServlet.deleteProject( PROJECT, repository ) );
        assertTrue( Files.notExists( project ) );
        assertFalse( repository.contains( PROJECT ) );
        try( var entries = Files.list( project.getParent() ) )
        {
            assertEquals( 0, entries.count() );
        }
    }

    private static Properties repositoryProperties(String name, Path path)
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, path.toString() );
        return properties;
    }
}
