package biouml.plugins.simulation.ode.jvode.jmh;

import java.util.Random;

/** Test-data generators shared by the benchmark and {@link Verify}. */
final class Data
{
    private Data()
    {
    }

    /** n values uniform in [lo, hi). */
    static double[] uniform(Random rnd, int n, double lo, double hi)
    {
        double[] v = new double[n];
        for( int i = 0; i < n; i++ )
            v[i] = lo + ( hi - lo ) * rnd.nextDouble();
        return v;
    }

    /** n values with magnitudes log-uniform in [10^minExp, 10^maxExp), random sign if requested. */
    static double[] logUniform(Random rnd, int n, double minExp, double maxExp, boolean randomSign)
    {
        double[] v = new double[n];
        for( int i = 0; i < n; i++ )
        {
            double m = Math.pow( 10, minExp + ( maxExp - minExp ) * rnd.nextDouble() );
            v[i] = randomSign && rnd.nextBoolean() ? -m : m;
        }
        return v;
    }

    private static final double[] SPECIAL = {0.0, -0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN,
            Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_NORMAL / 3, Double.MAX_VALUE, -Double.MAX_VALUE};

    /**
     * Mostly ordinary values spanning 16 orders of magnitude, with about 10% special values
     * (signed zeros, infinities, NaN, subnormals, MAX_VALUE). Used only for correctness checks:
     * subnormals are far slower on most CPUs, so never time with this data.
     */
    static double[] mixed(Random rnd, int n)
    {
        double[] v = logUniform( rnd, n, -8, 8, true );
        for( int i = 0; i < n; i++ )
            if( rnd.nextInt( 10 ) == 0 )
                v[i] = SPECIAL[rnd.nextInt( SPECIAL.length )];
        return v;
    }
}
