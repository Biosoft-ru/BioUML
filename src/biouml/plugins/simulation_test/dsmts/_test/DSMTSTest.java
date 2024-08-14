package biouml.plugins.simulation_test.dsmts._test;

import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;

import biouml.plugins.simulation_test.dsmts.CalculateDSMTSStatistics;
import biouml.plugins.simulation_test.dsmts.DSMTSSimulatorTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DSMTSTest extends TestCase
{


    public DSMTSTest(String name)
    {
        super(name);
    }


    public static Test suite()
    {
        TestSuite suite = new TestSuite(DSMTSTest.class.getName());

        suite.addTest(new DSMTSTest("test"));

        return suite;
    }

    public void test()
    {
        for( int i = 0; i < 1; i++ )
        {
            try
            {
                DSMTSSimulatorTest simulatorTest = new DSMTSSimulatorTest();
                simulatorTest.test();
                CalculateDSMTSStatistics calculator = new CalculateDSMTSStatistics(simulatorTest);
                calculator.test();

                Map<String, Set<Integer>> failedTests = calculator.getFailedTests();
                int totalTimePoints = StreamEx.of(failedTests.values()).mapToInt(set -> set.size()).sum();
                System.out.println("Failed tests: " + failedTests);
                System.out.println("Total failed time points: " + totalTimePoints);
                if( failedTests.size() > 0 )
                {
                    assertTrue("Too many time points failed ( " + totalTimePoints + " )",
                            totalTimePoints < DSMTSSimulatorTest.FAILED_TIME_POINTS_THRESHOLD);
                    failedTests.forEach( (test, failedPoints) ->
                        assertTrue("Too many filed time points for test " + test + " ( " + failedPoints.size() + " )",
                                failedPoints.size() < DSMTSSimulatorTest.FAILED_TEST_TIME_POINTS_THRESHOLD));
                }

            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }
    }


}