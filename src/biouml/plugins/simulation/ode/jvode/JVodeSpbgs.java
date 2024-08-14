package biouml.plugins.simulation.ode.jvode;

import java.util.logging.Level;
import java.util.Arrays;

import biouml.plugins.simulation.ode.OdeModel;

public class JVodeSpbgs extends IterativeSolver
{
    public static final int SPBCG_SUCCESS = 0; /* SPBCG algorithm converged          */
    public static final int SPBCG_RES_REDUCED = 1; /* SPBCG did NOT converge, but the
                                                   residual was reduced               */
    public static final int SPBCG_CONV_FAIL = 2; /* SPBCG algorithm failed to converge */
    public static final int SPBCG_PSOLVE_FAIL_REC = 3; /* psolve failed recoverably          */
    public static final int SPBCG_ATIMES_FAIL_REC = 4; /* atimes failed recoverably          */
    public static final int SPBCG_PSET_FAIL_REC = 5; /* pset faild recoverably             */

    public static final int SPBCG_MEM_NULL = -1; /* mem argument is NULL               */
    public static final int SPBCG_ATIMES_FAIL_UNREC = -2; /* atimes returned failure flag       */
    public static final int SPBCG_PSOLVE_FAIL_UNREC = -3; /* psolve failed unrecoverably        */
    public static final int SPBCG_PSET_FAIL_UNREC = -4; /* pset failed unrecoverably          */


    /*
     * -----------------------------------------------------------------
     * Function : CVSpbcg
     * -----------------------------------------------------------------
     * A call to the CVSpbcg function links the main CVODE integrator
     * with the CVSPBCG linear solver.
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
     *           optional input to the CVSPBCG solver. Pass 0 to
     *           use the default value CVSPBCG_MAXL=5.
     *
     * The return value of CVSpbcg is one of:
     *    CVSPILS_SUCCESS   if successful
     *    CVSPILS_MEM_NULL  if the cvode memory was NULL
     *    CVSPILS_MEM_FAIL  if there was a memory allocation failure
     *    CVSPILS_ILL_INPUT if a required vector operation is missing
     * The above constants are defined in cvode_spils.h
     *
     * -----------------------------------------------------------------
     */

    public int l_max;
    double[] r_star;
    double[] r;
    double[] p;
    double[] q;
    double[] u;
    double[] Ap;
    double[] vtemp;


    public JVodeSpbgs(Method method, OdeModel f, double[] u0, double t0, int pretype, int maxl) throws Exception
    {
        super(method, f, u0, t0);
//        iterationType = IterationType.SPBGS;
        /* Set ILS type */
        s_type = SPILS_SPBCG;

        /* Set Spbcg parameters that have been passed in call sequence */
        s_pretype = pretype;
        l_max = s_maxl = ( maxl <= 0 ) ? CVSPILS_MAXL : maxl;

        /* Set default values for the rest of the Spbcg parameters */
        s_eplifac = CVSPILS_EPLIN;

        lastFlag = CVSPILS_SUCCESS;

        setupNonNull = false;

        /* Check for legal pretype */
        if( ( pretype != PREC_NONE ) && ( pretype != PREC_LEFT ) && ( pretype != PREC_RIGHT ) && ( pretype != PREC_BOTH ) )
            throw new Exception(MSGS_BAD_PRETYPE);

        /* Allocate memory for ytemp and x */
        s_ytemp = new double[n];
        s_x = new double[n];


        /* Compute sqrtN from a dot product */
        Arrays.fill(s_ytemp, 1);
        s_sqrtN = Math.sqrt(VectorUtils.dotProd(s_ytemp, s_ytemp));

        r_star = new double[n];
        r = new double[n];
        p = new double[n];
        q = new double[n];
        ;
        u = new double[n];
        Ap = new double[n];
        vtemp = new double[n];

    }


    /*
     * -----------------------------------------------------------------
     * Function : CVSpbcgInit
     * -----------------------------------------------------------------
     * This routine does remaining initializations specific to the Spbcg
     * linear solver.
     * -----------------------------------------------------------------
     */
    @Override
    public int init()
    {
        /* Initialize counters */
        s_npe = s_nli = s_nps = s_ncfl = s_nstlpre = 0;
        s_njtimes = s_nfes = 0;

        /* Check for legal combination pretype - psolve */
        if( ( s_pretype != PREC_NONE ) && ( precondition == null ) )
        {
            throw new IllegalArgumentException(MSGS_PSOLVE_REQ);
        }

        /* Set setupNonNull = TRUE iff there is preconditioning
           (pretype != PREC_NONE)  and there is a preconditioning
           setup phase (pset !=null) */
        setupNonNull = ( s_pretype != PREC_NONE ) && ( precondition != null );

        /*  Set maxl in the SPBCG memory in case it was changed by the user */
        l_max = s_maxl;

        lastFlag = CVSPILS_SUCCESS;
        return 0;
    }

    /*
     * -----------------------------------------------------------------
     * Function : CVSpbcgSetup
     * -----------------------------------------------------------------
     * This routine does the setup operations for the Spbcg linear solver.
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
        double dgamma = Math.abs( ( gamma / gammaPrev ) - 1.0);
        boolean jbad = ( nSteps == 0 ) || ( nSteps > s_nstlpre + CVSPILS_MSBPRE )
                || ( ( convfail == FAIL_BAD_J ) && ( dgamma < CVSPILS_DGMAX ) ) || ( convfail == FAIL_OTHER );
        currentJacobian = jbad;
        boolean jok = !jbad;


        /* Call pset routine and possibly reset jcur */
        int retval = precondition.setup(tn, z[0], ftemp, jok, gamma);
        if( retval < 0 )
        {
            //CVode.processError(cv_mem, sundials_spbcgs.SPBCG_PSET_FAIL_UNREC, "CVSPBCG", "CVSpbcgSetup", cvode_spils.MSGS_PSET_FAILED);
            lastFlag = SPBCG_PSET_FAIL_UNREC;
        }
        if( retval > 0 )
        {
            lastFlag = SPBCG_PSET_FAIL_REC;
        }

        currentJacobian = jbad;

        /* If jcur = TRUE, increment npe and save nst value */
        if( currentJacobian )
        {
            s_npe++;
            s_nstlpre = nSteps;
        }

        lastFlag = SPBCG_SUCCESS;

        /* Return the same value that pset returned */
        return ( retval );
    }

    /*
     * -----------------------------------------------------------------
     * Function : CVSpbcgSolve
     * -----------------------------------------------------------------
     * This routine handles the call to the generic solver SpbcgSolve
     * for the solution of the linear system Ax = b with the SPBCG method.
     * The solution x is returned in the vector b.
     *
     * If the WRMS norm of b is small, we return x = b (if this is the first
     * Newton iteration) or x = 0 (if a later Newton iteration).
     *
     * Otherwise, we set the tolerance parameter and initial guess (x = 0),
     * call SpbcgSolve, and copy the solution x into b. The x-scaling and
     * b-scaling arrays are both equal to weight.
     *
     * The counters nli, nps, and ncfl are incremented, and the return value
     * is set according to the success of SpbcgSolve. The success flag is
     * returned if SpbcgSolve converged, or if this is the first Newton
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

        /* Set inputs delta and initial guess x = 0 to SpbcgSolve */
        s_delta = s_deltar * s_sqrtN;
        Arrays.fill(s_x, 0);

        /* Call SpbcgSolve and copy x to b */
        int retval = SpbcgSolve(s_x, b, s_pretype, s_delta, errorWeight, errorWeight);

        // VectorUtils.N_VScale(1.0, s_x, b);
        VectorUtils.copy(s_x, b);

        if( retval != SPBCG_SUCCESS )
            s_ncfl++;

        /* Interpret return value from SpbcgSolve */

        lastFlag = retval;

        switch( retval )
        {
            case SPBCG_SUCCESS:
                return ( 0 );
            case SPBCG_RES_REDUCED:
                if( mNewt == 0 )
                    return ( 0 );
                else
                    return ( 1 );
            case SPBCG_CONV_FAIL:
            case SPBCG_PSOLVE_FAIL_REC:
            case SPBCG_ATIMES_FAIL_REC:
                return ( 1 );
            case SPBCG_MEM_NULL:
                return ( -1 );
            case SPBCG_ATIMES_FAIL_UNREC:
                //CVode.processError(cv_mem, sundials_spbcgs.SPBCG_ATIMES_FAIL_UNREC, "CVSPBCG", "CVSpbcgSolve",
                //cvode_spils.MSGS_JTIMES_FAILED);
                return ( -1 );
            case SPBCG_PSOLVE_FAIL_UNREC:
                ////CVode.processError(cv_mem, sundials_spbcgs.SPBCG_PSOLVE_FAIL_UNREC, "CVSPBCG", "CVSpbcgSolve",
                //  cvode_spils.MSGS_PSOLVE_FAILED);
                return ( -1 );
        }

        return ( 0 );
    }



    /*
     * -----------------------------------------------------------------
     * Function : SpbcgSolve
     * -----------------------------------------------------------------
     * SpbcgSolve solves the linear system Ax = b by means of a scaled
     * preconditioned Bi-CGSTAB (SPBCG) iterative method.
     *
     *  mem  pointer to an internal memory block allocated during a
     *       prior call to SpbcgMalloc
     *
     *  A_data  pointer to a data structure containing information
     *          about the coefficient matrix A (passed to user-supplied
     *          function referenced by atimes (function pointer))
     *
     *  x  vector (type N_Vector) containing initial guess x_0 upon
     *     entry, but which upon return contains an approximate solution
     *     of the linear system Ax = b (solution only valid if return
     *     value is either SPBCG_SUCCESS or SPBCG_RES_REDUCED)
     *
     *  b  vector (type N_Vector) set to the right-hand side vector b
     *     of the linear system (undisturbed by function)
     *
     *  pretype  variable (type int) indicating the type of
     *           preconditioning to be used (see sundials_iterative.h)
     *
     *  delta  tolerance on the L2 norm of the scaled, preconditioned
     *         residual (if return value == SPBCG_SUCCESS, then
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
     *            is either SPBCG_SUCCESS or SPBCG_RES_REDUCED, then
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
    public int SpbcgSolve(double[] x, double[] b, int pretype, double delta, double[] sx, double[] sb)
    {
        try
        {
            double alpha, beta, omega, omega_denom, beta_num, beta_denom, r_norm, rho;
            int l, ier;

            boolean converged = false; /* Initialize converged flag */

            if( ( pretype != PREC_LEFT ) && ( pretype != PREC_RIGHT ) && ( pretype != PREC_BOTH ) )
                pretype = PREC_NONE;

            boolean preOnLeft = ( ( pretype == PREC_BOTH ) || ( pretype == PREC_LEFT ) );
            boolean preOnRight = ( ( pretype == PREC_BOTH ) || ( pretype == PREC_RIGHT ) );

            boolean scale_x = ( sx != null );
            boolean scale_b = ( sb != null );

            /* Set r_star to initial (unscaled) residual r_0 = b - A*x_0 */
            if( VectorUtils.dotProd(x, x) == 0 )
                //VectorUtils.N_VScale(1, b, r_star);
                VectorUtils.copy(b, r_star);
            else
            {
                ier = atimes(x, r_star);
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPBCG_ATIMES_FAIL_UNREC : SPBCG_ATIMES_FAIL_REC );
                VectorUtils.linearDiff(b, r_star, r_star);
            }

            /* Apply left preconditioner and b-scaling to r_star = r_0 */
            if( preOnLeft )
            {
                ier = pSolve(r_star, r, PREC_LEFT);
                s_nps++;
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPBCG_PSOLVE_FAIL_UNREC : SPBCG_PSOLVE_FAIL_REC );
            }
            else
                VectorUtils.copy(r_star, r);

            if( scale_b )
                VectorUtils.prod(sb, r, r_star);
            else
                VectorUtils.copy(r, r_star);

            /* Initialize beta_denom to the dot product of r0 with r0 */
            beta_denom = VectorUtils.dotProd(r_star, r_star);

            /* Set r_norm to L2 norm of r_star = sb P1_inv r_0, and
               return if small */
            r_norm = rho = Math.sqrt(beta_denom);
            if( r_norm <= delta )
                return ( SPBCG_SUCCESS );

            /* Copy r_star to r and p */
            VectorUtils.copy(r_star, r);
            VectorUtils.copy(r_star, p);

            /* Begin main iteration loop */
            for( l = 0; l < l_max; l++ )
            {
                s_nli++;

                /* Generate Ap = A-tilde p, where A-tilde = sb P1_inv A P2_inv sx_inv */

                /*   Apply x-scaling: vtemp = sx_inv p */
                if( scale_x )
                    VectorUtils.divide(p, sx, vtemp);
                else
                    VectorUtils.copy(p, vtemp);

                /*   Apply right preconditioner: vtemp = P2_inv sx_inv p */
                if( preOnRight )
                {
                    VectorUtils.copy(vtemp, Ap);
                    ier = pSolve(Ap, vtemp, PREC_RIGHT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPBCG_PSOLVE_FAIL_UNREC : SPBCG_PSOLVE_FAIL_REC );
                }

                /*   Apply A: Ap = A P2_inv sx_inv p */
                ier = atimes(vtemp, Ap);
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPBCG_ATIMES_FAIL_UNREC : SPBCG_ATIMES_FAIL_REC );

                /*   Apply left preconditioner: vtemp = P1_inv A P2_inv sx_inv p */
                if( preOnLeft )
                {
                    ier = pSolve(Ap, vtemp, PREC_LEFT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPBCG_PSOLVE_FAIL_UNREC : SPBCG_PSOLVE_FAIL_REC );
                }
                else
                    VectorUtils.copy(Ap, vtemp);

                /*   Apply b-scaling: Ap = sb P1_inv A P2_inv sx_inv p */
                if( scale_b )
                    VectorUtils.prod(sb, vtemp, Ap);
                else
                    VectorUtils.copy(vtemp, Ap);


                /* Calculate alpha = <r,r_star>/<Ap,r_star> */
                alpha = ( ( VectorUtils.dotProd(r, r_star) / VectorUtils.dotProd(Ap, r_star) ) );
                /* Update q = r - alpha*Ap = r - alpha*(sb P1_inv A P2_inv sx_inv p) */
                VectorUtils.linearSum( -alpha, Ap, r, q);
                /* Generate u = A-tilde q */

                /*   Apply x-scaling: vtemp = sx_inv q */
                if( scale_x )
                    VectorUtils.divide(q, sx, vtemp);
                else
                    VectorUtils.copy(q, vtemp);

                /*   Apply right preconditioner: vtemp = P2_inv sx_inv q */
                if( preOnRight )
                {
                    VectorUtils.copy(vtemp, u);
                    ier = pSolve(u, vtemp, PREC_RIGHT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPBCG_PSOLVE_FAIL_UNREC : SPBCG_PSOLVE_FAIL_REC );
                }

                /*   Apply A: u = A P2_inv sx_inv u */
                ier = atimes(vtemp, u);
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPBCG_ATIMES_FAIL_UNREC : SPBCG_ATIMES_FAIL_REC );

                /*   Apply left preconditioner: vtemp = P1_inv A P2_inv sx_inv p */
                if( preOnLeft )
                {
                    ier = pSolve(u, vtemp, PREC_LEFT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPBCG_PSOLVE_FAIL_UNREC : SPBCG_PSOLVE_FAIL_REC );
                }
                else
                    VectorUtils.copy(u, vtemp);

                /*   Apply b-scaling: u = sb P1_inv A P2_inv sx_inv u */
                if( scale_b )
                    VectorUtils.prod(sb, vtemp, u);
                else
                    VectorUtils.copy(vtemp, u);


                /* Calculate omega = <u,q>/<u,u> */
                omega_denom = VectorUtils.dotProd(u, u);
                if( omega_denom == 0 )
                    omega_denom = 1;
                omega = ( VectorUtils.dotProd(u, q) / omega_denom );

                /* Update x = x + alpha*p + omega*q */
                VectorUtils.linearSum(alpha, p, omega, q, vtemp);
                VectorUtils.linearSum(x, vtemp, x);

                /* Update the residual r = q - omega*u */
                VectorUtils.linearSum( -omega, u, q, r);

                /* Set rho = norm(r) and check convergence */
                rho = Math.sqrt(VectorUtils.dotProd(r, r));
                if( rho <= delta )
                {
                    converged = true;
                    break;
                }

                /* Not yet converged, continue iteration */
                /* Update beta = <rnew,r_star> / <rold,r_start> * alpha / omega */
                beta_num = VectorUtils.dotProd(r, r_star);
                beta = ( ( beta_num / beta_denom ) * ( alpha / omega ) );
                beta_denom = beta_num;

                /* Update p = r + beta*(p - omega*Ap) */
                VectorUtils.linearSum( -omega, Ap, p, vtemp);
                VectorUtils.linearSum(beta, vtemp, r, p);

            }

            /* Main loop finished */
            if( ( converged == true ) || ( rho < r_norm ) )
            {

                /* Apply the x-scaling and right preconditioner: x = P2_inv sx_inv x */
                if( scale_x )
                    VectorUtils.divide(x, sx, x);
                if( preOnRight )
                {
                    ier = pSolve(x, vtemp, PREC_RIGHT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPBCG_PSOLVE_FAIL_UNREC : SPBCG_PSOLVE_FAIL_REC );
                    VectorUtils.copy(vtemp, x);
                }

                if( converged == true )
                    return ( SPBCG_SUCCESS );
                else
                    return ( SPBCG_RES_REDUCED );
            }
            else
                return ( SPBCG_CONV_FAIL );
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage());
            return SPBCG_CONV_FAIL;
        }
    }

}
