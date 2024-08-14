package biouml.plugins.simulation.ode._test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import junit.framework.TestCase;

import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.ode.jvode.IterativeSolver;
import biouml.plugins.simulation.ode.jvode.JVodeSpbgs;
import biouml.plugins.simulation.ode.jvode.JVodeSpgmr;
import biouml.plugins.simulation.ode.jvode.JVodeSptfqmr;
import biouml.plugins.simulation.ode.jvode.Matrix;
import biouml.plugins.simulation.ode.jvode.MatrixUtils;
import biouml.plugins.simulation.ode.jvode.VectorUtils;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.Method;


/*
 * -----------------------------------------------------------------
 * $Revision: 1.20 $
 * $Date: 2013/11/07 05:27:34 $
 * -----------------------------------------------------------------
 * Programmer(s): Scott D. Cohen, Alan C. Hindmarsh and
 *                Radu Serban @ LLNL
 *
 * This example loops through the available iterative linear solvers:
 * SPGMR, SPBCG and SPTFQMR.
 *
 * Example problem:
 *
 * An ODE system is generated from the following 2-species diurnal
 * kinetics advection-diffusion PDE system in 2 space dimensions:
 *
 * dc(i)/dt = Kh*(d/dx)^2 c(i) + V*dc(i)/dx + (d/dy)(Kv(y)*dc(i)/dy)
 *                 + Ri(c1,c2,t)      for i = 1,2,   where
 *   R1(c1,c2,t) = -q1*c1*c3 - q2*c1*c2 + 2*q3(t)*c3 + q4(t)*c2 ,
 *   R2(c1,c2,t) =  q1*c1*c3 - q2*c1*c2 - q4(t)*c2 ,
 *   Kv(y) = Kv0*exp(y/5) ,
 * Kh, V, Kv0, q1, q2, and c3 are constants, and q3(t) and q4(t)
 * vary diurnally. The problem is posed on the square
 *   0 <= x <= 20,    30 <= y <= 50   (all in km),
 * with homogeneous Neumann boundary conditions, and for time t in
 *   0 <= t <= 86400 sec (1 day).
 * The PDE system is treated by central differences on a uniform
 * 10 x 10 mesh, with simple polynomial initial profiles.
 * The problem is solved with CVODE, with the BDF/GMRES,
 * BDF/Bi-CGStab, and BDF/TFQMR methods (i.e. using the CVSPGMR,
 * CVSPBCG and CVSPTFQMR linear solvers) and the block-diagonal
 * part of the Newton matrix as a left preconditioner. A copy of
 * the block-diagonal part of the Jacobian is saved and
 * conditionally reused within the Precond routine.
 * -----------------------------------------------------------------
 */


public class JVodeIterativeDemo extends TestCase
{
    public JVodeIterativeDemo(String name)
    {
        super(name);
    }

    /* Problem Constants */
    private static final int NUM_SPECIES = 2; /* number of species         */
    private static final double KH = 4.0e-6; /* horizontal diffusivity Kh */
    private static final double VEL = 0.001; /* advection velocity V      */
    private static final double KV0 = 1.0e-8; /* coefficient in Kv(y)      */
    private static final double Q1 = 1.63e-16; /* coefficients q1, q2, c3   */
    private static final double Q2 = 4.66e-16;
    private static final double C3 = 3.7e16;
    private static final double A3 = 22.62; /* coefficient in expression for q3(t) */
    private static final double A4 = 7.601; /* coefficient in expression for q4(t) */
    private static final double C1_SCALE = 1.0e6; /* coefficients in initial profiles    */
    private static final double C2_SCALE = 1.0e12;

    private static final double T0 = 0; /* initial time */
    private static final int NOUT = 12; /* number of output times */
    private static final double TWOHR = 7200.0; /* number of seconds in two hours  */
    private static final double HALFDAY = 4.32e4; /* number of seconds in a half day */
    private static final double PI = 3.1415926535898; /* pi */

    private static final double XMIN = 0; /* grid boundaries in x  */
    private static final double XMAX = 20.0;
    private static final double YMIN = 30.0; /* grid boundaries in y  */
    private static final double YMAX = 50.0;
    private static final double XMID = 10.0; /* grid midpoints in x,y */
    private static final double YMID = 40.0;

    private static final int MX = 10; /* MX = number of x mesh points */
    private static final int MY = 10; /* MY = number of y mesh points */
    private static final int NSMX = 20; /* NSMX = NUM_SPECIES*MX */
    private static final int MM = ( MX * MY ); /* MM = MX*MY */

    /* CVodeInit Constants */
    private static final double RTOL = 1.0e-5; /* scalar relative tolerance */
    private static final double FLOOR = 100.0; /* value of C1 or C2 at which tolerances */
    /* change from relative to absolute      */
    private static final double ATOL = ( RTOL * FLOOR ); /* scalar absolute tolerance */
    private static final int NEQ = ( NUM_SPECIES * MM ); /* NEQ = number of equations */

    /* Linear Solver Loop Constants */
    public final static int USE_SPGMR = 0;
    public final static int USE_SPBCG = 1;
    public final static int USE_SPTFQMR = 2;

    private static String path = "C://iterat.txt";

    private static BufferedWriter bw;

    //User data Load problem constants in data */
    private static final double om = PI / HALFDAY;
    private static final double dx = ( XMAX - XMIN ) / ( MX - 1 );
    private static final double dy = ( YMAX - YMIN ) / ( MY - 1 );
    private static final double hdco = KH / Math.pow(dx, 2);
    private static final double haco = VEL / ( 2.0 * dx );
    private static final double vdco = ( 1.0 / Math.pow(dy, 2) ) * KV0;

    private static double q4;


    /* IJKth is defined in order to isolate the translation from the
       mathematical 3-dimensional structure of the dependent variable vector
       to the underlying 1-dimensional storage. IJth is defined in order to
       write code which indexes into dense matrices with a (row,column)
       pair, where 1 <= row, column <= NUM_SPECIES.
       
       IJKth(vdata,i,j,k) references the element in the vdata array for
       species i at mesh point (j,k), where 1 <= i <= NUM_SPECIES,
       0 <= j <= MX-1, 0 <= k <= MY-1. The vdata array is obtained via
       the macro call vdata = NV_DATA_S(v), where v is an N_Vector.
       For each mesh point (j,k), the elements for species i and i+1 are
       contiguous within vdata.

       IJth(a,i,j) references the (i,j)th entry of the matrix realtype **a,
       where 1 <= i,j <= NUM_SPECIES. The small matrix routines in
       sundials_dense.h work with matrices stored by column in a 2-dimensional
       array. In C, arrays are indexed starting at 0, not 1. */


    public static double getIJKth(double[] v, int i, int j, int k)
    {
        return v[i - 1 + j * NUM_SPECIES + k * NSMX];
    }
    public static void setIJKth(double[] v, int i, int j, int k, double val)
    {
        v[i - 1 + j * NUM_SPECIES + k * NSMX] = val;
    }

    /*
      *-------------------------------
      * Main Program
      *-------------------------------
      */

    public void test() throws Exception
    {
        File file = new File(path);
        FileWriter fw = new FileWriter(file);
        bw = new BufferedWriter(fw);

        /* START: Loop through SPGMR, SPBCG and SPTFQMR linear solver modules */
        for( int linsolver = 0; linsolver < 3; ++linsolver )
        {
            /* initialize user data */
            Function f = new Function();

            IterativeSolver solver;
            /* Attach a linear solver module */
            switch( linsolver )
            {
                /* (a) SPGMR */
                case ( USE_SPGMR ):
                {
                    bw.write(" -------");
                    bw.write(" \n| SPGMR |\n");
                    bw.write(" -------\n");
                    solver = new JVodeSpgmr(Method.BDF, f, f.getInitialValues(), T0, IterativeSolver.PREC_LEFT, 0);
                    solver.setTolerances(RTOL, ATOL);
                    solver.setGSType(IterativeSolver.MODIFIED_GS);
                    break;
                }


                    /* (b) SPBCG */
                case ( USE_SPBCG ):
                {
                    bw.write(" -------");
                    bw.write(" \n| SPBCG |\n");
                    bw.write(" -------\n");
                    solver = new JVodeSpbgs(Method.BDF, f, f.getInitialValues(), T0, IterativeSolver.PREC_LEFT, 0);
                    solver.setTolerances(RTOL, ATOL);
                    break;
                }
                    /* (c) SPTFQMR */
                case ( USE_SPTFQMR ):
                {
                    /* Print header */
                    bw.write(" ---------");
                    bw.write(" \n| SPTFQMR |\n");
                    bw.write(" ---------\n");

                    solver = new JVodeSptfqmr(Method.BDF, f, f.getInitialValues(), T0, IterativeSolver.PREC_LEFT, 0);
                    solver.setTolerances(RTOL, ATOL);
                    break;
                }
                default:
                    throw new Exception();
            }

            /* Set preconditioner setup and solve routines Precond and PSolve,
               and the pointer to the user-defined block data */
            solver.setBandPreconditioner(NEQ, 2, 2);

            /* In loop over output points, call CVode, print results, test for error */
            bw.write(" \n2-species diurnal advection-diffusion problem\n\n");
            double tout = TWOHR;
            for( int iout = 1; iout <= NOUT; iout++, tout += TWOHR )
            {
                solver.start(tout);
                double[] u = solver.getY();
                printOutput(solver, u);
            }
            printFinalStats(solver, linsolver);

        } /* END: Loop through SPGMR, SPBCG and SPTFQMR linear solver modules */

        bw.close();
    }


    /* f routine. Compute RHS function f(t,u). */
    public static class Function extends JavaBaseModel
    {
        @Override
        public double[] dy_dt(double t, double[] u)
        {
            double q3, c1, c2, c1dn, c2dn, c1up, c2up, c1lt, c2lt;
            double c1rt, c2rt, cydn, cyup, hord1, hord2, horad1, horad2;
            double qq1, qq2, qq3, qq4, rkin1, rkin2, s, vertd1, vertd2, ydn, yup;

            double[] result = new double[u.length];
            int idn, iup, ileft, iright;

            //UserData data = (UserData)user_data;

            /* Set diurnal rate coefficients. */
            s = Math.sin(om * t);
            if( s > 0 )
            {
                q3 = Math.exp( -A3 / s);
                q4 = Math.exp( -A4 / s);
            }
            else
            {
                q3 = 0.0;
                q4 = 0.0;
            }

            /* Make local copies of problem variables, for efficiency. */

            /* Loop over all grid points. */
            for( int jy = 0; jy < MY; jy++ )
            {

                /* Set vertical diffusion coefficients at jy +- 1/2 */

                ydn = YMIN + ( jy - 0.5 ) * dy;
                yup = ydn + dy;
                cydn = vdco * Math.exp(0.2 * ydn);
                cyup = vdco * Math.exp(0.2 * yup);
                idn = ( jy == 0 ) ? 1 : -1;
                iup = ( jy == MY - 1 ) ? -1 : 1;
                for( int jx = 0; jx < MX; jx++ )
                {

                    /* Extract c1 and c2, and set kinetic rate terms. */

                    c1 = getIJKth(u, 1, jx, jy);
                    c2 = getIJKth(u, 2, jx, jy);
                    qq1 = Q1 * c1 * C3;
                    qq2 = Q2 * c1 * c2;
                    qq3 = q3 * C3;
                    qq4 = q4 * c2;
                    rkin1 = -qq1 - qq2 + 2 * qq3 + qq4;
                    rkin2 = qq1 - qq2 - qq4;

                    /* Set vertical diffusion terms. */

                    c1dn = getIJKth(u, 1, jx, jy + idn);
                    c2dn = getIJKth(u, 2, jx, jy + idn);
                    c1up = getIJKth(u, 1, jx, jy + iup);
                    c2up = getIJKth(u, 2, jx, jy + iup);
                    vertd1 = cyup * ( c1up - c1 ) - cydn * ( c1 - c1dn );
                    vertd2 = cyup * ( c2up - c2 ) - cydn * ( c2 - c2dn );

                    /* Set horizontal diffusion and advection terms. */

                    ileft = ( jx == 0 ) ? 1 : -1;
                    iright = ( jx == MX - 1 ) ? -1 : 1;
                    c1lt = getIJKth(u, 1, jx + ileft, jy);
                    c2lt = getIJKth(u, 2, jx + ileft, jy);
                    c1rt = getIJKth(u, 1, jx + iright, jy);
                    c2rt = getIJKth(u, 2, jx + iright, jy);
                    hord1 = hdco * ( c1rt - 2 * c1 + c1lt );
                    hord2 = hdco * ( c2rt - 2 * c2 + c2lt );
                    horad1 = haco * ( c1rt - c1lt );
                    horad2 = haco * ( c2rt - c2lt );

                    /* Load all terms into udot. */
                    setIJKth(result, 1, jx, jy, vertd1 + hord1 + horad1 + rkin1);
                    setIJKth(result, 2, jx, jy, vertd2 + hord2 + horad2 + rkin2);
                }
            }
            return result;
        }
      
        @Override
        public double[] getInitialValues()
        {
            double x, y, cx, cy;
            double[] u = new double[NEQ];
            /* Load initial profiles of c1 and c2 into u vector */
            for( int jy = 0; jy < MY; jy++ )
            {
                y = YMIN + jy * dy;
                cy = Math.pow( ( 0.1 ) * ( y - YMID ), 2);
                cy = 1.0 - cy + 0.5 * Math.pow(cy, 2);
                for( int jx = 0; jx < MX; jx++ )
                {
                    x = XMIN + jx * dx;
                    cx = Math.pow(0.1 * ( x - XMID ), 2);
                    cx = 1.0 - cx + 0.5 * Math.pow(cx, 2);
                    setIJKth(u, 1, jx, jy, C1_SCALE * cx * cy);
                    setIJKth(u, 2, jx, jy, C2_SCALE * cx * cy);
                }
            }
            return u;
        }
    }


    public static class UserPrecondition
    {
        double[][][][] P = new double[MX][MY][][];
        double[][][][] Jbd = new double[MX][MY][][];
        int[][][] pivot = new int[MX][MY][];

        public UserPrecondition()
        {
            for( int jx = 0; jx < MX; jx++ )
            {
                for( int jy = 0; jy < MY; jy++ )
                {
                    P[jx][jy] = MatrixUtils.newDenseMat(NUM_SPECIES, NUM_SPECIES);
                    Jbd[jx][jy] = MatrixUtils.newDenseMat(NUM_SPECIES, NUM_SPECIES);
                    pivot[jx][jy] = new int[NUM_SPECIES];
                }
            }
        }

        /* Preconditioner setup routine. Generate and preprocess P. */
        public int setup(double tn, double[] u, double[] fu, boolean jok, double gamma)
        {
            double c1, c2, cydn, cyup, diag, ydn, yup;
            int ier;
            ;

            /* Make local copies of pointers in user_data, and of pointer to u's data */
            if( jok )
            {
                /* jok = TRUE: Copy Jbd to P */
                for( int jy = 0; jy < MY; jy++ )
                {
                    for( int jx = 0; jx < MX; jx++ )
                    {
                        Matrix.denseCopy(Jbd[jx][jy], P[jx][jy], NUM_SPECIES, NUM_SPECIES);
                    }
                }
            }

            else
            {
                /* jok = FALSE: Generate Jbd from scratch and copy to P */

                /* Compute 2x2 diagonal Jacobian blocks (using q4 values
                   computed on the last f call).  Load into P. */
                for( int jy = 0; jy < MY; jy++ )
                {
                    ydn = YMIN + ( jy - 0.5 ) * dy;
                    yup = ydn + dy;
                    cydn = vdco * Math.exp(0.2 * ydn);
                    cyup = vdco * Math.exp(0.2 * yup);
                    diag = - ( cydn + cyup + 2 * hdco );
                    for( int jx = 0; jx < MX; jx++ )
                    {
                        c1 = getIJKth(u, 1, jx, jy);
                        c2 = getIJKth(u, 2, jx, jy);
                        //double[][] j = Jbd[jx][jy];
                        Jbd[jx][jy][0][0] = -Q1 * C3 - Q2 * c2 + diag;
                        Jbd[jx][jy][1][0] = -Q2 * c1 + q4;
                        Jbd[jx][jy][0][1] = Q1 * C3 - Q2 * c2;
                        Jbd[jx][jy][1][1] = -Q2 * c1 - q4 + diag;
                        Matrix.denseCopy(Jbd[jx][jy], P[jx][jy], NUM_SPECIES, NUM_SPECIES);
                    }
                }
            }

            /* Scale by -gamma */
            for( int jy = 0; jy < MY; jy++ )
            {
                for( int jx = 0; jx < MX; jx++ )
                {
                    Matrix.denseScale( -gamma, P[jx][jy], NUM_SPECIES, NUM_SPECIES);
                }
            }

            /* Add identity matrix and do LU decompositions on blocks in place. */

            for( int jx = 0; jx < MX; jx++ )
            {
                for( int jy = 0; jy < MY; jy++ )
                {
                    MatrixUtils.denseAddIdentity(P[jx][jy], NUM_SPECIES);
                    ier = MatrixUtils.denseGETRF(P[jx][jy], NUM_SPECIES, NUM_SPECIES, pivot[jx][jy]);
                    if( ier != 0 )
                        return ( 1 );
                }
            }
            return ( 0 );
        }

        /* Preconditioner solve routine */

        public int solve(double tn, double[] u, double[] fu, double[] r, double[] z, double gamma, double delta, int lr, double[] vtemp)
        {
            /* Extract the P and pivot arrays from user_data. */
            // VectorUtils.N_VScale(1, r, z);
            VectorUtils.copy(r, z);
            double[] v;
            /* Solve the block-diagonal system Px = r using LU factors stored
               in P and pivot data in pivot, and return the solution in z. */
            for( int jx = 0; jx < MX; jx++ )
            {
                for( int jy = 0; jy < MY; jy++ )
                {
                    v = new double[NUM_SPECIES];
                    for( int i = 0; i < v.length; i++ )
                    {
                        v[i] = getIJKth(z, i + 1, jx, jy);
                    }
                    MatrixUtils.denseGETRS(P[jx][jy], NUM_SPECIES, pivot[jx][jy], v);
                    for( int i = 0; i < v.length; i++ )
                    {
                        setIJKth(z, i + 1, jx, jy, v[i]);
                    }
                }
            }

            return ( 0 );
        }

    }


    /* Print current t, step count, order, stepsize, and sampled c1,c2 values */

    static void printOutput(IterativeSolver cv, double[] u) throws Exception
    {
        int mxh = MX / 2 - 1, myh = MY / 2 - 1, mx1 = MX - 1, my1 = MY - 1;
        bw.write("t = " + cv.getTime() + "   no. steps = " + cv.nSteps + "   order = " + cv.qu + "   stepsize = " + cv.hu + "\n");
        bw.write("c1 (bot.left/middle/top rt.) = " + getIJKth(u, 1, 0, 0) + "  " + getIJKth(u, 1, mxh, myh) + "  "
                + getIJKth(u, 1, mx1, my1) + "\n");
        bw.write("c2 (bot.left/middle/top rt.) = " + getIJKth(u, 2, 0, 0) + "  " + getIJKth(u, 2, mxh, myh) + "  "
                + getIJKth(u, 2, mx1, my1) + "\n\n");
    }

    /* Get and print final statistics */
    static void printFinalStats(IterativeSolver cv, int linsolver) throws Exception
    {
        bw.write("\nFinal Statistics.. \n\n");
        bw.write("nst     = " + cv.nSteps + "\n");
        bw.write("nfe     = " + cv.nFCalls + "     nfeLS   = " + cv.s_nfes + "\n");
        bw.write("nni     = " + cv.nNewtonIter + "     nli     = " + cv.s_nli + "\n");
        bw.write("nsetups = " + cv.nSetupCalls + "     netf    = " + cv.nTestFails + "\n");
        bw.write("npe     = " + cv.s_npe + "     nps     = " + cv.s_nps + "\n");
        bw.write("ncfn    = " + cv.nCorrFails + "     ncfl    = " + cv.s_ncfl + "\n");

        if( linsolver < 2 )
            bw.write("======================================================================\n\n");
    }
}