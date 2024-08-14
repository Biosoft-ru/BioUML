package biouml.plugins.simulation_test._test;

import biouml.plugins.simulation_test.dsmts._test.DSMTSTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Batch unit test for biouml.model.
 */
public class AutoTest extends TestCase
{
    /** Standart JUnit constructor */
    public AutoTest(String name)
    {
        super(name);
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());
        suite.addTest(SBMLTest.suite());
        suite.addTest(DSMTSTest.suite());
        suite.addTest(TestVirtualHuman.suite());
        return suite;
    }
}