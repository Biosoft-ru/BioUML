package biouml.plugins.simulation.ode._test;

import junit.framework.TestCase;

import biouml.plugins.simulation.ode.jvode.VectorUtils;

/**
 * Regression tests for the unrolled-by-4 VectorUtils hot paths. The unrolling
 * changed the loop bounds, so the lengths 0, 1, 2, 3, 4, 5, 7, 8, 9 are
 * covered explicitly (all boundary alignments of the unrolled block plus the
 * scalar remainder).
 */
public class VectorUtilsOptimizationTest extends TestCase
{
    private static final int[] LENGTHS = { 0, 1, 2, 3, 4, 5, 7, 8, 9, 12, 13 };

    public VectorUtilsOptimizationTest(String name)
    {
        super(name);
    }

    public void testLinearSumAxByAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] y = pattern( n, -0.7 );
            double[] z = new double[n];
            VectorUtils.linearSum( 0.25, x, 3.0, y, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "linearSum(n=" + n + ", i=" + i + ")",
                    0.25 * x[i] + 3.0 * y[i], z[i], 0.0 );
        }
    }

    public void testLinearSumXPlusYAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] y = pattern( n, -0.7 );
            double[] z = new double[n];
            VectorUtils.linearSum( x, y, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "linearSum x+y (n=" + n + ", i=" + i + ")",
                    x[i] + y[i], z[i], 0.0 );
        }
    }

    public void testAddAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] z = pattern( n, -2.1 );
            double[] expected = new double[n];
            for( int i = 0; i < n; i++ )
                expected[i] = z[i] + x[i];
            VectorUtils.add( x, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "add(n=" + n + ", i=" + i + ")", expected[i], z[i], 0.0 );
        }
    }

    public void testLinearSumSpecialValues()
    {
        // -0.0 and +0.0: addition semantics must be preserved exactly
        double[] x = { 0.0, 1.0, -1.0 };
        double[] y = { -0.0, 2.0, 0.0 };
        double[] z = new double[3];
        VectorUtils.linearSum( x, y, z );
        assertEquals( 0.0, z[0], 0.0 );
        assertEquals( 3.0, z[1], 0.0 );
        assertEquals( -1.0, z[2], 0.0 );
    }

    // The following methods were unrolled by 4 in the 2026-10-01 profiler
    // review (Newton-iteration / error-test / Jacobian hot paths). Each is a
    // pure elementwise transform, so the unrolled result must match the naive
    // math bit-for-bit (tolerance 0.0) across every boundary alignment.

    public void testWrmsNormAllLengths()
    {
        for( int n : LENGTHS )
        {
            if( n == 0 )
                continue; // wrmsNorm divides by n
            double[] x = pattern( n, 1.5 );
            double[] w = pattern( n, -0.7 );
            double ref = 0;
            for( int i = 0; i < n; i++ )
            {
                double p = x[i] * w[i];
                ref += p * p;
            }
            double expected = Math.sqrt( ref / n );
            assertEquals( "wrmsNorm(n=" + n + ")", expected, VectorUtils.wrmsNorm( x, w ), 0.0 );
        }
    }

    public void testLinearDiffAxMinusYAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] y = pattern( n, -0.7 );
            double[] z = new double[n];
            VectorUtils.linearDiff( 2.0, x, y, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "linearDiff(n=" + n + ", i=" + i + ")", 2.0 * x[i] - y[i], z[i], 0.0 );
        }
    }

    public void testLinearDiffXMinusYAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] y = pattern( n, -0.7 );
            double[] z = new double[n];
            VectorUtils.linearDiff( x, y, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "linearDiff x-y (n=" + n + ", i=" + i + ")", x[i] - y[i], z[i], 0.0 );
        }
    }

    public void testScaleCopyAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] z = new double[n];
            VectorUtils.scale( -3.0, x, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "scale(c,x,z)(n=" + n + ", i=" + i + ")", -3.0 * x[i], z[i], 0.0 );
        }
    }

    public void testScaleInPlaceAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] expected = x.clone();
            for( int i = 0; i < n; i++ )
                expected[i] *= -3.0;
            VectorUtils.scale( -3.0, x );
            for( int i = 0; i < n; i++ )
                assertEquals( "scale in-place(n=" + n + ", i=" + i + ")", expected[i], x[i], 0.0 );
        }
    }

    public void testLinearSumAxPlusYAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] y = pattern( n, -0.7 );
            double[] z = new double[n];
            VectorUtils.linearSum( 2.0, x, y, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "linearSum a*x+y (n=" + n + ", i=" + i + ")", 2.0 * x[i] + y[i], z[i], 0.0 );
        }
    }

    public void testLinearSumXPlusByAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] y = pattern( n, -0.7 );
            double[] z = new double[n];
            VectorUtils.linearSum( x, 2.0, y, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "linearSum x+b*y (n=" + n + ", i=" + i + ")", x[i] + 2.0 * y[i], z[i], 0.0 );
        }
    }

    public void testLinearSumAddScaledAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] z = pattern( n, -2.1 );
            double[] expected = z.clone();
            for( int i = 0; i < n; i++ )
                expected[i] += 2.0 * x[i];
            VectorUtils.linearSum( 2.0, x, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "linearSum z+=a*x (n=" + n + ", i=" + i + ")", expected[i], z[i], 0.0 );
        }
    }

    public void testSubstractAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] z = pattern( n, -2.1 );
            double[] expected = z.clone();
            for( int i = 0; i < n; i++ )
                expected[i] -= x[i];
            VectorUtils.substract( x, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "substract(n=" + n + ", i=" + i + ")", expected[i], z[i], 0.0 );
        }
    }

    public void testScaleDiffAllLengths()
    {
        for( int n : LENGTHS )
        {
            double[] x = pattern( n, 1.5 );
            double[] y = pattern( n, -0.7 );
            double[] z = new double[n];
            VectorUtils.scaleDiff( 2.0, x, y, z );
            for( int i = 0; i < n; i++ )
                assertEquals( "scaleDiff(n=" + n + ", i=" + i + ")", 2.0 * ( x[i] - y[i] ), z[i], 0.0 );
        }
    }

    private static double[] pattern(int n, double base)
    {
        double[] v = new double[n];
        for( int i = 0; i < n; i++ )
            v[i] = base * ( i + 1 ) + ( i % 2 == 0 ? 0.125 : -0.375 );
        return v;
    }
}
