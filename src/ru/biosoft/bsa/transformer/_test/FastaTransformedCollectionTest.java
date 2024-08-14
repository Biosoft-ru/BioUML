package ru.biosoft.bsa.transformer._test;

import java.io.File;
import java.io.FileInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.Entry;
import ru.biosoft.access._test.TransformedCollectionTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.util.ExProperties;

public class FastaTransformedCollectionTest extends TransformedCollectionTest
{
    /**
    * Returns original (needed) size of data collection.
    * @return Original size of data collection.
    */
    @Override
    public int getOriginalSize()
    {
        return 4;
    }

    /**
     * Returns original (needed) name of data collection.
     * @return Original name of data collection.
     */
    @Override
    public String getOriginalName()
    {
        return "fasta";
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

    public static final String path = "../data_resources/_test/sequences";

    /**
     *  Prepare to test case running.
     *  Restore all data files. Delete index file.
     *  Create TransformedDataCollection and data collection that used by it.
     *  Add ru.biosoft.access.core.DataCollectionListener to aggregated data collection.
     */
    @Override
    protected void setUp() throws Exception
    {
        ExProperties prop = new ExProperties(new File(path, "fasta.node" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX));

        prop.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, path);
        prop.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, path);

        // entry
        try( FileInputStream stream = new java.io.FileInputStream( path + "/fasta.format" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX ) )
        {
            properties.load( stream );
        }
        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, path);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, path);

        restoreFiles();

        TransformedDataCollection tdc = (TransformedDataCollection)CollectionFactory.createCollection(null, prop);
        setDataCollection(tdc); //transformed
        setAggDataCollection(tdc.getPrimaryCollection());
    }

    /**
    *
    */
    public static Test suite()
    {
        return new TestSuite(FastaTransformedCollectionTest.class);
    }

    /**
     * @todo Rename this method.
     */
    public void testFastaTransformer() throws Exception
    {
        checkTransformer("17036", "../data_resources/_test/fasta_17036.txt");
    }

    public void testSequenceSizes() throws Exception
    {
        AnnotatedSequence ss;
        ss = (AnnotatedSequence)dataCollection.get("11070");
        assertNotNull("Entry '11070' not found", ss);
        Sequence seq = ss.getSequence();
        assertNotNull("Sequence for entry '11070' not found", seq);
        assertEquals("Wrong length of '11070' sequence", 220, seq.getLength());

        ss = (AnnotatedSequence)dataCollection.get("17036");
        assertNotNull("Entry '17036' not found", ss);
        seq = ss.getSequence();
        assertNotNull("Sequence for entry '17036' not found", seq);
        assertEquals("Wrong length of '17036' sequence", 220, seq.getLength());

        ss = (AnnotatedSequence)dataCollection.get("15029");
        assertNotNull("Entry '15029' not found", ss);
        seq = ss.getSequence();
        assertNotNull("Sequence for entry '15029' not found", seq);
        assertEquals("Wrong length of '15029' sequence", 220, seq.getLength());

        ss = (AnnotatedSequence)dataCollection.get("16037");
        assertNotNull("Entry '16037' not found", ss);
        seq = ss.getSequence();
        assertNotNull("Sequence for entry '16037' not found", seq);
        assertEquals("Wrong length of '16037' sequence", 220, seq.getLength());
    }

    /**
     * @todo Implement.
     */
    @Override
    public void compare(DataElement de1, DataElement de2)
    {
    }
}