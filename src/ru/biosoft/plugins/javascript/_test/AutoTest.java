package ru.biosoft.plugins.javascript._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public AutoTest( String name )
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );
        suite.addTestSuite(TestJSVisiblePlugin.class);
        suite.addTestSuite( TestJSElement.class );
        suite.addTestSuite( TestGlobal.class );
        return suite;
    }
}