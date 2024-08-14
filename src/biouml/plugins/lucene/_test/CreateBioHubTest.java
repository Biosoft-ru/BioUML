package biouml.plugins.lucene._test;

import java.io.File;

import biouml.plugins.biopax.BioPAXQueryEngine;
import biouml.plugins.lucene.biohub.IndexBuilder;
//import biouml.plugins.transpath.TranspathQueryEngine;
import biouml.standard.StandardQueryEngine;
import biouml.workbench.graphsearch.QueryEngine;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

/**
 * This test is used for biohub lucene index creation.
 * Is not part of AutoTest!!!
 * {@link StandardQueryEngine} is use to build dependencies
 */
public class CreateBioHubTest extends TestCase
{
    public static final String repositoryPath = "../data";
    public static final String hubPath = "../hubs";
    public static final String moduleName = "Transpath";

    static DataCollection<?> module;

    public CreateBioHubTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(CreateBioHubTest.class.getName());

        suite.addTest(new CreateBioHubTest("testRepository"));
        suite.addTest(new CreateBioHubTest("testCreateIndex"));

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

        File hubRepository = new File(hubPath);
        if( hubRepository.exists() )
        {
            hubRepository.delete();
        }
    }

    public void testCreateIndex() throws Exception
    {
        /*ru.biosoft.access.core.DataCollection[] collections = new ru.biosoft.access.core.DataCollection[] {(DataCollection)CollectionFactory.getDataElement("Data/cell", module),
                (DataCollection)CollectionFactory.getDataElement("Data/compartment", module),
                (DataCollection)CollectionFactory.getDataElement("Data/concept", module),
                (DataCollection)CollectionFactory.getDataElement("Data/gene", module),
                (DataCollection)CollectionFactory.getDataElement("Data/protein", module),
                (DataCollection)CollectionFactory.getDataElement("Data/reaction", module),
                (DataCollection)CollectionFactory.getDataElement("Data/rna", module),
                (DataCollection)CollectionFactory.getDataElement("Data/substance", module)};
        */
        ru.biosoft.access.core.DataCollection[] collections = new ru.biosoft.access.core.DataCollection[] {CollectionFactory.getDataElement("Data/gene", module, DataCollection.class),
                CollectionFactory.getDataElement("Data/molecule", module, DataCollection.class),
                CollectionFactory.getDataElement("Data/reaction", module, DataCollection.class)};

        IndexBuilder indexBuilder = new IndexBuilder(hubPath);

        QueryEngine queryEngine = new BioPAXQueryEngine();

        JobControl jobControl = new AbstractJobControl(null)
        {
            protected int currentPercent = 0;

            @Override
            protected void doRun() throws JobControlException
            {
            }

            @Override
            public void setPreparedness(int percent)
            {
                if( percent > currentPercent )
                {
                    currentPercent = percent;
                    System.out.println("Status: " + percent + "%");
                }
            }
        };

        indexBuilder.buildIndexes(collections, queryEngine, jobControl);
    }
}
