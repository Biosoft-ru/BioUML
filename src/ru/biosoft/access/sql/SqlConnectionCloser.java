package ru.biosoft.access.sql;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Close sql connections when thread terminates.
 */
public class SqlConnectionCloser
{
    private static final Logger log = Logger.getLogger( SqlConnectionCloser.class.getName() );
    
    private Set<Pair> threadToCon = Collections.newSetFromMap( new ConcurrentHashMap<>() );
    

    public SqlConnectionCloser()
    {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor( new ThreadFactory()
        {
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread( r, "SQL connection closer" );
                t.setDaemon( true );
                return t;
            }
        } );
        executor.scheduleAtFixedRate( this::checkThreads, 0, 2, TimeUnit.MINUTES );
    }
    
    public void register(PersistentConnection con)
    {
        threadToCon.add( new Pair( Thread.currentThread(), con ) );
    }
    
    
    private void checkThreads()
    {
        Iterator<Pair> it = threadToCon.iterator();
        while(it.hasNext())
        {
            Pair e = it.next();
            Thread t = e.thread;
            PersistentConnection con = e.con;
            if(!t.isAlive())
            {
                    try
                    {
                        con.close();
                    }
                    catch( SQLException ex )
                    {
                        log.log(Level.SEVERE,  "While closing sql connections of thread " + t.getName(), ex );
                    }
                    it.remove();
            }
            else
            {
                con.checkTimeout( SqlConnectionPool.CONNECTION_TIMEOUT );
            }
        } 
    }

    private static class Pair
    {
        Thread thread;
        PersistentConnection con;

        public Pair(Thread thread, PersistentConnection con)
        {
            this.thread = thread;
            this.con = con;
        }

    }
}
