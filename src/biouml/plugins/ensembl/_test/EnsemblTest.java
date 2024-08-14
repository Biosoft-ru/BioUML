package biouml.plugins.ensembl._test;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa._test.BSATestUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.ensembl.access.EnsemblGeneDataCollection;
import biouml.standard.type.Gene;

public class EnsemblTest extends TestCase
{
    static EnsemblGeneDataCollection dc = null;

    public EnsemblTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(EnsemblTest.class.getName());

        suite.addTest(new EnsemblTest("testDataCollection"));
        suite.addTest(new EnsemblTest("testGeneCount"));
        suite.addTest(new EnsemblTest("testGeneNames"));
        suite.addTest(new EnsemblTest("testGene"));

        return suite;
    }

    protected void createDataCollection() throws Exception
    {
        BSATestUtils.createRepository();
        dc = DataElementPath.create("databases/Ensembl/Data/gene").getDataElement(EnsemblGeneDataCollection.class);
    }

    public void testGeneCount() throws Exception
    {
        createDataCollection();
        assertTrue("Empty database", dc.getSize() > 0);
        System.out.println(dc.getSize());
    }

    public void testGeneNames() throws Exception
    {
        createDataCollection();
        assertTrue("Empty database", dc.getNameList().size() > 0);
    }

    public void testGene() throws Exception
    {
        createDataCollection();
        Gene gene = dc.get("ENSG00000119787");
        assertNotNull("Cannot get gene", gene);
    }
}
