package biouml.plugins.simulation;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Common options for ODE simulators
 * @author axec
 */
public class OdeSimulatorOptions extends Options
{
    public static final String STATISTICS_OFF = "Off";
    public static final String STATISTICS_ON = "On";
    public static final String STATISTICS_INTERMEDIATE = "Intermediate";
    public static final String[] STATISTICS_MODS = new String[] {STATISTICS_OFF, STATISTICS_ON, STATISTICS_INTERMEDIATE};
    
    public static final double DEFAULT_RELATIVE_TOLERANCE = 1e-11;
    public static final double DEFAULT_ABSOLUTE_TOLERANCE = 1e-10;
    
    private double initialStep;
    private boolean eventLocation = true;    
    private String statisticsMode = STATISTICS_ON;
    private boolean stiffnessDetection;
    private boolean detectIncorrectNumbers = false;
    protected double absTolerance = DEFAULT_ABSOLUTE_TOLERANCE;
    protected double relTolerance = DEFAULT_RELATIVE_TOLERANCE;

    @PropertyName("Absolute tolerance")
    @PropertyDescription("Absolute tolerance.")
    public double getAtol()
    {
        return absTolerance;
    }

    public void setAtol(double absTolerance)
    {
        double oldValue = this.absTolerance;
        this.absTolerance = absTolerance;
        firePropertyChange( "atol", oldValue, absTolerance );
    }

    @PropertyName("Relative tolerance")
    @PropertyDescription("Relative tolerance.")
    public double getRtol()
    {
        return relTolerance;
    }

    public void setRtol(double relTolerance)
    {
        double oldValue = this.relTolerance;
        this.relTolerance = relTolerance;
        firePropertyChange( "rtol", oldValue, relTolerance );
    }

    @PropertyName("Locate events")
    @PropertyDescription("Locate events.")
    public boolean getEventLocation()
    {
        return eventLocation;
    }
    public void setEventLocation(boolean eventLocation)
    {
        boolean oldValue = this.eventLocation;
        this.eventLocation = eventLocation;
        firePropertyChange("eventLocation", oldValue, eventLocation);
    }
    
    @PropertyName("Internal time step")
    @PropertyDescription("Internal time step.")
    public double getInitialStep()
    {
        return initialStep;
    }
    public void setInitialStep(double initialStep)
    {
        double oldValue = this.initialStep;
        this.initialStep = initialStep;
        firePropertyChange( "initialStep", oldValue, initialStep );
    }
    
    @PropertyName("Statistics mode")
    @PropertyDescription("Statistics mode.")
    public String getStatisticsMode()
    {
        return statisticsMode;
    }
    public void setStatisticsMode(String statisticsMode)
    {
        String oldValue = this.statisticsMode;
        this.statisticsMode = statisticsMode;
        firePropertyChange( "statisticsModel", oldValue, statisticsMode );
    }

    @PropertyName("Stiffness detection")
    @PropertyDescription("Stiffness detection.")
    public boolean getStiffnessDetection()
    {
        return stiffnessDetection;
    }
    public void setStiffnessDetection(boolean stiffnessDetection)
    {
        boolean oldValue = this.stiffnessDetection;
        this.stiffnessDetection = stiffnessDetection;
        firePropertyChange( "stiffnessDetection", oldValue, stiffnessDetection );
    }

    @PropertyName("Detect incorrect numbers (NaNs and Infs)")
    @PropertyDescription("If true then simulation will end when NaN or Infinity encountered")
    public boolean isDetectIncorrectNumbers()
    {
        return detectIncorrectNumbers;
    }
    public void setDetectIncorrectNumbers(boolean detectIncorrectNumbers)
    {
        boolean oldValue = this.detectIncorrectNumbers;
        this.detectIncorrectNumbers = detectIncorrectNumbers;
        firePropertyChange( "detectNans", oldValue, detectIncorrectNumbers );
    }
}
