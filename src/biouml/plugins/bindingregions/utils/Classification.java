package biouml.plugins.bindingregions.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import one.util.streamex.IntStreamEx;

import org.apache.commons.lang.ArrayUtils;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import biouml.plugins.bindingregions.rscript.Rutils;
import biouml.plugins.bindingregions.utils.LinearRegression.MultivariateLinearRegression;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * @author yura
 *
 */
public class Classification
{
    public static String NAME_OF_TABLE_WITH_COEFFICIENTS = "coefficients";
    
    // to remove such i < max{indicesOfClasses[]} that i is not belong to {indicesOfClasses[0],..., indicesOfClasses[n-1]}
    // i.e. i-th class is empty and it is necessary remove such classes
    public static int[] removeEmptyClasses(int[] indicesOfClasses)
    {
        Set<Integer> distinctIndicesOfClusters = Clusterization.getDistinctIndicesOfClusters(indicesOfClasses);
        if( (int)MatrixUtils.getMaximalValue(indicesOfClasses)[0] + 1 == distinctIndicesOfClusters.size() ) return indicesOfClasses;
        Map<Integer, Integer> oldIndexAndNewIndex = new HashMap<>();
        int newIndex = 0;
        for( Integer oldIndex : distinctIndicesOfClusters )
            oldIndexAndNewIndex.put(oldIndex, newIndex++);
        int[] newIndicesOfClasses = IntStreamEx.of( indicesOfClasses ).map( oldIndexAndNewIndex::get ).toArray();
        return newIndicesOfClasses;
    }
    
    // it is copied
    public static Object[] getIndicesOfClassesAndNamesOfClasses(String[] namesOfClassesForEachObject)
    {
        String[] namesOfClasses = getNamesOfClasses(namesOfClassesForEachObject);
        int[] indicesOfClasses = getIndicesOfClasses(namesOfClassesForEachObject, namesOfClasses);
        return new Object[]{indicesOfClasses, namesOfClasses};
    }
    
    // it is copied
    public static String[] getNamesOfClasses(String[] namesOfClassesForEachObject)
    {
        Set<String> set = new HashSet<>();
        for( String name : namesOfClassesForEachObject )
            set.add(name);
        return set.toArray(new String[0]);
    }
    
    // it is copied
    public static int[] getIndicesOfClasses(String[] namesOfClassesForEachObject, String[] namesOfClasses)
    {
        int[] indicesOfClasses = new int[namesOfClassesForEachObject.length];
        for( int i = 0; i <namesOfClassesForEachObject.length; i++ )
            indicesOfClasses[i] = ArrayUtils.indexOf(namesOfClasses, namesOfClassesForEachObject[i]);
        return indicesOfClasses;
    }
    
    // it is copied
    public static String[] getNamesOfClassesForEachObject(int[] indicesOfClasses, String[] namesOfClasses)
    {
        String[] namesOfClassesForEachObject = new String[indicesOfClasses.length];
        for(int i = 0; i < indicesOfClasses.length; i++ )
            namesOfClassesForEachObject[i] = namesOfClasses[indicesOfClasses[i]];
        return namesOfClassesForEachObject;
    }
    
    /***
     * 
     * @param indicesOfClasses
     * @param predictedIndicesOfClasses
     * @return array : array[0] = totalTrueClassificationRate; array[1] = Map<Integer, Double> indicesOfClassesAndTrueClassificationRates;
     */
    public static Object[] getTrueClassificationRates(int[] indicesOfClasses, int[] predictedIndicesOfClasses)
    {
        TObjectIntMap<Integer> classesFrequencies = new TObjectIntHashMap<>();
        TObjectIntMap<Integer> truePredictedClassesFrequencies = new TObjectIntHashMap<>();
        for( int i = 0; i < indicesOfClasses.length; i++ )
        {
            classesFrequencies.adjustOrPutValue(indicesOfClasses[i], 1, 1);
            if( indicesOfClasses[i] == predictedIndicesOfClasses[i] )
                truePredictedClassesFrequencies.adjustOrPutValue(predictedIndicesOfClasses[i], 1, 1);
        }
        double trueClassificationFrequency = 0;
        Map<Integer, Double> indicesOfClassesAndTrueClassificationRates = new HashMap<>();
        for( Integer indexOfClass : classesFrequencies.keySet() )
        {
            int truePredictedClassFrequency = truePredictedClassesFrequencies.containsKey(indexOfClass) ? truePredictedClassesFrequencies.get(indexOfClass) : 0;
            indicesOfClassesAndTrueClassificationRates.put(indexOfClass, (double)truePredictedClassFrequency / classesFrequencies.get(indexOfClass));
            trueClassificationFrequency += truePredictedClassFrequency;
        }
        double totalTrueClassificationRate = trueClassificationFrequency / indicesOfClasses.length;
        return new Object[]{totalTrueClassificationRate, indicesOfClassesAndTrueClassificationRates};
    }
    
    public static void writeTrueClassificationRatesIntoTable(int[] indicesOfClasses, int[] predictedIndicesOfClasses, String[] namesOfClasses, String classifierName, DataElementPath pathToOutputs, String tableName)
    {
        Object[] objects = getTrueClassificationRates(indicesOfClasses, predictedIndicesOfClasses);
        Map<Integer, Double> indicesOfClassesAndTrueClassificationRates = (Map<Integer, Double>)objects[1];
        double[] rates = new double[indicesOfClassesAndTrueClassificationRates.size() + 1];
        String[] rowNames = new String[indicesOfClassesAndTrueClassificationRates.size() + 1];
        int index = 0;
        for( Entry<Integer, Double> entry : indicesOfClassesAndTrueClassificationRates.entrySet() )
        {
            rowNames[index] = namesOfClasses != null ? namesOfClasses[index] : classifierName + "_" + Integer.toString(entry.getKey());
            rates[index++] = entry.getValue();
        }
        rowNames[indicesOfClassesAndTrueClassificationRates.size()] = classifierName == null ? "whole set" : "whole set of " + classifierName;
        rates[indicesOfClassesAndTrueClassificationRates.size()] = (double)objects[0];
        TableUtils.writeDoubleTable(rates, rowNames, "True classification rate", pathToOutputs, tableName);
    }
    
    public static void writePredictionsIntoTable(int[] indicesOfClasses, double[] probabilitiesOfClasses, int[] predictedIndicesOfClasses, String[] namesOfClasses, String[] objectNames, DataElementPath pathToOutputs, String tableName)
    {
        String[] namesOfClassesForEachObject = getNamesOfClassesForEachObject(indicesOfClasses, namesOfClasses);
        String[] predictedNamesOfClassesForEachObject = getNamesOfClassesForEachObject(predictedIndicesOfClasses, namesOfClasses);
        String[][] data = new String[objectNames.length][];
        for( int i = 0; i < objectNames.length; i++ )
            data[i] = new String[]{namesOfClassesForEachObject[i], predictedNamesOfClassesForEachObject[i], indicesOfClasses[i] == predictedIndicesOfClasses[i] ? "+" : "-"};
        if( probabilitiesOfClasses == null )
            TableUtils.writeStringTable(data, objectNames, new String[]{"names_of_classes_for_each_object", "predicted_names_of_classes_for_each_object", "is_prediction_true"}, pathToOutputs.getChildPath(tableName));
        else
        {
            double[][] doubleData = new double[probabilitiesOfClasses.length][1];
            for( int i = 0; i < probabilitiesOfClasses.length; i++ )
                doubleData[i][0] = probabilitiesOfClasses[i];
            TableUtils.writeDoubleAndString(doubleData, data, objectNames, new String[]{"probabilities_of_classes"}, new String[]{"names_of_classes_for_each_object", "predicted_names_of_classes_for_each_object", "is_prediction_true"}, pathToOutputs, tableName);
        }
    }
    
    public static double calculationOfTotalTrueClassificationRate(int[] indicesOfClasses, int[] predictedIndicesOfClasses)
    {
        int trueClassifications = 0;
        for( int i = 0; i < indicesOfClasses.length; i++ )
            if( indicesOfClasses[i] == predictedIndicesOfClasses[i])
                trueClassifications += 1;
        return (double)trueClassifications / (double)predictedIndicesOfClasses.length;
    }

    /***************** ClassificationByMultivariateRegressionOfIndicatorMatrix : start ****************/
    public static class ClassificationByMultivariateRegressionOfIndicatorMatrix
    {
        // it is assumed that there are no empty classes !!!!!
        private static double[][] getIndicatorMatrix(int[] indicesOfClasses)
        {
            int numberOfClasses = 1 + (int)MatrixUtils.getMaximalValue(indicesOfClasses)[0];
            double[][] result = MatrixUtils.getMatrixWithEqualElements(indicesOfClasses.length, numberOfClasses, 0.0);
            for( int i = 0; i < indicesOfClasses.length; i++ )
                result[i][indicesOfClasses[i]] = 1.0;
            return result;
        }
        
        private static double[][] getCoefficientMatrixForClassPrediction(double[][] dataMatrix, int[] indicesOfClasses, int maxNumberOfIterations, double eps)
        {
            MultivariateLinearRegression mlr = new MultivariateLinearRegression(dataMatrix, getIndicatorMatrix(indicesOfClasses));
            return mlr.getCoefficients(maxNumberOfIterations, eps);
        }
        
        public static int predictIndexOfClass(double[] rowOfDataMatrix, double[][] coefficientMatrixForClassPrediction)
        {
            double[] predictions = MatrixUtils.getProductOfTransposedVectorAndRectangularMatrix(coefficientMatrixForClassPrediction, rowOfDataMatrix);
            return (int)MatrixUtils.getMaximalValue(predictions)[1];
        }
        
        public static int[] predictIndicesOfClasses(double[][] coefficientMatrixForClassPrediction, double[][] dataMatrix)
        {
            int[] result = new int[dataMatrix.length];
            for( int i = 0; i < dataMatrix.length; i++ )
                result[i] = predictIndexOfClass(dataMatrix[i], coefficientMatrixForClassPrediction);
            return result;
        }
        
        public static double[][] createClassificationModel(double[][] dataMatrix, int[] indicesOfClasses, int maxNumberOfIterations, double eps)
        {
            return getCoefficientMatrixForClassPrediction(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
        }
        
        public static void writeClassificationModel(double[][] coefficientMatrixForClassPrediction, String[] variableNames, String[] namesOfClasses, DataElementPath pathToOutputs, String tableName)
        {
            TableUtils.writeDoubleTable(coefficientMatrixForClassPrediction, variableNames, namesOfClasses, pathToOutputs, tableName);
        }
        
        /***
         * 
         * @param variableNamesInModel
         * @param pathToFolderWithSavedModel
         * @param tableName
         * @return double[][] coefficientMatrixForClassPrediction
         */
        public static double[][] readClassificationModel(String[] variableNamesInModel, DataElementPath pathToFolderWithSavedModel, String tableName)
        {
            Object[] objects =TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(tableName));
            return  MatrixUtils.getSubmatrixRowWise((String[])objects[0], (double[][])objects[2], variableNamesInModel);
        }
        
        /***
         * @param dataMatrix : dataMatrix can contain the intercept !
         * @param indicesOfClasses
         * @param maxNumberOfIterations
         * @param eps
         * @return Object[] array : array[0] = int[] predictedIndicesOfClasses; array[1] = double[][] coefficientMatrixForClassPrediction;
         */
        public static Object[] createClassificationModelAndPredictIndicesOfClasses(double[][] dataMatrix, int[] indicesOfClasses, int maxNumberOfIterations, double eps)
        {
            double[][] coefficientMatrixForClassPrediction = createClassificationModel(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
            int[] predictedIndicesOfClasses = predictIndicesOfClasses(coefficientMatrixForClassPrediction, dataMatrix);
            return new Object[]{predictedIndicesOfClasses, coefficientMatrixForClassPrediction};
        }
    }
    /***************** ClassificationByMultivariateRegressionOfIndicatorMatrix : finish ************/

    /************************************ LogisticRegression : start ****************************/
    public static class LogisticRegression
    {
        private double[] coefficients;
        
        // dim(dataMatrix) = n x m; dim(response) = n; dataMatrix must contain intercept !!!
        // indicesOfClasses[] (- {0, 1}
        public LogisticRegression(double[][] dataMatrix, int[] indicesOfClasses, double[] coefficientsInitialApproxomation, int numberOfIterativeSteps, int maxNumberOfIterations, double eps, Logger log)
        {
            this.coefficients = coefficientsInitialApproxomation != null ? coefficientsInitialApproxomation : new double[dataMatrix[0].length];
            createClassificationModel(dataMatrix, indicesOfClasses, numberOfIterativeSteps, maxNumberOfIterations, eps, log);
        }
        
        public LogisticRegression(double[] coefficients)
        {
            this.coefficients = coefficients;
        }
        
        private void createClassificationModel(double[][] dataMatrix, int[] indicesOfClasses, int numberOfIterativeSteps, int maxNumberOfIterations, double eps, Logger log)
        {
            double probabilityThreshold = 1.0E-10, trueClassificationRateOptimal = 0.0;
            double[] coefficientsOptimal = null;
            for( int iterations = 0; iterations < numberOfIterativeSteps; iterations++ )
            {
                double[] probabilities = calculateProbabilitiesOfFirstClass(dataMatrix);

                // 1. Correction of probabilities to avoid probabilities[i] == 0.0 or probabilities[i] == 1.0
                for( int i = 0; i < probabilities.length; i++ )
                {
                    if( probabilities[i] < probabilityThreshold )
                        probabilities[i] = probabilityThreshold;
                    else if( probabilities[i] > 1.0 - probabilityThreshold )
                        probabilities[i] = 1.0 - probabilityThreshold;
                }

                // 2. Calculation of coefficients
                double[] diagonal = new double[probabilities.length];
                for( int i = 0; i < diagonal.length; i++ )
                    diagonal[i] = probabilities[i] * (1.0 - probabilities[i]);
                double[] z = MatrixUtils.getProductOfRectangularMatrixAndVector(dataMatrix, coefficients);
                for( int i = 0; i < z.length; i++ )
                    z[i] += ((double)indicesOfClasses[i] - probabilities[i]) / diagonal[i];
                double[][] symmetricMatrix = MatrixUtils.getProductOfRectangularTransposedAndDiagonalAndRectangularMatrices(dataMatrix, diagonal);
                symmetricMatrix = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(symmetricMatrix, maxNumberOfIterations, eps);
                double[][] matrix = MatrixUtils.getProductOfRectangularTransposedAndDiagonalMatrices(dataMatrix, diagonal);
                matrix = MatrixUtils.getProductOfSymmetricAndRectangularMatrices(symmetricMatrix, matrix);
                coefficients = MatrixUtils.getProductOfRectangularMatrixAndVector(matrix, z);
                
                // 3. Identification of coefficientsOptimal and trueClassificationRateOptimal
                int[] predictedIndicesOfClasses = predictIndicesOfClasses(dataMatrix);
                double totalTrueClassificationRate = calculationOfTotalTrueClassificationRate(indicesOfClasses, predictedIndicesOfClasses);
                if( log != null )
                {
                    String s = "TCR = " + Double.toString(totalTrueClassificationRate) + " coefficients =";
                    for( double x : coefficients )
                        s += " " + Double.toString(x);
                    log.info(s);
                }
                if( totalTrueClassificationRate > trueClassificationRateOptimal )
                {
                    coefficientsOptimal = ArrayUtils.clone(coefficients);
                    trueClassificationRateOptimal = totalTrueClassificationRate;
                }
            }
            coefficients = coefficientsOptimal;
        }
        
        public void writeClassificationModel(String[] variableNames, DataElementPath pathToOutputs)
        {
            TableUtils.writeDoubleTable(coefficients, variableNames, "coefficients", pathToOutputs, NAME_OF_TABLE_WITH_COEFFICIENTS);
        }
        
        public static LogisticRegression readClassificationModel(String[] variableNamesInModel, DataElementPath pathToFolderWithSavedModel)
        {
            Object[] objects =TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(NAME_OF_TABLE_WITH_COEFFICIENTS));
            double[][] temporaryMatrix = MatrixUtils.getSubmatrixRowWise((String[])objects[0], (double[][])objects[2], variableNamesInModel);
            double[] coefficients = MatrixUtils.getColumn(temporaryMatrix, 0);
            return new LogisticRegression(coefficients);
        }
        
        public int[] predictIndicesOfClasses(double[][] dataMatrix)
        {
            return (int[])predictIndicesOfClassesWithProbabilities(dataMatrix)[0];
        }
        
        public Object[] predictIndicesOfClassesWithProbabilities(double[][] dataMatrix)
        {
            double[] probabilities = calculateProbabilitiesOfFirstClass(dataMatrix);
            int[] predictedIndicesOfClasses = new int[probabilities.length];
            for( int i = 0; i < predictedIndicesOfClasses.length; i++ )
            {
                predictedIndicesOfClasses[i] = probabilities[i] >= 0.5 ? 0 : 1;
                if( predictedIndicesOfClasses[i] == 1 )
                    probabilities[i] = 1.0 - probabilities[i]; 
            }
            return new Object[]{predictedIndicesOfClasses, probabilities};
        }
        
        private double[] calculateProbabilitiesOfFirstClass(double[][] dataMatrix)
        {
            double[] probabilities = new double[dataMatrix.length];
            for( int i = 0; i < dataMatrix.length; i++ )
                probabilities[i] = calculateLogisticFunction(MatrixUtils.getInnerProduct(coefficients, dataMatrix[i]));
            return probabilities;
        }
        
        public static double calculateLogisticFunction(double x)
        {
            return 1.0 / (1.0 + Math.exp(x));
        }
    }
    /************************************ LogisticRegression : finish ****************************/
    
    /************************************ Perceptron : start ****************************/
    public static class Perceptron
    {
        private double[] coefficients;
        
        // dim(dataMatrix) = n x m; dim(response) = n; dataMatrix must contain intercept !!!
        // indicesOfClasses[] (- {0, 1}
        public Perceptron(double[][] dataMatrix, int[] indicesOfClasses, double[] coefficientsInitialApproxomation, int numberOfIterativeSteps, int maxNumberOfIterations, double eps, Logger log)
        {
            this.coefficients = coefficientsInitialApproxomation != null ? coefficientsInitialApproxomation : new double[dataMatrix[0].length];
            createClassificationModel(dataMatrix, indicesOfClasses, numberOfIterativeSteps, maxNumberOfIterations, eps, log);
        }
        
        public Perceptron(double[] coefficients)
        {
            this.coefficients = coefficients;
        }
        
        private void createClassificationModel(double[][] dataMatrix, int[] indicesOfClasses, int numberOfIterativeSteps, int maxNumberOfIterations, double eps, Logger log)
        {
            double trueClassificationRateOptimal = 0.0;
            double[] coefficientsOptimal = null;
            for( int iterations = 0; iterations < numberOfIterativeSteps; iterations++ )
            {
                // 1. Identification of coefficientsOptimal and trueClassificationRateOptimal
                int[] predictedIndicesOfClasses = predictIndicesOfClasses(dataMatrix);
                double totalTrueClassificationRate = calculationOfTotalTrueClassificationRate(indicesOfClasses, predictedIndicesOfClasses);
                if( log != null )
                {
                    String s = "TCR = " + Double.toString(totalTrueClassificationRate) + " coefficients =";
                    for( double x : coefficients )
                        s += " " + Double.toString(x);
                    log.info(s);
                }
                if( totalTrueClassificationRate > trueClassificationRateOptimal )
                {
                    coefficientsOptimal = ArrayUtils.clone(coefficients);
                    trueClassificationRateOptimal = totalTrueClassificationRate;
                }
                
                // 2. Re-calculation of coefficients
                for( int i = 0; i < indicesOfClasses.length; i++ )
                    if( predictedIndicesOfClasses[i] != indicesOfClasses[i] )
                        for( int j = 0; j < coefficients.length; j++ )
                            if( indicesOfClasses[i] == 0 )
                                coefficients[j] += dataMatrix[i][j];
                            else
                                coefficients[j] -= dataMatrix[i][j];
            }
            coefficients = coefficientsOptimal;
        }
        
        public int[] predictIndicesOfClasses(double[][] dataMatrix)
        {
            int[] predictedIndicesOfClasses = new int[dataMatrix.length];
            for( int i = 0; i < predictedIndicesOfClasses.length; i++ )
                predictedIndicesOfClasses[i] = MatrixUtils.getInnerProduct(coefficients, dataMatrix[i]) > 0 ? 0 : 1;
            return predictedIndicesOfClasses;
        }
        
        public void writeClassificationModel(String[] variableNames, DataElementPath pathToOutputs)
        {
            TableUtils.writeDoubleTable(coefficients, variableNames, "coefficients", pathToOutputs, NAME_OF_TABLE_WITH_COEFFICIENTS);
        }
        
        public static Perceptron readClassificationModel(String[] variableNamesInModel, DataElementPath pathToFolderWithSavedModel)
        {
            Object[] objects =TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(NAME_OF_TABLE_WITH_COEFFICIENTS));
            double[][] temporaryMatrix = MatrixUtils.getSubmatrixRowWise((String[])objects[0], (double[][])objects[2], variableNamesInModel);
            double[] coefficients = MatrixUtils.getColumn(temporaryMatrix, 0);
            return new Perceptron(coefficients);
        }
    }
    /************************************ Perceptron : finish ****************************/

    /************************************ FisherLDA : start ****************************/
    public static class FisherLDA
    {
        public static final String NAME_OF_TABLE_WITH_FUNCTION_COEFFICIENTS = "linearDiscriminantFunction";
        public static final String NAME_OF_TABLE_WITH_MEAN_VECTORS_FOR_CLASSES = "meanVectorsForClasses";
        
        private static double[] getLinearDiscriminantFunction(double[][] dataMatrix, int[] indicesOfClasses, int maxNumberOfIterations, double eps)
        {
          Object[] objects = MultivariateSamples.getWithinAndBetweenAndTotalSSPmatrices(dataMatrix, indicesOfClasses);
          double[][] inverseWithinSSPmatrix = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod((double[][])objects[0], maxNumberOfIterations, eps);
          double[][] productMatrix = MatrixUtils.getProductOfSymmetricMatrices(inverseWithinSSPmatrix, (double[][])objects[1]);
          objects = MatrixUtils.getMaximalEigenValueOfSquareMatrixByLyusternikMethod(productMatrix, maxNumberOfIterations, eps);
          return (double[])objects[1];
        }

        private static int predictIndexOfClass(double[] rowOfDataMatrix, double[] linearDiscriminantFunction, double[][] meanVectorsForClasses)
        {
            double[] scores = new double[meanVectorsForClasses.length];
            for( int i = 0; i < meanVectorsForClasses.length; i++ )
            {
                double[] vector = MatrixUtils.getSubtractionOfVectors(rowOfDataMatrix, meanVectorsForClasses[i]);
                scores[i] = Math.abs(MatrixUtils.getInnerProduct(linearDiscriminantFunction, vector));
            }
            return (int)MatrixUtils.getMinimalValue(scores)[1];
        }

        public static int[] predictIndicesOfClasses(double[] linearDiscriminantFunction, double[][] meanVectorsForClasses, double[][] dataMatrix)
        {
            int[] predictedIndicesOfClasses = new int[dataMatrix.length];
        
            for( int i = 0; i < dataMatrix.length; i++ )
                predictedIndicesOfClasses[i] = predictIndexOfClass(dataMatrix[i], linearDiscriminantFunction, meanVectorsForClasses);
            return predictedIndicesOfClasses;
        }
        
        /***
         * 
         * @param dataMatrix
         * @param indicesOfClasses
         * @param maxNumberOfIterations
         * @param eps
         * @return Object[] array : array[0] = double[] linearDiscriminantFunction, array[1] = double[][] meanVectorsForClasses;
         */
        public static Object[] createClassificationModel(double[][] dataMatrix, int[] indicesOfClasses, int maxNumberOfIterations, double eps)
        {
            double[] linearDiscriminantFunction = getLinearDiscriminantFunction(dataMatrix, indicesOfClasses, maxNumberOfIterations, eps);
            Map<Integer, double[][]> classIndexAndDataMatrix = Clusterization.createClusterIndexAndDataMatrix(dataMatrix, indicesOfClasses);
            double[][] meanVectorsForClasses = new double[classIndexAndDataMatrix.size()][];
            for( int i = 0; i < meanVectorsForClasses.length; i++ )
                meanVectorsForClasses[i] = MultivariateSample.getMeanVector(classIndexAndDataMatrix.get(i));
            return new Object[]{linearDiscriminantFunction, meanVectorsForClasses};
        }
        
        public static void writeClassificationModel(double[] linearDiscriminantFunction, double[][] meanVectorsForClasses, String[] namesOfClasses, String[] variableNames, DataElementPath pathToOutputs, String nameOfTableWithLinearDiscriminantFunction, String nameOfTableWithMeanValuesForClasses)
        {
            TableUtils.writeDoubleTable(linearDiscriminantFunction, variableNames, "coefficient", pathToOutputs, nameOfTableWithLinearDiscriminantFunction);
            TableUtils.writeDoubleTable(meanVectorsForClasses, namesOfClasses, variableNames, pathToOutputs, nameOfTableWithMeanValuesForClasses);
        }
        
        /***
         * 
         * @param variableNamesInModel
         * @param namesOfClasses
         * @param pathToFolderWithSavedModel
         * @param nameOfTableWithLinearDiscriminantFunction
         * @param nameOfTableWithMeanVectorsForClasses
         * @return Object[] array : array[0] = double[] linearDiscriminantFunction, array[1] = double[][] meanVectorsForClasses;
         */
        public static Object[] readClassificationModel(String[] variableNamesInModel, String[] namesOfClasses, DataElementPath pathToFolderWithSavedModel, String nameOfTableWithLinearDiscriminantFunction, String nameOfTableWithMeanVectorsForClasses)
        {
            Object[] objects =TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(nameOfTableWithLinearDiscriminantFunction));
            double[][] temporaryMatrix = MatrixUtils.getSubmatrixRowWise((String[])objects[0], (double[][])objects[2], variableNamesInModel);
            double[] linearDiscriminantFunction = MatrixUtils.getColumn(temporaryMatrix, 0);
            objects = TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(nameOfTableWithMeanVectorsForClasses));
            double[][] meanVectorsForClasses = MatrixUtils.getSubmatrixRowWise((String[])objects[0], (double[][])objects[2], namesOfClasses);
            return new Object[]{linearDiscriminantFunction, meanVectorsForClasses};
        }
    }
    /**************************** FisherLDA : finish ****************************/

    /*********************** MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance : start ************/
    public static class MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance
    {
        public static final String NAME_OF_TABLE_WITH_INVERSE_COVARIANCE_MATRIX = "inverseCovarianceMatrix";

        /***
         * 
         * @param dataMatrix
         * @param indicesOfClasses
         * @param maxNumberOfIterations
         * @param eps
         * @return Object[] array : array[0] = double[][] meanVectorsForClasses, array[1] = double[][] inverseCovarianceMatrix;
         */
        public static Object[] createClassificationModel(double[][] dataMatrix, int[] indicesOfClasses, int maxNumberOfIterations, double eps)
        {
            Object[] objects = MultivariateSamples.getMeanVectorsAndCovarianceMatrix(dataMatrix, indicesOfClasses);
            double[][] inverseCovarianceMatrix = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod((double[][])objects[1], maxNumberOfIterations, eps);
            return new Object[]{objects[0], inverseCovarianceMatrix};
        }
        
        private static int predictIndexOfClass(double[] rowOfDataMatrix, double[][] meanVectorsForClasses, double[][] inverseCovarianceMatrix)
        {
            double[] scores = new double[meanVectorsForClasses.length];
            for( int i = 0; i < meanVectorsForClasses.length; i++ )
            {
                double[] vector = MatrixUtils.getSubtractionOfVectors(rowOfDataMatrix, meanVectorsForClasses[i]);
                scores[i] = MatrixUtils.getProductOfTransposedVectorAndSymmetricMatrixAndVector(inverseCovarianceMatrix, vector);
            }
            return (int)MatrixUtils.getMinimalValue(scores)[1];
        }

        public static int[] predictIndicesOfClasses(double[][] meanVectorsForClasses, double[][] inverseCovarianceMatrix, double[][] dataMatrix)
        {
            int[] predictedIndicesOfClasses = new int[dataMatrix.length];
            for( int i = 0; i < dataMatrix.length; i++ )
                predictedIndicesOfClasses[i] = predictIndexOfClass(dataMatrix[i], meanVectorsForClasses, inverseCovarianceMatrix);
            return predictedIndicesOfClasses;
        }
        
        public static void writeClassificationModel(double[][] meanVectorsForClasses, double[][] inverseCovarianceMatrix, String[] namesOfClasses, String[] variableNames, DataElementPath pathToOutputs, String nameOfTableWithInverseCovarianceMatrix, String nameOfTableWithMeanValuesForClasses)
        {
            double[][] squareMatrix = MatrixUtils.transformSymmetricMatrixToSquareMatrix(inverseCovarianceMatrix);
            TableUtils.writeDoubleTable(squareMatrix, variableNames, variableNames, pathToOutputs, nameOfTableWithInverseCovarianceMatrix);
            TableUtils.writeDoubleTable(meanVectorsForClasses, namesOfClasses, variableNames, pathToOutputs, nameOfTableWithMeanValuesForClasses);
        }

        /***
         * 
         * @param namesOfClasses
         * @param variableNames
         * @param pathToFolderWithSavedModel
         * @param nameOfTableWithMeanVectorsForClasses
         * @param nameOfTableWithInverseCovarianceMatrix
         * @return Object[] array : array[0] = double[][] meanVectorsForClasses, array[1] = double[][] inverseCovarianceMatrix;
         */
        public static Object[] readClassificationModel(String[] namesOfClasses, String[] variableNames, DataElementPath pathToFolderWithSavedModel, String nameOfTableWithMeanVectorsForClasses, String nameOfTableWithInverseCovarianceMatrix)
        {
            Object[] objects =TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(nameOfTableWithInverseCovarianceMatrix));
            double[][] temporaryMatrix = MatrixUtils.getSubmatrixRowWise((String[])objects[0], (double[][])objects[2], variableNames);
            double[][] inverseCovarianceMatrix = MatrixUtils.getLowerTriangularMatrix(temporaryMatrix);
            objects = TableUtils.readDoubleMatrixInTable(pathToFolderWithSavedModel.getChildPath(nameOfTableWithMeanVectorsForClasses));
            double[][] meanVectorsForClasses = MatrixUtils.getSubmatrixRowWise((String[])objects[0], (double[][])objects[2], namesOfClasses);
            return new Object[]{meanVectorsForClasses, inverseCovarianceMatrix};
        }
    }
    /**************** MaximumLikelihoodDiscriminationBasedOnMahalanobisDistance : finish ***************/
    
    /**************************** SVMcClassification : start ****************************/
    public static class SVMcClassification // SVMcClassification = Support Vector Machine C-classification
    {
        public static int[] createAndWriteClassificationModelUsingR(double[][] dataMatrix, String[] namesOfClassesForEachObject, String[] namesOfClasses, String scriptToCreateAndWriteModelForSVMcClassification, DataElementPath pathToOutputs, String classificationModelFileName, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            String[] inputObjectsNames = new String[]{"dataMatrix", "namesOfClassesForEachObject"};
            Object[] inputObjects = new Object[]{dataMatrix, namesOfClassesForEachObject};
            String[] outputObjectsNames = new String[]{"predictedNamesOfClassesForEachObject"};
            Object[] objects = Rutils.executeRscript(scriptToCreateAndWriteModelForSVMcClassification, inputObjectsNames, inputObjects, outputObjectsNames, pathToOutputs, classificationModelFileName, null, null, log, jobControl, from, to);
            return getIndicesOfClasses((String[])objects[0], namesOfClasses);
        }
    }
    /**************************** SVMcClassification : finish *************************/
    
    /**************************** EDDAclassification : start ****************************/
    public static class EDDAclassification // EDDAclassification = Eigenvalue Decomposition Discriminant Analysis based on Gaussian mixture modeling
    {
        public static int[] createAndWriteClassificationModelUsingR(double[][] dataMatrix, String[] namesOfClassesForEachObject, boolean areCovarianceMatricesEqual, String[] namesOfClasses, String scriptToCreateAndWriteModelForEDDAclassification, DataElementPath pathToOutputs, String classificationModelFileName, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            String modelType = areCovarianceMatricesEqual ? "EEE" : "VVV";
            String[] inputObjectsNames = new String[]{"dataMatrix", "namesOfClassesForEachObject", "modelType"};
            Object[] inputObjects = new Object[]{dataMatrix, namesOfClassesForEachObject, modelType};
            String[] outputObjectsNames = new String[]{"predictedNamesOfClassesForEachObject"};
            Object[] objects = Rutils.executeRscript(scriptToCreateAndWriteModelForEDDAclassification, inputObjectsNames, inputObjects, outputObjectsNames, pathToOutputs, classificationModelFileName, null, null, log, jobControl, from, to);
            return getIndicesOfClasses((String[])objects[0], namesOfClasses);
        }
    }
    /**************************** EDDAclassification : finish ****************************/
    
    private static Logger log = Logger.getLogger(Classification.class.getName());
}


