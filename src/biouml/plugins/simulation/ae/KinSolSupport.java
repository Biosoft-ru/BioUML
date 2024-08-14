package biouml.plugins.simulation.ae;

import java.util.Arrays;
import java.util.HashMap;

import biouml.plugins.simulation.ae.AeModel;
import biouml.plugins.simulation.ode.jvode.VectorUtils;

public abstract class KinSolSupport
{

    /* KINSOL default constants */
    public final static int RETRY_ITERATION = -998;
    public final static int CONTINUE_ITERATIONS = -999;
    public final static int STEP_TOO_SMALL = -997;
    public final static int CONSTR_VIOLATED = -996;

    /*
     * Algorithmic constants
     * ---------------------
     * MAX_RECVR   max. no. of attempts to correct a recoverable func error
     */
    public final static int MAX_RECVR = 5;

    public final static int DEBAG_DEFAULT = 0;
    public final static int MXITER_DEFAULT = 200;
    public final static int MXNBCF_DEFAULT = 10;
    public final static int MSBSET_DEFAULT = 10;
    public final static int MSBSET_SUB_DEFAULT = 5;

    public final static double OMEGA_MIN = 0.00001;
    public final static double OMEGA_MAX = 0.9;

    public final static double UNIT_ROUNDOFF = 2.220446049250313e-016;

    public final static double FTOL_DEFAULT = Math.pow(UNIT_ROUNDOFF, 0.3333333333333333);
    public final static double STOL_DEFAULT = Math.pow(UNIT_ROUNDOFF, 0.6666666666666667);

    /*
     * -----------------------------------------------------------------
     * Enumeration for inputs to KINSetEtaForm (eta choice)
     * -----------------------------------------------------------------
     * ETACONSTANT : use constant value for eta (default value is
     *                   0.1 but a different value can be specified via
     *                   a call to KINSetEtaConstValue)
     *
     * ETACHOICE1 : use choice #1 as given in Eisenstat and Walker's
     *                  paper of SIAM J.Sci.Comput.,17 (1996), pp 16-32,
     *                  wherein eta is defined to be:
     *
     *              eta(k+1) = ABS(||F(u_k+1)||_L2-||F(u_k)+J(u_k)*p_k||_L2)
     *                       ---------------------------------------------
     *                                       ||F(u_k)||_L2
     *
     *                                                      1+sqrt(5)
     *              eta_safe = eta(k)^ealpha where ealpha = ---------
     *                                                          2
     *
     * ETACHOICE2 : use choice #2 as given in Eisenstat and Walker's
     *                  paper wherein eta is defined to be:
     *
     *                                  [ ||F(u_k+1)||_L2 ]^ealpha
     *              eta(k+1) = egamma * [ --------------- ]
     *                                  [  ||F(u_k)||_L2  ]
     *
     *                  where egamma = [0,1] and ealpha = (1,2]
     *
     *              eta_safe = egamma*(eta(k)^ealpha)
     *
     *                  Note: The default values of the scalar
     *                  coefficients egamma and ealpha (both required)
     *                  are egamma = 0.9 and ealpha = 2.0, but the
     *                  routine KINSetEtaParams can be used to specify
     *                  different values.
     *
     * When using either ETACHOICE1 or ETACHOICE2, if
     * eta_safe > 0.1 then the following safeguard is applied:
     *
     *  eta(k+1) = MAX {eta(k+1), eta_safe}
     *
     * The following safeguards are always applied when using either
     * ETACHOICE1 or ETACHOICE2 so that eta_min <= eta <= eta_max:
     *
     *  eta(k+1) = MAX {eta(k+1), eta_min}
     *  eta(k+1) = MIN {eta(k+1), eta_max}
     *
     * where eta_min = 1.0e-4 and eta_max = 0.9 (see KINForcingTerm).
     * -----------------------------------------------------------------
     */
    public final static int ETACHOICE1 = 1;
    public final static int ETACHOICE2 = 2;
    public final static int ETACONSTANT = 3;


    /* ----------------------------------
      Enumeration for global strategy
      Choices are NONE and LINESEARCH.
      ----------------------------------*/
    public static final int NONE = 0;
    public static final int LINESEARCH = 1;

    /* -----------------------------
      problem specification data
      ----------------------------*/
    protected AeModel func; // nonlinear system function implementation

    protected double fTolerance = FTOL_DEFAULT;// stopping tolerance on L2-norm of function   value
    protected double stepTolerance = STOL_DEFAULT; // scaled step length tolerance
    protected int strategy = NONE; // choices are NONE and LINESEARCH
    protected int debagLevel = DEBAG_DEFAULT; // level of verbosity of output
    protected int mxiter = MXITER_DEFAULT; // maximum number of nonlinear iterations
    protected int msbset = MSBSET_DEFAULT; // maximum number of nonlinear iterations that may be performed between calls to the linear solver setup routine (setup)
    protected int msbset_sub = MSBSET_SUB_DEFAULT; // subinterval length for residual monitoring
    protected int mxnbcf = MXNBCF_DEFAULT; // maximum number of beta condition failures
    protected int etaflag = ETACHOICE1; // choices are ETACONSTANT, ETACHOICE1 and ETACHOICE2
    protected boolean noMinEps = false; // flag controlling whether or not the value of eps is bounded below
    protected boolean setupNonNull = false; // flag indicating if linear solver setup routine is non-null and if setup is used
    protected boolean constraintsSet = false; // flag indicating if constraints are being used
    protected boolean jacCurrent; // flag indicating if the Jacobian info. used by the linear solver is current
    protected boolean callForcingTerm; // flag set if using either ETACHOICE1 or ETACHOICE2
    protected boolean noResMon = false; // flag indicating if the nonlinear residual monitoring scheme should be used
    protected boolean retryIteration; // flag indicating if nonlinear iteration should be retried (set by residual monitoring algorithm)
    protected boolean updateFnormSub;// flag indicating if the fnorm associated with the subinterval needs to be updated (set by residual monitoring algorithm)

    protected double mxnewtstep; // maximum allowable scaled step length
    protected double sqrtRelfunc = Math.sqrt(UNIT_ROUNDOFF); // relative error bound for func(u)
    protected double stepl; // scaled length of current step
    protected double stepmul; // step scaling factor
    protected double eps; // current value of eps
    protected double eta; // current value of eta
    protected double etaGamma; // gamma value used in eta calculation (choice #2)
    protected double etaAlpha; // alpha value used in eta calculation (choice #2)
    protected boolean noInitSetup = false; // flag controlling whether or not the main routine makes an initial call to the linear solver setup routine (setup)         */
    protected double sthrsh; // threshold value for calling the linear solver setup routine

    /* counters */
    protected long iterationsNumber; // number of nonlinear iterations
    protected long modelFunctionCallNumber; // number of calls made to func routine
    protected long nnilset; // iterationsNumber when the linear solver    setup was last called
    protected long nnilset_sub; // iterationsNumber when the linear solver setup was last called (subinterval)
    protected long nbcf; // number of times the beta-condition could not be met in lineSearch
    protected long backTrackNumber; // number of backtracks performed by lineSearch
    protected long conseqStepsOfMaxSize; // number of consecutive steps of size  mxnewtstep taken

    /* vectors */
    protected double[] u; // solution vector/current iterate
    protected double[] uDelta; // incremental change vector (uDelta = uNew-u)
    protected double[] uNew; // next iterate (uNew = u+pp)
    protected double[] fValue; // vector containing result of nonlinear system function evaluated at a given iterate (fValue = func(u))                               */
    protected double[] uscale; // iterate scaling vector
    protected double[] fscale; // fval scaling vector
    protected double[] constraints; // constraints vector

    protected boolean inexactLs; // flag set by the linear solver module (in init) indicating whether this is an terative linear solver (TRUE), or a direct linear solver (FALSE)

    protected double fnorm; // value of L2-norm of fscale*fval
    protected double f1norm; // f1norm = 0.5*(fnorm)^2
    protected double res_norm; // value of L2-norm of residual (set by the linear solver)
    protected double sfdotJp; // value of scaled func(u) vector (fscale*fval) dotted with scaled J(u)*pp vector
    protected double sJpnorm; // value of L2-norm of fscale*(J(u)*pp)

    protected double fnormSub; // value of L2-norm of fscale*fval (subinterval)
    protected boolean evalOmega = true; // flag indicating that omega must be evaluated.
    protected double omega; /* constant value for real scalar used in test to determine if reduction of norm of nonlinear
                            residual is sufficient. Unless a valid constant
                            value is specified by the user, omega is estimated
                            from omega_min and omega_max at each iteration. */
    protected double omega_min = OMEGA_MIN; // lower bound on omega
    protected double omega_max = OMEGA_MAX;// upper bound on omega                               */

    protected int n; //system dimension

    protected double fnormp = -1;
    protected double f1normp = -1;
    protected boolean maxStepTaken = false;

    public KinSolSupport(AeModel f, double[] initialGuess)
    {
        if( f == null )
            throw new IllegalArgumentException("Righ hand side function is null.");

        try
        {
            f.solveAlgebraic(initialGuess);
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException("Probably right hand side function and initial guess are not compatible.");
        }

        func = f;
        n = initialGuess.length;
        u = Arrays.copyOf(initialGuess, n);

        sthrsh = 2;
        mxnewtstep = 0;

        eta = 0.1; /* default for ETACONSTANT */
        etaAlpha = 2; /* default for ETACHOICE2  */
        etaGamma = 0.9; /* default for ETACHOICE2  */
        omega = 0; /* default to using min/max    */

        uNew = new double[n];
        fValue = new double[n];
        uDelta = new double[n];

        setFlagToMessageMap();
    }

    /*-----------------------------------------
      Getting of result Information
     ---------------------------------------*/

    public double[] getY()
    {
        return u;
    }

    public long getNonLinearIterationsNumber()
    {
        return iterationsNumber;
    }

    public long getRHSFunctionCallNumbers()
    {
        return modelFunctionCallNumber;
    }

    /*-----------------------------------------
      Setting of optional parameters
     ---------------------------------------*/

    public void setStol(double tolerance)
    {
        if( tolerance < 0 )
            throw new IllegalArgumentException("Step tolerance must not be negative");
        stepTolerance = tolerance;
    }

    public void setFtol(double tolerance)
    {
        if( tolerance < 0 )
            throw new IllegalArgumentException("Function tolerance must not be negative");
        fTolerance = tolerance;
    }

    public void setConstraints(double[] constraints)
    {
        if( constraints.length != n )
            throw new IllegalArgumentException("Constarints length must equals to right hand side dimension = " + n);
        for( int i = 0; i < n; i++ )
            if( constraints[i] != 1 && constraints[i] != 0 && constraints[i] != -1 )
                throw new IllegalArgumentException("Illegal values in constraints vector: " + constraints[i] + ". Must be: 1, 0 or -1");
        this.constraints = Arrays.copyOf(constraints, n);
        constraintsSet = true;
    }

    public void removeConstraints()
    {
        constraints = null;
        constraintsSet = false;
    }

    public void setIntitalGuess(double[] initialGuess)
    {
        if( initialGuess.length != n )
            throw new IllegalArgumentException("Initial guess length must equals to right hand side dimension = " + n);
        this.u = Arrays.copyOf(initialGuess, n);
    }

    public void setMaximumIterations(int maxIterations)
    {
        if( maxIterations <= 0 )
            throw new IllegalArgumentException("Maximum iterations illegally negative: " + maxIterations);
        this.mxiter = maxIterations;
    }

    public void setMaxSetups(int max)
    {
        if( max > 0 )
            this.msbset = max;
    }

    public void setEtaFlag(int etaflag)
    {
        switch( etaflag )
        {
            case ETACONSTANT:
                this.etaflag = ETACONSTANT;
                break;
            case ETACHOICE2:
                this.etaflag = ETACHOICE2;
                break;
            default:
                this.etaflag = ETACHOICE1;
        }
    }

    public void setStartegy(int strategy)
    {
        if( ( strategy != NONE ) && ( strategy != LINESEARCH ) )
            throw new IllegalArgumentException("Illegal value for global strategy. Must be KinSol.LINESEARHC or KinSol.NONE");
        this.strategy = strategy;
    }

    public void setUScale(double[] usacle)
    {
        if( usacle.length != n )
            throw new IllegalArgumentException("U scale array length length must equals to right hand side dimension = " + n);
        if( VectorUtils.getMin(uscale) <= 0 )
            throw new IllegalArgumentException("Uscale has nonpositive elements.");
        this.uscale = Arrays.copyOf(uscale, n);
    }

    public void setFScale(double[] fsacle)
    {
        if( fsacle.length != n )
            throw new IllegalArgumentException("F scale array length must equals to right hand side dimension = " + n);
        if( VectorUtils.getMin(uscale) <= 0 )
            throw new IllegalArgumentException("Uscale has nonpositive elements.");
        this.fscale = Arrays.copyOf(fscale, n);
    }


    /* -----------------------------------------
      Output flags and their meanings
      -----------------------------------------*/

    HashMap<Integer, String> flagToMessage;
    public void setFlagToMessageMap()
    {
        flagToMessage = new HashMap<>();
        flagToMessage.put(SUCCESS, "Problem solved successfully");
        flagToMessage.put(INITIAL_GUESS_OK, "Inital guess already satisfies stopping criterion");
        flagToMessage.put(STEP_LT_STPTOL, "The stopping tolerance on scaled step length was satisfied");
        flagToMessage.put(LINESEARCH_NONCONV,
                "The line search algorithm was unable to find an iterate sufficiently distinct from the current iterate.");
        flagToMessage.put(MAXITER_REACHED, "The maximum number of iterations was reached before convergence.");
        flagToMessage.put(MXNEWT_5X_EXCEEDED, "Five consecutive steps have been taken that satisfy a scaled step length test.");
        flagToMessage.put(LINESEARCH_BCFAIL, "The line search algorithm was unable to satisfy the beta-condition for nbcfails iterations.");
        flagToMessage.put(LINSOLV_NO_RECOVERY,
                "The linear solver's solve function failed recoverably, but the Jacobian data is already current.");
        flagToMessage.put(LINIT_FAIL, "The linear solver's init function failed in an unrecoverable manner.");
        flagToMessage.put(LSETUP_FAIL,
                "The linear solver's setup function failed in an unrecoverable manner. Probably jacobian is degenerate.");
        flagToMessage.put(LSOLVE_FAIL, "The linear solver's solve function failed in an unrecoverable manner.");
        flagToMessage.put(SYSFUNC_FAIL, "The system function failed in an unrecoverable manner.");
        flagToMessage.put(FIRST_SYSFUNC_ERR, "The system function failed at the first call.");
    }


    //SUCCESS FLAGS
    public final static int SUCCESS = 0;
    public final static int INITIAL_GUESS_OK = 1;
    public final static int STEP_LT_STPTOL = 2;

    //ERROR FLAGS
    public final static int LINESEARCH_NONCONV = -1;
    public final static int MAXITER_REACHED = -2;
    public final static int MXNEWT_5X_EXCEEDED = -3;
    public final static int LINESEARCH_BCFAIL = -4;
    public final static int LINSOLV_NO_RECOVERY = -5;
    public final static int LINIT_FAIL = -6;
    public final static int LSETUP_FAIL = -7;
    public final static int LSOLVE_FAIL = -8;
    public final static int SYSFUNC_FAIL = -9;
    public final static int FIRST_SYSFUNC_ERR = -10;
    public final static int REPTD_SYSFUNC_ERR = -11;


    /*------------------------------
      interface for linear solver
     ------------------------------*/
    protected int init()
    {
        return 0;
    }

    protected int solve(double[] x, double[] y)
    {
        return 0;
    }

    protected int setup()
    {
        return 0;
    }


    //Wrapper for right hand side function
    protected int func(double[] u, double[] f)
    {
        try
        {
            double[] result = func.solveAlgebraic(u);
            VectorUtils.copy(result, f);
        }
        catch( Exception ex )
        {
            return 1;
        }
        return 0;
    }
}
