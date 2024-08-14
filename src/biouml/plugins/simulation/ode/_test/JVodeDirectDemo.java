package biouml.plugins.simulation.ode._test;

/*
 * -----------------------------------------------------------------
 * $Revision: 1.18 $
 * $Date: 2013/06/26 03:45:22 $
 * -----------------------------------------------------------------
 * Programmer(s): Scott D. Cohen, Alan C. Hindmarsh and
 *                Radu Serban @ LLNL
 * -----------------------------------------------------------------
 * Demonstration program for CVODE - direct linear solvers.
 * Two separate problems are solved using both the CV_ADAMS and CV_BDF
 * linear multistep methods in combination with CV_FUNCTIONAL and
 * CV_NEWTON iterations:
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
 *
 * For each problem, in the series of eight runs, CVodeInit is
 * called only once, for the first run, whereas CVodeReInit is
 * called for each of the remaining seven runs.
 *
 * Notes: This program demonstrates the usage of the sequential
 * macros NV_Ith_S, NV_DATA_S, DENSE_ELEM, BAND_COL, and
 * BAND_COL_ELEM. The NV_Ith_S macro is used to reference the
 * components of an N_Vector. It works for any size N=NEQ, but
 * due to efficiency concerns it should only by used when the
 * problem size is small. The Problem 1 right hand side and
 * Jacobian functions f1 and Jac1 both use NV_Ith_S. The NV_DATA_S
 * macro gives the user access to the memory used for the component
 * storage of an N_Vector. In the sequential case, the user may
 * assume that this is one contiguous array of reals. The NV_DATA_S
 * macro gives a more efficient means (than the NV_Ith_S macro) to
 * access the components of an N_Vector and should be used when the
 * problem size is large. The Problem 2 right hand side function f2
 * uses the NV_DATA_S macro. The DENSE_ELEM macro used in Jac1
 * gives access to an element of a dense matrix of type DlsMat.
 * It should be used only when the problem size is small (the size
 * of a DlsMat is NEQ x NEQ) due to efficiency concerns. For
 * larger problem sizes, the macro DENSE_COL can be used in order
 * to work directly with a column of a DlsMat. The BAND_COL and
 * BAND_COL_ELEM allow efficient columnwise access to the elements
 * of a band matrix of type DlsMat. These macros are used in the
 * Jac2 function.
 * -----------------------------------------------------------------
 */
import java.io.BufferedWriter;
import java.io.File;
import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.ode.jvode.DirectSolver;
import biouml.plugins.simulation.ode.jvode.JVode;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.plugins.simulation.ode.jvode.JVodeSupport;
import biouml.plugins.simulation.ode.jvode.Matrix;
import biouml.plugins.simulation.ode.jvode.DirectSolver.BandJacobian;
import biouml.plugins.simulation.ode.jvode.DirectSolver.DenseJacobian;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.IterationType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.JacobianType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.Method;

public class JVodeDirectDemo// extends TestCase
{

    /* Shared Problem Constants */
    private static final double ATOL = 1.0e-6;
    private static final double RTOL = 0.0;

    /* Problem #1 Constants */
    private static final int P1_NEQ = 2;
    private static final double P1_ETA = 3.0;
    private static final int P1_NOUT = 4;
    private static final double P1_T0 = 0.0;
    private static final double P1_T1 = 1.39283880203;
    private static final double P1_DTOUT = 2.214773875;
    private static final double P1_TOL_FACTOR = 1.0e4;

    /* Problem #2 Constants */
    private static final int P2_MESHX = 5;
    private static final int P2_MESHY = 5;
    private static final int P2_NEQ = P2_MESHX * P2_MESHY;
    private static final double P2_ALPH1 = 1.0;
    private static final double P2_ALPH2 = 1.0;
    private static final int P2_NOUT = 5;
    private static final int P2_ML = 5;
    private static final int P2_MU = 0;
    private static final double P2_T0 = 0.0;
    private static final double P2_T1 = 0.01;
    private static final double P2_TOUT_MULT = 10.0;
    private static final double P2_TOL_FACTOR = 1.0e3;

    private static boolean printInfo = false;

    private static String path = "C://Documents and Settings/axec/JVodeDirectDemo.txt";

    /* Implementation */
    public static void main(String[] args) throws Exception
    {
        File f = new File(path);
        try(BufferedWriter bw = ApplicationUtils.utfWriter( f ))
        {
            //Problem 1
            if( printInfo )
                printIntro1(bw);
            System.out.println(getTime1(bw, Method.ADAMS, IterationType.FUNCTIONAL, JacobianType.DENSE, false));
            System.out.println(getTime1(bw, Method.ADAMS, IterationType.NEWTON, JacobianType.DENSE, false));
            System.out.println(getTime1(bw, Method.ADAMS, IterationType.NEWTON, JacobianType.BAND, false));
            System.out.println(getTime1(bw, Method.ADAMS, IterationType.NEWTON, JacobianType.DIAG, true));

            System.out.println(getTime1(bw, Method.BDF, IterationType.FUNCTIONAL, JacobianType.DENSE, false));
            System.out.println(getTime1(bw, Method.BDF, IterationType.NEWTON, JacobianType.DENSE, false));
            System.out.println(getTime1(bw, Method.BDF, IterationType.NEWTON, JacobianType.BAND, false));
            System.out.println(getTime1(bw, Method.BDF, IterationType.NEWTON, JacobianType.DIAG, true));

            if( printInfo )
                printIntro2(bw);
            System.out.println(getTime2(bw, Method.ADAMS, IterationType.FUNCTIONAL, JacobianType.DENSE, false));
            System.out.println(getTime2(bw, Method.ADAMS, IterationType.NEWTON, JacobianType.DENSE, false));
            System.out.println(getTime2(bw, Method.ADAMS, IterationType.NEWTON, JacobianType.BAND, false));
            System.out.println(getTime2(bw, Method.ADAMS, IterationType.NEWTON, JacobianType.DIAG, true));

            System.out.println(getTime2(bw, Method.BDF, IterationType.FUNCTIONAL, JacobianType.DENSE, false));
            System.out.println(getTime2(bw, Method.BDF, IterationType.NEWTON, JacobianType.DENSE, false));
            System.out.println(getTime2(bw, Method.BDF, IterationType.NEWTON, JacobianType.BAND, false));
            System.out.println(getTime2(bw, Method.BDF, IterationType.NEWTON, JacobianType.DIAG, true));
        }
    }

    static double getTime1(BufferedWriter bw, Method method, IterationType iter, JacobianType jac, boolean userJacobian) throws Exception
    {
        double t = System.currentTimeMillis();
        for( int i = 0; i < 10000; i++ )
            problem1(bw, method, iter, jac, userJacobian);
        return ( System.currentTimeMillis() - t );
    }


    static double getTime2(BufferedWriter bw, Method method, IterationType iter, JacobianType jac, boolean userJacobian) throws Exception
    {
        double t = System.currentTimeMillis();
        for( int i = 0; i < 10000; i++ )
            problem2(bw, method, iter, jac, userJacobian);
        return ( System.currentTimeMillis() - t );
    }

    static void problem1(BufferedWriter bw, Method method, IterationType iter, JacobianType jac, boolean userJacobian) throws Exception
    {
        double er;
        Function1 f = new Function1();

        double ero = 0;

        JVode solver = JVode.createJVode(new JVodeOptions(method, iter, jac), P1_T0, f.getInitialValues(), f);

        solver.setTolerances(RTOL, ATOL);

        if( printInfo )
        {
            printHeader(bw, method, iter, jac, userJacobian);
            printHeader1(bw);
        }

        double tout = P1_T1;
        for( int iout = 1; iout <= P1_NOUT; iout++, tout += P1_DTOUT )
        {
            solver.start(tout);
            double[] y = solver.getY();

            if( printInfo )
                printOutput1(bw, solver.getTime(), y[0], y[1], solver.qu, solver.hu);

            if( iout % 2 == 0 )
            {
                er = Math.abs(y[0]) / ATOL;
                if( er > ero )
                    ero = er;

                if( printInfo )
                    if( er > P1_TOL_FACTOR )
                    {
                        printErrOutput(bw, P1_TOL_FACTOR);
                    }
            }
        }

        if( printInfo )
            printFinalStats(bw, solver, iter,jac, ero);
    }

    static void printIntro1(BufferedWriter bw) throws Exception
    {
        bw.write("Demonstration program for CVODE package - direct linear solvers\n");
        bw.write("\n\n");
        bw.write("Problem 1: Van der Pol oscillator\n");
        bw.write(" xdotdot - 3*(1 - x^2)*xdot + x = 0, x(0) = 2, xdot(0) = 0\n");
        bw.write(" neq = " + P1_NEQ + ",  reltol = " + RTOL + ",  abstol = " + ATOL);
    }

    static void printHeader1(BufferedWriter bw) throws Exception
    {
        bw.write("\n     t           x              xdot         qu     hu \n");
    }

    static void printOutput1(BufferedWriter bw, double t, double y0, double y1, int qu, double hu) throws Exception
    {
        bw.write(t + "\t" + y0 + "\t" + y1 + "\t" + qu + "\t" + hu + "\n");
    }

    public static class Function1 extends JavaBaseModel
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
        public void init()
        {
        }


        @Override
        public double[] getInitialValues()
        {
            return new double[] {2, 0};
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
            J.setDenseElement(1, 0, -2 * P1_ETA * y0 * y1 - 1);
            J.setDenseElement(1, 1, P1_ETA * ( 1 - y0 * y0 ));
            return 0;
        }
    }

    static void problem2(BufferedWriter bw, Method method, IterationType iter, JacobianType jac, boolean userJacobian) throws Exception
    {
        double er, erm, ero;

        Function2 f = new Function2();
        ero = 0;
        JVode solver = JVode.createJVode(new JVodeOptions(method, iter, jac), P2_T0, f.getInitialValues(), f);

        solver.setTolerances(RTOL, ATOL);

        if( printInfo )
        {
            printHeader(bw, method, iter, jac, userJacobian);
            printHeader2(bw);
        }

        double tout = P2_T1;
        for( int iout = 1; iout <= P2_NOUT; iout++, tout *= P2_TOUT_MULT )
        {
            solver.start(tout);

            erm = maxError(solver.getY(), solver.getTime());

            if( printInfo )
                printOutput2(bw, solver.getTime(), erm, solver.qu, solver.hu);

            er = erm / ATOL;
            if( er > ero )
                ero = er;

            if( printInfo )
                if( er > P2_TOL_FACTOR )
                {
                    printErrOutput(bw, P2_TOL_FACTOR);
                }
        }

        if( printInfo )
            printFinalStats(bw, solver, iter,jac, ero);
    }

    static void printIntro2(BufferedWriter bw) throws Exception
    {
        bw.write("\n\n-------------------------------------------------------------");
        bw.write("\n-------------------------------------------------------------");
        bw.write("\n\nProblem 2: ydot = A * y, where A is a banded lower\n");
        bw.write("triangular matrix derived from 2-D advection PDE\n\n");
        bw.write(" neq = " + P2_NEQ + ", ml = " + P2_ML + ", mu = " + P2_MU);
        bw.write(" itol = " + "CV_SS" + ", reltol = " + RTOL + ", abstol = " + ATOL);
        bw.write("\n      t        max.err      qu     hu \n");
    }

    static void printHeader2(BufferedWriter bw) throws Exception
    {
        bw.write("\n      t        max.err      qu     hu \n");
    }

    static void printOutput2(BufferedWriter bw, double t, double erm, int qu, double hu) throws Exception
    {
        bw.write(t + "\t" + erm + "\t" + qu + "\t" + hu + "\n");
    }

    public static class Function2 extends JavaBaseModel
    {

        @Override
        public double[] dy_dt(double t, double[] x)
        {
            int k;
            double d;
            double[] result = new double[x.length];
            /*
               Excluding boundaries,

               ydot    = f    = -2 y    + alpha1 * y      + alpha2 * y
                   i,j    i,j       i,j             i-1,j             i,j-1
            */

            for( int j = 0; j < P2_MESHY; j++ )
            {
                for( int i = 0; i < P2_MESHX; i++ )
                {
                    k = i + j * P2_MESHX;
                    d = -2 * x[k];
                    if( i != 0 )
                        d += P2_ALPH1 * x[k - 1];
                    if( j != 0 )
                        d += P2_ALPH2 * x[k - P2_MESHX];
                    result[k] = d;
                }
            }

            return result;
        }

        @Override
        public double[] getInitialValues()
        {
            double[] vals = new double[P2_NEQ];
            vals[0] = 1;
            return vals;
        }

        @Override
        public void init()
        {
            // TODO Auto-generated method stub

        }
    }

    public static class Jac2 implements BandJacobian
    {
        @Override
        public int getValue(int N, int mu, int ml, double tn, double[] y, double[] fy, Matrix J)
        {
            int k;

            /*
               The components of f(t,y) which depend on y    are
                                                         i,j
               f    , f      , and f      :
                i,j    i+1,j        i,j+1

               f    = -2 y    + alpha1 * y      + alpha2 * y
                i,j       i,j             i-1,j             i,j-1

               f      = -2 y      + alpha1 * y    + alpha2 * y
                i+1,j       i+1,j             i,j             i+1,j-1

               f      = -2 y      + alpha1 * y        + alpha2 * y
                i,j+1       i,j+1             i-1,j+1             i,j
            */

            for( int j = 0; j < P2_MESHY; j++ )
            {
                for( int i = 0; i < P2_MESHX; i++ )
                {
                    k = i + j * P2_MESHX;
                    J.setBandElement(k, k, -2);
                    if( i != P2_MESHX - 1 )
                        J.setBandElement(k + 1, k, P2_ALPH1);
                    if( j != P2_MESHY - 1 )
                        J.setBandElement(k + P2_MESHX, k, P2_ALPH2);
                }
            }
            return 0;
        }
    }
    static double maxError(double[] y, double t)
    {
        int k;
        double er;
        double ex = 0;
        double yt;
        double maxError = 0;
        double ifact_inv;
        double jfact_inv = 1;

        if( t == 0 )
            return ( 0 );

        if( t <= 30 )
            ex = Math.exp( -2 * t);

        for( int j = 0; j < P2_MESHY; j++ )
        {
            ifact_inv = 1;
            for( int i = 0; i < P2_MESHX; i++ )
            {
                k = i + j * P2_MESHX;
                yt = Math.pow(t, i + j) * ex * ifact_inv * jfact_inv;
                er = Math.abs(y[k] - yt);
                if( er > maxError )
                    maxError = er;
                ifact_inv /= ( i + 1 );
            }
            jfact_inv /= ( j + 1 );
        }
        return maxError;
    }

    static void printHeader(BufferedWriter bw, Method method, IterationType iter, JacobianType jac, boolean userJacobian) throws Exception
    {
        bw.write("\n\n-------------------------------------------------------------");
        bw.write("\n\nLinear Multistep Method : ");
        if( method == Method.ADAMS )
        {
            bw.write("ADAMS\n");
        }
        else
        {
            bw.write("BDF\n");
        }

        bw.write("Iteration               : ");

        bw.write("NEWTON\n");
        bw.write("Linear Solver           : ");

        if( iter == IterationType.FUNCTIONAL )
        {
            bw.write("FUNCTIONAL\n");
        }
        else
        {
            switch( jac )
            {
                case DENSE:
                    if( userJacobian )
                        bw.write("Dense, User-Supplied Jacobian\n");
                    else
                        bw.write("Dense, Difference Quotient Jacobian\n");
                    break;
                case DIAG:
                    bw.write("Diagonal Jacobian\n");
                    break;
                case BAND:
                    if( userJacobian )
                        bw.write("Band, User-Supplied Jacobian\n");
                    else
                        bw.write("Band, Difference Quotient Jacobian\n");
                    break;
            }
        }
    }

    static void printErrOutput(BufferedWriter bw, double tol_factor) throws Exception
    {
        bw.write("\n\n Error exceeds " + tol_factor + " * tolerance \n\n");
        return;
    }

    static void printFinalStats(BufferedWriter bw, JVodeSupport jvode,IterationType iter, JacobianType jac, double ero) throws Exception
    {
        int nje;
        bw.write("\n Final statistics for this run:\n\n");
        bw.write(" Number of steps                          =" + jvode.nSteps + "\n");
        bw.write(" Number of f-s                            =" + jvode.nFCalls + "\n");
        bw.write(" Number of setups                         =" + jvode.nSetupCalls + "\n");
        bw.write(" Number of nonlinear iterations           =" + jvode.nNewtonIter + "\n");
        bw.write(" Number of nonlinear convergence failures =" + jvode.nTestFails + "\n");
        bw.write(" Number of error test failures            =" + jvode.nCorrFails + "\n");

        if( iter != IterationType.FUNCTIONAL )
        {
            if( jac == JacobianType.DIAG )
            {
                nje = jvode.nSteps;
            }
            else
            {
                nje = ( (DirectSolver)jvode ).nJacCalls;
            }
            int nfeLS = ( (DirectSolver)jvode ).nfCallsDQ;
            bw.write(" Number of Jacobian evaluations           = " + nje + "\n");
            bw.write(" Number of f evals. in linear solver      = " + nfeLS + "\n\n");
        }
        bw.write(" Error overrun = " + ero + " \n");
    }
}
