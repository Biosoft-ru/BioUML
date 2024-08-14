package biouml.plugins.simulation.ode.jvode;

import java.util.logging.Level;
import java.util.Arrays;

import biouml.plugins.simulation.ode.OdeModel;

public class JVodeSpgmr extends IterativeSolver
{

    public final static int SPGMR_SUCCESS = 0; /* Converged                     */
    public final static int SPGMR_RES_REDUCED = 1; /* Did not converge, but reduced
                                                   norm of residual              */
    public final static int SPGMR_CONV_FAIL = 2; /* Failed to converge            */
    public final static int SPGMR_QRFACT_FAIL = 3; /* QRfact found singular matrix  */
    public final static int SPGMR_PSOLVE_FAIL_REC = 4; /* psolve failed recoverably     */
    public final static int SPGMR_ATIMES_FAIL_REC = 5; /* atimes failed recoverably     */
    public final static int SPGMR_PSET_FAIL_REC = 6; /* pset faild recoverably        */

    public final static int SPGMR_MEM_NULL = -1; /* mem argument is NULL          */
    public final static int SPGMR_ATIMES_FAIL_UNREC = -2; /* atimes returned failure flag  */
    public final static int SPGMR_PSOLVE_FAIL_UNREC = -3; /* psolve failed unrecoverably   */
    public final static int SPGMR_GS_FAIL = -4; /* Gram-Schmidt routine faiuled  */
    public final static int SPGMR_QRSOL_FAIL = -5; /* QRsol found singular R        */
    public final static int SPGMR_PSET_FAIL_UNREC = -6; /* pset failed unrecoverably     */

    /*
     * l_max is the maximum Krylov dimension that SpgmrSolve will be
     * permitted to use.
     *
     * V is the array of Krylov basis vectors v_1, ..., v_(l_max+1),
     * stored in V[0], ..., V[l_max], where l_max is the second
     * parameter to SpgmrMalloc. Each v_i is a vector of type
     * N_Vector.
     *
     * Hes is the (l_max+1) x l_max Hessenberg matrix. It is stored
     * row-wise so that the (i,j)th element is given by Hes[i][j].
     *
     * givens is a length 2*l_max array which represents the
     * Givens rotation matrices that arise in the algorithm. The
     * Givens rotation matrices F_0, F_1, ..., F_j, where F_i is
     *
     *             1
     *               1
     *                 c_i  -s_i      <--- row i
     *                 s_i   c_i
     *                           1
     *                             1
     *
     * are represented in the givens vector as
     * givens[0]=c_0, givens[1]=s_0, givens[2]=c_1, givens[3]=s_1,
     * ..., givens[2j]=c_j, givens[2j+1]=s_j.
     *
     * xcor is a vector (type N_Vector) which holds the scaled,
     * preconditioned correction to the initial guess.
     *
     * yg is a length (l_max+1) array of realtype used to hold "short"
     * vectors (e.g. y and g).
     *
     * vtemp is a vector (type N_Vector) used as temporary vector
     * storage during calculations.
     * -----------------------------------------------------------------
     */
    int l_max;
    double[][] V;
    double[][] Hes;
    double[] givens;
    double[] xcor;
    double[] yg;
    double[] vtemp;


    /**
     *
     * @param pretype   is the type of user preconditioning to be done.
     *           This must be one of the four enumeration constants
     *           PREC_NONE, PREC_LEFT, PREC_RIGHT, or PREC_BOTH defined
     *           in sundials_iterative.h.
     *           These correspond to no preconditioning,
     *           left preconditioning only, right preconditioning
     *           only, and both left and right preconditioning,
     *           respectively.
     *
     * @param maxl      is the maximum Krylov dimension. This is an
     *           optional input to the CVSPGMR solver. Pass 0 to
     *           use the default value CVSPGMR_MAXL=5.
     *
     * -----------------------------------------------------------------
     */
    public JVodeSpgmr(Method method, OdeModel f, double[] u0, double t0, int pretype, int maxl) throws Exception
    {
        super(method, f, u0, t0);
//        iterationType = IterationType.SPGMR;

        l_max = s_maxl = ( maxl <= 0 ) ? CVSPILS_MAXL : maxl;

        //this.l_max = l_max;
        V = new double[l_max + 1][n];
        Hes = new double[l_max + 1][l_max];
        givens = new double[2 * l_max];
        xcor = new double[n];
        yg = new double[l_max + 1];
        vtemp = new double[n];

        /* Set ILS type */
        s_type = SPILS_SPGMR;

        /* Set Spgmr parameters that have been passed in call sequence */
        s_pretype = pretype;

        /* Set default values for the rest of the Spgmr parameters */
        s_gstype = MODIFIED_GS;
        s_eplifac = CVSPILS_EPLIN;

        lastFlag = CVSPILS_SUCCESS;

        setupNonNull = false;

        /* Check for legal pretype */
        if( ( pretype != PREC_NONE ) && ( pretype != PREC_LEFT ) && ( pretype != PREC_RIGHT ) && ( pretype != PREC_BOTH ) )
            throw new Exception(MSGS_BAD_PRETYPE);

        /* Allocate memory for ytemp and x */
        s_ytemp = new double[n];
        s_x = new double[n];//VectorUtils.N_VClone(cv_tempv);

        /* Compute sqrtN from a dot product */
        Arrays.fill(s_ytemp,1);
        s_sqrtN = Math.sqrt(VectorUtils.dotProd(s_ytemp, s_ytemp));
    }

    /*
     * -----------------------------------------------------------------
     * CVSpgmrInit
     * -----------------------------------------------------------------
     * This routine does remaining initializations specific to the Spgmr
     * linear solver.
     * -----------------------------------------------------------------
     */

    @Override
    public int init()
    {

        /* Initialize counters */
        s_npe = s_nli = s_nps = s_ncfl = s_nstlpre = 0;
        s_njtimes = s_nfes = 0;

        /* Check for legal combination cvspils_mem.s_pretype - psolve */
        if( ( s_pretype != PREC_NONE ) && ( precondition == null ) )
            return -1;

        /* Set setupNonNull = TRUE iff there is preconditioning (cvspils_mem.s_pretype != PREC_NONE)
           and there is a preconditioning setup phase (pset != null)             */
        setupNonNull = ( s_pretype != PREC_NONE ) && ( precondition != null );

        lastFlag = CVSPILS_SUCCESS;
        return ( 0 );
    }

    /*
     * -----------------------------------------------------------------
     * CVSpgmrSetup
     * -----------------------------------------------------------------
     * This routine does the setup operations for the Spgmr linear solver.
     * It makes a decision as to whether or not to signal for re-evaluation
     * of Jacobian data in the pset routine, based on various state
     * variables, then it calls pset.  If we signal for re-evaluation,
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
            // CVode.processError(cv_mem, sundials_spgmr.SPGMR_PSET_FAIL_UNREC, "CVSPGMR", "CVSpgmrSetup", cvode_spils.MSGS_PSET_FAILED);
            lastFlag = SPGMR_PSET_FAIL_UNREC;
        }
        if( retval > 0 )
        {
            lastFlag = SPGMR_PSET_FAIL_REC;
        }

        currentJacobian = jbad;

        /* If jcur = TRUE, increment npe and save nst value */
        if( currentJacobian )
        {
            s_npe++;
            s_nstlpre = nSteps;
        }

        lastFlag = SPGMR_SUCCESS;

        /* Return the same value that pset returned */
        return ( retval );
    }

    /*
     * -----------------------------------------------------------------
     * CVSpgmrSolve
     * -----------------------------------------------------------------
     * This routine handles the call to the generic solver SpgmrSolve
     * for the solution of the linear system Ax = b with the SPGMR method,
     * without restarts.  The solution x is returned in the vector b.
     *
     * If the WRMS norm of b is small, we return x = b (if this is the first
     * Newton iteration) or x = 0 (if a later Newton iteration).
     *
     * Otherwise, we set the tolerance parameter and initial guess (x = 0),
     * call SpgmrSolve, and copy the solution x into b.  The x-scaling and
     * b-scaling arrays are both equal to weight, and no restarts are allowed.
     *
     * The counters nli, nps, and ncfl are incremented, and the return value
     * is set according to the success of SpgmrSolve.  The success flag is
     * returned if SpgmrSolve converged, or if this is the first Newton
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
                Arrays.fill(b,0);
            return 0;
        }

        /* Set vectors ycur and fcur for use by the Atimes and Psolve routines */
        s_ycur = y;
        s_fcur = ftemp;

        /* Set inputs delta and initial guess x = 0 to SpgmrSolve */
        s_delta = s_deltar * s_sqrtN;
        Arrays.fill(s_x,0);

        int retval = SpgmrSolve(b, 0, errorWeight, errorWeight);

        VectorUtils.copy( s_x, b);

        if( retval != SPGMR_SUCCESS )
            s_ncfl++;

        /* Interpret return value from SpgmrSolve */
        lastFlag = retval;

        switch( retval )
        {

            case SPGMR_SUCCESS:
                return ( 0 );
            case SPGMR_RES_REDUCED:
                if( mNewt == 0 )
                    return ( 0 );
                else
                    return ( 1 );
            case SPGMR_CONV_FAIL:
            case SPGMR_QRFACT_FAIL:
            case SPGMR_PSOLVE_FAIL_REC:
            case SPGMR_ATIMES_FAIL_REC:
                return ( 1 );
            case SPGMR_MEM_NULL:
                return ( -1 );
            case SPGMR_ATIMES_FAIL_UNREC:
                // CVode.processError(cv_mem, SPGMR_ATIMES_FAIL_UNREC, "CVSPGMR", "CVSpgmrSolve",
                //         MSGS_JTIMES_FAILED);
                return ( -1 );
            case SPGMR_PSOLVE_FAIL_UNREC:
                //CVode.processError(cv_mem, SPGMR_PSOLVE_FAIL_UNREC, "CVSPGMR", "CVSpgmrSolve",
                //       MSGS_PSOLVE_FAILED);
                return ( -1 );
            case SPGMR_GS_FAIL:
            case SPGMR_QRSOL_FAIL:
                return ( -1 );
        }

        return 0;
    }


    /*
     * -----------------------------------------------------------------
     * Function : SpgmrSolve
     * -----------------------------------------------------------------
     * SpgmrSolve solves the linear system Ax = b using the SPGMR
     * method. The return values are given by the symbolic constants
     * below. The first SpgmrSolve parameter is a pointer to memory
     * allocated by a prior call to SpgmrMalloc.
     *
     * mem is the pointer returned by SpgmrMalloc to the structure
     * containing the memory needed by SpgmrSolve.
     *
     * A_data is a pointer to information about the coefficient
     * matrix A. This pointer is passed to the user-supplied function
     * atimes.
     *
     * x is the initial guess x_0 upon entry and the solution
     * N_Vector upon exit with return value SPGMR_SUCCESS or
     * SPGMR_RES_REDUCED. For all other return values, the output x
     * is undefined.
     *
     * b is the right hand side N_Vector. It is undisturbed by this
     * function.
     *
     * pretype is the type of preconditioning to be used. Its
     * legal possible values are enumerated in iterativ.h. These
     * values are PREC_NONE=0, PREC_LEFT=1, PREC_RIGHT=2, and
     * PREC_BOTH=3.
     *
     * gstype is the type of Gram-Schmidt orthogonalization to be
     * used. Its legal values are enumerated in iterativ.h. These
     * values are MODIFIED_GS=0 and CLASSICAL_GS=1.
     *
     * delta is the tolerance on the L2 norm of the scaled,
     * preconditioned residual. On return with value SPGMR_SUCCESS,
     * this residual satisfies || s1 P1_inv (b - Ax) ||_2 <= delta.
     *
     * max_restarts is the maximum number of times the algorithm is
     * allowed to restart.
     *
     * P_data is a pointer to preconditioner information. This
     * pointer is passed to the user-supplied function psolve.
     *
     * s1 is an N_Vector of positive scale factors for P1-inv b, where
     * P1 is the left preconditioner. (Not tested for positivity.)
     * Pass NULL if no scaling on P1-inv b is required.
     *
     * s2 is an N_Vector of positive scale factors for P2 x, where
     * P2 is the right preconditioner. (Not tested for positivity.)
     * Pass NULL if no scaling on P2 x is required.
     *
     * atimes is the user-supplied function which performs the
     * operation of multiplying A by a given vector. Its description
     * is given in iterative.h.
     *
     * psolve is the user-supplied function which solves a
     * preconditioner system Pz = r, where P is P1 or P2. Its full
     * description is  given in iterativ.h. The psolve function will
     * not be called if pretype is NONE; in that case, the user
     * should pass NULL for psolve.
     *
     * res_norm is a pointer to the L2 norm of the scaled,
     * preconditioned residual. On return with value SPGMR_SUCCESS or
     * SPGMR_RES_REDUCED, (*res_norm) contains the value
     * || s1 P1_inv (b - Ax) ||_2 for the computed solution x.
     * For all other return values, (*res_norm) is undefined. The
     * caller is responsible for allocating the memory (*res_norm)
     * to be filled in by SpgmrSolve.
     *
     * nli is a pointer to the number of linear iterations done in
     * the execution of SpgmrSolve. The caller is responsible for
     * allocating the memory (*nli) to be filled in by SpgmrSolve.
     *
     * nps is a pointer to the number of calls made to psolve during
     * the execution of SpgmrSolve. The caller is responsible for
     * allocating the memory (*nps) to be filled in by SpgmrSolve.
     *
     * Note: Repeated calls can be made to SpgmrSolve with varying
     * input arguments. If, however, the problem size N or the
     * maximum Krylov dimension l_max changes, then a call to
     * SpgmrMalloc must be made to obtain new memory for SpgmrSolve
     * to use.
     * -----------------------------------------------------------------
     */
    public int SpgmrSolve(double[] b, int max_restarts, double[] s1, double[] s2)
    {
        try
        {
        double beta, rotation_product, r_norm, s_product, rho;
        int k, ier;

        /* Initialize some variables */
        int l_plus_1 = 0;
        int krydim = 0;

        boolean converged = false; /* Initialize converged flag */

        if( max_restarts < 0 )
            max_restarts = 0;

        if( ( s_pretype != PREC_LEFT ) && ( s_pretype != PREC_RIGHT ) && ( s_pretype != PREC_BOTH ) )
            s_pretype = PREC_NONE;

        boolean preOnLeft = ( ( s_pretype == PREC_LEFT ) || ( s_pretype == PREC_BOTH ) );
        boolean preOnRight = ( ( s_pretype == PREC_RIGHT ) || ( s_pretype == PREC_BOTH ) );
        boolean scale1 = ( s1 != null );
        boolean scale2 = ( s2 != null );

        /* Set vtemp and V[0] to initial (unscaled) residual r_0 = b - A*x_0. */
        if( VectorUtils.dotProd(s_x, s_x) == 0 )
        {
            VectorUtils.copy(b, vtemp);
        }
        else
        {
            ier = atimes(s_x, vtemp);
            if( ier != 0 )
                return ( ( ier < 0 ) ? SPGMR_ATIMES_FAIL_UNREC : SPGMR_ATIMES_FAIL_REC );
            VectorUtils.linearDiff( b, vtemp, vtemp);
        }
        VectorUtils.copy(vtemp, V[0]);

        /* Apply left preconditioner and left scaling to V[0] = r_0. */
        if( preOnLeft )
        {
            ier = pSolve(V[0], vtemp, PREC_LEFT);
            s_nps++;
            if( ier != 0 )
                return ( ( ier < 0 ) ? SPGMR_PSOLVE_FAIL_UNREC : SPGMR_PSOLVE_FAIL_REC );
        }
        else
        {
            VectorUtils.copy(V[0], vtemp);
        }

        if( scale1 )
        {
            VectorUtils.prod(s1, vtemp, V[0]);
        }
        else
        {
            VectorUtils.copy(vtemp, V[0]);
        }

        /* Set r_norm = beta to L2 norm of V[0] = s1 P1_inv r_0, and
          return if small.  */
        r_norm = beta = Math.sqrt(VectorUtils.dotProd(V[0], V[0]));
        if( r_norm <= s_delta )
            return ( SPGMR_SUCCESS );

        /* Initialize rho to avoid compiler warning message */
        rho = beta;

        /* Set xcor = 0. */
        Arrays.fill(xcor,0);


        /* Begin outer iterations: up to (max_restarts + 1) attempts. */
        for( int ntries = 0; ntries <= max_restarts; ntries++ )
        {

            /* Initialize the Hessenberg matrix Hes and Givens rotation
               product.  Normalize the initial vector V[0].             */
            for( int i = 0; i <= l_max; i++ )
            {
                for( int j = 0; j < l_max; j++ )
                {
                    Hes[i][j] = 0;
                }
            }

            rotation_product = 1;

            VectorUtils.scale(1.0 / r_norm, V[0]);

            /* Inner loop: generate Krylov sequence and Arnoldi basis. */
            for( int l = 0; l < l_max; l++ )
            {
                s_nli++;
                krydim = l_plus_1 = l + 1;

                /* Generate A-tilde V[l], where A-tilde = s1 P1_inv A P2_inv s2_inv. */

                /* Apply right scaling: vtemp = s2_inv V[l]. */
                if( scale2 )
                    VectorUtils.divide(V[l], s2, vtemp);
                else
                    VectorUtils.copy( V[l], vtemp);

                /* Apply right preconditioner: vtemp = P2_inv s2_inv V[l]. */
                if( preOnRight )
                {
                    VectorUtils.copy(vtemp, V[l_plus_1]);
                    ier = pSolve(V[l_plus_1], vtemp, PREC_RIGHT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPGMR_PSOLVE_FAIL_UNREC : SPGMR_PSOLVE_FAIL_REC );
                }

                /* Apply A: V[l+1] = A P2_inv s2_inv V[l]. */
                ier = atimes(vtemp, V[l_plus_1]);
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPGMR_ATIMES_FAIL_UNREC : SPGMR_ATIMES_FAIL_REC );

                /* Apply left preconditioning: vtemp = P1_inv A P2_inv s2_inv V[l]. */
                if( preOnLeft )
                {
                    ier = pSolve(V[l_plus_1], vtemp, PREC_LEFT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPGMR_PSOLVE_FAIL_UNREC : SPGMR_PSOLVE_FAIL_REC );
                }
                else
                {
                    VectorUtils.copy( V[l_plus_1], vtemp);
                }

                /* Apply left scaling: V[l+1] = s1 P1_inv A P2_inv s2_inv V[l]. */
                if( scale1 )
                {
                    VectorUtils.prod(s1, vtemp, V[l_plus_1]);
                }
                else
                {
                    VectorUtils.copy( vtemp, V[l_plus_1]);
                }

                /*  Orthogonalize V[l+1] against previous V[i]: V[l+1] = w_tilde. */

                if( s_gstype == CLASSICAL_GS )
                {
                    Hes[l_plus_1][l] = IterativeUtils.ClassicalGS(V, Hes, l_plus_1, l_max, vtemp, yg);
                }
                else
                {
                    Hes[l_plus_1][l] = IterativeUtils.ModifiedGS(V, Hes, l_plus_1, l_max);
                }

                /*  Update the QR factorization of Hes. */
                if( IterativeUtils.QRfact(krydim, Hes, givens, l) != 0 )
                    return ( SPGMR_QRFACT_FAIL );

                /*  Update residual norm estimate; break if convergence test passes. */

                rotation_product *= givens[2 * l + 1];
                /*cnt.res_norm = */rho = Math.abs(rotation_product * r_norm);

                if( rho <= s_delta )
                {
                    converged = true;
                    break;
                }

                /* Normalize V[l+1] with norm value from the Gram-Schmidt routine. */
                VectorUtils.scale(1.0 / Hes[l_plus_1][l], V[l_plus_1]);
            }

            /* Inner loop is done.  Compute the new correction vector xcor. */
            /* Construct g, then solve for y. */
            yg[0] = r_norm;
            for( int i = 1; i <= krydim; i++ )
                yg[i] = 0;
            if( IterativeUtils.QRsol(krydim, Hes, givens, yg) != 0 )
                return SPGMR_QRSOL_FAIL;

            /* Add correction vector V_l y to xcor. */
            for( k = 0; k < krydim; k++ )
                VectorUtils.linearSum(yg[k], V[k], xcor);

            /* If converged, construct the final solution vector x and return. */
            if( converged )
            {
                /* Apply right scaling and right precond.: vtemp = P2_inv s2_inv xcor. */
                if( scale2 )
                    VectorUtils.divide(xcor, s2, xcor);
                if( preOnRight )
                {
                    ier = pSolve(xcor, vtemp, PREC_RIGHT);
                    s_nps++;
                    if( ier != 0 )
                        return ( ( ier < 0 ) ? SPGMR_PSOLVE_FAIL_UNREC : SPGMR_PSOLVE_FAIL_REC );
                }
                else
                {
                    VectorUtils.copy(xcor, vtemp);
                }

                /* Add vtemp to initial x to get final solution x, and return */

                VectorUtils.linearSum(s_x, vtemp, s_x);

                return SPGMR_SUCCESS;
            }

            /* Not yet converged; if allowed, prepare for restart. */

            if( ntries == max_restarts )
                break;

            /* Construct last column of Q in yg. */

            s_product = 1;
            for( int i = krydim; i > 0; i-- )
            {
                yg[i] = s_product * givens[2 * i - 2];
                s_product *= givens[2 * i - 1];
            }
            yg[0] = s_product;

            /* Scale r_norm and yg. */
            r_norm *= s_product;
            for( int i = 0; i <= krydim; i++ )
                yg[i] *= r_norm;
            r_norm = Math.abs(r_norm);

            /* Multiply yg by V_(krydim+1) to get last residual vector; restart. */
            VectorUtils.scale(yg[0], V[0]);
            for( k = 1; k <= krydim; k++ )
                VectorUtils.linearSum(yg[k], V[k], V[0]);

        }

        /* Failed to converge, even after allowed restarts.
          If the residual norm was reduced below its initial value, compute
          and return x anyway.  Otherwise return failure flag. */

        if( rho < beta )
        {

            /* Apply right scaling and right precond.: vtemp = P2_inv s2_inv xcor. */

            if( scale2 )
                VectorUtils.divide(xcor, s2, xcor);
            if( preOnRight )
            {
                ier = pSolve(xcor, vtemp, PREC_RIGHT);
                s_nps++;
                if( ier != 0 )
                    return ( ( ier < 0 ) ? SPGMR_PSOLVE_FAIL_UNREC : SPGMR_PSOLVE_FAIL_REC );
            }
            else
            {
                VectorUtils.copy(xcor, vtemp);
            }

            /* Add vtemp to initial x to get final solution x, and return. */

            VectorUtils.linearSum(s_x, vtemp, s_x);

            return SPGMR_RES_REDUCED;
        }
        }
        catch (Exception ex)
        {
            log.log(Level.SEVERE, ex.getMessage());
            return SPGMR_CONV_FAIL;
        }
        return SPGMR_CONV_FAIL;
    }

}
