package biouml.plugins.optimization._test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysis.optimization.methods.OptimizationMethodUtils;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Equation;
import biouml.plugins.modelreduction.SteadyStateTaskParameters;
import biouml.plugins.optimization.ExperimentalTableSupport.WeightMethod;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationExperiment.ExperimentType;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.optimization.ParameterEstimationProblem;
import biouml.plugins.optimization.SimulationTaskRegistry;
import biouml.plugins.optimization.SingleExperimentParameterEstimation;
import biouml.plugins.simulation.SimulationTaskParameters;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.type.SpecieReference;

public class TestUtils
{
    private static final double k0 = 0.1;

    private static final double[] FITTING_PARAMS = new double[] {0.08, 0.04, 0.07};
    private static final String[] FITTING_PARAM_NAMES = new String[] {"k1", "k2", "k3"};

    /**
     * Creates the diagram
     * -> A -> B -> C ->
     */
    static Diagram createTestDiagram() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("d");

        //------------------------------ Create variables ------------------------------
        Node aNode = generator.createSpecies("A", 0.0);
        Node bNode = generator.createSpecies("B", 0.0);
        Node cNode = generator.createSpecies("C", 0.0);

        //------------------------------ Create reactions ------------------------------
        List<SpecieReference> speciesReferences;

        // -> A
        speciesReferences = new ArrayList<>();
        speciesReferences.add(generator.createSpeciesReference(aNode, SpecieReference.PRODUCT));
        generator.createReaction("k0", speciesReferences);

        // A -> B
        speciesReferences = new ArrayList<>();
        speciesReferences.add(generator.createSpeciesReference(aNode, SpecieReference.REACTANT));
        speciesReferences.add(generator.createSpeciesReference(bNode, SpecieReference.PRODUCT));
        generator.createReaction("k1 * $A",speciesReferences);

        // B <--> C
        speciesReferences = new ArrayList<>();
        speciesReferences.add(generator.createSpeciesReference(bNode, SpecieReference.REACTANT));
        speciesReferences.add(generator.createSpeciesReference(cNode, SpecieReference.PRODUCT));
        generator.createReaction("k2 * $B", speciesReferences);

        // C ->
        speciesReferences = new ArrayList<>();
        speciesReferences.add(generator.createSpeciesReference(cNode, SpecieReference.REACTANT));
        generator.createReaction( "k3 * $C", speciesReferences);

        // We fix the parameter k0 and evaluate parameters k1, k2, k3
        generator.getEModel().getVariable("k0").setInitialValue(k0);

        return generator.getDiagram();
    }

    /**
     * Creates the diagram
     * -> A -> B -> C ->
     */
    private static Diagram createTestDiagram2() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("d");

        generator.createEquation("x", "k0-k1*x", Equation.TYPE_RATE_BY_RULE);
        generator.createEquation("y", "k1*x-k2*y", Equation.TYPE_RATE_BY_RULE);
        generator.createEquation("z", "k2*y - k3*z", Equation.TYPE_RATE_BY_RULE);

        // We fix the parameter k0 and evaluate parameters k1, k2, k3
        generator.getEModel().getVariable("k0").setInitialValue(k0);

        return generator.getDiagram();
    }

    /**
     * Creates a problem to test the time course optimization
     */
    public static OptimizationProblem createTimeCourseProblem(OptimizationType type) throws Exception
    {
        Diagram diagram = createTestDiagram();

        ArrayList<Parameter> params = createFittingParams(getInitialValues(type));

        TableDataCollection tdc = createTimeCourseExperiment_1();
        OptimizationExperiment experiment = new OptimizationExperiment("exp_1", tdc);
        experiment.setExperimentType(ExperimentType.toString(ExperimentType.TIME_COURSE));
        experiment.setWeightMethod(WeightMethod.toString(WeightMethod.MEAN_SQUARE));
        experiment.initWeights();
        experiment.setParameterConnections(  crateAllConnections(tdc, diagram) );
        
        SimulationTaskParameters simulationParameters = new SimulationTaskParameters();
        JavaSimulationEngine jse = initSimulationEngine(diagram, 200.0);
        simulationParameters.setSimulationEngine(jse);

        return new SingleExperimentParameterEstimation(simulationParameters, experiment, params, null);
    }

    private static List<ParameterConnection> crateAllConnections(TableDataCollection tdc, Diagram diagram)
    {
        List<ParameterConnection> connections = new ArrayList<>();
        for( String colName : TableDataCollectionUtils.getColumnNames( tdc ) )
        {
            ParameterConnection connection = new ParameterConnection();
            connection.setDiagram( diagram );
            connection.setNameInDiagram( colName );
            connection.setNameInFile( colName );
            connections.add( connection );
        }
        return connections;
    }

    public static OptimizationProblem createTimeCourseProblem() throws Exception
    {
        return createTimeCourseProblem( OptimizationType.GLOBAL );
    }


    /**
     * Creates a problem to test the steady state optimization
     */
    public static OptimizationProblem createSteadyStateProblem(OptimizationType type) throws Exception
    {
        Diagram diagram = createTestDiagram();

        ArrayList<Parameter> params = createFittingParams(getInitialValues(type));
        TableDataCollection tdc = createSteadyStateExperiment_1();
        OptimizationExperiment experiment = new OptimizationExperiment("exp_1", tdc);
        experiment.setExperimentType(ExperimentType.toString(ExperimentType.STEADY_STATE));
        experiment.setWeightMethod(WeightMethod.toString(WeightMethod.MEAN_SQUARE));
        experiment.initWeights();
        experiment.setParameterConnections(  crateAllConnections(tdc, diagram) );
        SteadyStateTaskParameters simulationParameters = (SteadyStateTaskParameters) SimulationTaskRegistry.getSimulationTaskParameters(experiment);
        JavaSimulationEngine jse = initSimulationEngine(diagram, 1E5);
        simulationParameters.setDiagram( diagram );
        simulationParameters.setSimulationEngine(jse);

        return new SingleExperimentParameterEstimation(simulationParameters, experiment, params, null);
    }

    public static OptimizationProblem createSteadyStateProblem2() throws Exception
    {
        Diagram diagram = createTestDiagram2();

        ArrayList<Parameter> params = createFittingParams(getInitialValues(OptimizationType.GLOBAL));

        OptimizationExperiment experiment = new OptimizationExperiment("exp_3", createSteadyStateExperiment_3());
        experiment.setExperimentType(ExperimentType.toString(ExperimentType.STEADY_STATE));
        experiment.setWeightMethod(WeightMethod.toString(WeightMethod.MEAN_SQUARE));
        experiment.initWeights();

        SimulationTaskParameters simulationParameters = SimulationTaskRegistry.getSimulationTaskParameters(experiment);
        JavaSimulationEngine jse = initSimulationEngine(diagram, 1E5);
        simulationParameters.setDiagram( diagram );
        simulationParameters.setSimulationEngine(jse);

        return new SingleExperimentParameterEstimation(simulationParameters, experiment, params, null);
    }

    public static OptimizationProblem createSteadyStateProblem() throws Exception
    {
        return createSteadyStateProblem(OptimizationType.GLOBAL);
    }

    /**
     * Creates a problem to test the constraint optimization
     */
    public static OptimizationProblem createConstraintProblem() throws Exception
    {
        Diagram diagram = createTestDiagram();

        ArrayList<Parameter> params = createFittingParams(getInitialValues(OptimizationType.GLOBAL));
        
        //The steady state of $A
        TableDataCollection steadyExperiment = createSteadyStateExperiment_2();
        OptimizationExperiment exp_1 = new OptimizationExperiment("exp_1", steadyExperiment);
        exp_1.setExperimentType(ExperimentType.toString(ExperimentType.STEADY_STATE));
        exp_1.setWeightMethod(WeightMethod.toString(WeightMethod.MEAN_SQUARE));
        exp_1.initWeights();
        exp_1.setParameterConnections(  crateAllConnections(steadyExperiment, diagram) );
        
        //The experimental time course of $B
        TableDataCollection timeCourseExperiment = createTimeCourseExperiment_2();
        OptimizationExperiment exp_2 = new OptimizationExperiment("exp_2", timeCourseExperiment);
        exp_2.setExperimentType(ExperimentType.toString(ExperimentType.TIME_COURSE));
        exp_2.setWeightMethod(WeightMethod.toString(WeightMethod.MEAN_SQUARE));
        exp_2.initWeights();
        exp_2.setParameterConnections(  crateAllConnections(timeCourseExperiment, diagram) );
        
        List<OptimizationExperiment> experiments = new ArrayList<>();
        experiments.add(exp_1);
        experiments.add(exp_2);

        //The constraint to fix the concentration of $C
        OptimizationConstraint constr = new OptimizationConstraint();
        constr.setInitialTime(140.0);
        constr.setCompletionTime(200.0);
        constr.setFormula("abs($B - 1.75 * $C) < 0.03");

        List<OptimizationConstraint> constraints = new ArrayList<>();
        constraints.add(constr);

        OptimizationParameters optParams = new OptimizationParameters();
        optParams.setDiagram(diagram);
        optParams.setFittingParameters(params);
        optParams.setOptimizationExperiments(experiments);
        optParams.setOptimizationConstraints(constraints);

        Map<String, SimulationTaskParameters> simulationParameters = optParams.getSimulationTaskParameters();
        initSimulationEngine((JavaSimulationEngine)simulationParameters.get( "exp_1" ).getSimulationEngine(), 1E5);
        initSimulationEngine((JavaSimulationEngine)simulationParameters.get( "exp_2" ).getSimulationEngine(), 1E3);

        return new ParameterEstimationProblem(optParams);
    }
    /**
     * Creates a problem to test the optimization with relative experimental data
     */
    public static OptimizationProblem createRelativeDataProblem() throws Exception
    {
        Diagram diagram = createTestDiagram();

        ArrayList<Parameter> params = createFittingParams(getInitialValues(OptimizationType.GLOBAL));
        TableDataCollection tdc = createRelativeTimeCourseExperiment();
        OptimizationExperiment experiment = new OptimizationExperiment("exp_1", tdc);
        experiment.setExperimentType(ExperimentType.toString(ExperimentType.TIME_COURSE));
        experiment.setWeightMethod(WeightMethod.toString(WeightMethod.MEAN_SQUARE));
        initParametersConnections(experiment);
        experiment.initWeights();
        
        SimulationTaskParameters simulationParameters = new SimulationTaskParameters();
        JavaSimulationEngine jse = initSimulationEngine(diagram, 200.0);
        simulationParameters.setSimulationEngine(jse);

        return new SingleExperimentParameterEstimation(simulationParameters, experiment, params, null);
    }

    /**
     * The experimental time courses were generated with the parameter values {@link TestUtils#FITTING_PARAMS}
     */
    static TableDataCollection createTimeCourseExperiment_1()
    {
        Properties properties = new Properties();
        properties.setProperty("name", "tc_exp");

        TableDataCollection tdc = new StandardTableDataCollection(null, properties);

        tdc.getColumnModel().addColumn("time", Double.class);
        tdc.getColumnModel().addColumn("$A", Double.class);
        tdc.getColumnModel().addColumn("$B", Double.class);
        tdc.getColumnModel().addColumn("$C", Double.class);

        TableDataCollectionUtils.addRow(tdc, "0", new Object[] {0.0, 0.0, 0.0, 0.0});
        TableDataCollectionUtils.addRow(tdc, "1", new Object[] {10.0, 0.6883387948533444, 0.2717221801151035, 0.03349563350663659});
        TableDataCollectionUtils.addRow(tdc, "2", new Object[] {20.0, 0.9976293525057982, 0.7580964744021776, 0.17174784314272976});
        TableDataCollectionUtils.addRow(tdc, "3", new Object[] {40.0, 1.1990472450198237, 1.5924229199848805, 0.6016021278723264});
        TableDataCollectionUtils.addRow(tdc, "4", new Object[] {60.0, 1.2397128161822335, 2.066984601184078, 0.9699916305657151});
        TableDataCollectionUtils.addRow(tdc, "5", new Object[] {80.0, 1.2479230534013044, 2.3003428732947713, 1.1965562288217237});
        TableDataCollectionUtils.addRow(tdc, "6", new Object[] {100.0, 1.2495806717120017, 2.4092604621219715, 1.3170078872785116});
        TableDataCollectionUtils.addRow(tdc, "7", new Object[] {120.0, 1.2499153390761413, 2.4590205865789545, 1.3764557105823312});
        TableDataCollectionUtils.addRow(tdc, "8", new Object[] {140.0, 1.2499829072537445, 2.481544866891069, 1.4046272385784817});
        TableDataCollectionUtils.addRow(tdc, "9", new Object[] {160.0, 1.249996549033943, 2.4916991155539225, 1.4176751410498343});
        TableDataCollectionUtils.addRow(tdc, "10", new Object[] {180.0, 1.249999303261834, 2.496268464418288, 1.4236399990179944});
        TableDataCollectionUtils.addRow(tdc, "11", new Object[] {200.0, 1.2499998593309565, 2.4983229681853265, 1.426346556598886});

        return tdc;
    }

    /**
     * The experimental time course was generated with the parameter values {@link TestUtils#FITTING_PARAMS}
     */
    private static TableDataCollection createTimeCourseExperiment_2()
    {
        Properties properties = new Properties();
        properties.setProperty("name", "tc_exp");

        TableDataCollection tdc = new StandardTableDataCollection(null, properties);

        tdc.getColumnModel().addColumn("time", Double.class);
        tdc.getColumnModel().addColumn("$B", Double.class);

        TableDataCollectionUtils.addRow(tdc, "0", new Object[] {0.0, 0.0});
        TableDataCollectionUtils.addRow(tdc, "1", new Object[] {10.0, 0.2717221801151035});
        TableDataCollectionUtils.addRow(tdc, "2", new Object[] {20.0, 0.7580964744021776});
        TableDataCollectionUtils.addRow(tdc, "3", new Object[] {40.0, 1.5924229199848805});
        TableDataCollectionUtils.addRow(tdc, "4", new Object[] {60.0, 2.066984601184078});
        TableDataCollectionUtils.addRow(tdc, "5", new Object[] {80.0, 2.3003428732947713});
        TableDataCollectionUtils.addRow(tdc, "6", new Object[] {100.0, 2.4092604621219715});
        TableDataCollectionUtils.addRow(tdc, "7", new Object[] {120.0, 2.4590205865789545});
        TableDataCollectionUtils.addRow(tdc, "8", new Object[] {140.0, 2.481544866891069});
        TableDataCollectionUtils.addRow(tdc, "9", new Object[] {160.0, 2.4916991155539225});
        TableDataCollectionUtils.addRow(tdc, "10", new Object[] {180.0, 2.496268464418288});
        TableDataCollectionUtils.addRow(tdc, "11", new Object[] {200.0, 2.4983229681853265});

        return tdc;
    }

    /**
     * The experimental time courses were generated with the parameter values {@link TestUtils#FITTING_PARAMS}
     */
    private static TableDataCollection createRelativeTimeCourseExperiment()
    {
        Properties properties = new Properties();
        properties.setProperty("name", "tc_exp");

        TableDataCollection tdc = new StandardTableDataCollection(null, properties);

        tdc.getColumnModel().addColumn("time", Double.class);
        tdc.getColumnModel().addColumn("$A", Double.class);
        tdc.getColumnModel().addColumn("$B", Double.class);
        tdc.getColumnModel().addColumn("$C", Double.class);

        TableDataCollectionUtils.addRow(tdc, "0", new Object[] {0.0, 0.0, 0.0, 0.0});
        TableDataCollectionUtils.addRow(tdc, "1", new Object[] {10.0, 0.6883387948533444, 10.88, 2.35});
        TableDataCollectionUtils.addRow(tdc, "2", new Object[] {20.0, 0.9976293525057982, 30.34, 12.04});
        TableDataCollectionUtils.addRow(tdc, "3", new Object[] {40.0, 1.1990472450198237, 63.74, 42.18});
        TableDataCollectionUtils.addRow(tdc, "4", new Object[] {60.0, 1.2397128161822335, 82.74, 68.01});
        TableDataCollectionUtils.addRow(tdc, "5", new Object[] {80.0, 1.2479230534013044, 92.06, 83.89});
        TableDataCollectionUtils.addRow(tdc, "6", new Object[] {100.0, 1.2495806717120017, 96.44, 92.33});
        TableDataCollectionUtils.addRow(tdc, "7", new Object[] {120.0, 1.2499153390761413, 98.43, 96.5});
        TableDataCollectionUtils.addRow(tdc, "8", new Object[] {140.0, 1.2499829072537445, 99.32, 98.48});
        TableDataCollectionUtils.addRow(tdc, "9", new Object[] {160.0, 1.249996549033943, 99.73, 99.39});
        TableDataCollectionUtils.addRow(tdc, "10", new Object[] {180.0, 1.249999303261834, 99.92, 99.81});
        TableDataCollectionUtils.addRow(tdc, "11", new Object[] {200.0, 1.2499998593309565, 100, 100});

        return tdc;
    }

    private static void initParametersConnections(OptimizationExperiment experiment)
    {
        List<ParameterConnection> connections = experiment.getParameterConnections();
        for( ParameterConnection c : connections )
        {
            c.setNameInDiagram(c.getNameInFile());
        }
        connections.get(2).setRelativeTo(11);
        connections.get(3).setRelativeTo(11);
    }

    /**
     * The steady state of the test diagram was calculated by the formulas:
     * 
     * A = k0/k1; B = k0/k2; C = k0/k3
     */
    private static TableDataCollection createSteadyStateExperiment_1()
    {
        Properties properties = new Properties();
        properties.setProperty("name", "ss_exp");

        TableDataCollection tdc = new StandardTableDataCollection(null, properties);

        tdc.getColumnModel().addColumn("$A", Double.class);
        tdc.getColumnModel().addColumn("$B", Double.class);
        tdc.getColumnModel().addColumn("$C", Double.class);

        double A_ss = k0 / FITTING_PARAMS[0];
        double B_ss = k0 / FITTING_PARAMS[1];
        double C_ss = k0 / FITTING_PARAMS[2];

        TableDataCollectionUtils.addRow(tdc, "0", new Object[] {A_ss, B_ss, C_ss});

        return tdc;
    }

    /**
     * The steady state of $A was calculated by the formula: A = k0/k1.
     */
    private static TableDataCollection createSteadyStateExperiment_2()
    {
        Properties properties = new Properties();
        properties.setProperty("name", "ss_exp");

        TableDataCollection tdc = new StandardTableDataCollection(null, properties);
        tdc.getColumnModel().addColumn("$A", Double.class);
        TableDataCollectionUtils.addRow(tdc, "0", new Object[] {k0 / FITTING_PARAMS[0]});

        return tdc;
    }

    /**
     * The steady state of the test diagram was calculated by the formulas:
     * 
     * x = k0/k1; y = k0/k2; z = k0/k3
     */
    private static TableDataCollection createSteadyStateExperiment_3()
    {
        Properties properties = new Properties();
        properties.setProperty("name", "ss_exp");

        TableDataCollection tdc = new StandardTableDataCollection(null, properties);

        tdc.getColumnModel().addColumn("x", Double.class);
        tdc.getColumnModel().addColumn("y", Double.class);
        tdc.getColumnModel().addColumn("z", Double.class);

        double A_ss = k0 / FITTING_PARAMS[0];
        double B_ss = k0 / FITTING_PARAMS[1];
        double C_ss = k0 / FITTING_PARAMS[2];

        TableDataCollectionUtils.addRow(tdc, "0", new Object[] {A_ss, B_ss, C_ss});

        return tdc;
    }

    private static ArrayList<Parameter> createFittingParams(double[] initialValues) throws Exception
    {
        ArrayList<Parameter> params = new ArrayList<>();
        for( int i = 0; i < FITTING_PARAMS.length; ++i )
        {
            params.add(new Parameter(FITTING_PARAM_NAMES[i], initialValues[i], 0.0, 0.1));
        }
        return params;
    }

    private static JavaSimulationEngine initSimulationEngine(JavaSimulationEngine jse, double completionTime)
    {
        jse.setOutputDir( "../out" );
        jse.setRelTolerance( 1e-7 );
        jse.setAbsTolerance( 1e-8 );
        jse.setInitialTime( 0.0 );
        jse.setTimeIncrement( 1.0 );
        jse.setCompletionTime( completionTime );
        return jse;
    }

    private static JavaSimulationEngine initSimulationEngine(Diagram diagram, double completionTime)
    {
        JavaSimulationEngine jse = new JavaSimulationEngine();
        jse.setDiagram(diagram);
        return initSimulationEngine(jse, completionTime);
    }

    private static double[] getInitialValues(OptimizationType type)
    {
        switch( type )
        {
            case LOCAL:
                //Initial values near the solution PARAMS
                return new double[] {0.06, 0.04, 0.05};

            default:
                return new double[FITTING_PARAMS.length];
        }
    }

    public static enum OptimizationType
    {
        LOCAL, GLOBAL;
    }

    /**
     * Checks the solution
     */
    public static boolean isSolutionOk(double[] values)
    {
        return isSolutionOk(values, 0.01);
    }

    public static boolean isSolutionOk(double[] values, double accuracy)
    {
        double err = OptimizationMethodUtils.relativeError(values, FITTING_PARAMS);

        if( err < accuracy )
            return true;

        return false;
    }
}
