package biouml.plugins.simulation.ode;

import java.util.Arrays;
import com.developmentontheedge.beans.BeanInfoEx;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

/**
 * Simplest Euler scheme for ODE solving.
 * Updated with event detection
 * @author puz, axec
 *
 */
public class EulerSimple extends SimulatorSupport
{
    private static final double TIME_STEP_ERROR = 1E-9;
    protected ESOptions options = (ESOptions)getDefaultOptions();
    
    protected int nextSpanIndex = 1;
    protected EventDetector eventDetector;   
    protected double tOld; //prevous model time value
    protected double t; //current model time value
    protected double[] xOld; //vlaues of ODE variables at previous step
    protected double[] x; //current values of all ODE variables
    protected double tFinal; //final time
    protected int n; //size of the ODE system
    protected double h; // time step (variable)
    protected boolean locateEvent;
    
    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Euler";
        info.eventsSupport = true;
        return info;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new ESOptions();
    }   

    @Override
    public ESOptions getOptions()
    {
        return options;
    }

    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof ESOptions ) )
            throw new IllegalArgumentException("Only ESOptions are comaptible with EulerSimple Simulator");
        this.options = (ESOptions)options;
    }

    @Override
    public int[] getEvents()
    {
        return ( eventDetector != null ) ? eventDetector.getEventInfo() : null;
    }

    @Override
    public void init(Model model, double[] initialValues, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl)
            throws Exception
    {
        odeModel = (OdeModel)model;
        if( !odeModel.isInit() )
            odeModel.init();
        resultListeners = listeners;
        
        if( odeModel.hasFastOde() && this.preprocessFastReactions )
            initialValues = preprocessFastReactions();
        
        span = tspan;
        t = span.getTimeStart();
        tFinal = span.getTimeFinal();
        h = options.getInitialStep();
        n = initialValues.length; 
        x = StdMet.copyArray(initialValues);
        profile.init( x, t );
        nextSpanIndex = 1;
        locateEvent = options.getEventLocation();
        eventDetector = locateEvent? new EventDetector(odeModel, this): null;
    }

    @Override
    public boolean doStep() throws Exception
    {
        if( fireInitialValues || eventAtSpanPoint ) //TODO: probably unite these flags into one
        {
            fireSolutionUpdate(t, x); //also these probably should be moved to EventLoopSimulator, so we will have only one fireSolutionUpdate per step
            fireInitialValues = false;
            eventAtSpanPoint = false;
        }

        if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
            return false;
                
        double nextSpanPoint = span.getTime(nextSpanIndex);
        
        while( t < nextSpanPoint )
        {
            //store current values
            xOld = StdMet.copyArray(x);
            tOld = t;

            h = Math.min(h, nextSpanPoint - t); //restrict step size, so we won't get beyond span point
            
            if( locateEvent && eventDetector.detectEvent(xOld, tOld, h) ) // check if event happened between t and t + h
            {
                eventAtSpanPoint = Math.abs(nextSpanPoint - eventDetector.getEventTime()) < TIME_STEP_ERROR;
                if( !eventAtSpanPoint )
                    profile.setTime(eventDetector.getEventTime());
                else
                    profile.setTime(nextSpanPoint);
                profile.setStep(h * eventDetector.getTheta());
                profile.setX(eventDetector.getEventX());
                h = options.getInitialStep();
                return false;
            }

            //perform step
            integrationStep(x, xOld, t, h);
            t += h;
            
            //check if no incorrect arithmetic operations were made
            if (options.isDetectIncorrectNumbers() && SimulatorSupport.checkNaNs(x)) 
            {
                profile.setStep(h);
                profile.setTime(t);
                profile.setX(x);
                profile.setUnstable(true);
                return false;
            }
        }
        
        //update information after step
        nextSpanIndex++;
        fireSolutionUpdate(t, x);
        profile.setStep(h);
        profile.setTime(t);
        profile.setX(x);
        h = options.getInitialStep();
        return t < tFinal;
    }

    @Override
    public void integrationStep(double[] xNew, double[] xOld, double tOld, double h, double theta) throws Exception
    {
        integrationStep(xNew, xOld, tOld, h * theta);
    }

    public void integrationStep(double[] xNew, double[] xOld, double tOld, double h) throws Exception
    {
        double[] dydt = odeModel.dy_dt(tOld, xOld);
        
        for( int i = 0; i < n; i++ )
            xNew[i] = xOld[i] + dydt[i] * h;
    }

    public static class ESOptions extends OdeSimulatorOptions
    {
        public ESOptions()
        {
            setStiffnessDetection(false);
            setEventLocation(true);
            setStatisticsMode(OdeSimulatorOptions.STATISTICS_OFF);
            setInitialStep(0.001);
        }
    }

    public static class ESOptionsBeanInfo extends BeanInfoEx
    {
        public ESOptionsBeanInfo()
        {
            super( ESOptions.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
            add("initialStep");
            add("eventLocation");
            add("statisticsMode");
            add("detectIncorrectNumbers");
        }
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        odeModel.setCurrentValues(x0);
        x = Arrays.copyOf(odeModel.getY(), odeModel.getY().length);
    }
}
