package ru.biosoft.analysis;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import Jama.Matrix;

/**
 * Different additional functions
 * 
 * @author axec
 * 
 */
public class Util
{
    private Util()
    {
    }

    /**
     * Returns identity n-by-n matrix
     */
    public static double[][] identityMatrix(int n)
    {
        double[][] identity = new double[n][n];
        for( int i = 0; i < n; ++i )
        {
            identity[i][i] = 1;
        }
        return identity;
    }
    
    public static double getSquaredSumError(double[] a, double[] b)
    {
        return DoubleStreamEx.zip( a, b, (x, y) -> x - y ).map( val -> val * val ).sum();
    }

    /**
     * fast version of pow
     * @param base
     * @param power
     * @return
     */
    public static double pow(double base, int power)
    {
        if( power == 0 )
            return 1;
        double result = base;
        for( int i = 1; i < Math.abs(power); i++ )
            result *= base;
        if( power < 0 )
            return 1d / result;
        return result;
    }

    /**
     * pow for array
     * @param array
     * @param logarithmBase
     */
    public static double[] pow(double[] array, int power)
    {
        double[] result = new double[array.length];
        Arrays.setAll( result, i -> pow(array[i], power) );
        return result;
    }

    /**
     * pow for matrix
     * @param matrix
     * @param base
     */
    public static void pow(double base, double[][] power)
    {
        for( double[] arr : power )
        {
            pow(base, arr);
        }
    }

    /**
     * pow for array
     * @param array
     * @param logarithmBase
     * @return true if overflow occurred at some point
     */
    public static boolean pow(double base, double[] power)
    {
        boolean hasNaN = false;
        for( int i = 0; i < power.length; i++ )
        {
            if( Double.isNaN(power[i]) || Double.isInfinite(power[i]) )
                continue;
            power[i] = Math.pow(base, power[i]);
            hasNaN |= Double.isNaN(power[i]) || Double.isInfinite(power[i]);
        }
        return hasNaN;
    }

    /**
     * pow for array
     * @param array
     * @param logarithmBase
     */
    public static double[] pow(double[] array, double power)
    {
        double[] result = new double[array.length];
        Arrays.setAll( result, i -> Math.pow(array[i], power) );
        return result;
    }

    /**
     * Returns string representation of elapsed time: 1 hours 20 minutes 36 seconds from time in milliseconds
     * @param time
     * @return
     */
    public static String getElapsedTime(double t)
    {
        StringBuffer time = new StringBuffer();
        int elapsedSeconds = (int)t / 1000;//(int)jobControl.getElapsedTime() / 1000;

        int elapsedMSeconds = (int) ( t - elapsedSeconds * 1000 );

        if( elapsedMSeconds == 0 )
        {
            time.append("0 milliseconds ");
        }
        else
        {
            int elapsedMinutes = elapsedSeconds / 60;
            int elapsedHours = elapsedMinutes / 60;
            elapsedSeconds -= elapsedMinutes * 60;
            elapsedMinutes -= elapsedHours * 60;

            if( elapsedHours != 0 )
            {
                time.append(elapsedHours);
                time.append(" hours ");
            }
            if( elapsedMinutes != 0 )
            {
                time.append(elapsedMinutes);
                time.append(" minutes ");
            }
            if( elapsedSeconds != 0 )
            {
                time.append(elapsedSeconds);
                time.append(" seconds ");
            }
            if( elapsedMSeconds != 0 )
            {
                time.append(elapsedMSeconds);
                time.append(" milliseconds ");
            }
        }
        return time.toString();
    }

    /**
     * Transforms two input arrays into arrays without NaNs.
     * @param sample1
     * @param sample2
     * @return
     */
    public static double[][] avoidNaNs(double[] sample1, double[] sample2)
    {
        if( sample1.length != sample2.length )
            throw new IllegalArgumentException("Samples lengths must agree");
        TDoubleList vector1 = new TDoubleArrayList();
        TDoubleList vector2 = new TDoubleArrayList();
        for( int i = 0; i < sample1.length; i++ )
        {
            double val1 = sample1[i];
            double val2 = sample2[i];
            if( Double.isNaN(val1) || Double.isNaN(val2) )
                continue;
            vector1.add(val1);
            vector2.add(val2);
        }
        return new double[][] {vector1.toArray(), vector2.toArray()};
    }

    public static double[] avoidNaNs(double[] sample)
    {
    	return DoubleStreamEx.of(sample).remove( Double::isNaN ).toArray();
    }

    /**
     * Logarithm of any base
     */
    public static double log(double value, double base)
    {
        return Math.log(value) / Math.log(base);
    }

    public static double[] getColumnFromMatrix(double[][] matrix, int columnToGet)
    {
        if( ! ( matrix.length > 0 && matrix[0].length >= columnToGet ) )
            return null;
        return Stream.of(matrix).mapToDouble( row -> row[columnToGet] ).toArray();
    }

    public static double[][] getColumnsMatrix(double[][] matrix, int[] indices) //indices - indices of colums from matrix to form new matrix
    {
        return Stream.of( matrix ).map( row -> IntStreamEx.of( indices ).elements( row ).toArray() ).toArray( double[][]::new );
    }

    public static double[][] getLeftNullSpace(double[][] matrix) throws Exception // rows of result matrix form basis of left null space of given matrix
    {
        double[][] symmetric = matrixMultiply(matrix, matrixConjugate(matrix));
        double precision = 1e-15;
        double[][] eigenvectors = new double[symmetric.length][symmetric.length];
        if( isSimmetric(symmetric, precision) )
        {
            jacobi(symmetric, symmetric.length, eigenvectors, precision);
        }
        int zeroEigenValueCount = 0;
        for( int i = 0; i < symmetric.length; i++ )
            if( symmetric[i][i] < precision )
                zeroEigenValueCount++;
        int[] eigenVectorIndeces = new int[zeroEigenValueCount]; // indeces of columns corresponding to zero eigenvalues
        int eigenVectorIndex = 0;
        for( int i = 0; i < symmetric.length; i++ )
            if( symmetric[i][i] < precision )
            {
                eigenVectorIndeces[eigenVectorIndex++] = i;
            }
        double[][] leftNullSpace = getColumnsMatrix(eigenvectors, eigenVectorIndeces);
        return leftNullSpace;

    }

    public static boolean isSimmetric(double[][] matrix, double precision)
    {
        if( matrix.length != matrix[0].length )
            return false;
        for( int i = 0; i < matrix.length; i++ )
            for( int j = i + 1; j < matrix.length; j++ )
                if( Math.abs(matrix[i][j] - matrix[j][i]) > precision )
                    return false;
        return true;
    }

    public static void jacobi(double[][] coefficients, int numberOfEquation, double[][] solution, double precision)
    {
        int i, j, k;
        int maxI = 0, maxJ = 0;
        double max, fi;

        for( i = 0; i < numberOfEquation; i++ )
        {
            for( j = 0; j < numberOfEquation; j++ )
            {
                solution[i][j] = 0;
            }
            solution[i][i] = 1;
        }

        double[][] rotationMatrix = new double[numberOfEquation][numberOfEquation];

        double[][] temp = new double[numberOfEquation][numberOfEquation];

        double fault = 0.0;
        for( i = 0; i < numberOfEquation; i++ )
        {
            for( j = i + 1; j < numberOfEquation; j++ )
            {
                fault = fault + coefficients[i][j] * coefficients[i][j];
            }
        }
        fault = Math.sqrt(2 * fault);

        while( fault > precision )
        {
            max = 0.0;
            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = i + 1; j < numberOfEquation; j++ )
                {
                    if( coefficients[i][j] > 0 && coefficients[i][j] > max )
                    {
                        max = coefficients[i][j];
                        maxI = i;
                        maxJ = j;
                    }
                    else if( coefficients[i][j] < 0 && -coefficients[i][j] > max )
                    {
                        max = -coefficients[i][j];
                        maxI = i;
                        maxJ = j;
                    }
                }
            }
            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = 0; j < numberOfEquation; j++ )
                {
                    rotationMatrix[i][j] = 0;
                }
                rotationMatrix[i][i] = 1;
            }

            if( coefficients[maxI][maxI] == coefficients[maxJ][maxJ] )
            {
                rotationMatrix[maxI][maxI] = rotationMatrix[maxJ][maxJ] = rotationMatrix[maxJ][maxI] = Math.sqrt(2.0) / 2.0;
                rotationMatrix[maxI][maxJ] = -Math.sqrt(2.0) / 2.0;
            }
            else
            {
                fi = 0.5 * Math.atan( ( 2.0 * coefficients[maxI][maxJ] ) / ( coefficients[maxI][maxI] - coefficients[maxJ][maxJ] ));
                rotationMatrix[maxI][maxI] = rotationMatrix[maxJ][maxJ] = Math.cos(fi);
                rotationMatrix[maxI][maxJ] = -Math.sin(fi);
                rotationMatrix[maxJ][maxI] = Math.sin(fi);
            }

            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = 0; j < numberOfEquation; j++ )
                {
                    temp[i][j] = 0.0;
                }
            }
            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = 0; j < numberOfEquation; j++ )
                {
                    for( k = 0; k < numberOfEquation; k++ )
                    {
                        temp[i][j] = temp[i][j] + rotationMatrix[k][i] * coefficients[k][j];
                    }
                }
            }
            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = 0; j < numberOfEquation; j++ )
                {
                    coefficients[i][j] = 0.0;
                }
            }
            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = 0; j < numberOfEquation; j++ )
                {
                    for( k = 0; k < numberOfEquation; k++ )
                    {
                        coefficients[i][j] = coefficients[i][j] + temp[i][k] * rotationMatrix[k][j];
                    }
                }
            }
            fault = 0.0;
            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = i + 1; j < numberOfEquation; j++ )
                {
                    fault = fault + coefficients[i][j] * coefficients[i][j];
                }
            }
            fault = Math.sqrt(2 * fault);

            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = 0; j < numberOfEquation; j++ )
                {
                    temp[i][j] = 0.0;
                }
            }

            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = 0; j < numberOfEquation; j++ )
                {
                    for( k = 0; k < numberOfEquation; k++ )
                    {
                        temp[i][j] = temp[i][j] + solution[i][k] * rotationMatrix[k][j];
                    }
                }
            }

            for( i = 0; i < numberOfEquation; i++ )
            {
                for( j = 0; j < numberOfEquation; j++ )
                {
                    solution[i][j] = temp[i][j];
                }
            }
        }
    }

    /**
     * Matrix elements logarithm
     */
    public static void logMatrix(double[][] matrix)
    {
        for( int i = 0; i < matrix.length; i++ )
        {
            for( int j = 0; j < matrix[i].length; j++ )
            {
                if( matrix[i][j] <= 0 )
                {
                    matrix[i][j] = -9999;
                }
                else
                {
                    try
                    {
                        matrix[i][j] = Math.log10(matrix[i][j]);
                    }
                    catch( Exception e )
                    {
                        matrix[i][j] = -9999;
                    }
                }
            }
        }

    }
    /**
     * Reversing of symmetric, positively definite matrix
     */
    public static double[][] symMatrixReverse(double[][] matrix) throws Exception
    {
        int n = matrix.length;
        double[][] result = new double[n][n];
        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < n; j++ )
            {
                if( i == j )
                    result[i][j] = 1;
                else
                    result[i][j] = 0;
            }
        }

        for( int i = 0; i < n; i++ )
        {
            result[i] = symMatrixResolve(matrix, result[i]);
        }
        result = matrixConjugate(result);
        return result;
    }



    /**
     * Multiplying two matrices: matrix1*matrix2
     */
    public static double[][] matrixMultiply(double[][] matrix1, double[][] matrix2) throws Exception
    {
        if( matrix1[0].length != matrix2.length )
            throw new Exception("matrices dimensions doesn't match");
        double[][] result = new double[matrix1.length][matrix2[0].length];
        double[][] conMatrix2 = matrixConjugate(matrix2);
        for( int i = 0; i < matrix1.length; i++ )
        {
            for( int j = 0; j < matrix2[0].length; j++ )
            {
                result[i][j] = scalarMultiply(matrix1[i], conMatrix2[j]);
            }
        }
        return result;
    }
    
    /**
     * Multiplying matrix by factor
     */
    public static double[][] multiply(double factor, double[][] matrix)
    {
        double[][] result = new double[matrix.length][matrix.length];
        for( int i = 0; i < matrix.length; i++ )
            for( int j = 0; j < matrix[0].length; j++ )
                result[i][j] = factor * matrix[i][j];
        return result;
    }
    
    /**
     * Multiplying scalar on vector
     */
    public static double[] scalarXvector(double scalar, double[] vector)
    {
        return DoubleStreamEx.of( vector ).map( v -> scalar * v ).toArray();
    }

    /**
     * Fill existing array with one value
     */
    public static double[] fill(double[] array, double val)
    {
        for( int i = 0; i < array.length; i++ )
            array[i] = val;
        return array;
    }
    
    public static double[] constArray(int size, double val)
    {
        return fill(new double[size], val);
    }
        
    public static double[] copy(double[] dest, double[] src)
    {
        System.arraycopy(src, 0, dest, 0, src.length);
        return dest;
    }
    
    /**
     * sum of two vectors
     */
    public static double[] vectorSum(double[] vector1, double[] vector2)
    {
        return DoubleStreamEx.zip( vector1, vector2, Double::sum ).toArray();
    }
    
    public static double[] vectorDiff(double[] vector1, double[] vector2)
    {
        return DoubleStreamEx.zip( vector1, vector2, (a,b)->a-b ).toArray();
    }

    public static double vectorSecondNorm(double[] vector)
    {
        return Math.sqrt( Arrays.stream( vector ).map( x -> x * x ).sum() );
    }

    public static double vectorL1NormForDifference(double[] x, double[] y)
    {
        return DoubleStreamEx.zip( x, y, (a, b) -> Math.abs( a - b ) ).sum();
    }

    public static double matrixL1NormForDifference(double[][] x, double[][] y)
    {
        double result = 0;
        for( int i = 0; i < x.length; i++ )
        {
            result += vectorL1NormForDifference(x[i], y[i]);
        }
        return result;
    }
    /**
     * Conjugate matrix
     */
    public static double[][] matrixConjugate(double[][] matrix)
    {
        double[][] conMatrix = new double[matrix[0].length][matrix.length];
        for( int i = 0; i < matrix.length; i++ )
        {
            for( int j = 0; j < matrix[0].length; j++ )
            {
                conMatrix[j][i] = matrix[i][j];
            }
        }
        return conMatrix;
    }
    
    public static int[][] transpose(int[][] matrix)
    {
        int[][] conMatrix = new int[matrix[0].length][matrix.length];
        for( int i = 0; i < matrix.length; i++ )
        {
            for( int j = 0; j < matrix[0].length; j++ )
            {
                conMatrix[j][i] = matrix[i][j];
            }
        }
        return conMatrix;
    }

    /**
     * Scalar multiplying two vectors
     */
    public static double scalarMultiply(double[] vector1, double[] vector2) throws IllegalArgumentException
    {
        if( vector1.length != vector2.length )
            throw new IllegalArgumentException("vectors are with different lengths");
        double result = 0;
        for( int i = 0; i < vector1.length; i++ )
            result += vector1[i] * vector2[i];
        return result;
    }

    /**
     * Multiplying array and matrix: matrix*array
     */
    public static double[] rightMultiply(double[][] matrix, double[] array) throws IllegalArgumentException
    {
        if( matrix[0].length != array.length )
            throw new IllegalArgumentException("matrix and array dimensions are different");

        double[] result = new double[matrix.length];
        Arrays.setAll( result, i -> scalarMultiply(matrix[i], array) );
        return result;
    }


    /**
     * Solving linear equation with symmetric matrix using method of square roots
     */
    public static double[] symMatrixResolve(double[][] matrix, double[] left) throws IllegalArgumentException
    {
        if( matrix.length != matrix[1].length )
            throw new IllegalArgumentException("Matrix is not square");
        if( matrix.length != left.length )
            throw new IllegalArgumentException("Left side's dimension differs from matrix dimension");
        int n = matrix.length;
        double[][] s = new double[n][n];
        double[] k = new double[n];
        double[] x = new double[n];

        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < i; j++ )
                if( matrix[i][j] != matrix[j][i] )
                    throw new IllegalArgumentException("Matrix is not symmetric");
        }

        //Computation of up-triangular matrix S for initial matrix A decomposition: A = S'S
        for( int i = 0; i < n; i++ )
        {
            s[i][i] = matrix[i][i];
            for( int l = 0; l < i; l++ )
                s[i][i] -= s[l][i] * s[l][i];//Math.pow(s[l][i], 2);

            if( s[i][i] <= 0 )
                throw new IllegalArgumentException("Probably bad condition or determination of matrix.");

            s[i][i] = Math.sqrt(s[i][i]);

            for( int j = i + 1; j < n; j++ )
            {
                s[i][j] = matrix[i][j];
                for( int l = 0; l < i; l++ )
                    s[i][j] -= s[l][i] * s[l][j];
                s[i][j] /= s[i][i];
            }
        }

        //Solving up-triangular system Sk = F
        for( int i = 0; i < n; i++ )
        {
            k[i] = left[i];
            for( int l = 0; l < i; l++ )
                k[i] -= s[l][i] * k[l];
            k[i] /= s[i][i];

        }
        //Solving up-triangular system S'x = k
        for( int i = n - 1; i >= 0; i-- )
        {
            x[i] = k[i];
            for( int l = i + 1; l < n; l++ )
                x[i] -= s[i][l] * x[l];
            x[i] /= s[i][i];
        }

        return x;
    }

    /**
     * Appending two matrices into one (horizontally)
     */
    public static double[][] appendMatr(double matrix1[][], double matrix2[][])
    {
        int n1 = matrix1[0].length;
        int n2 = matrix2[0].length;
        if( matrix1.length != matrix2.length )
            return null;
        double[][] matrix = new double[matrix1.length][n1 + n2];
        int j = 0;
        while( j < n1 )
        {
            for( int i = 0; i < matrix1.length; i++ )
            {
                matrix[i][j] = matrix1[i][j];
            }
            j++;
        }
        j = 0;
        while( j < n2 )
        {
            for( int i = 0; i < matrix2.length; i++ )
            {
                matrix[i][j + n1] = matrix2[i][j];
            }
            j++;
        }
        return matrix;
    }

    /**
     * @return Submatrix from given <b>matrix</b> with indexes of columns from k1 to k2 and all rows
     * @throws Exception
     */
    public static double[][] subMatr(double[][] matrix, int k1, int k2)
    {
        if( k2 < k1 || k2 > matrix.length || k1 < 0 )
            return null;
        double[][] Matrix = new double[matrix.length][k2 - k1 + 1];
        for( int j = 0; j < k2 - k1 + 1; j++ )
        {
            for( int i = 0; i < matrix.length; i++ )
            {
                Matrix[i][j] = matrix[i][k1 + j];
            }
        }
        return Matrix;
    }

    public static class MatrixElementsStatistics
    {
        private final int[][] smallerElements;
        private final int[][] equalElements;
        private final int totalElements;

        public MatrixElementsStatistics(double[][] matrix)
        {
            int n = matrix.length;
            int[] rowLength = new int[n];

            for( int i = 0; i < n; i++ )
                rowLength[i] = matrix[i].length;

            double[] vector = getVectorByMatrix(matrix);
            totalElements = vector.length;
            int[] pos = sortHeap(vector);
            int[] orderVector = new int[totalElements];
            int[] rankVector = new int[totalElements];
            for( int i = 0; i < totalElements; i++ )
                rankVector[pos[i]] = i; //initial ranks

            for( int i = 0; i < totalElements; i++ )
            {
                int finish = i;
                int rank = rankVector[pos[i]];
                while( finish + 1 < totalElements && vector[finish + 1] == vector[finish] )
                {
                    finish++;
                    rank = Math.min(rank, rankVector[pos[i]]);
                }

                for( int j = i; j <= finish; j++ )
                {
                    rankVector[pos[j]] = rank;
                    orderVector[pos[j]] = finish - i + 1;
                }
                i = finish;
            }

            smallerElements = getMatrixByVector(rankVector, n, rowLength);
            equalElements = getMatrixByVector(orderVector, n, rowLength);
        }
        /**
         * returns number of elements in matrix which are greater or equal to current element
         */
        public int getGreaterElementsCount(int i, int j)
        {
            return totalElements - smallerElements[i][j];
        }
        /**
         * returns number of elements in matrix which are smaller or equal to current element
         */
        public int getSmallerElementsCount(int i, int j)
        {
            return smallerElements[i][j] + equalElements[i][j];
        }

        public int getSize()
        {
            return totalElements;
        }
    }

    public static double[] append(double[] ... arrays)
    {
        return Stream.of( arrays ).flatMapToDouble( Arrays::stream ).toArray();
    }

    /**
     * Appending two matrices into one (vertically)
     */
    public static double[][] append(double[][] ... matrices)
    {
        return Stream.of( matrices ).flatMap( Stream::of ).toArray( double[][]::new );
    }

    /**
     * Returns array, which contains initial non squared matrix rows framed in sequence.
     * Rows in matrix could have different length
     * {{1,2,3},{4,5},{6}} => {1,2,3,4,5,6}
     */
    public static double[] getVectorByMatrix(double[][] matrix)
    {
        return append(matrix);
    }

    /**
     * Returns array, which contains initial non squared matrix rows framed in sequence.
     * Rows in matrix could have different length
     * {{1,2,3},{4,5,6}} => {1,2,3,4,5,6}
     */
    public static int[][] getMatrixByVector(int[] vector, int rowCount, int[] rowLength)
    {
        int[][] matrix = new int[rowCount][];

        int startIndex = 0;
        for( int i = 0; i < rowCount; i++ )
        {
            int length = rowLength[i];
            matrix[i] = new int[length];
            System.arraycopy(vector, startIndex, matrix[i], 0, length);
            startIndex += length;
        }
        return matrix;
    }

    /**
     * Returns array, which contains initial non squared matrix rows framed in sequence.
     * Rows in matrix could have different length
     * {{1,2,3},{4,5,6}} => {1,2,3,4,5,6}
     */
    public static double[][] getMatrixByVector(double[] vector, int rowCount, int[] rowLength)
    {
        double[][] matrix = new double[rowCount][];

        int startIndex = 0;
        for( int i = 0; i < rowCount; i++ )
        {
            int length = rowLength[i];
            matrix[i] = new double[length];
            System.arraycopy(vector, startIndex, matrix[i], 0, length);
            startIndex += length;
        }
        return matrix;
    }

    /**
     * Returns a copy of 2D-array
     * @param array
     * @return
     */
    public static double[][] copy(double[][] array)
    {
        if( array == null )
            return null;
        return StreamEx.of(array).map( arr -> arr.clone() ).toArray( double[][]::new );
    }

    /**
     * Ascending array sort.
     *@return int[] pos: pos[i] contains old position of i-th element from the sorted array:
     * beforeSort[pos[i]] == afterSort[i];
     */
    public static int[] sort(double[] x)
    {
        if( x.length < 100 )
            return sortShell(x);
        else
            return sortHeap(x);
    }

    /**
     * Ascending array sort. For the array with length = n, computational complexity = n^1.2
     * Shell method. Recommended for usage when array.length <= 100;
     *@return int[] pos: pos[i] contains old position of i-th element from the sorted array:
     * beforeSort[pos[i]] == afterSort[i];
     */
    public static int[] sortShell(double[] x)
    {
        int[] pos = new int[x.length];
        for( int i = 0; i < x.length; i++ )
            pos[i] = i;

        int n = x.length - 1;
        double tempd = 0;
        int tempi = 0;
        boolean C;
        int i = 0;
        int j = 0;
        int g = (int) ( ( (double)n + 1 ) / 2 );

        do
        {
            i = g;
            do
            {
                j = i - g;
                C = true;
                do
                {
                    if( x[j] <= x[j + g] )
                        C = false;
                    else
                    {
                        tempd = x[j];
                        x[j] = x[j + g];
                        x[j + g] = tempd;

                        tempi = pos[j];
                        pos[j] = pos[j + g];
                        pos[j + g] = tempi;
                    }
                    j--;
                }
                while( ( j >= 0 ) && C );
                i++;
            }
            while( i <= n );
            g = (int) ( (double)g / 2 );
        }
        while( g > 0 );

        return pos;
    }

    /**
     * Ascending array sort. For the array with length = n, computational complexity = n*log(n)
     * Binary trees method. Recommended for usage when array.length >= 100;
     * @return int[] pos: pos[i] contains old position of i-th element from the sorted array:
     * beforeSort[pos[i]] == afterSort[i];
     */
    public static int[] sortHeap(double[] x)
    {
        if( x.length < 2 )
        {
            return new int[] {0};
        }
        int i, k, t = 0;
        int n = x.length;
        int[] pos = new int[n];
        for( i = 0; i < n; i++ )
            pos[i] = i;

        int tempi = 0;
        double tempd = 0;

        i = 2;
        do
        {
            t = i;
            while( t != 1 )
            {
                k = (int) ( (double)t / 2 );
                if( x[k - 1] >= x[t - 1] )
                    t = 1;
                else
                {
                    tempd = x[k - 1];
                    x[k - 1] = x[t - 1];
                    x[t - 1] = tempd;

                    tempi = pos[k - 1];
                    pos[k - 1] = pos[t - 1];
                    pos[t - 1] = tempi;

                    t = k;
                }
            }
            i++;
        }
        while( i <= n );
        i = n - 1;
        do
        {
            tempd = x[i];
            x[i] = x[0];
            x[0] = tempd;

            tempi = pos[i];
            pos[i] = pos[0];
            pos[0] = tempi;

            t = 1;

            while( t != 0 )
            {
                k = 2 * t;
                if( k > i )
                    t = 0;
                else
                {
                    if( k < i )
                    {
                        if( x[k] > x[k - 1] )
                            k++;
                    }
                    if( x[t - 1] >= x[k - 1] )
                        t = 0;
                    else
                    {
                        tempd = x[k - 1];
                        x[k - 1] = x[t - 1];
                        x[t - 1] = tempd;

                        tempi = pos[k - 1];
                        pos[k - 1] = pos[t - 1];
                        pos[t - 1] = tempi;

                        t = k;
                    }
                }
            }
            i--;
        }
        while( i >= 1 );

        return pos;
    }
    
    public static int[] sortHeap(int[] x)
    {
        if( x.length < 2 ) return new int[]{0};
        int i, k, t = 0, tempi = 0, tempd = 0;
        int[] pos = new int[x.length];
        for( i = 0; i < x.length; i++ )
            pos[i] = i;

        i = 2;
        do
        {
            t = i;
            while( t != 1 )
            {
                k = (int)((double)t / 2.0);
                if( x[k - 1] >= x[t - 1] )
                    t = 1;
                else
                {
                    tempd = x[k - 1];
                    x[k - 1] = x[t - 1];
                    x[t - 1] = tempd;
                    tempi = pos[k - 1];
                    pos[k - 1] = pos[t - 1];
                    pos[t - 1] = tempi;
                    t = k;
                }
            }
            i++;
        }
        while( i <= x.length );
        i = x.length - 1;
        do
        {
            tempd = x[i];
            x[i] = x[0];
            x[0] = tempd;
            tempi = pos[i];
            pos[i] = pos[0];
            pos[0] = tempi;
            t = 1;

            while( t != 0 )
            {
                k = 2 * t;
                if( k > i )
                    t = 0;
                else
                {
                    if( k < i )
                    {
                        if( x[k] > x[k - 1] )
                            k++;
                    }
                    if( x[t - 1] >= x[k - 1] )
                        t = 0;
                    else
                    {
                        tempd = x[k - 1];
                        x[k - 1] = x[t - 1];
                        x[t - 1] = tempd;
                        tempi = pos[k - 1];
                        pos[k - 1] = pos[t - 1];
                        pos[t - 1] = tempi;
                        t = k;
                    }
                }
            }
            i--;
        }
        while( i >= 1 );
        return pos;
    }

    /**
     * (MAT027) if x is smaller then a returns a if x bigger then b returns
     * @return a if x < a and b if x > b
     * @throws Exception
     *             if a > b
     */
    public static double restrict(double a, double b, double x)
    {
        if( a > b )
            throw new IllegalArgumentException("Left bound is lesser then right");
        return Math.max(Math.min(b, x), a);
    }

    /**
     * Returns fractional part of double value
     */
    public static double frac(double x)
    {
        return ( x - (int)x );
    }


    /**
     * Returns greater common factor of two positive integers
     */
    public static int getGCF(int a, int b)
    {
        int tempFactor;

        if( a < 1 || b < 1 )
            return -1;
        if( a == b )
            return a;
        for( tempFactor = (int) ( 0.5 * Math.min(a, b) ); tempFactor > 1; tempFactor-- )
        {
            if( frac((double)a / tempFactor) == 0 && frac((double)b / tempFactor) == 0 )
                return tempFactor;
        }
        return 1;
    }

    /**
     * Bessel function
     */
    public static double bessel(double x, double q) throws Exception
    {
        double result = 0;
        for( int k = 0; k < 100; k++ )
        {
            result += Math.pow( -x * x / 4, k) / ( Stat.gammaFunc(k + 1) * Stat.gammaFunc(k + q + 1) );
        }
        result *= Math.pow(x / 2, q);
        return result;
    }
    
    /**
     * Multivariate Epanechnikov kernel
     * TODO: check if it works correctly
     */
    public static double epanechninkovKernel(double[] x, double[][] w)
    {
        RealMatrix inversedMatrix = MatrixUtils.inverse(MatrixUtils.createRealMatrix(w));
        RealMatrix arg = MatrixUtils.createRowRealMatrix(x).multiply(inversedMatrix).multiply(MatrixUtils.createColumnRealMatrix(x));
        return epanechninkovKernel(arg.getEntry(0, 0), 1);
    }

    /**
     * Multivariate Normal kernel
     * TODO: optimize later
     */
    public static double normalKernel(double[] x, double[][] w)
    {
        RealMatrix inversedMatrix = MatrixUtils.inverse(MatrixUtils.createRealMatrix(w));
        RealMatrix arg = MatrixUtils.createRowRealMatrix(x).multiply(inversedMatrix).multiply(MatrixUtils.createColumnRealMatrix(x));
        return Math.exp( -arg.getEntry(0, 0) * 0.5) / ( Math.pow(2 * Math.PI, x.length / 2) );
    }

    public static double estimateDensity(double[] arg, double[][] x, double[][] w)
    {
        double det = new LUDecomposition(MatrixUtils.createRealMatrix(w)).getDeterminant();
        double val = 0;
        for( int i = 0; i < x.length; i++ )
            val += epanechninkovKernel(Util.vectorDiff(arg, x[i]), w);
        return val / (Math.sqrt(det)*x.length);
    }
    
    public static double estimateDensityNormal(double[] arg, double[][] x, double[][] w)
    {
        double det = new LUDecomposition(MatrixUtils.createRealMatrix(w)).getDeterminant();
        
        double val = 0;
        for( int i = 0; i < x.length; i++ )
            val += normalKernel(Util.vectorDiff(arg, x[i]), w);
        return val / (Math.sqrt(det)*x.length);
    }
    
    /**
     * Calculates simple multivariate bandwith matrix for kernel density estimation
     * Taken from R library "ks" (name of method - "Hns")
     */
    public static double[][] calculateKDEBandwidth(double[][] data)
    {
        double[][] variance = Stat.variance(data);
        double factor = Math.pow(4.0 / ( data.length * ( data[0].length + 2 ) ), 2.0 / ( data[0].length + 4 ));
        return Util.multiply(factor, variance);
    }

    
    public static double epanechninkovKernel(double u, double w)
    {
        double uw = u / w;
        if( Math.abs(uw) >= 1 )
            return 0;
        return 0.75 * ( 1 - uw * uw );
    }

    public static double epanechninkovDerivative(double u, double w)
    {
        if( Math.abs(u / w) >= 1 )
            return 0;
        return -1.5 * u / ( w * w );
    }

    /**
     * Nadarya-Watson kernel-weighted average
     * with Epanechnikov kernel
     * @param y - values of input function
     * @param x - argument values of input function, i.e. y[i] = F(x[i])
     * @param w - smoothing parameter, must be greater then max{|x[i]-x[j]| i,j}
     * @return value of smoothed function from argument arg
     */
    public static double nwAverage(double arg, double[] y, double[] x, double w)
    {
        int length = Math.min(y.length, x.length);
        double result = 0;
        double kernelSum = 0;
        for( int i = 0; i < length; i++ )
        {
            double kernel = epanechninkovKernel( ( arg - x[i] ), w);
            result += kernel * y[i];
            kernelSum += kernel;
        }
        if( kernelSum == 0 )
            return Double.MAX_VALUE;
        else
            return result / kernelSum;
    }

    /***
     * 
     * @param xValues
     * @param yValues
     * @param w
     * @return array: array[0] = smoothedXvalues[] (smoothed x-values); array[1] = smoothedYvalues[] (smoothed y-values);
     */
    public static double[][] nwAverage(double[] xValues, double[] yValues, double window)
    {
        double[] smoothedXvalues = DoubleStreamEx.of( xValues ).distinct().toArray();
        double[] smoothedYvalues = DoubleStreamEx.of( smoothedXvalues ).map( x -> nwAverage( x, yValues, xValues, window ) ).toArray();
        return new double[][] {smoothedXvalues, smoothedYvalues};
    }

    // old version
    /***
     * 
     * @param xValuesAndYvalues xValuesAndYvalues[*][0] = x-values; xValuesAndYvalues[*][1] - y-values
     * @param w smoothing window
     * @return List of 2 arrays; 1-st array = smoothed x - x-values; 2-nd array = smoothed y-values
     */
    /*******
    public static List<double[]> nwAverage(double[][] xValuesAndYvalues, double w)
    {
        List<double[]> result = new ArrayList<double[]>();
        double[] x = new double[xValuesAndYvalues.length];
        double[] y = new double[xValuesAndYvalues.length];
        for( int i = 0; i < xValuesAndYvalues.length; i++ )
        {
            x[i] = xValuesAndYvalues[i][0];
            y[i] = xValuesAndYvalues[i][1];
        }
        List<Double> distinctXvalues = new ArrayList<Double>();
        for( int i = 0; i < xValuesAndYvalues.length; i++ )
            if( ! distinctXvalues.contains(xValuesAndYvalues[i][0]) )
                distinctXvalues.add(xValuesAndYvalues[i][0]);
        double[] xValues = new double[distinctXvalues.size()];
        double[] yValues = new double[distinctXvalues.size()];
        for( int i = 0; i < distinctXvalues.size(); i++ )
        {
            xValues[i] = distinctXvalues.get(i);
            yValues[i] = nwAverage(xValues[i], y, x, w);
        }
        result.add(0, xValues);
        result.add(1, yValues);
        return result;
    }
    ******/

 // new version
    /***
     * 
     * @param xValuesAndYvalues xValuesAndYvalues[*][0] = x-values; xValuesAndYvalues[*][1] - y-values
     * @param window smoothing window
     * @return List of 2 arrays; 1-st array = smoothed x - x-values; 2-nd array = smoothed y-values
     */
    public static List<double[]> nwAverage(double[][] xValuesAndYvalues, double window)
    {
        List<double[]> result = new ArrayList<>();
        double[] x = new double[xValuesAndYvalues.length];
        double[] y = new double[xValuesAndYvalues.length];
        for( int i = 0; i < xValuesAndYvalues.length; i++ )
        {
            x[i] = xValuesAndYvalues[i][0];
            y[i] = xValuesAndYvalues[i][1];
        }
        double[][] smoothedCurve = nwAverage(x, y, window);
        result.add(0, smoothedCurve[0]);
        result.add(1, smoothedCurve[1]);
        return result;
    }

    public static double kernelDerivative(double arg, double[] y, double[] x, double w)
    {
        int length = Math.min(y.length, x.length);
        double sum = 0;
        double kernelSum = 0;
        double derivativeSum = 0;
        double derivativeKernelSum = 0;
        for( int i = 0; i < length; i++ )
        {
            double kernel = epanechninkovKernel( ( arg - x[i] ), w);
            double kernelDerivative = epanechninkovDerivative( ( arg - x[i] ), w);
            sum += kernel * y[i];
            kernelSum += kernel;
            derivativeSum += kernelDerivative * y[i];
            derivativeKernelSum += kernelDerivative;
        }
        return ( derivativeSum * kernelSum - derivativeKernelSum * sum ) / ( kernelSum * kernelSum );
    }

    /**
     * bijection of interval [a,b] on interval [c,d]
     * x from [a,b] is put in correspondence bijection(a,b,c,d,x) from [c,d]
     */
    public static double bijection(double a, double b, double c, double d, double x)
    {
        return d * ( x - a ) / ( b - a ) + c * ( x - b ) / ( a - b );
    }

    /**
     * evaluate value of first or second kind chebyshev polynomial
     * r - kind of polynomial (1 or 2)
     * n - degree of polynomial (n>=0)
     * x - point
     */
    public static double chebyshev(int r, int n, double x)
    {
        double result = 0;
        double tempd1, tempd2 = 0;
        if( r == 1 )
        {
            tempd1 = 1;
            tempd2 = x;
        }
        else
        {
            tempd1 = 1;
            tempd2 = 2 * x;
        }
        if( n == 0 )
            result = tempd1;
        if( n == 1 )
            result = tempd2;

        if( n >= 2 )
        {
            for( int i = 2; i <= n; i++ )
            {
                result = 2 * x * tempd2 - tempd1;
                tempd1 = tempd2;
                tempd2 = result;
            }
        }
        return result;
    }
    /**
     * Approximation of tabular data with first kind chebyshev polynomials
     * xx - mesh nodes (time points)
     * y - values at nodes (that should be approximated)
     * m - degree approximating of polynomials (number of chebyshev polynomials which will be used for approximation)
     * the result is array of m coefficients before corresponding chebyshev polynomials which provide least square approximation of our values on interval [-1,1]
     */
    public static double[] chebyshevApproximation(double[] xx, double[] y, int m)
    {
        //        if( xx.length != y.length )
        //            throw new Exception("Dimension mismatch of x and y array");
        int pointsNumb = y.length;
        double[][] A = new double[pointsNumb][m + 1]; //A - matrix of superdefinite system
        double[] x = new double[pointsNumb];//x - images of mesh nodes on interval [-1,1]

        for( int i = 0; i < pointsNumb; i++ )
        {
            x[i] = bijection(getMin(xx), getMax(xx), -1, 1, xx[i]);
        }

        for( int i = 0; i < pointsNumb; i++ )
        {
            for( int j = 0; j < m + 1; j++ )
            {
                A[i][j] = chebyshev(1, j, x[i]);
            }
        }

        double[][] N = new double[m + 1][m + 1]; // N - matrix of normal equation system
        double[] f = new double[m + 1]; // f - right part of normal equation system

        for( int i = 0; i < m + 1; i++ )
        {
            f[i] = 0;
            for( int k = 0; k < pointsNumb; k++ )// m+1
            {
                f[i] += y[k] * A[k][i];
            }
            for( int j = i; j < m + 1; j++ )
            {
                N[i][j] = 0;
                for( int k = 0; k < pointsNumb; k++ )//m+1
                {
                    N[i][j] += A[k][i] * A[k][j];
                }
                N[j][i] = N[i][j];
            }
        }

        return linearSolve(N, f);
    }
    /**
     * (used after chebyshevApproximation method)
     * C - obtained with chebyshevApproximation array of coefficients
     * [a,b] - interval of mesh values xx in chebyshevApproximation method, so if array xx is ascendancy ordered, then xx[0]=a, xx[xx.length-1]=b.
     * y - point from [a,b]
     * result is value of chebyshev approximation in point y.
     * (sum of chebyshev polynomials is calculated by Clenshaw method)
     */
    public static double chebApprValue(double[] C, double a, double b, double y)
    {
        int m = C.length - 1; // degree of polynom
        double x = bijection(a, b, -1, 1, y);
        double result = 0;
        double b1 = 0;
        double b2 = 0;
        int i = m;
        while( i >= 1 )
        {
            result = 2 * x * b1 - b2 + C[i];
            b2 = b1;
            b1 = result;
            i--;
        }
        result = -b2 + x * b1 + C[0];

        return result;
    }

    /**
     * calculates derivative of approximation obtained with chebyshevApproximation method
     * @param C - obtained with chebyshevApproximation array of coefficients
     * @param [a,b] - interval of mesh values xx in chebyshevApproximation method, so if array xx is ascendancy ordered, then xx[0]=a, xx[xx.length-1]=b
     * @param y - point from [a,b]
     * @result result is value of chebyshev approximation derivative in point y.
     */
    public static double chebyshevDerivative(double[] C, double a, double b, double y)
    {
        int m = C.length - 1; //degree of polynomial
        double x = bijection(a, b, -1, 1, y);
        double result = 0;
        for( int i = m; i > 0; i-- )
        {
            result += i * C[i] * chebyshev(2, i - 1, x);
        }
        return result;
    }


    /**
     * degree selection for approximating polynomial, should be used only if nothing about data structure in known.
     * delta - data measure of inaccuracy
     */
    public static int chebyshevDegreeAutoSelect(double[] xx, double[] y, double delta)
    {
        int degree = 1;
        int prev = 0;
        int[] lessabove = new int[xx.length - 2];
        double tempd = 0;
        double[] h = makeWidth(xx); // array of grid steps
        for( int i = 0; i < xx.length - 2; i++ )
        {
            tempd = y[i + 2] - y[i] * ( xx[i + 1] - xx[i + 2] ) / h[i] + y[i + 1] * ( xx[i + 2] - xx[i] ) / h[i];
            if( Math.abs(tempd) <= delta )
                lessabove[i] = 0;
            else
                lessabove[i] = (int)Math.signum(tempd);
        }
        for( int i = 0; i < xx.length - 2; i++ )
        {
            if( ( lessabove[i] != prev ) && ( lessabove[i] != 0 ) )
            {
                degree++;
                prev = lessabove[i];
            }
        }
        return degree;
    }


    /**
     * finds maximum value in array
     */
    public static double getMax(double[] x)
    {
        return Arrays.stream( x ).max().getAsDouble();
    }

    /**
     * finds minimum value in array
     */
    public static double getMin(double[] x)
    {
        return Arrays.stream( x ).min().getAsDouble();
    }

    static public class Smoothing
    {
        protected double[] function;
        protected double[] argument;
        protected String type;

        public Smoothing()
        {
            function = new double[0];
            argument = new double[0];
        }

        public Smoothing(double[] x, double[] f)
        {
            function = f;
            argument = x;
        }

        public void preprocess()
        {

        }

        public double getValue(double x)
        {
            return 0;
        }
        public double getDerivative(double x)
        {
            return 0;
        }

        public String getType()
        {
            return type;
        }

        @Override
        public Smoothing clone()
        {
            return new Smoothing();
        }
    }

    static public class LSSmoothing extends Smoothing
    {
        private final int n;
        private double[] C;
        private final double min;
        private final double max;

        public LSSmoothing(double[] x, double[] f, int n)
        {
            super(x, f);
            this.n = n;
            type = "LS";
            preprocess();
            min = getMin(argument);
            max = getMax(argument);
        }

        @Override
        public void preprocess()
        {
            C = chebyshevApproximation(argument, function, n);
        }

        @Override
        public double getValue(double x)
        {
            return chebApprValue(C, min, max, x);
        }

        @Override
        public double getDerivative(double x)
        {
            return chebyshevDerivative(C, min, max, x);
        }

        @Override
        public Smoothing clone()
        {
            return new LSSmoothing(argument, function, n);
        }
    }

    static public class KernelSmoothing extends Smoothing
    {
        private final double w;

        public KernelSmoothing(double[] x, double[] f, double w)
        {
            super(x, f);
            this.w = w;
            type = "Kernel";
        }

        @Override
        public double getValue(double x)
        {
            return nwAverage(x, function, argument, w);
        }

        @Override
        public double getDerivative(double x)
        {
            return kernelDerivative(x, function, argument, w);
        }

        @Override
        public Smoothing clone()
        {
            return new KernelSmoothing(argument, function, w);
        }
    }

    static public class SplineSmoothing extends Smoothing
    {
        protected double[][] res;
        private final double delta;

        public SplineSmoothing(double[] x, double[] f, double delta)
        {
            super(x, f);
            this.delta = delta;
            type = "Spline";
            preprocess();
        }

        @Override
        public void preprocess()
        {
            res = smoothingSplineApproximation(argument, function, delta);
        }

        @Override
        public double getValue(double x)
        {
            return smoothigSplineValue(argument, res[1], res[0], x, 0);
        }

        @Override
        public double getDerivative(double x)
        {
            return smoothigSplineValue(argument, res[1], res[0], x, 1);
        }

        public double getSecondDerivative(double x)
        {
            return smoothigSplineValue(argument, res[1], res[0], x, 2);
        }

        public double getThirdDerivative(double x)
        {
            return smoothigSplineValue(argument, res[1], res[0], x, 3);
        }

        @Override
        public Smoothing clone()
        {
            SplineSmoothing result = new SplineSmoothing(argument, function, delta);
            result.res[0] = this.res[0];
            return result;
        }
    }

    static public class FastSplineSmoothing extends SplineSmoothing
    {

        public FastSplineSmoothing(double[] x, double[] f, double delta)
        {
            super(x, f, delta);
            type = "Fast spline";
            preprocess();
        }

        @Override
        public void preprocess()
        {
            res = fastsmoothingSplineApproximation(argument, function);
        }

    }
    /**
     *  derivation of cubic smoothing spline coefficients
     */
    public static double[][] smoothingSplineApproximation(double[] x, double[] f, double delta)
    {
        double[][] result = new double[2][x.length];// result[0] = array of spline coefficients; result[1] = array of new spline  values in knots

        int n = x.length - 1;
        double[] h = makeWidth(x); // array of grid steps
        double[] ro = new double[n + 1]; // array of weight factors
        double[] D = new double[n + 1];
        int count = 0;
        boolean flag;
        for( int i = 0; i <= n; i++ )
        {
            D[i] = 0;
            result[1][i] = f[i];
        }

        double[][] matrix = new double[n + 1][n + 1];
        double[] g = new double[n + 1];

        do
        {

            for( int i = 0; i < n + 1; i++ )
            {
                if( Math.abs(D[i]) >= 0.00001 )
                {
                    ro[i] = 0.9 * delta / Math.abs(D[i]);
                }
                else
                {
                    ro[i] = 0;
                }
            }


            matrix[0][0] = 1;// h[0] / 3 + ( ro[0] + ro[1] ) / ( h[0] * h[0] ); // a[0]
            matrix[0][1] = 0;// h[0] / 6 - ( h[0] + h[1] ) * ro[1] / ( ( h[0] * h[0] ) * h[1] ) - ro[0] / ( h[0] * h[0] ); // b[0]
            matrix[1][0] = matrix[0][1];
            matrix[0][2] = 0;// ro[1] / ( h[0] * h[1] );// c[0]
            matrix[2][0] = matrix[0][2];
            g[0] = 0; //g[0]

            matrix[n][n] = 1;//h[n - 1] / 3 + ( ro[n - 1] + ro[n] ) / ( h[n - 1] * h[n - 1] );// a[N]
            matrix[n][n - 1] = 0;//h[n - 1] / 6 - ( h[n - 2] + h[n - 1] ) * ro[n - 1] / ( h[n - 1] * h[n - 1] * h[n - 2] ) - ro[n]/ ( h[n - 1] * h[n - 1] );// b[N-1]
            matrix[n - 1][n] = matrix[n][n - 1];
            matrix[n][n - 2] = 0;//ro[n - 1] / ( h[n - 1] * h[n - 2] );//c[N-2]
            matrix[n - 2][n] = matrix[n][n - 2];
            g[n] = 0;// g[N]

            for( int i = 1; i < n; i++ ) //a[]
            {
                matrix[i][i] = ( h[i - 1] + h[i] ) / 3 + ro[i - 1] / ( h[i - 1] * h[i - 1] ) + Math.pow(1 / h[i - 1] + 1 / h[i], 2) * ro[i]
                        + ro[i + 1] / ( h[i] * h[i] );//a[i]
                g[i] = ( f[i + 1] - f[i] ) / h[i] - ( f[i] - f[i - 1] ) / h[i - 1];
            }

            for( int i = 1; i < n - 1; i++ ) //b[]
            {
                matrix[i][i + 1] = h[i] / 6 - ( ( 1 / h[i - 1] + 1 / h[i] ) * ro[i] + ( 1 / h[i] + 1 / h[i + 1] ) * ro[i + 1] ) / h[i];
                matrix[i + 1][i] = matrix[i][i + 1];
            }

            for( int i = 1; i < n - 2; i++ ) //c[]
            {
                matrix[i][i + 2] = ro[i + 1] / ( h[i] * h[i + 1] );
                matrix[i + 2][i] = matrix[i][i + 2];
            }

            result[0] = linearSolve(matrix, g);

            D[0] = ( result[0][1] - result[0][0] ) / h[0];
            D[n] = ( result[0][n - 1] - result[0][n] ) / h[n - 1];
            for( int i = 1; i < n; i++ )
            {
                D[i] = ( result[0][i + 1] - result[0][i] ) / h[i] - ( result[0][i] - result[0][i - 1] ) / h[i - 1];
            }


            for( int i = 0; i < n + 1; i++ )
            {
                result[1][i] = f[i] - ro[i] * D[i];
            }

            flag = false;
            for( int i = 0; i < n + 1; i++ )
            {
                if( ro[i] * Math.abs(D[i]) > delta )
                {
                    flag = true;
                    break;
                }
            }

            count++;
        }
        while( ( flag || count < 10 ) && ( count < 50 ) );
        // ++++++++++++++++++++++++++++++++++
        return result;
    }

    /**
     * here delta must be the same as in smoothingSplineApproximation
     * M[] - array of spline coefficients retrieved during smoothingSplineApproximation procedure
     * derivativeDegree degree of derivative (derivativeDegree==0 means value of spline)
     */
    public static double smoothigSplineValue(double[] xx, double[] f, double[] M, double x, int derivativeDegree)
    //            throws Exception
    {
        //        if( derivativeDegree < 0 || derivativeDegree > 3 )
        //            throw new Exception("degree of derivative should be below 4 and above 0");
        double result = -9999;
        int N = xx.length - 1;
        double[] h = makeWidth(xx); // array of grid steps

        for( int i = 0; i < N; i++ )
        {
            if( x <= xx[i + 1] )
            {
                double t = ( x - xx[i] ) / h[i];
                if( derivativeDegree == 0 )
                    result = f[i] * ( 1 - t ) + f[i + 1] * t - h[i] * h[i] * t * ( 1 - t ) * ( ( 2 - t ) * M[i] + ( 1 + t ) * M[i + 1] )
                            / 6; // spline value
                else if( derivativeDegree == 1 )
                    result = ( f[i + 1] - f[i] ) / h[i] - h[i] * ( ( 2 - 6 * t + 3 * t * t ) * M[i] + ( 1 - 3 * t * t ) * M[i + 1] ) / 6; // first derivative
                else if( derivativeDegree == 2 )
                    result = M[i] * ( 1 - t ) + M[i + 1] * t; // second derivative
                else
                    result = ( M[i + 1] - M[i] ) / h[i]; // third derivative
                break;
            }
        }
        return result;
    }


    /**
     *  derivation of cubic smoothing spline coefficients
     */
    public static double[][] fastsmoothingSplineApproximation(double[] x, double[] f)
    {
        double[][] result = new double[2][x.length];// result[0] = array of spline coefficients; result[1] = array of new spline  values in knots
        int n = x.length - 1;
        int i = 0;
        double[] h = makeWidth(x); // array of grid steps
        double ro = Math.pow(Stat.mean(h), 3) / 24; //weigth factor


        double[][] matrix = new double[n + 1][n + 1];
        double[] g = new double[n + 1];
        double[] D = new double[n + 1];
        // +++++++++++++++++++++++++++++++

        matrix[0][0] = 1;// h[0] / 3 + 2*ro / ( h[0] * h[0] ); // a[0]
        matrix[0][1] = 0;// h[0] / 6 - ( h[0] + h[1] ) * ro / ( ( h[0] * h[0] ) * h[1] ) - ro / ( h[0] * h[0] ); // b[0]
        matrix[1][0] = matrix[0][1];
        matrix[0][2] = 0;// ro / ( h[0] * h[1] );// c[0]
        matrix[2][0] = matrix[0][2];
        g[0] = 0; //g[0]

        matrix[n][n] = 1;//h[n - 1] / 3 + 2*ro/ ( h[n - 1] * h[n - 1] );// a[N]
        matrix[n][n - 1] = 0;// h[n - 1] / 6 - ( h[n - 2] + h[n - 1] ) * ro / ( h[n - 1] * h[n - 1] * h[n - 2] ) - ro/ ( h[n - 1] * h[n - 1] );// b[N-1]
        matrix[n - 1][n] = matrix[n][n - 1];
        matrix[n][n - 2] = 0;// ro / ( h[n - 1] * h[n - 2] );//c[N-2]
        matrix[n - 2][n] = matrix[n][n - 2];
        g[n] = 0;// g[N]

        for( i = 1; i < n; i++ ) //a[]
        {
            matrix[i][i] = ( h[i - 1] + h[i] ) / 3 + ro / ( h[i - 1] * h[i - 1] ) + Math.pow(1 / h[i - 1] + 1 / h[i], 2) * ro + ro
                    / ( h[i] * h[i] );//a[i]
            g[i] = ( f[i + 1] - f[i] ) / h[i] - ( f[i] - f[i - 1] ) / h[i - 1];
        }

        for( i = 1; i < n - 1; i++ ) //b[]
        {
            matrix[i][i + 1] = h[i] / 6 - ( ( 1 / h[i - 1] + 1 / h[i] ) * ro + ( 1 / h[i] + 1 / h[i + 1] ) * ro ) / h[i];
            matrix[i + 1][i] = matrix[i][i + 1];
        }

        for( i = 1; i < n - 2; i++ ) //c[]
        {
            matrix[i][i + 2] = ro / ( h[i] * h[i + 1] );
            matrix[i + 2][i] = matrix[i][i + 2];
        }

        result[0] = linearSolve(matrix, g);

        D[0] = ( result[0][1] - result[0][0] ) / h[0];
        D[n] = ( result[0][n - 1] - result[0][n] ) / h[n - 1];
        for( i = 1; i < n; i++ )
            D[i] = ( result[0][i + 1] - result[0][i] ) / h[i] - ( result[0][i] - result[0][i - 1] ) / h[i - 1];


        for( i = 0; i < n + 1; i++ )
        {
            result[1][i] = f[i] - ro * D[i];
        }

        // ++++++++++++++++++++++++++++++++++
        return result;
    }


    /**
     *  xx - mesh knots
     *  makeWidth creates array of grid steps, so width[i] = xx[i+1]-xx[i] is size of i-th step.
     */
    public static double[] makeWidth(double[] xx)
    {
        return DoubleStreamEx.of( xx ).pairMap( (a, b) -> ( b - a ) ).toArray();
    }


    /**
     * conjugate gradient method
     * most  applicable for disperse matrixes
     */
    //    public static double[] conGradient(double[][] matrix, double[] right,double error) throws Exception
    //    {
    //        int iterNumb =20;
    //        boolean flag =true;
    //        if( matrix.length != matrix[1].length )
    //            throw new Exception("Matrix is not square");
    //        if( matrix.length != right.length )
    //            throw new Exception("Dimension of the right side differs from matrix dimension");
    //
    //        for( int i = 0; i < right.length; i++ )
    //        {
    //            for( int j = 0; j < i; j++ )
    //                if( matrix[i][j] != matrix[j][i] )
    //                    flag=false;
    //        }
    //
    //     double a=0, b =0;
    //     double[] tempvect1= new double[right.length];
    //     double[] tempvect2= new double[right.length];
    //     double tempd=0;
    //     double[] r0= new double[right.length];
    //     double[] r1= new double[right.length];
    //     double[] ans= new double[right.length];
    //     for (int i=0; i<right.length; i++)
    //         {
    //         r0[i]= right[i];
    //         ans[i]= 0;
    //         }
    //     double[] p= r0;
    //     if (right == ans) return ans;
    //     if (flag)
    //     {
    //     for (int iterCount=0; iterCount<iterNumb; iterCount++)
    //     {
    //       tempvect1 =rightMultiply(matrix,p);
    //       tempd = scalarMultiply(tempvect1,p);
    //       a= scalarMultiply(r0,r0)/tempd;
    //       tempvect1 =scalarXvector(a,p);
    //       ans= vectorSum(ans,tempvect1);
    //       tempvect1 =rightMultiply(matrix,p);
    //       tempvect2=scalarXvector(-a,tempvect1);
    //       r1= vectorSum(r0,tempvect2);
    //       //if (vectorSecondNorm(r1)<error) break;
    //       tempd =scalarMultiply(r0,r0);
    //       b= scalarMultiply(r1,r1)/tempd;
    //       tempvect1=scalarXvector(b,p);
    //       p= vectorSum(r1,tempvect1);
    //     }
    //     }
    //     else
    //     {
    //         double[] r0t= r0;
    //         double[] r1t= new double[right.length];
    //         double[] pt= r0t;
    //         for (int iterCount=0; iterCount<iterNumb; iterCount++)
    //         {
    //           a= scalarMultiply(r0,r0t)/scalarMultiply(rightMultiply(matrix,p),pt);
    //           ans= vectorSum(ans,scalarXvector(a,p));
    //           r1= vectorSum(r0,scalarXvector(-a,rightMultiply(matrix,p)));
    //           if (vectorSecondNorm(r1)<error) break;
    //           r1t= vectorSum(r0t,scalarXvector(-a,rightMultiply(matrixConjugate(matrix),pt)));
    //           b= scalarMultiply(r1,r1t)/scalarMultiply(r0,r0t);
    //           if (b==0) throw new Exception("method fall apart");
    //           p= vectorSum(r1,scalarXvector(b,p));
    //           pt= vectorSum(r1t,scalarXvector(b,pt));
    //         }
    //
    //     }
    //
    //     return ans;
    //    }
    /**
     * solves linear equations system
     * returns ans: matrix*ans=right;
     */
    public static double[] linearSolve(double[][] matrix, double[] right)// throws Exception
    {
        double[][] r = new double[right.length][];
        Arrays.setAll( r, i -> new double[] {right[i]} );
        Matrix A = new Matrix(matrix);
        Matrix b = new Matrix(r);
        Matrix x = A.solve(b);
        return x.getColumnPackedCopy();
    }



    /**
     * Cubic interpolation spline
     * @author axec
     *
     */
    public static class CubicSpline
    {
        private int n;
        private double[] a;
        private double[] b;
        private double[] c;
        private double[] d;
        private double[] x;

        public CubicSpline(double[] argument, double[] function)
        {
            init(argument, function);

            double[] h = new double[n - 1];
            for( int i = 0; i < n - 1; i++ )
                h[i] = argument[i + 1] - argument[i];

            double mu[] = new double[n - 1];
            double z[] = new double[n];

            double g = 0;
            for( int i = 1; i < n - 1; i++ )
            {
                g = 2 * ( x[i + 1] - x[i - 1] ) - h[i - 1] * mu[i - 1];
                mu[i] = h[i] / g;
                z[i] = ( 3 * ( a[i + 1] * h[i - 1] - a[i] * ( x[i + 1] - x[i - 1] ) + a[i - 1] * h[i] ) / ( h[i - 1] * h[i] ) - h[i - 1]
                        * z[i - 1] )
                        / g;
            }

            for( int j = n - 2; j >= 0; j-- )
            {
                c[j] = z[j] - mu[j] * c[j + 1];
                b[j] = ( a[j + 1] - a[j] ) / h[j] - h[j] * ( c[j + 1] + 2d * c[j] ) / 3;
                d[j] = ( c[j + 1] - c[j] ) / ( 3 * h[j] );


            }
        }

        private void init(double[] argument, double[] function)
        {
            n = argument.length;

            if( n < 3 )
                throw new IllegalArgumentException("There must be more than 3 points.");

            if( n != function.length )
                throw new IllegalArgumentException("Function and argument arrays lengths must agree.");

            for( int i = 1; i < n; i++ )
            {
                if( argument[i - 1] >= argument[i] )
                    throw new IllegalArgumentException("Argument values must be ascending.");
            }

            x = Arrays.copyOf(argument, n);
            a = Arrays.copyOf(function, n);
            b = new double[n];
            c = new double[n + 1];
            d = new double[n];
        }

        public double[] getArgument()
        {
            return x;
        }

        public double[] getA()
        {
            return a;
        }

        public double[] getB()
        {
            return b;
        }

        public double[] getC()
        {
            return c;
        }

        public double[] getD()
        {
            return d;
        }

        public double getValue(double argument)
        {
            if( argument < x[0] || argument > x[n - 1] )
                throw new IllegalArgumentException("Argument " + argument + "is out of interpolation range " + "[ " + x[0] + " , "
                        + x[n - 1] + " ]");

            int i;
            for( i = 0; i < n - 1; i++ )
            {
                if( argument >= x[i] && argument <= x[i + 1] )
                {
                    break;
                }
            }
            double diff = argument - x[i];
            return a[i] + b[i] * diff + c[i] * diff * diff + d[i] * diff * diff * diff;
        }
    }

    /**
     * Linear interpolation spline
     * @author helenka
     *
     */
    public static class LinearSpline
    {
        private int n;
        private double[] a;
        private double[] b;
        private double[] x;

        public LinearSpline(double[] argument, double[] function)
        {
            init(argument, function);

            for( int i = 0; i < n - 1; i++ )
                b[i] = ( a[i + 1] - a[i] ) / ( x[i + 1] - x[i] );
        }

        private void init(double[] argument, double[] function)
        {
            n = argument.length;

            if( n < 2 )
                throw new IllegalArgumentException("There must be more than 2 points.");

            if( n != function.length )
                throw new IllegalArgumentException("Function and argument arrays lengths must agree.");

            for( int i = 1; i < n; i++ )
            {
                if( argument[i - 1] >= argument[i] )
                    throw new IllegalArgumentException("Argument values must be ascending.");
            }

            x = Arrays.copyOf(argument, n);
            a = Arrays.copyOf(function, n);
            b = new double[n];
        }

        public double[] getArgument()
        {
            return x;
        }

        public double[] getA()
        {
            return a;
        }

        public double[] getB()
        {
            return b;
        }

        public double getValue(double argument)
        {
            if( argument < x[0] || argument > x[n - 1] )
                throw new IllegalArgumentException("Argument " + argument + "is out of interpolation range " + "[ " + x[0] + " , "
                        + x[n - 1] + " ]");

            int i;
            for( i = 0; i < n - 1; i++ )
            {
                if( argument >= x[i] && argument <= x[i + 1] )
                {
                    break;
                }
            }
            double diff = argument - x[i];
            return a[i] + b[i] * diff;
        }
    }

    //logarithm base features
    public final static int NONE = 0;
    public final static int LOG2 = 1;
    public final static int LOG10 = 2;
    public final static int LN = 3;

    public static final Map<String, Integer> logBaseNameToCode = ArrayUtils.toMap(new Object[][] { {"non-logarithmic", NONE}, {"log2", LOG2},
            {"log10", LOG10}, {"logE", LN}});

    public static String[] getLogarithmBaseNames()
    {
        return logBaseNameToCode.keySet().toArray(new String[logBaseNameToCode.keySet().size()]);
    }

    public static Integer getLogarithmBaseCode(String logarithmBaseName)
    {
        return logBaseNameToCode.get(logarithmBaseName);
    }

    public static double getLogarithmBase(String baseName)
    {
        return getLogarithmBase(getLogarithmBaseCode(baseName));
    }

    public static double getLogarithmBase(int code)
    {
        switch( code )
        {
            case NONE:
            {
                return 1;
            }
            case LOG2:
            {
                return 2;
            }
            case LOG10:
            {
                return 10;
            }
            case LN:
            {
                return Math.E;
            }
            default:
                return 1;
        }
    }

    public static String getRCodeLogTransform(String variableName, String in, String out)
    {
        int codeIn = getLogarithmBaseCode(in);
        int codeOut = getLogarithmBaseCode(out);
        return getRCodeLogTransform(variableName, codeIn, codeOut);
    }


    public static String getRCodeLogTransform(String variableName, int codeIn, int codeOut)
    {
        if( codeIn == codeOut )
            return variableName;


        if( codeOut == NONE )
        {
            return "exp(" + getRCodeLogTransform(variableName, codeIn, LN) + ")";
        }
        String baseIn = codeIn == LN ? "exp(1)" : "" + getLogarithmBase(codeIn) + "";
        String baseOut = codeOut == LN ? "exp(1)" : "" + getLogarithmBase(codeOut) + "";
        if( codeIn == NONE )
        {
            return "log(" + variableName + "," + baseOut + ")";
        }
        return variableName + "*log(" + baseIn + "," + baseOut + ")";
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gauss-Jordan elimination
    //

    public static GaussJordanElimination getGaussJordanElimination(double[][] matrix)
    {
        return new GaussJordanElimination(matrix);
    }

    /**
     * For an n-by-m matrix A the Gauss-Jordan elimination is an algorithm for getting n-by-m matrix R
     * in reduced row echelon form using elementary row operations so that M * A = R,
     * where the m-by-m matrix M is the product of elementary matrixes.
     */
    public static class GaussJordanElimination
    {
        private double[][] R;

        /**
         * Returns the matrix in reduced row echelon form.
         */
        public double[][] getR()
        {
            return this.R;
        }

        private double[][] M;

        /**
         * Returns the matrix representing a product of elementary matrixes
         * and mapping the initial matrix to the matrix R:
         * M * A = R
         */
        public double[][] getM()
        {
            return this.M;
        }

        private double[][] P;

        /**
         * Returns a permutation matrix, which is the product of all elementary
         * matrixes swiching rows of the initial matrix during the matrix R calculation.
         */
        public double[][] getP()
        {
            return this.P;
        }

        public GaussJordanElimination(double[][] arg)
        {
            getDecomposition(arg);
        }

        private void getDecomposition(double[][] arg)
        {
            int n = arg.length;
            int m = arg[0].length;

            M = identityMatrix(n);
            P = identityMatrix(n);

            R = new double[n][m];
            for( int i = 0; i < n; ++i )
            {
                for( int j = 0; j < m; ++j )
                {
                    R[i][j] = arg[i][j];
                }
            }

            int i = 0, j = 0;
            while( i < n && j < m )
            {
                int k = i;
                while( k < n && R[k][j] == 0 )
                    k++;

                if( k < n )
                {
                    if( k != i )
                    {
                        swich(R, M, P, i, k, j);
                    }

                    if( R[i][j] != 1 )
                    {
                        divide(R, M, i, j);
                    }

                    eliminate(R, M, i, j);
                    i++;
                }
                j++;
            }
        }

        /**
         * Switches all elements of matrixe R on row r with their counterparts on row l.
         */
        private void swich(double[][] R, double[][] M, double[][] P, int r, int l, int k)
        {
            int n = R.length;
            int m = R[0].length;

            double temp;
            for( int j = k; j < m; ++j )
            {
                temp = R[r][j];
                R[r][j] = R[l][j];
                R[l][j] = temp;
            }

            for( int i = 0; i < n; ++i )
            {
                temp = M[r][i];
                M[r][i] = M[l][i];
                M[l][i] = temp;
                
                temp = P[r][i];
                P[r][i] = P[l][i];
                P[l][i] = temp;
            }
        }

        /**
         * Divides all elements of R on row r by R[r][l] where R[r][l] is non zero.
         */
        private void divide(double[][] R, double[][] M, int r, int l)
        {
            int n = R.length;
            int m = R[0].length;

            for( int j = l + 1; j < m; ++j )
            {
                R[r][j] /= R[r][l];
            }

            for( int i = 0; i < n; ++i )
            {
                M[r][i] /= R[r][l];
            }

            R[r][l] = 1;
        }

        /**
         * Subtracts row r of the matrixes R multiplied by R[i][l] from every other row i of R.
         */
        private void eliminate(double[][] R, double[][] M, int r, int l)
        {
            int n = R.length;
            int m = R[0].length;

            for( int i = 1; i < n; ++i )
            {
                if( i != r && R[i][l] != 0 )
                {
                    for( int j = l + 1; j < m; ++j )
                    {
                        R[i][j] -= R[i][l] * R[r][j];
                    }

                    for( int j = 0; j < n; ++j )
                    {
                        M[i][j] -= R[i][l] * M[r][j];
                    }

                    R[i][l] = 0;
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    
}
