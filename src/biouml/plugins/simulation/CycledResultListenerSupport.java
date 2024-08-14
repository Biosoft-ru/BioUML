package biouml.plugins.simulation;

public abstract class CycledResultListenerSupport implements CycledResultListener
{
    private int cyclesNumber = 0;
    private int spanIndex = 0;
    
    public int getCurrentIndex()
    {
        return spanIndex;
    }
    
    public int getCyclesNumber()
    {
        return cyclesNumber;
    }
    
    @Override
    public final void add(double t, double[] y)
    {
        if( cyclesNumber == 0 )
        {
            addAsFirst(t, y);
        }
        else
        {
            update(t, y);
        }
        spanIndex++;
    }

    @Override
    public void start(Object model)
    {
        cyclesNumber = 0;
        spanIndex = 0;
    }

    @Override
    public final void startCycle()
    {
        cyclesNumber++;
        spanIndex = 0;
    }
  
    
    @Override
    public void finish()
    {
        // nothing by default;
    }
}
