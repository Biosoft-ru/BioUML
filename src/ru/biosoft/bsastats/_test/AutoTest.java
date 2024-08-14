package ru.biosoft.bsastats._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.bsastats._test.TestTrimLowQuality;

public class AutoTest extends TestCase
{
    public AutoTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTestSuite( TestTrimLowQuality.class );
        suite.addTestSuite( TestTrackStatistics.class );
        suite.addTestSuite( MicroRNAAlignerTest.class );
        return suite;
    }
}
