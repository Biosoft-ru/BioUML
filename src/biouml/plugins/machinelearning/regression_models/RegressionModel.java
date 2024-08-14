/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.chart.Chart;

/**
 * @author yura
 *
 */
public abstract class RegressionModel
{
    public static final String REGRESSION_1_OLS   = "Ordinary least squares regression";
    public static final String REGRESSION_2_WLS   = "Weighted least squares regression";
    public static final String REGRESSION_3_PC    = "Principal component regression";
    public static final String REGRESSION_4_RT    = "Tree-based regression";
    public static final String REGRESSION_5_RF    = "Random forest regression : under consruction";
    public static final String REGRESSION_6_RF_R  = "Random forest regression from R";
    public static final String REGRESSION_7_SVM_R = "Support vector machine from R";
    public static final String REGRESSION_8_SVM   = "Support vector machine : under consruction";
    public static final String REGRESSION_9_RIDGE = "Ridge regression";
    public static final String REGRESSION_10_COMB = "Combined regression (several regressions and one classification model)";

    public static final String REGRESSION_TYPE = "Regression type";
    public static final String RESPONSE_NAME   = "Response name";

    public static final String NAME_OF_TABLE_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME  = "Regression_type_and_response_name";
    public static final String NAME_OF_COLUMN_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME = "Value";
    public static final String NAME_OF_TABLE_WITH_VARIABLE_NAMES                     = "Variable_names";
    
    protected boolean doCalculateAccompaniedInformationWhenFit;
    protected boolean isModelFitted = true;
    protected String regressionType, responseName;
    protected String[] variableNames, objectNames;
    protected double[] predictedResponse, response;
    protected DataMatrix summaryOnModelAccuracy, extendedPredictions;
    protected Chart chartWithObservationsAndPredictions, chartForSimpleOlsRegression;
    
    // Constructor for fitting model.
    public RegressionModel(String regressionType, String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        this.doCalculateAccompaniedInformationWhenFit = doCalculateAccompaniedInformationWhenFit;
        this.regressionType = regressionType;
        this.responseName = responseName;
        this.variableNames = dataMatrix.getColumnNames();
        this.objectNames = dataMatrix.getRowNames();
        this.response = response;
        fitModel(dataMatrix, additionalInputParameters);
    }
    
    // Constructor for loading model.
    public RegressionModel(DataElementPath pathToInputFolder)
    {
        DataMatrixString dms = new DataMatrixString(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME), new String[]{NAME_OF_COLUMN_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME});
        String[] typeAndName = dms.getColumn(0);

        regressionType = typeAndName[0];
        responseName = typeAndName[1];
        loadModelParticular(pathToInputFolder);
    }
    
    public boolean isModelFitted()
    {
        return isModelFitted;
    }
    
    protected void fitModel(DataMatrix dataMatrix, Object[] additionalInputParameters)
    {
        fitModel(dataMatrix.getMatrix(), additionalInputParameters);
    }

    public void fitModel(double[][] matrix, Object[] additionalInputParameters)
    {
        fitModelParticular(matrix, additionalInputParameters);
        if( doCalculateAccompaniedInformationWhenFit )
        {
            calculateAccompaniedInformation();
            calculateAccompaniedSpecificInformation();

            // if regression is simple then chart for simple regression is created.
            if( matrix[0].length == 2 && ArrayUtils.contains(variableNames, ModelUtils.INTERCEPT) )
            {
                int index = ArrayUtils.indexOf(variableNames, ModelUtils.INTERCEPT);
                index = index == 0 ? 1 : 0;
                chartForSimpleOlsRegression = OrdinaryLeastSquaresRegressionModel.createChartForSimpleOlsRegression(responseName, response, variableNames[index], MatrixUtils.getColumn(matrix, index));
            }
        }
    }
    
    public void saveModel(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToStringTable(new String[]{REGRESSION_TYPE, RESPONSE_NAME}, NAME_OF_COLUMN_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME, new String[]{regressionType, responseName}, pathToOutputFolder, NAME_OF_TABLE_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME);
        saveModelParticular(pathToOutputFolder);
        if( doCalculateAccompaniedInformationWhenFit )
        {
            summaryOnModelAccuracy.writeDataMatrix(false, pathToOutputFolder, "Summary_on_model_accuracy", log);
            extendedPredictions.writeDataMatrix(false, pathToOutputFolder, "Response_predicted", log);
            TableAndFileUtils.addChartToTable(ChartUtils.CHART, chartWithObservationsAndPredictions, pathToOutputFolder.getChildPath("chart_observations_and_predictions"));
            if( chartForSimpleOlsRegression != null )
                TableAndFileUtils.addChartToTable(ChartUtils.CHART, chartForSimpleOlsRegression, pathToOutputFolder.getChildPath("chart_with_simple_OLS_regression"));
            saveAccompaniedSpecificInformation(pathToOutputFolder);
        }
    }

    public double[] predict(DataMatrix dataMatrix)
    {
        if( ! UtilsForArray.equal(variableNames, dataMatrix.getColumnNames()) ) return null;
        return predict(dataMatrix.getMatrix());
    }
    
    public String[] getVariableNames()
    {
        return variableNames;
    }
    
    public double[] predict(double[][] matrix)
    {
        return null;
    };

    protected void fitModelParticular(double[][] matrix, Object[] additionalInputParameters)
    {}
    
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {}

    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToStringTable(variableNames, "variable_names", variableNames, pathToOutputFolder, NAME_OF_TABLE_WITH_VARIABLE_NAMES);
    }
    
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {}
    
    public void calculateAccompaniedSpecificInformation()
    {}
    
    private void calculateAccompaniedInformation()
    {
        summaryOnModelAccuracy = getSummaryOnModelAccuracy();
        extendedPredictions = getExtendedPredictions();
        chartWithObservationsAndPredictions = OrdinaryLeastSquaresRegressionModel.createChartForSimpleOlsRegression("Predictions", predictedResponse, "Observations", response);
    }
    
    private DataMatrix getSummaryOnModelAccuracy()
    {
        return ModelUtils.getSummaryOnModelAccuracy(response, predictedResponse, variableNames.length);
    }
    
    private DataMatrix getExtendedPredictions()
    {
        double[][] matrix = new double[response.length][];
        for(int i = 0; i < response.length; i ++ )
            matrix[i] = new double[]{response[i], predictedResponse[i], response[i] - predictedResponse[i], Math.abs(response[i] - predictedResponse[i])};
        return new DataMatrix(objectNames, new String[]{responseName, responseName + "_predicted", "difference_between_response_and_prediction", "abs_difference_between_response_and_prediction"}, matrix);
    }
    
    public double getResidualVariance()
    {
        return ModelUtils.getResidualVariance(response, predictedResponse, variableNames.length);
    }

    /******************************* static methods ***************/
    
    public static RegressionModel loadModel(DataElementPath pathToInputFolder)
    {
        DataMatrixString dms = new DataMatrixString(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME), new String[]{NAME_OF_COLUMN_WITH_REGRESSION_TYPE_AND_RESPONSE_NAME});
        switch( dms.getColumn(0)[0] )
        {
            case REGRESSION_1_OLS   : return new OrdinaryLeastSquaresRegressionModel(pathToInputFolder);
            case REGRESSION_2_WLS   : return new WeightedLeastSquaresRegressionModel(pathToInputFolder);
            case REGRESSION_3_PC    : return new PrincipalComponentRegressionModel(pathToInputFolder);
            case REGRESSION_4_RT    : return new RegressionTreeModel(pathToInputFolder);
            case REGRESSION_6_RF_R  : return new RandomForestRegressionModelFromR(pathToInputFolder);
            case REGRESSION_7_SVM_R : return new SupportVectorMachineRegressionModelFromR(pathToInputFolder);
            case REGRESSION_9_RIDGE : return new RidgeRegressionModel(pathToInputFolder);
            case REGRESSION_10_COMB : return new CombinedRegressionModel(pathToInputFolder);
            default                 : return null;
        }
    }
    
    public static RegressionModel createModel(String regressionType, String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        RegressionModel regressionModel = null;
        switch( regressionType )
        {
            case REGRESSION_1_OLS   : regressionModel = new OrdinaryLeastSquaresRegressionModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case REGRESSION_2_WLS   : regressionModel = new WeightedLeastSquaresRegressionModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case REGRESSION_3_PC    : regressionModel = new PrincipalComponentRegressionModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case REGRESSION_4_RT    : regressionModel = new RegressionTreeModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case REGRESSION_6_RF_R  : regressionModel = new RandomForestRegressionModelFromR(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case REGRESSION_7_SVM_R : regressionModel = new SupportVectorMachineRegressionModelFromR(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case REGRESSION_9_RIDGE : regressionModel = new RidgeRegressionModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case REGRESSION_10_COMB : regressionModel = new CombinedRegressionModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            default                 : return null;
        }
        return regressionModel.isModelFitted() ? regressionModel : null; 
    }

    public static String[] getAvailableRegressionTypes()
    {
        return new String[]{REGRESSION_1_OLS, REGRESSION_2_WLS, REGRESSION_3_PC, REGRESSION_4_RT, REGRESSION_5_RF, REGRESSION_6_RF_R, REGRESSION_7_SVM_R, REGRESSION_9_RIDGE, REGRESSION_10_COMB};
    }
    
    protected static Logger log = Logger.getLogger(RegressionModel.class.getName());
}
