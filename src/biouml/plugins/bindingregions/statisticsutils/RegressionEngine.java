
package biouml.plugins.bindingregions.statisticsutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import one.util.streamex.IntStreamEx;

import org.apache.commons.lang.ArrayUtils;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.graphics.chart.Chart;
import biouml.plugins.bindingregions.rscript.RHelper;
import biouml.plugins.bindingregions.rscript.Rutils;
import biouml.plugins.bindingregions.utils.DataMatrix;
import biouml.plugins.bindingregions.utils.LinearRegression;
import biouml.plugins.bindingregions.utils.LinearRegression.ClusterizedRegression;
import biouml.plugins.bindingregions.utils.LinearRegression.LSregression;
import biouml.plugins.bindingregions.utils.LinearRegression.LSregressionInClusters;
import biouml.plugins.bindingregions.utils.LinearRegression.PrincipalComponentRegression;
import biouml.plugins.bindingregions.utils.LinearRegression.RandomForestRegression;
import biouml.plugins.bindingregions.utils.LinearRegression.SVMepsilonRegression;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.StatUtil;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TableUtils.ParticularTable;

/**
 * @author yura
 * Important note : it is assumed that the input response and data matrix are located in same table (TableDataCollection) !!!
 *
 */
public class RegressionEngine
{
    public static final String REGRESSION_1_LS = "LS-regression";
    public static final String REGRESSION_2_RF = "Random forest regression";
    public static final String REGRESSION_3_SVM_EPSILON = "SVM epsilon regression";
    public static final String REGRESSION_4_LS_IN_CLUSTERS = "LS-regressions in clusters";
    public static final String REGRESSION_5_LS_CLUSTERIZED = "Clusterized LS-regression";
    public static final String REGRESSION_6_PC = "Principal component regression";
    
    private static final String NAME_OF_FILE_WITH_RANDOM_FOREST_REGRESSION_MODEL = "randomForestRegression.model";
    private static final String NAME_OF_FILE_WITH_SVM_EPSILON_REGRESSION_MODEL = "SVMepsilonRegression.model";
    public static final String NAME_OF_TABLE_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME = "regressionTypeAndResponseName";
    
    public static final String CREATE_AND_WRITE_MODE = "Create model and write it";
    public static final String READ_AND_PREDICT_MODE = "Read model and predict";
    public static final String CROSS_VALIDATION_MODE = "Cross-validation of model";
    
    private final String regressionType;
    private final String[] objectNames;   // dim(objectNames) = n;
    private final String[] variableNames; // dim(variableNames) = m;
    private final double[][] dataMatrix;  // dim(dataMatrix) = n x m;
    private final String responseName;
    private double[] response;      // dim(response) = n;
    
    // Constructor for creation or cross-validation of regression model
    public RegressionEngine(String regressionType, String[] objectNames, String[] variableNames, double[][] dataMatrix, String responseName, double[] response)
    {
        this.regressionType = regressionType;
        this.objectNames = objectNames;
        this.variableNames = variableNames;
        this.dataMatrix = dataMatrix;
        this.responseName = responseName;
        this.response = response;
    }

    // Constructor for loading the saved regression model and predicting response
    // Important note : it is assumed that the input response and data matrix are located in same table or file !!!
    public RegressionEngine(DataElementPath pathToFolderWithSavedModel, DataElementPath pathToMatrix) throws Exception
    {
        String[] regressionTypeAndResponseName = readRegressionTypeAndResponseNameInTable(pathToFolderWithSavedModel, NAME_OF_TABLE_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME);
        this.regressionType = regressionTypeAndResponseName[0];
        this.responseName = regressionTypeAndResponseName[1];
        String[] variableNamesInModel = ParticularTable.readVariableNames(pathToFolderWithSavedModel, ParticularTable.NAME_OF_TABLE_WITH_VARIABLE_NAMES);
        this.variableNames = variableNamesInModel;
        
//        Object[] objects = TableUtils.readDataSubMatrix(pathToMatrix, variableNamesInModel);
//        objects = MatrixUtils.removeRowsWithMissingData((String[])objects[0], (double[][])objects[1]);
        Object[] objects = DataMatrix.readDoubleMatrixOrSubmatrix(pathToMatrix, variableNamesInModel);
        objects = MatrixUtils.removeRowsWithMissingData((String[])objects[0], (double[][])objects[2]);
        
        this.objectNames = (String[])objects[0];
        this.dataMatrix = (double[][])objects[1];
    }
    
    public static String[] getAvailableRegressionTypes()
    {
        return new String[]{REGRESSION_1_LS, REGRESSION_2_RF, REGRESSION_3_SVM_EPSILON, REGRESSION_6_PC, REGRESSION_4_LS_IN_CLUSTERS, REGRESSION_5_LS_CLUSTERIZED};
    }
    
    public static String[] getAvailableModes()
    {
        return new String[]{CREATE_AND_WRITE_MODE, READ_AND_PREDICT_MODE, CROSS_VALIDATION_MODE};
    }

    public double[] createAndWriteRegressionModel(DataElementPath pathToOutputs, Logger log, int maxNumberOfIterations, double eps, int maxNumberOfClusterizationSteps, boolean doPrintAccompaniedInformation, int numberOfClusters, String dataTransformationType, int numberOfPrincipalComponents, String principalComponentSortingType, boolean doAddResponseToClusterization, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        double[] predictedResponse = null;
        writeTableWithRegressionTypeAndResponseName(pathToOutputs, NAME_OF_TABLE_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME);
        ParticularTable.writeTableWithVariableNames(variableNames, pathToOutputs, ParticularTable.NAME_OF_TABLE_WITH_VARIABLE_NAMES);
        switch( regressionType )
        {
            case REGRESSION_1_LS             : LSregression lsr = new LSregression(variableNames, objectNames, dataMatrix, responseName, response);
                                               Object[] objects = lsr.getMultipleLinearRegressionByJacobiMethod(maxNumberOfIterations, eps, true);
                                               predictedResponse = (double[])objects[2];
                                               lsr.writeTableWithCoefficients((double[])objects[0], (double[])objects[4], (double[])objects[5], pathToOutputs, LinearRegression.NAME_OF_TABLE_WITH_COEFFICIENTS); break;
            case REGRESSION_2_RF             : String scriptToCreateAndWriteRandomForestRegression = RHelper.getScript("RegressionAnalysis", "CreateModelForRandomForestRegression");
                                               double[] proposedPrediction = RandomForestRegression.createAndWriteRegressionModelUsingR(scriptToCreateAndWriteRandomForestRegression, dataMatrix, variableNames, response, pathToOutputs, NAME_OF_FILE_WITH_RANDOM_FOREST_REGRESSION_MODEL, doPrintAccompaniedInformation, log, jobControl, from, from + (to - from) / 2);
                                               if( doPrintAccompaniedInformation )
                                                   LinearRegression.writeTableWithSummaryOnModelAccuracy(response, proposedPrediction, dataMatrix[0].length, pathToOutputs, "summaryOnModelAccuracy_proposedPrediction");
                                               String scriptToReadModelForRandomForestRegressionAndPredict = RHelper.getScript("RegressionAnalysis", "ReadModelForRandomForestRegressionAndPredict");
                                               predictedResponse = Rutils.readRegressionModelAndPredictResponseUsingR(dataMatrix, scriptToReadModelForRandomForestRegressionAndPredict, pathToOutputs, NAME_OF_FILE_WITH_RANDOM_FOREST_REGRESSION_MODEL, log, jobControl, from + (to - from) / 2, to); break;
            case REGRESSION_3_SVM_EPSILON    : String scriptToCreateAndWriteSVMepsilonRegression = RHelper.getScript("RegressionAnalysis", "CreateModelForSVMepsilonRegression");
                                               predictedResponse = SVMepsilonRegression.createAndWriteRegressionModelUsingR(scriptToCreateAndWriteSVMepsilonRegression, dataMatrix, response, pathToOutputs, NAME_OF_FILE_WITH_SVM_EPSILON_REGRESSION_MODEL, log, jobControl, from, to); break;
            case REGRESSION_4_LS_IN_CLUSTERS : predictedResponse = LSregressionInClusters.createAndWriteRegressionModel(variableNames, dataMatrix, response, numberOfClusters, dataTransformationType, doAddResponseToClusterization, maxNumberOfIterations, eps, doPrintAccompaniedInformation, pathToOutputs, "summaryOnClusterPrediction", jobControl, from, to); break;
            case REGRESSION_5_LS_CLUSTERIZED : predictedResponse = ClusterizedRegression.createAndWriteRegressionModel(variableNames, dataMatrix, response, numberOfClusters, dataTransformationType, doAddResponseToClusterization, maxNumberOfIterations, eps, maxNumberOfClusterizationSteps, doPrintAccompaniedInformation, pathToOutputs, "summaryOnClusterPrediction", jobControl, from, to); break;
            case REGRESSION_6_PC             : PrincipalComponentRegression pcr = new PrincipalComponentRegression(variableNames, dataMatrix, response, maxNumberOfIterations, eps, principalComponentSortingType);
                                               if( jobControl != null ) jobControl.setPreparedness(from + 3 * (to - from) / 4);
                                               predictedResponse = pcr.createAndWriteRegressionModel(dataMatrix, numberOfPrincipalComponents, pathToOutputs);
                                               if( doPrintAccompaniedInformation )
                                                   pcr.writeTableWithPCcoefficients(pathToOutputs, "principalComponentCoefficients");
                                               if( jobControl != null ) jobControl.setPreparedness(to); break;
            default                          : throw new Exception("This regression type '" + regressionType + "' is not supported in our regression analysis currently");
        }
        if( doPrintAccompaniedInformation )
        {
            LinearRegression.writeTableWithSummaryOnModelAccuracy(response, predictedResponse, dataMatrix[0].length, pathToOutputs, "summaryOnModelAccuracy");
            writePredictions(responseName, response, predictedResponse, objectNames, pathToOutputs);
            Chart chart = LSregression.createChartWithLineAndCloud(response, predictedResponse, null, null, null, null, "Observations", "Predictions");
            TableUtils.addChartToTable(TableUtils.CHART, chart, pathToOutputs.getChildPath("chart_observationsAndPredictions"));
            writeChartWithRegressionWhenVariableIsSingle(responseName, response, predictedResponse, dataMatrix, variableNames, pathToOutputs);
        }
        return predictedResponse;
    }
    
    private static void writePredictions(String responseName, double[] response, double[] predictedResponse, String[] objectNames, DataElementPath pathToOutputs)
    {
        String[] namesOfColumns = new String[]{responseName, responseName + "_predicted", "difference_between_" + responseName + "_and_" + responseName + "_predicted", "abs_difference_between_" + responseName + "_and_" + responseName + "_predicted"};
        double[][] matrix = new double[response.length][];
        for(int i = 0; i < response.length; i ++ )
            matrix[i] = new double[]{response[i], predictedResponse[i], response[i] - predictedResponse[i], Math.abs(response[i] - predictedResponse[i])};
        TableUtils.writeDoubleTable(matrix, objectNames, namesOfColumns, pathToOutputs, "response_predicted");
    }

    private static void writeChartWithRegressionWhenVariableIsSingle(String responseName, double[] response, double[] predictedResponse, double[][] dataMatrix, String[] variableNames, DataElementPath pathToOutputs)
    {
        int variableNumber = variableNames.length;
        if( ArrayUtils.contains(variableNames, LinearRegression.INTERCEPT) )
            variableNumber--;
        if( variableNumber != 1 ) return;
        int columnIndex = variableNames[0].equals(LinearRegression.INTERCEPT) ? 1 : 0;
        double[] xValues = MatrixUtils.getColumn(dataMatrix, columnIndex);
        Chart chart = TableUtils.createChart(xValues, predictedResponse, null, xValues, response, null, null, null, null, null, variableNames[columnIndex], responseName);
        TableUtils.addChartToTable(TableUtils.CHART, chart, pathToOutputs.getChildPath("chart_regressionCurve"));
    }
    
    public double[] readRegressionModelAndPredictResponse(DataElementPath pathToFolderWithSavedModel, DataElementPath pathToOutputs, Logger log, boolean doWritePrediction, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        double[] predictedResponse = null;
        switch( regressionType )
        {
            case REGRESSION_1_LS             : if( jobControl != null ) jobControl.setPreparedness(from);
                                               predictedResponse = LSregression.readRegressionModelAndPredictResponse(pathToFolderWithSavedModel, dataMatrix); break;
            case REGRESSION_2_RF             : String scriptToReadModelForRandomForestRegressionAndPredict = RHelper.getScript("RegressionAnalysis", "ReadModelForRandomForestRegressionAndPredict");
                                               predictedResponse = Rutils.readRegressionModelAndPredictResponseUsingR(dataMatrix, scriptToReadModelForRandomForestRegressionAndPredict, pathToFolderWithSavedModel, NAME_OF_FILE_WITH_RANDOM_FOREST_REGRESSION_MODEL, log, jobControl, from, to); break;
            case REGRESSION_3_SVM_EPSILON    : String scriptToReadModelForSVMepsilonRegressionAndPredict = RHelper.getScript("RegressionAnalysis", "ReadModelForSVMepsilonRegressionAndPredict");
                                               predictedResponse = Rutils.readRegressionModelAndPredictResponseUsingR(dataMatrix, scriptToReadModelForSVMepsilonRegressionAndPredict, pathToFolderWithSavedModel, NAME_OF_FILE_WITH_SVM_EPSILON_REGRESSION_MODEL, log, jobControl, from, to); break;
            case REGRESSION_4_LS_IN_CLUSTERS :
            case REGRESSION_5_LS_CLUSTERIZED : predictedResponse = LSregressionInClusters.readRegressionModelAndPredictResponse(pathToFolderWithSavedModel, variableNames, dataMatrix, jobControl, from, to); break;
            case REGRESSION_6_PC             : predictedResponse = PrincipalComponentRegression.readRegressionModelAndPredictResponse(pathToFolderWithSavedModel, dataMatrix); break;
            default                          : throw new Exception("This regression type '" + regressionType + "' is not supported in our regression analysis currently");
        }
        if( doWritePrediction )
            TableUtils.writeDoubleTable(predictedResponse, objectNames, responseName + "_predicted", pathToOutputs, "predictions_of_" + responseName +"_" + regressionType);
        if( jobControl != null ) jobControl.setPreparedness(to);
        return predictedResponse;
    }

    public void implementCrossValidation(int percentageOfDataForTraining, int numberOfClusters, boolean doAddResponseToClusterization, String dataTransformationType, DataElementPath pathToOutputs, String tableName, Logger log, int maxNumberOfIterations, double eps, int maxNumberOfClusterizationSteps, int numberOfPrincipalComponents, String principalComponentSortingType, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        Object[] objects = splitDataSet(dataMatrix, response, percentageOfDataForTraining);
        RegressionEngine re = new RegressionEngine(regressionType, null, variableNames, (double[][])objects[0], responseName, (double[])objects[1]);
        double[] responsePredictedInTrain = re.createAndWriteRegressionModel(pathToOutputs, log, maxNumberOfIterations, eps, maxNumberOfClusterizationSteps, false, numberOfClusters, dataTransformationType, numberOfPrincipalComponents, principalComponentSortingType, doAddResponseToClusterization, jobControl, from, from + (to - from) / 2);
        double[] valuesFromTrain = LinearRegression.getSummaryOnModelAccuracy((double[])objects[1], responsePredictedInTrain, ((double[][])objects[0])[0].length);
        re = new RegressionEngine(regressionType, null, variableNames, (double[][])objects[2], responseName, null);
        double[] responsePredictedInTest = re.readRegressionModelAndPredictResponse(pathToOutputs, null, log, false, jobControl, from + (to - from) / 2, to);
        double[] valuesFromTest = LinearRegression.getSummaryOnModelAccuracy((double[])objects[3], responsePredictedInTest, ((double[][])objects[2])[0].length);
        double[][] data = {valuesFromTrain, valuesFromTest};
        TableUtils.writeDoubleTable(MatrixUtils.getTransposedMatrix(data), new String[]{"Pearson correlation between observations and predictions", "Spearman correlation between observations and predictions", "Mean of observations", "Variance of observations", "Explained variance (in %)", "Number of observations"}, new String[]{"Training set", "Test set"}, pathToOutputs, tableName);
    }
    
    private void writeTableWithRegressionTypeAndResponseName(DataElementPath pathToOutputs, String tableName)
    {
        TableUtils.writeStringTable(new String[]{regressionType, responseName}, new String[]{"Regression type", "Response name"}, "value", pathToOutputs.getChildPath(tableName));
    }
    
    /***
     * 
     * @param pathToFolder
     * @param tableName
     * @return String[] array : array[0] = regressionType; array[1] = responseName;
     */
    public static String[] readRegressionTypeAndResponseNameInTable(DataElementPath pathToFolder, String tableName)
    {
        Map<String, String> tableColumn = TableUtils.readGivenColumnInStringTable(pathToFolder.getChildPath(tableName), "value");
        return new String[]{tableColumn.get("Regression type"), tableColumn.get("Response name")};
    }

    /***
     * 
     * @param dataMatrix
     * @param response
     * @param percentageOfDataForTraining
     * @return Object[] array : array[0] = dataMatrixForTrain; array[1] = responseForTrain; array[2] = dataMatrixForTest; array[3] = responseForTest;
     */
    private Object[] splitDataSet(double[][] dataMatrix, double[] response, int percentageOfDataForTraining)
    {
        int sizeForTrain = dataMatrix.length * percentageOfDataForTraining / 100, sizeForTest = dataMatrix.length - sizeForTrain;
        int[] indices = IntStreamEx.ofIndices(dataMatrix).toArray();
        StatUtil.shuffleVector(indices, 0);
        double[][] dataMatrixForTrain = new double[sizeForTrain][], dataMatrixForTest = new double[sizeForTest][];
        double[] responseForTrain = new double[sizeForTrain], responseForTest = new double[sizeForTest];
        for( int i = 0; i < sizeForTrain; i++ )
        {
            dataMatrixForTrain[i] = dataMatrix[indices[i]];
            responseForTrain[i] = response[indices[i]];
        }
        for( int i = 0; i < sizeForTest; i++ )
        {
            dataMatrixForTest[i] = dataMatrix[indices[i + sizeForTrain]];
            responseForTest[i] = response[indices[i + sizeForTrain]];
        }
        return new Object[]{dataMatrixForTrain, responseForTrain, dataMatrixForTest, responseForTest};
    }

    /***
     * 
     * @param pathToTableWithDataMatrix
     * @param variableNames
     * @param responseName
     * @return Object[] array; array[0] = String[] objectNames; array[1] = double[][] dataMatrix; array[2] = double[] response;
     */
// old version
//    public static Object[] readDataSubMatrixAndResponse(DataElementPath pathToTableWithDataMatrix, String[] variableNames, String responseName)
//    {
//        TableDataCollection table = pathToTableWithDataMatrix.getDataElement(TableDataCollection.class);
//        double[] response = TableUtils.readGivenColumnInDoubleTableAsArray(table, responseName);
//        Object[] objects = TableUtils.readDataSubMatrix(pathToTableWithDataMatrix, variableNames);
//        return removeObjectsWithMissingData((String[])objects[0], (double[][])objects[1], response);
//    }
    
    /***
     * 
     * @param pathToTableWithDataMatrix
     * @param variableNames
     * @param responseName
     * @return Object[] array; array[0] = String[] objectNames; array[1] = double[][] dataMatrix; array[2] = double[] response;
     * @throws IOException 
     */
    // new version
    public static Object[] readDataSubMatrixAndResponse(DataElementPath pathToMatrix, String[] variableNames, String responseName) throws IOException
    {
        double[] response = DataMatrix.readDoubleColumn(pathToMatrix, responseName);
        Object[] objects = DataMatrix.readDoubleMatrixOrSubmatrix(pathToMatrix, variableNames);
        return removeObjectsWithMissingData((String[])objects[0], (double[][])objects[2], response);
    }
    
    /***
     * remove all objects that contain missing data (NaN-values);
     * @param objectNames
     * @param dataMatrix
     * @param response
     * @return Object[] array; array[0] = String[] newObjectNames; array[1] = double[][] newDataMatrix; array[2] = double[] newResponse;
     */
    private static Object[] removeObjectsWithMissingData(String[] objectNames, double[][] dataMatrix, double[] response)
    {
        List<String> newObjectNames = new ArrayList<>();
        List<double[]> newDataMatrix = new ArrayList<>();
        List<Double> newResponse = new ArrayList<>();
        for( int i = 0; i < objectNames.length; i++ )
            if( ! MatrixUtils.doContainNaN(dataMatrix[i]) && ! Double.isNaN(response[i]) )
            {
                newObjectNames.add(objectNames[i]);
                newDataMatrix.add(dataMatrix[i]);
                newResponse.add(response[i]);
            }
        if( newObjectNames.isEmpty() ) return null;
        return new Object[]{newObjectNames.toArray(new String[0]), newDataMatrix.toArray(new double[newDataMatrix.size()][]), MatrixUtils.fromListToArray(newResponse)};
    }
    
    public static boolean doAddInterceptToRegression(String regressionType)
    {
        return( ! regressionType.equals(REGRESSION_2_RF) && ! regressionType.equals(REGRESSION_3_SVM_EPSILON) && ! regressionType.equals(REGRESSION_6_PC));
    }
}
