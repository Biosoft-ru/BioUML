package biouml.plugins.simulation_test.dsmts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation_test.SbmlCSVHandler;
import biouml.plugins.simulation_test.SemanticTestListParser;
import biouml.plugins.simulation_test.SimulatorTest;
import biouml.plugins.simulation_test.Status;
import biouml.plugins.simulation_test.TestDescription;
import biouml.plugins.simulation_test.TestLogger;
import biouml.plugins.stochastic.StochasticSimulationEngine;
import biouml.plugins.stochastic.solvers.GibsonBruckSolver;
import biouml.plugins.stochastic.solvers.GillespieEfficientSolver;
import biouml.plugins.stochastic.solvers.GillespieSolver;
import biouml.plugins.stochastic.solvers.MaxTSSolver;
import biouml.plugins.stochastic.solvers.TauLeapingSolver;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;

public class DSMTSSimulatorTest extends SimulatorTest
{
    
    private static final int SIMULATION_NUMBER_FACTOR = 10;

    protected String bioUMLDirectory = "../";

    protected String srcDirectory = bioUMLDirectory;;


    protected String baseDirectory = bioUMLDirectory + "data_resources/SBML tests/cases/stochastic/";

    //path to test diagram files
    protected String outDirectory = baseDirectory + "results/";

    protected String csvDirectory = outDirectory + "csvResults/";

    protected String detailsDir = outDirectory + "details";

    protected String javaOutPath = outDirectory + "java out/";

    protected String plotDirectory = outDirectory + "plots/";

    protected double initialTime = 0;
    protected double timeStep = 1;
    protected double maxTime = 50.0;

    protected int baseSimulationNumber = 1000;

    protected int simulationNumber = baseSimulationNumber;
    
    protected SimulationResult simulationResultMean;
    protected SimulationResult simulationResultSD;
    
    protected double[][] expectedSD;
    protected double[][] expectedMean;
    protected String[] varNames;
    protected double[] times;

    private boolean exactTesting = true;

    
    private List<String> failedTests = new ArrayList<>();
    
    public List<String> getFailedTests()
    {
        return failedTests;
    }

    /**
     * If test fails we rerun it up to testRuns times.
     */
    protected final static int TEST_RUN_LIMIT = 3;

    public static final int FAILED_TIME_POINTS_THRESHOLD = 10;

    public static final int FAILED_TEST_TIME_POINTS_THRESHOLD = 3;
    
    protected int currentTestRuns = 0;

    public void test()
    {
        failedTests = new ArrayList<>();
        
        simulationResult = new SimulationResult(null, "");
        testList = new SemanticTestListParser().parseTestList( new File( baseDirectory + "testList.txt" ) );//parseTestList(new File(modelListPath));

        ApplicationUtils.removeDir(new File(outDirectory));

        ( new File(javaOutPath) ).mkdirs();
        ( new File(csvDirectory) ).mkdirs();

        simulatorList = new HashMap<>();

        //        test( new GillespieSolver(), true );
        test( new GillespieEfficientSolver(), true );
        test( new GibsonBruckSolver(), true );
        //                simulator.setSeed( 5 );
        //        test( simulator, true );
        //test( new TauLeapingSolver(), false );
        //test( new MaxTSSolver(), false );
    }

    protected HashMap<String, Boolean> simulatorList;

    public void test(Simulator simulator, boolean isExactSimulator)
    {
        exactTesting = isExactSimulator;
        simulatorList.put(simulator.getInfo().name, exactTesting);

        System.out.println();
        System.out.println("Simulator " + simulator.getInfo().name + " testing");
        System.out.println();

        for( String modelName : testList )
        {
            File modelFile = new File(baseDirectory + modelName + "-sbml-l3v1.xml");
            File resultsFile = new File(baseDirectory + modelName + "-results.csv");

            if( !modelFile.exists() || !resultsFile.exists() )
            {
                System.out.println("Model " + modelName + " does not exist and was skipped");
                continue;
            }

            try
            {
                Diagram diagram = SbmlModelFactory.readDiagram(modelFile, null, modelFile.getName());
                System.out.println("The testing of " + diagram.getName());

                TestLogger logger = new DSMTSSimulationResultLogger(csvDirectory + "/" + simulator.getInfo().name, modelName);

                StochasticSimulationEngine engine = initEngine(diagram, simulator);

                currentTestRuns = 0;
                simulationNumber = baseSimulationNumber;
                executeTest(engine, logger, modelName);
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }

        System.out.println();
        System.out.println();
        System.out.println("The testing process of " + simulator.getInfo().name + " is finished.");
        //        System.out.println("Total number of failed tests: " + testFailed);
        //        System.out.println("Total number of failed comparisons: " + valuesFailed);
        //        System.out.println("Total simulation time: " + totalTime);
    }

    @Override
    protected void executeTest(SimulationEngine engine, TestLogger logger, String testName)
    {
        this.engine = engine;

        try
        {
            logger.testStarted(testName);

            if( engine instanceof StochasticSimulationEngine )
                ( (StochasticSimulationEngine)engine ).setSimulationNumber(simulationNumber);

            SbmlCSVHandler expectedHandler = new SbmlCSVHandler( new File( baseDirectory + testName + "-results.csv" ) );
            fillExpectedValues(expectedHandler);

            //simulate
            logger.simulationStarted();
            currentTestRuns++;
                
            simulationResult = engine.generateSimulationResult();
            String errString = engine.simulate( simulationResult );
            

            if( errString == null )
                saveSimulationResults( baseDirectory, testName, logger );
            else
                logger.error(Status.FAILED, errString);
            List<String> names = expectedHandler.getVariableNames();
            Set<String> selected = new HashSet<>();
            for( String name : names )
            {
                int index = name.lastIndexOf( "-sd" );
                if( index < 0 )
                    index = name.lastIndexOf( "-mean" );
                if( index < 0 )
                    continue;
                selected.add( name.substring( index ) );
            }
            Map<String, Integer> mangledMap = getMangledNamesMap( simulationResult.getVariableMap() );
            Map<String, Integer> filteredMangledMap = new HashMap<String, Integer>();
            for( String s : varNames )
                filteredMangledMap.put( s, mangledMap.get( s ) );

            boolean testPassed = checkResult( mangledMap );
            ( (DSMTSSimulationResultLogger)logger ).setVarNames( filteredMangledMap );
            //                        restart test if it failed TEST_RUN_LIMIT times;
            if( !testPassed )
            {
                if( currentTestRuns < TEST_RUN_LIMIT )
                {
                    System.out.println("Test not passed... try to increase number of simulations to " + simulationNumber * SIMULATION_NUMBER_FACTOR);
                    this.simulationNumber *= SIMULATION_NUMBER_FACTOR;
                    engine = initEngine(engine.getDiagram(), engine.getSimulator());
                    executeTest(engine, logger, testName);
                }
                else
                {
                    failedTests.add(testName);
                    System.out.println("Test run limit exceded!");
                }
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            logger.error(Status.FAILED, ex.getMessage());
        }
        logger.testCompleted();
    }

    private void saveSimulationResults(String testDirectory, String testName, TestLogger logger) throws Exception
    {
        Map mangledMap = new HashMap();
        if( simulationResult != null )
            mangledMap = getMangledNamesMap(simulationResult.getVariableMap());

        TestDescription testStatistics = new TestDescription();
        testStatistics.setSolverName(engine.getSolverName());


        if( logger.getStatus() != Status.SUCCESSFULL )
        {
            logger.setStatistics(testStatistics);
            return;
        }
        logger.setTimes(times);
        logger.setStatistics(testStatistics);

        ( (DSMTSSimulationResultLogger)logger ).setSimulationResult( simulationResult );
        ( (DSMTSSimulationResultLogger)logger ).setSimualtionNumber(simulationNumber);
        logger.simulationCompleted();
    }

    protected boolean checkResult(Map<String, Integer> varIndex)
    {
        StochasticSimulationResult result = (StochasticSimulationResult)simulationResult;

        for( int j = 0; j < varNames.length; j++ )
        {
            String varName = varNames[j];
            if( "time".equals(varName) || "Time".equals(varName) )
                continue;

            result.getValues();

            double[][] mean = simulationResult.getValues();
            double[][] sd = result.getSD();

            int index = varIndex.get( varName );

            for( int i = 0; i < times.length; i++ )
            {
                double simulatedMean = mean[i][index];
                double simulatedSD = sd[i][index];
                double testMean = expectedMean[j][i];
                double testSD = expectedSD[j][i];

                if( !CalculateDSMTSStatistics.checkMeanValues(simulatedMean, testMean, testSD, simulationNumber, exactTesting)
                        || !CalculateDSMTSStatistics.checkSDValues(simulatedSD, testSD, simulationNumber, exactTesting) )
                {
                    return false;
                }
            }
        }
        return true;
    }

    protected List<String> testList;

    protected void fillExpectedValues(SbmlCSVHandler handler)
    {
        List<String> originalNames = handler.getVariableNames();
        List<double[]> values = handler.getVariableValues();
        
        int varNumber = (originalNames.size() - 1)/2; // there are "time" variable and equal numbers of SD and Mean variables
        expectedMean = new double[varNumber][values.size()];
        expectedSD = new double[varNumber][values.size()];
        varNames = new String[varNumber];
        times = new double[values.size()];
        
        int sdCounter = 0;
        int meanCounter = 0;

        for( int j = 0; j < originalNames.size(); j++ )
        {
            String varName = originalNames.get(j);
            boolean isSD = varName.contains("-sd");
            boolean isMean =  varName.contains("-mean");

            if (varName.equals("time"))
            {
                for( int i = 0; i < values.size(); i++ )
                times[i] = values.get(i)[j];
            }
            
            if (!isSD && !isMean)
                continue;
            
            for( int i = 0; i < values.size(); i++ )
            {
                if( isSD )
                {
                    expectedSD[sdCounter][i] = values.get(i)[j];
                }
                else if( isMean )
                {
                    expectedMean[meanCounter][i] = values.get(i)[j];
                }
            }
            
            if( isSD )
            {
                varNames[sdCounter] = varName.replace("-sd", "");
                sdCounter++;
            }
            else if (isMean)
                meanCounter++;
        }
    }

    public StochasticSimulationEngine initEngine(Diagram diagram, Simulator simulator) throws Exception
    {
        Span span = new ArraySpan(0, maxTime, timeStep);
        StochasticSimulationEngine engine = new StochasticSimulationEngine();
        engine.setDiagram(diagram);
        engine.setSpan(span);
        engine.setSolver(simulator);
        engine.setOutputDir(javaOutPath);
        engine.setSrcDir(srcDirectory);
        engine.setTransformRates(false);
        return engine;
    }
}