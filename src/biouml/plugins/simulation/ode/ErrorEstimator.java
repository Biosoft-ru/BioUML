package biouml.plugins.simulation.ode;


/*
  class contains 2 error estimation schemes, one for embedded methods, and the other for
  methods with step doubling
 */
public class ErrorEstimator
{
    private ErrorEstimator()
    {
    }

    public static double[] embeddedEstimate(double h, double[] xold, double[] xnew, double[] xe, double[] atol, double[] rtol, double p,
            double aMax, double aMin, double alpha) throws Exception
    {
        // initializations

        int n = xold.length;

        double[] tau = new double[n];
        double[] ad1 = new double[n];
        double[] dq1 = new double[n];

        // calculations
        /*
        System.out.println("h = " + h);
        System.out.print("xold = ");
                for (int i = 0; i < xold.length; i++)
                    System.out.print(xold[i] + " ");
                System.out.print("\nxnew = ");
                        for (int i = 0; i < xnew.length; i++)
                            System.out.print(xnew[i] + " ");
        System.out.print("\n");


        System.out.println("\nxe = ");
                for (int i = 0; i < xe.length; i++)
                    System.out.print(xe[i] + " ");

        System.out.println("\natol = ");
                for (int i = 0; i < atol.length; i++)
                    System.out.print(atol[i] + " ");

        System.out.println("\nrtol = ");
                for (int i = 0; i < rtol.length; i++)
                    System.out.print(rtol[i] + " ");

        System.out.println("p = " + p);
        System.out.println("aMin = " + aMin);
        System.out.println("aMax = " + aMax);
        System.out.println("alpha = " + alpha);
        */
        StdMet.tau(tau, xold, xnew, atol, rtol); // get the array of
        // tolerances for this step

        StdMet.arrayDiff(ad1, xnew, xe); // (higer order solution - lower order solution)

        StdMet.dotQuo(dq1, ad1, tau); // ad1/tau

        double epsilon = StdMet.normRMS(dq1); // epsilon = rmsNorm(ad1/tau)


        // calculate h and see if it is optimal

        double hOpt = h * Math.pow( ( 1.0 / epsilon ), 1.0 / p); // calculate
        // optimal stepsize for next step

        double hNew = Math.min(aMax * h, Math.max(aMin * h, alpha * hOpt));
        // calculate hNew with h and hOpt

        double[] estimation = new double[2];

        estimation[0] = epsilon;
        estimation[1] = hNew;

        return estimation;
    }

    public static double[] stepdoublingEstimate(double h, double[] eta1, double[] eta2, double[] xtemp, double atol, double rtol,
            double p, double aMax,double aMin, double alpha) throws Exception
    {
        // initializations

        int n = eta1.length;

        double[] diff1 = new double[n];
        double[] err = new double[n];
        double[] eps = new double[n];
        double[] est = new double[n];

        // calculations

        StdMet.arrayDiff(diff1, eta2, eta1); // get error
        StdMet.stam(err, 1 / ( Math.pow(2, p) - 1 ), diff1);

        StdMet.epsilon(eps, xtemp, eta1, atol, rtol); // get epsilon
        StdMet.dotQuo(est, err, eps);

        double norm = StdMet.normRMS(est); // take norm of est

        // calculate h and see if it is optimal

        double hopt = h / ( Math.pow(Math.pow(2, p) * norm, ( 1 / ( p + 1 ) )) );

        double hNew = Math.min(aMax * h, Math.max( aMin * h, alpha * 2 * hopt));
        // calculate hNew with h and hOpt

        double[] estimation = new double[3];

        estimation[0] = hNew;
        estimation[1] = hopt;
        estimation[2] = norm;

        return estimation;
    }
}
