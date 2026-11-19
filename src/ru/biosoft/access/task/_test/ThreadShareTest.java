package ru.biosoft.access.task._test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.plugins.mcp.support.McpProviderSupport;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.task.RunnableTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.access.task.ThreadShare;

/**
 * The thread budget shared among the users with work ({@link ThreadShare}), as {@link TaskPool} applies it.
 *
 * <p>Pools are per user and live as long as the JVM, so each test uses users of its own and waits, at its end, until
 * their tasks are done: a pool without work does not count, so pools left by other tests do not change the shares.
 * Every blocking task has its own release latch, so a test can end one user's task and keep another's running. A
 * task accounting error ({@link TaskPool#getAccountingErrors}: a task counted out twice, say) fails the test.</p>
 */
public class ThreadShareTest extends AbstractBioUMLTest
{
    private static final AtomicInteger runs = new AtomicInteger();

    private Preferences saved;
    private Preferences preferences;
    /** A prefix for this test's user names. */
    private String prefix;
    private final List<StoppableTask> submitted = new ArrayList<>();
    /** {@link TaskPool#getAccountingErrors} when the test started: it must not grow. */
    private int accountingErrors;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        saved = Application.getPreferences();
        preferences = new Preferences();
        Application.setPreferences( preferences );
        set( "threadsNumber", 8 );
        prefix = "threadShare" + runs.incrementAndGet() + "-";
        accountingErrors = TaskPool.getAccountingErrors();
    }

    @Override
    protected void tearDown() throws Exception
    {
        for( StoppableTask task : submitted )
            task.release.countDown();
        for( StoppableTask task : submitted )
        {
            try
            {
                task.join();
            }
            catch( Exception e )
            {
                // a cancelled task
            }
        }
        Application.setPreferences( saved );
        // pools keep their last size: apply the restored preferences
        TaskPool.getInstance().getAvailableThreadsCount();
        super.tearDown();
        assertEquals( "task accounting errors", accountingErrors, TaskPool.getAccountingErrors() );
    }

    private void set(String name, int value)
    {
        preferences.remove( name );
        preferences.add( new DynamicProperty( name, Integer.class, value ) );
    }

    private <T> T as(String user, Callable<T> action)
    {
        return McpProviderSupport.withUserSession( prefix + user, () -> action.call() );
    }

    private int pending(String user)
    {
        return as( user, () -> TaskPool.getInstance().getPendingTasksCount() );
    }

    private int maxThreads(String user)
    {
        return as( user, () -> TaskPool.getInstance().getMaximumThreadsCount() );
    }

    /** A task of the user that counts down started when it runs, then blocks until its release (or the test's end). */
    private StoppableTask submitBlocking(String user, CountDownLatch started)
    {
        CountDownLatch release = new CountDownLatch( 1 );
        StoppableTask task = new StoppableTask( user, release, () -> {
            started.countDown();
            try
            {
                release.await( 60, TimeUnit.SECONDS );
            }
            catch( InterruptedException e )
            {
                Thread.currentThread().interrupt();
            }
        } );
        synchronized( submitted )
        {
            submitted.add( task );
        }
        as( user, () -> {
            TaskPool.getInstance().submit( task );
            return null;
        } );
        return task;
    }

    private static class StoppableTask extends RunnableTask
    {
        final CountDownLatch release;

        StoppableTask(String name, CountDownLatch release, Runnable runnable)
        {
            super( name, runnable );
            this.release = release;
        }

        @Override
        public void stop()
        {
            super.stop();
        }
    }

    /**
     * Pool sizes change after a task's thread finishes it (afterExecute), which nothing in the test can wait on: wait
     * for the size, at most 5 seconds.
     */
    private void awaitMaxThreads(String message, String user, int expected) throws InterruptedException
    {
        int actual = maxThreads( user );
        for( int i = 0; i < 100 && actual != expected; i++ )
        {
            Thread.sleep( 50 );
            actual = maxThreads( user );
        }
        assertEquals( message, expected, actual );
    }

    public void testPerUser()
    {
        // no budget: threadsNumber for each user, as before
        assertEquals( 28, ThreadShare.perUser( 0, 3, 28 ) );
        // a budget of 80, at most 80 per user
        assertEquals( 80, ThreadShare.perUser( 80, 1, 80 ) );
        assertEquals( 40, ThreadShare.perUser( 80, 2, 80 ) );
        assertEquals( 20, ThreadShare.perUser( 80, 4, 80 ) );
        assertEquals( 26, ThreadShare.perUser( 80, 3, 80 ) );
        // the budget caps the sum: 20 users get 4 each, not more
        assertEquals( 4, ThreadShare.perUser( 80, 20, 80 ) );
        // but every user with work gets one thread
        assertEquals( 1, ThreadShare.perUser( 80, 100, 80 ) );
        // no user with work counts as one; threadsNumber caps the share
        assertEquals( 28, ThreadShare.perUser( 80, 0, 28 ) );
        assertEquals( 1, ThreadShare.perUser( 80, 3, 0 ) );
    }

    /** One user: the pool follows the budget, capped by threadsNumber, and threadsNumber alone without one. */
    public void testPoolFollowsBudget()
    {
        assertEquals( 8, maxThreads( "a" ) );
        set( ThreadShare.BUDGET_PREFERENCE, 6 );
        assertEquals( 6, maxThreads( "a" ) );
        set( ThreadShare.BUDGET_PREFERENCE, 100 );
        assertEquals( 8, maxThreads( "a" ) );
        preferences.remove( ThreadShare.BUDGET_PREFERENCE );
        assertEquals( 8, maxThreads( "a" ) );
    }

    /** A task waiting with markCurrentThreadInactive keeps its extra thread when the share changes. */
    public void testWaitingThreadSurvivesResize() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 6 );
        AtomicInteger before = new AtomicInteger(), after = new AtomicInteger();
        RunnableTask task = new RunnableTask( "waiting", () -> {
            TaskPool pool = TaskPool.getInstance();
            pool.markCurrentThreadInactive();
            before.set( pool.getMaximumThreadsCount() );
            set( ThreadShare.BUDGET_PREFERENCE, 4 );
            after.set( pool.getMaximumThreadsCount() );
            pool.markCurrentThreadActive();
        } );
        as( "a", () -> {
            TaskPool.getInstance().submit( task );
            return null;
        } );
        task.join();
        // the share, and one thread more for the waiting task
        assertEquals( 7, before.get() );
        assertEquals( 5, after.get() );
    }

    /** Two users with work split the budget; when one is done, the other gets it all back. */
    public void testUsersShareBudget() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 8 );
        CountDownLatch startedA = new CountDownLatch( 1 ), startedB = new CountDownLatch( 1 );
        submitBlocking( "a", startedA );
        assertTrue( startedA.await( 10, TimeUnit.SECONDS ) );
        assertEquals( "one user with work: the whole budget", 8, maxThreads( "a" ) );
        StoppableTask b = submitBlocking( "b", startedB );
        assertTrue( startedB.await( 10, TimeUnit.SECONDS ) );
        assertEquals( "two users with work: half of the budget each", 4, maxThreads( "a" ) );
        assertEquals( 4, maxThreads( "b" ) );

        // only b is done (a's task keeps running): the work count of b drops after its thread finishes the task
        b.release.countDown();
        b.join();
        awaitMaxThreads( "the other user is done: the whole budget again", "a", 8 );
        // and a third user with work gets half of it again, so a is still counted
        CountDownLatch startedC = new CountDownLatch( 1 );
        submitBlocking( "c", startedC );
        assertTrue( startedC.await( 10, TimeUnit.SECONDS ) );
        assertEquals( "a still has work", 4, maxThreads( "a" ) );
    }

    /** Looking a pool up (how many threads could I use?) is not work: it takes no threads from the users with work. */
    public void testLookupIsNotWork() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 8 );
        CountDownLatch started = new CountDownLatch( 1 );
        submitBlocking( "a", started );
        assertTrue( started.await( 10, TimeUnit.SECONDS ) );
        // b only asks: it would get half if it had work, and a keeps the whole budget
        assertEquals( 4, maxThreads( "b" ) );
        assertEquals( 8, maxThreads( "a" ) );
        assertEquals( 4, as( "b", () -> TaskPool.getInstance().getAvailableThreadsCount() ).intValue() );
        assertEquals( 8, maxThreads( "a" ) );
    }

    /**
     * Users submitting at the same moment from their own threads: once all tasks run, the shares are those of all the
     * users with work, and add up to the budget.
     */
    public void testConcurrentSubmissionsStayWithinBudget() throws Exception
    {
        int users = 4, tasksPerUser = 2;
        set( ThreadShare.BUDGET_PREFERENCE, users * tasksPerUser );
        CountDownLatch started = new CountDownLatch( users * tasksPerUser );
        CyclicBarrier together = new CyclicBarrier( users );
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for( int u = 0; u < users; u++ )
        {
            String user = "u" + u;
            Thread thread = new Thread( () -> {
                try
                {
                    together.await( 10, TimeUnit.SECONDS );
                    for( int t = 0; t < tasksPerUser; t++ )
                        submitBlocking( user, started );
                }
                catch( Throwable t )
                {
                    failure.set( t );
                }
            } );
            threads.add( thread );
            thread.start();
        }
        for( Thread thread : threads )
            thread.join( 10_000 );
        assertNull( String.valueOf( failure.get() ), failure.get() );
        assertTrue( "not every task started", started.await( 10, TimeUnit.SECONDS ) );
        int total = 0;
        for( int u = 0; u < users; u++ )
        {
            int max = maxThreads( "u" + u );
            assertEquals( "u" + u, tasksPerUser, max );
            total += max;
        }
        assertEquals( users * tasksPerUser, total );
    }

    /** More users with work than the budget: one thread each, so that each makes progress. */
    public void testMoreUsersThanBudget() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 2 );
        CountDownLatch started = new CountDownLatch( 3 );
        for( String user : new String[] {"a", "b", "c"} )
            submitBlocking( user, started );
        assertTrue( "a user did not get a thread", started.await( 10, TimeUnit.SECONDS ) );
        for( String user : new String[] {"a", "b", "c"} )
            assertEquals( user, 1, maxThreads( user ) );
    }

    /**
     * A task cancelled while queued leaves the queue at once and is counted out once: while the user's running tasks
     * still block, and again when they end (which the accounting check of tearDown would report if counted twice).
     */
    public void testCancelledQueuedTaskIsNoWork() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 2 );
        CountDownLatch started = new CountDownLatch( 2 );
        StoppableTask a1 = submitBlocking( "a", started );
        StoppableTask a2 = submitBlocking( "a", started );
        assertTrue( started.await( 10, TimeUnit.SECONDS ) );
        CountDownLatch queuedStarted = new CountDownLatch( 1 );
        StoppableTask queued = submitBlocking( "a", queuedStarted );
        assertEquals( 3, pending( "a" ) );
        queued.stop();
        // a's running tasks still block, and the cancelled task is no work at once
        assertEquals( 2, pending( "a" ) );
        assertEquals( 2, maxThreads( "a" ) );

        // a running task cancelled is counted out when its thread finishes it, not twice
        a2.stop();
        a1.release.countDown();
        a2.release.countDown();
        a1.join();
        CountDownLatch startedB = new CountDownLatch( 1 );
        submitBlocking( "b", startedB );
        assertTrue( startedB.await( 10, TimeUnit.SECONDS ) );
        awaitMaxThreads( "a has no work left: b gets the whole budget", "b", 2 );
        assertEquals( "the cancelled task never ran", 1, queuedStarted.getCount() );
    }

    /**
     * Submissions racing cancellations, from several threads, on a pool whose only thread is taken: some tasks are
     * cancelled in the queue, some after a worker took them, some before they could start. Every one is counted out
     * once: no accounting error, and no work left when all are done.
     */
    public void testSubmitRacesCancel() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 1 );
        CountDownLatch started = new CountDownLatch( 1 );
        StoppableTask blocker = submitBlocking( "a", started );
        assertTrue( started.await( 10, TimeUnit.SECONDS ) );
        int threads = 4, perThread = 200;
        CyclicBarrier together = new CyclicBarrier( threads );
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> racers = new ArrayList<>();
        List<StoppableTask> quick = java.util.Collections.synchronizedList( new ArrayList<>() );
        for( int t = 0; t < threads; t++ )
        {
            Thread racer = new Thread( () -> {
                try
                {
                    together.await( 10, TimeUnit.SECONDS );
                    as( "a", () -> {
                        for( int i = 0; i < perThread; i++ )
                        {
                            StoppableTask task = new StoppableTask( "race", new CountDownLatch( 0 ), () -> {
                            } );
                            quick.add( task );
                            TaskPool.getInstance().submit( task );
                            if( i % 2 == 0 )
                                task.stop();
                            // the other half is cancelled later, by another racer or once the blocker is released
                        }
                        return null;
                    } );
                }
                catch( Throwable e )
                {
                    failure.set( e );
                }
            } );
            racers.add( racer );
            racer.start();
        }
        // cancel the rest while the racers still submit, and release the pool's only thread midway
        Thread canceller = new Thread( () -> {
            for( int round = 0; round < 50; round++ )
            {
                StoppableTask[] snapshot;
                synchronized( quick )
                {
                    snapshot = quick.toArray( new StoppableTask[0] );
                }
                for( StoppableTask task : snapshot )
                    task.stop();
                if( round == 25 )
                    blocker.release.countDown();
                Thread.yield();
            }
        } );
        canceller.start();
        for( Thread racer : racers )
            racer.join( 30_000 );
        canceller.join( 30_000 );
        assertNull( String.valueOf( failure.get() ), failure.get() );
        blocker.release.countDown();
        int left = pending( "a" );
        for( int i = 0; i < 100 && left != 0; i++ )
        {
            Thread.sleep( 50 );
            left = pending( "a" );
        }
        assertEquals( "work left after all tasks are done or cancelled", 0, left );
    }
}
