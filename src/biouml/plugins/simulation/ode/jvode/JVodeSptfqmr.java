package biouml.plugins.simulation.ode.jvode;

import java.util.logging.Level;
import java.util.Arrays;

import biouml.plugins.simulation.ode.OdeModel;


public class JVodeSptfqmr extends IterativeSolver
{

    public static final int SPTFQMR_SUCCESS = 0; /* SPTFQMR algorithm converged          */
    public static final int SPTFQMR_RES_REDUCED = 1; /* SPTFQMR did NOT converge, but the
                                                     residual was reduced                 */
    public static final int SPTFQMR_CONV_FAIL = 2; /* SPTFQMR algorithm failed to converge */
    public static final int SPTFQMR_PSOLVE_FAIL_REC = 3; /* psolve failed recoverably            */
    public static final int SPTFQMR_ATIMES_FAIL_REC = 4; /* atimes failed recoverably            */
    public static final int SPTFQMR_PSET_FAIL_REC = 5; /* pset faild recoverably               */

    public static final int SPTFQMR_MEM_NULL = -1; /* mem argument is NULL                 */
    public static final int SPTFQMR_ATIMES_FAIL_UNREC = -2; /* atimes returned failure flag         */
    public static final int SPTFQMR_PSOLVE_FAIL_UNREC = -3; /* psolve failed unrecoverably          */
    public static final int SPTFQMR_PSET_FAIL_UNREC = -4; /* pset failed unrecoverably            */

    public int l_max;
    double[] r_star;
    double[] q;
    double[] d;
    double[] v;
    double[] p;
    double[][] r;
    double[] u;
    double[] vtemp1;
    double[] vtemp2;
    double[] vtemp3;


    /*
     * -----------------------------------------------------------------
     * Function : CVSptfqmr
     * -----------------------------------------------------------------
     * A call to the CVSptfqmr function links the main CVODE integrator
     * with the CVSPTFQMR linear solver.
     *
     * cvode_mem is the pointer to the integrator memory returned by
     *           CVodeCreate.
     *
     * pretype   is the type of user preconditioning to be done.
     *           This must be one of the four enumeration constants
     *           PREC_NONE, PREC_LEFT, PREC_RIGHT, or PREC_BOTH defined
     *           in iterative.h. These correspond to no preconditioning,
     *           left preconditioning only, right preconditioning
     *           only, and both left and right preconditioning,
     *           respectively.
     *
     * maxl      is the maximum Krylov dimension. This is an
     *           optional input to the CVSPTFQMR solver. Pass 0 to
     *           use the default value CVSPILS_MAXL=5.
     *
     * The return value of CVSptfqmr is one of:
     *    CVSPILS_SUCCESS   if successful
     *    CVSPILS_MEM_NULL  if the cvode memory was NULL
     *    CVSPILS_MEM_FAIL  if there was a memory allocation failure
     *    CVSPILS_ILL_INPUT if a required vector operation is missing
     * The above constants are defined in cvode_spils.h
     *
     * -----------------------------------------------------------------
     */
    public JVodeSptfqmr(Method method, OdeModel f, double[] u0, double t0, int pretype, int maxl) throws Exception
    {
        super(method, f, u0, t0);
//        iterationType = IterationType.SPTFQMR;
        s_type = SPILS_SPTFQMR;

        /* Set Sptfqmr parameters that have been passed in call sequence */
        s_pretype = pretype;
        l_max = s_maxl = ( maxl <= 0 ) ? CVSPILS_MAXL : maxl;

        /* Set default values for the rest of the Sptfqmr parameters */
        s_eplifac = CVSPILS_EPLIN;

        lastFlag = CVSPILS_SUCCESS;

        setupNonNull = false;

        /* Check for legal pretype */
        if( ( pretype != PREC_NONE ) && ( pretype != PREC_LEFT ) && ( pretype != PREC_RIGHT ) && ( pretype != PREC_BOTH ) )
        {
            processError(MSGS_BAD_PRETYPE);
            //return (ILL_INPUT );
        }

        s_ytemp = new double[n];
        s_x = new double[n];

        /* Compute sqrtN from a dot product */
        Arrays.fill(s_ytemp, 1);
        s_sqrtN = Math.sqrt(VectorUtils.dotProd(s_ytemp, s_ytemp));


        r_star = new double[n];
        q = new double[n];
        d = new double[n];
        v = new double[n];
        p = new double[n];
        r = new double[2][n];
        u = new double[n];
        vtemp1 = new double[n];
        vtemp2 = new double[n];
        vtemp3 = new double[n];
    }

    /*
     * -----------------------------------------------------------------
     * Function : CVSptfqmrInit
     * -----------------------------------------------------------------
     * This routine does remaining initializations specific to the Sptfqmr
     * linear solver.
     * -----------------------------------------------------------------
     */

    @Override
    public int init()
    {

        /* Initialize counters */
        s_npe = s_nli = s_nps = s_ncfl = s_nstlpre = 0;
        s_njtimes = s_nfes = 0;


        /* Set setupNonNull = TRUE iff there is preconditioning
           (pretype != PREC_NONE)  and there is a preconditioning
           setup phase (pset != NULL) */
        setupNonNull = ( s_pretype != PREC_NONE ) && ( precondition != null );

        /*  Set maxl in the SPTFQMR memory in case it was changed by the user */
        l_max = s_maxl;

        lastFlag = CVSPILS_SUCCESS;
        return ( 0 );
    }

    /*
     * -----------------------------------------------------------------
     * Function : CVSptfqmrSetup
     * -----------------------------------------------------------------
     * This routine does the setup operations for the Sptfqmr linear solver.
     * It makes a decision as to whether or not to signal for reevaluation
     * of Jacobian data in the pset routine, based on various state
     * variables, then it calls pset. If we signal for reevaluation,
     * then we reset jcur = *jcurPtr to TRUE, regardless of the pset output.
     * In any case, if jcur == TRUE, we increment npe and save nst in nstlpre.
     * -----------------------------------------------------------------
     */

    @Override
    public int setup(int convfail)
    {
        /* Use nst, gamma/gammap, and convfail to set J eval. flag jok */
        double dgamma = Math.abs( ( gamma / gammaPrev ) - 1);
        boolean jbad = ( nSteps == 0 ) || ( nSteps > s_nstlpre + CVSPILS_MSBPRE )
                || ( ( convfail == FAIL_BAD_J ) && ( dgamma < CVSPILS_DGMAX ) ) || ( convfail == FAIL_OTHER );
        currentJacobian = jbad;
        boolean jok = !jbad;

        /* Call pset routine and possibly reset jcur */
        int retval = precondition.setup(tn, z[0], ftemp, jok, gamma);
        if( retval < 0 )
        {
            lastFlag = SPTFQMR_PSET_FAIL_UNREC;
        }
        if( retval > 0 )
        {
            lastFlag = SPTFQMR_PSET_FAIL_REC;
        }

        currentJacobian = jbad;

        /* If jcur = TRUE, increment npe and save nst value */
        if( currentJacobian )
        {
            s_npe++;
            s_nstlpre = nSteps;
        }

        lastFlag = SPTFQMR_SUCCESS;

        /* Return the same value that pset returned */
        return ( retval );
    }

    /*
     * -----------------------------------------------------------------
     * Function : CVSptfqmrSolve
     * -----------------------------------------------------------------
     * This routine handles the call to the generic solver SptfqmrSolve
     * for the solution of the linear system Ax = b with the SPTFQMR method.
     * The solution x is returned in the vector b.
     *
     * If the WRMS norm of b is small, we return x = b (if this is the first
     * Newton iteration) or x = 0 (if a later Newton iteration).
     *
     * Otherwise, we set the tolerance parameter and initial guess (x = 0),
     * call SptfqmrSolve, and copy the solution x into b. The x-scaling and
     * b-scaling arrays are both equal to weight.
     *
     * The counters nli, nps, and ncfl are incremented, and the return value
     * is set according to the success of SptfqmrSolve. The success flag is
     * returned if SptfqmrSolve converged, or if this is the first Newton
     * iteration and the residual norm was reduced below its initial value.
     * -----------------------------------------------------------------
     */

    @Override
    public int solve(double[] b)
    {

        /* Test norm(b); if small, return x = 0 or x = b */
        s_deltar = s_eplifac * tq[4];

        double bnorm = VectorUtils.wrmsNorm(b, errorWeight);
        if( bnorm <= s_deltar )
        {
            if( mNewt > 0 )
                Arrays.fill(b, 0);
            return ( 0 );
        }

        /* Set vectors ycur and fcur for use by the Atimes and Psolve routines */
        s_ycur = y;
        s_fcur = ftemp;

        /* Set inputs delta and initial guess x = 0 to SptfqmrSolve */
        s_delta = s_deltar * s_sqrtN;
        Arrays.fill(s_x, 0);


        /* Call SptfqmrSolve and copy x to b */
        int retval = SptfqmrSolve(s_x, b, s_pretype, s_delta, errorWeight, errorWeight);

        VectorUtils.copy(s_x, b);

        if( retval != SPTFQMR_SUCCESS )
            s_ncfl++;

        /* Interpret return value from SpgmrSolve */

        lastFlag = retval;

        switch( retval )
        {
            case SPTFQMR_SUCCESS:
                return 0;
            case SPTFQMR_RES_REDUCED:
                if( mNewt == 0 )
                    return 0;
                else
                    return 1;
            case SPTFQMR_CONV_FAIL:
            case SPTFQMR_PSOLVE_FAIL_REC:
            case SPTFQMR_ATIMES_FAIL_REC:
                return 1;
            case SPTFQMR_MEM_NULL:
                return -1;
            case SPTFQMR_ATIMES_FAIL_UNREC:
                ///CVode.processError(cv_mem, sundials_sptfqmr.SPTFQMR_ATIMES_FAIL_UNREC, "CVSPTFQMR", "CVSptfqmrSolve",
                ///        cvode_spils.MSGS_JTIMES_FAILED);
                return -1;
            case SPTFQMR_PSOLVE_FAIL_UNREC:
                /// CVode.processError(cv_mem, sundials_sptfqmr.SPTFQMR_PSOLVE_FAIL_UNREC, "CVSPTFQMR", "CVSptfqmrSolve",
                //        cvode_spils.MSGS_PSOLVE_FAILED);
                return -1;
        }

        return 0;
    }


    /*
     * -----------------------------------------------------------------
     * Function : SptfqmrSolve
     * -----------------------------------------------------------------
     * SptfqmrSolve solves the linear system Ax = b by means of a scaled
     * preconditioned Transpose-Free Quasi-Minimal Residual (SPTFQMR)
     * method.
     *
     *  mem  pointer to an internal memory block allocated during a
     *       prior call to SptfqmrMalloc
     *
     *  A_data  pointer to a data structure containing information
     *          about the coefficient matrix A (passed to user-supplied
     *          function referenced by atimes (function pointer))
     *
     *  x  vector (type N_Vector) containing initial guess x_0 upon
     *     entry, but which upon return contains an approximate solution
     *     of the linear system Ax = b (solution only valid if return
     *     value is either SPTFQMR_SUCCESS or SPTFQMR_RES_REDUCED)
     *
     *  b  vector (type N_Vector) set to the right-hand side vector b
     *     of the linear system (undisturbed by function)
     *
     *  pretype  variable (type int) indicating the type of
     *           preconditioning to be used (see sundials_iterative.h)
     *
     *  delta  tolerance on the L2 norm of the scaled, preconditioned
     *         residual (if return value == SPTFQMR_SUCCESS, then
     *         ||sb*P1_inv*(b-Ax)||_L2 <= delta)
     *
     *  P_data  pointer to a data structure containing preconditioner
     *          information (passed to user-supplied function referenced
     *          by psolve (function pointer))
     *
     *  sx  vector (type N_Vector) containing positive scaling factors
     *      for x (pass sx == NULL if scaling NOT required)
     *
     *  sb  vector (type N_Vector) containing positive scaling factors
     *      for b (pass sb == NULL if scaling NOT required)
     *
     *  atimes  user-supplied routine responsible for computing the
     *          matrix-vector product Ax (see sundials_iterative.h)
     *
     *  psolve  user-supplied routine responsible for solving the
     *          preconditioned linear system Pz = r (ignored if
     *          pretype == PREC_NONE) (see sundials_iterative.h)
     *
     *  res_norm  pointer (type realtype*) to the L2 norm of the
     *            scaled, preconditioned residual (if return value
     *            is either SPTFQMR_SUCCESS or SPTFQMR_RES_REDUCED, then
     *            *res_norm = ||sb*P1_inv*(b-Ax)||_L2, where x is
     *            the computed approximate solution, sb is the diagonal
     *            scaling matrix for the right-hand side b, and P1_inv
     *            is the inverse of the left-preconditioner matrix)
     *
     *  nli  pointer (type int*) to the total number of linear
     *       iterations performed
     *
     *  nps  pointer (type int*) to the total number of calls made
     *       to the psolve routine
     * -----------------------------------------------------------------
     */
    public int SptfqmrSolve(double[] x, double[] b, int pretype, double delta, double[] sx, double[] sb/*, Counters cnt*/)
    {
        try
        {
            double alpha, tau, eta, beta, c, sigma, v_bar, omega;
            double[] rho = new double[2];
            double r_init_norm, r_curr_norm;
            int n, m, ier;

            double temp_val = r_curr_norm = -1; /* Initialize to avoid compiler warnings */

            boolean converged = false; /* Initialize convergence flag */
            boolean b_ok = false;

            if( ( pretype != PREC_LEFT ) && ( pretype != PREC_RIGHT ) && ( pretype != PREC_BOTH ) )
                pretype = PREC_NONE;

            boolean preOnLeft = ( ( pretype == PREC_BOTH ) || ( pretype == PREC_LEFT ) );
            boolean preOnRight = ( ( pretype == PREC_BOTH ) || ( pretype == PREC_RIGHT ) );

            boolean scale_x = ( sx != null );
            boolean scale_b = ( sb != null );

            /* Set r_star to initial (unscaled) residual r_star = r_0 = b - A*x_0 */
            /* NOTE: if x == 0 then just set residual to b and continue */
            if( VectorUtils.dotProd(x, x) == 0 )
                VectorUtils.copy(b, r_star);
            else
            {
                ier = atimes(x, r_star);
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPTFQMR_ATIMES_FAIL_UNREC : SPTFQMR_ATIMES_FAIL_REC );
                VectorUtils.linearDiff(b, r_star, r_star);
            }

            /* Apply left preconditioner and b-scaling to r_star (or really just r_0) */
            if( preOnLeft )
            {
                ier = pSolve(r_star, vtemp1, PREC_LEFT);
                s_nps++;
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
            }
            else
                VectorUtils.copy(r_star, vtemp1);
            if( scale_b )
                VectorUtils.prod(sb, vtemp1, r_star);
            else
                VectorUtils.copy(vtemp1, r_star);

            /* Initialize rho[0] */
            /* NOTE: initialized here to reduce number of computations - avoid need
                     to compute r_star^T*r_star twice, and avoid needlessly squaring
                     values */
            rho[0] = VectorUtils.dotProd(r_star, r_star);

            /* Compute norm of initial residual (r_0) to see if we really need
               to do anything */
            /*cnt.res_norm = */r_init_norm = Math.sqrt(rho[0]);
            if( r_init_norm <= delta )
                return ( SPTFQMR_SUCCESS );

            /* Set v_ = A*r_0 (preconditioned and scaled) */
            if( scale_x )
                VectorUtils.divide(r_star, sx, vtemp1);
            else
                VectorUtils.copy(r_star, vtemp1);
            if( preOnRight )
            {
                VectorUtils.copy(vtemp1, v);
                ier = pSolve(v, vtemp1, PREC_RIGHT);
                s_nps++;
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
            }
            ier = atimes(vtemp1, v);
            if( ier != 0 )
                return ( ( ier < 0 ) ? SPTFQMR_ATIMES_FAIL_UNREC : SPTFQMR_ATIMES_FAIL_REC );
            if( preOnLeft )
            {
                ier = pSolve(v, vtemp1, PREC_LEFT);
                s_nps++;
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
            }
            else
                VectorUtils.copy(v, vtemp1);
            if( scale_b )
                VectorUtils.prod(sb, vtemp1, v);
            else
                VectorUtils.copy(vtemp1, v);

            /* Initialize remaining variables */
            VectorUtils.copy(r_star, r[0]);
            VectorUtils.copy(r_star, u);
            VectorUtils.copy(r_star, p);
            Arrays.fill(d, 0);

            tau = r_init_norm;
            v_bar = eta = 0;

            /* START outer loop */
            for( n = 0; n < l_max; ++n )
            {

                /* Increment linear iteration counter */
                s_nli++;

                /* sigma = r_star^T*v_ */
                sigma = VectorUtils.dotProd(r_star, v);

                /* alpha = rho[0]/sigma */
                alpha = rho[0] / sigma;

                /* q_ = u_-alpha*v_ */
                VectorUtils.linearSum( -alpha, v, u, q);

                /* r_[1] = r_[0]-alpha*A*(u_+q_) */
                VectorUtils.linearSum(u, q, r[1]);
                if( scale_x )
                    VectorUtils.divide(r[1], sx, r[1]);
                if( preOnRight )
                {
                    VectorUtils.copy(r[1], vtemp1);
                    ier = pSolve(vtemp1, r[1], PREC_RIGHT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
                }
                ier = atimes(r[1], vtemp1);
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPTFQMR_ATIMES_FAIL_UNREC : SPTFQMR_ATIMES_FAIL_REC );
                if( preOnLeft )
                {
                    ier = pSolve(vtemp1, r[1], PREC_LEFT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
                }
                else
                    VectorUtils.copy(vtemp1, r[1]);
                if( scale_b )
                    VectorUtils.prod(sb, r[1], vtemp1);
                else
                    VectorUtils.copy(r[1], vtemp1);
                VectorUtils.linearSum( -alpha, vtemp1, r[0], r[1]);

                /* START inner loop */
                for( m = 0; m < 2; ++m )
                {

                    /* d_ = [*]+(v_bar^2*eta/alpha)*d_ */
                    /* NOTES:
                     *   (1) [*] = u_ if m == 0, and q_ if m == 1
                     *   (2) using temp_val reduces the number of required computations
                     *       if the inner loop is executed twice
                     */
                    if( m == 0 )
                    {
                        temp_val = Math.sqrt(VectorUtils.dotProd(r[1], r[1]));
                        omega = Math.sqrt(Math.sqrt(VectorUtils.dotProd(r[0], r[0])) * temp_val);
                        VectorUtils.linearSum(v_bar * v_bar * eta / alpha, d, u, d);
                    }
                    else
                    {
                        omega = temp_val;
                        VectorUtils.linearSum(v_bar * v_bar * eta / alpha, d, q, d);
                    }

                    /* v_bar = omega/tau */
                    v_bar = omega / tau;

                    /* c = (1+v_bar^2)^(-1/2) */
                    c = 1.0 / Math.sqrt(1 + v_bar * v_bar);

                    /* tau = tau*v_bar*c */
                    tau = tau * v_bar * c;

                    /* eta = c^2*alpha */
                    eta = c * c * alpha;

                    /* x = x+eta*d_ */
                    VectorUtils.linearSum(eta, d, x);

                    /* Check for convergence... */
                    /* NOTE: just use approximation to norm of residual, if possible */
                    r_curr_norm = tau * Math.sqrt(m + 1);

                    /* Exit inner loop if iteration has converged based upon approximation
                    to norm of current residual */
                    if( r_curr_norm <= delta )
                    {
                        converged = true;
                        break;
                    }

                    /* Decide if actual norm of residual vector should be computed */
                    /* NOTES:
                     *   (1) if r_curr_norm > delta, then check if actual residual norm
                     *       is OK (recall we first compute an approximation)
                     *   (2) if r_curr_norm >= r_init_norm and m == 1 and n == l_max, then
                     *       compute actual residual norm to see if the iteration can be
                     *       saved
                     *   (3) the scaled and preconditioned right-hand side of the given
                     *       linear system (denoted by b) is only computed once, and the
                     *       result is stored in vtemp3 so it can be reused - reduces the
                     *       number of psovles if using left preconditioning
                     */
                    if( ( r_curr_norm > delta ) || ( r_curr_norm >= r_init_norm && m == 1 && n == l_max ) )
                    {

                        /* Compute norm of residual ||b-A*x||_2 (preconditioned and scaled) */
                        if( scale_x )
                            VectorUtils.divide(x, sx, vtemp1);
                        else
                            VectorUtils.copy(x, vtemp1);
                        if( preOnRight )
                        {
                            ier = pSolve(vtemp1, vtemp2, PREC_RIGHT);
                            s_nps++;
                            if( ier != 0 )
                                return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
                            VectorUtils.copy(vtemp2, vtemp1);
                        }
                        ier = atimes(vtemp1, vtemp2);
                        if( ier != 0 )
                            return ( ( ier < 0 ) ? SPTFQMR_ATIMES_FAIL_UNREC : SPTFQMR_ATIMES_FAIL_REC );
                        if( preOnLeft )
                        {
                            ier = pSolve(vtemp2, vtemp1, PREC_LEFT);
                            s_nps++;
                            if( ier != 0 )
                                return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
                        }
                        else
                            VectorUtils.copy(vtemp2, vtemp1);
                        if( scale_b )
                            VectorUtils.prod(sb, vtemp1, vtemp2);
                        else
                            VectorUtils.copy(vtemp1, vtemp2);
                        /* Only precondition and scale b once (result saved for reuse) */
                        if( !b_ok )
                        {
                            b_ok = true;
                            if( preOnLeft )
                            {
                                ier = pSolve(b, vtemp3, PREC_LEFT);
                                s_nps++;
                                if( ier != 0 )
                                    return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
                            }
                            else
                                VectorUtils.copy(b, vtemp3);
                            if( scale_b )
                                VectorUtils.prod(sb, vtemp3, vtemp3);
                        }
                        VectorUtils.linearDiff(vtemp3, vtemp2, vtemp1);
                        /*cnt.res_norm = */r_curr_norm = Math.sqrt(VectorUtils.dotProd(vtemp1, vtemp1));

                        /* Exit inner loop if inequality condition is satisfied
                           (meaning exit if we have converged) */
                        if( r_curr_norm <= delta )
                        {
                            converged = true;
                            break;
                        }

                    }

                } /* END inner loop */

                /* If converged, then exit outer loop as well */
                if( converged == true )
                    break;

                /* rho[1] = r_star^T*r_[1] */
                rho[1] = VectorUtils.dotProd(r_star, r[1]);

                /* beta = rho[1]/rho[0] */
                beta = rho[1] / rho[0];

                /* u_ = r_[1]+beta*q_ */
                VectorUtils.linearSum(beta, q, r[1], u);

                /* p_ = u_+beta*(q_+beta*p_) */
                VectorUtils.linearSum(beta, q, beta * beta, p, p);
                VectorUtils.linearSum(u, p, p);

                /* v_ = A*p_ */
                if( scale_x )
                    VectorUtils.divide(p, sx, vtemp1);
                else
                    VectorUtils.copy(p, vtemp1);
                if( preOnRight )
                {
                    VectorUtils.copy(vtemp1, v);
                    ier = pSolve(v, vtemp1, PREC_RIGHT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
                }
                ier = atimes(vtemp1, v);
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPTFQMR_ATIMES_FAIL_UNREC : SPTFQMR_ATIMES_FAIL_REC );
                if( preOnLeft )
                {
                    ier = pSolve(v, vtemp1, PREC_LEFT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
                }
                else
                    VectorUtils.copy(v, vtemp1);
                if( scale_b )
                    VectorUtils.prod(sb, vtemp1, v);
                else
                    VectorUtils.copy(vtemp1, v);

                /* Shift variable values */
                /* NOTE: reduces storage requirements */
                VectorUtils.copy(r[1], r[0]);
                rho[0] = rho[1];

            } /* END outer loop */

            /* Determine return value */
            /* If iteration converged or residual was reduced, then return current iterate (x) */
            if( ( converged == true ) || ( r_curr_norm < r_init_norm ) )
            {
                if( scale_x )
                    VectorUtils.divide(x, sx, x);
                if( preOnRight )
                {
                    ier = pSolve(x, vtemp1, PREC_RIGHT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPTFQMR_PSOLVE_FAIL_UNREC : SPTFQMR_PSOLVE_FAIL_REC );
                    VectorUtils.copy(vtemp1, x);
                }
                if( converged == true )
                    return ( SPTFQMR_SUCCESS );
                else
                    return ( SPTFQMR_RES_REDUCED );
            }
            /* Otherwise, return error code */
            else
                return ( SPTFQMR_CONV_FAIL );
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage());
            return SPTFQMR_CONV_FAIL;
        }
    }



}
