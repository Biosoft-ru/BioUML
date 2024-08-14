package ru.biosoft.bsa._test;

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

        suite.addTestSuite(LongFastaTest.class);
        suite.addTestSuite(AlphabetTest.class);
        suite.addTestSuite(SiteTest.class);
        suite.addTestSuite(SequenceRegionTest.class);
        suite.addTestSuite(CircularSequenceTest.class);
        suite.addTestSuite(SlicedSequenceTest.class);
        suite.addTestSuite(ImportTest.class);
        suite.addTestSuite(ExportTest.class);
        suite.addTestSuite(FastaExporterTest.class);
        suite.addTestSuite(IntervalMapTest.class);
        suite.addTestSuite(TestInterval.class);
        suite.addTestSuite(TestGCContentTrack.class);
        suite.addTestSuite(TrackTest.class);
        suite.addTestSuite(BAMTest.class);
        suite.addTestSuite(TestIPSSiteModel.class);
        suite.addTestSuite(IntervalTrackImporterTest.class);
        suite.addTestSuite(IntervalTrackExporterTest.class);
        suite.addTestSuite( TestSqlTrack.class );

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
