package ru.biosoft.access.security;

import ru.biosoft.access.sql.SqlConnectionPool;

/**
 * Thread which automatically registers itself as belonging to the session
 * @author lan
 */
public class SessionThread extends Thread
{
    protected void register()
    {
        SecurityManager.addThread(this);
        setPriority(NORM_PRIORITY-2);
    }

    public SessionThread()
    {
        register();
    }

    public SessionThread(Runnable runnable)
    {
        super(runnable);
        register();
    }

    public SessionThread(String name)
    {
        super(name);
        register();
    }

    public SessionThread(ThreadGroup group, Runnable runnable)
    {
        super(group, runnable);
        register();
    }

    public SessionThread(ThreadGroup group, String name)
    {
        super(group, name);
        register();
    }

    public SessionThread(Runnable runnable, String name)
    {
        super(runnable, name);
        register();
    }

    public SessionThread(ThreadGroup group, Runnable runnable, String name)
    {
        super(group, runnable, name);
        register();
    }

    public SessionThread(ThreadGroup group, Runnable runnable, String name, long stackSize)
    {
        super(group, runnable, name, stackSize);
        register();
    }
    
    public void doRun()
    {
        super.run();
    }

    @Override
    public void run()
    {
        try
        {
            doRun();
        }
        finally
        {
            SqlConnectionPool.closeMyConnections();
        }
    }
}
