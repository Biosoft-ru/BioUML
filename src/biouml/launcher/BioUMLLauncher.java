package biouml.launcher;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.util.Arrays;

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
            final String[] arg = new String[5 + rootFolders.length];
            int i = 0;
            arg[i++] = "-install";
            arg[i++] = serverPath;
            arg[i++] = "-application";
            arg[i++] = getApplicationName();
            arg[i++] = "-noExit";
            for( String rootFolder : rootFolders )
            {
                arg[i++] = rootFolder;
            }

            log.info( "BioUMLLauncher: " + Arrays.asList( arg ) );

            ( new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Main.main(arg);
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
