package biouml.plugins.simulation.ode.jvode;

import java.util.Arrays;

import java.util.logging.Logger;

import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.ode.OdeModel;

public class JVode extends JVodeSupport
{
    protected static final Logger log = Logger.getLogger(SimulatorSupport.class.getName());
    protected boolean isRunning = true;
    private boolean detectIncorrectNumbers = false;
    private final static double TINY = 1.0e-10;

    //Success results
    public final static int SUCCESS = 0;
    protected final static int TSTOP_RETURN = 1;
    public final static int ROOT_RETURN = 2;

    //Error results
    protected final static int TOO_MUCH_WORK = -1;
    protected final static int TOO_MUCH_ACC = -2;
    protected final static int ERR_FAILURE = -3;
    protected final static int CONV_FAILURE = -4;
    protected final static int LSETUP_FAIL = -5;
    protected final static int LSOLVE_FAIL = -6;
    protected final static int RHSFUNC_FAIL = -7;
    protected final static int REPTD_RHSFUNC_ERR = -8;
    protected final static int UNREC_RHSFUNC_ERR = -9;
    protected final static int ILL_INPUT = -11;
    protected final static int TOO_CLOSE = -12;
    public final static int TERMINATED = -13;
    protected final static int INCORRECT_CALCULATIONS = -14;

    /**
     * Either this is the first setup call for this step, or
     * the local error test failed on the previous attempt at this step (but the
     * Newton iteration converged).
     */
    protected final static int NO_FAILURES = 0;

    /**
    * This value is passed to setup if<br>
    * (a) The previous Newton corrector iteration did not converge and the
    * linear solver's setup routine indicated that its Jacobian- related data
    * is not current or (b) During the previous Newton corrector iteration, the
    * linear solver's solve routine failed in a recoverable manner and the
    * linear solver's setup routine indicated that its Jacobian-related data is
    * not current.
    */
    protected final static int FAIL_BAD_J = 1;

    /**
     * 
     * During the current internal step try, the previous Newton
     * iteration failed to converge even though the linear solver was using
     * current Jacobian-related data.
     */
    protected final static int FAIL_OTHER = 2;

    // Control constants for lower-level functions used by step
    private final static int DO_ERROR_TEST = +2;
    private final static int PREDICT_AGAIN = +3;
    private final static int CONV_FAIL = +4;
    private final static int TRY_AGAIN = +5;
    private final static int FIRST_CALL = +6;
    private final static int PREV_CONV_FAIL = +7;
    private final static int PREV_ERR_FAIL = +8;
    private final static int RHSFUNC_RECVR = +9;

    // Algorithmic constants
    private final static double FUZZ_FACTOR = 100.0;
    private final static double HLB_FACTOR = 100.0;
    private final static double HUB_FACTOR = 0.1;
    private final static double H_BIAS = 0.5;
    private final static int MAX_ITERS = 4;

    private final static double THRESH = 1.5;

    private final static double ETAMX2 = 10.0;
    private final static double ETAMX3 = 10.0;
    private final static double ETAMXF = 0.2;
    private final static double ETAMIN = 0.1;
    private final static double ETACF = 0.25;
    private final static double ADDON = 0.000001;
    private final static double BIAS1 = 6.0;
    private final static double BIAS2 = 6.0;
    private final static double BIAS3 = 10.0;
    private final static double ONEPSM = 1.000001;
    private final static int SMALL_NST = 10; // nst > SMALL_NST => use ETAMX3

    private final static int MXNEF1 = 3; // max no. of error test failures before forcing a reduction of order
    private final static int SMALL_NEF = 2; // if an error failure occurs and SMALL_NEF <= nef <= MXNEF1, then reset eta = MIN(eta, ETAMXF)
    private final static int LONG_WAIT = 10; // number of steps to wait before considering an order change when q==1 and MXNEF1 error test failures have occurred
    private final static double CRDOWN = 0.3;// constant used in the estimation of the convergence rate (crate) of theiterates for the nonlinear equation
    private final static double DGMAX = 0.3; // iter == NEWTON, |gamma/gammap-1| > DGMAX => call lsetup
    private final static double RDIV = 2; // declare divergence if ratio del/delp > RDIV
    private final static int MSBP = 20; // max no. of steps between lsetup calls

    protected JVode(Method method, OdeModel f, double[] u0, double t0)
    {
        super(method, f, u0, t0);
        iterationType = IterationType.FUNCTIONAL;
    }

    public static JVode createJVode(JVodeOptions options, double initialTime, double[] initialValue, OdeModel odeModel)
    {
        Method method = options.getMethodType();
        IterationType iterType = options.getIterationType();
        JacobianType jacType = options.getJacobianType();

        if( iterType == IterationType.FUNCTIONAL )
            return new JVode(method, odeModel, initialValue, initialTime);

        if( iterType == IterationType.NEWTON )
        {
            switch( jacType )
            {
                case DENSE:
                    return new JVodeDense(method, odeModel, initialValue, initialTime, null);
                case BAND:
                    return new JVodeBand(method, odeModel, initialValue, initialTime, options.getMu(), options.getMl(), null);
                case DIAG:
                    return new JVodeDiag(method, odeModel, initialValue, initialTime);
                default:
                    throw new IllegalArgumentException("Unknown JVODE jacobian type!");
            }
        }
        else
            throw new IllegalArgumentException("Unknown JVODE integration method!");
    }

    /**
     * Tells JVode that next start will be treated as first and variables such as step and initial values will be reinitialized
     */
    public void reset()
    {
        nSteps = 0;
    }

    public void stop()
    {
        isRunning = false;
    }

    /**
     * This routine is the main driver of the JVode
     * 
     * It integrates over a time interval defined by the user, by calling doStep
     * to do internal time steps.
     * 
     * The first time that JVode is called for a successfully initialized
     * problem, it computes a tentative initial step size h.
     * 
     * JVode supports two modes, normal and oneStep. In
     * the normal mode, the solver steps until it reaches or passes tout and
     * then interpolates to obtain y(tout). In the oneStep mode, it takes
     * one internal step and returns.
     * 
     * JVode integrates the ODE over an interval in t. If itask is NORMAL,
     * then the solver integrates from its current internal t value to a point
     * at or beyond tout, then interpolates to t = tout and returns y(tout) in
     * the user- allocated vector yout. If itask is ONE_STEP, then the solver
     * takes one internal time step and returns in yout the value of y at the
     * new internal time. In this case, tout is used only during the first call
     * to JVode to determine the direction of integration and the rough scale of
     * the t variable. If tstop is enabled (through a call to CVodeSetStopTime),
     * then JVode returns the solution at tstop. Once the integrator returns at
     * a tstop time, any future testing for tstop is disabled.
     * 
     * @param tout
     *            is the next time at which a computed solution is desired.
     * 
     * @param oneStepMode
     *            true means oneSTep type, false - normal type.
     * 
     * @return SUCCESS: JVode succeeded and no roots were found.
     * 
     *         ROOT_RETURN: JVode succeeded, and found one or more roots.
     * 
     *         TSTOP_RETURN: JVode succeeded and returned at tstop.
     * 
     *         ILL_INPUT: One of the inputs to JVode is illegal. This
     *         includes the situation when a component of the error weight
     *         vectors becomes < 0 during internal time-stepping. It also
     *         includes the situation where a root of one of the root functions
     *         was found both at t0 and very near t0.
     * 
     *         TOO_MUCH_WORK: The solver took mxstep internal steps but could
     *         not reach tout. The default value for mxstep is MXSTEP_DEFAULT =
     *         500.
     * 
     *         TOO_MUCH_ACC: The solver could not satisfy the accuracy
     *         demanded by the user for some internal step.
     * 
     *         ERR_FAILURE: Error test failures occurred too many times (=
     *         MXNEF = 7) during one internal time step or occurred with |h| =
     *         hmin.
     * 
     *         CONV_FAILURE: Convergence test failures occurred too many
     *         times (= MXNCF = 10) during one internal time step or occurred
     *         with |h| = hmin.
     * 
     *         LINIT_FAIL: The linear solver's initialization function failed.
     * 
     *         LSETUP_FAIL: The linear solver's setup routine failed in an unrecoverable manner.
     * 
     *         LSOLVE_FAIL: The linear solver's solve routine failed in an unrecoverable manner.
     *         -----------------------------------------------------------------
     */
    private int start(double tout, boolean oneStepMode) throws Exception
    {
        if( tout == tn )
            return SUCCESS; //if need to return in zero point

        if( y == null )
            throw new Exception("initial value is null.");

        if( !oneStepMode )
            toutCopy = tout;

        int state = SUCCESS;

        /* ----------------------------------------
         * 2. Initializations performed only at the first step (nSteps=0):
         * - initial setup
         * - initialize Nordsieck (z) history array
         * - compute initial step size
         * - check for approach to tstop
         * - check for approach to a root
         * ----------------------------------------*/
        if( nSteps == 0 )
        {
            z = new double[L_MAX][y.length];
            VectorUtils.copy(y, z[0]);
            initialSetup();

            /*
             * Call f at (t0,y0), set z[1] = y'(t0), set initial h and scale zn[1] by h.
             * Also check for zeros of root function g at and near t0.
             */
            try
            {
                z[1] = f.dy_dt(tn, z[0]);
                nFCalls++;
            }
            catch( Exception ex )
            {
                throw new Exception("At t = " + tn + ", the right-hand side routine failed in an unrecoverable manner.");
            }

            if( detectIncorrectNumbers && SimulatorSupport.checkNaNs(f.getCurrentState()) )
                return INCORRECT_CALCULATIONS;

            if( h0 == 0.0 )
            {
                double toutHin = ( tStopSet && ( tout - tn ) * ( tout - tStop ) > 0 ) ? tStop : tout;

                int hflag = computeStep(toutHin);
                if( hflag != SUCCESS )
                {
                    handleFailure(hflag);
                    return hflag;
                }
            }
            else if( ( tout - tn ) * h0 < 0.0 )
                throw new Exception("h0 and tout - t0 inconsistent.");

            h = Util.restrict(hMin, 1.0 / hMaxInv, h0);

            // Check for approach to tstop
            if( tStopSet )
            {
                if( ( tStop - tn ) * h < 0.0 )
                    throw new Exception(
                            "The value tstop = " + tStop + " is behind current t = " + tn + " in the direction of integration.");

                if( ( tn + h - tStop ) * h > 0.0 )
                    h = ( tStop - tn ) * ( 1 - 4 * UROUND );
            }

            // Scale zn[1] by h.
            hScale = h;
            h0u = h;
            hPrime = h;
            VectorUtils.scale(h, z[1]);

            // Check for zeros of root function g at and near t0.
            if( eventNumber > 0 )
                checkRoot1();
        }

        /* ------------------------------------------------------
         * 3. At following steps, perform stop tests:
         * - check for root in last step
         * - check if we passed tstop
         * - check if we passed tout (NORMAL mode)
         * - check if current tn was returned (ONE_STEP mode)
         * - check if we are close to tstop (adjust step size if needed)
         * -------------------------------------------------------*/
        else
        {
            h = Util.restrict(hMin, 1.0 / hMaxInv, h);

            /*
             * Estimate an infinitesimal time interval to be used as a roundoff
             * for time quantities (based on current time and step size)
             */
            double troundoff = FUZZ_FACTOR * UROUND * ( Math.abs(tn) + Math.abs(h) );

            /*
             * First, check for a root in the last step taken, other than the
             * last root found, if any. If onStepMode == true and y(tn) was not
             * returned because of an intervening root, return y(tn) now.
             */
            if( eventNumber > 0 )
            {
                int irfndp = lastStepRoot;

                if( checkRoot2() )
                {
                    cv_tretlast = tLo;
                    time = tLo;
                    return ROOT_RETURN;
                }

                // If tn is distinct from tretlast (within roundoff), check remaining interval for roots
                if( Math.abs(tn - cv_tretlast) > troundoff )
                {
                    if( checkRoot3(oneStepMode) )
                    {
                        lastStepRoot = 1;
                        cv_tretlast = tLo;
                        time = tLo;
                        return ROOT_RETURN;
                    }
                    else
                    {
                        lastStepRoot = 0;
                        if( irfndp == 1 && oneStepMode )
                        {
                            cv_tretlast = tn;
                            time = tn;
                            VectorUtils.copy(z[0], y);
                            return SUCCESS;
                        }
                    }
                }
            }

            // In normal mode, test if tout was reached
            if( !oneStepMode && ( tn - tout ) * h >= 0.0 )
            {
                cv_tretlast = time = tout;
                try
                {
                    getDky(tout, 0, y);
                }
                catch( Exception ex )
                {
                    processError("Trouble interpolating at tout = " + tout + " too far back in direction of integration");
                    throw new Exception(ex.getMessage());
                }
                return SUCCESS;
            }

            // In oneStepMode mode, test if tn was returned
            if( oneStepMode && Math.abs(tn - cv_tretlast) > troundoff )
            {
                cv_tretlast = tn;
                time = tn;
                VectorUtils.copy(z[0], y);
                return SUCCESS;
            }

            // Test for tn at tstop or near tstop
            if( tStopSet )
            {
                if( Math.abs(tn - tStop) <= troundoff )
                {
                    try
                    {
                        getDky(tStop, 0, y);
                    }
                    catch( Exception ex )
                    {
                        processError("The value tstop = " + tStop + " is behind current t = " + tn + " in the direction of integration.");
                        throw new Exception(ex.getMessage());
                    }
                    cv_tretlast = time = tStop;
                    tStopSet = false;
                    return TSTOP_RETURN;
                }

                // If next step would overtake tstop, adjust stepsize
                if( ( tn + hPrime - tStop ) * h > 0.0 )
                {
                    hPrime = ( tStop - tn ) * ( 1 - 4 * UROUND );
                    eta = hPrime / h;
                }
            }
        }

        /* --------------------------------------------------
         * 4. Looping point for internal steps
         * 4.1. check for errors (too many steps, too much accuracy requested, step size too small)
         * 4.2. take a new step
         * 4.3. stop on error
         * 4.4. perform stop tests:
         *  - check for root in last step
         *  - check if tout was passed
         *  - check if close to tstop
         *  - check if in ONE_STEP mode (must return)
         *  -------------------------------------------------- */
        int nstloc = 0;
        for( ;; )
        {

            hNext = h;
            qNext = q;

            // Reset and check ewt
            if( nSteps > 0 )
            {
                if( ewtFunc.getValue(z[0], errorWeight) != 0 )
                {
                    processError("At " + tn + ", error estimation failed (probably component of error has become <= 0).");
                    state = ILL_INPUT;
                    cv_tretlast = tn;
                    time = tn;
                    VectorUtils.copy(z[0], y);
                    break;
                }
            }

            // Check for too many steps
            if( ( maxSteps > 0 ) && ( nstloc >= maxSteps ) )
            {
                processError("At " + tn + ", mxstep steps taken before reaching tout.");
                state = TOO_MUCH_WORK;
                cv_tretlast = tn;
                time = tn;
                VectorUtils.copy(z[0], y);
                break;
            }

            // Check for too much accuracy requested
            toleranceScale = UROUND * VectorUtils.wrmsNorm(z[0], errorWeight);
            if( toleranceScale > 1 )
            {
                processError("At " + tn + ", too much accuracy requested.");
                state = TOO_MUCH_ACC;
                cv_tretlast = tn;
                time = tn;
                VectorUtils.copy(z[0], y);
                toleranceScale *= 2;
                break;
            }
            else
            {
                toleranceScale = 1;
            }

            // Check for h below roundoff level in tn
            if( tn + h == tn )
                nHnil++;

            // Take a step
            int kflag = doStep();

            // Process failed step cases, and exit loop
            if( kflag != SUCCESS )
            {
                handleFailure(kflag);
                cv_tretlast = tn;
                time = tn;
                VectorUtils.copy(z[0], y);
                state = kflag;
                break;
            }

            nstloc++;

            // Check for root in last step taken.
            if( eventNumber > 0 )
            {
                if( checkRoot3(oneStepMode) )
                {
                    lastStepRoot = 1;
                    state = ROOT_RETURN;
                    cv_tretlast = time = tLo;
                    break;
                }

                /*
                 * If we are at the end of the first step and we still have some
                 * event functions that are inactive, issue a warning as this
                 * may indicate a user error in the implementation of the root
                 * function.
                 */
                if( nSteps == 1 )
                {
                    boolean inactiveRoots = false;
                    for( int i = 0; i < eventNumber; i++ )
                    {
                        if( !eventActive[i] )
                        {
                            inactiveRoots = true;
                            break;
                        }
                    }
                    if( maxWrnMessages > 0 && inactiveRoots )
                    {
                        processError(
                                "At the end of the first step, there are still some root functions identically 0. This warning will not be issued again.");
                    }
                }
            }

            // In NORMAL mode, check if tout reached
            if( !oneStepMode && ( tn - tout ) * h >= 0.0 )
            {
                state = SUCCESS;
                cv_tretlast = tout;
                time = tout;
                getDky(tout, 0, y);
                qNext = qPrime;
                hNext = hPrime;
                break;
            }

            // Check if tn is at tstop or near tstop
            if( tStopSet )
            {
                double troundoff = FUZZ_FACTOR * UROUND * ( Math.abs(tn) + Math.abs(h) );
                if( Math.abs(tn - tStop) <= troundoff )
                {
                    getDky(tStop, 0, y);
                    cv_tretlast = tStop;
                    time = tStop;
                    tStopSet = false;
                    state = TSTOP_RETURN;
                    break;
                }

                if( ( tn + hPrime - tStop ) * h > 0.0 )
                {
                    hPrime = ( tStop - tn ) * ( 1 - 4 * UROUND );
                    eta = hPrime / h;
                }
            }

            // In ONE_STEP mode, copy y and exit loop
            if( oneStepMode )
            {
                state = SUCCESS;
                cv_tretlast = time = tn;
                VectorUtils.copy(z[0], y);
                qNext = qPrime;
                hNext = hPrime;
                break;
            }
        }
        return state;
    }

    /**
     * Make one step
     */
    public int start() throws Exception
    {
        return start(1, true);
    }

    public int start(double tout) throws Exception
    {
        return start(tout, false);
    }

    /**
     * This routine computes the k-th derivative of the interpolating polynomial
     * at the time t and stores the result in the vector dky. The formula is: q
     * dky = SUM c(j,k) * (t - tn)^(j-k) * h^(-j) * zn[j] , j=k where c(j,k) =
     * j*(j-1)*...*(j-k+1), q is the current order, and zn[j] is the j-th column
     * of the Nordsieck history array.
     * 
     */
    private void getDky(double t, int k, double[] dky) throws Exception
    {
        if( k < 0 || k > q )
            throw new Exception("Illegal value for k: " + k);

        // Allow for some slack
        double tfuzz = FUZZ_FACTOR * UROUND * ( Math.abs(tn) + Math.abs(hu) );
        if( hu < 0.0 )
            tfuzz = -tfuzz;
        double tp = tn - hu - tfuzz;
        double tn1 = tn + tfuzz;
        if( ( t - tp ) * ( t - tn1 ) > 0.0 )
            throw new Exception("Illegal value for t. t = " + t + " is not between tcur - hu = " + ( tn - hu ) + " and tcur = " + tn + ".");

        // Sum the differentiated interpolating polynomial
        double s = ( t - tn ) / h;
        for( int j = q; j >= k; j-- )
        {
            double c = 1;
            for( int i = j; i >= j - k + 1; i-- )
                c *= i;
            if( j == q )
                VectorUtils.scale(c, z[q], dky);
            else
                VectorUtils.linearSum(c, z[j], s, dky, dky);
        }
        if( k == 0 )
            return;
        double r = Math.pow(h, -k);
        VectorUtils.scale(r, dky);
        return;
    }

    /**
     * This routine performs input consistency checks at the first step. If
     * needed, it also checks the linear solver module and calls the linear
     * solver initialization routine.
     */
    private void initialSetup() throws Exception
    {
        // Did the user specify tolerances?
        if( ewtFunc == null )
            throw new Exception("No integration tolerances have been specified.");

        // Load initial error weights
        if( n > 0 && ewtFunc.getValue(z[0], errorWeight) != 0 )
            throw new Exception("Initial ewt has component(s) equal to zero (illegal).");

        if( init() != 0 )
            throw new Exception("The linear solver's init routine failed.");
    }

    /**
     * This routine computes a tentative initial step size h0. If tout is too
     * close to tn (= t0), then returns TOO_CLOSE and h remains
     * uninitialized. Note that here tout is either the value passed to JVode at
     * the first call or the value of tstop (if tstop is enabled and it is
     * closer to t0=tn than tout). If the RHS function fails unrecoverably,
     * hin returns RHSFUNC_FAIL. If the RHS function fails recoverably too
     * many times and recovery is not possible, returns
     * REPTD_RHSFUNC_ERR. Otherwise set h to the chosen value h0 and
     * returns SUCCESS.
     * 
     * The algorithm used seeks to find h0 as a solution of (WRMS norm of (h0^2
     * ydd / 2)) = 1, where ydd = estimated second derivative of y.
     * 
     * We start with an initial estimate equal to the geometric mean of the
     * lower and upper bounds on the step size.
     * 
     * Loop up to MAX_ITERS times to find h0. Stop if new and previous values
     * differ by a factor < 2. Stop if hnew/hg > 2 after one iteration, as this
     * probably means that the ydd value is bad because of cancellation error.
     * 
     * For each new proposed hg, we allow MAX_ITERS attempts to resolve a
     * possible recoverable failure from f() by reducing the proposed stepsize
     * by a factor of 0.2. If a legal stepsize still cannot be found, fall back
     * on a previous value if possible, or else return REPTD_RHSFUNC_ERR.
     * 
     * Finally, we apply a bias (0.5) and verify that h0 is within bounds.
     */
    private int computeStep(double tout)
    {
        double hnew = 0;
        double yddnrm = 0;
        double tdiff = tout - tn;

        // If tout is too close to tn, give up
        int sign = ( tdiff > 0.0 ) ? 1 : -1;
        double tdist = Math.abs(tdiff);
        double tround = UROUND * Math.max(Math.abs(tn), Math.abs(tout));

        if( tdist < 2 * tround )
            return TOO_CLOSE;

        /*
         * Set lower and upper bounds on h0, and take geometric mean as first
         * trial value. Exit with this value if the bounds cross each other.
         */
        double hlb = HLB_FACTOR * tround;
        double hub = upperBoundH0(tdist);
        double hg = Math.sqrt(hlb * hub);

        if( hub < hlb )
        {
            h = sign * hg;
            return SUCCESS;
        }

        // Outer loop
        boolean hnewOK = false;
        double hs = hg; // safeguard against 'uninitialized variable' warning

        for( int count1 = 1; count1 <= MAX_ITERS; count1++ )
        {
            // Attempts to estimate ydd
            boolean hgOK = false;
            for( int count2 = 1; count2 <= MAX_ITERS; count2++ )
            {
                try
                {
                    yddnrm = yddNorm(sign * hg);
                    // If successful, we can use ydd
                    hgOK = true;
                    break;
                }
                catch( IllegalArgumentException ex )
                {
                    hg *= 0.2; // f() failed recoverably; cut step size and test it again
                }
                catch( Exception ex )
                {
                    return RHSFUNC_FAIL;
                }
            }

            // If f() failed recoverably MAX_ITERS times
            if( !hgOK )
            {
                //Exit if this is the first or second pass. No recovery possible
                if( count1 <= 2 )
                    return REPTD_RHSFUNC_ERR;

                // We have a fall-back option. The value hs is a previous hnew which passed through f(). Use it and break.
                hnew = hs;
                break;
            }

            // The proposed step size is feasible. Save it.
            hs = hg;

            // If the stopping criteria was met, or if this is the last pass, stop
            if( ( hnewOK ) || ( count1 == MAX_ITERS ) )
            {
                hnew = hg;
                break;
            }

            // Propose new step size
            hnew = ( yddnrm * hub * hub > 2 ) ? Math.sqrt(2.0 / yddnrm) : Math.sqrt(hg * hub);
            double hrat = hnew / hg;

            // Accept hnew if it does not differ from hg by more than a factor of 2
            if( hrat > 0.5 && hrat < 2 )
                hnewOK = true;

            // After one pass, if ydd seems to be bad, use fall-back value.
            if( count1 > 1 && hrat > 2 )
            {
                hnew = hg;
                hnewOK = true;
            }
            // Send this value back through f()
            hg = hnew;
        }

        // Apply bounds, bias factor, and attach sign
        h0 = sign * Util.restrict(hlb, hub, H_BIAS * hnew);

        return SUCCESS;
    }

    /** This routine sets an upper bound on abs(h0) based on tdist = tn - t0 and the values of y[i]/y'[i]. */
    private double upperBoundH0(double tdist)
    {
        /* Bound based on |y0|/|y0'| -- allow at most an increase of HUB_FACTOR in y0 (based on a forward Euler step).
         * The weight factor is used as a safeguard against zero components in y0. */
        VectorUtils.abs(z[0], acor);
        ewtFunc.getValue(z[0], temp);
        VectorUtils.inv(temp, temp);
        VectorUtils.linearSum(HUB_FACTOR, acor, temp);
        VectorUtils.abs(z[1], acor);
        VectorUtils.divide(acor, temp, temp);

        // bound based on tdist -- allow at most a step of magnitude HUB_FACTOR*tdist
        return Math.min(HUB_FACTOR * tdist, 1.0 / VectorUtils.maxNorm(temp));
    }

    /**
     * This routine computes an estimate of the second derivative of y using a
     * difference quotient, and returns its WRMS norm.
     */
    private double yddNorm(double hg) throws Exception
    {
        VectorUtils.linearSum(hg, z[1], z[0], y);
        nFCalls++;
        temp = f.dy_dt(tn + hg, y);
        VectorUtils.scaleDiff(1.0 / hg, temp, z[1], temp);
        return VectorUtils.wrmsNorm(temp, errorWeight);
    }

    /**
     * This routine performs one internal cvode step, from tn to tn + h. It
     * calls other routines to do all the work.
     * 
     * The main operations done here are as follows: - preliminary adjustments
     * if a new step size was chosen; - prediction of the Nordsieck history
     * array zn at tn + h; - setting of multistep method coefficients and test
     * quantities; - solution of the nonlinear system; - testing the local
     * error; - updating zn and other state data if successful; - resetting
     * stepsize and order for the next step. - if SLDET is on, check for
     * stability, reduce order if necessary. On a failure in the nonlinear
     * system solution or error test, the step may be reattempted, depending on
     * the nature of the failure.
     */
    private int doStep() throws Exception
    {
        int kflag, eflag;

        double savedT = tn;
        nFlag = FIRST_CALL;
        nef = 0;
        ncf = 0;
        if( ( nSteps > 0 ) && ( hPrime != h ) )
            adjustParams();

        // Looping point for attempts to take a step
        for( ;; )
        {
            predictZ();
            set();

            solveNonLinearSystem();
            kflag = handleNFlag(savedT);

            /*
             * Go back in loop if we need to predict again
             * (nflag=PREV_CONV_FAIL)
             */
            if( kflag == PREDICT_AGAIN )
                continue;

            // Return if nonlinear solve failed and recovery not possible.
            if( kflag != DO_ERROR_TEST )
                return kflag;

            // Perform error test (nflag=SUCCESS)
            eflag = doErrorTest(savedT);

            // Go back zin loop if we need to predict again (nflag=PREV_ERR_FAIL)
            if( eflag == TRY_AGAIN )
                continue;

            // Return if error test failed and recovery not possible.
            if( eflag != SUCCESS )
                return eflag;

            // Error test passed (eflag=SUCCESS), break from loop
            break;
        }

        /*
         * Nonlinear system solve and error test were both successful. Update
         * data, and consider change of step and/or order.
         */
        completeStep();
        prepareNextStep();

        //If Stablilty Limit Detection is turned on, call stability limit detection routine for possible order reduction.
        if( stabLimitDetect )
            stabilityBDF();

        etaMax = ( nSteps <= SMALL_NST ) ? ETAMX2 : ETAMX3;

        //Finally, we rescale the acor array to be the estimated local error vector.
        VectorUtils.scale(tq[2], acor);
        return SUCCESS;
    }


    /**
     * This routine is called when a change in step size was decided upon, and
     * it handles the required adjustments to the history array zn. If there is
     * to be a change in order, we call adjustOrder and reset q, L = q+1, and
     * qwait. Then in any case, we call rescaleZ, which resets h and rescales
     * the Nordsieck array.
     */
    private void adjustParams()
    {
        if( qPrime != q )
        {
            adjustOrder(qPrime - q);
            q = qPrime;
            qPlusOne = q + 1;
            qWait = qPlusOne;
        }
        rescaleZ();
    }

    /**
     * This routine is a high level routine which handles an order change by an
     * amount deltaq (= +1 or -1). If a decrease in order is requested and q==2,
     * then the routine returns immediately. Otherwise adjustAdams or
     * adjustBDF is called to handle the order change (depending on the value
     * of lmm).
     */
    private void adjustOrder(int deltaq)
    {
        if( q == 2 && deltaq != 1 )
            return;

        switch( method )
        {
            case ADAMS:
                adjustAdams(deltaq);
                break;
            case BDF:
                adjustBDF(deltaq);
                break;
        }
    }

    /**
     * This routine adjusts the history array on a change of order q by deltaq,
     * in the case that lmm == ADAMS.
     */
    private void adjustAdams(int deltaq)
    {
        // On an order increase, set new column of zn to zero and return
        if( deltaq == 1 )
        {
            Arrays.fill(z[qPlusOne], 0);
            return;
        }

        /*
         * On an order decrease, each zn[j] is adjusted by a multiple of zn[q].
         * The coeffs. in the adjustment are the coeffs. of the polynomial: x q
         * * INT { u * ( u + xi_1 ) * ... * ( u + xi_{q-2} ) } du 0 where xi_j =
         * [t_n - t_(n-j)]/h => xi_0 = 0
         */

        for( int i = 0; i <= qMax; i++ )
            l[i] = 0.0;
        l[1] = 1;
        double hsum = 0.0;
        for( int j = 1; j <= q - 2; j++ )
        {
            hsum += tau[j];
            double xi = hsum / hScale;
            for( int i = j + 1; i >= 1; i-- )
                l[i] = l[i] * xi + l[i - 1];
        }

        for( int j = 1; j <= q - 2; j++ )
            l[j + 1] = q * ( l[j] / ( j + 1 ) );

        for( int j = 2; j < q; j++ )
            VectorUtils.linearSum( -l[j], z[q], z[j]);
    }

    /**
     * This is a high level routine which handles adjustments to the history
     * array on a change of order by deltaq in the case that lmm == BDF.
     * adjustBDF calls increaseBDF if deltaq = +1 and decreaseBDF if
     * deltaq = -1 to do the actual work.
     */
    private void adjustBDF(int deltaq)
    {
        switch( deltaq )
        {
            case 1:
                increaseBDF();
                return;
            case -1:
                decreaseBDF();
                return;
        }
    }

    /**
     * This routine adjusts the history array on an increase in the order q in
     * the case that lmm == BDF. A new column zn[q+1] is set equal to a
     * multiple of the saved vector (= acor) in zn[indx_acor]. Then each zn[j]
     * is adjusted by a multiple of zn[q+1]. The coefficients in the adjustment
     * are the coefficients of the polynomial x*x*(x+xi_1)*...*(x+xi_j), where
     * xi_j = [t_n - t_(n-j)]/h.
     */
    private void increaseBDF()
    {
        double alpha1 = 1, prod = 1, xiold = 1;

        for( int i = 0; i <= qMax; i++ )
            l[i] = 0.0;
        l[2] = 1;
        double alpha0 = -1;
        double hsum = hScale;
        if( q > 1 )
        {
            for( int j = 1; j < q; j++ )
            {
                hsum += tau[j + 1];
                double xi = hsum / hScale;
                prod *= xi;
                alpha0 -= 1.0 / ( j + 1 );
                alpha1 += 1.0 / xi;
                for( int i = j + 2; i >= 2; i-- )
                    l[i] = l[i] * xiold + l[i - 1];
                xiold = xi;
            }
        }
        double a1 = - ( alpha0 + alpha1 ) / prod;
        VectorUtils.scale(a1, z[acorIndex], z[qPlusOne]);

        for( int j = 2; j <= q; j++ )
            VectorUtils.linearSum(l[j], z[qPlusOne], z[j]);
    }

    /**
     * This routine adjusts the history array on a decrease in the order q in
     * the case that methodType == BDF. Each zn[j] is adjusted by a multiple of
     * zn[q]. The coefficients in the adjustment are the coefficients of the
     * polynomial x*x*(x+xi_1)*...*(x+xi_j), where xi_j = [t_n - t_(n-j)]/h.
     */
    private void decreaseBDF()
    {
        for( int i = 0; i <= qMax; i++ )
            l[i] = 0.0;
        l[2] = 1;
        double hsum = 0.0;
        for( int j = 1; j <= q - 2; j++ )
        {
            hsum += tau[j];
            for( int i = j + 2; i >= 2; i-- )
                l[i] = l[i] * ( hsum / hScale ) + l[i - 1];
        }

        for( int j = 2; j < q; j++ )
            VectorUtils.linearSum( -l[j], z[q], z[j]);
    }

    /**This routine rescales the Nordsieck array by multiplying the jth column zn[j] by eta^j, j = 1, ..., q. 
     * Then the value of h is rescaled by eta, and hscale is reset to h. */
    private void rescaleZ()
    {
        double factor = eta;
        for( int j = 1; j <= q; j++ )
        {
            VectorUtils.scale(factor, z[j]);
            factor *= eta;
        }
        h = hScale * eta;
        hNext = h;
        hScale = h;
        nscon = 0;
    }

    /**
     * This routine advances tn by the tentative step size h, and computes the
     * predicted array z_n(0), which is overwritten on zn. The prediction of zn
     * is done by repeated additions. If tstop is enabled, it is possible for tn
     * + h to be past tstop by roundoff, and in that case, we reset tn (after
     * incrementing by h) to tstop.
     */
    private void predictZ()
    {
        tn += h;
        if( tStopSet && ( tn - tStop ) * h > 0.0 )
            tn = tStop;

        for( int k = 1; k <= q; k++ )
            for( int j = q; j >= k; j-- )
                VectorUtils.add(z[j], z[j - 1]);
    }

    /**
     * This routine is a high level routine which calls setAdams or setBDF
     * to set the polynomial l, the test quantity array tq, and the related
     * variables rl1, gamma, and gamrat.
     * 
     * The array tq is loaded with constants used in the control of estimated
     * local errors and in the nonlinear convergence test. Specifically, while
     * running at order q, the components of tq are as follows: tq[1] = a
     * coefficient used to get the est. local error at order q-1 tq[2] = a
     * coefficient used to get the est. local error at order q tq[3] = a
     * coefficient used to get the est. local error at order q+1 tq[4] =
     * constant used in nonlinear iteration convergence test tq[5] = coefficient
     * used to get the order q+2 derivative vector used in the est. local error
     * at order q+1
     */
    private void set()
    {
        switch( method )
        {
            case ADAMS:
                setAdams();
                break;
            case BDF:
                setBDF();
                break;
        }
        rl1 = 1.0 / l[1];
        gamma = h * rl1;
        if( nSteps == 0 )
            gammaPrev = gamma;
        gammaRatio = ( nSteps > 0 ) ? gamma / gammaPrev : 1;
    }

    /**
     * This routine handles the computation of l and tq for the case of Admas method.<br>
     * 
     * The components of the array l are the coefficients of a polynomial<br>
     * Lambda(x) = l_0 + l_1 x + ... + l_q x^q, given by q-1 <br>
     * (d/dx) Lambda(x) = c * PRODUCT (1 + x / xi_i) ,<br>
     *  where i=1 Lambda(-1) = 0, Lambda(0) = 1, and c is a normalization factor. <br>
     *  Here xi_i = [t_n - t_(n-i)] / h.<br>
     * The array tq is set to test quantities used in the convergence test, the<br>
     * error test, and the selection of h at a new order.
     */
    private void setAdams()
    {
        if( q == 1 )
        {
            l[0] = l[1] = tq[1] = tq[5] = 1;
            tq[2] = 0.5;
            tq[3] = 1.0 / 12;
            tq[4] = convCoef / tq[2]; /* = 0.1 / tq[2] */
            return;
        }

        double[] m = new double[JVodeSupport.L_MAX];
        double[] mm = new double[3];
        double hsum = adamsStart(m);
        mm[0] = altSum(q - 1, m, 1);
        mm[1] = altSum(q - 1, m, 2);
        adamsFinish(m, mm, hsum);
    }

    /**
     * This routine generates in m[] the coefficients of the product polynomial needed for the Adams l and tq coefficients for q > 1.
     */
    private double adamsStart(double m[])
    {
        double hsum = h;
        m[0] = 1;
        for( int i = 1; i <= q; i++ )
            m[i] = 0.0;
        for( int j = 1; j < q; j++ )
        {
            if( j == q - 1 && qWait == 1 )
                tq[1] = q * altSum(q - 2, m, 2) / m[q - 2];

            double xi_inv = h / hsum;
            for( int i = j; i >= 1; i-- )
                m[i] += m[i - 1] * xi_inv;
            hsum += tau[j];
            // The m[i] are coefficients of product(1 to j) (1 + x/xi_i)
        }
        return hsum;
    }

    /** This routine completes the calculation of the Adams l and tq. */
    private void adamsFinish(double m[], double M[], double hsum)
    {
        double M0_inv = 1.0 / M[0];

        l[0] = 1;
        for( int i = 1; i <= q; i++ )
            l[i] = M0_inv * ( m[i - 1] / i );

        double xi = hsum / h;
        double xi_inv = 1.0 / xi;

        tq[2] = M[1] * M0_inv / xi;
        tq[5] = xi / l[q];

        if( qWait == 1 )
        {
            for( int i = q; i >= 1; i-- )
                m[i] += m[i - 1] * xi_inv;
            M[2] = altSum(q, m, 2);
            tq[3] = M[2] * M0_inv / qPlusOne;
        }
        tq[4] = convCoef / tq[2];
    }

    /**
     * Returns the value of the alternating sum sum (i= 0 ... iend) [
     * (-1)^i * (a[i] / (i + k)) ]. If iend < 0 then altSum returns 0. This
     * operation is needed to compute the integral, from -1 to 0, of a
     * polynomial x^(k-1) M(x) given the coefficients of M(x).
     */
    private double altSum(int iend, double a[], int k)
    {
        if( iend < 0 )
            return 0;
        double sum = 0;
        int sign = 1;
        for( int i = 0; i <= iend; i++ )
        {
            sum += sign * ( a[i] / ( i + k ) );
            sign = -sign;
        }
        return sum;
    }

    /**
     * This routine computes the coefficients l and tq in the case lmm ==
     * BDF. setBDF calls setTqBDF to set the test quantity array tq.
     * 
     * The components of the array l are the coefficients of a polynomial
     * Lambda(x) = l_0 + l_1 x + ... + l_q x^q, given by q-1 Lambda(x) = (1 + x
     * / xi*_q) * PRODUCT (1 + x / xi_i) , where i=1 xi_i = [t_n - t_(n-i)] / h.
     * 
     * The array tq is set to test quantities used in the convergence test, the
     * error test, and the selection of h at a new order.
     */
    private void setBDF()
    {
        double alpha0, alpha0_hat, xi_inv, xistar_inv, hsum;
        l[0] = l[1] = xi_inv = xistar_inv = 1;
        for(int i = 2; i <= q; i++ )
            l[i] = 0.0;
        alpha0 = alpha0_hat = -1;
        hsum = h;
        if( q > 1 )
        {
            for(int j = 2; j < q; j++ )
            {
                hsum += tau[j - 1];
                xi_inv = h / hsum;
                alpha0 -= 1.0 / j;
                for(int i = j; i >= 1; i-- )
                    l[i] += l[i - 1] * xi_inv;
                // The l[i] are coefficients of product(1 to j) (1 + x/xi_i)
            }

            // j = q
            alpha0 -= 1.0 / q;
            xistar_inv = -l[1] - alpha0;
            hsum += tau[q - 1];
            xi_inv = h / hsum;
            alpha0_hat = -l[1] - xi_inv;
            for(int i = q; i >= 1; i-- )
                l[i] += l[i - 1] * xistar_inv;
        }
        setTqBDF(hsum, alpha0, alpha0_hat, xi_inv, xistar_inv);
    }

    /**
     * This routine sets the test quantity array tq in the case lmm == BDF.
     */
    private void setTqBDF(double hsum, double alpha0, double alpha0_hat, double xi_inv, double xistar_inv)
    {
        double A1 = 1 - alpha0_hat + alpha0;
        double A2 = 1 + q * A1;
        tq[2] = Math.abs(A1 / ( alpha0 * A2 ));
        tq[5] = Math.abs(A2 * xistar_inv / ( l[q] * xi_inv ));
        if( qWait == 1 )
        {
            double C = xistar_inv / l[q];
            double A3 = alpha0 + 1.0 / q;
            double A4 = alpha0_hat + xi_inv;
            double Cpinv = ( 1 - A4 + A3 ) / A3;
            tq[1] = Math.abs(C * Cpinv);
            hsum += tau[q];
            xi_inv = h / hsum;
            double A5 = alpha0 - ( 1.0 / ( q + 1 ) );
            double A6 = alpha0_hat - xi_inv;
            double Cppinv = ( 1 - A6 + A5 ) / A2;
            tq[3] = Math.abs(Cppinv / ( xi_inv * ( q + 2 ) * A5 ));
        }
        tq[4] = convCoef / tq[2];
    }

    /**
     * This routine attempts to solve the nonlinear system associated with a
     * single implicit step of the linear multistep method. Depending on iter,
     * it calls solveFunctional or solveNewton to do the work.
     */
    private void solveNonLinearSystem() throws Exception
    {
        nFlag = ( iterationType == IterationType.FUNCTIONAL ) ? solveFunctional() : solveNewton();
    }

    /**
     * This routine attempts to solve the nonlinear system using functional
     * iteration (no matrices involved).
     * 
     * @return SUCCESS ---> continue with error test
     *         RHSFUNC_FAIL ---> halt the integration
     *         CONV_FAIL -+ RHSFUNC_RECVR -+-> predict again or stop if too many
     *         INCORRECT_CALCULATIONS ---> division by zero or other inccorrect calculations were made
     * 
     */
    private int solveFunctional() throws Exception
    {
        double dcon;

        // Initialize counter and evaluate f at predicted y
        convergenceRate = 1;
        int m = 0;

        try
        {
            nFCalls++;
            temp = f.dy_dt(tn, z[0]);
        }
        catch( IllegalArgumentException ex )
        {
            return RHSFUNC_RECVR;
        }
        catch( Exception ex )
        {
            return RHSFUNC_FAIL;

        }

        if( detectIncorrectNumbers && SimulatorSupport.checkNaNs(f.getCurrentState()) )
            return INCORRECT_CALCULATIONS;

        Arrays.fill(acor, 0);

        /* Initialize delp to avoid compiler warning message */
        double del;
        double delp = 0.0;

        for( ;; ) // Loop until convergence; accumulate corrections in acor
        {
            nNewtonIter++;

            /* Correct y directly from the last f value */
            VectorUtils.linearDiff(h, temp, z[1], temp);
            VectorUtils.scale(rl1, temp);
            VectorUtils.linearSum(rl1, temp, z[0], y);
            /* Get WRMS norm of current correction to use in convergence test */
            VectorUtils.linearDiff(temp, acor, acor);
            del = VectorUtils.wrmsNorm(acor, errorWeight);
            VectorUtils.copy(temp, acor);

            /*
             * Test for convergence. If m > 0, an estimate of the convergence
             * rate constant is stored in crate, and used in the test.
             */
            if( m > 0 )
                convergenceRate = Math.max(CRDOWN * convergenceRate, del / delp);
            dcon = del * Math.min(1, convergenceRate) / tq[4];
            if( dcon <= 1 )
            {
                acorNorm = ( m == 0 ) ? del : VectorUtils.wrmsNorm(acor, errorWeight);
                return SUCCESS; /* Convergence achieved */
            }

            /* Stop at maxcor iterations or if iter. seems to be diverging */
            m++;
            if( ( m == maxCor ) || ( ( m >= 2 ) && ( del > RDIV * delp ) ) )
                return CONV_FAIL;

            /* Save norm of correction, evaluate f, and loop again */
            delp = del;

            try
            {
                nFCalls++;
                temp = f.dy_dt(tn, y);
            }
            catch( IllegalArgumentException ex )
            {
                return RHSFUNC_RECVR;
            }
            catch( Exception ex )
            {
                return RHSFUNC_FAIL;
            }
            if( detectIncorrectNumbers && SimulatorSupport.checkNaNs(f.getCurrentState()) )
                return INCORRECT_CALCULATIONS;
        }
    }

    /**
     * This routine handles the Newton iteration. It calls lsetup if indicated,
     * calls doNewtonIteration to perform the iteration, and retries a failed
     * attempt at Newton iteration if that is indicated.
     * 
     * @return SUCCESS ---> continue with error test<br>
     *         RHSFUNC_FAIL -+ LSETUP_FAIL |-> halt the integration<br>
     *         LSOLVE_FAIL -+ CONV_FAIL -+ RHSFUNC_RECVR -+-> predict again or stop if too many<br>
     */
    private int solveNewton() throws Exception
    {
        boolean callSetup;

        // Set flag convfail, input to lsetup for its evaluation decision
        int convfail = ( nFlag == FIRST_CALL || nFlag == PREV_ERR_FAIL ) ? NO_FAILURES : FAIL_OTHER;

        // Decide whether or not to call setup routine (if one exists)
        if( setupNonNull )
        {
            callSetup = ( nFlag == PREV_CONV_FAIL ) || ( nFlag == PREV_ERR_FAIL ) || ( nSteps == 0 ) || ( nSteps >= nLastSetupSteps + MSBP )
                    || ( Math.abs(gammaRatio - 1) > DGMAX );
        }
        else
        {
            convergenceRate = 1;
            callSetup = false;
        }

        /*
         * Looping point for the solution of the nonlinear system. Evaluate f at the predicted y, call lsetup if indicated, and call doNewtonIteration
         * for the Newton iteration itself.
         */
        for( ;; )
        {
            try
            {
                nFCalls++;
                ftemp = f.dy_dt(tn, z[0]);
            }
            catch( IllegalArgumentException ex )
            {
                return RHSFUNC_RECVR;
            }
            catch( Exception ex )
            {
                return RHSFUNC_FAIL;

            }

            if( detectIncorrectNumbers && SimulatorSupport.checkNaNs(f.getCurrentState()) )
                return INCORRECT_CALCULATIONS;

            if( callSetup )
            {
                int ier = setup(convfail);
                nSetupCalls++;
                callSetup = false;
                gammaRatio = convergenceRate = 1;
                gammaPrev = gamma;
                nLastSetupSteps = nSteps;
                // Return if lsetup failed */
                if( ier < 0 )
                    return LSETUP_FAIL;
                if( ier > 0 )
                    return CONV_FAIL;
            }

            // Set acor to zero and load prediction into y vector */
            Arrays.fill(acor, 0);
            VectorUtils.copy(z[0], y);

            // Do the Newton iteration */
            int ier = doNewtonIteration();

            /*
             * If there is a convergence failure and the Jacobian-related data
             * appears not to be current, loop again with a call to lsetup in
             * which convfail=FAIL_BAD_J. Otherwise return.
             */
            if( ier != TRY_AGAIN )
                return ier;

            callSetup = true;
            convfail = FAIL_BAD_J;
        }
    }

    /**
     * This routine performs the Newton iteration. If the iteration succeeds, it
     * returns the value SUCCESS. If not, it may signal the solveNewton
     * routine to call lsetup again and reattempt the iteration, by returning
     * the value TRY_AGAIN. (In this case, solveNewton must set convfail to
     * FAIL_BAD_J before calling setup again). Otherwise, this routine
     * returns one of the appropriate values LSOLVE_FAIL, RHSFUNC_FAIL,
     * CONV_FAIL, or RHSFUNC_RECVR back to solveNewton.
     */
    int doNewtonIteration() throws Exception
    {
        int m;
        double del, delp;
        double[] b;

        mNewt = m = 0;

        // Initialize delp to avoid compiler warning message */
        del = delp = 0;

        // Looping point for Newton iteration */
        for( ;; )
        {
            if( !isRunning )
                return TERMINATED;

            // Evaluate the residual of the nonlinear system
            VectorUtils.linearSum(rl1, z[1], acor, temp);
            VectorUtils.linearDiff(gamma, ftemp, temp, temp);

            // Call the lsolve function
            b = VectorUtils.copy( temp );
            int retval = solve(b);
            nNewtonIter++;

            if( retval < 0 )
                return LSOLVE_FAIL;

            /*
             * If lsolve had a recoverable failure and Jacobian data is not
             * current, signal to try the solution again
             */
            if( retval > 0 )
                return ( !currentJacobian && setupNonNull ) ? TRY_AGAIN : CONV_FAIL;

            // Get WRMS norm of correction; add correction to acor and y
            del = VectorUtils.wrmsNorm(b, errorWeight);
            VectorUtils.linearSum(acor, b, acor);
            VectorUtils.linearSum(z[0], acor, y);

            /*
             * Test for convergence. If m > 0, an estimate of the convergence
             * rate constant is stored in crate, and used in the test.
             */
            if( m > 0 )
                convergenceRate = Math.max(CRDOWN * convergenceRate, del / delp);

            if( del * Math.min(1, convergenceRate) / tq[4] <= 1 )
            {
                acorNorm = ( m == 0 ) ? del : VectorUtils.wrmsNorm(acor, errorWeight);
                currentJacobian = false;
                return SUCCESS; // Nonlinear system was solved successfully
            }

            mNewt = ++m;

            /*
             * Stop at maxcor iterations or if iter. seems to be diverging. If
             * still not converged and Jacobian data is not current, signal to
             * try the solution again
             */
            if( m == maxCor || ( m >= 2 && del > RDIV * delp ) )
                return ( !currentJacobian && setupNonNull ) ? TRY_AGAIN : CONV_FAIL;

            // Save norm of correction, evaluate f, and loop again
            delp = del;

            try
            {
                nFCalls++;
                ftemp = f.dy_dt(tn, y);
            }
            catch( IllegalArgumentException ex )
            {
                return ( !currentJacobian && setupNonNull ) ? TRY_AGAIN : RHSFUNC_RECVR;
            }
            catch( Exception ex )
            {
                return RHSFUNC_FAIL;
            }

            if( detectIncorrectNumbers && SimulatorSupport.checkNaNs(f.getCurrentState()) )
                return INCORRECT_CALCULATIONS;
        } // end loop
    }

    /**
     * This routine takes action on the return value nflag = *nflagPtr returned
     * by CVNls, as follows:
     * 
     * If CVNls succeeded in solving the nonlinear system, then handleNFlag
     * returns the constant DO_ERROR_TEST, which tells JVode to perform the
     * error test.
     * 
     * If the nonlinear system was not solved successfully, then ncfn and ncf =
     * *ncfPtr are incremented and Nordsieck array zn is restored.
     * 
     * If the solution of the nonlinear system failed due to an unrecoverable
     * failure by setup, we return the value LSETUP_FAIL.
     * 
     * If it failed due to an unrecoverable failure in solve, then we return the
     * value LSOLVE_FAIL.
     * 
     * If it failed due to an unrecoverable failure in rhs, then we return the
     * value RHSFUNC_FAIL.
     * 
     * Otherwise, a recoverable failure occurred when solving the nonlinear
     * system (CVNls returned nflag == CONV_FAIL or RHSFUNC_RECVR). In this
     * case, if ncf is now equal to maxncf or |h| = hmin, we return the value
     * CONV_FAILURE (if nflag=CONV_FAIL) or REPTD_RHSFUNC_ERR (if
     * nflag=RHSFUNC_RECVR). If not, we set *nflagPtr = PREV_CONV_FAIL and
     * return the value PREDICT_AGAIN, telling JVode to reattempt the step.
     * 
     */
    int handleNFlag(double saved_t)
    {
        if( nFlag == SUCCESS )
            return DO_ERROR_TEST;

        // The nonlinear soln. failed; increment ncfn and restore zn
        nCorrFails++;
        restore(saved_t);

        if( nFlag == INCORRECT_CALCULATIONS || nFlag == LSETUP_FAIL || nFlag == LSOLVE_FAIL || nFlag == RHSFUNC_FAIL )
            return nFlag;

        // At this point, nflag = CONV_FAIL or RHSFUNC_RECVR; increment ncf
        ncf++;
        etaMax = 1;

        /*
         * If we had maxncf failures or |h| = hmin, return CONV_FAILURE or
         * REPTD_RHSFUNC_ERR.
         */
        if( Math.abs(h) <= hMin * ONEPSM || ncf == maxConvFails )
        {
            if( nFlag == CONV_FAIL )
                return CONV_FAILURE;
            if( nFlag == RHSFUNC_RECVR )
                return REPTD_RHSFUNC_ERR;
        }
        // Reduce step size; return to reattempt the step */
        eta = Math.max(ETACF, hMin / Math.abs(h));
        nFlag = PREV_CONV_FAIL;
        rescaleZ();
        return PREDICT_AGAIN;
    }

    /**
     * This routine restores the value of tn to saved_t and undoes the
     * prediction. After execution of restore, the Nordsieck array zn has the
     * same values as before the call predict.
     */
    private void restore(double savedTime)
    {
        tn = savedTime;
        for( int k = 1; k <= q; k++ )
        {
            for( int j = q; j >= k; j-- )
            {
                VectorUtils.substract(z[j], z[j - 1]);
            }
        }
    }

    /**
     * This routine performs the local error test.
     * If the test passes, doErrorTest returns SUCCESS.
     * 
     * If the test fails, we undo the step just taken (call restore) and
     * 
     * - if maxnef error test failures have occurred or if ABS(h) = hmin, we
     * return ERR_FAILURE.
     * 
     * - if more than MXNEF1 error test failures have occurred, an order
     * reduction is forced. If already at order 1, restart by reloading zn from
     * scratch. If f() fails we return either RHSFUNC_FAIL or
     * UNREC_RHSFUNC_ERR (no recovery is possible at this stage).
     * 
     * - otherwise, set *nflagPtr to PREV_ERR_FAIL, and return TRY_AGAIN.
     * 
     */
    private int doErrorTest(double savedTime)
    {
        dsm = acorNorm * tq[2];

        // If est. local error norm dsm passes test, return SUCCESS
        if( dsm <= 1 )
            return SUCCESS;

        // Test failed; increment counters, set nflag, and restore zn array
        nef++;
        nTestFails++;
        nFlag = PREV_ERR_FAIL;
        restore(savedTime);

        // At maxnef failures or |h| = hmin, return ERR_FAILURE
        if( Math.abs(h) <= hMin * ONEPSM || nef == maxTestFails )
            return ERR_FAILURE;

        // Set etamax = 1 to prevent step size increase at end of this step
        etaMax = 1;

        // Set h ratio eta from dsm, rescale, and return for retry of step
        if( nef <= MXNEF1 )
        {
            eta = 1.0 / ( Math.pow(BIAS2 * dsm, 1.0 / qPlusOne) + ADDON );
            eta = Math.max(ETAMIN, Math.max(eta, hMin / Math.abs(h)));
            if( nef >= SMALL_NEF )
                eta = Math.min(eta, ETAMXF);
            rescaleZ();
            return TRY_AGAIN;
        }

        // After MXNEF1 failures, force an order reduction and retry step
        if( q > 1 )
        {
            eta = Math.max(ETAMIN, hMin / Math.abs(h));
            adjustOrder( -1);
            qPlusOne = q;
            q--;
            qWait = qPlusOne;
            rescaleZ();
            return TRY_AGAIN;
        }

        // If already at order 1, restart: reload zn from scratch
        eta = Math.max(ETAMIN, hMin / Math.abs(h));
        h *= eta;
        hNext = h;
        hScale = h;
        qWait = LONG_WAIT;
        nscon = 0;

        try
        {
            nFCalls++;
            temp = f.dy_dt(tn, z[0]);
        }
        catch( IllegalArgumentException ex )
        {
            return UNREC_RHSFUNC_ERR;
        }
        catch( Exception ex )
        {
            return RHSFUNC_FAIL;
        }

        VectorUtils.scale(h, temp, z[1]);
        return TRY_AGAIN;
    }

    /*
     * =================================================================
     * Functions Implementation after succesful step
     * =================================================================
     */
    /**
     * This routine performs various update operations when the solution to the
     * nonlinear system has passed the local error test. We increment the step
     * counter nst, record the values hu and qu, update the tau array, and apply
     * the corrections to the zn array. The tau[i] are the last q values of h,
     * with tau[1] the most recent. The counter qwait is decremented, and if
     * qwait == 1 (and q < qmax) we save acor and tq[5] for a possible order
     * increase.
     */
    private void completeStep()
    {
        nSteps++;
        nscon++;
        hu = h;
        qu = q;

        for( int i = q; i >= 2; i-- )
            tau[i] = tau[i - 1];
        if( ( q == 1 ) && ( nSteps > 1 ) )
            tau[2] = tau[1];
        tau[1] = h;

        for( int j = 0; j <= q; j++ )
            VectorUtils.linearSum(l[j], acor, z[j]);
        qWait--;
        if( qWait == 1 && q != qMax )
        {
            VectorUtils.copy(acor, z[qMax]);
            tq5Saved = tq[5];
            acorIndex = qMax;
        }
    }

    /**
     * This routine handles the setting of stepsize and order for the next step
     * -- hprime and qprime. Along with hprime, it sets the ratio eta =
     * hprime/h. It also updates other state variables related to a change of
     * step size or order.
     */
    private void prepareNextStep()
    {
        // If etamax = 1, defer step size or order changes
        if( etaMax == 1 )
        {
            qWait = Math.max(qWait, 2);
            qPrime = q;
            hPrime = h;
            eta = 1;
            return;
        }

        // etaq is the ratio of new to old h at the current order
        etaq = 1.0 / ( Math.pow(BIAS2 * dsm, 1.0 / qPlusOne) + ADDON );

        // If no order change, adjust eta and acor in setEta and return
        if( qWait != 0 )
        {
            eta = etaq;
            qPrime = q;
            setEta();
            return;
        }

        /*
         * If qwait = 0, consider an order change. etaqm1 and etaqp1 are the
         * ratios of new to old h at orders q-1 and q+1, respectively.
         * chooseEta selects the largest; setEta adjusts eta and acor
         */
        qWait = 2;
        etaqm1 = computeEtaqm1();
        etaqp1 = computeEtaqp1();
        chooseEta();
        setEta();
    }

    /**
     * This routine adjusts the value of eta according to the various heuristic
     * limits and the optional input hmax. It also resets etamax to be the
     * estimated local error vector.
     */
    private void setEta()
    {
        // If eta below the threshhold THRESH, reject a change of step size
        if( eta < THRESH )
        {
            eta = 1;
            hPrime = h;
        }
        else
        {
            // Limit eta by etamax and hmax, then set hprime
            eta = Math.min(eta, etaMax);
            eta /= Math.max(1, Math.abs(h) * hMaxInv * eta);
            hPrime = h * eta;
            if( qPrime < q )
                nscon = 0;
        }
        // Reset etamax for the next step size change, and scale acor
    }

    /**
     * This routine computes and returns the value of etaqm1 for a possible
     * decrease in order by 1.
     */
    private double computeEtaqm1()
    {
        if( q > 1 )
        {
            double ddn = VectorUtils.wrmsNorm(z[q], errorWeight) * tq[1];
            return 1.0 / ( Math.pow(BIAS1 * ddn, 1.0 / q) + ADDON );
        }
        return 0;
    }

    /**
     * This routine computes and returns the value of etaqp1 for a possible
     * increase in order by 1.
     */
    private double computeEtaqp1()
    {
        if( q != qMax )
        {
            if( tq5Saved == 0 )
                return 0;
            double cquot = ( tq[5] / tq5Saved ) * Math.pow(h / tau[2], qPlusOne);
            VectorUtils.linearSum( -cquot, z[qMax], acor, temp);
            double dup = VectorUtils.wrmsNorm(temp, errorWeight) * tq[3];
            return 1.0 / ( Math.pow(BIAS3 * dup, 1.0 / ( qPlusOne + 1 )) + ADDON );
        }
        return 0;
    }

    /**
     * Given etaqm1, etaq, etaqp1 (the values of eta for qprime = q
     * - 1, q, or q + 1, respectively), this routine chooses the maximum eta
     * value, sets eta to that value, and sets qprime to the corresponding value
     * of q. If there is a tie, the preference order is to (1) keep the same
     * order, then (2) decrease the order, and finally (3) increase the order.
     * If the maximum eta value is below the threshhold THRESH, the order is
     * kept unchanged and eta is set to 1.
     */
    private void chooseEta()
    {
        double etam = Math.max(etaqm1, Math.max(etaq, etaqp1));
        if( etam < THRESH )
        {
            eta = 1;
            qPrime = q;
            return;
        }
        if( etam == etaq )
        {
            eta = etaq;
            qPrime = q;
        }
        else if( etam == etaqm1 )
        {
            eta = etaqm1;
            qPrime = q - 1;

        }
        else
        {
            eta = etaqp1;
            qPrime = q + 1;

            if( method == Method.BDF )
            {
                /*
                 * Store Delta_n in zn[qmax] to be used in order increase
                 * This happens at the last step of order q before an increase
                 * to order q+1, so it represents Delta_n in the ELTE at q+1
                 */
                VectorUtils.copy(acor, z[qMax]);
            }
        }
    }

    /**
     *
     */
    private void handleFailure(int flag)
    {
        switch( flag )
        {
            case ERR_FAILURE:
                processError("At t = " + tn + " and h = " + h + ", the error test failed repeatedly or with |h| = hmin.");
                break;
            case CONV_FAILURE:
                processError("At t = " + tn + " and h = " + h + ", the corrector convergence test failed repeatedly or with |h| = hmin.");
                break;
            case LSETUP_FAIL:
                processError("At t = " + tn + ", the setup routine failed in an unrecoverable manner.");
                break;
            case LSOLVE_FAIL:
                processError("At t = " + tn + ", the solve routine failed in an unrecoverable manner.");
                break;
            case RHSFUNC_FAIL:
                processError("At t = " + tn + ", the right-hand side routine failed in an unrecoverable manner.");
                break;
            case UNREC_RHSFUNC_ERR:
                processError("At t = " + tn + ", the right-hand side failed in a recoverable manner, but no recovery is possible.");
                break;
            case REPTD_RHSFUNC_ERR:
                processError("At t = " + tn + " repeated recoverable right-hand side function errors.");
                break;
            case TOO_CLOSE:
                processError("tout too close to t0 to start integration.");
                break;
            default:
                return;
        }
    }

    /*
     * ================================================================= BDF
     * Stability Limit Detection
     * =================================================================
     */

    /**
     * This routine handles the BDF Stability Limit Detection Algorithm STALD.
     * It is called if lmm = BDF and the SLDET option is on. If the order is
     * 3 or more, the required norm data is saved. If a decision to reduce order
     * has not already been made, and enough data has been saved, CVsldet is
     * called. If it signals a stability limit violation, the order is reduced,
     * and the step size is reset accordingly.
     */
    private void stabilityBDF()
    {
        /*
         * If order is 3 or greater, then save scaled derivative data, push old
         * data down in i, then add current values to top.
         */
        if( q >= 3 )
        {
            for( int k = 1; k <= 3; k++ )
                for( int i = 5; i >= 2; i-- )
                    ssdat[i][k] = ssdat[i - 1][k];

            double factorial = 1;
            for( int i = 1; i <= q - 1; i++ )
                factorial *= i;
            double sq = factorial * q * ( q + 1 ) * acorNorm / Math.max(tq[5], TINY);
            double sqm1 = factorial * q * VectorUtils.wrmsNorm(z[q], errorWeight);
            double sqm2 = factorial * VectorUtils.wrmsNorm(z[q - 1], errorWeight);
            ssdat[1][1] = sqm2 * sqm2;
            ssdat[1][2] = sqm1 * sqm1;
            ssdat[1][3] = sq * sq;
        }

        if( qPrime >= q )
        {

            /*
             * If order is 3 or greater, and enough ssdat has been saved, nscon
             * >= q+5, then call stability limit detection routine.
             */
            if( q >= 3 && nscon >= q + 5 )
            {
                if( checkStability() > 3 )
                {
                    /*
                     * A stability limit violation is indicated by a return flag
                     * of 4, 5, or 6. Reduce new order.
                     */
                    qPrime = q - 1;
                    eta = etaqm1;
                    eta = Math.min(eta, etaMax);
                    eta /= Math.max(1, Math.abs(h) * hMaxInv * eta);
                    hPrime = h * eta;
                    nOrderReduct = nOrderReduct + 1;
                }
            }
        }
        else
        {
            /*
             * Otherwise, let order increase happen, and reset stability limit
             * counter, nscon.
             */
            nscon = 0;
        }
    }

    /**
     * This routine detects stability limitation using stored scaled derivatives
     * data. sldet returns the magnitude of the dominate characteristic root,
     * rr. The presents of a stability limit is indicated by rr >
     * "something a little less then 1.0", and a positive kflag. This routine
     * should only be called if order is greater than or equal to 3, and data
     * has been collected for 5 time steps.
     * 
     * @return kflag = 1 -> Found stable characteristic root, normal matrix case
     *         kflag = 2 -> Found stable characteristic root, quartic solution
     *         kflag = 3 -> Found stable characteristic root, quartic solution,
     *         with Newton correction kflag = 4 -> Found stability violation,
     *         normal matrix case kflag = 5 -> Found stability violation,
     *         quartic solution kflag = 6 -> Found stability violation, quartic
     *         solution, with Newton correction
     * 
     *         kflag < 0 -> No stability limitation, or could not compute
     *         limitation.
     * 
     *         kflag = -1 -> Min/max ratio of ssdat too small. kflag = -2 -> For
     *         normal matrix case, vmax > vrrt2*vrrt2 kflag = -3 -> For normal
     *         matrix case, The three ratios are inconsistent. kflag = -4 ->
     *         Small coefficient prevents elimination of quartics. kflag = -5 ->
     *         R value from quartics not consistent. kflag = -6 -> No corrected
     *         root passes test on qk values kflag = -7 -> Trouble solving for
     *         sigsq. kflag = -8 -> Trouble solving for B, or R via B. kflag =
     *         -9 -> R via sigsq[k] disagrees with R from data.
     */
    int checkStability()
    {
        int i, k, j, it, kmin, kflag = 0;
        double[][] rat = new double[5][4];
        double[] rav = new double[4];
        double[] qkr = new double[4];
        double[] sigsq = new double[4];
        double[] smax = new double[4];
        double[] ssmax = new double[4];
        double[] drr = new double[4];
        double[] rrc = new double[4];
        double[] sqmx = new double[4];
        double[][] qjk = new double[4][4];
        double[] vrat = new double[5];
        double[][] qc = new double[6][4];
        double[][] qco = new double[6][4];
        double smink, smaxk, sumrat, sumrsq, vmin, vmax, drrmax, adrr;
        double tem, sqmax, saqk, qp, s, sqmaxk, saqj, sqmin;
        double rsa, rsb, rsc, rsd, rd1a, rd1b, rd1c;
        double rd2a, rd2b, rd3a, cest1, corr1;
        double ratp, ratm, qfac1, qfac2, bb, rrb;

        /* The following are cutoffs and tolerances used by this routine */
        double rrcut = 0.98;
        double vrrtol = 1.0e-4;
        double vrrt2 = 5.0e-4;
        double sqtol = 1.0e-3;
        double rrtol = 1.0e-2;
        double rr = 0;

        /* Index k corresponds to the degree of the interpolating polynomial. */
        /* k = 1 -> q-1 */
        /* k = 2 -> q */
        /* k = 3 -> q+1 */

        /* Index i is a backward-in-time index, i = 1 -> current time, */
        /* i = 2 -> previous step, etc */

        /* get maxima, minima, and variances, and form quartic coefficients */
        double[][] ssdat = this.ssdat;
        for( k = 1; k <= 3; k++ )
        {
            smink = ssdat[1][k];
            smaxk = 0;

            for( i = 1; i <= 5; i++ )
            {
                smink = Math.min(smink, ssdat[i][k]);
                smaxk = Math.min(smaxk, ssdat[i][k]);
            }

            if( smink < TINY * smaxk )
                return -1;

            smax[k] = smaxk;
            ssmax[k] = smaxk * smaxk;

            sumrat = 0;
            sumrsq = 0;
            for( i = 1; i <= 4; i++ )
            {
                rat[i][k] = ssdat[i][k] / ssdat[i + 1][k];
                sumrat = sumrat + rat[i][k];
                sumrsq = sumrsq + rat[i][k] * rat[i][k];
            }
            rav[k] = 0.25 * sumrat;
            vrat[k] = Math.abs(0.25 * sumrsq - rav[k] * rav[k]);

            qc[5][k] = ssdat[1][k] * ssdat[3][k] - ssdat[2][k] * ssdat[2][k];
            qc[4][k] = ssdat[2][k] * ssdat[3][k] - ssdat[1][k] * ssdat[4][k];
            qc[3][k] = 0;
            qc[2][k] = ssdat[2][k] * ssdat[5][k] - ssdat[3][k] * ssdat[4][k];
            qc[1][k] = ssdat[4][k] * ssdat[4][k] - ssdat[3][k] * ssdat[5][k];

            for( i = 1; i <= 5; i++ )
            {
                qco[i][k] = qc[i][k];
            }
        } /* End of k loop */

        /*
         * Isolate normal or nearly-normal matrix case. Three quartic will have
         * common or nearly-common roots in this case. Return a kflag = 1 if
         * this procedure works. If three root differ more than vrrt2, return
         * error kflag = -3.
         */

        vmin = Math.min(vrat[1], Math.min(vrat[2], vrat[3]));
        vmax = Math.max(vrat[1], Math.max(vrat[2], vrat[3]));

        if( vmin < vrrtol * vrrtol )
        {
            if( vmax > vrrt2 * vrrt2 )
            {
                kflag = -2;
                return kflag;
            }
            else
            {
                rr = ( rav[1] + rav[2] + rav[3] ) / 3;

                drrmax = 0;
                for( k = 1; k <= 3; k++ )
                {
                    adrr = Math.abs(rav[k] - rr);
                    drrmax = Math.max(drrmax, adrr);
                }
                if( drrmax > vrrt2 )
                    kflag = -3;
                kflag = 1;
                // can compute charactistic root, drop to next section
            }
        }
        else
        {
            // use the quartics to get rr.
            if( Math.abs(qco[1][1]) < TINY * ssmax[1] )
                return -4;

            tem = qco[1][2] / qco[1][1];
            for( i = 2; i <= 5; i++ )
                qco[i][2] = qco[i][2] - tem * qco[i][1];

            qco[1][2] = 0;
            tem = qco[1][3] / qco[1][1];
            for( i = 2; i <= 5; i++ )
                qco[i][3] = qco[i][3] - tem * qco[i][1];
            qco[1][3] = 0;

            if( Math.abs(qco[2][2]) < TINY * ssmax[2] )
                return -4;

            tem = qco[2][3] / qco[2][2];
            for( i = 3; i <= 5; i++ )
                qco[i][3] = qco[i][3] - tem * qco[i][2];

            if( Math.abs(qco[4][3]) < TINY * ssmax[3] )
                return -4;

            rr = -qco[5][3] / qco[4][3];

            if( rr < TINY || rr > 100 )
                return -5;

            for( k = 1; k <= 3; k++ )
                qkr[k] = qc[5][k] + rr * ( qc[4][k] + rr * rr * ( qc[2][k] + rr * qc[1][k] ) );

            sqmax = 0;
            for( k = 1; k <= 3; k++ )
            {
                saqk = Math.abs(qkr[k]) / ssmax[k];
                if( saqk > sqmax )
                    sqmax = saqk;
            }

            if( sqmax < sqtol )
            {
                kflag = 2;
                // can compute charactistic root, drop to "given rr,etc" */
            }
            else
            {
                sqmin = 0;
                // do Newton corrections to improve rr. */
                for( it = 1; it <= 3; it++ )
                {
                    for( k = 1; k <= 3; k++ )
                    {
                        qp = qc[4][k] + rr * rr * ( 3 * qc[2][k] + rr * 4 * qc[1][k] );
                        drr[k] = 0;
                        if( Math.abs(qp) > TINY * ssmax[k] )
                            drr[k] = -qkr[k] / qp;
                        rrc[k] = rr + drr[k];
                    }

                    for( k = 1; k <= 3; k++ )
                    {
                        s = rrc[k];
                        sqmaxk = 0;
                        for( j = 1; j <= 3; j++ )
                        {
                            qjk[j][k] = qc[5][j] + s * ( qc[4][j] + s * s * ( qc[2][j] + s * qc[1][j] ) );
                            saqj = Math.abs(qjk[j][k]) / ssmax[j];
                            if( saqj > sqmaxk )
                                sqmaxk = saqj;
                        }
                        sqmx[k] = sqmaxk;
                    }

                    sqmin = sqmx[1];
                    kmin = 1;
                    for( k = 2; k <= 3; k++ )
                    {
                        if( sqmx[k] < sqmin )
                        {
                            kmin = k;
                            sqmin = sqmx[k];
                        }
                    }
                    rr = rrc[kmin];

                    if( sqmin < sqtol )
                    {
                        kflag = 3;
                        /* can compute charactistic root */
                        /*
                         * break out of Newton correction loop and drop to
                         * "given rr,etc"
                         */
                        break;
                    }
                    else
                    {
                        for( j = 1; j <= 3; j++ )
                            qkr[j] = qjk[j][kmin];
                    }
                } // end of Newton correction loop */

                if( sqmin > sqtol )
                    return -6;

            } // end of if (sqmax < sqtol) else */
        } // end of if(vmin < vrrtol*vrrtol) else, quartics to get rr. */

        // given rr, find sigsq[k] and verify rr. */
        // All positive kflag drop to this section */

        for( k = 1; k <= 3; k++ )
        {
            rsa = ssdat[1][k];
            rsb = ssdat[2][k] * rr;
            rsc = ssdat[3][k] * rr * rr;
            rsd = ssdat[4][k] * rr * rr * rr;
            rd1a = rsa - rsb;
            rd1b = rsb - rsc;
            rd1c = rsc - rsd;
            rd2a = rd1a - rd1b;
            rd2b = rd1b - rd1c;
            rd3a = rd2a - rd2b;

            if( Math.abs(rd1b) < TINY * smax[k] )
                return -7;

            cest1 = -rd3a / rd1b;
            if( cest1 < TINY || cest1 > 4 )
                return -7;

            corr1 = ( rd2b / cest1 ) / ( rr * rr );
            sigsq[k] = ssdat[3][k] + corr1;
        }

        if( sigsq[2] < TINY )
            return -8;

        ratp = sigsq[3] / sigsq[2];
        ratm = sigsq[1] / sigsq[2];
        qfac1 = 0.25 * ( q * q - 1 );
        qfac2 = 2.0 / ( q - 1 );
        bb = ratp * ratm - 1 - qfac1 * ratp;
        tem = 1 - qfac2 * bb;

        if( Math.abs(tem) < TINY )
            return -8;

        rrb = 1.0 / tem;

        if( Math.abs(rrb - rr) > rrtol )
            return -9;

        // Check to see if rr is above cutoff rrcut
        if( rr > rrcut )
        {
            if( kflag == 1 )
                kflag = 4;
            if( kflag == 2 )
                kflag = 5;
            if( kflag == 3 )
                kflag = 6;
        }

        // All positive kflag returned at this point
        return kflag;
    }

    /*
     * =================================================================
     * Root finding
     * =================================================================
     */

    /**
     * This routine completes the initialization of rootfinding memory
     * information, and checks whether g has a zero both at and very near the
     * initial point of the IVP.
     */
    private void checkRoot1() throws Exception
    {
        for( int i = 0; i < eventNumber; i++ )
            eventInfo[i] = 0;
        tLo = tn;
        eventTol = ( Math.abs(tn) + Math.abs(h) ) * UROUND_100;

        // Evaluate g at initial t and check for zero values.
        try
        {
            nEventFuncionCalls = 1;
            eventFuncLo = f.checkEvent(tLo, z[0]);
        }
        catch( Exception ex )
        {
            throw new Exception("At t = " + tn + ", the rootfinding routine failed in an unrecoverable manner.");
        }
        boolean zroot = false;
        for( int i = 0; i < eventNumber; i++ )
        {
            if( Math.abs(eventFuncLo[i]) == 0 )
            {
                zroot = true;
                eventActive[i] = false;
            }
        }
        if( !zroot )
            return;

        // Some g_i is zero at t0; look at g at t0+(small increment).
        double hratio = Math.max(eventTol / Math.abs(h), 0.1);
        double smallh = hratio * h;
        tLo += smallh;
        VectorUtils.linearSum(hratio, z[1], z[0], y);
        try
        {
            eventFuncLo = f.checkEvent(tLo, y);
        }
        catch( Exception ex )
        {
            throw new Exception("At t = " + tn + ", the rootfinding routine failed in an unrecoverable manner.");
        }
        nEventFuncionCalls++;

        /* We check now only the components of g which were exactly 0.0 at t0 to
         * see if we can 'activate' them.
         */
        for( int i = 0; i < eventNumber; i++ )
        {
            if( !eventActive[i] && Math.abs(eventFuncLo[i]) != 0 )
                eventActive[i] = false;
        }
    }

    /**
     * This routine checks for exact zeros of g at the last root found, if the
     * last return was a root. It then checks for a close pair of zeros (an
     * error condition), and for a new root at a nearby point. The left endpoint
     * (tlo) of the search interval is adjusted if necessary to assure that all
     * g_i are nonzero there, before returning to do a root search in the
     * interval.
     * 
     * On entry, tlo = tretlast is the last value of tret returned by JVode.
     * This may be the previous tn, the previous tout value, or the last root
     * location.
     */
    private boolean checkRoot2() throws Exception
    {
        if( lastStepRoot == 0 )
            return false;

        getDky(tLo, 0, y);
        try
        {
            nEventFuncionCalls++;
            eventFuncLo = f.checkEvent(tLo, y);
        }
        catch( Exception ex )
        {
            throw new Exception("At t = " + tn + ", the rootfinding routine failed in an unrecoverable manner.");
        }

        boolean rootFound = false;
        for( int i = 0; i < eventNumber; i++ )
            eventInfo[i] = 0;
        for( int i = 0; i < eventNumber; i++ )
        {
            if( !eventActive[i] )
                continue;
            if( Math.abs(eventFuncLo[i]) == 0 )
            {
                rootFound = true;
                eventInfo[i] = 1;
            }
        }
        if( !rootFound )
            return false;

        // One or more g_i has a zero at tlo. Check g at tlo+smallh.
        eventTol = ( Math.abs(tn) + Math.abs(h) ) * UROUND_100;
        double smallh = ( h > 0 ) ? eventTol : -eventTol;
        tLo += smallh;
        if( ( tLo - tn ) * h >= 0 )
        {
            VectorUtils.linearSum(smallh / h, z[1], y);
        }
        else
        {
            getDky(tLo, 0, y);
        }
        try
        {
            nEventFuncionCalls++;
            eventFuncLo = f.checkEvent(tLo, y);
        }
        catch( Exception ex )
        {
            throw new Exception("At t = " + tn + ", the rootfinding routine failed in an unrecoverable manner.");
        }
        rootFound = false;
        for( int i = 0; i < eventNumber; i++ )
        {
            if( Math.abs(eventFuncLo[i]) == 0 )
            {
                if( !eventActive[i] )
                    continue;
                if( eventInfo[i] == 1 )
                    throw new Exception("Root found at and very near t = " + tLo + ".");
                rootFound = true;
                eventInfo[i] = 1;
            }
        }
        return rootFound;
    }

    /**
     * This routine interfaces to rootfind to look for a root of g between tlo
     * and either tn or tout, whichever comes first. Only roots beyond tlo in
     * the direction of integration are sought.
     */
    private boolean checkRoot3(boolean oneStepMode) throws Exception
    {
        // Set thi = tn or tout, whichever comes first; set y = y(thi).
        if( oneStepMode )
        {
            tHi = tn;
            VectorUtils.copy(z[0], y);
        }
        else
        {
            if( ( toutCopy - tn ) * h >= 0 )
            {
                tHi = tn;
                VectorUtils.copy(z[0], y);
            }
            else
            {
                tHi = toutCopy;
                getDky(tHi, 0, y);
            }
        }

        // Set ghi = g(thi) and call rootfind to search (tlo,thi) for roots.
        try
        {
            nEventFuncionCalls++;
            eventFuncHi = f.checkEvent(tHi, y);
        }
        catch( Exception ex )
        {
            throw new Exception("At t = " + tLo + ", the rootfinding routine failed in an unrecoverable manner.");
        }
        eventTol = ( Math.abs(tn) + Math.abs(h) ) * UROUND_100;

        boolean rootFound = rootfind();

        for( int i = 0; i < eventNumber; i++ )
        {
            if( !eventActive[i] && eventFuncRout[i] != 0 )
                eventActive[i] = true;
        }
        tLo = eventTime;
        for( int i = 0; i < eventNumber; i++ )
            eventFuncLo[i] = eventFuncRout[i];

        // If a root was found, interpolate to get y(trout) and return.
        if( rootFound )
        {
            getDky(eventTime, 0, y);
            return true;
        }
        return false;
    }

    /**
     * This routine solves for a root of g(t) between tlo and thi, if one
     * exists. Only roots of odd multiplicity (i.e. with a change of sign in one
     * of the g_i), or exact zeros, are found. Here the sign of tlo - thi is
     * arbitrary, but if multiple roots are found, the one closest to tlo is
     * returned.
     * 
     * The method used is the Illinois algorithm, a modified secant method.
     * Reference: Kathie L. Hiebert and Lawrence F. Shampine, Implicitly Defined
     * Output Points for Solutions of ODEs, Sandia National Laboratory Report
     * SAND80-0180, February 1980.
     * 
     * This routine uses the following parameters for communication:
     * 
     * @param nrtfn
     *            = number of functions g_i, or number of components of the
     *            vector-valued function g(t). Input only.
     * 
     * @param gfun
     *            = user-defined function for g(t). Its form is (void) gfun(t,
     *            y, gt, user_data)
     * 
     * @param rootdir
     *            = in array specifying the direction of zero-crossings. If
     *            rootdir[i] > 0, search for roots of g_i only if g_i is
     *            increasing; if rootdir[i] < 0, search for roots of g_i only if
     *            g_i is decreasing; otherwise always search for roots of g_i.
     * 
     * @param gactive
     *            = array specifying whether a component of g should or should
     *            not be monitored. gactive[i] is initially set to TRUE for all
     *            i=0,...,nrtfn-1, but it may be reset to FALSE if at the first
     *            step g[i] is 0.0 both at the I.C. and at a small perturbation
     *            of them. gactive[i] is then set back on TRUE only after the
     *            corresponding g function moves away from 0.0.
     * 
     * @param nge
     *            = cumulative counter for gfun calls.
     * 
     * @param ttol
     *            = a convergence tolerance for trout. Input only. When a root
     *            at trout is found, it is located only to within a tolerance of
     *            ttol. Typically, ttol should be set to a value on the order of
     *            100 * UROUND * max (ABS(tlo), ABS(thi)) where UROUND is the
     *            unit roundoff of the machine.
     * 
     * @param tlo
     *            , thi = endpoints of the interval in which roots are sought.
     *            On input, and must be distinct, but tlo - thi may be of either
     *            sign. The direction of integration is assumed to be from tlo
     *            to thi. On return, tlo and thi are the endpoints of the final
     *            relevant interval.
     * 
     * @param glo
     *            , ghi = arrays of length nrtfn containing the vectors g(tlo)
     *            and g(thi) respectively. Input and output. On input, none of
     *            the glo[i] should be zero.
     * 
     * @param trout
     *            = root location, if a root was found, or thi if not. Output
     *            only. If a root was found other than an exact zero of g, trout
     *            is the endpoint thi of the final interval bracketing the root,
     *            with size at most ttol.
     * 
     * @param grout
     *            = array of length nrtfn containing g(trout) on return.
     * 
     * @param iroots
     *            = int array of length nrtfn with root information. Output
     *            only. If a root was found, iroots indicates which components
     *            g_i have a root at trout. For i = 0, ..., nrtfn-1, iroots[i] =
     *            1 if g_i has a root and g_i is increasing, iroots[i] = -1 if
     *            g_i has a root and g_i is decreasing, and iroots[i] = 0 if g_i
     *            has no roots or g_i varies in the direction opposite to that
     *            indicated by rootdir[i].
     * 
     *            This routine returns an int equal to: RTFUNC_FAIL = -12 if
     *            the g function failed, or RTFOUND = 1 if a root of g was
     *            found, or SUCCESS = 0 otherwise.
     */
    private boolean rootfind() throws Exception
    {
        double tmid, gfrac, fracint, fracsub;
        int imax = 0;

        // First check for change in sign in ghi or for a zero in ghi.
        double maxfrac = 0;
        boolean rootFound = false;
        boolean sgnchg = false;
        for( int i = 0; i < eventNumber; i++ )
        {
            if( !eventActive[i] )
                continue;
            if( Math.abs(eventFuncHi[i]) == 0 )
            {
                if( eventDirections[i] * eventFuncLo[i] <= 0 )
                {
                    rootFound = true;
                }
            }
            else
            {
                if( ( eventFuncLo[i] * eventFuncHi[i] < 0 ) && ( eventDirections[i] * eventFuncLo[i] <= 0 ) )
                {
                    gfrac = Math.abs(eventFuncHi[i] / ( eventFuncHi[i] - eventFuncLo[i] ));
                    if( gfrac > maxfrac )
                    {
                        sgnchg = true;
                        maxfrac = gfrac;
                        imax = i;
                    }
                }
            }
        }

        //If no sign change was found, reset trout and grout.
        if( !sgnchg )
        {
            eventTime = tHi;
            for( int i = 0; i < eventNumber; i++ )
                eventFuncRout[i] = eventFuncHi[i];
            if( !rootFound )
                return false;
            for( int i = 0; i < eventNumber; i++ )
            {
                eventInfo[i] = 0;
                if( !eventActive[i] )
                    continue;
                if( Math.abs(eventFuncHi[i]) == 0 )
                    eventInfo[i] = eventFuncLo[i] > 0 ? -1 : 1;
            }
            return true;
        }

        // Initialize alpha to avoid compiler warning
        double alpha = 1;

        // A sign change was found. Loop to locate nearest root.
        int side = 0;
        int sideprev = -1;
        for( ;; )
        {
            /*
             * Set weight alpha. On the first two passes, set alpha = 1.
             * Thereafter, reset alpha according to the side (low vs high) of
             * the subinterval in which the sign change was found in the
             * previous two passes. If the sides were opposite, set alpha = 1.
             * If the sides were the same, then double alpha (if high side), or
             * halve alpha (if low side). The next guess tmid is the secant
             * method value if alpha = 1, but is closer to tlo if alpha < 1, and
             * closer to thi if alpha > 1.
             */
            if( sideprev == side )
                alpha = ( side == 2 ) ? alpha * 2 : alpha * 0.5;
            else
                alpha = 1;

            /*
             * Set next root approximation tmid and get g(tmid). If tmid is too
             * close to tlo or thi, adjust it inward, by a fractional distance
             * that is between 0.1 and 0.5.
             */
            tmid = tHi - ( tHi - tLo ) * eventFuncHi[imax] / ( eventFuncHi[imax] - alpha * eventFuncLo[imax] );
            if( Math.abs(tmid - tLo) < 0.5 * eventTol )
            {
                fracint = Math.abs(tHi - tLo) / eventTol;
                fracsub = ( fracint > 5 || fracint == 0) ? 0.1 : 0.5 / fracint;
                tmid = tLo + fracsub * ( tHi - tLo );
            }
            if( Math.abs(tHi - tmid) < 0.5 * eventTol )
            {
                fracint = Math.abs(tHi - tLo) / eventTol;
                fracsub = ( fracint > 5 || fracint == 0) ? 0.1 : 0.5 / fracint;
                tmid = tHi - fracsub * ( tHi - tLo );
            }

            getDky(tmid, 0, y);
            try
            {
                nEventFuncionCalls++;
                eventFuncRout = f.checkEvent(tmid, y);
            }
            catch( Exception ex )
            {
                throw new Exception("At t = " + tn + ", the rootfinding routine failed in an unrecoverable manner.");
            }
            /*
             * Check to see in which subinterval g changes sign, and reset imax.
             * Set side = 1 if sign change is on low side, or 2 if on high side.
             */
            maxfrac = 0;
            rootFound = false;
            sgnchg = false;
            sideprev = side;
            for( int i = 0; i < eventNumber; i++ )
            {
                if( !eventActive[i] )
                    continue;
                if( Math.abs(eventFuncRout[i]) == 0 )
                {
                    if( eventDirections[i] * eventFuncLo[i] <= 0 )
                        rootFound = true;
                }
                else
                {
                    if( ( eventFuncLo[i] * eventFuncRout[i] < 0 ) && ( eventDirections[i] * eventFuncLo[i] <= 0 ) )
                    {
                        gfrac = Math.abs(eventFuncRout[i] / ( eventFuncRout[i] - eventFuncLo[i] ));
                        if( gfrac > maxfrac )
                        {
                            sgnchg = true;
                            maxfrac = gfrac;
                            imax = i;
                        }
                    }
                }
            }
            if( sgnchg )
            {
                // Sign change found in (tlo,tmid); replace thi with tmid.
                tHi = tmid;
                for( int i = 0; i < eventNumber; i++ )
                    eventFuncHi[i] = eventFuncRout[i];
                side = 1;
                // Stop at root thi if converged; otherwise loop.
                if( Math.abs(tHi - tLo) <= eventTol )
                    break;
                continue; // Return to looping point.
            }

            if( rootFound )
            {
                // No sign change in (tlo,tmid), but g = 0 at tmid; return root tmid.
                tHi = tmid;
                for( int i = 0; i < eventNumber; i++ )
                    eventFuncHi[i] = eventFuncRout[i];
                break;
            }

            /*
             * No sign change in (tlo,tmid), and no zero at tmid. Sign change
             * must be in (tmid,thi). Replace tlo with tmid.
             */
            tLo = tmid;
            for( int i = 0; i < eventNumber; i++ )
                eventFuncLo[i] = eventFuncRout[i];
            side = 2;
            // Stop at root thi if converged; otherwise loop back.
            if( Math.abs(tHi - tLo) <= eventTol )
                break;

        } // End of root-search loop

        // Reset trout and grout, set iroots.
        eventTime = tHi;
        for( int i = 0; i < eventNumber; i++ )
        {
            eventFuncRout[i] = eventFuncHi[i];
            eventInfo[i] = 0;
            if( !eventActive[i] )
                continue;
            if( ( Math.abs(eventFuncHi[i]) == 0 || eventFuncLo[i] * eventFuncHi[i] < 0 ) && ( eventDirections[i] * eventFuncLo[i] <= 0 ) )
                eventInfo[i] = eventFuncLo[i] > 0 ? -1 : 1;
        }
        return true;
    }


    protected static void processError(String msgfmt)
    {
        log.info(msgfmt);
    }

    @Override
    int init()
    {
        return 0;
    }

    @Override
    int setup(int convfail)
    {
        return 0;
    }

    @Override
    int solve(double[] b)
    {
        return 0;
    }

    public void setDetectIncorrectNumbers(boolean detectIncorrectNumbers)
    {
        this.detectIncorrectNumbers = detectIncorrectNumbers;
    }
}
