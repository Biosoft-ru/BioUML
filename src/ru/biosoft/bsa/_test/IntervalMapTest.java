
package ru.biosoft.bsa._test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import ru.biosoft.bsa.IntervalMap;

public class IntervalMapTest extends TestCase
{
    private static void assertSetsEqual(String message, Collection<Integer> got, Integer... expected)
    {
        Set<Integer> set1 = new HashSet<>( got );
        Set<Integer> set2 = new HashSet<>( Arrays.asList( expected ) );
        assertEquals(message, set2, set1);
    }

    public void testIntervalMap()
    {
        IntervalMap<Integer> map = new IntervalMap<>();
        map.add(0, 100, 1);
        map.add(50, 150, 2);
        map.add(100, 200, 3);
        map.add(100, 110, 6);
        map.add(150, 150, 4);
        map.add(-500, -100, 5);
        assertSetsEqual("At -1", map.getIntervals(-1));
        assertSetsEqual("At MIN_INT", map.getIntervals(Integer.MIN_VALUE));
        assertSetsEqual("At MAX_INT", map.getIntervals(Integer.MAX_VALUE));
        assertSetsEqual("At 0", map.getIntervals(0), 1);
        assertSetsEqual("At 49", map.getIntervals(49), 1);
        assertSetsEqual("At 50", map.getIntervals(50), 1, 2);
        assertSetsEqual("At 100", map.getIntervals(100), 1, 2, 3, 6);
        assertSetsEqual("At 149", map.getIntervals(149), 2, 3);
        assertSetsEqual("At 150", map.getIntervals(150), 2, 3, 4);
        assertSetsEqual("At 151", map.getIntervals(151), 3);
        assertSetsEqual("At 200", map.getIntervals(200), 3);
        assertSetsEqual("At 201", map.getIntervals(201));
        assertSetsEqual("-10..100000", map.getIntervals(-10, 100000), 1,2,3,4,6);
        assertSetsEqual("MIN_INT..MAX_INT", map.getIntervals(Integer.MIN_VALUE, Integer.MAX_VALUE), 1,2,3,4,5,6);
        assertSetsEqual("150..151", map.getIntervals(150, 151), 2,3,4);
        assertSetsEqual("100..150", map.getIntervals(100, 150), 1,2,3,4,6);
        assertSetsEqual("200..300", map.getIntervals(200, 300), 3);
        assertSetsEqual("201..300", map.getIntervals(201, 300));
    };

    public void testZeroLengthIntervalMap()
    {
        IntervalMap<String> map = new IntervalMap<>();
        map.add( 2, 5, "a" );
        map.add( 3, 4, "b" );
        map.add( 4, 6, "c" );
        map.add( 5, 8, "d" );
        map.add( 1, 9, "e" );
        map.add( 10, 20, "l" );
        map.add( 6, 5, "i" );

        Collection<String> overlaps = map.getIntervals( 5, 4 );
        assertTrue( "Interval does not overlap insertion", overlaps.contains( "a" ) );
        assertTrue( "Interval does not overlap insertion", overlaps.contains( "c" ) );
        assertTrue( "Interval does not overlap insertion", overlaps.contains( "e" ) );
        assertFalse( "Interval overlaps left border", overlaps.contains( "b" ) );
        assertFalse( "Interval overlaps right border", overlaps.contains( "d" ) );

        overlaps = map.getIntervals( 14, 13 );
        assertTrue( "Interval does not overlap insertion", overlaps.contains( "l" ) );

        overlaps = map.getIntervals( 6, 5 );
        assertTrue( "Interval does not overlap insertion", overlaps.contains( "c" ) );
        assertTrue( "Interval does not overlap insertion", overlaps.contains( "d" ) );
        assertTrue( "Interval does not overlap insertion", overlaps.contains( "e" ) );
        assertTrue( "Interval does not overlap insertion", overlaps.contains( "i" ) );
    }
}
