package ru.biosoft.bsa.transformer._test;

import java.io.FileInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.FileEntryCollectionTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;

/**
 *
 */
public class FastaEntryCollectionTest extends FileEntryCollectionTest
{
    public static final String SEQUENCE_PATH = "../data_resources/_test/sequences";

    @Override
    protected int getOriginalSize()
    {
        return 4; // number of entries in data file
    }

    @Override
    protected String getOriginalName()
    {
        return "fasta";
    }

    @Override
    protected void setUp() throws Exception
    {
        try( FileInputStream stream = new java.io.FileInputStream(
                SEQUENCE_PATH + "/fasta.format" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX ) )
        {
            properties.load( stream );
        }

        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCE_PATH);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY,   SEQUENCE_PATH);
        properties.put(DataCollectionConfigConstants.CONFIG_FILE_PROPERTY, SEQUENCE_PATH + "/fasta.format");
        restoreFiles();

        setDataCollection( CollectionFactory.createCollection(null,properties ));
    }

    /**
     *
     */
    @Override
    protected void tearDown() throws Exception
    {
        dataCollection.close();
//        setDataCollection( null );
    }

    /** @todo Test names and name list */

    /**
     *
     */
    public static Test suite()
    {
//        TestSuite suite = new TestSuite();
//        suite.addTest( new FastaEntryCollectionTest("testPutRemove") );
//        return suite;
        return new TestSuite(FastaEntryCollectionTest.class);
    }




    /**
     *
     */
    public static void main( String[] args )
    {
        junit.swingui.TestRunner.run(FastaEntryCollectionTest.class);
//        junit.textui.TestRunner.run(FastaEntryCollectionTest.class);
    }
}