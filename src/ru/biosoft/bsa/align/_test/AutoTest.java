package ru.biosoft.bsa.align._test;

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

        suite.addTestSuite(AlignmentTest.class);
        suite.addTestSuite(NeedlemanWunschTest.class);

        return suite;
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(AutoTest.class);
        }
    }
} // end of AutoTest
