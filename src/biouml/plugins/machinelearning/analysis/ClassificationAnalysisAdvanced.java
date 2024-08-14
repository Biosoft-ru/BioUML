/* $Id$ */

package biouml.plugins.machinelearning.analysis;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.machinelearning.classification_models.ClassificationModel;
import biouml.plugins.machinelearning.classification_models.LinearClassificationModel;
import biouml.plugins.machinelearning.classification_models.PerceptronModel;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

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

/**
 * @author yura
 *    In order to add new classification model, it is necessary to do:
 * 1. Create model as extension of ClassificationModel.
 * 2. To add 'public static final String' to ClassificationModel.
 * 3. To change methods ClassificationModel.loadModel() and ClassificationModel.createModel().
 * 4. To change method ClassificationModel.getAvailableClassificationTypes().
 * 5. To change method LinearClassificationModel.doAddInterceptToClassification().
 * 6. To add the treatment of parameters of new model in method createModel() in this Class.
 */

public class ClassificationAnalysisAdvanced extends AnalysisMethodSupport<ClassificationAnalysisAdvanced.ClassificationAnalysisAdvancedParameters>
{
    public ClassificationAnalysisAdvanced(DataCollection<?> origin, String name)
    {
        super(origin, name, new ClassificationAnalysisAdvancedParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut()
    {
        log.info(" *****************************************************");
        log.info(" * Classification analysis. There are 4 modes :      *");
        log.info(" * 1. Create and save classification model           *");
        log.info(" * 2. Load classification model and predict response *");
        log.info(" * 3. Cross-validate classification model            *");
        log.info(" * 4. Variable (feature) selection                   *");
        log.info(" *****************************************************");

        String classificationMode = parameters.getClassificationMode();
        DataElementPath pathToDataMatrix = parameters.getPathToDataMatrix();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        
        // 1. Treatment of LOAD_AND_PREDICT_MODE.
        if( classificationMode.equals(ModelUtils.LOAD_AND_PREDICT_MODE) )
        {
            DataElementPath pathToFolderWithSavedModel = parameters.getPathToFolderWithSavedModel();
            ClassificationModel classificationModel = ClassificationModel.loadModel(pathToFolderWithSavedModel);
            String[] variableNames = classificationModel.getVariableNames();
            int interceptIndex =  ArrayUtils.indexOf(variableNames, ModelUtils.INTERCEPT);
            String[] names = interceptIndex < 0 ? variableNames : (String[])ArrayUtils.remove(variableNames, interceptIndex);
            DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, names);
            if( interceptIndex >= 0 )
                dataMatrix.addColumn(ModelUtils.INTERCEPT, UtilsForArray.getConstantArray(dataMatrix.getSize(), 1.0), interceptIndex);
            dataMatrix.removeRowsWithMissingData();
            classificationModel.predictAndSave(dataMatrix, pathToOutputFolder, "Predicted_values", log);
            return pathToOutputFolder.getDataCollection();
        }
        
        // 2. Read data matrix and response.
        String[] variableNames = parameters.getVariableNames();
        String classificationType = parameters.getClassificationType();
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, variableNames);
        if( LinearClassificationModel.doAddInterceptToClassification(classificationType) )
        {
            int interceptIndex = dataMatrix.getColumnNames().length;
            dataMatrix.addColumn(ModelUtils.INTERCEPT, UtilsForArray.getConstantArray(dataMatrix.getSize(), 1.0), interceptIndex);
        }
        String responseName = parameters.getResponseName();
        DataMatrixString dms = new DataMatrixString(pathToDataMatrix, new String[]{responseName});
        String[] response = dms.getColumn(responseName);
        response = dataMatrix.removeRowsWithMissingData(response);
        
        // 3. Treatment of remained modes.
        switch( classificationMode )
        {
            case ModelUtils.CREATE_AND_SAVE_MODE    : ClassificationModel classificationModel = createModel(classificationType, responseName, response, dataMatrix, true, pathToOutputFolder);
                                                      classificationModel.saveModel(pathToOutputFolder); break;
            case ModelUtils.CROSS_VALIDATION_MODE   : int percentageOfDataForTraining = parameters.getParametersForCrossValidation().getPercentageOfDataForTraining();
                                                      Object[] objects = ModelUtils.splitDataSet(dataMatrix, response, response.length * percentageOfDataForTraining / 100, 0);
                                                      classificationModel = createModel(classificationType, responseName, (String[])objects[4], (DataMatrix)objects[0], false, pathToOutputFolder);
                                                      int[] predictedIndices = classificationModel.predict((DataMatrix)objects[0]);
                                                      Object[] objs = UtilsForArray.getDistinctStringsAndIndices((String[])objects[4]);
                                                      DataMatrix trueClassificationRates = ModelUtils.getTrueClassificationRates((int[])objs[1], predictedIndices, (String[])objs[0]);
                                                      trueClassificationRates.replaceColumnName("True classification rate", "True classification rate (Training set)");
                                                      trueClassificationRates.replaceColumnName("Size of class", "Size of class (Training set)");
                                                      predictedIndices = classificationModel.predict((DataMatrix)objects[1]);
                                                      int[] testResponseIndices = UtilsForArray.getIndicesOfStrings((String[])objects[5], (String[])objs[0]);
                                                      DataMatrix trueClassificationRatesForTest = ModelUtils.getTrueClassificationRates(testResponseIndices, predictedIndices, (String[])objs[0]);
                                                      trueClassificationRatesForTest.replaceColumnName("True classification rate", "True classification rate (Test set)");
                                                      trueClassificationRatesForTest.replaceColumnName("Size of class", "Size of class (Test set)");
                                                      trueClassificationRates.addAnotherDataMatrixColumnWise(trueClassificationRatesForTest);
                                                      trueClassificationRates.writeDataMatrix(false, pathToOutputFolder, "True_classification_rates_cross_validation", log); break;
            case ModelUtils.VARIABLE_SELECTION_MODE : int numberOfSelectedVariables = Math.max(1, parameters.getParametersForVariableSelection().getNumberOfSelectedVariables());
                                                      String variableSelectionType = parameters.getParametersForVariableSelection().getVariableSelectionType();
                                                      switch( variableSelectionType )
                                                      {
                                                          case ModelUtils.STEPWISE_FORWARD_VARIABLE_ADDITION : Object[] additionalInputParameters = createAdditionalInputParameters(classificationType, responseName, response, dataMatrix, false, pathToOutputFolder);
                                                                                                               DataMatrix dm = ModelUtils.stepwiseForwardVariableSelectionInClassification(classificationType, responseName, response, dataMatrix, numberOfSelectedVariables, additionalInputParameters, false, jobControl, 0, 100);
                                                                                                               dm.writeDataMatrix(false, pathToOutputFolder, "selected_variables", log); break;
                                                          case ModelUtils.STEPWISE_BACKWARD_ELIMINATION      : log.info(ModelUtils.STEPWISE_BACKWARD_ELIMINATION + " is under construction");
                                                      } break;
        }
        return pathToOutputFolder.getDataCollection();
    }
    
    private Object[] createAdditionalInputParameters(String classificationType, String responseName, String[] response, DataMatrix dataMatrix, boolean doCalculateAccompaniedInformationWhenFit, DataElementPath pathToOutputFolder)
    {
        switch( classificationType )
        {
            case ClassificationModel.CLASSIFICATION_1_LDA   : int maxNumberOfRotations = parameters.getParametersForLdaClassification().getMaxNumberOfRotations();
                                                              double epsForRotations = parameters.getParametersForLdaClassification().getEpsForRotations();
                                                              int maxNumberOfIterationsInLyusternikMethod = parameters.getParametersForLdaClassification().getMaxNumberOfIterationsInLyusternikMethod();
                                                              double epsForLyusternikMethod = parameters.getParametersForLdaClassification().getEpsForLyusternikMethod();
                                                              return new Object[]{maxNumberOfRotations, epsForRotations, maxNumberOfIterationsInLyusternikMethod, epsForLyusternikMethod};
            case ClassificationModel.CLASSIFICATION_2_MLM   : maxNumberOfRotations = parameters.getParametersForMlmClassification().getMaxNumberOfRotations();
                                                              epsForRotations = parameters.getParametersForMlmClassification().getEpsForRotations();
                                                              return new Object[]{maxNumberOfRotations, epsForRotations};
            case ClassificationModel.CLASSIFICATION_3_SVM_R : return new Object[]{pathToOutputFolder, null, null};
            case ClassificationModel.CLASSIFICATION_4_PER   : String optimizationType = parameters.getParametersForPerClassification().getOptimizationType();
                                                              int maxNumberOfIterations = parameters.getParametersForPerClassification().getMaxNumberOfIterations();
                                                              double admissibleMisclassificationRate = parameters.getParametersForPerClassification().getAdmissibleMisclassificationRate();
                                                              return new Object[]{optimizationType, maxNumberOfIterations, admissibleMisclassificationRate};
            case ClassificationModel.CLASSIFICATION_5_LRM   : maxNumberOfIterations = parameters.getParametersForLrmClassification().getMaxNumberOfIterations();
                                                              admissibleMisclassificationRate = parameters.getParametersForLrmClassification().getAdmissibleMisclassificationRate();
                                                              maxNumberOfRotations = parameters.getParametersForLrmClassification().getMaxNumberOfIterations();
                                                              epsForRotations = parameters.getParametersForLrmClassification().getEpsForRotations();
                                                              return new Object[]{maxNumberOfIterations, admissibleMisclassificationRate, maxNumberOfRotations, epsForRotations};
            default                                         : return null;
        }
    }
    
    private ClassificationModel createModel(String classificationType, String responseName, String[] response, DataMatrix dataMatrix, boolean doCalculateAccompaniedInformationWhenFit, DataElementPath pathToOutputFolder)
    {
        Object[] additionalInputParameters = createAdditionalInputParameters(classificationType, responseName, response, dataMatrix, doCalculateAccompaniedInformationWhenFit, pathToOutputFolder);
        return ClassificationModel.createModel(classificationType, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_CLASSIFICATION_MODE = "Classification mode";
        public static final String PD_CLASSIFICATION_MODE = "Select classification mode";
        
        public static final String PN_CLASSIFICATION_TYPE = "Classification type";
        public static final String PD_CLASSIFICATION_TYPE = "Select classification type";
        
        public static final String PN_PATH_TO_DATA_MATRIX = "Path to data matrix";
        public static final String PD_PATH_TO_DATA_MATRIX = "Path to table or file with data matrix";
        
        public static final String PN_VARIABLE_NAMES = "Variable names";
        public static final String PD_VARIABLE_NAMES = "Select variable names";
        
        public static final String PN_RESPONSE_NAME = "Response name";
        public static final String PD_RESPONSE_NAME = "Select response name";
        
        public static final String PN_PATH_TO_FOLDER_WITH_SAVED_MODEL = "Path to folder with saved model";
        public static final String PD_PATH_TO_FOLDER_WITH_SAVED_MODEL = "Path to folder with saved model";
        
        public static final String PN_OPTIMIZATION_TYPE = "Optimization type";
        public static final String PD_OPTIMIZATION_TYPE = "Select optimization type";

        public static final String PN_ADMISSIBLE_MISCLASSIFICATION_RATE = "Admissible misclassification rate";
        public static final String PD_ADMISSIBLE_MISCLASSIFICATION_RATE = "Admissible misclassification rate";
        
        public static final String PN_MAX_NUMBER_OF_ROTATIONS = "Max number of rotations";
        public static final String PD_MAX_NUMBER_OF_ROTATIONS = "Maximal number of rotations for calculation of inverse matrix or eigen vectors";
        
        public static final String PN_EPS_FOR_ROTATIONS = "Epsilon for rotations";
        public static final String PD_EPS_FOR_ROTATIONS = "Epsilon for calculation of inverse matrix or eigen vectors";
        
        public static final String PN_MAX_NUMBER_OF_ITERATIONS = "Max number of iterations";
        public static final String PD_MAX_NUMBER_OF_ITERATIONS = "Max number of iterations";
        public static final String PD_MAX_NUMBER_OF_ITERATIONS_IN_LYUSTERNIK_METHOD = "Max number of iterations in Lyusternikm method for calculation of maximal eigen value and corresponding eigen vector";
        
        public static final String PN_EPS_IN_LYUSTERNIK_METHOD = "Epsilon for iterations in Lyusternik method";
        public static final String PD_EPS_IN_LYUSTERNIK_METHOD = "Epsilon for iterations in Lyusternik method";
        
        public static final String PN_NUMBER_OF_SELECTED_VARIABLES = "Number of selected variables";
        public static final String PD_NUMBER_OF_SELECTED_VARIABLES = "Number of selected variables";
        
        public static final String PN_VARIABLE_SELECTION_TYPE = "Variable selection type";
        public static final String PD_VARIABLE_SELECTION_TYPE = " Please, determine variable selection type";

        public static final String PN_PARAMETERS_FOR_LDA_CLASSIFICATION = "Parameters for LDA-classification";
        public static final String PD_PARAMETERS_FOR_LDA_CLASSIFICATION = "Please, determine parameters for LDA-classification (Fisher's linear discriminant analysis)";
        
        public static final String PN_PARAMETERS_FOR_MLM_CLASSIFICATION = "Parameters for maximum likelihood classification";
        public static final String PD_PARAMETERS_FOR_MLM_CLASSIFICATION = "Please, determine parameters for maximum likelihood classification based on multinormal distribution";
        
        public static final String PN_PARAMETERS_FOR_PER_CLASSIFICATION = "Parameters for perceptron classification";
        public static final String PD_PARAMETERS_FOR_PER_CLASSIFICATION = "Please, determine parameters for perceptron classification";
        
        public static final String PN_PARAMETERS_FOR_LRM_CLASSIFICATION = "Parameters for logistic regression";
        public static final String PD_PARAMETERS_FOR_LRM_CLASSIFICATION = "Please, determine parameters for logistic regression";
        
        public static final String PN_PARAMETERS_FOR_CROSS_VALIDATION = "Parameters for cross-validation";
        public static final String PD_PARAMETERS_FOR_CROSS_VALIDATION = "Please, determine parameters for cross-validation";
        
        public static final String PN_PARAMETERS_FOR_VARIABLE_SELECTION = "Parameters for variable selection";
        public static final String PD_PARAMETERS_FOR_VARIABLE_SELECTION = "Parameters for variable selection";
        
        public static final String PN_PERCENTAGE_OF_DATA_FOR_TRAINING = "Percentage of data for training";
        public static final String PD_PERCENTAGE_OF_DATA_FOR_TRAINING = "Proportion (in %) of data for training";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }
    
    public static class AllParameters extends AbstractAnalysisParameters
    {
        private String classificationMode = ModelUtils.CREATE_AND_SAVE_MODE;
        private String classificationType = ClassificationModel.CLASSIFICATION_1_LDA;
        private DataElementPath pathToDataMatrix;
        private String[] variableNames;
        private String responseName;
        private DataElementPath pathToFolderWithSavedModel;
        private String optimizationType;
        private double admissibleMisclassificationRate = 0.01;
        private int percentageOfDataForTraining = 50;
        private int maxNumberOfRotations = MatrixUtils.DEFAULT_MAX_NUMBER_OF_ROTATIONS;
        private double epsForRotations = MatrixUtils.DEFAULT_EPS_FOR_ROTATIONS;
        private int maxNumberOfIterations = 300;
        private int maxNumberOfIterationsInLyusternikMethod = MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_IN_LYUSTERNIK_METHOD;
        private double epsForLyusternikMethod = MatrixUtils.DEFAULT_EPS_IN_LYUSTERNIK_METHOD;
        private int numberOfSelectedVariables = 15;
        private String variableSelectionType = ModelUtils.STEPWISE_FORWARD_VARIABLE_ADDITION;
        private DataElementPath pathToOutputFolder;
        
        @PropertyName(MessageBundle.PN_CLASSIFICATION_MODE)
        @PropertyDescription(MessageBundle.PD_CLASSIFICATION_MODE)
        public String getClassificationMode()
        {
            return classificationMode;
        }
        public void setClassificationMode(String classificationMode)
        {
            Object oldValue = this.classificationMode;
            this.classificationMode = classificationMode;
            firePropertyChange("*", oldValue, classificationMode);
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
            firePropertyChange("*", oldValue, classificationType);
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

        @PropertyName(MessageBundle.PN_OPTIMIZATION_TYPE)
        @PropertyDescription(MessageBundle.PD_OPTIMIZATION_TYPE)
        public String getOptimizationType()
        {
            return optimizationType;
        }
        public void setOptimizationType(String optimizationType)
        {
            Object oldValue = this.optimizationType;
            this.optimizationType = optimizationType;
            firePropertyChange("optimizationType", oldValue, optimizationType);
        }
        
        @PropertyName(MessageBundle.PN_ADMISSIBLE_MISCLASSIFICATION_RATE)
        @PropertyDescription(MessageBundle.PD_ADMISSIBLE_MISCLASSIFICATION_RATE)
        public double getAdmissibleMisclassificationRate()
        {
            return admissibleMisclassificationRate;
        }
        public void setAdmissibleMisclassificationRate(double admissibleMisclassificationRate)
        {
            Object oldValue = this.admissibleMisclassificationRate;
            this.admissibleMisclassificationRate = admissibleMisclassificationRate;
            firePropertyChange("admissibleMisclassificationRate", oldValue, admissibleMisclassificationRate);
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

        @PropertyName(MessageBundle.PN_MAX_NUMBER_OF_ITERATIONS)
        @PropertyDescription(MessageBundle.PD_MAX_NUMBER_OF_ITERATIONS)
        public int getMaxNumberOfIterations()
        {
            return maxNumberOfIterations;
        }
        public void setMaxNumberOfIterations(int maxNumberOfIterations)
        {
            Object oldValue = this.maxNumberOfIterations;
            this.maxNumberOfIterations = maxNumberOfIterations;
            firePropertyChange("maxNumberOfIterations", oldValue, maxNumberOfIterations);
        }
        
        @PropertyName(MessageBundle.PN_MAX_NUMBER_OF_ITERATIONS)
        @PropertyDescription(MessageBundle.PD_MAX_NUMBER_OF_ITERATIONS_IN_LYUSTERNIK_METHOD)
        public int getMaxNumberOfIterationsInLyusternikMethod()
        {
            return maxNumberOfIterationsInLyusternikMethod;
        }
        public void setMaxNumberOfIterationsInLyusternikMethod(int maxNumberOfIterationsInLyusternikMethod)
        {
            Object oldValue = this.maxNumberOfIterationsInLyusternikMethod;
            this.maxNumberOfIterationsInLyusternikMethod = maxNumberOfIterationsInLyusternikMethod;
            firePropertyChange("maxNumberOfIterationsInLyusternikMethod", oldValue, maxNumberOfIterationsInLyusternikMethod);
        }

        @PropertyName(MessageBundle.PN_EPS_IN_LYUSTERNIK_METHOD)
        @PropertyDescription(MessageBundle.PD_EPS_IN_LYUSTERNIK_METHOD)
        public double getEpsForLyusternikMethod()
        {
            return epsForLyusternikMethod;
        }
        public void setEpsForLyusternikMethod(double epsForLyusternikMethod)
        {
            Object oldValue = this.epsForLyusternikMethod;
            this.epsForLyusternikMethod = epsForLyusternikMethod;
            firePropertyChange("epsForLyusternikMethod", oldValue, epsForLyusternikMethod);
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
    
    public static class ParametersForLdaClassification extends AllParameters
    {}
    
    public static class ParametersForLdaClassificationBeanInfo extends BeanInfoEx2<ParametersForLdaClassification>
    {
        public ParametersForLdaClassificationBeanInfo()
        {
            super(ParametersForLdaClassification.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("maxNumberOfRotations");
            add("epsForRotations");
            add("maxNumberOfIterationsInLyusternikMethod");
            add("epsForLyusternikMethod");
        }
    }
    
    public static class ParametersForMlmClassification extends AllParameters
    {}
    
    public static class ParametersForMlmClassificationBeanInfo extends BeanInfoEx2<ParametersForMlmClassification>
    {
        public ParametersForMlmClassificationBeanInfo()
        {
            super(ParametersForMlmClassification.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("maxNumberOfRotations");
            add("epsForRotations");
        }
    }
    
    public static class ParametersForPerClassification extends AllParameters
    {}
    
    public static class ParametersForPerClassificationBeanInfo extends BeanInfoEx2<ParametersForPerClassification>
    {
        public ParametersForPerClassificationBeanInfo()
        {
            super(ParametersForPerClassification.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("optimizationType", beanClass), OptimizationTypesEditor.class);
            add("maxNumberOfIterations");
            add("admissibleMisclassificationRate");
        }
    }
    
    public static class ParametersForLrmClassification extends AllParameters
    {}
    
    public static class ParametersForLrmClassificationBeanInfo extends BeanInfoEx2<ParametersForLrmClassification>
    {
        public ParametersForLrmClassificationBeanInfo()
        {
            super(ParametersForLrmClassification.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("maxNumberOfIterations");
            add("admissibleMisclassificationRate");
            add("maxNumberOfRotations");
            add("epsForRotations");
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
            add("numberOfSelectedVariables");
            add(new PropertyDescriptorEx("variableSelectionType", beanClass), VariableSelectionTypeSelector.class);
        }
    }
    
    public static class ClassificationAnalysisAdvancedParameters extends AllParameters
    {
        ParametersForLdaClassification parametersForLdaClassification;
        ParametersForMlmClassification parametersForMlmClassification;
        ParametersForPerClassification parametersForPerClassification;
        ParametersForLrmClassification parametersForLrmClassification;
        ParametersForCrossValidation parametersForCrossValidation;
        ParametersForVariableSelection parametersForVariableSelection;
        
        public ClassificationAnalysisAdvancedParameters()
        {
            setParametersForLdaClassification(new ParametersForLdaClassification());
            setParametersForMlmClassification(new ParametersForMlmClassification());
            setParametersForPerClassification(new ParametersForPerClassification());
            setParametersForLrmClassification(new ParametersForLrmClassification());
            setParametersForCrossValidation(new ParametersForCrossValidation());
            setParametersForVariableSelection(new ParametersForVariableSelection());
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_LDA_CLASSIFICATION)
        @PropertyDescription(MessageBundle.PN_PARAMETERS_FOR_LDA_CLASSIFICATION)
        public ParametersForLdaClassification getParametersForLdaClassification()
        {
            return parametersForLdaClassification;
        }
        public void setParametersForLdaClassification(ParametersForLdaClassification parametersForLdaClassification)
        {
            Object oldValue = this.parametersForLdaClassification;
            this.parametersForLdaClassification = parametersForLdaClassification;
            firePropertyChange("parametersForLdaClassification", oldValue, parametersForLdaClassification);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_MLM_CLASSIFICATION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_MLM_CLASSIFICATION)
        public ParametersForMlmClassification getParametersForMlmClassification()
        {
            return parametersForMlmClassification;
        }
        public void setParametersForMlmClassification(ParametersForMlmClassification parametersForMlmClassification)
        {
            Object oldValue = this.parametersForMlmClassification;
            this.parametersForMlmClassification = parametersForMlmClassification;
            firePropertyChange("parametersForMlmClassification", oldValue, parametersForMlmClassification);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_PER_CLASSIFICATION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_PER_CLASSIFICATION)
        public ParametersForPerClassification getParametersForPerClassification()
        {
            return parametersForPerClassification;
        }
        public void setParametersForPerClassification(ParametersForPerClassification parametersForPerClassification)
        {
            Object oldValue = this.parametersForPerClassification;
            this.parametersForPerClassification = parametersForPerClassification;
            firePropertyChange("parametersForPerClassification", oldValue, parametersForPerClassification);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_LRM_CLASSIFICATION)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_LRM_CLASSIFICATION)
        public ParametersForLrmClassification getParametersForLrmClassification()
        {
            return parametersForLrmClassification;
        }
        public void setParametersForLrmClassification(ParametersForLrmClassification parametersForLrmClassification)
        {
            Object oldValue = this.parametersForLrmClassification;
            this.parametersForLrmClassification = parametersForLrmClassification;
            firePropertyChange("parametersForLrmClassification", oldValue, parametersForLrmClassification);
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
        
        public boolean isClassificationTypeHidden()
        {
            return(getClassificationMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE));
        }
        
        public boolean areVariableNamesHidden()
        {
            return(getClassificationMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE));
        }
        
        public boolean isPathToFolderWithSavedModelHidden()
        {
            return(! getClassificationMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE));
        }
        
        public boolean isResponseNameHidden()
        {
            return(getClassificationMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE));
        }
        
        public boolean areParametersForLdaClassificationHidden()
        {
            return (getClassificationMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getClassificationType().equals(ClassificationModel.CLASSIFICATION_1_LDA));
        }
        
        public boolean areParametersForMlmClassificationHidden()
        {
            return (getClassificationMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getClassificationType().equals(ClassificationModel.CLASSIFICATION_2_MLM));
        }
        
        public boolean areParametersForPerClassificationHidden()
        {
            return (getClassificationMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getClassificationType().equals(ClassificationModel.CLASSIFICATION_4_PER));
        }
        
        public boolean areParametersForLrmClassificationHidden()
        {
            return (getClassificationMode().equals(ModelUtils.LOAD_AND_PREDICT_MODE) || ! getClassificationType().equals(ClassificationModel.CLASSIFICATION_5_LRM));
        }
        
        public boolean areParametersForCrossValidationHidden()
        {
            return ( ! getClassificationMode().equals(ModelUtils.CROSS_VALIDATION_MODE));
        }
        
        public boolean areParametersForVariableSelectionHidden()
        {
            return ( ! getClassificationMode().equals(ModelUtils.VARIABLE_SELECTION_MODE));
        }
    }
    
    public static class ClassificationModesEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return ModelUtils.getAvailableModes();
        }
    }
    
    public static class ClassificationTypesEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return ClassificationModel.getAvailableClassificationTypes();
        }
    }
    
    public static class OptimizationTypesEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return PerceptronModel.getAvailableOptimizationTypes();
        }
    }
    
    public static class VariableNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((ClassificationAnalysisAdvancedParameters)getBean()).getPathToDataMatrix();
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
                DataElementPath pathToDataMatrix = ((ClassificationAnalysisAdvancedParameters)getBean()).getPathToDataMatrix();
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
    
    public static class VariableSelectionTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return ModelUtils.getAvailableMethodsForvariableSelection();
        }
    }
    
    public static class ClassificationAnalysisAdvancedParametersBeanInfo extends BeanInfoEx2<ClassificationAnalysisAdvancedParameters>
    {
        public ClassificationAnalysisAdvancedParametersBeanInfo()
        {
            super(ClassificationAnalysisAdvancedParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("classificationMode", beanClass), ClassificationModesEditor.class);
            addHidden(new PropertyDescriptorEx("classificationType", beanClass), ClassificationTypesEditor.class, "isClassificationTypeHidden");
            add(DataElementPathEditor.registerInput("pathToDataMatrix", beanClass, DataElement.class, false));
            addHidden("variableNames", VariableNamesSelector.class, "areVariableNamesHidden");
            addHidden(new PropertyDescriptorEx("responseName", beanClass), ColumnNameSelector.class, "isResponseNameHidden");
            addHidden(DataElementPathEditor.registerInput("pathToFolderWithSavedModel", beanClass, FolderCollection.class), "isPathToFolderWithSavedModelHidden");
            addHidden("parametersForLdaClassification", "areParametersForLdaClassificationHidden");
            addHidden("parametersForMlmClassification", "areParametersForMlmClassificationHidden");
            addHidden("parametersForPerClassification", "areParametersForPerClassificationHidden");
            addHidden("parametersForLrmClassification", "areParametersForLrmClassificationHidden");
            addHidden("parametersForCrossValidation", "areParametersForCrossValidationHidden");
            addHidden("parametersForVariableSelection", "areParametersForVariableSelectionHidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}
