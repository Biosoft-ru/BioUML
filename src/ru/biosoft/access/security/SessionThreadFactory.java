package ru.biosoft.access.security;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lan
 *
 */
public class SessionThreadFactory implements ThreadFactory
{
    private AtomicInteger n = new AtomicInteger();
    private boolean byUser;
    
    public SessionThreadFactory()
    {
        this(false);
    }
    
    public SessionThreadFactory(boolean byUser)
    {
        this.byUser = byUser;
    }
    
    @Override
    public Thread newThread(Runnable r)
    {
        return new SessionThread(r, ( byUser ? "UserThread-" + SecurityManager.getSessionUser() : Thread.currentThread().getName() ) + "-#"
                + ( n.incrementAndGet() ));
    }
}
