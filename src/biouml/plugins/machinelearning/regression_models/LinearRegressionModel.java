/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import ru.biosoft.access.core.DataElementPath;

/**
 * @author yura
 *
 */
public class LinearRegressionModel extends RegressionModel
{
    public static final String NAME_OF_TABLE_WITH_COEFFICIENTS = "Regression_coefficients";
    public static final String NAME_OF_COLUMN_WITH_COEFFICIENTS = "Coefficient";
    
    protected double[] coefficients;
    
    public LinearRegressionModel(String regressionType, String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(regressionType, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public LinearRegressionModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    public void fitModelParticular(double[][] matrix, Object[] additionalInputParameters)
    {
        calculateCoefficientsAndPredictedResponse(matrix, additionalInputParameters);
    }
    
    @Override
    public double[] predict(double[][] matrix)
    {
        return MatrixUtils.getProductOfRectangularMatrixAndVector(matrix, coefficients);
    }

    @Override
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        DataMatrix dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_COEFFICIENTS), new String[]{NAME_OF_COLUMN_WITH_COEFFICIENTS});
        variableNames = dataMatrix.getRowNames();
        coefficients = dataMatrix.getColumn(NAME_OF_COLUMN_WITH_COEFFICIENTS);
    }

    // The aim of method : to determine this.coefficients and this.predictedResponse
    public void calculateCoefficientsAndPredictedResponse(double[][] matrix, Object[] additionalInputParameters)
    {}

    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToDoubleTable(variableNames, NAME_OF_COLUMN_WITH_COEFFICIENTS, coefficients, pathToOutputFolder, NAME_OF_TABLE_WITH_COEFFICIENTS);
    }
    
    /************************ Static methods *******************/
    
    public static boolean doAddInterceptToRegression(String regressionType)
    {
        return( ! regressionType.equals(REGRESSION_3_PC) && ! regressionType.equals(REGRESSION_4_RT) && ! regressionType.equals(REGRESSION_5_RF) && ! regressionType.equals(REGRESSION_6_RF_R) && ! regressionType.equals(REGRESSION_7_SVM_R) && ! regressionType.equals(REGRESSION_8_SVM));
    }
}