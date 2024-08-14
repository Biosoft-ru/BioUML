package biouml.plugins.simulation.ae._test;

import biouml.plugins.simulation.ae.AeModel;
import biouml.plugins.simulation.ae.KinSolver;
import junit.framework.TestCase;

public class KinSolTest extends TestCase
{
    /*
     *
     * This example solves a nonlinear system from.
     *
     * Source: "Handbook of Test Problems in Local and Global Optimization",
     *             C.A. Floudas, P.M. Pardalos et al.
     *             Kluwer Academic Publishers, 1999.
     * Test problem 4 from Section 14.1, Chapter 14: Ferraris and Tronconi
     * 
     * This problem involves a blend of trigonometric and exponential terms.
     *    0.5 sin(x1 x2) - 0.25 x2/pi - 0.5 x1 = 0
     *    (1-0.25/pi) ( exp(2 x1)-e ) + e x2 / pi - 2 e x1 = 0
     * such that
     *    0.25 <= x1 <=1.0
     *    1.5 <= x2 <= 2 pi
     * 
     * The treatment of the bound constraints on x1 and x2 is done using
     * the additional variables
     *    l1 = x1 - x1_min >= 0
     *    L1 = x1 - x1_max <= 0
     *    l2 = x2 - x2_min >= 0
     *    L2 = x2 - x2_max >= 0
     * 
     * and using the constraint feature in KINSOL to impose
     *    l1 >= 0    l2 >= 0
     *    L1 <= 0    L2 <= 0
     * 
     * The Ferraris-Tronconi test problem has two known solutions.
     * The nonlinear system is solved by KINSOL using different
     * combinations of globalization and Jacobian update strategies
     * and with different initial guesses (leading to one or the other
     * of the known solutions).
     *
     *
     * Constraints are imposed to make all components of the solution
     * positive.
     * -----------------------------------------------------------------
     */
    KinSolver solver;

    public static final double FTOL = 1E-9; /* function tolerance */
    public static final double STOL = 1E-5; /* step tolerance */
    StringBuffer result = new StringBuffer();

    public void test() throws Exception
    {

        double[] u1 = getInitialGuess1();
        double[] u2 = getInitialGuess2();
        double[] u3 = getInitialGuess();

        double[] c = new double[6]; //constraints array
        c[0] = 0; /* no constraint on x1 */
        c[1] = 0; /* no constraint on x2 */
        c[2] = 1; /* l1 = x1 - x1_min >= 0 */
        c[3] = -1; /* L1 = x1 - x1_max <= 0 */
        c[4] = 1; /* l2 = x2 - x2_min >= 0 */
        c[5] = -1; /* L2 = x2 - x22_min <= 0 */

        //        PrintHeader(FTOL, STOL);
        //
        //        result.append("\n------------------------------------------\n");
        //        result.append("\nInitial guess on lower bounds\n");
        //        result.append("  [x1,x2] = ");

        solveIt(new Func(), u1, c, KinSolver.NONE, 1);
        solveIt(new Func(), u1, c, KinSolver.LINESEARCH, 1);
        solveIt(new Func(), u1, c, KinSolver.NONE, 0);
        solveIt(new Func(), u1, c, KinSolver.LINESEARCH, 0);

        //        result.append("\n------------------------------------------\n");
        //        result.append("\nInitial guess in middle of feasible region\n");
        //        result.append("  [x1,x2] = ");
        //        PrintOutput(u2);

        solveIt(new Func(), u2, c, KinSolver.NONE, 1);
        solveIt(new Func(), u2, c, KinSolver.LINESEARCH, 1);
        solveIt(new Func(), u2, c, KinSolver.NONE, 0);
        solveIt(new Func(), u2, c, KinSolver.LINESEARCH, 0);

        //out for result demonstration
        //        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("C://KinSolTest.txt")));
        //        bw.write(result.toString());
        //        bw.close();

    }


    int solveIt(AeModel model, double[] u, double[] c, int glstr, int mset) throws Exception
    {
        //        result.append("\n");

        //        if( mset == 1 )
        //            result.append("Exact Newton");
        //        else
        //            result.append("Modified Newton");
        //
        //        if( glstr == KinSolver.NONE )
        //            result.append("\n");
        //        else
        //            result.append(" with line search\n");
        //        PrintOutput(u);
        solver = new KinSolver(model, u);
        solver.setIntitalGuess(u);
        solver.setConstraints(c);
        solver.setFtol(FTOL);
        solver.setStol(STOL);
        solver.setStartegy(glstr);
        solver.setMaxSetups(mset);
        
        solver.start();


        //        result.append("Solution:\n  [x1,x2] = ");
        //        PrintOutput(solver.getY());

        double[] check = model.solveAlgebraic(solver.getY());

        for( double ch : check )
        {
            assertEquals(ch, 0, FTOL);
        }
        //        PrintFinalStats();

        return 0;

    }

    /*
     *--------------------------------------------------------------------
     * FUNCTIONS CALLED BY KINSOL
     *--------------------------------------------------------------------
     */


    public static class SimpleF implements AeModel
    {
        @Override
        public double[] solveAlgebraic(double[] y)
        {
            double[] f = new double[2];
            f[0] = y[0];
            f[1] = y[0] + y[1] - 4;
            return f;
        }

        @Override
        public double[] getConstraints()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public double[] getInitialGuess()
    {
        return new double[] {1, 3};
    }

    public static class Func implements AeModel
    {
        @Override
        public double[] solveAlgebraic(double[] y)
        {
            double[] f = new double[6];
            double x1 = y[0];
            double x2 = y[1];
            double l1 = y[2];
            double L1 = y[3];
            double l2 = y[4];
            double L2 = y[5];

            f[0] = 0.5 * Math.sin(x1 * x2) - 0.25 * x2 / Math.PI - 0.5 * x1;
            f[1] = ( 1 - 0.25 / Math.PI ) * ( Math.exp(2 * x1) - Math.E ) + Math.E * x2 / Math.PI - 2 * Math.E * x1;
            f[2] = l1 - x1 + 0.25;
            f[3] = L1 - x1 + 1;
            f[4] = l2 - x2 + 1.5;
            f[5] = L2 - x2 + 2 * Math.PI;
            return f;
        }

        @Override
        public double[] getConstraints()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    /*
     * Initial guesses
     */
    static double[] getInitialGuess1()
    {
        /* There are two known solutions for this problem */
        double[] u = new double[6];
        /* this init. guess should take us to (0.29945; 2.83693) */
        double x1 = 0.25;
        double x2 = 1.5;

        u[0] = x1;
        u[1] = x2;
        u[2] = x1 - 0.25;
        u[3] = x1 - 1;
        u[4] = x2 - 1.5;
        u[5] = x2 - 2 * Math.PI;
        return u;
    }

    static double[] getInitialGuess2()
    {
        /* There are two known solutions for this problem */
        double[] u = new double[6];
        /* this init. guess should take us to (0.5; 3.1415926) */
        double x1 = 0.5 * ( 0.25 + 1 );
        double x2 = 0.5 * ( 1.5 + 2 * Math.PI );

        u[0] = x1;
        u[1] = x2;
        u[2] = x1 - 0.25;
        u[3] = x1 - 1;
        u[4] = x2 - 1.5;
        u[5] = x2 - 2 * Math.PI;
        return u;
    }

    /*
     * Print first lines of output (problem description)
     */

    void printHeader(double fnormtol, double scsteptol)
    {
        result.append("\nFerraris and Tronconi test problem\n");
        result.append("Tolerance parameters:\n");
        result.append("  fnormtol  = " + fnormtol + "\n  scsteptol = " + scsteptol + "\n");

    }
    /*
     * Print solution
     */

    void printOutput(double[] u)
    {
        result.append(u[0] + "  " + u[1] + "\n");
    }
    /*
     * Print final statistics contained in iopt
     */

    void printFinalStats()
    {
        result.append("Final Statistics:\n");
        result.append("  nni = " + solver.getNonLinearIterationsNumber() + "    nfe  = " + solver.getRHSFunctionCallNumbers() + "\n");
        result.append("  nje = " + solver.nje + "   nfeD = " + solver.nfeDQ + "\n");
    }
}
