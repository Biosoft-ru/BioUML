package biouml.standard._test;

import java.io.File;

import biouml.plugins.lucene.biohub.IndexBuilder;
import biouml.standard.BioHubQueryEngine;
import biouml.standard.StandardQueryEngine;
import biouml.standard.type.Base;
import biouml.workbench.graphsearch.QueryEngine;
import biouml.workbench.graphsearch.QueryOptions;
import biouml.workbench.graphsearch.SearchElement;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Testing of BioHubQueryEngine
 */
public class BioHubQueryEngineTest extends AbstractBioUMLTest
{
    public static final String repositoryPath = "../data/test/biouml/plugins/lucene/biohub";
    public static final String hubPath = "hubs";

    static DataCollection<?> module;

    public BioHubQueryEngineTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BioHubQueryEngineTest.class.getName());

        suite.addTest(new BioHubQueryEngineTest("testCreateIndex"));
        suite.addTest(new BioHubQueryEngineTest("testGraphSearch"));
        //suite.addTest(new BioHubQueryEngineTest("testPathSearch")); it is not correctly realized yet
        //suite.addTest(new BioHubQueryEngineTest("testPathSearch2"));
        suite.addTest(new BioHubQueryEngineTest("testRemoveIndexes"));

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
        assertEquals( "Incorrect genes collection", 3, genes.getSize() );

        DataCollection<?> proteins = CollectionFactory.getDataElement("Data/protein", module, DataCollection.class);
        assertEquals( "Incorrect proteins collection", 2, proteins.getSize() );

        DataCollection<?> reactions = CollectionFactory.getDataElement("Data/reaction", module, DataCollection.class);
        assertEquals( "Incorrect reactions collection", 2, reactions.getSize() );

        IndexBuilder indexBuilder = new IndexBuilder(hubPath);

        QueryEngine queryEngine = new StandardQueryEngine();

        indexBuilder.buildIndexes(new ru.biosoft.access.core.DataCollection[] {genes, proteins, reactions}, queryEngine, null);
    }

    public void testGraphSearch() throws Exception
    {
        QueryEngine queryEngine = new BioHubQueryEngine();
        TargetOptions dbOptions = new TargetOptions(module.getCompletePath());
        int priority = queryEngine.canSearchLinked(dbOptions);
        assertTrue("incorrect canSearchLinked result", priority > 0);

        QueryOptions queryOptions = new QueryOptions(1, BioHub.DIRECTION_BOTH);
        Base startBase = CollectionFactory.getDataElement("Data/gene/GEN000002", module, Base.class);
        SearchElement startNode = new SearchElement(startBase);
        SearchElement[] result = queryEngine.searchLinked(new SearchElement[] {startNode}, queryOptions, dbOptions, null);
        assertEquals( "incorrect result size", 6, result.length );

        queryOptions = new QueryOptions( 2, BioHub.DIRECTION_DOWN );
        startBase = CollectionFactory.getDataElement("Data/gene/GEN000001", module, Base.class);
        startNode = new SearchElement(startBase);
        result = queryEngine.searchLinked(new SearchElement[] {startNode}, queryOptions, dbOptions, null);
        assertEquals( "incorrect result size", 4, result.length );
    }

    public void testPathSearch() throws Exception
    {
        QueryEngine queryEngine = new BioHubQueryEngine();
        TargetOptions dbOptions = new TargetOptions(module.getCompletePath());
        int priority = queryEngine.canSearchPath(dbOptions);
        assertTrue("incorrect canSearchPath result", priority > 0);

        QueryOptions queryOptions = new QueryOptions(1, BioHub.DIRECTION_BOTH);
        Base startBase = CollectionFactory.getDataElement("Data/gene/GEN000002", module, Base.class);
        assertNotNull("Can not find element", startBase);
        SearchElement startNode = new SearchElement(startBase);
        Base endBase = CollectionFactory.getDataElement("Data/gene/GEN000003", module, Base.class);
        assertNotNull("Can not find element", endBase);
        SearchElement endNode = new SearchElement(endBase);

        SearchElement[] result = queryEngine.searchPath(new SearchElement[] {startNode, endNode}, queryOptions, dbOptions, null);
        assertNotNull("result is null", result);
        assertEquals( "incorrect result size", 3, result.length );
        assertEquals( "Incorrect first result element", startNode.getPath(), result[0].getPath() );
        assertEquals( "Incorrect last result element", endNode.getPath(), result[2].getPath() );

        result = queryEngine.searchPath(new SearchElement[] {endNode, startNode}, queryOptions, dbOptions, null);
        assertNotNull("result is null", result);
        assertEquals( "incorrect result size", 3, result.length );
        assertEquals( "Incorrect first result element", endNode.getPath(), result[0].getPath() );
        assertEquals( "Incorrect last result element", startNode.getPath(), result[2].getPath() );
    }

    public void testPathSearch2() throws Exception
    {
        QueryEngine queryEngine = new BioHubQueryEngine();
        TargetOptions dbOptions = new TargetOptions(module.getCompletePath());
        int priority = queryEngine.canSearchPath(dbOptions);
        assertTrue("incorrect canSearchPath result", priority > 0);

        Base startBase = CollectionFactory.getDataElement("Data/gene/GEN000001", module, Base.class);
        SearchElement startNode = new SearchElement(startBase);
        Base endBase = CollectionFactory.getDataElement("Data/gene/GEN000003", module, Base.class);
        SearchElement endNode = new SearchElement(endBase);

        SearchElement[] result = queryEngine.searchPath(new SearchElement[] {startNode, endNode},
                new QueryOptions(2, BioHub.DIRECTION_DOWN), dbOptions, null);
        assertNotNull("result is null", result);
        assertEquals( "incorrect result size", 5, result.length );
        assertEquals( "Incorrect first result element", startNode.getPath(), result[0].getPath() );
        assertEquals( "Incorrect second result element", "test/biohub/Data/reaction/RCT000001", result[1].getPath() );
        assertEquals( "Incorrect third result element", "test/biohub/Data/gene/GEN000002", result[2].getPath() );
        assertEquals( "Incorrect fourth result element", "test/biohub/Data/reaction/RCT000002", result[3].getPath() );
        assertEquals( "Incorrect last result element", endNode.getPath(), result[4].getPath() );

        result = queryEngine.searchPath(new SearchElement[] {endNode, startNode}, new QueryOptions(2, BioHub.DIRECTION_DOWN), dbOptions,
                null);
        assertNotNull("result is null", result);
        assertEquals( "incorrect result size", 3, result.length );
        assertEquals( "Incorrect first result element", endNode.getPath(), result[0].getPath() );
        assertEquals( "Incorrect second result element", "test/biohub/Data/protein/PRT000002", result[1].getPath() );
        assertEquals( "Incorrect last result element", startNode.getPath(), result[2].getPath() );
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
