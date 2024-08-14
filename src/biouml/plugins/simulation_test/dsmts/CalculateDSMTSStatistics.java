package biouml.plugins.simulation_test.dsmts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ru.biosoft.util.ExProperties;
import biouml.plugins.simulation_test.SbmlCSVHandler;
import biouml.plugins.simulation_test.Status;

public class CalculateDSMTSStatistics
{

    //path were to put results
    protected String testDirectory;

    //path to test diagram files
    protected String outDirectory;


    protected String detailsDir;

    protected String csvDirectory;

    private boolean exactTesting = true;

    protected final static double SMALL_CONSTANT_FOR_APPROXIMATE_TESTING = 0.000010;
    protected final static double LEFT_BOUNDARY_FOR_APPROXIMATE_TETSTING = 0.98;
    protected final static double RIGHT_BOUNDARY_FOR_APPROXIMATE_TETSTING = 1.02;

    protected List<String> testList;
    protected HashMap<String, Boolean> simulatorList;
    protected int simulationNumber;

    protected Map<String, Set<Integer>> failedTests;
    
    public Map<String, Set<Integer>> getFailedTests()
    {
        return failedTests;
    }
    
    public CalculateDSMTSStatistics(DSMTSSimulatorTest simulatorTest)
    {
        testDirectory = simulatorTest.baseDirectory;
        outDirectory = simulatorTest.outDirectory;
        detailsDir = simulatorTest.detailsDir;
        csvDirectory = simulatorTest.csvDirectory;
        this.testList = simulatorTest.testList;
        simulatorList = simulatorTest.simulatorList;
        failedTests = new HashMap<>();
    }

    public void test()
    {
        for( Map.Entry<String, Boolean> entry : simulatorList.entrySet() )
            generateStatisticGroup(csvDirectory + entry.getKey() + "/", entry.getKey(), entry.getValue());
    }

    public void generateStatisticGroup(String csvDirectory, String simulatorName, boolean exactTesting)
    {
        try
        {
            this.exactTesting = exactTesting;

            File report = new File(outDirectory + "SemanticTests-" + simulatorName + ".html");
            FileWriter writer = new FileWriter(report);
            DSMTSReportLogger logger = new DSMTSReportLogger(null, writer, testDirectory, detailsDir, csvDirectory, true);

            for( String testName : testList )
            {
                logger.testStarted(testName);

                File infoFile = new File(csvDirectory + testName + ".info");

                File simulatedMeanFile = new File(csvDirectory + testName + "-BioUML.mean.csv");
                File simulatedSDFile = new File(csvDirectory + testName + "-BioUML.sd.csv");

                File expectedResults = new File(testDirectory + testName + "-results.csv"); 

                File modelFile = new File(testDirectory + testName + "-sbml-l3v1.xml");

                if( !modelFile.exists() || !expectedResults.exists() )
                    continue;

                if( !infoFile.exists() )
                {
                    System.out.println("Can not calculate statistics: infoFile " + infoFile.getName() + " not found");
                    logger.setStatus(Status.CSV_ERROR);
                    logger.testCompleted();
                    continue;
                }


                logger.setSbmlLevel("");
                logger.setModelFilePath("../../" + testName + ".xml");
                logger.setJavaFilePath("../java%20out/" + testName.replace("-", "_") + ".java");
                logger.setExactTesting(exactTesting);
                logger.setSimulationEngineName(simulatorName);

                Properties properties = new ExProperties(infoFile);
                logger.setSimulationTime(Long.parseLong(properties.getProperty(DSMTSSimulationResultLogger.SIMULATIONTIME)));
                
                simulationNumber = Integer.parseInt(properties.getProperty(DSMTSSimulationResultLogger.SIMULATION_NUMBER));
                logger.setSimulationNumber(simulationNumber);
                double initialTime = Double.parseDouble(properties.getProperty(DSMTSSimulationResultLogger.INITIAL_TIME));
                logger.setInitialTime(initialTime);

                double duration = Double.parseDouble(properties.getProperty(DSMTSSimulationResultLogger.COMPLETION_TIME));
                duration -= initialTime;
                logger.setDuration(duration);
                logger.setStep(Double.parseDouble(properties.getProperty(DSMTSSimulationResultLogger.STEP)));

                if( !simulatedSDFile.exists() || !simulatedMeanFile.exists() )
                {
                    String problem = properties.getProperty(DSMTSSimulationResultLogger.MESSAGES);
                    if( problem != null && problem.contains("Compilation error:") )
                    {
                        logger.setStatus(Status.COMPILATION_FAILED);
                        logger.error(Integer.parseInt(properties.getProperty(DSMTSSimulationResultLogger.STATUS)), properties
                                .getProperty(DSMTSSimulationResultLogger.MESSAGES));
                    }
                }

                SbmlCSVHandler simulatedMeanHandler = new SbmlCSVHandler(simulatedMeanFile);
                SbmlCSVHandler simulatedSDHandler = new SbmlCSVHandler(simulatedSDFile);

                SbmlCSVHandler testMeanHandler = new SbmlCSVHandler(expectedResults);

                generatePlot(simulatedMeanHandler, testMeanHandler, "-mean", "Mean Values", new File(csvDirectory + testName + "-mean.png"));
                generatePlot(simulatedSDHandler, testMeanHandler, "-sd", "Standard Deviation", new File(csvDirectory + testName + "-sd.png"));

                fillDifferenceLog(logger, simulatedMeanHandler, simulatedSDHandler, testMeanHandler);

                logger.testCompleted();
            }
            logger.complete();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    protected boolean fillDifferenceLog(DSMTSReportLogger logger, SbmlCSVHandler simulatedMeanCsvHandler,
            SbmlCSVHandler simulatedSDCsvHandler, SbmlCSVHandler expectedResults)
    {
        List<String> variableNames = simulatedMeanCsvHandler.getVariableNames();

        double[] simulatedMeanTimes = simulatedMeanCsvHandler.getTimes();
        double[] simulatedSDTimes = simulatedSDCsvHandler.getTimes();
        double[] testMeanTimes = expectedResults.getTimes();
        
        if( !equals(simulatedMeanTimes, testMeanTimes, 1E-7) || !equals(simulatedMeanTimes, simulatedSDTimes, 1E-7) )
        {
            logger.error(Status.RESULT_DIFFER, "time points differs");
            return false;
        }

        Set<Integer> failedTimePoints = new HashSet<>();
        
        List<double[]> simulatedMeanValues = simulatedMeanCsvHandler.getVariableValues();
        List<double[]> simulatedSDValues = simulatedSDCsvHandler.getVariableValues();
        List<double[]> expectedValues = expectedResults.getVariableValues();

        for( int j = 0; j < variableNames.size(); j++ )
        {
            String varName = variableNames.get(j);
            if( "time".equals(varName) || "Time".equals(varName) )
                continue;

            int simulatedMeanIndex = simulatedMeanCsvHandler.getVariableNames().indexOf(varName);
            int testMeanIndex = expectedResults.getVariableNames().indexOf(varName+"-mean");
            int simualtedSDIndex = simulatedSDCsvHandler.getVariableNames().indexOf(varName);
            int testSDIndex = expectedResults.getVariableNames().indexOf(varName+"-sd");

            boolean meanError = false;
            boolean sdError = false;

            for( int i = 0; i < testMeanTimes.length; i++ )
            {
                double simulatedMean = simulatedMeanValues.get(i)[simulatedMeanIndex];
                double testMean = expectedValues.get(i)[testMeanIndex];
                double simulatedSD = simulatedSDValues.get(i)[simualtedSDIndex];
                double testSD = expectedValues.get(i)[testSDIndex];

                if( !checkMeanValues(simulatedMean, testMean, testSD, simulationNumber, exactTesting) )
                {
                    if( !meanError && !sdError ) // for space economy
                    {
                        System.err.println("mean values differ, row=" + i + ", column=" + ( j ) + ", should be " + testMean + ", but was "
                                + simulatedMean);

                        logger.error(DSMTSStatus.MEAN_VALUES_ERROR, "mean values differ, row=" + i + ", column=" + ( i + 1 )
                                + ", should be " + testMean + ", but was " + simulatedMean);
                    }
                    meanError = true;
                }

                if( !checkSDValues(simulatedSD, testSD, simulationNumber, exactTesting) )
                {
                    if( !meanError && !sdError ) // for space economy
                    {
                        System.err.println("standard deviation values differ, row=" + i + ", column=" + ( j ) + ", should be " + testSD
                                + ", but was " + simulatedSD);

                        logger.error(DSMTSStatus.STANDARD_DEVIATION_ERROR, "standard values differ, row=" + i + ", column=" + ( i + 1 )
                                + ", should be " + simulatedSD + ", but was " + testSD);
                    }
                    sdError = true;
                }

                if (meanError || sdError)
                    failedTimePoints.add(i);
                
            }

            if ( failedTimePoints.size() > 0)
                failedTests.put(logger.getCurrentTest(), failedTimePoints);
            
            if( meanError && !sdError )
                logger.setStatus(DSMTSStatus.MEAN_VALUES_ERROR);
            else if( sdError && !meanError )
                logger.setStatus(DSMTSStatus.STANDARD_DEVIATION_ERROR);
            else if( sdError && meanError )
                logger.setStatus(DSMTSStatus.NUMERICALLY_WRONG);
        }
        return true;
    }

    public static boolean checkMeanValues(double mean, double exactMean, double exactSD, int simulationNumber, boolean exactTesting)
    {
        if( exactTesting )
        {
            if( exactSD == 0 )
                return mean == exactMean;
            double stat = ( mean - exactMean ) * Math.sqrt(simulationNumber) / exactSD;
            return Math.abs(stat) < 3;
        }
        return checkApproximately(mean, exactMean);
    }

    public static boolean checkSDValues(double sd, double exactSD, int simulationNumber, boolean exactTesting)
    {
        if( exactTesting )
        {
            if( exactSD == 0 )
                return sd == 0;
            double stat = ( ( sd * sd ) / ( exactSD * exactSD ) - 1 ) * Math.sqrt(simulationNumber / 2.0);
            return Math.abs(stat) < 5;
        }
        return checkApproximately(sd, exactSD);
    }

    private static boolean checkApproximately(double value, double exactValue)
    {
        if( exactValue == 0 )
            return value < SMALL_CONSTANT_FOR_APPROXIMATE_TESTING;
        return ( value / exactValue >= LEFT_BOUNDARY_FOR_APPROXIMATE_TETSTING )
                && ( value / exactValue <= RIGHT_BOUNDARY_FOR_APPROXIMATE_TETSTING );
    }

    private void generatePlot(SbmlCSVHandler simulated, SbmlCSVHandler expected, String suffixInExpected, String title, File file)
    {
        try
        {
            double[] testTimes = expected.getTimes();

            List<String> varNames = simulated.getVariableNames();

            XYSeriesCollection collection = new XYSeriesCollection();
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setDrawSeriesLineAsPath(true);

            int seriesIndex = 0;

            List<Stroke> strokes = new ArrayList<>();
            strokes.add(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));
            strokes.add(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f));
            strokes.add(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {9, 4}, 0.0f));
            strokes.add(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {9, 4}, 0.0f));
            strokes.add(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {1, 5}, 0.0f));
            strokes.add(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {1, 5}, 0.0f));

            for( String varName : varNames )
            {

                if( varName.equals("time") || varName.equals("Time") )
                    continue;

                XYSeries testSeries = new XYSeries(varName);
                XYSeries simulatedSeries = new XYSeries(varName + " BioUML");

                int simulatedIndex = simulated.getVariableNames().indexOf(varName);
                int testIndex = expected.getVariableNames().indexOf(varName+suffixInExpected);

                for( double time : testTimes )
                {
                    double[] simulatedValues = simulated.getValues(time, 1E-7);
                    double[] testValues = expected.getValues(time, 1E-7);

                    simulatedSeries.add(time, simulatedValues[simulatedIndex]);
                    testSeries.add(time, testValues[testIndex]);
                }

                renderer.setSeriesShapesVisible(seriesIndex, false);
                renderer.setSeriesShapesVisible(seriesIndex + 1, false);
                renderer.setSeriesStroke(seriesIndex, strokes.get(seriesIndex));
                renderer.setSeriesStroke(seriesIndex + 1, strokes.get(seriesIndex + 1));

                renderer.setSeriesPaint(seriesIndex, Color.black);
                renderer.setSeriesPaint(seriesIndex + 1, Color.red);
                renderer.setDrawSeriesLineAsPath(true);
                collection.addSeries(testSeries);
                collection.addSeries(simulatedSeries);
                seriesIndex += 2;
            }

            JFreeChart chart = ChartFactory.createXYLineChart(title, "Time", "", collection, PlotOrientation.VERTICAL, true, // legend
                    true, // tool tips
                    false // URLs
                    );
           
            chart.getXYPlot().setRenderer(renderer);
            chart.getXYPlot().setBackgroundPaint(Color.white);
            chart.setBackgroundPaint(Color.white);

            BufferedImage bi = chart.createBufferedImage(800, 600);

            ImageIO.write(bi, "png", file);
        }
        catch( Exception ex )
        {
            System.out.println("Can not create plot because of " + ex.getMessage());
        }
    }

    public static boolean equals(double[] arr1, double[] arr2, double accuracy)
    {
        int n = arr1.length;
        if( n != arr2.length )
            return false;

        for( int i = 0; i < n; i++ )
        {
            if( !equals(arr1[i], arr2[i], accuracy) )
                return false;
        }
        return true;
    }

    public static boolean equals(double a, double b, double accuracy)
    {
        return Math.abs(a - b) <= accuracy * Math.abs(a) + Math.abs(b);
    }

}
