/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;

/**
 * @author yura
 *
 * Input:
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = int maxNumberOfRotations;
 *          additionalInputParameters[1] = double epsForRotations;
 *          additionalInputParameters[2] = double shrinkageParameter (>= 0); 
 */
public class RidgeRegressionModel extends LinearRegressionModel
{
    protected int maxNumberOfRotations, numberOfProcessedRotations;
    protected DataMatrix numberOfRotations;
    
    public RidgeRegressionModel(String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(REGRESSION_9_RIDGE, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }

    public RidgeRegressionModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    public void calculateCoefficientsAndPredictedResponse(double[][] matrix, Object[] additionalInputParameters)
    {
        if( matrix.length != response.length || matrix.length <= matrix[0].length ) return;
        maxNumberOfRotations = (int)additionalInputParameters[0];
        double epsForRotations = (double)additionalInputParameters[1], shrinkageParameter = (double)additionalInputParameters[2];
        double[][] mat = MatrixUtils.getProductOfTransposedMatrixAndMatrix(matrix);
        mat = MatrixUtils.getSumOfMatrices(mat, UtilsForArray.getConstantArray(mat.length, shrinkageParameter));
        Object[] objects = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(mat, maxNumberOfRotations, epsForRotations);
        mat = (double[][])objects[1];
        numberOfProcessedRotations = (int)objects[0];
        double[] vector = MatrixUtils.getProductOfTransposedMatrixAndVector(matrix, response);
        coefficients = MatrixUtils.getProductOfSymmetricMatrixAndVector(mat, vector);
        predictedResponse = predict(matrix);
    }
    
    @Override
    public void calculateAccompaniedSpecificInformation()
    {
        numberOfRotations = ModelUtils.getNumberOfRotations(maxNumberOfRotations, numberOfProcessedRotations);
    }
    
    @Override
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {
        numberOfRotations.writeDataMatrix(false, pathToOutputFolder, "Number_of_rotations", log); 
    }
}