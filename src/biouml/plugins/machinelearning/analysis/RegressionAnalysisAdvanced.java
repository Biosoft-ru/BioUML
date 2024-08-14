/* $Id$ */

package biouml.plugins.machinelearning.analysis;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixChar;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.classification_models.ClassificationModel;
import biouml.plugins.machinelearning.regression_models.CombinedRegressionModel;
import biouml.plugins.machinelearning.regression_models.LinearRegressionModel;
import biouml.plugins.machinelearning.regression_models.PrincipalComponentRegressionModel;
import biouml.plugins.machinelearning.regression_models.RegressionModel;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

/**
 * @author yura
 * In order to add new regression model, it is necessary to do:
 * 1. Create model as extension of RegressionModel.
 * 2. To add 'public static final String' with model name to RegressionModel.
 * 3. To change methods RegressionModel.loadModel() and RegressionModel.createModel()
 * 4. To change method RegressionModel.getAvailableRegressionTypes().
 * 5. To change method LinearRegressionModel.doAddInterceptToRegression().
 * 6. To add the treatment of parameters of new model in method createModel() in this Class.
 */

// TODO: Add K-folds to cross-validation.

public class RegressionAnalysisAdvanced extends AnalysisMethodSupport<RegressionAnalysisAdvanced.RegressionAnalysisAdvancedParameters>
{
    public static final String OUTLIER_DETECTION_MODE = "Outlier detection";

    public RegressionAnalysisAdvanced(DataCollection<?> origin, String name)
    {
        super(origin, name, new RegressionAnalysisAdvancedParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info(" *************************************************");
        log.info(" * Regression analysis. There are 5 modes :      *");
        log.info(" * 1. Create and save regression model           *");
        log.info(" * 2. Load regression model and predict response *");
        log.info(" * 3. Cross-validate regression model            *");
        log.info(" * 4. Variable (feature) selection               *");
        log.info(" * 5. Outlier detection                          *");
        log.info(" *************************************************");

        String regressionMode = parameters.getRegressionMode();
        DataElementPath pathToDataMatrix = parameters.getPathToDataMatrix();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        
        // 1. Treatment of LOAD_AND_PREDICT_MODE.
        if( regressionMode.equals(ModelUtils.LOAD_AND_PREDICT_MODE) )
        {
            DataElementPath pathToFolderWithSavedModel = parameters.getPathToFolderWithSavedModel();
            RegressionModel regressionModel = RegressionModel.loadModel(pathToFolderWithSavedModel);
            String[] variableNames = regressionModel.getVariableNames();
            int interceptIndex =  ArrayUtils.indexOf(variableNames, ModelUtils.INTERCEPT);
            String[] names = interceptIndex < 0 ? variableNames : (String[])ArrayUtils.remove(variableNames, interceptIndex);
            DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, names);
            if( interceptIndex >= 0 )
                dataMatrix.addColumn(ModelUtils.INTERCEPT, UtilsForArray.getConstantArray(dataMatrix.getSize(), 1.0), interceptIndex);
            dataMatrix.removeRowsWithMissingData();
            DataMatrix dm = new DataMatrix(dataMatrix.getRowNames(), "Predicted_values", regressionModel.predict(dataMatrix));
            dm.writeDataMatrix(false, pathToOutputFolder, "Predicted_values", log);
            return pathToOutputFolder.getDataCollection();
        }
        
        // 2. Read data matrix and response.
        String[] variableNames = parameters.getVariableNames();
        String regressionType = parameters.getRegressionType();
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, variableNames);
        if( LinearRegressionModel.doAddInterceptToRegression(regressionType) )
        {
            int interceptIndex = dataMatrix.getColumnNames().length;
            dataMatrix.addColumn(ModelUtils.INTERCEPT, UtilsForArray.getConstantArray(dataMatrix.getSize(), 1.0), interceptIndex);
        }
        String responseName = parameters.getResponseName();
        DataMatrix dm = new DataMatrix(pathToDataMatrix, new String[]{responseName});
        double[] response = dm.getColumn(responseName);
        response = dataMatrix.removeRowsWithMissingData(response);
        
        // 3. Treatment of remained modes.
        switch( regressionMode )
        {
            case ModelUtils.CREATE_AND_SAVE_MODE    : RegressionModel regressionModel = createModel(regressionType, responseName, response, dataMatrix, true, pathToOutputFolder);
                                                      regressionModel.saveModel(pathToOutputFolder); break;
            case ModelUtils.CROSS_VALIDATION_MODE   : int percentageOfDataForTraining = parameters.getParametersForCrossValidation().getPercentageOfDataForTraining();
                                                      Object[] objects = ModelUtils.splitDataSet(dataMatrix, response, response.length * percentageOfDataForTraining / 100, 0);
                                                      regressionModel = createModel(regressionType, responseName, (double[])objects[2], (DataMatrix)objects[0], false, pathToOutputFolder);
                                                      double[] predictedResponse = regressionModel.predict((DataMatrix)objects[0]);
                                                      DataMatrix accuracySummary = ModelUtils.getSummaryOnModelAccuracy((double[])objects[2], predictedResponse, dataMatrix.getColumnNames().length);
                                                      accuracySummary.replaceColumnName("Value", "Training set");
                                                      predictedResponse = regressionModel.predict((DataMatrix)objects[1]);
                                                      DataMatrix accuracySummaryForTest = ModelUtils.getSummaryOnModelAccuracy((double[])objects[3], predictedResponse, dataMatrix.getColumnNames().length);
                                                      accuracySummaryForTest.replaceColumnName("Value", "Test set");
                                                      accuracySummary.addAnotherDataMatrixColumnWise(accuracySummaryForTest);
                                                      accuracySummary.writeDataMatrix(false, pathToOutputFolder, "Cross_validation_accuracy", log); break;
            case ModelUtils.VARIABLE_SELECTION_MODE : int numberOfSelectedVariables = Math.max(1, parameters.getParametersForVariableSelection().getNumberOfSelectedVariables());
                                                      String variableSelectionCriterion = parameters.getParametersForVariableSelection().getVariableSelectionCriterion();
                                                      String variableSelectionType = parameters.getParametersForVariableSelection().getVariableSelectionType();
                                                      Object[]additionalInputParameters = null;
                                                      switch( variableSelectionType )
                                                      {
                                                          case ModelUtils.STEPWISE_FORWARD_VARIABLE_ADDITION : additionalInputParameters = createAdditionalInputParameters(regressionType, responseName, response, dataMatrix, false, pathToOutputFolder);
                                                                                                               dm = ModelUtils.stepwiseForwardVariableSelectionInRegression(regressionType, responseName, response, dataMatrix, numberOfSelectedVariables, variableSelectionCriterion, additionalInputParameters, false, jobControl, 0, 100);
                                                                                                               break;
                                                          case ModelUtils.STEPWISE_BACKWARD_ELIMINATION      : log.info(ModelUtils.STEPWISE_BACKWARD_ELIMINATION + " is under construction");
                                                      }
                                                      dm.writeDataMatrix(false, pathToOutputFolder, "selected_variables", log);
                                                      dm = dataMatrix.getSubDataMatrixColumnWise(dm.getRowNames());
                                                      dm.writeDataMatrix(true, pathToOutputFolder, "selected_data_matrix", log);
                                                      regressionModel = RegressionModel.createModel(regressionType, responseName, response, dm, additionalInputParameters, true);
                                                      regressionModel.saveModel(pathToOutputFolder); break;
            case OUTLIER_DETECTION_MODE             : double multiplierForSigma = parameters.getParametersForOutlierDetection().getMultiplierForSigma();
                                                      int numberOfOutlierDetectionSteps =  parameters.getParametersForOutlierDetection().getNumberOfOutlierDetectionSteps();
                                                      additionalInputParameters = createAdditionalInputParameters(regressionType, responseName, response, dataMatrix, false, pathToOutputFolder);
                                                      objects = ModelUtils.outlierDetection(numberOfOutlierDetectionSteps,  multiplierForSigma, regressionType, responseName, response, dataMatrix, additionalInputParameters, jobControl, 0, 100);
                                                      dm = (DataMatrix)objects[0];
                                                      dm.writeDataMatrix(false, pathToOutputFolder, "summary_on_outlier_detection", log);
                                                      DataMatrixChar dmc = (DataMatrixChar)objects[1];
                                                      dmc.writeDataMatrixChar(pathToOutputFolder, "outlier_indicators", log);
                                                      regressionModel = (RegressionModel)objects[2];
                                                      regressionModel.saveModel(pathToOutputFolder); break;
        }
        return pathToOutputFolder.getDataCollection();
    }

    private Object[] createAdditionalInputParameters(String regressionType, String responseName, double[] response, DataMatrix dataMatrix, boolean doCalculateAccompaniedInformationWhenFit, DataElementPath pathToOutputFolder)
    {
        switch( regressionType )
        {
            case RegressionModel.REGRESSION_1_OLS   : int maxNumberOfRotations = parameters.getParametersForOlsRegression().getMaxNumberOfRotations();
                                                      double epsForRotations = parameters.getParametersForOlsRegression().getEpsForRotations();
                                                      return new Object[]{maxNumberOfRotations, epsForRotations};
            case RegressionModel.REGRESSION_2_WLS   : maxNumberOfRotations = parameters.getParametersForWlsRegression().getMaxNumberOfRotations();
                                                      epsForRotations = parameters.getParametersForWlsRegression().getEpsForRotations();
                                                      return new Object[]{maxNumberOfRotations, epsForRotations};
            case RegressionModel.REGRESSION_3_PC    : maxNumberOfRotations = parameters.getParametersForPcRegression().getMaxNumberOfRotations();
                                                      epsForRotations = parameters.getParametersForPcRegression().getEpsForRotations();
                                                      int numberOfPrincipalComponents = parameters.getParametersForPcRegression().getNumberOfPrincipalComponents();
                                                      String principalComponentSortingType = parameters.getParametersForPcRegression().getPrincipalComponentSortingType();
                                                      return new Object[]{maxNumberOfRotations, epsForRotations, numberOfPrincipalComponents, principalComponentSortingType};
            case RegressionModel.REGRESSION_4_RT    : int minimalNodeSize = parameters.getParametersForRtRegression().getMinimalNodeSize();
                                                      double minimalVariance = parameters.getParametersForRtRegression().getMinimalVariance();
                                                      return new Object[]{minimalNodeSize, minimalVariance};
            case RegressionModel.REGRESSION_6_RF_R  : return new Object[]{pathToOutputFolder, null, null};
            case RegressionModel.REGRESSION_7_SVM_R : return new Object[]{pathToOutputFolder, null, null};
            case RegressionModel.REGRESSION_9_RIDGE : maxNumberOfRotations = parameters.getParametersForRidgeRegression().getMaxNumberOfRotations();
                                                      epsForRotations = parameters.getParametersForRidgeRegression().getEpsForRotations();
                                                      double shrinkageParameter = parameters.getParametersForRidgeRegression().getShrinkageParameter();
                                                      return new Object[]{maxNumberOfRotations, epsForRotations, shrinkageParameter};
            case RegressionModel.REGRESSION_10_COMB : int numberOfRegressions = parameters.getParametersForCombinedRegression().getNumberOfRegressions();
                                                      String regressionsType = parameters.getParametersForCombinedRegression().getRegressionsType();
                                                      int numberOfVariablesForRegressions = parameters.getParametersForCombinedRegression().getNumberOfVariablesForRegressions();
                                                      int numberOfOutlierDetectionSteps = parameters.getParametersForCombinedRegression().getNumberOfOutlierDetectionSteps();
                                                      double multiplierForSigma = parameters.getParametersForCombinedRegression().getMultiplierForSigma();
                                                      String classificationType = parameters.getParametersForCombinedRegression().getClassificationType();
                                                      int numberOfVariablesForClassification = parameters.getParametersForCombinedRegression().getNumberOfVariablesForClassification();
                                                      String typeOfVariableSelectionInClassification = parameters.getParametersForCombinedRegression().getTypeOfVariableSelectionInClassification();
                                                      Object[] additionalInputParametersForClassification = null, additionalInputParametersForRegressions = null;
                                                      return new Object[]{typeOfVariableSelectionInClassification, numberOfVariablesForClassification, regressionsType, additionalInputParametersForRegressions, classificationType, additionalInputParametersForClassification, numberOfRegressions, numberOfVariablesForRegressions, numberOfOutlierDetectionSteps, multiplierForSigma};
            default                                 : return null;
        }
    }

    private RegressionModel createModel(String regressionType, String responseName, double[] response, DataMatrix dataMatrix, boolean doCalculateAccompaniedInformationWhenFit, DataElementPath pathToOutputFolder)
    {
        Object[] additionalInputParameters = createAdditionalInputParameters(regressionType, responseName, response, dataMatrix, doCalculateAccompaniedInformationWhenFit, pathToOutputFolder);
        return RegressionModel.createModel(regressionType, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    /************************************************************/
    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_REGRESSION_MODE = "Regression mode";
        public static final String PD_REGRESSION_MODE = "Select regression mode";
        
        public static final String PN_REGRESSION_TYPE = "Regression type";
        public static final String PD_REGRESSION_TYPE = "Select regression type";
        
        public static final String PN_PATH_TO_DATA_MATRIX = "Path to data matrix";
        public static final String PD_PATH_TO_DATA_MATRIX = "Path to table or file with data matrix";
        
        public static final String PN_VARIABLE_NAMES = "Variable names";
        public static final String PD_VARIABLE_NAMES = "Select variable names";
        
        public static final String PN_RESPONSE_NAME = "Response name";
        public static final String PD_RESPONSE_NAME = "Select response name";
        
        public static final String PN_PATH_TO_FOLDER_WITH_SAVED_MODEL = "Path to folder with saved model";
        public static final String PD_PATH_TO_FOLDER_WITH_SAVED_MODEL = "Path to folder with saved model";
        
        public static final String PN_MAX_NUMBER_OF_ROTATIONS = "Max number of rotations";
        public static final String PD_MAX_NUMBER_OF_ROTATIONS = "Maximal number of rotations for calculation of inverse matrix or eigen vectors";

        public static final String PN_EPS_FOR_ROTATIONS = "Epsilon for rotations";
        public static final String PD_EPS_FOR_ROTATIONS = "Epsilon for calculation of inverse matrix or eigen vectors";
        
        public static final String PN_NUMBER_OF_PRINCIPAL_COMPONENTS = "Number of principal components";
        public static final String PD_NUMBER_OF_PRINCIPAL_COMPONENTS = "Number of principal components";
        
        public static final String PN_PRINCIPAL_COMPONENT_SORTING_TYPE = "Principal component sorting type";
        public static final String PD_PRINCIPAL_COMPONENT_SORTING_TYPE = "Sorting type of principal components";
        
        public static final String PN_MINIMAL_NODE_SIZE = "Minimal node size";
        public static final String PD_MINIMAL_NODE_SIZE = "Minimal size of node";

        public static final String PN_MINIMAL_VARIANCE = "Minimal variance";
        public static final String PD_MINIMAL_VARIANCE = "Minimal variance";
        
        public static final String PN_SHINKAGE_PARAMETER = "Shrinkage parameter";
        public static final String PD_SHINKAGE_PARAMETER = "Shrinkage parameter, k >= 0";

        public static final String PN_PARAMETERS_FOR_OLS_REGRESSION = "Parameters for OLS-regression";
        public static final String PD_PARAMETERS_FOR_OLS_REGRESSION = "Please, determine parameters for Odinary least squares regression";
        
        public static final String PN_PARAMETERS_FOR_WLS_REGRESSION = "Parameters for WLS-regression";
        public static final String PD_PARAMETERS_FOR_WLS_REGRESSION = "Please, determine parameters for Weighted least squares regression";
        
        public static final String PN_PARAMETERS_FOR_PC_REGRESSION = "Parameters for PC-regression";
        public static final String PD_PARAMETERS_FOR_PC_REGRESSION = "Please, determine parameters for Principal component regression";
        
        public static final String PN_PARAMETERS_FOR_RT_REGRESSION = "Parameters for Tree-based regression";
        public static final String PD_PARAMETERS_FOR_RT_REGRESSION = "Please, determine parameters for Tree-based regression";
        
        public static final String PN_PARAMETERS_FOR_RIDGE_REGRESSION = "Parameters for Ridge regression";
        public static final String PD_PARAMETERS_FOR_RIDGE_REGRESSION = "Please, determine parameters for Ridge regression";
        
        public static final String PN_PARAMETERS_FOR_COMBINED_REGRESSION = "Parameters for combined regression";
        public static final String PD_PARAMETERS_FOR_COMBINED_REGRESSION = "Please, determine parameters for combined regression";
        
        public static final String PN_PARAMETERS_FOR_CROSS_VALIDATION = "Parameters for cross-validation";
        public static final String PD_PARAMETERS_FOR_CROSS_VALIDATION = "Please, determine parameters for cross-validation";
       
        public static final String PN_PARAMETERS_FOR_VARIABLE_SELECTION = "Parameters for variable selection";
        public static final String PD_PARAMETERS_FOR_VARIABLE_SELECTION = "Parameters for variable selection";
        
        public static final String PN_PARAMETERS_FOR_OUTLIER_DETECTION = "Parameters for outlier detection";
        public static final String PD_PARAMETERS_FOR_OUTLIER_DETECTION = "Parameters for outlier detection";
        
        public static final String PN_PERCENTAGE_OF_DATA_FOR_TRAINING = "Percentage of data for training";
        public static final String PD_PERCENTAGE_OF_DATA_FOR_TRAINING = "Proportion (in %) of data for training";
        
        public static final String PN_NUMBER_OF_SELECTED_VARIABLES = "Number of selected variables";
        public static final String PD_NUMBER_OF_SELECTED_VARIABLES = "Number of selected variables";
        
        public static final String PN_VARIABLE_SELECTION_CRITERION = "Variable selection criterion";
        public static final String PD_VARIABLE_SELECTION_CRITERION = "Please, determine variable selection criterion";
        
        public static final String PN_VARIABLE_SELECTION_TYPE = "Variable selection type";
        public static final String PD_VARIABLE_SELECTION_TYPE = " Please, determine variable selection type";
        
        public static final String PN_MULTIPLIER_FOR_SIGMA = "Multiplier for sigma, t";
        public static final String PD_MULTIPLIER_FOR_SIGMA = "Observation x is outlier if Abs(x - predicted x) > t * sigma";
        
        public static final String PN_NUMBER_OF_OUTLIER_DETECTION_STEPS = "Number of outlier detection steps";
        public static final String PD_NUMBER_OF_OUTLIER_DETECTION_STEPS = "Number of outlier detection steps";
        
        public static final String PN_NUMBER_OF_REGRESSIONS = "Number of regressions";
        public static final String PD_NUMBER_OF_REGRESSIONS = "Number of regressions";
        
        public static final String PN_REGRESSIONS_TYPE = "Regressions type";
        public static final String PD_REGRESSIONS_TYPE = "Select regressions type";
        
        public static final String PN_NUMBER_OF_VARIABLES_FOR_REGRESSIONS = "Number of variables for regressions";
        public static final String PD_NUMBER_OF_VARIABLES_FOR_REGRESSIONS = "Number of variables for each regression";
        
        public static final String PN_CLASSIFICATION_TYPE = "Classification type";
        public static final String PD_CLASSIFICATION_TYPE = "Select classification type";
        
        public static final String PN_NUMBER_OF_VARIABLES_FOR_CLASSIFICATION = "Number of variables for classification";
        public static final String PD_NUMBER_OF_VARIABLES_FOR_CLASSIFICATION = "Number of variables for classification";
        
        public static final String PN_TYPE_OF_VARIABLESELECTION_IN_CLASSIFIaCATION = "Type of variable selection in classification";
        public static final String PD_TYPE_OF_VARIABLESELECTION_IN_CLASSIFIaCATION = "Type of variable selection in classification";

        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }
    
    public static class AllParameters extends AbstractAnalysisParameters
    {
        private String regressionMode = ModelUtils.CREATE_AND_SAVE_MODE;
        private String regressionType = RegressionModel.REGRESSION_1_OLS;
        private DataElementPath pathToDataMatrix;
        private String[] variableNames;
        private String responseName;
        private DataElementPath pathToFolderWithSavedModel;
        private int maxNumberOfRotations = MatrixUtils.DEFAULT_MAX_NUMBER_OF_ROTATIONS;
        private double epsForRotations = MatrixUtils.DEFAULT_EPS_FOR_ROTATIONS;
        private int percentageOfDataForTraining = 50;
        private int numberOfPrincipalComponents = 1;
        private String principalComponentSortingType;
        private int minimalNodeSize = 3;
        private double minimalVariance = 0.03;
        private double shrinkageParameter = 0.1;
        private int numberOfSelectedVariables = 15;
        private String variableSelectionCriterion = ModelUtils.PEARSON_CORRELATION_CRITERION;
        private String variableSelectionType = ModelUtils.STEPWISE_FORWARD_VARIABLE_ADDITION;
        private double multiplierForSigma = 3.0;
        private int numberOfOutlierDetectionSteps = 5;
        private int numberOfRegressions = 2;
        private String regressionsType = RegressionModel.REGRESSION_1_OLS;
        private int numberOfVariablesForRegressions = 10;
        private String classificationType = ClassificationModel.CLASSIFICATION_1_LDA;
        private int numberOfVariablesForClassification = 7;
        private String typeOfVariableSelectionInClassification = CombinedRegressionModel.SELECT_VARIABLES_IN_CLASSIFICATION_BY_MAXIMIZATION_OF_CORRELATION;

        private DataElementPath pathToOutputFolder;
        
        @PropertyName(MessageBundle.PN_REGRESSION_MODE)
        @PropertyDescription(MessageBundle.PD_REGRESSION_MODE)
        public String getRegressionMode()
        {
            return regressionMode;
        }
        public void setRegressionMode(String regressionMode)
        {
            Object oldValue = this.regressionMode;
            this.regressionMode = regressionMode;
            firePropertyChange("*", oldValue, regressionMode);
        }
        
        @PropertyName(MessageBundle.PN_REGRESSION_TYPE)
        @PropertyDescription(MessageBundle.PD_REGRESSION_TYPE)
        public String getRegressionType()
        {
            return regressionType;
        }
        public void setRegressionType(String regressionType)
        {
            Object oldValue = this.regressionType;
            this.regressionType = regressionType;
            firePropertyChange("*", oldValue, regressionType);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_DATA_MATRIX)
        @PropertyDescription(MessageBundle.PD_PATH_TO_DATA_MATRIX)
        public DataElementPath getPathToDataMatrix()
        {
            return pathToDataMatrix;
        }
        public void setPathToDataMatrix(DataElementPath pathToDataMatrix)
        {
            Object oldValue = this.pathToDataMatrix;
            this.pathToDataMatrix = pathToDataMatrix;
            firePropertyChange("pathToDataMatrix", oldValue, pathToDataMatrix);
        }
        
        @PropertyName(MessageBundle.PN_VARIABLE_NAMES)
        @PropertyDescription(MessageBundle.PD_VARIABLE_NAMES)
        public String[] getVariableNames()
        {
            return variableNames;
        }
        public void setVariableNames(String[] variableNames)
        {
            Object oldValue = this.variableNames;
            this.variableNames = variableNames;
            firePropertyChange("variableNames", oldValue, variableNames);
        }
        
        @PropertyName(MessageBundle.PN_RESPONSE_NAME)
        @PropertyDescription(MessageBundle.PD_RESPONSE_NAME)
        public String getResponseName()
        {
            return responseName;
        }
        public void setResponseName(String responseName)
        {
            Object oldValue = this.responseName;
            this.responseName = responseName;
            firePropertyChange("responseName", oldValue, responseName);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_SAVED_MODEL)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_SAVED_MODEL)
        public DataElementPath getPathToFolderWithSavedModel()
        {
            return pathToFolderWithSavedModel;
        }
        public void setPathToFolderWithSavedModel(DataElementPath pathToFolderWithSavedModel)
        {
            Object oldValue = this.pathToFolderWithSavedModel;
            this.pathToFolderWithSavedModel = pathToFolderWithSavedModel;
            firePropertyChange("pathToFolderWithSavedModel", oldValue, pathToFolderWithSavedModel);
        }
        
        @PropertyName(MessageBundle.PN_PERCENTAGE_OF_DATA_FOR_TRAINING)
        @PropertyDescription(MessageBundle.PD_PERCENTAGE_OF_DATA_FOR_TRAINING)
        public int getPercentageOfDataForTraining()
        {
            return percentageOfDataForTraining;
        }
        public void setPercentageOfDataForTraining(int percentageOfDataForTraining)
        {
            Object oldValue = this.percentageOfDataForTraining;
            this.percentageOfDataForTraining = percentageOfDataForTraining;
            firePropertyChange("percentageOfDataForTraining", oldValue, percentageOfDataForTraining);
        }
        
        @PropertyName(MessageBundle.PN_MAX_NUMBER_OF_ROTATIONS)
        @PropertyDescription(MessageBundle.PD_MAX_NUMBER_OF_ROTATIONS)
        public int getMaxNumberOfRotations()
        {
            return maxNumberOfRotations;
        }
        public void setMaxNumberOfRotations(int maxNumberOfRotations)
        {
            Object oldValue = this.maxNumberOfRotations;
            this.maxNumberOfRotations = maxNumberOfRotations;
            firePropertyChange("maxNumberOfRotations", oldValue, maxNumberOfRotations);
        }
        
        @PropertyName(MessageBundle.PN_EPS_FOR_ROTATIONS)
        @PropertyDescription(MessageBundle.PD_EPS_FOR_ROTATIONS)
        public double getEpsForRotations()
        {
            return epsForRotations;
        }
        public void setEpsForRotations(double epsForRotations)
        {
            Object oldValue = this.epsForRotations;
            this.epsForRotations = epsForRotations;
            firePropertyChange("epsForRotations", oldValue, epsForRotations);
        }
        
        @PropertyName(MessageBundle.PN_NUMBER_OF_PRINCIPAL_COMPONENTS)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_PRINCIPAL_COMPONENTS)
        public int getNumberOfPrincipalComponents()
        {
            return numberOfPrincipalComponents;
        }
        public void setNumberOfPrincipalComponents(int numberOfPrincipalComponents)
        {
            Object oldValue = this.numberOfPrincipalComponents;
            this.numberOfPrincipalComponents = numberOfPrincipalComponents;
            firePropertyChange("numberOfPrincipalComponents", oldValue, numberOfPrincipalComponents);
        }
        
        @PropertyName(MessageBundle.PN_PRINCIPAL_COMPONENT_SORTING_TYPE)
        @PropertyDescription(MessageBundle.PD_PRINCIPAL_COMPONENT_SORTING_TYPE)
        public String getPrincipalComponentSortingType()
        {
            return principalComponentSortingType;
        }
        public void setPrincipalComponentSortingType(String principalComponentSortingType)
        {
            Object oldValue = this.principalComponentSortingType;
            this.principalComponentSortingType = principalComponentSortingType;
            firePropertyChange("principalComponentSortingType", oldValue, principalComponentSortingType);
        }
        
        @PropertyName(MessageBundle.PN_MINIMAL_NODE_SIZE)
        @PropertyDescription(MessageBundle.PD_MINIMAL_NODE_SIZE)
        public int getMinimalNodeSize()
        {
            return minimalNodeSize;
        }
        public void setMinimalNodeSize(int minimalNodeSize)
        {
            Object oldValue = this.minimalNodeSize;
            this.minimalNodeSize = minimalNodeSize;
            firePropertyChange("minimalNodeSize", oldValue, minimalNodeSize);
        }

        @PropertyName(MessageBundle.PN_MINIMAL_VARIANCE)
        @PropertyDescription(MessageBundle.PD_MINIMAL_VARIANCE)
        public double getMinimalVariance()
        {
            return minimalVariance;
        }
        public void setMinimalVariance(double minimalVariance)
        {
            Object oldValue = this.minimalVariance;
            this.minimalVariance = minimalVariance;
            firePropertyChange("minimalVariance", oldValue, minimalVariance);
        }
        
        @PropertyName(MessageBundle.PN_SHINKAGE_PARAMETER)
        @PropertyDescription(MessageBundle.PD_SHINKAGE_PARAMETER)
        public double getShrinkageParameter()
        {
            return shrinkageParameter;
        }
        public void setShrinkageParameter(double shrinkageParameter)
        {
            Object oldValue = this.shrinkageParameter;
            this.shrinkageParameter = shrinkageParameter;
            firePropertyChange("shrinkageParameter", oldValue, shrinkageParameter);
        }
        
        @PropertyName(MessageBundle.PN_NUMBER_OF_SELECTED_VARIABLES)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_SELECTED_VARIABLES)
        public int getNumberOfSelectedVariables()
        {
            return numberOfSelectedVariables;
        }
        public void setNumberOfSelectedVariables(int numberOfSelectedVariables)
        {
            Object oldValue = this.numberOfSelectedVariables;
            this.numberOfSelectedVariables = numberOfSelectedVariables;
            firePropertyChange("numberOfSelectedVariables", oldValue, numberOfSelectedVariables);
        }
        
        @PropertyName(MessageBundle.PN_VARIABLE_SELECTION_CRITERION)
        @PropertyDescription(MessageBundle.PD_VARIABLE_SELECTION_CRITERION)
        public String getVariableSelectionCriterion()
        {
            return variableSelectionCriterion;
        }
        public void setVariableSelectionCriterion(String variableSelectionCriterion)
        {
            Object oldValue = this.variableSelectionCriterion;
            this.variableSelectionCriterion = variableSelectionCriterion;
            firePropertyChange("variableSelectionCriterion", oldValue, variableSelectionCriterion);
        }
        
        @PropertyName(MessageBundle.PN_VARIABLE_SELECTION_TYPE)
        @PropertyDescription(MessageBundle.PD_VARIABLE_SELECTION_TYPE)
        public String getVariableSelectionType()
        {
            return variableSelectionType;
        }
        public void setVariableSelectionType(String variableSelectionType)
        {
            Object oldValue = this.variableSelectionType;
            this.variableSelectionType = variableSelectionType;
            firePropertyChange("variableSelectionType", oldValue, variableSelectionType);
        }

        @PropertyName(MessageBundle.PN_MULTIPLIER_FOR_SIGMA)
        @PropertyDescription(MessageBundle.PD_MULTIPLIER_FOR_SIGMA)
        public double getMultiplierForSigma()
        {
            return multiplierForSigma;
        }
        public void setMultiplierForSigma(double multiplierForSigma)
        {
            Object oldValue = this.multiplierForSigma;
            this.multiplierForSigma = multiplierForSigma;
            firePropertyChange("multiplierForSigma", oldValue, multiplierForSigma);
        }
        
        @PropertyName(MessageBundle.PN_NUMBER_OF_OUTLIER_DETECTION_STEPS)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_OUTLIER_DETECTION_STEPS)
        public int getNumberOfOutlierDetectionSteps()
        {
            return numberOfOutlierDetectionSteps;
        }
        public void setNumberOfOutlierDetectionSteps(int numberOfOutlierDetectionSteps)
        {
            Object oldValue = this.numberOfOutlierDetectionSteps;
            this.numberOfOutlierDetectionSteps = numberOfOutlierDetectionSteps;
            firePropertyChange("numberOfOutlierDetectionSteps", oldValue, numberOfOutlierDetectionSteps);
        }
        
        @PropertyName(MessageBundle.PN_NUMBER_OF_REGRESSIONS)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_REGRESSIONS)
        public int getNumberOfRegressions()
        {
            return numberOfRegressions;
        }
        public void setNumberOfRegressions(int numberOfRegressions)
        {
            Object oldValue = this.numberOfRegressions;
            this.numberOfRegressions = numberOfRegressions;
            firePropertyChange("numberOfRegressions", oldValue, numberOfRegressions);
        }
        
        @PropertyName(MessageBundle.PN_REGRESSIONS_TYPE)
        @PropertyDescription(MessageBundle.PD_REGRESSIONS_TYPE)
        public String getRegressionsType()
        {
            return regressionsType;
        }
        public void setRegressionsType(String regressionsType)
        {
            Object oldValue = this.regressionsType;
            this.regressionsType = regressionsType;
            firePropertyChange("regressionsType", oldValue, regressionType);
        }

        @PropertyName(MessageBundle.PN_NUMBER_OF_VARIABLES_FOR_REGRESSIONS)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_VARIABLES_FOR_REGRESSIONS)
        public int getNumberOfVariablesForRegressions()
        {
            return numberOfVariablesForRegressions;
        }
        public void setNumberOfVariablesForRegressions(int numberOfVariablesForRegressions)
        {
            Object oldValue = this.numberOfVariablesForRegressions;
            this.numberOfVariablesForRegressions = numberOfVariablesForRegressions;
            firePropertyChange("numberOfVariablesForRegressions", oldValue, numberOfVariablesForRegressions);
        }
        
        @PropertyName(MessageBundle.PN_CLASSIFICATION_TYPE)
        @PropertyDescription(MessageBundle.PD_CLASSIFICATION_TYPE)
        public String getClassificationType()
        {
            return classificationType;
        }
        public void setClassificationType(String classificationType)
        {
            Object oldValue = this.classificationType;
            this.classificationType = classificationType;
            firePropertyChange("classificationType", oldValue, classificationType);
        }
        
        @PropertyName(MessageBundle.PN_NUMBER_OF_VARIABLES_FOR_CLASSIFICATION)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_VARIABLES_FOR_CLASSIFICATION)
        public int getNumberOfVariablesForClassification()
        {
            return numberOfVariablesForClassification;
        }
        public void setNumberOfVariablesForClassification(int numberOfVariablesForClassification)
        {
            Object oldValue = this.numberOfVariablesForClassification;
            this.numberOfVariablesForClassification = numberOfVariablesForClassification;
            firePropertyChange("numberOfVariablesForClassification", oldValue, numberOfVariablesForClassification);
        }
        
        @PropertyName(MessageBundle.PD_TYPE_OF_VARIABLESELECTION_IN_CLASSIFIaCATION)
        @PropertyDescription(MessageBundle.PN_TYPE_OF_VARIABLESELECTION_IN_CLASSIFIaCATION)
        public String getTypeOfVariableSelectionInClassification()
        {
            return typeOfVariableSelectionInClassification;
        }
        public void setTypeOfVariableSelectionInClassification(String typeOfVariableSelectionInClassification)
        {
            Object oldValue = this.typeOfVariableSelectionInClassification;
            this.typeOfVariableSelectionInClassification = typeOfVariableSelectionInClassification;
            firePropertyChange("typeOfVariableSelectionInClassification", oldValue, typeOfVariableSelectionInClassification);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_OUTPUT_FOLDER)
        @PropertyDescription(MessageBundle.PD_PATH_TO_OUTPUT_FOLDER)
        public DataElementPath getPathToOutputFolder()
        {
            return pathToOutputFolder;
        }
        public void setPathToOutputFolder(DataElementPath pathToOutputFolder)
        {
            Object oldValue = this.pathToOutputFolder;
            this.pathToOutputFolder = pathToOutputFolder;
            firePropertyChange("pathToOutputFolder", oldValue, pathToOutputFolder);
        }
    }

    public static class ParametersForOlsRegression extends AllParameters
    {}
    
    public static class ParametersForOlsRegressionBeanInfo extends BeanInfoEx2<ParametersForOlsRegression>
    {
        public ParametersForOlsRegressionBeanInfo()
        {
            super(ParametersForOlsRegression.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("maxNumberOfRotations");
            add("epsForRotations");
        }
    }
    
    public static class ParametersForWlsRegression extends AllParameters
    {}
    
    public static class ParametersForWlsRegressionBeanInfo extends BeanInfoEx2<ParametersForWlsRegression>
    {
        public ParametersForWlsRegressionBeanInfo()
        {
            super(ParametersForWlsRegression.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("maxNumberOfRotations");
            add("epsForRotations");
        }
    }
    
    public static class ParametersForPcRegression extends AllParameters
    {}
    
    public static class ParametersForPcRegressionBeanInfo extends BeanInfoEx2<ParametersForPcRegression>
    {
        public ParametersForPcRegressionBeanInfo()
        {
            super(ParametersForPcRegression.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("maxNumberOfRotations");
            add("epsForRotations");
            add("numberOfPrincipalComponents");
            add(new PropertyDescriptorEx("principalComponentSortingType", beanClass), PrincipalComponentSortingTypeSelector.class);
        }
    }
    
    public static class ParametersForRtRegression extends AllParameters
    {}
    
    public static class ParametersForRtRegressionBeanInfo extends BeanInfoEx2<ParametersForRtRegression>
    {
        public ParametersForRtRegressionBeanInfo()
        {
            super(ParametersForRtRegression.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("minimalNodeSize");
            add("minimalVariance");
        }
    }
    
    public static class ParametersForRidgeRegression extends AllParameters
    {}
    
    public static class ParametersForRidgeRegressionBeanInfo extends BeanInfoEx2<ParametersForRidgeRegression>
    {
        public ParametersForRidgeRegressionBeanInfo()
        {
            super(ParametersForRidgeRegression.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("maxNumberOfRotations");
            add("epsForRotations");
            add("shrinkageParameter");
        }
    }
    
    public static class ParametersForCombinedRegression extends AllParameters
    {}
    
    public static class ParametersForCombinedRegressionBeanInfo extends BeanInfoEx2<ParametersForCombinedRegression>
    {
        public ParametersForCombinedRegressionBeanInfo()
        {
            super(ParametersForCombinedRegression.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("numberOfRegressions");
            add(new PropertyDescriptorEx("regressionsType", beanClass), RegressionsTypeSelector.class);
            add("numberOfVariablesForRegressions");
            add("numberOfOutlierDetectionSteps");
            add("multiplierForSigma");
            add(new PropertyDescriptorEx("classificationType", beanClass), ClassificationTypeSelector.class);
            add("numberOfVariablesForClassification");
            add(new PropertyDescriptorEx("typeOfVariableSelectionInClassification", beanClass), SelectorOfTypeOfVariableSelectionInClassification.class);
        }
    }
    
    public static class ParametersForCrossValidation extends AllParameters
    {}
    
    public static class ParametersForCrossValidationBeanInfo extends BeanInfoEx2<ParametersForCrossValidation>
    {
        public ParametersForCrossValidationBeanInfo()
        {
            super(ParametersForCrossValidation.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("percentageOfDataForTraining");
        }
    }
    
    public static class ParametersForVariableSelection extends AllParameters
    {}
    
    public static class ParametersForVariableSelectionBeanInfo extends BeanInfoEx2<ParametersForVariableSelection>
    {
        public ParametersForVariableSelectionBeanInfo()
        {
            super(ParametersForVariableSelection.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            // add("numberOfS+electedVariables"); ??????
            add("numberOfSelectedVariables");
            add(new PropertyDescriptorEx("variableSelectionCriterion", beanClass), VariableSelectionCriterionSelector.class);
            add(new PropertyDescriptorEx("variableSelectionType", beanClass), VariableSelectionTypeSelector.class);
        }
    }
    
    public static class ParametersForOutlierDetection extends AllParameters
    {}
    
    public static class ParametersForOutlierDetectionBeanInfo extends BeanInfoEx2<ParametersForOutlierDetection>
    {
        public ParametersForOutlierDetectionBeanInfo()
        {
            super(ParametersForOutlierDetection.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("multiplierForSigma");
            add("numberOfOutlierDetectionSteps");
        }
    }
    
    public static class RegressionAnalysisAdvancedParameters extends AllParameters
    {
        ParametersForOlsRegression parametersForOlsRegression;
        ParametersForWlsRegression parametersForWlsRegression;
        ParametersForPcRegression parametersForPcRegression;
        ParametersForRtRegression parametersForRtRegression;
        ParametersForRidgeRegression parametersForRidgeRegression;
        ParametersForCombinedRegression parametersForCombinedRegression;
        ParametersForCrossValidation parametersForCrossValidation;
        ParametersForVariableSelection parametersForVariableSelection;
        ParametersForOutlierDetection parametersForOutlierDetection;
        
        public RegressionAnalysisAdvancedParameters()
        {
            setParametersForOlsRegression(new ParametersForOlsRegression());
            setParametersForWlsRegression(new ParametersForWlsRegression());
            setParametersForPcRegression(new ParametersForPcRegression());
            setParametersForRtRegression(new ParametersForRtRegression());
            setParametersForRidgeRegression(new ParametersForRidgeRegression());
            setParametersForCombinedRegression(new ParametersForCombinedRegression());
            setParametersForCrossValidation(new ParametersForCrossValidation());
            setParametersForVariableSelection(new ParametersForVariableSelection());
            setParametersForOutlierDetection(new ParametersForOutlierDetection());
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OLS_REGRESSION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OLS_REGRESSION)
        public ParametersForOlsRegression getParametersForOlsRegression()
        {
            return parametersForOlsRegression;
        }
        public void setParametersForOlsRegression(ParametersForOlsRegression parametersForOlsRegression)
        {
            Object oldValue = this.parametersForOlsRegression;
            this.parametersForOlsRegression = parametersForOlsRegression;
            firePropertyChange("parametersForOlsRegression", oldValue, parametersForOlsRegression);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_WLS_REGRESSION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_WLS_REGRESSION)
        public ParametersForWlsRegression getParametersForWlsRegression()
        {
            return parametersForWlsRegression;
        }
        public void setParametersForWlsRegression(ParametersForWlsRegression parametersForWlsRegression)
        {
            Object oldValue = this.parametersForWlsRegression;
            this.parametersForWlsRegression = parametersForWlsRegression;
            firePropertyChange("parametersForWlsRegression", oldValue, parametersForWlsRegression);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_PC_REGRESSION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_PC_REGRESSION)
        public ParametersForPcRegression getParametersForPcRegression()
        {
            return parametersForPcRegression;
        }
        public void setParametersForPcRegression(ParametersForPcRegression parametersForPcRegression)
        {
            Object oldValue = this.parametersForPcRegression;
            this.parametersForPcRegression = parametersForPcRegression;
            firePropertyChange("parametersForPcRegression", oldValue, parametersForPcRegression);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_RT_REGRESSION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_RT_REGRESSION)
        public ParametersForRtRegression getParametersForRtRegression()
        {
            return parametersForRtRegression;
        }
        public void setParametersForRtRegression(ParametersForRtRegression parametersForRtRegression)
        {
            Object oldValue = this.parametersForRtRegression;
            this.parametersForRtRegression = parametersForRtRegression;
            firePropertyChange("parametersForRtRegression", oldValue, parametersForRtRegression);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_RIDGE_REGRESSION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_RIDGE_REGRESSION)
        public ParametersForRidgeRegression getParametersForRidgeRegression()
        {
            return parametersForRidgeRegression;
        }
        public void setParametersForRidgeRegression(ParametersForRidgeRegression parametersForRidgeRegression)
        {
            Object oldValue = this.parametersForRidgeRegression;
            this.parametersForRidgeRegression = parametersForRidgeRegression;
            firePropertyChange("parametersForRidgeRegression", oldValue, parametersForRidgeRegression);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_COMBINED_REGRESSION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_COMBINED_REGRESSION)
        public ParametersForCombinedRegression getParametersForCombinedRegression()
        {
            return parametersForCombinedRegression;
        }
        public void setParametersForCombinedRegression(ParametersForCombinedRegression parametersForCombinedRegression)
        {
            Object oldValue = this.parametersForCombinedRegression;
            this.parametersForCombinedRegression = parametersForCombinedRegression;
            firePropertyChange("parametersForCombinedRegression", oldValue, parametersForCombinedRegression);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_CROSS_VALIDATION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_CROSS_VALIDATION)
        public ParametersForCrossValidation getParametersForCrossValidation()
        {
            return parametersForCrossValidation;
        }
        public void setParametersForCrossValidation(ParametersForCrossValidation parametersForCrossValidation)
        {
            Object oldValue = this.parametersForCrossValidation;
            this.parametersForCrossValidation = parametersForCrossValidation;
            firePropertyChange("parametersForCrossValidation", oldValue, parametersForCrossValidation);
        }

        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_VARIABLE_SELECTION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_VARIABLE_SELECTION)
        public ParametersForVariableSelection getParametersForVariableSelection()
        {
            return parametersForVariableSelection;
        }
        public void setParametersForVariableSelection(ParametersForVariableSelection parametersForVariableSelection)
        {
            Object oldValue = this.parametersForVariableSelection;
            this.parametersForVariableSelection = parametersForVariableSelection;
            firePropertyChange("parametersForVariableSelection", oldValue, parametersForVariableSelection);
        }

        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OUTLIER_DETECTION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OUTLIER_DETECTION)
        public ParametersForOutlierDetection getParametersForOutlierDetection()
        {
            return parametersForOutlierDetection;
        }
        
        public void setParametersForOutlierDetection(ParametersForOutlierDetection parametersForOutlierDetection)
        {
            Object oldValue = this.parametersForOutlierDetection;
            this.parametersForOutlierDetection = parametersForOutlierDetection;
            firePropertyChange("parametersForOutlierDetection", oldValue, parametersForOutlierDetection);
        }

        public boolean isRegressionTypeHidden()
        {
            return(getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE));
        }
        
        public boolean isPathToFolderWithSavedRegressionModelHidden()
        {
            return(! getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE));
        }
        
        public boolean areVariableNamesHidden()
        {
            return(getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE));
        }
        
        public boolean isResponseNameHidden()
        {
            return(getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE));
        }
        
        public boolean areParametersForOlsRegressionHidden()
        {
            return (getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getRegressionType().equals(RegressionModel.REGRESSION_1_OLS));
        }
        
        public boolean areParametersForWlsRegressionHidden()
        {
            return (getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getRegressionType().equals(RegressionModel.REGRESSION_2_WLS));
        }
        
        public boolean areParametersForPcRegressionHidden()
        {
            return (getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getRegressionType().equals(RegressionModel.REGRESSION_3_PC));
        }
        
        public boolean areParametersForRtRegressionHidden()
        {
            return (getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getRegressionType().equals(RegressionModel.REGRESSION_4_RT));
        }
        
        public boolean areParametersForRidgeRegressionHidden()
        {
            return (getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getRegressionType().equals(RegressionModel.REGRESSION_9_RIDGE));
        }
        
        public boolean areParametersForCombinedRegressionHidden()
        {
            return (getRegressionMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getRegressionType().equals(RegressionModel.REGRESSION_10_COMB));
        }

        public boolean areParametersForCrossValidationHidden()
        {
            return ( ! getRegressionMode().equals(ModelUtils.CROSS_VALIDATION_MODE));
        }
        
        public boolean areParametersForVariableSelectionHidden()
        {
            return ( ! getRegressionMode().equals(ModelUtils.VARIABLE_SELECTION_MODE));
        }
        
        public boolean areParametersForOutlierDetectionHidden()
        {
            return ( ! getRegressionMode().equals(OUTLIER_DETECTION_MODE));
        }
    }
    
    public static class RegressionModesSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return (String[])ArrayUtils.add(ModelUtils.getAvailableModes(), OUTLIER_DETECTION_MODE);
        }
    }
    
    public static class RegressionTypesSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return RegressionModel.getAvailableRegressionTypes();
        }
    }
    
    public static class VariableSelectionTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return ModelUtils.getAvailableMethodsForvariableSelection();
        }
    }
    
    public static class RegressionsTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[]{RegressionModel.REGRESSION_1_OLS, RegressionModel.REGRESSION_2_WLS};
        }
    }
    
    public static class ClassificationTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[]{ClassificationModel.CLASSIFICATION_1_LDA, ClassificationModel.CLASSIFICATION_2_MLM};
        }
    }
    
    public static class SelectorOfTypeOfVariableSelectionInClassification extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[]{CombinedRegressionModel.SELECT_VARIABLES_IN_CLASSIFICATION_BY_MAXIMIZATION_OF_CORRELATION, CombinedRegressionModel.SELECT_VARIABLES_IN_CLASSIFICATION_BY_TRUE_CLASSIFICATION_RATE};
        }
    }
    
    public static class  VariableSelectionCriterionSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[]{ModelUtils.PEARSON_CORRELATION_CRITERION, ModelUtils.SPEARMAN_CORRELATION_CRITERION};
        }
    }
    
    public static class PrincipalComponentSortingTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return PrincipalComponentRegressionModel.getAvailableTypesOfPcSorting();
        }
    }
    
    public static class VariableNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((RegressionAnalysisAdvancedParameters)getBean()).getPathToDataMatrix();
                String[] columnNames = TableAndFileUtils.getColumnNames(pathToDataMatrix);
                Arrays.sort(columnNames, String.CASE_INSENSITIVE_ORDER);
                return columnNames;

            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select table (or file) with data)"};
            }
            catch( Exception e )
            {
                return new String[]{"(table (or file) doesn't contain the columns)"};
            }
        }
    }

    public static class ColumnNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((RegressionAnalysisAdvancedParameters)getBean()).getPathToDataMatrix();
                String[] columnNames = TableAndFileUtils.getColumnNames(pathToDataMatrix);
                Arrays.sort(columnNames, String.CASE_INSENSITIVE_ORDER);
                return columnNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select table (or file) with data)"};
            }
            catch( Exception e )
            {
                return new String[]{"(table (or file) doesn't contain the columns)"};
            }
        }
    }

    public static class RegressionAnalysisAdvancedParametersBeanInfo extends BeanInfoEx2<RegressionAnalysisAdvancedParameters>
    {
        public RegressionAnalysisAdvancedParametersBeanInfo()
        {
            super(RegressionAnalysisAdvancedParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("regressionMode", beanClass), RegressionModesSelector.class);
            addHidden(new PropertyDescriptorEx("regressionType", beanClass), RegressionTypesSelector.class, "isRegressionTypeHidden");
            add(DataElementPathEditor.registerInput("pathToDataMatrix", beanClass, DataElement.class, false));
            addHidden("variableNames", VariableNamesSelector.class, "areVariableNamesHidden");
            addHidden(new PropertyDescriptorEx("responseName", beanClass), ColumnNameSelector.class, "isResponseNameHidden");
            addHidden(DataElementPathEditor.registerInput("pathToFolderWithSavedModel", beanClass, FolderCollection.class), "isPathToFolderWithSavedRegressionModelHidden");
            addHidden("parametersForOlsRegression", "areParametersForOlsRegressionHidden");
            addHidden("parametersForWlsRegression", "areParametersForWlsRegressionHidden");
            addHidden("parametersForPcRegression", "areParametersForPcRegressionHidden");
            addHidden("parametersForRtRegression", "areParametersForRtRegressionHidden");
            addHidden("parametersForRidgeRegression", "areParametersForRidgeRegressionHidden");
            addHidden("parametersForCombinedRegression", "areParametersForCombinedRegressionHidden");
            addHidden("parametersForCrossValidation", "areParametersForCrossValidationHidden");
            addHidden("parametersForVariableSelection", "areParametersForVariableSelectionHidden");
            addHidden("parametersForOutlierDetection", "areParametersForOutlierDetectionHidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}
