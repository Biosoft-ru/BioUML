package biouml.plugins.simulation.ode.jvode.jmh;

/**
 * The VectorUtils loops as changed by commit d6308847 (unrolled by 4),
 * copied verbatim from the patch.
 *
 * This is the candidate ("_unrolled") side of every A/B pair in {@link VectorUtilsBenchmark}.
 * If VectorUtils changes, update this file and {@link Scalar} together.
 */
public final class Unrolled
{
    private Unrolled()
    {
    }

    /** z = c * x */
    public static void scale(double c, double[] x, double[] z)
    {
        int n = x.length;
        int i = 0;
        // Unroll by 4: used in getDky / Adams predictor / error-test paths.
        for( ; i + 3 < n; i += 4 )
        {
            z[i]     = c * x[i];
            z[i + 1] = c * x[i + 1];
            z[i + 2] = c * x[i + 2];
            z[i + 3] = c * x[i + 3];
        }
        for( ; i < n; i++ )
            z[i] = c * x[i];
    }

    /** sqrt( sum((x[i] * w[i])^2) / n ), single accumulator, original order */
    public static double wrmsNorm(double[] x, double[] w)
    {
        double prodi;
        double sum = 0;
        int n = x.length;
        int i = 0;
        // Unroll by 4: wrmsNorm runs in the Newton-iteration inner loop (and the
        // local-error / step-size test), so reduce per-element loop overhead.
        // The left-to-right accumulation order is preserved exactly, so the
        // result is bit-identical to the scalar loop.
        for( ; i + 3 < n; i += 4 )
        {
            prodi = x[i] * w[i];
            sum += prodi * prodi;
            prodi = x[i + 1] * w[i + 1];
            sum += prodi * prodi;
            prodi = x[i + 2] * w[i + 2];
            sum += prodi * prodi;
            prodi = x[i + 3] * w[i + 3];
            sum += prodi * prodi;
        }
        for( ; i < n; i++ )
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
        int i = 0;
        // Unroll by 4: used in the error-test / correction accumulation paths.
        for( ; i + 3 < n; i += 4 )
        {
            z[i]     = x[i]     - y[i];
            z[i + 1] = x[i + 1] - y[i + 1];
            z[i + 2] = x[i + 2] - y[i + 2];
            z[i + 3] = x[i + 3] - y[i + 3];
        }
        for( ; i < n; i++ )
            z[i] = x[i] - y[i];
    }

    /** z = a * (x - y) */
    public static void scaleDiff(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        int i = 0;
        // Unroll by 4: written once per column of the finite-difference Jacobian.
        for( ; i + 3 < n; i += 4 )
        {
            z[i]     = a * ( x[i]     - y[i] );
            z[i + 1] = a * ( x[i + 1] - y[i + 1] );
            z[i + 2] = a * ( x[i + 2] - y[i + 2] );
            z[i + 3] = a * ( x[i + 3] - y[i + 3] );
        }
        for( ; i < n; i++ )
            z[i] = a * ( x[i] - y[i] );
        return;
    }

    /** z = a * x + y */
    public static void linearSum(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        int i = 0;
        // Unroll by 4: used in the Adams / setTq coefficient paths.
        for( ; i + 3 < n; i += 4 )
        {
            z[i]     = a * x[i]     + y[i];
            z[i + 1] = a * x[i + 1] + y[i + 1];
            z[i + 2] = a * x[i + 2] + y[i + 2];
            z[i + 3] = a * x[i + 3] + y[i + 3];
        }
        for( ; i < n; i++ )
            z[i] = a * x[i] + y[i];
    }

    /** z = x + b * y */
    public static void linearSum(double[] x, double b, double[] y, double[] z)
    {
        int n = x.length;
        int i = 0;
        // Unroll by 4: used in the EwtSet weight construction and Adams paths.
        for( ; i + 3 < n; i += 4 )
        {
            z[i]     = x[i]     + b * y[i];
            z[i + 1] = x[i + 1] + b * y[i + 1];
            z[i + 2] = x[i + 2] + b * y[i + 2];
            z[i + 3] = x[i + 3] + b * y[i + 3];
        }
        for( ; i < n; i++ )
            z[i] = x[i] + b * y[i];
    }

    /** z += a * x */
    public static void linearSum(double a, double[] x, double[] z)
    {
        int n = x.length;
        int i = 0;
        // Unroll by 4: used in the Adams order-raising path.
        for( ; i + 3 < n; i += 4 )
        {
            z[i]     += a * x[i];
            z[i + 1] += a * x[i + 1];
            z[i + 2] += a * x[i + 2];
            z[i + 3] += a * x[i + 3];
        }
        for( ; i < n; i++ )
            z[i] += a * x[i];
    }

    /** z = a * x - y */
    public static void linearDiff(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        int i = 0;
        // Unroll by 4: runs once per Newton-iteration residual evaluation.
        for( ; i + 3 < n; i += 4 )
        {
            z[i]     = a * x[i]     - y[i];
            z[i + 1] = a * x[i + 1] - y[i + 1];
            z[i + 2] = a * x[i + 2] - y[i + 2];
            z[i + 3] = a * x[i + 3] - y[i + 3];
        }
        for( ; i < n; i++ )
            z[i] = a * x[i] - y[i];
    }

    /** x *= a */
    public static void scale(double a, double[] x)
    {
        int n = x.length;
        int i = 0;
        // Unroll by 4: in-place scaling on the BDF correction vector.
        for( ; i + 3 < n; i += 4 )
        {
            x[i]     *= a;
            x[i + 1] *= a;
            x[i + 2] *= a;
            x[i + 3] *= a;
        }
        for( ; i < n; i++ )
            x[i] *= a;
    }

    /** z -= x */
    public static void substract(double[] x, double[] z)
    {
        int n = x.length;
        int i = 0;
        // Unroll by 4: Adams order-lowering (adamsOrderDown) path.
        for( ; i + 3 < n; i += 4 )
        {
            z[i]     -= x[i];
            z[i + 1] -= x[i + 1];
            z[i + 2] -= x[i + 2];
            z[i + 3] -= x[i + 3];
        }
        for( ; i < n; i++ )
            z[i] -= x[i];
    }
}
