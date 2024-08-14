package ru.biosoft.tasks.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SlurmQueueWatcher
{
    private static final Logger log = Logger.getLogger( SlurmQueueWatcher.class.getName() );

    private static final Map<List<String>, SlurmQueueWatcher> instances = new ConcurrentHashMap<>();
    
    public static SlurmQueueWatcher getInstance(List<String> squeueCMD)
    {
        return instances.computeIfAbsent( squeueCMD, SlurmQueueWatcher::new );
    }
    
    
    private SlurmQueueWatcher(List<String> squeueCMD)
    {
        log.log( Level.INFO, "Creating new watcher for " + squeueCMD );
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate( createRunnable(squeueCMD), 0, 1, TimeUnit.SECONDS );
    }

    private Map<Long, Consumer<SlurmJobState>> watchers = new ConcurrentHashMap<>();
    public void watchJobState(long jobId, Consumer<SlurmJobState> callback)
    {
        log.log( Level.INFO, "WATCHING job " + jobId );
        watchers.put( jobId, callback );
    }
    
    public void unwatch(long jobId)
    {
        log.log( Level.INFO, "UNWATCHING job " + jobId );
        watchers.remove( jobId );
    }
        
    private Runnable createRunnable(List<String> squeueCMD)
    {
        return () -> {
            Map<Long, Consumer<SlurmJobState>> localWatchers = new HashMap<>( watchers );
            String jobs = localWatchers.keySet().stream().map( String::valueOf ).collect( Collectors.joining( "," ) );
            if(jobs.isEmpty())
                return;
            List<String> cmd = new ArrayList<>(squeueCMD);
            cmd.add( "-t" );
            cmd.add("all" );
            cmd.add( "-j" );
            cmd.add( jobs);
            cmd.add( "--noheader" );
            cmd.add("-o" );
            cmd.add( "%i-%t" );
            try
            {
                //log.log( Level.INFO, "Checking queue state " + cmd  );
                ProcessBuilder pb = new ProcessBuilder( cmd );
                //pb.redirectError( new File("/dev/null") );
                Process proc = pb.start();
                proc.getOutputStream().close();
                BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

                String line;
                while( ( line = err.readLine() ) != null )
                {
                    log.log( Level.SEVERE, line );
                }

                // output will be like
                // 3-CD
                // 4-CG      
                
                InputStream in = proc.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader( in ));
                  
                ArrayList<Long> processed = new ArrayList<>();
                while((line = reader.readLine()) != null)
                { 
                    //log.info( line );
                    String[] parts = line.split( "-", 2 );
                    Long jobId = Long.parseLong( parts[0] );
                    processed.add( jobId );
                    Consumer<SlurmJobState> callback = localWatchers.get( jobId );
                    if( callback != null )
                    { 
                        callback.accept( SlurmJobState.valueOf( parts[1] ) );
                    }
                } 
                proc.waitFor();
                // Check for lost entries because squeue stops reporting job ID after few munutes
                // in this case we will assume CD - Job has terminated all processes on all nodes.
                for( Map.Entry<Long, Consumer<SlurmJobState>> entry : localWatchers.entrySet() )
                {
                    if( !processed.contains( entry.getKey() ) )
                    {
                        log.log( Level.INFO, "Finishing STRAY job " + entry.getKey() );
                        entry.getValue().accept( SlurmJobState.valueOf( "CD" ) );
                    }
                }
            }
            catch( IOException | InterruptedException e )
            {
                log.log( Level.SEVERE, "Error running squeue", e );
            }
        };
    }

/*
    private void parseSqueueLine(String line)
    {
        String[] parts = line.split( "-", 2 );
        long jobId = Long.parseLong( parts[0] );
        SlurmJobState state = SlurmJobState.valueOf( parts[1] );
        Consumer<SlurmJobState> callback = watchers.get( jobId );
        if(callback == null)
            return;
        callback.accept( state );
    }
*/
}
