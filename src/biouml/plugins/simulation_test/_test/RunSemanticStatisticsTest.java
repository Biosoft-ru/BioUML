package biouml.plugins.simulation_test._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.logging.LogManager;

import biouml.plugins.simulation_test.CalculateSemanticStatistics;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RunSemanticStatisticsTest extends TestCase
{
    /** Standart JUnit constructor */
    public RunSemanticStatisticsTest(String name)
    {
        super(name);

        File configFile = new File( "biouml/plugins/simulation_test/_test/log.lcf" );
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
        TestSuite suite = new TestSuite(RunSemanticStatisticsTest.class.getName());
        suite.addTest(new RunSemanticStatisticsTest("runTests"));
        return suite;
    }


    public void runTests() throws Exception
    {
        new CalculateSemanticStatistics().startStatisticCalculation(new ArrayList<String>(), false);
    }
}
