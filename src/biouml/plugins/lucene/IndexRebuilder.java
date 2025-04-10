package biouml.plugins.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.LogManager;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import biouml.model.Module;
import biouml.workbench.ConsoleApplicationSupport;
import ru.biosoft.access.WildcardPathSet;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TextUtil2;

/**
 * All indexes rebuilder.
 */
public class IndexRebuilder extends ConsoleApplicationSupport
{
    @Override
    public Object start(IApplicationContext arg0)
    {
        try
        {
            File configFile = new File( "biouml.lcf" );
            try( FileInputStream fis = new FileInputStream( configFile ) )
            {
                LogManager.getLogManager().readConfiguration( fis );
            }
            catch( Exception e1 )
            {
                System.err.println( "Error init logging: " + e1.getMessage() );
            }

            Object arg = arg0.getArguments().get( "application.args" );
            String[] args = (String[])arg;

            log.info("***************** start ****************");
            if(args.length < 2)
            {
                System.err.println("Please supply arguments: properties file, preferences file");
                return null;
            }
            rebuildIndexes(args[0], args[1]);
            log.info("***************** end ****************");
        }
        catch( Throwable t )
        {
            System.err.println("Can not start Rebuild index application: " + t);
            t.printStackTrace();

            System.out.println("\nPress any key to continue");
            try
            {
                System.in.read();
            }
            catch( Throwable ignore )
            {
            }
        }

        return IApplication.EXIT_OK;
    }

    /**
     * Rebuild indexes
     */
    public void rebuildIndexes(String propFileName, String prefFileName)
    {
        try
        {
            String sessionID = "index_rebuild_session";
            Properties properties = new ExProperties(new File(propFileName));

            String[] repositoryPaths = TextUtil2.split( properties.getProperty( "repositories" ), ';');

            String repositoryPath = repositoryPaths[0];

            Properties securityProperties = getProperties(repositoryPath);

            GlobalDatabaseManager.initDatabaseManager(securityProperties);
            SecurityManager.initSecurityManager(securityProperties);

            SecurityManager.addThreadToSessionRecord(Thread.currentThread(), sessionID);
            loadPreferences(prefFileName);

            for(String path : repositoryPaths)
            {
                String name = CollectionFactory.createRepository(path).getName();
                log.info( "Repository created: "+path+" -> "+name );
            }

            String username = properties.getProperty("user");
            String password = properties.getProperty("pass");
            String indexFolderName = properties.getProperty("folder", LuceneUtils.INDEX_FOLDER_NAME);
            SecurityManager.commonLogin( username, password, null, null );
            log.info( "Login is successful: "+username );
            DataElementPathSet modules = new WildcardPathSet(properties.getProperty("modules"));
            for( DataElementPath modulePath : modules )
            {
                log.info("*** Processing collection " + modulePath);
                Module module = modulePath.getDataElement( Module.class );
                LuceneQuerySystem luceneFacade = LuceneUtils.getLuceneFacade(module);
                if( luceneFacade instanceof LuceneQuerySystemImpl )
                {
                    File dir = new File(module.getInfo().getProperties().getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, "." ),
                            indexFolderName);
                    log.info( "Lucene dir: "+dir );
                    ( (LuceneQuerySystemImpl)luceneFacade ).setLuceneDir(dir.getPath());
                }
                if( luceneFacade != null && !luceneFacade.getCollectionsNamesWithIndexes().isEmpty() )
                {
                    log.info("*** Creating indexes for " + module.getCompletePath());
                    FunctionJobControl jobControl = new FunctionJobControl(null);
                    jobControl.addListener(new JobControlListenerAdapter()
                    {
                        @Override
                        public void valueChanged(JobControlEvent event)
                        {
                            int preparedness = event.getPreparedness();
                            char[] arr = new char[50];
                            for(int i=0; i<arr.length; i++) arr[i] = i*100/arr.length<=preparedness ? '#' : ' ';
                            System.out.print("\r["+new String(arr)+"] "+preparedness+"%");
                        }

                        @Override
                        public void jobTerminated(JobControlEvent event)
                        {
                            System.out.println();
                        }
                    });
                    luceneFacade.createIndex(log, jobControl);
                }
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    protected Properties getProperties(String repositoryPath) throws IOException
    {
        File file = new File(repositoryPath + "/security.properties");
        return new ExProperties(file);
    }
}
