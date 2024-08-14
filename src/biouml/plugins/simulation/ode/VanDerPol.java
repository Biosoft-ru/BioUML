package biouml.plugins.simulation.ode;

import biouml.plugins.simulation.java.JavaBaseModel;


/*
   ODE function for the van der Pol problem, a stiff ODE
*/
public class VanDerPol extends JavaBaseModel
{
    @Override
    public double[] getInitialValues()
    {
        double[] result = new double[2];
        result[0] = 2.0; // - x0
        result[1] = 0.0; // - x1
        return result;

    }

    @Override
    public double[] dy_dt(double t, double[] x)
    {
        double[] xp = new double[x.length];

        xp[0] = x[1];
        xp[1] = ( ( 1 - x[0] * x[0] ) * x[1] - x[0] ) / eps;

        return ( xp );
    }

    // instance variables

    /*
       this class has instance variables, but it still properly
       implements Fv as it overloads the f and g methods
    */
    private static final double eps = 0.000001;
}