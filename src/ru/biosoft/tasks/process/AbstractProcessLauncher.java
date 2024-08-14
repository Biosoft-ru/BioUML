package ru.biosoft.tasks.process;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.NullScriptEnvironment;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.util.TempFileManager;

import com.developmentontheedge.beans.Preferences;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * @author lan
 *
 */
public abstract class AbstractProcessLauncher implements ProcessLauncher
{
    public static final String SHARED_FOLDER = "sharedFolder";
    public static final String REMOTE_FOLDER = "remoteFolder";
    protected String command;
    protected File directory;
    protected ScriptEnvironment env = new NullScriptEnvironment();
    protected Map<String, String> environ = new HashMap<>();
    protected FunctionJobControl fjc;
    protected MachineResources resources = defaultResources();
    protected volatile File sharedFolder;
    protected String sharedFolderStr;
    protected String remoteFolderStr;
    protected volatile boolean running;

    protected File []inputFiles;
    protected File []outputFiles;

    protected AbstractProcessLauncher()
    {
        this.sharedFolderStr = null;
    }

    protected AbstractProcessLauncher(Preferences config)
    {
        this.sharedFolderStr = config.getStringValue(SHARED_FOLDER, null);
        this.remoteFolderStr = config.getStringValue(REMOTE_FOLDER, null);
    }

    @Override
    public void setCommand(String command)
    {
        this.command = command;
    }

    @Override
    public void setDirectory(File directory)
    {
        this.directory = directory;
    }

    @Override
    public void setInputFiles(File ... inputFiles)
    {
        this.inputFiles = inputFiles;
    }

    @Override
    public File[] getOutputFiles()
    {
        return outputFiles;
    }

    @Override
    public void setOutputFiles(File ... outputFiles)
    {
        this.outputFiles = outputFiles;
    }

    @Override
    public void setEnvironment(ScriptEnvironment env)
    {
        this.env = env;
    }

    @Override
    public void addEnv(String var, String value)
    {
        this.environ.put(var, value);
    }

    @Override
    public void setJobControl(FunctionJobControl fjc)
    {
        this.fjc = fjc;
    }

    @Override
    public void setResources(MachineResources resources)
    {
        this.resources = resources;
    }

    private MachineResources defaultResources()
    {
        return new MachineResources(1, 512 * 1024 * 1024, 1024 * 1024 * 1024);
    }

    @Override
    public File getSharedFolder()
    {
        if(sharedFolder == null)
        {
            synchronized(this)
            {
                if(sharedFolder == null)
                {
                    try
                    {
                        TempFileManager tempFileManager = sharedFolderStr == null ? TempFileManager.getDefaultManager() : TempFileManager
                                .getManager(new File(sharedFolderStr));
                        sharedFolder = tempFileManager.dir("shared");
                    }
                    catch( IOException e )
                    {
                        throw ExceptionRegistry.translateException(e);
                    }
                }
            }
        }
        return sharedFolder;
    }

    @Override
    public boolean isRunning()
    {
        return running;
    }

    @Override
    public int executeAndWait() throws LoggedException, InterruptedException
    {
        execute();
        return waitFor();
    }

    public static String subst( String text, String fromText, String toText, String defText )
    {
        if( text == null )
        {
            return text;
        }
        int prevPos = 0;
        String newText = toText == null || "".equals( toText ) ? defText : toText;
        for( int pos = text.indexOf( fromText, prevPos ); pos >= 0;
             pos = text.indexOf( fromText, prevPos + newText.length() ) )
        {
            prevPos = pos;
            text = new StringBuffer( text ).replace( pos, pos + fromText.length(), newText ).toString();
        }
        return text;
    }
}
