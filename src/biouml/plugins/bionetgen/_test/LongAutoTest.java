package biouml.plugins.bionetgen._test;

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
        suite.addTest( BionetgenSimulationTest.suite() );
        return suite;
    }

}
