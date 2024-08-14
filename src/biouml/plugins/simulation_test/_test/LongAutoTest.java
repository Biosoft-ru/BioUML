package biouml.plugins.simulation_test._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LongAutoTest extends TestCase
{

    public LongAutoTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( LongAutoTest.class.getName() );
        suite.addTest( SBMLOldTest.suite() );
        suite.addTest( ReaderWriterTest.suite() );
        return suite;
    }

}
