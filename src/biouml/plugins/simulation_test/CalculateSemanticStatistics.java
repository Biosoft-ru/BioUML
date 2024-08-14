package biouml.plugins.simulation_test;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Properties;

import ru.biosoft.util.ExProperties;

public class CalculateSemanticStatistics
{
    private static final String bioUMLDirectory = "../";

    private static final String baseDirectory = bioUMLDirectory + "data_resources/SBML tests/test_suite_2.1.0_final/cases/";

    //path were to put results
    private String outDirectory = baseDirectory + "results/";
    private String outWithCategories = outDirectory + "with categories";

    private String outWithoutCategories = outDirectory + "without categories";

    //path to test diagram files
    private String testDirectory = baseDirectory + "semantic/";

    private String detailsDir = baseDirectory + "details/";

    public static final double DEFAULT_ZERO_SEMANTIC = SemanticSimulatorTest.DEFAULT_ZERO_SEMANTIC;

    private static final boolean oldStyleError = false;

    private static String testListName = "testList.txt";

    private static String categoriesListName = ".cases-tags-map";

    private boolean withCategories = false;

    public void startStatisticCalculation(List<String> testResultPaths, boolean withCategories)
    {
        generateStatistics(testResultPaths, withCategories);
    }

    private int failedTests = 0;

    public int getFailedTestsNumber()
    {
        return failedTests;
    }

    public void setBaseDirectory(String baseDirectory)
    {
        this.testDirectory = baseDirectory + "semantic/";
//        this.outDirectory = baseDirectory + "results/";
//        this.detailsDir = outDirectory + "details/";
//        this.outWithCategories = outDirectory + "with categories";
//        this.outWithoutCategories = outDirectory + "without categories";
    }
    
    public void setOutDirectory(String outDirectory)
    {
//        this.testDirectory = baseDirectory + "semantic/";
        this.outDirectory = outDirectory;
        this.detailsDir = outDirectory + "details/";
        this.outWithCategories = outDirectory + "with categories";
        this.outWithoutCategories = outDirectory + "without categories";
    }

    public void generateStatistics(List<String> testResultPaths, boolean withCategories)
    {
        this.withCategories = withCategories;
        for( String testResultPath : testResultPaths )
            generateStatisticGroup(testResultPath);
    }

    public void generateStatisticGroup(String testPath)
    {
        List<String> tests = SemanticTestListParser.parseTestList(new File(testDirectory + testListName));

        try
        {
            String reportDir = withCategories ? outWithCategories : outWithoutCategories;
            new File(reportDir).mkdirs();

            String csvDirectory = outDirectory + testPath + "/csvResults/";

            FileWriter writer = new FileWriter(reportDir + "/SemanticTests-" + testPath + ".html"); //report file

            File categoriesFile = withCategories ? new File(testDirectory + categoriesListName) : null;
            SemanticTestLogger logger = new SemanticTestLogger(categoriesFile, writer, testDirectory, detailsDir, csvDirectory, true);
            logger.diagramFigsPath = outDirectory +"figs_out/";
            for( String testName : tests )
            {
                String testPathName = testName+"/"+testName;
                logger.testStarted(testPathName);

               
                File infoFile = new File(csvDirectory + testPathName + ".info");
                File csvFile = new File(csvDirectory + testPathName + ".BioUML.csv");

                File results = new File(testDirectory + testPathName + "-results" + ".csv");

                if( csvFile.exists() && results.exists() )
                {
                    boolean failedAlready = false;
                    if( infoFile.exists() )
                    {
                        Properties properties = new ExProperties(infoFile);
                        setProperties(logger, properties);
                        if( !properties.getProperty("status").equals("0") )
                        {
                            logger.error(Integer.parseInt(properties.getProperty("status")), properties.getProperty("messages"));
                            failedTests++;
                            failedAlready = true;
                        }
                    }
                    if( !failedAlready )
                    {
                        SbmlCSVHandler bioumlCsvHandler = new SbmlCSVHandler(csvFile, logger.isTimeCourse());
                        SbmlCSVHandler resultCsvHandler = new SbmlCSVHandler(results, logger.isTimeCourse());

                        if( !fillDifferenceLog(logger, bioumlCsvHandler, resultCsvHandler) )
                            failedTests++;
                    }
                }
                else if( infoFile.exists() )
                {
                    failedTests++;
                    logger.status = Status.CSV_ERROR;
                    Properties properties = new ExProperties(infoFile);
                    setProperties(logger, properties);
                    String problem = properties.getProperty("messages");
                    if( problem != null )
                    {
                        if( problem.contains("Problem is too stiff for this solver") )
                            logger.status = Status.PROBLEM_IS_STIFF;
                        else if( problem.contains("Compilation error:") )
                            logger.status = Status.COMPILATION_FAILED;
                    }

//                    logger.error(Integer.parseInt(properties.getProperty("status")), properties.getProperty("messages"));
                    
                    int simulationStatus =Integer.parseInt(properties.getProperty("status"));
                    if (simulationStatus != Status.SUCCESSFULL)
                        logger.status = simulationStatus;
                    logger.messages = properties.getProperty("messages"); 
                    
                }
                else//TODO: special treatment of missed models for current level
                {
                    continue;
//                    logger.status = Status.CSV_ERROR;
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
     * @param bioumlCsvHandler
     * @param testCsvHandler
     * @return false if test was not passed
     */
    protected boolean fillDifferenceLog(SemanticTestLogger logger, SbmlCSVHandler resultCsvHandler, SbmlCSVHandler testCsvHandler)
    {
        List<String> variableNames = resultCsvHandler.getVariableNames();

        if( resultCsvHandler.getVariableValues() == null )
        {
            logger.error(Status.CSV_ERROR, "simulatedValues == null");
            return false;
        }

        if (!logger.isTimeCourse())
        {
            double[] testValues = resultCsvHandler.getVariableValues().get(0);
            double[] simulatedValues = testCsvHandler.getVariableValues().get(0);
            for( int i = 0; i < variableNames.size() - 1; i++ )
            {
                String varName = variableNames.get(i);
                double testValue = testValues[testCsvHandler.getVariableNames().indexOf(varName)];
                double simulatedValue = simulatedValues[resultCsvHandler.getVariableNames().indexOf(varName)];
                if( significantlyDiffer(testValue, simulatedValue, logger.getATol(), logger.getRTol()) )
                {
                    System.err.println("values differ, row=" + 0 + ", column=" + ( i + 1 ) + ", should be " + testValue
                            + ", but was " + simulatedValue);

                    logger.error(Status.NUMERICALLY_WRONG, "values differ, row=" + 0 + ", column=" + ( i + 1 ) + ", should be "
                            + testValue + ", but was " + simulatedValue);

                    return false;
                }
                else if( !almostEqual(testValue, simulatedValue, logger.getATol(), logger.getRTol())
                        && logger.getStatus() != Status.NUMERICALLY_WRONG )
                {
                    logger.error(Status.NEEDS_TUNING, "values differ, row=" + 0 + ", column=" + ( i + 1 ) + ", should be "
                            + testValue + ", but was " + simulatedValue);

                    return false;
                }
            }
            return true;
        }
        
        double[] times = testCsvHandler.getTimes();
        if( times == null)
        {
            logger.error(Status.CSV_ERROR, "times == null");
            return false;
        }
        
        for( double time : times )
        {
            int timeIndex = resultCsvHandler.findIndexByTime(time, 1E-7);
            double[] simulatedValues = resultCsvHandler.getVariableValues().get(timeIndex);
            double[] testValues = testCsvHandler.getValues(time, 1E-7);

            if( time == times.length )
            {
                logger.error(Status.RESULT_DIFFER, "No corresponding time found for value " + testValues[0]);
                return false;
            }

            for( int i = 0; i < variableNames.size() - 1; i++ )
            {
                String varName = variableNames.get(i + 1);
                double testValue = testValues[testCsvHandler.getVariableNames().indexOf(varName)];
                double simulatedValue = simulatedValues[resultCsvHandler.getVariableNames().indexOf(varName)];
                if( significantlyDiffer(testValue, simulatedValue, logger.getATol(), logger.getRTol()) )
                {
                    System.err.println("values differ, row=" + timeIndex + ", column=" + ( i + 1 ) + ", should be " + testValue
                            + ", but was " + simulatedValue);

                    logger.error(Status.NUMERICALLY_WRONG, "values differ, row=" + timeIndex + ", column=" + ( i + 1 ) + ", should be "
                            + testValue + ", but was " + simulatedValue);

                    return false;
                }
                else if( !almostEqual(testValue, simulatedValue, logger.getATol(), logger.getRTol())
                        && logger.getStatus() != Status.NUMERICALLY_WRONG )
                {
                    logger.error(Status.NEEDS_TUNING, "values differ, row=" + timeIndex + ", column=" + ( i + 1 ) + ", should be "
                            + testValue + ", but was " + simulatedValue);

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

        if( oldStyleError )
        {
            if( ( a == 0 && b < 1e-10 ) || ( b == 0 && a < 1e-10 ) )
                return true;
            return ( Math.abs(a) < DEFAULT_ZERO_SEMANTIC && Math.abs(b) < DEFAULT_ZERO_SEMANTIC ) || ! ( a < 0.999 * b || b < 0.999 * a );
        }
        else
        {

            return ( ( tolerable(new BigDecimal(a), new BigDecimal(b), new BigDecimal(aTol), new BigDecimal(rTol)) ) || ( Math.abs(a) < DEFAULT_ZERO_SEMANTIC && Math
                    .abs(b) < DEFAULT_ZERO_SEMANTIC ) );
            //            return ( ( Math.abs(a - b) < rTol * Math.abs(a + aTol) ) || ( Math.abs(a) < DEFAULT_ZERO_SEMANTIC && Math.abs(b) < DEFAULT_ZERO_SEMANTIC ) );

        }
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

    private void setProperties(SemanticTestLogger logger, Properties properties)
    {
        logger.setSimulationEngineName(properties.getProperty("simulator"));
        logger.setSbmlLevel(properties.getProperty("sbmllevel"));
        logger.setSimulationTime(Long.parseLong(properties.getProperty("time")));
        logger.setATol(Double.parseDouble(properties.getProperty("atol")));
        logger.setRTol(Double.parseDouble(properties.getProperty("rtol")));
        logger.setTimeCourse(Boolean.parseBoolean(properties.getProperty("timecourse")));
        logger.setScriptName(properties.getProperty("scriptname"));
    }
}
