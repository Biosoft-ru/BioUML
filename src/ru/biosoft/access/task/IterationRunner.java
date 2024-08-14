package ru.biosoft.access.task;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import ru.biosoft.access.task.TaskPool.ThreadPool;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Runs ru.biosoft.jobcontrol.Iteration on each element of supplied
 * collection inside ru.biosoft.access.task.TaskPool using no more then maxThreads (including calling thread).
 * The tasks are submitted in batches to the TaskPool, allowing to run the batch for TaskPool.ITERATE_SWITCH_MILLIS miliseconds.
 * This will allow TaskPool to execute other tasks in between.
 */
public class IterationRunner<T>
{
    private Iteration<T> iteration;
    
    private Object[] elements;
    private AtomicInteger idx = new AtomicInteger( 0 );
    
    private int maxThreads;

    private TaskPool taskPool = TaskPool.getInstance();
    private ThreadPool userPool;
    private JobControl jc;
    
    private String taskNamePrefix;
    
    
    private AtomicInteger finished = new AtomicInteger(0);//number of finished tasks, either with success or error 
    private Throwable error;
    private Object lock = new Object();//used to read/write of 'finished', 'error' and 'cancel' fields
    
    private boolean cancelled;
    
    public IterationRunner(Iteration<T> iteration, Collection<T> elements, JobControl jc, int maxThreads)
    {
        this.iteration = iteration;
        this.elements = elements.toArray();
        this.jc = jc;

        userPool =  taskPool.getUserPool();
        this.maxThreads = maxThreads;
    }
    
    public void run() throws InterruptedException, ExecutionException
    {
        taskNamePrefix = taskPool.getThreadName();
        
        for(int i = 0; i < maxThreads-1; i++)
            submitNewTask(taskNamePrefix + "-#" + i);
        
        runInCurrentThread(Long.MAX_VALUE);

        waitForOtherThreads();
    }

    private void waitForOtherThreads() throws InterruptedException, ExecutionException
    {
        synchronized( lock )
        {
            //wait for successfull finish of all tasks or error
            while( finished.get() < elements.length && error == null && !cancelled)
                lock.wait();
            if(error != null || cancelled)
            {
                //wait for already launched tasks
                while(finished.get() < idx.get())
                    lock.wait();
                
                if(error != null)
                    throw new ExecutionException( error );
            }
        }
    }

    private void runInCurrentThread(long maxDuration)
    {
        long startTime = System.currentTimeMillis();
        while( (System.currentTimeMillis() - startTime) < maxDuration )
        {
            if(jc != null && jc.getStatus() == JobControl.TERMINATED_BY_REQUEST)
                cancel();
                
            synchronized( lock )
            {
                if( error != null || cancelled )
                    return;
            }

            int i = idx.getAndUpdate( old->Math.min( elements.length, old + 1 ) );
            if( i >= elements.length )
                break;

            @SuppressWarnings ( "unchecked" )
            T element = (T)elements[i];
            
            try
            {
                if(!iteration.run( element ))
                {
                    cancel();
                    break;
                }
            }
            catch( Throwable t )
            {
                error( t );
                break;
            }
            finally
            {
               finishIteration();
            }
        }
        
    }

    private void error(Throwable t)
    {
        synchronized(lock)
        {
            error = t;
            lock.notify();
        }
    }

    private void cancel()
    {
        synchronized(lock)
        {
            cancelled = true;
            lock.notify();
        }
    }
    
    private void finishIteration()
    {
        int curFinished;
        synchronized( lock )
        {
            curFinished = finished.incrementAndGet();
            lock.notify();
        }
        if(jc != null)
            jc.setPreparedness( (int)curFinished*100 / elements.length );
    }
    
    private void submitNewTask(String name)
    {       
        synchronized( lock )
        {
            if(idx.get() >= elements.length || error != null || cancelled)
                return;
        }

        Task task = new RunnableTask(name, ()->{
            runInCurrentThread( TaskPool.ITERATE_SWITCH_MILLIS );
            submitNewTask( name );
        });
        
        userPool.submit(task);
    }
    
}
