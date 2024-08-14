package ru.biosoft.tasks.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SlurmFileWatcher
{
    
    //@FunctionalInterface
    public interface NewDataAction
    {
        void accept(byte[] data, int size);
        void flush(); 
    }
    
    private static final Logger log = Logger.getLogger( SlurmFileWatcher.class.getName() ); 
    
    private static class Holder
    {
        static SlurmFileWatcher instance = new SlurmFileWatcher();
    }
    
    public static final SlurmFileWatcher getInstance()
    {
        return Holder.instance;
    }
    
    private final Map<String, FileRecord> files = new ConcurrentHashMap<>();
    
    public void watchFile(SlurmProcessLauncher launcher, String file, NewDataAction action)
    {
        files.put( file, new FileRecord( launcher, file, action ) );
    }

    public void watchFile(String file, NewDataAction action)
    {
        files.put( file, new FileRecord( null, file, action ) );
    }
    
    public void unwatch(String file)
    {
        FileRecord rec = files.remove( file );
        rec.action.flush();
    }
    
    private static class FileRecord
    {
        SlurmProcessLauncher launcher;
        String file;
        NewDataAction action;        
        long readen;
        
        FileRecord(SlurmProcessLauncher launcher, String file, NewDataAction action)
        {
            this.launcher = launcher;
            this.file = file;
            this.action = action;
        }
    }

    private byte[] buffer = new byte[1024];
    private SlurmFileWatcher()
    {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable command = () -> {
            for(FileRecord rec : files.values())
            {
                if( rec.launcher == null )
                {
                    if(new File( rec.file ).length() > rec.readen)
                    {
                        try(FileInputStream fis = new FileInputStream( rec.file ))
                        {
                            fis.skip( rec.readen );
                            int count = fis.read( buffer );
                            rec.action.accept( buffer, count );
                            rec.readen += count;
                        }
                        catch(FileNotFoundException e)
                        {
                            //Expected behavior, file will be created later
                        }
                        catch( IOException e )
                        {
                            log.log( Level.SEVERE, "Error watching slurm file", e );
                        }
                    }

                    continue;
                }

                List<String> cat = rec.launcher.getCommandPrefix( "ssh" );
                cat.add( "cat" );
                cat.add( rec.file );
                try
                {
                    log.log( Level.INFO, "Checking remote file " + cat );
                    ProcessBuilder processBuilder = new ProcessBuilder( cat );
                    Process proc = processBuilder.start();
                    String catResult = rec.launcher.readProcessOutput( proc, true );
                    //log.log( Level.INFO, "RESULT = " + catResult );
                    if( catResult != null && catResult.length() > rec.readen )
                    {
                        int count = catResult.length() - ( int )rec.readen;
                        rec.action.accept( catResult.substring( ( int )rec.readen ).getBytes( "UTF-8" ), count );
                        rec.readen += count;
                    }
                }
                catch( IOException e )
                {
                    log.log( Level.SEVERE, "Error watching remote slurm file", e );
                }
            }
        };
        executor.scheduleAtFixedRate( command , 0, 1, TimeUnit.SECONDS );
    }
}
