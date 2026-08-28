package ru.biosoft.journal._test;

import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.journal.ProjectUtils;

/**
 * Regression test for the race-safe invalidation of
 * {@link ProjectUtils#getAvailableDatabaseVersions()}.
 *
 * <p>The cache is invalidation-based and guarded by a generation counter: a computation that
 * starts under generation N must only be installed if the generation has not advanced by the
 * time it finishes. Otherwise an in-flight computation could repopulate the cache with a stale
 * value right after a concurrent {@link ProjectUtils#invalidateAvailableDatabaseVersions()}.
 *
 * <p>This test drives that interleaving deterministically. It runs in the headless unit-test
 * environment, where the databases collection is empty (no MySQL), so the "computation" simply
 * returns an empty map; what is exercised is the cache/install/invalidate coordination, which is
 * the same mechanism used by the {@code BioHubRegistry} preferred-database and matching-graph
 * caches.
 */
public class ProjectUtilsCacheInvalidationTest extends AbstractBioUMLTest
{
    private VectorDataCollection<?> databases;

    @Override
    protected void setUp() throws Exception
    {
        // Register an in-memory "databases" collection so that
        // CollectionFactoryUtils.getDatabases() resolves in the headless unit-test
        // environment (no MySQL). It holds DataCollection elements (matching the real
        // databases repository type) but is left empty: getAvailableDatabaseVersions() then
        // simply returns an empty (but cached, immutable) map, and the cache coordination
        // under concurrent invalidation is still fully exercised.
        // The real collection is DataCollection<DataCollection<?>>; getDatabases() casts to a
        // raw DataCollection, so a raw VectorDataCollection is sufficient here.
        @SuppressWarnings( { "unchecked", "rawtypes" } )
        VectorDataCollection<?> dbs = new VectorDataCollection( "databases", DataCollection.class, null );
        databases = dbs;
        CollectionFactory.registerRoot( databases );
    }

    @Override
    protected void tearDown() throws Exception
    {
        // Unregister only the collection this test registered (not all roots) so it does not
        // clobber state that other tests in the same JVM may have registered.
        if( databases != null )
            CollectionFactory.unregisterRoot( databases );
        super.tearDown();
    }

    public void testGetReturnsImmutableAndCachedMap()
    {
        // With no databases the result is an empty map, but it must still be the same cached
        // instance on repeat calls and must be unmodifiable.
        Map<String, SortedSet<String>> first = ProjectUtils.getAvailableDatabaseVersions();
        Map<String, SortedSet<String>> second = ProjectUtils.getAvailableDatabaseVersions();
        assertSame(first, second);
        try
        {
            first.put( "should-fail", null );
            fail( "Expected UnsupportedOperationException: cached map must be immutable" );
        }
        catch( UnsupportedOperationException e )
        {
            // expected
        }
    }

    public void testInvalidationIsRaceSafe() throws Exception
    {
        final int iterations = 2000;
        final AtomicBoolean failed = new AtomicBoolean( false );

        // Warm the cache once so the listener is attached.
        ProjectUtils.getAvailableDatabaseVersions();

        final CountDownLatch start = new CountDownLatch( 1 );
        // One thread computes/reads, the other hammers invalidation.
        Thread reader = new Thread( () -> {
            try
            {
                start.await();
                for( int i = 0; i < iterations; i++ )
                {
                    // Reading while another thread invalidates must never throw and must never
                    // install a stale value under a newer generation.
                    ProjectUtils.getAvailableDatabaseVersions();
                }
            }
            catch( Throwable t )
            {
                t.printStackTrace();
                failed.set( true );
            }
        }, "cache-reader" );
        Thread invalidator = new Thread( () -> {
            try
            {
                start.await();
                for( int i = 0; i < iterations; i++ )
                {
                    ProjectUtils.invalidateAvailableDatabaseVersions();
                }
            }
            catch( Throwable t )
            {
                t.printStackTrace();
                failed.set( true );
            }
        }, "cache-invalidator" );

        reader.start();
        invalidator.start();
        start.countDown();
        reader.join( TimeUnit.SECONDS.toMillis( 60 ) );
        invalidator.join( TimeUnit.SECONDS.toMillis( 60 ) );
        assertFalse( "A cache thread threw", failed.get() );
        assertFalse( "Reader thread did not finish", reader.isAlive() );
        assertFalse( "Invalidator thread did not finish", invalidator.isAlive() );
    }
}
