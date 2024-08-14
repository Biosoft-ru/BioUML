/* $Id$ */

package biouml.plugins.machinelearning.classification_models;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;

/**
 * @author yura
 *
 */
public class LinearClassificationModel extends ClassificationModel
{
    public static final String NAME_OF_TABLE_WITH_COEFFICIENTS = "Coefficients";
    public static final String NAME_OF_COLUMN_WITH_COEFFICIENTS = "Coefficient";

    protected double[] coefficients;
    
    public LinearClassificationModel(String classificationType, String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(classificationType, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public LinearClassificationModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    protected void fitModelParticular(double[][] matrix, Object[] additionalInputParameters)
    {
        calculateCoefficientsAndPredictedIndices(matrix, additionalInputParameters);
    }
    
    // The aim of method : to determine this.coefficients and this.predictedIndices
    protected void calculateCoefficientsAndPredictedIndices(double[][] matrix, Object[] additionalInputParameters)
    {}
    
    @Override
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        DataMatrix dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_COEFFICIENTS), new String[]{NAME_OF_COLUMN_WITH_COEFFICIENTS});
        variableNames = dataMatrix.getRowNames();
        coefficients = dataMatrix.getColumn(NAME_OF_COLUMN_WITH_COEFFICIENTS);
    }
    
    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToDoubleTable(variableNames, NAME_OF_COLUMN_WITH_COEFFICIENTS, coefficients, pathToOutputFolder, NAME_OF_TABLE_WITH_COEFFICIENTS);
    }
    
    /************************ Static methods *******************/
    
    public static boolean doAddInterceptToClassification(String classificationType)
    {
        return(classificationType.equals(CLASSIFICATION_4_PER) || classificationType.equals(CLASSIFICATION_5_LRM));
    }
}
