package biouml.plugins.simulation.ode;


/*
   interpolant for the Dormand-Prince scheme
 */
public class BthDopr implements Btheta
{

    // class still properly overloads the Btheta class
    public BthDopr(double[] b) throws Exception
    { // as it overloads the f method
        this.b = new double[b.length];

        StdMet.copyArray(this.b, b); // get b's from Butcher scheme, as they are
        // needed for each evaluation of the interpolant function
    }

    // methods

    @Override
    public double[] f(double theta) // the interpolant function
    {
        double[] bTh = new double[7]; // 7 is the number of stages in Dormand-Prince

        bTh[0] = theta1(theta) * b[0] + theta * (theta - 1.0) * (theta - 1.0) - theta2(theta) * 5.0 * (2558722523.0 - 31403016.0 * theta) / 11282082432.0;
        bTh[1] = 0.0;
        bTh[2] = theta1(theta) * b[2] + theta2(theta) * 100.0 * (882725551.0 - 15701508.0 * theta) / 32700410799.0;
        bTh[3] = theta1(theta) * b[3] - theta2(theta) * 25.0 * (443332067.0 - 31403016.0 * theta) / 1880347072.0;
        bTh[4] = theta1(theta) * b[4] + theta2(theta) * 32805.0 * (23143187.0 - 3489224.0 * theta) / 199316789632.0;
        bTh[5] = theta1(theta) * b[5] - theta2(theta) * 55.0 * (29972135.0 - 7076736.0 * theta) / 822651844.0;
        bTh[6] = theta * theta * (theta - 1.0) + theta2(theta) * 10.0 * (7414447.0 - 829305.0 * theta) / 29380423.0;

        return (bTh);
    }

    /*
       1st method that does a calculation that is common among the weights in
       the interpolant function
     */
    public double theta1(double theta)
    {
        return (theta * theta * (3.0 - 2.0 * theta));
    }

    /*
       2nd method that does a calculation that is common among the weights in
       the interpolant function
     */
    public double theta2(double theta)
    {
        return (theta * theta * (theta - 1.0) * (theta - 1.0));
    }

    // instance variables

    private double[] b; // the b array of the Dormand-Prince Butcher tableau
}