package biouml.plugins.simulation;

import biouml.standard.simulation.SimulationResult;

public interface ModelRunnerListener
{
    public void resultReady(SimulationResult simulationResult, double[] variableValues, String[] variableNames);
}
