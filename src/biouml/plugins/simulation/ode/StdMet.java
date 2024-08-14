
package biouml.plugins.simulation.ode;

import java.util.Arrays;

/**
 * contains a large amount of methods that perform routine array and matrix
 * calculations which are called in many routines of this package
 */
public class StdMet
{
    // constructors

    // methods

    // unary operating array/matrix methods, miscellaneous methods

    /*
       method prints to screen the array passed to it (each element on a separate
       line)
     */
    public static void printArray(double[] a)
    {
        for( int i = 0; i < a.length; i++ )
            System.out.println(a[i] + "   ");
    }

    public static String toString(double[] a)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for( int i = 0; i < a.length; i++ )
        {
            stringBuilder.append(a[i]);
            stringBuilder.append("   ");
        }
        return stringBuilder.toString();
    }

    /*
       method sets array passed to it an array of all zeros
     */
    public static void zeroArray(double[] a)
    {
        Arrays.fill(a, 0.);
    }

    /*
       method copies the contents of one array to the other (the two array
       arguments must have the same dimensions, program exits if this is
       not the case)
     */
    public static void copyArray(double[] a, double[] b) throws Exception
    {
        if( a.length != b.length )
            throw new Exception("array dimensions don't agree");

        System.arraycopy(b, 0, a, 0, b.length);
    }

    public static double[] copyArray(double[] b) throws Exception
    {
        double[] a = new double[b.length];
        System.arraycopy(b, 0, a, 0, b.length);
        return a;
    }

    /*
       method copies the contents of one matrix to the other (the two matix
       arguments must have the same dimensions)
     */
    public static void copyMatrix(double[][] a, double[][] b) throws Exception
    {
        final int an = a[0].length;
        final int bn = b[0].length;
        if( ( an != bn ) || ( a.length != b.length ) )
            throw new Exception("matrix dimensions don't agree");

        for( int i = 0; i < a.length; i++ ) // copy all elements from matrix b to
        {
            for( int j = 0; j < an; j++ )
                // matrix a
                a[i][j] = b[i][j];
        }
    }

    public static double[][] copyMatrix(double[][] b) throws Exception
    {
        final int n = b[0].length;
        double[][] a = new double[b.length][n];
        for( int i = 0; i < a.length; i++ )
        {
            for( int j = 0; j < n; j++ )
                a[i][j] = b[i][j];
        }
        return a;
    }

    /*
       method takes the piecewise product of one array and the scalar value, and copies
       it into the other array (both arrays must have the same dimensions)
     */
    public static void stam(double[] result, double scalar, double[] array) throws Exception
    {
        if( result.length != array.length )
            throw new Exception("array dimensions don't agree");

        for( int i = 0; i < array.length; i++ )
            result[i] = scalar * array[i];
    }

    // binary operating array/matrix methods

    /*
       method gets the piecewise sum of two of the arrays and copies this sum into
       the third array (all three array arguments must have the same
       dimensions)
     */
    public static void arraySum(double[] c, double[] a, double[] b) throws Exception
    {
        if( ( a.length != b.length ) || ( a.length != c.length ) )
            throw new Exception("array dimensions don't agree");

        for( int i = 0; i < a.length; i++ )
            c[i] = a[i] + b[i];
    }

    /*
       method gets the difference of two of the arrays and copies this
       difference into the third array (all three array arguments must
       have the same dimensions)
     */
    public static void arrayDiff(double[] c, double[] a, double[] b) throws Exception
    {
        if( ( a.length != b.length ) || ( a.length != c.length ) )
            throw new Exception("array dimensions don't agree");

        for( int i = 0; i < a.length; i++ )
            c[i] = a[i] - b[i];
    }

    /*
       method gets the dot product of the 2 arrays (an array resulting in the
       multiplication of each element in array a by each element in array
       b) and copies this to the result array (all three array arguments must
       have the same dimensions)
     */
    public static void dotProduct(double[] result, double[] a, double[] b) throws Exception
    {
        if( ( result.length != a.length ) || ( result.length != b.length ) )
            throw new Exception("array dimensions don't agree");

        for( int i = 0; i < a.length; i++ )
            result[i] = a[i] * b[i];
    }

    /*
       method gets the piecewise quotient of 2 arrays (an array resulting in
       the division of each element in array a by each element in array b)
       and copies this to the result vector (all three array arguments must
       have the same dimensions)
     */
    public static void dotQuo(double[] result, double[] a, double[] b) throws Exception
    {
        if( ( result.length != a.length ) || ( result.length != b.length ) )
            throw new Exception("array dimensions don't agree");

        for( int i = 0; i < a.length; i++ )
            // get quotient of ith elements of
            result[i] = a[i] / b[i];
    }

    /*
       method takes the product of a matrix and an array and returns an array
       (matrix width must be the same as array lengths)
     */
    public static void matrixVectorProduct(double[] result, double[][] matrix, double[] array) throws Exception
    {
        if( ( matrix[0].length != array.length ) || ( result.length != array.length ) )
            throw new Exception("array dimensions don't agree");

        for( int i = 0; i < array.length; i++ )
        {
            double sigma = 0; // the sum of each row multiplication
            double[] matrix_ith_row = matrix[i];
            for( int j = 0; j < array.length; j++ )
                // the sum of above row
                sigma += matrix_ith_row[j] * array[j];

            result[i] = sigma; // put the sum in ith position of result array
        }
    }

    // norms

    /**
     * method takes the infinity norm of the array and returns it
     */
    public static double normInf(double[] vector)
    {
        double max = -Double.MAX_VALUE;
        double abs;
        for( int i = 0; i < vector.length; i++ )
        {
            abs = Math.abs(vector[i]);
            if( abs > max )
                max = abs;
        }
        return max;
    }

    /**
     * method takes the rms-norm (root mean square) of the vector and returns it
     */
    public static double normRMS(double[] vector)
    {
        double v, sum = 0.0;
        for( int i = 0; i < vector.length; i++ )
        {
            v = vector[i];
            sum += v * v;
        }
        sum /= vector.length;
        return Math.sqrt(sum);
    }

    /**
     * Calculates Euclidean norm of the vector
     */
    public static double normEuclidean(double[] vector)
    {
        double v, sum = 0.0;
        for( int i = 0; i < vector.length; i++ )
        {
            v = vector[i];
            sum += v * v;
        }
        return Math.sqrt(sum);
    }


    // tolerance methods

    /*
       method calculates epsilon with arrays xn, xnPlusOne, atol and rtol and returns
       epsilon in the result array (all arrays must have the same dimensions)
     */
    public static void epsilon(double[] result, double[] xn, double[] xnPlusOne, double atol, double rtol) throws Exception
    {
        if( ( result.length != xn.length ) || ( result.length != xnPlusOne.length ) )
            throw new Exception("array dimensions don't agree");

        for( int i = 0; i < result.length; i++ )
            result[i] = atol + Math.max(Math.abs(xn[i]), Math.abs(xnPlusOne[i])) * rtol;
    }

    /*
       method calculates tau with arrays xn, xnPlusOne, absolute tolerance
       and relative tolerance and returns tau in the result array
      (all arrays must have the same dimensions)
     */
    public static void tau(double[] result, double[] xn, double[] xnPlusOne, double[] atol, double[] rtol) throws Exception
    {
        if( ( result.length != xn.length ) || ( result.length != xnPlusOne.length ) || ( result.length != atol.length )
                || ( result.length != rtol.length ) )
        {
            throw new Exception("array dimensions don't agree");
        }

        for( int i = 0; i < result.length; i++ )
            result[i] = atol[i] + Math.max(Math.abs(xn[i]), Math.abs(xnPlusOne[i])) * rtol[i];
    }

    /**
     * Method generates double array with equal elements value and given length
     */
    public static double[] generateArray(double value, int length) throws IllegalArgumentException
    {
        if( length < 0 )
            throw new IllegalArgumentException("Negative array length " + length);
        double[] result = new double[length];
        for( int i = 0; i < length; i++ )
            result[i] = value;
        return result;
    }

}