package biouml.plugins.simulation.ode;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.OdeSimulatorOptionsBeanInfo;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

import java.util.logging.Level;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/** Special explicit Runge Kutta scheme known as the Dormand-Prince routine,
 *  which is 5th order/4th order, a 7-stage method that can solve
 *  for an ODE.  It also features stiffness detection and event location.
 */
public class DormandPrince extends SimulatorSupport
{
    private static final double P = 5.0; // // the higher order method of a Dormand-Prince scheme
    private static final double ALPHA = 0.9; // safety factor
    private static final int MAXFCN = 50000; // default amount of function evaluations after which we check problem on stiffness on every step
    private static final int NSUCCESS = 50; // after 50 succesfull steps check ratio
    private static final int NFAILED = 10; // if 10 failed after 50 successful, check for stiffness
    private static final int K1 = 5; // last 2 rows of matrix of K values of Dormand-Prince scheme
    private static final int K2 = 6;
    private static final double BOUND = 3.25; // stability region boundary for dopr


    // variables dealing with general parameters (section 1 of constructor)
    private double t0; // the starting time
    private double tFinal; // the stopping time
    private double tfBackup;

    private double[] x0; // the initial value
    private Btableau butcher; // the Butcher tableau for the scheme
    private int s; // the number of stages of this Runge-Kutta scheme

    private double[][] a; // the matrix a of the given Butcher tableau
    private double[] b; // the array b of the given Butcher tableau
    private double[] bhat; // the array bhat of the given Butcher tableau
    private double[] c; // the array c of the given Butcher tableau

    private boolean FSALenabled; // whether first same as last functionality of the
    // scheme (if this scheme has the property to begin with) is enabled

    private double[] atol; // absolute tolerances for each solution array entry
    private double[] rtol; // relative tolerances for each solutionn array entry

    private static final double MINVALUE = 1.000000e-016;
    private int n; // dimension of ODE

    // variables that are used in the integration loop
    private double tNew;
    private double tOld; // stores the current t value
    private double[] xOld; // stores the current x value (xold
    private double[] xNew; // stores the next x value (xnew)
    private double[] xe; // the error estimation for embedded method
    private double[][] K; // matrix of K values (s rows of size n)
    private double[] g1;
    private double[] g2;
    private double[] as;

    // error control variables
    private double epsilon; // determines whether we accept or reject next

    // step
    private double h; // the stepsize of the integration
    private double hNew; // the stepsize to take for the next step
    private double minStepFactor;
    private double maxStepFactor;

    private int nreject; // number of rejected steps counter
    private int naccept; // number of accepted steps counter
    private double avgStepSize; // the average stepsize throughout integration

    // iteration affected variables
    private boolean firstStep; // switch verifies if loop of routine is on first step
    private boolean justAccepted; // switch verifies whether previous step was an accepted step or not (for the purpose of FSAL functionality)

    private int spanLength; // length of the times array (length = 0 if times = null)
    private boolean interpolate; // whether to do interpolation or not

    // variables for stiffness detection
    private boolean detectSiffness; // whether to detect stiffness or not
    private int fevalTotal; // number of function evaluations thus far
    private double tFirst; // the first time after a check is done
    private int nsuccess; // number of successful steps thus far (from start or last check)
    private int nfailed; // number of failed steps thus far (from start or last check)
    private int checks1; // number of stiffness checks due to function evaluations > MAXFCN
    private int checks2; // number of stiffness checks due to 10 fail b/f 50 succeed

    // variables for event location
    private boolean eventOccured;
    private boolean precizeEvent;
    private EventDetector eventDetector;

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "DormandPrince";
        info.eventsSupport = true;
        return info;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new DPOptions(0, true, true, OdeSimulatorOptions.STATISTICS_OFF);
    }

    DPOptions options = (DPOptions)getDefaultOptions();

    @Override
    public Options getOptions()
    {
        return options;
    }

    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof DPOptions ) )
            throw new IllegalArgumentException("Only DPOptions options are compatible with DormandPrince simulator");
        this.options = (DPOptions)options;
    }

    @Override
    public boolean doStep()
    {
        try
        {
            init(odeModel, x0, span, resultListeners, jobControl);
            routine();

            return ! ( troubleFlag || tNew >= tFinal );
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage());
            profile.setUnstable(true);
            return false;
        }

        //        this.profile.setStiff(false);
        //        this.profile.setUnstable(false);
        //        troubleFlag = false;
        //
        //        if( eventOnPreviousOccured )
        //            nextTimePointIndex = 0;
        //
        //        this.t0 = ( nextTimePointIndex == 0 ) ? this.times[nextTimePointIndex] : this.times[nextTimePointIndex - 1]; // store initial and final
        //        this.tf = ( nextTimePointIndex == 0 ) ? this.times[nextTimePointIndex + 1] : this.times[nextTimePointIndex]; // points of time span
        //
        //        this.n = x0.length; // dimension of ODE
        //        this.setInitialValues(this.getProfile().getX());
        //
        //        double h0 = options.getInitialStep();
        //        if( h0 <= 0.0 ) // h calculation depends on the value of h0
        //        {
        //            try
        //            {
        //                Initsss init = new Initsss(this.odeModel, this.span, x0, atol, rtol); // call
        //                this.h = init.get_h(); // initial step size selection routine and
        //            }
        //            catch( Exception e )
        //            {
        //            }
        //        }
        //        else
        //        {
        //            this.h = h0; // step size, so we assign h to this value
        //        }
        //        events = null;
        //        try
        //        {
        //            routine();
        //        }
        //        catch( Exception e )
        //        {
        //        }

        //        if( troubleFlag || nextTimePointIndex >= times.length )
        //            return false;
        //        else
        //            return true;
    }

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( options.getAtol() <= 0.0 )
            throw new Exception("Absolute tolerance must be greater than zero");

        if( options.getRtol() <= 0.0 )
            throw new Exception("Relative tolerance must be greater than zero");

        if( !tspan.isProper() )
            throw new Exception("Improper span: times are out of order");

        if (!model.isInit())
            model.init();
        
        this.odeModel = (OdeModel)model; // store the function
        
        if( odeModel.hasFastOde() && this.preprocessFastReactions )
            x0 = preprocessFastReactions();
        
        // initial value and tolerances
        this.x0 = StdMet.copyArray(x0);// store the initial value
        this.n = x0.length; // dimension of ODE

        atol = StdMet.generateArray(options.getAtol(), n);
        rtol = StdMet.generateArray(options.getRtol(), n);

        span = tspan;
        spanLength = span.getLength();
        nextSpanPointIndex = 1;
        this.t0 = span.getTimeStart(); // store initial and final
        this.tNew = t0;
        this.tFinal = span.getTimeFinal(); // points of time span
        this.tfBackup = tFinal;

        this.jobControl = jobControl;
      

        profile.init( x0, t0 );

        // Butcher array calculations initializations
        butcher = new Btableau("dopr54");
        a = StdMet.copyMatrix(butcher.get_a());
        b = StdMet.copyArray(butcher.get_b());
        bhat = StdMet.copyArray(butcher.get_bEmb());
        c = StdMet.copyArray(butcher.get_c());
        s = butcher.getbl(); // how many stages this Runge-Kutta scheme will execute in

        // general calculations
        h = options.getInitialStep();
        if( h <= 0.0 )
            h = new Initsss(odeModel, span, x0, atol, rtol).get_h(); // call initial step size selection routine and get intiial step from it

        interpolate = ( spanLength != 0 );
        detectSiffness = options.getStiffnessDetection();
        fevalTotal = 0;

        precizeEvent = options.getPresizeEvents();
        locateEvents = options.getEventLocation();
        eventOccured = false;
        if( locateEvents )
            eventDetector = new EventDetector(odeModel, this);

        statisticsMode = options.getStatisticsMode();
        minStepFactor = options.getMinStepFactor();
        maxStepFactor = options.getMaxStepFactor();
        resultListeners = listeners;
        troubleFlag = false;
    }

    /**
     method computes the solution to the ODE depending on parameters and calculations
     given to and done by the constructor
     */
    public void routine() throws Exception
    {
        if( fireInitialValues || eventAtSpanPoint )
        {
            fireSolutionUpdate(t0, x0);
            fireInitialValues = false;
            eventAtSpanPoint = false;
        }

        tOld = t0; // initialize told to t0 (the starting time)
        xOld = StdMet.copyArray(x0);//new double[n]; // initialize the arrays xold, and xnew, two
        xNew = new double[n]; // arrays that will represent each of the arrays of the solution as it integrates
        xe = new double[n]; // initialize error estimate arrays

        K = new double[s][n]; // a matrix of K values

        nreject = 0; // no step rejections yet
        naccept = 0; // nor step acceptions

        firstStep = true; // we will be starting first step soon

        // stiffness detection initializations
        tFirst = tOld; // start tFirst off at t0
        as = new double[n]; // temporary variable for an array sum
        g1 = new double[n]; // for calculating rho for stiffness
        g2 = new double[n]; // detection

        while( tNew < tFinal )
        {
            if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                return;

            // event location routine
            if( locateEvents )
            {
                eventDetector.detectEvent(xOld, tOld, h);
                eventOccured = eventDetector.isEventDetected();

                if( eventOccured )
                {
                    //make step directly to event point
                    h *= eventDetector.getTheta();

                    if( precizeEvent )
                        xNew = eventDetector.getEventX();
                    else
                        integrationStep(xNew, xOld, h, b);

                    tfBackup = tFinal; //backup for the case of rejected step
                    tFinal = tOld + h;

                    outStatistics("Event detected at t = " + tNew);
                }
            }

            if( !eventOccured )
            {
                integrationStep(xNew, xOld, h, b);
            }

            firstStep = false; // whether accepted or rejected, first step is over
            justAccepted = false; // default false (will toggle on if step is accepted)

            tNew = tOld + h;
            boolean success = checkError();
            if( troubleFlag )
                return;

            if( eventOccured )
            {
                //out due to event
                if( success )
                {
                    outputSolution();
                    break;
                }

                //event happend but step was rejected
                nreject++; // we reject a step and up the counter to
                nfailed++; // keep track for stiffness
                h /= 2; // we cut h in half, seeing as either half will be too small
                tFinal = tfBackup; //restore tf

                outStatistics("Step rejected.", "New h = " + h); // note that time and solution do not change so we do not output such
                continue;
            }

            fevalTotal += s; // another s function evaluations are done
            checkStiffness1();
            if( troubleFlag )
                return;

            if( success ) //step accepted
            {
                outputSolution();
                tOld = tNew;
                xOld = StdMet.copyArray(xNew);

                if( nsuccess == 0 ) // if check was just done, update the time
                    tFirst = tOld;

                h = hNew;
                maxStepFactor = options.getMaxStepFactor(); // restore after step acceptance
                naccept++;
                nsuccess++; // keep track for stiffness detection
                justAccepted = true; // toggle for FSAL functionality
                outStatistics("Step accepted, new h=" + h, "Solution = " + StdMet.toString(xOld), "t = " + tOld);
            }

            // step rejected
            else
            {

                h = hNew;
                maxStepFactor = 1.0; // set amax to 1 after a step rejection
                nreject++;
                nfailed++; // keep track for stiffness detection
                outStatistics("Step rejected", "New h = " + h); // note that time and solution do not change so we do not output such

                checkStiffness2();

                if( troubleFlag )
                    return;
            }
        }
        profile.setStep(avgStepSize);
        profile.setTime(tNew);
        profile.setX(xNew);
    }

    protected void calculateK(double h, double[] as1, double[] g1, double[] g2) throws Exception
    {
        double[] sigma = new double[n];
        double[] stam = new double[n];

        /*
         this loop calculates each row i in the K matrix using the
         Butcher tableau, its inner loop (the sum), and function
         evaluations
         */
        for( int i = 0; i < s; i++ ) // loop for the K matrix
        {
            /*
             this loop calculates the ith row of the K matrix
             using the ith row of the a array of the given Butcher tableau
             and all of the rows of K before it
             */
            for( int j = 0; j < i; j++ ) // the loop for each row
            {
                StdMet.stam(stam, a[i][j], K[j]); // a[i][j]*K[j]
                StdMet.arraySum(sigma, sigma, stam); // sigma = sigma + a[i][j]*K[j]
            }

            if( ! ( ( i == 0 ) && !firstStep && FSALenabled ) )
            {
                StdMet.copyArray(g1, as1); // this effectively gets the second last g of the loop calculations

                StdMet.stam(stam, h, sigma); // sigma = sigma*h
                StdMet.arraySum(as1, xOld, stam); // as1 = xold + stam1
                K[i] = odeModel.dy_dt(tOld + h * c[i], as1); // K[i] = f(told + h*c[i], as1)
                StdMet.zeroArray(sigma); // set sigma array to array of zeros

                StdMet.copyArray(g2, as1); // this gets the the last g of the loop calculations
            }
            else if( justAccepted ) // do this only if previous step was accepted
            {
                StdMet.copyArray(K[0], K[s - 1]); // else we copy the last row from previous step into first row of present step
            }
        }
    }

    /**
    this loop takes the weighted average of all of the rows in the
    K matrix using the b array of the Butcher tableau
    -> this loop is the weighted average for a 5th order ERK method and
    is used to compute xnew
    */
    protected void calculateNewX(double[] xNew, double[] xOld, double h, double[] b) throws Exception
    {
        double[] sigma = new double[n];
        double[] stam = new double[n];
        for( int i = 0; i < s; i++ ) // loop for xnew
        {
            StdMet.stam(stam, h * b[i], K[i]); // h*b[i]*K[i]
            StdMet.arraySum(sigma, sigma, stam); // sigma = sigma + h*b[i]*K[i]
        }
        StdMet.arraySum(xNew, xOld, sigma); // xnew = xold + sigma
    }

    protected double[] integrationStep(double[] xNew, double[] xOld, double h, double[] b) throws Exception
    {
        calculateK(h, as, g1, g2);
        calculateNewX(xNew, xOld, h, b);
        return xNew;
    }

    @Override
    public void integrationStep(double[] xNew, double[] xOld, double tOld, double h, double theta) throws Exception
    {
        if( precizeEvent )
            integrationStep(xNew, xOld, h * theta, b);
        else
            calculateNewX(xNew, xOld, h, butcher.get_btheta().f(theta));
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

    /**
    This function can be used for continuous output. It provides an
    approximation to the solution at time <b>time</b>.
    */
    protected double[] getSolution(double time) throws Exception
    {
        double[] x = new double[n];
        double theta = ( time - tOld ) / h;
        calculateNewX(x, xOld, h, butcher.get_btheta().f(theta));
        return x;
    }

    /**
     * Perform error estimation
     * @throws Exception
     */
    private boolean checkError() throws Exception
    {
        integrationStep(xe, xOld, h, bhat);
        double[] estimation = ErrorEstimator.embeddedEstimate(h, xOld, xNew, xe, atol, rtol, P, maxStepFactor, minStepFactor, ALPHA);

        epsilon = estimation[0]; // get required information from this estimation
        hNew = estimation[1];

        // stretch the last step if it is within 10% of tf - (told + h)
        if( ( 1.1 * hNew >= tFinal - tNew ) && ( epsilon <= 1.0 ) )
            hNew = tFinal - tNew;

        if( Double.isNaN(epsilon) ) //something has gone wrong, solver is unstable
            setUnstable("Error estimation failed");

        return epsilon <= 1;
    }

    private void checkStiffness1() throws Exception
    {
        if( fevalTotal > MAXFCN && detectSiffness )
        {
            checks1++;

            if( hNew < MINVALUE )
                setStiff("Problem is stiff due to the minimum value of step = " + h);

            double hRho = StiffnessDetector.calc_hRho(h, K[K2], K[K1], g2, g1);

            outStatistics("a check due to MAXFCN was done.", "h*rho = " + hRho);

            if( hRho > BOUND || Double.isNaN(hRho) )
                setStiff("Problem is stiff due to maximum ode right side calls at t = " + tOld);

            fevalTotal = 0;
        }
    }

    private void checkStiffness2() throws Exception
    {
        if( ( nfailed >= NFAILED ) && detectSiffness )
        {
            if( nsuccess <= NSUCCESS )
            {
                avgStepSize = ( tOld - tFirst ) / nsuccess;
                if( avgStepSize == 0 ) // in the event that there are only rejected steps from
                    avgStepSize = h; // the start, make it so a check can be done

                if( ( ( h <= minStepFactor * avgStepSize ) && ( h >= avgStepSize / maxStepFactor ) )
                        && ( fevalTotal > MAXFCN * ( tOld - t0 ) / ( tFinal - t0 ) ) )
                {
                    checks2++;

                    double hRho = StiffnessDetector.calc_hRho(h, K[K2], K[K1], g2, g1);
                    outStatistics("A check due to ratio was done", "h*rho = " + hRho);

                    if( hRho > BOUND || Double.isNaN(hRho) )
                    {
                        setStiff("Problem is stiff due to ratio at t = " + tOld);

                        outStatistics("Solution at this time:", StdMet.toString(xOld), "# of checks due to MAXFCN: " + checks1,
                                "# of checks due to MAXFCN: " + checks1, "accepted: " + naccept, "rejected: " + nreject);
                        return;
                    }
                }
            }
            nsuccess = 0;
            nfailed = 0;
        }
    }

    private void setStiff(String message)
    {
        outError(message);
        troubleFlag = true;
        profile.setStiff(true);

        if( jobControl != null )
            jobControl.terminate();
    }

    private void setUnstable(String message)
    {
        outError(message);
        troubleFlag = true;
        profile.setUnstable(true);

        if( jobControl != null )
            jobControl.terminate();
    }


    @Override
    public int[] getEvents()
    {
        return ( eventDetector != null ) ? eventDetector.getEventInfo() : null;
    }

    private int nextSpanPointIndex;
    private boolean troubleFlag;

    public static class DPOptions extends OdeSimulatorOptions
    {
        private double maxStepFactor = 5;
        private double minStepFactor = 0.2;
        private boolean presizeEvents = false;

        @PropertyName("Presize event location")
        @PropertyDescription("Slower but more safe for models with many events.")
        public boolean getPresizeEvents()
        {
            return presizeEvents;
        }
        public void setPresizeEvents(boolean presizeEvents)
        {
            boolean oldValue = this.presizeEvents;
            this.presizeEvents = presizeEvents;
            firePropertyChange( "presizeEvents", oldValue, presizeEvents );
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
            firePropertyChange("maxStepFactor", oldValue, factor);
        }

        @PropertyName("Min step factor")
        @PropertyDescription("Bound for step size growh: newH/oldH >= Min step factor.")
        public double getMinStepFactor()
        {

            return minStepFactor;
        }

        public void setMinStepFactor(double factor)
        {
            if( factor > 1 || factor <= 0 )
                return;
            double oldValue = this.minStepFactor;
            minStepFactor = factor;
            firePropertyChange("minStepFactor", oldValue, factor);
        }

        public DPOptions()
        {
            this(0, true, true, OdeSimulatorOptions.STATISTICS_OFF);
        }

        public DPOptions(double initialStep, boolean stiffnessDetection, boolean eventLocation, String statisticsMode)
        {
            setInitialStep(initialStep);
            setStiffnessDetection(stiffnessDetection);
            setEventLocation(eventLocation);
            setStatisticsMode(statisticsMode);
        }
    }

    static public class DPOptionsBeanInfo extends OdeSimulatorOptionsBeanInfo
    {
        public DPOptionsBeanInfo()
        {
            super(DPOptions.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add("initialStep");
            add("maxStepFactor");
            add("minStepFactor");
            add("eventLocation");
            add("presizeEvents");
            add("stiffnessDetection");
        }


    }

    @Override
    public void setInitialValues(double[] x0)
    {
        try
        {
            odeModel.setCurrentValues(x0);
            StdMet.copyArray(this.x0, odeModel.getY());
        }
        catch( Exception e )
        {
        }
    }
}
