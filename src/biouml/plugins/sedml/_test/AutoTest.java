package biouml.plugins.sedml._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest
        extends TestCase
{
    public static Test suite ( )
    {
        TestSuite suite = new TestSuite ( AutoTest.class.getName ( ) );
        suite.addTestSuite( TestListOfModelsBuilder.class );
        suite.addTestSuite( TestSedMlImporter.class );
        suite.addTestSuite( TestRangesBuilder.class );
        suite.addTestSuite( TestMathMLUtils.class );
        suite.addTestSuite( TestListOfModelsParser.class );
        return suite;
    }

    /**
     * Run test in TestRunner. If args[0].startsWith("text") then textui runner
     * runs, otherwise swingui runner runs.
     * 
     * @param args[0]
     *            Type of test runner.
     */
    public static void main ( String[] args )
    {
        if ( args != null && args.length != 0 && args[0].startsWith ( "text" ) )
        {
            junit.textui.TestRunner.run ( suite ( ) );
        }
        else
        {
            junit.swingui.TestRunner.run ( AutoTest.class );
        }
    }
}
