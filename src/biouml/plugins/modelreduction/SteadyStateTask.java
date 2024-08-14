package biouml.plugins.modelreduction;

import java.util.logging.Level;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.ParallelSimulationEngine;
import biouml.plugins.simulation.SimulationTask;
import biouml.plugins.simulation.SimulationTaskParameters;

public class SteadyStateTask extends SimulationTask
{
    private SteadyStateAnalysis analysis;

    public SteadyStateTask(ParallelSimulationEngine parallelEngine, String[] names)
    {
        super(parallelEngine, names);

        analysis = new SteadyStateAnalysis(null, "");

    }

    @Override
    protected Object getResult(SimulationEngine engine, Model baseModel)
    {
        try
        {
            return analysis.findSteadyState(baseModel, engine);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not perform the steady state simulation task: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setParameters(SimulationTaskParameters steadyStateParameters)
    {
        //init simulation parameters
        SimulationEngine engine = steadyStateParameters.getSimulationEngine().clone();
        engine.setSolver(initSolver(steadyStateParameters.getSimulationEngine()));
        //
        SteadyStateAnalysisParameters parameters = analysis.getParameters();
        parameters.setDiagram( engine.getDiagram() );
        parameters.setEngine(engine);

        //init steady state parameters
        if( steadyStateParameters instanceof SteadyStateTaskParameters )
            parameters.setSteadyStateParmeters((SteadyStateTaskParameters)steadyStateParameters);
    }
}
