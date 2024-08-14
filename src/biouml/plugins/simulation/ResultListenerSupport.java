package biouml.plugins.simulation;

import biouml.standard.simulation.ResultListener;

public class ResultListenerSupport implements ResultListener
{

    protected Object model;

    @Override
    public void add(double t, double[] y) throws Exception
    {
    }

    @Override
    public void start(Object model)
    {
        this.model = model;
    }

    public ResultListenerSupport(Object model)
    {
        this.model = model;
    }
    
    public ResultListenerSupport()
    {
    }

}
