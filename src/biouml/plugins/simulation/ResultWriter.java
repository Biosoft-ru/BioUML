package biouml.plugins.simulation;

import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.standard.simulation.SimulationResult;

public class ResultWriter implements CycledResultListener
{
    protected SimulationResult simulationResult = null;
    protected Object model;
    private int skipPoints;
    
    public ResultWriter(SimulationResult simulationResult)
    {
        this.simulationResult = simulationResult;
    }
    
    public void setSkipPoints(int skipPoints)
    {
        this.skipPoints = skipPoints;
    }

    @Override
    public void start(Object model)
    {
        this.model = model;

        if( model instanceof JavaBaseModel )
            ( (JavaBaseModel)model ).clear();
        
        if (simulationResult instanceof CycledResultListener)
            ( (CycledResultListener)simulationResult ).start(model);
    }

    @Override
    public void add(double t, double[] y)
    {
        if(skipPoints  > 0)
        {
            skipPoints--;
            return;
        }
        simulationResult.add( t, y );
    }

    public SimulationResult getResults()
    {
        return simulationResult;
    }

    @Override
    public void finish()
    {
        if (simulationResult instanceof CycledResultListener)
            ( (CycledResultListener)simulationResult ).finish();
    }
    
    @Override
    public void addAsFirst(double t, double[] y)
    {
        // TODO Auto-generated method stub        
    }

    @Override
    public void update(double t, double[] y)
    {
        // TODO Auto-generated method stub      
    }

    @Override
    public void startCycle()
    {
        if (simulationResult instanceof CycledResultListener)
            ( (CycledResultListener)simulationResult ).startCycle();   
    }
}