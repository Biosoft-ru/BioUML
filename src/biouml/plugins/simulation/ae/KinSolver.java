package biouml.plugins.simulation.ae;

import biouml.plugins.simulation.ae.AeModel;
import biouml.plugins.simulation.ode.jvode.Matrix;
import biouml.plugins.simulation.ode.jvode.MatrixUtils;
import biouml.plugins.simulation.ode.jvode.VectorUtils;

public class KinSolver extends KinSolEngine
{

    public KinSolver(AeModel f, double[] initialGuess) throws Exception
    {
        super(f, initialGuess, NONE);
        /* Set default Jacobian routine and Jacobian data */
        jacDQ = true;

        setupNonNull = true;

        /* Allocate memory for J and pivot array */
        jacobianMatrix = new Matrix(n, n);

        pivots = new int[n];

        /* This is a direct linear solver */
        inexactLs = false;
    }

    protected boolean jacDQ; /* TRUE if using internal DQ Jacobian approx.   */
    protected Matrix jacobianMatrix; /* problem Jacobian                             */
    protected int[] pivots; /* pivot array for PM = LU                      */


    //information for testing purposes
    public long nje; /* no. of calls to jac                          */
    public long nfeDQ; /* no. of calls to F due to DQ Jacobian approx. */



    /**
     * This routine does remaining initializations specific to the dense
     * linear solver.
     */
    @Override
    protected int init()
    {
        return 0;
    }

    /**
    * This routine does the setup operations for the dense linear solver.
    * It calls the dense LU factorization routine.
    */
    @Override
    protected int setup()
    {
        nje++;
        if( jac(u, fValue, jacobianMatrix) != 0 )
            return -1;

        /* Do LU factorization of J. Return 0 if the LU was complete; otherwise return -1 */
        if( MatrixUtils.DenseGETRF(jacobianMatrix, pivots) > 0 )
            return -1;
        return 0;
    }

    /**
     * This routine handles the solve operation for the dense linear solver
     * by calling the dense backsolve routine.  The returned value is 0.
     */
    @Override
    protected int solve(double[] x, double[] b/*, double *res_norm*/)
    {
        /* Copy the right-hand side into x */
        VectorUtils.copy(b, x);


        /* Back-solve and get solution in x */
        MatrixUtils.DenseGETRS(jacobianMatrix, pivots, x);

        /* Compute the terms Jpnorm and sfdotJp for use in the global strategy
           routines and in KINForcingTerm. Both of these terms are subsequently
           corrected if the step is reduced by constraints or the line search.

           sJpnorm is the norm of the scaled product (scaled by fscale) of
           the current Jacobian matrix J and the step vector p.

           sfdotJp is the dot product of the scaled f vector and the scaled
           vector J*p, where the scaling uses fscale. */

        sJpnorm = VectorUtils.l2Norm(b, fscale);
        VectorUtils.prod(b, fscale, b);
        VectorUtils.prod(b, fscale, b);
        sfdotJp = VectorUtils.dotProd(fValue, b);
        return 0;
    }


    /**
     * This routine generates a dense difference quotient approximation to
     * the Jacobian of F(u). It assumes that a dense matrix of type
     * DlsMat is stored column-wise, and that elements within each column
     * are contiguous. The address of the jth column of J is obtained via
     * the macro DENSE_COL and this pointer is associated with an double[]
     * using the N_VGetArrayPointer/N_VSetArrayPointer functions.
     * Finally, the actual computation of the jth column of the Jacobian is
     * done with a call to N_VLinearSum.
     *
     * The increment used in the finite-difference approximation
     *   J_ij = ( F_i(u+sigma_j * e_j) - F_i(u)  ) / sigma_j
     * is
     *  sigma_j = max{|u_j|, |1/uscale_j|} * sqrt(uround)
     *
     * Note: uscale_j = 1/typ(u_j)
     *
     * NOTE: Any type of failure of the system function her leads to an
     *       unrecoverable failure of the Jacobian function and thus
     *       of the linear solver setup function, stopping KINSOL.
     */
    int jac(double[] u, double[] f, Matrix Jac)
    {
        double inc, sign;
        int retval = 0;

        double[] ftemp = new double[n];
        double[] utemp = new double[n];
        VectorUtils.copy(u, utemp);
        for( int j = 0; j < n; j++ )
        {
            double ujsaved = utemp[j];
            sign = ( ujsaved >= 0 ) ? 1 : -1;
            inc = sqrtRelfunc * Math.max(Math.abs(ujsaved), 1.0 / uscale[j]) * sign;
            utemp[j] += inc;
            retval = func(utemp, ftemp);
            nfeDQ++;
            if( retval != 0 )
                break;
            utemp[j] = ujsaved;
            VectorUtils.scaleDiff(1.0 / inc, ftemp, f, Jac.cols[j]);
        }
        return retval;
    }

}
