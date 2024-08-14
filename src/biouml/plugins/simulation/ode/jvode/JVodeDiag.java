package biouml.plugins.simulation.ode.jvode;

import biouml.plugins.simulation.ode.OdeModel;

public class JVodeDiag extends DirectSolver
{

    /* Additional last_flag values */
    public static final int CVDIAG_INV_FAIL = -5;
    public static final int CVDIAG_RHSFUNC_UNRECVR = -6;
    public static final int CVDIAG_RHSFUNC_RECVR = -7;


    protected JVodeDiag(Method method, OdeModel f, double[] u0, double t0)
    {
        super(method, f, u0, t0);

        iterationType = IterationType.NEWTON;
        jacobianType = JacobianType.BAND;
        iM = new double[n];
    }

    double gammaSaved; /* gammasv = gamma at the last call to setup or solve */
    double[] iM; /* M = (I - gamma J)^{-1} , gamma = h / l1 */

    @Override
    public int init()
    {
        return 0;
    }

    /**
     * This routine does the setup operations for the diagonal linear solver. It
     * constructs a diagonal approximation to the Newton matrix M = I - gamma*J,
     * updates counters, and inverts M.
     */
    @Override
    public int setup(int convfail)
    {
        // Form y with perturbation = FRACT*(func. iter. correction)
        double r = 0.1 * rl1;
        int n = ftemp.length;
        for( int i = 0; i < n; i++ )
        {
            acor[i] = h * ftemp[i] - z[1][i];
            y[i] = r * acor[i] + z[0][i];
        }

        /* Evaluate f at perturbed y */
        try
        {
            nfCallsDQ++;
            iM = f.dy_dt(tn, y);
        }
        catch( IllegalArgumentException ex )
        {
            lastFlag = CVDIAG_RHSFUNC_RECVR;
            return 1;
        }
        catch( Exception ex )
        {
            processError("The right-hand side routine failed in an unrecoverable manner.");
            lastFlag = CVDIAG_RHSFUNC_UNRECVR;
            return -1;

        }

        // Construct M = I - gamma*J with J = diag(deltaf_i/deltay_i)
        for( int i = 0; i < n; i++ )
        {
            iM[i] = ( 0.1 * acor[i] ) - h * (iM[i] - ftemp[i]);
            y[i] = acor[i] * errorWeight[i];

            // Protect against deltay_i being at roundoff level
            if(Math.abs(y[i]) >= UROUND)
            {
                y[i] = acor[i] * 0.1;
                iM[i] = iM[i] / y[i];
            } else
            {
                y[i] = -1;
                iM[i] = -1;
            }
        }

        // Invert M with test for zero components
        boolean invOK = VectorUtils.invTest(iM, iM);
        if( !invOK )
        {
            lastFlag = CVDIAG_INV_FAIL;
            return 1;
        }

        // Set currentJacobian = true, save gamma in gammasv, and return */
        currentJacobian = true;
        gammaSaved = gamma;
        lastFlag = SUCCESS;
        return SUCCESS;
    }

    /**
    *This
     * routine performs the solve operation for the diagonal linear solver. If
     * necessary it first updates gamma in M = I - gamma*J.
     */

    @Override
    public int solve(double[] b)
    {
        /* If gamma has changed, update factor in M, and save gamma value */
        if( gammaSaved != gamma )
        {
            double r = gamma / gammaSaved;
            int n = iM.length;
            for( int i = 0; i < n; i++ )
            {
                iM[i] = (1.0 / iM[i] - 1.0) * r + 1.0;
            }
            boolean invOK = VectorUtils.invTest(iM, iM);
            if( !invOK )
            {
                lastFlag = CVDIAG_INV_FAIL;
                return 1;
            }
            gammaSaved = gamma;
        }
        /* Apply M-inverse to b */
        VectorUtils.prod(b, iM, b);
        lastFlag = SUCCESS;
        return 0;
    }
}
