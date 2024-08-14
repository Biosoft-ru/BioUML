/* $Id$ */

package biouml.plugins.machinelearning.classification_models;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;

/**
 * @author yura
 *
 */

public abstract class ClassificationModel
{
    public static final String CLASSIFICATION_1_LDA   = "Fisher's LDA (linear discriminant analysis)";
    public static final String CLASSIFICATION_2_MLM   = "Maximum likelihood classification based on multinormal distribution";
    public static final String CLASSIFICATION_3_SVM_R = "Support vector machine classification from R";
    public static final String CLASSIFICATION_4_PER   = "Perceptron";
    public static final String CLASSIFICATION_5_LRM   = "Logistic regression";

    public static final String CLASSIFICATION_TYPE = "Classification type";
    public static final String RESPONSE_NAME = "Response name";

    public static final String NAME_OF_TABLE_WITH_CLASSIFICATION_TYPE_AND_RESPONSE_NAME = "Classification_type_and_response_name";
    public static final String NAME_OF_COLUMN_WITH_CLASSIFICATION_TYPE_AND_RESPONSE_NAME = "Value";
    public static final String NAME_OF_TABLE_WITH_VARIABLE_NAMES = "Variable_names";
    public static final String NAME_OF_TABLE_WITH_DISTINCT_CLASS_NAMES = "Distinct_class_names";

    protected boolean doCalculateAccompaniedInformationWhenFit;
    protected boolean isModelFitted = true;
    protected String classificationType, responseName;
    protected String[] variableNames, objectNames;
    public String[] distinctClassNames;
    protected int[] responseIndices; // dim(responseIndices) = dim(response); response[i] = distinctClassesNames[responseIndices[i]];
    public int[] predictedIndices;
    protected double[] probabilities;
    protected DataMatrix trueClassificationRates;
    protected DataMatrixString extendedPredictions;
    
    // Constructor for fitting model.
    public ClassificationModel(String classificationType, String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        this.doCalculateAccompaniedInformationWhenFit = doCalculateAccompaniedInformationWhenFit;
        this.classificationType = classificationType;
        this.responseName = responseName;
        variableNames = dataMatrix.getColumnNames();
        objectNames = dataMatrix.getRowNames();
        Object[] objects = UtilsForArray.getDistinctStringsAndIndices(response);
        distinctClassNames = (String[])objects[0];
        responseIndices = (int[])objects[1];
        fitModel(dataMatrix, additionalInputParameters);
    }
    
    // Constructor for loading model.
    public ClassificationModel(DataElementPath pathToInputFolder)
    {
        DataMatrixString dms = new DataMatrixString(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_CLASSIFICATION_TYPE_AND_RESPONSE_NAME), new String[]{NAME_OF_COLUMN_WITH_CLASSIFICATION_TYPE_AND_RESPONSE_NAME});
        String[] typeAndName = dms.getColumn(0);
        classificationType = typeAndName[0];
        responseName = typeAndName[1];
        distinctClassNames = TableAndFileUtils.getRowNamesInTable(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_DISTINCT_CLASS_NAMES));
        loadModelParticular(pathToInputFolder);
    }
    
    public void fitModel(DataMatrix dataMatrix, Object[] additionalInputParameters)
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
        }
    }
    
    public boolean isModelFitted()
    {
        return isModelFitted;
    }
    
    public void predictAndSave(DataMatrix dataMatrix, DataElementPath pathToOutputFolder, String tableName, Logger log)
    {
        DataMatrixString dms = new DataMatrixString(dataMatrix.getRowNames(), "Predicted_values", transform(predict(dataMatrix)));
        if( probabilities == null )
            dms.writeDataMatrixString(false, pathToOutputFolder, tableName, log);
        else
        {
            DataMatrix dm = new DataMatrix(dataMatrix.getRowNames(), "Probabilities", probabilities);
            dm.writeDataMatrix(false, dms, pathToOutputFolder, tableName, log);
        }
    }
    
    public int[] predict(DataMatrix dataMatrix)
    {
        if( ! UtilsForArray.equal(variableNames, dataMatrix.getColumnNames()) ) return null;
        return predict(dataMatrix.getMatrix());
    }
    
    public int[] predict(double[][] matrix)
    {
        int[] predictedIndices = new int[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
            predictedIndices[i] = predict(matrix[i]);
        return predictedIndices;
    }
    
    protected int predict(double[] rowOfMatrix)
    {
        return 0;
    }
    
    private void calculateAccompaniedInformation()
    {
        trueClassificationRates = ModelUtils.getTrueClassificationRates(responseIndices, predictedIndices, distinctClassNames);
        extendedPredictions = getExtendedPredictions();
    }
    
    private DataMatrixString getExtendedPredictions()
    {
        String[] response = transform(responseIndices), predictedResponse = transform(predictedIndices);
        String[][] matrix = new String[predictedResponse.length][];
        for(int i = 0; i < predictedResponse.length; i ++ )
            matrix[i] = new String[]{response[i], predictedResponse[i], response[i].equals(predictedResponse[i]) ? "+" : "-"};
        return new DataMatrixString(objectNames, new String[]{responseName, responseName + "_predicted", "is_predicted_correctly"}, matrix);
    }
    
    public String[] transform(int[] indices)
    {
        return UtilsForArray.transformIntArrayToStringArray(indices, distinctClassNames);
    }
    
    public void saveModel(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToStringTable(new String[]{CLASSIFICATION_TYPE, RESPONSE_NAME}, NAME_OF_COLUMN_WITH_CLASSIFICATION_TYPE_AND_RESPONSE_NAME, new String[]{classificationType, responseName}, pathToOutputFolder, NAME_OF_TABLE_WITH_CLASSIFICATION_TYPE_AND_RESPONSE_NAME);
        TableAndFileUtils.writeColumnToStringTable(distinctClassNames, "distinct_class_names", distinctClassNames, pathToOutputFolder, NAME_OF_TABLE_WITH_DISTINCT_CLASS_NAMES);
        saveModelParticular(pathToOutputFolder);
        if( doCalculateAccompaniedInformationWhenFit )
        {
            trueClassificationRates.writeDataMatrix(false, pathToOutputFolder, "True_classification_rates", log);
            if( probabilities == null )
                extendedPredictions.writeDataMatrixString(false, pathToOutputFolder, "Response_predicted", log);
            else
            {
                DataMatrix dm = new DataMatrix(objectNames, "Probabilities", probabilities);
                dm.writeDataMatrix(false, extendedPredictions, pathToOutputFolder, "Response_predicted", log);
            }
            saveAccompaniedSpecificInformation(pathToOutputFolder);
        }
    }

    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        TableAndFileUtils.writeColumnToStringTable(variableNames, "variable_names", variableNames, pathToOutputFolder, NAME_OF_TABLE_WITH_VARIABLE_NAMES);
    }

    public String[] getVariableNames()
    {
        return variableNames;
    }
    
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {}
    
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {}
    
    protected void fitModelParticular(double[][] matrix, Object[] additionalInputParameters)
    {}
    
    public void calculateAccompaniedSpecificInformation()
    {}
    
    /************************* static methods ******************************/

    public static ClassificationModel loadModel(DataElementPath pathToInputFolder)
    {
        DataMatrixString dms = new DataMatrixString(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_CLASSIFICATION_TYPE_AND_RESPONSE_NAME), new String[]{NAME_OF_COLUMN_WITH_CLASSIFICATION_TYPE_AND_RESPONSE_NAME});
        switch( dms.getColumn(0)[0] )
        {
            case CLASSIFICATION_1_LDA   : return new FisherDiscriminantModel(pathToInputFolder);
            case CLASSIFICATION_2_MLM   : return new MaximumLikelihoodModel(pathToInputFolder);
            case CLASSIFICATION_3_SVM_R : return new SupportVectorMachineClassificationModelFromR(pathToInputFolder);
            case CLASSIFICATION_4_PER   : return new PerceptronModel(pathToInputFolder);
            case CLASSIFICATION_5_LRM   : return new LogisticRegressionClassificationModel(pathToInputFolder);
            default                     : return null;
        }
    }
    
    public static ClassificationModel createModel(String classificationType, String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        ClassificationModel classificationModel = null;
        switch( classificationType )
        {
            case CLASSIFICATION_1_LDA   : classificationModel = new FisherDiscriminantModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case CLASSIFICATION_2_MLM   : classificationModel = new MaximumLikelihoodModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case CLASSIFICATION_3_SVM_R : classificationModel = new SupportVectorMachineClassificationModelFromR(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case CLASSIFICATION_4_PER   : classificationModel = new PerceptronModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            case CLASSIFICATION_5_LRM   : classificationModel = new LogisticRegressionClassificationModel(responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit); break;
            default                     : return null;
        }
        return classificationModel.isModelFitted() ? classificationModel : null; 
    }
    
    public static String[] getAvailableClassificationTypes()
    {
        return new String[]{CLASSIFICATION_1_LDA, CLASSIFICATION_2_MLM, CLASSIFICATION_3_SVM_R, CLASSIFICATION_4_PER, CLASSIFICATION_5_LRM};
    }
    
    protected static Logger log = Logger.getLogger(ClassificationModel.class.getName());
}
