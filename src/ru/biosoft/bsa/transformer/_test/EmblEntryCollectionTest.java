package ru.biosoft.bsa.transformer._test;

import java.io.FileInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.FileEntryCollectionTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;

public class EmblEntryCollectionTest extends FileEntryCollectionTest
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
        return "embl";
    }

    @Override
    protected void setUp() throws Exception
    {
        System.gc();
        try( FileInputStream stream = new java.io.FileInputStream( SEQUENCE_PATH + "/embl.format" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX ) )
        {
            properties.load( stream );
        }
        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCE_PATH);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, SEQUENCE_PATH);
        restoreFiles();
        setDataCollection( CollectionFactory.createCollection(null,properties ));
    }

    @Override
    protected void tearDown() throws Exception
    {
        dataCollection.close();
        setDataCollection( null );
    }

    public static Test suite()
    {
        return new TestSuite(EmblEntryCollectionTest.class);
    }
}