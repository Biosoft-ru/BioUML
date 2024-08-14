package biouml.standard.simulation.plot._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    
    public static Test suite ( )
    {
        TestSuite suite = new TestSuite ( AutoTest.class.getName ( ) );
        suite.addTest ( PlotSqlTransformerTest.suite ( ) );
        suite.addTest ( PlotTransformerTest.suite ( ) );
        return suite;
    }
    
}
