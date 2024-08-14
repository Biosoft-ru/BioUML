package biouml.plugins.simulation;

import biouml.standard.simulation.ResultListener;

/**
 * Base class for all result listeners, that filters its input.
 * By default, input is completelly redirected to output listeners.
 */
public class ResultListenerFilter implements ResultListener
{
    /**
     * Listener of the filtered result
     */
    ResultListener[] outputListener;

    public ResultListenerFilter(ResultListener[] outputListener)
    {
        this.outputListener = outputListener;
    }
    
    @Override
    public void start(Object model)
    {
        for (ResultListener rl : outputListener)
            rl.start(model);
    }

    @Override
    public void add(double t, double[] x) throws Exception
    {
        for (ResultListener rl : outputListener)
            rl.add(t, x);
    }
}