package ru.biosoft.server.servlets.webservices._test;

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

        suite.addTestSuite(TestHTTPServer.class);
        suite.addTestSuite(TestWebDiagramsProvider.class);
        suite.addTestSuite(TestWebActionsProvider.class);
        suite.addTestSuite(TestWebScriptsProvider.class);
        suite.addTestSuite(TestWebTablesProvider.class);
        suite.addTestSuite(TestWebProviderSupport.class);
        suite.addTestSuite(TestContentProvider.class);
        suite.addTestSuite(TestImportProvider.class);
        suite.addTestSuite(TestExportProvider.class);
        suite.addTestSuite(TestWebBeanProvider.class);
        suite.addTestSuite(TestPerspectivesProvider.class);

        return suite;
    }
}
