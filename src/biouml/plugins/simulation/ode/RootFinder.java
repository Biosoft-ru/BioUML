package biouml.plugins.simulation.ode;


/*
  class contains methods for finding the root of a function using the safeguarded secant
  method, that is: the secant root finding method, safeguarded by bisection.  This class
  was made to be used in particular with event location feature of ODE solvers
 */
public class RootFinder
{
    // constructors

    public RootFinder(OdeModel function, Btableau butcher, double h, double told, double[] xold, double[][] K)
    {
        this.f = function;
        this.butcher = butcher;
        this.h = h;
        this.told = told;
        this.n = xold.length;
        this.s = butcher.getbl();
        this.xold = xold;
        this.s = butcher.getbl();
        this.K = K;
    }


    public double safeguarded_secant(int i, double ga, double gb) throws Exception
    {
        sigmaInterp = new double[n];
        x_interp = new double[n];
        stam1 = new double[n];

        thetaStarFound = false; // whether we found thetaStar yet (or are close enough to it)

        double thetaA = 0;
        double thetaB = 1;

        // check if either endpoint is the event
        if( ga == 0 )
        {
            thetaStarFound = true;
            thetaStar = thetaA;
        }
        else if( gb == 0 )
        {
            thetaStarFound = true;
            thetaStar = thetaB;
        }

        secError = Math.abs(thetaB - thetaA); // error is simply the bracket that the root exists in

        /*
          therefore the event must occur somewhere in between, this is essentially
          root finding problem that we are going to solve with safeguarded secant
          method, a hybrid method of secant and bisection root finding methods, so
          we loop until we converge to a thetaStar that is as close as the tolerance
         */while( !thetaStarFound && ( secError > SECTOL ) )
        {
            thetaStar = thetaB - gb * ( thetaB - thetaA ) / ( gb - ga ); // attemt a secant method iteration

            final Btheta btheta = butcher.get_btheta();
            /*
              if the approximation falls outside of the
              bracket, fall back on the reliability of
              bisection
             */
            if( ( thetaStar <= thetaA ) || ( thetaStar >= thetaB ) )
            {
                thetaStar = 0.5 * ( thetaA + thetaB ); // a bisection iteration

                /*
                  this loop takes the weighted average of all of the rows in the
                  K matrix using the functions of theta of the Butcher tableau
                  -> this loop is the weighted average for an ERK method and it
                  is used to interpolate two solution points
                 */
                for( int j = 0; j < s; j++ ) // loop for interpolant
                {
                    StdMet.stam(stam1, h * btheta.f(thetaStar)[j], K[j]); // h*f(thetaStar)[i]*K[i]

                    StdMet.arraySum(sigmaInterp, sigmaInterp, stam1); // sigmaInterp = sigmaInterp + h*f(thetaStar)[i]*K[i]
                }

                StdMet.arraySum(x_interp, xold, sigmaInterp); // x_interp = xold + sigmaInterp

                StdMet.zeroArray(sigmaInterp); // clear out sigmaInterp for the next sigmaInterp

                // now we have the interpolated point (x_interp) due to interpolation

                gAtTStar = f.checkEvent(told + h * thetaStar, x_interp)[i]; // evaluate g at above point

                if( gAtTStar == 0 ) // if interpolated point is the event point exactly, it has gone far enough
                {
                    thetaStarFound = true;
                }

                if( ga * gAtTStar >= 0 ) // else we see which side that event fell on and close
                {
                    thetaA = thetaStar; // a side of the bracket accordingly
                }
                else
                {
                    thetaB = thetaStar;

                    /*
                      here, error is based on the bracket
                      size only (safer than just saying
                      error is cut in half)
                     */
                }
                secError = thetaB - thetaA;
            }
            else
            // else go on with the faster secant iteration
            {
                /*
                  error is estimated as the difference between the iterates
                  (thetaB of last step and thetaStar of this step) added to a small
                  value dependent on thetaStar so that error does not equal 0,
                  (because that is impossible)
                 */
                secError = Math.abs(thetaStar - thetaB) + SECTOL / 4.0;

                /*
                  this loop takes the weighted average of all of the rows in the
                  K matrix using the functions of theta of the Butcher tableau
                  -> this loop is the weighted average for an ERK method and it
                  is used to interpolate two solution points
                 */
                for( int j = 0; j < s; j++ ) // loop for interpolant
                {
                    StdMet.stam(stam1, h * btheta.f(thetaStar)[j], K[j]); // h*f(thetaStar)[i]*K[i]
                    StdMet.arraySum(sigmaInterp, sigmaInterp, stam1); // sigmaInterp = sigmaInterp + h*f(thetaStar)[i]*K[i]
                }

                StdMet.arraySum(x_interp, xold, sigmaInterp); // x_interp = xold + sigmaInterp

                StdMet.zeroArray(sigmaInterp); // clear out sigmaInterp for the next sigmaInterp

                // now we have the interpolated point (x_interp) due to interpolation

                gAtTStar = f.checkEvent(told + h * thetaStar, x_interp)[i];

                if( gAtTStar == 0 ) // if interpolated point is the event point exactly, it has gone far enough
                {
                    thetaStarFound = true;

                    // else bring the bracket over to the new approx of the root (closing in on the root)

                }
                if( ga * gAtTStar >= 0 )
                {
                    thetaA = thetaStar;
                }
                else
                {
                    thetaB = thetaStar;
                }
            }
        }

        return ( thetaStar/*-SECTOL*/);
    }

    // instance variables

    private OdeModel f;
    private Btableau butcher;
    private double h;
    private double told;
    private int n;
    private double[] xold;
    private int s;
    private double[][] K;


    private boolean thetaStarFound; // whether we found thetaStar yet (or are close enough to it)
    private double thetaStar; // the value of thetaStar (as we converge to true answer)
    private double secError; // current error in the safeguarded secant method

    private double gAtTStar; // stores an evalutaion of g

    // interpolation

    private double[] sigmaInterp;
    private double[] x_interp;
    private double[] stam1;

    // finals

    public static final double SECTOL = 1.0E-10; // tol = 10^-10 for secant method (sufficient for most problems)

}
