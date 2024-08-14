package biouml.plugins.simulation;

import biouml.standard.simulation.SimulationResult;

public class CycledResultWriter extends CycledResultListenerSupport
{
    protected SimulationResult simulationResult = null;

    public CycledResultWriter(SimulationResult simulationResult)
    {
        this.simulationResult = simulationResult;
    }

    @Override
    public void addAsFirst(double t, double[] y)
    {
        simulationResult.add(t, y);
    }

    @Override
    public void update(double t, double[] y)
    {
        double[] curValues = simulationResult.getValue(getCurrentIndex());
        for( int i = 0; i < y.length; i++ )
        {
            curValues[i] += y[i];
        }
    }

    public SimulationResult getResults()
    {
        return simulationResult;
    }

    @Override
    public void finish()
    {
        double[] times = simulationResult.getTimes();

        for( int i = 0; i < times.length; i++ )
        {
            double[] values = simulationResult.getValue(i);
            for( int j = 0; j < values.length; j++ )
                values[j] /= getCyclesNumber();
        }
    }
}
