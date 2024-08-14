package biouml.plugins.downloadext._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Only automated (not visual) tests for nightly build should be included here.
 */
public class AutoTest extends TestCase
{
    public AutoTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );

        suite.addTestSuite( TestUpload.class );

        return suite;
    }
}
