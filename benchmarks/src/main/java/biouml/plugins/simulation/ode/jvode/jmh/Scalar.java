package biouml.plugins.simulation.ode.jvode.jmh;

/**
 * The VectorUtils loops as they were BEFORE commit d6308847 (parent 97c8dac1),
 * copied from biouml.plugins.simulation.ode.jvode.VectorUtils.
 *
 * This is the baseline ("_scalar") side of every A/B pair in {@link VectorUtilsBenchmark}.
 * If VectorUtils changes, update this file and {@link Unrolled} together.
 */
public final class Scalar
{
    private Scalar()
    {
    }

    /** z = c * x */
    public static void scale(double c, double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
            z[i] = c * x[i];
    }

    /**
     * sqrt( sum((x[i] * w[i])^2) / n )
     * <p>
     * The return statement is outside the diff context; it is taken to be
     * {@code Math.sqrt( sum / n )}, which is what VectorUtilsOptimizationTest expects.
     */
    public static double wrmsNorm(double[] x, double[] w)
    {
        double prodi;
        double sum = 0;
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            prodi = x[i] * w[i];
            sum += prodi * prodi;
        }
        return Math.sqrt( sum / n );
    }

    /** z = x - y */
    public static void linearDiff(double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = x[i] - y[i];
        }
    }

    /** z = a * (x - y) */
    public static void scaleDiff(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = a * ( x[i] - y[i] );
        }
        return;
    }

    /** z = a * x + y */
    public static void linearSum(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = a * x[i] + y[i];
        }
    }

    /** z = x + b * y */
    public static void linearSum(double[] x, double b, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = x[i] + b * y[i];
        }
    }

    /** z += a * x */
    public static void linearSum(double a, double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] += a * x[i];
        }
    }

    /** z = a * x - y */
    public static void linearDiff(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = a * x[i] - y[i];
        }
    }

    /** x *= a */
    public static void scale(double a, double[] x)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            x[i] *= a;
        }

    }

    /** z -= x */
    public static void substract(double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] -= x[i];
        }
    }
}
