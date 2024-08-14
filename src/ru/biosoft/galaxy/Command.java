package ru.biosoft.galaxy;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import one.util.streamex.EntryStream;

import org.apache.commons.io.FileUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.galaxy.ProcessSupport.SystemUser;
import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.ConditionalParameter;
import ru.biosoft.galaxy.parameters.ConfigParameter;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.tasks.process.LauncherFactory;
import ru.biosoft.tasks.process.ProcessLauncher;

/**
 * Tool command info and execution support
 */
@CodePrivilege ( CodePrivilegeType.LAUNCH )
public class Command
{
    private static final Logger log = Logger.getLogger( Command.class.getName() );

    private ProcessLauncher launcher;

    protected String interpreter;
    protected String command;
    protected String realCommandLine;
    protected File toolDir;
    protected File tmpPath;
    protected File workDir;
    private final GalaxyMethodInfo methodInfo;

    public Command(String interpreter, String command, File path, GalaxyMethodInfo methodInfo)
    {
        this.interpreter = interpreter;
        this.command = command;
        this.toolDir = path;
        this.methodInfo = methodInfo;
    }

    public String getInterpreter()
    {
        return interpreter;
    }

    public String getCommand()
    {
        return command;
    }

    public String getRealCommandLine()
    {
        return realCommandLine;
    }

    public File getToolDir()
    {
        return toolDir;
    }

    public synchronized File getTempPath()
    {
        if( tmpPath == null )
        {
            initLauncher();
            tmpPath = new File(launcher.getSharedFolder(), "galaxy_command_tmp");
            tmpPath.mkdirs();
        }
        return tmpPath;
    }

    public synchronized File getWorkDir()
    {
        if( workDir == null )
        {
            initLauncher();
            workDir = new File(launcher.getSharedFolder(), "galaxy_command_work_dir");
            workDir.mkdirs();
        }
        return workDir;
    }

    private synchronized void initLauncher()
    {
        if( launcher == null )
        {
            String launcherName = GalaxyDataCollection.getLauncherName();
            if( launcherName == null )
            {
                launcher = LauncherFactory.getDefaultLauncher();

            }
            else
                try
                {
                    launcher = LauncherFactory.getLauncher(launcherName);
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException(e);
                }
        }
    }

    /**
     * Execute current command with specified parameters
     * @param log
     * @param fjc
     */
    public boolean execute(ParametersContainer parameters, SystemUser systemUser, final Logger log, FunctionJobControl fjc)
            throws Exception
    {
        setMetadata(parameters);

        int i = 0;
        for( Parameter p : parameters.values() )
        {
            if( p instanceof ConfigParameter )
            {
                ( (ConfigParameter)p ).processTemplate(parameters, toolDir);
                File tmpConfigPath = new File(getTempPath(), "config_" + ( ++i ));
                ( (ConfigParameter)p ).setConfigFile(tmpConfigPath);
            }
        }

        String cmd = GalaxyFactory.fillTemplate(command, parameters, toolDir).replaceAll("[\n\r]", " ").trim();
        String[] cmdParts = cmd.split("\\s+");
        String scriptName = cmdParts[0];
        if( ( scriptName.equals("python") || scriptName.equals("bash") ) && cmdParts.length > 1 )
            scriptName = cmdParts[1];
        File wrapperScript = new File(toolDir, scriptName);
        if( wrapperScript.exists() )
        {
            int pos = cmd.indexOf(scriptName);
            if( pos >= 0 )
            {
                cmd = cmd.substring(0, pos) + wrapperScript.getAbsolutePath() + cmd.substring(pos + scriptName.length());
            }
        }
        cmd = interpreter + " " + cmd;

        String depConfigCmd = getDependencyConfigCmd();
        if(!depConfigCmd.isEmpty())
            cmd = depConfigCmd + cmd;

        log.log(Level.FINE, cmd);
        //log.log( Level.INFO, "Command line = " + cmd );
        realCommandLine = cmd;

        initLauncher();
        launcher.setCommand(cmd);
        if(needsGalaxyLibs())
            GalaxyFactory.setupPythonEnvironment( launcher );
        launcher.setDirectory(getWorkDir());
        launcher.setEnvironment(new LogScriptEnvironment(log));
        launcher.setJobControl(fjc);
        int exitValue = launcher.executeAndWait();

        postProcess( parameters );

        //setMetadata(parameters, true);

        return exitValue == 0;
    }
    
    private boolean needsGalaxyLibs()
    {
        for(Requirement req : methodInfo.getRequirements())
            if(req.getType().equals( Requirement.Type.GALAXY_LIB ))
                return true;
        return false;
    }

    private String getDependencyConfigCmd()
    {
        File toolDependecyDir = GalaxyDataCollection.getGalaxyToolDependencyDir();
        if(toolDependecyDir == null)
            return "";
        StringBuilder cmd = new StringBuilder();
        for(Requirement req : methodInfo.getRequirements())
            if(req.getType().equals( Requirement.Type.PACKAGE ))
            {
                File toolDir = new File(toolDependecyDir, req.getName());
                if(!toolDir.exists())
                {
                    log.warning( "Missing dependency " + req );
                    continue;
                }
                File packageBase = new File(toolDir, req.getVersion());
                if(req.getVersion().isEmpty() || !packageBase.exists())
                    packageBase = new File(toolDir, "default");
                if(!packageBase.exists())
                    continue;

                ToolShedElement toolShedElement = methodInfo.getToolShedElement();
                if( toolShedElement != null )
                {
                    File shedPackageBase = new File( packageBase, toolShedElement.getRepositoryOwner() );
                    shedPackageBase = new File( shedPackageBase, toolShedElement.getRepositoryName() );
                    shedPackageBase = new File( shedPackageBase, toolShedElement.getRevision() );
                    if( shedPackageBase.exists() )
                        packageBase = shedPackageBase;
                }

                cmd.append( "PACKAGE_BASE=\"" + packageBase + "\";export PACKAGE_BASE;" );
                File envFile = new File(packageBase, "env.sh");
                if(envFile.exists())
                    cmd.append( "source \"" + envFile.getAbsolutePath() + "\";");
                else
                {
                    File binFolder = new File(packageBase, "bin");
                    File path = binFolder.exists() ? binFolder : packageBase;
                    cmd.append( "PATH=\"" + path.getAbsolutePath() + ":$PATH\"; export PATH;"  );
                }
            }

        return cmd.toString();
    }

    private void postProcess(ParametersContainer parameters) throws IOException
    {
        for( Parameter p : parameters.values() )
            if( p.isOutput() && p instanceof FileParameter && p.getAttributes().containsKey( MethodInfoParser.FROM_WORK_DIR_ATTR ) )
            {
                File src = new File(getWorkDir(), (String)p.getAttributes().get(MethodInfoParser.FROM_WORK_DIR_ATTR));
                File dst = ( (FileParameter)p ).getFile();
                if(src.exists() && dst != null)
                    FileUtils.moveFile(src, dst);
            }
    }

    public static void setMetadata(Map<String, Parameter> parameters) throws Exception
    {
        for( Parameter p : parameters.values() )
            if( p instanceof FileParameter )
            {
                if( p.toString().isEmpty() )//value not set
                    continue;
                Map<String, MetaParameter> meta = null;
                if( p.isOutput() )
                {
                    Actions actions = (Actions)p.getAttributes().get( MethodInfoParser.ACTIONS_ATTR );
                    if(actions != null)
                        actions.apply( (FileParameter)p, (ParametersContainer)parameters );
                }
                else
                {
                    meta = GalaxyFactory.getMetadata( (FileParameter)p );
                }
                if( meta == null )
                    continue;
                EntryStream.of( meta ).removeKeys( p.getMetadata()::containsKey )
                    .mapValues( MetaParameter::clone ).forKeyValue( p.getMetadata()::put );
                //dbkey should be accessible in parameter fields
                if( p.getMetadata().containsKey("dbkey") )
                    p.getParameterFields().put("dbkey", p.getMetadata().get("dbkey").getValue().toString());

            }
            else if( p instanceof ArrayParameter )
            {
                ArrayParameter arrayParameter = (ArrayParameter)p;
                for( Map<String, Parameter> childs : arrayParameter.getValues() )
                    setMetadata(childs);
            }
            else if( p instanceof ConditionalParameter )
            {
                String keyValue = ( (ConditionalParameter)p ).getKeyParameter().toString();
                Map<String, Parameter> childs = ( (ConditionalParameter)p ).getWhenParameters(keyValue);
                setMetadata(childs);
            }
    }

    public void interrupt()
    {
        if( launcher != null )
            launcher.terminate();
    }

}