package com.developmentontheedge.log;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class ThreadFilter implements Filter
{

    private final long targetThreadId;
    private final ThreadGroup tg;

    public ThreadFilter(Thread currentThread)
    {
        targetThreadId = currentThread.getId();
        tg = currentThread.getThreadGroup();
    }

    @Override
    public boolean isLoggable(LogRecord record)
    {
        Thread logThread = Thread.currentThread();
        return logThread.getId() == targetThreadId;
    }

}
