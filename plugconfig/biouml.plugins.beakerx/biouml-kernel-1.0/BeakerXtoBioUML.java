package biouml.plugins.beakerx;

import biouml.plugins.server.access.ClientDataCollection;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.util.logging.LogManager;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.StreamEx;
		
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

import ru.biosoft.access.ImageElement;
import ru.biosoft.access.CollectionFactory;
import ru.biosoft.access.DataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.View.ModelResolver;
import ru.biosoft.graphics.access.DataElementModelResolver;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.plugins.javascript.JScriptShellEnvironment;
import ru.biosoft.plugins.javascript.JScriptContext;

import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.tomcat.TomcatConnection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.NetworkConfigurator;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.UserPermissions;

import static com.twosigma.beakerx.DefaultJVMVariables.IMPORTS;
import static com.twosigma.beakerx.kernel.Utils.uuid;

import com.twosigma.beakerx.AutotranslationServiceImpl;
import com.twosigma.beakerx.BeakerXCommRepository;
import com.twosigma.beakerx.CommRepository;
import com.twosigma.beakerx.NamespaceClient;
import com.twosigma.beakerx.evaluator.Evaluator;
import com.twosigma.beakerx.handler.KernelHandler;
import com.twosigma.beakerx.kernel.CacheFolderFactory;
import com.twosigma.beakerx.kernel.CloseKernelAction;
import com.twosigma.beakerx.kernel.CustomMagicCommandsEmptyImpl;
import com.twosigma.beakerx.kernel.Kernel;
import com.twosigma.beakerx.kernel.KernelConfigurationFile;
import com.twosigma.beakerx.kernel.EvaluatorParameters;
import com.twosigma.beakerx.kernel.KernelRunner;
import com.twosigma.beakerx.kernel.KernelSocketsFactory;
import com.twosigma.beakerx.kernel.KernelSocketsFactoryImpl;
import com.twosigma.beakerx.kernel.handler.CommOpenHandler;
import com.twosigma.beakerx.message.Message;

public class BeakerXtoBioUML implements IApplication
{
    private static Logger log = Logger.getLogger( BeakerXtoBioUML.class.getName() );

    private static final ModelResolver viewModelResolver = new DataElementModelResolver();
    /** Top level function that starts the shell. */

    private String serverPath = "";

    @Override
    public Object start(IApplicationContext arg0)
    {
        try
        {
            configureJUL();
             
            if( new File( "/BioUML_Server/" ).exists() )
            {
                System.setProperty( "biouml.server.path", serverPath = "/BioUML_Server/" );
            }

            NetworkConfigurator.initNetworkConfiguration();
            loadPreferences();

            Object arg = arg0.getArguments().get( "application.args" );
            List<String> commandLineArgs = arg instanceof String[] ? 
                new ArrayList<>( Arrays.asList( ( String[] )arg ) ) :
                Collections.emptyList();

            if( commandLineArgs.size() < 1 )
            {
                System.err.println( "This application should not be launched directly" );
                return null;
            }

            String host = null;

            final java.util.HashMap<String,String> loginData = new java.util.HashMap<>();

            String userLoginFile = "/home/jovyan/work/.user.txt";  

            try
            { 
                try( java.util.stream.Stream<String> stream = Files.lines(Paths.get( userLoginFile ) ) ) 
                {
                    stream.forEach( line -> {
                        String[] parts = line.split( "\t" );
                        loginData.put( parts[ 0 ], parts[ 1 ] );
                    } );
                }

                host = loginData.get( "url" ) + "/biouml/";
                log.info( "BioUML server: " + host );
            }
            catch( Exception exc )
            {
                log.log( Level.SEVERE, userLoginFile, exc );
            }

            java.nio.file.Path currentRelativePath = java.nio.file.Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();

            log.info( "Working directory = '" + s + "'" );
            
            if( loginData.get( "url" ) != null && loginData.get( "user" ) != null && loginData.get( "pass" ) != null )
            {
                System.setProperty(TomcatConnection.CONNECTION_TIMEOUT_PROPERTY, String.valueOf(86_400_000)); // One day
                log.info( "Creating remote repository 'databases'..." );
                createRepository("databases", host, loginData.get( "user" ), loginData.get( "pass" ) );

                if( commandLineArgs.contains( "-local" ) )
                {
                    log.info( "Creating local repository 'data'..." );
                    CollectionFactory.createRepository( serverPath + "resources" );
                    commandLineArgs.remove( "-local" );
                }
                else
                {
                    log.info( "Creating remote repository 'data'..." );
                    createRepository("data", host, loginData.get( "user" ), loginData.get( "pass" ) );
                }

                log.info( "Creating local repository '" + serverPath + "analyses'..." );
                CollectionFactory.createRepository( serverPath + "analyses" );
            }
            else
            {
                log.severe( "Unable to find BioUML server login data" );
            } 

            Plugins.getPlugins();
            View.setModelResolver( viewModelResolver );

            System.out.println( "---BeakerX over BioUML Jupyter notebook started---" );
            log.info( "---BeakerX over BioUML Jupyter notebook started---" );
            log.info( "Main ClassLoader: " + this.getClass().getClassLoader() );

            KernelRunner.run(() -> {
                String id = uuid();
                KernelConfigurationFile configurationFile = 
                    new KernelConfigurationFile( commandLineArgs.toArray( new String[ 0 ] ) );
                KernelSocketsFactoryImpl kernelSocketsFactory = new KernelSocketsFactoryImpl( configurationFile );

                BeakerXCommRepository beakerXCommRepository = new BeakerXCommRepository();

                BeakerXtoBioUMLEvaluator evaluator = new BeakerXtoBioUMLEvaluator( 
                        id, id,
                        getEvaluatorParameters(),
                        NamespaceClient.create(id, configurationFile, beakerXCommRepository) );

                return new BeakerXKernel( id, evaluator, kernelSocketsFactory, beakerXCommRepository );
            });

        }
        catch( Throwable t )
        {
            System.err.println(ExceptionRegistry.log(t));
        }
        return IApplication.EXIT_OK;
    }

    private void loadPreferences()
    {
        Preferences preferences = new Preferences();
        String prefFile = new File( serverPath + "preferences.xml").exists() ? serverPath + "preferences.xml" : null;
        prefFile = new File(serverPath + "conf/preferences.xml").exists() ? serverPath + "conf/preferences.xml" : prefFile;
        if( prefFile != null )
        {
            log.info( "Got preferences from " + prefFile );    
            preferences.load( prefFile );
            Application.setPreferences( preferences );
        } 
        else
        {
            log.warning( "Preferences file not found" );
        }   
    }

    /**
     * @todo change to ClinetDataCollection
     */
    private void createRepository(String name, String host, String sessionId)
    {
        Properties properties = new ExProperties();
        properties.setProperty(DataCollection.NAME_PROPERTY, name);
        properties.setProperty(DataCollection.PLUGINS_PROPERTY, "biouml.plugins.server;ru.biosoft.server.tomcat");
        properties.setProperty(DataCollection.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
        properties.setProperty(DataCollection.IS_ROOT, String.valueOf(true));
        properties.setProperty(ClientConnection.URL_PROPERTY, host);
        properties.setProperty(ClientConnection.CONNECTION_TYPE, TomcatConnection.class.getName());
        //properties.setProperty(RemoteCollection.SHARED_SQL_PROPERTY, String.valueOf(true));
        //properties.setProperty(RemoteCollection.SESSION_PROPERTY, sessionId);
        //new RemoteCollection(null, properties);
    }

    private void createRepository(String name, String host, String user, String pass) throws Exception
    {
        Properties properties = new ExProperties();
        properties.setProperty(DataCollection.NAME_PROPERTY, name);
        properties.setProperty(DataCollection.PLUGINS_PROPERTY, "biouml.plugins.server;ru.biosoft.server.tomcat");
        properties.setProperty(DataCollection.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
        properties.setProperty(DataCollection.IS_ROOT, String.valueOf(true));

        //properties.setProperty(ClientConnection.URL_PROPERTY, host);
        properties.setProperty(ClientDataCollection.SERVER_URL, host);
        properties.setProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME, name);

        ClientDataCollection data = new ClientDataCollection(null, properties);
        data.login( user, pass );   

        //properties.setProperty(ClientConnection.CONNECTION_TYPE, TomcatConnection.class.getName());
        //properties.setProperty(RemoteCollection.SHARED_SQL_PROPERTY, String.valueOf(true));
        //properties.setProperty(RemoteCollection.USERNAME_PROPERTY, user);
        //properties.setProperty(RemoteCollection.PASSWORD_PROPERTY, pass);
        //new RemoteCollection(null, properties);
    }


    private static void configureJUL()
    {
        try( java.io.InputStream fis = Platform.getBundle( "biouml.plugins.beakerx" ).getEntry( "beakerx.lcf" ).openStream() )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( IOException e )
        {
            System.out.println( "Cannot configure java.util.logging.Logger: " + e.getMessage() );
            e.printStackTrace( System.out );
        }
    }

    @Override
    public void stop()
    {
    }

    public static EvaluatorParameters getEvaluatorParameters() 
    {
        HashMap<String, Object> kernelParameters = new HashMap<>();
        //kernelParameters.put( IMPORTS, new GroovyDefaultVariables().getImports() );
        return new EvaluatorParameters( kernelParameters );
    }
}
