package ru.biosoft.tasks.process;

import java.io.File;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.script.ScriptEnvironment;

/**
 * @author lan
 *
 */
public interface ProcessLauncher
{
    public void setCommand(String command);
    public void setDirectory(File directory);
    public void setInputFiles(File ... inputFiles);
    public File[] getOutputFiles();
    public void setOutputFiles(File ... outputFiles);
    public void setEnvironment(ScriptEnvironment env);
    public void setJobControl(FunctionJobControl fjc);
    public void addEnv(String var, String value);
    public void setResources(MachineResources resources);
    
    public File getSharedFolder();

    public void execute() throws LoggedException;
    public boolean isRunning();
    public int waitFor() throws LoggedException, InterruptedException;
    
    public int executeAndWait() throws LoggedException, InterruptedException;
    public void terminate();
}
