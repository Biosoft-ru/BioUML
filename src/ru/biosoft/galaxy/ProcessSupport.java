package ru.biosoft.galaxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Logger;

/**
 * Contains utility methods to get process object with dependency on operating system and users.
 */
public class ProcessSupport
{
    protected static final Logger log = Logger.getLogger(ProcessSupport.class.getName());

    protected static final String USER_PREFIX = "galaxy_";
    protected static final AtomicInteger currentUserId = new AtomicInteger(1);

    protected static final String ADD_USER_SCRIPT = "create_galaxy_user.sh";
    protected static final String DEL_USER_SCRIPT = "remove_galaxy_user.sh";
    protected static final String RUN_SCRIPT = "run_galaxy.sh";

    /**
     * Get available system user for running galaxy process
     */
    public synchronized static SystemUser getSystemUser()
    {
        //Create new system user
        File scriptsPath = GalaxyDataCollection.getSystemScriptsPath();
        if( scriptsPath != null && scriptsPath.exists() )
        {
            SystemUser newUser = new SystemUser(currentUserId.getAndIncrement());
            if( executeScript(new File(scriptsPath, ADD_USER_SCRIPT),
                    new String[] {Integer.toString(newUser.getId()), newUser.getUsername()}) )
            {
                return newUser;
            }
        }
        return null;
    }
    /**
     * Return user to store and mark as available for next running
     */
    public synchronized static void releaseSystemUser(SystemUser systemUser)
    {
        if( systemUser != null )
        {
            File scriptsPath = GalaxyDataCollection.getSystemScriptsPath();
            if( scriptsPath != null && scriptsPath.exists() )
            {
                executeScript(new File(scriptsPath, DEL_USER_SCRIPT),
                        new String[] {Integer.toString(systemUser.getId()), systemUser.getUsername()});
            }
        }
    }

    protected static boolean executeScript(File scriptFile, String[] params)
    {
        boolean result = false;
        if( scriptFile.exists() )
        {
            try
            {
                List<String> paramsList = new ArrayList<>();
                paramsList.add("sudo");
                paramsList.add(scriptFile.getAbsolutePath());
                for( String param : params )
                    paramsList.add(param);

                ProcessBuilder processBuilder = new ProcessBuilder(paramsList);
                Process proc = processBuilder.start();

                try (BufferedReader inputReader = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
                        BufferedReader errorReader = new BufferedReader( new InputStreamReader( proc.getErrorStream() ) ))
                {
                    proc.waitFor();

                    StringBuffer errors = new StringBuffer();
                    String line;
                    while( ( line = errorReader.readLine() ) != null )
                    {
                        errors.append( line );
                        errors.append( '\n' );
                    }

                    if( errors.length() == 0 )
                    {
                        result = true;
                    }
                    else
                    {
                        log.warning( errors.toString() );
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot execute script", e);
            }
        }
        return result;
    }

    public static ProcessBuilder getProcessBuilder(String cmd, SystemUser user)
    {
        ProcessBuilder processBuilder;
        if( System.getProperty("os.name").startsWith("Windows") )
        {
            processBuilder = new ProcessBuilder("cmd", "/c", cmd);
            log.warning("Running with current tomcat user. Other users are not supported for Windows server.");
        }
        else
        {
            if( user == null )
            {
                // Use glaunch.sh script which helps to properly kill all the children processes
                processBuilder = new ProcessBuilder(new File(GalaxyFactory.getScriptPath(), "glaunch.sh").toString(), cmd);
                log.warning("Running with current tomcat user. No additional users available for current server");
            }
            else
            {
                File scriptsPath = GalaxyDataCollection.getSystemScriptsPath();
                String PATH = System.getenv().get("PATH");
                String PYTHONPATH = GalaxyDataCollection.getGalaxyDistFiles().getLibFolder().getAbsolutePath();
                if( System.getenv().containsKey("PYTHONPATH") )
                    PYTHONPATH = System.getenv().get("PYTHONPATH") + File.pathSeparator + PYTHONPATH;
                processBuilder = new ProcessBuilder("sudo", "env", "PYTHONPATH=" + PYTHONPATH, "PATH=" + PATH, new File(scriptsPath,
                        RUN_SCRIPT).getAbsolutePath(), Integer.toString(user.getId()), cmd);
            }
        }
        return processBuilder;
    }

    /**
     * System user properties
     */
    protected static class SystemUser
    {
        protected int id;

        public SystemUser(int id)
        {
            this.id = id;
        }

        public String getUsername()
        {
            return USER_PREFIX + id;
        }

        public int getId()
        {
            return id;
        }
    }
}
