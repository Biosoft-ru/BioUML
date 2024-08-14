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
public class GbEmblEntryCollectionTest extends FileEntryCollectionTest
{

    private static String SEQUENCES_PATH = "../data_resources/_test/sequences";

    @Override
    protected int getOriginalSize()
    {
        return 1; // number of entries in data file
    }

    @Override
    protected String getOriginalName()
    {
        return "gbembl";
    }

    @Override
    protected void setUp() throws Exception
    {
        try( FileInputStream stream = new java.io.FileInputStream(
                SEQUENCES_PATH + "/gbembl.format" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX ) )
        {
            properties.load( stream );
        }

        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCES_PATH);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, SEQUENCES_PATH);
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
        dataCollection = null;
    }

    /** @todo Test names and name list */

    /**
     *
     */
    public static Test suite()
    {
//        TestSuite suite = new TestSuite();
//        suite.addTest( new GbEmblEntryCollectionTest("testGetSize") );
//        return suite;
        return new TestSuite(GbEmblEntryCollectionTest.class);
    }

    /**
     *
     */
    public static void main( String[] args )
    {
        junit.swingui.TestRunner.run(GbEmblEntryCollectionTest.class);
    }
}