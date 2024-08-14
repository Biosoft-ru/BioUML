package biouml.plugins.simulation.ode.jvode;

import java.util.Arrays;

import one.util.streamex.DoubleStreamEx;

public class VectorUtils
{

    private VectorUtils()
    {

    }

    /**
     *   z = a*x + b*y
     */
    public static void linearSum(double a, double[] x, double b, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
            z[i] = ( a * x[i] ) + ( b * y[i] );
    }

    /**
     *   z[i] = x[i]*y[i] for i = 0, 1, ..., n-1
     */
    public static void prod(double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
            z[i] = x[i] * y[i];
    }

    /**
     *  z[i] = x[i]/y[i] for i = 0, 1, ..., n-1
     */
    public static void divide(double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
            z[i] = x[i] / y[i];
    }

    /**
     *  z = c*x
     */
    public static void scale(double c, double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
            z[i] = c * x[i];
    }

    /**
     *   z[i] = |x[i]| for i = 0, 1, ..., n-1
     */
    public static void abs(double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
            z[i] = Math.abs(x[i]);
    }

    /**
     *   z[i] = 1/x[i] for i = 0, 1, ..., n-1
     *   This routine does not check for division by 0. It should be
     *   called only with ann_Vector_Serial x which is guaranteed to have
     *   all non-zero components.
     */
    public static void inv(double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = 1.0 / x[i];
        }
    }

    /**
     *   z[i] = x[i] + b   for i = 0, 1, ..., n-1
     */
    public static void addConst(double[] x, double b, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = x[i] + b;
        }
    }
    /**
     *   x[i] +=  b   for i = 0, 1, ..., n-1
     */
    public static void addConst(double b, double[] x)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            x[i] += b;
        }
    }

    /**
     *   Returns the dot product of two vectors:
     *         sum (i = 0 to n-1) {x[i]*y[i]}
     */
    public static double dotProd(double[] x, double[] y)
    {
        int n = x.length;
        double sum = 0;
        for( int i = 0; i < n; i++ )
        {
            sum += x[i] * y[i];
        }
        return sum;
    }

    /**
     *   Returns the maximum norm of x:
     *         max (i = 0 to n-1) ABS(x[i])
     */
    public static double maxNorm(double[] x)
    {
        double max = 0;
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            max = Math.max(max, Math.abs(x[i]));
        }

        return max;
    }

    /**
     *   Returns the weighted root mean square norm of x with weight
     *   vector w:
     *         sqrt [(sum (i = 0 to n-1) {(x[i]*w[i])^2})/n]
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
        return Math.sqrt(sum / n);
    }

    /**
     *   Returns the weighted root mean square norm of x with weight
     *   vector w, masked by the elements of id:
     *         sqrt [(sum (i = 0 to n-1) {(x[i]*w[i]*msk[i])^2})/n]
     *   where msk[i] = 1.0 if id[i] > 0 and
     *         msk[i] = 0.0 if id[i] < 0
     */
    public static double wrmsNormMask(double[] x, double[] w, double[] id)
    {
        double prodi;
        double sum = 0;
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            if( id[i] > 0 )
            {
                prodi = x[i] * w[i];
                sum += prodi * prodi;
            }
        }
        return Math.sqrt(sum / n);
    }

    /**
     *   Returns the smallest element of x:
     *         min (i = 0 to n-1) x[i]
     */
    public static double getMin(double[] x)
    {
        int n = x.length;
        double min = x[0];
        for( int i = 1; i < n; i++ )
        {
            if( x[i] < min )
                min = x[i];
        }
        return min;
    }

    /**
     *   Returns the weighted Euclidean L2 norm of x with weight
     *   vector w:
     *         sqrt [(sum (i = 0 to n-1) {(x[i]*w[i])^2})]
     */
    public static double l2Norm(double[] x, double[] w)
    {
        double prodi;
        double sum = 0;
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            prodi = x[i] * w[i];
            sum += prodi * prodi;
        }
        return Math.sqrt(sum);
    }

    /**
     *   Returns the L1 norm of x:
     *         sum (i = 0 to n-1) {ABS(x[i])}
     */
    public static double l1Norm(double[] x)
    {
        double sum = 0;
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            sum += Math.abs(x[i]);
        }
        return sum;
    }

    /**
     *   Performs the operation
     *          z[i] = 1.0 if ABS(x[i]) >= c   i = 0, 1, ..., n-1
     *                 0.0 otherwise
     */
    public static void compare(double c, double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = ( Math.abs(x[i]) >= c ) ? 1 : 0;
        }
    }

    /**
     *   Performs the operation z[i] = 1/x[i] with a test for
     *   x[i] == 0.0 before inverting x[i].
     *   This routine returns TRUE if all components of x are non-zero
     *   (successful inversion) and returns FALSE otherwise.
     */
    public static boolean invTest(double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            if( x[i] == 0 )
                return false;
            z[i] = 1.0 / x[i];
        }
        return true;
    }

    /**
     *   Performs the operation :
     *       m[i] = 1.0 if constraint test fails for x[i]
     *       m[i] = 0.0 if constraint test passes for x[i]
     *   where the constraint tests are as follows:
     *      If c[i] = +2.0, then x[i] must be >  0.0.
     *      If c[i] = +1.0, then x[i] must be >= 0.0.
     *      If c[i] = -1.0, then x[i] must be <= 0.0.
     *      If c[i] = -2.0, then x[i] must be <  0.0.
     *   This routine returns a boolean FALSE if any element failed
     *   the constraint test, TRUE if all passed. It also sets a
     *   mask vector m, with elements equal to 1.0 where the
     *   corresponding constraint test failed, and equal to 0.0
     *   where the constraint test passed.
     *   This routine is specialized in that it is used only for
     *   constraint checking.
     */
    public static boolean constrMask(double[] c, double[] x, double[] m)
    {
        boolean test;
        int n = x.length;

        test = true;
        for( int i = 0; i < n; i++ )
        {
            m[i] = 0;
            if( c[i] == 0 )
                continue;
            if( c[i] > 1.5 || c[i] < -1.5 )
            {
                if( x[i] * c[i] <= 0 )
                {
                    test = false;
                    m[i] = 1;
                }
                continue;
            }
            if( c[i] > 0.5 || c[i] < -0.5 )
            {
                if( x[i] * c[i] < 0 )
                {
                    test = false;
                    m[i] = 1;
                }
            }
        }
        return test;
    }

    /**
     *   Performs the operation :
     *       minq  = min ( num[i]/denom[i]) over all i such that
     *       denom[i] != 0.
     *   This routine returns the minimum of the quotients obtained
     *   by term-wise dividing num[i] by denom[i]. A zero element
     *   in denom will be skipped. If no such quotients are found,
     *   then the large value BIG_REAL is returned.
     */
    public static double minQuotient(double[] num, double[] denom)
    {
        int n = num.length;
        boolean notEvenOnce = true;
        double min = Double.POSITIVE_INFINITY;
        for( int i = 0; i < n; i++ )
        {
            if( denom[i] == 0 )
                continue;

            if( !notEvenOnce )
                min = Math.min(min, num[i] / denom[i]);
            else
            {
                min = num[i] / denom[i];
                notEvenOnce = false;
            }

        }
        return min;
    }

    /*
     * -----------------------------------------------------------------
     * utility functions
     * -----------------------------------------------------------------
     */

    public static double[] newVector(double val, int n)
    {
        double[] result = new double[n];
        Arrays.fill(result, val);
        return result;
    }

    public static void copy(double[] x, double[] z)
    {
        System.arraycopy(x, 0, z, 0, x.length);
    }

    public static double[] copy(double[] x)
    {
        double[] result = new double[x.length];
        System.arraycopy(x, 0, result, 0, x.length);
        return result;
    }

    /**
     * z[i] = x[i] + y[i];
     */
    public static void linearSum(double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = x[i] + y[i];
        }
    }

    /**
     * z[i] = x[i] - y[i];
     */
    public static void linearDiff(double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = x[i] - y[i];
        }
    }

    /**
     * z[i] = -x[i]
     */
    public static void neg(double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = -x[i];
        }
    }

    /**
     * z[i] = a*(x[i] + y[i])
     */
    public static void scaleSum(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = a * ( x[i] + y[i] );
        }
        return;
    }

    /**
     * z[i] = a*(x[i] - y[i])
     */
    public static void scaleDiff(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = a * ( x[i] - y[i] );
        }
        return;
    }

    /**
     * z[i] = a*x[i] + y[i]
     */
    public static void linearSum(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = a * x[i] + y[i];
        }
    }

    /**
     * z[i] = a*x[i] + y[i]
     */
    public static void linearSum(double[] x, double b, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = x[i] + b * y[i];
        }
    }

    /**
     * z[i] += a*x[i]
     */
    public static void linearSum(double a, double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] += a * x[i];
        }
    }


    /**
     * z[i] = a*x[i] - y[i]
     */
    public static void linearDiff(double a, double[] x, double[] y, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] = a * x[i] - y[i];
        }
    }


    /**
     * x[i] *= a
     */
    public static void scale(double a, double[] x)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            x[i] *= a;
        }

    }

    /**
     * z[i] += x[i]
     */
    public static void add(double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] += x[i];
        }
    }


    /**
     * z[i] -= x[i]
     */
    public static void substract(double[] x, double[] z)
    {
        int n = x.length;
        for( int i = 0; i < n; i++ )
        {
            z[i] -= x[i];
        }
    }

    /**
     * y[i] += a*x[i]
     */
    public static void addScaled(double a, double[] x, double[] y)
    {
        int n = x.length;
        if( a == 1 )
        {
            for( int i = 0; i < n; i++ )
            {
                y[i] += x[i];
            }
            return;
        }
        if( a == -1 )
        {
            for( int i = 0; i < n; i++ )

            {
                y[i] -= x[i];
            }
            return;
        }
        for( int i = 0; i < n; i++ )
        {
            y[i] += a * x[i];
        }
        return;
    }


    public static String toString(double[] y)
    {
        return DoubleStreamEx.of( y ).mapToObj( val -> val + "\n" ).joining();
    }

}
