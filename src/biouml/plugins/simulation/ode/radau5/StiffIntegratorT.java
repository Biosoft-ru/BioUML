package biouml.plugins.simulation.ode.radau5;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

public class StiffIntegratorT extends IntegratorT
{
    //constants
    private static final double t11 = 9.1232394870892942792e-02;
    private static final double t12 = -0.14125529502095420843;
    private static final double t13 = -3.0029194105147424492e-02;
    private static final double t21 = 0.24171793270710701896;
    private static final double t22 = 0.20412935229379993199;
    private static final double t23 = 0.38294211275726193779;
    private static final double t31 = 0.96604818261509293619;
    private static final double ti11 = 4.3255798900631553510;
    private static final double ti12 = 0.33919925181580986954;
    private static final double ti13 = 0.54177053993587487119;
    private static final double ti21 = -4.1787185915519047273;
    private static final double ti22 = -0.32768282076106238708;
    private static final double ti23 = 0.47662355450055045196;
    private static final double ti31 = -0.50287263494578687595;
    private static final double ti32 = 2.5719269498556054292;
    private static final double ti33 = -0.59603920482822492497;

    private static final double sq6 = Math.sqrt(6.0);
    private static final double c1 = ( 4.0 - sq6 ) / 10.0;
    private static final double c2 = ( 4.0 + sq6 ) / 10.0;
    private static final double c1m1 = c1 - 1.0;
    private static final double c2m1 = c2 - 1.0;
    private static final double c1mc2 = c1 - c2;
    private static final double u1 = 1.0 / ( ( 6.0 + Math.pow(81.0, 1.0 / 3.0) - Math.pow(9.0, 1.0 / 3.0) ) / 30.0 );
    private static final double preAlph = ( 12.0 - Math.pow(81.0, 1.0 / 3.0) + Math.pow(9.0, 1.0 / 3.0) ) / 60.0;
    private static final double preBeta = ( Math.pow(81.0, 1.0 / 3.0) + Math.pow(9.0, 1.0 / 3.0) ) * Math.sqrt(3.0) / 60.0;
    private static final double cno = preAlph * preAlph + preBeta * preBeta;
    private static final double alph = preAlph / cno;
    private static final double beta = preBeta / cno;


    private static final int MAS_IDENTITY_JAC_FULL = 1;
    private static final int MAS_IDENTITY_JAC_BAND = 2;
    private static final int MAS_BAND_JAC_FULL = 3;
    private static final int MAS_BAND_JAC_BAND = 4;
    private static final int MAS_FULL_JAC_FULL = 5;
    private static final int MAS_IDENTITY_JAC_FULL_HESS = 7;


    // number of non-zero diagonals below main diagonal of Jacobian matrix
    //(if mljac = dimx, matrix is full)
    int mljac;
    // number of non-zero diagonals above main diagonal of Jacobian matrix
    int mujac;
    // differential equation is in explicit form (implicitEquation = 0) or not (implicitEquation = 1)
    boolean implicitEquation;
    // number of non-zero diagonals below main diagonal of mass matrix
    //(if mlmas = dimx, matrix is full)
    int mlmas;
    // number of non-zero diagonals above main diagonal of mass matrix
    int mumas;
    // maximal number of Newton iterations
    int nit;
    // switch for starting values of Newton iterations
    boolean startn;
    // parameters for differential-algebraic components
    int nind1;
    int nind2;
    int nind3;
    // step size control
    boolean predictGustaffson;
    // parameters for second order equations
    int m1;
    int m2;
    int nm1;

    // stopping criterion for Newton's method, usually chosen < 1
    double fnewt;
    // quot1 and quot2--if quot1 < hnew/hold < quot2, step size = const
    double quot1;
    double quot2;
    // decides whether the Jacobian should be recomputed
    double thet;
    // banded or not?
    boolean jband;
    // row-dimensions of the 2-D arrays -- fjac, e1, e2, and fmas
    int ldjac;
    int lde1;
    int ldmas;
    // job number (used in linear algebra routines for type of problem--banded or not, etc)
    int ijob;

    // number of jacobian evaluations
    int njac;
    // number of lu-decompositions of both matrices
    int ndec;
    // number of forward-backward substitutions, of both systems;
    // the nstep forward-backward substitutions, needed for step
    //size selection, are not counted
    int nsol;

    // constants that are used in linear algebra routines
    int mle;
    int mue;
    int mbjac;
    int mbb;
    int mdiag;
    int mdiff;
    int mbdiag;

    double fac1;
    double alphn;
    double betan;

    // variables used in program
    double err;
    boolean caljac;
    boolean calhes;
    boolean first;
    boolean reject;

    // arrays used in program
    double[] z1;
    double[] z2;
    double[] z3;
    double[] y0;
    double[] scal;
    double[] f1;
    double[] f2;
    double[] f3;
    double[] cont;
    int[] ip1;
    int[] ip2;
    int[] iphes;
    double[][] e1;
    double[][] e2r;
    double[][] e2i;

    // Jacobian matrix
    double[][] fjac;
    // mass matrix
    double[][] fmas;

    // value of solution on previous step
    double[] yold;

    public int[] events;
    public boolean eventTriggered = false;

    // get number of Jacobian evaluations
    public int NumJacobian()
    {
        return njac;
    }

    // get number of lu-decompositions of both matrices
    public int NumDecomp()
    {
        return ndec;
    }

    // get number of forward-backward substitutions, of both systems;
    public int NumSol()
    {
        return nsol;
    }

    public StiffIntegratorT(Model model, double y[], Span span, double[] rtoler, double[] atoler, Radau5Options options,
            ResultListener[] resultListeners, SimulatorProfile profile) throws Exception
    {
        super(model, y, span, rtoler, atoler, options, resultListeners, profile);
        mljac = options.getMljac();
        mujac = options.getMujac();
        mlmas = options.getMlmas();
        mumas = options.getMumas();
        nit = options.getNit();
        startn = options.isStartn();
        fnewt = options.getFnewt();
        quot1 = options.getQuot1();
        quot2 = options.getQuot2();
        thet = options.getThet();
        njac = 0;
        ndec = 0;
        nsol = 0;
        fac1 = 0;
        alphn = 0;
        betan = 0;
        err = 0;
        caljac = true;
        calhes = true;
        first = true;
        reject = false;

        // Check and change the tolerances
        for( int i = 0; i < n; i++ )
        {
            if( ( atoler[i] <= 0.0 ) || ( rtoler[i] <= 10.0 * UROUND ) )
                throw new IllegalArgumentException("Tolerances i-th elements are too small: atol = " + atoler[i] + ", rtol = " + rtoler[i]);

            double quot = atoler[i] / rtoler[i];
            rtoler[i] = 0.1 * Math.pow(rtoler[i], 2.0 / 3.0);
            atoler[i] = rtoler[i] * quot;
        }

        // initial step length
        if( Math.abs(h) < 10.0 * UROUND )
            h = 1.0e-6;

        // parameters for step size selection
        if( ( facl < 1.0 ) || ( facr > 1.0 ) )
            throw new IllegalArgumentException("Incorrect input facl or facr = " + facl + "   " + facr);

        //maximal number of Newton iterations
        else if( nit <= 0 )
            throw new IllegalArgumentException("Number of newton iterations can not be negative: " + nit);

        // parameters for differential-algebraic components set to constant for now
        // we are not supposed to solve such systems for now
        //        if( nind1 == 0 )
        //            nind1 = n;
        //        if( nind1 + nind2 + nind3 != n )
        //            throw new IllegalArgumentException("Incorrect nind1, nind2, nind3 = " + nind1 + "   " + nind2 + "   " + nind3);
        nind1 = n;

        // pred--step size control
        predictGustaffson = options.isPredictGustafsson();

        //Axec:
        //parameters for implicit systems solving set to constant
        //we are not supposed to solve such systems for now
        //
        ////        if( m1 == 0 )
        //            m2 = n;
        //        if( m2 == 0 )
        //            m2 = m1;
        //        nm1 = n - m1;
        //        if( ( m1 < 0 ) || ( m2 < 0 ) || ( m1 + m2 > n ) )
        //            throw new IllegalArgumentException("Incorrect m1 = " + m1 + " , m2 = " + m2+". Should be m1, m2 >=0 && m1+m2=n, where n = "+n);

        m1 = 0;
        m2 = n;
        nm1 = n;

        // fnewt--stopping criterion for Newton's method, usually chosen < 1
        if( fnewt == 0.0 )
            fnewt = Math.max(10.0 * UROUND / rtoler[0], Math.min(0.03, Math.sqrt(rtoler[0])));
        if( fnewt <= UROUND / rtoler[0] )
            throw new IllegalArgumentException("Too small fnewt = " + fnewt);

        // quot1 and quot2--if quot1 < hnew/hold < quot2, step size = const
        if( ( quot1 > 1.0 ) || ( quot2 < 1.0 ) )
            throw new IllegalArgumentException("Incorrect quot1 = " + quot1 + "(must be <=1) or quot2  = " + quot2 + " (must be >=1). ");

        // thet--decides whether the Jacobian should be recomputed
        else if( thet >= 1.0 )
            throw new IllegalArgumentException("Incorrect thet = " + thet + ". It must be < 1.");

        if( mljac < 0 )
            mljac = n;
        if( mujac < 0 )
            mujac = n;

        // implicit, banded or not?
        jband = ( mljac < nm1 );

        // Computation of the row-dimensions of the 2-D arrays
        // Jacobian and matrices e1, e2
        if( jband )
        {
            ldjac = mljac + mujac + 1;
            lde1 = mljac + ldjac;
        }
        else
        {
            mljac = nm1;
            mujac = nm1;
            ldjac = nm1;
            lde1 = nm1;
        }

        //        if( implicitEquation )
        //        {
        //            if( mlmas != nm1 )
        //            {
        //                ldmas = mlmas + mumas + 1;
        //                ijob = ( jband ) ? 4 : 3;
        //            }
        //            else
        //            {
        //                mumas = nm1;
        //                ldmas = nm1;
        //                ijob = 5;
        //            }
        //        }
        //        else
        //        {
        ldmas = 0;
        if( jband )
        {
            ijob = 2;
        }
        else
        {
            ijob = 1;
            if( n > 2 && options.isHessenberg() )
                ijob = 7;
        }
        //        }
        // for second-order equations increase ijob by 10
        //        if( m1 > 0 )
        //            ijob += 10;
        ldmas = Math.max(1, ldmas);

        if( ( mlmas > mljac ) || ( mumas > mujac ) )
            throw new IllegalArgumentException("bandwith of 'mas' not smaller than bandwith of 'jac'");

        // Define constants used in linear algebra routines
        mle = mljac;
        mue = mujac;
        mbjac = mljac + mujac + 1;
        mbb = mlmas + mumas + 1;
        mdiag = mle + mue;
        mdiff = mle + mue - mumas;
        mbdiag = mumas + 1;

        yold = new double[n];
        z1 = new double[n];
        z2 = new double[n];
        z3 = new double[n];
        y0 = new double[n];
        scal = new double[n];
        f1 = new double[n];
        f2 = new double[n];
        f3 = new double[n];
        cont = new double[4 * n];
        ip1 = new int[nm1];
        ip2 = new int[nm1];
        iphes = new int[n];

        fjac = new double[ldjac][n];

        fmas = new double[ldmas][n];

        e1 = new double[lde1][nm1];
        e2r = new double[lde1][nm1];
        e2i = new double[lde1][nm1];
    }

    protected boolean failed;

    public void doIntegration() throws Exception
    {
        failed = false;
        profile.setUnstable(false);
        int flag = doCoreIntegration();

        profile.setTime(x);
        profile.setX(y);

        handleFlag(flag);

        // restore tolerances
        for( int i = 0; i < n; i++ )
        {
            double quot = atoler[i] / rtoler[i];
            rtoler[i] = Math.pow(10.0 * rtoler[i], 1.5);
            atoler[i] = rtoler[i] * quot;
        }
        return;

    }

    private void handleFlag(int flag)
    {
        if( flag < 0 )
            failed = true;
        switch( flag )
        {
            case SINGULAR_MATRIX:
            {
                outStatistics("Exit of RADAU5 at x = " + x);
                outError("Matrix is repeatedly singular");
                profile.setUnstable(true);
                break;
            }
            case TOO_SMALL_N_MAX:
            {
                outStatistics("Exit of RADAU5 at x = " + x);
                outError("More than nmax = " + nmax + " steps are needed");
                profile.setStiff(true);
                break;
            }
            case TOO_SMALL_STEP_SIZE:
            {
                outStatistics("Exit of RADAU5 at x = " + x);
                outError("Step size too small, h = " + h);
                profile.setStiff(true);
                break;
            }
            case ALGEBRAIC_ROUTINE_FAILED:
            {
                outError("Step size too small, h = " + h);
                profile.setUnstable(true);
                break;
            }
        }
    }

    protected static final int SUCCESS = 1;
    protected static final int SOLUTION_OUTPUT = 2;
    protected static final int ALGEBRAIC_ROUTINE_FAILED = -1;
    protected static final int TOO_SMALL_N_MAX = -2;
    protected static final int TOO_SMALL_STEP_SIZE = -3;
    protected static final int SINGULAR_MATRIX = -4;

    @Override
    protected int doCoreIntegration() throws Exception
    {
        double posneg = Math.signum(xend - x);
        double hmaxn = Math.min(Math.abs(hmax), Math.abs(xend - x));
        double cfac = safe * ( 1 + 2 * nit );

        hold = h = posneg * Math.min(Math.abs(h), hmaxn);

        System.arraycopy(y, 0, yold, 0, n);

        boolean last = false;

        if( ( x + h * 1.0001 - xend ) * posneg >= 0.0 )
        {
            h = xend - x;
            last = true;
        }

        double hopt = h;
        double faccon = 1.0;

        System.arraycopy(y, 0, cont, 0, n);
        //        int irtrn = outSolution();
        //        if( irtrn < 0 )
        //        {
        //            outStatistics("Exit of RADAU5 at x = " + x);
        //            return SOLUTION_OUTPUT;
        //        }

        for( int i = 0; i < n; i++ )
            scal[i] = atoler[i] + rtoler[i] * Math.abs(y[i]);

        y0 = odeModel.dy_dt(x, y);
        nfcn++;

        double hacc = 0, erracc = 0, thqold = 0;
        int nsing = 0, ier = 0;

        // basic integration step
        computeJacobian();
        boolean loop = true;
        while( loop && !eventTriggered )
        {
            loop = false;
            eventTriggered = false;

            System.arraycopy(y, 0, yold, 0, n);
            // compute the matrices e1 and e2 and their decompositions
            fac1 = u1 / h;
            alphn = alph / h;
            betan = beta / h;

            ier = DecompReal();

            if( ier != 0 )
            {
                if( ier == -1 )
                    return ALGEBRAIC_ROUTINE_FAILED;
                nsing++;
                if( nsing >= 5 )
                    return SINGULAR_MATRIX;

                h *= 0.5;
                reject = true;
                last = false;
                if( !caljac )
                    computeJacobian();
                loop = true;
                continue;
            }

            ier = decompComplex();

            if( ier != 0 )
            {
                if( ier == -1 )
                    return ALGEBRAIC_ROUTINE_FAILED;
                nsing++;
                if( nsing >= 5 )
                    return SINGULAR_MATRIX;

                h *= 0.5;
                reject = true;
                last = false;
                if( !caljac )
                    computeJacobian();
                loop = true;
                continue;
            }
            ndec++;

            while( true && !eventTriggered )
            {
                nstep++;
                if( nstep >= nmax )
                    return TOO_SMALL_N_MAX;

                if( 0.1 * Math.abs(h) <= Math.abs(x) * UROUND )
                    return TOO_SMALL_STEP_SIZE;

                // check the index of the problem
                //                if( nind2 != 0 )
                //                { // is index 2
                //                    for( int i = nind1; i < nind1 + nind2; i++ )
                //                        scal[i] = scal[i] / hhfac;
                //                }
                //
                //                if( nind3 != 0 )
                //                { // is index 3
                //                    for( int i = nind1 + nind2; i < nind1 + nind2 + nind3; i++ )
                //                        scal[i] = scal[i] / ( hhfac * hhfac );
                //                }

                double xph = x + h;
                //  starting values for Newton iteration
                if( first || startn )
                {
                    for( int i = 0; i < n; i++ )
                        z1[i] = z2[i] = z3[i] = f1[i] = f2[i] = f3[i] = 0.0;
                }
                else
                {
                    double c3q = h / hold;
                    double c1q = c1 * c3q;
                    double c2q = c2 * c3q;
                    double ak1, ak2, ak3;
                    for( int i = 0; i < n; i++ )
                    {
                        ak1 = cont[i + n];
                        ak2 = cont[i + 2 * n];
                        ak3 = cont[i + 3 * n];
                        z1[i] = c1q * ( ak1 + ( c1q - c2m1 ) * ( ak2 + ( c1q - c1m1 ) * ak3 ) );
                        z2[i] = c2q * ( ak1 + ( c2q - c2m1 ) * ( ak2 + ( c2q - c1m1 ) * ak3 ) );
                        z3[i] = c3q * ( ak1 + ( c3q - c2m1 ) * ( ak2 + ( c3q - c1m1 ) * ak3 ) );
                        f1[i] = ti11 * z1[i] + ti12 * z2[i] + ti13 * z3[i];
                        f2[i] = ti21 * z1[i] + ti22 * z2[i] + ti23 * z3[i];
                        f3[i] = ti31 * z1[i] + ti32 * z2[i] + ti33 * z3[i];
                    }
                }

                //  loop for the simplified Newton iteration
                int newt = 0;
                faccon = Math.pow(Math.max(faccon, UROUND), 0.8);
                double theta = Math.abs(thet);
                double dyno, dynold = 0;

                while( true )
                {
                    if( newt >= nit )
                    {
                        if( ier != 0 )
                        {
                            nsing++;
                            if( nsing >= 5 )
                                return SINGULAR_MATRIX;
                        }
                        h *= 0.5;
                        reject = true;
                        last = false;
                        if( !caljac )
                            computeJacobian();
                        loop = true;
                        break;
                    }
                    // compute the right-hand side
                    for( int i = 0; i < n; i++ )
                        cont[i] = y[i] + z1[i];
                    z1 = odeModel.dy_dt(x + c1 * h, cont);

                    for( int i = 0; i < n; i++ )
                        cont[i] = y[i] + z2[i];
                    z2 = odeModel.dy_dt(x + c2 * h, cont);

                    for( int i = 0; i < n; i++ )
                        cont[i] = y[i] + z3[i];
                    z3 = odeModel.dy_dt(xph, cont);

                    nfcn += 3;

                    // solve the linear systems
                    for( int i = 0; i < n; i++ )
                    {
                        double a1 = z1[i];
                        double a2 = z2[i];
                        double a3 = z3[i];
                        z1[i] = ti11 * a1 + ti12 * a2 + ti13 * a3;
                        z2[i] = ti21 * a1 + ti22 * a2 + ti23 * a3;
                        z3[i] = ti31 * a1 + ti32 * a2 + ti33 * a3;
                    }
                    ier = linearSolve();
                    if( ier == -1 )
                        return ALGEBRAIC_ROUTINE_FAILED;
                    nsol++;
                    newt++;
                    dyno = 0.0;
                    double denom;
                    for( int i = 0; i < n; i++ )
                    {
                        denom = scal[i];
                        dyno = dyno + Math.pow(z1[i] / denom, 2) + Math.pow(z2[i] / denom, 2) + Math.pow(z3[i] / denom, 2);
                    }
                    dyno = Math.sqrt(dyno / ( 3 * n ));
                    // bad convergence or number of iterations to large
                    if( ( newt > 1 ) && ( newt < nit ) )
                    {
                        double thq = dyno / dynold;
                        theta = ( newt == 2 ) ? thq : Math.sqrt(thq * thqold);
                        thqold = thq;
                        if( theta < 0.99 )
                        {
                            faccon = theta / ( 1.0 - theta );
                            double dyth = faccon * dyno * Math.pow(theta, nit - 1 - newt) / fnewt;
                            if( dyth >= 1.0 )
                            {
                                double qnewt = Math.max(1.0e-4, Math.min(20.0, dyth));
                                h *= 0.8 * Math.pow(qnewt, -1.0 / ( 3.0 + nit - newt ));
                                reject = true;
                                last = false;
                                if( caljac )
                                    computeJacobian();
                                loop = true;
                                break;
                            }
                        }
                        else
                        {
                            if( ier != 0 )
                            {
                                nsing++;
                                if( nsing >= 5 )
                                    return SINGULAR_MATRIX;
                            }
                            h *= 0.5;
                            reject = true;
                            last = false;
                            if( !caljac )
                                computeJacobian();
                            loop = true;
                            break;
                        }
                    }
                    dynold = Math.max(dyno, UROUND);
                    for( int i = 0; i < n; i++ )
                    {
                        f1[i] = f1[i] + z1[i];
                        f2[i] = f2[i] + z2[i];
                        f3[i] = f3[i] + z3[i];
                        z1[i] = t11 * f1[i] + t12 * f2[i] + t13 * f3[i];
                        z2[i] = t21 * f1[i] + t22 * f2[i] + t23 * f3[i];
                        z3[i] = t31 * f1[i] + f2[i];
                    }
                    if( faccon * dyno <= fnewt )
                        break;
                }

                if( loop )
                    break;

                // error estimation
                err = 0.0;
                ier = estimateError();
                if( ier == -1 )
                    return -1;

                // computation of hnew -- require 0.2 <= hnew/h <= 8.
                double fac = Math.min(safe, cfac / ( newt + 2 * nit ));
                double quot = Math.max(facr, Math.min(facl, Math.pow(err, 0.25) / fac));
                double hnew = h / quot;

                //  is the error small enough ?
                if( err < 1.0 )
                {
                    // step is accepted
                    first = false;
                    naccpt++;
                    if( predictGustaffson )
                    {
                        // predictive controller of Gustafsson
                        if( naccpt > 1 )
                        {
                            double facgus = ( hacc / ( h ) ) * Math.pow(err * err / erracc, 0.25) / safe;
                            facgus = Math.max(facr, Math.min(facl, facgus));
                            quot = Math.max(quot, facgus);
                            hnew = h / quot;
                        }
                        hacc = h;
                        erracc = Math.max(1.0e-2, err);
                    }
                    xold = x;
                    hold = h;
                    x = xph;
                    double ak, acont3;
                    for( int i = 0; i < n; i++ )
                    {
                        y[i] = y[i] + z3[i];
                        cont[i + n] = ( z2[i] - z3[i] ) / c2m1;
                        ak = ( z1[i] - z2[i] ) / c1mc2;
                        acont3 = z1[i] / c1;
                        acont3 = ( ak - acont3 ) / c2;
                        cont[i + 2 * n] = ( ak - cont[i + n] ) / c1m1;
                        cont[i + 3 * n] = cont[i + 2 * n] - acont3;
                    }

                    for( int i = 0; i < n; i++ )
                        scal[i] = atoler[i] + rtoler[i] * Math.abs(y[i]);

                    System.arraycopy(y, 0, cont, 0, n);

                    outSolution();

                    caljac = false;
                    if( last )
                    {
                        h = hopt;
                        return SUCCESS;
                    }
                    y0 = odeModel.dy_dt(x, y);
                    nfcn++;
                    hnew = posneg * Math.min(Math.abs(hnew), hmaxn);
                    hopt = Math.min(h, hnew);
                    if( reject )
                        hnew = posneg * Math.min(Math.abs(hnew), Math.abs(h));
                    reject = false;
                    if( ( x + hnew / quot1 - xend ) * posneg >= 0.0 )
                    {
                        h = xend - x;
                        last = true;
                    }
                    else
                    {
                        double qt = hnew / h;
                        if( ( theta <= thet ) && ( qt >= quot1 ) && ( qt <= quot2 ) )
                            continue;
                        h = hnew;
                    }
                    if( theta > thet )
                        computeJacobian();
                    loop = true;
                }
                else
                {
                    // step is rejected
                    reject = true;
                    last = false;
                    h = ( first ) ? h * 0.1 : hnew;
                    if( naccpt >= 1 )
                        nrejct++;
                    if( !caljac )
                        computeJacobian();
                    loop = true;
                }
                break;
            }
        }

        return SUCCESS;
    }

    private void computeJacobian() throws Exception
    {
        njac++;

        if( jband )
        {
            // Jacobian is banded
            int mujacp = mujac + 1;
            int md = Math.min(mbjac, m2);
            for( int mm1 = 0; mm1 < m1 / m2 + 1; mm1++ )
            {
                for( int k = 0; k < md; k++ )
                {
                    int j = k + mm1 * m2;
                    while( true )
                    {
                        f1[j] = y[j];
                        f2[j] = Math.sqrt(UROUND * Math.max(1.0e-5, Math.abs(y[j])));
                        y[j] = y[j] + f2[j];
                        j += md;
                        if( j > ( mm1 + 1 ) * m2 - 1 )
                            break;
                    }
                    System.arraycopy(odeModel.dy_dt(x, y), 0, cont, 0, n);
                    j = k + mm1 * m2;
                    int j1 = k;
                    int lbeg = Math.max(0, j1 - mujac) + m1;
                    int lend, mujacj;
                    while( true )
                    {
                        lend = Math.min(m2, j1 + mljac) + m1;
                        y[j] = f1[j];
                        mujacj = mujacp - j1 - m1 - 1;
                        for( int l = lbeg; l <= lend; l++ )
                        {
                            fjac[l + mujacj][j] = ( cont[l] - y0[l] ) / f2[j];
                        }
                        j += md;
                        j1 += md;
                        lbeg = lend + 1;
                        if( j > ( mm1 + 1 ) * m2 - 1 )
                            break;
                    }
                }
            }
        }
        else
        {
            // Jacobian is full
            double delta, ysafe;
            for( int i = 0; i < n; i++ )
            {
                ysafe = y[i];
                delta = Math.sqrt(UROUND * Math.max(1.0e-5, Math.abs(ysafe)));
                y[i] = ysafe + delta;
                System.arraycopy(odeModel.dy_dt(x, y), 0, cont, 0, n);
                for( int j = m1; j < n; j++ )
                    fjac[j - m1][i] = ( cont[j] - y0[j] ) / delta;
                y[i] = ysafe;
            }
        }

        caljac = true;
        calhes = true;

        return;
    }

    /**
      This function can be used for continuous output. It provides an
      approximation to the solution at time <b>t</b>.
      It returns the value of the collocation polynomial, defined for
      the last successfully computed step.
    */
    private double[] getSolution(double time)
    {
        double[] solution = new double[n];
        for( int i = 0; i < n; i++ )
        {
            double s = ( time - x ) / hold;
            solution[i] = ( cont[i] + s * ( cont[i + n] + ( s - c2m1 ) * ( cont[i + 2 * n] + ( s - c1m1 ) * cont[i + 3 * n] ) ) );
        }
        return solution;
    }

    protected boolean eventAtSpanPoint;

    /**
     * Function that controls the output of the results.
    */
    public void outSolution() throws Exception
    {
        eventAtSpanPoint = false;
        double[] oldEvents = odeModel.checkEvent(xold, yold);
        double[] newEvents = odeModel.checkEvent(x, y);
        events = new int[oldEvents.length];
        double endOfStep = inspectEvents(oldEvents, newEvents);

        double nextSpanPoint = span.getTime(nextSpanIndex);
        while( nextSpanIndex < span.getLength() && nextSpanPoint < endOfStep )
        {
            fireSolutionUpdate(nextSpanPoint, getSolution(nextSpanPoint));
            nextSpanIndex++;
            if (nextSpanIndex >= span.getLength())
                break;
            nextSpanPoint = span.getTime(nextSpanIndex);
        }

        if( nextSpanPoint == endOfStep )
        {
            if( eventTriggered )
                eventAtSpanPoint = true;
            else
                fireSolutionUpdate(nextSpanPoint, getSolution(nextSpanPoint));
        }
        
        if( eventTriggered )
        {
            y = getSolution(endOfStep);
            x = endOfStep;
        }
    }

    private double inspectEvents(double[] oldEvents, double[] newEvents) throws Exception // returns time of closest to xold event, if there was no event - returns x (end of step);
    {
        double minEventTime = xend + 1;

        for( int j = 0; j < events.length; j++ )
        {
            events[j] = 0;
            if( oldEvents[j] == -1 && newEvents[j] == 1 )
            {
                eventTriggered = true;
                double eventTime = findEventTime(j);
                if( eventTime < minEventTime )
                {
                    for( int i = 0; i < j; i++ )
                        events[i] = 0;
                    events[j] = 1;
                    minEventTime = eventTime;
                }
                else if( equals(eventTime, minEventTime) )
                {
                    events[j] = 1;
                }
            }
        }

        if( eventTriggered )
            return minEventTime;
        return x;
    }

    boolean equals(double time1, double time2)
    {
        return Math.abs(time1 - time2) < 1E-16;
    }

    private double findEventTime(int eventIndex) throws Exception //returns time of event in the coordinate with index indexOfEventCoordinate
    {
        double leftPoint = xold;
        double rightPoint = x;
        while( rightPoint - leftPoint > 1e-12 )
        {
            double middle = ( rightPoint + leftPoint ) / 2;
            
            if (middle == leftPoint || middle == rightPoint) //this happens when we ran out of double accuracy
                return middle;
            
            double[] solutionInXd = getSolution(middle);
            double[] pastTimeEvents = odeModel.checkEvent(leftPoint, yold);
            double[] presentTimeEvents = odeModel.checkEvent(middle, solutionInXd);
            if( pastTimeEvents[eventIndex] * presentTimeEvents[eventIndex] < 0 )
                rightPoint = middle;
            else
                leftPoint = middle;
        }
        return rightPoint; // rigth point here is needed to avoid repeat of event on the next step after processing event in EventLoopSimulator
    }

    int DecompReal()
    {
        int/* mm,*/ier = 0;
        //double sum;

        switch( ijob )
        {
            case ( MAS_IDENTITY_JAC_FULL ):
                // mass = identity, Jacobian a full matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < n; i++ )
                    {
                        e1[i][j] = -fjac[i][j];
                    }
                    e1[j][j] += fac1;
                }
                ier = Decsol.dec(n, e1, ip1);
                break;

            case ( MAS_IDENTITY_JAC_BAND ):
                // mass = identity, Jacobian a banded matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < mbjac; i++ )
                    {
                        e1[i + mle][j] = -fjac[i][j];
                    }
                    e1[mdiag][j] += fac1;
                }
                ier = Decsol.decb(n, e1, mle, mue, ip1);
                break;

            case ( MAS_BAND_JAC_FULL ):
                // mass is a banded matrix, Jacobian a full matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < n; i++ )
                        e1[i][j] = -fjac[i][j];
                    for( int i = Math.max(0, j - mumas); i < Math.min(n, j + mlmas + 1); i++ )
                        e1[i][j] += fac1 * fmas[i - j + mbdiag - 1][j];
                }
                ier = Decsol.dec(n, e1, ip1);
                break;

            case ( MAS_BAND_JAC_BAND ):
                // mass is a banded matrix, Jacobian a banded matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < mbjac; i++ )
                        e1[i + mle][j] = -fjac[i][j];
                    for( int i = 0; i < mbb; i++ )
                        e1[i + mdiff][j] += fac1 * fmas[i][j];
                }
                ier = Decsol.decb(n, e1, mle, mue, ip1);
                break;

            case ( MAS_FULL_JAC_FULL ):
                // mass is a full matrix, Jacobian a full matrix
                for( int j = 0; j < n; j++ )
                    for( int i = 0; i < n; i++ )
                        e1[i][j] = fmas[i][j] * fac1 - fjac[i][j];
                ier = Decsol.dec(n, e1, ip1);
                break;

            case ( MAS_IDENTITY_JAC_FULL_HESS ):
                // mass = identity, Jacobian a full matrix, Hessenberg-option
                if( calhes )
                    Decsol.elmhes(n, 0, n, fjac, iphes);
                calhes = false;
                for( int j = 0; j < n - 1; j++ )
                    e1[j + 1][j] = -fjac[j + 1][j];
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i <= j; i++ )
                        e1[i][j] = -fjac[i][j];
                    e1[j][j] += fac1;
                }
                ier = Decsol.dech(n, e1, 1, ip1);
                break;

            //            case ( 11 ):
            //                // mass = identity, Jacobian a full matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < nm1; i++ )
            //                    {
            //                        e1[i][j] = -fjac[i][j + m1];
            //                    }
            //                    e1[j][j] += fac1;
            //                }
            //                break;
            //
            //            case ( 12 ):
            //                // mass = identity, Jacobian a banded matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < mbjac; i++ )
            //                        e1[i + mle][j] = -fjac[i][j + m1];
            //                    e1[mdiag][j] += fac1;
            //                }
            //                break;
            //
            //            case ( 13 ):
            //                // mass is a banded matrix, Jacobian a full matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < nm1; i++ )
            //                        e1[i][j] = -fjac[i][j + m1];
            //                    for( int i = Math.max(0, j - mumas); i < Math.min(n, j + mlmas + 1); i++ )
            //                        e1[i][j] += fac1 * fmas[i - j + mbdiag - 1][j];
            //                }
            //                break;
            //
            //            case ( 14 ):
            //                // mass is a banded matrix, Jacobian a banded matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < mbjac; i++ )
            //                        e1[i + mle][j] = -fjac[i][j + m1];
            //                    for( int i = 0; i < mbb; i++ )
            //                        e1[i + mdiff][j] += fac1 * fmas[i][j];
            //                }
            //                break;
            //
            //            case ( 15 ):
            //                // mass is a full matrix, Jacobian a full matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                    for( int i = 0; i < nm1; i++ )
            //                        e1[i][j] = fmas[i][j] * fac1 - fjac[i][j + m1];
            //                break;
            default:
                return -1;
        }

        //        switch( ijob )
        //        {
        //            case ( 1 ):
        //            case ( 2 ):
        //            case ( 3 ):
        //            case ( 4 ):
        //            case ( 5 ):
        //            case ( 7 ):
        //                break;
        //
        //            case ( 11 ):
        //            case ( 13 ):
        //            case ( 15 ):
        //                mm = m1 / m2;
        //                for( int j = 0; j < m2; j++ )
        //                {
        //                    for( int i = 0; i < nm1; i++ )
        //                    {
        //                        sum = 0.0;
        //                        for( int k = 0; k < mm; k++ )
        //                            sum = ( sum + fjac[i][j + k * m2] ) / fac1;
        //                        e1[i][j] -= sum;
        //                    }
        //                }
        //                ier = Decsol.dec(nm1, e1, ip1);
        //                break;
        //
        //            case ( 12 ):
        //            case ( 14 ):
        //                mm = m1 / m2;
        //                for( int j = 0; j < m2; j++ )
        //                {
        //                    for( int i = 0; i < mbjac; i++ )
        //                    {
        //                        sum = 0.0;
        //                        for( int k = 0; k < mm; k++ )
        //                            sum = ( sum + fjac[i][j + k * m2] ) / fac1;
        //                        e1[i + mle][j] -= sum;
        //                    }
        //                }
        //                ier = Decsol.decb(nm1, e1, mle, mue, ip1);
        //                break;
        //            default:
        //                return -1;
        //        }

        return ier;
    }

    int decompComplex()
    {

        int /*mm,*/ier = 0;
        double bb;//, ffma, abno, alp, bet, sumr, sumi, sums;

        switch( ijob )
        {
            case ( MAS_IDENTITY_JAC_FULL ):
                // mass = identity, Jacobian a full matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < n; i++ )
                    {
                        e2r[i][j] = -fjac[i][j];
                        e2i[i][j] = 0.0;
                    }
                    e2r[j][j] += alphn;
                    e2i[j][j] = betan;
                }
                ier = Decsol.decc(n, e2r, e2i, ip2);
                break;

            case ( MAS_IDENTITY_JAC_BAND ):
                // mass = identiy, Jacobian a banded matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < mbjac; i++ )
                    {
                        e2r[i + mle][j] = -fjac[i][j];
                        e2i[i + mle][j] = 0.0;
                    }
                    e2r[mdiag][j] += alphn;
                    e2i[mdiag][j] = betan;
                }
                ier = Decsol.decbc(n, e2r, e2i, mle, mue, ip2);
                break;

            case ( MAS_BAND_JAC_FULL ):
                // mass is a banded matrix, Jacobian a full matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < n; i++ )
                    {
                        e2r[i][j] = -fjac[i][j];
                        e2i[i][j] = 0.0;
                    }
                }
                for( int j = 0; j < n; j++ )
                {
                    for( int i = Math.max(0, j - mumas); i < Math.min(n, j + mlmas + 1); i++ )
                    {
                        bb = fmas[i - j + mbdiag - 1][j];
                        e2r[i][j] += alphn * bb;
                        e2i[i][j] = betan * bb;
                    }
                }
                ier = Decsol.decc(n, e2r, e2i, ip2);
                break;

            case ( MAS_BAND_JAC_BAND ):
                // mass is a banded matrix, Jacobian a banded matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < mbjac; i++ )
                    {
                        e2r[i + mle][j] = -fjac[i][j];
                        e2i[i + mle][j] = 0.0;
                    }
                    for( int i = Math.max(0, mumas - j); i < Math.min(mbb, mumas - j + n); i++ )
                    {
                        bb = fmas[i][j];
                        e2r[i + mdiff][j] += alphn * bb;
                        e2i[i + mdiff][j] = betan * bb;
                    }
                }
                ier = Decsol.decbc(n, e2r, e2i, mle, mue, ip2);
                break;

            case ( MAS_FULL_JAC_FULL ):
                // mass is a full matrix, Jacobian a full matrix
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i < n; i++ )
                    {
                        bb = fmas[i][j];
                        e2r[i][j] = alphn * bb - fjac[i][j];
                        e2i[i][j] = betan * bb;
                    }
                }
                ier = Decsol.decc(n, e2r, e2i, ip2);
                break;
            case ( MAS_IDENTITY_JAC_FULL_HESS ):
                // mass = identity, Jacobian a full matrix, Hessenberg-option
                for( int j = 0; j < n - 1; j++ )
                {
                    e2r[j + 1][j] = -fjac[j + 1][j];
                    e2i[j + 1][j] = 0.0;
                }
                for( int j = 0; j < n; j++ )
                {
                    for( int i = 0; i <= j; i++ )
                    {
                        e2i[i][j] = 0.0;
                        e2r[i][j] = -fjac[i][j];
                    }
                    e2r[j][j] += alphn;
                    e2i[j][j] = betan;
                }
                ier = Decsol.dechc(n, e2r, e2i, 1, ip2);
                break;

            //            case ( 11 ):
            //                // mass = identity, Jacobian a full matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < nm1; i++ )
            //                    {
            //                        e2r[i][j] = -fjac[i][j + m1];
            //                        e2i[i][j] = 0.0;
            //                    }
            //                    e2r[j][j] += alphn;
            //                    e2i[j][j] = betan;
            //                }
            //                break;
            //
            //            case ( 12 ):
            //                // mass = identity, Jacobian a banded matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < mbjac; i++ )
            //                    {
            //                        e2r[i + mle][j] = -fjac[i][j + m1];
            //                        e2i[i + mle][j] = 0.0;
            //                    }
            //                    e2r[mdiag][j] += alphn;
            //                    e2i[mdiag][j] += betan;
            //                }
            //                break;
            //
            //            case ( 13 ):
            //                // mass is a banded matrix, Jacobian a full matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < nm1; i++ )
            //                    {
            //                        e2r[i][j] = -fjac[i][j + m1];
            //                        e2i[i][j] = 0.0;
            //                    }
            //                    for( int i = Math.max(0, j - mumas); i < Math.min(nm1, j + mlmas + 1); i++ )
            //                    {
            //                        ffma = fmas[i - j + mbdiag - 1][j];
            //                        e2r[j][j] += alphn * ffma;
            //                        e2i[j][j] += betan * ffma;
            //                    }
            //                }
            //                break;
            //
            //            case ( 14 ):
            //                // mass is a banded matrix, Jacobian a banded matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < mbjac; i++ )
            //                    {
            //                        e2r[i + mle][j] = -fjac[i][j + m1];
            //                        e2i[i + mle][j] = 0.0;
            //                    }
            //                    for( int i = 0; i < mbb; i++ )
            //                    {
            //                        ffma = fmas[i][j];
            //                        e2r[i + mdiff][j] += alphn * ffma;
            //                        e2i[i + mdiff][j] += betan * ffma;
            //                    }
            //                }
            //                break;
            //
            //            case ( 15 ):
            //                // mass is a full matrix, Jacobian a full matrix, second order
            //                for( int j = 0; j < nm1; j++ )
            //                {
            //                    for( int i = 0; i < nm1; i++ )
            //                    {
            //                        e2r[i][j] = alphn * fmas[i][j] - fjac[i][j + m1];
            //                        e2i[i][j] = betan * fmas[i][j];
            //                    }
            //                }
            //                break;
            //            default:
            //                return -1;

        }

        //        switch( ijob )
        //        {
        //            case ( 1 ):
        //            case ( 2 ):
        //            case ( 3 ):
        //            case ( 4 ):
        //            case ( 5 ):
        //            case ( 7 ):
        //                break;

        //            case ( 11 ):
        //            case ( 13 ):
        //            case ( 15 ):
        //                mm = m1 / m2;
        //                abno = alphn * alphn + betan * betan;
        //                alp = alphn / abno;
        //                bet = betan / abno;
        //                for( int j = 0; j < m2; j++ )
        //                {
        //                    for( int i = 0; i < nm1; i++ )
        //                    {
        //                        sumr = sumi = 0.0;
        //                        for( int k = 0; k < mm; k++ )
        //                        {
        //                            sums = sumr + fjac[i][j + k * m2];
        //                            sumr = sums * alp + sumi * bet;
        //                            sumi = sumi * alp - sums * bet;
        //                        }
        //                        e2r[i][j] -= sumr;
        //                        e2i[i][j] -= sumi;
        //                    }
        //                }
        //                ier = Decsol.decc(nm1, e2r, e2i, ip2);
        //                break;
        //
        //            case ( 12 ):
        //            case ( 14 ):
        //                mm = m1 / m2;
        //                abno = alphn * alphn + betan * betan;
        //                alp = alphn / abno;
        //                bet = betan / abno;
        //                for( int j = 0; j < m2; j++ )
        //                {
        //                    for( int i = 0; i < mbjac; i++ )
        //                    {
        //                        sumr = sumi = 0.0;
        //                        for( int k = 0; k < mm; k++ )
        //                        {
        //                            sums = sumr + fjac[i][j + k * m2];
        //                            sumr = sums * alp + sumi * bet;
        //                            sumi = sumi * alp - sums * bet;
        //                        }
        //                        e2r[i + mle][j] -= sumr;
        //                        e2i[i + mle][j] -= sumi;
        //                    }
        //                }
        //                ier = Decsol.decbc(nm1, e2r, e2i, mle, mue, ip2);
        //                break;
        //            default:
        //                return -1;
        //        }

        return ( ier );

    } // DecompComplex

    int linearSolve()
    {

        int /*mm,*/mp, mp1, ii, /*jkm, mpi,*/ier = 0;
        double /*abno = 0,*/bb, e1imp, s1, s2, s3;//, sum1, sum2, sum3, sumh;
        double /*ffja, z2i, z3i,*/zsafe;

        switch( ijob )
        {
            case ( MAS_IDENTITY_JAC_FULL ):
                // mass = identity, Jacobian a full matrix
                for( int i = 0; i < n; i++ )
                {
                    s2 = -f2[i];
                    s3 = -f3[i];
                    z1[i] -= f1[i] * fac1;
                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
                }
                Decsol.sol(n, e1, z1, ip1);
                Decsol.solc(n, e2r, e2i, z2, z3, ip2);
                break;

            case ( MAS_IDENTITY_JAC_BAND ):
                // mass = identity, Jacobian a banded matrix
                for( int i = 0; i < n; i++ )
                {
                    s2 = -f2[i];
                    s3 = -f3[i];
                    z1[i] -= f1[i] * fac1;
                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
                }
                Decsol.solb(n, e1, mle, mue, z1, ip1);
                Decsol.solbc(n, e2r, e2i, mle, mue, z2, z3, ip2);
                break;

            case ( MAS_BAND_JAC_FULL ):
                // mass is a banded matrix, Jacobian a full matrix
                for( int i = 0; i < n; i++ )
                {
                    s1 = s2 = s3 = 0.0;
                    for( int j = Math.max(0, i - mlmas); j < Math.min(n, i + mumas + 1); j++ )
                    {
                        bb = fmas[i - j + mbdiag - 1][j];
                        s1 -= bb * f1[j];
                        s2 -= bb * f2[j];
                        s3 -= bb * f3[j];
                    }
                    z1[i] += s1 * fac1;
                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
                }
                Decsol.sol(n, e1, z1, ip1);
                Decsol.solc(n, e2r, e2i, z2, z3, ip2);
                break;

            case ( MAS_BAND_JAC_BAND ):
                // mass is a banded matrix, Jacobian a banded matrix
                for( int i = 0; i < n; i++ )
                {
                    s1 = s2 = s3 = 0.0;
                    for( int j = Math.max(0, i - mlmas); j < Math.min(n, i + mumas + 1); j++ )
                    {
                        bb = fmas[i - j + mbdiag - 1][j];
                        s1 -= bb * f1[j];
                        s2 -= bb * f2[j];
                        s3 -= bb * f3[j];
                    }
                    z1[i] += s1 * fac1;
                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
                }
                Decsol.solb(n, e1, mle, mue, z1, ip1);
                Decsol.solbc(n, e2r, e2i, mle, mue, z2, z3, ip2);
                break;

            case ( MAS_FULL_JAC_FULL ):
                // mass is a full matrix, Jacobian a full matrix
                for( int i = 0; i < n; i++ )
                {
                    s1 = s2 = s3 = 0.0;
                    for( int j = 0; j < n; j++ )
                    {
                        bb = fmas[i][j];
                        s1 -= bb * f1[j];
                        s2 -= bb * f2[j];
                        s3 -= bb * f3[j];
                    }
                    z1[i] += s1 * fac1;
                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
                }
                Decsol.sol(n, e1, z1, ip1);
                Decsol.solc(n, e2r, e2i, z2, z3, ip2);
                break;
            case ( MAS_IDENTITY_JAC_FULL_HESS ):
                // mass = identity, Jacobian a full matrix, Hessenberg-option
                for( int i = 0; i < n; i++ )
                {
                    s2 = -f2[i];
                    s3 = -f3[i];
                    z1[i] -= f1[i] * fac1;
                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
                }
                for( int mm1 = n - 3; mm1 >= 0; mm1-- )
                {
                    mp = n - mm1 - 2;
                    mp1 = mp - 1;
                    ii = iphes[mp];
                    if( ii != mp )
                    {
                        zsafe = z1[mp];
                        z1[mp] = z1[ii];
                        z1[ii] = zsafe;
                        zsafe = z2[mp];
                        z2[mp] = z2[ii];
                        z2[ii] = zsafe;
                        zsafe = z3[mp];
                        z3[mp] = z3[ii];
                        z3[ii] = zsafe;
                    }
                    for( int i = mp + 1; i < n; i++ )
                    {
                        e1imp = fjac[i][mp1];
                        z1[i] -= e1imp * z1[mp];
                        z2[i] -= e1imp * z2[mp];
                        z3[i] -= e1imp * z3[mp];
                    }
                }
                Decsol.solh(n, e1, 1, z1, ip1);
                Decsol.solhc(n, e2r, e2i, 1, z2, z3, ip2);
                for( int mm1 = 0; mm1 < n - 2; mm1++ )
                {
                    mp = n - mm1 - 2;
                    mp1 = mp - 1;
                    for( int i = mp; i < n; i++ )
                    {
                        e1imp = fjac[i][mp1];
                        z1[i] += e1imp * z1[mp];
                        z2[i] += e1imp * z2[mp];
                        z3[i] += e1imp * z3[mp];
                    }
                    ii = iphes[mp];
                    if( ii != mp )
                    {
                        zsafe = z1[mp];
                        z1[mp] = z1[ii];
                        z1[ii] = zsafe;
                        zsafe = z2[mp];
                        z2[mp] = z2[ii];
                        z2[ii] = zsafe;
                        zsafe = z3[mp];
                        z3[mp] = z3[ii];
                        z3[ii] = zsafe;
                    }
                }
                break;

            //            case ( 11 ):
            //                // mass = identity, Jacobian a full matrix, second order
            //            case ( 12 ):
            //                // ---  b = identity, Jacobian a banded matrix, second order
            //                for( int i = 0; i < n; i++ )
            //                {
            //                    s2 = -f2[i];
            //                    s3 = -f3[i];
            //                    z1[i] -= f1[i] * fac1;
            //                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
            //                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
            //                }
            //                break;
            //
            //            case ( 13 ):
            //                // mass is a banded matrix, Jacobian a full matrix, second order
            //            case ( 14 ):
            //                // mass is a banded matrix, Jacobian a banded matrix, second order
            //                for( int i = 0; i < m1; i++ )
            //                {
            //                    s2 = -f2[i];
            //                    s3 = -f3[i];
            //                    z1[i] -= f1[i] * fac1;
            //                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
            //                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
            //                }
            //                for( int i = 0; i < nm1; i++ )
            //                {
            //                    s1 = s2 = s3 = 0.0;
            //                    for( int j = Math.max(0, i - mlmas); j < Math.min(nm1, i + mumas + 1); j++ )
            //                    {
            //                        bb = fmas[i - j + mbdiag - 1][j];
            //                        s1 -= bb * f1[j + m1];
            //                        s2 -= bb * f2[j + m1];
            //                        s3 -= bb * f3[j + m1];
            //                    }
            //                    z1[i + m1] += s1 * fac1;
            //                    z2[i + m1] = z2[i + m1] + s2 * alphn - s3 * betan;
            //                    z3[i + m1] = z3[i + m1] + s3 * alphn + s2 * betan;
            //                }
            //                break;
            //
            //            case ( 15 ):
            //                // mass is a full matrix, Jacobian a full matrix, second order
            //                for( int i = 0; i < m1; i++ )
            //                {
            //                    s2 = -f2[i];
            //                    s3 = -f3[i];
            //                    z1[i] -= f1[i] * fac1;
            //                    z2[i] = z2[i] + s2 * alphn - s3 * betan;
            //                    z3[i] = z3[i] + s3 * alphn + s2 * betan;
            //                }
            //                for( int i = 0; i < nm1; i++ )
            //                {
            //                    s1 = s2 = s3 = 0.0;
            //                    for( int j = 0; j < nm1; j++ )
            //                    {
            //                        bb = fmas[i][j];
            //                        s1 -= bb * f1[j + m1];
            //                        s2 -= bb * f2[j + m1];
            //                        s3 -= bb * f3[j + m1];
            //                    }
            //                    z1[i + m1] += s1 * fac1;
            //                    z2[i + m1] = z2[i + m1] + s2 * alphn - s3 * betan;
            //                    z3[i + m1] = z3[i + m1] + s3 * alphn + s2 * betan;
            //                }
            //                break;
            default:
                return -1;
        }

        //        switch( ijob )
        //        {
        //            case ( 1 ):
        //            case ( 2 ):
        //            case ( 3 ):
        //            case ( 4 ):
        //            case ( 5 ):
        //            case ( 7 ):
        //                break;
        //
        //            case ( 11 ):
        //            case ( 13 ):
        //            case ( 15 ):
        //                abno = alphn * alphn + betan * betan;
        //                mm = m1 / m2;
        //                for( int j = 0; j < m2; j++ )
        //                {
        //                    sum1 = sum2 = sum3 = 0.0;
        //                    for( int k = mm - 1; k >= 0; k-- )
        //                    {
        //                        jkm = j + k * m2;
        //                        sum1 = ( z1[jkm] + sum1 ) / fac1;
        //                        sumh = ( z2[jkm] + sum2 ) / abno;
        //                        sum3 = ( z3[jkm] + sum3 ) / abno;
        //                        sum2 = sumh * alphn + sum3 * betan;
        //                        sum3 = sum3 * alphn - sumh * betan;
        //                        for( int i = 0; i < nm1; i++ )
        //                        {
        //                            z1[i + m1] += fjac[i][jkm] * sum1;
        //                            z2[i + m1] += fjac[i][jkm] * sum2;
        //                            z3[i + m1] += fjac[i][jkm] * sum3;
        //                        }
        //                    }
        //                }
        //
        //                double[] tempz1 = new double[z1.length - m1];
        //                System.arraycopy(z1, m1, tempz1, 0, tempz1.length);
        //
        //                Decsol.sol(nm1, e1, tempz1, ip1);
        //
        //                System.arraycopy(tempz1, 0, z1, m1, tempz1.length);
        //
        //                double[] tempz2 = new double[z2.length - m1];
        //                System.arraycopy(z2, m1, tempz2, 0, tempz2.length);
        //                double[] tempz3 = new double[z3.length - m1];
        //                System.arraycopy(z3, m1, tempz3, 0, tempz3.length);
        //
        //                Decsol.solc(nm1, e2r, e2i, tempz2, tempz3, ip2);
        //
        //                System.arraycopy(tempz2, 0, z2, m1, tempz2.length);
        //                System.arraycopy(tempz3, 0, z3, m1, tempz3.length);
        //
        //                break;
        //
        //            case ( 12 ):
        //            case ( 14 ):
        //                abno = alphn * alphn + betan * betan;
        //                mm = m1 / m2;
        //                for( int j = 0; j < m2; j++ )
        //                {
        //                    sum1 = sum2 = sum3 = 0.0;
        //                    for( int k = mm - 1; k >= 0; k-- )
        //                    {
        //                        jkm = j + k * m2;
        //                        sum1 = ( z1[jkm] + sum1 ) / fac1;
        //                        sumh = ( z2[jkm] + sum2 ) / abno;
        //                        sum3 = ( z3[jkm] + sum3 ) / abno;
        //                        sum2 = sumh * alphn + sum3 * betan;
        //                        sum3 = sum3 * alphn - sumh * betan;
        //                        for( int i = Math.max(0, j - mujac); i < Math.min(nm1, j + mljac + 1); i++ )
        //                        {
        //                            ffja = fjac[i + mujac - j][jkm];
        //                            z1[i + m1] += ffja * sum1;
        //                            z2[i + m1] += ffja * sum2;
        //                            z3[i + m1] += ffja * sum3;
        //                        }
        //                    }
        //                }
        //
        //                tempz1 = new double[z1.length - m1];
        //                System.arraycopy(z1, m1, tempz1, 0, tempz1.length);
        //
        //                Decsol.solb(nm1, e1, mle, mue, tempz1, ip1);
        //
        //                System.arraycopy(tempz1, 0, z1, m1, tempz1.length);
        //
        //                tempz2 = new double[z2.length - m1];
        //                System.arraycopy(z2, m1, tempz2, 0, tempz2.length);
        //                tempz3 = new double[z3.length - m1];
        //                System.arraycopy(z3, m1, tempz3, 0, tempz3.length);
        //
        //                Decsol.solbc(nm1, e2r, e2i, mle, mue, tempz2, tempz3, ip2);
        //
        //                System.arraycopy(tempz2, 0, z2, m1, tempz2.length);
        //                System.arraycopy(tempz3, 0, z3, m1, tempz3.length);
        //                break;
        //            default:
        //                return -1;
        //        }
        //
        //        switch( ijob )
        //        {
        //            case ( 1 ):
        //            case ( 2 ):
        //            case ( 3 ):
        //            case ( 4 ):
        //            case ( 5 ):
        //            case ( 7 ):
        //                break;
        //
        //            case ( 11 ):
        //            case ( 12 ):
        //            case ( 13 ):
        //            case ( 14 ):
        //            case ( 15 ):
        //                for( int i = m1 - 1; i >= 0; i-- )
        //                {
        //                    mpi = m2 + i;
        //                    z1[i] = ( z1[i] + z1[mpi] ) / fac1;
        //                    z2i = z2[i] + z2[mpi];
        //                    z3i = z3[i] + z3[mpi];
        //                    z3[i] = ( z3i * alphn - z2i * betan ) / abno;
        //                    z2[i] = ( z2i * alphn + z3i * betan ) / abno;
        //                }
        //                break;
        //            default:
        //                return -1;
        //
        //        }

        return ( ier );

    } // LinearSolve

    int estimateError() throws Exception
    {

        int mm = 0, ii, mp, ier = 0;
        double sum, zsafe;

        double hee1 = - ( 13.0 + 7.0 * sq6 ) / ( 3.0 * h );
        double hee2 = ( -13.0 + 7.0 * sq6 ) / ( 3.0 * h );
        double hee3 = -1.0 / ( 3.0 * h );

        switch( ijob )
        {
            case ( MAS_IDENTITY_JAC_FULL ):
                // mass = identity, Jacobian a full matrix
                for( int i = 0; i < n; i++ )
                {
                    f2[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
                    cont[i] = f2[i] + y0[i];
                }
                Decsol.sol(n, e1, cont, ip1);
                break;

            case ( MAS_IDENTITY_JAC_BAND ):
                // mass = identity, Jacobian a banded matrix
                for( int i = 0; i < n; i++ )
                {
                    f2[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
                    cont[i] = f2[i] + y0[i];
                }
                Decsol.solb(n, e1, mle, mue, cont, ip1);
                break;

            case ( MAS_BAND_JAC_FULL ):
                // mass is a banded matrix, Jacobian a full matrix
                for( int i = 0; i < n; i++ )
                    f1[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
                for( int i = 0; i < n; i++ )
                {
                    sum = 0.0;
                    for( int j = Math.max(0, i - mlmas); j < Math.min(n, i + mumas + 1); j++ )
                        sum += fmas[i - j + mbdiag - 1][j] * f1[j];
                    f2[i] = sum;
                    cont[i] = sum + y0[i];
                }
                Decsol.sol(n, e1, cont, ip1);
                break;

            case ( MAS_BAND_JAC_BAND ):
                // mass is a banded matrix, Jacobian a banded matrix
                for( int i = 0; i < n; i++ )
                    f1[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
                for( int i = 0; i < n; i++ )
                {
                    sum = 0.0;
                    for( int j = Math.max(0, i - mlmas); j < Math.min(n, i + mumas + 1); j++ )
                        sum = sum + fmas[i - j + mbdiag - 1][j] * f1[j];
                    f2[i] = sum;
                    cont[i] = sum + y0[i];
                }
                Decsol.solb(n, e1, mle, mue, cont, ip1);
                break;

            case ( MAS_FULL_JAC_FULL ):
                // mass is a full matrix, Jacobian a full matrix
                for( int i = 0; i < n; i++ )
                    f1[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
                for( int i = 0; i < n; i++ )
                {
                    sum = 0.0;
                    for( int j = 0; j < n; j++ )
                        sum += fmas[j][i] * f1[j];
                    f2[i] = sum;
                    cont[i] = sum + y0[i];
                }
                Decsol.sol(n, e1, cont, ip1);
                break;

            case ( MAS_IDENTITY_JAC_FULL_HESS ):
                // mass = identity, Jacobian a full matrix, Hessenberg-option
                for( int i = 0; i < n; i++ )
                {
                    f2[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
                    cont[i] = f2[i] + y0[i];
                }
                for( int mm1 = n - 3; mm1 >= 0; mm1-- )
                {
                    mp = n - mm1 - 2;
                    ii = iphes[mp];
                    if( ii != mp )
                    {
                        zsafe = cont[mp];
                        cont[mp] = cont[ii];
                        cont[ii] = zsafe;
                    }
                    for( int i = mp; i < n; i++ )
                        cont[i] -= fjac[i][mp - 1] * cont[mp];
                }
                Decsol.solh(n, e1, 1, cont, ip1);
                for( int mm1 = 0; mm1 < n - 2; mm1++ )
                {
                    mp = n - mm1 - 2;
                    for( int i = mp; i < n; i++ )
                        cont[i] += fjac[i][mp - 1] * cont[mp];
                    ii = iphes[mp];
                    if( ii != mp )
                    {
                        zsafe = cont[mp];
                        cont[mp] = cont[ii];
                        cont[ii] = zsafe;
                    }
                }
                break;

            //            case ( 11 ):
            //                // mass = identity, Jacobian a full matrix, second order
            //                for( int i = 0; i < n; i++ )
            //                {
            //                    f2[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
            //                    cont[i] = f2[i] + y0[i];
            //                }
            //                break;
            //
            //            case ( 12 ):
            //                // mass = identity, Jacobian a banded matrix, second order
            //                for( int i = 0; i < n; i++ )
            //                {
            //                    f2[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
            //                    cont[i] = f2[i] + y0[i];
            //                }
            //                break;
            //
            //            case ( 13 ):
            //                // mass is a banded matrix, Jacobian a full matrix, second order
            //                for( int i = 0; i < m1; i++ )
            //                {
            //                    f1[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
            //                    cont[i] = f2[i] + y0[i];
            //                }
            //                for( int i = m1; i < n; i++ )
            //                    f1[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
            //                for( int i = 0; i < nm1; i++ )
            //                {
            //                    sum = 0.0;
            //                    for( int j = Math.max(0, i - mlmas); j < Math.min(nm1, i + mumas + 1); j++ )
            //                        sum += fmas[i - j + mbdiag - 1][j] * f1[j + m1];
            //                    f2[i + m1] = sum;
            //                    cont[i + m1] = sum + y0[i + m1];
            //                }
            //                break;
            //
            //            case ( 14 ):
            //                // mass is a banded matrix, Jacobian a banded matrix, second order
            //                for( int i = 0; i < m1; i++ )
            //                {
            //                    f2[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
            //                    cont[i] = f2[i] + y0[i];
            //                }
            //                for( int i = m1; i < n; i++ )
            //                    f1[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
            //                for( int i = 0; i < nm1; i++ )
            //                {
            //                    sum = 0.0;
            //                    for( int j = Math.max(0, i - mlmas); j < Math.min(nm1, i + mumas + 1); j++ )
            //                        sum += fmas[i - j + mbdiag - 1][j] * f1[j + m1];
            //                    f2[i + m1] = sum;
            //                    cont[i + m1] = sum + y0[i + m1];
            //                }
            //                break;
            //
            //            case ( 15 ):
            //                // mass is a banded matrix, Jacobian a full matrix, second order
            //                for( int i = 0; i < m1; i++ )
            //                {
            //                    f2[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
            //                    cont[i] = f2[i] + y0[i];
            //                }
            //                for( int i = m1; i < n; i++ )
            //                    f1[i] = hee1 * z1[i] + hee2 * z2[i] + hee3 * z3[i];
            //                for( int i = 0; i < nm1; i++ )
            //                {
            //                    sum = 0.0;
            //                    for( int j = 0; j < nm1; j++ )
            //                        sum += fmas[j][i] * f1[j + m1];
            //                    f2[i + m1] = sum;
            //                    cont[i + m1] = sum + y0[i + m1];
            //                }
            //                break;
            default:
                return -1;
        }

        //        switch( ijob )
        //        {
        //            case ( 1 ):
        //            case ( 2 ):
        //            case ( 3 ):
        //            case ( 4 ):
        //            case ( 5 ):
        //            case ( 7 ):
        //                break;
        //
        //            case ( 11 ):
        //            case ( 13 ):
        //            case ( 15 ):
        //                mm = m1 / m2;
        //                for( int j = 0; j < m2; j++ )
        //                {
        //                    sum = 0.0;
        //                    for( int k = mm - 1; k >= 0; k-- )
        //                    {
        //                        sum = ( cont[j + k * m2] + sum ) / fac1;
        //                        for( int i = 0; i < nm1; i++ )
        //                            cont[i + m1] += fjac[i][j + k * m2] * sum;
        //                    }
        //                }
        //
        //                double[] tempcont = new double[cont.length - m1];
        //                System.arraycopy(cont, m1, tempcont, 0, tempcont.length);
        //
        //                Decsol.sol(nm1, e1, tempcont, ip1);
        //
        //                System.arraycopy(tempcont, 0, cont, m1, tempcont.length);
        //
        //                for( int i = m1 - 1; i >= 0; i-- )
        //                    cont[i] = ( cont[i] + cont[m2 + i] ) / fac1;
        //                break;
        //
        //            case ( 12 ):
        //            case ( 14 ):
        //                mm = m1 / m2;
        //                for( int j = 0; j < m2; j++ )
        //                {
        //                    sum = 0.0;
        //                    for( int k = mm - 1; k >= 0; k-- )
        //                    {
        //                        sum = ( cont[j + k * m2] + sum ) / fac1;
        //                        for( int i = Math.max(0, j - mujac); i < Math.min(nm1, j + mljac); i++ )
        //                            cont[i + m1] += fjac[i + mujac - j][j + k * m2] * sum;
        //                    }
        //                }
        //
        //                tempcont = new double[cont.length - m1];
        //                System.arraycopy(cont, m1, tempcont, 0, tempcont.length);
        //
        //                Decsol.solb(nm1, e1, mle, mue, tempcont, ip1);
        //
        //                System.arraycopy(tempcont, 0, cont, m1, tempcont.length);
        //
        //                for( int i = m1 - 1; i >= 0; i-- )
        //                    cont[i] = ( cont[i] + cont[m2 + i] ) / fac1;
        //                break;
        //            default:
        //                return -1;
        //
        //        }

        err = 0.0;
        for( int i = 0; i < n; i++ )
            err += Math.pow(cont[i] / scal[i], 2);
        err = Math.max(Math.sqrt(err / n), 1.0e-10);

        if( err < 1.0 )
            return ( ier );

        if( first || reject )
        {
            for( int i = 0; i < n; i++ )
                cont[i] = y[i] + cont[i];
            f1 = odeModel.dy_dt(x, cont);
            nfcn++;
            for( int i = 0; i < n; i++ )
                cont[i] = f1[i] + f2[i];

            switch( ijob )
            {
                case ( 1 ):
                case ( 3 ):
                case ( 5 ):
                    // full matrix option
                    Decsol.sol(n, e1, cont, ip1);
                    break;

                case ( 2 ):
                case ( 4 ):
                    // banded matrix option
                    Decsol.solb(n, e1, mle, mue, cont, ip1);
                    break;

                case ( 7 ):
                    //Hessenberg matrix option
                    // mass = identity, Jacobian a full matrix, Hessenberg-option
                    for( int mm1 = n - 3; mm1 >= 0; mm1-- )
                    {
                        mp = n - mm1 - 2;
                        ii = iphes[mp];
                        if( ii != mp )
                        {
                            zsafe = cont[mp];
                            cont[mp] = cont[ii];
                            cont[ii] = zsafe;
                        }
                        for( int i = mp; i < n; i++ )
                            cont[i] -= fjac[i][mp - 1] * cont[mp];
                    }
                    Decsol.solh(n, e1, 1, cont, ip1);
                    for( int mm1 = 0; mm1 < n - 2; mm1++ )
                    {
                        mp = n - mm1 - 2;
                        for( int i = mp; i < n; i++ )
                            cont[i] += fjac[i][mp - 1] * cont[mp];
                        ii = iphes[mp];
                        if( ii != mp )
                        {
                            zsafe = cont[mp];
                            cont[mp] = cont[ii];
                            cont[ii] = zsafe;
                        }
                    }
                    break;

                //                case ( 11 ):
                //                case ( 13 ):
                //                case ( 15 ):
                //                    // Full matrix option, second order
                //                    for( int j = 0; j < m2; j++ )
                //                    {
                //                        sum = 0.0;
                //                        for( int k = mm - 1; k >= 0; k-- )
                //                        {
                //                            sum = ( cont[j + k * m2] + sum ) / fac1;
                //                            for( int i = 0; i < nm1; i++ )
                //                                cont[i + m1] += fjac[i][j + k * m2] * sum;
                //                        }
                //                    }
                //
                //                    double[] tempcont = new double[cont.length - m1];
                //                    System.arraycopy(cont, m1, tempcont, 0, tempcont.length);
                //
                //                    Decsol.sol(nm1, e1, tempcont, ip1);
                //
                //                    System.arraycopy(tempcont, 0, cont, m1, tempcont.length);
                //
                //                    for( int i = m1 - 1; i >= 0; i-- )
                //                        cont[i] = ( cont[i] + cont[m2 + i] ) / fac1;
                //                    break;
                //
                //                case ( 12 ):
                //                case ( 14 ):
                //                    // Banded matrix option, second order
                //                    for( int j = 0; j < m2; j++ )
                //                    {
                //                        sum = 0.0;
                //                        for( int k = mm - 1; k >= 0; k-- )
                //                        {
                //                            sum = ( cont[j + k * m2] + sum ) / fac1;
                //                            for( int i = Math.max(0, j - mujac); i < Math.min(nm1, j + mljac); i++ )
                //                                cont[i + m1] += fjac[i + mujac - j][j + k * m2] * sum;
                //                        }
                //                    }
                //
                //                    tempcont = new double[cont.length - m1];
                //                    System.arraycopy(cont, m1, tempcont, 0, tempcont.length);
                //
                //                    Decsol.solb(nm1, e1, mle, mue, tempcont, ip1);
                //
                //                    System.arraycopy(tempcont, 0, cont, m1, tempcont.length);
                //
                //                    for( int i = m1 - 1; i >= 0; i-- )
                //                        cont[i] = ( cont[i] + cont[m2 + i] ) / fac1;
                //                    break;
                default:
                    return -1;
            }

            err = 0.0;
            for( int i = 0; i < n; i++ )
                err += Math.pow(cont[i] / scal[i], 2);
            err = Math.max(Math.sqrt(err / n), 1.0e-10);
        }

        return ier;

    } // ErrorEstimate


}