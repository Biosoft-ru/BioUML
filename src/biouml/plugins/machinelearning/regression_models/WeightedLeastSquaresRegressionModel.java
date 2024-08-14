/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;
import ru.biosoft.access.core.DataElementPath;

/**
 * @author yura
 * Weighted least squares regression is particular case of generalized least squares regression. 
 * Regression y = X * b + eps;
 * Assumption: Var(y) is diagonal, Var(y) = sigma * diagonal(|yi|);
 */

public class WeightedLeastSquaresRegressionModel extends OrdinaryLeastSquaresRegressionModel
{
    private double[]  diagonalInverse;

    /***
     * Input:
     * Object[] additionalInputParameters :
     *          additionalInputParameters[0] = int maxNumberOfRotations;
     *          additionalInputParameters[1] = double epsForRotations; 
     */
    public WeightedLeastSquaresRegressionModel(String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
        regressionType = REGRESSION_2_WLS;
    }
    
    public WeightedLeastSquaresRegressionModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    public void calculateCoefficientsAndPredictedResponse(double[][] matrix, Object[] additionalInputParameters)
    {
        if( matrix.length != response.length || matrix.length <= matrix[0].length ) return;
        maxNumberOfRotations = (int)additionalInputParameters[0];
        double epsForRotations = (double)additionalInputParameters[1];
        double[] diagonal = composeDiagonalVarianceMatrix();
        diagonalInverse = VectorOperations.getInverseVector(diagonal);
        double[][] mat = MatrixUtils.getProductOfTransposedRectangularAndDiagonalAndRectangularMatrices(matrix, diagonalInverse);
        Object[] objects = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(mat, maxNumberOfRotations, epsForRotations);
        covarianceMatrix = (double[][])objects[1];
        numberOfProcessedRotations = (int)objects[0];
        double[] vector = MatrixUtils.getProductOfDiagonalMatrixAndVector(diagonalInverse, response);
        vector = MatrixUtils.getProductOfTransposedMatrixAndVector(matrix, vector);
        coefficients = MatrixUtils.getProductOfSymmetricMatrixAndVector(covarianceMatrix, vector);
        predictedResponse = predict(matrix);
    }
    
    // diagonal[i] = Abs(response[i]),  if Abs(response[i]) > 0;
    //             = non-zero Minimum of Abs(response[]), if Abs(response[i]) == 0.
    private double[] composeDiagonalVarianceMatrix()
    {
        double[] diagonal = PrimitiveOperations.getAbs(response);
        double[] minAndMax = PrimitiveOperations.getMinAndMax(diagonal);
        if( minAndMax[0] > 0.0 ) return diagonal;
        double nonZeroMinimum = minAndMax[1];
        if( nonZeroMinimum <= 0.0 ) return null;
        for( double x : diagonal )
            if( x < nonZeroMinimum && x > 0.0 )
                nonZeroMinimum = x;
        for( int i = 0; i < diagonal.length; i++ )
            if( diagonal[i] == 0.0 )
                diagonal[i] = nonZeroMinimum;
        return diagonal;
    }
    
    @Override
    public double getResidualVariance()
    {
        return MatrixUtils.getProductOfTransposedVectorAndDiagonalMatrixAndVector(VectorOperations.getSubtractionOfVectors(response, predictedResponse), diagonalInverse) / (double)(response.length - variableNames.length);
    }
}
