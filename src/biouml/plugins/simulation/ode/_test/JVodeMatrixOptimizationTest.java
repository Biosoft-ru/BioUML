package biouml.plugins.simulation.ode._test;

import junit.framework.TestCase;

import biouml.plugins.simulation.ode.jvode.Matrix;
import biouml.plugins.simulation.ode.jvode.MatrixUtils;

/**
 * Regression tests for the dense LU solve (MatrixUtils.denseGETRS) and the
 * dense matrix copy (Matrix.denseCopy), covering the boundary and zero cases
 * exercised by the zero-skip and arraycopy optimizations.
 *
 * NOTE on layout: the JVode port stores a square LU factorization in
 * column-major order — math element (i, j) lives in a[j][i]. denseGETRF
 * writes the multipliers/sub-diagonal into a[i][k] (the i-th entry of the
 * k-th array) and denseGETRS reads them back the same way. The reference
 * solver below is written against the math (row-major) matrix and is
 * layout-independent.
 */
public class JVodeMatrixOptimizationTest extends TestCase
{
    public JVodeMatrixOptimizationTest(String name)
    {
        super(name);
    }

    // ---- denseGETRS ------------------------------------------------------

    private void assertSolvesAxEqualsB(double[][] A, double[] bOrig, int n)
    {
        // LU-decompose a copy of A (stored column-major by denseGETRF) and solve
        // the system with the production path.
        double[][] lu = new double[n][];
        for( int j = 0; j < n; j++ )
        {
            lu[j] = new double[n];
            for( int i = 0; i < n; i++ )
                lu[j][i] = A[i][j]; // column-major copy
        }
        int[] p = new int[n];
        int info = MatrixUtils.denseGETRF( lu, n, n, p );
        assertEquals( "denseGETRF failed", 0, info );
        double[] b = bOrig.clone();
        MatrixUtils.denseGETRS( lu, n, p, b );

        // Reference: Gaussian elimination with partial pivoting on a fresh
        // row-major copy of the SAME math matrix.
        double[] ref = solveRowMajor( A, bOrig, n );

        // The production solution must satisfy A x = b (residual check).
        // This is the strongest guarantee and is layout-independent.
        for( int i = 0; i < n; i++ )
        {
            double s = 0;
            for( int j = 0; j < n; j++ )
                s += A[i][j] * b[j];
            assertEquals( "residual row " + i + " (A x != b)", bOrig[i], s, 1e-9 );
        }
        // And it must match the independent reference solver.
        for( int i = 0; i < n; i++ )
            assertEquals( "component " + i, ref[i], b[i], 1e-9 );
    }

    public void testDenseGETRSDenseRHS()
    {
        double[][] A = {
            { 4, -1,  2, 0 },
            { 1,  5, -1,  1 },
            { 2, -1,  4, -1 },
            { 0,  1, -1,  3 }
        };
        double[] b = { 2.5, -1.0, 7.25, 4.0 };
        assertSolvesAxEqualsB( A, b, 4 );
    }

    public void testDenseGETRSRHSWithZeros()
    {
        double[][] A = {
            { 4, -1,  2, 0 },
            { 1,  5, -1,  1 },
            { 2, -1,  4, -1 },
            { 0,  1, -1,  3 }
        };
        double[] b = { 2.5, 0.0, 7.25, 0.0 };
        assertSolvesAxEqualsB( A, b, 4 );
    }

    public void testDenseGETRSAllZeroRHS()
    {
        double[][] A = {
            { 4, -1,  2, 0 },
            { 1,  5, -1,  1 },
            { 2, -1,  4, -1 },
            { 0,  1, -1,  3 }
        };
        double[] b = { 0, 0, 0, 0 };
        assertSolvesAxEqualsB( A, b, 4 );
    }

    public void testDenseGETRSElementsAreNegZero()
    {
        // b elements are -0.0; the zero-skip must treat them exactly like 0.0
        double[][] A = {
            { 4, -1,  2, 0 },
            { 1,  5, -1,  1 },
            { 2, -1,  4, -1 },
            { 0,  1, -1,  3 }
        };
        double[] b = { -0.0, -0.0, -0.0, -0.0 };
        assertSolvesAxEqualsB( A, b, 4 );
    }

    public void testDenseGETRSUnitMatrix()
    {
        double[][] A = { { 1, 0 }, { 0, 1 } };
        double[] b = { 3.0, -2.0 };
        assertSolvesAxEqualsB( A, b, 2 );
    }

    public void testDenseGETRSTriangularNeedsPivot()
    {
        // A[0][0] == 0 so denseGETRF must pivot; exercises the p-permutation path
        double[][] A = {
            { 0, 1 },
            { 1, 2 }
        };
        double[] b = { 1.0, 3.0 };
        assertSolvesAxEqualsB( A, b, 2 );
    }

    // ---- denseCopy -------------------------------------------------------

    public void testDenseCopyNormal()
    {
        double[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        double[][] b = { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
        Matrix.denseCopy( a, b, 3, 3 );
        assertMatrixEquals( a, b );
    }

    public void testDenseCopyRectangular()
    {
        double[][] a = { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } };
        double[][] b = { { 0,0,0,0 }, { 0,0,0,0 } };
        Matrix.denseCopy( a, b, 4, 2 );
        assertMatrixEquals( a, b );
    }

    public void testDenseCopyZeroRows()
    {
        double[][] a = { { 1, 2 }, { 3, 4 } };
        double[][] b = { { 9, 9 }, { 9, 9 } };
        Matrix.denseCopy( a, b, 0, 2 );
        assertEquals( 9, b[0][0], 0.0 );
        assertEquals( 9, b[1][1], 0.0 );
    }

    public void testDenseCopyZeroCols()
    {
        double[][] a = { { 1, 2 }, { 3, 4 } };
        double[][] b = { { 0, 0 }, { 0, 0 } };
        Matrix.denseCopy( a, b, 2, 0 );
        assertEquals( 0, b[0][0], 0.0 );
    }

    // ---- helpers ---------------------------------------------------------

    /**
     * Gaussian elimination with partial pivoting on a row-major matrix A.
     * Returns x such that A x = b. Independent of the JVode port's
     * column-major storage, so it serves as a true reference.
     */
    private static double[] solveRowMajor(double[][] A, double[] bOrig, int n)
    {
        double[][] m = new double[n][];
        double[] v = new double[n];
        for( int i = 0; i < n; i++ )
        {
            m[i] = new double[n];
            System.arraycopy( A[i], 0, m[i], 0, n );
            v[i] = bOrig[i];
        }
        for( int col = 0; col < n; col++ )
        {
            int piv = col;
            for( int i = col + 1; i < n; i++ )
                if( Math.abs( m[i][col] ) > Math.abs( m[piv][col] ) )
                    piv = i;
            if( piv != col )
            {
                double[] tmp = m[piv];
                m[piv] = m[col];
                m[col] = tmp;
                double tv = v[piv];
                v[piv] = v[col];
                v[col] = tv;
            }
            for( int i = 0; i < n; i++ )
            {
                if( i == col )
                    continue;
                double f = m[i][col] / m[col][col];
                for( int j = 0; j < n; j++ )
                    m[i][j] -= f * m[col][j];
                v[i] -= f * v[col];
            }
        }
        double[] x = new double[n];
        for( int i = 0; i < n; i++ )
            x[i] = v[i] / m[i][i];
        return x;
    }

    private static void assertMatrixEquals(double[][] a, double[][] b)
    {
        for( int i = 0; i < a.length; i++ )
            for( int j = 0; j < a[i].length; j++ )
                assertEquals( "cell " + i + "," + j, a[i][j], b[i][j], 0.0 );
    }
}
