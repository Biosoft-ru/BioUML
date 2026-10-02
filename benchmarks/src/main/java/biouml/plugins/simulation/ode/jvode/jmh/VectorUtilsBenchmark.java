package biouml.plugins.simulation.ode.jvode.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * A/B microbenchmark for commit d6308847 ("unroll remaining VectorUtils hot loops in JVode solver").
 * <p>
 * Each operation is measured as {@code <operation>_scalar} (pre-patch loop, {@link Scalar}) and
 * {@code <operation>_unrolled} (patched loop, {@link Unrolled}). wrmsNorm additionally has
 * {@code _acc2} / {@code _acc4}: multi-accumulator variants that are faster in principle but not
 * bit-identical (see {@link MultiAccumulator}). summarize.py relies on this naming.
 * <p>
 * Each benchmark runs in its own forked JVM, so the JIT profile of one variant cannot
 * influence another.
 * <p>
 * Data: x, y, z uniform in [-1, 1), w uniform in [0.5, 2). All values stay normal (no subnormals,
 * which would dominate the timing). In-place operations are set up so repeated calls cannot drift
 * into overflow or underflow:
 * <ul>
 * <li>scaleInPlace multiplies by -1.0, so magnitudes never change;</li>
 * <li>linearSumAddScaled (z += a*x) and substract (z -= x) drift linearly. Even after 10^10 calls
 * |z| stays around 10^10, far from overflow, and FP add/sub cost does not depend on magnitude.</li>
 * </ul>
 */
@BenchmarkMode( Mode.AverageTime )
@OutputTimeUnit( TimeUnit.NANOSECONDS )
@Warmup( iterations = 3, time = 1 )
@Measurement( iterations = 5, time = 1 )
@Fork( 2 )
@State( Scope.Thread )
public class VectorUtilsBenchmark
{
    /** Vector length, i.e. the number of ODE state variables. Override with -p n=... */
    @Param( {"8", "32", "128", "512", "2048"} )
    public int n;

    // Instance fields, not constants: C2 cannot fold them into the loops,
    // just as it cannot fold the arguments JVode passes at its call sites.
    private double a = 1.3;
    private double b = -0.7;
    private double c = -3.1;
    private double sign = -1.0;

    private double[] x;
    private double[] y;
    private double[] w;
    private double[] z;

    @Setup( Level.Trial )
    public void setUp()
    {
        Random rnd = new Random( 42 );
        x = Data.uniform( rnd, n, -1, 1 );
        y = Data.uniform( rnd, n, -1, 1 );
        w = Data.uniform( rnd, n, 0.5, 2 );
        z = Data.uniform( rnd, n, -1, 1 );
    }

    // ---- wrmsNorm: sqrt(sum((x*w)^2) / n) ----------------------------------------------------

    @Benchmark
    public double wrmsNorm_scalar()
    {
        return Scalar.wrmsNorm( x, w );
    }

    @Benchmark
    public double wrmsNorm_unrolled()
    {
        return Unrolled.wrmsNorm( x, w );
    }

    @Benchmark
    public double wrmsNorm_acc2()
    {
        return MultiAccumulator.wrmsNorm2( x, w );
    }

    @Benchmark
    public double wrmsNorm_acc4()
    {
        return MultiAccumulator.wrmsNorm4( x, w );
    }

    // ---- scale(c, x, z): z = c*x ---------------------------------------------------------------

    @Benchmark
    public void scaleCopy_scalar()
    {
        Scalar.scale( c, x, z );
    }

    @Benchmark
    public void scaleCopy_unrolled()
    {
        Unrolled.scale( c, x, z );
    }

    // ---- scale(a, x): x *= a, in place ----------------------------------------------------------

    @Benchmark
    public void scaleInPlace_scalar()
    {
        Scalar.scale( sign, z );
    }

    @Benchmark
    public void scaleInPlace_unrolled()
    {
        Unrolled.scale( sign, z );
    }

    // ---- linearDiff(x, y, z): z = x - y ---------------------------------------------------------

    @Benchmark
    public void linearDiff_scalar()
    {
        Scalar.linearDiff( x, y, z );
    }

    @Benchmark
    public void linearDiff_unrolled()
    {
        Unrolled.linearDiff( x, y, z );
    }

    // ---- linearDiff(a, x, y, z): z = a*x - y ----------------------------------------------------

    @Benchmark
    public void linearDiffScaled_scalar()
    {
        Scalar.linearDiff( a, x, y, z );
    }

    @Benchmark
    public void linearDiffScaled_unrolled()
    {
        Unrolled.linearDiff( a, x, y, z );
    }

    // ---- linearSum(a, x, y, z): z = a*x + y -----------------------------------------------------

    @Benchmark
    public void linearSumAxPlusY_scalar()
    {
        Scalar.linearSum( a, x, y, z );
    }

    @Benchmark
    public void linearSumAxPlusY_unrolled()
    {
        Unrolled.linearSum( a, x, y, z );
    }

    // ---- linearSum(x, b, y, z): z = x + b*y -----------------------------------------------------

    @Benchmark
    public void linearSumXPlusBy_scalar()
    {
        Scalar.linearSum( x, b, y, z );
    }

    @Benchmark
    public void linearSumXPlusBy_unrolled()
    {
        Unrolled.linearSum( x, b, y, z );
    }

    // ---- linearSum(a, x, z): z += a*x, in place --------------------------------------------------

    @Benchmark
    public void linearSumAddScaled_scalar()
    {
        Scalar.linearSum( a, x, z );
    }

    @Benchmark
    public void linearSumAddScaled_unrolled()
    {
        Unrolled.linearSum( a, x, z );
    }

    // ---- substract(x, z): z -= x, in place -------------------------------------------------------

    @Benchmark
    public void substract_scalar()
    {
        Scalar.substract( x, z );
    }

    @Benchmark
    public void substract_unrolled()
    {
        Unrolled.substract( x, z );
    }

    // ---- scaleDiff(a, x, y, z): z = a*(x - y) ----------------------------------------------------

    @Benchmark
    public void scaleDiff_scalar()
    {
        Scalar.scaleDiff( a, x, y, z );
    }

    @Benchmark
    public void scaleDiff_unrolled()
    {
        Unrolled.scaleDiff( a, x, y, z );
    }
}
