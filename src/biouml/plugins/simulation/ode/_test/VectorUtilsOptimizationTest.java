package biouml.plugins.simulation.ode._test;

import junit.framework.TestCase;

import biouml.plugins.simulation.ode.jvode.VectorUtils;

/**
 * Regression tests for the VectorUtils hot-path loops (linearSum, add and the
 * elementwise transforms). These are exact, tolerance-0.0 checks of the math:
 * every element must match the scalar expression bit-for-bit. The lengths
 * 0, 1, 2, 3, 4, 5, 7, 8, 9, 12, 13 cover the unroll-boundary alignments the
 * loops were written for, so any future change to a loop body is checked across
 * every remainder alignment.
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

    private static double[] pattern(int n, double base)
    {
        double[] v = new double[n];
        for( int i = 0; i < n; i++ )
            v[i] = base * ( i + 1 ) + ( i % 2 == 0 ? 0.125 : -0.375 );
        return v;
    }
}
