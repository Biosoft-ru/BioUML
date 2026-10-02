package biouml.plugins.simulation.ode.jvode.jmh;

/**
 * wrmsNorm with independent partial sums. NOT part of the patch.
 * <p>
 * A single-accumulator FP sum is limited by add latency: every {@code sum += p * p}
 * waits for the previous one, and unrolling does not change that. Splitting the sum
 * into k independent chains lets the CPU overlap k adds. The price is a different
 * rounding order, so results can differ from the scalar loop by a few ULPs;
 * {@link Verify} reports by how much, and how each variant compares with an exact sum.
 */
public final class MultiAccumulator
{
    private MultiAccumulator()
    {
    }

    /** Two partial sums (even / odd elements), combined at the end. */
    public static double wrmsNorm2(double[] x, double[] w)
    {
        int n = x.length;
        double s0 = 0, s1 = 0;
        int i = 0;
        for( ; i + 1 < n; i += 2 )
        {
            double p0 = x[i] * w[i];
            double p1 = x[i + 1] * w[i + 1];
            s0 += p0 * p0;
            s1 += p1 * p1;
        }
        if( i < n )
        {
            double p = x[i] * w[i];
            s0 += p * p;
        }
        return Math.sqrt( ( s0 + s1 ) / n );
    }

    /** Four partial sums (element index mod 4), combined pairwise at the end. */
    public static double wrmsNorm4(double[] x, double[] w)
    {
        int n = x.length;
        double s0 = 0, s1 = 0, s2 = 0, s3 = 0;
        int i = 0;
        for( ; i + 3 < n; i += 4 )
        {
            double p0 = x[i] * w[i];
            double p1 = x[i + 1] * w[i + 1];
            double p2 = x[i + 2] * w[i + 2];
            double p3 = x[i + 3] * w[i + 3];
            s0 += p0 * p0;
            s1 += p1 * p1;
            s2 += p2 * p2;
            s3 += p3 * p3;
        }
        for( ; i < n; i++ )
        {
            double p = x[i] * w[i];
            s0 += p * p;
        }
        return Math.sqrt( ( ( s0 + s1 ) + ( s2 + s3 ) ) / n );
    }
}
