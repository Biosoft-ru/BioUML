package biouml.plugins.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters.StateInfo;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.analysis.optimization.Parameter;
import biouml.model.Diagram;
import biouml.plugins.simulation.SimulationTaskParameters;

/**
 * Parameters for optimization document
 */
public class OptimizationParameters extends AbstractAnalysisParameters
{
    public static final String OPTIMIZATION_EXPERIMENTS = "optimizationExperiments";
    public static final String OPTIMIZATION_CONSTRAINTS = "optimizationConstraints";
    public static final String FITTING_PARAMETERS = "fittingParameters";

    private Diagram diagram;
    protected OptimizationMethodParameters optimizerParameters;
    private Map<String, SimulationTaskParameters> simulationTaskParameters;
    protected List<StateInfo> stateInfos = new ArrayList<>();
    private List<OptimizationExperiment> optimizationExperiments;
    private List<OptimizationConstraint> optimizationConstraints;
    private List<Parameter> fittingParameters;

    public OptimizationParameters()
    {
        optimizationExperiments = new ArrayList<>();
        optimizationConstraints = new ArrayList<>();
        fittingParameters = new ArrayList<>();
        simulationTaskParameters = new HashMap<>();
    }

    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
        if( simulationTaskParameters != null )
        {
            for( SimulationTaskParameters stp : simulationTaskParameters.values() )
                stp.setDiagram( diagram );
        }
        if( optimizerParameters != null )
            optimizerParameters.setDiagramPath( DataElementPath.create( diagram ) );
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    public OptimizationMethodParameters getOptimizerParameters()
    {
        return optimizerParameters;
    }
    public void setOptimizerParameters(OptimizationMethodParameters optimizerParameters)
    {
        this.optimizerParameters = optimizerParameters;
        optimizerParameters.setDiagramPath(DataElementPath.create(diagram)); //diagram should remain unchanged if we change optimization method
    }

    public List<StateInfo> getStateInfos()
    {
        return this.stateInfos;
    }
    public void setStateInfos(List<StateInfo> stateInfos)
    {
        this.stateInfos = stateInfos;
    }
    public void addStateInfo(String statePath)
    {
        stateInfos.add(new StateInfo(statePath));
    }

    public void addSimulationResult(String statePath, String simulationResultPath)
    {
        StreamEx.of(stateInfos).findFirst(si -> si.getPath().equals(statePath)).ifPresent(si -> si.addResult(simulationResultPath));
    }

    public String[] getResults(String statePath)
    {
        return StreamEx.of(stateInfos).findFirst(si -> si.getPath().equals(statePath)).map(si -> si.getResults()).orElse(null);
    }

    public String[] getStatePaths()
    {
        return StreamEx.of(stateInfos).map(StateInfo::getPath).toArray(String[]::new);
    }

    public List<OptimizationExperiment> getOptimizationExperiments()
    {
        return this.optimizationExperiments;
    }

    public OptimizationExperiment getOptimizationExperiment(String name)
    {
        return StreamEx.of(optimizationExperiments).findFirst(e -> e.getName().equals(name)).orElse(null);
    }

    public List<OptimizationConstraint> getOptimizationConstraints()
    {
        return this.optimizationConstraints;
    }
    public void setOptimizationExperiments(List<OptimizationExperiment> optExp)
    {
        List<OptimizationExperiment> oldValue = new ArrayList<>(optimizationExperiments);
        this.optimizationExperiments = optExp;

        Map<String, SimulationTaskParameters> refreshedParameters = SimulationTaskRegistry.getSimulationTaskParameters(optExp, simulationTaskParameters, diagram);
        this.setSimulationTaskParameters(refreshedParameters);

        for( OptimizationConstraint constraint : optimizationConstraints )
        {
            constraint.setAvailableExperiments( optExp );
        }

        firePropertyChange(OPTIMIZATION_EXPERIMENTS, oldValue, optExp);
    }
    public void setOptimizationConstraints(List<OptimizationConstraint> optConstr)
    {
        List<OptimizationConstraint> oldValue = new ArrayList<>(optimizationConstraints);
        this.optimizationConstraints = optConstr;
        firePropertyChange(OPTIMIZATION_CONSTRAINTS, oldValue, optConstr);
    }

    public List<Parameter> getFittingParameters()
    {
        return this.fittingParameters;
    }
    public void setFittingParameters(List<Parameter> params)
    {
        List<Parameter> oldValue = new ArrayList<>(fittingParameters);
        this.fittingParameters = params;
        firePropertyChange(FITTING_PARAMETERS, oldValue, params);
    }

    public Map<String, SimulationTaskParameters> getSimulationTaskParameters()
    {
        return this.simulationTaskParameters;
    }
    public void setSimulationTaskParameters(Map<String, SimulationTaskParameters> simulationTaskParameters)
    {
        this.simulationTaskParameters = simulationTaskParameters;
    }

    @Override
    public void read(Properties properties, String prefix)
    {
        String className = properties.getProperty(prefix + "optimizer.class");
        AnalysisParameters optimizerParams = AnalysisParametersFactory.getEmptyParametersObject(className);
        if( optimizerParams instanceof OptimizationMethodParameters )
        {
            optimizerParams.read(properties, prefix + "optimizer.");
            optimizerParameters = (OptimizationMethodParameters)optimizerParams;
        }
    }

    @Override
    public void write(Properties properties, String prefix)
    {
        optimizerParameters.write(properties, prefix + "optimizer.");
        properties.put(prefix + "optimizer.class", optimizerParameters.getClass().getName());

        //save experiments info
        int i = 0;
        for( OptimizationExperiment oe : optimizationExperiments )
        {
            properties.put(prefix + "experiment." + Integer.toString(i), oe.toString());
            i++;
        }
    }

    public static String[] getFittingParametersNames(List<Parameter> parameters)
    {
        return StreamEx.of(parameters).map(Parameter::getName).toArray(String[]::new);
    }

    @Override
    public OptimizationParameters clone()
    {
        OptimizationParameters clone = (OptimizationParameters)super.clone();
        clone.setDiagram(getDiagram());
        clone.setFittingParameters(StreamEx.of(getFittingParameters()).map(Parameter::copy).toList());
        clone.setOptimizationExperiments(StreamEx.of(getOptimizationExperiments()).map(OptimizationExperiment::clone).toList());
        clone.setOptimizationConstraints(StreamEx.of(getOptimizationConstraints()).map(OptimizationConstraint::clone).toList());
        for(OptimizationConstraint constraint : clone.getOptimizationConstraints())
        {
            constraint.setAvailableExperiments( clone.getOptimizationExperiments() );
        }
        clone.setOptimizerParameters(getOptimizerParameters());
        clone.setSimulationTaskParameters(getSimulationTaskParameters());
        clone.setStateInfos(getStateInfos());
        return clone;
    }
}
