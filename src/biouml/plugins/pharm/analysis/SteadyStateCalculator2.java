package biouml.plugins.pharm.analysis;

import java.util.Map;
import java.util.logging.Level;

import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.modelreduction.SteadyStateAnalysis;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaBaseModel;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

public class SteadyStateCalculator2 implements PatientCalculator
{
    private Model model;
    private double[] initialValues;
    private SimulationEngine engine;
    private SteadyStateAnalysis analysis;
    private int[] estimatedIndices;
    private int[] observedIndices;

    
    public SteadyStateCalculator2(SimulationEngine engine, String[] inputNames, String[] outputNames) throws Exception
    {
        this.model = engine.createModel();
        this.model.init();
        initialValues = model.getCurrentValues();
        Map<String, Integer> mapping = engine.getVarPathIndexMapping();
        estimatedIndices = StreamEx.of(inputNames).mapToInt(v -> mapping.get(v)).toArray();
        observedIndices = StreamEx.of(outputNames).mapToInt(v -> mapping.get(v)).toArray();
        this.engine = engine;
        initAnalysis(engine);
    }

    public SteadyStateCalculator2(SimulationEngine engine, int[] inputIndices, int[] outputIndices) throws Exception
    {
        this.engine = engine;
        estimatedIndices = inputIndices;
        observedIndices = outputIndices;
        this.model = engine.createModel();
        this.model.init();
        initialValues = model.getCurrentValues();
        initAnalysis(engine);
    }

    public void setValidationSize(int validationSize)
    {
        analysis.getParameters().setValidationSize(validationSize);
    }
    
    public void setStartSearchTime(double startSearchTime)
    {
        analysis.getParameters().setStartSearchTime(startSearchTime);
    }
    
    public void setAtol(double atol)
    {
        analysis.getParameters().setAbsoluteTolerance(atol);
    }
    
    private void initAnalysis(SimulationEngine engine)
    {
        analysis = new SteadyStateAnalysis(null, "");
        analysis.getParameters().setEngine(engine);
        analysis.getParameters().getEngineWrapper().getEngine().setLogLevel( Level.SEVERE );
        analysis.getParameters().setValidationSize(10);
        analysis.getParameters().setStartSearchTime(01E2);
        analysis.getParameters().setAbsoluteTolerance(10);
        analysis.getParameters().setVariableIndices(observedIndices);
    }
    
    private static boolean constraintsViolated(Model model)
 {
		if (model instanceof JavaBaseModel) 
		{
			return ((JavaBaseModel) model).isConstraintViolated();
		} 
		else if (model instanceof AgentBasedModel) 
		{
			return StreamEx.of(((AgentBasedModel) model).getAgents())
					.select(ModelAgent.class).map(agent -> agent.getModel())
					.select(JavaBaseModel.class)
					.anyMatch(m -> m.isConstraintViolated());
		}
		return false;
	}

    @Override
    public Patient calculate(double[] input) throws Exception
    {
        double[] values = initialValues.clone();//model.getCurrentValues();        
        for( int i = 0; i < estimatedIndices.length; i++ )
            values[estimatedIndices[i]] = input[i];
        model.setCurrentValues(values);

        Map<String, Double> steadyState = analysis.findSteadyState(model, engine);
        
        if (constraintsViolated(model))
        {
        	 System.out.println("Constraints were violated");
        	 return new Patient(input, new double[observedIndices.length], true);
        }
        
        if( steadyState == null )
            return new Patient(input, new double[observedIndices.length], true);

        double[] modelValues = model.getCurrentValues();
        double[] observed = IntStreamEx.of(observedIndices).mapToDouble(i -> modelValues[i]).toArray();
        return new Patient(input, observed, modelValues);
    }
}