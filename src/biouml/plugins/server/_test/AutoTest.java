package biouml.plugins.server._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Batch unit test for biouml.model.
 */
public class AutoTest extends TestCase
{
    /** Standart JUnit constructor */
    public AutoTest ( String name )
    {
        super ( name );
    }

    /** Make suite of tests. */
    public static Test suite ( )
    {
        TestSuite suite = new TestSuite ( AutoTest.class.getName ( ) );
        
        suite.addTest ( ServerTest.suite ( ) );
        //TODO: add tests

        return suite;
    }
}