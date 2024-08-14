/* $Id$ */

package biouml.plugins.machinelearning.classification_models;

import biouml.plugins.machinelearning.utils.DataMatrix;
import ru.biosoft.access.core.DataElementPath;

/**
 * @author yura
 * Input:
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = DataElementPath pathToFolderWithRmodel;
 *          additionalInputParameters[1] = String[] inputRobjectsNames;
 *          additionalInputParameters[2] = Object[] inputRobjects;
 *
 */
public class SupportVectorMachineClassificationModelFromR extends ClassificationModelFromR
{
    private static final String NAME_OF_FILE_WITH_R_SCRIPT_TO_CREATE_R_MODEL = "CreateModelForSupportVectorMachineClassification";
    private static final String NAME_OF_FILE_WITH_R_SCRIPT_TO_LOAD_R_MODEL_AND_PREDICT = "LoadModelForSupportVectorMachineClassification";
    private static final String[] OUTPUT_R_OBJECTS_NAMES = new String[]{"predictedResponse"};
    private static final String CLASSIFICATION_R_MODEL_FILE_NAME = "R_support_vector_machine_classification_model";

    public SupportVectorMachineClassificationModelFromR(String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(ClassificationModel.CLASSIFICATION_3_SVM_R, responseName, response, dataMatrix, extendAdditionalInputParameters(additionalInputParameters), doCalculateAccompaniedInformationWhenFit);
    }
    
    public SupportVectorMachineClassificationModelFromR(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    /**************** static methods ************************/
    
    private static Object[] extendAdditionalInputParameters(Object[] additionalInputParameters)
    {
        return new Object[]{additionalInputParameters[0], CLASSIFICATION_R_MODEL_FILE_NAME, NAME_OF_FILE_WITH_R_SCRIPT_TO_CREATE_R_MODEL, OUTPUT_R_OBJECTS_NAMES, NAME_OF_FILE_WITH_R_SCRIPT_TO_LOAD_R_MODEL_AND_PREDICT, additionalInputParameters[1], additionalInputParameters[2]};
    }
}
