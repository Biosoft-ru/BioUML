package biouml.plugins.simulation_test;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.converters.SBGNCompositeConverter;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.IterationType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.JacobianType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.Method;
import biouml.plugins.simulation_test.fbc.FbcSbmlTest;
import biouml.plugins.simulation_test.fbc.FbcSbmlTest.FBCLogger;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.state.State;
import biouml.workbench.diagram.ImageExporter;
import biouml.workbench.graph.DiagramToGraphTransformer;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.graph.FastGridLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.Maps;
import ru.biosoft.util.TextUtil2;

public class SemanticSimulatorTest extends SimulatorTest
{
    private static final String bioUMLDirectory = "../";

    //path to src directory of BioUML
    private static final String srcDirectory = bioUMLDirectory;


    private String baseDirectory = bioUMLDirectory + "data_resources/SBML tests/cases/";
    //        static String baseDirectory = bioUMLDirectory + "data_resources/SBML tests/test_suite_v2.1.0/cases/";
    //    static String baseDirectory = bioUMLDirectory + "data_resources/SBML tests/sbml-test-cases-2.0.2/cases/";
    //            static String baseDirectory = bioUMLDirectory + "data_resources/SBML tests/";

    //path were to put results
    private String testDirectory = baseDirectory + "semantic/";

    //path to test diagram files
    private String outDirectory = baseDirectory + "results/";

    private String bioumlSettingsDirectory = baseDirectory + "BioUML-settings/";

    //    private String detailsDir = outDirectory + "details";

    //path for generated java code (models)
    private String javaOutDirectory = outDirectory + "/java_out/";

    private final String figsOutDirectory = outDirectory + "/figs_out/";
    private String solverName;

    //if true it will copy all csv results to separate zip file which can be later uploaded to SBML Test Suite Database (http://sbml.org/Facilities/Database)
    private boolean extractCSV = false;

    public final boolean generateImages = false;

    //default simulation tolerances: all SBML tests passes with those parameters
    public static final double DEFAULT_ZERO_SEMANTIC = 1E-15;
    public static final double DEFAULT_ATOL_SEMANTIC = 1E-12;
    public static final double DEFAULT_RTOL_SEMANTIC = 1E-10;

    //those are sued for solver options
    protected double simulationATOL = DEFAULT_ATOL_SEMANTIC;//1E-18;
    protected double simulationRTOL = DEFAULT_RTOL_SEMANTIC;//1E-14;

    protected double startTime = 0;
    protected double completionTime = 0;
    protected double timeIncrement = 0;

    protected String sbmlLevel;
    protected String testURL;

    protected Span span;

    protected boolean timeCourseTest = true;

    protected ArrayList<String> generatedFoldersList = new ArrayList<>();

    public SemanticSimulatorTest()
    {

    }

    public SemanticSimulatorTest(boolean oldTest)
    {
        this.oldTest = oldTest;
    }

    public void setBaseDirectory(String baseDirectory)
    {
        this.baseDirectory = baseDirectory;
        this.testDirectory = baseDirectory + "semantic/";
        //        this.outDirectory = baseDirectory + "results/";
        //        this.javaOutDirectory = outDirectory + "/java_out/";
    }

    public void setOutDirectory(String outDirectory)
    {
        //        this.testDirectory = baseDirectory + "semantic/";
        this.outDirectory = outDirectory;
        this.javaOutDirectory = outDirectory + "/java_out/";
        //        this.outWithCategories = outDirectory + "with categories";
        //        this.outWithoutCategories = outDirectory + "without categories";
    }

    public boolean oldTest = false;

    public void executeSemanticTests(String testName)
    {
        try
        {
            setPreferences();
            ApplicationUtils.removeDir(new File(outDirectory));

            //            testAgentBased("l3v1");

            //Jvode
            if( oldTest )
            {
                testJVode(Method.BDF, IterationType.NEWTON, JacobianType.DENSE, "l1v2");
                testJVode(Method.ADAMS, IterationType.NEWTON, JacobianType.DENSE, "l2v1");
                testJVode(Method.BDF, IterationType.NEWTON, JacobianType.DENSE, "l2v2");
                testJVode(Method.ADAMS, IterationType.NEWTON, JacobianType.DENSE, "l2v3");
                testJVode(Method.BDF, IterationType.NEWTON, JacobianType.DENSE, "l2v4");
                testJVode(Method.BDF, IterationType.NEWTON, JacobianType.DENSE, "l3v1");
            }
            else
            {
                testJVode(Method.ADAMS, IterationType.NEWTON, JacobianType.DENSE, "highest");
            }

            //            testSimulator(new DormandPrince(), "DP","l1v2");
            //            testSimulator(new DormandPrince(), "DP","l2v1");
            //            testSimulator(new DormandPrince(), "DP","l2v2");
            //            testSimulator(new DormandPrince(), "DP","l2v3");
            //            testSimulator(new DormandPrince(), "DP","l2v4");
            //            testSimulator(new DormandPrince(), "DP", "l3v1");
            //            testSimulator(new ImexSD(), "Imex", "l3v1");
            //            testSimulator(new Radau5(), "Radau5", "l3v1");
            //            testSimulator(new EulerSimple(), "Euler", "l3v1");
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    public List<String> getGeneratedFoldersList()
    {
        return generatedFoldersList;
    }

    public void testAgentBased(String lv) throws Exception
    {
        this.solverName = "agent";
        String folderName = "agent" + "_" + lv;
        generatedFoldersList.add(folderName);
        engine = new AgentModelSimulationEngine();
        testSimulationEngine("testList.txt", engine, "-sbml-" + lv, folderName);
    }

    public void testJVode(Method method, IterationType iter, JacobianType jac, String lv) throws Exception
    {
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setJobControl(new FunctionJobControl(Logger.getLogger(SemanticSimulatorTest.class.getName())));

        String name = "";
        if( method == Method.BDF )
            name = "BDF";
        else if( method == Method.ADAMS )
            name = "Adams";

        if( iter == IterationType.FUNCTIONAL )
        {
            name += "_functional";
        }
        else
        {
            name += "_newton";
            switch( jac )
            {
                case DENSE:
                {
                    name += "_dense";
                    break;
                }
                case BAND:
                {
                    name += "_band";
                    break;
                }
                case DIAG:
                {
                    name += "_diag";
                    break;
                }
                default:
                    break;
            }
        }
        JVodeSolver solver = new JVodeSolver();
        JVodeOptions options = new JVodeOptions(method, iter, jac);
        options.setHMaxInv(10000);
        solver.setOptions(options);
        engine.setSolver(solver);

        String folderName = name + "_" + lv;
        this.solverName = name;
        generatedFoldersList.add(folderName);
        testSimulationEngine("testList.txt", engine, lv, folderName);
    }
    public void testSimulator(Simulator simulator, String simulatorName, String lv) throws Exception
    {
        Logger log = Logger.getLogger(SemanticSimulatorTest.class.getName());

        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setJobControl(new FunctionJobControl(log));
        engine.setSolver(simulator);

        solverName = engine.getSolverName();
        String folderName = solverName + "_" + lv;
        generatedFoldersList.add(folderName);
        testSimulationEngine("testList.txt", engine, "-sbml-" + lv, folderName);
    }

    public void testSimulationEngine(String testListName, SimulationEngine engine, String sbmlLevel, String testPostfix) throws Exception
    {
        ( new File(javaOutDirectory) ).mkdirs();
        engine.setOutputDir(javaOutDirectory);
        engine.setSrcDir(srcDirectory);
        engine.setNeedToShowPlot(false);

        if( engine instanceof JavaSimulationEngine )
            ( (JavaSimulationEngine)engine ).setTemplateType(JavaSimulationEngine.TEMPLATE_LARGE_ONLY);

        this.sbmlLevel = sbmlLevel;
        List<String> testList = SemanticTestListParser.parseTestList(new File(testDirectory + testListName));
        for( String testName : testList )
        {
            TestLogger logger = new DefaultTestLogger(outDirectory + testPostfix + "/csvResults/", testName + "/" + testName);
            executeTest(engine, logger, testName + "/" + testName);
            logger.complete();
        }

        if( extractCSV )
            retrieveCSVResult(outDirectory + testPostfix + "/csvResults", outDirectory + testPostfix + "/BioUML results.zip");
    }

    private static void retrieveCSVResult(String path, String zipArchive) throws Exception
    {
        File inputDir = new File(path);
        File targetDir = new File(zipArchive);
        targetDir.delete();
        targetDir.createNewFile();
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(targetDir)))
        {
            for( File csv : StreamEx.of(inputDir.listFiles())
                    .flatMap(f -> StreamEx.of(f.listFiles()).filter(csv -> csv.getName().endsWith(".csv"))) )
            {
                ZipEntry entry = new ZipEntry(csv.getName());
                zip.putNextEntry(entry);
                zip.write(Files.readAllBytes(csv.toPath()));
                zip.closeEntry();
            }
        }
    }

    private File findHighestLevel(String testName)
    {
        String[] levels = new String[] {"l3v2", "l3v1", "l2v5", "l2v4", "l2v3", "l2v2", "l2v1", "l1v2"};

        for( int i = 0; i < levels.length; i++ )
        {
            String modelPath = testDirectory + testName + "-sbml-" + levels[i] + ".xml";
            File sbmlModelFile = new File(modelPath);
            if( sbmlModelFile.exists() )
                return sbmlModelFile;
        }
        return null;
    }

    @Override
    public void executeTest(SimulationEngine engine, TestLogger logger, String testName)
    {
        logger.testStarted(testName);

        System.out.println("Model name = " + testName);

        Diagram initialDiagram = null;
        Diagram processedDiagram = null;
        String errString = null;
        try
        {
            zero = DEFAULT_ZERO_SEMANTIC;

            File sbmlModelFile = ( sbmlLevel.equals("highest") ) ? findHighestLevel(testName)
                    : new File(testDirectory + testName + "-sbml-" + sbmlLevel + ".xml");

            if( sbmlModelFile == null || !sbmlModelFile.exists() )
            {
                logger.warn("SBML model " + testName + " for level " + sbmlLevel + " was not found");
                System.out.println("SBML model " + testName + " was not found");
                logger.simulationCompleted();
                saveSimulationResults(testDirectory, testName, logger);
                return;
            }
            Diagram diagram = SbmlModelFactory.readDiagram(sbmlModelFile, null, null);

            if( diagram == null )
            {
                logger.error(Status.MODEL_NOT_SUPPORTED, errString);
                logger.testCompleted();
                saveSimulationResults(testDirectory, testName, logger);
            }

            initialDiagram = diagram.clone(null, diagram.getName());
            setOutputToAmount(diagram);//we will transform all species that are required to concentration manually after
            
            this.engine = engine;
            
            if( isFBC(diagram) )
            {
                logger.testStarted(testName);

                timeCourseTest = false;
                readSimulationParameters(engine, testDirectory, testName);
                FbcSbmlTest test = new FbcSbmlTest();
                logger = new FBCLogger(logger.getOutputPath(), testName);
                test.setTestDir(testDirectory);

                solverName = "Simplex method";
                System.out.println("Solver name = " + solverName);
                logger.simulationStarted();
                test.testDiagram(diagram, testName, logger);
                logger.simulationCompleted();

                saveSimulationResults(testDirectory, testName, logger);
                logger.testCompleted();

                if( generateImages )
                    outputFigures(diagram, null, TextUtil2.split(testName, '/')[0]);
                return;
            }
            else
                timeCourseTest = true;

            
            engine.setDiagram(diagram);
            readSimulationParameters(engine, testDirectory, testName);

            span = new UniformSpan(startTime, completionTime, timeIncrement);
            initEngine(engine);

            prepareSimulatorOptions(engine);
            readBioUMLSettings(testName, engine);

            simulationResult = new SimulationResult(null, "tmp");

            logger.simulationStarted();

            Model model = null;
            try
            {
                model = engine.createModel();
            }
            catch( Exception ex )
            {
                errString = "Compilation error:" + ex.getMessage();
                ex.printStackTrace();
                return;
            }

            if( model != null )
                errString = engine.simulate(model, simulationResult);
            else
            {
                logger.error(Status.FAILED, errString);
                logger.simulationCompleted();
                saveSimulationResults(testDirectory, testName, logger);
            }

            if( errString != null )
            {
                if( errString.equals(JavaSimulationEngine.UNSTABLE_PROBLEM) )
                    logger.error(Status.FAILED, errString);
                else if( errString.equals(JavaSimulationEngine.STIFF_PROBLEM) )
                    logger.error(Status.PROBLEM_IS_STIFF, errString);
                else if( errString.contains("Compilation error:") )
                    logger.error(Status.COMPILATION_FAILED, errString);
                else
                    logger.error(Status.FAILED, errString);
            }
            logger.simulationCompleted();

            String modelName = engine.normalize(diagram.getName().replaceAll("\\.xml", ""));
            String scriptName = modelName;
            scriptName += DiagramUtility.isComposite(initialDiagram) ? "_plain.java" : ".java";
            logger.setScriptName(scriptName);

            saveSimulationResults(testDirectory, testName, logger);
            engine.clearContext();

            zero = DEFAULT_ZERO_SEMANTIC;
        }
        catch( Exception e )
        {
            e.printStackTrace();
            saveSimulationResults(testDirectory, testName, logger);
            engine.clearContext();
            logger.error(Status.FAILED, e.getMessage());
        }

        logger.testCompleted();

        //test is finished - generate image
        if( generateImages && initialDiagram != null )
        {
            try
            {
                if( DiagramUtility.isComposite(initialDiagram) )
                    processedDiagram = engine.getDiagram();

                outputFigures(initialDiagram, processedDiagram, TextUtil2.split(testName, '/')[0]);
            }
            catch( Exception e )
            {
            }
        }
    }

    protected void initJavaSimulationEngine(JavaSimulationEngine engine)
    {
        engine.setRelTolerance(simulationRTOL);
        engine.setAbsTolerance(simulationATOL);
    }

    protected void initEngine(SimulationEngine engine)
    {
        initEngine(engine, 1);
    }

    protected void initEngine(SimulationEngine engine, double timeScale)
    {
        if( engine instanceof JavaSimulationEngine )
            initJavaSimulationEngine((JavaSimulationEngine)engine);

        engine.setInitialTime(startTime / timeScale);
        engine.setCompletionTime(completionTime / timeScale);
        engine.setTimeIncrement(timeIncrement / timeScale);

        if( engine instanceof AgentSimulationEngineWrapper )
            initEngine( ( (AgentSimulationEngineWrapper)engine ).getEngine(), timeScale);

//        if( engine instanceof AgentModelSimulationEngine )
//        {
//            initEngine( ( (AgentModelSimulationEngine)engine ).getMainEngine(), timeScale);
//            for( AgentSimulationEngineWrapper subEngine : ( (AgentModelSimulationEngine)engine ).getEngines() )
//                initEngine(subEngine, subEngine.getTimeScale());
//        }
    }

    public void readSimulationParameters(SimulationEngine engine, String testDirectory, String testName) throws Exception
    {
        engine.setInitialTime(0);

        String time = "duration:";
        String steps = "steps:";
        String step = "step:";
        String atolTag = "absolute:";
        String rtolTag = "relative:";
        String amount = "amount:";
        String concentration = "concentration:";
        String extension = "-settings.txt";

        File paramFile = new File(testDirectory + testName + extension);
        String line = null;

        outputInAmount = new ArrayList<>();
        outputInConcentration = new ArrayList<>();

        try (BufferedReader br = ApplicationUtils.utfReader(paramFile))
        {
            while( ( line = br.readLine() ) != null )
            {
                if( line.trim().equals("") )
                    continue;

                StringTokenizer st = new StringTokenizer(line);

                String firstCharacter = st.nextToken();


                if( firstCharacter.equals(time) )
                {
                    if( st.hasMoreTokens() )
                        completionTime = Double.parseDouble(st.nextToken());
                }
                else if( firstCharacter.equals(steps) )
                {
                    if( st.hasMoreTokens() )
                        timeIncrement = ( completionTime - startTime ) / Double.parseDouble(st.nextToken());
                }
                else if( firstCharacter.equals(step) )
                {
                    if( st.hasMoreTokens() )
                        timeIncrement = Double.parseDouble(st.nextToken());
                }

                // next are not simulation parameters! They are used only form result comparison
                else if( firstCharacter.equals(atolTag) )
                {
                    if( st.hasMoreTokens() )
                        atol = Double.parseDouble(st.nextToken());
                }
                else if( firstCharacter.equals(rtolTag) )
                {
                    if( st.hasMoreTokens() )
                        rtol = Double.parseDouble(st.nextToken());
                }
                else if( firstCharacter.equals("ZERO") )
                {
                    if( st.hasMoreTokens() )
                        zero = Double.parseDouble(st.nextToken());
                }
                else if( firstCharacter.equals(amount) )
                {
                    while( st.hasMoreTokens() )
                    {
                        String name = st.nextToken().replace(",", "");
                        outputInAmount.add(name.trim());
                    }
                }
                else if( firstCharacter.equals(concentration) )
                {
                    while( st.hasMoreTokens() )
                    {
                        String name = st.nextToken().replace(",", "");
                        outputInConcentration.add(name.trim());
                    }
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
    public void saveSimulationResults(String testDirectory, String testName, TestLogger logger)
    {
        String addPostfix = "-results";

        File csvFile = new File(testDirectory + testURL + addPostfix + ".csv");
        if( !csvFile.exists() )
        {
            logger.error(Status.CSV_ERROR, "CSV file absents, test=" + testName + ", file=" + csvFile.getName());
            return;
        }

        TestDescription testStatistics = new TestDescription("-sbml-" + sbmlLevel, solverName);

        if( logger.getStatus() != Status.SUCCESSFULL )
        {
            logger.setStatistics(testStatistics);
            return;
        }

        try
        {
            SbmlCSVHandler csvHandler = new SbmlCSVHandler(csvFile, timeCourseTest);

            if( timeCourseTest )
            {
                processResult(simulationResult);

                int resultLength = simulationResult.getValues().length;
                int spanSize = span.getLength();
                if( resultLength != spanSize )
                    System.out.println("Simulation result contains " + resultLength + " time points, while " + spanSize + " are needed.");


                Map<String, Integer> mangledMap = getMangledNamesMap(simulationResult.getVariableMap());

                if( mangledMap == null ) // make dummy empty map for it
                    mangledMap = new HashMap<>();

                // interpolate values for required variables
                String[] requiredVariables = StreamEx.of(csvHandler.getVariableNames()).toArray(String[]::new);
                SimulationResult interpolatedSimulationResult = prepareValues(requiredVariables, mangledMap, logger);

                if( interpolatedSimulationResult == null )
                {
                    logger.error(Status.FAILED, "Error when creating interpolated simulation result.");
                    return;
                }
                logger.setSimulationResult(interpolatedSimulationResult);
            }

            testStatistics.setZero(zero);
            testStatistics.setAtol(atol);
            testStatistics.setRtol(rtol);
            testStatistics.setStep(engine != null ? engine.getTimeIncrement() : 0);

            logger.setTimes(csvHandler.getTimes());
            logger.setStatistics(testStatistics);
        }
        catch( Exception ex )
        {
            logger.setStatistics(testStatistics);
            return;
        }
    }

    public static Map<String, Integer> getMangledNamesMap(Map<String, Integer> variableMap)
    {
        return variableMap == null ? null : Maps.transformKeys(variableMap, SemanticSimulatorTest::getMangledName);
    }

    /**
     * Converting result values to quantity types specified by result settings
     * Note: result quantity type is not specified by SBML, in BioUML it is the same as initial quantity type
     * @param result
     */
    protected void processResult(SimulationResult result)
    {
        //find parent compartment size and apply it;
        Map<String, Integer> variableIndices = simulationResult.getVariableMap();
        double[][] values = simulationResult.getValues();
        for( Map.Entry<String, Integer> entry : variableIndices.entrySet() )
        {
            String variableName = entry.getKey();
            if( !variableName.contains(".") )//means it is root compartment
                continue;
            boolean resultConcentration = this.outputInConcentration.contains(getMangledName(variableName));
            if( resultConcentration )
            {
                String compartmentName = variableName.substring(0, variableName.lastIndexOf("."));
                for( int i = 0; i < values.length; i++ )
                    values[i][entry.getValue()] /= values[i][variableIndices.get(compartmentName)];
            }
        }

        Map<String, String> replaces = new HashMap<>();
        EModel emodel = engine.getExecutableModel();
        for( Entry<String, Integer> entry : result.getVariableMap().entrySet() )
        {
            String key = entry.getKey();
            if( !key.endsWith("_CONFLICTS_WITH_CONSTANT_") )
                continue;
            String suspect = key.substring(0, key.length() - 25);
            if( emodel.containsConstant(suspect) )
                replaces.put(key, suspect);
        }

        for( Entry<String, String> replace : replaces.entrySet() )
        {
            Integer index = result.getVariableMap().get(replace.getKey());
            result.getVariableMap().remove(replace.getKey());
            result.getVariableMap().put(replace.getValue(), index);
        }

    }

    private boolean isFBC(Diagram diagram)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty("Packages");
        if( dp != null && dp.getValue() instanceof String[] )
        {
            for( String packageName : (String[])dp.getValue() )
            {
                if( "fbc".equals(packageName) )
                    return true;
            }
        }
        return false;
    }

    protected void layout(Diagram diagram) throws Exception
    {
        FastGridLayouter layouter = new FastGridLayouter();
        PathwayLayouter pl = new PathwayLayouter(layouter);

        DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
        DiagramViewOptions viewOptions = diagram.getViewOptions();
        for( SubDiagram node : diagram.stream(SubDiagram.class) )
        {
            node.setFixed(true);
            viewBuilder.createCompartmentView(node, viewOptions, ApplicationUtils.getGraphics());
            node.stream(Node.class).filter(Util::isPort).forEach(n -> {
                CompositeSemanticController.movePortToEdge(n, node, new Dimension(0, 0), false);
            });
            node.setView(null);
        }

        boolean notificationEnabled = diagram.isNotificationEnabled();
        //        boolean propagationEnabled = diagram.isPropagationEnabled();
        diagram.setNotificationEnabled(false);
        //        diagram.setPropagationEnabled(false);
        State currentState = diagram.getCurrentState();
        if( currentState != null )
            currentState.getStateUndoManager().undoDeleted();
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        pl.doLayout(graph, null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
        if( currentState != null )
            currentState.getStateUndoManager().redoDeleted();
        diagram.setNotificationEnabled(notificationEnabled);
    }
    protected void outputFigures(Diagram diagram, Diagram processedDiagram, String testName) throws Exception
    {
        File figsDir = new File(figsOutDirectory + testName + "/");
        figsDir.mkdirs();

        HashMap<String, List<String>> fileHierarchy = new HashMap<>();
        SBGNConverterNew converter;

        //        output processed (plain) diagram
        if( processedDiagram != null )
        {
            //patch: remove it with preprocessor
            for( Node node : processedDiagram.getNodes() )
            {
                if( Util.isPort(node) )
                    processedDiagram.remove(node.getName());
            }
            converter = new SBGNCompositeConverter();
            processedDiagram = converter.convert(processedDiagram, null);
            ImageExporter imageWriter = new ImageExporter();
            Properties properties = new Properties();
            properties.setProperty(DataElementExporterRegistry.FORMAT, "PNG");
            properties.setProperty(DataElementExporterRegistry.SUFFIX, ".png");
            imageWriter.init(properties);
            File file = new File(figsDir, processedDiagram.getName() + ".png");
            layout(processedDiagram);
            imageWriter.doExport(processedDiagram, file);
            List<String> finalDiagrams = new ArrayList<>();
            finalDiagrams.add(processedDiagram.getName());
            fileHierarchy.put("processed", finalDiagrams);
        }

        diagram = SBGNConverterNew.convert(diagram);

        generateFigs(figsDir, diagram.getName(), diagram, fileHierarchy);

        File f = new File(figsDir, "figures.txt");
        try (BufferedWriter bw = ApplicationUtils.utfWriter(f))
        {
            for( Map.Entry<String, List<String>> entry : fileHierarchy.entrySet() )
            {
                bw.write(entry.getKey());
                for( String child : entry.getValue() )
                {
                    bw.write("\t");
                    bw.write(child);
                }
                bw.write("\n");
            }
        }
    }


    protected void generateFigs(File outDir, String fileName, Diagram diagram, HashMap<String, List<String>> fileHierarchy) throws Exception
    {
        ImageExporter imageWriter = new ImageExporter();
        Properties properties = new Properties();
        properties.setProperty(DataElementExporterRegistry.FORMAT, "PNG");
        properties.setProperty(DataElementExporterRegistry.SUFFIX, ".png");
        imageWriter.init(properties);
        File file = new File(outDir, fileName + ".png");
        layout(diagram);
        imageWriter.doExport(diagram, file);

        List<String> childFiles = new ArrayList<>();

        if( DiagramUtility.isComposite(diagram) )
        {
            for( SubDiagram subDiagram : diagram.stream(SubDiagram.class) )
            {
                Diagram innerDiagram = subDiagram.getDiagram();
                boolean notificationEnabled = innerDiagram.isNotificationEnabled();
                innerDiagram.setNotificationEnabled(false);
                if( subDiagram.getState() != null )
                    innerDiagram.setStateEditingMode(subDiagram.getState());
                String newFileName = fileName + "_" + subDiagram.getName();
                childFiles.add(newFileName);
                generateFigs(outDir, newFileName, subDiagram.getDiagram(), fileHierarchy);
                innerDiagram.setNotificationEnabled(notificationEnabled);
            }
        }
        fileHierarchy.put(fileName, childFiles);
    }


    protected void setPreferences() throws Exception
    {
        CollectionFactory.createRepository("../data");
        Application.setPreferences(new Preferences());
    }

    //??
    protected static void setOutputToAmount(Diagram diagram)
    {
        diagram.getRole(EModel.class).getVariableRoles().forEach(v -> v.setOutputQuantityType(VariableRole.AMOUNT_TYPE));
        diagram.recursiveStream().select(SubDiagram.class).forEach(s -> setOutputToAmount(s.getDiagram()));
    }

    /**
     * Reads BioUML-specific settings (needed for some tests)
     */
    private void readBioUMLSettings(String testName, SimulationEngine engine) throws Exception
    {
        testName = testName.split("/")[0];
        File settingsFile = new File(bioumlSettingsDirectory + testName + "-SETTINGS.txt");
        if( !settingsFile.exists() )
            return;
        List<String> settings = ApplicationUtils.readAsList(settingsFile);

        for( String s : settings )
        {
            String[] parts = s.split(" ");
            if( parts.length > 1 )
            {
                switch( parts[0] )
                {
                    case "timeStep":
                    {
                        engine.setTimeIncrement(Double.parseDouble(parts[1]));
                        break;
                    }
                }
            }
        }
    }
}
