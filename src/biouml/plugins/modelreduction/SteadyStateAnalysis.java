package biouml.plugins.modelreduction;

import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.Maps;
import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Preprocessor;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.state.State;

//TODO: check the method for the composite diagrams
@ClassIcon ( "resources/steady-state-analysis.gif" )
public class SteadyStateAnalysis extends AnalysisMethodSupport<SteadyStateAnalysisParameters> implements ResultListener
{
    
    private double relativeTolerance;
    private double absoluteTolerance;
    
    /**
     * Number of consequent time points in which control variables should have the same values. Distance between them is equal to time increment of simulation engine.
     */
    private int validationSize;

    /**
     * Deque of variable values arrays. At every moment validationSize consequent arrays of are stored in memory for steady state checking.
     * Each array contains values of all control variables at certain time point.
     */
    private ArrayDeque<double[]> variableValues;

    /**
     * Simulation engine used for diagram numerical calculation
     */
    protected SimulationEngine engine;

    private double[] relativeAccuracies;
    private int[] variableIndices;
    private boolean steadyStateReached = false;
    private int badIndex = -1;
    private int timeIndex;

    public SteadyStateAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new SteadyStateAnalysisParameters());
    }

    public SteadyStateAnalysis(DataCollection<?> origin, String name, Diagram diagram)
    {
        super(origin, name, new SteadyStateAnalysisParameters(diagram));
    }

    protected SteadyStateAnalysis(DataCollection<?> origin, String name, SteadyStateAnalysisParameters parameters)
    {
        super(origin, name, parameters);
    }

    protected Model getModel(Diagram diagram)
    {
        engine = parameters.getEngineWrapper().getEngine();
        engine.setDiagram(diagram);

        try
        {
            Model model = engine.createModel();
            Map<String, Integer> varPathMapping = engine.getVarPathIndexMapping();
            String[] inputVariables = VariableSet.getVariablePaths( getParameters().getVariableNames());
            variableIndices = StreamEx.of(inputVariables).mapToInt(v->varPathMapping.get(v)).toArray();
         
            return model;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Can not generate the model because of ", ex);
            return null;
        }
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info("New Steady State Analysis started");

        Diagram input = parameters.getInput().getDataElement(Diagram.class);

        if( !parameters.getInputState().equals(Diagram.NON_STATE) )
            input.setCurrentStateName(parameters.getInputState());

        Model model = getModel(input);
        Map<String, Double> steadyStateValues = findSteadyState(model);

        if( jobControl.isStopped() )
            return null;

        if( steadyStateValues == null )
        {
            log.info("Steady state not found!");
            return null;
        }

       
        log.info("Steady state is successfully found at model time = " + steadyStateValues.get("time"));

        switch( parameters.getOutputType() )
        {
            case SteadyStateAnalysisParameters.OUTPUT_SIMULATION_RESULT_TYPE:
            {
                return saveToSimulationResult(steadyStateValues);
            }
            case SteadyStateAnalysisParameters.OUTPUT_TABLE_TYPE:
            {
                return saveToTable(steadyStateValues, input);
            }
            default:
            {
                return saveToDiagram(steadyStateValues, input);
            }
        }
    }

    public Map<String, Double> findSteadyState(Diagram diagram) throws Exception
    {
        return findSteadyState(getModel(diagram), null);
    }

    /**
     * Method is used to avoid new model generating, therefore we may have no diagram and variable name - index mappings.<br>
     * variableNames parameter will be ignored and steady state will be checked using all model variables.<br>
     * Use at your own risk!<br>
     * TODO: check correctness.
     * @param model
     * @return
     * @throws Exception
     */
    public Map<String, Double> findSteadyState(Model model) throws Exception
    {
        return findSteadyState(model, null);
    }

    public Map<String, Double> findSteadyState(SimulationEngine engine) throws Exception
    {
        return findSteadyState(engine.createModel(), engine);
    }

    public Map<String, Double> findSteadyState(Model model, SimulationEngine engine) throws Exception
    {
        if( model == null )
            return null;

        if( engine != null )
        {
            this.engine = engine;
            SimulationEngine currentEngine = parameters.getEngineWrapper().getEngine();
            if( currentEngine != null )
            {
                this.engine.setInitialTime(currentEngine.getInitialTime());
                this.engine.setCompletionTime(currentEngine.getCompletionTime());
                this.engine.setTimeIncrement(currentEngine.getTimeIncrement());
                this.engine.setOutputDir(currentEngine.getOutputDir());
            }
        }
        else
            this.engine = parameters.getEngineWrapper().getEngine();
        
        steadyStateReached = false;
        variableIndices = parameters.getVariableIndices();
        if( variableIndices == null )
        {
            Map<String, Integer> varPathMapping = this.engine.getVarPathIndexMapping();
            VariableSet[] variables = getParameters().getVariableNames();
            String[] inputVariables = ( variables.length > 0 ) ? VariableSet.getVariablePaths(getParameters().getVariableNames())
                    : findVariables(this.engine.getDiagram());
            variableIndices = StreamEx.of( inputVariables ).map( v -> varPathMapping.get( v ) ).nonNull().mapToInt( x -> x ).toArray();
        }
        timeIndex = this.engine.getVarPathIndexMapping().get("time");

        relativeTolerance = parameters.getRelativeTolerance();
        absoluteTolerance = parameters.getAbsoluteTolerance();
        validationSize = parameters.getValidationSize();
        
        this.start(model);
        this.engine.simulate(model, new ResultListener[] {this});

        if (this.engine.getSimulator().getProfile().getErrorMessage() != null)
        {
            log.info("Steady state was not reached because simulation failed");
            return null;
        }
        
        if( !steadyStateReached )
        {
            if( badIndex != -1 )
            {
                String name = StreamEx.of(this.engine.getVarPathIndexMapping().entrySet()).findAny(e -> e.getValue().equals(badIndex))
                        .map(e -> e.getKey()).get();
                Variable var = this.engine.getDiagram().getRole(EModel.class).getVariable(name);

                if( var instanceof VariableRole )
                    name = ( (VariableRole)var ).getDiagramElement().getTitle();
                log.log(Level.SEVERE, "Steady state was not reached because of variable " + name);
            }
            return null;
        }
        
//        System.out.println("Steady state reached at time "+ model.getCurrentValues()[timeIndex]);
        log.info("Steady state reached at time "+ model.getCurrentValues()[timeIndex]);
        return getSteadyStateValues(variableValues.getFirst());
    }
    
    /**
     * Method returns variable paths of all variables except for time(s) and autogenerated variables.
     * In some cases (e.g. optimization) we do not currently specify variables that should be used for steady state
     * In this case we try to determine this variables automatically
     * TODO: probably it should be rewritten in terms of VariableSet[] with (ALL VARIABLES) set
     */
    private String[] findVariables(Diagram diagram)
    {
        String diagramPath = DiagramUtility.generatPath(diagram);
        Set<String> variables = diagram.getRole( EModel.class ).getVariables().stream()
                .filter( v -> ( !v.getType().equals( Variable.TYPE_TIME ) ) )
                .filter( v -> ! ( Boolean.TRUE.equals( v.getAttributes().getValue( Preprocessor.AUTOGENERATED_VAR ) ) ) )
                .map( v -> DiagramUtility.generatPath( diagramPath, v.getName() ) ).collect( Collectors.toSet() );
        
        for (SubDiagram subdiagram: Util.getSubDiagrams(diagram))
            variables.addAll(StreamEx.of(findVariables(subdiagram.getDiagram())).toSet());
   
        return variables.toArray(new String[variables.size()]);
    }
    
    private void setValues(Diagram output, Map<String, Double> values) throws Exception
    {
        EModel emodel = output.getRole(EModel.class);

        boolean isNotify = emodel.isNotificationEnabled();
        emodel.setNotificationEnabled(true);
        if( parameters.getStateName() != null && !parameters.getStateName().isEmpty() )
        {
            State newState = new State(null, output, parameters.getStateName());
            output.addState(newState);
            output.setStateEditingMode(newState);
        }

        for( Map.Entry<String, Double> entry : values.entrySet() )
        {
            String variable = entry.getKey();

            Variable var = emodel.getVariable(variable);
            if( var != null )
                var.setInitialValue(entry.getValue());
        }
//        emodel.setInitialTime(values.get(TIME_VARIABLE));
        emodel.setNotificationEnabled(isNotify);
        output.restore();
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        if( t < parameters.getStartSearchTime() )
            return;
        if( relativeAccuracies == null )
            relativeAccuracies = new double[y.length];
        updateRelativeAccuracies(y);

        if( variableValues.size() == validationSize )
            variableValues.pollFirst();
        variableValues.add(y);

        if( jobControl.isStopped() || checkNaN(t, y) || isSteadyStateReached() )
            stopSimulation();
    }

    private void updateRelativeAccuracies(double[] y)
    {
        for( int i = 0; i < y.length; ++i )
                relativeAccuracies[i] = Math.max(relativeTolerance * y[i], relativeAccuracies[i]);
    }

    private Map<String, Double> getSteadyStateValues(double[] values)
    {
        return Maps.transformValues(engine.getVarPathIndexMapping(), index -> values[index]);
    }

    private void stopSimulation()
    {
        if( engine != null )
            engine.stopSimulation();
    }

    private boolean checkNaN(double t, double[] y)
    {
        boolean result = false;
        
        for( int i = 0; i < y.length; i++ )
        {
            if( Double.isNaN(y[i]) )
            {
                log.log(Level.SEVERE, "Value of variable " + findVariableName(i) + " turned NaN on time " + t);
                result = true;
            }
        }
        return result;
    }

    private String findVariableName(int index)
    {
        return StreamEx.ofKeys(engine.getVarIndexMapping(), v -> v == index).findAny().orElse("NOT_FOUND_VARIABLE");
    }

    /**
     * Check if we already reached steady state
     */
    private boolean isSteadyStateReached()
    {
        steadyStateReached = false;
        if( variableValues.size() < validationSize )
            return false;
           
        Iterator<double[]> iter = variableValues.iterator();
        double[] first = iter.next();
        while( iter.hasNext() )
        {
            if( !equals(first, iter.next()) )
                return false;
        }
        
        steadyStateReached = true;
        return true;
    }


    /**
     * check equality of <b>a</b> and <b>b</b> arrays, but do not take into account index <b>exclude</b>
     */
    private boolean equals(double[] a, double[] b)
    {
        double[] c = new double[a.length];

        double max = 0;
        for( int i : variableIndices )
        {
            c[i] = Math.abs(a[i] - b[i]);

            if( c[i] > max )
            {
                badIndex = i;
                max = c[i];
            }
        }

        double cMax = DoubleStreamEx.of(c).max().getAsDouble();
        return cMax < absoluteTolerance;
        //        if( cMax > relativeAccuracies[0] && cMax > absoluteTolerance && i != exclude )//!= )
        //            return false;
        //        return true;
    }

    @Override
    public void start(Object model)
    {
        variableValues = new ArrayDeque<>();
        relativeAccuracies = null;
    }

    private Diagram saveToDiagram(Map<String, Double> steadyState, Diagram input) throws Exception
    {
        Diagram output = input;
        DataCollection resultOrigin = input.getOrigin();

        if( !parameters.getOutput().equals(parameters.getInput()) )
        {
            resultOrigin = parameters.getOutput().getParentCollection();
            output = input.clone(resultOrigin, parameters.getOutput().getName());
        }

        setValues(output, steadyState); //set steady state values and parameters
        resultOrigin.put(output);
        return output;
    }

    private SimulationResult saveToSimulationResult(Map<String, Double> steadyState)
    {
        Map<String, Integer> compactVariableMap = new HashMap<>();
        double[] compactValues = new double[steadyState.size()];
        int i = 0;
        for( Map.Entry<String, Double> entry : steadyState.entrySet() )
        {
            compactValues[i] = entry.getValue();
            compactVariableMap.put(entry.getKey(), i++);
        }

        DataElementPath outputPath = parameters.getSimulationResult();
        SimulationResult simulationResult = new SimulationResult(outputPath.getParentCollection(), outputPath.getName());
        simulationResult.setDiagramPath(parameters.getInput());
        simulationResult.setVariableMap(compactVariableMap);
        simulationResult.add(0, compactValues);
        outputPath.save(simulationResult);
        return simulationResult;
    }

    private TableDataCollection saveToTable(Map<String, Double> steadyState, Diagram input)
    {
        Map<String, Integer> compactVariableMap = new HashMap<>();
        double[] compactValues = new double[steadyState.size()];
        int i = 0;
        for( Map.Entry<String, Double> entry : steadyState.entrySet() )
        {
            compactValues[i] = entry.getValue();
            compactVariableMap.put(entry.getKey(), i++);
        }

        DataElementPath outputPath = parameters.getTableResult();
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(outputPath);

        table.getColumnModel().addColumn("Initial value", DataType.Float);
        table.getColumnModel().addColumn("Value", DataType.Float);
        table.getColumnModel().addColumn("Absolute difference", DataType.Float);

        for( Entry<String, Double> e : steadyState.entrySet() )
        {
            String name = e.getKey();
            Variable var = Util.getVariable(input, name);
            if( var != null ) //there can be autogenerated variables which are not present in the diagram
            {
                double initialValue = var.getInitialValue();
                double steadyValue = e.getValue();
                double diff = Math.abs(initialValue - steadyState.get(name));
                TableDataCollectionUtils.addRow(table, e.getKey(), new Object[] {initialValue, steadyValue, diff});
            }
        }
        return table;
    }

    public static double[] select(double[] array, int[] indices)
    {
        return IntStreamEx.of(indices).mapToDouble(i -> array[i]).toArray();
    }

    protected String generateID(int index)
    {
        if(index < 10)
          return "000" + index;
        else if(index < 100)
          return "00" + index;
        else if(index < 1000)
          return "0" + index;
        return Integer.toString(index);
    }

    protected Object[] generateRow(String name, double[] values)
    {
        Object[] row = new Object[values.length + 1];
        row[0] = name;
        for( int j = 0; j < values.length; ++j )
        {
            row[j + 1] = values[j];
        }
        return row;
    }
}
