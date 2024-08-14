/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.StudentDistribution;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.chart.Chart;

/**
 * @author yura
 *
 * Input:
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = int maxNumberOfRotations;
 *          additionalInputParameters[1] = double epsForRotations; 
 */
public class OrdinaryLeastSquaresRegressionModel extends LinearRegressionModel
{
    protected double[][] covarianceMatrix;
    protected int maxNumberOfRotations, numberOfProcessedRotations;
    protected DataMatrix coefficientSignificances, numberOfRotations;
    
    public OrdinaryLeastSquaresRegressionModel(String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(REGRESSION_1_OLS, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }

    public OrdinaryLeastSquaresRegressionModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
 
    @Override
    public void calculateCoefficientsAndPredictedResponse(double[][] matrix, Object[] additionalInputParameters)
    {
        if( matrix.length != response.length || matrix.length <= matrix[0].length ) return;
        maxNumberOfRotations = additionalInputParameters == null ? MatrixUtils.DEFAULT_MAX_NUMBER_OF_ROTATIONS : (int)additionalInputParameters[0];
        double epsForRotations = additionalInputParameters == null ? MatrixUtils.DEFAULT_EPS_FOR_ROTATIONS : (double)additionalInputParameters[1];
        double[][] mat = MatrixUtils.getProductOfTransposedMatrixAndMatrix(matrix);
        Object[] objects = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(mat, maxNumberOfRotations, epsForRotations);
        if( objects == null )
        {
            isModelFitted = false;
            return;
        }
        covarianceMatrix = (double[][])objects[1];
        numberOfProcessedRotations = (int)objects[0];
        double[] vector = MatrixUtils.getProductOfTransposedMatrixAndVector(matrix, response);
        coefficients = MatrixUtils.getProductOfSymmetricMatrixAndVector(covarianceMatrix, vector);
        predictedResponse = predict(matrix);
    }
    
    @Override
    public void calculateAccompaniedSpecificInformation()
    {
        coefficientSignificances = getCoefficientSignificances();
        numberOfRotations = ModelUtils.getNumberOfRotations(maxNumberOfRotations, numberOfProcessedRotations);
    }
    
    @Override
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {
        coefficientSignificances.writeDataMatrix(false, pathToOutputFolder, NAME_OF_TABLE_WITH_COEFFICIENTS, log);
        numberOfRotations.writeDataMatrix(false, pathToOutputFolder, "Number_of_rotations", log); 
    }

    protected DataMatrix getCoefficientSignificances()
    {
        double[][] matrix = new double[variableNames.length][];
        for( int i = 0; i < variableNames.length; i++ )
        {
            double statistic = coefficients[i] / Math.sqrt(getResidualVariance() * covarianceMatrix[i][i]);
            // double pValue = 2.0 * DistributionFunctions.studentDistribution(Math.abs(statistic), response.length - variableNames.length, 80)[1];
            // double pValue = 2.0 * (1.0 - StudentDistribution.getDistributionFunction(Math.abs(statistic), response.length - variableNames.length, 80));
            double pValue = StudentDistribution.getPvalueForAbsStudent(Math.abs(statistic), response.length - variableNames.length, 80);
            matrix[i] = new double[]{coefficients[i], statistic, pValue};
        }
        return new DataMatrix(variableNames, new String[]{NAME_OF_COLUMN_WITH_COEFFICIENTS, "Statistic (Z-score)", "p-value"}, matrix);
    }
    
    /****************** Static methods ********************/
    // simple linear regression y = a + b * x;
    public static Chart createChartForSimpleOlsRegression(String responseName, double[] response, String variableName, double[] variable)
    {
        double[][] matrix = new double[variable.length][];
        for( int i = 0; i < variable.length; i++ )
            matrix[i] = new double[]{1.0, variable[i]};
        DataMatrix dataMatrix = new DataMatrix(null, new String[]{ModelUtils.INTERCEPT, variableName}, matrix);
        OrdinaryLeastSquaresRegressionModel olsRegressionModel = new OrdinaryLeastSquaresRegressionModel(responseName, response, dataMatrix, new Object[]{3, 1.0E-10}, false);
        double[] minAndMax = PrimitiveOperations.getMinAndMax(variable);
        matrix = new double[][]{new double[]{1.0, minAndMax[0]}, new double[]{1.0, minAndMax[1]}};
        double[] prediction = olsRegressionModel.predict(matrix);
        return ChartUtils.createChart(minAndMax, prediction, null, variable, response, null, null, variableName, responseName, true);
    }
}
