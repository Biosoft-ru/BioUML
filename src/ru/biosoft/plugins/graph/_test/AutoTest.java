package ru.biosoft.plugins.graph._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );
        suite.addTestSuite ( TestHierarchicLayouter.class );
        return suite;
    }
    
    /**
     * Run test in TestRunner. If args[0].startsWith("text") then textui runner
     * runs, otherwise swingui runner runs.
     * 
     * @param args
     *            Type of test runner.
     */
    public static void main ( String[] args )
    {
        if ( args != null && args.length != 0 && args[0].startsWith ( "text" ) )
            junit.textui.TestRunner.run ( AutoTest.class );
        else
            junit.swingui.TestRunner.run ( AutoTest.class );
    }
}
