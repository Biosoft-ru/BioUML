package biouml.plugins.node;

import java.io.File;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.tasks.process.LauncherFactory;
import ru.biosoft.tasks.process.MachineResources;
import ru.biosoft.tasks.process.ProcessLauncher;
import ru.biosoft.util.TempFileManager;

import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * @author lan
 */
@CodePrivilege({CodePrivilegeType.LAUNCH, CodePrivilegeType.SHARED_FOLDER_ACCESS})
public class NodeLauncher
{
    @CodePrivilege(CodePrivilegeType.SHARED_FOLDER_ACCESS)
    public static class ProcessMonitor
    {
        private ProcessLauncher launcher;
        private File file;

        protected ProcessMonitor(ProcessLauncher launcher, File file)
        {
            this.launcher = launcher;
            this.file = file;
        }

        public void join() throws LoggedException, InterruptedException
        {
            try
            {
                launcher.waitFor();
            }
            finally
            {
                if(file != null)
                    file.delete();
            }
        }
    }

    public static void runScript(DataElementPath scriptPath, ScriptEnvironment env, FunctionJobControl fjc, MachineResources resources)
    {
        runScript(scriptPath.getDataElement(JSElement.class).getContent(), env, fjc, resources);
    }

    public static void runScript(String scriptContent, ScriptEnvironment env, FunctionJobControl fjc, MachineResources resources)
    {
        try
        {
            runScriptBackground(scriptContent, env, fjc, resources).join();
        }
        catch( InterruptedException e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    public static ProcessMonitor runScriptBackground(String scriptContent, ScriptEnvironment env, FunctionJobControl fjc, MachineResources resources)
    {
        File file = null;
        try
        {
            NodeConfig config = NodeConfig.getInstance();
            ProcessLauncher launcher = LauncherFactory.getLauncher( config.getLauncherId() );
            if( launcher == null )
                throw new Exception( "BioUML node is not configured: Launcher is invalid." );
            File nodePath = new File( config.getBioumlServerPath() );
            file = TempFileManager.getManager( launcher.getSharedFolder() ).file( ".js", scriptContent );
            String sessionId = SecurityManager.createNodeSessionId();
            launcher.setDirectory( nodePath );
            launcher.setCommand( config.getJavaPath() + " " + config.getJavaOpts()
                    + " -jar plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -application biouml.plugins.node.launcher \""
                    + config.getServerLink() + "\" \"" + sessionId + "\" <" + file.getAbsolutePath() );
            launcher.setEnvironment( env );
            launcher.setJobControl( fjc );
            if( resources != null )
                launcher.setResources( resources );
            launcher.execute();
            return new ProcessMonitor( launcher, file );
        }
        catch( Exception e )
        {
            if(file != null)
                file.delete();
            throw ExceptionRegistry.translateException(e);
        }
    }
}
