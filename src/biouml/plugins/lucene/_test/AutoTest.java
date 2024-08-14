package biouml.plugins.lucene._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );
        suite.addTest( LuceneTest.suite() );
        suite.addTest( BioHubTest.suite() );
        suite.addTestSuite( LuceneWebTest.class );

        return suite;
    }
}