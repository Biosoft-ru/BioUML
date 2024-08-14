package ru.biosoft.bsa._test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.biosoft.bsa.Interval;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class TestInterval extends TestCase
{
    public void testIntervalBasics()
    {
        // Construction and getters
        Interval interval = new Interval(100, 200);
        assertEquals(100, interval.getFrom());
        assertEquals(200, interval.getTo());
        assertEquals(101, interval.getLength());
        assertEquals(150, interval.getCenter());
        assertEquals(0, new Interval(5).getLength());
        
        // Comparison
        assertTrue(interval.equals(interval));
        assertFalse(interval.equals(null));
        assertFalse(interval.equals("test"));
        assertFalse(interval.equals(new Interval(100,199)));
        assertFalse(interval.equals(new Interval(101,200)));
        assertTrue(interval.equals(new Interval(100,200)));
        assertTrue(interval.compareTo(new Interval(120,140))<0);
        assertTrue(interval.compareTo(new Interval(20,140))>0);
        assertTrue(interval.compareTo(new Interval(100,140))>0);
        
        // Tests for position
        assertTrue(interval.inside(170));
        assertTrue(interval.inside(100));
        assertTrue(interval.inside(200));
        assertFalse(interval.inside(201));
        assertFalse(interval.inside(Integer.MAX_VALUE));
        assertEquals(0.297, interval.getPointPos(130), 0.001);
        assertEquals(0.0, interval.getIntervalPos(new Interval(100,100)));
        assertEquals(1.0, interval.getIntervalPos(new Interval(200,200)));
        assertEquals(0.5, interval.getIntervalPos(new Interval(150,150)));
        assertEquals(0.0, interval.getIntervalPos(new Interval(100,170)));
        assertEquals(0.0, interval.getIntervalPos(new Interval(100,200)));
        assertEquals(1.0, interval.getIntervalPos(new Interval(130,200)));
        assertEquals(0.5, interval.getIntervalPos(new Interval(110,190)));
        assertEquals(0.2, interval.getIntervalPos(new Interval(110,160)));
        assertTrue(interval.inside(new Interval(130,150)));
        assertTrue(interval.inside(new Interval(130,200)));
        assertFalse(interval.inside(new Interval(130,201)));
        assertTrue(interval.inside(interval));
        assertFalse(interval.inside(new Interval(0,300)));
        assertTrue(new Interval(0,300).inside(interval));
        
        // Intersection
        Interval interval2 = new Interval(170, 240);
        assertEquals(interval2, interval2.intersect(interval2));
        assertTrue(interval2.intersects(interval));
        assertTrue(interval2.intersects(interval2));
        assertNull(interval2.intersect(new Interval(1,100)));
        assertEquals(new Interval(170,200), interval2.intersect(interval));
        assertEquals(new Interval(170,200), interval.intersect(interval2));
        assertTrue(new Interval(200,300).intersects(interval));
        assertTrue(new Interval(0,100).intersects(interval));
        assertFalse(new Interval(201,300).intersects(interval));
        assertFalse(new Interval(0,99).intersects(interval));
        
        // Fitting
        assertEquals(new Interval(130,200), interval2.fit(interval));
        assertEquals(new Interval(170,240), interval.fit(interval2));
        assertEquals(new Interval(130,140), new Interval(130,140).fit(interval));
        
        // Zoom
        assertEquals(new Interval(125,175), interval.zoomToLength(51));
        assertEquals(new Interval(0,300), interval.zoomToLength(301));
        Interval interval1 = new Interval(200,200);
        assertEquals(new Interval(50, 350), interval1.zoomToLength(301) );
        Interval interval3 = new Interval(200,201);
        assertEquals(new Interval(51,351), interval3.zoomToLength(301));
        assertEquals(new Interval(150,150), interval.zoom(0));
        assertEquals(new Interval(150,200), interval.zoom(200, 0.5));
        
        // Split
        List<Interval> list = interval.split(1);
        assertEquals(1, list.size());
        assertEquals(list.get(0), interval);
        
        interval3 = new Interval(1000000000, 1000001000);
        list = interval3.split(3);
        checkList3(list, interval3);
        list = interval3.splitByStep(400);
        checkList3(list, interval3);
        assertEquals(400, list.get(0).getLength());
        assertEquals(400, list.get(1).getLength());
    }
    
    public void testCoverage()
    {
        Interval interval = new Interval(57, 86);
        
        List<Interval> list = new ArrayList<>();
        
        assertEquals( 0, interval.coverage( list ));
        
        list.add( new Interval(40, 45) );
        assertEquals( 0, interval.coverage( list ) );
        
        list.add( new Interval(40, 61) );
        assertEquals( 5, interval.coverage( list ) );
        
        list.add( new Interval(50, 60) );
        assertEquals( 5, interval.coverage( list ) );
        
        list.add( new Interval(85, 90) );
        assertEquals( 7, interval.coverage( list ) );
        
        list.add( new Interval(0, 100) );
        Collections.sort( list );
        assertEquals(interval.getLength(), interval.coverage( list ));
    }

    /**
     * @param list
     * @param interval
     */
    private void checkList3(List<Interval> list, Interval interval)
    {
        assertEquals(3, list.size());
        assertFalse(list.get(0).intersects(list.get(1)));
        assertFalse(list.get(1).intersects(list.get(2)));
        assertFalse(list.get(0).intersects(list.get(2)));
        assertEquals(interval.getFrom(), list.get(0).getFrom());
        assertEquals(list.get(0).getTo()+1, list.get(1).getFrom());
        assertEquals(list.get(1).getTo()+1, list.get(2).getFrom());
        assertEquals(list.get(2).getTo(), interval.getTo());
    }
}
