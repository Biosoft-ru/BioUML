package biouml.plugins.ensembl.analysis._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public AutoTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());
        suite.addTestSuite(TestSNPListToTrack.class);
        return suite;
    }
}