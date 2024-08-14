package ru.biosoft.bsa.transformer._test;

import java.io.File;
import java.io.FileInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.ExProperties;

/**
 * @todo testCase for check VetoException thrown in aggregated collection.
 */
public class GenbankTransformedCollectionTest extends ContainedSiteTransformedCollectionTest
{
    private static String SEQUENCES_PATH = "../data_resources/_test/sequences";

    /**
     * Returns original (needed) size of data collection.
     * @return Original size of data collection.
     */
    @Override
    public int getOriginalSize()
    {
        return 1;
    }

    /**
     * Returns original (needed) name of data collection.
     * @return Original name of data collection.
     */
    @Override
    public String getOriginalName()
    {
        return "genbank";
    }

    @Override
    public Class getOutputClass()
    {
        return AnnotatedSequence.class;
    }

    @Override
    public Class getInputClass()
    {
        return Entry.class;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Utility
    //
    /**
     *  Prepare to test case running.
     *  Restore all data files. Delete index file.
     *  Create TransformedDataCollection and data collection that used by it.
     *  Add ru.biosoft.access.core.DataCollectionListener to aggregated data collection.
     */
    @Override
    protected void setUp() throws Exception
    {
        ExProperties prop = new ExProperties(new File(SEQUENCES_PATH, "genbank.node"+DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX)); // transformed

        prop.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCES_PATH);
        prop.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, SEQUENCES_PATH);

        // entry
        try( FileInputStream stream = new FileInputStream( SEQUENCES_PATH + "/genbank.format" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX ) )
        {
            properties.load( stream );
        }

        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCES_PATH);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, SEQUENCES_PATH);
        restoreFiles();

        TransformedDataCollection tdc = (TransformedDataCollection)CollectionFactory.createCollection(null, prop);
        setDataCollection(tdc); //transformed
        setAggDataCollection(tdc.getPrimaryCollection());
    }

    /**
     *  Create test suite.
     */
    static public Test suite()
    {
        TestSuite suite = new TestSuite("GenbankTransformedCollectionTest");
        //        suite.addTest(new GenbankTransformedCollectionTest("testTransformer"));
        suite.addTestSuite(GenbankTransformedCollectionTest.class);
        return suite;
    }

    @Override
    public void compare(DataElement di1, DataElement di2) throws Exception
    {
        AnnotatedSequence ss1 = (AnnotatedSequence)di1;
        AnnotatedSequence ss2 = (AnnotatedSequence)di2;
        assertEquals("Maps has different sizes.", ss1.getSize(), ss2.getSize());
        checkProperties(ss1.getProperties(), ss2.getProperties());
        for(Track track1 : ss1)
        {
            String name = track1.getName();
            Track track2 = ss2.get(name);

            DataCollection<Site> track2Sites = track2.getAllSites();

            for(Site site1 : track1.getAllSites())
            {
                String name2 = site1.getName();
                Site site2 = track2Sites.get(name2);

                assertNotNull("site with name <" + name + "> not found in di1.", site1);
                assertNotNull("site with name <" + name + "> not found in di2.", site2);
                //assertEquals("Wrong start in " + name, site1.getStart(), site2.getStart());
                //assertEquals("Wrong length", site1.getLength(), site2.getLength());
                //assertEquals("Wrong precision", site1.getPrecision(), site2.getPrecision());
                //assertEquals("Wrong strand", site1.getStrand(), site2.getStrand());
                //assertEquals("Wrong type", site1.getType(), site2.getType());
                //checkProperties(site1.getProperties(), site2.getProperties());
            }
        }

        // check of sequences
        Sequence seq1 = ss1.getSequence();
        Sequence seq2 = ss2.getSequence();

        assertEquals(seq1.isCircular(), seq2.isCircular());
        assertEquals(seq1.getLength(), seq2.getLength());

        for( int i = 1; i <= seq1.getLength(); i++ )
            assertEquals(seq1.getLetterAt(i), seq2.getLetterAt(i));
    }

    public void testGenbankTransformer() throws Exception
    {
        checkTransformer("Hs21_1835", "../data_resources/_test/genbank_Hs21_1835.txt");
    }

    public void testSequenceSizes() throws Exception
    {
        AnnotatedSequence ss;
        ss = (AnnotatedSequence)dataCollection.get("Hs21_1835");
        assertNotNull("Entry 'Hs21_1835' not found", ss);
        Sequence seq = ss.getSequence();
        assertNotNull("Sequence for entry 'Hs21_1835' not found", seq);
        assertEquals("Wrong length of 'Hs21_1835' sequence", 281116, seq.getLength());
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
            junit.swingui.TestRunner.run(GenbankTransformedCollectionTest.class);
        }
    }
}
