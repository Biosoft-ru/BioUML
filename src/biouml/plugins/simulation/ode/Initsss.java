package biouml.plugins.simulation.ode;

import biouml.plugins.simulation.Span;


/*
   class contains routine for calculating and initial step size to
   be used with an embedded Runge-Kutta scheme
 */
public class Initsss
{
    // constructors

    /*
       constructor constructs an initial step size seleciton object (which
       contains th initial stepsize) given the ODE function, the tspan, the
       vector of initial values, the absolute tolerance, and the relative
       tolerance
     */
    public Initsss(OdeModel function, Span tspan, double[] x0, double[] atol, double[] rtol) throws Exception
    {
        // initializations

        this.f = function; // get function and the time span

        this.t0 = tspan.getTimeStart();
        this.tf = tspan.getTimeFinal();

        this.x0 = new double[x0.length]; // initialize x0, atol, and rtol
        this.atol = new double[atol.length];
        this.rtol = new double[rtol.length];

        StdMet.copyArray(this.x0, x0); // fill x0, atol, and rtol
        StdMet.copyArray(this.atol, atol);
        StdMet.copyArray(this.rtol, rtol);

        hmax = Math.abs(0.1 * (tf - t0)); // hmax is 1 tenth of the interval
        hmin = 16.0 * EPS * t0; // hmin is safely larger than machine epsilon
        absh = hmax; // set absh to hmax for now

        this.threshold = new double[atol.length]; // threshold = atol / rtol
        StdMet.dotQuo(threshold, atol, rtol);

        this.aRtolPow = new double[rtol.length];

        for (int i = 0; i < rtol.length; i++)
        {
            aRtolPow[i] = Math.pow(rtol[i], POW);

        }
        StdMet.stam(aRtolPow, ALPHA, aRtolPow); // aRtolPow = ALPHA * rtol^POW

        this.max = new double[x0.length];

        for (int i = 0; i < x0.length; i++) // max[i] = max(abs(x[i]), threshold)
        {
            max[i] = Math.max(Math.abs(x0[i]), threshold[i]);
        }

        f0 = f.dy_dt(t0, x0); // do a function evaluation

        this.fOverMax = new double[f0.length]; // fOverMax = f0 / max

        StdMet.dotQuo(fOverMax, f0, max);

        double normF = StdMet.normInf(fOverMax); // normF = normInf(f0 / max)
        double normR = StdMet.normInf(aRtolPow); // normR = normInf(ALPHA * rtol^POW)

        this.rh = normF / normR; // rh = normInf(f0/max) / normInf(ALPHA * rtol^POW)

        if (absh * rh > 1) // in case absh is too big
        {
            absh = 1 / rh;
        }
        absh = Math.max(absh, hmin); // just in case absh is too small
        this.h = absh; // we have selected the initial step size
    }

    /*
       constructor is the same as the above constructor and chooses the
       initial step size with all above parameters but has an extra
       argument maxStep, which will be the upper bound for the initial
       step that this routine chooses
     */
    public Initsss(OdeModel function, Span tspan, double[] x0, double[] atol, double[] rtol, double maxStep) throws Exception
    {
        // initializations

        this.f = function; // get function and time span

        this.t0 = tspan.getTimeStart();
        this.tf = tspan.getTimeFinal();

        this.x0 = new double[x0.length]; // initialize x0, atol, and rtol
        this.atol = new double[atol.length];
        this.rtol = new double[rtol.length];

        StdMet.copyArray(this.x0, x0); // fill x0, atol, and rtol
        StdMet.copyArray(this.atol, atol);
        StdMet.copyArray(this.rtol, rtol);

        this.maxStep = maxStep; // get user defined maximum step

        hmax = Math.min(Math.abs(tf - t0), Math.abs(maxStep)); // hmax cannot
        // exceed interval (also handles if user inputs a negative value)
        hmin = 16.0 * EPS * t0; // hmin is safely larger than machine epsilon
        absh = hmax; // set absh to hmax for now

        this.threshold = new double[atol.length]; // threshold = atol / rtol
        StdMet.dotQuo(threshold, atol, rtol);

        this.aRtolPow = new double[rtol.length];

        for (int i = 0; i < rtol.length; i++)
        {
            aRtolPow[i] = Math.pow(rtol[i], POW);

        }
        StdMet.stam(aRtolPow, ALPHA, aRtolPow); // aRtolPow = ALPHA * rtol^POW

        this.max = new double[x0.length];

        for (int i = 0; i < x0.length; i++) // max[i] = max(abs(x[i]), threshold)
        {
            max[i] = Math.max(Math.abs(x0[i]), threshold[i]);

        }
        f0 = f.dy_dt(t0, x0); // do a function evaluation

        this.fOverMax = new double[f0.length]; // fOverMax = f0 / max
        StdMet.dotQuo(fOverMax, f0, max);

        double normF = StdMet.normInf(fOverMax); // normF = normInf(f0 / max)
        double normR = StdMet.normInf(aRtolPow); // normR = normInf(ALPHA * rtol^POW)

        this.rh = normF / normR; // rh = normInf(f0/max) / normInf(ALPHA * rtol^POW)

        if (absh * rh > 1) // in case absh is too big
        {
            absh = 1 / rh;

        }
        absh = Math.max(absh, hmin); // just in case absh is too small
        this.h = absh; // we have selected the initial step size
    }

    // methods

    /*
         method returns the initial stepsize that has been selected
     */
    public double get_h()
    {
        return h;
    }

    // instance variables

    private OdeModel f; // the OdeModel function

    private double t0; // initial time
    private double tf; // final time

    private double[] x0; // the initial value of the problem
    private double[] atol; // array of absolute tolerances
    private double[] rtol; // array of relative tolerances

    private double hmax; // maximum possible step h
    private double hmin; // minimum possible step h
    private double htry; // related to user defined initial step
    private double absh; // temporary variables for intermediate decisions
    private double rh; // a factor to chose h upon (conditions of system)
    private double h; // the initial stepsize selected

    private double[] threshold; // vector of thresholds
    private double[] aRtolPow; // the ALPHA * rtol^POW array
    private double[] max; // the max(abs(x[i]), threshold) array
    private double[] f0; // a function evaluation
    private double[] fOverMax; // the f0 / max array

    private double maxStep; // maximum possible step h (user defined)

    private static final double ALPHA = 0.8; // safety factor
    private static final double POW = 1.0 / 5.0; // order of method inverted
    private static final double EPS = 2.220446049250313 * 1.0E-16; // machine epsilon
}