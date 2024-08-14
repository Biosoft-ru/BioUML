
package biouml.plugins.bindingregions.statisticsutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import one.util.streamex.IntStreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.bindingregions.rscript.RHelper;
import biouml.plugins.bindingregions.rscript.Rutils;
import biouml.plugins.bindingregions.utils.Classification;
import biouml.plugins.bindingregions.utils.Classification.ClassificationByMultivariateRegressionOfIndicatorMatrix;
import biouml.plugins.bindingregions.utils.Classification.EDDAclassification;
import biouml.plugins.bindingregions.utils.Classification.FisherLDA;
import biouml.plugins.bindingregions.utils.Classification.LogisticRegression;
import biouml.plugins.bindingregions.utils.Classification.MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance;
import biouml.plugins.bindingregions.utils.Classification.Perceptron;
import biouml.plugins.bindingregions.utils.Classification.SVMcClassification;
import biouml.plugins.bindingregions.utils.DataMatrix;
import biouml.plugins.bindingregions.utils.LinearRegression;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.MultivariateSample;
import biouml.plugins.bindingregions.utils.StatUtil;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TableUtils.FileUtils;
import biouml.plugins.bindingregions.utils.TableUtils.ParticularTable;

/**
 * @author yura
 * Important note : it is assumed that the input indices of classes and data matrix are located in same table (TableDataCollection) !!!
 * Method CLASSIFICATION_5_EDDA is under construction !!!!
 *
 */
public class ClassificationEngine
{
    public static final String CLASSIFICATION_1_MRIM = "Classification by multivariate regression of indicator matrix";
    public static final String CLASSIFICATION_2_FISHER_LDA = "Fisher's LDA (linear discriminant analysis)";
    public static final String CLASSIFICATION_3_MUXIMUM_LIKELIHOOD_MAHALANOBIS = "Maximum likelihood discrimination based on Mahalanobis distance";
    public static final String CLASSIFICATION_4_SVM_C_CLASSIFICATION = "Support Vector Machine C-classification";
    public static final String CLASSIFICATION_5_EDDA = "EDDA : Eigenvalue Decomposition Discriminant Analysis based on Gaussian mixture modeling";
    public static final String CLASSIFICATION_6_LOGISTIC_REGRESSION = "Logistic regression";
    public static final String CLASSIFICATION_7_PERCEPTRON = "Perceptron";

    public static final String NAME_OF_TABLE_WITH_CLASSIFICATION_TYPE_AND_CLASSIFIER_NAME = "classificationTypeAndClassifierName";
    public static final String NAME_OF_TABLE_WITH_COEFFICIENT_MATRIX = "coefficientMatrixForClassPrediction";
    public static final String NAME_OF_TABLE_WITH_NAMES_OF_CLASSES = "namesOfClasses";
    
    public static final String NAMES_OF_CLASSES = "Names of classes";
    
    private static final String NAME_OF_FILE_WITH_SVM_C_CLASSIFICATION_MODEL = "SVMcClassification.model";
    private static final String NAME_OF_FILE_WITH_EDDA_CLASSIFICATION_MODEL = "EDDAclassification.model";
    
    private final String classificationMode;
    private String classificationType;
    private final boolean areCovarianceMatricesEqual;
    // private final DataElementPath pathToTableWithDataMatrix;
    private final DataElementPath pathToDataMatrix;
    private String[] variableNames;
    private final String classifierName;
    private final DataElementPath pathToFolderWithSavedModel;
    private final int percentageOfDataForTraining;
    private final DataElementPath pathToOutputs;
    
    // public ClassificationEngine(String classificationMode, String classificationType, boolean areCovarianceMatricesEqual, DataElementPath pathToTableWithDataMatrix, String[] variableNames, String classifierName, DataElementPath pathToFolderWithSavedModel, int percentageOfDataForTraining, DataElementPath pathToOutputs)
    public ClassificationEngine(String classificationMode, String classificationType, boolean areCovarianceMatricesEqual, DataElementPath pathToDataMatrix, String[] variableNames, String classifierName, DataElementPath pathToFolderWithSavedModel, int percentageOfDataForTraining, DataElementPath pathToOutputs)
    {
        this.classificationMode = classificationMode;
        this.classificationType = classificationType;
        this.areCovarianceMatricesEqual = areCovarianceMatricesEqual;
        // this.pathToTableWithDataMatrix = pathToTableWithDataMatrix;
        this.pathToDataMatrix = pathToDataMatrix;
        this.variableNames = variableNames;
        this.classifierName = classifierName;
        this.pathToFolderWithSavedModel = pathToFolderWithSavedModel;
        this.percentageOfDataForTraining = percentageOfDataForTraining;
        this.pathToOutputs = pathToOutputs;
    }
    
    public static String[] getAvailableClassificationTypes()
    {
        return new String[]{CLASSIFICATION_2_FISHER_LDA, CLASSIFICATION_3_MUXIMUM_LIKELIHOOD_MAHALANOBIS, CLASSIFICATION_4_SVM_C_CLASSIFICATION, CLASSIFICATION_5_EDDA, CLASSIFICATION_6_LOGISTIC_REGRESSION, CLASSIFICATION_7_PERCEPTRON, CLASSIFICATION_1_MRIM};
    }
    
    public void implementClassificationAnalysis(Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int[] indicesOfClasses = null;
        int maxNumberOfIterations = MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE;
        double eps = MatrixUtils.DEFAULT_EPS_FOR_INVERSE;
        Object[] objects;
        double[][] dataMatrix;
        String[] namesOfClasses = null;
        switch( classificationMode )
        {
            case RegressionEngine.CREATE_AND_WRITE_MODE : log.info("Read data matrix and names of classes in table");
                                                          objects = readDataSubMatrixAndNamesOfClassesForEachObject();
                                                          dataMatrix = (double[][])objects[1];
                                                          String[] objectNames = (String[])objects[0];
                                                          String[] namesOfClassesForEachObject = (String[])objects[2];
                                                          

                                                          
                                                          
                                                          ////////////////////////////// temp
//                                                          int n = 0;
//                                                          for( int i = 0; i < namesOfClassesForEachObject.length; i++ )
//                                                              if(namesOfClassesForEachObject[i].equals("No")) n++;
//                                                          log.info("N = " + namesOfClassesForEachObject.length + " n = " + n);
//
//                                                          double[][] dmatrix = new double[2 * n][];
//                                                          String[] names = new String[2 * n], oNames = new String[2 * n];
//                                                          int index = 0, numberOfYes = 0;
//                                                          for( int i = 0; i < namesOfClassesForEachObject.length; i++ )
//                                                          {
//                                                              if(index == 2*n) break;
//                                                              if( numberOfYes > n && namesOfClassesForEachObject[i].equals("Yes")) continue;
//                                                              if(namesOfClassesForEachObject[i].equals("Yes"))
//                                                                  numberOfYes++;
//                                                                  
//                                                              dmatrix[index] = dataMatrix[i];
//                                                              names[index] = namesOfClassesForEachObject[i];
//                                                              oNames[index] = objectNames[i];
//                                                              log.info("index = " + index + " names[index] = " + names[index] + " oNames[index] = " + oNames[index]);
//                                                              index++;
//                                                          }
//                                                          for( int i = 0; i < names.length; i++ )
//                                                              if( names[i] == null )
//                                                                  log.info(" i = " + i + " names = " + names[i]);
//                                                          namesOfClassesForEachObject = names;
//                                                          dataMatrix = dmatrix;
//                                                          objectNames = oNames;
//                                                          log.info("new dim  = " + dataMatrix.length);
//                                                          log.info("namesOfClassesForEachObject = " + namesOfClassesForEachObject[0] + " objectNames = " + objectNames[0]);
//                                                          log.info("namesOfClassesForEachObject = " + namesOfClassesForEachObject[1] + " objectNames = " + objectNames[1]);
//                                                          objects = new Object[]{objectNames, dataMatrix, namesOfClassesForEachObject};
//                                                          Object[] objects1 = Classification.getIndicesOfClassesAndNamesOfClasses((String[])objects[2]);
//                                                          namesOfClasses = (String[])objects1[1];
//                                                          for( int i = 0; i <namesOfClasses.length; i++ )
//                                                              log.info("i  = " + i + " namesOfClasses = " + namesOfClasses[i]);
                                                          //////////////////////////// temp

                                                          
                                                          
                                                          
                                                          objects = Classification.getIndicesOfClassesAndNamesOfClasses((String[])objects[2]);
                                                          indicesOfClasses = (int[])objects[0];
                                                          namesOfClasses = (String[])objects[1];
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 4);
                                                          log.info("Create and write classification model");
                                                          createAndWriteClassificationModel(objectNames, dataMatrix, indicesOfClasses, namesOfClasses, maxNumberOfIterations, eps, true, log, jobControl, from + (to - from) / 4, to); break;
            case RegressionEngine.READ_AND_PREDICT_MODE : log.info("Read data matrix and classification model and predict the classes");
                                                          String[] variableNamesInModel = ParticularTable.readVariableNames(pathToFolderWithSavedModel, ParticularTable.NAME_OF_TABLE_WITH_VARIABLE_NAMES);
                                                          // TODO: To test reading matrix!!!
                                                          // objects = TableUtils.readDataSubMatrix(pathToDataMatrix, variableNamesInModel);
                                                          objects = DataMatrix.readDoubleMatrixOrSubmatrix(pathToDataMatrix, variableNamesInModel);
                                                          // objects = MatrixUtils.removeRowsWithMissingData((String[])objects[0], (double[][])objects[1]);
                                                          objects = MatrixUtils.removeRowsWithMissingData((String[])objects[0], (double[][])objects[2]);
                                                          dataMatrix = (double[][])objects[1];
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 4);
                                                          readClassificationModelAndPredictClasses(dataMatrix, (String[])objects[0], variableNamesInModel, pathToFolderWithSavedModel, pathToOutputs, log, true, jobControl, from + (to - from) / 4, to); break;
            case RegressionEngine.CROSS_VALIDATION_MODE : log.info("Read data matrix and names of classes in table");
                                                          objects = readDataSubMatrixAndNamesOfClassesForEachObject();
                                                          dataMatrix = (double[][])objects[1];
                                                          objects = Classification.getIndicesOfClassesAndNamesOfClasses((String[])objects[2]);
                                                          indicesOfClasses = (int[])objects[0];
                                                          namesOfClasses = (String[])objects[1];
                                                          if( jobControl != null ) jobControl.setPreparedness(from + (to - from) / 4);
                                                          if( classificationType.equals(CLASSIFICATION_1_MRIM) || classificationType.equals(CLASSIFICATION_6_LOGISTIC_REGRESSION) || classificationType.equals(CLASSIFICATION_7_PERCEPTRON) )
                                                              variableNames = LinearRegression.addInterceptToRegression(variableNames, dataMatrix);
                                                          log.info("Cross-validation: create classification model on trainig data set and assess it on test data set");
                                                          objects = splitDataSet(dataMatrix, indicesOfClasses, percentageOfDataForTraining);
                                                          int[] indicesOfClassesPredictedInTrain = createAndWriteClassificationModel(null, (double[][])objects[0], (int[])objects[1], namesOfClasses, maxNumberOfIterations, eps, false, log, jobControl, from + (to - from) / 4, from + (to - from) / 2);
                                                          int[] indicesOfClassesPredictedInTest = readClassificationModelAndPredictClasses((double[][])objects[2], null, variableNames, pathToOutputs, pathToOutputs, log, false, jobControl, from + (to - from) / 2, from + 4 * (to - from) / 5);
                                                          writeTrueClassificationRatesIntoTable((int[])objects[1], indicesOfClassesPredictedInTrain, (int[])objects[3], indicesOfClassesPredictedInTest, namesOfClasses, null, pathToOutputs, "trueClassificationRates_crossValidation");
                                                          if( jobControl != null ) jobControl.setPreparedness(to); break;
            default                                     : throw new Exception("This mode '" + classificationMode + "' is not supported in our classification analysis currently");
        }
    }

    private static void writeTrueClassificationRatesIntoTable(int[] indicesOfClassesInTrain, int[] indicesOfClassesPredictedInTrain, int[] indicesOfClassesInTest, int[] indicesOfClassesPredictedInTest, String[] namesOfClasses, String classifierName, DataElementPath pathToOutputs, String tableName)
    {
        Object[] objects = Classification.getTrueClassificationRates(indicesOfClassesInTrain, indicesOfClassesPredictedInTrain);
        Map<Integer, Double> indicesOfClassesAndTrueClassificationRatesInTrain = (Map<Integer, Double>)objects[1];
        Object[] objs = Classification.getTrueClassificationRates(indicesOfClassesInTest, indicesOfClassesPredictedInTest);
        Map<Integer, Double> indicesOfClassesAndTrueClassificationRatesInTest = (Map<Integer, Double>)objs[1];
        double[][] rates = new double[indicesOfClassesAndTrueClassificationRatesInTrain.size() + 1][2];
        String[] rowNames = new String[indicesOfClassesAndTrueClassificationRatesInTrain.size() + 1];
        int index = 0;
        for( Entry<Integer, Double> entry : indicesOfClassesAndTrueClassificationRatesInTrain.entrySet() )
        {
            rowNames[index] = namesOfClasses != null ? namesOfClasses[index] : classifierName + "_" + Integer.toString(entry.getKey());
            rates[index][0] = entry.getValue();
            rates[index++][1] = indicesOfClassesAndTrueClassificationRatesInTest.get(entry.getKey());
        }
        rowNames[indicesOfClassesAndTrueClassificationRatesInTrain.size()] = classifierName == null ? "whole set" : "whole set of " + classifierName;
        rates[indicesOfClassesAndTrueClassificationRatesInTrain.size()][0] = (double)objects[0];
        rates[indicesOfClassesAndTrueClassificationRatesInTrain.size()][1] = (double)objs[0];

        TableUtils.writeDoubleTable(rates, rowNames, new String[]{"Training set", "Test set"}, pathToOutputs, tableName);
    }

    private int[] createAndWriteClassificationModel(String[] objectNames, double[][] dataMatrix, int[] indicesOfClasses, String[] namesOfClasses, int maxNumberOfIterations, double eps, boolean doPrintAccompaniedInformation, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        Object[] objects = null;
        int[] predictedIndicesOfClasses = null;
        String[] namesOfClassesForEachObject = null;
        double[] probabilitiesOfClasses = null;
        switch( classificationType )
        {
            case CLASSIFICATION_1_MRIM                           : variableNames = LinearRegression.addInterceptToRegression(variableNames, dataMatrix);
                                                                   objects  = ClassificationByMultivariateRegressionOfIndicatorMatrix.createClassificationModelAndPredictIndicesOfClasses(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
                                                                   double[][] coefficientMatrixForClassPrediction = ClassificationByMultivariateRegressionOfIndicatorMatrix.createClassificationModel(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
                                                                   ClassificationByMultivariateRegressionOfIndicatorMatrix.writeClassificationModel(coefficientMatrixForClassPrediction, variableNames, namesOfClasses, pathToOutputs, NAME_OF_TABLE_WITH_COEFFICIENT_MATRIX);
                                                                   predictedIndicesOfClasses = ClassificationByMultivariateRegressionOfIndicatorMatrix.predictIndicesOfClasses(coefficientMatrixForClassPrediction, dataMatrix); break;
            case CLASSIFICATION_2_FISHER_LDA                     : objects = FisherLDA.createClassificationModel(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
                                                                   FisherLDA.writeClassificationModel((double[])objects[0], (double[][])objects[1], namesOfClasses, variableNames, pathToOutputs, FisherLDA.NAME_OF_TABLE_WITH_FUNCTION_COEFFICIENTS, FisherLDA.NAME_OF_TABLE_WITH_MEAN_VECTORS_FOR_CLASSES);
                                                                   predictedIndicesOfClasses = FisherLDA.predictIndicesOfClasses((double[])objects[0], (double[][])objects[1], dataMatrix); break;
            case CLASSIFICATION_3_MUXIMUM_LIKELIHOOD_MAHALANOBIS : objects = MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance.createClassificationModel(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
                                                                   MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance.writeClassificationModel((double[][])objects[0], (double[][])objects[1], namesOfClasses, variableNames, pathToOutputs, MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance.NAME_OF_TABLE_WITH_INVERSE_COVARIANCE_MATRIX, FisherLDA.NAME_OF_TABLE_WITH_MEAN_VECTORS_FOR_CLASSES);
                                                                   predictedIndicesOfClasses = MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance.predictIndicesOfClasses((double[][])objects[0], (double[][])objects[1], dataMatrix); break;
            case CLASSIFICATION_4_SVM_C_CLASSIFICATION           : namesOfClassesForEachObject = Classification.getNamesOfClassesForEachObject(indicesOfClasses, namesOfClasses);
                                                                   String scriptToCreateAndWriteModelForSVMcClassification = RHelper.getScript("ClassificationAnalysis", "CreateModelForSVMc-classification");
                                                                   predictedIndicesOfClasses = SVMcClassification.createAndWriteClassificationModelUsingR(dataMatrix, namesOfClassesForEachObject, namesOfClasses, scriptToCreateAndWriteModelForSVMcClassification, pathToOutputs, NAME_OF_FILE_WITH_SVM_C_CLASSIFICATION_MODEL, log, jobControl, from, to); break;
            case CLASSIFICATION_5_EDDA                           : namesOfClassesForEachObject = Classification.getNamesOfClassesForEachObject(indicesOfClasses, namesOfClasses);
                                                                   String scriptToCreateAndWriteModelForEDDAclassification = RHelper.getScript("ClassificationAnalysis", "CreateModelForEDDAclassification");
                                                                   predictedIndicesOfClasses = EDDAclassification.createAndWriteClassificationModelUsingR(dataMatrix, namesOfClassesForEachObject, areCovarianceMatricesEqual, namesOfClasses, scriptToCreateAndWriteModelForEDDAclassification, pathToOutputs, NAME_OF_FILE_WITH_EDDA_CLASSIFICATION_MODEL, log, jobControl, from, to); break;
            case CLASSIFICATION_6_LOGISTIC_REGRESSION            : /***test();***/
                                                                   if( namesOfClasses.length != 2 ) throw new Exception("Input data is not for binary classification");
                                                                   variableNames = LinearRegression.addInterceptToRegression(variableNames, dataMatrix);
                                                                   int numberOfIterativeSteps = 100;
                                                                   // double[] coefficientsInitialApproxomation = MultivariateSample.getMeanVector(dataMatrix);
                                                                   double[] coefficientsInitialApproxomation = null;
                                                                   LogisticRegression logisticRegression = new LogisticRegression(dataMatrix, indicesOfClasses, coefficientsInitialApproxomation, numberOfIterativeSteps, maxNumberOfIterations, eps, log);
                                                                   logisticRegression.writeClassificationModel(variableNames, pathToOutputs);
                                                                   objects = logisticRegression.predictIndicesOfClassesWithProbabilities(dataMatrix);
                                                                   predictedIndicesOfClasses = (int[])objects[0];
                                                                   probabilitiesOfClasses = (double[])objects[1]; break;
            case CLASSIFICATION_7_PERCEPTRON                     : if( namesOfClasses.length != 2 ) throw new Exception("Input data is not for binary classification");
                                                                   variableNames = LinearRegression.addInterceptToRegression(variableNames, dataMatrix);
                                                                   numberOfIterativeSteps = 300;
                                                                   coefficientsInitialApproxomation = MultivariateSample.getMeanVector(dataMatrix);
                                                                   // coefficientsInitialApproxomation = null;
                                                                   Perceptron perceptron = new Perceptron(dataMatrix, indicesOfClasses, coefficientsInitialApproxomation, numberOfIterativeSteps, maxNumberOfIterations, eps, log);
                                                                   perceptron.writeClassificationModel(variableNames, pathToOutputs);
                                                                   predictedIndicesOfClasses = perceptron.predictIndicesOfClasses(dataMatrix); break;
            default                                              : throw new Exception("This classification type '" + classificationType + "' is not supported in our regression analysis currently");
        }
        if( doPrintAccompaniedInformation )
        {
            Classification.writeTrueClassificationRatesIntoTable(indicesOfClasses, predictedIndicesOfClasses, namesOfClasses, classifierName, pathToOutputs, "trueClassificationRates");
            Classification.writePredictionsIntoTable(indicesOfClasses, probabilitiesOfClasses, predictedIndicesOfClasses, namesOfClasses, objectNames, pathToOutputs, "predictions");
        }
        writeTableWithClassificationTypeAndClassifierName(pathToOutputs, NAME_OF_TABLE_WITH_CLASSIFICATION_TYPE_AND_CLASSIFIER_NAME);
        ParticularTable.writeTableWithVariableNames(variableNames, pathToOutputs, ParticularTable.NAME_OF_TABLE_WITH_VARIABLE_NAMES);
        writeTableWithNamesOfClasses(classifierName, namesOfClasses, pathToOutputs, NAME_OF_TABLE_WITH_NAMES_OF_CLASSES);
        return predictedIndicesOfClasses;
    }
    
//    public static void test()
//    {
//        double[][] dataMatrix = new double[][]{new double[]{1.0, 2.0},
//                                               new double[]{3.0, -4.0},
//                                               new double[]{-5.0, 6.0}};
//        double[] diagonal = new double[]{1.0, 2.0, 3.0};
//        MatrixUtils.printMatrix(log, "dataMatrix", dataMatrix);
//        
//        double[][] matrix1 = MatrixUtils.getProductOfRectangularTransposedAndDiagonalMatrices(dataMatrix, diagonal);
//        MatrixUtils.printMatrix(log, "matrix1", matrix1);
//
//        double[][] matrix2 = MatrixUtils.getProductOfRectangularTransposedAndDiagonalAndRectangularMatrices(dataMatrix, diagonal);
//        MatrixUtils.printMatrix(log, "matrix2", matrix2);
//        
//        double[][] matrix = MatrixUtils.getProductOfSymmetricAndRectangularMatrices(matrix2, matrix1);
//        MatrixUtils.printMatrix(log, "matrix", matrix);
//    }

    
    private void writeTableWithClassificationTypeAndClassifierName(DataElementPath pathToOutputs, String tableName)
    {
        TableUtils.writeStringTable(new String[]{classificationType, classifierName}, new String[]{"Classification type", "Classifier name"}, "value", pathToOutputs.getChildPath(tableName));
    }
    
    /***
     * 
     * @param pathToFolder
     * @param tableName
     * @return String[] array : array[0] = (String) classificationType; array[1] = (String) classifierName;
     */
    private static String[] readClassificationTypeAndClassifierNameInTable(DataElementPath pathToFolder, String tableName)
    {
        Map<String, String> tableColumn = TableUtils.readGivenColumnInStringTable(pathToFolder.getChildPath(tableName), "value");
        return new String[]{tableColumn.get("Classification type"), tableColumn.get("Classifier name")};
    }

    private static void writeTableWithNamesOfClasses(String classifierName, String[] namesOfClasses, DataElementPath pathToOutputs, String tableName)
    {
        String[] namesOfColumns = new String[namesOfClasses.length];
        for( int i = 0; i < namesOfClasses.length; i++ )
            namesOfColumns[i] = classifierName + "_" + Integer.toString(i);
        TableUtils.writeStringTable(new String[][]{namesOfClasses}, new String[]{NAMES_OF_CLASSES}, namesOfColumns, pathToOutputs.getChildPath(tableName));
    }

    private static String[] readNamesOfClassesInTable(DataElementPath pathToFolderWithSavedModel, String tableName) throws Exception
    {
        TableDataCollection table = pathToFolderWithSavedModel.getChildPath(tableName).getDataElement(TableDataCollection.class);
        return TableUtils.readGivenRowInStringTable(table, NAMES_OF_CLASSES);
    }

    private static void writePredictionsIntoTable(String[] objectNames, double[] probabilitiesOfClasses, String[] namesOfClassesForEachObject, String nameOfColumnWithPrediction, DataElementPath pathToOutputs, String tableName)
    {
        if( probabilitiesOfClasses == null )
            TableUtils.writeStringTable(namesOfClassesForEachObject, objectNames, nameOfColumnWithPrediction, pathToOutputs.getChildPath(tableName));
        else
        {
            String[][] stringData = new String[probabilitiesOfClasses.length][1];
            double[][] doubleData = new double[probabilitiesOfClasses.length][1];
            for( int i = 0; i < probabilitiesOfClasses.length; i++ )
            {
                doubleData[i][0] = probabilitiesOfClasses[i];
                stringData[i][0] = namesOfClassesForEachObject[i];
            }
            TableUtils.writeDoubleAndString(doubleData, stringData, objectNames, new String[]{"probabilities"}, new String[]{nameOfColumnWithPrediction}, pathToOutputs, tableName);
        }
    }

    private int[] readClassificationModelAndPredictClasses(double[][] dataMatrix, String[] objectNames, String[] variableNamesInModel, DataElementPath pathToFolderWithSavedModel, DataElementPath pathToOutputs, Logger log, boolean doWritePrediction, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        double[] probabilitiesOfClasses = null;
        if( jobControl != null ) jobControl.setPreparedness(from);
        String[] classificationTypeAndClassifier = readClassificationTypeAndClassifierNameInTable(pathToFolderWithSavedModel, NAME_OF_TABLE_WITH_CLASSIFICATION_TYPE_AND_CLASSIFIER_NAME);
        String[] namesOfClasses = readNamesOfClassesInTable(pathToFolderWithSavedModel, NAME_OF_TABLE_WITH_NAMES_OF_CLASSES);
        classificationType = classificationTypeAndClassifier[0];
        int[] predictedIndicesOfClasses = null;
        Object[] objects = null;
        switch( classificationType )
        {
            case CLASSIFICATION_1_MRIM                           : double[][] coefficientMatrixForClassPrediction = ClassificationByMultivariateRegressionOfIndicatorMatrix.readClassificationModel(variableNamesInModel, pathToFolderWithSavedModel, NAME_OF_TABLE_WITH_COEFFICIENT_MATRIX);
                                                                   predictedIndicesOfClasses = ClassificationByMultivariateRegressionOfIndicatorMatrix.predictIndicesOfClasses(coefficientMatrixForClassPrediction, dataMatrix);
                                                                   if( doWritePrediction )
                                                                       variableNamesInModel = MatrixUtils.removeGivenColumn(dataMatrix, variableNamesInModel, LinearRegression.INTERCEPT); break;
            case CLASSIFICATION_2_FISHER_LDA                     : objects = FisherLDA.readClassificationModel(variableNamesInModel, namesOfClasses, pathToFolderWithSavedModel, FisherLDA.NAME_OF_TABLE_WITH_FUNCTION_COEFFICIENTS, FisherLDA.NAME_OF_TABLE_WITH_MEAN_VECTORS_FOR_CLASSES);
                                                                   predictedIndicesOfClasses = FisherLDA.predictIndicesOfClasses((double[])objects[0], (double[][])objects[1], dataMatrix); break;
            case CLASSIFICATION_3_MUXIMUM_LIKELIHOOD_MAHALANOBIS : objects = MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance.readClassificationModel(namesOfClasses, variableNamesInModel, pathToFolderWithSavedModel, FisherLDA.NAME_OF_TABLE_WITH_MEAN_VECTORS_FOR_CLASSES, MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance.NAME_OF_TABLE_WITH_INVERSE_COVARIANCE_MATRIX);
                                                                   predictedIndicesOfClasses = MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance.predictIndicesOfClasses((double[][])objects[0], (double[][])objects[1], dataMatrix); break;
            case CLASSIFICATION_4_SVM_C_CLASSIFICATION           : String scriptToReadModelForSVMcClassificationAndPredict = RHelper.getScript("ClassificationAnalysis", "ReadModelForSVMc-classificationAndPredict");
                                                                   predictedIndicesOfClasses = Rutils.readClassificationModelAndPredictIndicesOfClassesUsingR(dataMatrix, namesOfClasses, scriptToReadModelForSVMcClassificationAndPredict, pathToFolderWithSavedModel, NAME_OF_FILE_WITH_SVM_C_CLASSIFICATION_MODEL, log, jobControl, from, to); break;
            case CLASSIFICATION_5_EDDA                           : String scriptToReadModelForEDDAclassificationAndPredict = RHelper.getScript("ClassificationAnalysis", "ReadModelForEDDAclassificationAndPredict");
                                                                   predictedIndicesOfClasses = Rutils.readClassificationModelAndPredictIndicesOfClassesUsingR(dataMatrix, namesOfClasses, scriptToReadModelForEDDAclassificationAndPredict, pathToFolderWithSavedModel, NAME_OF_FILE_WITH_EDDA_CLASSIFICATION_MODEL, log, jobControl, from, to); break;
            case CLASSIFICATION_6_LOGISTIC_REGRESSION            : LogisticRegression logisticRegression = LogisticRegression.readClassificationModel(variableNamesInModel, pathToFolderWithSavedModel);
                                                                   // predictedIndicesOfClasses = logisticRegression.predictIndicesOfClasses(dataMatrix); break;
                                                                   objects = logisticRegression.predictIndicesOfClassesWithProbabilities(dataMatrix);
                                                                   predictedIndicesOfClasses = (int[])objects[0];
                                                                   probabilitiesOfClasses = (double[])objects[1]; break;
            case CLASSIFICATION_7_PERCEPTRON                     : Perceptron perceptron = Perceptron.readClassificationModel(variableNamesInModel, pathToFolderWithSavedModel);
                                                                   predictedIndicesOfClasses = perceptron.predictIndicesOfClasses(dataMatrix); break;
            default                                              : throw new Exception("This classification type '" + classificationType + "' is not supported in our classification analysis currently");
        }
        if( doWritePrediction )
        {
            String predictedClassifierName = classificationTypeAndClassifier[1] + "_predicted";
            String[] namesOfClassesForEachObject = Classification.getNamesOfClassesForEachObject(predictedIndicesOfClasses, namesOfClasses);
            //writeDoubleDataMatrixAndStringColumnIntoTable(dataMatrix, objectNames, variableNamesInModel, predictedClassifierName, namesOfClassesForEachObject, pathToOutputs, "predictionsOfClasses");
            writePredictionsIntoTable(objectNames, probabilitiesOfClasses, namesOfClassesForEachObject, predictedClassifierName, pathToOutputs, "predictions");
        }
        if( jobControl != null ) jobControl.setPreparedness(to);
        return predictedIndicesOfClasses;
    }

    /***
     * 
     * @param dataMatrix
     * @param indicesOfClasses
     * @param percentageOfDataForTraining
     * @return Object[] array : array[0] = double[][] dataMatrixForTrain; array[1] = int[] indicesOfClassesForTrain; array[2] = double[][] dataMatrixForTest; array[3] = int[] indicesOfClassesForTest;
     */
    private Object[] splitDataSet(double[][] dataMatrix, int[] indicesOfClasses, int percentageOfDataForTraining)
    {
        int sizeForTrain = dataMatrix.length * percentageOfDataForTraining / 100, sizeForTest = dataMatrix.length - sizeForTrain;
        int[] indices = IntStreamEx.ofIndices(dataMatrix).toArray();
        StatUtil.shuffleVector(indices, 0);
        double[][] dataMatrixForTrain = new double[sizeForTrain][], dataMatrixForTest = new double[sizeForTest][];
        int[] indicesOfClassesForTrain = new int[sizeForTrain], indicesOfClassesForTest = new int[sizeForTest];
        for( int i = 0; i < sizeForTrain; i++ )
        {
            dataMatrixForTrain[i] = dataMatrix[indices[i]];
            indicesOfClassesForTrain[i] = indicesOfClasses[indices[i]];
        }
        for( int i = 0; i < sizeForTest; i++ )
        {
            dataMatrixForTest[i] = dataMatrix[indices[i + sizeForTrain]];
            indicesOfClassesForTest[i] = indicesOfClasses[indices[i + sizeForTrain]];
        }
        return new Object[]{dataMatrixForTrain, indicesOfClassesForTrain, dataMatrixForTest, indicesOfClassesForTest};
    }

    private Object[] readDataSubMatrixAndNamesOfClassesForEachObject() throws IOException
    {
        if( ! (pathToDataMatrix.getDataElement() instanceof TableDataCollection) )
        {
            Object[] objects = FileUtils.readMatrixOrSubmatix(pathToDataMatrix, new String[]{classifierName}, FileUtils.STRING_TYPE);
            String[][] stringMatrix = (String[][])objects[2];
            String[] namesOfClassesForEachObject = new String[stringMatrix.length];
            for( int i = 0; i < stringMatrix.length; i++ )
                namesOfClassesForEachObject[i] = stringMatrix[i][0];
            objects = FileUtils.readMatrixOrSubmatix(pathToDataMatrix, variableNames, FileUtils.DOUBLE_TYPE);
            return removeObjectsWithMissingData((String[])objects[0], (double[][])objects[2], namesOfClassesForEachObject);
        }
        Object[] objects = TableUtils.readDataSubMatrixAndStringColumn(pathToDataMatrix, variableNames, classifierName);
        return removeObjectsWithMissingData((String[])objects[0], (double[][])objects[1], (String[])objects[2]);
    }
    
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
    
    //////////
    private static Logger log = Logger.getLogger(ClassificationEngine.class.getName());

}
