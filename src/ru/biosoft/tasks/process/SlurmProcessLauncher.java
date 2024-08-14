package ru.biosoft.tasks.process;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.task.TaskPool;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.tasks.process.SlurmFileWatcher.NewDataAction;

public class SlurmProcessLauncher extends AbstractProcessLauncher
{
    private String remoteHost;//not null if sbatch should be run on remote host using ssh
    private String user = "biouml";
    private String port = "22";
    private File privateKeyFile;
    protected List<String> extraSSHOptions;
    
    private static final Logger log = Logger.getLogger( SlurmProcessLauncher.class.getName() );
    public static final String JOB_ID_LINE = "Submitted batch job ";
    
    private long jobId = -1;

    private String remoteUUID;  
    private String remoteFolder;  
       
    public SlurmProcessLauncher(Preferences prefs)
    {
        super(prefs);
        remoteHost = prefs.getStringValue(SSHProcessLauncher.HOST, null);
        port = prefs.getStringValue(SSHProcessLauncher.PORT, "22");
        user = prefs.getStringValue(SSHProcessLauncher.USER, "biouml");
        String extraSSHOptionsString = prefs.getStringValue(SSHProcessLauncher.EXTRA_SSH_OPTIONS, null);
        if( extraSSHOptionsString != null )
        {
            extraSSHOptions = Arrays.asList( extraSSHOptionsString.split( " " ) );
        }
        String filePath = prefs.getStringValue(SSHProcessLauncher.PRIVATE_KEY_FILE, null);
        if( filePath != null )
        { 
            privateKeyFile = new File(filePath);
        }  
 
    }

    protected String readProcessOutput( Process proc, boolean bInputOnly ) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String full = "";
        String line;
        while((line = reader.readLine()) != null)
        { 
            if( !"".equals( full ) )
            {
                full += "\n";
            }
            full += line;
        }
 
        if( bInputOnly )
        {
            return full;
        }

        reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        while((line = reader.readLine()) != null)
        { 
            if( !"".equals( full ) )
            {
                full += "\n";
            }
            full += line;
        }
        return full; 
    }

    @Override
    public void execute() throws LoggedException
    {
        remoteUUID = null;
        remoteFolder = null; 
        try
        {
            if( remoteFolderStr != null && inputFiles != null && inputFiles.length > 0 )
            {
                remoteUUID = java.util.UUID.randomUUID().toString();
                remoteFolder = remoteFolderStr + "/" + remoteUUID; 

                List<String> mkdir = getCommandPrefix( "ssh" );
                mkdir.add( "mkdir" );
                mkdir.add( remoteFolder );
                ProcessBuilder mkdirProcessBuilder = new ProcessBuilder( mkdir );
                Process mkdirProc = mkdirProcessBuilder.start();
                String mkdirOutput = readProcessOutput( mkdirProc, false );
                int exitCode = mkdirProc.waitFor();
                if( exitCode != 0 )
                {
                    env.error( "" + mkdir );
                    throw new RuntimeException("" + mkdirOutput );
                } 

                List<String> scp = getCommandPrefix( "scp" );
                String host = scp.get( scp.size() - 1 );
                scp.remove( scp.size() - 1 );
                scp.add( "-C" );  
                for( int i = 0; i < inputFiles.length; i++ )
                {
                    scp.add( inputFiles[ i ].getCanonicalPath() );
                    command = subst( command, "${inputFile" + i + "}", remoteFolder + "/" + inputFiles[ i ].getName(), "" );
                }
                scp.add( host + ":" + remoteFolder );
                env.print( "Copying input files to remote folder '" + remoteFolder + "'..." );
                ProcessBuilder scpPocessBuilder = new ProcessBuilder( scp );                
                Process scpProc = scpPocessBuilder.start();
                String scpOutput = readProcessOutput( scpProc, false );

                exitCode = scpProc.waitFor();
                if( exitCode != 0 )
                {
                    env.error( "" + scp  );
                    throw new RuntimeException("Cannot copy files to remote folder " + scpOutput );
                } 
            }
            else if( inputFiles != null && inputFiles.length > 0 ) // just hack if no remote folder
            {
                for( int i = 0; i < inputFiles.length; i++ )
                {
                    command = subst( command, "${inputFile" + i + "}", inputFiles[ i ].getCanonicalPath(), "" );
                }
            }

            StringBuilder script = buildScriptFile();
            List<String> cmd = getCommandPrefix( "ssh" );
            cmd.add( "sbatch" );

            log.info( "Executing " + cmd + "\n" + script );

            ProcessBuilder processBuilder = new ProcessBuilder( cmd );
            
            Process proc = processBuilder.start();

            BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( proc.getOutputStream() ) );

            writer.write( script.toString() );
            writer.flush();
            writer.close();

            jobId = parseJobId( proc );//will read proc stdout

            int exitCode = proc.waitFor();
            if(exitCode != 0)
            {
                throw new RuntimeException("sbatch exitCode=" + exitCode);
            } 
                       
            SlurmFileWatcher.getInstance().watchFile( remoteFolder != null ? this : null, getStdoutFile(), new LineBuffer() {

                @Override
                public void acceptLine(String line)
                {
                    env.print( line );
                }
            } );
            
            SlurmFileWatcher.getInstance().watchFile( remoteFolder != null ? this : null, getStderrFile(), new LineBuffer() {

                @Override
                public void acceptLine(String line)
                {
                    env.error( line );
                }
            } );

        }
        catch( IOException | InterruptedException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    //process builder that will run command: sbatch script
    protected List<String> getCommandPrefix( String sshCommand )
    {
        List<String> cmd = new ArrayList<>();
        if(remoteHost != null)
        {
            cmd.add( sshCommand );

            if( "scp".equals( sshCommand ) )
            {
                cmd.add( "-P" ); cmd.add( port );
            }
            else
            {
                cmd.add( "-p" ); cmd.add( port );
            }
            
            //We want to connect to different hosts with the same key, so disable host identity checking
            cmd.add("-o"); cmd.add( "UserKnownHostsFile=/dev/null" );
            cmd.add("-o"); cmd.add( "StrictHostKeyChecking=no" );
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
        }

        return cmd;
        
    }
    
    private static abstract class LineBuffer implements NewDataAction
    {
        ByteArrayOutputStream prevData = new ByteArrayOutputStream();
        @Override
        public void accept(byte[] data, int size)
        {
/*
            try
            {
                log.info( "accept, size = " + size+ ", data = " + new String( data, "UTF-8" ) );
            } 
            catch( java.io.UnsupportedEncodingException enc )
            {
            }
*/
            int start = 0;
            for(int i = 0; i < size; i++)
            { 
                if(data[i] == '\n' || data[i] == '\r')
                {
                    prevData.write( data, start, i-start );
                    acceptLine( new String( prevData.toByteArray() ) );
                    prevData.reset();
                    if(data[i] == '\r' && i+1 < size && data[i+1] == '\n' )
                        i++;
                    start = i+1;
                }
            }
            if(start < size)
            { 
                prevData.write( data, start, size-start );
            } 
        }

        public void flush()
        {
            String str = new String( prevData.toByteArray() );
            if( str.length() > 0 )
            {            
                acceptLine( str );
                prevData.reset();
            } 
        }
        
        public abstract void acceptLine(String line);

    }

    public void setJobId( long jobId )
    {
        this.jobId = jobId;
    }

    private long parseJobId(Process proc) throws IOException, NumberFormatException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String full = "";
        String line;
        while((line = reader.readLine()) != null)
        { 
            if( !"".equals( full ) )
            {
                full += "\n";
            }
            full += line;
            if(line.startsWith( JOB_ID_LINE ))
            {
                env.print( line );
                String jobId = line.substring( JOB_ID_LINE.length() ).trim();
                return Long.parseLong( jobId );
            }
            else
            {
                env.warn( "Ignoring sbatch output line: " + line );
            }
        }

        reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        while((line = reader.readLine()) != null)
        { 
            if( !"".equals( full ) )
            {
                full += "\n";
            }
            full += line;
        }  

        throw new RuntimeException("Unexpected sbatch output: " + full );
    }
    
    private StringBuilder buildScriptFile() throws IOException
    {
        StringBuilder content = new StringBuilder();
        content.append( "#!/bin/bash" ).append( '\n' );
        if( directory != null )
        {
            content.append( "#SBATCH -D " ).append( directory ).append( '\n' );
        }

        content.append( "#SBATCH -o " ).append( getStdoutFile() ).append( '\n' );
        content.append( "#SBATCH -e " ).append( getStderrFile() ).append( '\n' );
        
        content.append( "#SBATCH -c " ).append( resources.getCpu()).append( '\n' );
        int megabytes = (int)Math.ceil( resources.getMemory()/1024.0/1024.0);
        content.append( "#SBATCH --mem " ).append( megabytes ).append( '\n' );
        
        environ.forEach( (var,val)->content.append( var ).append( '=' ).append( '\'' ).append( val ).append( "'\n" ) );
        
        content.append( command ).append( '\n' );
        return content;

        //File scriptFile = getScriptFile();
        //ApplicationUtils.writeString( scriptFile, content.toString() );
        //return scriptFile;
    }

/*    
    private File getScriptFile()
    {
        return new File(getSharedFolder(), "run.sh");
    }
*/

    private String getStderrFile()
    {
        if( remoteFolder != null )
        {
            return remoteFolder + "/stderr";
        }

        try
        {  
            return new File(getSharedFolder(), "stderr").getCanonicalPath();
        }
        catch( IOException ioe )
        {
           throw new RuntimeException( ioe );
        }
    }

    private String getStdoutFile()
    {
        if( remoteFolder != null )
        {
            return remoteFolder + "/stdout";
        }

        try
        {  
            return new File(getSharedFolder(), "stdout").getCanonicalPath();
        }
        catch( IOException ioe )
        {
           throw new RuntimeException( ioe );
        }
    }

    public String getRemoteFolderBase()
    {
        return remoteFolderStr;
    } 

    public void setRemoteFolder( String remoteFolder )
    {
        this.remoteFolder = remoteFolder;
    } 

    public void downloadRemoteFilesIfNecessary()
    {
        if( remoteFolder == null )
        {
            return;
        }
        List<String> filesToSkip = new ArrayList<String>();
        for( int i = 0; inputFiles != null && i < inputFiles.length; i++ )
        {
            filesToSkip.add( remoteFolder + "/" + inputFiles[ i ].getName() );  
            filesToSkip.add( remoteFolder + "/stdout" );  
            filesToSkip.add( remoteFolder + "/stderr" );  
        }

        List<String> find = getCommandPrefix( "ssh" );
        find.add( "find" );
        find.add( remoteFolder );
        find.add( "-type" );
        find.add( "f" );
        find.add( "-size" );
        find.add( "+0" );
        find.add( "-print" );

        env.print( "Checking for remote files: " + find + "..." );

        try
        {
            ProcessBuilder findProcessBuilder = new ProcessBuilder( find );
            Process findProc = findProcessBuilder.start();
            String findOutput = readProcessOutput( findProc, false );
            int exitCode = findProc.waitFor();
            if( exitCode != 0 )
            {
                env.error( "" + find );
                env.error( "" + findOutput );
                return;
            }

            List<String> filesToDownload = new ArrayList<String>();
            for( String fileName: findOutput.split( "\n" ) )
            {
                fileName = fileName.trim();
                if( filesToSkip.contains( fileName ) )
                {
                    continue;
                } 
                filesToDownload.add( fileName ); 
            }         

            if( filesToDownload.size() == 0 )
            {
                return;
            }

            env.print( "Downloading remote files " + filesToDownload + "..." );

            List<String> scp = getCommandPrefix( "scp" );
            String host = scp.get( scp.size() - 1 );
            scp.remove( scp.size() - 1 );
            scp.add( "-C" );  
            for( int i = 0; i < filesToDownload.size(); i++ )
            {
                scp.add( host + ":" + filesToDownload.get( i ) );
            }
            scp.add( getSharedFolder().getCanonicalPath() );

            ProcessBuilder scpPocessBuilder = new ProcessBuilder( scp );                
            Process scpProc = scpPocessBuilder.start();
            String scpOutput = readProcessOutput( scpProc, false );

            exitCode = scpProc.waitFor();
            if( exitCode != 0 )
            {
                env.error( "" + scp  );
                env.error( "Cannot copy files from remote folder " + scpOutput );
                return;
            } 

            List<File> receivedFiles = new ArrayList<>();
            for( int i = 0; i < filesToDownload.size(); i++ )
            {
                File check = new File( getSharedFolder(), subst( filesToDownload.get( i ), remoteFolder, "", "" ) );
                if( check.exists() )
                { 
                    receivedFiles.add( check ); 
                }
            }
            env.print( "Received files " + receivedFiles );
            setOutputFiles( receivedFiles.toArray( new File[ 0 ] ) ); 
        }
        catch( Exception e )
        {
            env.error( e.getMessage() );
            log.log( Level.SEVERE, e.getMessage(), e );
        }
    } 

    
    @Override
    public int waitFor() throws LoggedException, InterruptedException
    {
        BlockingQueue<SlurmJobState> queue = new LinkedBlockingQueue<>();        

        List<String> squeueCMD = getCommandPrefix( "ssh" );
        squeueCMD.add( "squeue" );
        SlurmQueueWatcher.getInstance(squeueCMD).watchJobState( jobId, (state) -> {
            switch( state )
            {
                case CD:
                case CA:
                case F:
                case NF:
                case PR:
                case TO:
                    env.print( "waitFor got the state for job #" + jobId + ": " + state );
                    SlurmQueueWatcher.getInstance(squeueCMD).unwatch( jobId );
                    try
                    {
                        SlurmFileWatcher.getInstance().unwatch( getStdoutFile() );
                        SlurmFileWatcher.getInstance().unwatch( getStderrFile() );
                    }
                    catch( Exception exc )
                    {
                        env.error( "Unable to unwatch stdout and stderr: " + exc.getMessage() );
                    }    
                    queue.add( state );
            }
        } );

        TaskPool.getInstance().markCurrentThreadInactive();
        try
        {
            SlurmJobState state = queue.take();
            downloadRemoteFilesIfNecessary();
            return state == SlurmJobState.CD ? 0 : 1;
        }
        finally
        {
            TaskPool.getInstance().markCurrentThreadActive();
        }
    }

    @Override
    public void terminate()
    {
        try
        {
            List<String> cmd = getCommandPrefix( "ssh" );
            cmd.add( "scancel" );
            cmd.add( String.valueOf( jobId ) );
            ProcessBuilder processBuilder  = new ProcessBuilder( cmd );
            processBuilder.start();
        }
        catch( IOException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }
    
}
