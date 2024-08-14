package biouml.plugins.simulation.ode.jvode;

import java.util.logging.Level;

import biouml.plugins.simulation.ode.OdeModel;

public class JVodeDense extends DirectSolver
{
    private DenseJacobian jacobian; // dense Jacobian routine to be called

    protected JVodeDense(Method method, OdeModel f, double[] u0, double t0, DenseJacobian jac)
    {
        super(method, f, u0, t0);
        iterationType = IterationType.NEWTON;
        jacobianType = JacobianType.DENSE;
        matr = new Matrix(n, n);
        savedJ = new Matrix(n, n);
        pivots = new int[n];

        if( jac != null )
            jacobian = jac;

        lastFlag = SUCCESS;
    }

    public void setUserJacobian(DenseJacobian jacobian)
    {
        this.jacobian = jacobian;
    }

    @Override
    public int init()
    {
        return 0;
    }

    /**
     * This routine does the setup operations for the dense linear solver.
     * It makes a decision whether or not to call the Jacobian evaluation
     * routine based on various state variables, and if not it uses the
     * saved copy.  In any case, it constructs the Newton matrix
     * M = I - gamma*J, updates counters, and calls the dense LU
     * factorization routine.
     */
    @Override
    public int setup(int convfail)
    {
        // Use nst, gamma/gammap, and convfail to set J eval. flag jok
        double dgamma = Math.abs( ( gamma / gammaPrev ) - 1.0);

        boolean jbad = ( nSteps == 0 ) || ( nSteps > nStepsAtLastJac + MAX_STEPS_BETWEEN_JAC )
                || ( ( convfail == JVode.FAIL_BAD_J ) && ( dgamma < MAX_GAMMA_CHANGE_BETWEEN_JAC ) ) || ( convfail == JVode.FAIL_OTHER );
        if( !jbad )
        {
            // If jok = TRUE, use saved copy of J
            currentJacobian = false;
            savedJ.denseCopy(matr);
        }
        else
        {
            // If jok = FALSE, call jac routine for new J value
            nJacCalls++;
            nStepsAtLastJac = nSteps;
            currentJacobian = true;
            matr.setToZero();

            int retval = jacobian();

            if( retval < 0 )
            {
                processError("The Jacobian routine failed in an unrecoverable manner.");
                lastFlag = JACFUNC_UNRECVR;
                return -1;
            }
            if( retval > 0 )
            {
                lastFlag = JACFUNC_RECVR;
                return 1;
            }
            matr.denseCopy(savedJ);
        }
        // Scale and add I to get M = I - gamma*J
        matr.scale( -gamma);
        matr.addIdentity();

        // Do LU factorization of M
        return MatrixUtils.DenseGETRF(matr, pivots) > 0 ? 1 : 0;
    }

    /**
     * This routine handles the solve operation for the dense linear solver
     * by calling the dense backsolve routine.  The returned value is 0.
     */
    @Override
    public int solve(double[] b)
    {
        MatrixUtils.DenseGETRS(matr, pivots, b);

        // If BDF, scale the correction to account for change in gamma
        if( method == Method.BDF && gammaRatio != 1.0 )
            VectorUtils.scale(2.0 / ( 1.0 + gammaRatio ), b);

        lastFlag = SUCCESS;
        return SUCCESS;
    }

    /**
     * This routine generates a dense difference quotient approximation to
     * the Jacobian of f(t,y). It assumes that a dense matrix of type
     * DlsMat is stored column-wise, and that elements within each column
     * are contiguous. The address of the jth column of J is obtained via
     * the macro DENSE_COL and this pointer is associated with an N_Vector
     * using the N_VGetArrayPointer/N_VSetArrayPointer functions.
     * Finally, the actual computation of the jth column of the Jacobian is
     * done with a call to N_VLinearSum.
     *
     * N   is the problem size.
     *
     * Jac is the dense matrix (of type DlsMat) that will be loaded
     *     by a CVDlsDenseJacFn with an approximation to the Jacobian
     *     matrix J = (df_i/dy_j) at the point (t,y).
     *
     * t   is the current value of the independent variable.
     *
     * y   is the current value of the dependent variable vector,
     *     namely the predicted value of y(t).
     *
     * fy  is the vector f(t,y).
     *
     * user_data is a pointer to user data - the same as the user_data
     *     parameter passed to CVodeSetFdata.
     *
     * tmp1, tmp2, and tmp3 are pointers to memory allocated for
     * vectors of length N which can be used by a CVDlsDenseJacFn
     * as temporary storage or work space.
     *
     * A CVDlsDenseJacFn should return 0 if successful, a positive
     * value if a recoverable error occurred, and a negative value if
     * an unrecoverable error occurred.
     *
     * -----------------------------------------------------------------
     *
     * NOTE: The following are two efficient ways to load a dense Jac:
     * (1) (with macros - no explicit data structure references)
     *     for (j=0; j < Neq; j++) {
     *       col_j = DENSE_COL(Jac,j);
     *       for (i=0; i < Neq; i++) {
     *         generate J_ij = the (i,j)th Jacobian element
     *         col_j[i] = J_ij;
     *       }
     *     }
     * (2) (without macros - explicit data structure references)
     *     for (j=0; j < Neq; j++) {
     *       col_j = (Jac->data)[j];
     *       for (i=0; i < Neq; i++) {
     *         generate J_ij = the (i,j)th Jacobian element
     *         col_j[i] = J_ij;
     *       }
     *     }
     * A third way, using the DENSE_ELEM(A,i,j) macro, is much less
     * efficient in general.  It is only appropriate for use in small
     * problems in which efficiency of access is NOT a major concern.
     *
     * NOTE: If the user's Jacobian routine needs other quantities,
     *     they are accessible as follows: hcur (the current stepsize)
     *     and ewt (the error weight vector) are accessible through
     *     CVodeGetCurrentStep and CVodeGetErrWeights, respectively
     *     (see cvode.h). The unit roundoff is available as
     *     UNIT_ROUNDOFF defined in sundials_types.h.
     */

    public int jacobian()
    {
        try
        {
            double inc, inc_inv, yjsaved;
            /* Set minimum increment based on uround and norm of f */
            //            double srur = Math.sqrt(UROUND);
            double fnorm = VectorUtils.wrmsNorm(ftemp, errorWeight);
            double minInc = ( fnorm != 0.0 ) ? ( 1000 * Math.abs(h) * UROUND * n * fnorm ) : 1.0;

            for( int j = 0; j < n; j++ )
            {
                double[] jthCol = new double[n];
                yjsaved = z[0][j];
                double v1 = UROUND_SQRT * Math.abs(yjsaved);
                double v2 = minInc / errorWeight[j];
                inc = v1 > v2 ? v1 : v2;
                z[0][j] = yjsaved + inc;
                acor = f.dy_dt(tn, z[0]);
                nfCallsDQ++;
                z[0][j] = yjsaved;
                inc_inv = 1.0 / inc;
                VectorUtils.scaleDiff(inc_inv, acor, ftemp, jthCol);
                matr.cols[j] = jthCol;
            }
            return 0;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage());
            return -1;
        }
    }

}
