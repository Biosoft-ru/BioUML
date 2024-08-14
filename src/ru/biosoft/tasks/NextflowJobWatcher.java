package ru.biosoft.tasks;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

public class NextflowJobWatcher
{
    private static final Logger log = Logger.getLogger( NextflowJobWatcher.class.getName() );
    
    private NextflowService nextflow;

    private Map<String, Set<Handler>> handlers = new ConcurrentHashMap<>();
    
    public NextflowJobWatcher(NextflowService nextflow)
    {
        this.nextflow = nextflow;
        start();
    }

    @FunctionalInterface
    public static interface Handler
    {
        void statusUpdate(JSONObject params);
    }
    
    
    public void register(String jobId, Handler handler)
    {
        Set<Handler> thisJobHandlers = handlers.computeIfAbsent( jobId, k->Collections.newSetFromMap( new ConcurrentHashMap<>() ) );
        thisJobHandlers.add( handler );
    }
    
    public void unregister(String jobId, Handler handler)
    {
        Set<Handler> thisJobHandlers = handlers.get( jobId );
        thisJobHandlers.remove( handler );
    }
    

    private void start()
    {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor( new ThreadFactory()
        {
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread( r, "NextflowJobWatcher" );
                t.setDaemon( true );
                return t;
            }
        } );
        executor.scheduleAtFixedRate( this::update, 0, 5, TimeUnit.SECONDS );
    }
    
    private void update()
    {
        handlers.forEach( (jobId, thisJobHandlers) -> {
            JSONObject result;
            try
            {
                result = nextflow.status( jobId );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not request nextflow job status for " + jobId, e);
                return;
            }
            thisJobHandlers.forEach( h->{
                try {
                    h.statusUpdate(result);
                } catch(Throwable t)
                {
                    log.log( Level.SEVERE, "Error handling nextflow job status update for " + jobId, t );
                }
            });
        });
        
    }
    
    
    
    
    
}
