package biouml.plugins.enrichment._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());
        suite.addTestSuite(RepositoryHubTest.class);
        suite.addTestSuite(EnrichmentUtilsTest.class);
        return suite;
    }
}
