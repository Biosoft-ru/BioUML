package ru.biosoft.bsa.analysis._test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping;
import junit.framework.TestCase;

/**
 * Tests the default {@link CoordinateMapping#mapInterval(Interval)} implementation.
 *
 * The contract under test: the returned {@link Collection} holds the <em>distinct</em>
 * mapped positions (deduplicated), with unmapped positions (-1) skipped. Callers use the
 * result only for emptiness checks and best/iterate scans, which are order- and
 * dedup-insensitive — but the implementation still must return distinct values, so these
 * tests assert both the set content and the result cardinality (which a mere set-equality
 * check would not catch if duplicates were returned).
 */
public class CoordinateMappingTest extends TestCase
{
    // Identity mapping on [1, 10]: get(x) = x. For an identity mapping the mapped value
    // (y - i = (from+i) - i) is the constant 'from', so the whole interval collapses to
    // a single distinct value.
    private static final CoordinateMapping IDENTITY = new CoordinateMapping()
    {
        @Override
        public int get(int x)
        {
            return x >= 1 && x <= 10 ? x : -1;
        }

        @Override
        public Interval getBounds()
        {
            return new Interval(1, 10);
        }
    };

    // CONSTANT_TARGET: every position maps to the same target (get(x) = 1), so
    // mapped = 1 - i varies with i, producing multiple distinct values. Exercises the
    // 2+ distinct-value branch.
    private static final CoordinateMapping CONSTANT_TARGET = new CoordinateMapping()
    {
        @Override
        public int get(int x)
        {
            return x >= 1 && x <= 10 ? 1 : -1;
        }

        @Override
        public Interval getBounds()
        {
            return new Interval(1, 10);
        }
    };

    // Identity except x=3 and x=7 are unmapped (-1). Verifies that -1 positions are
    // skipped without affecting the (constant) mapped value.
    private static final CoordinateMapping WITH_GAPS = new CoordinateMapping()
    {
        @Override
        public int get(int x)
        {
            if(x < 1 || x > 10) return -1;
            if(x == 3 || x == 7) return -1;
            return x;
        }

        @Override
        public Interval getBounds()
        {
            return new Interval(1, 10);
        }
    };

    // A non-identity mapping whose mapped values are genuinely distinct, with a
    // deliberate duplicate (i=0 and i=2 both map to 2) to exercise deduplication.
    private static final CoordinateMapping MULTI_DISTINCT = new CoordinateMapping()
    {
        @Override
        public int get(int x)
        {
            return x >= 1 && x <= 10 ? x + (x % 2) : -1; // even x -> x, odd x -> x+1
        }

        @Override
        public Interval getBounds()
        {
            return new Interval(1, 10);
        }
    };

    private static Set<Integer> toSet(Collection<Integer> c)
    {
        Set<Integer> s = new HashSet<>();
        for( Integer i : c ) s.add(i);
        return s;
    }

    public void testEmptyWhenAllUnmapped()
    {
        assertTrue(WITH_GAPS.mapInterval(new Interval(3, 3)).isEmpty());
    }

    public void testIdentitySinglePosition()
    {
        Collection<Integer> r = IDENTITY.mapInterval(new Interval(5, 5));
        assertEquals(1, r.size());
        assertTrue(r.contains(5));
    }

    public void testIdentityIntervalCollapsesToOne()
    {
        // mapped is constant (= from) for an identity mapping -> exactly one value.
        Collection<Integer> r = IDENTITY.mapInterval(new Interval(2, 6));
        assertEquals(1, r.size());
        assertTrue(r.contains(2));
    }

    public void testLargeIntervalSingleDistinctValue()
    {
        // A long interval that still produces a single distinct value: must return
        // exactly one element (this is the common case the allocation must not
        // regress — the result size is the distinct count, not the interval length).
        Collection<Integer> r = IDENTITY.mapInterval(new Interval(1, 10));
        assertEquals(1, r.size());
        assertTrue(r.contains(1));
    }

    public void testConstantTargetMultipleDistinct()
    {
        // get(x) = 1 for all x in [1,10]; mapped = 1 - i.
        // Interval [2,4] -> i = 0,1,2 -> mapped = 1, 0, -1 -> 3 distinct.
        Collection<Integer> r = CONSTANT_TARGET.mapInterval(new Interval(2, 4));
        Set<Integer> expected = new HashSet<>();
        expected.add(1); expected.add(0); expected.add(-1);
        assertEquals(expected, toSet(r));
        assertEquals(3, r.size()); // cardinality: no duplicates
    }

    public void testGapsSkipUnmapped()
    {
        // Interval [2,4] covers x = 2,3,4; x=3 is unmapped (-1). mapped is still the
        // constant 2 for the identity positions -> a single value {2}, gaps skipped.
        Collection<Integer> r = WITH_GAPS.mapInterval(new Interval(2, 4));
        assertEquals(1, r.size());
        assertTrue(r.contains(2));
    }

    public void testAgainstBruteForceReference()
    {
        // Brute-force reference: the distinct set of (get(from+i) - i) over the
        // interval, skipping get == -1. This is independent of the
        // implementation's dedup/growth logic, so it settles correctness.
        for( int a = 1; a <= 10; a++ )
            for( int b = a; b <= 10; b++ )
            {
                Set<Integer> expected = new HashSet<>();
                for( int i = 0; i < b - a + 1; i++ )
                {
                    int y = MULTI_DISTINCT.get( a + i );
                    if( y != -1 )
                        expected.add( y - i );
                }
                Collection<Integer> r = MULTI_DISTINCT.mapInterval( new Interval( a, b ) );
                assertEquals( "content for [" + a + "," + b + "]", expected, toSet( r ) );
                assertEquals( "cardinality for [" + a + "," + b + "]", expected.size(), r.size() );
            }
    }

    public void testManyDistinctGrowsBuffer()
    {
        // CONSTANT_TARGET: mapped = 1 - i, all distinct. Interval [1,6] ->
        // mapped = 1,0,-1,-2,-3,-4 (6 distinct), exercising the int[] growth path
        // (capacity 2 -> 4 -> 8) and confirming no value is lost during growth.
        Collection<Integer> r = CONSTANT_TARGET.mapInterval(new Interval(1, 6));
        Set<Integer> expected = new HashSet<>();
        for( int v = 1; v >= -4; v-- ) expected.add( v );
        assertEquals(expected, toSet(r));
        assertEquals(6, r.size());
    }

    // Fixed-table mapping used for adversarial cases: it produces repeated values,
    // long runs of duplicates, irregular -1 gaps, and many distinct values, all of
    // which the linear-scan + growth + dedup logic must handle correctly. The table
    // is defined over source coords 1..12; out-of-range coords map to -1 (unmapped).
    private static final int[] ADVERSARIAL_TABLE = {
        0, // index 0 unused (source coords start at 1)
        5, 5, 5, -1, 9, 9, -1, 3, 3, 3, 12, 12 // coords 1..12
    };
    private static final CoordinateMapping ADVERSARIAL = new CoordinateMapping()
    {
        @Override
        public int get(int x)
        {
            return x >= 1 && x < ADVERSARIAL_TABLE.length ? ADVERSARIAL_TABLE[x] : -1;
        }

        @Override
        public Interval getBounds()
        {
            return new Interval(1, 12);
        }
    };

    private static Set<Integer> referenceDistinct(CoordinateMapping m, int from, int to)
    {
        Set<Integer> expected = new HashSet<>();
        for( int i = 0; i <= to - from; i++ )
        {
            int y = m.get( from + i );
            if( y != -1 )
                expected.add( y - i );
        }
        return expected;
    }

    public void testAdversarialGapsAndDuplicates()
    {
        // ADVERSARIAL over [1,12] has repeated values (runs of 5, runs of 3, runs of
        // 9), irregular -1 gaps (coords 4 and 7), and several distinct mapped values.
        // Verify against the brute-force reference for both content and cardinality.
        Collection<Integer> r = ADVERSARIAL.mapInterval( new Interval( 1, 12 ) );
        Set<Integer> expected = referenceDistinct( ADVERSARIAL, 1, 12 );
        assertEquals(expected, toSet(r));
        assertEquals(expected.size(), r.size());
    }

    public void testAdversarialGrowDuplicateGrowDuplicate()
    {
        // Exercises the most complicated state: grow -> duplicate -> grow -> duplicate.
        // With a table mapping, distinct mapped values appear interleaved with runs of
        // duplicates that must be dropped after the buffer has already grown.
        // Sweep every sub-interval of the adversarial mapping against the reference.
        for( int a = 1; a <= 12; a++ )
            for( int b = a; b <= 12; b++ )
            {
                Set<Integer> expected = referenceDistinct( ADVERSARIAL, a, b );
                Collection<Integer> r = ADVERSARIAL.mapInterval( new Interval( a, b ) );
                assertEquals("content for [" + a + "," + b + "]", expected, toSet(r));
                assertEquals("cardinality for [" + a + "," + b + "]", expected.size(), r.size());
            }
    }

    public void testManyDistinctThenDuplicatesAfterGrowth()
    {
        // A mapping whose mapped values (get(from+i) - i) first form a run of distinct
        // values that force the buffer to grow past capacity 2 and 4, and then repeat
        // earlier values, to confirm the dedup scan works once the buffer has grown.
        // Desired mapped sequence for from=1, i=0..11: 1,2,3,4,5,6,1,2,3,4,5,1.
        // Since mapped = get(1+i) - i, we need get(x) = mapped + i = mapped + (x-1).
        int[] desiredMapped = {1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 1}; // i = 0..11
        int[] table = new int[13]; // table[x] = get(x), x = 1..12
        for( int x = 1; x <= 12; x++ )
            table[x] = desiredMapped[x - 1] + (x - 1);
        CoordinateMapping m = new CoordinateMapping()
        {
            @Override
            public int get(int x)
            {
                return x >= 1 && x < table.length ? table[x] : -1;
            }

            @Override
            public Interval getBounds()
            {
                return new Interval(1, 12);
            }
        };
        // The distinct mapped values are {1,2,3,4,5,6}; the repeats (1,2,3,4,5,1)
        // must be dropped even though the buffer has already grown.
        Set<Integer> expected = new HashSet<>();
        for( int v = 1; v <= 6; v++ ) expected.add( v );
        Collection<Integer> r = m.mapInterval( new Interval( 1, 12 ) );
        assertEquals(expected, toSet(r));
        assertEquals(6, r.size()); // 6 distinct, 6 duplicates dropped
    }
}
