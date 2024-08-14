package biouml.plugins.simulation.ode.jvode;

public class Util
{
    /**
     * (MAT027) if x is smaller then a returns a if x biger then b returns
     * @return a if x < a and b if x > b
     * @throws Exception
     *             if a > b
     */
    public static double restrict(double a, double b, double x)
    {
        if( a > b )
            throw new IllegalArgumentException("Left bound is lesser then right");
        return Math.max(Math.min(b, x), a);
    }
}
