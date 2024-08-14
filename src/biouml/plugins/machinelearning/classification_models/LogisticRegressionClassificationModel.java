/* $Id$ */

package biouml.plugins.machinelearning.classification_models;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.MathUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;

/**
 * @author yura
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = int maxNumberOfIterations 
 *          additionalInputParameters[1] = double admissibleMisclassificationRate;
 *          additionalInputParameters[2] = int maxNumberOfRotations;
 *          additionalInputParameters[3] = double epsForRotations; 
 */
public class LogisticRegressionClassificationModel extends LinearClassificationModel
{
    private int maxNumberOfIterations, numberOfProcessedIterations, optimalIterationIndex, maxNumberOfRotations, maxNumberOfProcessedRotations;
    private double probabilityThreshold = 1.0E-10;
    private DataMatrix numberOfIterations;
    
    public LogisticRegressionClassificationModel(String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(CLASSIFICATION_5_LRM, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public LogisticRegressionClassificationModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    // TODO: To test!!!
    @Override
    public void calculateCoefficientsAndPredictedIndices(double[][] matrix, Object[] additionalInputParameters)
    {
        maxNumberOfIterations = (int)additionalInputParameters[0];
        double admissibleMisclassificationRate = (double)additionalInputParameters[1], epsForRotations = (double)additionalInputParameters[3];
        maxNumberOfRotations = (int)additionalInputParameters[2];
        double missClassificationRateOptimal = 1.1;
        //TODO: To consider specified coefficientsInitialApproxomation.
        double[] coefficientsInitialApproxomation = null, coefficientsOptimal = null;
        coefficients = coefficientsInitialApproxomation != null ? coefficientsInitialApproxomation : new double[matrix[0].length];
        for( numberOfProcessedIterations = 1; numberOfProcessedIterations <= maxNumberOfIterations; numberOfProcessedIterations++ )
        {
            // 1. Calculate probabilities and correct them to avoid probabilities[i] == 0.0 or probabilities[i] == 1.0.
            double[] probabilities = calculateProbabilitiesOfFirstClass(matrix);
            for( int i = 0; i < probabilities.length; i++ )
            {
                // TODO:
//                if( probabilities[i] < probabilityThreshold )
//                    probabilities[i] = probabilityThreshold;
//                else if( probabilities[i] > 1.0 - probabilityThreshold )
//                    probabilities[i] = 1.0 - probabilityThreshold;
                if( probabilities[i] <= 0.0 )
                    probabilities[i] = probabilityThreshold;
                else if( probabilities[i] >= 1.0 )
                    probabilities[i] = 1.0 - probabilityThreshold;
            }
            
            // 2. Identification of coefficientsOptimal and missclassificationRateOptimal.
            int[] predictedIndices = predict(matrix);
            int[] falsePredictionIndices = UtilsForArray.getIndicesOfUnequalElements(responseIndices, predictedIndices);
            double missClassificationRate = (double)falsePredictionIndices.length / (double)responseIndices.length;
            if( missClassificationRate <= admissibleMisclassificationRate )
            {
                optimalIterationIndex = numberOfProcessedIterations;
                break;
            }
            
            // TODO: temporary
            String s = "TCR = " + Double.toString(1.0 - missClassificationRate) + " coefficients = ";
            for( double x : coefficients )
                s += " " + Double.toString(x);
            log.info(s);
            
            if( missClassificationRate < missClassificationRateOptimal )
            {
                coefficientsOptimal = ArrayUtils.clone(coefficients);
                missClassificationRateOptimal = missClassificationRate;
                optimalIterationIndex = numberOfProcessedIterations;
            }
            
            // 3. Re-calculation of coefficients.
            double[] diagonal = new double[probabilities.length];
            for( int i = 0; i < diagonal.length; i++ )
                diagonal[i] = probabilities[i] * (1.0 - probabilities[i]);
            double[] z = MatrixUtils.getProductOfRectangularMatrixAndVector(matrix, coefficients);
            for( int i = 0; i < z.length; i++ )
                z[i] += ((double)responseIndices[i] - probabilities[i]) / diagonal[i];
            double[][] symmetricMatrix = MatrixUtils.getProductOfTransposedRectangularAndDiagonalAndRectangularMatrices(matrix, diagonal);
            Object[] objects = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(symmetricMatrix, maxNumberOfRotations, epsForRotations);
            maxNumberOfProcessedRotations = Math.max(maxNumberOfProcessedRotations, (int)objects[0]);
            symmetricMatrix = (double[][])objects[1];
            double[][] mat = MatrixUtils.getProductOfTransposedRectangularAndDiagonalMatrices(matrix, diagonal);
            mat = MatrixUtils.getProductOfSymmetricAndRectangularMatrices(symmetricMatrix, mat);
            coefficients = MatrixUtils.getProductOfRectangularMatrixAndVector(mat, z);
        }
        numberOfProcessedIterations = Math.min(numberOfProcessedIterations, maxNumberOfIterations);
        coefficients = coefficientsOptimal;
        predictedIndices = predict(matrix);
    }
    
    @Override
    public int[] predict(double[][] matrix)
    {
        probabilities = calculateProbabilitiesOfFirstClass(matrix);
        int[] predictedIndices = new int[probabilities.length];
        for( int i = 0; i < predictedIndices.length; i++ )
        {
            predictedIndices[i] = probabilities[i] >= 0.5 ? 0 : 1;
            // TODO:
            if( predictedIndices[i] == 1 )
                probabilities[i] = 1.0 - probabilities[i]; 
        }
        return predictedIndices;
    }
    
    private double[] calculateProbabilitiesOfFirstClass(double[][] matrix)
    {
        double[] probabilities = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
            probabilities[i] = MathUtils.logisticFunction(VectorOperations.getInnerProduct(coefficients, matrix[i]));
        return probabilities;
    }
    
    @Override
    public void calculateAccompaniedSpecificInformation()
    {
        numberOfIterations = getNumberOfIterations();
    }

    @Override
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {
        numberOfIterations.writeDataMatrix(false, pathToOutputFolder, "Number_of_iterations", log); 
    }
    
    private DataMatrix getNumberOfIterations()
    {
        return new DataMatrix(new String[]{"Maximal number of iterations", "Number of processed iterations", "Optimal iteration number", "Maximal number of rotations", "Maximal number of processed rotations"}, "Value", new double[]{(double)maxNumberOfIterations, (double)numberOfProcessedIterations, (double)optimalIterationIndex, (double)maxNumberOfRotations, (double)maxNumberOfProcessedRotations});
    }
}
