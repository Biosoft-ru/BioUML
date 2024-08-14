package ru.biosoft.access._test;

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

        suite.addTestSuite(TestExceptionHandling.class);
        suite.addTestSuite(TestOsgiManifestParser.class);
        suite.addTestSuite(BasicDataElements.class);
        suite.addTestSuite(VectorDataCollectionTest.class);
        suite.addTestSuite(TestGenericDataCollection.class);
        suite.addTestSuite(TestScriptTypeRegistry.class);
        suite.addTestSuite(DataElementPathTest.class);
        suite.addTestSuite(SymbolicLinksTest.class);
        suite.addTestSuite(FilteredDataCollectionTest.class);
        suite.addTestSuite(ZipHtmlDataCollectionTest.class);
        suite.addTestSuite(ImageConnectionTest.class);
        suite.addTestSuite(DataCollectionUtilsTest.class);
        suite.addTest(JDBM2IndexTest.suite());
        suite.addTest(BTreeIndexTest.suite());
        suite.addTestSuite( TestLogging.class );
        suite.addTestSuite( TestLogScriptEnvironment.class );

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
