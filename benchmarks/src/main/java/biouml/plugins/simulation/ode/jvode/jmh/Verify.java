package biouml.plugins.simulation.ode.jvode.jmh;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Correctness companion to {@link VectorUtilsBenchmark}. Run it before trusting any timing:
 *
 * <pre>
 * java -cp target/benchmarks.jar biouml.plugins.simulation.ode.jvode.jmh.Verify
 * </pre>
 *
 * <ol>
 * <li>Checks that every {@link Unrolled} method is bit-identical to its {@link Scalar} original over
 * lengths 0..67 and around 128/512/2048, on data that includes signed zeros, infinities, NaN and
 * subnormals, and for aliased calls (z == x, z == y) as JVode makes them. All NaNs count as equal:
 * Java does not specify which NaN bit pattern arithmetic produces (see {@link #sameBits}).</li>
 * <li>Reports how far the {@link MultiAccumulator} wrmsNorm variants differ from the scalar result,
 * and how accurate each variant is against an exact BigDecimal sum.</li>
 * </ol>
 * Exits with status 1 if any bit-identity check fails.
 */
public final class Verify
{
    // Not powers of two, so the multiplications actually round. (With 2.0, a*(x-y) and a*x - a*y
    // give the same bits, and a test could not tell them apart.)
    private static final double A = 1.3;
    private static final double B = -0.7;

    private Verify()
    {
    }

    public static void main(String[] args)
    {
        boolean ok = checkBitIdentity();
        System.out.println();
        wrmsNormAccuracy();
        if( !ok )
            System.exit( 1 );
    }

    // ---------------------------------------------------------------------------------------------
    // 1. Unrolled == Scalar, bit for bit
    // ---------------------------------------------------------------------------------------------

    private interface Kernel
    {
        void run(double[] x, double[] y, double[] z);
    }

    private static final class Op
    {
        final String name;
        final Kernel scalar;
        final Kernel unrolled;

        Op(String name, Kernel scalar, Kernel unrolled)
        {
            this.name = name;
            this.scalar = scalar;
            this.unrolled = unrolled;
        }
    }

    private static List<Op> ops()
    {
        List<Op> ops = new ArrayList<>();
        ops.add( new Op( "scale(c,x,z)", (x, y, z) -> Scalar.scale( A, x, z ), (x, y, z) -> Unrolled.scale( A, x, z ) ) );
        ops.add( new Op( "scale(a,x)", (x, y, z) -> Scalar.scale( A, z ), (x, y, z) -> Unrolled.scale( A, z ) ) );
        ops.add( new Op( "linearDiff(x,y,z)", Scalar::linearDiff, Unrolled::linearDiff ) );
        ops.add( new Op( "linearDiff(a,x,y,z)", (x, y, z) -> Scalar.linearDiff( A, x, y, z ),
                (x, y, z) -> Unrolled.linearDiff( A, x, y, z ) ) );
        ops.add( new Op( "linearSum(a,x,y,z)", (x, y, z) -> Scalar.linearSum( A, x, y, z ),
                (x, y, z) -> Unrolled.linearSum( A, x, y, z ) ) );
        ops.add( new Op( "linearSum(x,b,y,z)", (x, y, z) -> Scalar.linearSum( x, B, y, z ),
                (x, y, z) -> Unrolled.linearSum( x, B, y, z ) ) );
        ops.add( new Op( "linearSum(a,x,z)", (x, y, z) -> Scalar.linearSum( A, x, z ),
                (x, y, z) -> Unrolled.linearSum( A, x, z ) ) );
        ops.add( new Op( "substract(x,z)", (x, y, z) -> Scalar.substract( x, z ), (x, y, z) -> Unrolled.substract( x, z ) ) );
        ops.add( new Op( "scaleDiff(a,x,y,z)", (x, y, z) -> Scalar.scaleDiff( A, x, y, z ),
                (x, y, z) -> Unrolled.scaleDiff( A, x, y, z ) ) );
        return ops;
    }

    private enum Alias
    {
        NONE, Z_IS_X, Z_IS_Y
    }

    private static int[] lengths()
    {
        int[] extra = {127, 128, 129, 511, 512, 513, 2047, 2048, 2049};
        int[] all = new int[68 + extra.length];
        for( int i = 0; i < 68; i++ )
            all[i] = i;
        System.arraycopy( extra, 0, all, 68, extra.length );
        return all;
    }

    private static boolean checkBitIdentity()
    {
        System.out.println( "1. Unrolled vs Scalar: bit-identity" );
        Random rnd = new Random( 2026_10_01L );
        List<Op> ops = ops();
        long comparisons = 0;
        int mismatches = 0;

        for( int n : lengths() )
        {
            int trials = n < 68 ? 25 : 5;
            for( int t = 0; t < trials; t++ )
            {
                double[] x = Data.mixed( rnd, n );
                double[] y = Data.mixed( rnd, n );
                double[] z = Data.mixed( rnd, n );

                // wrmsNorm (n = 0 gives NaN in both versions: 0.0 / 0)
                double ws = Scalar.wrmsNorm( x, y );
                double wu = Unrolled.wrmsNorm( x, y );
                comparisons++;
                if( !sameBits( ws, wu ) )
                    mismatches += report( "wrmsNorm", n, Alias.NONE, "result", -1, ws, wu, mismatches );

                for( Op op : ops )
                {
                    for( Alias alias : Alias.values() )
                    {
                        double[][] s = setUp( x, y, z, alias );
                        double[][] u = setUp( x, y, z, alias );
                        op.scalar.run( s[0], s[1], s[2] );
                        op.unrolled.run( u[0], u[1], u[2] );
                        String[] names = {"x", "y", "z"};
                        for( int k = 0; k < 3; k++ )
                        {
                            comparisons++;
                            int at = firstBitDifference( s[k], u[k] );
                            if( at >= 0 )
                                mismatches += report( op.name, n, alias, names[k], at, s[k][at], u[k][at], mismatches );
                        }
                    }
                }
            }
        }
        System.out.printf( "   %,d array/result comparisons over %d lengths, %d mismatches -> %s%n", comparisons,
                lengths().length, mismatches, mismatches == 0 ? "OK" : "FAILED" );
        return mismatches == 0;
    }

    /** Fresh copies of x, y, z, with z replaced by x or y when aliased. */
    private static double[][] setUp(double[] x, double[] y, double[] z, Alias alias)
    {
        double[] xc = x.clone();
        double[] yc = y.clone();
        double[] zc = alias == Alias.Z_IS_X ? xc : alias == Alias.Z_IS_Y ? yc : z.clone();
        return new double[][] {xc, yc, zc};
    }

    private static int firstBitDifference(double[] p, double[] q)
    {
        for( int i = 0; i < p.length; i++ )
            if( !sameBits( p[i], q[i] ) )
                return i;
        return -1;
    }

    /**
     * Same bits, except that all NaNs count as equal (doubleToLongBits, as Double.equals uses).
     * The sign and payload of a NaN produced by arithmetic are unspecified in Java: when two NaN
     * operands meet, which one propagates depends on the operand order the JIT happens to emit,
     * so even two compilations of the same loop can disagree. -0.0 and 0.0 are still distinguished.
     */
    private static boolean sameBits(double p, double q)
    {
        return Double.doubleToLongBits( p ) == Double.doubleToLongBits( q );
    }

    private static int report(String op, int n, Alias alias, String array, int index, double expected, double actual,
            int alreadyReported)
    {
        if( alreadyReported < 20 )
            System.out.printf( "   MISMATCH %s n=%d alias=%s %s[%d]: scalar=%s unrolled=%s%n", op, n, alias, array, index,
                    expected, actual );
        return 1;
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Multi-accumulator wrmsNorm: drift from scalar, and accuracy against an exact sum
    // ---------------------------------------------------------------------------------------------

    private interface Norm
    {
        double apply(double[] x, double[] w);
    }

    private static void wrmsNormAccuracy()
    {
        System.out.println( "2. wrmsNorm variants: error against an exact sum, and difference from the scalar result" );
        System.out.println( "   Errors in ULPs of the exact result. 'exact' sums the (double-rounded) products x[i]*w[i]" );
        System.out.println( "   squared and added without rounding, then rounds sqrt(sum/n) once." );
        System.out.println();
        System.out.printf( "   %-8s %5s  %-9s %13s %14s %16s %16s%n", "data", "n", "variant", "max err ulp", "mean err ulp",
                "differs/scalar", "max diff ulp" );

        String[] variantNames = {"scalar", "acc2", "acc4"};
        Norm[] variants = {Scalar::wrmsNorm, MultiAccumulator::wrmsNorm2, MultiAccumulator::wrmsNorm4};
        int samples = 1000;

        for( String data : new String[] {"uniform", "spread"} )
        {
            Random rnd = new Random( 7 );
            for( int n : new int[] {8, 32, 128, 512, 2048} )
            {
                double[] maxErr = new double[variants.length];
                double[] sumErr = new double[variants.length];
                int[] differs = new int[variants.length];
                double[] maxDiff = new double[variants.length];

                for( int s = 0; s < samples; s++ )
                {
                    double[] x;
                    double[] w;
                    if( data.equals( "uniform" ) )
                    {
                        // Same distribution as the benchmark: terms of similar size.
                        x = Data.uniform( rnd, n, -1, 1 );
                        w = Data.uniform( rnd, n, 0.5, 2 );
                    }
                    else
                    {
                        // Corrections 1e-6..1e-2 times weights 1e2..1e6: terms spread over 8 decades,
                        // closer to what JVode feeds wrmsNorm.
                        x = Data.logUniform( rnd, n, -6, -2, true );
                        w = Data.logUniform( rnd, n, 2, 6, false );
                    }
                    double exact = exactWrmsNorm( x, w );
                    double ulp = Math.ulp( exact );
                    double scalar = Scalar.wrmsNorm( x, w );

                    for( int v = 0; v < variants.length; v++ )
                    {
                        double r = variants[v].apply( x, w );
                        double err = Math.abs( r - exact ) / ulp;
                        maxErr[v] = Math.max( maxErr[v], err );
                        sumErr[v] += err;
                        if( !sameBits( r, scalar ) )
                        {
                            differs[v]++;
                            maxDiff[v] = Math.max( maxDiff[v], Math.abs( r - scalar ) / Math.ulp( scalar ) );
                        }
                    }
                }
                for( int v = 0; v < variants.length; v++ )
                    System.out.printf( "   %-8s %5d  %-9s %13.2f %14.3f %15.1f%% %16.1f%n", data, n, variantNames[v], maxErr[v],
                            sumErr[v] / samples, 100.0 * differs[v] / samples, maxDiff[v] );
            }
        }
    }

    /** sqrt(sum(p_i^2) / n) where p_i = x[i]*w[i] rounded to double, as in every variant; rest exact. */
    private static double exactWrmsNorm(double[] x, double[] w)
    {
        BigDecimal sum = BigDecimal.ZERO;
        for( int i = 0; i < x.length; i++ )
        {
            BigDecimal p = new BigDecimal( x[i] * w[i] );
            sum = sum.add( p.multiply( p ) );
        }
        MathContext mc = new MathContext( 40 );
        return sum.divide( BigDecimal.valueOf( x.length ), mc ).sqrt( mc ).doubleValue();
    }
}
