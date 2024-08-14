/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.JobControl;
import biouml.plugins.machinelearning.classification_models.ClassificationModel;
import biouml.plugins.machinelearning.regression_models.RegressionModel;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixChar;
import biouml.plugins.machinelearning.utils.MetaAnalysis.Homogeneity;
import biouml.plugins.machinelearning.utils.StatUtils.RandomUtils;
import biouml.plugins.machinelearning.utils.StatUtils.SimilaritiesAndDissimilarities;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSamples;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.Distance;

/**
 * @author yura
 *
 */
public class ModelUtils
{
    public static final String CREATE_AND_SAVE_MODE = "Create model and save it";
    public static final String LOAD_AND_PREDICT_MODE = "Load model and predict";
    public static final String CROSS_VALIDATION_MODE = "Cross-validation of model";
    public static final String VARIABLE_SELECTION_MODE = "Variable (feature) selection";
    
    public static final String STEPWISE_FORWARD_VARIABLE_ADDITION = "Stepwise forward variable addition";
    public static final String STEPWISE_BACKWARD_ELIMINATION = "Stepwise backward variable elimination: Under construction!";
    
    public static final String NAME_OF_TABLE_WITH_MEANS_IN_CLASSES = "Means_in_classes";
    
    public static final String INTERCEPT = "Intercept";
    
    public static final String PEARSON_CORRELATION_CRITERION  = "Maximization of Pearson correlation between response and predicted response";
    public static final String SPEARMAN_CORRELATION_CRITERION = "Maximization of Spearman correlation between response and predicted response";
    
    // For regression and classification  models.
    public static String[] getAvailableModes()
    {
        return new String[]{CREATE_AND_SAVE_MODE, LOAD_AND_PREDICT_MODE, CROSS_VALIDATION_MODE, VARIABLE_SELECTION_MODE};
    }
    
    public static String[] getAvailableMethodsForvariableSelection()
    {
        return new String[]{STEPWISE_FORWARD_VARIABLE_ADDITION, STEPWISE_BACKWARD_ELIMINATION};
    }
    
    // For regression analysis.
    public static Object[] splitDataSet(DataMatrix dataMatrix, double[] array, int firstSubsetSize, int seed)
    {
//        int[] indices = UtilsForArray.getStandardIndices(array.length);
//        RandomUtils.permuteVector(indices, seed);
        int[] indices = RandomUtils.getRandomIndices(array.length, seed);
        int[] indicesForFirstSubset = UtilsForArray.copySubarray(indices, 0, firstSubsetSize);
        return DataMatrix.splitRowWise(dataMatrix, array, null, indicesForFirstSubset);
    }
    
    
    public static Object[] selectFirstVariableInClassification(String[] response, DataMatrix dataMatrix)
    {
        double criterionValue = Double.MAX_VALUE;
        String[] variableNames = dataMatrix.getColumnNames();
        double[][] matrix = dataMatrix.getMatrix();
        int index = -1;
        for( int i = 0; i < variableNames.length; i++ )
        {
            UnivariateSamples us = new UnivariateSamples(response, MatrixUtils.getColumn(matrix, i));
            Homogeneity homogeneity = new Homogeneity(us);
            DataMatrix dm = homogeneity.performTestsOfHomogeneity(new String[]{Homogeneity.KRUSKAL_WALLIS_TEST});
            double pValue = dm.getMatrix()[0][1];
            if( ! Double.isNaN(pValue) && pValue < criterionValue )
            {
                criterionValue = pValue;
                index = i;
            }
        }
        return new Object[]{criterionValue, index}; 
    }

    // For classification analysis.
    public static DataMatrix stepwiseForwardVariableSelectionInClassification(String classificationType, String responseName, String[] response, DataMatrix dataMatrix, int numberOfSelectedVariables, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit, JobControl jobControl, int from, int to)
    {
        // 1. Select 1-st variable.
        Object[] objects = selectFirstVariableInClassification(response, dataMatrix);
        double criterionValue = (double)objects[0];
        int index = (int)objects[1];

        // 2. Initialize arrays.
        String[] variableNames = dataMatrix.getColumnNames();
        double[] criterionValues = new double[numberOfSelectedVariables];
        criterionValues[0] = criterionValue;
        String[] variableNamesSelected = new String[numberOfSelectedVariables];
        variableNamesSelected[0] = variableNames[index];
        boolean[] doSelected = UtilsForArray.getConstantArray(variableNames.length, false);
        doSelected[index] = true;
        DataMatrix dataMatrixSelected = new DataMatrix(dataMatrix.getRowNames(), variableNamesSelected[0], dataMatrix.getColumn(variableNamesSelected[0]));
        log.info("i = 0 variableNamesSelected[i] = " + variableNamesSelected[0] + " criterionValues[i] = " + criterionValues[0]);
        
        // 3. Select next variables.
        objects = UtilsForArray.getDistinctStringsAndIndices(response);
        String[] distinctClassNames = (String[])objects[0];
        int[] responseIndices = (int[])objects[1];
        int difference = to - from;
        for( int i = 1; i < numberOfSelectedVariables; i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + i * difference / numberOfSelectedVariables);
            dataMatrixSelected.addColumn("", UtilsForArray.getConstantArray(dataMatrixSelected.getSize(), Double.NaN), i);
            criterionValues[i] = 0.0;
            for( int j = 0; j < variableNames.length; j++ )
            {
                if( doSelected[j] ) continue;
                dataMatrixSelected.fillColumn(dataMatrix.getColumn(j), i);
                ClassificationModel classificationModel = ClassificationModel.createModel(classificationType, responseName, response, dataMatrixSelected, additionalInputParameters, false);
                if( classificationModel == null ) continue;
                int[] predictedIndices = classificationModel.predict(dataMatrixSelected);
                DataMatrix dm = ModelUtils.getTrueClassificationRates(responseIndices, predictedIndices, distinctClassNames);
                double tcr = dm.getMatrix()[dm.getSize() - 1][0];
                if( ! Double.isNaN(tcr) && tcr > criterionValues[i] )
                {
                    criterionValues[i] = tcr;
                    index = j;
                }
            }
            variableNamesSelected[i] = variableNames[index];
            doSelected[index] = true;
            dataMatrixSelected.fillColumn(dataMatrix.getColumn(index), i);
            log.info("i = " + i + " variableNamesSelected[i] = " + variableNamesSelected[i] + " criterionValues[i] = " + criterionValues[i]);
        }
        return new DataMatrix(variableNamesSelected, "p-value of Kruskal-Wallis test (if i = 0) or True Classification Rate (if i > 0)", criterionValues);
    }

    // For regression analysis.
    public static DataMatrix stepwiseForwardVariableSelectionInRegression(String regressionType, String responseName, double[] response, DataMatrix dataMatrix, int numberOfSelectedVariables, String variableSelectionCriterion, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit, JobControl jobControl, int from, int to)
    {
        // 1. Select 1-st variable.
        double[] criterionValues = new double[numberOfSelectedVariables];
        criterionValues[0] = -Double.MAX_VALUE;
        String[] variableNames = dataMatrix.getColumnNames(), variableNamesSelected = new String[numberOfSelectedVariables];
        double[][] matrix = dataMatrix.getMatrix();
        boolean[] doSelected = UtilsForArray.getConstantArray(variableNames.length, false);
        int index = -1;
        for( int i = 0; i < variableNames.length; i++ )
        {
            double x = calculateVariableSelectionCriterion(variableSelectionCriterion, response, MatrixUtils.getColumn(matrix, i));
            if( ! Double.isNaN(x) && x > criterionValues[0] )
            {
                criterionValues[0] = x;
                index = i;
            }
        }
        variableNamesSelected[0] = variableNames[index];
        doSelected[index] = true;
        DataMatrix dataMatrixSelected = new DataMatrix(dataMatrix.getRowNames(), variableNamesSelected[0], dataMatrix.getColumn(variableNamesSelected[0]));
        //log.info("i = 0 variableNamesSelected[i] = " + variableNamesSelected[0] + " criterionValues[i] = " + criterionValues[0]);
        
        // 2. Select next variables.
        int difference = to - from;
        for( int i = 1; i < numberOfSelectedVariables; i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + i * difference / numberOfSelectedVariables);
            dataMatrixSelected.addColumn("", UtilsForArray.getConstantArray(dataMatrixSelected.getSize(), Double.NaN), i);
            criterionValues[i] = -Double.MAX_VALUE;
            for( int j = 0; j < variableNames.length; j++ )
            {
                if( doSelected[j] ) continue;
                dataMatrixSelected.fillColumn(dataMatrix.getColumn(j), i);
                RegressionModel regressionModel = RegressionModel.createModel(regressionType, responseName, response, dataMatrixSelected, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
                if( regressionModel == null ) continue;
                double[] predictedResponse = regressionModel.predict(dataMatrixSelected);
                double x = calculateVariableSelectionCriterion(variableSelectionCriterion, response, predictedResponse);
                if( ! Double.isNaN(x) && x > criterionValues[i] )
                {
                    criterionValues[i] = x;
                    index = j;
                }
            }
            variableNamesSelected[i] = variableNames[index];
            doSelected[index] = true;
            dataMatrixSelected.fillColumn(dataMatrix.getColumn(index), i);
            //log.info("i = " + i + " variableNamesSelected[i] = " + variableNamesSelected[i] + " criterionValues[i] = " + criterionValues[i]);
        }
        return new DataMatrix(variableNamesSelected, variableSelectionCriterion, criterionValues);
    }
    
    // For regression analysis.
    public static Object[] outlierDetection(int numberOfOutlierDetectionSteps, double multiplierForSigma, String regressionType, String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, JobControl jobControl, int from, int to)
    {
        int difference = to - from, n = dataMatrix.getSize();
        if( n <= 2 ) return null;
        char[][] areOutlierMatrix = new char[numberOfOutlierDetectionSteps][];
        char[] areOutliers = null;
        DataMatrix dataMatrixNew = dataMatrix;
        double[] responseNew = response;
        double[][] summary = new double[numberOfOutlierDetectionSteps][];
        RegressionModel regressionModel = null;
        for( int i = 0; i < numberOfOutlierDetectionSteps; i++ )
        {
            // 1. Create dataMatrixNew and responseNew by using not outliers.
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / numberOfOutlierDetectionSteps);
            if( i > 0 )
            {
                List<Integer> list = new ArrayList<>();
                for( int j = 0; j < n; j++ )
                    if( areOutliers[j] == '-' )
                        list.add(j);
                Object[] objects = DataMatrix.splitRowWise(dataMatrix, response, null, UtilsGeneral.fromListIntegerToArray(list));
                dataMatrixNew = (DataMatrix)objects[0];
                responseNew = (double[])objects[2];
            }

            // 2. Calculate areOutliersNew, areOutlierMatrix[i] and summary[i].
            boolean doCalculateAccompaniedInformationWhenFit = i == numberOfOutlierDetectionSteps - 1 ? true : false;
            regressionModel = RegressionModel.createModel(regressionType, responseName, responseNew, dataMatrixNew, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
            char[] areOutliersNew = UtilsForArray.getConstantArray(n, '-');
            double[] predictedResponse = regressionModel.predict(dataMatrixNew);
            double pearsonCorr = SimilaritiesAndDissimilarities.getPearsonCorrelation(responseNew, predictedResponse);
            double sigma = Math.sqrt(ModelUtils.getResidualVariance(responseNew, predictedResponse, dataMatrixNew.getColumnNames().length));
            predictedResponse = regressionModel.predict(dataMatrix);
            for( int j = 0; j < n; j++ )
                if( Math.abs(response[j] - predictedResponse[j]) > multiplierForSigma * sigma )
                    areOutliersNew[j] = '+';
            areOutliers = areOutliersNew;
            areOutlierMatrix[i] = areOutliersNew;
            summary[i] = new double[]{(double)dataMatrixNew.getSize(), pearsonCorr};
        }
        // 3. Output results.
        String[] stepNames = new String[numberOfOutlierDetectionSteps];
        for( int i = 0; i < numberOfOutlierDetectionSteps; i++ )
            stepNames[i] = "Detection_step_" + String.valueOf(i);
        DataMatrix dm = new DataMatrix(stepNames, new String[]{"Data size without outliers", "Pearson correlation"}, summary);
        DataMatrixChar dmc = new DataMatrixChar(dataMatrix.getRowNames(), stepNames, MatrixUtils.getTransposedMatrix(areOutlierMatrix));
        List<Integer> indicesOfNonOutliers = new ArrayList<>(), indicesOfOutliers = new ArrayList<>();
        for( int i = 0; i < areOutliers.length; i++ )
            if( areOutliers[i] == '-' )
                indicesOfNonOutliers.add(i);
            else
                indicesOfOutliers.add(i);
        return new Object[]{dm, dmc, regressionModel, UtilsGeneral.fromListIntegerToArray(indicesOfNonOutliers), UtilsGeneral.fromListIntegerToArray(indicesOfOutliers)};
    }
    
    public static double calculateVariableSelectionCriterion(String variableSelectionCriterion, double[]response, double[] vector)
    {
        switch( variableSelectionCriterion )
        {
            case PEARSON_CORRELATION_CRITERION  : return SimilaritiesAndDissimilarities.getPearsonCorrelation(response, vector);
            case SPEARMAN_CORRELATION_CRITERION : return SimilaritiesAndDissimilarities.getSpearmanCorrelation(response, vector);
        }
        return Double.NaN;
    }

    // For classification  analysis.
    public static Object[] splitDataSet(DataMatrix dataMatrix, String[] array, int firstSubsetSize, int seed)
    {
//        int[] indices = UtilsForArray.getStandardIndices(array.length);
//        RandomUtils.permuteVector(indices, seed);
        int[] indices = RandomUtils.getRandomIndices(array.length, seed);
        int[] indicesForFirstSubset = UtilsForArray.copySubarray(indices, 0, firstSubsetSize);
        return DataMatrix.splitRowWise(dataMatrix, null, array, indicesForFirstSubset);
    }
    
    // For regression analysis.
    public static DataMatrix getSummaryOnModelAccuracy(double[] response, double[] predictedResponse, int numberOfVariables)
    {
        double pearsonCorr = SimilaritiesAndDissimilarities.getPearsonCorrelation(response, predictedResponse);
        double spearmanCorr = SimilaritiesAndDissimilarities.getSpearmanCorrelation(response, predictedResponse);
        double[] meanAndVariance = UnivariateSample.getMeanAndVariance(response);
        double mean = meanAndVariance[0], variance = meanAndVariance[1], residualVariance = getResidualVariance(response, predictedResponse, numberOfVariables);
        double varExplained = residualVariance >= variance ? 0.0 : 100.0 * (variance - residualVariance) / variance;
        return new DataMatrix(new String[]{"Pearson correlation between observations and predictions", "Spearman correlation between observations and predictions", "Mean of observations", "Variance of observations", "Explained variance (in %)", "Number of observations"}, "Value", new double[]{pearsonCorr, spearmanCorr, mean, variance, varExplained, (double)response.length});
    }
    
    // For regression analysis.
    public static double getResidualVariance(double[] response, double[] predictedResponse, int numberOfVariables)
    {
        return Distance.getEuclideanSquared(response, predictedResponse) / (double)(response.length - numberOfVariables);
    }
    
    // For classification models.
    public static void saveMeanVectorsInClasses(String[] distinctClassNames, String[]variableNames, double[][] meanVectorsInClasses, DataElementPath pathToOutputFolder)
    {
        DataMatrix dm = new DataMatrix(distinctClassNames, variableNames, meanVectorsInClasses);
        dm.writeDataMatrix(false, pathToOutputFolder, NAME_OF_TABLE_WITH_MEANS_IN_CLASSES, log);
    }
    
    // For classification  models.
    // old version
//    public static DataMatrix getTrueClassificationRates(int[] responseIndices, int[] predictedIndices, String[] distinctClassNames)
//    {
//        // 1. Calculation of frequencies.
//        int[] frequencies = new int[distinctClassNames.length], sizes = new int[distinctClassNames.length];
//        for( int i = 0; i < responseIndices.length; i++ )
//        {
//            sizes[responseIndices[i]]++;
//            if( responseIndices[i] == predictedIndices[i] )
//                frequencies[responseIndices[i]]++;
//        }
//        
//        // 2, Calculation of TCRs (True Classification Rates).
//        double[] tcrs = new double[distinctClassNames.length + 1];
//        for( int i = 0; i < frequencies.length; i++ )
//            tcrs[i] = (double)frequencies[i] / (double) sizes[i];
//        tcrs[distinctClassNames.length] = (double)PrimitiveOperations.getSum(frequencies) / (double)responseIndices.length;
//        
//        // 3. Output dataMatrix.
//        return new DataMatrix((String[])ArrayUtils.add(distinctClassNames, "Whole set"), "True classification rates", tcrs);
//    }
    
    // new version
    public static DataMatrix getTrueClassificationRates(int[] responseIndices, int[] predictedIndices, String[] distinctClassNames)
    {
        // 1. Calculation of frequencies.
        int[] frequencies = new int[distinctClassNames.length], sizes = new int[distinctClassNames.length];
        for( int i = 0; i < responseIndices.length; i++ )
        {
            sizes[responseIndices[i]]++;
            if( responseIndices[i] == predictedIndices[i] )
                frequencies[responseIndices[i]]++;
        }
        
        // 2. Calculation of TCRs (True Classification Rates).
        double[][] matrix = new double[distinctClassNames.length + 1][2];
        for( int i = 0; i < distinctClassNames.length; i++ )
        {
            matrix[i][0] = (double)frequencies[i] / (double) sizes[i]; // TCR for current class
            matrix[i][1] = (double) sizes[i]; // size of current class
        }
        matrix[distinctClassNames.length][0] = (double)PrimitiveOperations.getSum(frequencies) / (double)responseIndices.length;
        matrix[distinctClassNames.length][1] = (double)responseIndices.length;
        
        // 3. Output dataMatrix.
        return new DataMatrix((String[])ArrayUtils.add(distinctClassNames, "Whole set"), new String[]{"True classification rate", "Size of class"}, matrix);
    }
    
    // For regression and classification  models.
    public static DataMatrix getNumberOfRotations(int maxNumberOfRotations, int numberOfProcessedRotations)
    {
        return new DataMatrix(new String[]{"Maximal number of rotations", "Number of processed rotations"}, "Value", new double[]{(double)maxNumberOfRotations, (double)numberOfProcessedRotations});
    }

    protected static Logger log = Logger.getLogger(ModelUtils.class.getName());
}
