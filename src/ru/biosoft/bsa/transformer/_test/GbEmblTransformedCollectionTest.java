package ru.biosoft.bsa.transformer._test;

import java.io.File;
import java.io.FileInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.util.ExProperties;

/**
 *
 */
public class GbEmblTransformedCollectionTest extends ContainedSiteTransformedCollectionTest
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
        return "gbembl";
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
        ExProperties prop = new ExProperties(new File(SEQUENCES_PATH, "gbembl.node" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX));
        prop.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCES_PATH);
        prop.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, SEQUENCES_PATH);

        // entry
        try( FileInputStream stream = new java.io.FileInputStream(
                SEQUENCES_PATH + "/gbembl.format" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX ) )
        {
            properties.load( stream );
        }
        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCES_PATH);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, SEQUENCES_PATH);

        restoreFiles();

        TransformedDataCollection tdc = (TransformedDataCollection)CollectionFactory.createCollection( null,prop );
        setDataCollection( tdc );   //transformed
        setAggDataCollection( tdc.getPrimaryCollection() );
    }

    /**
     *  Create test suite.
     */
    static public Test suite()
    {
        TestSuite suite = new TestSuite( "GbEmblTransformedCollectionTest" );
//        suite.addTest(new GbEmblTransformedCollectionTest("testPutRemove"));
        suite.addTestSuite( GbEmblTransformedCollectionTest.class );
        return suite;
    }

    /**
     * @todo Implement.
     */
    @Override
    public void compare(DataElement de1, DataElement de2)
    {
    }
}