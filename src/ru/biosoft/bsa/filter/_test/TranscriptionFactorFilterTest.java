package ru.biosoft.bsa.filter._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.MutableFilter;
import ru.biosoft.bsa.filter.TranscriptionFactorFilter;


/**
 *
 */
public class TranscriptionFactorFilterTest extends TestCase
{
    /** Standart JUnit constructor */
    public TranscriptionFactorFilterTest( String name )
    {
        super( name );
    }

    public void testFiltersNotStatic()
    {
        TranscriptionFactorFilter tff1 = new TranscriptionFactorFilter(null,null);
        Filter[] filters1 = tff1.getFilter();
        assertTrue( "filters1 array can't be empty",filters1.length>0 );
        TranscriptionFactorFilter tff2 = new TranscriptionFactorFilter(null,null);
        Filter[] filters2 = tff2.getFilter();
        assertTrue( "filters2 array can't be empty",filters2.length>0 );
        assertEquals( "filters1 differs from filters2 array",filters1.length,filters1.length );
        for( int i=0; i<filters1.length; i++ )
        {
            assertTrue( "Filter["+i+"] can't be same. "+filters1[i].getClass().getName(),filters1[i]!=filters2[i] );
        }
    }

    public void testTaxonFilter()
    {
        TranscriptionFactorFilter tff1 = new TranscriptionFactorFilter(null,null);
        TranscriptionFactorFilter tff2 = new TranscriptionFactorFilter(null,null);
        Filter[] filters1 = tff1.getFilter();
        assertTrue( "filters1 array can't be empty",filters1.length>0 );
        Filter[] filters2 = tff2.getFilter();
        assertTrue( "filters2 array can't be empty",filters2.length>0 );
        MutableFilter filter1 = (MutableFilter)filters1[0];
        MutableFilter filter2 = (MutableFilter)filters2[0];
        filter2.setEnabled( true );
        try
        {
            tff1.isAcceptable( null );
        }
        catch( Exception exc )
        {
            fail( "ttf1 should be disabled and no exception should be thrown" );
        }
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TranscriptionFactorFilterTest.class.getName());

        suite.addTest(new TranscriptionFactorFilterTest("testFiltersNotStatic"));
        suite.addTest(new TranscriptionFactorFilterTest("testTaxonFilter"));
      
        return suite;
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main( String[] args )
    {
        if ( args != null && args.length!=0 && args[ 0 ].startsWith( "text" ) )
            { junit.textui.TestRunner.run( suite() ); }
        else { junit.swingui.TestRunner.run( TranscriptionFactorFilterTest.class ); }
    }
} // end of TranscriptionFactorFilterTest
