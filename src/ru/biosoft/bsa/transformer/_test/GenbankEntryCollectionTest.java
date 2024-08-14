package ru.biosoft.bsa.transformer._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.FileEntryCollectionTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;

/**
 * @todo LOW Comment
 */
public class GenbankEntryCollectionTest extends FileEntryCollectionTest
{
    private static String SEQUENCES_PATH = "../data_resources/_test/sequences";

    @Override
    protected int getOriginalSize()
    {
        return 1; // number of entries in data file
    }

    /**
     *
     */
    @Override
    protected String getOriginalName()
    {
        return "genbank";
    }

    /**
     *
     */
    @Override
    protected void setUp() throws Exception
    {
        File configFile = new File( "./ru/biosoft/bsa/_test/LocalRepositoryTest.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }

        try( FileInputStream stream = new java.io.FileInputStream(
                SEQUENCES_PATH + "/genbank.format" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX ) )
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
        return new TestSuite(GenbankEntryCollectionTest.class);
    }

    /**
     *
     */
    public static void main( String[] args )
    {
        junit.swingui.TestRunner.run(GenbankEntryCollectionTest.class);
    }
}
