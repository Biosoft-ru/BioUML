package biouml.plugins.keynodes._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );
        suite.addTest( KeyNodesTest.suite() );

        suite.addTest( KeyNodeAnalysisTest.suite() );
        suite.addTest( LongestChainFinderTest.suite() );
        suite.addTest( SaveHitsAnalysisTest.suite() );
        suite.addTest( SaveNetworkAnalysisTest.suite() );
        suite.addTest( KeyNodeVisualizationTest.suite() );
        suite.addTest( ShortestPathClusteringTest.suite() );
        suite.addTest( AllPathClusteringTest.suite() );
        suite.addTest( ShortestPathBetweenSetsFinderTest.suite() );

        return suite;
    }
}
