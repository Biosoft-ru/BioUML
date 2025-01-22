package biouml.launcher;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;
import org.eclipse.equinox.launcher.Main;

import ru.biosoft.server.tomcat.ConnectionServlet;

@SuppressWarnings ( "serial" )
public class BioUMLLauncher extends ConnectionServlet
{
    public BioUMLLauncher()
    {
    }

    protected String getApplicationName()
    {
        return "ru.biosoft.server.tomcat.empty";
    }

    /**
     * Get array of paths to root data collections folders
     * @param serverPath path to server repository
     * @return
     */
    protected String[] getRootDataFolders(String serverPath)
    {
        return new String[] {serverPath + "/repo", serverPath + "/resources", serverPath + "/analyses", serverPath + "/users",  serverPath + "/history"};
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        try
        {
            String serverPath = config.getInitParameter("path");
            System.setProperty("biouml.server.path", serverPath);

            initLogging();

            String[] rootFolders = getRootDataFolders(serverPath);
            ArrayList<String> args = new ArrayList<>();
            args.add( "-install" );
            args.add( serverPath );
            args.add( "-application" );
            args.add( getApplicationName() );
            args.add( "-noExit" );
            for( String rootFolder : rootFolders )
            {
                if( new java.io.File( rootFolder ).exists() )
                {
                    args.add( rootFolder );
                }
                else
                {
                    log.info( "" + rootFolder + " is missing. Skipping. " );
                }  
            }

            log.info( "BioUMLLauncher: " + args );

            ( new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Main.main( args.toArray( new String[ 0 ] ) );
                    }
                    catch( Throwable t )
                    {
                        t.printStackTrace();
                    }
                }
            } ).start();
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
        try
        {
            while( System.getProperties().get("BioUML.Server.ServiceRegistry") == null )
                Thread.sleep(1000);
            while( System.getProperties().get("BioUML.Server.Request") == null )
                Thread.sleep(1000);
        }
        catch( InterruptedException e )
        {
            e.printStackTrace();
            throw new ServletException(e);
        }

        services = (Class<?>)System.getProperties().get("BioUML.Server.ServiceRegistry");
        System.getProperties().remove("BioUML.Server.ServiceRegistry");
        request = (Class<?>)System.getProperties().get("BioUML.Server.Request");
        System.getProperties().remove("BioUML.Server.Request");
        postInit();
    }

    private void initLogging()
    {
        //String serverPath = System.getProperty("biouml.server.path");
        //File configFile = new File("server.lcf");
        //if(!configFile.exists())
        //{
        //    File confDir = new File(serverPath, "conf");
        //    configFile = new File(confDir, "server.lcf");
        //}
        //PropertyConfigurator.configure(configFile.getAbsolutePath());
        log = Logger.getLogger("statistic");
    }

    @Override
    public void destroy()
    {
        System.getProperties().put("BioUML.Server.Exit", "true");
        super.destroy();
    }
}
