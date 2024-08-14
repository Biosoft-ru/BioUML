package ru.biosoft.bsa.transformer._test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.util.ExProperties;

/**
 * @todo testCase for check VetoException thrown in aggregated collection.
 */
public class SequenceQuerySystemTest extends TestCase
{
    private static String SEQUENCES_PATH = "../data_resources/_test/sequences";
    /**
     * Standard JUnit constructor
     */
    public SequenceQuerySystemTest( String name )
    {
        super(name);
    }


    ////////////////////////////////////////////////////////////////////////////
    // Utility
    //

    protected static final boolean isUnix = System.getProperty( "line.separator" ).length() == 1;

    protected static final String emblConfigFile = SEQUENCES_PATH + "/embl.node.config";

    protected DataCollection loadDataCollection(String configPath) throws Exception
    {
        ExProperties properties = new ExProperties(new File(configPath));
        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY,  SEQUENCES_PATH);
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY,    SEQUENCES_PATH);
        properties.put(DataCollectionConfigConstants.CONFIG_FILE_PROPERTY,  "embl.node.config");

        return CollectionFactory.createCollection(null, properties);
    }

    protected void checkSequence(DataCollection dc, Index index, String name,
                                 int seqLen, int offsetWin, int offsetUnix) throws Exception
    {
        dc.get(name);

        Index.IndexEntry  entry = (Index.IndexEntry)index.get(name);
        assertNotNull("Entry '" + name + "' not found",entry );
        assertEquals( "Wrong length for '" + name + "' sequence", seqLen, entry.len);
        assertEquals( "Wrong start of '" + name + "' sequence",   isUnix ? offsetUnix : offsetWin, entry.from);
    }

    ////////////////////////////////////////////////////////////////////////////

    public void testMakeEmblConfigFile() throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( emblConfigFile ); PrintWriter pw = new PrintWriter( fos ))
        {
            pw.println( "name = embl\n" +
                    "class = ru.biosoft.access.core.TransformedDataCollection\n" +
                    "transformer  = ru.biosoft.bsa.transformer.EmblTransformer\n" +
                    "nextConfig   = embl.format.config\n" +
                    "node-image  =  sequenceSet.gif\n" +
                    "childrenNodeImage = sequence.gif\n");
        }
    }

    /**
     * Tests whether SequenceQuerySystem was added to config file.
     */
    public void testUpdateConfigFile() throws Exception
    {
        loadDataCollection(emblConfigFile);

        Properties properties = new ExProperties(new File(emblConfigFile));

        assertNotNull("query system was not added to config file",
                      properties.getProperty(QuerySystem.QUERY_SYSTEM_CLASS));

        // configPath is added by ExProperties constructor, thus 8
        assertNotNull("config path presents",
                properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY));
        assertEquals("Number of properties in config file", 8, properties.size());
    }

    public void testFastaSequence() throws Exception
    {
        File indexFile = new File(SEQUENCES_PATH + "/fasta.seq.idx");
        indexFile.delete();
        indexFile = new File(SEQUENCES_PATH + "/fasta.seq.id");
        indexFile.delete();

        ApplicationUtils.copyFile( new File(SEQUENCES_PATH, "fasta.seq"), new File(SEQUENCES_PATH, "fasta.seq.orig") );
        DataCollection dc = loadDataCollection(SEQUENCES_PATH + "/fasta.node.config");
        QuerySystem querySystem = dc.getInfo().getQuerySystem();
        Index index = querySystem.getIndex("sequence");

        checkSequence(dc, index, "17036", 220, 1, 1);
        checkSequence(dc, index, "11070", 220, 1, 1);
        checkSequence(dc, index, "15029", 220, 1, 1);
        checkSequence(dc, index, "16037", 220, 1, 1);

        dc.close();
    }

    public void testEmblSequence() throws Exception
    {
        File indexFile = new File(SEQUENCES_PATH + "/embl.seq.idx");
        indexFile.delete();
        indexFile = new File(SEQUENCES_PATH + "/embl.seq.id");
        indexFile.delete();

        ApplicationUtils.copyFile( new File(SEQUENCES_PATH, "embl.seq"), new File(SEQUENCES_PATH, "embl.seq.orig") );
        DataCollection dc = loadDataCollection(SEQUENCES_PATH + "/embl.node.config");
        QuerySystem querySystem = dc.getInfo().getQuerySystem();
        Index index = querySystem.getIndex("sequence");

        checkSequence(dc, index, "ADHBADA2", 1145, 62, 62);
        checkSequence(dc, index, "AGHBD",    1959, 41, 41);
        checkSequence(dc, index, "AMCHS",    3574, 67, 67);
        checkSequence(dc, index, "AMDEFA",   4269, 96, 96);
    }

    public void testGenbankSequence() throws Exception
    {
        File indexFile = new File(SEQUENCES_PATH + "/genbank.seq.idx");
        indexFile.delete();
        indexFile = new File(SEQUENCES_PATH + "/genbank.seq.id");
        indexFile.delete();

        ApplicationUtils.copyFile( new File(SEQUENCES_PATH, "genbank.seq"), new File(SEQUENCES_PATH, "genbank.seq.orig") );
        DataCollection dc = loadDataCollection(SEQUENCES_PATH + "/genbank.node.config");
        QuerySystem querySystem = dc.getInfo().getQuerySystem();
        Index index = querySystem.getIndex("sequence");

        checkSequence(dc, index, "Hs21_1835", 281116, 1144, 1144);
    }


    /**
     *  Create test suite.
     */
    static public Test suite()
    {
//        TestSuite suite = new TestSuite( SequenceQuerySystemTest.class );

        TestSuite suite = new TestSuite("SequenceQuerySystemTest");
        suite.addTest(new SequenceQuerySystemTest("testMakeEmblConfigFile"));
        suite.addTest(new SequenceQuerySystemTest("testUpdateConfigFile"));

        suite.addTest(new SequenceQuerySystemTest("testFastaSequence"));
        suite.addTest(new SequenceQuerySystemTest("testEmblSequence"));
        suite.addTest(new SequenceQuerySystemTest("testGenbankSequence"));

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
        else { junit.swingui.TestRunner.run( SequenceQuerySystemTest.class ); }
    }
}