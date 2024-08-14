package ru.biosoft.util._test;

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

        suite.addTestSuite(TestCache.class);
        suite.addTestSuite(TestFieldMap.class);
        suite.addTestSuite(TestArchives.class);
        suite.addTestSuite(DPSPropertiesTest.class);
        suite.addTestSuite(BeanUtilTest.class);
        suite.addTestSuite(SizeLimitInputStreamTest.class);
        suite.addTestSuite(LimitedTextBufferTest.class);
        suite.addTestSuite(HtmlUtilTest.class);
        suite.addTestSuite(RhinoUtilsTest.class);
        suite.addTestSuite(CollectionsTest.class);
        suite.addTestSuite(ColorUtilsTest.class);
        suite.addTestSuite(TextUtilTest.class);
        suite.addTestSuite(TestListUtil.class);
        suite.addTestSuite(LazyStringsTest.class);
        suite.addTestSuite(TestJsonUtils.class);
        suite.addTestSuite( TestBeanAsMapUtil.class );
        suite.addTestSuite( ExPropertiesTest.class );
        // Add new tests here ...

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
