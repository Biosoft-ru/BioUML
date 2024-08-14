package ru.biosoft.bsa.transformer._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

public class AutoTest extends TestCase
{
    /** Make suite of tests. */
    public static Test suite()
    {
        File configFile = new File( "./ru/biosoft/bsa/transformer/_test/test.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }

        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTest(SequenceQuerySystemTest.suite());
        suite.addTest(SiteQuerySystemTest.suite());

        suite.addTest(FastaEntryCollectionTest.suite());
        suite.addTest(FastaTransformedCollectionTest.suite());

        suite.addTest(EmblEntryCollectionTest.suite());
        suite.addTest(EmblTransformedCollectionTest.suite());

        suite.addTest(GenbankEntryCollectionTest.suite());
        suite.addTest(GenbankTransformedCollectionTest.suite());

        suite.addTest(GbEmblEntryCollectionTest.suite());
        suite.addTest(GbEmblTransformedCollectionTest.suite());

        suite.addTest(EmblTrackTransformerTest.suite());

        suite.addTestSuite( ProjectTransformerTest.class );

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
