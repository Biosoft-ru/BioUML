package biouml.plugins.simulation_test._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.plugins.simulation_test.CalculateSemanticStatistics;
import biouml.plugins.simulation_test.SemanticSimulatorTest;


public class SBMLOldTest extends AbstractBioUMLTest
{
    /** Standart JUnit constructor */
    public SBMLOldTest(String name)
    {
        super(name);

        File configFile = new File( "./biouml/plugins/simulation_test/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SBMLOldTest.class.getName());
        suite.addTest(new SBMLOldTest("testSBML"));
        return suite;
    }

    static String DEFAULT_TEST_DIR = "../data_resources/SBML tests/cases/";
    static String DEFAULT_OUT_DIR = "../data_resources/SBML tests/cases/results_old/";

    public static void main(String ... args) throws Exception
    {
        testSBML();
    }

    public static void testSBML() throws Exception
    {
        SemanticSimulatorTest semanticSimulatorTest = new SemanticSimulatorTest(true);
        semanticSimulatorTest.setOutDirectory( DEFAULT_OUT_DIR );
        CalculateSemanticStatistics statisticsCalulator = new CalculateSemanticStatistics();

        String userDirectory = System.getProperty("biouml.sbmltest.path");
        if( userDirectory != null && new File(userDirectory).exists() )
        {
            semanticSimulatorTest.setBaseDirectory(userDirectory);
            semanticSimulatorTest.setOutDirectory( userDirectory+"results/" );
            statisticsCalulator.setBaseDirectory(userDirectory);
            statisticsCalulator.setOutDirectory( userDirectory+"results/" );
        }
        else
        {
            if( !new File(DEFAULT_TEST_DIR).exists() )
                fail("Can not find SBML test suite directory:" + DEFAULT_TEST_DIR + ". Try to run with another parameter.");

            semanticSimulatorTest.setBaseDirectory(DEFAULT_TEST_DIR);
            semanticSimulatorTest.setOutDirectory(DEFAULT_OUT_DIR );
            statisticsCalulator.setBaseDirectory(DEFAULT_TEST_DIR);
            statisticsCalulator.setOutDirectory( DEFAULT_OUT_DIR );
        }

        double time = System.currentTimeMillis();
        semanticSimulatorTest.executeSemanticTests("SemanticTests");
        List<String> results = semanticSimulatorTest.getGeneratedFoldersList();
        statisticsCalulator.startStatisticCalculation(results, true);
        statisticsCalulator.startStatisticCalculation(results, false);
        int failedTests = statisticsCalulator.getFailedTestsNumber();
        System.out.println("Elapsed time: " + ( System.currentTimeMillis() - time ) + " ms.");
        assertEquals(failedTests + " tests failed. ", 0, failedTests);
    }
}
