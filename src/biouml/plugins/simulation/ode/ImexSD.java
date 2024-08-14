package biouml.plugins.simulation.ode;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.OdeSimulatorOptionsBeanInfo;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

/**
 *
 * @author sanya
 * @since Jan 23, 2005
 */
public class ImexSD extends SimulatorSupport
{
    // instance variables
    private Btableau butcher;
    private Imex1S engine;

    private int stepNum;
    private int stepLimit;

    private double t0; // the initial and final times
    private double tFinal;
    private double tfBackup;

    private double tNew;
    private double atol; // absolute tolerance
    private double rtol; // relative tolerance
    private double h; // stepsize
    private boolean interpolate;

    private double tOld; // time in each step
    private double[] u0;
    private double[] xOld; // function value for eta1
    private double[] xNew; // function value for eta2

    private double hNew;

    private boolean troubleFlag;

    private static final double alpha = 0.9; // safety factor
    private double maxStepFactor = 5.0; // step size growth limit
    private double minStepFactor = 0.2; // step size growth limit
    private double p; // order of IMEX routine

    private int nreject; // number of rejected steps counter
    private int naccept; // number of accepted steps counter


    private boolean eventOccured;
    private EventDetector eventDetector;

    private int nextSpanPointIndex;

    private static final double MIN_FRAGMENTATION = 1.0e15;

    public ImexSD() throws Exception
    {
        butcher = new Btableau("imex443"); // get the Butcher tableau
        engine = new Imex1S(butcher); // the Butcher tableau
    }

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Imex";
        info.eventsSupport = true;
        return info;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new ImexOptions(0.01, true, OdeSimulatorOptions.STATISTICS_OFF);
    }

    ImexOptions options = (ImexOptions)getDefaultOptions();
    @Override
    public Options getOptions()
    {
        return options;
    }

    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof ImexOptions ) )
            throw new IllegalArgumentException("Only ImexOptions are compatible with Imex simulator");
        this.options = (ImexOptions)options;
    }

    @Override
    public int[] getEvents()
    {
        return ( eventDetector != null ) ? eventDetector.getEventInfo() : null;
    }

    @Override
    public boolean doStep() throws Exception
    {
        init(odeModel, u0, span, resultListeners, jobControl);

        routine();

        return ! ( troubleFlag || tNew >= tFinal );
    }

    @Override
    public void init(Model model, double[] u0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( !tspan.isProper() )
            throw new Exception("Improper span: times are out of order");

        span = tspan;

        odeModel = (OdeModel)model; // get the ODE function

        if( !odeModel.isInit() )
            odeModel.init();

        this.jobControl = jobControl;

        t0 = tspan.getTimeStart();
        tFinal = tspan.getTimeFinal();
        tOld = t0;
        tNew = t0;

        atol = options.getAtol(); // get absolute tolerance
        rtol = options.getRtol(); // get relative tolerance
        h = options.getInitialStep();
        minStepFactor = options.minStepFactor;
        maxStepFactor = options.maxStepFactor;
        stepLimit = options.stepLimit;

        hNew = h;

        // parameter independent initializations
        p = 3; // p = 3 is a good choice for most cases (IMEX443 and ones like it)

        nreject = 0; // no step rejections yet
        naccept = 0; // nor step acceptions

        this.u0 = StdMet.copyArray(u0);
        xOld = StdMet.copyArray(u0);
        xNew = StdMet.copyArray(u0);

        //      initializations for event location
        locateEvents = options.getEventLocation();
        eventDetector = new EventDetector(odeModel, this);
        eventOccured = false;
        resultListeners = listeners;
        interpolate = span.getLength() > 1;
        nextSpanPointIndex = 1;

        stepNum = 0;

        profile.setUnstable(false);
        profile.setStiff(false);
        troubleFlag = false;
    }


    public void routine() throws Exception
    {
        outStatistics("Starting simulation", "Atol: " + atol, "Rtol:" + rtol, "Initial step:" + h);

        if( fireInitialValues || eventAtSpanPoint )
        {
            fireSolutionUpdate(t0, u0);
            fireInitialValues = false;
            eventAtSpanPoint = false;
        }

        double minStepSize = ( tFinal - t0 ) / MIN_FRAGMENTATION;
        troubleFlag = false;

        // the loop
        while( tNew < tFinal )
        {
            if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                return;

            if( stepNum > stepLimit )
            {
                troubleFlag = true;
                outError("Step limit " + stepLimit + " exceeded.");
                profile.setUnstable(true);
                return;
            }

            stepNum++;

            // event location routine
            if( locateEvents )
            {
                eventDetector.detectEvent(xOld, tOld, h);
                eventOccured = eventDetector.isEventDetected();

                if( eventOccured )
                {
                    xNew = eventDetector.getEventX();
                    tNew = eventDetector.getEventTime();
                    h *= eventDetector.getTheta();

                    tfBackup = tFinal; //backup for the case of rejected step
                    tFinal = tOld + h;
                }
            }

            //do usual step
            if( !eventOccured )
            {
                xNew = engine.doOneStep(odeModel, tOld, xOld, h / 2); // do a step with h/2
                xNew = engine.doOneStep(odeModel, tOld + h / 2, xNew, h / 2);
            }

            tNew = tOld + h;

            boolean success = checkError();
            if( troubleFlag )
                return;

            if( eventOccured )
            {
                if( success )
                {
                    outputSolution();
                    naccept++;
                    outIntermediate("Event detected at t = " + tNew, "Final t = " + tOld, "Final u = " + StdMet.toString(xNew),
                            "Rejected: " + nreject, "Accepeted: " + naccept);
                    break;
                }

                nreject++; // we reject a step
                tFinal = tfBackup;
                h /= 2; // we cut h in half, seeing as neither half will be too small
                outStatistics("Step rejected, new h = " + h);
                continue;

            }

            if( !success ) // we reject the step
            {
                h = hNew;
                nreject++;
                outStatistics("Step rejected", "New h = " + h);
                continue;
            }

            if( h <= minStepSize ) //problem is stiff
            {
                outError("Can't calculate model with necessary precision");
                troubleFlag = true;
                profile.setStiff(true);

                if( jobControl != null )
                    jobControl.terminate();
                return;
            }

            //accept step
            outputSolution();
            naccept++;
            StdMet.copyArray(xOld, xNew); // enough to h, accept step and increment related counters
            tOld = tNew;
            outStatistics("Step accepted", "new h = " + h, "Solution = " + StdMet.toString(xNew));
        }

        profile.setTime(tNew);
        profile.setX(xNew);
    }
    /**
     * Function that controls the output of the results.
    */
    protected void outputSolution() throws Exception
    {
        if( !interpolate )
        {
            fireSolutionUpdate(tNew, xNew);
            return;
        }

        double nextSpanPoint = span.getTime(nextSpanPointIndex);
        while( nextSpanPointIndex < span.getLength() && nextSpanPoint < tNew )
        {
            fireSolutionUpdate(nextSpanPoint, getSolution(nextSpanPoint));
            nextSpanPointIndex++;
            if( nextSpanPointIndex >= span.getLength() )
                break;
            nextSpanPoint = span.getTime(nextSpanPointIndex);
        }

        if( nextSpanPoint == tNew )
        {
            if( eventOccured )
                eventAtSpanPoint = true;
            else
            {
                fireSolutionUpdate(nextSpanPoint, getSolution(nextSpanPoint));
                nextSpanPointIndex++;
            }
        }
    }

    private double[] getSolution(double time) throws Exception
    {
        return engine.doOneStep(odeModel, tOld, xOld, time - tOld);
    }

    @Override
    public void integrationStep(double[] xNew, double[] xOld, double tOld, double h, double theta) throws Exception
    {
        StdMet.copyArray(xNew, engine.doOneStep(odeModel, tOld, xOld, h * theta));
    }

    private boolean checkError() throws Exception
    {
        try
        {
            double[] xe = engine.doOneStep(odeModel, tOld, xOld, h); // do a step with h
            double[] estimation = ErrorEstimator
                    .stepdoublingEstimate(h, xe, xNew, xOld, atol, rtol, p, maxStepFactor, minStepFactor, alpha);

            hNew = estimation[0];
            double hopt = estimation[1];
            double norm = estimation[2];

            if( Double.isNaN(norm) )
            {
                // so, something has gone wrong, solver is unstable
                outError("Problem is unstable");
                outIntermediate("Accepted: " + naccept, "Rejected: " + nreject);

                if( jobControl != null )
                    jobControl.functionFinished();
                return false;
            }

            if( ( tFinal - tOld ) <= hNew )
                hNew = tFinal - tOld; // that puts us right on tf may be smaller than suggested step size, so we take the smaller to land

            return h / hopt <= 3;
        }
        catch( Exception ex )
        {
            outError("Error estimation failed: " + ex.getMessage());
            troubleFlag = true;
            return false;
        }

    }

    public static class ImexOptions extends OdeSimulatorOptions
    {
        private int stepLimit = 100000;
        private double maxStepFactor = 5.0;
        private double minStepFactor = 0.2;
        
        @PropertyName("Steps limit")
        @PropertyDescription( "Limitation for solver steps count.")
        public int getStepLimit()
        {
            return this.stepLimit;
        }
        public void setStepLimit(int stepLimit)
        {
            int oldValue = this.stepLimit;
            this.stepLimit = stepLimit;
            firePropertyChange( "stepLimit", oldValue, stepLimit );
        }

      
        @PropertyName("Max step factor")
        @PropertyDescription("Bound for step size growh: newH/oldH <= Max step factor.")
        public double getMaxStepFactor()
        {
            return maxStepFactor;
        }
        public void setMaxStepFactor(double factor)
        {
            if( factor < 1 )
                return;
            double oldValue = this.maxStepFactor;
            maxStepFactor = factor;
            firePropertyChange("maxStepFactor", oldValue, maxStepFactor);
        }

        @PropertyName("Min step factor")
        @PropertyDescription("Bound for step size growh: newH/oldH >= Min step factor.")
        public double getMinStepFactor()
        {

            return minStepFactor;
        }
        public void setMinStepFactor(double factor)
        {
            if( factor >1 || factor <= 0 )
            return;
                double oldValue = this.minStepFactor;
                minStepFactor = factor;
                firePropertyChange( "minStepFactor", oldValue, minStepFactor );
        }

        public ImexOptions()
        {
            this(0, true, STATISTICS_ON);
        }
        
        public ImexOptions(double initialStep, boolean eventLocation, String statisticsMode)
        {
            setInitialStep(initialStep);
            setEventLocation(eventLocation);
            setStatisticsMode(statisticsMode);
        }
    }

    static public class ImexOptionsBeanInfo extends OdeSimulatorOptionsBeanInfo
    {
        public ImexOptionsBeanInfo()
        {
            super(ImexOptions.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add("stepLimit");
            add("maxStepFactor");
            add("minStepFactor");
        }
    }

    @Override
    public void setInitialValues(double[] x0)
    {
        // TODO Auto-generated method stub
    };
}
