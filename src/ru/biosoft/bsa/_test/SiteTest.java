package ru.biosoft.bsa._test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;

/**
 * Site test.
 */
public class SiteTest extends TestCase
{
    protected SiteImpl site;

    public static final String TEST_SEQ = "aattggcc";
    //public static final String TEST_SEQ_INV = "ccggttaa";
    private static final int START = 1;
    private static final int LENGTH = 2;
    private static final int STRAND = Site.STRAND_PLUS;
    //private Sequence sequence = new LinearSequence( seqString.getBytes(), Nucleotide5LetterAlphabet.getInstance() );
    private static final Object SOURCE = new Object();

    /**
     * Standard JUnit constructor
     */
    public SiteTest( String name )
    {
        super(name);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utility
    //

    private Sequence createSequence(String seqString)
    {
        return new LinearSequence( seqString, Nucleotide5LetterAlphabet.getInstance() );
    }

    /**
     *  Prepare to test case running.
     */
    @Override
    protected void setUp() throws Exception
    {
        site = new SiteImpl(null, "test_seq", START, LENGTH, STRAND, createSequence(TEST_SEQ) );
    }

    public void checkSite(int start, int length, int strand, int from, int to, String sequence, String canonicalSequence) throws Exception
    {
        assertEquals("start property", start, site.getStart());
        assertEquals("length property", length, site.getLength());
        assertEquals("srand property", strand, site.getStrand());
        assertEquals("from property", from, site.getFrom());
        assertEquals("to property", to, site.getTo());
        assertEquals("sequence property", createSequence(sequence), site.getSequence());
        assertEquals("canonicalSequence property", createSequence(canonicalSequence), site.getCanonicSequence());
    }

    ////////////////////////////////////////////////////////////////////////////
    // Tests
    //
    public void testInitial() throws Exception
    {
        checkSite(START, LENGTH, STRAND, 1, 2, "aa", "aa");
    }

    private PropertyChangeListener createPCL(final String name, final PropertyValue[] propertyVlaues)
    {
        PropertyChangeListener listener = new PropertyChangeListener()
        {
            int eventNum = 0;
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                TestCase.assertTrue(name + " PropertyChangeListener: not expected event occured", propertyVlaues.length > eventNum);
                TestCase.assertTrue(name + " PropertyChangeListener: " + propertyVlaues[eventNum].getPropertyName()
                        + " property values are not equals", propertyVlaues[eventNum].equals(evt));
                eventNum++;
            }
        };
        return listener;
    }

    public void testStartChangingToMinusOne() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site Start property", new PropertyVlaue[] {});
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setStart(-1);
            checkSite(START, LENGTH, STRAND, 1, 2, "aa", "aa");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testStartChangingToTen() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site Start property", new PropertyVlaue[]
//            {
//                new PropertyVlaue("start", new Integer(START), new Integer(8)),
//                new PropertyVlaue("from", null, null),
//                new PropertyVlaue("to", null, null),
//            });
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setStart(10);
            checkSite(8, 1, STRAND, 8, 8, "c", "c");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testLengthChangingToZero() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site Length property", new PropertyVlaue[]
//            {
//                new PropertyVlaue("length", new Integer(LENGTH), new Integer(1)),
//            });
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setLength(0);
            checkSite(START, 1, STRAND, 1, 1, "a", "a");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testLengthChangingToTen() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site Length property", new PropertyVlaue[]
//            {
//                new PropertyVlaue("length", new Integer(LENGTH), new Integer(8)),
//            });
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setLength(10);
            checkSite(START, 8, STRAND, 1, 8, "aattggcc", "aattggcc");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testFromChangingToOne() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site from property", new PropertyVlaue[] {} );
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setFrom(1);
            checkSite(START, LENGTH, STRAND, START, 2, "aa", "aa");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testFromChangingToZero() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site from property", new PropertyVlaue[] {});
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setFrom(0);
            checkSite(START, LENGTH, STRAND, START, 2, "aa", "aa");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testFromChangingToTen() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site from property", new PropertyVlaue[]
//            {

//                new PropertyVlaue("start", new Integer(START), new Integer(8)),
//                new PropertyVlaue("from", null, null),
//                new PropertyVlaue("to", null, null),
//            });
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setFrom(10);
            checkSite(8, 1, STRAND, 8, 8, "c", "c");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testFromChangingToMoreThanOneLetters() throws Exception
    {
        site = new SiteImpl(null, "test_seq", 4, LENGTH, STRAND, createSequence(TEST_SEQ) );
        site.setFrom(1);
        checkSite(1, 5, STRAND, 1, 5, "aattg", "aattg");
    }


    public void testToChangingToOne() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site to property", new PropertyVlaue[]
//            {
//                new PropertyVlaue("to", new Integer(2), new Integer(8)),
//            });
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setTo(1);
            checkSite(START, 1, STRAND, 1, 1, "a", "a");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testToChangingToZero() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site to property", new PropertyVlaue[]
//            {
//                new PropertyVlaue("to", new Integer(2), new Integer(8)),
//            });
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setTo(0);
            checkSite(START, 1, STRAND, 1, 1, "a", "a");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testToChangingToTen() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site to property", new PropertyVlaue[]
//            {
//                new PropertyVlaue("to", new Integer(2), new Integer(8)),
//            });
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setTo(10);
            checkSite(START, 8, STRAND, 1, 8, "aattggcc", "aattggcc");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    public void testSrandChangingToMinus() throws Exception
    {
//        PropertyChangeListener listener = createPCL("Site srand property", new PropertyVlaue[]
//            {
//                new PropertyVlaue("srand", new Integer(2), new Integer(8)),
//            });
//        site.addPropertyChangeListener(listener);
        try
        {
            site.setStrand(Site.STRAND_MINUS);
            checkSite(2, LENGTH, Site.STRAND_MINUS, 1, 2, "tt", "aa");
        }
        finally
        {
//            site.removePropertyChangeListener(listener);
        }
    }

    /**
     *  Create test suite.
     */
    static public junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( SiteTest.class );
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
        if ( args != null && args.length>0 && args[0].startsWith( "text" ) )
            { junit.textui.TestRunner.run( suite() ); }
        else { junit.swingui.TestRunner.run( SiteTest.class ); }
    }


    @SuppressWarnings ( "serial" )
    public static class PropertyValue extends PropertyChangeEvent
    {
        public PropertyValue(String propertyName, Object oldValue, Object newValue)
        {
            super(SOURCE, propertyName, oldValue, newValue);
        }

        @Override
        public int hashCode()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj)
        {
            if ( this == obj ) return true;
            if (obj instanceof PropertyChangeEvent) return false;
            if (getPropertyName()!=null && getPropertyName().equals(((PropertyChangeEvent)obj).getPropertyName()))
            {
                if ((getOldValue() == null && ((PropertyValue)obj).getOldValue() == null)
                        || (getOldValue() != null && getOldValue().equals(((PropertyValue)obj).getOldValue())))
                {
                    if ((getNewValue() == null && ((PropertyValue)obj).getNewValue() == null)
                            || (getNewValue() != null && getNewValue().equals(((PropertyValue)obj).getNewValue())))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}