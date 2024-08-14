package biouml.plugins.simulation.ode.jvode;

import biouml.plugins.simulation.ode.OdeModel;

public class JVodeBand extends DirectSolver
{

    private BandJacobian jacobian; // band Jacobian routine to be called
    private int ml; // lower bandwidth of Jacobian
    private int mu; // upper bandwidth of Jacobian
    private int smu; // upper bandwith of M = MIN(N-1,d_mu+d_ml)

    //Create Band version
    protected JVodeBand(Method method, OdeModel f, double[] u0, double t0, int mupper, int mlower, BandJacobian jac)
    {
        super(method, f, u0, t0);
        iterationType = IterationType.NEWTON;
        jacobianType = JacobianType.BAND;

        /* Load half-bandwiths in cvdls_mem */
        ml = mlower;
        mu = mupper;

        /* Test ml and mu for legality */
        if( ( ml < 0 ) || ( mu < 0 ) || ( ml >= n ) || ( mu >= n ) )
            throw new IllegalArgumentException("Illegal bandwidth parameter(s). Must have 0 <=  ml, mu <= N-1.");

        /* Set extended upper half-bandwith for M (required for pivoting) */
        smu = Math.min(n - 1, mu + ml);

        matr = new Matrix(n, mu, ml, smu);
        savedJ = new Matrix(n, mu, ml, mu);
        pivots = new int[n];

        lastFlag = SUCCESS;

        if( jac != null )
            jacobian = jac;
    }

    public void setUserJacobian(BandJacobian jacobian)
    {
        this.jacobian = jacobian;
    }

    /**This routine does remaining initializations specific to the band linear solver.*/
    @Override
    public int init()
    {
        return 0;
    }

    /**
     * This routine does the setup operations for the band linear solver.
     * It makes a decision whether or not to call the Jacobian evaluation
     * routine based on various state variables, and if not it uses the
     * saved copy.  In any case, it constructs the Newton matrix
     * M = I - gamma*J, updates counters, and calls the band LU
     * factorization routine.
     */
    @Override
    public int setup(int convfail)
    {

        /* Use nst, gamma/gammap, and convfail to set J eval. flag jok */
        double dgamma = Math.abs( ( gamma / gammaPrev ) - 1.0);
        boolean jbad = ( nSteps == 0 ) || ( nSteps > nStepsAtLastJac + MAX_STEPS_BETWEEN_JAC )
                || ( ( convfail == JVode.FAIL_BAD_J ) && ( dgamma < MAX_GAMMA_CHANGE_BETWEEN_JAC ) ) || ( convfail == JVode.FAIL_OTHER );
        if( !jbad )
        {
            /* If jok = TRUE, use saved copy of J */
            currentJacobian = false;
            savedJ.bandCopy(matr, mu, ml);
        }
        else
        {
            /* If jok = FALSE, call jac routine for new J value */
            nJacCalls++;
            nStepsAtLastJac = nSteps;
            currentJacobian = true;
            matr.setToZero();
            if( jacobian != null )
            {
                jacobian.getValue(n, mu, ml, tn, z[0], ftemp, matr);
            }
            else
            {
                try
                {
                    jacobian();
                }
                catch( Exception ex )
                {
                    processError("The Jacobian routine failed in an unrecoverable manner.");
                    lastFlag = JACFUNC_UNRECVR;
                    return -1;
                }
            }
            matr.bandCopy(savedJ, mu, ml);
        }
        /* Scale and add I to get M = I - gamma*J */
        matr.scale( -gamma);
        matr.addIdentity();

        /* Do LU factorization of M */
        int ier = MatrixUtils.BandGBTRF(matr, pivots);
        /* Return 0 if the LU was complete; otherwise return 1 */
        if( ier > 0 )
        {
            lastFlag = ier;
            return 1;
        }
        lastFlag = SUCCESS;
        return 0;
    }

    /**
     * This routine handles the solve operation for the band linear solver
     * by calling the band backsolve routine.  The return value is 0.
     */
    @Override
    public int solve(double[] b)
    {
        MatrixUtils.BandGBTRS(matr, pivots, b);
        /* If BDF, scale the correction to account for change in gamma */
        if( ( method == Method.BDF ) && ( gammaRatio != 1.0 ) )
            VectorUtils.scale(2.0 / ( 1.0 + gammaRatio ), b);
        lastFlag = SUCCESS;
        return 0;
    }



    /**
     * This routine generates a banded difference quotient approximation to
     * the Jacobian of f(t,y).  It assumes that a band matrix of type
     * DlsMat is stored column-wise, and that elements within each column
     * are contiguous. This makes it possible to get the address of a column
     * of J via the macro BAND_COL and to write a simple for loop to set
     * each of the elements of a column in succession.
     *
     * N is the length of all vector arguments.
     *
     * mupper is the upper half-bandwidth of the approximate banded
     * Jacobian. This parameter is the same as the mupper parameter
     * passed by the user to the linear solver initialization function.
     *
     * mlower is the lower half-bandwidth of the approximate banded
     * Jacobian. This parameter is the same as the mlower parameter
     * passed by the user to the linear solver initialization function.
     *
     * t is the current value of the independent variable.
     *
     * y is the current value of the dependent variable vector,
     *      namely the predicted value of y(t).
     *
     * fy is the vector f(t,y).
     *
     * Jac is the band matrix (of type DlsMat) that will be loaded
     * by a CVDlsBandJacFn with an approximation to the Jacobian matrix
     * Jac = (df_i/dy_j) at the point (t,y).
     * Three efficient ways to load J are:
     *
     * (1) (with macros - no explicit data structure references)
     *    for (j=0; j < n; j++) {
     *       col_j = BAND_COL(Jac,j);
     *       for (i=j-mupper; i <= j+mlower; i++) {
     *         generate J_ij = the (i,j)th Jacobian element
     *         BAND_COL_ELEM(col_j,i,j) = J_ij;
     *       }
     *     }
     *
     * (2) (with BAND_COL macro, but without BAND_COL_ELEM macro)
     *    for (j=0; j < n; j++) {
     *       col_j = BAND_COL(Jac,j);
     *       for (k=-mupper; k <= mlower; k++) {
     *         generate J_ij = the (i,j)th Jacobian element, i=j+k
     *         col_j[k] = J_ij;
     *       }
     *     }
     *
     * (3) (without macros - explicit data structure references)
     *     offset = Jac->smu;
     *     for (j=0; j < n; j++) {
     *       col_j = ((Jac->data)[j])+offset;
     *       for (k=-mupper; k <= mlower; k++) {
     *         generate J_ij = the (i,j)th Jacobian element, i=j+k
     *         col_j[k] = J_ij;
     *       }
     *     }
     * Caution: Jac->smu is generally NOT the same as mupper.
     *
     * The BAND_ELEM(A,i,j) macro is appropriate for use in small
     * problems in which efficiency of access is NOT a major concern.
     *
     * user_data is a pointer to user data - the same as the user_data
     *          parameter passed to CVodeSetFdata.
     *
     * NOTE: If the user's Jacobian routine needs other quantities,
     *     they are accessible as follows: hcur (the current stepsize)
     *     and ewt (the error weight vector) are accessible through
     *     CVodeGetCurrentStep and CVodeGetErrWeights, respectively
     *     (see cvode.h). The unit roundoff is available as
     *     UNIT_ROUNDOFF defined in sundials_types.h
     *
     * tmp1, tmp2, and tmp3 are pointers to memory allocated for
     * vectors of length N which can be used by a CVDlsBandJacFn
     * as temporary storage or work space.
     *
     * A CVDlsBandJacFn should return 0 if successful, a positive value
     * if a recoverable error occurred, and a negative value if an
     * unrecoverable error occurred.
     * -----------------------------------------------------------------
     */
    public void jacobian() throws Exception
    {
        VectorUtils.copy(z[0], y); // Load ytemp with y = predicted y vector
        double fnorm = VectorUtils.wrmsNorm(ftemp, errorWeight);
        double minInc = ( fnorm != 0.0 ) ? ( 1000 * Math.abs(h) * UROUND * n * fnorm ) : 1.0;

        // Set bandwidth and number of column groups for band differencing
        int width = ml + mu + 1;
        int ngroups = Math.min(width, n);

        for( int group = 1; group <= ngroups; group++ ) // Loop over column groups
        {
            for( int j = group - 1; j < n; j += width )// Increment all y_j in group
                y[j] += Math.max(UROUND_SQRT * Math.abs(z[0][j]), minInc / errorWeight[j]);

            nfCallsDQ++;
            acor = f.dy_dt(tn, y); // Evaluate f with incremented y

            // Restore ytemp, then form and load difference quotients
            for( int j = group - 1; j < n; j += width )
            {
                y[j] = z[0][j];
                double incInv = 1.0 / Math.max(UROUND_SQRT * Math.abs(z[0][j]), minInc / errorWeight[j]);
                int i1 = Math.max(0, j - mu);
                int i2 = Math.min(j + ml, n - 1);
                for( int i = i1; i <= i2; i++ )
                    matr.setBandElement(i, j, incInv * ( acor[i] - ftemp[i] ));
            }
        }
        return;
    }
}
