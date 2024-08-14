
package biouml.plugins.bindingregions.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;
import java.util.logging.Logger;

import biouml.plugins.bindingregions.rscript.Rutils;
import biouml.plugins.bindingregions.utils.Classification.ClassificationByMultivariateRegressionOfIndicatorMatrix;
import biouml.plugins.bindingregions.utils.Clusterization.KMeansAlgorithm;
import biouml.plugins.bindingregions.utils.MatrixUtils.Distance;
import biouml.plugins.bindingregions.utils.MultivariateSample.Transformation;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.TableUtils.ParticularTable;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysis.Util;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;

/**
 * @author yura
 *
 */
public class LinearRegression
{
    public static final String INTERCEPT = "intercept";
    public static final String NAME_OF_TABLE_WITH_COEFFICIENTS = "regressionCoefficients";
    
    public static final String ADD_INTERCEPT = "Add intercept";
    public static final String ADD_ALL_INTERACTIONS = "Add all interactions";
    public static final String ADD_SQUARED_VARIABLES = "Add all squared variables";
//  public static final String ADD_NOTHING = "Add nothing";

    /***************** general stuff : start *********************/
    // it is copied
    public static double getResidualVariance(double[] response, double[] predictedResponse, int numberOfVariables)
    {
        return MatrixUtils.Distance.getEuclideanSquared(response, predictedResponse) / (response.length - numberOfVariables);
    }
    
    // it is copied
    /***
     * 
     * @param response
     * @param predictedResponse
     * @param numberOfVariables
     * @return double[] array : array[0] = Pearson correlation between observations and predictions; array[1] = Spearman correlation between observations and predictions; array[2] = Mean of observations; array[3] = Variance of observations; array[4] = Explained variance (in %); array[5] = Number of observations;
     * @throws Exception
     */
    public static double[] getSummaryOnModelAccuracy(double[] response, double[] predictedResponse, int numberOfVariables) throws Exception
    {
        double pearsonCorr = Stat.pearsonCorrelation(response, predictedResponse);
        double spearmanCorr = StatUtil.getSpearmanCorrelation(response, predictedResponse);
        double[] meanAndSigma = Stat.getMeanAndSigma(response);
        double meanResponse = meanAndSigma[0];
        double variance = meanAndSigma[1] * meanAndSigma[1];
        double residualVariance = getResidualVariance(response, predictedResponse, numberOfVariables);
        double varExplained = residualVariance >= variance ? 0.0 : 100.0 * (variance - residualVariance) / variance;
        return new double[]{pearsonCorr, spearmanCorr, meanResponse, variance, varExplained, response.length};
    }
    
    // it is copied
    public static void writeTableWithSummaryOnModelAccuracy(double[] response, double[] predictedResponse, int numberOfVariables, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        double[] valuesForTable = getSummaryOnModelAccuracy(response, predictedResponse, numberOfVariables);
        TableUtils.writeDoubleTable(valuesForTable, new String[]{"Pearson correlation between observations and predictions", "Spearman correlation between observations and predictions", "Mean of observations", "Variance of observations", "Explained variance (in %)", "Number of observations"}, "value", pathToOutputs, tableName);
    }
    
    // it is copied
    public static double[] getPredictions(double[][] dataMatrix, double[] coefficients)
    {
        return MatrixUtils.getProductOfRectangularMatrixAndVector(dataMatrix, coefficients);
    }
    
    // it is copied
    public static String[] addInterceptToRegression(String[] variableNames, double[][] dataMatrix)
    {
        if( ArrayUtils.contains(variableNames, INTERCEPT) ) return variableNames;
        int m = variableNames.length;
        String[] newNames = (String[])ArrayUtils.add(variableNames, m, INTERCEPT);
        MatrixUtils.addColumnToMatrix(dataMatrix, MatrixUtils.getConstantVector(dataMatrix.length, 1.0));        
        return newNames;
    }
    
    public static Object[] extendDataMatrix(String[] variableNames, double[][] dataMatrix, String[] dataMatrixExtensions)
    {
        int n = dataMatrix.length, m = variableNames.length;
        String[] newVariableNames = new String[m];
        System.arraycopy(variableNames, 0, newVariableNames, 0, m);
        double[][] newDataMatrix = MatrixUtils.getClone(dataMatrix);
        if( ArrayUtils.contains(dataMatrixExtensions, ADD_SQUARED_VARIABLES) )
            for( int j = 0; j < m; j++ )
            {
                newVariableNames = (String[])ArrayUtils.add(newVariableNames, newVariableNames.length, "squared(" + variableNames[j] + ")");
                for( int i = 0; i < n; i++ )
                    newDataMatrix[i] = ArrayUtils.add(newDataMatrix[i], m + j, dataMatrix[i][j] * dataMatrix[i][j]);
            }
        if( ArrayUtils.contains(dataMatrixExtensions, ADD_ALL_INTERACTIONS) )
            for( int j = 0; j < m; j++ )
                for( int jj = 0; jj < j; jj++ )
                {
                    newVariableNames = (String[])ArrayUtils.add(newVariableNames, newVariableNames.length, "interaction (" + variableNames[j] + ") x (" + variableNames[jj] + ")");
                    for( int i = 0; i < n; i++ )
                        newDataMatrix[i] = ArrayUtils.add(newDataMatrix[i], newDataMatrix[i].length, dataMatrix[i][j] * dataMatrix[i][jj]);
                }
        if( ArrayUtils.contains(dataMatrixExtensions, ADD_INTERCEPT) )
            newVariableNames = addInterceptToRegression(newVariableNames, newDataMatrix);
        return new Object[]{newVariableNames, newDataMatrix};
    }
    /***************** general stuff : finish *********************/

    /*********************** LSregression : start *****************/
    public static class LSregression
    {
        private final String[] variableNames; // dim(variableNames) = m;
        private String[] objectNames;   // dim(objectNames) = n;
        private final double[][] dataMatrix;  // dim(dataMatrix) = n x m;
        private final String responseName;
        private final double[] response;      // dim(response) = n;
        
        public LSregression(String[] variableNames, String[] objectNames, double[][] dataMatrix, String responseName, double[] response)
        {
            this.variableNames = variableNames;
            this.objectNames = objectNames;
            this.dataMatrix = dataMatrix;
            this.responseName = responseName;
            this.response = response;
        }

        // it is copied
        // simple linear regression y = a + b * x;
        public LSregression(String variableName, double[] xValues, String responseName, double[] yValues)
        {
            this.variableNames = variableName != null ? new String[]{INTERCEPT, variableName} : null;
            int n = xValues.length;
            double[][] matrix = new double[n][];
            for( int i = 0; i < n; i++ )
                matrix[i] = new double[]{1.0, xValues[i]};
            this.dataMatrix = matrix;
            this.responseName = responseName;
            this.response = yValues;
        }

        ///// it is copied
        /***
         * 
         * @param maxNumberOfIterations - maximal number of iterations (for inverse matrix calculation)
         * @param eps if ( maximum of absolute values of off-diagonal elements of matrix ) < eps then iterative process will be terminated (for inverse matrix calculation)
         * @param doConsiderSignificance - if true, then significance of regression coefficients will be determined
         * @return array Object[];  dimension(array) = {6 if isSignificance == true; 4 otherwise}.
         *                          Object[0] = double[];
         *                                      the estimates of the regression coefficients
         *                          Object[1] = double[][]
         *                                      the lower triangular part of the symmetric
         *                                      covariance matrix for parameters (up to the
         *                                      variance of the response variable);
         *                          Object[2] = double[]
         *                                      the predictions of the response variable
         *                          Object[3] = double
         *                                      the residual variance of the response variable.
         *                          !!! only if( doConsiderSignificance == true )
         *                          Object[4] = double[] statistics;
         *                                      statistics[i] = i-th statistic for testing hypothesis that i-th regression coefficient is equal to zero
         *                          !!! only if( doConsiderSignificance == true )
         *                          Object[5] = double[] pValues;
         *                                      pValues[i] = p-value for test of hypothesis that i-th regression coefficient is equal to zero
         */
        public Object[] getMultipleLinearRegressionByJacobiMethod(int maxNumberOfIterations, double eps, boolean doConsiderSignificance)
        {
            int m = dataMatrix[0].length;
            if( dataMatrix.length != response.length || dataMatrix.length <= m) return null;
            double[][] matrix = MatrixUtils.getProductOfTransposedMatrixAndMatrix(dataMatrix);
            double[][] covarianceMatrix = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(matrix, maxNumberOfIterations, eps);
            double[] vector = MatrixUtils.getProductOfTransposedMatrixAndVector(dataMatrix, response);
            double[] coefficients = MatrixUtils.getProductOfSymmetricMatrixAndVector(covarianceMatrix, vector);
            double[] predictions = getPredictions(dataMatrix, coefficients);
            double residualVariance = getResidualVariance(response, predictions, m);
            if ( ! doConsiderSignificance )
                return new Object[]{coefficients, covarianceMatrix, predictions, residualVariance};
            double sigma = Math.sqrt(residualVariance);
            double[] statistics = new double[m], pValues = new double[m];
            for( int i = 0; i < m; i++ )
            {
                 statistics[i] = coefficients[i] / (sigma * Math.sqrt(covarianceMatrix[i][i]));
                 pValues[i] = Stat.studentDistribution(Math.abs(statistics[i]), dataMatrix.length - m, 80)[1];
            }
            return new Object[]{coefficients, covarianceMatrix, predictions, residualVariance, statistics, pValues};
        }

        ///// it is copied !!!!
        // line is linear regression y = a + b * x
        public static Chart createChartWithLineAndCloud(double[] xCloud, double[] yCloud, Double xMin, Double xMax, Double yMin, Double yMax, String xName, String yName)
        {
            LSregression lsr = new LSregression(null, xCloud, null, yCloud);
            double[] coeffs = (double[])lsr.getMultipleLinearRegressionByJacobiMethod(MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE, MatrixUtils.DEFAULT_EPS_FOR_INVERSE, false)[0];
            double[] linearXvalues = Stat.getMinAndMax(xCloud);
            double[] linearYvalues = new double[]{coeffs[0] + coeffs[1] * linearXvalues[0], coeffs[0] + coeffs[1] * linearXvalues[1]};
            return TableUtils.createChart(linearXvalues, linearYvalues, null, xCloud, yCloud, null, xMin, xMax, yMin, yMax, xName, yName);
        }
        
        // it is copied
        public void writeTableWithCoefficients(double[] coefficients, double[] statistics, double[] pValues, DataElementPath pathToOutputs, String tableName)
        {
            int m = variableNames.length;
            if( coefficients.length != m || statistics.length != m || pValues.length != m ) return;
            double[][] data = new double[m][];
            for( int i = 0; i < m; i++ )
                data[i] = new double[]{coefficients[i], statistics[i], pValues[i]};
            TableUtils.writeDoubleTable(data, variableNames, new String[] {"coefficient", "statistic (Z-score)", "p-value"}, pathToOutputs, tableName);
        }
        
        // it is copied
        public Object[] getResultsOfLSregression(int maxNumberOfIterations, double eps, DataElementPath pathToOutputs) throws Exception
        {
            Object[] objects = getMultipleLinearRegressionByJacobiMethod(maxNumberOfIterations, eps, true);
            double[] predictedResponse = (double[])objects[2];
            writeTableWithCoefficients((double[])objects[0], (double[])objects[4], (double[])objects[5], pathToOutputs, NAME_OF_TABLE_WITH_COEFFICIENTS);
            writeTableWithSummaryOnModelAccuracy(response, predictedResponse, dataMatrix[0].length, pathToOutputs, "summaryOnModelAccuracy");
            Chart chart = createChartWithLineAndCloud(response, predictedResponse, null, null, null, null, "Observations", "Predictions");
            TableUtils.addChartToTable(TableUtils.CHART, chart, pathToOutputs.getChildPath("chart_observationsAndPredictions"));
            return objects;
        }
        
        public static double[] readRegressionModelAndPredictResponse(DataElementPath pathToFolderWithSavedModel, double[][] dataMatrix) throws Exception
        {
            String[] variableNamesInModel = ParticularTable.readVariableNames(pathToFolderWithSavedModel, ParticularTable.NAME_OF_TABLE_WITH_VARIABLE_NAMES);
            TableDataCollection table = pathToFolderWithSavedModel.getChildPath(NAME_OF_TABLE_WITH_COEFFICIENTS).getDataElement(TableDataCollection.class);
            Map<String, Double> preCoefficients = TableUtils.readSignificantCoefficientNameAnValuesInDoubleColumn(table, "coefficient", "p-Value", 1.01);
            double[] coefficients = StreamEx.of(variableNamesInModel).mapToDouble(preCoefficients::get).toArray();
            return getPredictions(dataMatrix, coefficients);
        }

        /***
         * 
         * @param dataPoint
         * @param responseOfDataPoint
         * @param predictionOfDataPoint
         * @param responseVarianse
         * @param regressionCoeffitientsCovarianceMatrix
         * @return array: array[0] = studentized residual for given data point; array[1] = p-value of studentized residual
         */
        private static double[] getStudentizedResidualForGivenDataPoint(double[]dataPoint, double responseOfDataPoint, double predictionOfDataPoint, double responseVariance, int degreesOfFreedom, double[][] regressionCoefficientsCovarianceMatrix)
        {
            double variance = responseVariance * (1.0 - MatrixUtils.getProductOfTransposedVectorAndSymmetricMatrixAndVector(regressionCoefficientsCovarianceMatrix, dataPoint));
            double studentizedResidual = (responseOfDataPoint - predictionOfDataPoint) / Math.sqrt(variance);
            double pValue = Stat.studentDistribution(Math.abs(studentizedResidual), degreesOfFreedom, 80)[1];
            return new double[]{studentizedResidual, pValue};
        }

        public void writeTablesWithRelationshipsBetweenResponseAndEveryVariable(DataElementPath pathToOutputs, String chartTableName, String correlationTableName) throws Exception
        {
            String[] namesOfColumns = new String[]{"Pearson correlation", "Spearman correlation"};
            for( int i = 0; i < variableNames.length; i++ )
            {
                if( variableNames[i].equals(INTERCEPT) ) continue;
                double[] variable = MatrixUtils.getColumn(dataMatrix, i);
                Chart chart = createChartWithLineAndCloud(variable, response, null, null, null, null, variableNames[i], responseName);
                TableUtils.addChartToTable(variableNames[i], chart, pathToOutputs.getChildPath(chartTableName));
                double[] row = new double[]{Stat.pearsonCorrelation(variable, response), StatUtil.getSpearmanCorrelation(variable, response)};
                TableUtils.addRowToDoubleTable(row, variableNames[i], namesOfColumns, pathToOutputs, correlationTableName);
                double window = DensityEstimation.getWindow(variable, DensityEstimation.WINDOW_WIDTH_03, 0.0);
                double[][] smoothedCurve = Util.nwAverage(variable, response, window);
                chart = TableUtils.createChart(smoothedCurve[0], smoothedCurve[1], "", null, null, null, null, null, null, null, variableNames[i], "smoothed " + responseName);
                TableUtils.addChartToTable("smoothed version : " + variableNames[i], chart, pathToOutputs.getChildPath(chartTableName));
                row = new double[]{Stat.pearsonCorrelation(smoothedCurve[0], smoothedCurve[1]), StatUtil.getSpearmanCorrelation(smoothedCurve[0], smoothedCurve[1])};
                TableUtils.addRowToDoubleTable(row, "smoothed version : " + variableNames[i], namesOfColumns, pathToOutputs, correlationTableName);
            }
        }
    }
    /*********************** LSregression : finish *******************************/
    
    /*********************** MultivariateLinearRegression : start *****************/
    public static class MultivariateLinearRegression
    {
        private final double[][] dataMatrix;  // dim(dataMatrix) = n x m;
        private final double[][] responses;   // dim(response) = n x k;
        
        public MultivariateLinearRegression(double[][] dataMatrix, double[][] responses)
        {
            this.dataMatrix = dataMatrix;
            this.responses = responses;
        }
        
        public double[][] getCoefficients(int maxNumberOfIterations, double eps)
        {
            double[][] mat = MatrixUtils.getProductOfRectangularTransposedMatrixAndMatrix(dataMatrix, responses);
            double[][] matrix = MatrixUtils.getProductOfTransposedMatrixAndMatrix(dataMatrix);
            matrix = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(matrix, maxNumberOfIterations, eps);
            return MatrixUtils.getProductOfSymmetricAndRectangularMatrices(matrix, mat);
        }
        
        public static TableDataCollection writeRegressionModel(double[][] coefficients, String[] variableNames, String[] responseNames, DataElementPath pathToOutputs, String tableName)
        {
            return TableUtils.writeDoubleTable(coefficients, variableNames, responseNames, pathToOutputs, tableName);
        }

        /***
         * 
         * @param pathToFolderWithSavedModel
         * @param nameOfTableWithCoefficients
         * @return array; dim(array) = 3 : array[0] = String[] variableNames; array[1] = responseNames; array[2] = double[][] coefficients;
         */
        public static Object[] readRegressionModel(DataElementPath pathToFolderWithSavedModel, String nameOfTableWithCoefficients)
        {
            return TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(nameOfTableWithCoefficients));
        }
    }
    /******************************** MultivariateLinearRegression : finish ********************/
    
    /******************************* PrincipalComponentRegression : start ***********************/
    public static class PrincipalComponentRegression
    {
        public static final String TYPE_OF_PC_SORTING_EIGEN_VALUE = "Eigen value magnitude";
        public static final String TYPE_OF_PC_SORTING_EXPLAINED_VARIANCE = "Variance explained by principle component";
        public static final String TYPE_OF_PC_SORTING_PC_SIGNIFICANCE = "Principle component significance";
        
        private static final String NAME_OF_TABLE_WITH_VARIABLE_MEANS = "variableMeans";
        private static final String NAME_OF_TABLE_WITH_RESPONSE_MEAN = "responseMean";
        private static final String NAME_OF_COLUMN_WITH_REGRESSION_COEFFICIENTS = "regression coefficient";
        private static final String NAME_OF_COLUMN_WITH_VARIABLE_MEANS = "variable mean";
        private static final String NAME_OF_COLUMN_WITH_RESPONSE_MEAN = "response mean";
        
        private final String[] variableNames;
        private final double[] eigenValues;
        private final double[][] eigenVectors;
        private final double responseMean;
        private final double[] variableMeans;
        private final double[] pcCoefficients;
        private final double[] varianceProportions;
        private final double[] statisticsForPCcoefficients;
        private final double[] pValuesForPCcoefficients;
        private final double pcSigma;
        private final int numberOfObservations;
        private final String principalComponentSortingType;
        private final int[] sortedPositions; // for principalComponentSortingType = TYPE_OF_PC_SORTING_EIGEN_VALUE : eigenValues[sortedPositions[0]] <= eigenValues[sortedPositions[1]] <= ...

        public PrincipalComponentRegression(String[] variableNames, double[][] dataMatrix, double[] response, int maxNumberOfIterations, double eps, String principalComponentSortingType)
        {
            numberOfObservations = dataMatrix.length;
            this.variableNames = variableNames;
            responseMean = Stat.mean(response);
            variableMeans = MultivariateSample.getMeanVector(dataMatrix);
            double[][] transformedDataMatrix = Transformation.transformToZeroMeans(dataMatrix, variableMeans);
            double[][] covarianceMatrix = MultivariateSample.getCovarianceMatrix(transformedDataMatrix);
            Object[] objects = MatrixUtils.getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(covarianceMatrix, maxNumberOfIterations, eps);
            eigenValues = (double[])objects[1];
            eigenVectors = (double[][])objects[2];
            double[][] pcTransformedDataMatrix = MatrixUtils.getProductOfRectangularMatrices(transformedDataMatrix, eigenVectors);
            double[] transformedResponse = MatrixUtils.getSumOfVectors(response, -responseMean);
            pcCoefficients = MatrixUtils.getProductOfTransposedMatrixAndVector(pcTransformedDataMatrix, transformedResponse);
            for( int i = 0; i < pcCoefficients.length; i++)
                pcCoefficients[i] /= eigenValues[i] * numberOfObservations;
            double x = numberOfObservations / MatrixUtils.getInnerProduct(transformedResponse, transformedResponse);
            varianceProportions = new double[pcCoefficients.length];
            for( int i = 0; i < pcCoefficients.length; i++ )
                varianceProportions[i] = eigenValues[i] * pcCoefficients[i] * pcCoefficients[i] * x;
            double[] pcPrediction = LinearRegression.getPredictions(pcTransformedDataMatrix, pcCoefficients);
            pcSigma = Math.sqrt(Distance.getEuclideanSquared(transformedResponse, pcPrediction) / (numberOfObservations - variableNames.length - 1));
            statisticsForPCcoefficients = new double[pcCoefficients.length];
            pValuesForPCcoefficients = new double[pcCoefficients.length];
            for( int i = 0; i < pcCoefficients.length; i++ )
            {
                statisticsForPCcoefficients[i] = pcCoefficients[i] * Math.sqrt(numberOfObservations * eigenValues[i]) / pcSigma;
                pValuesForPCcoefficients[i] = Stat.studentDistribution(Math.abs(statisticsForPCcoefficients[i]), numberOfObservations - pcCoefficients.length - 1, 80)[1];
            }
            this.principalComponentSortingType = principalComponentSortingType;
            sortedPositions = sortPrincipalComponents(this.principalComponentSortingType);
        }
        
        public static String[] getAvailableTypesOfPCsorting()
        {
            return new String[]{TYPE_OF_PC_SORTING_EIGEN_VALUE, TYPE_OF_PC_SORTING_PC_SIGNIFICANCE, TYPE_OF_PC_SORTING_EXPLAINED_VARIANCE};
        }

        private int[] sortPrincipalComponents(String principalComponentSortingType)
        {
            switch( principalComponentSortingType )
            {
                case TYPE_OF_PC_SORTING_EIGEN_VALUE        : return Util.sortHeap(eigenValues.clone());
                case TYPE_OF_PC_SORTING_EXPLAINED_VARIANCE : return Util.sortHeap(varianceProportions.clone());
                case TYPE_OF_PC_SORTING_PC_SIGNIFICANCE    : double[] negativePvalues = new double[pValuesForPCcoefficients.length];
                                                             for( int i = 0; i < negativePvalues.length; i++ )
                                                                 negativePvalues[i] = -pValuesForPCcoefficients[i];
                                                             return Util.sortHeap(negativePvalues);
                default                                    : return null;
            }
        }
        
        private static double[] getPredictions(double responseMean, double[] variableMeans, double[] regressionCoefficients, double[][] dataMatrix)
        {
            double[] predictions = new double[dataMatrix.length];
            for( int i = 0; i < dataMatrix.length; i++ )
            {
                double[] dataPoint = MatrixUtils.getSubtractionOfVectors(dataMatrix[i], variableMeans);
                predictions[i] = responseMean + MatrixUtils.getInnerProduct(dataPoint, regressionCoefficients);
            }
            return predictions;
        }
        
        private static double[] transformPCcoefficientsToRegressionCoefficients(int numberOfPCs, double[][] eigenVectors, int[] sortedPositions, double[] pcCoefficients)
        {
            double[] newPCcoefficients = pcCoefficients.clone();
            for( int i = 0; i < pcCoefficients.length - numberOfPCs; i++ )
                newPCcoefficients[sortedPositions[i]] = 0.0;
            return MatrixUtils.getProductOfRectangularMatrixAndVector(eigenVectors, newPCcoefficients);
        }
        
        public void writeTableWithPCcoefficients(DataElementPath pathToOutputs, String tableName)
        {
            double[][] data = new double[variableNames.length][];
            for( int i = 0; i < variableNames.length; i++ )
                data[i] = new double[]{pcCoefficients[i], eigenValues[i], varianceProportions[i] * 100, statisticsForPCcoefficients[i], pValuesForPCcoefficients[i]};
            String[] pcNames = new String[variableNames.length];
            int[] sortedPositions = sortPrincipalComponents(TYPE_OF_PC_SORTING_EIGEN_VALUE);
            for( int i = 0; i < variableNames.length; i++ )
                pcNames[sortedPositions[i]] = "Principle_component_" + Integer.toString(i);
            TableUtils.writeDoubleTable(data, pcNames, new String[] {"regression coefficient", "eigen value", "response variance explained by principal component (in %)", "statistic (Z-score)", "p-value"}, pathToOutputs, tableName);
        }

        public double[] createAndWriteRegressionModel(double[][] dataMatrix, int numberOfPCs, DataElementPath pathToOutputs)
        {
            double[] regressionCoefficients = transformPCcoefficientsToRegressionCoefficients(numberOfPCs, eigenVectors, sortedPositions, pcCoefficients);
            TableUtils.writeDoubleTable(regressionCoefficients, variableNames, NAME_OF_COLUMN_WITH_REGRESSION_COEFFICIENTS, pathToOutputs, NAME_OF_TABLE_WITH_COEFFICIENTS);
            TableUtils.writeDoubleTable(variableMeans, variableNames, NAME_OF_COLUMN_WITH_VARIABLE_MEANS, pathToOutputs, NAME_OF_TABLE_WITH_VARIABLE_MEANS);
            TableUtils.writeDoubleTable(new double[]{responseMean}, new String[]{NAME_OF_COLUMN_WITH_RESPONSE_MEAN}, NAME_OF_COLUMN_WITH_RESPONSE_MEAN, pathToOutputs, NAME_OF_TABLE_WITH_RESPONSE_MEAN);
            return getPredictions(responseMean, variableMeans, regressionCoefficients, dataMatrix);
        }
        
        public static double[] readRegressionModelAndPredictResponse(DataElementPath pathToFolderWithSavedModel, double[][] dataMatrix) throws Exception
        {
            double responseMean = TableUtils.readGivenColumnInDoubleTableAsArray(pathToFolderWithSavedModel.getChildPath(NAME_OF_TABLE_WITH_RESPONSE_MEAN).getDataElement(TableDataCollection.class), NAME_OF_COLUMN_WITH_RESPONSE_MEAN)[0];
            String[] variableNamesInModel = ParticularTable.readVariableNames(pathToFolderWithSavedModel, ParticularTable.NAME_OF_TABLE_WITH_VARIABLE_NAMES);
            Map<String, Double> preVariableMeans = TableUtils.readGivenColumnInDoubleTableAsMap(pathToFolderWithSavedModel.getChildPath(NAME_OF_TABLE_WITH_VARIABLE_MEANS), NAME_OF_COLUMN_WITH_VARIABLE_MEANS);
            double[] variableMeans = StreamEx.of(variableNamesInModel).mapToDouble(preVariableMeans::get).toArray();
            Map<String, Double> preCoefficients = TableUtils.readGivenColumnInDoubleTableAsMap(pathToFolderWithSavedModel.getChildPath(NAME_OF_TABLE_WITH_COEFFICIENTS), NAME_OF_COLUMN_WITH_REGRESSION_COEFFICIENTS);
            double[] regressionCoefficients = StreamEx.of(variableNamesInModel).mapToDouble(preCoefficients::get).toArray();
            return getPredictions(responseMean, variableMeans, regressionCoefficients, dataMatrix);
        }
    }
    /**************************** PrincipalComponentRegression : finish ********************/
    
    /**************************** RandomForestRegression : start **************************/
    public static class RandomForestRegression
    {
        public static double[] createAndWriteRegressionModelUsingR(String scriptToCreateAndWriteRegressionModel, double[][] dataMatrix, String[] variableNames, double[] response, DataElementPath pathToOutputs, String regressionModelFileName, boolean doPrintAccompaniedInformation, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            Object[] objects = Rutils.executeRscript(scriptToCreateAndWriteRegressionModel, new String[]{"dataMatrix", "response"}, new Object[]{dataMatrix, response}, new String[]{"predictedResponse", "importance", "importanceColumnNames"}, pathToOutputs, regressionModelFileName, null, null, log, jobControl, from, to);
            if( doPrintAccompaniedInformation )
            {
                double[][] importance = (double[][])objects[1];
                String[] importanceColumnNames = (String[])objects[2];
                if( importance != null && importanceColumnNames != null )
                    TableUtils.writeDoubleTable(importance, variableNames, importanceColumnNames, pathToOutputs, "variableImportances");
            }
            return (double[])objects[0];
        }
    }
    /******************************** RandomForestRegression : finish **************************/

    /******************************** SVMepsilonRegression : start **************************/
    public static class SVMepsilonRegression
    {
        public static double[] createAndWriteRegressionModelUsingR(String scriptToCreateAndWriteRegressionModel, double[][] dataMatrix, double[] response, DataElementPath pathToOutputs, String regressionModelFileName, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            return (double[])Rutils.executeRscript(scriptToCreateAndWriteRegressionModel, new String[]{"dataMatrix", "response"}, new Object[]{dataMatrix, response}, new String[]{"predictedResponse"}, pathToOutputs, regressionModelFileName, null, null, log, jobControl, from, to)[0];
        }
    }
    /******************************** SVMepsilonRegression : finish **************************/

    /******************************** ClusterizedRegression : start **************************/
    public static class ClusterizedRegression
    {
        private static int[] getNewIndicesOfClusters(double[][] dataMatrix, double[] response, int[] indicesOfClusters, Map<Integer, double[]> clusterIndexAndCoefficients)
        {
            int[] result = new int[dataMatrix.length];
            for( int i = 0; i < dataMatrix.length; i++ )
            {
                double minDifference =  Double.MAX_VALUE;
                result[i] = indicesOfClusters[i];
                for( Entry<Integer, double[]> entry : clusterIndexAndCoefficients.entrySet() )
                {
                    int clusterIndex = entry.getKey();
                    double[] coefficients = entry.getValue();
                    if( coefficients == null ) continue;
                    double prediction = MatrixUtils.getInnerProduct(dataMatrix[i], coefficients);
                    double difference = Math.abs(prediction - response[i]);
                    if( difference < minDifference )
                    {
                        minDifference = difference;
                        result[i] = clusterIndex;
                    }
                }
            }
            return result;
        }

        // this method implements the clusterized regression
        private static Object[] getClusterIndexAndCoefficientsAndNewIndicesOfClusters(double[][] dataMatrix, double[] response, int[] initialIndicesOfClusters, int maxNumberOfIterationsToInverseMatrix, double eps, int maxNumberOfClusterizationSteps)
        {
            Map<Integer, double[]> clusterIndexAndCoefficients = null;
            int numberOfClusterizationSteps = 0;
            int[] indicesOfClusters = initialIndicesOfClusters;
            while( true )
            {
                clusterIndexAndCoefficients = LSregressionInClusters.getClusterIndexAndCoefficients(maxNumberOfIterationsToInverseMatrix, eps, dataMatrix, response, indicesOfClusters);
                int[] newIndicesOfClusters = getNewIndicesOfClusters(dataMatrix, response, indicesOfClusters, clusterIndexAndCoefficients);
                int i = 0;
                for( ; i < indicesOfClusters.length; i++ )
                    if( indicesOfClusters[i] != newIndicesOfClusters[i] )
                    {
                        indicesOfClusters = newIndicesOfClusters;
                        break;
                    }
                if( i >= indicesOfClusters.length || ++numberOfClusterizationSteps >= maxNumberOfClusterizationSteps ) break;
            }
            return new Object[]{clusterIndexAndCoefficients, indicesOfClusters, numberOfClusterizationSteps};
        }

        // dataMatrix can contain the intercept column;
        // the structure of this method is very closed to method 'createAndWriteRegressionModel' in class 'LSregressionInClusters'
        public static double[] createAndWriteRegressionModel(String[] variableNames, double[][] dataMatrix, double[] response, int numberOfClusters, String dataTransformationType, boolean doAddResponseToClusterization, int maxNumberOfIterations, double eps, int maxNumberOfClusterizationSteps, boolean doPrintAccompaniedInformation, DataElementPath pathToOutputs, String NameOfTableForTrueClassificationRates, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            int[] indicesOfClusters = LSregressionInClusters.getIndicesOfClustersByClusterization(dataMatrix, variableNames, response, numberOfClusters, dataTransformationType, doAddResponseToClusterization);
            if( jobControl != null )
                jobControl.setPreparedness(from + (to - from) / 2);
            indicesOfClusters = Classification.removeEmptyClasses(indicesOfClusters);
            Object[] objects = getClusterIndexAndCoefficientsAndNewIndicesOfClusters(dataMatrix, response, indicesOfClusters, maxNumberOfIterations, eps, maxNumberOfClusterizationSteps);
            Map<Integer, double[]> clusterIndexAndCoefficients = (Map<Integer, double[]>)objects[0];
            indicesOfClusters = (int[])objects[1];
            LSregressionInClusters.writeTableWithCoefficients(clusterIndexAndCoefficients, variableNames, pathToOutputs, LSregressionInClusters.NAME_OF_TABLE_FOR_RESPONSE_PREDICTION);
            Object[] objs = ClassificationByMultivariateRegressionOfIndicatorMatrix.createClassificationModelAndPredictIndicesOfClasses(dataMatrix, indicesOfClusters, maxNumberOfIterations, eps);
            double[][] coefficientMatrixForClassPrediction = (double[][])objs[1];
            Map<Integer, double[]> map = new HashMap<>();
            for( int i = 0; i < coefficientMatrixForClassPrediction[0].length; i++ )
                map.put(i, MatrixUtils.getColumn(coefficientMatrixForClassPrediction, i));
            LSregressionInClusters.writeTableWithCoefficients(map, variableNames, pathToOutputs, LSregressionInClusters.NAME_OF_TABLE_FOR_CLUSTER_PREDICTION);
            if( doPrintAccompaniedInformation )
            {
                // int[] predictedIndicesOfClusters = Classification.predictIndicesOfClasses(coefficientMatrixForClassPrediction, dataMatrix);
                int[] predictedIndicesOfClusters = (int[])objs[0];
                Classification.writeTrueClassificationRatesIntoTable(indicesOfClusters, predictedIndicesOfClusters, null, "cluster", pathToOutputs, NameOfTableForTrueClassificationRates);
                // TODO: to remove this output ???
                for( int i = 0; i < indicesOfClusters.length; i++ )
                    log.info("i = " + i + " indicesOfClusters[i] = " + indicesOfClusters[i] + " predictedIndicesOfClusters[i] = " + predictedIndicesOfClusters[i]);
                int numberOfClusterizationSteps = (int)objects[2];
                log.info("number of clusterization steps = " + numberOfClusterizationSteps);
            }
            if( jobControl != null )
                jobControl.setPreparedness(to);
            return LSregressionInClusters.getPredictionOfResponse(dataMatrix, coefficientMatrixForClassPrediction, clusterIndexAndCoefficients);
        }
    }
    /***************************** ClusterizedRegression : finish ****************************/

    /***************************** LSregressionInClusters : start ****************************/
    public static class LSregressionInClusters
    {
        public static final String NAME_OF_TABLE_FOR_RESPONSE_PREDICTION = "regressionCoefficientsInClusters";
        public static final String NAME_OF_TABLE_FOR_CLUSTER_PREDICTION = "coefficientMatrixForClusterPrediction";

        // dataMatrix can contain the intercept column
        public static double[] createAndWriteRegressionModel(String[] variableNames, double[][] dataMatrix, double[] response, int numberOfClusters, String dataTransformationType, boolean doAddResponseToClusterization, int maxNumberOfIterations, double eps, boolean doPrintAccompaniedInformation, DataElementPath pathToOutputs, String tableName, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            int[] indicesOfClusters = getIndicesOfClustersByClusterization(dataMatrix, variableNames, response, numberOfClusters, dataTransformationType, doAddResponseToClusterization);
            if( jobControl != null )
                jobControl.setPreparedness(from + (to - from) / 2);
            indicesOfClusters = Classification.removeEmptyClasses(indicesOfClusters);
            Map<Integer, double[]> clusterIndexAndCoefficients = getClusterIndexAndCoefficients(maxNumberOfIterations, eps, dataMatrix, response, indicesOfClusters);
            writeTableWithCoefficients(clusterIndexAndCoefficients, variableNames, pathToOutputs, NAME_OF_TABLE_FOR_RESPONSE_PREDICTION);
            Object[] objects = ClassificationByMultivariateRegressionOfIndicatorMatrix.createClassificationModelAndPredictIndicesOfClasses(dataMatrix, indicesOfClusters, maxNumberOfIterations, eps);
            double[][] coefficientMatrixForClassPrediction = (double[][])objects[1];
            Map<Integer, double[]> map = new HashMap<>();
            for( int i = 0; i < coefficientMatrixForClassPrediction[0].length; i++ )
                map.put(i, MatrixUtils.getColumn(coefficientMatrixForClassPrediction, i));
            writeTableWithCoefficients(map, variableNames, pathToOutputs, NAME_OF_TABLE_FOR_CLUSTER_PREDICTION);
            if( doPrintAccompaniedInformation )
            {
                int[] predictedIndicesOfClusters = (int[])objects[0];
                Classification.writeTrueClassificationRatesIntoTable(indicesOfClusters, predictedIndicesOfClusters, null, "cluster", pathToOutputs, tableName);
                // TODO: to remove this output ???
                for( int i = 0; i < indicesOfClusters.length; i++ )
                    log.info("i = " + i + " indicesOfClusters[i] = " + indicesOfClusters[i] + " predictedIndicesOfClusters[i] = " + predictedIndicesOfClusters[i]);
            }
            if( jobControl != null )
                jobControl.setPreparedness(to);
            return getPredictionOfResponse(dataMatrix, coefficientMatrixForClassPrediction, clusterIndexAndCoefficients);
        }
        
        public static double[] readRegressionModelAndPredictResponse(DataElementPath pathToFolderWithSavedModel, String[] variableNames, double[][] dataMatrix, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            Map<Integer, double[]> clusterIndexAndCoefficients = readClusterIndexAndCoefficientsInTable(pathToFolderWithSavedModel, NAME_OF_TABLE_FOR_RESPONSE_PREDICTION, variableNames);
            Map<Integer, double[]> mapForCoefficientMatrix = readClusterIndexAndCoefficientsInTable(pathToFolderWithSavedModel, NAME_OF_TABLE_FOR_CLUSTER_PREDICTION, variableNames);
            if( clusterIndexAndCoefficients == null || mapForCoefficientMatrix == null ) return null;
            double[][] coefficientMatrixForClassPrediction = transformMapToMatrix(mapForCoefficientMatrix);
            if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 2);
            double[] predictedResponse = getPredictionOfResponse(dataMatrix, coefficientMatrixForClassPrediction, clusterIndexAndCoefficients);
            if( jobControl != null ) jobControl.setPreparedness(to);
            return predictedResponse;
        }
        
        private static double[][] transformMapToMatrix(Map<Integer, double[]> mapForCoefficientMatrix)
        {
            int n = mapForCoefficientMatrix.get(0).length, m = mapForCoefficientMatrix.size();
            double[][] matrix = new double[n][m];
            for( int j = 0; j < m; j++ )
            {
                double[] vector = mapForCoefficientMatrix.get(j);
                for( int i = 0; i < n; i++ )
                    matrix[i][j] = vector[i];
            }
            return matrix;
        }
        
        private static Map<Integer, double[]> readClusterIndexAndCoefficientsInTable(DataElementPath pathToFolderWithSavedModel, String tableName, String[] variableNames)
        {
            Map<Integer, double[]> clusterIndexAndCoefficients = new HashMap<>();
            Object[] objects = TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(tableName));
            String[] rowNames = (String[])objects[0];
            String[] columnNames = (String[])objects[1];
            if( rowNames.length != variableNames.length ) return null;
            double[][] matrix = (double[][])objects[2];
            for( String columnName : columnNames )
            {
                int clusterIndex = Integer.parseInt(columnName.split("_")[1]);
                double[] coefficients = new double[variableNames.length];
                for( int i = 0; i < variableNames.length; i++ )
                {
                    int index = ArrayUtils.indexOf(rowNames, variableNames[i]);
                    if( index < 0 ) return null;
                    coefficients[i] = matrix[index][clusterIndex];
                }
                clusterIndexAndCoefficients.put(clusterIndex, coefficients);
            }
            return clusterIndexAndCoefficients;
        }

        private static double[] getPredictionOfResponse(double[][] dataMatrix, double[][] coefficientMatrixForClassPrediction, Map<Integer, double[]> clusterIndexAndCoefficients)
        {
            double[] predictedResponse = new double[dataMatrix.length];
            for( int i = 0; i < dataMatrix.length; i++ )
                predictedResponse[i] = getPredictionOfResponse(dataMatrix[i], coefficientMatrixForClassPrediction, clusterIndexAndCoefficients);
            return predictedResponse;
        }

        private static double getPredictionOfResponse(double[] rowOfDataMatrix, double[][] coefficientMatrixForClassPrediction, Map<Integer, double[]> clusterIndexAndCoefficients)
        {
            int predictedIndexOfClass = ClassificationByMultivariateRegressionOfIndicatorMatrix.predictIndexOfClass(rowOfDataMatrix, coefficientMatrixForClassPrediction);
            return MatrixUtils.getInnerProduct(rowOfDataMatrix, clusterIndexAndCoefficients.get(predictedIndexOfClass));
        }

        private static Object[] getDataMatrixAndResponseForCluster(int clusterIndex, double[][] dataMatrix, double[] response, int[] indicesOfClusters)
        {
            int size = 0;
            for( int index : indicesOfClusters )
                if( index == clusterIndex )
                    size++;
            if( size == 0 ) return null;
            double[][] clusterDataMatrix = new double[size][];
            double[] clusterResponse = new double[size];
            int index = 0;
            for( int i = 0; i < indicesOfClusters.length; i++ )
                if( clusterIndex == indicesOfClusters[i])
                {
                    clusterDataMatrix[index] = dataMatrix[i];
                    clusterResponse[index++] = response[i];
                }
            return new Object[]{clusterDataMatrix, clusterResponse};
        }
        
        public static Map<Integer, double[]> getClusterIndexAndCoefficients(int maxNumberOfIterations, double eps, double[][] dataMatrix, double[] response, int[] indicesOfClusters)
        {
            Map<Integer, double[]> result = new HashMap<>();
            Set<Integer> distinctClusterIndices = Clusterization.getDistinctIndicesOfClusters(indicesOfClusters);
            for( int clusterIndex : distinctClusterIndices )
            {
                Object[] objects = getDataMatrixAndResponseForCluster(clusterIndex, dataMatrix, response, indicesOfClusters);
                if( objects == null || ((double[][])objects[0]).length <= ((double[][])objects[0])[0].length ) return null;
                LSregression lsr = new LSregression(null, null, (double[][])objects[0], null, (double[])objects[1]);
                double[] coefficients = (double[])lsr.getMultipleLinearRegressionByJacobiMethod(maxNumberOfIterations, eps, false)[0];
                result.put(clusterIndex, coefficients);
            }
            return result;
        }
        
        public static int[] getIndicesOfClustersByClusterization(double[][] dataMatrix, String[] variableNames, double[] response, int numberOfClusters, String transformationType, boolean doAddResponseToClusterization) throws Exception
        {
            double[][] matrixForClusterization = MatrixUtils.getClone(dataMatrix);
            MatrixUtils.removeGivenColumn(matrixForClusterization, variableNames, INTERCEPT);
            if( doAddResponseToClusterization )
                MatrixUtils.addColumnToMatrix(matrixForClusterization, response);
            matrixForClusterization = Transformation.transformData(matrixForClusterization, transformationType);
            KMeansAlgorithm kMeansAlgorithm = new KMeansAlgorithm(matrixForClusterization, null, numberOfClusters, Distance.EUCLIDEAN);
            kMeansAlgorithm.implementKmeansAlgorithm();
            return kMeansAlgorithm.getIndicesOfClusters();
        }
        
        public static void writeTableWithCoefficients(Map<Integer, double[]> clusterIndexAndCoefficients, String[] variableNames, DataElementPath pathToOutputs, String tableName)
        {
            int m = variableNames.length,  k = clusterIndexAndCoefficients.size(), index = 0;
            double[][] coefficients = new double[m][k];
            String[] columnNames = new String[k];
            for( Entry<Integer, double[]> entry : clusterIndexAndCoefficients.entrySet() )
            {
                double[] coeff = entry.getValue();
                for( int i = 0; i < m; i++ )
                    coefficients[i][index] = coeff[i];
                columnNames[index++] = Clusterization.CLUSTER + Integer.toString(entry.getKey());
            }
            TableUtils.writeDoubleTable(coefficients, variableNames, columnNames, pathToOutputs, tableName);
        }
    }
    /********************************** LSregressionInClusters : finish ************************/
    
    //temp
    private static Logger log = Logger.getLogger(LinearRegression.class.getName());
}
