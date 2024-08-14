package ru.biosoft.tasks.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.Preferences;

/**
 * Launch process remotely with ssh
 * @author ivan
 *
 */
public class SSHProcessLauncher extends LocalProcessLauncher
{
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PRIVATE_KEY_FILE = "private-key-file";
    public static final String EXTRA_SSH_OPTIONS = "extra-ssh-options";
    
    protected String remoteHost;
    protected String user = "biouml";
    protected File privateKeyFile;
    protected List<String> extraSSHOptions;
    
    public SSHProcessLauncher(String host, String user, File privateKeyFile)
    {
        remoteHost = host;
        this.user = user;
        this.privateKeyFile = privateKeyFile;
    }
    
    public SSHProcessLauncher(Preferences config)
    {
        super(config);
        remoteHost = config.getStringValue(HOST, null);
        user = config.getStringValue(USER, "biouml");
        String extraSSHOptionsString = config.getStringValue(EXTRA_SSH_OPTIONS, null);
        if( extraSSHOptionsString != null )
        {
            extraSSHOptions = Arrays.asList( extraSSHOptionsString.split( " " ) );
        }
        String filePath = config.getStringValue(PRIVATE_KEY_FILE, null);
        if( filePath != null )
        { 
            privateKeyFile = new File(filePath);
        } 
    }
    
    protected SSHProcessLauncher() { }

    @Override
    protected ProcessBuilder getProcessBuilder()
    {
        StringBuilder script = new StringBuilder();
        script.append("cd '" + directory + "';\n");
        for(Map.Entry<String, String> var : environ.entrySet())
            script.append(var.getKey() + "='" + var.getValue() + "' ");
        script.append(command);
        
        List<String> cmd = new ArrayList<>();
        cmd.add("ssh");
        
        //We want to connect to different hosts with the same key, so disable host identity checking
        cmd.add("-o"); cmd.add("UserKnownHostsFile=/dev/null");
        cmd.add("-o"); cmd.add("StrictHostKeyChecking=no");
        cmd.add("-o"); cmd.add( "LogLevel=quiet" );
        
        if(privateKeyFile != null)
        {
            cmd.add("-i");
            cmd.add(privateKeyFile.getAbsolutePath());
        }
         
        if( extraSSHOptions != null )
        {
            cmd.addAll( extraSSHOptions );
        }
        
        String url = remoteHost;
        if(user != null)
            url = user + "@" + remoteHost;
        cmd.add(url);
        
        cmd.add(script.toString());
        
        return new ProcessBuilder(cmd);
    }
    
    @Override
    protected boolean isLocal()
    {
        return remoteHost.equals( "localhost" ) || remoteHost.equals( "127.0.0.1" );
    }
}
