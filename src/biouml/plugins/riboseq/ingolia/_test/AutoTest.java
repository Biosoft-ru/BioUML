package biouml.plugins.riboseq.ingolia._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public AutoTest(String name)
    {
        super( name );
    }
    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );

        suite.addTestSuite( TestAlignmentConverter.class );
        suite.addTestSuite( TestEnsemblTranscriptsProvider.class );
        suite.addTestSuite( TestASiteBuilder.class );
        suite.addTestSuite( TestBuildASiteOffsetTable.class );
        suite.addTestSuite( TestStartSiteReadsCounter.class );

        return suite;
    }
}
