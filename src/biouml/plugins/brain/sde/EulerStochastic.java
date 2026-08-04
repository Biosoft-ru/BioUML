package biouml.plugins.brain.sde;

import java.util.HashMap;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulationEngineLogger;
import biouml.plugins.simulation.SimulatorInfo;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.ode.EulerSimple;
import biouml.plugins.simulation.ode.StdMet;
import biouml.plugins.stochastic.StochasticModel;
import biouml.standard.simulation.ResultListener;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Simplest Euler-Maruyama scheme for SDE solving.
 */
public class EulerStochastic extends EulerSimple
{
	private static final double TIME_STEP_ERROR = 1E-9;
	protected SimpleEventDetector eventDetector;
	// Reusable scratch arrays to avoid per-step allocation (profile: integrationStep + dy_dt ~17% of CPU)
	private double[] dydtStochasticScratch;
	private double[] dydtDeterministicScratch;  
	
    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Euler stochastic";
        info.eventsSupport = true;
        return info;
    }

    @Override
    public void setOptions(Options options)
    {
        if(!(options instanceof ESOptions))
        {
            throw new IllegalArgumentException("Only ESOptions are comaptible with EulerStochastic Simulator");
        }
        this.options = (ESOptions)options;
    }

    @Override
    public int[] getEvents()
    {
        return (eventDetector != null) ? eventDetector.getEventInfo() : null;
    }
    
    @Override
    protected void fireSolutionUpdate(double t, double[] x) throws Exception
    {
        if( odeModel != null )
        {
            double[] y = odeModel.extendResult(t, x.clone());

            odeModel.updateHistory(t);
            
            updateStochasticValuesArrays();

            if( resultListeners != null )
            {
                for( int i = 0; i < resultListeners.length; i++ )
                    resultListeners[i].add(t, y);
            }
        }
    }
    
    @Override
    public void init(Model model, double[] initialValues, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl)
            throws Exception
    {
        if(!(model instanceof SdeModel))
        {
            throw new IllegalArgumentException("Wrong model class" + model.getClass() + ". Only SdeModel class allowed for solver "
                    + this.getClass()); 
        }
        
        odeModel = (SdeModel)model;
        if(!odeModel.isInit())
        {
            odeModel.init();
        }
        resultListeners = listeners;
        
        if(odeModel.hasFastOde() && this.preprocessFastReactions)
        {
            initialValues = preprocessFastReactions();
        }
        
        span = tspan;
        t = span.getTimeStart();
        tFinal = span.getTimeFinal();
        h = options.getInitialStep();
        n = initialValues.length; 
        x = StdMet.copyArray(initialValues);
        profile.init(x, t);
        nextSpanIndex = 1;
        locateEvent = options.getEventLocation();
        eventDetector = locateEvent ? new SimpleEventDetector(odeModel, this) : null;
        // Pre-allocate scratch arrays for derivatives (profile: eliminates per-step allocation)
        dydtStochasticScratch = new double[n];
        dydtDeterministicScratch = new double[n];
    }
    
    @Override
    public boolean doStep() throws Exception
    {
        if (fireInitialValues || eventAtSpanPoint)
        {
            fireSolutionUpdate(t, x);
            fireInitialValues = false;
            eventAtSpanPoint = false;
        }

        if (terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST)
            return false;
                
        double nextSpanPoint = span.getTime(nextSpanIndex);
        
        SimulationEngineLogger log = new SimulationEngineLogger(biouml.plugins.simulation.java.MessageBundle.class.getName(), getClass());
        
        while (t < nextSpanPoint)
        {
            // store current values
            xOld = StdMet.copyArray(x);
            tOld = t;

            h = Math.min(h, nextSpanPoint - t); // restrict step size, so we won't get beyond span point
            
            ((SdeModel)odeModel).stochasticValuesCurrentTime = t + h;
            integrationStep(x, xOld, t, h); // perform step and save calculated values in the x array.
            t += h;

            boolean eventDetected = locateEvent ? eventDetector.detectEvent(xOld, tOld, x, t) : false;
            
            if (t < nextSpanPoint)
            {
                updateStochasticValuesArrays();
            }
            
            //if (locateEvent && eventDetector.detectEvent(xOld, tOld, x, t)) // check if an event happened in new grid node
            if (eventDetected)
            {
                eventAtSpanPoint = Math.abs(nextSpanPoint - eventDetector.getEventTime()) < TIME_STEP_ERROR; // check if an event happened on a plot point
                
                if(!eventAtSpanPoint)
                {
                    profile.setTime(eventDetector.getEventTime());
                }
                else
                {
                    profile.setTime(nextSpanPoint);
                }             
                profile.setStep(h * eventDetector.getTheta());
                profile.setX(eventDetector.getEventX());
                h = options.getInitialStep();
                return false;
            }

            // check if no incorrect arithmetic operations were made
            if (options.isDetectIncorrectNumbers() && SimulatorSupport.checkNaNs(x)) 
            {
                profile.setStep(h);
                profile.setTime(t);
                profile.setX(x);
                profile.setUnstable(true);
                return false;
            }
        }
        
        // update information after step
        nextSpanIndex++;
        fireSolutionUpdate(t, x);
        profile.setStep(h);
        profile.setTime(t);
        profile.setX(x);
        h = options.getInitialStep();
        return t < tFinal;
    }

    @Override
    public void integrationStep(double[] xNew, double[] xOld, double tOld, double h) throws Exception
    {
        SdeModel sdeModel = (SdeModel)odeModel;

        // Reuse scratch arrays to avoid per-step allocation (profile: ~17% of CPU in integrationStep + dy_dt)
        if (dydtStochasticScratch == null || dydtStochasticScratch.length != n)
        {
            dydtStochasticScratch = new double[n];
        }
        if (dydtDeterministicScratch == null || dydtDeterministicScratch.length != n)
        {
            dydtDeterministicScratch = new double[n];
        }
        sdeModel.dy_dt_deterministic(tOld, xOld, dydtDeterministicScratch);
        sdeModel.dy_dt_stochastic(tOld, xOld, dydtStochasticScratch);

        for( int i = 0; i < n; i++ )
        {
            xNew[i] = xOld[i] + dydtDeterministicScratch[i] * h + dydtStochasticScratch[i] * Math.sqrt(h);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void updateStochasticValuesArrays()
    {
        SdeModel sdeModel = (SdeModel)odeModel;

        // Swap mappings instead of copying to avoid HashMap allocation per step
        // (profile: updateStochasticValuesArrays called every step, HashMap allocation is significant)
        java.util.Map<Integer, Double> temp = sdeModel.stochasticValuesPreviousMapping;
        sdeModel.stochasticValuesPreviousMapping = sdeModel.stochasticValuesCurrentMapping;
        sdeModel.stochasticValuesCurrentMapping = temp;
        sdeModel.stochasticValuesCurrentMapping.clear();
        sdeModel.stochasticValuesPreviousTime = t;
    }
}
