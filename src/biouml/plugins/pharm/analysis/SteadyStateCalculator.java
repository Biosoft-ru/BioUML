package biouml.plugins.pharm.analysis;

import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang.ArrayUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.modelreduction.SteadyStateAnalysis;
import biouml.plugins.simulation.OdeSimulatorOptions;
import one.util.streamex.StreamEx;

@Deprecated
public class SteadyStateCalculator implements PatientCalculator
{
    Diagram diagram;
    String[] estimatedVariables;
    String[] observedVariables;

    public SteadyStateCalculator(Diagram diagram, String[] inputNames, String[] outputNames)
    {
        this.diagram = diagram;
        estimatedVariables = inputNames;
        observedVariables = outputNames;
    }

    @Override
    public Patient calculate(double[] input) throws Exception
    {
        EModel emodel = diagram.getRole(EModel.class);
        for( int i = 0; i < estimatedVariables.length; i++ )
            emodel.getVariable(estimatedVariables[i]).setInitialValue(input[i]);

        SteadyStateAnalysis analysis = new SteadyStateAnalysis(null, "");
        analysis.getParameters().getEngineWrapper().getEngine().setLogLevel( Level.SEVERE );
        analysis.getParameters().setValidationSize(10);
        analysis.getParameters().setStartSearchTime(0);
        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(10);
        OdeSimulatorOptions options = (OdeSimulatorOptions)analysis.getParameters().getEngineWrapper().getEngine().getSimulatorOptions();
        options.setAtol(1E-6);
        options.setRtol(1E-6);
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime(10000);
        analysis.getParameters().setAbsoluteTolerance(1E-3);
        analysis.getParameters().setRelativeTolerance(1E-3);
        //        analysis.getParameters().setVariableNames(observedVariables);
        Map<String, Double> steadyState = analysis.findSteadyState(diagram);
        double[] observed = ( steadyState == null ) ? new double[observedVariables.length]
                : ArrayUtils.toPrimitive(StreamEx.of(observedVariables).map(s -> steadyState.get(s)).toArray(Double[]::new));
        return new Patient(input, observed);
    }
}