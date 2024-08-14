/* $Id$ */

package biouml.plugins.machinelearning.classification_models;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;

/**
 * @author yura
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = String optimizationType;
 *          additionalInputParameters[1] = int maxNumberOfIterations 
 *          additionalInputParameters[2] = double admissibleMisclassificationRate;
 */
public class PerceptronModel extends LinearClassificationModel
{
    public static final String OPTIMIZATION_TYPE_STANDARD_GDM = "Standard gradient descent method";
    public static final String OPTIMIZATION_TYPE_STOCHASTIC_GDM = "Stochastic gradient descent method : under construction!!!";

    private int maxNumberOfIterations, numberOfProcessedIterations, optimalIterationIndex;
    private DataMatrix numberOfIterations;
    
    public PerceptronModel(String responseName, String[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(CLASSIFICATION_4_PER, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public PerceptronModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    public void calculateCoefficientsAndPredictedIndices(double[][] matrix, Object[] additionalInputParameters)
    {
        String optimizationType = (String)additionalInputParameters[0];
        maxNumberOfIterations = (int)additionalInputParameters[1];
        double admissibleMisclassificationRate = (double)additionalInputParameters[2];
        
        //TODO: To consider specified coefficientsInitialApproxomation.
        double[] coefficientsInitialApproxomation = null;
        coefficients = coefficientsInitialApproxomation != null ? coefficientsInitialApproxomation : new double[matrix[0].length];
        double missclassificationRateOptimal = 1.1;
        double[] coefficientsOptimal = null;
        for( numberOfProcessedIterations = 1; numberOfProcessedIterations <= maxNumberOfIterations; numberOfProcessedIterations++ )
        {
            // 1. Identification of coefficientsOptimal and missclassificationRateOptimal
            int[] predictedIndices = predict(matrix);
            int[] falsePredictionIndices = UtilsForArray.getIndicesOfUnequalElements(responseIndices, predictedIndices);
            double missClassificationRate = (double)falsePredictionIndices.length / (double)responseIndices.length;
            if( missClassificationRate <= admissibleMisclassificationRate )
            {
                optimalIterationIndex = numberOfProcessedIterations;
                break;
            }
            
            // TODO: temporary
            String s = "TCR = " + Double.toString(1.0 - missClassificationRate) + " coefficients =";
            for( double x : coefficients )
                s += " " + Double.toString(x);
            log.info(s);

            if( missClassificationRate < missclassificationRateOptimal )
            {
                coefficientsOptimal = ArrayUtils.clone(coefficients);
                missclassificationRateOptimal = missClassificationRate;
                optimalIterationIndex = numberOfProcessedIterations;
            }
            
            // 2. Re-calculation of coefficients
            switch( optimizationType )
            {
                case OPTIMIZATION_TYPE_STANDARD_GDM   : for( int i = 0; i < falsePredictionIndices.length; i++ )
                                                            for( int j = 0; j < coefficients.length; j++ )
                                                                if( responseIndices[falsePredictionIndices[i]] == 0 )
                                                                    coefficients[j] += matrix[falsePredictionIndices[i]][j];
                                                                else
                                                                    coefficients[j] -= matrix[falsePredictionIndices[i]][j];
                                                        break;
                // TODO: To implement this type of optimization.
                case OPTIMIZATION_TYPE_STOCHASTIC_GDM : coefficients = null;
                                                        break;
            }
        }
        numberOfProcessedIterations = Math.min(numberOfProcessedIterations, maxNumberOfIterations);
        coefficients = coefficientsOptimal;
        predictedIndices = predict(matrix);
    }
    
    @Override
    protected int predict(double[] rowOfMatrix)
    {
        return VectorOperations.getInnerProduct(coefficients, rowOfMatrix) > 0 ? 0 : 1;
    }
    
    @Override
    public void calculateAccompaniedSpecificInformation()
    {
        numberOfIterations = getNumberOfIterations();
    }

    @Override
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {
        numberOfIterations.writeDataMatrix(false, pathToOutputFolder, "Number_of_iterations", log); 
    }
    
    private DataMatrix getNumberOfIterations()
    {
        return new DataMatrix(new String[]{"Maximal number of iterations", "Number of processed iterations", "Optimal iteration number"}, "Value", new double[]{(double)maxNumberOfIterations, (double)numberOfProcessedIterations, (double)optimalIterationIndex});
    }
    
    /************************************ static methods ****************/
    public static String[] getAvailableOptimizationTypes()
    {
        return new String[]{OPTIMIZATION_TYPE_STANDARD_GDM, OPTIMIZATION_TYPE_STOCHASTIC_GDM};
    }
}
