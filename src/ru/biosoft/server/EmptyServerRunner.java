package ru.biosoft.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import biouml.splash.BioUMLConsoleSplasher;
import ru.biosoft.access.AccessCoreInit;
import ru.biosoft.access.BiosoftIconManager;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionListenerRegistry;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.QuerySystemRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.exception.BiosoftExceptionTranslator;
import ru.biosoft.access.file.FileDataCollection;
import ru.biosoft.access.security.BiosoftClassLoading;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.support.SessionPreferences;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.ExceptionTranslator;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.View.ModelResolver;
import ru.biosoft.graphics.access.DataElementModelResolver;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.server.servlets.webservices.imports.ImportProvider;
import ru.biosoft.server.servlets.webservices.providers.WebLogProvider;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.JULBeanLogger;
import ru.biosoft.util.NetworkConfigurator;
import ru.biosoft.workbench.Framework;

public class EmptyServerRunner implements IApplication
{
    protected static final Logger log = Logger.getLogger(EmptyServerRunner.class.getName());

    public static final String CONFIG_FILE = "preferences.xml";
    public static final String JAVA_LIBRARY_PATH = "JAVA_LIBRARY_PATH";

    private static final ModelResolver viewModelResolver = new DataElementModelResolver();

    private static final ExceptionTranslator translator = new BiosoftExceptionTranslator();

    ///////////////////////////////////////////////////////////////////
    // Eclipse runner
    //

    /** Top level Eclipse function that starts the server. */
    @Override
    public Object start(IApplicationContext arg0)
    {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        AccessCoreInit.init();
        
        QuerySystemRegistry.initQuerySystems();
        DataCollectionListenerRegistry.initDataCollectionListeners();

        cofigureJUL();
        ExceptionRegistry.setExceptionRegistry( translator );
        loadPreferences();
        View.setModelResolver( viewModelResolver );
        initWebLogger();

        String[] commandLineArgs = new String[0];
        Object arg = arg0.getArguments().get( "application.args" );
        if( arg instanceof String[] )
            commandLineArgs = (String[])arg;

        Properties systemProperties = getSecurityProperties(commandLineArgs);

        //initialize bioumlsupport database manager
        GlobalDatabaseManager.initDatabaseManager(systemProperties);
        SecurityManager.initSecurityManager(systemProperties);
        NetworkConfigurator.initNetworkConfiguration();

        try
        {
            for(String commandLineArg: commandLineArgs)
            {
                try
                {
                    log.info("Load repository: " + commandLineArg);
                    CollectionFactoryUtils.init();
                    Framework.initRepository( commandLineArg );
                    //CollectionFactory.createRepository(commandLineArg);
                }
                catch( Throwable t )
                {
                    ExceptionRegistry.log(t);
                }
            }
            ServletRegistry.initServlets(commandLineArgs);

            System.getProperties().put("BioUML.Server.ServiceRegistry", ServiceRegistry.class);
            System.getProperties().put("BioUML.Server.Request", Request.class);

            //init task manager database history listener
            TaskManager.getInstance().init( systemProperties );

            ImportProvider.init();

            BioUMLConsoleSplasher.printSplash();
            
            InitScripts.runSafe();

            while( true )
            {
                if( "true".equals(System.getProperties().get("BioUML.Server.Exit")) )
                {
                    System.getProperties().remove("BioUML.Server.Exit");
                    try
                    {
                        Plugins.getPlugins().close();
                        Framework.closeRepositories();
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Failed to close collections", e );
                    }
                    break;
                }
                Thread.sleep(1000);
            }
        }
        catch( Throwable t )
        {
            System.err.println("Can not start BioUML, error: " + t);
        }

        return IApplication.EXIT_OK;
    }

    protected void loadPreferences()
    {
        String serverPath = System.getProperty("biouml.server.path");

        File preferencesFile = new File(serverPath, CONFIG_FILE);
        if(!preferencesFile.exists())
        {
            File confDir = new File(serverPath, "conf");
            preferencesFile = new File(confDir, CONFIG_FILE);
        }

        SessionPreferences preferences = new SessionPreferences();
        try
        {
            preferences.load(preferencesFile.getAbsolutePath());
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Load preferences error", t);
        }
        Application.setPreferences(preferences);

        if( preferences.getValue( JAVA_LIBRARY_PATH ) != null )
        {
            ClassLoading.addJavaLibraryPath( preferences.getStringValue( JAVA_LIBRARY_PATH, null ) );
        }
    }

    protected Properties getSecurityProperties(String[] args)
    {
        String serverPath = System.getProperty("biouml.server.path");
        Properties result = loadSecurityPropertiesFromDir(new File(serverPath, "conf"));
        if(result != null)
            return result;
        for( String arg : args )
        {
            result = loadSecurityPropertiesFromDir(new File(arg));
            if(result != null)
                return result;
        }
        return null;
    }

    private Properties loadSecurityPropertiesFromDir(File dir)
    {
        try
        {
            File file = new File(dir, "security.properties");
            if( file.exists() )
            {
                Properties properties = new Properties();
                try( FileReader reader = new FileReader( file ) )
                {
                    properties.load( reader );
                }

                return properties;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "can not init security properties", e);
        }
        return null;
    }

    /*
     * We try to configure java.util.logging here.
     * tomcat uses it's own wrapper org.apache.juli, that can not be configured this way.
     * logging.properties file should be copied to WEB-INF/classes directory of biouml server application for org.apache.juli configuration
     */
    protected void cofigureJUL()
    {
        String serverPath = System.getProperty( "biouml.server.path" );
        File confDir = new File( serverPath, "conf" );
        File configFile = new File( confDir, "server.lcf" );

        if( !configFile.exists() )
        {
            configFile = new File( "server.lcf" );
        }

        try( InputStream inputStream = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( inputStream );
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE,  "Can not configure java.util.logging.Logger", e );
        }
        JULBeanLogger.install();
    }


    //Initialize logger shown in web interface. Should be called after preferences are loaded.
    protected void initWebLogger()
    {
        Preferences logPreferences = Application.getPreferences().getPreferencesValue( "WebLogConfig" );
        if( logPreferences != null )
        {
            if( logPreferences.getBooleanValue( "useWebLog", false ) )
                WebLogProvider.initWebLogger( logPreferences );
        }
    }

    @Override
    public void stop()
    {
    }

}
