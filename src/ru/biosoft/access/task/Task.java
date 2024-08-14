package ru.biosoft.access.task;

import java.util.concurrent.ExecutionException;

/**
 * New style tasks
 * @author lan
 *
 */
public interface Task extends Runnable
{
    /**
     * Task having this weight or less is considered to be quick and will be launched without thread pool
     */
    public static final double WEIGHT_QUICK = 0.1;
    public static final double WEIGHT_NORMAL = 1.1;
    
    /**
     * @return user-friendly name of the task
     */
    public String getName();
    
    /**
     * @return parent task if applicable
     */
    public Task getParent();
    
    /**
     * @return estimation of task execution time (0 = very quick task, 1 = quite long task)
     */
    public double estimateWeight();
    
    /**
     * @return estimation of allocated memory in bytes
     */
    public long estimateMemory();
    
    /**
     * Called before submission to check whether task is valid
     */
    public boolean canSubmit();
    
    /**
     * @return true if task is finished
     */
    public boolean isDone();

    /**
     * Wait till task finishes
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void join() throws InterruptedException, ExecutionException;

    /**
     * @return true if task is stopped by request
     */
    public boolean isStopped();
}
