package biouml.plugins.lucene._test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import biouml.plugins.lucene.biohub.IndexBuilder;
import biouml.plugins.lucene.biohub.LuceneBasedBioHub;
import biouml.standard.StandardQueryEngine;
import biouml.workbench.graphsearch.QueryEngine;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class BioHubTest extends AbstractBioUMLTest
{
    public static final String repositoryPath = "../data/test/biouml/plugins/lucene/biohub";
    public static final String hubPath = repositoryPath + "/hub";

    static DataCollection<?> module;

    public BioHubTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BioHubTest.class.getName());

        suite.addTest(new BioHubTest("testCreateIndex"));
        suite.addTest(new BioHubTest("testSimpleSearch"));
        suite.addTest(new BioHubTest("testSimpleSearch2"));
        suite.addTest(new BioHubTest("testDirectionSearch"));
        suite.addTest(new BioHubTest("testDirectionSearch2"));
        suite.addTest(new BioHubTest("testPathSearch"));
        suite.addTest(new BioHubTest("testRemoveIndexes"));

        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.unregisterAllRoot();
        DataCollection<?> root = CollectionFactory.createRepository(repositoryPath);
        assertNotNull("Can not load repository", root);

        DataElement moduleElement = root.get("biohub");
        assertNotNull("Can not load biohub test module", moduleElement);
        assertTrue("Can not load biohub test module", moduleElement instanceof DataCollection);
        module = (DataCollection<?>)moduleElement;

        File hubRepository = new File(hubPath);
        if( hubRepository.exists() )
        {
            hubRepository.delete();
        }
    }

    public void testCreateIndex() throws Exception
    {
        DataCollection<?> genes = CollectionFactory.getDataElement("Data/gene", module, DataCollection.class);
        assertEquals("Incorrect genes collection", genes.getSize(), 3);

        DataCollection<?> proteins = CollectionFactory.getDataElement("Data/protein", module, DataCollection.class);
        assertEquals("Incorrect proteins collection", proteins.getSize(), 2);

        DataCollection<?> reactions = CollectionFactory.getDataElement("Data/reaction", module, DataCollection.class);
        assertEquals("Incorrect reactions collection", reactions.getSize(), 2);

        IndexBuilder indexBuilder = new IndexBuilder(hubPath);

        QueryEngine queryEngine = new StandardQueryEngine();

        indexBuilder.buildIndexes(new ru.biosoft.access.core.DataCollection[] {genes, proteins, reactions}, queryEngine, null);
    }

    public void testSimpleSearch() throws Exception
    {
        String gene2Name = "test/biohub/Data/gene/GEN000002";
        Properties properties = new Properties();
        properties.put(LuceneBasedBioHub.HUB_DIR_ATTR, hubPath);
        BioHub bioHub = new LuceneBasedBioHub(properties);
        TargetOptions dbOptions = new TargetOptions(new String[] {"test/biohub"}, true);

        int priority = bioHub.getPriority(dbOptions);
        assertTrue("Impossible priority value: " + priority, priority > 0);

        Element[] result = bioHub.getReference(new Element(gene2Name), dbOptions, null, 1, BioHub.DIRECTION_BOTH);
        assertNotNull("result is null", result);
        assertEquals( "Incorrect result size", 6, result.length );

        List<String> correctResults = new ArrayList<>();
        correctResults.add("test/biohub/Data/reaction/RCT000001");
        correctResults.add("test/biohub/Data/reaction/RCT000002");
        correctResults.add("test/biohub/Data/protein/PRT000001");
        correctResults.add("test/biohub/Data/protein/PRT000002");
        correctResults.add("test/biohub/Data/gene/GEN000001");
        correctResults.add("test/biohub/Data/gene/GEN000003");
        for( Element e : result )
        {
            if( correctResults.contains(e.getPath()) )
            {
                correctResults.remove(e.getPath());
            }
            else
            {
                fail("Incorrect search result");
            }
        }
    }

    public void testSimpleSearch2() throws Exception
    {
        String gene2Name = "test/biohub/Data/protein/PRT000001";
        Properties properties = new Properties();
        properties.put(LuceneBasedBioHub.HUB_DIR_ATTR, hubPath);
        BioHub bioHub = new LuceneBasedBioHub(properties);
        TargetOptions dbOptions = new TargetOptions(new String[] {"test/biohub"}, true);

        int priority = bioHub.getPriority(dbOptions);
        assertTrue("Impossible priority value: " + priority, priority > 0);

        Element[] result = bioHub.getReference(new Element(gene2Name), dbOptions, null, 1, BioHub.DIRECTION_BOTH);
        assertNotNull("result is null", result);
        assertEquals( "Incorrect result size", 4, result.length );

        List<String> correctResults = new ArrayList<>();
        correctResults.add("test/biohub/Data/protein/PRT000002");
        correctResults.add("test/biohub/Data/reaction/RCT000001");
        correctResults.add("test/biohub/Data/gene/GEN000001");
        correctResults.add("test/biohub/Data/gene/GEN000002");
        for( Element e : result )
        {
            if( correctResults.contains(e.getPath()) )
            {
                correctResults.remove(e.getPath());
            }
            else
            {
                fail("Incorrect search result");
            }
        }
    }

    public void testDirectionSearch() throws Exception
    {
        String gene2Name = "test/biohub/Data/gene/GEN000002";
        Properties properties = new Properties();
        properties.put(LuceneBasedBioHub.HUB_DIR_ATTR, hubPath);
        BioHub bioHub = new LuceneBasedBioHub(properties);
        TargetOptions dbOptions = new TargetOptions(new String[] {"test/biohub"}, true);

        int priority = bioHub.getPriority(dbOptions);
        assertTrue("Impossible priority value: " + priority, priority > 0);

        Element[] result = bioHub.getReference(new Element(gene2Name), dbOptions, null, 1, BioHub.DIRECTION_DOWN);
        assertNotNull("result is null", result);
        assertEquals( "Incorrect result size", 2, result.length );

        List<String> correctResults = new ArrayList<>();
        correctResults.add("test/biohub/Data/reaction/RCT000002");
        correctResults.add("test/biohub/Data/gene/GEN000003");
        for( Element e : result )
        {
            if( correctResults.contains(e.getPath()) )
            {
                correctResults.remove(e.getPath());
            }
            else
            {
                fail("Incorrect search result");
            }
        }
    }

    public void testDirectionSearch2() throws Exception
    {
        String gene2Name = "test/biohub/Data/gene/GEN000002";
        Properties properties = new Properties();
        properties.put(LuceneBasedBioHub.HUB_DIR_ATTR, hubPath);
        BioHub bioHub = new LuceneBasedBioHub(properties);
        TargetOptions dbOptions = new TargetOptions(new String[] {"test/biohub"}, true);

        int priority = bioHub.getPriority(dbOptions);
        assertTrue("Impossible priority value: " + priority, priority > 0);

        Element[] result = bioHub.getReference(new Element(gene2Name), dbOptions, null, 1, BioHub.DIRECTION_UP);
        assertNotNull("result is null", result);
        assertEquals( "Incorrect result size", 3, result.length );

        List<String> correctResults = new ArrayList<>();
        correctResults.add("test/biohub/Data/reaction/RCT000001");
        correctResults.add("test/biohub/Data/gene/GEN000001");
        correctResults.add("test/biohub/Data/protein/PRT000001");
        for( Element e : result )
        {
            if( correctResults.contains(e.getPath()) )
            {
                correctResults.remove(e.getPath());
            }
            else
            {
                fail("Incorrect search result");
            }
        }
    }

    public void testPathSearch() throws Exception
    {
        String inputName = "test/biohub/Data/protein/PRT000001";
        String outputName = "test/biohub/Data/protein/PRT000002";
        Properties properties = new Properties();
        properties.put(LuceneBasedBioHub.HUB_DIR_ATTR, hubPath);
        BioHub bioHub = new LuceneBasedBioHub(properties);
        TargetOptions dbOptions = new TargetOptions(new String[] {"test/biohub"}, true);

        int priority = bioHub.getPriority(dbOptions);
        assertTrue("Impossible priority value: " + priority, priority > 0);

        Element[] result = bioHub.getMinimalPath( new Element( inputName ), new Element( outputName ), dbOptions, null, 1,
                BioHub.DIRECTION_BOTH );
        assertNotNull("result is null", result);
        assertEquals( "Incorrect result size", 2, result.length );
        assertEquals( "Incorrect first result", inputName, result[result.length - 1].getPath() );
        assertEquals( "Incorrect second result", outputName, result[0].getPath() );
    }

    public void testRemoveIndexes() throws Exception
    {
        File hubRepository = new File(hubPath);
        if( hubRepository.exists() )
        {
            hubRepository.delete();
        }
    }
}
