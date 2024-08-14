package com.developmentontheedge.application._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Batch unit test for ru.biosoftgui package.
 */
public class Test extends TestCase
{
    /** Standart JUnit constructor */
    public Test( String name )
    {
        super( name );
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( Test.class.getName() );
        suite.addTest( AutoTest.suite() );
        // Add new tests here ...
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
        if ( args != null && args.length>0 && args[0].startsWith( "text" ) )
            { junit.textui.TestRunner.run( suite() ); }
        else { junit.swingui.TestRunner.run( Test.class ); }
    }
} // end of Test