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
