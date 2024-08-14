package biouml.plugins.pharm._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );
        suite.addTest( TestExamples.suite() );
        suite.addTest( TestPopulationSampling.suite() );
        return suite;
    }
} 