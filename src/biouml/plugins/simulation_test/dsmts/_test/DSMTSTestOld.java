

package biouml.plugins.simulation_test.dsmts._test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.util.TextUtil2;
import biouml.model.Diagram;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.plugins.stochastic.StochasticModel;
import biouml.plugins.stochastic.StochasticSimulationEngine;
import biouml.plugins.stochastic.solvers.GillespieSolver;
import biouml.plugins.stochastic.solvers.TauLeapingSolver;
import biouml.standard.simulation.ResultListener;

public class DSMTSTestOld extends TestCase implements ResultListener
{
    double[][] results;

    double[] timePoints;

    int sampleLength;
    int spanIndex;

    double initialTime = 0;
    double timeStep = 1;
    double maxTime = 50.0;

    final int numberOfRepeations = 1;

    static final double SMALL_CONSTANT_FOR_APPROXIMATE_TESTING = 0.000010;

    double maxErrorMean;
    double maxErrorSD;

    int testFailed = 0;
    int valuesFailed = 0;
    int totalTime = 0;
    int speciesNumber;

    long g = 0;
    long nG = 0;

    List<Integer> variableIndicesList;

    String root = "../";
    String testDir = root + "data_resources/SBML stochastic tests/";
    String modelPath = testDir + "models/";
    String modelListPath = modelPath + "model-list";
    String javaOutPath = testDir + "java out/";

    public DSMTSTestOld(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(DSMTSTestOld.class.getName());

        suite.addTest(new DSMTSTestOld("test"));

        return suite;
    }

    public void test() throws Exception
    {
        test(new GillespieSolver(), true);
//        test(new GillespieIncorrectSolver(), true);
//        test(new GillespieEfficientSolver(), true);
//        test(new GibsonBruckSolver(), true);
//        test(new TauLeapingSolver(), false);
//        test(new MaxTSSolver(), false);
//        test(new MaxTSSolverUsingGillespie(), false);
    }


    public void test(Simulator simulator, boolean isExactSimulator) throws Exception
    {
        System.out.println();
        System.out.println("Simulator " + simulator.getInfo().name + " testing");
        System.out.println();
        File modelDirectory = new File(modelPath);
        File modelList = new File(modelListPath);
        valuesFailed = 0;
        testFailed = 0;
        totalTime = 0;

        assert ( modelDirectory.isDirectory() );
        assertNotNull("Can not find test diagram directory", modelDirectory);

        try(BufferedReader br = ApplicationUtils.asciiReader( modelList ))
        {
            String modelName;
            while( ( modelName = br.readLine() ) != null )
            {
                File modelFile = new File(modelPath + modelName + ".xml");
                File sdFile = new File(modelPath + modelName + "-sd.csv");
                File meanFile = new File(modelPath + modelName + "-mean.csv");
                
                if( !modelFile.exists() || !sdFile.exists() || !meanFile.exists() )
                {
                    System.out.println("Model " + modelName + " does not exist and was skipped");
                    continue;
                }
                
                try
                {
                    testEngine(modelFile, sdFile, meanFile, simulator, isExactSimulator);
                }
                catch( Exception ex )
                {
                    System.out.println("Test of model " + modelName + "failed");
                    ex.printStackTrace();
                }
            }
        }

        System.out.println();
        System.out.println();
        System.out.println("The testing process of " + simulator.getInfo().name + " is finished.");
        System.out.println("Total number of failed tests: " + testFailed);
        System.out.println("Total number of failed comparisons: " + valuesFailed);
        System.out.println("Total simulation time: " + totalTime);
    }

    private void testEngine(File modelFile, File sdFile, File meanFile, Simulator simulator, boolean exactSimualtor) throws Exception
    {
        //init
        Diagram diagram = SbmlModelFactory.readDiagram(modelFile, null, modelFile.getName());
        System.out.println("");
        System.out.print("The testing of " + diagram.getName());
        Span span = new ArraySpan(0, maxTime, timeStep);

        StochasticSimulationEngine engine = initEngine(diagram, simulator, span);
        StochasticModel model = (StochasticModel)engine.createModel();

        sampleLength = span.getLength();

        initVariableIndecesList(meanFile, diagram, engine);
        double[][] exactMeanValues = readExactValuesFromFiles(meanFile, diagram, engine);
        double[][] exactStandartDeviationValues = readExactValuesFromFiles(sdFile, diagram, engine);

        double[][] standartDeviationSquered = new double[sampleLength][speciesNumber];
        double[][] meanValues = new double[sampleLength][speciesNumber];

        nG = 0;
        g = 0;
        maxErrorMean = 0;
        maxErrorSD = 0;

        //simulate
        long simulationTime = 0;
        for( int i = 0; i < numberOfRepeations; i++ )
        {
            long time = System.currentTimeMillis();
            engine.simulate(model, new ResultListener[] {this});
            simulationTime += System.currentTimeMillis() - time;

            addMatrix(meanValues, results);
            addSquaredDiff(standartDeviationSquered, results, exactMeanValues);
            if( simulator instanceof TauLeapingSolver )
            {
                nG += ( (TauLeapingSolver)simulator ).notGillespieRuns;
                g += ( (TauLeapingSolver)simulator ).totalGillespieRuns;
            }
            if( spanIndex < 51 )
            {
                throw new Exception("Not fired at last point!!!!");
            }
            //                        showgraphics("Graphics " + i + " Solver: " + simulator.getInfo().name, results, exactMeanValues, results, exactMeanValues);
        }

        if( simulator instanceof TauLeapingSolver )
        {
            if( nG == 0 )
                System.out.print(" (pure gillespie) ");
            else
            {
                System.out.print(" (" + g / 10000.0 + "|" + nG / 10000.0 + ") ");
            }
        }


        totalTime += simulationTime;
        divideByScalar(meanValues, numberOfRepeations);
        divideByScalar(standartDeviationSquered, numberOfRepeations);

        //compare results
        int meanTestFailed = 0;
        int sdTestFailed = 0;
        if( exactSimualtor )
        {
            meanTestFailed = testMeanValues(meanValues, exactMeanValues, exactStandartDeviationValues);
            sdTestFailed = testStandartDeviation(standartDeviationSquered, exactStandartDeviationValues);
        }
        else
        {
            squereRoot(standartDeviationSquered);
            meanTestFailed = testApproximate(meanValues, exactMeanValues, true);
            sdTestFailed = testApproximate(standartDeviationSquered, exactStandartDeviationValues, false);
        }

        // showgraphics(diagram.getName(), meanValues, exactMeanValues, standartDeviationSquered, exactStandartDeviationValues);

        if( meanTestFailed == 0 && sdTestFailed == 0 )
            System.out.print(" passed successfully, simulation time: " + simulationTime);
        else
        {
            testFailed++;
            valuesFailed += ( meanTestFailed + sdTestFailed );
            System.out.println(" failed, simulation time: " + simulationTime);
            if( meanTestFailed != 0 )
                System.out.println("Mean Values failed: " + meanTestFailed + " max error = " + maxErrorMean);
            if( sdTestFailed != 0 )
                System.out.println("Standart Deviation failed: " + sdTestFailed + " max error = " + maxErrorSD);
        }

        System.out.println();
    }
    public StochasticSimulationEngine initEngine(Diagram diagram, Simulator simulator, Span span) throws Exception
    {
        StochasticSimulationEngine engine = new StochasticSimulationEngine();
        engine.setDiagram(diagram);
        engine.setSpan(span);
        engine.setSolver(simulator);
        engine.setOutputDir(javaOutPath);
        engine.setSrcDir( root + "src" );
        return engine;
    }

    public void addMatrix(double[][] target, double[][] arr)
    {
        for( int i = 0; i < target.length; i++ )
        {
            for( int j = 0; j < target[i].length; j++ )
            {
                target[i][j] += arr[i][j];
            }
        }
    }

    public void addSquaredDiff(double[][] target, double[][] arr1, double[][] arr2)
    {
        for( int i = 0; i < target.length; i++ )
        {
            for( int j = 0; j < target[i].length; j++ )
            {
                double diff = arr1[i][j] - arr2[i][j];
                target[i][j] += diff * diff;
            }
        }
    }

    public void divideByScalar(double[][] matrix, double scalar)
    {
        for( int i = 0; i < matrix.length; i++ )
        {
            for( int j = 0; j < matrix[i].length; j++ )
            {
                matrix[i][j] /= scalar;
            }
        }
    }

    public void squereRoot(double[][] matrix)
    {
        for( int i = 0; i < matrix.length; i++ )
        {
            for( int j = 0; j < matrix[i].length; j++ )
            {
                matrix[i][j] = Math.sqrt(matrix[i][j]);
            }
        }
    }

    public int testMeanValues(double[][] meanValues, double[][] exactMeanValues, double[][] exactStandartDeviationValues)
    {
        int failedTests = 0;
        for( int i = 0; i < meanValues.length; i++ )
        {
            for( int j = 0; j < meanValues[i].length; j++ )
            {
                if( exactStandartDeviationValues[i][j] == 0 )
                    continue;
                double stat = ( meanValues[i][j] - exactMeanValues[i][j] ) * Math.sqrt(numberOfRepeations)
                        / exactStandartDeviationValues[i][j];
                if( Math.abs(stat) > 3 )
                {
                    failedTests++;
                }
            }
        }
        return failedTests;
    }

    public int testStandartDeviation(double[][] standartDeviation, double[][] exactStandartDeviationValues)
    {
        int failedTests = 0;
        for( int i = 0; i < standartDeviation.length; i++ )
        {
            for( int j = 0; j < standartDeviation[i].length; j++ )
            {
                if( exactStandartDeviationValues[i][j] == 0 )
                    continue;
                double stat = ( standartDeviation[i][j] / ( exactStandartDeviationValues[i][j] * exactStandartDeviationValues[i][j] ) - 1 )
                        * Math.sqrt(numberOfRepeations / 2.0);

                if( Math.abs(stat) > 5 )
                {
                    failedTests++;
                }
            }
        }
        return failedTests;
    }

    public int testApproximate(double[][] result, double[][] exactResult, boolean mean)
    {
        int failedTests = 0;
        for( int i = 0; i < result.length; i++ )
        {
            for( int j = 0; j < result[i].length; j++ )
            {
                if( exactResult[i][j] == 0 )
                {
                    if( Math.abs(result[i][j]) > SMALL_CONSTANT_FOR_APPROXIMATE_TESTING )
                    {
                        failedTests++;
                    }
                }
                else
                {
                    double fraction = result[i][j] / exactResult[i][j];

                    if( ( fraction < 0.98 ) || ( fraction > 1.02 ) )
                    {
                        failedTests++;
                        if( mean )
                            maxErrorMean = Math.max(maxErrorMean, Math.abs(fraction - 1));
                        else
                            maxErrorSD = Math.max(maxErrorSD, Math.abs(fraction - 1));
                    }
                }
            }
        }
        return failedTests;
    }

    public void initVariableIndecesList(File file, Diagram diagram, StochasticSimulationEngine engine) throws Exception
    {
        variableIndicesList = new ArrayList<>();
        String line;
        try(BufferedReader br = ApplicationUtils.asciiReader( file ))
        {
            line = br.readLine();
        }
        if( line == null )
            throw new IllegalArgumentException("File '" + file.getName() + "' is empty");
        String[] species = TextUtil2.split( line, ',' );
        for( String specie : species )
        {
            specie = specie.trim();
            specie = specie.replace("\"", ""); //is needed because in result files there are names X and sometimes "X"
            if( specie.equals("time") || specie.equals("Time") )
                continue;
            variableIndicesList.add(getSpecies(diagram, specie, engine));
        }
        speciesNumber = variableIndicesList.size();
    }

    public int getSpecies(Diagram diagram, String st, StochasticSimulationEngine engine) throws Exception
    {
        String name = diagram.findNode( st ).getRole( VariableRole.class ).getName();
        return engine.getVarIndex(name);
    }

    public double[][] readExactValuesFromFiles(File file, Diagram diagram, StochasticSimulationEngine engine) throws Exception
    {
        double[][] exactValues = new double[sampleLength][speciesNumber];
        try(BufferedReader reader = ApplicationUtils.asciiReader( file ))
        {
            String line = reader.readLine(); //skip first line
            int i = 0;
            while( ( line = reader.readLine() ) != null )
            {
                String[] vals = TextUtil2.split( line, ',' );

                for( int j = 0; j < speciesNumber; j++ )
                {
                    exactValues[i][j] = Double.parseDouble(vals[j + 1].trim()); //skip first column "time"
                }
                i++;
            }
            return exactValues;
        }
    }


    @Override
    public void add(double t, double[] y) throws Exception
    {
        for( int i = 0; i < speciesNumber; i++ )
        {
            try
            {
                results[spanIndex][i] = y[variableIndicesList.get(i)];
                timePoints[spanIndex] = t;
            }
            catch( Exception ex )
            {
                //                System.out.println("!!!!!!!!!");
            }
        }
        spanIndex++;
    }

    @Override
    public void start(Object model)
    {
        results = new double[sampleLength][speciesNumber];
        timePoints = new double[sampleLength];
        spanIndex = 0;
    }

    public void showgraphics(String name, double[][] meanValues, double[][] exactMeanValues, double[][] sdValues, double[][] esactSDValues)
            throws Exception
    {
        for( int j = 0; j < speciesNumber; j++ )
        {
            XYSeries series1 = new XYSeries("mean");
            XYSeries series2 = new XYSeries("exactmean");
            XYSeries series3 = new XYSeries("sd");
            XYSeries series4 = new XYSeries("exactsd");

            for( int i = 0; i < sampleLength; i++ )
            {
                series1.add(i, meanValues[i][j]);
                series2.add(i, exactMeanValues[i][j]);

                series3.add(i, Math.sqrt(sdValues[i][j]));
                series4.add(i, esactSDValues[i][j]);
            }

            XYSeriesCollection collection = new XYSeriesCollection();
            collection.addSeries(series1);
            collection.addSeries(series2);

            collection.addSeries(series3);
            collection.addSeries(series4);

            JFrame frame = new JFrame("Species " + Integer.toString(j) + " from file");
            Container content = frame.getContentPane();


            JFreeChart chart = ChartFactory.createXYLineChart(name, "Time", "", collection, PlotOrientation.VERTICAL, true, // legend
                    true, // tool tips
                    false // URLs
                    );

            chart.getXYPlot().setBackgroundPaint(Color.white);
            chart.setBackgroundPaint(Color.white);
            content.add(new ChartPanel(chart));
            frame.setSize(800, 600);
            frame.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    System.exit(0);
                }
            });
            frame.setVisible(true);
        }

    }

}
