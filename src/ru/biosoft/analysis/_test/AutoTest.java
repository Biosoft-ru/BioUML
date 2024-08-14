package ru.biosoft.analysis._test;

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

        suite.addTestSuite(BinomialTest.class);
        //suite.addTestSuite(ClusterAnalysisTest.class);
        suite.addTestSuite(HypergeometricAnalysisTest.class);
        suite.addTestSuite(PoissonTest.class);
        suite.addTestSuite(HypergeometricTest.class);
        suite.addTestSuite(CorrelationTest.class);
        suite.addTestSuite(FoldChangeTest.class);
        suite.addTestSuite(SplineTest.class);
        suite.addTestSuite(WilcoxonTest.class);
        suite.addTestSuite(StatTest.class);
        suite.addTestSuite(GammaTest.class);
        suite.addTestSuite(DensityEstimationTest.class);
        
        //Test suites for the simple methods from {@link ru.biosoft.analysis.Util}
        suite.addTestSuite(GaussJordanEliminationTest.class);

        suite.addTestSuite(AnalysesTest.class);
        
        suite.addTest( TestSuperAnnotateTable.suite() );
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
