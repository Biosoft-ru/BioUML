package biouml.plugins.simulation.ode.jvode;

import biouml.plugins.simulation.ode.OdeModel;

public abstract class IterativeSolver extends JVode
{

    /* Algorithmic constants */
    public static final int MAX_ITERS = 3; /* max. number oTf attempts to recover in DQ J*v */

    //Precondition types
    public final static int PREC_NONE = 0; //The iterative linear solver should not use preconditioning.
    public final static int PREC_LEFT = 1; //The iterative linear solver uses preconditioning on the left only.
    public final static int PREC_RIGHT = 2; //The iterative linear solver uses preconditioning on the right only.
    public final static int PREC_BOTH = 3; //The iterative linear solver uses preconditioning on both the left and the right.


    public final static int MODIFIED_GS = 1; //The iterative solver uses the modified Gram-Schmidt routine ModifiedGS listed in this file.
    public final static int CLASSICAL_GS = 2; //The iterative solver uses the classical Gram-Schmidt routine ClassicalGS listed in this file.

    /*
     * -----------------------------------------------------------------
     * CVSPILS return values
     * -----------------------------------------------------------------
     */
    public static final int CVSPILS_SUCCESS = 0;
    public static final int CVSPILS_MEM_NULL = -1;
    public static final int CVSPILS_LMEM_NULL = -2;
    public static final int CVSPILS_ILL_INPUT = -3;
    public static final int CVSPILS_MEM_FAIL = -4;
    public static final int CVSPILS_PMEM_NULL = -5;

    public static final int CVSPILS_MAXL = 5; //default value for the maximum Krylov dimension
    public static final int CVSPILS_MSBPRE = 50; //maximum number of steps between preconditioner evaluations
    public static final double CVSPILS_DGMAX = 0.2; //maximum change in gamma between prconditioner evaluations
    public static final double CVSPILS_EPLIN = 0.05; //default value for factor by which the tolerance on the nonlinear iteration ismultiplied to get a tolerance on the linear iteration

    /* Types of iterative linear solvers */
    public static final int SPILS_SPGMR = 1;
    public static final int SPILS_SPBCG = 2;
    public static final int SPILS_SPTFQMR = 3;

    /*
     * -----------------------------------------------------------------
     * Error Messages
     * -----------------------------------------------------------------
     */
    public static final String MSGS_CVMEM_NULL = "Integrator memory is NULL.";
    public static final String MSGS_MEM_FAIL = "A memory request failed.";
    public static final String MSGS_BAD_NVECTOR = "A required vector operation is not implemented.";
    public static final String MSGS_BAD_LSTYPE = "Incompatible linear solver type.";
    public static final String MSGS_BAD_PRETYPE = "Illegal value for pretype. Legal values are PREC_NONE, PREC_LEFT, PREC_RIGHT, and PREC_BOTH.";
    public static final String MSGS_PSOLVE_REQ = "pretype != PREC_NONE, but PSOLVE = NULL is illegal.";
    public static final String MSGS_LMEM_NULL = "Linear solver memory is NULL.";
    public static final String MSGS_BAD_GSTYPE = "Illegal value for gstype. Legal values are MODIFIED_GS and CLASSICAL_GS.";
    public static final String MSGS_BAD_EPLIN = "eplifac < 0 illegal.";

    public static final String MSGS_PSET_FAILED = "The preconditioner setup routine failed in an unrecoverable manner.";
    public static final String MSGS_PSOLVE_FAILED = "The preconditioner solve routine failed in an unrecoverable manner.";
    public static final String MSGS_JTIMES_FAILED = "The Jacobian x vector routine failed in an unrecoverable manner.";

    public IterativeSolver(Method method, OdeModel f, double[] u0, double t0)
    {
        super(method, f, u0, t0);
    }

    Precondition precondition;

    int s_type; /* type of scaled preconditioned iterative LS   */

    int s_pretype; /* type of preconditioning                      */
    int s_gstype; /* type of Gram-Schmidt orthogonalization       */
    double s_sqrtN; /* sqrt(N)                                      */
    double s_eplifac; /* eplifac = user specified or EPLIN_DEFAULT    */
    double s_deltar; /* deltar = delt * tq4                          */
    double s_delta; /* delta = deltar * sqrtN                       */
    int s_maxl; /* maxl = maximum dimension of the Krylov space */

    public int s_nstlpre; /* value of nst at the last pset call           */
    public int s_npe; /* npe = total number of pset calls             */
    public int s_nli; /* nli = total number of linear iterations      */
    public int s_nps; /* nps = total number of psolve calls           */
    public int s_ncfl; /* ncfl = total number of convergence failures  */
    public int s_njtimes; /* njtimes = total number of calls to jtimes    */
    public int s_nfes; /* nfeSG = total number of calls to f for
                        difference quotient Jacobian-vector products */

    double[] s_ytemp; /* temp vector passed to jtimes and psolve      */
    double[] s_x; /* temp vector used by CVSpilsSolve             */
    double[] s_ycur; /* CVODE current y vector in Newton Iteration   */
    double[] s_fcur; /* fcur = f(tn, ycur)                           */

    int lastFlag; /* last error flag returned by any function     */


    /*
     * -----------------------------------------------------------------
     * Optional inputs to the CVSPILS linear solver
     * -----------------------------------------------------------------
     *
     * CVSpilsSetPrecType resets the type of preconditioner, pretype,
     *                from the value previously set.
     *                This must be one of PREC_NONE, PREC_LEFT,
     *                PREC_RIGHT, or PREC_BOTH.
     *
     * CVSpilsSetGSType specifies the type of Gram-Schmidt
     *                orthogonalization to be used. This must be one of
     *                the two enumeration constants MODIFIED_GS or
     *                CLASSICAL_GS defined in iterative.h. These correspond
     *                to using modified Gram-Schmidt and classical
     *                Gram-Schmidt, respectively.
     *                Default value is MODIFIED_GS.
     *
     * CVSpilsSetMaxl resets the maximum Krylov subspace size, maxl,
     *                from the value previously set.
     *                An input value <= 0, gives the default value.
     *
     * CVSpilsSetEpsLin specifies the factor by which the tolerance on
     *                the nonlinear iteration is multiplied to get a
     *                tolerance on the linear iteration.
     *                Default value is 0.05.
     *
     * CVSpilsSetPreconditioner specifies the PrecSetup and PrecSolve functions.
     *                Default is NULL for both arguments (no preconditioning)
     *
     * CVSpilsSetJacTimesVecFn specifies the jtimes function. Default is to
     *                use an internal finite difference approximation routine.
     *
     * The return value of CVSpilsSet* is one of:
     *    CVSPILS_SUCCESS   if successful
     *    CVSPILS_MEM_NULL  if the cvode memory was NULL
     *    CVSPILS_LMEM_NULL if the linear solver memory was NULL
     *    CVSPILS_ILL_INPUT if an input has an illegal value
     * -----------------------------------------------------------------
     */
    void setPrecType(int pretype)
    {
        if( ( pretype != PREC_NONE ) && ( pretype != PREC_LEFT ) && ( pretype != PREC_RIGHT ) && ( pretype != PREC_BOTH ) )
            throw new IllegalArgumentException(MSGS_BAD_PRETYPE);
        s_pretype = pretype;
    }


    public void setGSType(int gstype)
    {
        if( ( gstype != MODIFIED_GS ) && ( gstype != CLASSICAL_GS ) )
            throw new IllegalArgumentException(MSGS_BAD_GSTYPE);
        s_gstype = gstype;
    }

    void setMaxl(int maxl) throws Exception
    {
        if( s_type == SPILS_SPGMR )
            throw new Exception(MSGS_BAD_LSTYPE);
        s_maxl = ( maxl <= 0 ) ? CVSPILS_MAXL : maxl;
    }

    void setEpsLin(double eplifac)
    {
        if( eplifac < 0 )
            throw new IllegalArgumentException(MSGS_BAD_EPLIN);
        s_eplifac = ( eplifac == 0.0 ) ? CVSPILS_EPLIN : eplifac;

    }
    
    public void setBandPreconditioner(int n, int mu, int ml) throws Exception
    {
        this.precondition = new BandPrecondition(n, mu, ml);
    }

    public void setPreconditioner(Precondition precondition)
    {
        this.precondition = precondition;
    }
    public Precondition getPrecondition()
    {
        return precondition;
    }


    int getLastFlag(Object cvode_mem)
    {
        return lastFlag;
    }


    /**
     * This routine generates the matrix-vector product z = Mv, where
     * M = I - gamma*J. The product J*v is obtained by calling the jtimes
     * routine. It is then scaled by -gamma and added to v to obtain M*v.
     * The return value is the same as the value returned by jtimes --
     * 0 if successful, nonzero otherwise.
     */
    public int atimes(double[] v, double[] z) throws Exception
    {

        double[] temp = jTimes(v, tn, s_ycur, s_fcur, s_ytemp);
        s_njtimes++;
        VectorUtils.linearSum( -gamma, temp, v, z);
        return 0;
    }


    /**
     * This routine interfaces between the generic SpgmrSolve routine and
     * the user's psolve routine.  It passes to psolve all required state
     * information from cvode_mem.  Its return value is the same as that
     * returned by psolve. Note that the generic SPGMR solver guarantees
     * that CVSpilsPSolve will not be called in the case in which
     * preconditioning is not done. This is the only case in which the
     * user's psolve routine is allowed to be NULL.
     */
    public int pSolve(double[] r, double[] z, int lr)
    {
        return precondition.solve(tn, s_ycur, s_fcur, r, z, gamma, s_delta, lr, s_ytemp);
    }

    /**
     * This routine generates a difference quotient approximation to
     * the Jacobian times vector f_y(t,y) * v. The approximation is
     * Jv = vnrm[f(y + v/vnrm) - f(y)], where vnrm = (WRMS norm of v) is
     * input, i.e. the WRMS norm of v/vnrm is 1.
     * -----------------------------------------------------------------
     */
    public double[] jTimes(double[] v, double t, double[] y, double[] fy, double[] work) throws Exception
    {
        /* data is cvode_mem */
        /* Initialize perturbation to 1/||v|| */
        double sig = 1.0 / VectorUtils.wrmsNorm(v, errorWeight);
        double[] z = new double[y.length];
        for( int iter = 0; iter < MAX_ITERS; iter++ )
        {
            /* Set work = y + sig*v */
            VectorUtils.linearSum(sig, v, y, work);
            /* Set Jv = f(tn, y+sig*v) */
            try
            {
                s_nfes++;
                z = this.f.dy_dt(t, work);
                break; //if success;
            }
            catch( IllegalArgumentException ex )
            {
                sig *= 0.25;
            }
        }

        /* Replace Jv by (Jv - fy)/sig */
        double siginv = 1.0 / sig;
        VectorUtils.scaleDiff(siginv, z, fy, z);
        return z;
    }

    public class BandPrecondition implements Precondition
    {
        public static final double MIN_INC_MULT = 1000.0;

        /* Data set by user in CVBandPrecInit */
        int N;
        int ml, mu;

        Matrix savedJ;
        Matrix savedP;
        int[] pivots;

        int nfeBP;

         /**
         * N is the problem size.
         * mu is the upper half bandwidth.
         * ml is the lower half bandwidth.
         *
         */
        public BandPrecondition(int N, int mu, int ml) throws Exception
        {
            int mup, mlp, storagemu;

            this.N = N;
            this.mu = mup = Math.min(N - 1, Math.max(0, mu));
            this.ml = mlp = Math.min(N - 1, Math.max(0, ml));

            /* Initialize nfeBP counter */
            nfeBP = 0;

            /* Allocate memory for saved banded Jacobian approximation. */
            savedJ = new Matrix(N, mup, mlp, mup);

            /* Allocate memory for banded preconditioner. */
            storagemu = Math.min(N - 1, mup + mlp);
            savedP = new Matrix(N, mup, mlp, storagemu);

            /* Allocate memory for pivot array. */
            pivots = new int[N];
        }

        /**
         * @return
         * the number of calls made from BanPrecondition to the user's right-hand side routine f.
         */
        public int getNumRhsEvals()
        {
            return nfeBP;
        }
        
        /**
         * Uses a banded
         * difference quotient Jacobian to create a preconditioner.
         * CVBandPrecSetup calculates a new J, if necessary, then
         * calculates P = I - gamma*J, and does an LU factorization of P.
         *
         * The parameters of CVBandPrecSetup are as follows:
         *
         * t       is the current value of the independent variable.
         *
         * y       is the current value of the dependent variable vector,
         *         namely the predicted value of y(t).
         *
         * fy      is the vector f(t,y).
         *
         * jok     is an input flag indicating whether Jacobian-related
         *         data needs to be recomputed, as follows:
         *           jok == FALSE means recompute Jacobian-related data
         *                  from scratch.
         *           jok == TRUE means that Jacobian data from the
         *                  previous PrecSetup call will be reused
         *                  (with the current value of gamma).
         *         A CVBandPrecSetup call with jok == TRUE should only
         *         occur after a call with jok == FALSE.
         *                       but saved data was reused.
         *
         * gamma   is the scalar appearing in the Newton matrix.
         * The value to be returned by the CVBandPrecSetup function is
         *   0  if successful, or
         *   1  if the band factorization failed.
         * -----------------------------------------------------------------
         */
        @Override
        public int setup(double t, double[] y, double[] fy, boolean jok, double gamma)
        {
            if( jok )
            {
                /* If jok = TRUE, use saved copy of J. */
                savedJ.bandCopy(savedP, mu, ml);
            }
            else
            {
                /* If jok = FALSE, call CVBandPDQJac for new J value. */
                savedJ.setToZero();
                try
                {
                    BandPDQJac(t, y, fy);
                }
                catch( IllegalArgumentException ex )
                {
                    return 1;
                }
                catch( Exception ex )
                {
                    return -1;
                }
                savedJ.bandCopy(savedP, mu, ml);

            }

            /* Scale and add I to get savedP = I - gamma*J. */
            savedP.scale( -gamma);
            savedP.addIdentity();

            /* Do LU factorization of matrix. */
            int retval = MatrixUtils.BandGBTRF(savedP, pivots);

            /* Return 0 if the LU was complete; otherwise return 1. */
            if( retval > 0 )
                return 1;
            return 0;
        }

        /**
         * CVBandPrecSolve solves a linear system P z = r, where P is the
         * matrix computed by CVBandPrecond.
         *
         * The parameters of CVBandPrecSolve used here are as follows:
         *
         * r is the right-hand side vector of the linear system.
         * z is the output vector computed by CVBandPrecSolve.
         *
         * The value returned by the CVBandPrecSolve function is always 0,
         * indicating success.
         */
        @Override
        public int solve(double t, double[] y, double[] fy, double[] r, double[] z, double gamma, double delta, int lr, double[] tmp)
        {
            VectorUtils.copy(r, z);
            /* Do band backsolve on the vector z. */
            MatrixUtils.BandGBTRS(savedP, pivots, z);

            return 0;
        }



        /**
         * This routine generates a banded difference quotient approximation to
         * the Jacobian of f(t,y). It assumes that a band matrix of type
         * DlsMat is stored column-wise, and that elements within each column
         * are contiguous. This makes it possible to get the address of a column
         * of J via the macro BAND_COL and to write a simple for loop to set
         * each of the elements of a column in succession.
         * -----------------------------------------------------------------
         */
        void BandPDQJac(double t, double[] y, double[] fy) throws Exception
        {
            double inc, inc_inv;

            /* Load ytemp with y = predicted y vector. */
            double[] ytemp = new double[y.length];
            VectorUtils.copy(y, ytemp);
            /* Set minimum increment based on uround and norm of f. */
//            double srur = Math.sqrt(UROUND);
            double fnorm = VectorUtils.wrmsNorm(fy, errorWeight);
            double minInc = ( fnorm != 1.0 ) ? ( MIN_INC_MULT * Math.abs(h) * UROUND * N * fnorm ) : 1.0;

            /* Set bandwidth and number of column groups for band differencing. */
            int width = ml + mu + 1;
            int ngroups = Math.min(width, N);

            for( int group = 1; group <= ngroups; group++ )
            {
                /* Increment all y_j in group. */
                for( int j = group - 1; j < N; j += width )
                {
                    inc = Math.max(UROUND_SQRT * Math.abs(y[j]), minInc / errorWeight[j]);
                    ytemp[j] += inc;
                }

                /* Evaluate f with incremented y. */
                temp = f.dy_dt(t, ytemp);

                nfeBP++;

                /* Restore ytemp, then form and load difference quotients. */
                for( int j = group - 1; j < N; j += width )
                {
                    ytemp[j] = y[j];
                    inc = Math.max(UROUND_SQRT * Math.abs(y[j]), minInc / errorWeight[j]);
                    inc_inv = 1.0 / inc;
                    int i1 = Math.max(0, j - mu);
                    int i2 = Math.min(j + ml, N - 1);
                    for( int i = i1; i <= i2; i++ )
                    {
                        savedJ.setBandElement(i, j, inc_inv * ( temp[i] - fy[i] ));
                    }
                }
            }
            return;
        }
    }
    
    public static interface Precondition
    {
        /**
         * Setup calculates a new J, if necessary, then
         * calculates P = I - gamma*J, and does an LU factorization of P.
         *
         * The parameters of CVBandPrecSetup are as follows:
         *
         * t       is the current value of the independent variable.
         *
         * y       is the current value of the dependent variable vector,
         *         namely the predicted value of y(t).
         *
         * fy      is the vector f(t,y).
         *
         * jok     is an input flag indicating whether Jacobian-related
         *         data needs to be recomputed, as follows:
         *           jok == FALSE means recompute Jacobian-related data
         *                  from scratch.
         *           jok == TRUE means that Jacobian data from the
         *                  previous PrecSetup call will be reused
         *                  (with the current value of gamma).
         *         A CVBandPrecSetup call with jok == TRUE should only
         *         occur after a call with jok == FALSE.
         *
         *
         * gamma   is the scalar appearing in the Newton matrix.
         *
         * The value to be returned by the CVBandPrecSetup function is
         *   0  if successful, or
         *   1  if the band factorization failed.
         * -----------------------------------------------------------------
         */
        public int setup(double t, double[] y, double[] fy, boolean jok, double gamma);


        /**solves the preconditioner equation Pz = r for the
        * vector z. The caller is responsible for allocating memory for
        * the z vector. The parameter P_data is a pointer to any
        * information about P which the function needs in order to do
        * its job. The parameter lr is input, and indicates whether P
        * is to be taken as the left preconditioner or the right
        * preconditioner: lr = 1 for left and lr = 2 for right.
        * If preconditioning is on one side only, lr can be ignored.
        * The vector r is unchanged.
        * A PSolveFn returns 0 if successful and a non-zero value if
        * unsuccessful.  On a failure, a negative return value indicates
        * an unrecoverable condition, while a positive value indicates
        * a recoverable one, in which the calling routine may reattempt
        * the solution after updating preconditioner data.
        */
        public int solve(double t, double[] y, double[] fy, double[] r, double[] z, double gamma, double delta, int lr, double[] tmp);

    }

}