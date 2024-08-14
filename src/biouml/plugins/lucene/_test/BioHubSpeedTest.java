package biouml.plugins.lucene._test;

import biouml.standard.BioHubQueryEngine;
import biouml.standard.SqlQueryEngine;
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
 * Compare BioHubQueryEngine and SqlQueryEngine search speed on Biopath collection
 */
public class BioHubSpeedTest extends AbstractBioUMLTest
{
    public static final String repositoryPath = "../data";
    public static final String hubPath = "../hubs";
    public static final String moduleName = "Biopath";

    private static DataCollection<?> module;
    private static QueryEngine hubQueryEngine;
    private static QueryEngine sqlQueryEngine;

    public BioHubSpeedTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BioHubTest.class.getName());

        suite.addTest(new BioHubSpeedTest("testRepository"));
        suite.addTest(new BioHubSpeedTest("testSimpleSearch"));
        suite.addTest(new BioHubSpeedTest("testSimpleSearch2"));
        suite.addTest(new BioHubSpeedTest("testSimpleSearch3"));
        suite.addTest(new BioHubSpeedTest("testSearch"));

        return suite;
    }

    public void testRepository() throws Exception
    {
        DataCollection<?> root = CollectionFactory.createRepository(repositoryPath);
        assertNotNull("Can not load repository", root);

        DataElement moduleElement = root.get(moduleName);
        assertNotNull("Can not load biohub test module", moduleElement);
        assertTrue("Can not load biohub test module", moduleElement instanceof DataCollection);
        module = (DataCollection<?>)moduleElement;

        hubQueryEngine = new BioHubQueryEngine();
        sqlQueryEngine = new SqlQueryEngine();
    }

    public void testSimpleSearch() throws Exception
    {
        Base startElement = CollectionFactory.getDataElement("Data/protein/PRT000030", module, Base.class);
        processCompareTest(new SearchElement[] {new SearchElement(startElement)}, new QueryOptions(1, BioHub.DIRECTION_BOTH));
    }

    public void testSimpleSearch2() throws Exception
    {
        Base startElement = CollectionFactory.getDataElement("Data/protein/PRT000030", module, Base.class);
        processCompareTest(new SearchElement[] {new SearchElement(startElement)}, new QueryOptions(1, BioHub.DIRECTION_UP));
    }

    public void testSimpleSearch3() throws Exception
    {
        Base startElement = CollectionFactory.getDataElement("Data/protein/PRT000030", module, Base.class);
        processCompareTest(new SearchElement[] {new SearchElement(startElement)}, new QueryOptions(1, BioHub.DIRECTION_DOWN));
    }
    
    public void testSearch() throws Exception
    {
        Base startElement = CollectionFactory.getDataElement("Data/protein/PRT000030", module, Base.class);
        processCompareTest(new SearchElement[] {new SearchElement(startElement)}, new QueryOptions(2, BioHub.DIRECTION_DOWN));
    }
    
    public void testSearch2() throws Exception
    {
        Base startElement = CollectionFactory.getDataElement("Data/protein/PRT001975", module, Base.class);
        processCompareTest(new SearchElement[] {new SearchElement(startElement)}, new QueryOptions(1, BioHub.DIRECTION_BOTH));
    }
    
    protected void processCompareTest(SearchElement[] startNodes, QueryOptions queryOptions) throws Exception
    {
        TargetOptions dbOptions = new TargetOptions(module.getCompletePath());
        assertTrue("Can not use BioHub query engine", hubQueryEngine.canSearchLinked(dbOptions) > 0);
        assertTrue("Can not use SQL query engine", sqlQueryEngine.canSearchLinked(dbOptions) > 0);

        long time = System.currentTimeMillis();
        SearchElement[] result1 = sqlQueryEngine.searchLinked(startNodes, queryOptions, dbOptions, null);
        assertNotNull(result1);
        long time1 = System.currentTimeMillis() - time;

        time = System.currentTimeMillis();
        SearchElement[] result2 = hubQueryEngine.searchLinked(startNodes, queryOptions, dbOptions, null);
        assertNotNull(result2);
        long time2 = System.currentTimeMillis() - time;

        System.out.println("SQL=" + time1 + "\tHUB=" + time2);
    }
}
