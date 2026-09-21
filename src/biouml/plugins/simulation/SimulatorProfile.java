package biouml.plugins.simulation;

/**
 * Data returned by simulator
 * 
 * @author puz
 *
 */
public class SimulatorProfile
{
    private double[] x;
    private double step;
    private double time;
    private boolean isUnstable;
    private boolean isStiff;
    private String errorMessage;

    public void init(double[] x, double t)
    {
        this.x = x;
        this.time = t;
        errorMessage = null;
        isUnstable = false;
        isStiff = false;
        
    }
    
    public double[] getX()
    {
        return x;
    }
    public void setX(double[] x)
    {
        // Reuse the backing buffer when the length is unchanged, avoiding a fresh
        // allocation and GC pressure on repeated state updates.
        //
        // Note: getX() returns this backing buffer, not a snapshot. Callers that
        // need to retain a previous state must copy the returned array.
        if( this.x == null || this.x.length != x.length )
            this.x = new double[x.length];
        System.arraycopy(x, 0, this.x, 0, x.length);
    }
    public double getStep()
    {
        return step;
    }
    public void setStep(double step)
    {
        this.step = step;
    }
    public double getTime()
    {
        return time;
    }
    public void setTime(double time)
    {
        this.time = time;
    }
    public boolean isUnstable()
    {
        return isUnstable;
    }
    public void setUnstable(boolean isUnstable)
    {
        this.isUnstable = isUnstable;
    }
    public boolean isStiff()
    {
        return isStiff;
    }
    public void setStiff(boolean isStiff)
    {
        this.isStiff = isStiff;
    }
    public String getErrorMessage()
    {
        return errorMessage;
    }
    public void setErrorMessage(String message)
    {
        this.errorMessage = message;
    }

}
