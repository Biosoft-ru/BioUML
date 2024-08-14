
package biouml.plugins.bindingregions.utils;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.graphics.chart.Chart;

/**
 * @author yura
 * Multivariate sample is represented by dataMatrix;
 * dim(dataMatrix) = n x m; it contains m samples; each sample (located in matrix column) consists of n elements
 */

/***
 * 
 * @param dataMatrix : dim(dataMatrix) = n x m; it contains m samples; each sample consists of n elements
 * @return sample mean vector
 */
public class MultivariateSample
{
    // it is copied
    public static @Nonnull double[] getMeanVector(double[][] dataMatrix)
    {
        int n = dataMatrix.length, m = dataMatrix[0].length;
        double vector[] = new double[m];
        for( int j = 0; j < m; j++ )
            for( int i = 0; i < n; i++ )
                vector[j] += dataMatrix[i][j];
        return MatrixUtils.getProductOfVectorAndScalar(vector, 1.0 / n);
    }

    // it is copied
    public static double[][] getCovarianceMatrix(double[][] dataMatrix, double[] meanVector)
    {
        double[][] matrix = MatrixUtils.getProductXtrHX(dataMatrix, meanVector);
        return MatrixUtils.getProductOfMatrixAndScalar(matrix, 1.0 / dataMatrix.length);
    }

    // it is copied
    /***
     * 
     * @param dataMatrix
     * @return SSP matrix (SSP is Sum Of Squares and Products)
     */
    public static double[][] getSSPmatrix(double[][] dataMatrix)
    {
        return MatrixUtils.getProductXtrHX(dataMatrix, getMeanVector(dataMatrix));
    }
    
    public static double[][] getCovarianceMatrix(double[][] dataMatrix)
    {
        double[] meanVector = getMeanVector(dataMatrix);
        return getCovarianceMatrix(dataMatrix, meanVector);
    }

    /***
     * 
     * @param meanVector
     * @param inverseCovarianceMatrix
     * @param dataMatrix
     * @return array : array[0] = skewness, array[1] = kurtosis;
     */
    private static double[] getSkewnessAndKurtosis(double[] meanVector, double[][] inverseCovarianceMatrix, double[][] dataMatrix)
    {
        double skewness = 0.0, kurtosis = 0.0, gii3Sum = 0.0, gij3Sum = 0.0;
        for( int i = 0; i < dataMatrix.length; i++ )
        {
            double[] shiftedVector1 = MatrixUtils.getSubtractionOfVectors(dataMatrix[i], meanVector);
            double[] vector = MatrixUtils.getProductOfSymmetricMatrixAndVector(inverseCovarianceMatrix, shiftedVector1);
            double gii = MatrixUtils.getInnerProduct(vector, shiftedVector1);
            double gii2 = gii * gii;
            kurtosis += gii2;
            gii3Sum += gii2 * gii;
            for( int j = 0; j < i; j++ )
            {
                double[] shiftedVector2 = MatrixUtils.getSubtractionOfVectors(dataMatrix[j], meanVector);
                double gij = MatrixUtils.getInnerProduct(vector, shiftedVector2);
                gij3Sum += gij * gij * gij;
            }
        }
        kurtosis /= dataMatrix.length;
        skewness = (gii3Sum + 2.0 * gij3Sum) / (dataMatrix.length * dataMatrix.length);
        return new double[]{skewness, kurtosis};
    }
    
    /***
     * 
     * @param dataMatrix
     * @param maxNumberOfIterations
     * @param eps
     * @return array : array[0] = skewness, array[1] = kurtosis;
     */
    private static double[] getSkewnessAndKurtosis(double[][] dataMatrix, int maxNumberOfIterations, double eps)
    {
        double[] meanVector = getMeanVector(dataMatrix);
        double[][] covarianceMatrix = getCovarianceMatrix(dataMatrix, meanVector);
        double[][] inverseCovarianceMatrix = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(covarianceMatrix, maxNumberOfIterations, eps);
        return getSkewnessAndKurtosis(meanVector, inverseCovarianceMatrix, dataMatrix);
    }
    
    /***
     * 
     * @param numberOfObjects
     * @param numberOfVariables
     * @param skewness
     * @return array : array[0] = statistic for multinormality test; array[1] = p-value
     * @throws Exception
     */
    private static double[] getStatisticAndPvalueForSkewness(int numberOfObjects, int numberOfVariables, double skewness) throws Exception
    {
        double statistic = numberOfObjects * skewness / 6.0;
        double degreesOfFreedom = numberOfVariables * (numberOfVariables + 1) * (numberOfVariables + 2) / 6.0;
        double pValue = 1.0 - Stat.chiDistribution(statistic, degreesOfFreedom);
        return new double[]{statistic, pValue};
    }
    
    /***
     * 
     * @param numberOfObjects
     * @param numberOfVariables
     * @param kurtosis
     * @return array : array[0] = statistic for multinormality test; array[1] = p-value
     */
    private static double[] getStatisticAndPvalueForKurtosis(int numberOfObjects, int numberOfVariables, double kurtosis)
    {
        double x = numberOfVariables * (numberOfVariables + 2.0);
        double statistic = (kurtosis - x) / Math.sqrt(8.0 * x / numberOfObjects);
        double pValue = 2.0 * (1.0 - Stat.standartNormalDistribution(Math.abs(statistic)));
        return new double[]{statistic, pValue};
    }
    
    public static void writeTableWithMultinormalityTest(double[][] dataMatrix, int maxNumberOfIterations, double eps, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        double[] skewnessAndKurtosis = getSkewnessAndKurtosis(dataMatrix, maxNumberOfIterations, eps);
        double[] statisticAndPvalueForSkewness = getStatisticAndPvalueForSkewness(dataMatrix.length, dataMatrix[0].length, skewnessAndKurtosis[0]);
        double[] statisticAndPvalueForKurtosis = getStatisticAndPvalueForKurtosis(dataMatrix.length, dataMatrix[0].length, skewnessAndKurtosis[1]);
        double[][] data = new double[][]{{skewnessAndKurtosis[0], statisticAndPvalueForSkewness[0], statisticAndPvalueForSkewness[1]}, {skewnessAndKurtosis[1], statisticAndPvalueForKurtosis[0], statisticAndPvalueForKurtosis[1]}};
        TableUtils.writeDoubleTable(data, new String[]{"skewness", "kurtosis"}, new String[]{"value", "statistic", "p-value"}, pathToOutputs, tableName);
    }

    public static void writeTableWithTransformedDataMatrix(String dataTransformationType, String[] objectNames, String[] variableNames, double[][] dataMatrix, DataElementPath pathToOutputs, String tableName)
    {
        double[][] transformedDataMatrix = Transformation.transformData(dataMatrix, dataTransformationType);
        String[] newVariableNames = new String[variableNames.length];
        for( int i = 0 ; i < variableNames.length; i++ )
            newVariableNames[i] = variableNames[i] + "_transformed";
        TableUtils.writeDoubleTable(transformedDataMatrix, objectNames, newVariableNames, pathToOutputs, tableName);
    }
    
    public static double[] getSigmaVector(double[][] dataMatrix)
    {
        double[] diagonal = MatrixUtils.getDiagonalOfMatrix(getCovarianceMatrix(dataMatrix));
        return MatrixUtils.getSqrtVector(diagonal);
    }
    
    public static @Nonnull double[][] getCorrelationMatrixFromCovarianceMatrix(double[][] covarianceMatrix)
    {
        int n = covarianceMatrix.length;
        double[][] result = MatrixUtils.getClone(covarianceMatrix);
        for( int i = 0; i < n; i++ )
        {
            if( covarianceMatrix[i][i] <= 0.0 )
                throw new IllegalArgumentException("Correlation matrix could not be created: covariance matrix contains elements <= 0 on the main diagonal");
            result[i][i] = Math.sqrt(covarianceMatrix[i][i]);
        }
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < i; j++ )
                result[i][j] /= result[i][i] * result[j][j];
        for( int i = 0; i < n; i++ )
            result[i][i] = 1.0;
        return result;
    }
    
    public static @Nonnull double[][] getCorrelationMatrix(double[][] dataMatrix)
    {
        return getCorrelationMatrixFromCovarianceMatrix(getCovarianceMatrix(dataMatrix));
    }

    public static void writeTableWithCovarianceOrCorrelationMatrix(boolean isCovarianceMatrix, String[] variableNames, double[][] dataMatrix, DataElementPath pathToOutputs, String tableName)
    {
        double[][] symmetricMatrix = isCovarianceMatrix ? getCovarianceMatrix(dataMatrix) : getCorrelationMatrix(dataMatrix);
        TableUtils.writeTableWithSquareSymmetricMatrix(variableNames, symmetricMatrix, pathToOutputs, tableName);
    }
    
    public static void writeIndividualChartsWithSmoothedDensities(String[] variableNames, double[][] dataMatrix, Boolean doAddTwoZeroPoints, Map<String, Double> nameAndMultipliers, String windowSelector, Double givenWindow, DataElementPath pathToOutputs, String tableName)
    {
        for( int i = 0; i < variableNames.length; i++ )
        {
            Map<String, double[]> map = new HashMap<>();
            map.put("", MatrixUtils.getColumn(dataMatrix, i));
            Chart chart = DensityEstimation.chartWithSmoothedDensities(map, variableNames[i], doAddTwoZeroPoints, nameAndMultipliers, windowSelector, givenWindow);
            TableUtils.addChartToTable(variableNames[i], chart, pathToOutputs.getChildPath(tableName));
        }
    }
    
    public static void writeTableWithMeanAndSigma(String[] variableNames, double[][] dataMatrix, DataElementPath pathToOutputs, String tableName)
    {
        double[][] data = new double[variableNames.length][];
        for( int i = 0; i < variableNames.length; i++ )
        {
            double[] univariateSample = MatrixUtils.getColumn(dataMatrix, i);
            double[] meanAndSigma = Stat.getMeanAndSigma(univariateSample);
            data[i] = new double[]{univariateSample.length, meanAndSigma[0], meanAndSigma[1]};
        }
        TableUtils.writeDoubleTable(data, variableNames, new String[] {"Sample size", "Mean", "Sigma"}, pathToOutputs, tableName);
    }
    
    /****************** Transformation : start **********************/
    public static class Transformation
    {
        public static final String NO_TRANSFORMATION = "No transformation";
        private static final String TRANSFORMATION_MIN_MAX = "Transformation to zero min-values and unit max-values";
        private static final String TRANSFORMATION_MEANS = "Transformation to zero means";
        private static final String TRANSFORMATION_MEAN_SIGMA = "Z-score transformation";
        public static final String TRANSFORMATION_RANKS = "Rank transformation";
        public static final String TRANSFORMATION_MAHALANOBIS = "Mahalanobis transformation";
        public static final String TRANSFORMATION_PRINCIPAL_COMPONENTS = "Principal component transformation";


        public static String[] getTransformationTypes()
        {
            return new String[]{TRANSFORMATION_RANKS, TRANSFORMATION_MIN_MAX, TRANSFORMATION_MEANS, TRANSFORMATION_MEAN_SIGMA, TRANSFORMATION_MAHALANOBIS, TRANSFORMATION_PRINCIPAL_COMPONENTS, NO_TRANSFORMATION};
        }
        
        /***
         * Transform each column of matrix X = (x1,...,xN)' -> Y = (y1,...,yN)'
         * yi = (xi - min(X)) / range(X), where range(X) = max(X) - min(X);
         * 
         * @param dataMatrix
         */
        private static double[][] transformToRangeZeroOne(double[][] dataMatrix)
        {
            int n = dataMatrix.length, m = dataMatrix[0].length;
            double[][] result = new double[n][m];
            for( int j = 0; j < m; j++ )
            {
                double min = dataMatrix[0][j], max = min;
                for( int i = 0; i < n; i++ )
                {
                    min = Math.min(min, dataMatrix[i][j]);
                    max = Math.max(max, dataMatrix[i][j]);
                }
                double range = max - min;
                for( int i = 0; i < n; i++ )
                    result[i][j] = (dataMatrix[i][j] - min) / range;
            }
            return result;
        }

        // it is copied
        /***
         * 
         * @param dataMatrix
         * @param meanVector - mean values for columns of data matrix
         * @return transformed data matrix in which all means of columns are zero
         */
        public static double[][] transformToZeroMeans(double[][] dataMatrix, double[] meanVector)
        {
            double[][] result = new double[dataMatrix.length][];
            for( int i = 0; i < dataMatrix.length; i++ )
                result[i] = MatrixUtils.getSubtractionOfVectors(dataMatrix[i], meanVector);
            return result;
        }
        
        private static double[][] transformToZeroMeans(double[][] dataMatrix)
        {
            return transformToZeroMeans(dataMatrix, getMeanVector(dataMatrix));
        }
        
        private static double[][] mahalanobisTransformation(double[][] dataMatrix, int maxNumberOfIterations, double eps)
        {
            double[][] result = new double[dataMatrix.length][];
            double[] meanVector = getMeanVector(dataMatrix);
            double[][] covarianceMatrix = getCovarianceMatrix(dataMatrix, meanVector);
            double[][] halfInverseSymmetricMatrix = MatrixUtils.getHalfInverseSymmetricMatrixByJacobiMethod(covarianceMatrix, maxNumberOfIterations, eps);
            for( int i = 0; i < dataMatrix.length; i++ )
            {
                result[i] = MatrixUtils.getSubtractionOfVectors(dataMatrix[i], meanVector);
                result[i] = MatrixUtils.getProductOfSymmetricMatrixAndVector(halfInverseSymmetricMatrix, result[i]);
            }
            return result;
        }
        
        // TODO: to sort eigenvectors in increasing order of eigenvalues
        // TODO: to test the correct usage of MatrixUtils.getProductOfTransposedVectorAndRectangularMatrix()
        private static double[][] principalComponentTransformation(double[][] dataMatrix, int maxNumberOfIterations, double eps)
        {
            double[][] result = new double[dataMatrix.length][];
            double[] meanVector = getMeanVector(dataMatrix);
            double[][] covarianceMatrix = getCovarianceMatrix(dataMatrix, meanVector);
            Object[] objects = MatrixUtils.getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(covarianceMatrix, maxNumberOfIterations, eps);
            for( int i = 0; i < dataMatrix.length; i++ )
            {
                result[i] = MatrixUtils.getSubtractionOfVectors(dataMatrix[i], meanVector);
                result[i] = MatrixUtils.getProductOfTransposedVectorAndRectangularMatrix((double[][])objects[2], result[i]);
            }
            return result;
        }

        /***
         * Transform each column of matrix X = (x1,...,xN)' -> Y = (y1,...,yN)'
         * yi = (xi - mean(X)) / sigma(X)
         * @param dataMatrix
         * @return transformed data matrix
         */
        private static double[][] transformToZeroMeanAndUnitSigma(double[][] dataMatrix)
        {
            int n = dataMatrix.length, m = dataMatrix[0].length;
            double[][] result = new double[n][m];
            double[] mean = getMeanVector(dataMatrix);
            double[] sigmaVector = getSigmaVector(dataMatrix);
            for( int j = 0; j < m; j++ )
                for( int i = 0; i < n; i++ )
                    result[i][j] = ( dataMatrix[i][j] - mean[j] ) / sigmaVector[j];
            return result;
        }

        /***
         * Transform each column of matrix X = (x1,...,xN)' -> Y = (y1,...,yN)'
         * where yi = Rank(xi)
         * @param dataMatrix
         * @return transformed data matrix
         */
        private static double[][] transformToRanksColumnWise(double[][] dataMatrix)
        {
            int n = dataMatrix.length, m = dataMatrix[0].length;
            double ranks[] = new double[n];
            double[][] result = new double[n][m];
            for( int j = 0; j < m; j++ )
            {
                /***
                double[] column = new double[n];
                for( int i = 0; i < n; i++ )
                    column[i] = dataMatrix[i][j];
                ***/
                double[] column = MatrixUtils.getColumn(dataMatrix, j);
                Stat.getRanks(column, ranks, new double[2]);
                for( int i = 0; i < n; i++ )
                    result[i][j] = ranks[i];
            }
            return result;
        }

        // TODO: to test mahalanobisTransformation() and principalComponentTransformation()
        public static double[][] transformData(double[][] dataMatrix, String transformationType)
        {
            switch( transformationType )
            {
                default                                  :
                case NO_TRANSFORMATION                   : return dataMatrix;
                case TRANSFORMATION_MIN_MAX              : return transformToRangeZeroOne(dataMatrix);
                case TRANSFORMATION_MEANS                : return transformToZeroMeans(dataMatrix);
                case TRANSFORMATION_MEAN_SIGMA           : return transformToZeroMeanAndUnitSigma(dataMatrix);
                case TRANSFORMATION_RANKS                : return transformToRanksColumnWise(dataMatrix);
                case TRANSFORMATION_MAHALANOBIS          : return mahalanobisTransformation(dataMatrix, MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE, MatrixUtils.DEFAULT_EPS_FOR_INVERSE);
                case TRANSFORMATION_PRINCIPAL_COMPONENTS : return principalComponentTransformation(dataMatrix, MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE, MatrixUtils.DEFAULT_EPS_FOR_INVERSE);
            }
        }
    }
    /****************** Transformation : finish **********************/
}
