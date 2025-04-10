package ru.biosoft.access.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang.mutable.MutableBoolean;

import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionThreadFactory;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
@CodePrivilege(CodePrivilegeType.THREAD)
public class TaskPool
{
    static final int ITERATE_SWITCH_MILLIS = 5000;
    private static TaskPool instance = new TaskPool();
    private static ThreadLocal<Task> threadTask = new ThreadLocal<>();
    private static ConcurrentMap<String, ThreadPool> userThreads = new ConcurrentHashMap<>();
    

    protected static class ThreadPool extends ThreadPoolExecutor
    {
        public ThreadPool(int threadsNumber)
        {
            super(threadsNumber, threadsNumber, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new SessionThreadFactory(true));
            allowCoreThreadTimeOut(true);
        }

        public void updatePoolSize(int threadsNumber)
        {
            if(getCorePoolSize() < threadsNumber)
            {
                setMaximumPoolSize(threadsNumber);
                setCorePoolSize(threadsNumber);
            }else if(getCorePoolSize() > threadsNumber)
            {
                setCorePoolSize(threadsNumber);
                setMaximumPoolSize(threadsNumber);
            }
        }
        
        public synchronized void changePoolSize(int delta)
        {
            int curSize = getCorePoolSize();
            updatePoolSize( curSize + delta );
        }

        public void submit(Task task)
        {
            if(!task.canSubmit()) return;
            if(task.estimateWeight() == 0.0)
            {   // Quick task: bypass the pool
                task.run();
            } else
            {
                Future<?> future = submit(new TaskLaunch(task, SecurityManager.getSession()));
                if(task instanceof AbstractTask)
                {
                    ((AbstractTask)task).setFuture(future);
                }
            }
        }

        public int getAvailableThreadsCount()
        {
            return getMaximumPoolSize()-getActiveCount();
        }

        public boolean hasAvailableThread()
        {
            return getAvailableThreadsCount() > 0;
        }
    }

    protected static class TaskLaunch implements Runnable
    {
        private final Task task;
        private final String sessionId;

        public TaskLaunch(Task task, String sessionId)
        {
            this.task = task;
            this.sessionId = sessionId;
        }

        @Override
        public void run()
        {
            SecurityManager.addThreadToSessionRecord( Thread.currentThread(), sessionId );
            threadTask.set(task);
            inactive.set( false );
            Thread.currentThread().setName("Task: "+task);
            try
            {
                task.run();
            }
            finally
            {
                if(inactive.get())
                {
                    getInstance().getUserPool().changePoolSize( -1 );
                    inactive.set( false );
                }
                threadTask.set(null);
                SecurityManager.removeThreadFromSessionRecord();
            }
            Thread.currentThread().setName("Idle (user: "+SecurityManager.getSessionUser()+")");
        }
    }

    private TaskPool()
    {

    }

    protected ThreadPool getUserPool()
    {
        String user = TextUtil2.nullToEmpty( SecurityManager.getSessionUser() );
        int threadsNumber = SecurityManager.getMaximumThreadsNumber();
        ThreadPool executor = userThreads.get(user);
        if(executor != null)
        {
            return executor;
        }
        executor = new ThreadPool(threadsNumber);
        ThreadPool oldValue = userThreads.putIfAbsent(user, executor);
        return oldValue == null ? executor : oldValue;
    }

    public int getAvailableThreadsCount()
    {
        return getUserPool().getAvailableThreadsCount();
    }

    public boolean hasAvailableThread()
    {
        return getAvailableThreadsCount() > 0;
    }

    public void submit(Task task)
    {
        if(!task.canSubmit()) return;
        if(task.estimateWeight() == 0.0)
        {   // Quick task: bypass the pool
            task.run();
        } else
        {
            ThreadPoolExecutor userPool = getUserPool();
            Future<?> future = userPool.submit(new TaskLaunch(task, SecurityManager.getSession()));
            if(task instanceof AbstractTask)
            {
                ((AbstractTask)task).setFuture(future);
            }
        }
    }

    public void submitAll(Collection<Task> tasks)
    {
        ThreadPoolExecutor userPool = getUserPool();
        for(Task task: tasks)
        {
            Future<?> future = userPool.submit(new TaskLaunch(task, SecurityManager.getSession()));
            if(task instanceof AbstractTask)
            {
                ((AbstractTask)task).setFuture(future);
            }
        }
    }

    /**
     * Run given runnable in several threads (at most maxThreads will be used) including current thread
     * and wait till all finish. The real number of threads depends on available resources in user thread pool,
     * but at least one thread (current) will be used.
     * @param runnable runnable to launch
     * @param maxThreads maximal number of threads to use
     * @throws InterruptedException if current thread was interrupted when waiting till other threads finish
     * @throws ExecutionException if exception occur in other threads
     */
    public void executeMultiple(Runnable runnable, int maxThreads) throws InterruptedException, ExecutionException
    {
        ThreadPool userPool = getUserPool();
        if(maxThreads == 1 || !userPool.hasAvailableThread())
        {
            try
            {
                runnable.run();
            }
            catch( Throwable t )
            {
                throw new ExecutionException(t);
            }
        } else
        {
            int i = 1;
            List<Task> tasks = new ArrayList<>(maxThreads);
            do
            {
                Task task = new RunnableTask(getThreadName()+"-#"+(++i), runnable);
                tasks.add(task);
                userPool.submit(task);
            } while(userPool.hasAvailableThread() && i<maxThreads);
            try
            {
                runnable.run();
            }
            catch( Throwable t )
            {
                throw new ExecutionException(t);
            }
            for(Task task: tasks)
            {
                task.join();
            }
        }
    }

    public void executeMultiple(Runnable runnable) throws InterruptedException, ExecutionException
    {
        executeMultiple(runnable, SecurityManager.getMaximumThreadsNumber());
    }

    /**
     * Iterate over collection in parallel (with JobControl)
     * @param <T> type of collection elements
     * @param collection collection to iterate over
     * @param iteration iteration code
     * @param jobControl jobControl (will react on cancel and update progress)
     * @param maxThreads maximal number of threads
     * @throws ExecutionException if iteration produced an exception
     * @throws InterruptedException if current thread was interrupted
     */
    public <T> void iterate(Collection<T> collection, final Iteration<T> iteration, final JobControl jobControl, int maxThreads) throws InterruptedException, ExecutionException
    {
       new IterationRunner<>( iteration, collection, jobControl, maxThreads ).run();
    }
    
    public <T> void iterate(Collection<T> collection, final Iteration<T> iteration, final JobControl jobControl) throws InterruptedException, ExecutionException
    {
        iterate(collection, iteration, jobControl, SecurityManager.getMaximumThreadsNumber());
    }

    public <T> void iterate(Collection<T> collection, final ExceptionalConsumer<T> iteration, final JobControl jobControl) throws InterruptedException, ExecutionException
    {
        iterate(collection, ExceptionalConsumer.iteration( iteration ), jobControl, SecurityManager.getMaximumThreadsNumber());
    }

    /**
     * Iterate over collection in parallel (without JobControl)
     * @param <T> type of collection elements
     * @param collection collection to iterate over
     * @param iteration iteration code
     * @param maxThreads maximal number of threads
     * @throws ExecutionException if iteration produced an exception
     * @throws InterruptedException if current thread was interrupted
     */
    public <T> void iterate(Collection<T> collection, final Iteration<T> iteration, int maxThreads) throws InterruptedException, ExecutionException
    {
        iterate( collection, iteration, null, maxThreads );
    }

    public <T> void iterate(Collection<T> collection, final ExceptionalConsumer<T> iteration) throws InterruptedException, ExecutionException
    {
        iterate(collection, ExceptionalConsumer.iteration(iteration), SecurityManager.getMaximumThreadsNumber());
    }

    public <T> void iterate(Collection<T> collection, final Iteration<T> iteration) throws InterruptedException, ExecutionException
    {
        iterate(collection, iteration, SecurityManager.getMaximumThreadsNumber());
    }

    public <I,O> List<O> map(final List<I> collection, final Function<I, O> transformer, int maxThreads) throws InterruptedException, ExecutionException
    {
        final AtomicInteger index = new AtomicInteger(0);
        final int size = collection.size();
        final Object[] result = new Object[size];
        final MutableBoolean finished = new MutableBoolean(false);
        while(!finished.booleanValue())
        {
            final long startTime = System.currentTimeMillis();
            executeMultiple(() -> {
                while(System.currentTimeMillis()-startTime < ITERATE_SWITCH_MILLIS)
                {
                    int curIndex = index.getAndIncrement();
                    if(curIndex >= size)
                    {
                        finished.setValue(true);
                        break;
                    }
                    I element = collection.get(curIndex);
                    try
                    {
                        result[curIndex] = transformer.apply(element);
                    }
                    catch( Exception e )
                    {
                        throw ExceptionRegistry.translateException(e);
                    }
                }
            }, Math.min(maxThreads, size));
        }
        return (List<O>)Arrays.asList(result);
    }

    public <I,O> List<O> map(final List<I> collection, final Function<I, O> transformer) throws InterruptedException, ExecutionException
    {
        return map(collection, transformer, SecurityManager.getMaximumThreadsNumber());
    }

    public Task getThreadTask()
    {
        return threadTask.get();
    }

    public String getThreadName()
    {
        Task task = getThreadTask();
        return task == null ? Thread.currentThread().getName() : task.getName();
    }

    /**
     * Task thread can be marked as inactive before going to sleep/wait,
     * in which case @code {@link ThreadPool} will produce one extra thread that
     * will execute other tasks of the user until this thread become active again.
     */
    private static ThreadLocal<Boolean> inactive = new ThreadLocal<>();
    
    public void markCurrentThreadInactive()
    {
        Boolean inactiveFlag = inactive.get();
        if(inactiveFlag == null)
            return;//Allow to call only from task thread
        if(inactiveFlag)
            return;//ignore already inactive
        
        inactive.set( true );
        getUserPool().changePoolSize( 1 );
    }
    
    public void markCurrentThreadActive()
    {
        Boolean inactiveFlag = inactive.get();
        if(inactiveFlag == null)
            return;//Allow to call only from task thread
        if(!inactiveFlag)
            return;//ignore already active
        inactive.set( false );
        getUserPool().changePoolSize( -1 );
    }
    

    public static TaskPool getInstance()
    {
        return instance;
    }
}
