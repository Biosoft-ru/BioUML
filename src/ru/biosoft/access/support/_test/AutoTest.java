package ru.biosoft.access.support._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Batch unit test for ru.biosoft.access.support
 */
public class AutoTest extends TestCase
{
    
    /** Standart JUnit constructor */
    public AutoTest ( String name )
    {
        super ( name );
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );
        suite.addTestSuite ( TestSetPropertyCommand.class );
        suite.addTest ( new MultyChoicePaneTest ( "testInitData" ) );
        
        return suite;
    }
    
}