package biouml.plugins.riboseq._test;

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
        super( name );
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );

        suite.addTestSuite( BAMSitesTest.class );
        suite.addTestSuite( BEDSitesTest.class );
        suite.addTestSuite( SiteClusterTest.class );
        suite.addTestSuite( ChromosomeClustersTest.class );
        suite.addTestSuite( RiboSeqAnalysisTest.class );
        suite.addTestSuite( SelectionTrustClustersAnalysisTest.class );
        //suite.addTestSuite( ComparatorAnalysisTest.class );
        suite.addTestSuite( TestTranscriptomeMappability.class );
        suite.addTestSuite( TestSequenceMappability.class );
        suite.addTestSuite( TestCountReadsInTranscripts.class );

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
        if( args != null && args.length > 0 && args[0].startsWith( "text" ) )
        {
            junit.textui.TestRunner.run( suite() );
        }
        else
        {
            junit.swingui.TestRunner.run( AutoTest.class );
        }
    }
} // end of AutoTest
