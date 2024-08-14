package ru.biosoft.access;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class FileCollectionWatcher
{
    public static FileCollectionWatcher getInstance()
    {
        return InstanceHolder.instance;
    }
    
    public void register(FileCollection c) { collections.add( c ); }
    public void unregister(FileCollection c) { collections.remove( c ); }
    
    
    private static class InstanceHolder {
        static FileCollectionWatcher instance = new FileCollectionWatcher(); 
    }
    
    private Set<FileCollection> collections = Collections.newSetFromMap( new ConcurrentHashMap<>() );
    
    private FileCollectionWatcher()
    {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor( new ThreadFactory()
        {
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread( r, "FileCollectionWatcher" );
                t.setDaemon( true );
                return t;
            }
        } );
        executor.scheduleAtFixedRate( this::update, 0, 1, TimeUnit.SECONDS );
    }
    
    private void update()
    {
           for(FileCollection c : collections)
               c.reinit();
    }
}
