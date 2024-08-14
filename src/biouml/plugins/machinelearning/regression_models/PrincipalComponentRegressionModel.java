/* $Id$ */

package biouml.plugins.machinelearning.regression_models;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Util;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.StudentDistribution;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.StatUtils.MultivariateSample;
import biouml.plugins.machinelearning.utils.StatUtils.PrincipalComponents;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;

/**
 * @author yura
 * Input:
 * Object[] additionalInputParameters :
 *          additionalInputParameters[0] = int maxNumberOfRotations;
 *          additionalInputParameters[1] = double epsForRotations; 
 *          additionalInputParameters[2] = int numberOfPrincipalComponents;
 *          additionalInputParameters[3] = String principalComponentSortingType;
 */
public class PrincipalComponentRegressionModel extends LinearRegressionModel
{
    public static final String TYPE_OF_PC_SORTING_EIGEN_VALUE = "Eigen value magnitude";
    public static final String TYPE_OF_PC_SORTING_EXPLAINED_VARIANCE = "Variance explained by principle component";
    public static final String TYPE_OF_PC_SORTING_PC_SIGNIFICANCE = "Principle component significance";

    private static final String NAME_OF_TABLE_WITH_RESPONSE_MEAN = "Response_mean";
    public static final String NAME_OF_COLUMN_WITH_VARIABLE_MEANS = "Variable_means";

    
    double responseMean;
    double[] variableMeans, pcCoefficients, eigenValues, statisticsForPCcoefficients, pValuesForPCcoefficients, varianceProportions;
    DataMatrix numberOfRotations, principalComponentCoefficients;
    
    public PrincipalComponentRegressionModel(String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
    {
        super(REGRESSION_3_PC, responseName, response, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformationWhenFit);
    }
    
    public PrincipalComponentRegressionModel(DataElementPath pathToInputFolder)
    {
        super(pathToInputFolder);
    }
    
    @Override
    public void calculateCoefficientsAndPredictedResponse(double[][] matrix, Object[] additionalInputParameters)
    {
        if( matrix.length != response.length || matrix.length <= matrix[0].length ) return;
        int maxNumberOfRotations = (int)additionalInputParameters[0];
        double epsForRotations = (double)additionalInputParameters[1];
        int numberOfPrincipalComponents = (int)additionalInputParameters[2];
        String principalComponentSortingType = (String)additionalInputParameters[3];
        
        // 1. Identification of principal components.
        PrincipalComponents pcs = new PrincipalComponents(matrix, maxNumberOfRotations, epsForRotations);
        if( doCalculateAccompaniedInformationWhenFit )
            numberOfRotations = ModelUtils.getNumberOfRotations(maxNumberOfRotations, pcs.getNumberOfProcessedRotations());
        double[][] principalComponents = pcs.getPrincipalComponents(), eigenVectors = pcs.getEigenVectors();
        eigenValues = pcs.getEigenValues();
        
        // 2. Calculation of responseMean and variableMeans.
        responseMean = PrimitiveOperations.getAverage(response);
        variableMeans = MultivariateSample.getMeanVector(matrix);

        // 3. Calculation of pcCoefficients and varianceProportions (in %).
        double[] responseTransformed = VectorOperations.getSum(response, - responseMean);
        pcCoefficients = MatrixUtils.getProductOfTransposedMatrixAndVector(principalComponents, responseTransformed);
        for( int i = 0; i < pcCoefficients.length; i++)
            pcCoefficients[i] /= eigenValues[i] * (double)response.length;
        double x = (double)response.length / VectorOperations.getInnerProduct(responseTransformed, responseTransformed);
        varianceProportions = new double[pcCoefficients.length];
        for( int i = 0; i < pcCoefficients.length; i++ )
            varianceProportions[i] = 100.0 * eigenValues[i] * pcCoefficients[i] * pcCoefficients[i] * x;

        // 4. Calculation of statisticsForPCcoefficients and pValuesForPCcoefficients.
        double[] prediction = MatrixUtils.getProductOfRectangularMatrixAndVector(principalComponents, pcCoefficients);
        double pcSigma = Math.sqrt(ModelUtils.getResidualVariance(responseTransformed, prediction, variableNames.length + 1));
        statisticsForPCcoefficients = new double[pcCoefficients.length]; 
        pValuesForPCcoefficients = new double[pcCoefficients.length];
        for( int i = 0; i < pcCoefficients.length; i++ )
        {
            statisticsForPCcoefficients[i] = pcCoefficients[i] * Math.sqrt((double)response.length * eigenValues[i]) / pcSigma;
            // pValuesForPCcoefficients[i] = 2.0 * DistributionFunctions.studentDistribution(Math.abs(statisticsForPCcoefficients[i]), response.length - pcCoefficients.length - 1, 80)[1];
            // pValuesForPCcoefficients[i] = 2.0 * (1.0 - StudentDistribution.getDistributionFunction(Math.abs(statisticsForPCcoefficients[i]), response.length - pcCoefficients.length - 1, 80));
            pValuesForPCcoefficients[i] = StudentDistribution.getPvalueForAbsStudent(Math.abs(statisticsForPCcoefficients[i]), response.length - pcCoefficients.length - 1, 80);
        }

        // 5. Calculation of coefficients and predictedResponse.
        int[] sortedPositions = sortPrincipalComponents(principalComponentSortingType);
        coefficients = transformPCcoefficientsToRegressionCoefficients(numberOfPrincipalComponents, eigenVectors, sortedPositions);
        predictedResponse = predict(matrix);
    }
    
    @Override
    public double[] predict(double[][] matrix)
    {
        double[] result = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
        {
            double[] vector = VectorOperations.getSubtractionOfVectors(matrix[i], variableMeans);
            result[i] = responseMean + VectorOperations.getInnerProduct(vector, coefficients);
        }
        return result;
    }
    
    @Override
    public void calculateAccompaniedSpecificInformation()
    {
        principalComponentCoefficients = createPcInfo();
    }
    
    @Override
    public void saveAccompaniedSpecificInformation(DataElementPath pathToOutputFolder)
    {
        numberOfRotations.writeDataMatrix(false, pathToOutputFolder, "Number_of_rotations", log);
        principalComponentCoefficients.writeDataMatrix(false, pathToOutputFolder, "Principal_component_coefficients", log);
    }
    
    // TODO: to sort principal components by sorting eigen-values !!!
    private DataMatrix createPcInfo()
    {
        String[] rowNames = new String[pcCoefficients.length];
        for( int i = 0; i < pcCoefficients.length; i++ )
            rowNames[i] = "Principal_component_" + Integer.toString(i + 1);
        double[][] matrix = new double[pcCoefficients.length][];
        for( int i = 0; i < pcCoefficients.length; i++ )
            matrix[i] = new double[]{pcCoefficients[i], eigenValues[i], varianceProportions[i], statisticsForPCcoefficients[i], pValuesForPCcoefficients[i]};
        return new DataMatrix(rowNames, new String[]{"Coefficient", "Eigen value", "Response variance explained by principal component (in %)",  "Statistic (Z-score)", "p-value"}, matrix);
    }
    
    @Override
    public void loadModelParticular(DataElementPath pathToInputFolder)
    {
        DataMatrix dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_COEFFICIENTS), null);
        variableNames = dataMatrix.getRowNames();
        coefficients = dataMatrix.getColumn(NAME_OF_COLUMN_WITH_COEFFICIENTS);
        variableMeans = dataMatrix.getColumn(NAME_OF_COLUMN_WITH_VARIABLE_MEANS);
        dataMatrix = new DataMatrix(pathToInputFolder.getChildPath(NAME_OF_TABLE_WITH_RESPONSE_MEAN), null);
        responseMean = dataMatrix.getMatrix()[0][0];
    }

    @Override
    public void saveModelParticular(DataElementPath pathToOutputFolder)
    {
        double[][] matrix = new double[variableNames.length][];
        for( int i = 0; i < variableNames.length; i++ )
            matrix[i] = new double[]{coefficients[i], variableMeans[i]};
        DataMatrix dm = new DataMatrix(variableNames, new String[]{NAME_OF_COLUMN_WITH_COEFFICIENTS, NAME_OF_COLUMN_WITH_VARIABLE_MEANS}, matrix);
        dm.writeDataMatrix(false, pathToOutputFolder, NAME_OF_TABLE_WITH_COEFFICIENTS, log);
        TableAndFileUtils.writeColumnToDoubleTable(new String[]{"response mean"}, "response mean", new double[]{responseMean}, pathToOutputFolder, NAME_OF_TABLE_WITH_RESPONSE_MEAN);
    }
    
    private int[] sortPrincipalComponents(String principalComponentSortingType)
    {
        switch( principalComponentSortingType )
        {
            case TYPE_OF_PC_SORTING_EIGEN_VALUE        : return Util.sortHeap(eigenValues.clone());
            case TYPE_OF_PC_SORTING_EXPLAINED_VARIANCE : return Util.sortHeap(varianceProportions.clone());
            case TYPE_OF_PC_SORTING_PC_SIGNIFICANCE    : double[] negativePvalues = new double[pValuesForPCcoefficients.length];
                                                         for( int i = 0; i < negativePvalues.length; i++ )
                                                             negativePvalues[i] = - pValuesForPCcoefficients[i];
                                                         return Util.sortHeap(negativePvalues);
            default                                    : return null;
        }
    }
    
    private double[] transformPCcoefficientsToRegressionCoefficients(int numberOfPrincipalComponents, double[][] eigenVectors, int[] sortedPositions)
    {
        double[] newPCcoefficients = pcCoefficients.clone();
        for( int i = 0; i < pcCoefficients.length - numberOfPrincipalComponents; i++ )
            newPCcoefficients[sortedPositions[i]] = 0.0;
        return MatrixUtils.getProductOfRectangularMatrixAndVector(eigenVectors, newPCcoefficients);
    }
    
    /************************* Static methods *************************/
    
    public static String[] getAvailableTypesOfPcSorting()
    {
        return new String[]{TYPE_OF_PC_SORTING_EIGEN_VALUE, TYPE_OF_PC_SORTING_EXPLAINED_VARIANCE, TYPE_OF_PC_SORTING_PC_SIGNIFICANCE};
    }
}
