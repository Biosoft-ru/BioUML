package ru.biosoft.galaxy._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Only automated (not visual) tests for nightly build should be included here.
 */
public class AutoTest extends TestCase
{
    /** Standard JUnit constructor */
    public AutoTest(String name)
    {
        super(name);
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTestSuite(TestGalaxyParsing.class);
        suite.addTestSuite( TestFormatRegistry.class );
        suite.addTestSuite( TestGalaxyMacro.class );

        return suite;
    }
}