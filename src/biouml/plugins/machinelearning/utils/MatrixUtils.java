/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.Norm;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorTransformation;
import ru.biosoft.analysis.Util;

/**
 * @author yura
 *
 */
public class MatrixUtils
{
    public static final int DEFAULT_MAX_NUMBER_OF_ROTATIONS = 10000;
    public static final double DEFAULT_EPS_FOR_ROTATIONS = 1.0E-5;
    public static final int DEFAULT_MAX_NUMBER_OF_ITERATIONS_IN_LYUSTERNIK_METHOD = 10000;
    public static final double DEFAULT_EPS_IN_LYUSTERNIK_METHOD = 1.0E-5;
    
    public static double[] getProductOfRectangularMatrixAndVector(double[][] matrix, double[] vector)
    {
        double[] result = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
        result[i] = VectorOperations.getInnerProduct(matrix[i], vector);
        return result;
    }
    
    public static double getProductOfTransposedVectorAndDiagonalMatrixAndVector(double[] vector, double[] diagonal)
    {
        double result = 0.0;
        for( int i = 0; i < vector.length; i++ )
            result += vector[i] * vector[i] * diagonal[i];
        return result;
    }
    
    public static double[][] getSubtractionOfMatrices(double[][] matrix1, double[][] matrix2)
    {
        double[][] result = new double[matrix1.length][];
        for( int i = 0; i < matrix1.length; i++ )
            result[i] = VectorOperations.getSubtractionOfVectors(matrix1[i], matrix2[i]);
        return result;
    }
    
    public static double[][] getSumOfMatrices(double[][] matrix1, double[][] matrix2)
    {
        double[][] result = new double[matrix1.length][];
        for( int i = 0; i < matrix1.length; i++ )
            result[i] = VectorOperations.getSumOfVectors(matrix1[i], matrix2[i]);
        return result;
    }
    
    public static double[][] getSumOfMatrices(double[][] squaredMatrix, double[] diagonalMatrix)
    {
        double[][] result = getClone(squaredMatrix);
        for( int i = 0; i < squaredMatrix.length; i++ )
            result[i][i] += diagonalMatrix[i];
        return result;
    }
    
    public static double[] getSumsOfRows(double[][] matrix)
    {
        double[] result = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
            result[i] = PrimitiveOperations.getSum(matrix[i]);
        return result;
    }
    
    public static double[] getSumsOfColumns(double[][] matrix)
    {
        int n = matrix.length, m = matrix[0].length;
        double result[] = new double[m];
        for( int j = 0; j < m; j++ )
            for( int i = 0; i < n; i++ )
                result[j] += matrix[i][j];
        return result;
    }

    public static double[][] getProductOfMatrixAndScalar(double[][] matrix, double scalar)
    {
        double[][] result = new double[matrix.length][];
        for( int i = 0; i < matrix.length; i++ )
            result[i] = VectorOperations.getProductOfVectorAndScalar(matrix[i], scalar);
        return result;
    }
    
    /***
     * 
     * @param matrix
     * @return lower triangular part of symmetric matrix Y = X'X, where X is input matrix
     */
    public static double[][] getProductOfTransposedMatrixAndMatrix(double[][] matrix)
    {
        int m = matrix[0].length;
        double[][] result = getLowerTriangularMatrix(m);
        for( int i = 0; i < m; i++ )
            for( int j = 0; j <= i; j++ )
                result[i][j] = getProductOfTwoColumns(matrix, i,  matrix, j);
        return result;
    }
    
    /***
     * 
     * @param matrix - is lower triangular part of symmetric matrix
     * @param vector
     * @return
     */
    public static double[] getProductOfSymmetricMatrixAndVector(double[][] matrix, double[] vector)
    {
        double[] result = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
        {
            int j = 0;
            for( ; j <= i; j++ )
                result[i] += matrix[i][j] * vector[j];
            for( ; j < matrix.length; j++ )
                result[i] += matrix[j][i] * vector[j];
        }
        return result;
    }
    
    public static double[][] getProductOfSymmetricAndRectangularMatrices(double[][] symmetricMatrix, double[][] rectangularMatrix)
    {
        int n = symmetricMatrix.length, m = rectangularMatrix[0].length;
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
    
    public static double[][] getProductOfSymmetricMatrices(double[][] symmetricMatrix1, double[][] symmetricMatrix2)
    {
        double[][] result = new double[symmetricMatrix1.length][symmetricMatrix1.length];
        for( int i = 0; i < symmetricMatrix1.length; i++ )
            for( int j = 0; j < symmetricMatrix1.length; j++ )
                for( int k = 0; k < symmetricMatrix1.length; k++ )
                {
                    double x1 = i >= k ? symmetricMatrix1[i][k] : symmetricMatrix1[k][i];
                    double x2 = k >= j ? symmetricMatrix2[k][j] : symmetricMatrix2[j][k];
                    result[i][j] += x1 * x2;
                }
        return result;
    }
    
    public static double[] getProductOfTransposedMatrixAndVector(double[][] matrix, double[] vector)
    {
        int m = matrix[0].length;
        double[] result = new double[m];
        for( int i = 0; i < m; i++ )
            result[i] = getProductOfColumnAndVector(matrix, i, vector);
        return result;
    }
    
    public static double getProductOfColumnAndVector(double[][] matrix, int columnIndex, double[] vector)
    {
        double result = 0.0;
        for( int i = 0; i < vector.length; i++ )
            result += vector[i] * matrix[i][columnIndex];
        return result;
    }
    
    /***
     * 
     * @param squaredMatrix
     * @param diagonal is diagonal part of diagonal matrix
     * @return lower triangular part of symmetric matrix X = A * B * A',
     *  where A is squared matrix, B is diagonal matrix and A' is the transposed A.
     */
    public static double[][] getProductOfSquaredAndDiagonalAndSquaredTransposedMatrices(double[][] squaredMatrix, double[] diagonal)
    {
        double[][] result = getLowerTriangularMatrix(squaredMatrix.length);
        for( int i = 0; i < squaredMatrix.length; i++ )
            for( int j = 0; j <= i; j++ )
                for( int k = 0; k < squaredMatrix.length; k++ )
                    result[i][j] += diagonal[k] * squaredMatrix[i][k] * squaredMatrix[j][k];
        return result;
    }
    
    public static double[] getProductOfTransposedVectorAndRectangularMatrix(double[][] matrix, double[] vector)
    {
        int n = matrix.length, m = matrix[0].length;
        double[] result = new double[m];
        for( int j = 0; j < m; j++ )
            for( int i = 0; i < n; i++ )
                result[j] += vector[i] * matrix[i][j];
        return result;
    }
    
    public static double[] getProductOfDiagonalMatrixAndVector(double[] diagonal, double[] vector)
    {
        double[] result = new double[diagonal.length];
        for( int i = 0; i < diagonal.length; i++ )
            result[i] = diagonal[i] * vector[i];
        return result;
    }
    
    public static double[][] getProductOfRectangularMatrices(double[][] firstMatrix, double[][] secondMatrix)
    {
        int n = firstMatrix.length , m = secondMatrix[0].length;
        double[][] result = new double[n][m];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < m; j++ )
                result[i][j] = getProductOfColumnAndVector(secondMatrix, j, firstMatrix[i]);
        return result;
    }
    
    /***
     * !!! the output matrix often is called as SSP matrix (SSP is Sum of Squares and Products) !!!
     * 
     * @param rectangularMatrix - the input rectangular matrix X;
     * @param means mean values for columns of X;
     * @return lower triangular part of symmetric matrix that is the product X'* H * X
     *         where X is input matrix, H = I - (1/n) * 1_ * 1_' is the centring matrix,
     *         n is number of rows in matrix X, I is identity matrix, 1_ is unit vector.
     */
    public static double[][] getProductXtrHX(double[][] rectangularMatrix, double[] means)
    {
        int n = rectangularMatrix.length, m = rectangularMatrix[0].length;
        double[][] result = getLowerTriangularMatrix(m);
        for( int i = 0; i < m; i++ )
            for( int j = 0; j <= i; j++ )
                for( int ii = 0; ii < n; ii++ )
                    result[i][j] += (rectangularMatrix[ii][i] - means[i]) * (rectangularMatrix[ii][j] - means[j]);
        return result;
    }
    
    /***
     * 
     * @param rectangularMatrix - the input rectangular matrix X;
     * @param diagonal - diagonal matrix;
     * @return lower triangular part of symmetric matrix that is the product X' * D * X where X is rectangular matrix and D is diagonal matrix
     */
    public static double[][] getProductOfTransposedRectangularAndDiagonalAndRectangularMatrices(double[][] rectangularMatrix, double[] diagonal)
    {
        int n = rectangularMatrix.length, m = rectangularMatrix[0].length;
        double[][] result = getLowerTriangularMatrix(m);
        for( int i = 0; i < m; i++ )
            for( int j = 0; j <= i; j++ )
                for( int ii = 0; ii < n; ii++ )
                    result[i][j] += rectangularMatrix[ii][i] * diagonal[ii] * rectangularMatrix[ii][j];
        return result;
    }
    
    /***
     * 
     * @param rectangularMatrix - the rectangular matrix X;
     * @param diagonal - diagonal matrix D;
     * @return the resulted rectangular matrix - the product X' * D 
     */
    public static double[][] getProductOfTransposedRectangularAndDiagonalMatrices(double[][] rectangularMatrix, double[] diagonal)
    {
        int n = rectangularMatrix.length, m = rectangularMatrix[0].length;
        double[][] result = new double[m][n];
        for( int i = 0; i < m; i++ )
            for( int j = 0; j < n; j++ )
                result[i][j] = rectangularMatrix[j][i] * diagonal[j];
        return result;
    }
    
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
    
    public static double[][] getLowerTriangularMatrix(int n)
    {
        double result[][] = new double[n][];
        for( int i = 0; i < n; i++ )
             result[i] = new double[i + 1];
         return result;
    }
    
    public static double[][] getLowerTriangularMatrix(double[][] squareMatrix)
    {
        double[][] result = new double[squareMatrix.length][];
        for( int i = 0; i < squareMatrix.length; i++ )
            result[i] = UtilsForArray.copySubarray(squareMatrix[i], 0, i + 1);
        return result;
    }
    
    public static double getProductOfTwoColumns(double[][] matrix1, int columnIndex1,  double[][] matrix2, int columnIndex2)
    {
        double result = 0.0;
        for( int i = 0; i < matrix1.length; i++ )
            result += matrix1[i][columnIndex1] * matrix2[i][columnIndex2];
        return result;
    }

    public static String[] getColumn(String[][] matrix, int columnIndex)
    {
        String[] result = new String[matrix.length];
        for( int i = 0; i < result.length; i++ )
            result[i] = matrix[i][columnIndex];
        return result;
    }
    
    public static double[] getColumn(double[][] matrix, int columnIndex)
    {
        double[] result = new double[matrix.length];
        for( int i = 0; i < result.length; i++ )
            result[i] = matrix[i][columnIndex];
        return result;
    }
    
    public static int[] getColumn(int[][] matrix, int columnIndex)
    {
        int[] result = new int[matrix.length];
        for( int i = 0; i < result.length; i++ )
            result[i] = matrix[i][columnIndex];
        return result;
    }
    
    public static double[][] getSubMatrixColumnWise(int[] columnIndices, double[][] matrix)
    {
        double[][] result = new double[matrix.length][columnIndices.length];
        for( int i = 0; i < matrix.length; i++ )
            for( int j = 0; j < columnIndices.length; j++ )
                result[i][j] = matrix[i][columnIndices[j]];
        return result;
    }

    public static String[][] transformVectorToMatrixWithSingleColumn(String[] vector)
    {
        String[][] result = new String[vector.length][1];
        for( int i = 0; i < vector.length; i++ )
            result[i][0] = vector[i];
        return result;
    }
    
    public static double[][] transformVectorToMatrixWithSingleColumn(double[] vector)
    {
        double[][] result = new double[vector.length][1];
        for( int i = 0; i < vector.length; i++ )
            result[i][0] = vector[i];
        return result;
    }
    
    public static int[][] transformVectorToMatrixWithSingleColumn(int[] vector)
    {
        int[][] result = new int[vector.length][1];
        for( int i = 0; i < vector.length; i++ )
            result[i][0] = vector[i];
        return result;
    }
    
    public static double[][] transformVectorToMatrixWithSingleRow(double[] vector)
    {
        return new double[][]{vector};
    }
    
    public static String[][] transformVectorToMatrixWithSingleRow(String[] vector)
    {
        return new String[][]{vector};
    }
    
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
   
    /***
     * 
     * @param matrix - lower triangular part of symmetric matrix
     * @param maxNumberOfRotations - maximal number of rotations
     * @param eps - if ( maximum of absolute values of off-diagonal elements of matrix ) < eps then iterative process will be terminated
     * @return Objects[] objects: objects[0] = number of processed rotations;
     *                            objects[1] = double[][] lower triangular part of inverse symmetric matrix
     */
    public static Object[] getInverseSymmetricMatrixByJacobiMethod(double[][] matrix, int maxNumberOfRotations, double eps)
    {
        if( matrix.length == 1 ) return new double[][]{{1.0 / matrix[0][0]}};
        Object[] objects = getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(matrix, maxNumberOfRotations, eps);
        double[] inverseEigenValues = VectorOperations.getInverseVector((double[])objects[1]);
        if( inverseEigenValues == null ) return null;
        return new Object[]{objects[0], getProductOfSquaredAndDiagonalAndSquaredTransposedMatrices((double[][])objects[2], inverseEigenValues)};
    }
    
    public static Object[] getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(double[][] matrix, int maxNumberOfRotations, double eps)
    {
        int n = matrix.length;
        double[][] mat = getClone(matrix), eVectors = getIdenticalMatrix(n);
        int rotationNumber = 1;
        for( ; rotationNumber < maxNumberOfRotations; rotationNumber++ )
        {
            // 1. Identification of off-diagonal element of matrix 'mat' with maximal abs-value
            int iMax = 1, jMax = 0;
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
            if( max < eps ) return new Object[]{rotationNumber, getDiagonalOfMatrix(mat), eVectors};

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
        return new Object[]{maxNumberOfRotations, getDiagonalOfMatrix(mat), eVectors};
    }
    
    // TODO: To move it into appropriate Class
    private static boolean isProductPositive(double x, double y)
    {
        if( (x < 0.0 && y > 0.0) || (x > 0.0 && y < 0.0) ) return false;
        return true;
    }

    /***
     * 
     * @param squareMatrix
     * @param maxNumberOfIterations
     * @param eps
     * @return Object[] array : array[0] = (double) maximal  eigen value of matrix;
     *                          array[1] = double[] corresponding eigen vector;
     *                          array[2] = int number of processed iterations;
     */
    public static Object[] getMaximalEigenValueOfSquareMatrixByLyusternikMethod(double[][] squareMatrix, int maxNumberOfIterations, double eps)
    {
        double[] eigenVector = UtilsForArray.getConstantArray(squareMatrix.length, 1.0);
        double oldEigenValue = Norm.getEuclideanL2(eigenVector), eigenValue = oldEigenValue;
        eigenVector = VectorOperations.getProductOfVectorAndScalar(eigenVector, 1.0 / oldEigenValue);
        for( int i = 0; i < maxNumberOfIterations; i++ )
        {
            eigenVector = getProductOfRectangularMatrixAndVector(squareMatrix, eigenVector);
            eigenValue = Norm.getEuclideanL2(eigenVector);
            eigenVector = VectorOperations.getProductOfVectorAndScalar(eigenVector, 1.0 / eigenValue);
            if( eigenValue == 0.0 || Math.abs(oldEigenValue - eigenValue) / eigenValue < eps ) return new Object[]{eigenValue, eigenVector, i + 1};
            oldEigenValue = eigenValue;
        }
        return new Object[]{eigenValue, eigenVector, maxNumberOfIterations};
    }
    
    /***
     * 
     * @param n - number of columns and rows in identical matrix
     * @return identical matrix X : X[i,i] = 1; X[i,j] = 0, if i != j
     */
    public static double[][] getIdenticalMatrix(int n)
    {
        double[][] result = new double[n][n];
        for( int i = 0; i < n; i++ )
            result[i][i] = 1.0;
        return result;
    }
    
    public static double[][] getClone(double[][] matrix)
    {
        return Util.copy(matrix);
    }
    
    public static double[] getDiagonalOfMatrix(double[][] matrix)
    {
        double[] result = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
            result[i] = matrix[i][i];
        return result;
    }
    
    public static double[][] getSubMatrixRowWise(int[] rowIndices, double[][] matrix)
    {
        double[][] result = new double[rowIndices.length][];
        for( int i = 0; i < rowIndices.length; i++ )
            result[i] = matrix[rowIndices[i]];
        return result;
    }
    
    public static double[][] getSubMatrixRowWise(int startIndexInclusive, int endIndexExclusive,  double[][] matrix)
    {
        double[][] result = new double[endIndexExclusive - startIndexInclusive][];
        for( int i = startIndexInclusive; i < endIndexExclusive; i++ )
            result[i - startIndexInclusive] = matrix[i];
        return result;
    }
    
    public static Object[] getRanksWithTieCorrections(double[][] matrix)
    {
        boolean[] doSortInIncreasingOrder = new boolean[matrix[0].length];
        for( int i = 0; i < matrix[0].length; i++ )
            doSortInIncreasingOrder[i] = true;
        return getRanksWithTieCorrections(matrix, doSortInIncreasingOrder);
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
    
    public static boolean[][] getTransposedMatrix(boolean[][] matrix)
    {
        int n = matrix.length, m = matrix[0].length;
        boolean[][] result = new boolean[m][n];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < m; j++ )
            	result[j][i] = matrix[i][j];
        return result;
    }
    
    public static char[][] getTransposedMatrix(char[][] matrix)
    {
        int n = matrix.length, m = matrix[0].length;
        char[][] result = new char[m][n];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < m; j++ )
                result[j][i] = matrix[i][j];
        return result;
    }

    public static Object[] getRanksWithTieCorrections(double[][] matrix, boolean[] doSortInIncreasingOrder)
    {
        double[][] ranks = new double[matrix.length][matrix[0].length];
        double[] tieCorrections1 = new double[matrix[0].length], tieCorrections2 = new double[matrix[0].length];
        for( int j = 0; j < matrix[0].length; j++ )
        {
            // 1. Calculate indicesForNonNan, indicesForNan and array (it consists of not NaN-values).
            List<Integer> list1 = new ArrayList<>(), list2 = new ArrayList<>();
            List<Double> list3 = new ArrayList<>();
            double[] column = getColumn(matrix, j);
            for( int i = 0; i < column.length; i++ )
                if( Double.isNaN(column[i]) )
                    list2.add(i);
                else
                {
                    list1.add(i);
                    list3.add(column[i]);
                }
            int[] indicesForNonNan = UtilsGeneral.fromListIntegerToArray(list1), indicesForNan = UtilsGeneral.fromListIntegerToArray(list2);
            double[] array = UtilsGeneral.fromListToArray(list3);
            
            // 2. Calculate ranks for j-th list without NaN.
            if( ! doSortInIncreasingOrder[j] )
                array = VectorOperations.getProductOfVectorAndScalar(array, -1.0);
            Object[] objects = VectorOperations.getRanksWithTieCorrections(array);
            double[] ranksForGivenList = (double[])objects[0];
            tieCorrections1[j] = (double)objects[1];
            tieCorrections2[j] = (double)objects[2];
            for( int ii = 0; ii < ranksForGivenList.length; ii++ )
                ranks[indicesForNonNan[ii]][j] = ranksForGivenList[ii];

            // 3. Calculate ranks for NaN-values in j-th list.
            if( indicesForNan.length > 0 )
            {
                double rankForNan = (double)indicesForNonNan.length + 0.5 * (double)(matrix.length - indicesForNonNan.length + 1);
                for( int ii = 0; ii < indicesForNan.length; ii++ )
                    ranks[indicesForNan[ii]][j] = rankForNan;
                double x = (double)indicesForNan.length * (double)(indicesForNan.length - 1);
                tieCorrections1[j] += x;
                tieCorrections2[j] += x * (double)(indicesForNan.length + 1);
            }
        }
        return new Object[]{ranks, tieCorrections1, tieCorrections2};
    }
    
    public static double[][] getRanks(double[][] matrix, boolean[] doSortInIncreasingOrder)
    {
        return (double[][])getRanksWithTieCorrections(matrix, doSortInIncreasingOrder)[0];
    }
    
    public static void printMatrix(double[][] matrix)
    {
    	printMatrix(matrix, matrix.length);
    }

    public static void printMatrix(double[][] matrix, int numberOfRowsForPrint)
    {
		log.info("--- matrix: dim = " + matrix.length +  " x " + matrix[0].length);
        int n = Math.min(matrix.length, numberOfRowsForPrint);
        for( int i = 0; i < n; i++ )
        {
    		String str = "";
            for( int j = 0; j < matrix[0].length; j++ )
            	str += " " + Double.toString(matrix[i][j]);
            log.info(str);
        }
    }
    
    public static void fillColumn(double[][] matrix, double[] columnValues, int columnIndex)
    {
        for( int i = 0; i < matrix.length; i++ )
            matrix[i][columnIndex] = columnValues[i]; 
    }
    
    public static void removeColumn(double[][] matrix, int columnIndex)
    {
        for( int i = 0; i < matrix.length; i++ )
            matrix[i] = ArrayUtils.remove(matrix[i], columnIndex);
    }
    
    public static void removeColumns(double[][] matrix, int[] columnIndexes)
    {
        for( int i = 0; i < matrix.length; i++ )
            matrix[i] = org.apache.commons.lang3.ArrayUtils.removeAll(matrix[i], columnIndexes);
    }
    
    public static void addColumnToMatrix(double[][] matrix, double[] newColumn)
    {
        addColumnToMatrix(matrix, newColumn, matrix[0].length);
    }

    public static void addColumnToMatrix(double[][] matrix, double[] newColumn, int newColumnIndex)
    {
        for( int i = 0; i < matrix.length; i++ )
            matrix[i] = ArrayUtils.add(matrix[i], newColumnIndex, newColumn[i]);
    }
    
    public static double[] concatinateRows(double[][] matrix)
    {
        double[] result = new double[0];
        for( int i = 0; i < matrix.length; i++ )
            result = ArrayUtils.addAll(result, matrix[i]);
        return result;
    }
    
    /******************* MatrixTransformation: start *******************/
    public static class MatrixTransformation
    {
        public static double[][] getLgMatrixWithReplacement(double[][] matrix, double threshold, double replacement)
        {
            double[][] result = new double[matrix.length][matrix[0].length];
            for( int i = 0; i < matrix.length; i++ )
                for( int j = 0; j < matrix[0].length; j++ )
                    result[i][j] = matrix[i][j] >= threshold ? Math.log10(matrix[i][j]) : replacement;
            return result;
        }
        
        public static double[][] getLgMatrixWithReplacement(double[][] matrix)
        {
            return getLgMatrixWithReplacement(matrix, 1.0, 0.0);
        }
        
        public static double[][] transformToZeroAndOneRangeColumnWise(double[][] matrix)
        {
            double[][] result = getTransposedMatrix(matrix);
            for( int i = 0; i < result.length; i++ )
                result[i] = VectorTransformation.toZeroAndOneRange(result[i]);
            return getTransposedMatrix(result);
        }
    }
    /******************* MatrixTransformation: end *******************/
    
    static Logger log = Logger.getLogger(MatrixUtils.class.getName());
}
