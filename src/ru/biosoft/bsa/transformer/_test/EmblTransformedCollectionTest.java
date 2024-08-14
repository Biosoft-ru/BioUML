package ru.biosoft.bsa.transformer._test;

import java.io.File;
import java.io.FileInputStream;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.util.ExProperties;

/**
 * @todo testCase for check VetoException thrown in aggregated collection.
 */
public class EmblTransformedCollectionTest extends ContainedSiteTransformedCollectionTest
{
    public static final String SEQUENCE_PATH = "../data_resources/_test/sequences";

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
        return "embl";
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

    ////////////////////////////////////////////////////////////////////////////
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
        ApplicationUtils.copyFile(SEQUENCE_PATH + "/embl.node" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX, SEQUENCE_PATH + "/embl.node"
                + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX + ".orig");
        
        ExProperties prop = new ExProperties(new File(SEQUENCE_PATH, "embl.node" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX));

        prop.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCE_PATH);
        prop.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, SEQUENCE_PATH);

        // entry
        try( FileInputStream stream = new java.io.FileInputStream( SEQUENCE_PATH + "/embl.format" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX ) )
        {
            properties.load( stream );
        }

        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, SEQUENCE_PATH);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, SEQUENCE_PATH);
        restoreFiles();

        TransformedDataCollection tdc = (TransformedDataCollection)CollectionFactory.createCollection( null,prop );
        setDataCollection( tdc );   //transformed
        setAggDataCollection( tdc.getPrimaryCollection() );
    }

    public void testEmblTransformer() throws Exception
    {
        checkTransformer("ADHBADA2","../data_resources/_test/embl_ADHBADA2.txt");
    }

    public void testSequenceSizes() throws Exception
    {
        AnnotatedSequence ss;
        ss = (AnnotatedSequence)dataCollection.get("ADHBADA2");
        assertNotNull("Entry 'ADHBADA2' not found",ss );
        Sequence seq = ss.getSequence();
        assertNotNull("Sequence for entry 'ADHBADA2' not found",seq );
        assertEquals( "Wrong length of 'ADHBADA2' sequence",1145,seq.getLength() );
        ss = (AnnotatedSequence)dataCollection.get("AGHBD");
        assertNotNull("Entry 'AGHBD' not found",ss );
        seq = ss.getSequence();
        assertNotNull("Sequence for entry 'AGHBD' not found",seq );
        assertEquals( "Wrong length of 'AGHBD' sequence",1959,seq.getLength() );
        ss = (AnnotatedSequence)dataCollection.get("AMCHS");
        assertNotNull("Entry 'AMCHS' not found",ss );
        seq = ss.getSequence();
        assertNotNull("Sequence for entry 'AMCHS' not found",seq );
        assertEquals( "Wrong length of 'AMCHS' sequence",3574,seq.getLength() );
        ss = (AnnotatedSequence)dataCollection.get("AMDEFA");
        assertNotNull("Entry 'AMDEFA' not found",ss );
        seq = ss.getSequence();
        assertNotNull("Sequence for entry 'AMDEFA' not found",seq );
        assertEquals( "Wrong length of 'AMDEFA' sequence",4269,seq.getLength() );
    }

    public static Test suite()
    {
        return new TestSuite(EmblTransformedCollectionTest.class);
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
        else { junit.swingui.TestRunner.run( EmblTransformedCollectionTest.class ); }
    }
}
