package biouml.plugins.simulation.ode.jvode;

import biouml.plugins.simulation.ode.OdeModel;

public abstract class DirectSolver extends JVode
{

    /* Additional last_flag values */

    protected final static int JACFUNC_UNRECVR = -5;
    protected final static int JACFUNC_RECVR = -6;


    protected final static int MAX_STEPS_BETWEEN_JAC = 50; //maximum number of steps between Jacobian evaluations
    protected final static double MAX_GAMMA_CHANGE_BETWEEN_JAC = 0.2; //maximum change in gamma between Jacobian evaluations

    Matrix matr; /* M = I - gamma * df/dy                        */
    Matrix savedJ; /* savedJ = old Jacobian                        */

    int[] pivots; /* pivots = pivot array for PM = LU             */

    int nStepsAtLastJac; /* nstlj = nst at last Jacobian eval.           */
    public int nJacCalls; /* nje = no. of calls to jac                    */
    public int nfCallsDQ; /* no. of calls to f due to DQ Jacobian approx. */
    int lastFlag; /* last error return flag                       */

    public DirectSolver(Method method, OdeModel f, double[] u0, double t0)
    {
        super(method, f, u0, t0);
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
    public static interface DenseJacobian
    {
        public int getValue(int N, double t, double[] y, double[] fy, Matrix Jac);
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
    public static interface BandJacobian
    {
        public int getValue(int N, int mupper, int mlower, double t, double[] y, double[] fy, Matrix Jac);

    }
}
