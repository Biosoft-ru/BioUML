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
 * their tasks are done: a pool without work does not count, so pools left by other tests do not change the shares.</p>
 */
public class ThreadShareTest extends AbstractBioUMLTest
{
    private static final AtomicInteger runs = new AtomicInteger();

    private Preferences saved;
    private Preferences preferences;
    /** A prefix for this test's user names. */
    private String prefix;
    /** Releases every blocking task this test submitted. */
    private CountDownLatch release;
    private final List<RunnableTask> submitted = new ArrayList<>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        saved = Application.getPreferences();
        preferences = new Preferences();
        Application.setPreferences( preferences );
        set( "threadsNumber", 8 );
        prefix = "threadShare" + runs.incrementAndGet() + "-";
        release = new CountDownLatch( 1 );
    }

    @Override
    protected void tearDown() throws Exception
    {
        release.countDown();
        for( RunnableTask task : submitted )
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

    private int maxThreads(String user)
    {
        return as( user, () -> TaskPool.getInstance().getMaximumThreadsCount() );
    }

    /** A task of the user that counts down started when it runs, then blocks until the test ends. */
    private StoppableTask submitBlocking(String user, CountDownLatch started)
    {
        StoppableTask task = new StoppableTask( user, () -> {
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
        StoppableTask(String name, Runnable runnable)
        {
            super( name, runnable );
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

        // b is done: the work count of b drops after its thread finishes the task
        CountDownLatch releaseB = release;
        release = new CountDownLatch( 1 );
        releaseB.countDown();
        b.join();
        awaitMaxThreads( "the other user is done: the whole budget again", "a", 8 );
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

    /** A task cancelled while queued stops counting as work once its pool drops it. */
    public void testCancelledQueuedTaskIsNoWork() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 2 );
        CountDownLatch started = new CountDownLatch( 2 );
        submitBlocking( "a", started );
        submitBlocking( "a", started );
        assertTrue( started.await( 10, TimeUnit.SECONDS ) );
        StoppableTask queued = submitBlocking( "a", new CountDownLatch( 1 ) );
        queued.stop();
        // a's two running tasks end; its cancelled one is taken from the queue and dropped
        CountDownLatch releaseA = release;
        release = new CountDownLatch( 1 );
        releaseA.countDown();

        CountDownLatch startedB = new CountDownLatch( 1 );
        submitBlocking( "b", startedB );
        assertTrue( startedB.await( 10, TimeUnit.SECONDS ) );
        awaitMaxThreads( "a has no work left: b gets the whole budget", "b", 2 );
    }
}
