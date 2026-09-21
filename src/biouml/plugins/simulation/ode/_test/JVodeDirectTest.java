package biouml.plugins.simulation.ode._test;

import junit.framework.TestCase;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.ode.jvode.JVode;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.plugins.simulation.ode.jvode.Matrix;
import biouml.plugins.simulation.ode.jvode.DirectSolver.DenseJacobian;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.IterationType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.JacobianType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.Method;

/**
 * Test for ported version of CVODE - JVode
 * Source:
 * -----------------------------------------------------------------
 * $Revision: 1.15 $
 * $Date: 2013/06/26 03:45:22 $
 * -----------------------------------------------------------------
 * Programmer(s): Scott D. Cohen, Alan C. Hindmarsh and
 *                Radu Serban @ LLNL
 * -----------------------------------------------------------------
 *
 * Problem 1: Van der Pol oscillator
 *   xdotdot - 3*(1 - x^2)*xdot + x = 0, x(0) = 2, xdot(0) = 0.
 * This second-order ODE is converted to a first-order system by
 * defining y0 = x and y1 = xdot.
 * The NEWTON iteration cases use the following types of Jacobian
 * approximation: (1) dense, user-supplied, (2) dense, difference
 * quotient approximation, (3) diagonal approximation.
 *
 * Problem 2: ydot = A * y, where A is a banded lower triangular
 * matrix derived from 2-D advection PDE.
 * The NEWTON iteration cases use the following types of Jacobian
 * approximation: (1) band, user-supplied, (2) band, difference
 * quotient approximation, (3) diagonal approximation.
 * -----------------------------------------------------------------
 */
public class JVodeDirectTest extends TestCase
{
    public JVodeDirectTest(String name)
    {
        super(name);
    }

    private static final double ACCURACY = 1e-4; // relative tolerance for x/xdot values

    /* Shared Problem Constants */
    private static final double ATOL = 1.0e-6;
    private static final double RTOL = 0.0;

    /* Problem #1 Constants */
    private static final int P1_NOUT = 4;
    private static final double P1_T0 = 0.0;
    private static final double P1_T1 = 1.39283880203;
    private static final double P1_DTOUT = 2.214773875;
    private static final double P1_TOL_FACTOR = 1.0e4;


    public void test() throws Exception
    {
        test(IterationType.FUNCTIONAL, Method.ADAMS);
        test(IterationType.NEWTON, Method.ADAMS);

        test(IterationType.FUNCTIONAL, Method.BDF);
        test(IterationType.NEWTON, Method.BDF);
    }

    static void test(IterationType iterationMethod, Method solverMethod) throws Exception
    {
        double[] t = new double[P1_NOUT];
        double[] x = new double[P1_NOUT];
        double[] xdot = new double[P1_NOUT];
        double err = 0;

        Function f = new Function();

        JVode solver = JVode.createJVode(new JVodeOptions(solverMethod, iterationMethod, JacobianType.DENSE), P1_T0, f.getInitialValues(), f);

        solver.setTolerances(RTOL, ATOL);

        double tout = P1_T1;
        for( int i = 0; i < P1_NOUT; i++, tout += P1_DTOUT )
        {
            solver.start(tout);

            double[] y = solver.getY();
            x[i] = y[0];
            xdot[i] = y[1];
            t[i] = solver.getTime();

            if( i % 2 != 0 )
            {
                err = Math.max(err, Math.abs(y[0]) / ATOL);
                if( err > P1_TOL_FACTOR )
                {
                    throw new Exception("Test failed: error = " + err + " exceeds " + P1_TOL_FACTOR + " * tolerance");

                }
            }
        }
        Values result = new Values(t, x, xdot, err, solver.nSteps);
        Values control = getControlValues(iterationMethod, solverMethod);

        checkResult(result, control, ACCURACY);
    }
    public static class Function extends JavaBaseModel
    {

        @Override
        public double[] dy_dt(double t, double[] x)
        {
            double[] result = new double[x.length];
            result[0] = x[1];
            result[1] = ( 1 - Math.pow(x[0], 2) ) * 3 * x[1] - x[0];
            return result;
        }

        @Override
        public double[] getInitialValues()
        {
           return new double[] {2, 0};
        }

        @Override
        public void init()
        {
            // TODO Auto-generated method stub

        }

    }

    public static class Jac1 implements DenseJacobian
    {
        @Override
        public int getValue(int N, double tn, double[] y, double[] fy, Matrix J)
        {
            double y0 = y[0];
            double y1 = y[1];
            J.setDenseElement(0, 1, 1);
            J.setDenseElement(1, 0, -2 * 3 * y0 * y1 - 1);
            J.setDenseElement(1, 1, 3 * ( 1 - Math.pow(y0, 2) ));
            return 0;
        }
    }

    static class Values
    {
        double[] t;
        double[] x;
        double[] xdot;
        double error;
        long steps;

        Values()
        {

        }

        Values(double[] t, double[] x, double[] xdot, double error, long steps)
        {
            this.t = t;
            this.x = x;
            this.xdot = xdot;
            this.error = error;
            this.steps = steps;
        }
    }

    static void checkResult(Values v1, Values v2, double accuracy)
    {
        checkResult(v1.x, v2.x, accuracy);
        checkResult(v1.xdot, v2.xdot, accuracy);
        // error is relative (|y|/atol), so allow proportional tolerance
        double maxErr = Math.max(Math.abs(v1.error), Math.abs(v2.error));
        assert ( Math.abs(v1.error - v2.error) <= Math.max(accuracy, maxErr * 0.15) );
        // step count can vary by platform/JVM; allow up to 20% difference
        long maxSteps = Math.max(v1.steps, v2.steps);
        assert ( Math.abs(v1.steps - v2.steps) <= Math.max(10, (long)(maxSteps * 0.2)) );
    }

    static void checkResult(double[] a, double[] b, double accuracy)
    {
        for( int i = 0; i < a.length; i++ )
        {
            assert ( Math.abs(a[i] - b[i]) <= accuracy );
        }
    }

    static Values getControlValues(IterationType iterationMethod, Method method) throws Exception
    {
        Values values;
        if( method == Method.ADAMS )
        {
            switch( iterationMethod )
            {
                case FUNCTIONAL:
                    values = new Values(t, x1, xdot1, error1, steps1);
                    break;
                case NEWTON:
                    values = new Values(t, x3, xdot3, error3, steps3);
                    break;
                default:
                    throw new Exception("Wrong jacobian approximation method: " + iterationMethod);
            }
        }
        else
        {
            switch( iterationMethod )
            {
                case FUNCTIONAL:
                    values = new Values(t, x5, xdot5, error5, steps5);
                    break;
                case NEWTON:
                    values = new Values(t, x7, xdot7, error7, steps7);
                    break;
                default:
                    throw new Exception("Wrong jacobian approximation method: " + iterationMethod);
            }
        }
        return values;
    }

    //CONTROL VALUES
    static double[] t = new double[] {1.39283880203, 3.60761267703, 5.82238655203, 8.03716042703};


    //Adams Functional
    static double[] x1 = new double[] {1.6801023268162343, -1.1861896705690766E-5, -1.6801010556258285, 1.1076385628447372E-4};
    static double[] xdot1 = new double[] { -0.29105548459826336, -3.1687486335316604, 0.2910610779376795, 3.1690500560730768};
    static double error1 = 110.76385628447372;
    static int steps1 = 327;

    //Adams Dense
    static double[] x2 = new double[] {1.6801024914994949, 2.4294320297348765E-6, -1.6801020717060329, 1.9907829835925234E-5};
    static double[] xdot2 = new double[] { -0.29105610508219454, -3.1687028628489116, 0.29106192103916884, 3.1687870874873694};
    static double error2 = 22.80464233304913;
    static int steps2 = 195;

    //Adams Dense User
    static double[] x3 = new double[] {1.6801023324987119, -2.280464233304913E-5, -1.6801031814320986, -9.847417806714033E-6};
    static double[] xdot3 = new double[] { -0.2910562035069947, -3.168786050656946, 0.2910592128267901, 3.168689938743011};
    static double error3 = 22.80464233304913;
    static int steps3 = 195;

    //Adams Diagonal
    static double[] x4 = new double[] {1.6801028550625958, 6.360710201329736E-5, -1.6801099344755124, -6.970491212767766E-5};
    static double[] xdot4 = new double[] { -0.29105373522546096, -3.1685313383642386, 0.29105699239019434, 3.1685094427765765};
    static double error4 = 69.70491212767767;
    static int steps4 = 240;

    //BDF Functional
    static double[] x5 = new double[] {1.6801022589245145, -1.256408564830041E-4, -1.6800907858589438, 2.703108856823228E-4};
    static double[] xdot5 = new double[] { -0.291058922780968, -3.16908846600444, 0.29106389001081107, 3.169526128885115};
    static double error5 = 270.3108856823228;
    static int steps5 = 294;

    //BDF Dense
    static double[] x6 = new double[] {1.68010208127005, -5.4690696524133976E-5, -1.6800964491466883, 1.543115999549996E-4};
    static double[] xdot6 = new double[] { -0.29105594974445903, -3.1688572832856274, 0.29106099846831673, 3.1691698746411983};
    static double error6 = 154.31159995499962;
    static int steps6 = 265;

    //BDF
    static double[] x7 = new double[] {1.6801023762603131, -5.8419984958963875E-5, -1.6800954684967382, 9.617368543085592E-5};
    static double[] xdot7 = new double[] { -0.29105753762301745, -3.1688630547584564, 0.2910618955189015, 3.16898522247514};
    static double error7 = 96.17368543085593;
    static int steps7 = 276;

    static double[] x8 = new double[] {1.6801022106991073, -9.835005997029488E-5, -1.6800935075000067, 1.6663961591930482E-4};
    static double[] xdot8 = new double[] { -0.2910560914329728, -3.168997073351836, 0.291062759351555, 3.1691980738208763};
    static double error8 = 166.63961591930482;
    static int steps8 = 266;
}
