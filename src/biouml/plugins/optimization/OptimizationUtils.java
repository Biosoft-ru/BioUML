package biouml.plugins.optimization;

import java.util.List;

import one.util.streamex.StreamEx;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.optimization.Parameter;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.state.State;

public class OptimizationUtils
{
    public static final DataElementPath DOCUMENTS = DataElementPath.create("data/Examples/Optimization/Data/Documents");
    public static final DataElementPath EXPERIMENTS = DataElementPath.create("data/Examples/Optimization/Data/Experiments");
    public static final DataElementPath SIMULATIONS = DataElementPath.create("data/Examples/Optimization/Data/Simulations");

    public static void checkOptimization(Optimization model, OptimizationConstraintCalculator calculator)
    {
        OptimizationParameters params = model.getParameters();
        Diagram diagram = model.getDiagram();

        List<Parameter> fParams = params.getFittingParameters();
        List<OptimizationExperiment> experiments = params.getOptimizationExperiments();
        OptimizationMethodParameters methodParams = model.getOptimizationMethod().getParameters();

        if( methodParams.getResultPath() == null
                || ! DataCollectionUtils.checkPrimaryElementType( methodParams.getResultPath().optParentCollection(), FolderCollection.class ) )
            throw new IllegalArgumentException(MessageBundle.getMessage("WARN_OPTIMIZATION_EXECUTION_0"));

        if( diagram == null )
            throw new IllegalArgumentException(MessageBundle.getMessage("WARN_OPTIMIZATION_EXECUTION_1"));
        if( diagram.getRole() == null || ! ( diagram.getRole() instanceof EModel ) )
            throw new IllegalArgumentException(MessageBundle.format("WARN_OPTIMIZATION_EXECUTION_2", new Object[] {diagram.getName()}));
        if( fParams == null || fParams.size() == 0 )
            throw new IllegalArgumentException(MessageBundle.getMessage("WARN_OPTIMIZATION_EXECUTION_3"));
        if( experiments == null || experiments.size() == 0 )
            throw new IllegalArgumentException(MessageBundle.getMessage("WARN_OPTIMIZATION_EXECUTION_4"));
        for( Parameter param : fParams )
        {
            if( param.getValue() < param.getLowerBound() || param.getValue() > param.getUpperBound() )
            {
                throw new IllegalArgumentException(
                        MessageBundle.format( "WARN_OPTIMIZATION_EXECUTION_5", new Object[] {param.getName(), fParams.indexOf( param )} ) );
            }
        }

        for( OptimizationExperiment exp : experiments )
        {
            String time = exp.getVariableNameInFile("time");
            if( exp.isTimeCourse() && time.equals("") )
            {
                throw new IllegalArgumentException(MessageBundle.format("WARN_OPTIMIZATION_EXECUTION_6", new Object[] {exp.getName()}));
            }
            if( exp.getTableSupport().getTable() == null )
            {
                throw new IllegalArgumentException(MessageBundle.format("WARN_OPTIMIZATION_EXECUTION_7", new Object[] {exp.getName()}));
            }
            if( exp.isSteadyState() && exp.getTableSupport().getTable().getSize() > 1 )
            {
                throw new IllegalArgumentException(MessageBundle.format("WARN_OPTIMIZATION_EXECUTION_8", new Object[] {exp.getName()}));
            }
        }

        calculator.parseConstraints(params.getOptimizationConstraints(), diagram);
    }

    public static SimulationResult refreshOptimizationDiagram(Object[] results, OptimizationParameters optParams) throws Exception
    {
        SimulationResult retVal = null;
        //clear everything first
        optParams.getStateInfos().clear();
        if( results != null )
        {
            //first add all stateInfos
            for( State state : StreamEx.of(results).select( State.class ) )
            {
                optParams.addStateInfo( state.getCompletePath().toString() );
            }

            //adding of simulation results required exsting of stateInfos
            for( SimulationResult sr : StreamEx.of(results).select( SimulationResult.class ) )
            {
                String statePath = (String)sr.getAttributes().getProperty( "statePath" ).getValue();
                optParams.addSimulationResult( statePath, sr.getCompletePath().toString() );
                retVal = sr; // strange, but ok
            }
        }
        return retVal;
    }
}
