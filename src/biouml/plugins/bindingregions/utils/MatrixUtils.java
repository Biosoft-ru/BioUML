package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;
import java.util.logging.Logger;

import ru.biosoft.analysis.Util;

/**
 * @author yura
 *
 */
public class MatrixUtils
{
    // it is copied
    public static final int DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE = 10000;
    public static final double DEFAULT_EPS_FOR_INVERSE = 1.0E-5;

    // it is copied
    public static void printMatrix(Logger log, String name, double[][] matrix)
    {
        if( name != null )
            log.info("dim(" + name + ") = " + matrix.length + " x " + matrix[0].length + "; " + name + " =");
        EntryStream.of( matrix ).mapKeyValue( (i, row) -> i + ") " + DoubleStreamEx.of( row ).boxed().joining( " " ) )
                .forEach( log::info );
    }

    // it is copied
    public static void printVector(Logger log, String name, double[] vector)
    {
        if( name != null )
            log.info(name + " dim = " + vector.length);
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < vector.length; i++ )
            sb.append(" ").append(i).append(") ").append(vector[i]);
        log.info(sb.toString());
    }
    
    public static void printVector(Logger log, String name, int[] vector)
    {
        if( name != null )
            log.info(name + " dim = " + vector.length);
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < vector.length; i++ )
            sb.append(" ").append(i).append(") ").append(vector[i]);
        log.info(sb.toString());
    }

    // it is copied
    public static double[][] getProductOfRectangularMatrices(double[][] firstMatrix, double[][] secondMatrix)
    {
        int n = firstMatrix.length , m = secondMatrix[0].length, mm = firstMatrix[0].length;
        if( mm != secondMatrix.length ) throw new IllegalArgumentException("Wrong matrix dimensions: can not multiply " + n + "x" + mm + " and " + secondMatrix.length + "x" + m + " matrixes");
        double[][] result = new double[n][m];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < m; j++ )
                for( int k = 0; k < mm; k++ )
                    result[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
        return result;
    }

    /***
     * 
     * @param firstMatrix = X1
     * @param secondMatrix = X2
     * @return resulted matrix = X1' * X2, where X1' is transpose of X1
     */
    public static double[][] getProductOfRectangularTransposedMatrixAndMatrix(double[][] firstMatrix, double[][] secondMatrix)
    {
        int n = firstMatrix.length , m = firstMatrix[0].length, k = secondMatrix[0].length;
        double[][] result = new double[m][k];
        for( int i = 0; i < m; i++ )
            for( int j = 0; j < k; j++ )
                for( int ii = 0; ii < n; ii++ )
                    result[i][j] += firstMatrix[ii][i] * secondMatrix[ii][j];
        return result;
    }

    //// it is copied
    public static double getInnerProduct(double[] vector1, double[] vector2)
    {
        return DoubleStreamEx.zip( vector1, vector2, (a, b) -> ( a * b ) ).sum();
    }

    //// it is copied
    public static double[] getProductOfRectangularMatrixAndVector(double[][] matrix, double[] vector)
    {
        int n = matrix.length;
        if( matrix[0].length != vector.length ) throw new IllegalArgumentException("Wrong vector dimension: can not multiply matrix " + n + "x" + matrix[0].length + " by vector " + vector.length);
        return StreamEx.of( matrix ).mapToDouble( row -> getInnerProduct( row, vector ) ).toArray();
    }
    
    // it is copied
    public static double[] getProductOfTransposedVectorAndRectangularMatrix(double[][] matrix, double[] vector)
    {
        int n = matrix.length, m = matrix[0].length;
        double[] result = new double[m];
        for( int j = 0; j < m; j++ )
            for( int i = 0; i < n; i++ )
                result[j] += vector[i] * matrix[i][j];
        return result;
    }

    /// it is copied
    public static double[] getProductOfTransposedMatrixAndVector(double[][] matrix, double[] vector)
    {
        int n = matrix.length, m = matrix[0].length;
        if( n != vector.length ) throw new IllegalArgumentException("Wrong vector dimension: can not multiply matrix " + m + "x" + n + " by vector " + vector.length);
        double[] result = new double[m];
        for( int i = 0; i < m; i++ )
            for( int j = 0; j < n; j++ )
                result[i] += matrix[j][i] * vector[j];
        return result;
    }

    // it is copied
    /***
     * @param symmetricMatrix[][] - lower triangular part of symmetric matrix
     * @param vector
     * @return product X' * S * X, where X - vector, S - symmetric matrix;
     */
    public static double getProductOfTransposedVectorAndSymmetricMatrixAndVector(double[][] symmetricMatrix, double[] vector)
    {
        double result = 0;
        int n = vector.length;
        for( int i = 1; i < n; i++ )
            for( int j = 0; j < i; j++ )
                result += vector[i] * vector[j] * symmetricMatrix[i][j];
        result *= 2.0;
        for( int i = 0; i < n; i++ )
            result += vector[i] * vector[i] * symmetricMatrix[i][i];
        return result;
    }

    // it is copied
    /***
     * 
     * @param matrix - is lower triangular part of symmetric matrix
     * @param vector
     * @return
     */
    public static @Nonnull double[] getProductOfSymmetricMatrixAndVector(double[][] matrix, double[] vector)
    {
        int n = matrix.length;
        if( n != vector.length )
            throw new IllegalArgumentException("Wrong vector dimension: can not multiply matrix " + n + "x" + n + " by vector "
                    + vector.length);
        double[] result = new double[n];
        for( int i = 0; i < n; i++ )
        {
            int j = 0;
            for( ; j <= i; j++ )
                result[i] += matrix[i][j] * vector[j];
            for( ; j < n; j++ )
                result[i] += matrix[j][i] * vector[j];
        }
        return result;
    }
    
    // it is copied
    public static double[] getProductOfDiagonalMatrixAndVector(double[] diagonal, double[] vector)
    {
        double[] result = new double[diagonal.length];
        for( int i = 0; i < diagonal.length; i++ )
            result[i] = diagonal[i] * vector[i];
        return result;
    }

    ///// it is copied !!!
    /***
     * 
     * @param matrix
     * @return lower triangular part of symmetric matrix Y = X'X, where X is input matrix
     */
    public static double[][] getProductOfTransposedMatrixAndMatrix(double[][] matrix)
    {
        int n = matrix.length, m = matrix[0].length;
        double[][] result = getLowerTriangularMatrix(m);
        for( int i = 0; i < m; i++ )
            for( int j = 0; j <= i; j++ )
            {
                for( int k = 0; k < n; k++ )
                    result[i][j] += matrix[k][i] * matrix[k][j];
            }
        return result;
    }

    //////////// it is copied !!!
    public static @Nonnull double[][] getLowerTriangularMatrix(int n)
    {
        return IntStreamEx.rangeClosed( 1, n ).mapToObj( double[]::new ).toArray( double[][]::new );
    }
    
    public static @Nonnull int[][] getLowerTriangularIntegerMatrix(int n)
    {
        return IntStreamEx.rangeClosed( 1, n ).mapToObj( int[]::new ).toArray( int[][]::new );
    }
    
    // it is copied
    public static double[][] getLowerTriangularMatrix(double[][] squareMatrix)
    {
        double[][] result = new double[squareMatrix.length][];
        for( int i = 0; i < squareMatrix.length; i++ )
        {
            double[] vector = new double[i + 1];
            for( int j = 0; j <= i; j++ )
                vector[j] = squareMatrix[i][j];
            result[i] = vector;
        }
        return result;
    }

    public static double[][] getTransposedMatrix(double[][] matrix)
    {
        int n = matrix.length, m = matrix[0].length;
        double[][] result = new double[m][n];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < m; j++ )
                result[j][i] = matrix[i][j];
        return result;
    }

    public static @Nonnull double[][] getProductOfRectangularAndSymmetricMatrices(double[][] rectangularMatrix, double[][] symmetricMatrix)
    {
        int n = rectangularMatrix.length, m = symmetricMatrix.length;
        if( rectangularMatrix[0].length != m )
            throw new IllegalArgumentException("Wrong matrix dimensions: can not multiply " + n + "x" + rectangularMatrix[0].length
                    + " and " + m + "x" + m + " matrixes");
        double[][] result = new double[n][m];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < m; j++ )
            {
                int k = 0;
                for( ; k <= j; k++ )
                    result[i][j] += rectangularMatrix[i][k] * symmetricMatrix[j][k];
                for( ; k < m; k++ )
                    result[i][j] += rectangularMatrix[i][k] * symmetricMatrix[k][j];
            }
        return result;
    }

    // it is copied
    public static @Nonnull double[][] getProductOfSymmetricAndRectangularMatrices(double[][] symmetricMatrix, double[][] rectangularMatrix)
    {
        int n = symmetricMatrix.length, m = rectangularMatrix[0].length;
        if( rectangularMatrix.length != n )
            throw new IllegalArgumentException("Wrong matrix dimensions: can not multiply " + n + "x" + n + " and "
                    + rectangularMatrix.length + "x" + m + " matrixes");
        double[][] result = new double[n][m];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < m; j++ )
            {
                int k = 0;
                for( ; k <= i; k++ )
                    result[i][j] += symmetricMatrix[i][k] * rectangularMatrix[k][j];
                for( ; k < n; k++ )
                    result[i][j] += symmetricMatrix[k][i] * rectangularMatrix[k][j];
            }
        return result;
    }
    
    // it is copied improved version
    public static double[][] getProductOfSymmetricMatrices(double[][] symmetricMatrix1, double[][] symmetricMatrix2)
    {
        double[][] squareMatrix1 = transformSymmetricMatrixToSquareMatrix(symmetricMatrix1);
        double[][] squareMatrix2 = transformSymmetricMatrixToSquareMatrix(symmetricMatrix2);
        return getProductOfRectangularMatrices(squareMatrix1, squareMatrix2);
    }

    //// It is copied
    /***
     * 
     * @param n - number of columns and rows in identical matrix
     * @return identical matrix X : X[i,i]=1; X[i,j]=0, if i!=j
     */
    public static double[][] getIdenticalMatrix(int n)
    {
        double[][] result = new double[n][n];
        for( int i = 0; i < n; i++ )
            result[i][i] = 1.0;
        return result;
    }
    // it is copied
    public static double[] getConstantVector(int dimension, double scalar)
    {
        double[] result = new double[dimension];
        Arrays.fill(result, scalar);
        return result;
    }

    ///// it is copied
    public static double[] getDiagonalOfMatrix(double[][] matrix)
    {
        return IntStreamEx.ofIndices( matrix ).mapToDouble( i -> matrix[i][i] ).toArray();
    }

    /// it is copied
    private static boolean isProductPositive(double x, double y)
    {
        boolean result = true;
        if( ( x < 0.0 && y > 0.0 ) || ( x > 0.0 && y < 0.0 ) )
            result = false;
        return result;
    }

    // it is copied
    public static double[][] getClone(double[][] matrix)
    {
        return Util.copy(matrix);
    }

    // It is copied
    /***
     * 
     * @param squareMatrix
     * @param maxNumberOfIterations
     * @param eps
     * @return Object[] array : array[0] = (double) maximal  eigen value of matrix; array[1] = double[] corresponding eigen vector;
     */
    public static Object[] getMaximalEigenValueOfSquareMatrixByLyusternikMethod(double[][] squareMatrix, int maxNumberOfIterations, double eps)
    {
        double[] eigenVector = getConstantVector(squareMatrix.length, 1.0);
        double oldEigenValue = Norm.getEuclidean(eigenVector), eigenValue = oldEigenValue;
        eigenVector = getProductOfVectorAndScalar(eigenVector, 1.0 / oldEigenValue);
        for( int i = 0; i < maxNumberOfIterations; i++ )
        {
            eigenVector = getProductOfRectangularMatrixAndVector(squareMatrix, eigenVector);
            eigenValue = Norm.getEuclidean(eigenVector);
            eigenVector = getProductOfVectorAndScalar(eigenVector, 1.0 / eigenValue);
            // if( Math.abs(oldEigenValue - eigenValue) < eps ) break;
            if( eigenValue == 0.0 || Math.abs(oldEigenValue - eigenValue) / eigenValue < eps ) break;
            oldEigenValue = eigenValue;
        }
        return new Object[]{eigenValue, eigenVector};
    }

    // it is copied
    /***
     * 
     * @param matrix - lower triangular part of symmetric matrix
     * @param maxNumberOfIterations - maximal number of iterations
     * @param eps  - if ( maximum of absolute values of off-diagonal elements of matrix ) < eps then iterative process will be terminated
     * @return  array Object[] objects : objects[0] = (int) number of processed iterations;
     *          objects[1] = (double[]) eigenvalues;
     *          objects[2] = (double[][]) eigenvectors (as columns of this array);
     */
    public static Object[] getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(double[][] matrix, int maxNumberOfIterations, double eps)
    {
        Object[] objects = new Object[3];
        double[][] mat = getClone(matrix);
        int n = matrix.length;
        double[][] eVectors = getIdenticalMatrix(n);
        objects[2] = eVectors;
        int iterationNumber = 0;
        for( ; iterationNumber < maxNumberOfIterations; iterationNumber++ )
        {
            // 1. Identification of off-diagonal element of matrix 'mat' with maximal abs-value
            int iMax = 1;
            int jMax = 0;
            double max = Math.abs(mat[iMax][jMax]);
            for( int i = 0; i < n; i++ )
                for( int j = 0; j < i; j++ )
                {
                    double x = Math.abs(mat[i][j]);
                    if( x > max )
                    {
                        iMax = i;
                        jMax = j;
                        max = x;
                    }
                }
            objects[0] = iterationNumber;
            objects[1] = getDiagonalOfMatrix(mat);
            if( max < eps ) break;

            // 2. Identification sin and cos
            double x = 2.0 * mat[iMax][jMax], y = mat[iMax][iMax] - mat[jMax][jMax], z = 0.5 * Math.abs(y) / Math.sqrt(x * x + y * y);
            double cos = Math.sqrt(0.5 + z), sin = Math.sqrt(0.5 - z);
            if( !isProductPositive(x, y) )
                sin = -sin;

            // 3. Recalculation of eVectors : eVectors := eVectors * Uij, where Uij is trivial rotation matrix
            for( int i = 0; i < n; i++ )
            {
                double new1 = cos * eVectors[i][iMax] + sin * eVectors[i][jMax];
                double new2 = cos * eVectors[i][jMax] - sin * eVectors[i][iMax];
                eVectors[i][iMax] = new1;
                eVectors[i][jMax] = new2;
            }

            // 4. Recalculation of matrix 'mat' : 'mat' := 'mat' * Uij, where Uij is trivial rotation matrix
            for( int i = 0; i < n; i++ )
                if( i != iMax && i != jMax )
                {
                    double u1 = i > iMax ? mat[i][iMax] : mat[iMax][i];
                    double u2 = i > jMax ? mat[i][jMax] : mat[jMax][i];
                    double uu1 = u1 * cos + u2 * sin;
                    double uu2 = u2 * cos - u1 * sin;
                    if( i > iMax )
                        mat[i][iMax] = uu1;
                    else
                        mat[iMax][i] = uu1;
                    if( i > jMax )
                        mat[i][jMax] = uu2;
                    else
                        mat[jMax][i] = uu2;
                }
            double cos2 = cos * cos, sin2 = sin * sin, sinCos = sin * cos;
            double u11 = mat[iMax][iMax] * cos2 + 2.0 * mat[iMax][jMax] * sinCos + mat[jMax][jMax] * sin2;
            double u22 = mat[iMax][iMax] * sin2 - 2.0 * mat[iMax][jMax] * sinCos + mat[jMax][jMax] * cos2;
            mat[iMax][iMax] = u11;
            mat[jMax][jMax] = u22;
            mat[iMax][jMax] = 0.0;
        }
        if( iterationNumber == maxNumberOfIterations )
            objects[1] = getDiagonalOfMatrix(mat);
        return objects;
    }

    //// it is copied
    public static double[] getInverseVector(double[] vector)
    {
        int n = vector.length;
        double[] result = new double[n];
        for( int i = 0; i < n; i++ )
        {
            if( vector[i] == 0.0 ) return null;
            result[i] = 1.0 / vector[i];
        }
        return result;
    }
    
    public static double[] getSqrtVector(double[] vector)
    {
        int n = vector.length;
        double[] result = new double[n];
        for( int i = 0; i < n; i++ )
        {
            if( vector[i] < 0.0 ) return null;
            result[i] = Math.sqrt(vector[i]);
        }
        return result;
    }

    ///////// it is moved
    /***
     * 
     * @param squaredMatrix
     * @param diagonal is diagonal part of diagonal matrix
     * @return lower triangular part of symmetric matrix X = A * B * A',
     *  where A is squared matrix, B is diagonal matrix and A' is the transposed A.
     */
    public static double[][] getProductOfSquaredAndDiagonalAndSquaredTransposedMatrices(double[][] squaredMatrix, double[] diagonal)
    {
        int n = squaredMatrix.length;
        double[][] result = getLowerTriangularMatrix(n);
        for( int i = 0; i < n; i++ )
            for( int j = 0; j <= i; j++ )
                for( int k = 0; k < n; k++ )
                    result[i][j] += diagonal[k] * squaredMatrix[i][k] * squaredMatrix[j][k];
        return result;
    }
    
    // it is copied
    /***
     * 
     * @param rectangularMatrix - the input rectangular matrix X;
     * @param diagonal - diagonal matrix;
     * @return lower triangular part of symmetric matrix that is the product X' * D * X where X is rectangular matrix and D is diagonal matrix
     */
    public static double[][] getProductOfRectangularTransposedAndDiagonalAndRectangularMatrices(double[][] rectangularMatrix, double[] diagonal)
    {
        int n = rectangularMatrix.length, m = rectangularMatrix[0].length;
        double[][] result = getLowerTriangularMatrix(m);
        for( int i = 0; i < m; i++ )
            for( int j = 0; j <= i; j++ )
                for( int ii = 0; ii < n; ii++ )
                    result[i][j] += rectangularMatrix[ii][i] * diagonal[ii] * rectangularMatrix[ii][j];
        return result;
    }
    
    // It is copied
    /***
     * 
     * @param rectangularMatrix - the rectangular matrix X;
     * @param diagonal - diagonal matrix D;
     * @return the resulted rectangular matrix - the product X' * D 
     */
    public static double[][] getProductOfRectangularTransposedAndDiagonalMatrices(double[][] rectangularMatrix, double[] diagonal)
    {
        int n = rectangularMatrix.length, m = rectangularMatrix[0].length;
        double[][] result = new double[m][n];
        for( int i = 0; i < m; i++ )
            for( int j = 0; j < n; j++ )
                result[i][j] = rectangularMatrix[j][i] * diagonal[j];
        return result;
    }
    
    // it is copied
    public static double getProductOfTransposedVectorAndDiagonalMatrixAndVector(double[] vector, double[] diagonal)
    {
        double result = 0.0;
        for( int i = 0; i < vector.length; i++ )
            result += vector[i] * vector[i] * diagonal[i];
        return result;
    }

    // it is copied
    /***
     * 
     * @param matrix - lower triangular part of symmetric matrix
     * @param maxNumberOfIterations - maximal number of iterations
     * @param eps - if ( maximum of absolute values of off-diagonal elements of matrix ) < eps then iterative process will be terminated
     * @return lower triangular part of inverse symmetric matrix
     */
    public static double[][] getInverseSymmetricMatrixByJacobiMethod(double[][] matrix, int maxNumberOfIterations, double eps)
    {
        if( matrix.length == 1 ) return new double[][]{{1.0 / matrix[0][0]}};
        Object[] objects = getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(matrix, maxNumberOfIterations, eps);
        double[] inverseEigenValues = getInverseVector((double[])objects[1]);
        if( inverseEigenValues == null ) return null;
        return getProductOfSquaredAndDiagonalAndSquaredTransposedMatrices((double[][])objects[2], inverseEigenValues);
    }
    
    /***
     * 
     * @param symmetricMatrix A
     * @param maxNumberOfIterations
     * @param eps
     * @return lower triangular part of symmetric matrix X = A**(-1/2), i.e X * X = inverse(A), where A is input symmetric matrix
     */
    public static double[][] getHalfInverseSymmetricMatrixByJacobiMethod(double[][] symmetricMatrix, int maxNumberOfIterations, double eps)
    {
        if( symmetricMatrix.length == 1 ) return new double[][]{{1.0 / Math.sqrt(symmetricMatrix[0][0])}};
        Object[] objects = getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(symmetricMatrix, maxNumberOfIterations, eps);
        double[] inverseEigenValues = getInverseVector((double[])objects[1]);
        if( inverseEigenValues == null ) return null;
        inverseEigenValues = getSqrtVector(inverseEigenValues);
        return getProductOfSquaredAndDiagonalAndSquaredTransposedMatrices((double[][])objects[2], inverseEigenValues);
    }

    // it is copied
    public static @Nonnull double[] getProductOfVectorAndScalar(double[] vector, double scalar)
    {
        return DoubleStreamEx.of( vector ).map( v -> v * scalar ).toArray();
    }

    // it is copied
    public static @Nonnull double[][] getProductOfMatrixAndScalar(double[][] matrix, double scalar)
    {
        return StreamEx.of( matrix ).map( vec -> getProductOfVectorAndScalar( vec, scalar ) ).toArray( double[][]::new );
    }

    // it is copied
    public static double[] getSubtractionOfVectors(double[] vector1, double[] vector2)
    {
        return DoubleStreamEx.zip( vector1, vector2, (a, b) -> (a - b)).toArray();
    }

    // it is copied
    public static double[] getSumOfVectors(double[] vector, double scalar)
    {
        return DoubleStreamEx.of( vector ).map( v -> v + scalar ).toArray();
    }
    
    public static int[] getSumOfVectors(int[] vector, int scalar)
    {
        return IntStreamEx.of( vector ).map( v -> v + scalar ).toArray();
    }

    // it is copied
    public static double[] getSumOfVectors(double[] vector1, double[] vector2)
    {
        return DoubleStreamEx.zip( vector1, vector2, Double::sum).toArray();
    }

    // it  is copied
    public static double[][] getSumOfMatrices(double[][] matrix1, double[][] matrix2)
    {
        return StreamEx.zip( matrix1, matrix2, MatrixUtils::getSumOfVectors ).toArray( double[][]::new );
    }
    
    // it is copied
    public static double[][] getSubtractionOfMatrices(double[][] matrix1, double[][] matrix2)
    {
        return StreamEx.zip(matrix1, matrix2, MatrixUtils::getSubtractionOfVectors).toArray(double[][]::new);
    }

    // it is copied
    /***
     * 
     * @param rectangularMatrix - the input rectangular matrix X;
     * @param means mean values for columns of X;
     * @return lower triangular part of symmetric matrix that is the product X'* H * X where X is input matrix, H = I - (1/n) * 1_ * 1_' is
     * the centring matrix, n is number of rows in matrix X, I is identity matrix, 1_ is unit vector
     */
    public static @Nonnull double[][] getProductXtrHX(@Nonnull double[][] rectangularMatrix, @Nonnull double[] means)
    {
        int n = rectangularMatrix.length, m = rectangularMatrix[0].length;
        double[][] result = getLowerTriangularMatrix(m);
        for( int i = 0; i < m; i++ )
            for( int j = 0; j <= i; j++ )
                for( int ii = 0; ii < n; ii++ )
                    result[i][j] += ( rectangularMatrix[ii][i] - means[i] ) * ( rectangularMatrix[ii][j] - means[j] );
        return result;
    }

    private static void sortByPosition(List<double[]> list, final int sortColumnNumber)
    {
        Collections.sort(list, Comparator.comparingDouble(arr -> arr[sortColumnNumber]));
    }

    /***
     * Sort rows of matrix by sorting values of vector
     * @param vector; dim(vector) = n;
     * @param matrix; dim(matrix) = n x m;
     */
    public static void sortByVectorValues(double[] vector, double[][] matrix)
    {
        int n = vector.length, m = matrix[0].length;
        List<double[]> list = new ArrayList<>();
        for( int i = 0; i < n; i++ )
        {
            double[] newRow = ArrayUtils.add(matrix[i].clone(), m, vector[i]);
            list.add(newRow);
        }
        sortByPosition(list, m);
        for( int i = 0; i < n; i++ )
        {
            double[] row = list.get(i);
            vector[i] = row[m];
            matrix[i] = ArrayUtils.remove(row, m);
        }
    }
    
    // it is copied
    public static double[] sortInAscendingOrder(double[] vector)
    {
        double[] result = new double[vector.length];
        int[] positions = Util.sortHeap(vector.clone());
        for( int i = 0; i < vector.length; i++ )
            result[i] = vector[positions[i]];
        return result;
    }
  
    // it is copied
    public static double[] getColumn(double[][] matrix, int columnIndex)
    {
        return StreamEx.of(matrix).mapToDouble( vec -> vec[columnIndex] ).toArray();
    }
    
    // it is copied
    public static String[] getColumn(String[][] matrix, int columnIndex)
    {
        String[] result = new String[matrix.length];
        for( int i = 0; i < result.length; i++ )
            result[i] = matrix[i][columnIndex];
        return result;
    }

    // it is copied
    public static void removeGivenColumn(double[][] matrix, int columnIndex)
    {
        for( int i = 0; i < matrix.length; i++ )
            matrix[i] = ArrayUtils.remove(matrix[i], columnIndex);
    }

    /***
     * 
     * @param matrix
     * @param namesOfColumns
     * @param nameOfColumnToRemove
     * @return new array with names of columns; the column of matrix is also removed
     */
    public static String[] removeGivenColumn(double[][] matrix, String[] namesOfColumns, String nameOfColumnToRemove)
    {
        int index = ArrayUtils.indexOf(namesOfColumns, nameOfColumnToRemove);
        if( index < 0 ) return namesOfColumns;
        removeGivenColumn(matrix, index);
        return (String[])ArrayUtils.remove(namesOfColumns, index);
    }
    
//    public static double[] getVectorWithEqualElements(int dimention, double element)
//    {
//        return DoubleStreamEx.constant( element, dimention ).toArray();
//    }

    ///////////////// old version
//    public static double[][] getMatrixWithEqualElements(int numberOfRows, int numberOfColumns, double element)
//    {
//        return IntStreamEx.range( numberOfRows ).mapToObj( i -> getVectorWithEqualElements( numberOfColumns, element ) )
//                .toArray( double[][]::new );
//    }
    
    /////// new version
    public static double[][] getMatrixWithEqualElements(int numberOfRows, int numberOfColumns, double element)
    {
        return IntStreamEx.range(numberOfRows).mapToObj(i -> getConstantVector(numberOfColumns, element)).toArray(double[][]::new);
    }


    // it is copied
    public static double[][] transformSymmetricMatrixToSquareMatrix(double[][] symmetricMatrix)
    {
        int n = symmetricMatrix.length;
        double[][] result = new double[n][n];
        for( int i = 0; i < n; i++ )
            result[i][i] = symmetricMatrix[i][i];
        for( int i = 1; i < n; i++ )
            for( int j = 0; j < i; j++ )
                result[i][j] = result[j][i] = symmetricMatrix[i][j];
        return result;
    }
    
    public static double[][] transformVectorToMatrixWithSingleColumn(double[] vector)
    {
        double[][] result = new double[vector.length][1];
        for( int i = 0; i < vector.length; i++ )
            result[i][0] = vector[i];
        return result;
    }

    // it is copied
    public static void addColumnToMatrix(double[][] matrix, double[] newColumn)
    {
        addColumnToMatrix(matrix, newColumn, matrix[0].length);
    }

    // it is copied
    public static void addColumnToMatrix(double[][] matrix, double[] newColumn, int newColumnIndex)
    {
        for( int i = 0; i < matrix.length; i++ )
            matrix[i] = ArrayUtils.add(matrix[i], newColumnIndex, newColumn[i]);
    }
    
    public static double[][] getSubmatrixRowWise(double[][] matrix, int firstRowIndex, int lastRowIndex)
    {
        return StreamEx.of(matrix).limit( lastRowIndex+1 ).skip( firstRowIndex ).toArray( double[][]::new );
    }
    
    public static double[][] getSubmatrixRowWise(String[] rowNamesInMatrix, double[][] matrix, String[] rowNamesInSubmatrix)
    {
        double[][] result = new double[rowNamesInSubmatrix.length][];
        for( int i = 0; i < rowNamesInSubmatrix.length; i++ )
        {
            int index = ArrayUtils.indexOf(rowNamesInMatrix, rowNamesInSubmatrix[i]);
            if( index < 0 ) return null;
            result[i] = matrix[index];
        }
        return result;
    }
    
    ///// it is copied
    /***
     * 
     * @param vector
     * @return array objects: objects[0] = (double) maximal value; objects[1] = (int) index of maximal value;
     */
    public static Object[] getMaximalValue(double[] vector)
    {
        int maxIndex = IntStreamEx.ofIndices(vector).maxByDouble(idx -> vector[idx]).getAsInt();
        return new Object[]{vector[maxIndex], maxIndex};
    }

    //// it is copied
    /***
     * 
     * @param vector
     * @return array objects: objects[0] = (double) minimal value; objects[1] = (int) index of minimal value;
     */
    public static Object[] getMinimalValue(double[] vector)
    {
        int minIndex = IntStreamEx.ofIndices(vector).minByDouble(idx -> vector[idx]).getAsInt();
        return new Object[]{vector[minIndex], minIndex};
    }
    
    /***
     * 
     * @param vector
     * @return array objects: objects[0] = maximal value; objects[1] = index of maximal value;
     */
    public static Object[] getMaximalValue(int[] vector)
    {
        int maxIndex = IntStreamEx.ofIndices( vector ).maxByInt( idx -> vector[idx] ).getAsInt();
        return new Object[] {vector[maxIndex], maxIndex};
    }
    
    // it is copied
    public static boolean doContainNaN(double[] vector)
    {
        for( double element : vector )
            if( Double.isNaN(element) ) return true;
        return false;
    }
    
    /***
     * remove all rows that contain missing data (NaN-values);
     * @param rowNames
     * @param matrix
     * @return Object[] array; array[0] = String[] newRowNames; array[1] = double[][] newMatrix;
     */
    public static Object[] removeRowsWithMissingData(String[] rowNames, double[][] matrix)
    {
        List<String> newRowNames = new ArrayList<>();
        List<double[]> newMatrix = new ArrayList<>();
        for( int i = 0; i < rowNames.length; i++ )
            if( ! doContainNaN(matrix[i]) )
            {
                newRowNames.add(rowNames[i]);
                newMatrix.add(matrix[i]);
            }
        if( newRowNames.isEmpty() ) return null;
        return new Object[]{newRowNames.toArray(new String[0]), newMatrix.toArray(new double[newMatrix.size()][])};
    }
    
    public static Object[] removeObjectsWithMissingData(String[] rowNames, double[] vector)
    {
        List<String> newRowNames = new ArrayList<>();
        List<Double> newVector = new ArrayList<>();
        for( int i = 0; i < rowNames.length; i++ )
            if( rowNames[i] != null && !rowNames[i].isEmpty() && !Double.isNaN( vector[i] ) )
            {
                newRowNames.add(rowNames[i]);
                newVector.add(vector[i]);
            }
        if( newRowNames.isEmpty() ) return null;
        return new Object[]{newRowNames.toArray(new String[0]), fromListToArray(newVector)};
    }

    
    public static double[] removeMissingData(double[] vector)
    {
        List<Double> newVector = new ArrayList<>();
        for( double x : vector )
            if( ! Double.isNaN(x) )
                newVector.add(x);
        return fromListToArray(newVector);
    }

    //// it is copied
    public static double[] fromListToArray(List<Double> data)
    {
        return StreamEx.of(data).mapToDouble(Double::doubleValue).toArray();
    }
    
    public static int[] fromIntegerListToArray(List<Integer> data)
    {
        return StreamEx.of(data).mapToInt(Integer::intValue).toArray();
    }
    
    /***
     * 
     * @param rowOrColumnNames
     * @param squareSymmetricMatrix
     * @return array objects: objects[0] - new names of  rows and/or columns of rearranged square symmetric matrix; objects[1] - rearranged square symmetric matrix;
     */
    public static Object[] rearrangeSquareSymmetricMatrix(String[] rowOrColumnNames, double[][] squareSymmetricMatrix)
    {
        double[][] rearrangedMatrix = new double[squareSymmetricMatrix.length][squareSymmetricMatrix.length];
        String[] newNames = (String[])ArrayUtils.clone(rowOrColumnNames);
        Arrays.sort(newNames, String.CASE_INSENSITIVE_ORDER);
        for( int i = 0; i < newNames.length; i++ )
        {
            int indexI = ArrayUtils.indexOf(rowOrColumnNames, newNames[i]);
            for( int j = 0; j <= i; j++ )
            {
                int indexJ = ArrayUtils.indexOf(rowOrColumnNames, newNames[j]);
                rearrangedMatrix[i][j] = rearrangedMatrix[j][i] = squareSymmetricMatrix[indexI][indexJ];
            }
        }
        return new Object[]{newNames, rearrangedMatrix};
    }
    
    //// it is copied
    public static boolean equals(String[] vector1, String[] vector2)
    {
        if( vector1.length != vector2.length ) return false;
        for( int i = 0; i < vector1.length; i++ )
            if( ! vector1[i].equals(vector2[i]) ) return false;
        return true;
    }

    // it is copied
    /****************** Norm : begin  *******************************/
    public static class Norm
    {
        public static double getEuclidean(double[] vector)
        {
            double result = 0.0;
            for( double x : vector )
                result += x * x;
            return Math.sqrt(result);
        }
    }
    /****************** Norm : end  ******************************/
    
    ////////////////////////////  it is copied !!!!!!!!!!!!!!!!
    /****************** Distance : begin  ******************************/
    public static class Distance
    {
        public static final String EUCLIDEAN = "Euclidean distance";
        public static final String EUCLIDEAN_SQUARED = "Euclidean squared distance";
        public static final String MANHATTAN = "Manhattan distance";
        
        public static String[] getAvailableDistanceTypes()
        {
            return new String[]{EUCLIDEAN, EUCLIDEAN_SQUARED, MANHATTAN};
        }

        public static double getEuclidean(double[] vector1, double[] vector2)
        {
            return Math.sqrt(getEuclideanSquared(vector1, vector2));
        }
        
        public static double getEuclideanSquared(double[] vector1, double[] vector2)
        {
            return DoubleStreamEx.zip( vector1, vector2, (a, b) -> a - b).map( x -> x * x ).sum();
        }
        
        public static double getManhattan(double[] vector1, double[] vector2)
        {
            double result = 0.0;
            for( int i = 0; i < vector1.length; i++ )
                result += Math.abs(vector1[i] - vector2[i]);
            return result;
        }
        
        public static double getDistance(String distanceType, double[] vector1, double[] vector2) throws Exception
        {
            switch( distanceType )
            {
                case EUCLIDEAN         : return getEuclidean(vector1, vector2);
                case EUCLIDEAN_SQUARED : return getEuclideanSquared(vector1, vector2);
                case MANHATTAN         : return getManhattan(vector1, vector2);
                default                : throw new Exception("This distance type '" + distanceType + "' is not supported in our analysis currently");
            }
        }
    }
    /****************** Distance : end  ******************************/
    
  //temp
    private static Logger log = Logger.getLogger(MatrixUtils.class.getName());
}
