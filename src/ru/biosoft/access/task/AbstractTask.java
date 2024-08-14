package ru.biosoft.access.task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author lan
 *
 */
public abstract class AbstractTask implements Task
{
    private Task parent;
    private String name;
    private boolean stopped = false;
    private boolean finished = false;
    private boolean started = false;
    protected Future<?> future = null;
    
    public AbstractTask(String name)
    {
        this.parent = TaskPool.getInstance().getThreadTask();
        this.name = name;
    }

    @Override
    public Task getParent()
    {
        return parent;
    }

    @Override
    public double estimateWeight()
    {
        return 1;
    }

    @Override
    public long estimateMemory()
    {
        return 0;
    }

    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public boolean isStopped()
    {
        return stopped;
    }

    protected void stop()
    {
        stopped = true;
        if(future != null)
        {
            future.cancel(false);
        }
    }

    @Override
    public void run()
    {
        if(started || stopped || finished)
            throw new IllegalStateException("Unable to start task again");
        started = true;
        try
        {
            doRun();
        }
        finally
        {
            started = false;
        }
    }
    
    protected abstract void doRun();

    protected void setFuture(Future<?> future)
    {
        this.future = future;
    }
    
    @Override
    public void join() throws InterruptedException, ExecutionException
    {
        if(this.future != null)
            this.future.get();
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public boolean canSubmit()
    {
        return true;
    }

    @Override
    public boolean isDone()
    {
        return finished;
    }
}
