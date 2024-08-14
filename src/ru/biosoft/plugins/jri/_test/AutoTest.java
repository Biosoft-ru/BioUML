package ru.biosoft.plugins.jri._test;

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
        suite.addTestSuite(RDirectTest.class);
        suite.addTestSuite(TestRLexer.class);
        return suite;
    }
}
