/* $Id$ */

package biouml.plugins.machinelearning.classification_models;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.MultivariateSamples;
import biouml.plugins.machinelearning.utils.StatUtils.SimilaritiesAndDissimilarities;

/**
 * @author yura
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = int maxNumberOfRotations;
 *          additionalInputParameters[1] = double epsForRotations; 
 */
public class MaximumLikelihoodModel extends ClassificationModel
{
    public static final String NAME_OF_TABLE_WITH_INVERSE_COVARIANCE_MATRIX = "Inverse_covariance_matrix";

    protected int maxNumberOfRotations, numberOfProcessedRotations;
    protected double[][] meanVectorsInClasses, inverseCovarianceMatrix;
    private DataMatrix numberOfRotations;
    
    public MaximumLikelihoodModel(String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(ClassificationModel.CLASSIFICATION_2_MLM, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public MaximumLikelihoodModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }

    @Override
    public void fitModelParticular(double[][] matrix, Object[] additionalInputParameters)
    {
        maxNumberOfRotations = (int)additionalInputParameters[0];
        double epsForRotations = (double)additionalInputParameters[1];
        MultivariateSamples mss = new MultivariateSamples(matrix, transform(responseIndices));
        Object[] objects = mss.getMeanVectorsAndCovarianceMatrix();
        meanVectorsInClasses =  (double[][])objects[0];
        objects = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod((double[][])objects[1], maxNumberOfRotations, epsForRotations);
        numberOfProcessedRotations = (int)objects[0];
        inverseCovarianceMatrix = (double[][])objects[1];
        predictedIndices = predict(matrix);
    }
    
    @Override
    protected int predict(double[] rowOfMatrix)
    {
        double[] scores = new double[meanVectorsInClasses.length];
        for( int i = 0; i < meanVectorsInClasses.length; i++ )
            scores[i] = SimilaritiesAndDissimilarities.getMahalanobisSquaredDistance(rowOfMatrix, meanVectorsInClasses[i], inverseCovarianceMatrix);
        return (int)PrimitiveOperations.getMin(scores)[0];
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
    
    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        ModelUtils.saveMeanVectorsInClasses(distinctClassNames, variableNames, meanVectorsInClasses, pathToOutputFolder);
        DataMatrix dm = new DataMatrix(variableNames, variableNames, MatrixUtils.transformSymmetricMatrixToSquareMatrix(inverseCovarianceMatrix));
        dm.writeDataMatrix(false, pathToOutputFolder, NAME_OF_TABLE_WITH_INVERSE_COVARIANCE_MATRIX, log);
    }
    
    @Override
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        DataMatrix dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(ModelUtils.NAME_OF_TABLE_WITH_MEANS_IN_CLASSES), null);
        distinctClassNames = dataMatrix.getRowNames();
        variableNames = dataMatrix.getColumnNames();
        meanVectorsInClasses = dataMatrix.getMatrix();
        dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_INVERSE_COVARIANCE_MATRIX), null);
        inverseCovarianceMatrix = MatrixUtils.getLowerTriangularMatrix(dataMatrix.getMatrix());
    }
}
