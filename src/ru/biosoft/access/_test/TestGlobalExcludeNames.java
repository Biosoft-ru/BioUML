package ru.biosoft.access._test;

import java.io.File;
import java.util.List;
import java.util.Properties;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

/**
 * Verifies that the global {@code exclude-names} property (stored under the "Global" set of
 * preferences.xml) hides matching sub-directories in every {@link LocalRepository}, in addition to
 * the per-folder {@code exclude-names} defined in a folder's own config.
 */
public class TestGlobalExcludeNames extends AbstractBioUMLTest
{
    private LocalRepository repository;
    private File repositoryDir;
    private Preferences previousPreferences;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        previousPreferences = Application.getPreferences();
    }

    @Override
    protected void tearDown() throws Exception
    {
        try
        {
            if(repository != null)
                repository.close();
        }
        catch( Exception ignore )
        {
        }
        if(repositoryDir != null)
            com.developmentontheedge.application.ApplicationUtils.removeDir( repositoryDir );
        Application.setPreferences( previousPreferences );
        super.tearDown();
    }

    /** Builds a repository containing the requested sub-directories. */
    private void buildRepository( String... subDirs ) throws Exception
    {
        repositoryDir = TempFiles.dir( "globalExcludeRepo" );
        for( String subDir : subDirs )
            new File( repositoryDir, subDir ).mkdirs();

        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "globalExcludeTest" );
        properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, repositoryDir.getAbsolutePath() );
        repository = new LocalRepository( null, properties );
        CollectionFactory.registerRoot( repository );
    }

    /** Stores a global exclude-names value in the "Global" preferences set. */
    private void setGlobalExcludeNames( String value )
    {
        Preferences preferences = new Preferences();
        Preferences global = new Preferences();
        global.addValue( LocalRepository.GLOBAL_EXCLUDE_NAMES, value );
        preferences.add( new DynamicProperty( "Global", "Global", "Global preferences", Preferences.class, global ) );
        Application.setPreferences( preferences );
    }

    /** Plain (no config file) sub-directories hidden by the global exclude-names. */
    public void testGlobalExcludePlainFolders() throws Exception
    {
        buildRepository( "luceneIndex", ".git", "visible" );
        setGlobalExcludeNames( "luceneIndex;.git" );

        assertFalse( "luceneIndex should be hidden", repository.contains( "luceneIndex" ) );
        assertFalse( ".git should be hidden", repository.contains( ".git" ) );
        assertTrue( "visible should be present", repository.contains( "visible" ) );
        List<String> names = repository.getNameList();
        assertFalse( names.contains( "luceneIndex" ) );
        assertFalse( names.contains( ".git" ) );
        assertTrue( names.contains( "visible" ) );
    }

    /** A sub-directory that HAS a default.config is also hidden when its name matches the global exclude-names. */
    public void testGlobalExcludeConfigFolder() throws Exception
    {
        File repoDir = TempFiles.dir( "globalExcludeRepoConfig" );
        File configDir = new File( repoDir, "luceneIndex" );
        configDir.mkdirs();
        File visibleDir = new File( repoDir, "visible" );
        visibleDir.mkdirs();

        ExProperties config = new ExProperties();
        config.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "luceneIndex" );
        config.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        config.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, configDir.getAbsolutePath() );
        com.developmentontheedge.application.ApplicationUtils.writeString( new File( configDir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ), config.toString() );

        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "globalExcludeTestConfig" );
        properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, repoDir.getAbsolutePath() );
        repository = new LocalRepository( null, properties );
        CollectionFactory.registerRoot( repository );
        repositoryDir = repoDir;

        setGlobalExcludeNames( "luceneIndex" );

        assertFalse( "config-bearing luceneIndex should be hidden", repository.contains( "luceneIndex" ) );
        assertTrue( "visible should be present", repository.contains( "visible" ) );
    }

    /** No global exclude-names set: nothing is hidden. */
    public void testNoGlobalExclude() throws Exception
    {
        buildRepository( "luceneIndex", ".git", "visible" );
        Application.setPreferences( new Preferences() );

        assertTrue( "luceneIndex should be visible", repository.contains( "luceneIndex" ) );
        assertTrue( ".git should be visible", repository.contains( ".git" ) );
        assertTrue( "visible should be present", repository.contains( "visible" ) );
    }

    /** Per-folder exclude-names still works when no global exclude-names is set. */
    public void testPerFolderExcludeStillWorks() throws Exception
    {
        File repoDir = TempFiles.dir( "globalExcludeRepoPerFolder" );
        File hiddenDir = new File( repoDir, "secret" );
        hiddenDir.mkdirs();
        File visibleDir = new File( repoDir, "visible" );
        visibleDir.mkdirs();

        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "globalExcludeTestPerFolder" );
        properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, repoDir.getAbsolutePath() );
        properties.setProperty( LocalRepository.EXCLUDE_NAMES, "secret" );
        repository = new LocalRepository( null, properties );
        CollectionFactory.registerRoot( repository );
        repositoryDir = repoDir;

        Application.setPreferences( new Preferences() );

        assertFalse( "secret should be hidden by per-folder exclude-names", repository.contains( "secret" ) );
        assertTrue( "visible should be present", repository.contains( "visible" ) );
    }
}
