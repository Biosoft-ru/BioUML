package biouml.plugins.bionetgen._test;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.util.ExProperties;
import biouml.plugins.bionetgen.diagram.BionetgenConstants;

import com.developmentontheedge.application.ApplicationUtils;

public class BionetgenStatisticsCalculator
{
    public static final double DEFAULT_ZERO_SEMANTIC = 1E-15;

    //path were to put results
    private final String outDirectory;

    //path to test diagram files
    private final String testDirectory;

    private final String detailsDir;

    private static String testListName = "testList";

    private int failedTests = 0;

    public int getFailedTestsNumber()
    {
        return failedTests;
    }

    public BionetgenStatisticsCalculator(String testDirectory, String outDirectory)
    {
        this.testDirectory = testDirectory;
        this.outDirectory = outDirectory;
        this.detailsDir = outDirectory + "details/";
    }

    public void generateStatisticGroup()
    {
        try
        {
            List<String> tests = ApplicationUtils.readAsList(new File(testDirectory + testListName));

            String reportDir = outDirectory + "report/";
            File dir = new File(reportDir);
            if( !dir.exists() && !dir.mkdirs() )
                throw new Exception("Failed to create report directory");

            //report file
            PrintWriter writer = new PrintWriter(reportDir + "BionetgenSimulationTests-statistic.html", StandardCharsets.UTF_8.toString());

            BionetgenTestLogger logger = new BionetgenTestLogger( writer, testDirectory, detailsDir, outDirectory );

            for( String testName : tests )
            {
                logger.testStarted(testName);

                File infoFile = new File(outDirectory + testName + ".info");
                File csvFile = new File(testDirectory + testName + ".gdat");

                File results = new File(outDirectory + testName + "-results" + ".csv");

                if( csvFile.exists() && results.exists() )
                {
                    boolean failedAlready = false;
                    if( infoFile.exists() )
                    {
                        Properties properties = new ExProperties(infoFile);
                        logger.setSimulationEngineName(properties.getProperty(BionetgenSimulationTest.SIMULATOR));
                        logger.setSimulationTime(Long.parseLong(properties.getProperty(BionetgenSimulationTest.SIMULATION_TIME)));
                        logger.setATol(Double.parseDouble(properties.getProperty(BionetgenConstants.ATOL_PARAM)));
                        logger.setRTol(Double.parseDouble(properties.getProperty(BionetgenConstants.RTOL_PARAM)));
                    }
                    if( !failedAlready )
                    {
                        Map<String, double[]> bioumlResults = BionetgenTestUtility.readResults(outDirectory + testName + "-results.csv");
                        Map<String, double[]> testResults = BionetgenTestUtility.readResults(testDirectory + testName + ".gdat");

                        if( !fillDifferenceLog(logger, bioumlResults, testResults) )
                            failedTests++;
                    }
                }
                else
                {
                    if( infoFile.exists() )
                        failedTests++;
                    logger.status = Status.CSV_ERROR;
                }
                logger.testCompleted();
            }
            logger.complete();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
    /**
     * 
     * @param logger
     * @param bioumlResults
     * @param testResults
     * @return false if test was not passed
     */
    protected boolean fillDifferenceLog(BionetgenTestLogger logger, Map<String, double[]> bioumlResults, Map<String, double[]> testResults)
    {
        String[] variableNames = testResults.keySet().toArray(new String[testResults.size()]);

        if( bioumlResults == null || bioumlResults.isEmpty() || bioumlResults.get( "time" ).length == 0 )
        {
            logger.error(Status.CSV_ERROR, "simulatedValues == null");
            return false;
        }

        double[] times = testResults.get("time");

        if( times == null )
        {
            logger.error(Status.CSV_ERROR, "times == null");
            return false;
        }

        for( int i = 0; i < variableNames.length - 1; i++ )
        {
            String varName = variableNames[i + 1];
            for( int j = 0; j < times.length; j++ )
            {
                double testValue = testResults.get(varName)[j];
                double simulatedValue = bioumlResults.get(varName)[j];
                if( significantlyDiffer(testValue, simulatedValue, logger.getATol(), logger.getRTol()) )
                {
                    System.err.println("values differ, row=" + j + ", column=" + ( i + 1 ) + ", should be " + testValue + ", but was "
                            + simulatedValue);

                    logger.error(Status.NUMERICALLY_WRONG, "values differ, row=" + j + ", column=" + ( i + 1 ) + ", should be " + testValue
                            + ", but was " + simulatedValue);

                    return false;
                }
                else if( !almostEqual(testValue, simulatedValue, logger.getATol(), logger.getRTol())
                        && logger.getStatus() != Status.NUMERICALLY_WRONG )
                {
                    logger.error(Status.NEEDS_TUNING, "values differ, row=" + j + ", column=" + ( i + 1 ) + ", should be " + testValue
                            + ", but was " + simulatedValue);

                    return false;
                }
            }
        }
        return true;
    }

    public static boolean almostEqual(double a, double b, double aTol, double rTol)
    {
        if( Double.isNaN(a) )
        {
            return Double.isNaN(b);
        }
        else if( Double.isNaN(b) )
            return false;

        if( Double.isInfinite(a) || Double.isInfinite(b) )
            return a == b;

        return ( ( tolerable( new BigDecimal( a ), new BigDecimal( b ), new BigDecimal( aTol ), new BigDecimal( rTol ) ) )
                || ( Math.abs( a ) < DEFAULT_ZERO_SEMANTIC && Math.abs( b ) < DEFAULT_ZERO_SEMANTIC ) );
    }

    public static boolean significantlyDiffer(double a, double b, double aTol, double rTol)
    {
        return !almostEqual(a, b, aTol, rTol) && Math.abs(a - b) > 5e-1 * ( Math.abs(a) + Math.abs(b) );
    }

    private static final boolean tolerable(BigDecimal expected, BigDecimal actual, BigDecimal absTol, BigDecimal relTol)
    {
        MathContext mc = new MathContext(expected.precision());
        BigDecimal adjusted = actual.round(mc);
        BigDecimal actualDiff = expected.subtract(adjusted).abs();
        BigDecimal allowableDiff = absTol.add(relTol.multiply(expected.abs()));

        return ( actualDiff.compareTo(allowableDiff) <= 0 );
    }

}
