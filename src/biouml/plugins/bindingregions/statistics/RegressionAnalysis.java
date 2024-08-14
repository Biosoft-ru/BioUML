package biouml.plugins.bindingregions.statistics;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.ColumnNameSelectorExtended;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.DataTransformationTypeSelector;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.PrincipalComponentSortingTypeSelector;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.RegressionOrClassificationModesEditor;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.RegressionTypesEditor;
// import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.VariableNamesSelector;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.VariableNamesSelectorExtended;
import biouml.plugins.bindingregions.statisticsutils.RegressionEngine;
import biouml.plugins.bindingregions.utils.LinearRegression;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
// import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;


/**
 * @author yura
 * There are 3 options:
 * 1. Create regression model for given data matrix and response vector
 * 2. Read regression model and predict response for given data matrix
 * 3. Implement cross-validation of regression model
 */

// TODO: To test carefully 'Clusterized LS-regression'

public class RegressionAnalysis extends AnalysisMethodSupport<RegressionAnalysis.RegressionAnalysisParameters>
{
    public RegressionAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new RegressionAnalysisParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("**************************************");
        log.info("Regression analysis. There are 3 modes :");
        log.info("1. Create and write regression model");
        log.info("2. Read regression model and predict response");
        log.info("3. Cross-validate regression model");
        log.info("**************************************");

        String regressionMode = parameters.getRegressionMode();
        String regressionType = parameters.getRegressionType();
        // DataElementPath pathToTableWithDataMatrix = parameters.getPathToTableWithDataMatrix();
        DataElementPath pathToDataMatrix = parameters.getPathToDataMatrix();
        String[] variableNames = parameters.getVariableNames();
        String responseName = parameters.getResponseName();
        boolean doAddResponseToClusterization = parameters.getDoAddResponseToClusterization();
        String dataTransformationType = parameters.getDataTransformationType();
        int numberOfClusters = Math.max(2, parameters.getNumberOfClusters());
        int numberOfPrincipalComponents = Math.max(1, parameters.getNumberOfPrincipalComponents());
        String principalComponentSortingType = parameters.getPrincipalComponentSortingType();
        DataElementPath pathToFolderWithSavedModel = parameters.getPathToFolderWithSavedModel();
        int percentageOfDataForTraining = parameters.getPercentageOfDataForTraining();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        
        // 1. Input parameter correction on hiddenness
        if( parameters.isRegressionTypeHidden() ) regressionType = null;
        if( parameters.areVariableNamesForRegressionHidden() ) variableNames = null;
        if( parameters.isDataTransformationTypeHidden() ) dataTransformationType = null;
        if( parameters.isNumberOfClustersHidden() ) numberOfClusters = 0;
        if( parameters.isNumberOfPrincipalComponentsHidden() ) numberOfPrincipalComponents = 0;
        if( parameters.isPrincipalComponentSortingTypeHidden() ) principalComponentSortingType = null;
        if( parameters.isResponseNameHidden() ) responseName = null;
        if( parameters.isDoAddResponseToClusterizationHidden() ) doAddResponseToClusterization = false;
        if( parameters.isPathToFolderWithSavedRegressionModelHidden() ) pathToFolderWithSavedModel = null;
        if( parameters.isPercentageOfDataForTrainingHidden() ) percentageOfDataForTraining = 0;
        
        // 2. Implementation of regression analysis
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        // implementRegressionAnalysis(regressionMode, regressionType, numberOfClusters, dataTransformationType, doAddResponseToClusterization, pathToTableWithDataMatrix, variableNames, responseName, pathToFolderWithSavedModel, percentageOfDataForTraining, numberOfPrincipalComponents, principalComponentSortingType, pathToOutputs, jobControl, 0, 100);
        implementRegressionAnalysis(regressionMode, regressionType, numberOfClusters, dataTransformationType, doAddResponseToClusterization, pathToDataMatrix, variableNames, responseName, pathToFolderWithSavedModel, percentageOfDataForTraining, numberOfPrincipalComponents, principalComponentSortingType, pathToOutputs, jobControl, 0, 100);
        return pathToOutputs.getDataCollection();
    }
    
    private void implementRegressionAnalysis(String regressionMode, String regressionType, int numberOfClusters, String dataTransformationType, boolean doAddResponseToClusterization, DataElementPath pathToTableWithDataMatrix, String[] variableNames, String responseName, DataElementPath pathToFolderWithSavedModel, int percentageOfDataForTraining, int numberOfPrincipalComponents, String principalComponentSortingType, DataElementPath pathToOutputs, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int maxNumberOfIterations = MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE, maxNumberOfClusterizationSteps = 3000;
        double eps = MatrixUtils.DEFAULT_EPS_FOR_INVERSE;
        Object[] objects;
        RegressionEngine re;
        switch( regressionMode )
        {
            case RegressionEngine.CREATE_AND_WRITE_MODE : log.info("Read data matrix and response vector in table or file");
                                                          objects = RegressionEngine.readDataSubMatrixAndResponse(pathToTableWithDataMatrix, variableNames, responseName);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 4);
                                                          log.info("Create and write regression model");
                                                          if( RegressionEngine.doAddInterceptToRegression(regressionType) )
                                                              variableNames = LinearRegression.addInterceptToRegression(variableNames, (double[][])objects[1]);
                                                          re = new RegressionEngine(regressionType, (String[])objects[0], variableNames, (double[][])objects[1], responseName, (double[])objects[2]);
                                                          re.createAndWriteRegressionModel(pathToOutputs, log, maxNumberOfIterations, eps, maxNumberOfClusterizationSteps, true, numberOfClusters, dataTransformationType, numberOfPrincipalComponents, principalComponentSortingType, doAddResponseToClusterization, jobControl, from + (to - from) / 4, to);
                                                          break;
            case RegressionEngine.READ_AND_PREDICT_MODE : log.info("Read data matrix and regression model and predict the response");
                                                          re = new RegressionEngine(pathToFolderWithSavedModel, pathToTableWithDataMatrix);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 4);
                                                          re.readRegressionModelAndPredictResponse(pathToFolderWithSavedModel, pathToOutputs, log, true, jobControl, from + (to - from) / 4, to);
                                                          break;
            case RegressionEngine.CROSS_VALIDATION_MODE : log.info("Read data matrix and response vector in table or file");
                                                          objects = RegressionEngine.readDataSubMatrixAndResponse(pathToTableWithDataMatrix, variableNames, responseName);
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 4);
                                                          if( RegressionEngine.doAddInterceptToRegression(regressionType) )
                                                              variableNames = LinearRegression.addInterceptToRegression(variableNames, (double[][])objects[1]);
                                                          re = new RegressionEngine(regressionType, (String[])objects[0], variableNames, (double[][])objects[1], responseName, (double[])objects[2]);
                                                          log.info("Cross-validation: create regression model on trainig data set and assess it on test data set");
                                                          re.implementCrossValidation(percentageOfDataForTraining, numberOfClusters, doAddResponseToClusterization, dataTransformationType, pathToOutputs, "crossValidation", log, maxNumberOfIterations, eps, maxNumberOfClusterizationSteps, numberOfPrincipalComponents, principalComponentSortingType, jobControl, from + (to - from) / 4, to);
                                                          break;
            default                                     : throw new Exception("This mode '" + regressionMode + "' is not supported in our regression analysis currently");
        }
    }

    public static class RegressionAnalysisParameters extends AbstractStatisticalAnalysisParameters
    {}
    
    public static class RegressionAnalysisParametersBeanInfo extends BeanInfoEx2<RegressionAnalysisParameters>
    {
        public RegressionAnalysisParametersBeanInfo()
        {
            super(RegressionAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("regressionMode", beanClass), RegressionOrClassificationModesEditor.class);
            addHidden(new PropertyDescriptorEx("regressionType", beanClass), RegressionTypesEditor.class, "isRegressionTypeHidden");
            
            // add(DataElementPathEditor.registerInput("pathToTableWithDataMatrix", beanClass, TableDataCollection.class, false));
            add(DataElementPathEditor.registerInput("pathToDataMatrix", beanClass, DataElement.class, false));
            
            // addHidden("variableNames", VariableNamesSelector.class, "areVariableNamesForRegressionHidden");
            addHidden("variableNames", VariableNamesSelectorExtended.class, "areVariableNamesForRegressionHidden");
            
            addHidden(new PropertyDescriptorEx("dataTransformationType", beanClass), DataTransformationTypeSelector.class, "isDataTransformationTypeHidden");
            addHidden("numberOfClusters", "isNumberOfClustersHidden");
            addHidden("numberOfPrincipalComponents", "isNumberOfPrincipalComponentsHidden");
            addHidden(new PropertyDescriptorEx("principalComponentSortingType", beanClass), PrincipalComponentSortingTypeSelector.class, "isPrincipalComponentSortingTypeHidden");

            // addHidden(ColumnNameSelector.registerNumericSelector("responseName", beanClass, "pathToTableWithDataMatrix", false), "isResponseNameHidden");
            addHidden(new PropertyDescriptorEx("responseName", beanClass), ColumnNameSelectorExtended.class, "isResponseNameHidden");
            
            addHidden("doAddResponseToClusterization", "isDoAddResponseToClusterizationHidden");
            addHidden(DataElementPathEditor.registerInput("pathToFolderWithSavedModel", beanClass, FolderCollection.class), "isPathToFolderWithSavedRegressionModelHidden");
            addHidden("percentageOfDataForTraining", "isPercentageOfDataForTrainingHidden");
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class, false));
        }
    }
}
