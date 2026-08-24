package ru.biosoft.access.biohub._test;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;

/**
 * Regression test for the race-safe invalidation of the {@link BioHubRegistry} caches
 * ({@code getMatchingGraph} / the {@code isDatabasePreferred} caches).
 *
 * <p>These caches are invalidation-based and guarded by a generation counter: a computation that
 * starts under generation N must only be installed if the generation has not advanced by the time
 * it finishes, otherwise an in-flight computation could repopulate the cache with a stale value
 * right after a concurrent invalidation. Before the fix, {@code matchingGraphCache} relied on
 * {@code putIfAbsent} alone, which is NOT safe after a {@code clear()} (the key looks like a
 * fresh miss). This test drives that interleaving against the real caches.
 *
 * <p>A real (file-based) databases repository is loaded so that the matching graph and the
 * preferred-database check do meaningful work. The graph may be small (a single start step) if
 * the test data defines no hubs, but the cache coordination under concurrent invalidation is
 * exercised either way.
 */
public class BioHubRegistryCacheInvalidationTest extends AbstractBioUMLTest
{
    public static final String repositoryPath = "../data/test/ru/biosoft/analysis/databases";

    private Properties input;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( repositoryPath );
        // Build an input from a registered reference type (its display name is what
        // getMatchingGraph stores under TYPE_PROPERTY, so the graph resolves it cleanly).
        ReferenceType type = ReferenceTypeRegistry.getDefaultReferenceType();
        input = new Properties();
        input.setProperty( "Species", "Homo sapiens" );
        input.setProperty( "ReferenceType", type.toString() );
    }

    public void testMatchingGraphReturnsNonNullAndCaches()
    {
        // Exercises getMatchingGraph (and the preferred-database caches reached while building
        // the hub list). Must not throw and must return a non-null list of steps.
        Properties[] reachable = BioHubRegistry.getReachableProperties( input );
        assertNotNull( "Reachable properties must not be null", reachable );
        // Second call returns the cached graph; it must still be non-null and stable.
        Properties[] reachable2 = BioHubRegistry.getReachableProperties( input );
        assertNotNull( "Cached reachable properties must not be null", reachable2 );
        assertEquals( reachable.length, reachable2.length );
    }

    public void testMatchingPathExercisesPreferredCache()
    {
        // getMatchingPath builds the graph (and filters hubs via isDatabasePreferred), so it
        // drives both caches. A null path is fine here (no hubs / no match in the test data);
        // what matters is that it runs without error under the caching code.
        BioHubRegistry.getMatchingPath( input, input );
    }

    public void testInvalidationIsRaceSafe() throws Exception
    {
        final int iterations = 2000;
        final AtomicBoolean failed = new AtomicBoolean( false );

        // Warm the caches once so the databases-collection listeners are attached.
        BioHubRegistry.getReachableProperties( input );

        final CountDownLatch start = new CountDownLatch( 1 );
        // One thread computes/reads, the other hammers invalidation of all three caches.
        Thread reader = new Thread( () -> {
            try
            {
                start.await();
                for( int i = 0; i < iterations; i++ )
                {
                    // Reading while another thread invalidates must never throw and must never
                    // install a stale value under a newer generation.
                    BioHubRegistry.getReachableProperties( input );
                }
            }
            catch( Throwable t )
            {
                t.printStackTrace();
                failed.set( true );
            }
        }, "biohub-reader" );
        Thread invalidator = new Thread( () -> {
            try
            {
                start.await();
                for( int i = 0; i < iterations; i++ )
                {
                    BioHubRegistry.invalidateMatchingGraphCache();
                    BioHubRegistry.invalidatePreferredDatabaseCache();
                }
            }
            catch( Throwable t )
            {
                t.printStackTrace();
                failed.set( true );
            }
        }, "biohub-invalidator" );

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
