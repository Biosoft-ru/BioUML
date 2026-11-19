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
import java.util.logging.Logger;

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
    

    private static final Logger log = Logger.getLogger( TaskPool.class.getName() );
    /** The threads per user of the last rebalance, to log only a change. */
    private static volatile int lastShare = -1;

    protected static class ThreadPool extends ThreadPoolExecutor
    {
        /** This user's share of threads ({@link ThreadShare}). */
        private int share;
        /** Threads added while tasks of this pool wait ({@link TaskPool#markCurrentThreadInactive}): kept on a resize. */
        private volatile int waitingThreads;

        public ThreadPool(int threadsNumber)
        {
            super(threadsNumber, threadsNumber, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new SessionThreadFactory(true));
            allowCoreThreadTimeOut(true);
            share = threadsNumber;
        }

        /**
         * Set this user's share. A smaller pool interrupts no running task: each finishes its current item, and the
         * iterations that requeue every {@link TaskPool#ITERATE_SWITCH_MILLIS} then run on fewer threads.
         */
        public synchronized void setShare(int share)
        {
            if( share == this.share )
                return;
            this.share = share;
            updatePoolSize( Math.max( 1, share + waitingThreads ) );
        }

        /** Whether this user has work: a running task that is not just waiting, or a queued one. */
        public boolean hasWork()
        {
            return getActiveCount() > waitingThreads || !getQueue().isEmpty();
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
        
        /** A task of this pool starts (+1) or stops (-1) waiting: the pool grows or shrinks by one thread around its share. */
        public synchronized void changePoolSize(int delta)
        {
            waitingThreads = Math.max( 0, waitingThreads + delta );
            updatePoolSize( Math.max( 1, share + waitingThreads ) );
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
                // a task ended: the user may have no work left, and the others more threads
                rebalance( null );
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
        ThreadPool executor = userThreads.get(user);
        if(executor == null)
        {
            executor = new ThreadPool(SecurityManager.getMaximumThreadsNumber());
            ThreadPool oldValue = userThreads.putIfAbsent(user, executor);
            if(oldValue != null)
                executor = oldValue;
        }
        // the pool is looked up to submit work to it (or to see how much it could take): counts as a user with work
        rebalance( executor );
        return executor;
    }

    /**
     * Give every user's pool its share of the thread budget ({@link ThreadShare}); without a budget, threadsNumber.
     * Cheap (one pass over the users' pools, a resize only when the share changed), so it runs whenever a pool is
     * looked up and whenever a task ends.
     * @param submitting a pool about to get work, counted as busy; or null
     */
    static void rebalance(ThreadPool submitting)
    {
        int max = SecurityManager.getMaximumThreadsNumber();
        int budget = ThreadShare.budget();
        int users = 0;
        if( budget > 0 )
        {
            for( ThreadPool pool : userThreads.values() )
                if( pool == submitting || pool.hasWork() )
                    users++;
        }
        int share = ThreadShare.perUser( budget, users, ThreadShare.minPerUser(), max );
        for( ThreadPool pool : userThreads.values() )
            pool.setShare( share );
        if( share != lastShare )
        {
            lastShare = share;
            if( budget > 0 )
                log.info( "Task threads per user: " + share + " (budget " + budget + ", " + users + " user(s) with work, max "
                        + max + ")" );
        }
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
