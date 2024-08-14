
package biouml.plugins.bindingregions.statistics;

import java.util.ArrayList;
import java.util.List;

import one.util.streamex.IntStreamEx;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.RegressionOrClassificationModesEditor;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.VariableNamesSelector;
import biouml.plugins.bindingregions.statisticsutils.RegressionEngine;
import biouml.plugins.bindingregions.utils.LinearRegression;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.LinearRegression.MultivariateLinearRegression;
import biouml.plugins.bindingregions.utils.MultivariateSample;
import biouml.plugins.bindingregions.utils.StatUtil;
import biouml.plugins.bindingregions.utils.TableUtils;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author yura
 *
 */
public class MultivariateRegressionAnalysis extends AnalysisMethodSupport<MultivariateRegressionAnalysis.MultivariateRegressionAnalysisParameters>
{
    public MultivariateRegressionAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new MultivariateRegressionAnalysisParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Multivariate regression analysis. There are 3 modes :");
        log.info("1. Create and write regression model");
        log.info("2. Read regression model and predict responses");
        log.info("3. Cross-validate regression model");
        String regressionMode = parameters.getRegressionMode();
        DataElementPath pathToTableWithDataMatrix = parameters.getPathToTableWithDataMatrix();
        String[] variableNames = parameters.getVariableNames();
        String[] responseNames = parameters.getResponseNames();
        DataElementPath pathToFolderWithSavedModel = parameters.getPathToFolderWithSavedModel();
        int percentageOfDataForTraining = parameters.getPercentageOfDataForTraining();
        DataElementPath pathToOutputs = parameters.getOutputPath();

        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        implementMultivariateRegressionAnalysis(regressionMode, pathToTableWithDataMatrix, variableNames, responseNames, pathToFolderWithSavedModel, percentageOfDataForTraining, pathToOutputs, jobControl, 0, 100);
        return pathToOutputs.getDataCollection();
    }
    
    private void implementMultivariateRegressionAnalysis(String regressionMode, DataElementPath pathToTableWithDataMatrix, String[] variableNames, String[] responseNames, DataElementPath pathToFolderWithSavedModel, int percentageOfDataForTraining, DataElementPath pathToOutputs, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int maxNumberOfIterations = MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE;
        double eps = MatrixUtils.DEFAULT_EPS_FOR_INVERSE;
        Object[] objects;
        MultivariateLinearRegression mlr;
        double[][] coefficients, predictedResponses;
        switch( regressionMode )
        {
            case RegressionEngine.CREATE_AND_WRITE_MODE : log.info("Read data matrix and responses in table");
                                                          objects = readDataSubMatrixAndResponses(pathToTableWithDataMatrix, variableNames, responseNames);
                                                          variableNames = LinearRegression.addInterceptToRegression(variableNames, (double[][])objects[1]);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 4);
                                                          log.info("Create and write regression model");
                                                          mlr = new MultivariateLinearRegression((double[][])objects[1], (double[][])objects[2]);
                                                          coefficients = mlr.getCoefficients(maxNumberOfIterations, eps);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + 3 * (to - from) / 4);
                                                          MultivariateLinearRegression.writeRegressionModel(coefficients, variableNames, responseNames, pathToOutputs, LinearRegression.NAME_OF_TABLE_WITH_COEFFICIENTS);
                                                          predictedResponses = MatrixUtils.getProductOfRectangularMatrices((double[][])objects[1], coefficients);
                                                          log.info("Write summary on regression model");
                                                          writeTableWithSummaryOnModelAccuracy((double[][])objects[2], predictedResponses, responseNames, variableNames.length, pathToOutputs, "summaryOnModelAccuracy");
                                                          break;
            case RegressionEngine.READ_AND_PREDICT_MODE : log.info("Read data matrix and regression model and predict the responses");
                                                          objects = MultivariateLinearRegression.readRegressionModel(pathToFolderWithSavedModel, LinearRegression.NAME_OF_TABLE_WITH_COEFFICIENTS);
                                                          String[] modelVariableNames = (String[])objects[0], modelResponseNames = (String[])objects[1];
                                                          double[][] modelCoefficients = (double[][])objects[2];
                                                          Object[] objs = TableUtils.readDataSubMatrix(pathToTableWithDataMatrix, modelVariableNames);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 2);
                                                          predictedResponses = MatrixUtils.getProductOfRectangularMatrices((double[][])objs[1], modelCoefficients);
                                                          TableUtils.writeDoubleTable(predictedResponses, (String[])objs[0], modelResponseNames, pathToOutputs, "predictions");
                                                          break;
            case RegressionEngine.CROSS_VALIDATION_MODE : log.info("Read data matrix and responses in table");
                                                          objects = readDataSubMatrixAndResponses(pathToTableWithDataMatrix, variableNames, responseNames);
                                                          variableNames = LinearRegression.addInterceptToRegression(variableNames, (double[][])objects[1]);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 4);
                                                          log.info("Cross-validation: create regression model on trainig data set and assess it on test data set");
                                                          objs = splitData((double[][])objects[1], (double[][])objects[2], percentageOfDataForTraining);
                                                          log.info("Treatment of trainig data");
                                                          mlr = new MultivariateLinearRegression((double[][])objs[0], (double[][])objs[1]);
                                                          coefficients = mlr.getCoefficients(maxNumberOfIterations, eps);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 2);
                                                          double[][] responsesPredictedInTrain = MatrixUtils.getProductOfRectangularMatrices((double[][])objs[0], coefficients);
                                                          double[] valuesFromTrain = getMeanSummaryOnModelAccuracy((double[][])objs[1], responsesPredictedInTrain, variableNames.length);
                                                          log.info("Treatment of test data");
                                                          mlr = new MultivariateLinearRegression((double[][])objs[2], (double[][])objs[3]);
                                                          coefficients = mlr.getCoefficients(maxNumberOfIterations, eps);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + 3 * (to - from) / 4);
                                                          double[][] responsePredictedInTest = MatrixUtils.getProductOfRectangularMatrices((double[][])objs[2], coefficients);
                                                          double[] valuesFromTest = getMeanSummaryOnModelAccuracy((double[][])objs[3], responsePredictedInTest, variableNames.length);
                                                          double[][] data = {valuesFromTrain, valuesFromTest};
                                                          TableUtils.writeDoubleTable(MatrixUtils.getTransposedMatrix(data), new String[]{"Pearson correlation between observations and predictions", "Spearman correlation between observations and predictions", "Mean of observations", "Variance of observations", "Explained variance (in %)", "Number of observations"}, new String[]{"Mean values on training set", "Mean values on test set"}, pathToOutputs, "crossValidation");
                                                          break;
            default                                     : throw new Exception("This mode '" + regressionMode + "' is not supported in our regression analysis currently");
        }
        if( jobControl != null ) jobControl.setPreparedness(to);
    }
    
    private double[][] getSummaryOnModelAccuracy(double[][] responses, double[][] predictedResponses, int numberOfVariables) throws Exception
    {
        double[][] result = new double[responses[0].length][];
        double[][] transposedResponses = MatrixUtils.getTransposedMatrix(responses);
        double[][] transposedPredictedResponses = MatrixUtils.getTransposedMatrix(predictedResponses);
        for( int i = 0; i < responses[0].length; i++ )
            result[i] = LinearRegression.getSummaryOnModelAccuracy(transposedResponses[i], transposedPredictedResponses[i], numberOfVariables);
        double[] meanValues = MultivariateSample.getMeanVector(result);
        result = MatrixUtils.getTransposedMatrix(result);
        MatrixUtils.addColumnToMatrix(result, meanValues);
        return result;
    }
    
    private double[] getMeanSummaryOnModelAccuracy(double[][] responses, double[][] predictedResponses, int numberOfVariables) throws Exception
    {
        double[][] individualValues = new double[responses[0].length][];
        double[][] transposedResponses = MatrixUtils.getTransposedMatrix(responses);
        double[][] transposedPredictedResponses = MatrixUtils.getTransposedMatrix(predictedResponses);
        for( int i = 0; i < responses[0].length; i++ )
            individualValues[i] = LinearRegression.getSummaryOnModelAccuracy(transposedResponses[i], transposedPredictedResponses[i], numberOfVariables);
        return MultivariateSample.getMeanVector(individualValues);
    }
    
    private void writeTableWithSummaryOnModelAccuracy(double[][] responses, double[][] predictedResponses, String[] responseNames, int numberOfVariables, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        double[][] valuesForTable = getSummaryOnModelAccuracy(responses, predictedResponses, numberOfVariables);
        String[] namesOfColumns = (String[])ArrayUtils.add(responseNames, responseNames.length, " Mean values");
        TableUtils.writeDoubleTable(valuesForTable, new String[]{"Pearson correlation between observations and predictions", "Spearman correlation between observations and predictions", "Mean of observations", "Variance of observations", "Explained variance (in %)", "Number of observations"}, namesOfColumns, pathToOutputs, tableName);
    }
    
    private Object[] splitData(double[][] dataMatrix, double[][] responses, int percentageOfDataForTraining)
    {
        int sizeForTrain = dataMatrix.length * percentageOfDataForTraining / 100, sizeForTest = dataMatrix.length - sizeForTrain;
        int[] indices = IntStreamEx.ofIndices(dataMatrix).toArray();
        StatUtil.shuffleVector(indices, 0);
        double[][] dataMatrixForTrain = new double[sizeForTrain][], dataMatrixForTest = new double[sizeForTest][];
        double[][] responsesForTrain = new double[sizeForTrain][], responsesForTest = new double[sizeForTest][];
        for( int i = 0; i < sizeForTrain; i++ )
        {
            dataMatrixForTrain[i] = dataMatrix[indices[i]];
            responsesForTrain[i] = responses[indices[i]];
        }
        for( int i = 0; i < sizeForTest; i++ )
        {
            dataMatrixForTest[i] = dataMatrix[indices[i + sizeForTrain]];
            responsesForTest[i] = responses[indices[i + sizeForTrain]];
        }
        return new Object[]{dataMatrixForTrain, responsesForTrain, dataMatrixForTest, responsesForTest};
    }
    
    /***
     * 
     * @param pathToTableWithDataMatrix
     * @param variableNames
     * @param responseNames
     * @return Object[] array; array[0] = String[] objectNames; array[1] = double[][] dataMatrix; array[2] = double[][] responses;
     */
    private static Object[] readDataSubMatrixAndResponses(DataElementPath pathToTableWithDataMatrix, String[] variableNames, String[] responseNames)
    {
        Object[] objs = TableUtils.readDataSubMatrix(pathToTableWithDataMatrix, responseNames);
        Object[] objects = TableUtils.readDataSubMatrix(pathToTableWithDataMatrix, variableNames);
        return removeObjectsWithMissingData((String[])objects[0], (double[][])objects[1], (double[][])objs[1]);
    }
    
    /***
     * remove all objects that contain missing data (NaN-values);
     * @param objectNames
     * @param dataMatrix
     * @param responses
     * @return Object[] array; array[0] = String[] newObjectNames; array[1] = double[][] newDataMatrix; array[2] = double[][] newResponses;
     */
    private static Object[] removeObjectsWithMissingData(String[] objectNames, double[][] dataMatrix, double[][] responses)
    {
        List<String> newObjectNames = new ArrayList<>();
        List<double[]> newDataMatrix = new ArrayList<>();
        List<double[]> newResponses = new ArrayList<>();
        for( int i = 0; i < objectNames.length; i++ )
            if( ! MatrixUtils.doContainNaN(dataMatrix[i]) && ! MatrixUtils.doContainNaN(responses[i]) )
            {
                newObjectNames.add(objectNames[i]);
                newDataMatrix.add(dataMatrix[i]);
                newResponses.add(responses[i]);
            }
        if( newObjectNames.isEmpty() ) return null;
        return new Object[]{newObjectNames.toArray(new String[0]), newDataMatrix.toArray(new double[newDataMatrix.size()][]), newResponses.toArray(new double[newResponses.size()][])};
    }

    public static class MultivariateRegressionAnalysisParameters extends AbstractStatisticalAnalysisParameters
    {}
    
    public static class MultivariateRegressionAnalysisParametersBeanInfo extends BeanInfoEx2<MultivariateRegressionAnalysisParameters>
    {
        public MultivariateRegressionAnalysisParametersBeanInfo()
        {
            super(MultivariateRegressionAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("regressionMode", beanClass), RegressionOrClassificationModesEditor.class);
            add(DataElementPathEditor.registerInput("pathToTableWithDataMatrix", beanClass, TableDataCollection.class));
            addHidden("variableNames", VariableNamesSelector.class, "areVariableNamesForRegressionHidden");
            addHidden("responseNames", VariableNamesSelector.class, "areResponseNamesHidden");
            addHidden(DataElementPathEditor.registerInput("pathToFolderWithSavedModel", beanClass, FolderCollection.class), "isPathToFolderWithSavedRegressionModelHidden");
            addHidden("percentageOfDataForTraining", "isPercentageOfDataForTrainingHidden");
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class, false));
        }
    }
}
