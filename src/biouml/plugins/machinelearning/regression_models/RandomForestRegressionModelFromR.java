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

public class RandomForestRegressionModelFromR extends RegressionModelFromR
{
    private static final String NAME_OF_FILE_WITH_R_SCRIPT_TO_CREATE_R_MODEL = "CreateModelForRandomForestRegression";
    private static final String NAME_OF_FILE_WITH_R_SCRIPT_TO_LOAD_R_MODEL_AND_PREDICT = "LoadModelForRandomForestRegression";
    private static final String[] OUTPUT_R_OBJECTS_NAMES = new String[]{"predictedResponse", "importance", "importanceColumnNames"};
    private static final String REGRESSION_R_MODEL_FILE_NAME = "R_random_forest_regression_model";

    public RandomForestRegressionModelFromR(String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(RegressionModel.REGRESSION_6_RF_R, responseName, response, dataMatrix, extendAdditionalInputParameters(additionalInputParameters), doCalculateAccompaniedInformationWhenFit);
    }
    
    public RandomForestRegressionModelFromR(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {
        double[][] importance = (double[][])outputRObjects[1];
        String[] importanceColumnNames = (String[])outputRObjects[2];
        DataMatrix dm = new DataMatrix(variableNames, importanceColumnNames, importance);
        dm.writeDataMatrix(false, pathToOutputFolder, "Variable_importance", log);
    }
    
    /**************** static methods ************************/
    
    private static Object[] extendAdditionalInputParameters(Object[] additionalInputParameters)
    {
        return new Object[]{additionalInputParameters[0], REGRESSION_R_MODEL_FILE_NAME, NAME_OF_FILE_WITH_R_SCRIPT_TO_CREATE_R_MODEL, OUTPUT_R_OBJECTS_NAMES, NAME_OF_FILE_WITH_R_SCRIPT_TO_LOAD_R_MODEL_AND_PREDICT, additionalInputParameters[1], additionalInputParameters[2]};
    }
}
