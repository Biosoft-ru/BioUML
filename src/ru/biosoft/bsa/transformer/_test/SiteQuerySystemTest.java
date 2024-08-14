
package ru.biosoft.bsa.transformer._test;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.BTreeRangeIndex;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.Index.StringIndexEntry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.TrackImpl;
import ru.biosoft.bsa.transformer.SiteQuerySystem;
import ru.biosoft.util.TempFiles;

public class SiteQuerySystemTest extends AbstractBioUMLTest
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        dir = TempFiles.dir( "sites" );
    }
    
    protected File dir;

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        ApplicationUtils.removeDir( dir );
    }

    public SiteQuerySystemTest(String name)
    {
        super(name);
    }

    ////////////////////////////////////////////////////////////////////////////

    private static AnnotatedSequence map;

    public void testIndex() throws Exception
    {
        File testDir = new File(dir, "test");
        BTreeRangeIndex index = new BTreeRangeIndex(testDir, "test.id", testDir.getPath(), 1024);

        index.put(new BTreeRangeIndex.IntKey(1).serializeToString(), new StringIndexEntry("name_1"));
        index.put(new BTreeRangeIndex.IntKey(2).serializeToString(), new StringIndexEntry("name_2"));
        index.put(new BTreeRangeIndex.IntKey(3).serializeToString(), new StringIndexEntry("name_3"));
        
        index.close();
    }


    ////////////////////////////////////////////////////////////////////////////

    public void testCreateMap() throws Exception
    {
        LinearSequence seq = new LinearSequence( "agtc agtc agtc agtc", Nucleotide5LetterAlphabet.getInstance() );

        Properties props = new Properties();
        props.put( DataCollectionConfigConstants.NAME_PROPERTY, "testMap" );
        props.put( DataCollectionConfigConstants.CLASS_PROPERTY, MapAsVector.class.getName() );
        props.put(MapAsVector.SITE_SEQUENCE_PROPERTY, seq);

        props.put( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.getPath() );
        props.put(SiteQuerySystem.QUERY_SYSTEM_CLASS, SiteQuerySystem.class.getName());
        props.put(SiteQuerySystem.INDEX_BLOCK_SIZE, "1024");

        map = (AnnotatedSequence)CollectionFactory.createCollection(null, props);
        
        map.close();
        
        assertNotNull("CollectionInfo is null",  map.getInfo());
        assertNotNull("SiteQuerySystem is null", map.getInfo().getQuerySystem());

        SiteQuerySystem sqs = (SiteQuerySystem)map.getInfo().getQuerySystem();
        assertNotNull("From index is null",  sqs.getIndex(SiteQuerySystem.FROM));
        assertNotNull("To index is null",    sqs.getIndex(SiteQuerySystem.TO));
    }

    public void testAddSite() throws Exception
    {
        TrackImpl track = new TrackImpl("track 1", map);
        map.put(track);
        track.addSite(new SiteImpl(map, "site 1", 1, 5, 2, null));
    }

    ////////////////////////////////////////////////////////////////////////////

    /** Create test suite. */
    static public Test suite()
    {
        TestSuite suite = new TestSuite("SiteQuerySystemTest");

        suite.addTest(new SiteQuerySystemTest("testIndex"));
        suite.addTest(new SiteQuerySystemTest("testCreateMap"));
        suite.addTest(new SiteQuerySystemTest("testAddSite"));

        return suite;
    }

    /** Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner. */
    public static void main(String[] args)
    {
        if (args != null && args.length > 0 && args[0].startsWith("text"))
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(SiteQuerySystemTest.class);
        }
    }
}
