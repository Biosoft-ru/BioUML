package biouml.plugins.kegg.type.access._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 */
public class AutoTest extends TestCase
{
    /** Standart JUnit constructor */
    public AutoTest( String name )
    {
        super( name );
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());
        suite.addTestSuite( TestCompoundTransformer.class );
        suite.addTestSuite( TestEnzymeTransformer.class );
        suite.addTestSuite( TestGlycanTransformer.class );
        suite.addTestSuite( TestReactionTransformer.class );
        suite.addTestSuite( TestOrthologTransformer.class );
        return suite;
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main( String[] args )
    {
        if ( args != null && args.length!=0 && args[ 0 ].startsWith( "text" ) )
            { junit.textui.TestRunner.run( suite() ); }
        else { junit.swingui.TestRunner.run( AutoTest.class ); }
    }
} // end of AutoTest