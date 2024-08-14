package biouml.plugins.simulation.ode.jvode;

import java.util.Arrays;

import biouml.plugins.simulation.ode.OdeModel;

public abstract class JVodeSupport
{
    public final static int ADAMS_Q_MAX = 12; // max value of q for method == ADAMS
    public final static int BDF_Q_MAX = 5; // max value of q for method == BDF
    public final static int Q_MAX = ADAMS_Q_MAX; // max value of q for either method
    public final static int L_MAX = ( Q_MAX + 1 ); // max value of L for either method
    public final static int NUM_TESTS = 5; // number of error test quantities

    public final static double CORTES = 0.1;
    public final static int MXNCF = 10; //max no. of convergence failures during one step try
    public final static int MXNEF = 7; //max no. of error test failures during one step try
    public final static int NLS_MAXCOR = 3; //maximum no. of corrector iterations for the nonlinear solver

    public final static int MXHNIL_DEFAULT = 10; // mxhnil default value
    public final static int MXSTEP_DEFAULT = 100000; // mxstep default value
    public final static double HMIN_DEFAULT = 1E-100; // hmin default value
    public final static double HMAX_INV_DEFAULT = 1E-100; // hmax_inv default value

    private final static double ETAMX1 = 10000.0;

    public final static double UROUND = 2.220446049250313e-016;// machine unit roundoff

    public final static double UROUND_100 = UROUND * 100;

    public final static double UROUND_SQRT = Math.sqrt(UROUND);

    /*--------------------------
      Problem Specification Data
      --------------------------*/
    protected OdeModel f; // y' = f(t,y(t))

    /**
     * The user of the JVODE package specifies whether to use the ADAMS (Adams-Moulton) or BDF (Backward Differentiation
     * Formula) linear multistep method. The BDF method is recommended for stiff problems, and the ADAMS method is
     * recommended for nonstiff problems.
     */
    public enum Method
    {
        ADAMS, BDF;
    }

    /**
    * At each internal time step, a nonlinear equation must be solved. The user can specify either FUNCTIONAL
    * iteration, which does not require linear algebra, or a NEWTON iteration, which requires the solution of linear
    * systems. In the NEWTON case, the user also specifies a linear solver. NEWTON is recommended in case of
    * stiff problems.
    */
    public enum IterationType
    {
        NEWTON, FUNCTIONAL;
    }

    public enum JacobianType
    {
        DENSE, BAND, DIAG;
    }

    Method method;
    IterationType iterationType;
    JacobianType jacobianType;

    protected double rtol; // relative tolerance
    protected double[] atolVector; // vector absolute tolerance

    protected EwtFn ewtFunc; // function to set ewt

    protected double time;
    public double getTime()
    {
        return time;
    }

    /*-----------------------
      Nordsieck History Array
      -----------------------*/
    protected double[][] z = new double[L_MAX][]; /* Nordsieck array, of size N x (q+1).
                                                  zn[j] is a vector of length N (j=0,...,q)
                                                  zn[j] = [1/factorial(j)] * h^j * (jth
                                                  derivative of the interpolating polynomial       */

    /*--------------------------
      other vectors of length n
      -------------------------*/
    protected int n;
    protected double[] errorWeight; // error weight vector
    protected double[] y;
    public double[] getY()
    {
        return Arrays.copyOf(y, y.length);
    }

    public void setInitialValues(double[] x0, double t0) throws Exception
    {
        y = Arrays.copyOf(x0, n);
        for( int j = 0; j <= qMax; j++ )
            z[j] = new double[n];
        z[0] = Arrays.copyOf(x0, n);
        tn = t0;
        time = t0;
        f.extendResult(t0, x0);
    }

    protected double[] acor; /* In the context of the solution of the nonlinear
                             equation, acor = y_n(m) - y_n(0). On return,
                             this vector is scaled to give the est. local err.   */
    protected double[] temp; // temporary storage vector
    protected double[] ftemp; // temporary storage vector

    /*-----------------
      Tstop information
      -----------------*/
    protected boolean tStopSet;
    protected double tStop;

    /*---------
      Step Data
      ---------*/
    protected int q; // current order
    protected int qPrime; // order to be used on the next step  = q-1, q, or q+1
    protected int qNext; // order to be used on the next step
    protected int qWait; // number of internal steps to wait before considering a change in q
    protected int qPlusOne; // L = q + 1

    protected double h0; // initial step size
    protected double h; // current step size
    protected double hPrime; // step size to be used on the next step
    protected double hNext; // step size to be used on the next step
    protected double eta; // eta = hprime / h
    protected double hScale; // value of h used in zn
    protected double tn; // current internal value of t
    protected double cv_tretlast; // value of tret last returned by JVode

    protected double[] tau = new double[L_MAX + 1]; // array of previous q+1 successful step sizes indexed from 1 to q+1
    protected double[] tq = new double[NUM_TESTS + 1];// array of test quantities indexed from 1 to NUM_TESTS(=5)
    protected double[] l = new double[L_MAX]; //coefficients of l(x) (degree q poly)

    protected double rl1; // the scalar 1/l[1]
    protected double gamma; // gamma = h * rl1
    protected double gammaPrev; // gamma at the last setup call
    protected double gammaRatio; // gamma / gammap

    protected double convergenceRate; // estimated corrector convergence rate
    protected double acorNorm; // | acor | wrms
    protected double convCoef = CORTES; // coeficient in nonlinear convergence test
    protected int mNewt; // Newton iteration counter

    /*------
      Limits
      ------*/
    protected int qMax; // q <= qmax
    protected int maxSteps = MXSTEP_DEFAULT; // maximum number of internal steps for one user call

    public void setMaxSteps(int max)
    {
        maxSteps = max;
    }

    public int getMaxSteps()
    {
        return maxSteps;
    }

    protected int maxCor = NLS_MAXCOR; // maximum number of corrector iterations for the  solution of the nonlinear equation
    protected int maxHnil = MXHNIL_DEFAULT; // maximum number of warning messages issued to the user that t + h == t for the next internal step
    protected int maxTestFails = MXNEF; // maximum number of error test failures
    protected int maxConvFails = MXNCF; // maximum number of nonlinear convergence failures

    protected double hMin = HMIN_DEFAULT; // |h| >= hmin
    public void setHMin(double min)
    {
        hMin = min;
    }
    protected double hMaxInv = HMAX_INV_DEFAULT; // |h| <= 1/hmax_inv
    public void setHMaxInv(double max)
    {
        hMaxInv = max;
    }
    double etaMax = ETAMX1; /* eta <= etamax                                      */

    //Technical values
    protected int nFlag;
    protected double dsm;
    protected int nef;
    protected int ncf;

    /*--------
      Counters
      --------*/
    public int nSteps; /* number of internal steps taken                  */
    public int nFCalls; /* cv_nfe number of f calls                               */
    public int nCorrFails; /*cv_ncfn number of corrector convergence failures        */
    public int nTestFails;/*cv_netf number of error test failures                   */
    public int nNewtonIter; /*cv_nni number of Newton iterations performed           */
    public int nSetupCalls; /*cv_nsetups number of setup calls                           */
    public int nHnil; //cv_nhnil number of messages issued to the user that   t + h == t for the next iternal step

    protected double etaqm1; /* ratio of new to old h for order q-1             */
    protected double etaq; /* ratio of new to old h for order q               */
    protected double etaqp1; /* ratio of new to old h for order q+1             */

    /*------------
      Saved Values
      ------------*/
    public int qu; /* last successful q value used                */
    protected long nLastSetupSteps; /* step number of last setup call              */
    protected double h0u; /* actual initial stepsize                     */
    public double hu; /* last successful h value used                */
    protected double tq5Saved; /* saved value of tq[5]                        */
    protected boolean currentJacobian; /* is Jacobian info. for lin. solver current?  */
    protected double toleranceScale; /* tolerance scale factor                      */
    protected int acorIndex; /* index of the zn vector with saved acor      */
    protected boolean setupNonNull; /* does setup do anything?                     */

    /*-------------------------
      Stability Limit Detection
      -------------------------*/
    protected boolean stabLimitDetect = false;; /* is Stability Limit Detection on?             */
    protected double[][] ssdat = new double[6][4]; /* scaled data array for STALD                  */
    protected int nscon; /* counter for STALD method                     */
    protected long nOrderReduct; /* counter for number of order reductions       */

    /*----------------
      Rootfinding Data
      ----------------*/
    protected int eventNumber; // number of components of g
    protected int[] eventInfo; // array for root information
    public int[] getEventInfo()
    {
        return eventInfo;
    }
    protected int[] eventDirections; // array specifying direction of zero-crossing
    protected double tLo; // nearest endpoint of interval in root search
    protected double tHi; //farthest endpoint of interval in root search
    protected double eventTime; // t value returned by rootfinding routine
    protected double[] eventFuncLo; // saved array of g values at t = tlo
    protected double[] eventFuncHi; // saved array of g values at t = thi
    protected double[] eventFuncRout; // array of g values at t = trout
    protected double toutCopy; // copy of tout (if NORMAL mode)
    protected double eventTol; // tolerance on root location
    protected int lastStepRoot; // flag showing whether last step had a root
    public long nEventFuncionCalls; // counter for g evaluations
    protected boolean[] eventActive; // array with active/inactive event functions
    protected int maxWrnMessages; // number of warning messages about possible g==0  */

    /*------------------
    Linear Solver Data
    ------------------*/
    abstract int init();

    abstract int setup(int convfail);

    abstract int solve(double[] b);

    public JVodeSupport(Method method, OdeModel model, double[] u0, double t0) throws IllegalArgumentException
    {
        if( model == null )
            throw new IllegalArgumentException("Model is null.");

        n = u0.length;
        y = Arrays.copyOf(u0, n); //intiial values
        tn = t0; //initial time
        time = t0;
        f = model; //right hand function

        this.method = method;
        this.iterationType = IterationType.FUNCTIONAL;

        /* Set default values for integrator optional inputs */
        //        userErrorFun = false;
        qMax = ( method == Method.ADAMS ) ? ADAMS_Q_MAX : BDF_Q_MAX;
        tStopSet = false;

        /* Initialize root finding variables */
        maxWrnMessages = 1;

        /* Set the saved value qmax_alloc */
        errorWeight = new double[n];
        acor = new double[n];
        temp = new double[n];
        ftemp = new double[n];

        for( int j = 0; j <= qMax; j++ )
            z[j] = new double[n];
        VectorUtils.copy(y, z[0]);

        // Set step parameters
        q = 1;
        qPlusOne = 2;
        qWait = qPlusOne;
        toleranceScale = 1;
        setupNonNull = true;

        //Events Initialization
        eventNumber = 0;

        try
        {
            double[] test = f.checkEvent(tn, y);
            if( test != null )
                eventNumber = test.length;
        }
        catch( Exception ex )
        {
            return;
        }

        if( eventNumber == 0 )
            return;

        // Allocate necessary memory and return
        eventFuncLo = new double[eventNumber];
        eventFuncHi = new double[eventNumber];
        eventFuncRout = new double[eventNumber];
        eventInfo = new int[eventNumber];
        eventDirections = new int[eventNumber];
        eventActive = new boolean[eventNumber];

        // Set default values for rootdir (both directions)
        for( int i = 0; i < eventNumber; i++ )
            eventDirections[i] = 1;

        // Set default values for gactive (all active)
        for( int i = 0; i < eventNumber; i++ )
            eventActive[i] = true;
    }


    /**
     * Sets scalar relative tolerance and scalar absolute tolerances
     */
    public void setTolerances(double reltol, double abstol)
    {
        setTolerances(reltol, VectorUtils.newVector(abstol, n));
    }

    /**
     * Sets scalar relative tolerance and vector of absolute tolerances
     */
    public void setTolerances(double reltol, double[] abstol)
    {
        // Check inputs
        if( reltol < 0 )
            throw new IllegalArgumentException("reltol < 0 illegal.");

        if( abstol.length != 0 && VectorUtils.getMin(abstol) < 0 )
            throw new IllegalArgumentException("abstol < 0 illegal.");

        atolVector = VectorUtils.copy(abstol);
        rtol = reltol;
        ewtFunc = new EwtSet();
    }


    /**
     * Sets user-provided tolerance function
     * @param efun
     */
    public void setTolerances(EwtFn efun)
    {
        ewtFunc = efun;
    }

    /**
     * This class is responsible for setting the error weight vector ewt, according to tol_type, as follows:<br>
     * ewt[i] = 1 / (reltol * ABS(ycur[i]) + *abstol), i=0,...<br>
     * EwtSet returns 0 if ewt is successfully set as above to a<br>
     * positive vector and -1 otherwise. In the latter case, ewt is considered undefined.<br>
     */
    public class EwtSet implements EwtFn
    {
        @Override
        public int getValue(double[] ycur, double[] weight)
        {
            VectorUtils.abs(ycur, weight);
            VectorUtils.linearSum(rtol, weight, atolVector, weight);
            if( VectorUtils.getMin(weight) <= 0 )
                return -1;
            VectorUtils.inv(weight, weight);
            return 0;
        }
    }

    /**
    * A function e, which sets the error weight vector ewt, must have type EwtFn.<br>
    * The function e takes as input the current dependent variable y.<br>
    * It must set the vector of error weights used in the WRMS norm:<br>
    * <br>
    *   ||y||_WRMS = sqrt [ 1/N * sum ( ewt_i * y_i)^2 ]<br>
    *<br>
    * Typically, the vector ewt has components:<br>
    * <br>
    *   ewt_i = 1 / (reltol * |y_i| + abstol_i)<br>
    */
    public static interface EwtFn
    {
        public int getValue(double[] y, double[] ewt);
    }

}
