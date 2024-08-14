package biouml.plugins.simulation.ode;


/*
  class contains a stiffness detector which returns a number representing how close
  to the stability region of the method the solver is, when solving the IVP
 */
public class StiffnessDetector
{
    private StiffnessDetector()
    {
    }

    public static double calc_hRho(double h, double[] K7, double[] K6, double[] g7, double[] g6) throws Exception
    {
        int n = K7.length;

        double[] diff1 = new double[n]; // 2 array differences
        double[] diff2 = new double[n];
        double norm1;
        double norm2;
        double rho;
        double hRho; // how close to boundary we are

        StdMet.arrayDiff(diff1, K7, K6); // K7 - K6
        StdMet.arrayDiff(diff2, g7, g6); // g7 - g6

        norm1 = StdMet.normRMS(diff1); // ||K7 - K6||
        norm2 = StdMet.normRMS(diff2); // ||g7 - g6||

        rho = norm1 / norm2;
        hRho = h * rho;

        return (hRho);
    }
}
