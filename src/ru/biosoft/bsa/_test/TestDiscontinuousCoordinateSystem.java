package ru.biosoft.bsa._test;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import ru.biosoft.bsa.DiscontinuousCoordinateSystem;
import ru.biosoft.bsa.Interval;

public class TestDiscontinuousCoordinateSystem extends TestCase
{
    public void test1()
    {
        List<Interval> regions = Arrays.asList( new Interval(1310085,1310537), new Interval(1309380,1309825), new Interval(1309110,1309282));
        DiscontinuousCoordinateSystem coordSystem = new DiscontinuousCoordinateSystem( regions, true );
        assertEquals( 1310130, coordSystem.translateCoordinateBack( 407 ) );
    }
}
