/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;

/**
 * @author yura
 * Input:
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = DataElementPath pathToFolderWithRmodel;
 *          additionalInputParameters[1] = String[] inputRobjectsNames;
 *          additionalInputParameters[2] = Object[] inputRobjects;
 */
public class SupportVectorMachineRegressionModelFromR extends RegressionModelFromR
{
    private static final String NAME_OF_FILE_WITH_R_SCRIPT_TO_CREATE_R_MODEL = "CreateModelForSupportVectorMachineRegression";
    private static final String NAME_OF_FILE_WITH_R_SCRIPT_TO_LOAD_R_MODEL_AND_PREDICT = "LoadModelForSupportVectorMachineRegression";
    private static final String[] OUTPUT_R_OBJECTS_NAMES = new String[]{"predictedResponse"};
    private static final String REGRESSION_R_MODEL_FILE_NAME = "R_support_vector_machine_regression_model";

    public SupportVectorMachineRegressionModelFromR(String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(RegressionModel.REGRESSION_7_SVM_R, responseName, response, dataMatrix, extendAdditionalInputParameters(additionalInputParameters), doCalculateAccompaniedInformationWhenFit);
    }
    
    public SupportVectorMachineRegressionModelFromR(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    /**************** static methods ************************/
    
    private static Object[] extendAdditionalInputParameters(Object[] additionalInputParameters)
    {
        return new Object[]{additionalInputParameters[0], REGRESSION_R_MODEL_FILE_NAME, NAME_OF_FILE_WITH_R_SCRIPT_TO_CREATE_R_MODEL, OUTPUT_R_OBJECTS_NAMES, NAME_OF_FILE_WITH_R_SCRIPT_TO_LOAD_R_MODEL_AND_PREDICT, additionalInputParameters[1], additionalInputParameters[2]};
    }
}
