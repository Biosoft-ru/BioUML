package biouml.plugins.stochastic._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());
        
        //TODO: this test has reference for unknown database/my
        //suite.addTest(ModelGeneratorTest.suite());
        
        return suite;
    }
}