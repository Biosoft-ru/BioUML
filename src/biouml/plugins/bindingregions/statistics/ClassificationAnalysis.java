
package biouml.plugins.bindingregions.statistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.ClassificationTypesEditor;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.ColumnNameSelectorExtended;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.RegressionOrClassificationModesEditor;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.VariableNamesSelectorExtended;
import biouml.plugins.bindingregions.statisticsutils.ClassificationEngine;
import biouml.plugins.bindingregions.utils.Classification;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.MultivariateSamples;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TableUtils.FileUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author yura
 * There are 3 options:
 * 1. Create classification model for given data matrix and names of classes (or indices of classes)
 * 2. Read classification model and classify given data matrix (i. e. predict indices of classes)
 * 3. Implement cross-validation of classification model
 */

public class ClassificationAnalysis extends AnalysisMethodSupport<ClassificationAnalysis.ClassificationAnalysisParameters>
{
    public ClassificationAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new ClassificationAnalysisParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Classification analysis. There are 3 modes :");
        log.info("1. Create classification model for given data matrix and names of classes (or indices of classes)");
        log.info("2. Read classification model and classify given data matrix (i. e. predict indices of classes)");
        log.info("3. Implement cross-validation of classification model");
        
        String classificationMode = parameters.getClassificationMode();
        String classificationType = parameters.getClassificationType();
        boolean areCovarianceMatricesEqual = parameters.getAreCovarianceMatricesEqual();
        // DataElementPath pathToTableWithDataMatrix = parameters.getPathToTableWithDataMatrix();
        DataElementPath pathToDataMatrix = parameters.getPathToDataMatrix();
        String[] variableNames = parameters.getVariableNames();
        String classifierName = parameters.getClassifierName();
        DataElementPath pathToFolderWithSavedModel = parameters.getPathToFolderWithSavedModel();
        int percentageOfDataForTraining = parameters.getPercentageOfDataForTraining();
        DataElementPath pathToOutputs = parameters.getOutputPath();

        // 1. Input parameter correction on hiddenness
        if( parameters.isClassificationTypeHidden() ) classificationType = null;
        if( parameters.isAreCovarianceMatricesEqualHidden() ) areCovarianceMatricesEqual = false;
        if( parameters.areVariableNamesForClassificationHidden() ) variableNames = null;
        if( parameters.isClassifierNameHidden() ) classifierName = null;
        if( parameters.isPathToFolderWithSavedClassificationModelHidden() ) pathToFolderWithSavedModel = null;
        if( parameters.isPercentageOfDataForTrainingInClassificationHidden() ) percentageOfDataForTraining = -1;

        // 2. Implementation of classification analysis
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        
        
        
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! temp
        // testForCovMatrix(pathToDataMatrix, variableNames, classifierName, pathToOutputs);
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! temp
        
        
        
        ClassificationEngine ce = new ClassificationEngine(classificationMode, classificationType, areCovarianceMatricesEqual, pathToDataMatrix, variableNames, classifierName, pathToFolderWithSavedModel, percentageOfDataForTraining, pathToOutputs);
        ce.implementClassificationAnalysis(log, jobControl, 0, 100);
        return pathToOutputs.getDataCollection();
    }
    
    //!!!!!!!!!!!!!!!!!!!! temporary !!!!!!!!!!!!! it is need for test only !!!!!!!!
    private void testForCovMatrix(DataElementPath pathToDataMatrix, String[] variableNames, String classifierName, DataElementPath pathToOutputFolder) throws IOException
    {
        // 1. Read data
        Object[] objects = null;
        if( ! (pathToDataMatrix.getDataElement() instanceof TableDataCollection) )
        {
            objects = FileUtils.readMatrixOrSubmatix(pathToDataMatrix, new String[]{classifierName}, FileUtils.STRING_TYPE);
            String[][] stringMatrix = (String[][])objects[2];
            String[] namesOfClassesForEachObject = new String[stringMatrix.length];
            for( int i = 0; i < stringMatrix.length; i++ )
                namesOfClassesForEachObject[i] = stringMatrix[i][0];
            objects = FileUtils.readMatrixOrSubmatix(pathToDataMatrix, variableNames, FileUtils.DOUBLE_TYPE);
            objects = removeObjectsWithMissingData((String[])objects[0], (double[][])objects[2], namesOfClassesForEachObject);
        }
        else
        {
            objects = TableUtils.readDataSubMatrixAndStringColumn(pathToDataMatrix, variableNames, classifierName);
            objects = removeObjectsWithMissingData((String[])objects[0], (double[][])objects[1], (String[])objects[2]);
        }
        
        // 2. Calculate withinSSPmatrix - matrix and their eigenvalues.
        String[] objectNames = (String[])objects[0], namesOfClassesForEachObject = (String[])objects[2];
        double[][] dataMatrix = (double[][])objects[1];
        log.info("dim(dataMatrix) = " + dataMatrix.length + " " + dataMatrix[0].length);
        objects = Classification.getIndicesOfClassesAndNamesOfClasses(namesOfClassesForEachObject);
        int[] indicesOfClasses = (int[])objects[0];
        int maxNumberOfIterations = MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE;
        double eps = MatrixUtils.DEFAULT_EPS_FOR_INVERSE;
        //objects = FisherLDA.createClassificationModel(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
        // double[] linearDiscriminantFunction = FisherLDA.getLinearDiscriminantFunction(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
        objects = MultivariateSamples.getWithinAndBetweenAndTotalSSPmatrices(dataMatrix, indicesOfClasses);
        double[][] withinSSPmatrix = (double[][])objects[0];
        log.info("dim(withinSSPmatrix) = " + withinSSPmatrix.length + " " + withinSSPmatrix[0].length);
        //objects = getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(withinSSPmatrix, maxNumberOfIterations, eps);
        objects = MatrixUtils.getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(withinSSPmatrix, maxNumberOfIterations, eps);
        double[] eigenValues = (double[])objects[1];
        log.info("numberOfIterations = " + (int)objects[0]);
        TableUtils.writeDoubleTable(eigenValues, variableNames, "eigen_values", pathToOutputFolder, "eigen_values_for_withinSSPmatrix");
        log.info("!!!!!!!!!!!! eigenValues are calculated !!!!!!!!!!!!");
    }
    
    //!!!!!!!!!!!!!!!!!!!! temporary !!!!!!!!!!!!! it is need for test only !!!!!!!!
    /////////////// It is from ClassificationEngene
    /***
     * remove all objects that contain missing data (NaN-values);
     * @param objectNames
     * @param dataMatrix
     * @param response
     * @return Object[] array; array[0] = String[] newObjectNames; array[1] = double[][] newDataMatrix; array[2] = String[] newNamesOfClassesForEachObject;
     */
    private static Object[] removeObjectsWithMissingData(String[] objectNames, double[][] dataMatrix, String[] namesOfClassesForEachObject)
    {
        List<String> newObjectNames = new ArrayList<>(), newNamesOfClassesForEachObject = new ArrayList<>();
        List<double[]> newDataMatrix = new ArrayList<>();
        for( int i = 0; i < objectNames.length; i++ )
            if( ! MatrixUtils.doContainNaN(dataMatrix[i]) )
            {
                newObjectNames.add(objectNames[i]);
                newDataMatrix.add(dataMatrix[i]);
                newNamesOfClassesForEachObject.add(namesOfClassesForEachObject[i]);
            }
        if( newObjectNames.isEmpty() ) return null;
        return new Object[]{newObjectNames.toArray(new String[0]), newDataMatrix.toArray(new double[newDataMatrix.size()][]), newNamesOfClassesForEachObject.toArray(new String[0])};
    }
    
    public static class ClassificationAnalysisParameters extends AbstractStatisticalAnalysisParameters
    {}
    
    public static class ClassificationAnalysisParametersBeanInfo extends BeanInfoEx2<ClassificationAnalysisParameters>
    {
        public ClassificationAnalysisParametersBeanInfo()
        {
            super(ClassificationAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("classificationMode", beanClass), RegressionOrClassificationModesEditor.class);
            addHidden(new PropertyDescriptorEx("classificationType", beanClass), ClassificationTypesEditor.class, "isClassificationTypeHidden");
            addHidden("areCovarianceMatricesEqual", "isAreCovarianceMatricesEqualHidden");

//          add(DataElementPathEditor.registerInput("pathToTableWithDataMatrix", beanClass, TableDataCollection.class));
            add(DataElementPathEditor.registerInput("pathToDataMatrix", beanClass, DataElement.class, false));
            
            //addHidden("variableNames", VariableNamesSelector.class, "areVariableNamesForClassificationHidden");
            addHidden("variableNames", VariableNamesSelectorExtended.class, "areVariableNamesForClassificationHidden");
            
            
//          addHidden(new PropertyDescriptorEx("classifierName", beanClass), ClassifierNameEditor.class, "isClassifierNameHidden");
            // addHidden(new PropertyDescriptorEx("classifierName", beanClass), ColumnNameSelector.class, "isClassifierNameHidden");
            addHidden(new PropertyDescriptorEx("classifierName", beanClass), ColumnNameSelectorExtended.class, "isClassifierNameHidden");
            
            addHidden(DataElementPathEditor.registerInput("pathToFolderWithSavedModel", beanClass, FolderCollection.class), "isPathToFolderWithSavedClassificationModelHidden");
            addHidden("percentageOfDataForTraining", "isPercentageOfDataForTrainingInClassificationHidden");
            
//          add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class));
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class, false));
        }
    }
}
