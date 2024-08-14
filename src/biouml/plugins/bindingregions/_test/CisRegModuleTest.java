
package biouml.plugins.bindingregions._test;

import biouml.plugins.bindingregions.cisregmodule.CisRegModule;
import biouml.plugins.bindingregions.cisregmodule.CisRegModuleParameters;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa._test.BSATestUtils;

/**
 * @author yura
 *
 */
public class CisRegModuleTest extends TestCase
{
    public CisRegModuleTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(CisRegModuleTest.class.getName());
        suite.addTest(new CisRegModuleTest("test"));
        return suite;
    }

    public final static DataElementPath OUTPUT_DATA_COLLECTION_PATH = DataElementPath.create("data/Collaboration/yura_test/Data/Tables");

    public final static int mode = 3;

    public final static String OUTPUT_NAME = "result";

    public static void main(String ... args)
    {
        try
        {
            CisRegModule analysis = new CisRegModule(null, "");

            CisRegModuleParameters parameters = new CisRegModuleParameters();

            parameters.setSequencePath(createSequenceDataElementPath());
            parameters.setCisRegModuleTable(createOutputDataElementPath());

            analysis.setParameters(parameters);

            analysis.justAnalyzeAndPut();
        }
        catch( Exception ex )
        {
            System.out.println("Error:" + ex.getMessage());
        }
    }
    public void test() throws Exception
    {

        CisRegModule analysis = new CisRegModule(null, "");

        CisRegModuleParameters parameters = new CisRegModuleParameters();

        parameters.setSequencePath(createSequenceDataElementPath());
        parameters.setCisRegModuleTable(createOutputDataElementPath());

        analysis.setParameters(parameters);

        analysis.justAnalyzeAndPut();
    }

    public static DataElementPath createSequenceDataElementPath() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath de = DataElementPath.create("databases/Ensembl/Sequences/chromosomes NCBI36");
        assertTrue(de.exists());
        return de;
    }

    public static DataElementPath createOutputDataElementPath() throws Exception
    {

        String repositoryPath = "../data_resources";
        CollectionFactory.createRepository(repositoryPath);
        assertTrue(OUTPUT_DATA_COLLECTION_PATH.exists());
        DataElementPath outputPath = OUTPUT_DATA_COLLECTION_PATH.getChildPath(OUTPUT_NAME);
        
        return outputPath;
    }

}
