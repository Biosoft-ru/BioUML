

package biouml.plugins.simulation;

import biouml.standard.simulation.ResultListener;

public interface CycledResultListener extends ResultListener
{
   
    public abstract void finish();
    
    public abstract void addAsFirst(double t, double[] y);
    
    public abstract void update(double t , double[] y);
    
    /**
     * should be called before new cycle is operating (except first cycle)
     */
    public void startCycle();
    
}
