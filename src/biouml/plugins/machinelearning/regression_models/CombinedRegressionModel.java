/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.JobControl;
import biouml.plugins.machinelearning.classification_models.ClassificationModel;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.StatUtils.SimilaritiesAndDissimilarities;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;

/**
 * @author yura
 * CombinedRegressionModel is the combination of k regression models (with the same given regressionType) and classification model.
 * The input data (from dataMatrix) is divided into k classes. The regression model is constructed for each class.
 * Classes are identified by classification model.
 * 
 * Input:
 *          additionalInputParameters[0] = String typeOfVariableSelectionInClassification;
 *          additionalInputParameters[1] = int numberOfVariablesForClassification;
 *          additionalInputParameters[2] = String regressionsType; 
 *          additionalInputParameters[3] = Object[] additionalInputParametersForRegressions;
 *          additionalInputParameters[4] = String classificationType;
 *          additionalInputParameters[5] = Object[] additionalInputParametersForClassification;
 *          additionalInputParameters[6] = int numberOfRegressions;
 *          additionalInputParameters[7] = int numberOfVariablesForRegressions;
 *          additionalInputParameters[8] = int numberOfOutlierDetectionSteps;
 *          additionalInputParameters[9] = double multiplierForSigma;
 */

public class CombinedRegressionModel extends RegressionModel
{
    private static final String COMMON_PART_OF_REGRESSION_NAMES = "Regression_";
    public static final String NAME_OF_FOLDER_WITH_CLASSIFICATION_MODEL = "Classification_model";
    
    public static final String SELECT_VARIABLES_IN_CLASSIFICATION_BY_TRUE_CLASSIFICATION_RATE = "Selection of variables in classification by maximization of True Classification Rate";
    public static final String SELECT_VARIABLES_IN_CLASSIFICATION_BY_MAXIMIZATION_OF_CORRELATION = "Selection of variables in classification by maximization of correlation between response and predicted response";

    protected RegressionModel[] regressionModels; // dim(regressionModels) = k;
    protected String[] regressionNames; // dim(regressionModels) = k;
    protected ClassificationModel classificationModel;
    protected DataMatrix selectedVariablesInClassification;
    protected DataMatrix summaryOnSingleRegressionAccuracy;
    protected Map<String, DataMatrix> regressionNameAndSelectedVariables;

    
    public CombinedRegressionModel(String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(REGRESSION_10_COMB, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public CombinedRegressionModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    protected void fitModel(DataMatrix dataMatrix, Object[] additionalInputParameters)
    {
        implementIterativeSteps(dataMatrix, additionalInputParameters);
        fitModel(dataMatrix.getMatrix(), additionalInputParameters);
    }
    
    @Override
    public double[] predict(DataMatrix dataMatrix)
    {
        // 1. Calculate matricesForRegressions.
        double[] result = new double[dataMatrix.getSize()];
        double[][][] matricesForRegressions = new double[regressionModels.length][][];
        for( int i = 0; i < regressionModels.length; i++ )
        {
            String[] variableNamesForRegression = regressionModels[i].variableNames;
            DataMatrix dm = dataMatrix.getSubDataMatrixColumnWise(variableNamesForRegression);
            matricesForRegressions[i] = dm.getMatrix();
        }
        
        // 2. Calculate predictedIndices.
        String[] variableNamesForClassification = classificationModel.getVariableNames();
        DataMatrix dmForClassification = dataMatrix.getSubDataMatrixColumnWise(variableNamesForClassification);
        int[] predictedIndices = classificationModel.predict(dmForClassification);
        
        // 3. Predict response
        for( int i = 0; i < result.length; i++ )
        {
            double[][] matrix = new double[][]{matricesForRegressions[predictedIndices[i]][i]};
            result[i] = regressionModels[predictedIndices[i]].predict(matrix)[0];
        }
        return result;
    }
    
    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        // 1. Save classification model.
        DataElementPath pathToClassificationModel = pathToOutputFolder.getChildPath(NAME_OF_FOLDER_WITH_CLASSIFICATION_MODEL);
        DataCollectionUtils.createFoldersForPath(pathToClassificationModel.getChildPath(""));
        classificationModel.saveModel(pathToClassificationModel);
        selectedVariablesInClassification.writeDataMatrix(false, pathToClassificationModel, "selected_variables", log);

        // 2. Save regression models.
        for( int i = 0; i < regressionModels.length; i++ )
        {
            DataElementPath pathToRegressionModel = pathToOutputFolder.getChildPath(regressionNames[i]);
            DataCollectionUtils.createFoldersForPath(pathToRegressionModel.getChildPath(""));
            regressionModels[i].saveModel(pathToRegressionModel);
            DataMatrix dm = regressionNameAndSelectedVariables.get(regressionNames[i]);
            dm.writeDataMatrix(false, pathToRegressionModel, "selected_variables", log);
        }
        TableAndFileUtils.writeColumnToStringTable(variableNames, "variable_names", variableNames, pathToOutputFolder, NAME_OF_TABLE_WITH_VARIABLE_NAMES);
    }
    
    @Override    
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {
        summaryOnSingleRegressionAccuracy.writeDataMatrix(false, pathToOutputFolder, "Summary_on_model_accuracy_with_single_regression", log);
    }

    @Override    
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        // 1. Load classificationModel and regressionModels.
        classificationModel = ClassificationModel.loadModel(pathToInputFolder.getChildPath(NAME_OF_FOLDER_WITH_CLASSIFICATION_MODEL));
        regressionNames = classificationModel.distinctClassNames;
        regressionModels = new RegressionModel[regressionNames.length];
        for( int i = 0; i < regressionNames.length; i++ )
            regressionModels[i] = RegressionModel.loadModel(pathToInputFolder.getChildPath(regressionNames[i]));

        // 2. Create total variableNames.
//        Set<String> set = new HashSet<>();
//        String[] variables = classificationModel.getVariableNames(); 
//        for( String var : variables )
//            set.add(var);
//        for( RegressionModel rm : regressionModels )
//            for( String var : rm.getVariableNames() )
//                set.add(var);
//        variableNames = set.toArray(new String[0]);
        DataMatrix dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_VARIABLE_NAMES), null);
        variableNames = dataMatrix.getRowNames();
    }
    
    private void implementIterativeSteps(DataMatrix dataMatrix, Object[] additionalInputParameters)
    {
        // 1. Initiation of parameters.
        String responseNameForClassification = "Regressions";
        int numberOfVariablesForClassification = (int)additionalInputParameters[1], numberOfRegressions = (int)additionalInputParameters[6], numberOfVariablesForRegressions = (int)additionalInputParameters[7], numberOfOutlierDetectionSteps = (int)additionalInputParameters[8];
        String typeOfVariableSelectionInClassification = (String)additionalInputParameters[0];
        String regressionsType = (String)additionalInputParameters[2], classificationType = (String)additionalInputParameters[4];
        Object[] additionalInputParametersForRegressions = (Object[])additionalInputParameters[3], additionalInputParametersForClassification = (Object[])additionalInputParameters[5];
        double multiplierForSigma = (double)additionalInputParameters[9];
        Object[] objs = defineRegressions(numberOfRegressions, regressionsType, additionalInputParametersForRegressions, numberOfVariablesForRegressions, numberOfOutlierDetectionSteps, multiplierForSigma, responseName, response, dataMatrix, null, 0, 0);
        DataMatrixString namesOfVariablesForRegressionModels = (DataMatrixString)objs[1]; // rows of matrix are the distinct class names; i-th row contains names of variables for regression model in i-th class;
        String[] responseForClassification = (String[])objs[0];
        Object[] objects = UtilsForArray.getDistinctStringsAndIndices(responseForClassification);
        regressionNames = (String[])objects[0]; 
        
        // 2. Create regression models.
        createRegressionModels(responseForClassification, dataMatrix, regressionsType, namesOfVariablesForRegressionModels, additionalInputParametersForRegressions);
        
        // 3. Create classificationModel.
        selectedVariablesInClassification = variableSelectionInClassification(typeOfVariableSelectionInClassification, classificationType, responseNameForClassification, responseForClassification, dataMatrix, numberOfVariablesForClassification, additionalInputParametersForClassification);
        log.info("***** Summary on classification model\n" + selectedVariablesInClassification.toString());
        String[] variableNamesForClassification = selectedVariablesInClassification.getRowNames();
        DataMatrix dmForClassification = dataMatrix.getSubDataMatrixColumnWise(variableNamesForClassification);
        classificationModel = ClassificationModel.createModel(classificationType, responseNameForClassification, responseForClassification, dmForClassification, additionalInputParametersForClassification, doCalculateAccompaniedInformationWhenFit);
        
        // 4. Calculate variableNames.
        String[] names = classificationModel.getVariableNames();
        for( RegressionModel rm : regressionModels )
            names = (String[])ArrayUtils.addAll(names, rm.getVariableNames());
        variableNames = UtilsGeneral.getDistinctValues(names);
        
        if( doCalculateAccompaniedInformationWhenFit )
        {
            predictedResponse = predict(dataMatrix);
            createSummaryOnSingleRegressionAccuracy();
        }
    }
    
    // TODO: to check it!
    private void createSummaryOnSingleRegressionAccuracy()
    {
        String[] predictedClasses = classificationModel.transform(classificationModel.predictedIndices); //.dms.getColumn("Regressions_predicted");
        List<Double> listRes = new ArrayList<>(), listPredRes = new ArrayList<>();
        String regressionName = COMMON_PART_OF_REGRESSION_NAMES + String.valueOf(0);
        for( int i = 0; i < response.length; i++ )
        {
            if( ! predictedClasses[i].equals(regressionName) ) continue;
            listRes.add(response[i]);
            listPredRes.add(predictedResponse[i]);
        }
        double[] responseNew = UtilsGeneral.fromListToArray(listRes), predictedResponseNew = UtilsGeneral.fromListToArray(listPredRes);
        int index = ArrayUtils.indexOf(regressionNames, regressionName);
        summaryOnSingleRegressionAccuracy = ModelUtils.getSummaryOnModelAccuracy(responseNew, predictedResponseNew, regressionModels[index].getVariableNames().length);
    }

    private DataMatrix variableSelectionInClassification(String typeOfVariableSelectionInClassification, String classificationType, String responseNameForClassification, String[] responseForClassification, DataMatrix dataMatrix, int numberOfSelectedVariables, Object[] additionalInputParameters)
    {
        switch( typeOfVariableSelectionInClassification )
        {
            case SELECT_VARIABLES_IN_CLASSIFICATION_BY_TRUE_CLASSIFICATION_RATE    : return ModelUtils.stepwiseForwardVariableSelectionInClassification(classificationType, responseNameForClassification, responseForClassification, dataMatrix, numberOfSelectedVariables, additionalInputParameters, false, null, 0, 0);
            case SELECT_VARIABLES_IN_CLASSIFICATION_BY_MAXIMIZATION_OF_CORRELATION : return stepwiseForwardVariableSelectionInClassification(typeOfVariableSelectionInClassification, classificationType, responseNameForClassification, responseForClassification, dataMatrix, numberOfSelectedVariables, additionalInputParameters);
        }
        return null;
    }
    
    private DataMatrix stepwiseForwardVariableSelectionInClassification(String typeOfVariableSelectionInClassification, String classificationType, String responseNameForClassification, String[] responseForClassification, DataMatrix dataMatrix, int numberOfSelectedVariables, Object[] additionalInputParameters)
    {
        // 1. Select 1-st variable.
        Object[] objects = ModelUtils.selectFirstVariableInClassification(responseForClassification, dataMatrix);
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
        
        // 3. Calculate predictions by using  available regression models
        // dim(allPredictions) = n x k, where k number of regressions.
        double[][] allPredictions = predictByAvailableRegressionModels(dataMatrix);
        
        // 4. Select next variables.
        objects = UtilsForArray.getDistinctStringsAndIndices(responseForClassification);
        for( int i = 1; i < numberOfSelectedVariables; i++ )
        {
            dataMatrixSelected.addColumn("", UtilsForArray.getConstantArray(dataMatrixSelected.getSize(), Double.NaN), i);
            criterionValues[i] = - Double.MAX_VALUE;
            for( int j = 0; j < variableNames.length; j++ )
            {
                if( doSelected[j] ) continue;
                dataMatrixSelected.fillColumn(dataMatrix.getColumn(j), i);
                ClassificationModel classificationModel = ClassificationModel.createModel(classificationType, responseNameForClassification, responseForClassification, dataMatrixSelected, additionalInputParameters, false);
                if( classificationModel == null ) continue;
                int[] predictedIndices = classificationModel.predict(dataMatrixSelected);
                
                double[] predictedResponse = new double[allPredictions.length];
                for( int ii = 0; ii < predictedResponse.length; ii++ )
                    predictedResponse[ii] = allPredictions[ii][predictedIndices[ii]];
                double correlation = SimilaritiesAndDissimilarities.getPearsonCorrelation(response, predictedResponse);
                if( ! Double.isNaN(correlation) && correlation > criterionValues[i] )
                {
                    criterionValues[i] = correlation;
                    index = j;
                }
            }
            variableNamesSelected[i] = variableNames[index];
            doSelected[index] = true;
            dataMatrixSelected.fillColumn(dataMatrix.getColumn(index), i);
            log.info("i = " + i + " variableNamesSelected[i] = " + variableNamesSelected[i] + " criterionValues[i] = " + criterionValues[i]);
        }
        return new DataMatrix(variableNamesSelected, "p-value of Kruskal-Wallis test (if i = 0) or " + typeOfVariableSelectionInClassification + " (if i > 0)", criterionValues);
    }
    
    private Object[] defineRegressions(int numberOfRegressions, String regressionsType, Object[] additionalInputParametersForRegressions, int numberOfVariablesForRegressions, int numberOfOutlierDetectionSteps, double multiplierForSigma, String responseName, double[] response, DataMatrix dataMatrix, JobControl jobControl, int from, int to)
    {
        // 1. Initialize some parameters.
        String[] responseForClassification = UtilsForArray.getConstantArray(response.length, COMMON_PART_OF_REGRESSION_NAMES + String.valueOf(numberOfRegressions - 1));
        boolean doCalculateAccompaniedInformationWhenFitRegressions = false;
        String variableSelectionCriterion = ModelUtils.PEARSON_CORRELATION_CRITERION;
        int difference = to - from;
        double[] responseNew = response;
        DataMatrix dataMatrixNew = dataMatrix;
        String[][] namesOfVariablesForRegressions = new String[numberOfRegressions][];
        int[] globalIndicesForDataMatrixNew = UtilsForArray.getStandardIndices(response.length);
        regressionNameAndSelectedVariables = new HashMap<>();

        // 2. Stepwise construction of regressions by splitting response and dataMatrix.
        for( int i = 0; i < numberOfRegressions - 1; i++ )
        {
            // 2.1. Variable selection for current regression.
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / numberOfRegressions);
            DataMatrix dm = ModelUtils.stepwiseForwardVariableSelectionInRegression(regressionsType, responseName, responseNew, dataMatrixNew, numberOfVariablesForRegressions, variableSelectionCriterion, additionalInputParametersForRegressions, doCalculateAccompaniedInformationWhenFitRegressions, null, 0, 0);
            String className = COMMON_PART_OF_REGRESSION_NAMES + String.valueOf(i);
            regressionNameAndSelectedVariables.put(className, dm);
            namesOfVariablesForRegressions[i] = dm.getRowNames();
            log.info("***** Regression No = " + i + "\nSummary on variable selection in regression\n" + dm.toString());
            
            // 2.2. Outlier detection: calculate part responseForClassificationInitial and recalculate responseNew and dataMatrixNew.
            DataMatrix dataMatrixWithSelectedVariables = dataMatrixNew.getSubDataMatrixColumnWise(namesOfVariablesForRegressions[i]);
            Object[] objects = ModelUtils.outlierDetection(numberOfOutlierDetectionSteps, multiplierForSigma, regressionsType, responseName, responseNew, dataMatrixWithSelectedVariables, additionalInputParametersForRegressions, null, 0, 0);
            DataMatrix summary = (DataMatrix)objects[0];
            log.info("***** Regression No = " + i + "\nSummary on outlier detection\n" + summary.toString());
            int[] localIndicesOfNonOutliers = (int[])objects[3], localIndicesOfOutliers = (int[])objects[4];
            int[] globalIndices = transformLocalToGlobalIndices(globalIndicesForDataMatrixNew, localIndicesOfNonOutliers);
            for(int j = 0; j < globalIndices.length; j++ )
                responseForClassification[globalIndices[j]] = className;
            globalIndicesForDataMatrixNew = transformLocalToGlobalIndices(globalIndicesForDataMatrixNew, localIndicesOfOutliers);
            objects = DataMatrix.splitRowWise(dataMatrixNew, responseNew, null, localIndicesOfOutliers);
            dataMatrixNew = (DataMatrix)objects[0];
            responseNew = (double[])objects[2];
            
            // 2.3. Variable selection for last regression.
            if( i == numberOfRegressions - 2 )
            {
                dm = ModelUtils.stepwiseForwardVariableSelectionInRegression(regressionsType, responseName, responseNew, dataMatrixNew, numberOfVariablesForRegressions, variableSelectionCriterion, additionalInputParametersForRegressions, doCalculateAccompaniedInformationWhenFitRegressions, null, 0, 0);
                regressionNameAndSelectedVariables.put(COMMON_PART_OF_REGRESSION_NAMES + String.valueOf(i + 1), dm);
                namesOfVariablesForRegressions[i + 1] = dm.getRowNames();
                log.info("***** Regression No = " + Integer.toString(i + 1) + "\nSummary on variable selection in regression\n" + dm.toString());
            }
        }
        
        // 3. Output results.
        String[] rowNames = new String[numberOfRegressions];
        for( int i = 0; i < rowNames.length; i++ )
            rowNames[i] = COMMON_PART_OF_REGRESSION_NAMES + i;
        DataMatrixString namesOfVariablesForRegressionModels = new DataMatrixString(rowNames, null, namesOfVariablesForRegressions);
        return new Object[]{responseForClassification, namesOfVariablesForRegressionModels};
    }
    
    // dim(localIndices) <= dim(globalIndices)
    private static int[] transformLocalToGlobalIndices(int[] globalIndices, int[] localIndices)
    {
        int[] result = new int[localIndices.length];
        for(int i = 0; i < localIndices.length; i++ )
            result[i] = globalIndices[localIndices[i]];
        return result;
    }
    
    private void createRegressionModels(String[] responseForClassification, DataMatrix dataMatrix, String regressionType, DataMatrixString namesOfVariablesForRegressionModel, Object[] additionalInputParametersForRegressionModels)
    {
        regressionModels = new RegressionModel[regressionNames.length];
        for( int i = 0; i < regressionNames.length; i++ )
        {
            String[] variableNames = namesOfVariablesForRegressionModel.getRow(regressionNames[i]);
            DataMatrix dataMatrixForRegression = dataMatrix.getSubDataMatrixColumnWise(variableNames);
            int[] indices = UtilsForArray.getIndicesOfString(responseForClassification, regressionNames[i]);
            Object[] objects = DataMatrix.splitRowWise(dataMatrixForRegression, response, null, indices);
            dataMatrixForRegression = (DataMatrix)objects[0];
            double[] responseForRegression = (double[])objects[2];
            regressionModels[i] = RegressionModel.createModel(regressionType, responseName, responseForRegression, dataMatrixForRegression, additionalInputParametersForRegressionModels, doCalculateAccompaniedInformationWhenFit);
        }
    }
    
    // dim(result) = n x k, where k number of regressions.
    private double[][] predictByAvailableRegressionModels(DataMatrix dataMatrix)
    {
        double[][] result = new double[regressionModels.length][];
        for( int i = 0; i < regressionModels.length; i++ )
        {
            String[] variableNames = regressionModels[i].variableNames;
            DataMatrix dm = dataMatrix.getSubDataMatrixColumnWise(variableNames);
            result[i] = regressionModels[i].predict(dm.getMatrix());
        }
        return MatrixUtils.getTransposedMatrix(result);
    }
    
    // TODO: ???
    private int[] predictOptimalResponseForClassification(DataMatrix dataMatrix)
    {
        int[] predictedIndices = new int[response.length];
        double[][] differences = predictByAvailableRegressionModels(dataMatrix);
        for( int i = 0; i < response.length; i++ )
            for( int j = 0; j < differences[i].length; j++ )
                differences[i][j] = Math.abs(differences[i][j] - response[i]);
        for( int i = 0; i < response.length; i++ )
            predictedIndices[i] = (int)PrimitiveOperations.getMin(differences[i])[0];
        return predictedIndices;
    }
}
