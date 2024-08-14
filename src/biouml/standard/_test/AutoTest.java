package biouml.standard._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Batch unit test for biouml.model.
 */
public class AutoTest extends TestCase
{
    /** Standart JUnit constructor */
    public AutoTest( String name )
    {
        super(name);
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );
        suite.addTest( StandardModuleTypeTest.suite() );
        suite.addTest( TestStandardQueryEngine.suite() );
        suite.addTest( BioHubQueryEngineTest.suite() );
        suite.addTestSuite( SpeciesTest.class );
        return suite;
    }
}