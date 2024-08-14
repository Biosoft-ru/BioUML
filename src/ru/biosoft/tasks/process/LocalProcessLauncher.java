package ru.biosoft.tasks.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.util.ApplicationUtils;

import com.developmentontheedge.beans.Preferences;

/**
 * @author lan
 *
 */
@CodePrivilege ( { CodePrivilegeType.THREAD, CodePrivilegeType.TEMP_RESOURCES_ACCESS } )
public class LocalProcessLauncher extends AbstractProcessLauncher
{
    private static final Logger log = Logger.getLogger( LocalProcessLauncher.class.getName() );

    protected Process proc;
    private BufferedReader inputReader;
    private BufferedReader errorReader;
    private Thread inputReaderThread;
    private Thread errorReaderThread;

    private final class InputReaderThread extends Thread
    {
        private final BufferedReader inputReader;
        private final ScriptEnvironment env;
        /**
         * @param name
         * @param inputReader
         * @param env
         */
        private InputReaderThread(String name, BufferedReader inputReader, ScriptEnvironment env)
        {
            super(name);
            this.inputReader = inputReader;
            this.env = env;
        }
        @Override
        public void run()
        {
            while( isRunning() )
            {
                try
                {
                    String line = inputReader.readLine();
                    if(line == null)
                        break;
                    if( !updateProgress(line) )
                        env.print(line);
                }
                catch( IOException e )
                {
                    break;
                }
            }
            try
            {
                while( inputReader.ready() )
                {
                    String line = inputReader.readLine();
                    if( line == null )
                        break;
                    if( !updateProgress(line) )
                        env.print(line);
                }
            }
            catch( IOException e )
            {
            }
            finally
            {
                onFinish();
            }
        }

        private boolean updateProgress(String line)
        {
            if( !line.startsWith("$Percent$ = ") )
                return false;
            int percent;
            try
            {
                percent = Integer.parseInt(line.substring("$Percent$ = ".length()).trim());
            }
            catch( NumberFormatException e )
            {
                return false;
            }
            if( fjc != null )
                fjc.setPreparedness(percent);
            return true;
        }
    }

    private final class ErrorReaderThread extends Thread
    {
        private final BufferedReader errorReader;
        private final ScriptEnvironment env;
        /**
         * @param name
         * @param errorReader
         * @param env
         * @param errors
         */
        private ErrorReaderThread(String name, BufferedReader errorReader, ScriptEnvironment env)
        {
            super(name);
            this.errorReader = errorReader;
            this.env = env;
        }
        @Override
        public void run()
        {
            while( isRunning() )
            {
                try
                {
                    String line = errorReader.readLine();
                    if(line == null)
                        break;
                    env.error(line);
                }
                catch( IOException e )
                {
                    break;
                }
            }
            try
            {
                while( errorReader.ready() )
                {
                    String line = errorReader.readLine();
                    if( line == null )
                        break;
                    env.error(line);
                }
            }
            catch( IOException e )
            {
            }
        }
    }

    public LocalProcessLauncher()
    {
    }

    public LocalProcessLauncher(Preferences prefs)
    {
        super(prefs);
    }

    protected ProcessBuilder getProcessBuilder() throws IOException
    {
        ProcessBuilder processBuilder;

        if( inputFiles != null && inputFiles.length > 0 )
        {
            for( int i = 0; i < inputFiles.length; i++ )
            {
                command = subst( command, "${inputFile" + i + "}", inputFiles[ i ].getCanonicalPath(), "" );
            }
        }

        if( System.getProperty("os.name").startsWith("Windows") )
        {
            processBuilder = new ProcessBuilder("cmd", "/c", command);
        }
        else
        {
            File launcher;
            try
            {
                launcher = ApplicationUtils.resolvePluginPath( "ru.biosoft.workbench:glaunch.sh" ).extract();
            }
            catch( IOException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            processBuilder = new ProcessBuilder(launcher.getAbsolutePath(),
                    command);
        }

        log.info( "Got command = " + command ); 

        if( directory != null )
            processBuilder.directory(directory);
        processBuilder.environment().putAll(environ);
        return processBuilder;
    }

    @Override
    public void terminate()
    {
        if( proc != null )
        {
            proc.destroy();
            running = false;
        }
    }

    @Override
    public void execute() throws LoggedException
    {
        if( fjc != null )
        {
            fjc.functionStarted();
        }
        try
        {
            ProcessBuilder processBuilder = getProcessBuilder();
            proc = processBuilder.start();
            running = true;
            InputStream inputStream = proc.getInputStream();
            InputStream errorStream = proc.getErrorStream();
            inputReader = new BufferedReader(new InputStreamReader(inputStream));
            errorReader = new BufferedReader(new InputStreamReader(errorStream));
            inputReaderThread = new InputReaderThread("inputReader", inputReader, env);
            errorReaderThread = new ErrorReaderThread("errorReader", errorReader, env);
            inputReaderThread.start();
            errorReaderThread.start();
        }
        catch( Exception e )
        {
            if( fjc != null )
                fjc.functionTerminatedByError(e);
            throw ExceptionRegistry.translateException(e);
        }
    }
    
    protected boolean isLocal()
    {
        return true;
    }

    @Override
    public int waitFor() throws LoggedException, InterruptedException
    {
        if( !isRunning() )
            return -1;
        int code;
        
        boolean local = isLocal();
        if(!local)
            TaskPool.getInstance().markCurrentThreadInactive();
        try
        {
            code = proc.waitFor();
            running = false;
            inputReaderThread.join();
            errorReaderThread.join();
        }
        finally
        {
            if(!local)
                TaskPool.getInstance().markCurrentThreadActive();
        }
        
        try
        {
            inputReader.close();
        }
        catch( IOException ex )
        {
        }
        try
        {
            errorReader.close();
        }
        catch( IOException ex )
        {
        }
        if( fjc != null )
            fjc.functionFinished();
        return code;
    }

    protected void onFinish()
    {

    }
}
