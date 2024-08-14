package biouml.plugins.simulation.ode._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite ()
    {
        TestSuite suite = new TestSuite ( AutoTest.class.getName () );
        suite.addTestSuite(JVodeDirectTest.class);
        return suite;
    }
}