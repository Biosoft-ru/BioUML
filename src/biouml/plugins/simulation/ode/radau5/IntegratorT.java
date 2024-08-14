package biouml.plugins.simulation.ode.radau5;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.ode.OdeModel;
import biouml.plugins.simulation.ode.StdMet;
import biouml.standard.simulation.ResultListener;

public class IntegratorT
{
    protected static final Logger log = Logger.getLogger(SimulatorSupport.class.getName());

    // smallest number satisfying 1.0 + uround > 1.0
    public final static double UROUND = 1E-16;
    protected int nextSpanIndex;

    public IntegratorT(Model model, double y[], Span span, double[] rtoler, double[] atoler, Radau5Options options, ResultListener[] resultListeners,
            SimulatorProfile profile) throws Exception
    {
        this.odeModel = (OdeModel)model;
        this.y = y;
        n = y.length;
        x = span.getTimeStart();
        nextSpanIndex = 1;
        xend = span.getTimeFinal();
        this.rtoler = rtoler;
        this.atoler = atoler;
        h = options.getHinit();
        hold = h;
        hmax = options.getHmax();
        nmax = options.getNmax();
        safe = options.getSafe();
        facl = options.getFacl();
        facr = options.getFacr();
        statisticsMode = options.getStatisticsMode();
        nfcn = 0;
        nstep = 0;
        naccpt = 0;
        nrejct = 0;
        xold = x;
        this.span = span;
        this.resultListeners = resultListeners;
        this.profile = profile;

        // rtoler, the relative tolerance of the integration
        if( rtoler == null )
            this.rtoler = StdMet.generateArray(1.0e-7, n);

        // atoler, the absolute tolerance of the integration
        if( atoler == null )
            this.atoler = StdMet.generateArray(1.0e-7, n);

        // -------- maximal step size
        if( hmax == 0.0 )
            hmax = xend - x;

        // -------- nmax--maximal number of steps
        if( nmax == 0 )
            nmax = 100000;
        if( nmax <= 0 )
            throw new IllegalArgumentException("Incorrect nmax = " + nmax);

        // --------- safe--safety factor in step size prediction
        if( safe == 0.0 )
            safe = 0.9;
        if( ( safe <= 0.001 ) || ( safe >= 1.0 ) )
            throw new IllegalArgumentException("Safety factor must be in the domain (0.001, 1.0) but was " + safe);

    }

    String statisticsMode;

    // get number of function evaluations
    public int NumFunction()
    {
        return nfcn;
    }
    // get number of attempted steps
    public int NumStep()
    {
        return nstep;
    }
    // get number of accepted steps
    public int NumAccept()
    {
        return naccpt;
    }
    // get number of rejected steps
    public int NumReject()
    {
        return nrejct;
    }

    // CoreIntegrator
    protected int doCoreIntegration() throws Exception
    {
        return 0;
    }

    // Member variables

    protected OdeModel odeModel;
    // dimension of system
    int n;
    // vector for y values
    double[] y;
    // independent variable (usually time)
    double x;
    // final value for independent variable
    double xend;
    // time step for intermediate output
    double dx;

    // relative error tolerance
    double[] rtoler;
    // absolute error tolerance
    double[] atoler;

    // integration step length
    double h;

    // Derived variables

    // maximal step size
    double hmax;
    // maximal number of steps
    int nmax;

    // safety factor in step size prediction
    double safe;
    // facl, facr--parameters for step size selection
    double facl;
    double facr;

    // Counting variables

    // number of function evaluations (not counting those in numerical
    // Jacobian calculations)
    int nfcn;
    // number of attempted steps
    int nstep;
    // number of accepted steps
    int naccpt;
    // number of rejected steps
    int nrejct;

    // stores past value of x
    double xold;
    // stores past value of h
    double hold;

    Span span;

    ResultListener[] resultListeners;

    SimulatorProfile profile;

    protected synchronized void fireSolutionUpdate(double t, double[] x) throws Exception
    {
        if( odeModel != null )
        {
            double[] y = odeModel.extendResult(t, x);

            odeModel.updateHistory(t);

            if( resultListeners != null )
            {
                for( int i = 0; i < resultListeners.length; i++ )
                    resultListeners[i].add(t, y);
            }
        }
    }

    protected void outError(String message)
    {
        log.log(Level.SEVERE, message);
        System.out.println(message);
    }

    protected void outStatistics(String ... messages)
    {
        if( !statisticsMode.equals(OdeSimulatorOptions.STATISTICS_ON) )
            return;
        for( String message : messages )
        {
            log.info(message);
            System.out.println(message);
        }
        System.out.println();
    }

    protected void outIntermediate(String ... messages)
    {
        if( statisticsMode.equals(OdeSimulatorOptions.STATISTICS_OFF) )
            return;
        for( String message : messages )
        {
            log.info(message);
            System.out.println(message);
        }
        System.out.println();
    }
}