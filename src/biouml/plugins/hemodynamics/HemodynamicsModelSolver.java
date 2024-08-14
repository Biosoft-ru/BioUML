package biouml.plugins.hemodynamics;

import one.util.streamex.DoubleStreamEx;
import Jama.*;
import biouml.plugins.simulation.Model;

import ru.biosoft.jobcontrol.FunctionJobControl;

import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

public class HemodynamicsModelSolver extends HemodynamicsSolver
{
    private final boolean elasticCapillary = false;
    private final boolean narrowingVessels = false;//true;
    public double kp = 1;
    public double kp1 = 0;
    public double kp2 = 0;
    
    public enum BloodLoss
    {
        NO, SIMPLE, EQUAL_AREA;
    }
    
    public BloodLoss bloodLossType = BloodLoss.NO;
    
    //    private boolean read = false;
    private boolean debugMode = false;
    public void etDebugMode(boolean mode)
    {
        this.debugMode = mode;
    }

    private double[] sign;
    protected ArterialBinaryTreeModel atm;

    /**
     * cross-sectional area of fully relaxed vessel
     */
    private double[][] area0;
    private double[][] dadz;
    public double[] gamma;
    
    public double[] beta;
    public double[] lengths;
    public double[] resistance;
    private static final double DP = 1333.223684; // Pa in 10 mm hg
    private double frictionK;

    //result values
    private double[][] area, flow, impedance;
    
    //private double[][][] dp1;
    public double pressureDelay = 0.1;
    
    int integrationSegments, m, rho = 1;

    private double tDelta, time = 0, tMax;

    private int n; //number of vessels
    private int n2;

    protected boolean isRunning = false;
    protected FunctionJobControl jobControl;

    private double renalOutputFlow = 0;//8.0;

    private SimpleVessel rRenalVessel;
    private SimpleVessel lRenalVessel;

    private int inputCondition;
    private int outputCondition;
    private double currentSystole;
    private double[] integrationFactor;

    //profiling
//    double tBoundary = 0;
//    double tQR11 = 0;
//    double tQR12 = 0;
//    double tStrt = 0;
//    double tOrt = 0;
//    double tInt = 0;
//    double tTotal = 0;
//    double tRet = 0;
//    double tIntPrep = 0;
//    double tRest = 0;
//    double tGF = 0;
//    double tUpdate = 0;
    //        double timeInit = 0;
    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( !model.isInit() )
            model.init();

        atm = (ArterialBinaryTreeModel)model;
        //                double timeInit0 = System.currentTimeMillis();
        inputCondition = parameters.getInputConditionCode();
        outputCondition = parameters.getOutputConditionCode();

        this.resultListeners = listeners;

        for( ResultListener listener : listeners )
            listener.start( model );

        integrationSegments = (int) atm.integrationSegments;
        integrationSegmentsInv = 1.0/integrationSegments;
        m = (int) atm.vesselSegments;

        if( tspan != null )
        {
            this.tDelta = tspan.getTime( 1 ) - tspan.getTimeStart();
            this.tMax = tspan.getTimeFinal();
            this.time = tspan.getTimeStart();
        }
        integrationFactor = new double[integrationSegments];
        for (int i=0; i<integrationSegments; i++)
            integrationFactor[i] = i + 0.5;
        
        isRunning = true;
        profile = new SimulatorProfile();
        profile.setX( new double[] {0} );
        
        frictionK = /*22 */8* Math.PI * atm.bloodViscosity;
        //        if (read)
        //                Util.readParameters( new File("C:/matrices/arterialParameters.txt"), atm , solverStepsNum + 1);

        n = atm.size(); //number of vessels
        n2 = 2 * n;

        area0 = new double[n][m + 1];
        gamma = new double[n];
        
        beta = new double[n];
        lengths = new double[n];
        resistance = new double[n];
        sign = new double[n];
        impedance = new double[n][m + 1];
        flow = new double[n][m + 1];
        area = new double[n][m + 1];
        dadz = new double[n][m + 1];

        //int size =  (int)(pressureDelay / tDelta);
        //dp1 = new double[size][n][m + 1];
        
        //read model parameters
        this.initSolver();

        aPrognosed = new double[n][2];
        qPrognosed = new double[n][2];

        if( !parameters.isFromZero() )
        {
            new InitialValuesCalculator().calculate( atm, inputCondition, outputCondition );

            for( int i = 0; i < n; i++ )
            {
                SimpleVessel v = atm.vessels.get( i );
                for( int j = 0; j < m + 1; j++ )
                {
                    int k = sign[i] < 0 ? m - j : j;
                    area[i][j] = v.area[k];
                    flow[i][j] = v.flow[k];
                }
                v.pulseWave = new double[m + 1];
                v.fullPressure = new double[m + 1];
                v.velocity = new double[m + 1];
                aPrognosed[i][0] = area[i][0];
                qPrognosed[i][0] = flow[i][0];

                aPrognosed[i][1] = area[i][m];
                qPrognosed[i][1] = flow[i][m];
            }

        }
        else
        {
            // initial values for A, P and Q
            for( int i = 0; i < n; i++ )
            {
                SimpleVessel v = atm.vessels.get( i );
                v.pressure = new double[m + 1];
                v.fullPressure = new double[m + 1];
                v.pulseWave = new double[m + 1];
                v.velocity = new double[m + 1];
                for( int j = 0; j < m + 1; j++ )
                {
                    area[i][j] = atm.vessels.get( i ).area0[j];
                    flow[i][j] = 0;
                  
                }

                aPrognosed[i][0] = area[i][0];
                qPrognosed[i][0] = flow[i][0];

                aPrognosed[i][1] = area[i][m];
                qPrognosed[i][1] = flow[i][m];

                v.setArea( area[i] );
                v.setFlow( flow[i] );
                //                        timeInit += System.currentTimeMillis() - timeInit0;
            }

        }
        if( atm.vesselMap.containsKey( ArterialBinaryTreeModel.RRENAL_KEY ) )
            rRenalVessel = atm.vesselMap.get( ArterialBinaryTreeModel.RRENAL_KEY );
        if( atm.vesselMap.containsKey( ArterialBinaryTreeModel.LRENAL_KEY ) )
            lRenalVessel = atm.vesselMap.get( ArterialBinaryTreeModel.LRENAL_KEY );

        conditionGenerator = new MatrixResolver();

        //initial parameters
        updateModelParameters( time );
        currentSystole = atm.systole;
    }

    MatrixResolver conditionGenerator;

    private double integrationSegmentsInv;

    @Override
    public boolean doStep() throws Exception
    {
        if( time < tMax && isRunning )
        {
            if( debugMode )
            {
                TreeIntegrityValidator validator = new TreeIntegrityValidator( atm );
            }
//            double t0 = System.currentTimeMillis();


            time += tDelta;
            profile.setTime( time );

//            double tBoundary0 = System.currentTimeMillis();

            conditionGenerator.generateConditions( true );
            Matrix L = conditionGenerator.getMatrix();
            Matrix l = conditionGenerator.getVector();

            conditionGenerator.generateConditions( false );
            Matrix R = conditionGenerator.getMatrix().transpose();
            Matrix r = conditionGenerator.getVector();


            //            if (time > 15)
            //            {
            //                Util.print( L, new File( "C:/matrices/L" ) );
            //                Util.print( R, new File( "C:/matrices/R" ) );
            //                Util.print( l, new File( "C:/matrices/lvector" ) );
            //                Util.print( r, new File( "C:/matrices/rvector" ) );
            //                System.out.println( "" );
            //            }
//            tBoundary += System.currentTimeMillis() - tBoundary0;

//            double tQR10 = System.currentTimeMillis();

            // experiment repeat marching was here
            Matrix L1 = new Matrix( n2, n2 );
            L1.setMatrix( 0, n2 - 1, 0, n - 1, L );
            QRDecomposition qr = new QRDecomposition( L1 );

//            tQR11 += System.currentTimeMillis() - tQR10;

//            double tQR120 = System.currentTimeMillis();

            Matrix qLeft = qr.getQFullMatrix();
            Matrix rMatrix = qr.getRMatrix().getMatrix( 0, n - 1, 0, n - 1 );

            Matrix m2 = Util.solveBottomTriangle( rMatrix.transpose(), l );
            Matrix Y_f = new Matrix( n2, m + 1 );
            Y_f.setMatrix( 0, n - 1, 0, 0, m2 );
            Y_f.setMatrix( 0, n2 - 1, 0, 0, qLeft.times( Y_f.getMatrix( 0, n2 - 1, 0, 0 ) ) );

            Matrix[] Y_0 = new Matrix[2];
            Y_0[1] = new Matrix( n2, n );

            Matrix[] Z_0 = new Matrix[m + 1];
            Z_0[0] = qLeft.getMatrix( 0, n2 - 1, n, n2 - 1 );

            Matrix Z_f = new Matrix( n2, m + 1 );
            Z_f.setMatrix( 0, n2 - 1, 0, 0, Y_f.getMatrix( 0, n2 - 1, 0, 0 ) );

//            tQR12 += System.currentTimeMillis() - tQR120;

            Matrix[] Ort = new Matrix[m];

            double[][] f1 = new double[n][2];
            double[] f2 = new double[2];

            double[][][] G1 = new double[n][2][2];
            double[][] G2 = new double[2][2];
            
//            double[][] f1_old = new double[n][2];
//            double[] f2_old = new double[2];

//            double[][][] G1_old = new double[n][2][2];
//            double[][] G2_old = new double[2][2];
            
//            double tGF0 = System.currentTimeMillis();
            // straight marching
//            for( int j = 0; j < n; j++ )
//                calculateGF_narrowing( G1[j], f1[j], j, aPrognosed[j][0], qPrognosed[j][0], area0[j][0], dadz[j][0] );

            if( narrowingVessels )
                for( int j = 0; j < n; j++ )
                    calculateGF_narrowing(G1[j], f1[j], j, aPrognosed[j][0], qPrognosed[j][0], area0[j][0], dadz[j][0]);
            else
                for( int j = 0; j < n; j++ )
                    calculateGF_old( G1[j], f1[j], j, aPrognosed[j][0], qPrognosed[j][0] );
//            tGF0 += System.currentTimeMillis() - tGF0;

            for( int i = 0; i < m; i++ )// dimensional segmentation loop
            {
                for( int j = 0; j < n; j++ ) // vessel loop
                {
//                    double tIntPrep0 = System.currentTimeMillis();
                    //                                                            double t2 = System.currentTimeMillis();
                    if( narrowingVessels )
                        calculateGF_narrowing(G2, f2, j, area[j][i + 1], flow[j][i + 1], area0[j][i + 1], dadz[j][i + 1]);
                    else
                        calculateGF_old(G2, f2, j, area[j][i + 1], flow[j][i + 1]);
                    
                    // solution of Cauchy problem
                    double[][] deltaG = new double[][] {
                            { ( G2[0][0] - G1[j][0][0] ) * integrationSegmentsInv, ( G2[0][1] - G1[j][0][1] ) * integrationSegmentsInv},
                            { ( G2[1][0] - G1[j][1][0] ) * integrationSegmentsInv, ( G2[1][1] - G1[j][1][1] ) * integrationSegmentsInv}};
//
                    double[] deltaF = new double[] { ( f2[0] - f1[j][0] ) * integrationSegmentsInv, ( f2[1] - f1[j][1] ) * integrationSegmentsInv};
                                
                    double[][] z_0 = Util.subMatrix( Z_0[i].getArray(), j * 2, 0, 2, n );
                    double[] z_f = Util.columnVector( Z_f.getArray(), j * 2, i, 2 );

//                    tIntPrep += System.currentTimeMillis() - tIntPrep0;
                    // integrating Euler method

                    for( int k = 0; k < integrationSegments; k++ )
                    {
//                        double tInt0 = System.currentTimeMillis();
                        double factor = integrationFactor[k];
                        // A_ = G1[j]+factor*del_mat+E
                        double[][] A_ = new double[][] {{G1[j][0][0] + factor * deltaG[0][0] + 1, G1[j][0][1] + factor * deltaG[0][1]},
                                                        {G1[j][1][0] + factor * deltaG[1][0], G1[j][1][1] + factor * deltaG[1][1] + 1}};

                        //ff = f1[j]+factor*del_fun
                        double[] ff = new double[] {f1[j][0] + factor * deltaF[0], f1[j][1] + factor * deltaF[1]};

                        //z_0 = A*z_0
                        z_0 = Util.multiply2m(A_, z_0);
                        //z_f = A*z_f+ff
                        z_f = new double[] {A_[0][0] * z_f[0] + A_[0][1] * z_f[1] + ff[0], A_[1][0] * z_f[0] + A_[1][1] * z_f[1] + ff[1]};

//                        tInt += System.currentTimeMillis() - tInt0;
                    }

//                    tIntPrep0 = System.currentTimeMillis();
                    // new value
                    Y_0[1].setMatrix(j * 2, j * 2 + 1, 0, n - 1, new Matrix( z_0 ) );
                    Y_f.setMatrix(j * 2, j * 2 + 1, i + 1, i + 1, new Matrix(z_f, 2));

                    G1[j] = new double[][] { {G2[0][0], G2[0][1]}, {G2[1][0], G2[1][1]}};
                    f1[j] = new double[] {f2[0], f2[1]};
//                    tIntPrep += System.currentTimeMillis() - tIntPrep0;
                }

//                double tOrt0 = System.currentTimeMillis();
                // orthogonalizaiton
                Matrix Y = new Matrix( n2, n + 1 );
                Y.setMatrix( 0, n2 - 1, 0, n - 1, Y_0[1] );
                Y.setMatrix( 0, n2 - 1, n, n, Y_f.getMatrix( 0, n2 - 1, i + 1, i + 1 ) );

                qr = new QRDecomposition( Y );
                Matrix qRight = qr.getQ();
                Matrix rRight = qr.getRMatrix();

                Z_f.setMatrix( 0, n2 - 1, i + 1, i + 1, qRight.getMatrix( 0, n2 - 1, n, n ).times( rRight.get( n, n ) ) );

                rRight.set( n, n, 1 );
                Z_0[i + 1] = qRight.getMatrix( 0, n2 - 1, 0, n - 1 );
                Ort[i] = rRight.getMatrix( 0, n, 0, n );

//                tOrt += System.currentTimeMillis() - tOrt0;
            }

//            double tRet0 = System.currentTimeMillis();

            Matrix alpha = ( ( R.times( Z_0[m] ) ).inverse() ).times( r.minus( R.times( Z_f.getMatrix( 0, n2 - 1,
                    m, m ) ) ) );

            // return marching
            Matrix Beta = new Matrix( n + 1, m + 1 );
            Beta.setMatrix( 0, n - 1, m, m, alpha );
            Beta.set( n, m, 1 );

            Matrix Z_ = new Matrix( n2, n + 1 );
            Z_.setMatrix( 0, n2 - 1, 0, n - 1, Z_0[m].getMatrix( 0, n2 - 1, 0, n - 1 ) );
            Z_.setMatrix( 0, n2 - 1, n, n, Z_f.getMatrix( 0, n2 - 1, m, m ) );

            Matrix Z = new Matrix( n2, m + 1 );
            Z.setMatrix( 0, n2 - 1, m, m, Z_.times( Beta.getMatrix( 0, n, m, m ) ) );

            for( int i = m - 1; i >= 0; i-- )
            {
                Beta.setMatrix( 0, n, i, i, Util.solveUpperTriangle( Ort[i], Beta.getMatrix( 0, n, i + 1, i + 1 ) ) );
                Z_.setMatrix( 0, n2 - 1, 0, n - 1, Z_0[i] );
                Z_.setMatrix( 0, n2 - 1, n, n, Z_f.getMatrix( 0, n2 - 1, i, i ) );
                Z.setMatrix( 0, n2 - 1, i, i, Z_.times( Beta.getMatrix( 0, n, i, i ) ) );
            }

//            tRet += System.currentTimeMillis() - tRet0;
            //            Matrix Zcolumn  = Z.getMatrix( 0, 109, 0, 0 );
            //            Matrix lClalc = L.times( Zcolumn.transpose() );;

//            double tRest0 = System.currentTimeMillis();
            for( int i = 0; i < n; i++ )
            {
                if( parameters.isOldLinearisation() )
                {
                    aPrognosed[i][0] = 2 * Z.get( i * 2, 0 ) - area[i][0];//area[i][0];
                    qPrognosed[i][0] = 2 * Z.get( i * 2 + 1, 0 ) - flow[i][0];

                    aPrognosed[i][1] = 2 * Z.get( i * 2, m ) - area[i][m];
                    qPrognosed[i][1] = 2 * Z.get( i * 2 + 1, m ) - flow[i][m];

                }
                else
                {
                    aPrognosed[i][0] = Z.get( i * 2, 0 );
                    qPrognosed[i][0] = Z.get( i * 2 + 1, 0 );

                    aPrognosed[i][1] = Z.get( i * 2, m );
                    qPrognosed[i][1] = Z.get( i * 2 + 1, m );
                }

                //obtaining area and blood flow results
                SimpleVessel v = atm.vessels.get( i );
                double[] pressure = new double[m + 1];
                double[] velocity = new double[m + 1];
                double[] fullPressure = new double[m + 1];
                double[] pulseWave = new double[m+1];
                for( int j = 0; j < m + 1; j++ )
                {
                    area[i][j] = Z.get(i * 2, j);
                    flow[i][j] = Z.get(i * 2 + 1, j);
                    impedance[i][j] = Math.sqrt(rho*beta[i]/2*Math.sqrt(area[i][j]))/area[i][j];
                    
                    //                    double externPressure = 0;
                    //                    if( atm.vessels.get( i ).getTitle().equals( "L. Subclavian II" ) )
                    //                        externPressure = atm.externalPressure;
                    //calculating pressure according to Hooke law
                    //                    pressure[i][j] = beta[i] * ( Math.sqrt( area[i][j] ) - Math.sqrt( area0[i] ) ) / ( area0[i] * DP ) + externPressure;
                    pulseWave[j] = Math.sqrt(beta[i]/(2*rho*Math.sqrt(area[i][j])));
                    pressure[j] = beta[i] * ( Math.sqrt(area[i][j]) - Math.sqrt(area0[i][j]) ) / ( area0[i][j] * DP );

//                    pressure[j] = kp * pressure[j] + kp1 * p1[9][i][j];
                    velocity[j] = flow[i][j] / area[i][j];
                    fullPressure[j] =  pressure[j] + 0.5*velocity[j]*velocity[j]/DP;
                }
                
               // for (int j=dp1.length - 1; j>0; j--)
               // {
//                    p1[ii] = p1[ii-1].clone();
                //    dp1[j] = dp1[j-1].clone();
                //}
//                p1[0][i] = pressure.clone();
                //dp1[0][i] = Util.calcDerivative(pressure, m);
//                p2[i] = p1[i];
//                p1[i] = pressure.clone();
//                dp2[i] = dp1[i].clone();
//                dp1[i] = Util.calcDerivative(pressure, m);
                                
                v.setArea( area[i] );
                v.setFlow( flow[i] );
                v.setPressure( pressure );
                v.setVelocity(velocity);
                v.setFullPressure( fullPressure );
                v.setPulseWave(pulseWave);
                v.setImpedance(impedance[i]);
            }

//            tRest = System.currentTimeMillis() - tRest0;

//            if( time >= 2 && !outDone )
//            {
//                System.out.println( "tBoundary= " + tBoundary );
//                System.out.println( "tQR1= " + tQR11 );
//                System.out.println( "tQR12= " + tQR12 );
//                System.out.println( "tStrt= " + tStrt );
//                System.out.println( "tOrt= " + tOrt );
//                System.out.println( "tIntPrep= " + tIntPrep );
//                System.out.println( "tInt= " + tInt );
//                System.out.println( "tRet= " + tRet );
//                System.out.println( "tRest= " + tRest );
//                System.out.println( "tGF " + tGF );
//                System.out.println( "tUpdate= " + tUpdate );
//                System.out.println( "tTotal= " + tTotal );
//                outDone = true;
//            }

            //updating after step
//            double tUpdate0 = System.currentTimeMillis();
            updateModelParameters( time );
//            tUpdate = System.currentTimeMillis() - tUpdate0;
//            tTotal += System.currentTimeMillis() - t0;
        }

        return ( time < tMax ) && isRunning;
    }

//    private  boolean outDone = false;
    private void calculateGF_delay(double[][] G, double[] f, int j, double a, double q, double dp1, double dp2)
    {
        double c = kp * beta[j] * Math.sqrt(a) / ( 2 * rho * area0[j][0] );
        double qa = q / a;
        double qac = qa * qa - c;
        G[0][0] = -sign[j] * 2 * q / ( qac * a ); //?
        G[0][1] = sign[j] / qac;
        G[1][0] = -sign[j];
        G[1][1] = 0;

        f[0] = sign[j] * ( 1 + tDelta * ( frictionK / a + a / ( rho * q ) * ( kp1 * dp1 + kp2 * dp2 ) ) ) * q / qac;
        f[1] = sign[j] * a;
    }

    private void calculateGF_narrowing(double[][] G, double[] f, int j, double a, double q, double a0, double dadz)
    {
        double cos = Math.cos(gamma[j]);
        double c = beta[j] * Math.sqrt(a) / ( 2 * rho * a0 );
        double qa = q / a;
        double g = qa * qa - c + frictionK * qa * Math.sin(gamma[j]) / ( 2 * Math.sqrt(a * Math.PI) );

        G[0][0] = -sign[j] * 2 * cos * qa / g; //?
        G[0][1] = sign[j] / g;
        G[1][0] = -sign[j] * cos;
        G[1][1] = 0;

        double addon = a * beta[j] / rho * ( Math.sqrt(a) / ( a0 * a0 ) - 1 / ( 2 * Math.sqrt(a0 * a0 * a0) ));

        f[0] = sign[j] * ( q * (  2 * cos - 1 + frictionK * cos * tDelta / a ) - addon * dadz)/g;
        f[1] = sign[j] * a * cos;
        
        double c_old = beta[j] * Math.sqrt( a ) / ( 2 * rho * area0[j][0] );
        double qa_old = q / a;
        double qac_old = qa * qa - c_old;
        double[][] G_old = new double[2][2];
        double[] f_old = new double[2];
        G_old[0][0] = -sign[j] * 2 * qa / qac_old;
        G_old[0][1] = sign[j] / qac_old;
        G_old[1][0] = -sign[j];
        G_old[1][1] = 0;

        f_old[0] = sign[j] * q * ( 1 + frictionK * tDelta / a ) / qac_old;
        f_old[1] = sign[j] * a;
    }

    private void calculateGF_old(double[][] G, double[] f, int j, double a, double q)
    {
        double c = beta[j] * Math.sqrt( a ) / ( 2 * rho * area0[j][0] );
        double qa = q / a;
        double qac = qa * qa - c;
        G[0][0] = -sign[j] * 2 * qa / qac;
        G[0][1] = sign[j] / qac;
        G[1][0] = -sign[j];
        G[1][1] = 0;

        f[0] = sign[j] * q * ( 1 + frictionK * tDelta / a ) / qac;
        f[1] = sign[j] * a;
    }
    

    @Override
    public void stop()
    {
        isRunning = false;
    }

    public double beta1 = 1;
    public double beta2 = 1;
    public double beta3 = 1;

    public void initSolver()
    {
        for( SimpleVessel v : atm.vessels )
        {
            double betaFactor = beta2;
            if( v.left == null )
                betaFactor = beta3;
            else if( v.equals( atm.root ) )
                betaFactor = beta1;
            v.beta = v.beta * betaFactor;

            int i = v.index;
            lengths[i] = v.length;
            area0[i][0] = v.unweightedArea;
            
            if (narrowingVessels)
            {
                area0[i][m] = v.unweightedArea1 <= 0 ? v.unweightedArea : v.unweightedArea1;

                double radius0 = Math.sqrt(v.unweightedArea / Math.PI);
                double radius1 = Math.sqrt(v.unweightedArea1 / Math.PI);

                dadz[i][0] = 2 * Math.PI * radius0 * ( radius1 - radius0 ) / v.length;
                dadz[i][m] = 2 * Math.PI * radius1 * ( radius1 - radius0 ) / v.length;

                for( int j = 1; j < m; j++ )
                {
                    double radius = radius0 + (double)j / m * ( radius1 - radius0 );
                    area0[i][j] = radius * radius * Math.PI;
                    dadz[i][j] = 2 * Math.PI * radius * ( radius1 - radius0 ) / v.length;
                }
                gamma[i] = Math.atan((radius0 - radius1)/ v.length);
            }
            else
            {
                for( int j = 1; j <= m; j++ )
                {
                    area0[i][j] = v.unweightedArea;
                }
            }
            
            
            v.area0 = area0[i].clone();
           
            beta[i] = v.beta;
            resistance[i] = 8 * 0.035 * lengths[i] * Math.PI / ( area0[i][0] * area0[i][0] );
            sign[i] = (Util.isEvenNode( v ) ? 1.0 / tDelta : -1.0 / tDelta) * lengths[i] / ( m * integrationSegments );
        }
    }

    public double p_s_base = 150;
    public double p_d_base = 70;

    public double outletFlow;
    public double outletArea;

    public class MatrixResolver
    {
        public Matrix getMatrix()
        {
            return matrix.getMatrix( 0, n2 - 1, 0, n - 1 );
        }

        public Matrix getVector()
        {
            return matrix.getMatrix( n2, n2, 0, n - 1 ).transpose();
        }

        protected Matrix matrix;

        protected int index;
        private final double[] a, b, c;

        public MatrixResolver()
        {
            a = new double[n];
            b = new double[n];
            c = new double[n];
        }

        public void generateConditions(boolean leftCondition)
        {
            matrix = new Matrix(n2 + 1, n);
//            outletArea = 0;
            outletFlow = 0;
            index = 0;

            int i = leftCondition ? 0 : 1;
            int k = leftCondition ? 0 : m;

            for( int j = 0; j < n; j++ )
            {

                double aj = aPrognosed[j][i];
                double qj = qPrognosed[j][i];

                double a0 = area0[j][k];
                
                if( parameters.isOldLinearisation() )
                {
                    a[j] = beta[j] / (a0 * ( Math.sqrt( aj ) + Math.sqrt( a0 ) ) );
                    b[j] = ( rho * qj ) / ( 2 * aj * aj );
                    c[j] = beta[j] / ( Math.sqrt( aj ) + Math.sqrt( a0 ) );
                }
                else
                {
                    if( parameters.isUseFullPressureConservation() )
                    {
                        b[j] = ( rho * qj ) / ( aj * aj );
                        a[j] = beta[j] / ( 2 * a0 * Math.sqrt( aj ) ) - b[j] * qj / aj;
                        c[j] = -b[j] * qj / 2 - beta[j] * ( Math.sqrt( aj ) / 2 - Math.sqrt(a0 ) ) / a0; //?
                    }
                    else
                    {
                        b[j] = 0;
                        a[j] = beta[j] / ( 2 * a0 * Math.sqrt( aj ) );
                        c[j] = -beta[j] * ( Math.sqrt( aj ) / 2 - Math.sqrt( a0 ) ) / a0; //?
                    }
                }

            }

            if( leftCondition )
                generateInputCondition();

            for( SimpleVessel vessel : atm.vessels )
            {
                if( Util.isEvenNode( vessel ) == leftCondition )
                    continue;

                if( Util.isTerminal( vessel ) )
                    generateOutputCondition( vessel );
                else
                    generateBranchingCondition( vessel );
            }
        }

        public void generateInputCondition()
        {
            Matrix conditionRow;
            switch( inputCondition )
            {
                case HemodynamicsOptions.FLUX_INITIAL_CONDITION:
                {
                    conditionRow = resolveFluxFirstCondition( 0, atm.inputFlow );
                    break;
                }
                case HemodynamicsOptions.PRESSURE_INITIAL_CONDITION:
                {
                    conditionRow = resolvePressureFirstCondition( atm.root, atm.inputPressure );
                    break;
                }
                case HemodynamicsOptions.FILTRATION_INITIAL_CONDITION:
                {
                    conditionRow = generateFiltrationStartCondition( atm.root );
                    break;
                }
                default:
                {
                    conditionRow = resolveAreaFirstCondition( 0, atm.inputArea );
                }
            }
            matrix.setMatrix( 0, n2, index, index, conditionRow );
            index++;
        }

        public void generateOutputCondition(SimpleVessel v)
        {
            Matrix singlePressureEndCondition;
//            if( ( v.getTitle().equalsIgnoreCase( "R. Renal" ) ) || ( v.getTitle().equalsIgnoreCase( "L. Renal" ) ) )
//            {
//                singlePressureEndCondition = resolveRenalFiltrationCondition( v );
//            }
//            else
//            {

                switch( outputCondition )
                {
                    case HemodynamicsOptions.FLUX_INITIAL_CONDITION:
                    {
                        double flow = atm.outputFlow / atm.outputArea;

                        singlePressureEndCondition = resolveFluxEndCondition( v.index, flow );
                        break;
                    }
                    case HemodynamicsOptions.PRESSURE_INITIAL_CONDITION:
                    {
                        singlePressureEndCondition = resolvePressureEndCondition( v, atm.outputPressure );
                        break;
                    }
                default:
                {
                    if( elasticCapillary )
                        singlePressureEndCondition = generateFiltrationEndCondition(v);
                    else
                        singlePressureEndCondition = parameters.isOldLinearisation() ? resolveFiltrationEndConditionOld(v)
                                : resolveFiltrationEndCondition(v);
                }
//                }
            }

            matrix.setMatrix( 0, n2, index, index, singlePressureEndCondition );
            index++;
        }

        public void generateBranchingCondition(SimpleVessel v)
        {
            if( v.left != null )
            {
                Matrix singlePressureConditionLeft = resolveSinglePressureCondition( v, v.left.index );
                matrix.setMatrix( 0, n2, index, index, singlePressureConditionLeft );
                index++;
            }
            if( v.right != null )
            {
                Matrix singlePressureConditionRight = resolveSinglePressureCondition( v, v.right.index );
                matrix.setMatrix( 0, n2, index, index, singlePressureConditionRight );
                index++;
            }
            if( v.right != null && v.left != null )
            {

                Matrix singleFluxCondition = bloodLossType == BloodLoss.NO ? resolveSingleFluxCondition( v.index, v.left.index,
                        v.right.index ) : resolveSingleFluxConditionWithBloodLoss( v.index, v.left.index, v.right.index );
                matrix.setMatrix( 0, n2, index, index, singleFluxCondition );
                index++;
            }
            else
            {
                SimpleVessel nonNullVessel = v.left != null ? v.left : v.right;
                Matrix singleFluxCondition = bloodLossType == BloodLoss.NO ? resolveSingleFluxCondition( v.index, nonNullVessel.index )
                        : resolveSingleFluxConditionWithBloodLoss( v.index, nonNullVessel.index );
                matrix.setMatrix( 0, n2, index, index, singleFluxCondition );
                index++;
            }
        }

        private Matrix resolveSinglePressureCondition(SimpleVessel vessel, int childIndex)
        {
            Matrix result = new Matrix( n2 + 1, 1 );
            int parentIndex = vessel.index;
            result.set( 2 * parentIndex, 0, a[parentIndex] );
            result.set( 2 * parentIndex + 1, 0, b[parentIndex] );
            result.set( 2 * childIndex, 0, -a[childIndex] );
            result.set( 2 * childIndex + 1, 0, -b[childIndex] );
            result.set( n2, 0, c[parentIndex] - c[childIndex] );
            return result;

        }

        private Matrix resolveSingleFluxCondition(int i, int j, int k)
        {
            double loss = atm.bloodLoss;
            if( atm.vessels.get( i ).getTitle().equals( "Ascending Aorta" ) )
                loss = 0;
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 2 * i + 1, 0, 1 - loss );
            result.set( 2 * j + 1, 0, -1 );
            result.set( 2 * k + 1, 0, -1 );
            return result;
        }

        
        private Matrix resolveSingleFluxConditionWithBloodLoss(int i, int j, int k)
        {
            double loss = atm.bloodLoss;
            if( atm.vessels.get( i ).getTitle().equals( "Ascending Aorta" ) )
                loss = 0;
            
            SimpleVessel vessel = atm.vessels.get(i);
            SimpleVessel child1 = atm.vessels.get(j);
            SimpleVessel child2 = atm.vessels.get(k);
            double area = vessel.getArea()[m];
            double area1 = child1.getArea()[m];
            double area2 = child2.getArea()[m];
            
            double outArea;
            if( bloodLossType == BloodLoss.SIMPLE )
            {
                outArea = ( Math.sqrt( area ) + Math.sqrt( area0[i][m] ) * ( 10 * Math.sqrt( 10 ) - 1 ) ) / 100;
                outArea *= outArea;
            }
            else
            {
                outArea = Math.max( area - area1 - area2, 0 );
            }
            double relativeArea = outArea / atm.outputArea;

            double outflow = relativeArea / ( atm.capillaryResistance )    * (vessel.pressure[m] - atm.venousPressure);

            outletFlow += outflow;
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 2 * i + 1, 0, 1 - loss );
            result.set( 2 * j + 1, 0, -1 );
            result.set( 2 * k + 1, 0, -1 );
            
            result.set( n2, 0, outflow );
            return result;
        }
        
        private Matrix resolveSingleFluxConditionWithBloodLoss(int i, int j)
        {
            double loss = atm.bloodLoss;
            if( atm.vessels.get( i ).getTitle().equals( "Ascending Aorta" ) )
                loss = 0;
            
            SimpleVessel vessel = atm.vessels.get(i);
            SimpleVessel child = atm.vessels.get(j);
            double area = vessel.getArea()[m];
            double childArea = child.getArea()[m];
            double outArea;
            if( bloodLossType == BloodLoss.SIMPLE )
            {
                outArea = ( Math.sqrt( area ) + Math.sqrt( area0[i][m] ) * ( 10 * Math.sqrt( 10 ) - 1 ) ) / 100;
                outArea *= outArea;
            }
            else
            {
                outArea = Math.max( area - childArea, 0 );
            }
            double relativeArea = outArea/ atm.outputArea;//outArea / atm.outputArea;

            double outflow = relativeArea / ( atm.capillaryResistance )    * (vessel.pressure[m] - atm.venousPressure);
            
            outletFlow += outflow;
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 2 * i + 1, 0, 1 - loss);
            result.set( 2 * j + 1, 0, -1 );

            result.set( n2, 0, outflow );
            return result;
        }

        private Matrix resolveSingleFluxCondition(int i, int j)
        {
            double loss = atm.bloodLoss;
            if( atm.vessels.get( i ).getTitle().equals( "Ascending Aorta" ) )
                loss = 0;
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 2 * i + 1, 0, 1 - loss);
            result.set( 2 * j + 1, 0, -1 );
            return result;
        }



        private Matrix resolveFluxEndCondition(int vesselIndex, double flow)
        {
            Matrix result = new Matrix( n2 + 1, 1 );

            result.set( 2 * vesselIndex, 0, -flow );
            result.set( 2 * vesselIndex + 1, 0, 1 );
            result.set( n2, 0, 0 );

            return result;

        }

        private Matrix resolveAreaEndCondition(int vesselIndex, double areaOutput)
        {
            Matrix result = new Matrix( n2 + 1, 1 );

            result.set( 2 * vesselIndex, 0, 1 );
            result.set( 2 * vesselIndex + 1, 0, 0 );
            result.set( n2, 0, areaOutput );// 70

            return result;

        }

        private Matrix resolveFiltrationWithKidneyEndCondition(SimpleVessel v)
        {
            int vesselIndex = v.index;
            double summaryArea = atm.outputArea;

            if( lRenalVessel != null )
                summaryArea -= lRenalVessel.getArea()[m];

            if( rRenalVessel != null )
                summaryArea -= rRenalVessel.getArea()[m];

            double outputFlowWithoutRenalKof = v.getArea()[m] / summaryArea;

            double KD = outputFlowWithoutRenalKof / ( DP * atm.capillaryResistance );
            double areaEndRoot = Math.sqrt( v.getArea()[m] );
            double areaCof = -KD * beta[vesselIndex] / ( 2 * area0[vesselIndex][m] * areaEndRoot );
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 2 * vesselIndex, 0, areaCof );
            result.set( 2 * vesselIndex + 1, 0, 1 );

            result.set( n2, 0, ( KD * beta[vesselIndex] / area0[vesselIndex][m] ) * ( areaEndRoot / 2 - Math.sqrt( area0[vesselIndex][m] ) ) - KD
                    * atm.venousPressure * DP - outputFlowWithoutRenalKof * renalOutputFlow );

            return result;

        }


        private Matrix resolveFiltrationEndCondition(SimpleVessel v)
        {
            int index = v.index;
            double areaEnd = v.getArea()[m];
            
          
            double capillaryResistance = atm.capillaryResistance* atm.outputArea / areaEnd;
//            double capillaryResistance3 = Util.calcPWV(v.area[m], v.beta);
            double capillaryResistance3 = Util.calcNoReflectionResistance(v, atm.venousPressure);
            
            if( v.getTitle().equals("Brachiocefalic_3") )
            {
//                capillaryResistance = Util.calcNoReflectionResistance(v, atm.venousPressure);
//                if( !Double.isNaN(capillaryResistance3) )
//                    capillaryResistance = capillaryResistance3;
            }
            double KD = Double.isNaN(capillaryResistance)? 0: 1 / ( DP *capillaryResistance);

            if( parameters.isModelArteriols() )
            {
                double minArea = Util.calculateMinArea(atm);
                double arteriolarResistance = Util.calculateArteriolarResistance(v, atm.bloodViscosity, minArea, capillaryResistance);
                if( Math.abs(time - 2) < 0.0001 )
                {
                    double vesselEndResistance = Util.calculateResistance(v, m, atm.bloodViscosity);
                    System.out.println(v.getTitle() + "\t" + v.length + "\t" + Math.max(0,Math.log(areaEnd/minArea)/Math.log(2)) + "\t" + areaEnd + "\t" + vesselEndResistance + "\t"
                            + arteriolarResistance + "\t" + capillaryResistance+"\t"+ atm.capillaryResistance+"\t"+atm.outputArea);
                }
                KD = 1 / ( DP * arteriolarResistance);
            }
                       
            double areaEndRoot = Math.sqrt( areaEnd );
            double areaCof = -KD * v.beta / ( 2 * area0[index][m] * areaEndRoot );
            Matrix result = new Matrix( 2 * atm.size() + 1, 1 );
            result.set( 2 * index, 0, areaCof );
            result.set( 2 * index + 1, 0, 1 );
            result.set( 2 * atm.size(), 0, ( KD * v.beta / area0[index][m] ) * ( areaEndRoot / 2 - Math.sqrt( area0[index][m] ) ) - KD
                    * atm.venousPressure * DP );


            return result;

        }

        /**
         * Generate row for boundary condition for given vessel.<br>
         * M x (A,Q,-1)T = 0
         * This condition is based on the filtration law:
         * Q = K_f*(P-P_v)
         * K_f - filtration coefficient it is assumed to be a part of whole conductivity in proportion to vessel end area
         * P_V - pressure in veins
         * 
         * According to state equation:
         * P = beta(sqrt(A)-sqrt(A0))/A0
         * After linearization:
         * P = beta/A0*(sqrt(A)/2-1/sqrt(A0))+beta*(A/2*A0*sqrt(A0))
         * 
         * Capillary conductivity is assumed to depend on pressure: K(t) = c1 + c2*P(t)
         * @param v
         * @return
         */
        private Matrix generateFiltrationEndCondition(SimpleVessel v)
        {
            int index = v.index;
            double areaEnd = v.getArea()[m];
            double sqrtA = Math.sqrt(areaEnd);
            double sqrtA0 = Math.sqrt(v.area0[m]);
            double k_m =  areaEnd / atm.outputArea * 1 / (atm.capillaryResistance * DP); // conductivity is proportional to vessel end area
            double k_s = atm.capillaryConductivityFactor*k_m;
            double p_s = p_s_base*DP;
            double p_d = p_d_base*DP;
            double c1 = 2 * ( k_s - k_m ) / ( p_s - p_d );
            double c2 = - ( k_s * ( p_s + p_d ) - 2 * k_m * p_s ) / ( p_s - p_d );
            double c2_c1p = c2 - c1 * atm.venousPressure*DP;
            double beta_a0 = v.beta/v.area0[m];
            double d =  beta_a0*( c2_c1p - 2 * c1 *  beta_a0 * sqrtA0 );
                   
            //Boundary condition in the next form:
            //a * Area + b * Flow = c
            double a = -c1 * beta_a0*beta_a0 - d / ( 2 * sqrtA );
            double b = 1;
            double c = d *  sqrtA / 2 + beta_a0*(c1 * v.beta - c2_c1p * sqrtA0) - c2 * atm.venousPressure*DP;
        
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 2 * index, 0, a );
            result.set( 2 * index + 1, 0, b);
            result.set( 2 * n, 0, c );
            
//            if (oldStyle.minus(result).norm1() > 0.00001)
//                System.out.println(oldStyle.minus(result).normInf());
            return result;
        }



        private Matrix resolveFiltrationEndConditionOld(SimpleVessel v)
        {
            int index = v.index;
            double areaEnd = v.getArea()[m];
            double KD = areaEnd / ( DP * atm.outputArea * atm.capillaryResistance );
            Matrix result = new Matrix( 2 * atm.size() + 1, 1 );
            result.set( 2 * index, 0, KD * a[index] );
            result.set( 2 * index + 1, 0, -1 );
            result.set( 2 * atm.size(), 0, KD * c[index] + atm.venousPressure * DP * KD + areaEnd );
            return result;
        }

        /**
         * 
         */
        private Matrix resolveRenalFiltrationCondition(SimpleVessel v)
        {
            double kofRenalFiltration = 1.0;
            int vesselIndex = v.index;
            double KD = v.getArea()[m] * kofRenalFiltration / ( atm.kidneyResistance * ( DP * 3 / 50 ) * ( //DP*3/50 - dimension transform coefficient
                    lRenalVessel.getArea()[m] + rRenalVessel.getArea()[m] ) );

            double areaEndRoot = Math.sqrt( v.getArea()[m] );
            double areaCof = -KD * beta[vesselIndex] / ( 2 * area0[vesselIndex][m] * areaEndRoot );
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 2 * vesselIndex, 0, areaCof );
            result.set( 2 * vesselIndex + 1, 0, 1 );

            result.set( n2, 0, ( KD * beta[vesselIndex] / area0[vesselIndex][m] ) * ( areaEndRoot / 2 - Math.sqrt( area0[vesselIndex][m] ) ) );

            return result;

        }


        private Matrix resolveFluxFirstCondition(int vesselIndex, double fluxInput)
        {
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 0, 0, 0 );//InitialArea no
            result.set( 1, 0, 1 );//InitialFlux yes
            result.set( n2, 0, fluxInput ); // edit

            return result;

        }

        private Matrix resolveAreaFirstCondition(int vesselIndex, double areaInput)
        {
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 0, 0, 1 );//InitialArea yes
            result.set( 1, 0, 0 );//InitialFlux no
            result.set( n2, 0, areaInput ); // edit

            return result;

        }

        private Matrix generateFiltrationStartCondition(SimpleVessel v)
        {
            double areaStart = Math.sqrt( v.getArea()[0] );
            Matrix result = new Matrix( 2 * atm.size() + 1, 1 );

            if( atm.systole != 1|| atm.ventriclePressure < atm.inputPressure)
            {
                result.set( 2 * index, 0, 0 );
                result.set( 2 * index + 1, 0, 1 );
                result.set( 2 * atm.size(), 0, 0 );
            }
            else
            {
                result.set( 2 * index, 0, atm.arterialResistance * v.beta / ( 2 * v.area0[0] * areaStart ) / DP );
                result.set( 2 * index + 1, 0, 1 );
                result.set(
                        2 * atm.size(),
                        0,
                        atm.arterialResistance * ( atm.ventriclePressure - v.beta / v.area0[0] * ( areaStart / 2 - Math.sqrt( v.area0[0] ) ) / DP ) );
            }
            return result;
        }
        
        private Matrix resolvePressureEndCondition(SimpleVessel v, double pressureOutput)
        {
            int index = v.index;
            Matrix result = new Matrix( n2 + 1, 1 );
            result.set( 2 * index, 0, 1 );
            result.set( 2 * index + 1, 0, 0 );
//            double areaEndRoot = Math.sqrt( v.getArea()[m] );

            result.set(n2, 0,
                    2 * Math.sqrt(v.getArea()[m]) * ( pressureOutput * DP * area0[index][m] / beta[index] + Math.sqrt(area0[index][m]) )
                            - v.getArea()[m] );
            return result;
        }

        private Matrix resolvePressureFirstCondition(SimpleVessel v, double pressureInput)
        {
            int index = v.index;
            Matrix result = new Matrix( n2 + 1, 1 );

            result.set( 0, 0, 1 );
            result.set( 1, 0, 0 );
            double areaEndRoot = Math.sqrt( v.getArea()[0] );
            result.set( n2, 0, 2 * areaEndRoot
                    * ( pressureInput * DP * area0[index][0] / beta[index] + Math.sqrt( area0[index][0] ) - areaEndRoot / 2 ) );

            return result;
        }
        // ///////////////////////////////////////////////////////////////
    }


    @Override
    public void setInitialValues(double[] x0)
    {
        try
        {
            this.atm.setCurrentValues(x0);
        }
        catch( Exception e )
        {

        }
    }

    private double[][] aPrognosed;
    private double[][] qPrognosed;


    private void updateModelParameters(double time)
    {
        atm.time = time;
       
        atm.kidneyInputFlow = renalOutputFlow * 3 / 50;
        atm.totalVolume = calculateTotalVolume();
        atm.outputArea = this.getOutputArea();
        
        if( this.bloodLossType == BloodLoss.EQUAL_AREA )
        {
            this.outletArea = this.getOutletArea( atm.root );
            atm.outputArea += outletArea;
        }
        atm.averagePressure = getAveragePressure(atm.totalVolume);
        //        atm.arterialResistance = 8 * 35 * atm.root.length * Math.PI / ( atm.root.area[0] * atm.root.area[0] );
        atm.arterialResistance = calculateTotalResistance2();//8 * Math.PI*atm.bloodViscosity*atm.root.length/( atm.root.area[0] * atm.root.area[0] )/DP*10;
        if( n > 54 )
        {
            getRenalParameters();
        }
        

        
        if( inputCondition != HemodynamicsOptions.PRESSURE_INITIAL_CONDITION )
        {
            atm.inputPressure = atm.root.getPressure()[0];
        }
        if( inputCondition != HemodynamicsOptions.FLUX_INITIAL_CONDITION )
        {
            atm.inputFlow = atm.root.getFlow()[0];
        }

        if( outputCondition != HemodynamicsOptions.PRESSURE_INITIAL_CONDITION )
        {
            atm.outputPressure = getOutputPressure();
        }
        if( outputCondition != HemodynamicsOptions.FLUX_INITIAL_CONDITION )
        {
            atm.outputFlow = getOutputFlow() + this.outletFlow;
        }


        atm.calculateParameters( time );
        try
        {
            //                        if (time >= 9.461)
            //                        {
            //                            Util.writeParameters( new File("C:/matrices/arterialParameters.txt"), atm );
            //                            System.exit( 0);
            //                        }
            fireSolutionUpdate();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    //-----------------Computation of average pressure in total system-------------------------------
    public double getAveragePressure(double systemVolume)
    {
        double averagePressure = 0;
        for( int j = 0; j < n; j++ )
        {
            SimpleVessel v = atm.vessels.get( j );
            double step = lengths[j] / ( 2 * m );
            for( int i = 0; i < m; i++ )
                averagePressure += ( v.getPressure()[i + 1] * v.getArea()[i + 1] + v.getPressure()[i] * v.getArea()[i] ) * step;
        }
        return averagePressure / systemVolume;
    }

    /**
     * Computation of total volume of system
     * @return
     */
    public double calculateTotalVolume()
    {
        double systemVolume = 0;
        for( int j = 0; j < n; j++ )
        {
            double step = lengths[j] / ( 2 * m );
            systemVolume += DoubleStreamEx.of( area[j] ).pairMap( Double::sum ).sum() * step;
        }
        return systemVolume;

    }

    public double calculateTotalResistance()
    {
        for( int j = 0; j < n; j++ )
        {
            double step = lengths[j] / ( 2 * m );
            double sum = DoubleStreamEx.of( area[j] ).map( a -> a * a ).pairMap( Double::sum ).sum() * step;
            atm.vessels.get( j ).resistance = 8 * atm.bloodViscosity * Math.PI * lengths[j] * lengths[j] / sum;
        }
        return calculateTreeResistance( atm.root ) * atm.nueroReceptorsControl / ( atm.vascularity * DP/10 );
    }

    /**
     * Calculate as a sum of resistances on vessel segments. Radius consider to be constant on segment.
     * R = R_1 +... +R_n
     * where R_i = 8*NU*l_i / pi*(r_i)^4
     * l_i - length of segmnent
     * r_i - radius of segment
     * l_1 = ... = l_n = delta
     * finally:
     * R = 8*NU*delta*pi*((1/A_1)^2+...+(1/A_n)^2)
     * @return
     */
    public double calculateTotalResistance2()
    {
        for( SimpleVessel vessel : atm.vessels )
        {
            double delta = vessel.length / ( m );
            double sum = 0;
            for( int i = 0; i < m; i++ )
                sum += 1 / ( vessel.getArea()[i] * vessel.getArea()[i] );// * step;

            vessel.resistance = 8 * atm.bloodViscosity * delta * Math.PI * sum;

            if( this.parameters.isModelArteriols() &&  vessel.left == null && vessel.right == null )
                vessel.resistance += Util.calculateArteriolarResistance(vessel, atm.bloodViscosity, Util.calculateMinArea(atm),
                        atm.capillaryResistance);
        }
        return calculateTreeResistance(atm.root) * atm.nueroReceptorsControl / ( atm.vascularity * DP / 10 );
    }

    public double calculateTotalResistance3()
    {
        for( SimpleVessel vessel : atm.vessels )
            vessel.resistance = (vessel.pressure[0] - vessel.pressure[vessel.pressure.length - 1]) / vessel.flow[1];
        return calculateTreeResistance( atm.root ) * atm.nueroReceptorsControl / ( atm.vascularity);
    }

    private double getOutputFlow()
    {
        return getOutputFlow( atm.root );
    }


    public double getOutputPressure()
    {
        return getOutputPressure( atm.root, atm.outputArea );
    }

    public double getOutputArea()
    {
        outletArea = 0;
        return getOutputArea( atm.root );
    }

    private double getOutputFlow(SimpleVessel v)
    {
        double outputFlow = 0;

        if( v.left != null )
            outputFlow += getOutputFlow( v.left );
        if( v.right != null )
            outputFlow += getOutputFlow( v.right );
        if( ( v.left == null ) && ( v.right == null ) )
            outputFlow += v.getFlow()[m];
        return outputFlow;
    }

    private double getOutletArea(SimpleVessel v)
    {
        double outputArea = 0;

        double branchArea = 0;
        if( v.left != null )
        {
            outputArea += getOutputArea( v.left );
            branchArea += v.left.getArea()[0];
        }
        if( v.right != null )
        {
            outputArea += getOutputArea( v.right );
            branchArea += v.right.getArea()[0];
        }
        if( ( v.left == null ) && ( v.right == null ) )
        {
            return 0;
        }
        else
        {
            outputArea += Math.max(v.getArea()[m] - branchArea, 0);
        }
        return outputArea;
    }
    
    private double getOutputArea(SimpleVessel v)
    {
        double outputArea = 0;

        if( v.left != null )
            outputArea += getOutputArea( v.left );
        if( v.right != null )
            outputArea += getOutputArea( v.right );
        if( ( v.left == null ) && ( v.right == null ) )
        {
            outputArea += v.getArea()[m];//*v.getArea()[vesselSegments];
        }
        else if (bloodLossType == BloodLoss.SIMPLE)
        {
            double outlet = (Math.sqrt(v.getArea()[m]) + Math.sqrt(v.area0[m])* (10*Math.sqrt(10) - 1)) / 100;
            outletArea += outlet*outlet;
            outputArea += outlet*outlet;
        }
        return outputArea;
    }

    private double getOutputPressure(SimpleVessel v, double areaEndSum)
    {
        double outputPressure = 0;

        if( v.left != null )
            outputPressure += getOutputPressure( v.left, areaEndSum );
        if( v.right != null )
            outputPressure += getOutputPressure( v.right, areaEndSum );
        if( ( v.left == null ) && ( v.right == null ) )
            outputPressure += ( v.getPressure()[m] * v.getArea()[m] ) / ( areaEndSum );
        return outputPressure;
    }

    public void getRenalParameters()
    {
        renalOutputFlow = 0;
        atm.renalConductivity = 0;

        //--------------R renal parameters
        if( rRenalVessel != null )
        {
            renalOutputFlow += rRenalVessel.getFlow()[m];
            atm.renalConductivity += resistance[rRenalVessel.index];
        }
        //--------------L renal parameters

        if( lRenalVessel != null )
        {
            renalOutputFlow += lRenalVessel.getFlow()[m];
            atm.renalConductivity += resistance[lRenalVessel.index];
        }

        atm.renalConductivity *= 15.2 * 50 / ( 3 * DP );
    }

    protected ResultListener[] resultListeners = null;
    protected void fireSolutionUpdate() throws Exception
    {
        if( atm != null )
        {
            double[] y = atm.getCurrentValues();

            if( resultListeners != null )
            {
                for( ResultListener resultListener : resultListeners )
                    resultListener.add( time, y );
            }
        }
    }

    public double calculateTreeResistance(SimpleVessel v)
    {
        if( v.left != null && v.right != null )
            return v.resistance + 1 / ( 1 / calculateTreeResistance( v.left ) + 1 / calculateTreeResistance( v.right ) );
        else if( v.right != null )
            return v.resistance + calculateTreeResistance( v.right );
        else if( v.left != null )
            return v.resistance + calculateTreeResistance( v.left );
        else
            return v.resistance;
    }
}
