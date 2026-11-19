package ru.biosoft.access.task._test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.plugins.mcp.support.McpProviderSupport;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.task.RunnableTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.access.task.ThreadShare;

/** The thread budget shared among the users with work ({@link ThreadShare}), as {@link TaskPool} applies it. */
public class ThreadShareTest extends AbstractBioUMLTest
{
    private Preferences saved;
    private Preferences preferences;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        saved = Application.getPreferences();
        preferences = new Preferences();
        Application.setPreferences( preferences );
        set( "threadsNumber", 8 );
    }

    @Override
    protected void tearDown() throws Exception
    {
        Application.setPreferences( saved );
        // pools keep their last size: give them threadsNumber of the restored preferences again
        TaskPool.getInstance().getAvailableThreadsCount();
        super.tearDown();
    }

    private void set(String name, int value)
    {
        preferences.remove( name );
        preferences.add( new DynamicProperty( name, Integer.class, value ) );
    }

    public void testPerUser()
    {
        // no budget: threadsNumber for each user, as before
        assertEquals( 28, ThreadShare.perUser( 0, 3, 8, 28 ) );
        // a budget of 80 on a 90-CPU server, at most 80 and at least 8 per user
        assertEquals( 80, ThreadShare.perUser( 80, 1, 8, 80 ) );
        assertEquals( 40, ThreadShare.perUser( 80, 2, 8, 80 ) );
        assertEquals( 20, ThreadShare.perUser( 80, 4, 8, 80 ) );
        assertEquals( 8, ThreadShare.perUser( 80, 20, 8, 80 ) );
        // no user with work counts as one; threadsNumber caps the share; the minimum never exceeds it
        assertEquals( 28, ThreadShare.perUser( 80, 0, 8, 28 ) );
        assertEquals( 4, ThreadShare.perUser( 80, 30, 8, 4 ) );
        assertEquals( 1, ThreadShare.perUser( 5, 10, 0, 8 ) );
    }

    /** One user: the pool follows the budget, capped by threadsNumber, and threadsNumber alone without one. */
    public void testPoolFollowsBudget()
    {
        TaskPool pool = TaskPool.getInstance();
        assertEquals( 8, pool.getAvailableThreadsCount() );
        set( ThreadShare.BUDGET_PREFERENCE, 6 );
        set( ThreadShare.MIN_PER_USER_PREFERENCE, 2 );
        assertEquals( 6, pool.getAvailableThreadsCount() );
        set( ThreadShare.BUDGET_PREFERENCE, 100 );
        assertEquals( 8, pool.getAvailableThreadsCount() );
        preferences.remove( ThreadShare.BUDGET_PREFERENCE );
        assertEquals( 8, pool.getAvailableThreadsCount() );
    }

    /** A task waiting with markCurrentThreadInactive keeps its extra thread when the share changes. */
    public void testWaitingThreadSurvivesResize() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 6 );
        set( ThreadShare.MIN_PER_USER_PREFERENCE, 2 );
        AtomicInteger before = new AtomicInteger(), after = new AtomicInteger();
        RunnableTask task = new RunnableTask( "waiting", () -> {
            TaskPool pool = TaskPool.getInstance();
            pool.markCurrentThreadInactive();
            before.set( pool.getAvailableThreadsCount() );
            set( ThreadShare.BUDGET_PREFERENCE, 4 );
            after.set( pool.getAvailableThreadsCount() );
            pool.markCurrentThreadActive();
        } );
        TaskPool.getInstance().submit( task );
        task.join();
        // share + 1 waiting thread, minus this running one
        assertEquals( 6, before.get() );
        assertEquals( 4, after.get() );
    }

    /** Two users with work split the budget; when one is done, the other gets it all back. */
    public void testUsersShareBudget() throws Exception
    {
        set( ThreadShare.BUDGET_PREFERENCE, 8 );
        set( ThreadShare.MIN_PER_USER_PREFERENCE, 2 );
        CountDownLatch started = new CountDownLatch( 1 ), release = new CountDownLatch( 1 );
        RunnableTask other = new RunnableTask( "other user", () -> {
            started.countDown();
            try
            {
                release.await( 30, TimeUnit.SECONDS );
            }
            catch( InterruptedException e )
            {
                Thread.currentThread().interrupt();
            }
        } );
        McpProviderSupport.withUserSession( "threadShareTestUser", () -> {
            TaskPool.getInstance().submit( other );
            return null;
        } );
        try
        {
            assertTrue( "the other user's task did not start", started.await( 10, TimeUnit.SECONDS ) );
            assertEquals( "two users with work: half of the budget each", 4, TaskPool.getInstance().getAvailableThreadsCount() );
        }
        finally
        {
            release.countDown();
            other.join();
        }
        int available = 0;
        for( int i = 0; i < 50 && available != 8; i++ )
        {
            Thread.sleep( 100 );
            available = TaskPool.getInstance().getAvailableThreadsCount();
        }
        assertEquals( "the other user is done: the whole budget again", 8, available );
    }
}
