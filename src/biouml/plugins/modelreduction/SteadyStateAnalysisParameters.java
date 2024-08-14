package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineWrapper;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@PropertyName("Parameters")
public class SteadyStateAnalysisParameters extends AbstractAnalysisParameters
{
    protected static final String OUTPUT_DIAGRAM_TYPE = "Diagram";
    protected static final String OUTPUT_SIMULATION_RESULT_TYPE = "Simulation Result"; 
    protected static final String OUTPUT_TABLE_TYPE = "Table"; 
        
    private DataElementPath input;
    private String inputState = Diagram.NON_STATE;
    private Diagram diagram;
    private DataElementPath output;
    private DataElementPath simulationResult;
    private DataElementPath tableResult;
    
    private SimulationEngineWrapper engineWrapper;
    private double startSearchTime = 0.0;
    private int validationSize = 100;
    private double absoluteTolerance = 1.0E-15;
    private double relativeTolerance = 1.0E-3;
    private String stateName;
    private VariableSet[] variableNames = new VariableSet[] {};
    int[] variableIndices = null; //allows to bypass variableNames
    
    private String outputType = OUTPUT_DIAGRAM_TYPE;
    
    public SteadyStateAnalysisParameters()
    {
        setEngineWrapper(new SimulationEngineWrapper());
    }

    public SteadyStateAnalysisParameters(Diagram diagram)
    {
        setEngineWrapper(new SimulationEngineWrapper(diagram));
        initDiagram( diagram );
    }

    @PropertyName("Stady state variables")
    @PropertyDescription("Variables which values will be used for steady state detection. If no variables selected then all variables will be used.")
    public VariableSet[] getVariableNames()
    {
        return variableNames;
    }
    public void setVariableNames(VariableSet ... variableNames)
    { 
        this.variableNames = variableNames;
        for (VariableSet var: variableNames)
        {
            var.setEngine(getEngineWrapper().getEngine());
            var.setDiagram(this.getDiagram());
        }
    }
    
    @PropertyName("Start time")
    @PropertyDescription("Start time for steady state detection.")
    public double getStartSearchTime()
    {
        return startSearchTime;
    }
    public void setStartSearchTime(double startSearchTime)
    {
        Object oldValue = this.startSearchTime;
        this.startSearchTime = startSearchTime;
        firePropertyChange("startSearchTime", oldValue, this.startSearchTime);
    }

    @PropertyName("State")
    @PropertyDescription("Diagram state.")
    public String getStateName()
    {
        return stateName;
    }
    public void setStateName(String stateName)
    {
        Object oldValue = this.stateName;
        this.stateName = stateName;
        firePropertyChange("stateName", oldValue, this.stateName);
    }

    @PropertyName("Simulation parameters")
    @PropertyDescription("Simulation parameters.")
    public SimulationEngineWrapper getEngineWrapper()
    {
        return engineWrapper;
    }
    public void setEngineWrapper(SimulationEngineWrapper engineWrapper)
    {
        Object oldValue = this.engineWrapper;
        this.engineWrapper = engineWrapper;
        this.engineWrapper.setParent(this, "engineWrapper");
        firePropertyChange("engineWrapper", oldValue, this.engineWrapper);
    }

    @PropertyName("Absolute tolerance")
    @PropertyDescription("Absolute tolerance for steady state.")
    public double getAbsoluteTolerance()
    {
        return absoluteTolerance;
    }
    public void setAbsoluteTolerance(double tolerance)
    {
        Object oldValue = this.absoluteTolerance;
        this.absoluteTolerance = tolerance;
        firePropertyChange("absoluteTolerance", oldValue, this.absoluteTolerance);
    }

    @PropertyName("Relative tolerance")
    @PropertyDescription("Relative tolerance for steady state.")
    public double getRelativeTolerance()
    {
        return relativeTolerance;
    }
    public void setRelativeTolerance(double tolerance)
    {
        Object oldValue = this.relativeTolerance;
        this.relativeTolerance = tolerance;
        firePropertyChange("relativeTolerance", oldValue, this.relativeTolerance);
    }

    @PropertyName("Time points number")
    @PropertyDescription("Number of consequent time points for steady state detection.")
    public int getValidationSize()
    {
        return validationSize;
    }
    public void setValidationSize(int validationSize)
    {
        Object oldValue = this.validationSize;
        this.validationSize = validationSize;
        firePropertyChange("validationSize", oldValue, this.validationSize);
    }

    @PropertyName("Input diagram")
    @PropertyDescription("Input diagram.")
    public DataElementPath getInput()
    {
        return input;
    }
    public void setInput(DataElementPath input)
    {
        try
        {
            setDiagram(input.getDataElement(Diagram.class));
        }
        catch( Exception ex )
        {
            return;
        }
        
        Object oldValue = this.input;
        this.input = input;
        firePropertyChange("input", oldValue, input);
    }

    public void setDiagram(Diagram diagram)
    {
        engineWrapper.setDiagram(diagram);
        initDiagram( diagram );
    }

    private void initDiagram(Diagram diagram)
    {
        this.diagram = diagram;
        engineWrapper.getEngine().setCompletionTime(5E6);
        engineWrapper.getEngine().setTimeIncrement(100);
        for (VariableSet var: variableNames)
            var.setDiagram(diagram);
    }
    
    public Diagram getDiagram()
    {
        return diagram;
    }
    
    @PropertyName ( "Result diagram" )
    @PropertyDescription ( "Result diagram." )
    public DataElementPath getOutput()
    {
        return output;
    }
    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }

    @PropertyName ( "Simulation result" )
    public DataElementPath getSimulationResult()
    {
        return simulationResult;
    }

    public void setSimulationResult(DataElementPath simulationResult)
    {
        DataElementPath oldValue = this.simulationResult;
        this.simulationResult = simulationResult;
        firePropertyChange("simulationResult", oldValue, outputType);
    }

    @PropertyName ( "Result table" )
    public DataElementPath getTableResult()
    {
        return tableResult;
    }

    public void setTableResult(DataElementPath tableResult)
    {
        this.tableResult = tableResult;
    }

    @PropertyName("Output type")
    @PropertyDescription("Output type.")
    public String getOutputType()
    {
        return outputType;
    }
    public void setOutputType(String outputType)
    {
        String oldValue = this.outputType;
        this.outputType = outputType;
        firePropertyChange("outputType", oldValue, outputType);
        firePropertyChange("*", oldValue, outputType);
    }
    
    /**
     * Initializing parameters specific to steady state detection
     */
    public void setSteadyStateParmeters(SteadyStateTaskParameters parameters)
    {
        setAbsoluteTolerance(parameters.getAbsoluteTolerance());
        setRelativeTolerance(parameters.getRelativeTolerance());
        setValidationSize(parameters.getValidationSize());
        setStartSearchTime(parameters.getStartSearchTime());

        String[] variables = parameters.getVariableNames();
        if( variables.length == 0 )
            variables = new String[] {VariableSet.RATE_VARIABLES};
        setVariableNames( new VariableSet( parameters.getSimulationEngine().getDiagram(), variables ) );

        SimulationEngine engine = parameters.getEngineWrapper().getEngine();
        
        getEngineWrapper().getEngine().setInitialTime(engine.getInitialTime());
        getEngineWrapper().getEngine().setCompletionTime(engine.getCompletionTime());
        getEngineWrapper().getEngine().setTimeIncrement(engine.getTimeIncrement());
    }
    
    public void setEngine(SimulationEngine engine)
    {
        engineWrapper.setEngine(engine);
    }

    public boolean isOutputSimulationResultHidden()
    {
        return !OUTPUT_SIMULATION_RESULT_TYPE.equals( outputType );
    }
    
    public boolean isOutputTableHidden()
    {
        return !OUTPUT_TABLE_TYPE.equals( outputType );
    }
    
    public boolean isOutputDiagramHidden()
    {
        return !OUTPUT_DIAGRAM_TYPE.equals( outputType );
    }

    @PropertyName("Input state")
    public String getInputState()
    {
        return inputState;
    }
    public void setInputState(String inputState)
    {
        this.inputState = inputState;
    }
    
    public StreamEx<String> getAvailableStates()
    {
        return diagram == null? StreamEx.of(Diagram.NON_STATE): StreamEx.of(diagram.getStateNames());
    }

    //Not presented in user interface, can be used to override variable names
    public int[] getVariableIndices()
    {
        return variableIndices;
    }
    public void setVariableIndices(int[] variableIndices)
    {
        this.variableIndices = variableIndices;
    }
}
