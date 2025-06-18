package biouml.plugins.sbml._test;

import java.io.BufferedReader;
import java.io.File;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.util.Maps;
import ru.biosoft.util.TextUtil2;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.OdeResultListenerFilter;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.DormandPrince;
import biouml.plugins.simulation.ode.ImexSD;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;


/** Batch unit test for biouml.model package. */
public class SbmlTestSimulationEngineAdapter
{
    SimulationResult simulationResult;

    OdeSimulationEngine engine = null;
    int modelType;
    String testURL;

    public static final double DEFAULT_ZERO_BIOMODELS = 1e-100;
    public static final double DEFAULT_ZERO_SEMANTIC = 1e-20;

    public static final double DEFAULT_INITIAL_STEP = 0.0;
    public static final int CONTROL_POINTS_NUMBER = 50;

    static double zero;
    static double initialStep = DEFAULT_INITIAL_STEP;

    public static final String TEST_DIRECTORY = "test directory";
    public static final String TEST_NAME = "test name";
    public static final String CSV_DIRECTORY = "csv directory";
    public static final String OUT_DIRECTORY = "out directory";

    public static final Logger log = Logger.getLogger(SbmlTestSimulationEngineAdapter.class.getName());

    public void processSemanticSuiteTest(OdeSimulationEngine engine, String testDirectory, String testName, String sbmlLevel,
            TestLogger logger, String outDirectory)
    {
        logger.testStarted(testName);

        try
        {
            zero = DEFAULT_ZERO_SEMANTIC;

            this.engine = engine;

            File sbmlModelFile = new File(testDirectory + testName + sbmlLevel + ".xml");

            long time = System.currentTimeMillis();

            Diagram diagram = SbmlModelFactory.readDiagram(sbmlModelFile, null, null);

            long time1 = System.currentTimeMillis();
            System.out.println("reading time (seconds): " + ( time1 - time ) / 1000.);
            time = time1;

            engine.setDiagram(diagram);

            EModel emodel = diagram.getRole( EModel.class );
            modelType = emodel.getModelType();

            System.out.println("model = " + testName + sbmlLevel);
            
            readSimulationParameters(engine, testDirectory, testName);
            prepareSimulatorOptions();

            simulationResult = new SimulationResult(null, "tmp");

            /**
             * @todo create some method for more precise initialization of
             * simulation engine internals without generating all the model.
             */
            File[] files = engine.generateModel(true);

            if( modelType == EModel.STATIC_TYPE )
            {
                engine.initSimulationResult(simulationResult);

                //                System.err.println("Model is completely static: no parameter is changed.");
                logger.warn("Model is completely static: no parameter is changed.");

                if( logger instanceof HtmlSemanticTestStatisticsLogger )
                {
                    HtmlSemanticTestStatisticsLogger htmlSemanticTestStatisticsLogger = (HtmlSemanticTestStatisticsLogger)logger;
                    htmlSemanticTestStatisticsLogger.setScriptName(null);
                    htmlSemanticTestStatisticsLogger.setSbmlLevel(sbmlLevel);
                    if( outDirectory != null )
                        htmlSemanticTestStatisticsLogger.setDetailsDir(outDirectory + "/details");
                }
            }
            else
            {
                logger.simulationStarted();

                String errString = engine.simulate(files, simulationResult);
                
                if( errString != null && errString.length() > 0 )
                    log.log(Level.SEVERE, errString);

                logger.simulationCompleted();

                if( logger instanceof HtmlSemanticTestStatisticsLogger )
                {
                    String modelName = engine.normalize(diagram.getName().replaceAll("\\.xml", ""));
                    String scriptName = modelName + ".java";
                    HtmlSemanticTestStatisticsLogger htmlSemanticTestStatisticsLogger = (HtmlSemanticTestStatisticsLogger)logger;
                    htmlSemanticTestStatisticsLogger.setScriptName(scriptName);
                    htmlSemanticTestStatisticsLogger.setSbmlLevel(sbmlLevel);
                    if( outDirectory != null )
                        htmlSemanticTestStatisticsLogger.setDetailsDir(outDirectory + "/details");
                }
            }

            compareResults(testDirectory, testName, logger);
            engine.clearContext();

            zero = DEFAULT_ZERO_SEMANTIC;
        }
        catch( Exception e )
        {
            logger.error(e);
        }

        logger.testCompleted();
    }
    public void processBiomodelsTest(OdeSimulationEngine engine, String sbmlLevel, TestLogger logger, Properties engineProperties)
    {
        String testDirectory = engineProperties.getProperty(TEST_DIRECTORY, "");
        String testName = engineProperties.getProperty(TEST_NAME, "");
        String csvDirectory = engineProperties.getProperty(CSV_DIRECTORY, "");
        String outDirectory = engineProperties.getProperty(OUT_DIRECTORY, "");

        zero = DEFAULT_ZERO_BIOMODELS;

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
        engine.setAbsTolerance(Double.parseDouble("1e-15"));
        engine.setRelTolerance(Double.parseDouble("1e-12"));
        engine.setSolver(new DormandPrince());

        List<String> variables = new ArrayList<>();

        readSimulationParameters2(engine, testDirectory, testName, variables);

        ( (HtmlBiomodelsTestStatisticsLogger)logger ).setSimulationEngine(engine);
        ( (HtmlBiomodelsTestStatisticsLogger)logger ).setZero(zero);
        logger.testStarted(testName);

        EModel emodel = diagram.getRole( EModel.class );
        modelType = emodel.getModelType();

        prepareSimulatorOptions();

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

                if( engine.simulate(files, new ResultListener[] {new OdeResultListenerFilter(new ResultListener[] {writer})}) != null )
                {
                    logger.error(Status.FAILED, "simulate error");
                }
            }
            catch( Exception e )
            {
                logger.error(Status.FAILED, "simulate error");
            }

            logger.simulationCompleted();

            if( logger instanceof HtmlSemanticTestStatisticsLogger )
            {
                String modelName = engine.normalize(diagram.getName().replaceAll("\\.xml", ""));
                String scriptName = modelName + ".java";
                ( (HtmlSemanticTestStatisticsLogger)logger ).setScriptName(scriptName);
                ( (HtmlSemanticTestStatisticsLogger)logger ).setSbmlLevel(sbmlLevel);
            }
        }

        try
        {
            compareBiomodelsResults(csvDirectory, outDirectory, testName, logger, variables);
            engine.clearContext();
        }
        catch( Exception e )
        {
        }

        zero = DEFAULT_ZERO_BIOMODELS;
        initialStep = DEFAULT_INITIAL_STEP;

        logger.testCompleted();
    }
    static final String[] SOLVER_EXTENSIONS = new String[] {"copasi", "CVODE", "edu.kgi.roadRunner", "Jarnac", "jsim", "Oscill8 Core",
            "SBWOdeSolver", "MathSBML", "SBToolbox2"};

    static final String[] SOLVER_COLORS = new String[] {"#ffff77", "#ffaa77", "#77ffff", "#aaffaa", "#ffaaaa", "#aaaaff", "#aa77ff",
            "#ff77aa", "#ffffaa"};

    private static SbmlCSVHander[] getCSVHandlers(String csvDirectory, String testName, TestLogger logger)
    {
        SbmlCSVHander[] handlers = new SbmlCSVHander[SOLVER_EXTENSIONS.length];
        for( int i = 0; i < SOLVER_EXTENSIONS.length; i++ )
        {
            File csvFile = new File(csvDirectory + testName + "." + SOLVER_EXTENSIONS[i] + ".csv");
            if( !csvFile.exists() )
            {
                log.warning("CSV file absents, test=" + testName + ", file=" + csvFile.getName());
            }
            else
            {
                handlers[i] = new SbmlCSVHander(csvFile, logger);
            }
        }
        return handlers;
    }

    private void compareBiomodelsResults(String csvDirectory, String outDirectory, String testName, TestLogger logger,
            List<String> variables) throws Exception
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

        if( logger instanceof HtmlBiomodelsTestStatisticsLogger )
        {
            ( (HtmlBiomodelsTestStatisticsLogger)logger ).setOutDirectory(outDirectory);
        }

        SimulationResult calculatedResult = null;

        TestStatistics testStatistics = new TestStatistics();
        testStatistics.setZero(zero);

        SbmlCSVHander[] handlers = getCSVHandlers(csvDirectory, testName, logger);

        Map<String, Integer> variablesStatistic = new HashMap<>();
        int goodSolverCount = 0;

        for( int i = 0; i < handlers.length; i++ )
        {
            if( handlers[i] != null )
            {
                List<String> variableNames = handlers[i].getVariableNames();
                List<double[]> variableValues = handlers[i].getVariableValues();

                if( variableNames == null || variableNames.size() == 0
                        || ( variableNames.size() == 1 && variableNames.get(0).equals("time") ) )
                {
                    log.warning("No variables to compare");
                }
                else
                {
                    if( !variableValues.isEmpty() )
                    {
                        testStatistics.setControlPoints(getControlPoints());
                        log.info("Control points are set");
                        testStatistics.addResult(SOLVER_EXTENSIONS[i], variableNames, variableValues);
                        for( String vName : variableNames )
                        {
                            variablesStatistic.merge(vName, 1, (a, b) -> a+b);
                        }
                        goodSolverCount++;
                    }
                    else
                    {
                        //logger.error(Status.CSV_ERROR, csvDirectory + testName + "." + SOLVER_EXTENSIONS[i] + ".csv file has wrong structure");
                    }
                }
            }
        }

        String[] requiredVariables = StreamEx.ofKeys( variablesStatistic, goodSolverCount == 1 ? val -> true : val -> val > 1 ).toArray(
                String[]::new );
        try
        {
            calculatedResult = prepareValues( requiredVariables, engine.getExecutableModel(), mangledMap, logger );
        }
        catch( Exception e )
        {
        }

        if( logger instanceof HtmlBiomodelsTestStatisticsLogger )
        {
            HtmlBiomodelsTestStatisticsLogger statisticsLogger = (HtmlBiomodelsTestStatisticsLogger)logger;
            statisticsLogger.setStatistics(testStatistics);
            statisticsLogger.setSimulationResult(calculatedResult);
        }
    }

    private double[] getControlPoints()
    {
        double[] p = new double[CONTROL_POINTS_NUMBER];
        double t1 = engine.getCompletionTime();
        double t0 = engine.getInitialTime();
        double delta = ( t1 - t0 ) / CONTROL_POINTS_NUMBER;
        for( int i = 0; i < CONTROL_POINTS_NUMBER; i++ )
            p[i] = t0 + i * delta;

        return p;
    }


    public void readSimulationParameters(OdeSimulationEngine engine, String testDirectory, String testName) throws Exception
    {
        engine.clearContext();
        engine.setInitialTime(0);

        File paramFile = new File(testDirectory + testName + ".test");
        try(BufferedReader br = ApplicationUtils.utfReader( paramFile ))
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
                else if( firstCharacter.equals("STEPS") )
                {
                    engine.setTimeIncrement( ( engine.getCompletionTime() - engine.getInitialTime() ) / Double.parseDouble(st.nextToken()));
                }
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
                else if( firstCharacter.equals("URL") )
                {
                    // ignore - URL is incorrect
                    // testURL = st.nextToken().substring(4).trim();
                }
            }
        }

        testURL = testName;

        // special trick for JDesigner
        if( testURL.endsWith("jdesigner") )
            testURL = testURL.substring(0, testURL.length() - 10);
    }

    public void readSimulationParameters2(OdeSimulationEngine engine, String testDirectory, String testName, List<String> variables)
    {
        File paramFile = new File(testDirectory + testName + ".parameters");
        try(BufferedReader br = ApplicationUtils.utfReader( paramFile ))
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
                    String[] vars = TextUtil2.split( st.nextToken(), ',' );
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

    private void prepareSimulatorOptions()
    {
        if( engine instanceof JavaSimulationEngine )
        {
            OdeSimulationEngine javaEngine = engine;
            SimulatorSupport simulatorSupport = (SimulatorSupport)javaEngine.getSimulator();
            String solverName = engine.getSolverName();
            double iStep = ( initialStep == DEFAULT_INITIAL_STEP ) ? engine.getTimeIncrement() : initialStep;
            if( "DormandPrince".equals(solverName) )
            {
                simulatorSupport.setOptions(new DormandPrince.DPOptions(iStep, false, EModel.isOfType(modelType, EModel.EVENT_TYPE),
                        OdeSimulatorOptions.STATISTICS_ON));
            }
            else if( "Imex".equals(solverName) )
            {
                simulatorSupport.setOptions(new ImexSD.ImexOptions(iStep, EModel.isOfType(modelType, EModel.EVENT_TYPE),OdeSimulatorOptions.STATISTICS_ON));
            }
        }
    }

    public void compareResults(String testDirectory, String testName, TestLogger logger) throws Exception
    {
        if(testName.contains("event-assignment/functions-event-assignment"))
        {
            System.out.println("");
        }
        Map<String, Integer> mangledMap = null;
        if( simulationResult != null )
        {
            mangledMap = getMangledNamesMap(simulationResult.getVariableMap());
        }

        if( mangledMap == null )
        {
            // make dummy empty map for it
            mangledMap = new HashMap<>();
        }

        File csvFile = new File(testDirectory + testURL + ".CSV");
        if( !csvFile.exists() )
        {
            logger.error(Status.CSV_ERROR, "CSV file absents, test=" + testName + ", file=" + csvFile.getName());
            return;
        }

        SbmlCSVHander csvHandler = new SbmlCSVHander(csvFile, logger);
        if( logger.getStatus() != Status.SUCCESSFULL )
        {
            return;
        }

        List<String> variableNames = csvHandler.getVariableNames();

        // interpolate values for required variables
        int varCount = variableNames.size();
        String[] requiredVariables = new String[varCount];
        variableNames.toArray(requiredVariables);

        SimulationResult interpolatedSimulationResult = prepareValues(requiredVariables, engine.getExecutableModel(), mangledMap, logger);

        if( logger instanceof HtmlSemanticTestStatisticsLogger )
        {
            ( (HtmlSemanticTestStatisticsLogger)logger ).setSimulationResult(interpolatedSimulationResult);
        }

        mangledMap = interpolatedSimulationResult.getVariableMap();

        if( varCount < mangledMap.size() )
            varCount = mangledMap.size();

        int[] indeces = new int[varCount];
        int counter = 0;

        for( String name : variableNames )
        {
            if( !name.equals("time") )
            {
                if( mangledMap.containsKey(name) )
                    indeces[counter++] = mangledMap.get(name).intValue();
                else
                {
                    logger.error(Status.RESULT_DIFFER, "???");
                    indeces[counter++] = -1;
                }
            }
        }

        double[] times = interpolatedSimulationResult.getTimes();

        if( times == null )
        {
            logger.error(Status.CSV_ERROR, "times == null");
            return;
        }

        double[][] simulatedValues = interpolatedSimulationResult.getValues();

        if( simulatedValues == null )
        {
            logger.error(Status.CSV_ERROR, "simulatedValues == null");
            return;
        }

        // skip initial time: we already know values at initial moment
        //        iter.next();
        for( double[] csvValues : csvHandler.getVariableValues() )
        {
            // find corresponding time in simulated values
            int _time = 0;
            //String simulatedTimes = "";
            for( ; _time < times.length; )
            {
                //simulatedTimes += times[_time] + " ";
                if( Math.abs(csvValues[0] - times[_time]) <= 1e-7 * ( Math.abs(csvValues[0]) + Math.abs(times[_time]) ) )
                    break;

                _time++;
            }

            if( _time == times.length )
            {
                logger.error(Status.RESULT_DIFFER, "No corresponding time found for value " + csvValues[0]);
                return;
            }

            for( int i = 0; i < varCount - 1; i++ )
            {
                if( indeces[i] >= 0 && i < csvValues.length - 1 )
                {
                    if( significantlyDiffer(csvValues[i + 1], simulatedValues[_time][indeces[i]]) )
                    {
                        System.err.println("values differ, row=" + _time + ", column=" + ( i + 1 ) + ", should be " + csvValues[i + 1]
                                + ", but was " + simulatedValues[_time][indeces[i]]);

                        logger.error(Status.NUMERICALLY_WRONG, "values differ, row=" + _time + ", column=" + ( i + 1 ) + ", should be "
                                + csvValues[i + 1] + ", but was " + simulatedValues[_time][indeces[i]]);
                    }
                    else if( !almostEqual(csvValues[i + 1], simulatedValues[_time][indeces[i]])
                            && logger.getStatus() != Status.NUMERICALLY_WRONG )
                    {
                        logger.error(Status.NEEDS_TUNING, "values differ, row=" + _time + ", column=" + ( i + 1 ) + ", should be "
                                + csvValues[i + 1] + ", but was " + simulatedValues[_time][indeces[i]]);
                    }
                }
            }
        }
    }

    public SimulationResult prepareValues(String[] requiredVariables, EModel model, Map<String, Integer> mangledMap, TestLogger logger)
    {
        SimulationResult newSimulationResult = new SimulationResult(simulationResult.getOrigin(), simulationResult.getName());

        boolean createInterpolation = modelType != EModel.STATIC_TYPE && modelType != EModel.STATIC_EQUATIONS_TYPE;

        SimulationResult resultToBeUsed = simulationResult;

        //extract variables map
        Map<String, VariableRole> modelVariables = new HashMap<>();
        for( Variable var : model.getVariables() )
        {
            if( var instanceof VariableRole )
            {
                VariableRole vRole = (VariableRole)var;
                String name = vRole.getName();
                if( name.indexOf("$") != -1 )
                {
                    name = name.substring(name.indexOf("$") + 1);
                }
                if( name.length() > 1 && name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"' )
                {
                    name = name.substring(1, name.length() - 1);
                }
                modelVariables.put(name, vRole);
            }
        }

        if( createInterpolation )
        {
            newSimulationResult = simulationResult.approximate(engine.getInitialTime(), engine.getCompletionTime(), engine
                    .getTimeIncrement());
            resultToBeUsed = newSimulationResult;
        }

        double oldValues[][] = resultToBeUsed.getValues();

        int n = (int) ( ( engine.getCompletionTime() - engine.getInitialTime() ) / engine.getTimeIncrement() );
        if( engine.getInitialTime() + n * engine.getTimeIncrement() < engine.getCompletionTime() )
            n += 2;
        else
            n++;

        if( oldValues != null && modelType != EModel.STATIC_EQUATIONS_TYPE )
            n = oldValues.length;

        boolean containsTime = false;
        for( int i = 0; i < requiredVariables.length; i++ )
        {
            if( !"time".equals(requiredVariables[i]) )
            {
                containsTime = true;
                break;
            }
        }
        double[] newTimes = new double[n];
        double[][] newValues = new double[n][requiredVariables.length - ( containsTime ? 1 : 0 )];
        Map<String, Integer> newVariableMap = new HashMap<>();

        int counter = 0;
        for( int i = 0; i < requiredVariables.length; i++ )
        {
            if( !requiredVariables[i].equals("time") )
            {
                if( !mangledMap.containsKey(requiredVariables[i]) )
                {
                    logger.warn("Variable '" + requiredVariables[i]
                            + "' is not contained in simulation result: will be substituted with initial value");
                }

                if( modelType == EModel.STATIC_TYPE )
                {
                    double value = getVarInitialValue(requiredVariables[i]);
                    for( int j = 0; j < n; j++ )
                        newValues[j][counter] = value;
                }
                else if( modelType == EModel.STATIC_EQUATIONS_TYPE )
                {
                    double value = Double.NaN;
                    if( mangledMap.containsKey(requiredVariables[i]) )
                    {
                        Object index = mangledMap.get(requiredVariables[i]);
                        if( index != null && oldValues != null && oldValues.length > 0 )
                        {
                            // fill it with the values calculated
                            // from static equations
                            value = oldValues[0][ ( (Integer)index ).intValue()];
                        }
                    }
                    else
                        value = getVarInitialValue(requiredVariables[i]);

                    for( int j = 0; j < n; j++ )
                        newValues[j][counter] = value;
                }
                else
                {
                    if( mangledMap.containsKey(requiredVariables[i]) )
                    {
                        // fill it in with regular simulated values
                        int index = mangledMap.get(requiredVariables[i]).intValue();
                        for( int j = 0; j < n; j++ )
                            newValues[j][counter] = oldValues[j][index];
                    }
                    else
                    {
                        // fill it in with initial values
                        double value = getVarInitialValue(requiredVariables[i]);
                        for( int j = 0; j < n; j++ )
                            newValues[j][counter] = value;
                    }
                }

                // find parent compartment size and apply it
                double compartmentSize = 1.0;
                String compartmentName = null;
                for( String vName : modelVariables.keySet() )
                {
                    int pointPos = vName.lastIndexOf(".");
                    if( pointPos != -1 )
                    {
                        if( vName.substring(pointPos + 1, vName.length()).equals(requiredVariables[i]) )
                        {
                            compartmentName = vName.substring(0, pointPos);
                            break;
                        }
                    }
                }
                if( compartmentName != null && modelVariables.containsKey(compartmentName) )
                {
                    compartmentSize = modelVariables.get(compartmentName).getInitialValue();
                }

                //                for( int j = 0; j < n; j++ )
                //                    newValues[j][counter] = newValues[j][counter] / compartmentSize;

                newVariableMap.put(requiredVariables[i], counter);

                counter++;
            }
        }

        for( int j = 0; j < n; j++ )
            newTimes[j] = engine.getInitialTime() + j * engine.getTimeIncrement();

        newSimulationResult.setTimes(newTimes);
        newSimulationResult.setValues(newValues);
        newSimulationResult.setVariableMap(newVariableMap);

        return newSimulationResult;
    }
    public SimulationResult getSimulationResult()
    {
        return simulationResult;
    }

    /////////////////////////////////////////////////////////////////////////
    //  Utilities
    //

    protected double getVarInitialValue(String varName)
    {
        for(Variable var : engine.getExecutableModel().getVariables())
        {
            if( getMangledName(var.getName()).equals(varName) )
                return var.getInitialValue();
        }
        return 0.;
    }

    public static boolean almostEqual(double a, double b)
    {
        return ( Math.abs(a) < zero && Math.abs(b) < zero ) || ! ( a < 0.999 * b || b < 0.999 * a );
        /*
         return (Math.abs(a) < 1e-20 && Math.abs(b) < 1e-20) ||
         (Math.abs(a - b) <= 5e-3 * (Math.abs(a) + Math.abs(b)));
         */
    }

    public static boolean significantlyDiffer(double a, double b)
    {
        return !almostEqual(a, b) && Math.abs(a - b) > 5e-1 * ( Math.abs(a) + Math.abs(b) );
    }

    public static String getMangledName(String name)
    {
        char[] fullName = name.toCharArray();

        int len = 0;
        for( int i = fullName.length - 1; i >= 0; i-- )
        {
            if( fullName[i] == '.' || fullName[i] == '$' )
                break;

            len++;
        }

        char[] shortName = new char[len];
        for( int i = fullName.length - 1, j = len - 1; i >= 0; i-- )
        {
            if( fullName[i] == '.' || fullName[i] == '$' )
                break;

            shortName[j--] = fullName[i];
        }
        return new String(shortName).replaceAll("\"", "");
    }

    public static Map<String, Integer> getMangledNamesMap(Map<String, Integer> variableMap)
    {
        if( variableMap == null )
            return null;

        return Maps.transformKeys( variableMap, SbmlTestSimulationEngineAdapter::getMangledName );
    }
}
