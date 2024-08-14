
package biouml.plugins.bindingregions.statistics;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.statisticsutils.ClassificationEngine;
import biouml.plugins.bindingregions.statisticsutils.ClusterizationEngine;
import biouml.plugins.bindingregions.statisticsutils.RegressionEngine;
import biouml.plugins.bindingregions.utils.DataMatrix;
import biouml.plugins.bindingregions.utils.LinearRegression;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.LinearRegression.PrincipalComponentRegression;
import biouml.plugins.bindingregions.utils.Clusterization.IndicesOfClusteringQuality;
import biouml.plugins.bindingregions.utils.MatrixUtils.Distance;
import biouml.plugins.bindingregions.utils.MultivariateSample.Transformation;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/***
 * @author yura
 ***/

public class AbstractStatisticalAnalysisParameters extends AbstractAnalysisParameters
{
    private double pValue = 0.05;
    private String regressionMode = RegressionEngine.CREATE_AND_WRITE_MODE;
    private String regressionType = RegressionEngine.REGRESSION_1_LS;
    private String classificationMode = RegressionEngine.CREATE_AND_WRITE_MODE;
    private String classificationType = ClassificationEngine.CLASSIFICATION_2_FISHER_LDA;
    private String clusterizationType = ClusterizationEngine.CLUSTERIZATION_1_K_MEANS;
    private String distanceType = Distance.EUCLIDEAN;
    private DataElementPath pathToTableWithDataMatrix;
    private DataElementPath pathToDataMatrix;
    private String[] variableNames;
    private String[] dataMatrixExtensions;
    private String dataTransformationType = Transformation.TRANSFORMATION_RANKS;
    private int numberOfClusters = 2;
    private int numberOfPrincipalComponents = 1;
    private String principalComponentSortingType;
    private String responseName;
    private String classifierName;
    private String[] responseNames;
    private boolean doAddResponseToClusterization;
    private DataElementPath pathToFolderWithSavedModel;
    private int percentageOfDataForTraining = 50;
    private String[] outputOptions;
    private String[] clusteringQualityIndexNames;
    private boolean areCovarianceMatricesEqual;
    private SmoothingParameters smoothingParameters;
    private int maxIterations = 15; 
    private DataElementPath outputPath;
    
    @PropertyName(MessageBundle.PN_P_VALUE_THRESHOLD)
    @PropertyDescription(MessageBundle.PD_P_VALUE_THRESHOLD)
    public double getPValue()
    {
        return pValue;
    }
    public void setPValue(double pValue)
    {
        Object oldValue = this.pValue;
        this.pValue = pValue;
        firePropertyChange("pValue", oldValue, pValue);
    }

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

    @PropertyName(MessageBundle.PN_ARE_COVARIANCE_MATRICES_EQUAL)
    @PropertyDescription(MessageBundle.PD_ARE_COVARIANCE_MATRICES_EQUAL)
    public boolean getAreCovarianceMatricesEqual()
    {
        return areCovarianceMatricesEqual;
    }
    public void setAreCovarianceMatricesEqual(boolean areCovarianceMatricesEqual)
    {
        Object oldValue = this.areCovarianceMatricesEqual;
        this.areCovarianceMatricesEqual = areCovarianceMatricesEqual;
        firePropertyChange("areCovarianceMatricesEqual", oldValue, areCovarianceMatricesEqual);
    }
    
    @PropertyName(MessageBundle.PN_CLUSTERIZATION_TYPE)
    @PropertyDescription(MessageBundle.PD_CLUSTERIZATION_TYPE)
    public String getClusterizationType()
    {
        return clusterizationType;
    }
    public void setClusterizationType(String clusterizationType)
    {
        Object oldValue = this.clusterizationType;
        this.clusterizationType = clusterizationType;
        firePropertyChange("*", oldValue, clusterizationType);
    }
    
    @PropertyName(MessageBundle.PN_DISTANCE_TYPE)
    @PropertyDescription(MessageBundle.PD_DISTANCE_TYPE)
    public String getDistanceType()
    {
        return distanceType;
    }
    public void setDistanceType(String distanceType)
    {
        Object oldValue = this.distanceType;
        this.distanceType = distanceType;
        firePropertyChange("*", oldValue, distanceType);
    }

    @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_DATA_MATRIX)
    @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_DATA_MATRIX)
    public DataElementPath getPathToTableWithDataMatrix()
    {
        return pathToTableWithDataMatrix;
    }
    public void setPathToTableWithDataMatrix(DataElementPath pathToTableWithDataMatrix)
    {
        Object oldValue = this.pathToTableWithDataMatrix;
        this.pathToTableWithDataMatrix = pathToTableWithDataMatrix;
        firePropertyChange("pathToTableWithDataMatrix", oldValue, pathToTableWithDataMatrix);
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
    
    @PropertyName(MessageBundle.PN_DATA_MATRIX_EXTENSIONS)
    @PropertyDescription(MessageBundle.PD_DATA_MATRIX_EXTENSIONS)
    public String[] getDataMatrixExtensions()
    {
        return dataMatrixExtensions;
    }
    public void setDataMatrixExtensions(String[] dataMatrixExtensions)
    {
        Object oldValue = this.dataMatrixExtensions;
        this.dataMatrixExtensions = dataMatrixExtensions;
        firePropertyChange("dataMatrixExtensions", oldValue, dataMatrixExtensions);
    }
    
    @PropertyName(MessageBundle.PN_DO_ADD_RESPONSE_TO_CLUSTERIZATION)
    @PropertyDescription(MessageBundle.PD_DO_ADD_RESPONSE_TO_CLUSTERIZATION)
    public boolean getDoAddResponseToClusterization()
    {
        return doAddResponseToClusterization;
    }
    public void setDoAddResponseToClusterization(boolean doAddResponseToClusterization)
    {
        Object oldValue = this.doAddResponseToClusterization;
        this.doAddResponseToClusterization = doAddResponseToClusterization;
        firePropertyChange("doAddResponseToClusterization", oldValue, doAddResponseToClusterization);
    }
    
    @PropertyName(MessageBundle.PN_TRANSFORMATION_TYPE)
    @PropertyDescription(MessageBundle.PD_TRANSFORMATION_TYPE)
    public String getDataTransformationType()
    {
        return dataTransformationType;
    }
    public void setDataTransformationType(String dataTransformationType)
    {
        Object oldValue = this.dataTransformationType;
        this.dataTransformationType = dataTransformationType;
        firePropertyChange("dataTransformationType", oldValue, dataTransformationType);
    }
    
    @PropertyName(MessageBundle.PN_NUMBER_OF_CLUSTERS)
    @PropertyDescription(MessageBundle.PD_NUMBER_OF_CLUSTERS)
    public int getNumberOfClusters()
    {
        return numberOfClusters;
    }
    public void setNumberOfClusters(int numberOfClusters)
    {
        Object oldValue = this.numberOfClusters;
        this.numberOfClusters = numberOfClusters;
        firePropertyChange("numberOfClusters", oldValue, numberOfClusters);
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
        firePropertyChange("*", oldValue, responseName);
    }

    @PropertyName(MessageBundle.PN_CLASSIFIER_NAME)
    @PropertyDescription(MessageBundle.PD_CLASSIFIER_NAME)
    public String getClassifierName()
    {
        return classifierName;
    }
    public void setClassifierName(String classifierName)
    {
        Object oldValue = this.classifierName;
        this.classifierName = classifierName;
        firePropertyChange("classifierName", oldValue, classifierName);
    }
    
    @PropertyName(MessageBundle.PN_RESPONSE_NAMES)
    @PropertyDescription(MessageBundle.PD_RESPONSE_NAMES)
    public String[] getResponseNames()
    {
        return responseNames;
    }
    public void setResponseNames(String[] responseNames)
    {
        Object oldValue = this.responseNames;
        this.responseNames = responseNames;
        firePropertyChange("*", oldValue, responseNames);
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
        if( percentageOfDataForTraining <= 0 || percentageOfDataForTraining >= 100 )
            percentageOfDataForTraining = 50;
        return percentageOfDataForTraining;
    }
    public void setPercentageOfDataForTraining(int percentageOfDataForTraining)
    {
        Object oldValue = this.percentageOfDataForTraining;
        this.percentageOfDataForTraining = percentageOfDataForTraining;
        firePropertyChange("percentageOfDataForTraining", oldValue, percentageOfDataForTraining);
    }

    @PropertyName(MessageBundle.PN_OUTPUT_OPTIONS)
    @PropertyDescription(MessageBundle.PD_OUTPUT_OPTIONS)
    public String[] getOutputOptions()
    {
        return outputOptions;
    }
    public void setOutputOptions(String[] outputOptions)
    {
        Object oldValue = this.outputOptions;
        this.outputOptions = outputOptions;
        firePropertyChange("*", oldValue, outputOptions);
    }
    
    @PropertyName(MessageBundle.PN_CLUSTERING_QUALITY_INDICES_NAMES)
    @PropertyDescription(MessageBundle.PD_CLUSTERING_QUALITY_INDICES_NAMES)
    public String[] getClusteringQualityIndexNames()
    {
        return clusteringQualityIndexNames;
    }
    public void setClusteringQualityIndexNames(String[] clusteringQualityIndexNames)
    {
        Object oldValue = this.clusteringQualityIndexNames;
        this.clusteringQualityIndexNames = clusteringQualityIndexNames;
        firePropertyChange("clusteringQualityIndexNames", oldValue, clusteringQualityIndexNames);
    }
    
    @PropertyName("Smoothing parameters")
    public SmoothingParameters getSmoothingParameters()
    {
        return smoothingParameters;
    }
    public void setSmoothingParameters(SmoothingParameters smoothingParameters)
    {
        Object oldValue = this.smoothingParameters;
        this.smoothingParameters = withPropagation(this.smoothingParameters, smoothingParameters);
        firePropertyChange("smoothingParameters", oldValue, smoothingParameters);
    }
    
    @PropertyName(MessageBundle.PN_MAX_ITERATIONS)
    @PropertyDescription(MessageBundle.PD_MAX_ITERATIONS)
    public int getMaxIterations()
    {
        return maxIterations;
    }
    public void setMaxIterations(int maxIterations)
    {
        Object oldValue = this.maxIterations;
        this.maxIterations = maxIterations;
        firePropertyChange("maxIterations", oldValue, maxIterations);
    }

    @PropertyName(MessageBundle.PN_OUTPUT_PATH)
    @PropertyDescription(MessageBundle.PD_OUTPUT_PATH)
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, outputPath);
    }

    public static class RegressionTypesEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return RegressionEngine.getAvailableRegressionTypes();
        }
    }
    
    public static class ClassificationTypesEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return ClassificationEngine.getAvailableClassificationTypes();
        }
    }
    
    public static class ClusterizationTypesEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return ClusterizationEngine.getAvailableClusterizationTypes();
        }
    }
    
    public static class RegressionOrClassificationModesEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return RegressionEngine.getAvailableModes();
        }
    }
    
    public static class DataTransformationTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return Transformation.getTransformationTypes();
        }
    }
    
    public static class PrincipalComponentSortingTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return PrincipalComponentRegression.getAvailableTypesOfPCsorting();
        }
    }
    
    public static class DistanceTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return Distance.getAvailableDistanceTypes();
        }
    }

    public static class SmoothingWindowWidthTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return DensityEstimation.getSmoothingWindowWidthTypesAvailable();
        }
    }
    
    public static class ClusteringQualityIndexNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                return IndicesOfClusteringQuality.getQualityIndexNamesAvailableInClusterCrit();
            }
            catch( Exception e )
            {
                return new String[] {"(please select clustering quality indices)"};
            }
        }
    }

    // This selector is used for TableDataCollection only
    public static class VariableNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                //TableDataCollection table = ((AbstractStatisticalAnalysisParameters)getBean()).getPathToTableWithDataMatrix().getDataElement(TableDataCollection.class);
                //return TableUtils.getColumnNamesInTable(table);
                DataElementPath pathToDataMatrix = ((AbstractStatisticalAnalysisParameters)getBean()).getPathToTableWithDataMatrix();
                return DataMatrix.getColumnNames(pathToDataMatrix);
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select table with data)"};
            }
            catch( Exception e )
            {
                return new String[]{"(table doesn't contain the columns)"};
            }
        }
    }
    
    // This selector is used for TableDataCollection or for File
    public static class VariableNamesSelectorExtended extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((AbstractStatisticalAnalysisParameters)getBean()).getPathToDataMatrix();
                // return DataMatrix.getColumnNames(pathToDataMatrix);
                String[] columnNames = DataMatrix.getColumnNames(pathToDataMatrix);
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

    // This selector is used for TableDataCollection only
    public static class ColumnNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            try
            {
                TableDataCollection table = ((AbstractStatisticalAnalysisParameters)getBean()).getPathToTableWithDataMatrix().getDataElement(TableDataCollection.class);
//              return table.columns().map(TableColumn::getName).sorted().toArray( String[]::new );
                return table.columns().map(TableColumn::getName).toArray(String[]::new);
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select table with data)"};
            }
            catch( Exception e )
            {
                return new String[]{"(table doesn't contain the columns)"};
            }
        }
    }
    
    // This selector is used for TableDataCollection or for File
    public static class ColumnNameSelectorExtended extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((AbstractStatisticalAnalysisParameters)getBean()).getPathToDataMatrix();
                return DataMatrix.getColumnNames(pathToDataMatrix);
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

    // TODO: to replace it by class 'ColumnNameSelector'; 
    // Example of usage of ColumnNameSelector : add(new PropertyDescriptorEx("classifierName", beanClass), ColumnNameSelector.class);
    public static class ClassifierNameEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            try
            {
                TableDataCollection table = ((AbstractStatisticalAnalysisParameters)getBean()).getPathToTableWithDataMatrix().getDataElement(TableDataCollection.class);
                return table.columns().map(TableColumn::getName).toArray(String[]::new);
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select table with data)"};
            }
            catch( Exception e )
            {
                return new String[]{"(table doesn't contain the columns)"};
            }
        }
    }
    
    public static class DataMatrixExtensionsSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            return new String[]{LinearRegression.ADD_INTERCEPT, LinearRegression.ADD_ALL_INTERACTIONS, LinearRegression.ADD_SQUARED_VARIABLES};
        }
    }
    
    public boolean isRegressionTypeHidden()
    {
        return(getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }
    
    public boolean isClassificationTypeHidden()
    {
        return(getClassificationMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }
    
    public boolean isPathToFolderWithSavedRegressionModelHidden()
    {
        return( ! getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }
    
    public boolean isPathToFolderWithSavedClassificationModelHidden()
    {
        return( ! getClassificationMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }
    
    public boolean isPercentageOfDataForTrainingHidden()
    {
        return( ! getRegressionMode().equals(RegressionEngine.CROSS_VALIDATION_MODE));
    }
    
    public boolean isPercentageOfDataForTrainingInClassificationHidden()
    {
        return( ! getClassificationMode().equals(RegressionEngine.CROSS_VALIDATION_MODE));
    }
    
    public boolean areVariableNamesForRegressionHidden()
    {
        return(getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }
    
    public boolean areVariableNamesForClassificationHidden()
    {
        return(getClassificationMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }

    public boolean isResponseNameHidden()
    {
        return(getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }
    
    public boolean isClassifierNameHidden()
    {
        return(getClassificationMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }

    public boolean areResponseNamesHidden()
    {
        return(getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE));
    }

    public boolean isNumberOfClustersHidden()
    {
        String regressionType = getRegressionType();
        return(getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE) || ! (regressionType.equals(RegressionEngine.REGRESSION_4_LS_IN_CLUSTERS) || regressionType.equals(RegressionEngine.REGRESSION_5_LS_CLUSTERIZED)));
    }
    
    public boolean isNumberOfPrincipalComponentsHidden()
    {
        String regressionType = getRegressionType();
        return(getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE) || ! regressionType.equals(RegressionEngine.REGRESSION_6_PC));
    }
    
    public boolean isPrincipalComponentSortingTypeHidden()
    {
        String regressionType = getRegressionType();
        return(getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE) || ! regressionType.equals(RegressionEngine.REGRESSION_6_PC));
    }
    
    public boolean isDataTransformationTypeHidden()
    {
        String regressionType = getRegressionType();
        return(getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE) || ! (regressionType.equals(RegressionEngine.REGRESSION_4_LS_IN_CLUSTERS) || regressionType.equals(RegressionEngine.REGRESSION_5_LS_CLUSTERIZED)));
    }
    
    public boolean isDoAddResponseToClusterizationHidden()
    {
        String regressionType = getRegressionType();
        return (getRegressionMode().equals(RegressionEngine.READ_AND_PREDICT_MODE) || ! (regressionType.equals(RegressionEngine.REGRESSION_4_LS_IN_CLUSTERS) || regressionType.equals(RegressionEngine.REGRESSION_5_LS_CLUSTERIZED)));
    }
    
    public boolean isDistanceTypeHidden()
    {
        String clusterizationType = getClusterizationType();
        String[] outputOptions = getOutputOptions();
        return ! clusterizationType.equals(ClusterizationEngine.CLUSTERIZATION_1_K_MEANS) && ! clusterizationType.equals(ClusterizationEngine.CLUSTERIZATION_2_FUNNY) && ! ArrayUtils.contains(outputOptions, ClusterizationEngine.OUTPUT_7_WRITE_DISTANCES_BETWEEN_CENTERS) && ! ArrayUtils.contains(outputOptions, ClusterizationEngine.OUTPUT_8_WRITE_DISTANCES_BETWEEN_CENTERS_TRANSFORMED);
    }
    
    public boolean isAreCovarianceMatricesEqualHidden()
    {
        if( isClassificationTypeHidden() ) return true;
        return ! getClassificationType().equals(ClassificationEngine.CLASSIFICATION_5_EDDA);
    }
    /*************** SmoothingParameters : start ********************/
    public static class SmoothingParameters extends AbstractAnalysisParameters
    {
        private String smoothingWindowWidthType;
        private double givenSmoothingWindowWidth;
        
        @PropertyName(MessageBundle.PN_SMOOTHING_WINDOW_WIDTH_TYPE)
        @PropertyDescription(MessageBundle.PD_SMOOTHING_WINDOW_WIDTH_TYPE)
        public String getSmoothingWindowWidthType()
        {
            return smoothingWindowWidthType;
        }
        public void setSmoothingWindowWidthType(String smoothingWindowWidthType)
        {
            Object oldValue = this.smoothingWindowWidthType;
            this.smoothingWindowWidthType = smoothingWindowWidthType;
            firePropertyChange("*", oldValue, smoothingWindowWidthType);
        }

        @PropertyName(MessageBundle.PN_GIVEN_SMOOTHING_WINDOW_WIDTH)
        @PropertyDescription(MessageBundle.PD_GIVEN_SMOOTHING_WINDOW_WIDTH)
        public double getGivenSmoothingWindowWidth()
        {
            return givenSmoothingWindowWidth;
        }
        public void setGivenSmoothingWindowWidth(double givenSmoothingWindowWidth)
        {
            Object oldValue = this.givenSmoothingWindowWidth;
            this.givenSmoothingWindowWidth = givenSmoothingWindowWidth;
            firePropertyChange("givenSmoothingWindowWidth", oldValue, givenSmoothingWindowWidth);
        }
        
        public boolean isGivenSmoothingWindowWidthHidden()
        {
            String smoothingWindowWidthType = getSmoothingWindowWidthType();
            return smoothingWindowWidthType == null || ! smoothingWindowWidthType.equals(DensityEstimation.WINDOW_WIDTH_04);
        }
    }
    
    public static class SmoothingParametersBeanInfo extends BeanInfoEx2<SmoothingParameters>
    {
        public SmoothingParametersBeanInfo()
        {
            super(SmoothingParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("smoothingWindowWidthType", beanClass), SmoothingWindowWidthTypeSelector.class);
            addHidden("givenSmoothingWindowWidth", "isGivenSmoothingWindowWidthHidden");
        }
    }
    /****************** SmoothingParameters : finish *********************/
}
