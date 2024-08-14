package biouml.plugins.node;

import java.io.File;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.tasks.process.AbstractProcessLauncher;
import ru.biosoft.tasks.process.LauncherFactory;
import ru.biosoft.tasks.process.ProcessLauncher;
import ru.biosoft.util.LazyValue;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;

public class NodeConfig
{
    private static final String NODE_CONFIG = "NodeConfig";
    /**
     * The path to BioUML server in Node host
     */
    private final String bioumlServerPath;
    private static final String BIOUML_SERVER_PATH = "Path";

    /**
     * The URL of BioUML Master
     */
    private final String serverLink;
    private static final String BIOUML_MASTER_LINK = "ServerLink";

    /**
     * The path to 'java' binary on Node host
     */
    private final String javaPath;
    private static final String JAVA_PATH = "JavaPath";

    /**
     * Java options used to launch BioUMLNode on Node host
     */
    private final String javaOpts;
    private static final String JAVA_OPTS = "JavaOpts";

    /**
     * The launcher used to launch BioUMLNode process from Master host on Node host
     */
    private final String launcherId;
    private static final String LAUNCHER = "Launcher";

    private NodeConfig(Preferences preferences) throws Exception
    {
        bioumlServerPath = preferences.getStringValue( BIOUML_SERVER_PATH, System.getProperty( "biouml.server.path" ) );
        if( bioumlServerPath == null )
            throw new Exception( "BioUML node is not configured: Path is missing." );
        serverLink = preferences.getStringValue( BIOUML_MASTER_LINK, null );
        if( serverLink == null )
            throw new Exception( "BioUML node is not configured: ServerLink is missing." );
        javaPath = preferences.getStringValue( JAVA_PATH, "java" );
        javaOpts = preferences.getStringValue( JAVA_OPTS, "" );
        launcherId = preferences.getStringValue( LAUNCHER, null );
        if( launcherId == null )
            throw new Exception( "BioUML node is not configured: Launcher is missing." );
    }

    public String getBioumlServerPath()
    {
        return bioumlServerPath;
    }

    public String getServerLink()
    {
        return serverLink;
    }

    public String getJavaPath()
    {
        return javaPath;
    }

    public String getJavaOpts()
    {
        return javaOpts;
    }

    public String getLauncherId()
    {
        return launcherId;
    }
    
    public File getSharedFolder() throws Exception
    {
        ProcessLauncher launcher = LauncherFactory.getLauncher( getLauncherId() );
        return launcher.getSharedFolder();
    }
    

    private static LazyValue<NodeConfig> instance = new LazyValue<NodeConfig>( NODE_CONFIG )
    {
        @Override
        protected NodeConfig doGet() throws Exception
        {
            Preferences nodePreferences = Application.getPreferences().getPreferencesValue( NODE_CONFIG );
            if( nodePreferences == null )
                throw new IllegalStateException( "BioUML node is not configured: NodeConfig is missing." );
            NodeConfig config = new NodeConfig( nodePreferences );
            initSecurityManager( config );
            return config;
        }

        private void initSecurityManager(NodeConfig config) throws Exception
        {
            Preferences launcherConfig = LauncherFactory.getLauncherConfig( config.getLauncherId() );
            if( launcherConfig == null )
                return;
            String sharedFolderPath = launcherConfig.getStringValue( AbstractProcessLauncher.SHARED_FOLDER, null );
            if( sharedFolderPath != null )
                BiosoftSecurityManager.setSharedFolder( sharedFolderPath );
        }
    };

    public static NodeConfig getInstance()
    {
        return instance.get();
    }
    
    public static boolean isConfigured()
    {
        Preferences preferences = Application.getPreferences();
        if(preferences == null)
            return false;
        return preferences.getPreferencesValue( NODE_CONFIG ) != null;
    }
}
