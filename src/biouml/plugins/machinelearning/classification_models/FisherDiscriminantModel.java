/* $Id$ */

package biouml.plugins.machinelearning.classification_models;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.StatUtils.MultivariateSamples;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;

/**
 * @author yura
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = int maxNumberOfRotations;
 *          additionalInputParameters[1] = double epsForRotations; 
 *          additionalInputParameters[2] = int maxNumberOfIterationsInLyusternikMethod;
 *          additionalInputParameters[3] = double epsForLyusternikMethod; 
 */
public class FisherDiscriminantModel extends LinearClassificationModel
{
    protected int maxNumberOfRotations, numberOfProcessedRotations, maxNumberOfIterationsInLyusternikMethod, numberOfProcessedIterationsInLyusternikMethod;
    protected double[][] meanVectorsInClasses; // meansInClasses[i] = mean vector in i-th class;
    protected DataMatrix numberOfRotationsAndIterations;
    
    public FisherDiscriminantModel(String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(CLASSIFICATION_1_LDA, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public FisherDiscriminantModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    protected void calculateCoefficientsAndPredictedIndices(double[][] matrix, Object[] additionalInputParameters)
    {
        maxNumberOfRotations = additionalInputParameters == null ? MatrixUtils.DEFAULT_MAX_NUMBER_OF_ROTATIONS : (int)additionalInputParameters[0];
        maxNumberOfIterationsInLyusternikMethod = additionalInputParameters == null ? MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_IN_LYUSTERNIK_METHOD : (int)additionalInputParameters[2];
        double epsForRotations = additionalInputParameters == null ? MatrixUtils.DEFAULT_EPS_FOR_ROTATIONS : (double)additionalInputParameters[1];
        double epsForLyusternikMethod = additionalInputParameters == null ? MatrixUtils.DEFAULT_EPS_IN_LYUSTERNIK_METHOD : (double)additionalInputParameters[3];
        coefficients = getLinearDiscriminantFunction(matrix, maxNumberOfRotations, epsForRotations, maxNumberOfIterationsInLyusternikMethod, epsForLyusternikMethod);
        predictedIndices = isModelFitted ? predict(matrix) : null;
    }
    
    private double[] getLinearDiscriminantFunction(double[][] matrix, int maxNumberOfRotations, double epsForRotations, int maxNumberOfIterationsInLyusternikMethod, double epsForLyusternikMethod)
    {
        Object[] objects = MultivariateSamples.getWithinAndBetweenAndTotalSspMatrices(matrix, transform(responseIndices));
        meanVectorsInClasses =  (double[][])objects[4];
        Object[] objs = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod((double[][])objects[0], maxNumberOfRotations, epsForRotations);
        if( objs == null )
        {
            isModelFitted = false;
            return null;
        }
        numberOfProcessedRotations = (int)objs[0];
        double[][] inverseWithinSspMatrix = (double[][])objs[1];
        double[][] productMatrix = MatrixUtils.getProductOfSymmetricMatrices(inverseWithinSspMatrix, (double[][])objects[1]);
        objects = MatrixUtils.getMaximalEigenValueOfSquareMatrixByLyusternikMethod(productMatrix, maxNumberOfIterationsInLyusternikMethod, epsForLyusternikMethod);
        numberOfProcessedIterationsInLyusternikMethod = (int)objects[2]; 
        return (double[])objects[1];
    }
    
    @Override
    protected int predict(double[] rowOfMatrix)
    {
        double[] scores = new double[meanVectorsInClasses.length];
        for( int i = 0; i < meanVectorsInClasses.length; i++ )
        {
            double[] vector = VectorOperations.getSubtractionOfVectors(rowOfMatrix, meanVectorsInClasses[i]);
            scores[i] = Math.abs(VectorOperations.getInnerProduct(coefficients, vector));
        }
        return (int)PrimitiveOperations.getMin(scores)[0];
    }
    
    @Override
    public void calculateAccompaniedSpecificInformation()
    {
        numberOfRotationsAndIterations = getNumberOfRotationsAndIterations();
    }
    
    @Override
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {
        numberOfRotationsAndIterations.writeDataMatrix(false, pathToOutputFolder, "Number_of_rotations_and_iterations", log); 
    }
    
    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToDoubleTable(variableNames, NAME_OF_COLUMN_WITH_COEFFICIENTS, coefficients, pathToOutputFolder, NAME_OF_TABLE_WITH_COEFFICIENTS);
        ModelUtils.saveMeanVectorsInClasses(distinctClassNames, variableNames, meanVectorsInClasses, pathToOutputFolder);
    }
    
    @Override
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        DataMatrix dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_COEFFICIENTS), new String[]{NAME_OF_COLUMN_WITH_COEFFICIENTS});
        variableNames = dataMatrix.getRowNames();
        coefficients = dataMatrix.getColumn(NAME_OF_COLUMN_WITH_COEFFICIENTS);
        dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(ModelUtils.NAME_OF_TABLE_WITH_MEANS_IN_CLASSES), null);
        meanVectorsInClasses = dataMatrix.getMatrix(); 
        distinctClassNames = dataMatrix.getRowNames();
    }
    
    private DataMatrix getNumberOfRotationsAndIterations()
    {
        return new DataMatrix(new String[]{"Maximal number of rotations for inverting matrix", "Number of processed rotations", "Maximal number of iterations in Lyusternik method", "Number of processed iterations"}, "Value", new double[]{(double)maxNumberOfRotations, (double)numberOfProcessedRotations, (double)maxNumberOfIterationsInLyusternikMethod, (double)numberOfProcessedIterationsInLyusternikMethod});
    }
}
