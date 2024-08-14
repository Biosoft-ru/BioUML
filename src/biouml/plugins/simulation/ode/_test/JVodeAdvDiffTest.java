package biouml.plugins.simulation.ode._test;

/*
 * -----------------------------------------------------------------
 * $Revision: 1.7 $
 * $Date: 2013/06/26 03:45:22 $
 * -----------------------------------------------------------------
 * Programmer(s): Scott D. Cohen, Alan C. Hindmarsh and
 *                Radu Serban @ LLNL
 * -----------------------------------------------------------------
 * Example problem:
 *
 * The following is a simple example problem with a banded Jacobian,
 * with the program for its solution by CVODE.
 * The problem is the semi-discrete form of the advection-diffusion
 * equation in 2-D:
 *   du/dt = d^2 u / dx^2 + .5 du/dx + d^2 u / dy^2
 * on the rectangle 0 <= x <= 2, 0 <= y <= 1, and the time
 * interval 0 <= t <= 1. Homogeneous Dirichlet boundary conditions
 * are posed, and the initial condition is
 *   u(x,y,t=0) = x(2-x)y(1-y)exp(5xy).
 * The PDE is discretized on a uniform MX+2 by MY+2 grid with
 * central differencing, and with boundary values eliminated,
 * leaving an ODE system of size NEQ = MX*MY.
 * This program solves the problem with the BDF method, Newton
 * iteration with the CVBAND band linear solver, and a user-supplied
 * Jacobian routine.
 * It uses scalar relative and absolute tolerances.
 * Output is printed at t = .1, .2, ..., 1.
 * Run statistics (optional outputs) are printed at the end.
 * -----------------------------------------------------------------
 * */
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.ode.jvode.DirectSolver;
import biouml.plugins.simulation.ode.jvode.JVode;
import biouml.plugins.simulation.ode.jvode.JVodeBand;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.plugins.simulation.ode.jvode.Matrix;
import biouml.plugins.simulation.ode.jvode.DirectSolver.BandJacobian;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.IterationType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.JacobianType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.Method;


public class JVodeAdvDiffTest
{
    /* Problem Constants */
    public static final double XMAX = 2.0; /* domain boundaries         */
    public static final double YMAX = 1.0;
    public static final int MX = 10; /* mesh dimensions           */
    public static final int MY = 5;
    public static final int NEQ = MX * MY; /* number of equations       */
    public static final double ATOL = 1.0e-5; /* scalar absolute tolerance */
    public static final double T0 = 0.0; /* initial time              */
    public static final double T1 = 0.1; /* first output time         */
    public static final double DTOUT = 0.1; /* output time increment     */
    public static final int NOUT = 10; /* number of output times    */

    public static final double ZERO = 0.0;
    public static final double HALF = 0.5;
    public static final double ONE = 1.0;
    public static final double TWO = 2.0;
    public static final double FIVE = 5.0;

    private static final String path = "C:/cvAdvDiff_bnd.out";

    public static double getIJth(double[] vdata, int i, int j)
    {
        return vdata[ ( j - 1 ) + ( i - 1 ) * MY];
    }
    public static double setIJth(double[] vdata, int i, int j, double val)
    {
        return vdata[ ( j - 1 ) + ( i - 1 ) * MY] = val;
    }

    public static final double dx = XMAX / ( MX + 1 );
    public static final double dy = YMAX / ( MY + 1 );
    public static final double hdcoef = 1.0 / ( dx * dx );
    public static final double hacoef = 0.5 / ( 2 * dx );
    public static final double vdcoef = 1.0 / ( dy * dy );

    /*
     *-------------------------------
     * Main Program
     *-------------------------------
     */

    public static void main(String[] args) throws Exception
    {

        F f = new F();
        f.init();
        JVodeOptions options = new JVodeOptions(Method.BDF, IterationType.NEWTON, JacobianType.BAND);
        options.setMl(MY);
        options.setMu(MY);
        JVode jvode = JVode.createJVode(options, T0, f.getInitialValues(), f);
        
        ((JVodeBand)jvode).setUserJacobian(new Jac());
        
        System.out.println(StrictMath.nextAfter(1.0, 0) - 1);
        jvode.setTolerances(0, ATOL);

        //StringBuffer output = new StringBuffer();
        // double umax = VectorUtils.maxNorm(u);
        //output.append(PrintHeader(0, ATOL, umax));
        double tout = T1;

        for( int iout = 1; iout <= NOUT; iout++, tout += DTOUT )
        {
            jvode.start(tout);
            //umax = VectorUtils.maxNorm(u);
            //long nst = jvode.nSteps;
            //output.append(PrintOutput(jvode.getTime(), umax, nst));
        }
        //output.append(PrintFinalStats(jvode)); /* Print some final statistics   */
        //try
        //{
        //    File file = new File(path);
        //    FileWriter fw = new FileWriter(file);
        // bw = new BufferedWriter(fw);
        // bw.write(output.toString());
        // bw.close();
        // fw.close();
        // }
        //catch( Exception e )
        // {
        //     System.out.print("Can't open file!");
        //}
    }


    public static class F extends JavaBaseModel
    {

        @Override
        public double[] dy_dt(double t, double[] u)
        {
            double[] du = new double[NEQ];
            double uij, udn, uup, ult, urt, hdiff, hadv, vdiff;

            /* Loop over all grid points. */

            for( int j = 1; j <= MY; j++ )
            {
                for( int i = 1; i <= MX; i++ )
                {
                    /* Extract u at x_i, y_j and four neighboring points */

                    uij = getIJth(u, i, j);
                    udn = ( j == 1 ) ? ZERO : getIJth(u, i, j - 1);
                    uup = ( j == MY ) ? ZERO : getIJth(u, i, j + 1);
                    ult = ( i == 1 ) ? ZERO : getIJth(u, i - 1, j);
                    urt = ( i == MX ) ? ZERO : getIJth(u, i + 1, j);

                    /* Set diffusion and advection terms and load into udot */
                    hdiff = hdcoef * ( ult - TWO * uij + urt );
                    hadv = hacoef * ( urt - ult );
                    vdiff = vdcoef * ( uup - TWO * uij + udn );
                    setIJth(du, i, j, hdiff + hadv + vdiff);
                }
            }

            return du;
        }

        @Override
        public void init()
        {


        }

        @Override
        public double[] getInitialValues()
        {
            double x, y;
            double[] u = new double[NEQ];

            for( int j = 1; j <= MY; j++ )
            {
                y = j * dy;
                for( int i = 1; i <= MX; i++ )
                {
                    x = i * dx;
                    setIJth(u, i, j, x * ( XMAX - x ) * y * ( YMAX - y ) * Math.exp(FIVE * x * y));
                }
            }
            return u;
        }
    }

    /* Jacobian routine. Compute J(t,u). */

    public static class Jac implements BandJacobian
    {
        @Override
        public int getValue(int N, int mu, int ml, double t, double[] u, double[] fu, Matrix J)
        {
            int k;

            /*
              The components of f = udot that depend on u(i,j) are
              f(i,j), f(i-1,j), f(i+1,j), f(i,j-1), f(i,j+1), with
                df(i,j)/du(i,j) = -2 (1/dx^2 + 1/dy^2)
                df(i-1,j)/du(i,j) = 1/dx^2 + .25/dx  (if i > 1)
                df(i+1,j)/du(i,j) = 1/dx^2 - .25/dx  (if i < MX)
                df(i,j-1)/du(i,j) = 1/dy^2           (if j > 1)
                df(i,j+1)/du(i,j) = 1/dy^2           (if j < MY)
            */

            for( int j = 1; j <= MY; j++ )
            {
                for( int i = 1; i <= MX; i++ )
                {
                    k = j - 1 + ( i - 1 ) * MY;

                    J.setBandElement(k, k, -TWO * ( vdcoef + hdcoef ));
                    if( i != 1 )
                        J.setBandElement(k - MY, k, hdcoef + hacoef);
                    if( i != MX )
                        J.setBandElement(k + MY, k, hdcoef - hacoef);
                    if( j != 1 )
                        J.setBandElement(k - 1, k, vdcoef);
                    if( j != MY )
                        J.setBandElement(k + 1, k, vdcoef);
                }
            }

            return ( 0 );
        }
    }
    /*
     *-------------------------------
     * Private helper functions
     *-------------------------------
     */



    /* Print first lines of output (problem description) */

    static String printHeader(double reltol, double abstol, double umax)
    {
        StringBuffer buf = new StringBuffer("\n2-D Advection-Diffusion Equation\n");
        buf.append("Mesh dimensions = ");
        buf.append(MX);
        buf.append(" X ");
        buf.append(MY);
        buf.append("\nTotal system size = ");

        buf.append(NEQ);
        buf.append("\nTolerance parameters: reltol = ");
        buf.append(reltol);
        buf.append(" abstol = ");
        buf.append(abstol);
        buf.append("\nAt t = ");
        buf.append(T0);
        buf.append(" max.norm(u) = ");
        buf.append(umax);

        return buf.toString();
    }

    /* Print current value */

    static String printOutput(double t, double umax, long nst)
    {
        StringBuffer buf = new StringBuffer("\n  At t = ");
        buf.append(t);
        buf.append(" max.norm(u) = ");
        buf.append(umax);
        buf.append(" nst = ");
        buf.append(nst);
        return buf.toString();
    }

    /*
     * Get and print some final statistics
     */
    static String printFinalStats(JVode jv)
    {
        StringBuffer buf = new StringBuffer("\nFinal Statistics:\n");
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
        //buf.append(" netf = ");
        //buf.append(jv.n);
        buf.append(" nge = ");
        buf.append(jv.nEventFuncionCalls);


        return buf.toString();
    }

}
