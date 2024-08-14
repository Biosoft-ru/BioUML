package biouml.plugins.simulation.ode._test;

import java.io.BufferedWriter;
import java.io.File;
import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.TestCase;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.ode.jvode.DirectSolver;
import biouml.plugins.simulation.ode.jvode.JVode;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.plugins.simulation.ode.jvode.Matrix;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.IterationType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.JacobianType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.Method;


/**
 * Test for JVode Events
 * Source:
 * -----------------------------------------------------------------
 * $Revision: 1.15 $
 * $Date: 2013/06/26 03:42:58 $
 * -----------------------------------------------------------------
 * Programmer(s): Scott D. Cohen, Alan C. Hindmarsh and
 *                Radu Serban @ LLNL
 * -----------------------------------------------------------------
 * Example problem:
 * 
 * The following is a simple example problem, with the coding
 * needed for its solution by CVODE. The problem is from
 * chemical kinetics, and consists of the following three rate
 * equations:
 *    dy1/dt = -.04*y1 + 1.e4*y2*y3
 *    dy2/dt = .04*y1 - 1.e4*y2*y3 - 3.e7*(y2)^2
 *    dy3/dt = 3.e7*(y2)^2
 * on the interval from t = 0.0 to t = 4.e10, with initial
 * conditions: y1 = 1.0, y2 = y3 = 0. The problem is stiff.
 * While integrating the system, we also use the rootfinding
 * feature to find the points at which y1 = 1e-4 or at which
 * y3 = 0.01. This program solves the problem with the BDF method,
 * Newton iteration with the CVDENSE dense linear solver, and a
 * user-supplied Jacobian routine.
 * It uses a scalar relative tolerance and a vector absolute
 * tolerance. Output is printed in decades from t = .4 to t = 4.e10.
 * Run statistics (optional outputs) are printed at the end.
 * -----------------------------------------------------------------
 */
public class JVodeEventTest extends TestCase
{
    public JVodeEventTest(String name)
    {
        super(name);
    }


    private static final double RTOL = 1.0e-4; /* scalar relative tolerance            */
    private static final double ATOL1 = 1.0e-8; /* vector absolute tolerance components */
    private static final double T0 = 0; /* initial time           */

    private static String path = "C://Roberts_dns_result.txt"; // result file


    /*
     *-------------------------------
     * Main Program
     *-------------------------------
     */

    public void test() throws Exception
    {
        File file = new File(path);
        try(BufferedWriter bw = ApplicationUtils.utfAppender( file ))
        {
            JavaBaseModel f = new F();
            f.init();
            JVode solver = JVode.createJVode(new JVodeOptions(Method.BDF, IterationType.NEWTON, JacobianType.DENSE), T0, f
                    .getInitialValues(), f);
            solver.setTolerances(RTOL, ATOL1);
            bw.write("\n3-species kinetics problem\n\n");

            ArraySpan span = new ArraySpan(0, 5, 0.2);

            for( int i=0; i<span.getLength(); i++)
            {
                double time = span.getTime(i);
                
                int flag = solver.start(time);

                bw.write(makeResultString(solver));

                if( flag == JVode.ROOT_RETURN )
                {
                    bw.write(printRootInfo(solver));
                    solver.getY()[0] = 1;
                }
            }

            /* Print some final statistics */
            bw.write(printFinalStats(solver));
        }
    }

    static class F extends JavaBaseModel
    {
        private double time;
        @Override
        public double[] dy_dt(double t, double[] y)
        {
            double[] ydot = new double[y.length];
            ydot[0] = -0.04 * y[0] + 1.0e4 * y[1] * y[2];
            ydot[2] = 3.0e7 * y[1] * y[1];
            ydot[1] = -ydot[0] - ydot[2];
            return ydot;
        }


        @Override
        public double[] getInitialValues()
        {
            return new double[] {1, 0, 0};
        }


        @Override
        public void init()
        {

        }

        @Override
        public double[] checkEvent(double t, double[] y)
        {
            double[] result = new double[1];
            if( y[0] > 0.9 )
                result[0] = -1;
            if( y[0] < 0.9 )
                result[0] = 1;
            return result;
        }

        public double[] getEvent(double time, double[] x)
        {
            this.time = time;


            double[] flagsv3 = new double[1];
            if( x[0] / 1 < 0.1 )
                flagsv3[0] = 1;
            else if( x[0] / 1 > 0.1 )
                flagsv3[0] = -1;
            else
                flagsv3[0] = 0;
            return flagsv3;
        }
    }

    static class Jac implements DirectSolver.DenseJacobian
    {
        @Override
        public int getValue(int N, double t, double[] y, double[] fy, Matrix J)
        {
            double y1 = y[1];
            double y2 = y[2];
            J.cols[0][0] = -0.04;
            J.cols[1][0] = 1.0e4 * y2;
            J.cols[2][0] = 1.0e4 * y1;
            J.cols[0][1] = 0.04;
            J.cols[1][1] = -1.0e4 * y2 - 6.0e7 * y1;
            J.cols[2][1] = -1.0e4 * y1;
            J.cols[1][2] = 6.0e7 * y1;
            return 0;
        }
    }

    /*
     *-------------------------------
     * Private helper functions
     *-------------------------------
     */

    static String makeResultString(JVode solver)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("At t = ");
        buf.append(solver.getTime());
        buf.append(" y = ");
        for( double y : solver.getY() )
        {
            buf.append(y);
            buf.append("\t");
        }
        buf.append("\n");
        return buf.toString();
    }


    static String printRootInfo(JVode solver)
    {
        int[] roots = solver.getEventInfo();
        StringBuilder buf = new StringBuilder("rootsfound[] = ");
        for( int root : roots )
        {
            buf.append(root);
            buf.append("\t");
        }
        buf.append("\n");
        return buf.toString();
    }

    /*
     * Get and print some final statistics
     */
    static String printFinalStats(JVode jv)
    {
        StringBuilder buf = new StringBuilder("\nFinal Statistics:\n");
        buf.append("nst = ");
        buf.append(jv.nSteps);
        buf.append(" nfe = ");
        buf.append(jv.nFCalls);
        buf.append(" nsetups = ");
        buf.append(jv.nSetupCalls);
        buf.append(" nfeLS = ");
        buf.append(jv.nTestFails);
        buf.append(" nje = ");
        buf.append( ( (DirectSolver)jv ).nJacCalls);
        buf.append(" nni = ");
        buf.append(jv.nNewtonIter);
        buf.append(" ncfn = ");
        buf.append(jv.nCorrFails);
        buf.append(" nge = ");
        buf.append(jv.nEventFuncionCalls);


        return buf.toString();
    }
}