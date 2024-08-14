package biouml.plugins.simulation_test;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.util.TextUtil;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.OdeResultListenerFilter;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.OdeSimulationEngine;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.DormandPrince;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;

public class BiomodelsSimulatorTest extends SimulatorTest
{
    protected static final String outDirectory = "C:/SBML_tests/biomodels/biomodels/results_test/";
    protected static final String testDirectory = "C:/SBML_tests/biomodels/biomodels/test_subset/";

    //private static final String outDirectory  = "F:/SBML_tests/biomodels/results_test_new/";
    //private static final String testDirectory = "F:/SBML_tests/biomodels/biomodels/test_subset2/";

    static String javaOutDirectory = outDirectory+"java/";

    public static final double DEFAULT_ZERO_BIOMODELS = 1e-100;

    protected double times[];

    public void executeBiomodelsTests(String testName)
    {
        generateTimes();

        Logger log = Logger.getLogger( BiomodelsSimulatorTest.class.getName() );

        ( new File(outDirectory) ).mkdirs();

        File file = new File(testDirectory);

        String[] tests = file.list();

        for( String testFileName : tests )
        {
            if( testFileName.endsWith(".xml") )
            {
                System.out.println(testFileName);
                JavaSimulationEngine engine = new JavaSimulationEngine();
                FunctionJobControl jobControl = new FunctionJobControl(log);
                ( engine ).setJobControl(jobControl);
                ( new File(javaOutDirectory) ).mkdirs();
                engine.setOutputDir(javaOutDirectory);
                engine.setNeedToShowPlot(false);
                TestLogger logger = new DefaultTestLogger(outDirectory, testFileName);
                try
                {
                    SbmlTestThread testThread = new SbmlTestThread(engine, logger, testFileName, this);
                    testThread.start();

                    int watchDog = 300; //try to get result only 5 minutes
                    while( testThread.isAlive() )
                    {
                        watchDog--;
                        if( watchDog == 0 )
                        {
                            testThread.stop();
                            logger.error(Status.FAILED, "time out");
                            logger.testCompleted();
                            break;
                        }
                        Thread.sleep(1000);
                    }
                }
                catch( Exception e )
                {
                    logger.error(Status.FAILED, e.getMessage());
                    logger.testCompleted();
                }
            }
        }
    }

    protected void generateTimes()
    {
        times = new double[1001];
        double t = 0.0;
        for( int i = 0; i < times.length - 1; i++ )
        {
            times[i] = t;
            t += 0.01;
        }
        times[1000] = 9.999;
    }

    @Override
    public void executeTest(SimulationEngine engine, TestLogger logger, String testName)
    {
        if( engine instanceof OdeSimulationEngine )
        {
            logger.error(Status.FAILED, "Wrong simulation engine, OdeSimulationEngine expected ");
            return;
        }

        zero = DEFAULT_ZERO_BIOMODELS;
        initialStep = DEFAULT_INITIAL_STEP;
        this.engine = engine;

        File sbmlModelFile = new File(testDirectory + testName);

        long time = System.currentTimeMillis();
        Diagram diagram = null;
        try
        {
            diagram = SbmlModelFactory.readDiagram(sbmlModelFile, null, null);
        }
        catch( Exception e )
        {
            logger.error(Status.FAILED, "Can't read SBML diagram");
            return;
        }

        long time1 = System.currentTimeMillis();
        System.out.println("reading time (seconds): " + ( time1 - time ) / 1000);
        time = time1;

        engine.setDiagram(diagram);

        // can be changed
        engine.setInitialTime(Integer.parseInt("0"));
        engine.setCompletionTime(Integer.parseInt("10"));
        engine.setTimeIncrement(Double.parseDouble("1e-2"));
        ((OdeSimulationEngine)engine).setAbsTolerance(Double.parseDouble("1e-15"));
        ((OdeSimulationEngine)engine).setRelTolerance(Double.parseDouble("1e-12"));
        engine.setSolver(new DormandPrince());

        List<String> variables = new ArrayList<>();

        readSimulationParameters2(((OdeSimulationEngine)engine), testDirectory, testName, variables);

        EModel emodel = diagram.getRole( EModel.class );
        modelType = emodel.getModelType();

        prepareSimulatorOptions(engine);

        simulationResult = new SimulationResult(null, "tmp");

        /**
         * @todo create some method for more precise initialization of
         * simulation engine internals without generating all the model.
         */
        boolean modelCreated = false;
        File[] files = null;
        try
        {
            files = engine.generateModel(true);
            modelCreated = true;
        }
        catch( Exception e )
        {
            logger.error(Status.FAILED, "Can't generate model");
        }

        if( modelType != EModel.STATIC_TYPE && modelCreated )
        {
            logger.simulationStarted();

            ResultWriter writer = null;
            try
            {
                engine.initSimulationResult(simulationResult);
                writer = new ResultWriter(simulationResult);

                if( ((OdeSimulationEngine)engine).simulate(files, new ResultListener[] {new OdeResultListenerFilter(new ResultListener[] {writer})}) != null )
                {
                    logger.error(Status.FAILED, "simulate error");
                }
            }
            catch( Exception e )
            {
                logger.error(Status.FAILED, "simulate error");
            }

            logger.simulationCompleted();
        }

        try
        {
            processBiomodelsResults(outDirectory, testName, logger, variables);
            engine.clearContext();
        }
        catch( Exception e )
        {
        }

        zero = DEFAULT_ZERO_BIOMODELS;
        initialStep = DEFAULT_INITIAL_STEP;
        logger.testCompleted();
    }

    public void readSimulationParameters2(OdeSimulationEngine engine, String testDirectory, String testName, List<String> variables)
    {
        File paramFile = new File(testDirectory + testName + ".parameters");
        try(BufferedReader br = ApplicationUtils.asciiReader(paramFile))
        {
            String line = null;

            while( ( line = br.readLine() ) != null )
            {
                if( line.trim().equals("") )
                    continue;

                StringTokenizer st = new StringTokenizer(line);

                String firstCharacter = st.nextToken();

                if( firstCharacter.equals("TIME") )
                    engine.setCompletionTime(Double.parseDouble(st.nextToken()));
                else if( firstCharacter.equals("STEP") )
                {
                    engine.setTimeIncrement(Double.parseDouble(st.nextToken()));
                }
                else if( firstCharacter.equals("ATOL") )
                {
                    engine.setAbsTolerance(Double.parseDouble(st.nextToken()));
                }
                else if( firstCharacter.equals("RTOL") )
                {
                    engine.setRelTolerance(Double.parseDouble(st.nextToken()));
                }
                else if( firstCharacter.equals("ZERO") )
                {
                    zero = Double.parseDouble(st.nextToken());
                }
                else if( firstCharacter.equals("TINC") )
                {
                    engine.setTimeIncrement(Double.parseDouble(st.nextToken()));
                }
                else if( firstCharacter.equals("ISTEP") )
                {
                    initialStep = Double.parseDouble(st.nextToken());
                }
                else if( firstCharacter.equals("SOLVER") )
                {
                    String solverClass = st.nextToken();
                    try
                    {
                        Object solver = Class.forName(solverClass).newInstance();
                        engine.setSolver(solver);
                    }
                    catch( Exception e )
                    {
                    }
                }
                else if( firstCharacter.equals("VARS") )
                {
                    String[] vars = TextUtil.split( st.nextToken(), ',' );
                    for( String v : vars )
                    {
                        variables.add(v);
                    }
                }
            }
        }
        catch( Exception e )
        {
            //do nothing if can't read params
        }
    }

    private void processBiomodelsResults(String outDirectory, String testName, TestLogger logger, List<String> variables) throws Exception
    {
        Map<String, Integer> mangledMap = null;
        if( simulationResult != null && simulationResult.getVariableMap() != null )
        {
            mangledMap = getMangledNamesMap(simulationResult.getVariableMap());
        }

        if( mangledMap == null )
        {
            // make dummy empty map for it
            mangledMap = new HashMap<>();
        }

        String[] requiredVariables = null;
        if( variables != null && variables.size() > 0 )
        {
            requiredVariables = variables.toArray(new String[variables.size()]);
        }
        else
        {
            requiredVariables = mangledMap.keySet().toArray(new String[mangledMap.keySet().size()]);
        }

        SimulationResult calculatedResult = null;

        TestDescription testStatistics = new TestDescription();
        testStatistics.setZero(zero);
        testStatistics.setAtol(((OdeSimulationEngine)engine).getAbsTolerance());
        testStatistics.setRtol(((OdeSimulationEngine)engine).getRelTolerance());
        testStatistics.setStep(engine.getTimeIncrement());
        testStatistics.setSolverName(engine.getSolverName());

        try
        {
            calculatedResult = prepareValues(requiredVariables, mangledMap, logger);
        }
        catch( Exception e )
        {
            calculatedResult = simulationResult;
            logger.error(Status.FAILED, e.getMessage());
        }

        logger.setTimes(times);
        logger.setStatistics(testStatistics);
        logger.setSimulationResult(calculatedResult);
    }
}
