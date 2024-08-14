/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.analysis.Util;
import ru.biosoft.graphics.chart.Chart;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.ChiSquaredDistribution;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.NormalDistribution;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample.DensityEstimation;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.MathUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.Distance;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;

/**
 * @author yura
 *
 */
public class StatUtils
{
    /****************** StatisticalTests : start ***************************/
    public static class StatisticalTests
    {
        public static class ChiSquaredIndependenceTestForTwoDimensionContingencyTable
        {
        	// from STA063();
        	// There is two dimensional binary sample {(xi, yi), i = 1,...,n};
        	// Then n01 := number of cases when (xi = false yi = true);  
        	private static Object[] calculateStatistics(int n00, int n01, int n10, int n11, int n)
        	{
        		double x = ((double)(n00 + n01)) * ((double)(n10 + n11)) * ((double)(n00 + n10)) * ((double)(n01 + n11));
        		if( x <= 0.0 ) return null;
        		x = ((double)n) / x;
        		double statistic = Math.abs(((double)n00) * ((double)n11) - ((double)n01) * ((double)n10));
        		double statisticAdjusted = statistic - 0.5 * ((double)n);
        		statistic = statistic * statistic * x;
        		statisticAdjusted = statisticAdjusted * statisticAdjusted * x;
        		return new Object[]{statistic, statisticAdjusted};
        	}
        	
        	private static Object[] performTest(int n00, int n01, int n10, int n11, int n)
        	{
        		Object[] objects = calculateStatistics(n00, n01, n10, n11, n);
        		double statistic = (double)objects[0], statisticAdjusted = (double)objects[1];
        		double pValue = 1.0 - ChiSquaredDistribution.getDistributionFunction(statistic, 1, 100);
        		double pValueAdjusted = 1.0 - ChiSquaredDistribution.getDistributionFunction(statisticAdjusted, 1, 100);
        		return new Object[]{statistic, pValue, statisticAdjusted, pValueAdjusted};
        	}
        	
        	// return contingencyTable[4] = {n00, n01, n10, n11}
        	private static int[] calculateContingencyTable(boolean[] sampleOne, boolean[] sampleSecond)
        	{
        		int n00 = 0, n01 = 0, n10 = 0, n11 = 0;
                for( int i = 0; i < sampleOne.length; i++ )
                {
                	if( sampleOne[i] == false && sampleSecond[i] == false )
                		n00++;
                	else if( sampleOne[i] == false && sampleSecond[i] == true )
                		n01++;
                	else if( sampleOne[i] == true && sampleSecond[i] == false )
                		n10++;
                	else if( sampleOne[i] == true && sampleSecond[i] == true )
                		n11++;
                }
        		return new int[]{n00, n01, n10, n11};
        	}
        	
        	// return : Object[] = {statistic, pValue, statisticAdjusted, pValueAdjusted}
        	public static Object[] performTest(boolean[] sampleOne, boolean[] sampleSecond)
        	{
        		int[] contingencyTable = calculateContingencyTable(sampleOne, sampleSecond);
        		return performTest(contingencyTable[0], contingencyTable[1], contingencyTable[2], contingencyTable[3], sampleOne.length);
        	}
        }
    }
    /****************** StatisticalTests : end *****************************/
    
    /****************** RandomUtils : start  ****************************/
    public static class RandomUtils
    {
    	public static int[] getRandomIndices(int dimensionOfIndices, int seed)
    	{
    		int[] indices = UtilsForArray.getStandardIndices(dimensionOfIndices);
            permuteVector(indices, seed);
            return indices;
    	}
        
        public static void permuteVector(int[] vector, int seed)
        {
            Random randomNumberGenerator = new Random(seed);
            permuteVector(vector, randomNumberGenerator);
        }
        
        public static void permuteVector(int[] vector, Random randomNumberGenerator)
        {
            for( int i = vector.length - 1; i > 0; i-- )
            {
                int j = randomNumberGenerator.nextInt(i + 1), tmp = vector[i];
                vector[i] = vector[j];
                vector[j] = tmp;
            }
        }
        
        public static void permuteVector(double[] vector, Random randomNumberGenerator)
        {
            for( int i = vector.length - 1; i > 0; i-- )
            {
                int j = randomNumberGenerator.nextInt(i + 1);
                double tmp = vector[i];
                vector[i] = vector[j];
                vector[j] = tmp;
            }
        }

        // old
//        public static int[] selectIndicesRandomly(int numberOfExistingIndices, int numberOfSelectedIndices, int seed)
//        {
//            int[] indices = UtilsForArray.getStandardIndices(numberOfExistingIndices);
//            permuteVector(indices, seed);
//            return UtilsForArray.copySubarray(indices, 0, numberOfSelectedIndices);
//        }
        
        // new
        public static int[] selectIndicesRandomly(int numberOfExistingIndices, int numberOfSelectedIndices, int seed)
        {
            int[] indices = getRandomIndices(numberOfExistingIndices, seed);
            return UtilsForArray.copySubarray(indices, 0, numberOfSelectedIndices);
        }

        public static double[] selectSubsampleRandomly(double[] sample, int numberOfSelectedIndices, int seed)
        {
        	double[] result = new double[numberOfSelectedIndices];
        	int[] randomArray = RandomUtils.selectIndicesRandomly(sample.length, numberOfSelectedIndices, seed);
        	for( int i = 0; i < numberOfSelectedIndices; i++ )
        		result[i] = sample[randomArray[i]];
        	return result;
        }
        
        public static double[] selectSubsampleRandomly(double[] sample, int numberOfSelectedIndices)
        {
        	return selectSubsampleRandomly(sample, numberOfSelectedIndices, 0);
        }
        
        public static double[][] permuteMatrixColumns(double[][] matrix, Random randomNumberGenerator)
        {
            double[][] result = new double[matrix.length][matrix[0].length];
            for( int j = 0; j < matrix[0].length; j++ )
            {
                double[] column = MatrixUtils.getColumn(matrix, j);
                permuteVector(column, randomNumberGenerator);
                MatrixUtils.fillColumn(result, column, j);
            }
            return result;
        }
    }
    /****************** RandomUtils : end  **************************************/
    
    /****************** PrincipalComponents : start  ****************************/
    public static class PrincipalComponents
    {
        private double[] variableMeans, eigenValues;
        private double[][] eigenVectors, principalComponents; // Principal components are the columns of this matrix.
        private int numberOfProcessedRotations;
        
        public PrincipalComponents(double[][] matrix, int maxNumberOfRotations, double epsForRotations)
        {
            variableMeans = MultivariateSample.getMeanVector(matrix);
            double[][] covarianceMatrix = MultivariateSample.getCovarianceMatrix(matrix, variableMeans);
            Object[] objects = MatrixUtils.getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(covarianceMatrix, maxNumberOfRotations, epsForRotations);
            numberOfProcessedRotations = (int)objects[0];
            eigenValues = (double[])objects[1];
            eigenVectors = (double[][])objects[2];
            principalComponents = MultivariateSample.transformationToZeroMeans(matrix, variableMeans);
            principalComponents = MatrixUtils.getProductOfRectangularMatrices(principalComponents, eigenVectors);
        }
        
        public double[][] getPrincipalComponents()
        {
            return principalComponents;
        }
        
        public double[] getEigenValues()
        {
            return eigenValues;
        }
        
        public double[][] getEigenVectors()
        {
            return eigenVectors;
        }
        
        public int getNumberOfProcessedRotations()
        {
            return numberOfProcessedRotations;
        }
    }
    /****************** PrincipalComponents : end  ****************************/

    /****************** MultivariateSample : start  ***************************/
    public static class MultivariateSample
    {
        public static double[] getMeanVector(double[][] matrix)
        {
            double[] vector = MatrixUtils.getSumsOfColumns(matrix);
            return VectorOperations.getProductOfVectorAndScalar(vector, 1.0 / (double)matrix.length);
        }

        public static double[] getMeanVectorWhenNanExist(double[][] matrix)
        {
            int[] sizes = new int[matrix[0].length];
            double[] sums = new double[matrix[0].length];
            for( int i = 0; i < matrix.length; i++ )
                for( int j = 0; j < sums.length; j++ )
                    if( ! Double.isNaN(matrix[i][j]) )
                    {
                        sizes[j]++;
                        sums[j] += matrix[i][j];
                    }
            for( int i = 0; i < sums.length; i++ )
                sums[i] = sizes[i] == 0 ? Double.NaN : sums[i] / (double)sizes[i];
            return sums;
        }
        
        public static double[][] getCovarianceMatrix(double[][] matrix, double[] meanVector)
        {
            double[][] mat = MatrixUtils.getProductXtrHX(matrix, meanVector);
            return MatrixUtils.getProductOfMatrixAndScalar(mat, 1.0 / matrix.length);
        }
        
        public static double[][] getCovarianceMatrix(double[][] matrix)
        {
            return getCovarianceMatrix(matrix, getMeanVector(matrix));
        }
        
        public static double[][] transformationToZeroMeans(double[][] matrix, double[] meanVector)
        {
            double[][] result = new double[matrix.length][];
            for( int i = 0; i < matrix.length; i++ )
                result[i] = VectorOperations.getSubtractionOfVectors(matrix[i], meanVector);
            return result;
        }
        
        public static Object[] getMeansAndSigmas(double[][] matrix)
        {
        	double[][] mat = MatrixUtils.getTransposedMatrix(matrix);
        	double[] means = new double[mat.length], sigmas = new double[mat.length];
            for( int i = 0; i < mat.length; i++ )
            {
            	double[] meanAndSigma = UnivariateSample.getMeanAndSigma(mat[i]);
            	means[i] = meanAndSigma[0];
            	sigmas[i] = meanAndSigma[1];
            }
            return new Object[]{means, sigmas};
        }
    }
    /****************** MultivariateSample : end  ****************************/
    
    /****************** MultivariateSamples : start  *************************/
    public static class MultivariateSamples
    {
        private int wholeSize;
        private String[] distinctSampleNames;
        private int[] sampleIndices;
        private double[][][] matrices; // matrices[i][][] = i-th multivariate sample;

        
        public MultivariateSamples(double[][] matrix, String[] samplesNames)
        {
            wholeSize = matrix.length; 
            Object[] objects = UtilsForArray.getDistinctStringsAndIndices(samplesNames);
            distinctSampleNames = (String[])objects[0];
            sampleIndices = (int[])objects[1];
            matrices = calculateSampleMatrices(getSampleMatricesAsMap(matrix));
        }

        // TODO: to test it!!!
//        public MultivariateSamples(double[][] matrix, int[] sampleIndices, String generalSubnameOfEverySample)
//        {
//            this(matrix, calculateSampleNames(generalSubnameOfEverySample, sampleIndices));
//        }
//        
//        private static String[] calculateSampleNames(String generalSubnameOfEverySample, int[] sampleIndices)
//        {
//        	String[] result = new String[sampleIndices.length];
//        	for( int i = 0; i < sampleIndices.length; i++ )
//        		result[i] = generalSubnameOfEverySample + "_" + Integer.toString(i);
//        	return result;
//        }

        // meanVectorsInSamples[i][] = meanVector for i-th sample
        public double[][] getMeanVectorsInSamples()
        {
//            double[][] meanVectorsInSamples = new double[matrices.length][];
//            for( int i = 0; i < matrices.length; i++ )
//                meanVectorsInSamples[i] = MultivariateSample.getMeanVector(matrices[i]);
//            return meanVectorsInSamples;
        	return getMeanVectorsInSamples(matrices);
        }
        
        public double[][] getWithinSspMatrix(double[][] meanVectorsInSamples)
        {
            double[][] withinSspMatrix = MatrixUtils.getProductXtrHX(matrices[0], meanVectorsInSamples[0]);
            for( int i = 1; i < matrices.length; i++ )
                withinSspMatrix = MatrixUtils.getSumOfMatrices(withinSspMatrix, MatrixUtils.getProductXtrHX(matrices[i], meanVectorsInSamples[i]));
            return withinSspMatrix;
        }
        
        public double[][] getWithinCovarianceMatrix(double[][] meanVectorsInSamples)
        {
            double[][] withinSspMatrix = getWithinSspMatrix(meanVectorsInSamples);
            return MatrixUtils.getProductOfMatrixAndScalar(withinSspMatrix, 1.0 / (double)(wholeSize - meanVectorsInSamples[0].length));
        }
        
        public Object[] getMeanVectorsAndCovarianceMatrix()
        {
            double[][] meanVectorsInSamples = getMeanVectorsInSamples(), covarianceMatrix = getWithinCovarianceMatrix(meanVectorsInSamples);
            return new Object[]{meanVectorsInSamples, covarianceMatrix};
        }
        
       private Map<Integer, List<double[]>> getSampleMatricesAsMap(double[][] matrix)
        {
            Map<Integer, List<double[]>> result = new HashMap<>();
            for( int i = 0; i < sampleIndices.length; i++ )
                result.computeIfAbsent(sampleIndices[i], key -> new ArrayList<>()).add(matrix[i]);
            return result;
        }

        // Output: double[i][][] = i-th multivariate sample;
        private double[][][] calculateSampleMatrices(Map<Integer, List<double[]>> map)
        {
            int max = (int)PrimitiveOperations.getMax(UtilsGeneral.fromSetIntegerToArray(map.keySet()))[1];
            double[][][] result = new double[max + 1][][];
            for( int i = 0; i <= max; i++ )
            {
                List<double[]> list = map.get(i);
                result[i] = list.toArray(new double[list.size()][]);
            }
            return result;
        }
        
        /***************** static methods ********************/
        public static Object[] getWithinAndBetweenAndTotalSspMatrices(double[][] matrix, String[] samplesNames)
        {
            MultivariateSamples mss = new MultivariateSamples(matrix, samplesNames);
            double[][] meanVectorsInSamples = mss.getMeanVectorsInSamples(), withinSspMatrix =  mss.getWithinSspMatrix(meanVectorsInSamples);
            double[] meanVectorTotal = MultivariateSample.getMeanVector(matrix);
            double[][] totalSspMatrix = MatrixUtils.getProductXtrHX(matrix, meanVectorTotal), betweenSspMatrix = MatrixUtils.getSubtractionOfMatrices(totalSspMatrix, withinSspMatrix);
            return new Object[]{withinSspMatrix, betweenSspMatrix, totalSspMatrix, meanVectorTotal, meanVectorsInSamples};
        }
        
        // meanVectorsInSamples[i][] = meanVector for i-th sample
        public static double[][] getMeanVectorsInSamples(double[][][] matrices)
        {
            double[][] meanVectorsInSamples = new double[matrices.length][];
            for( int i = 0; i < matrices.length; i++ )
                meanVectorsInSamples[i] = MultivariateSample.getMeanVector(matrices[i]);
            return meanVectorsInSamples;
        }
        
        public static Object[] getMeansAndSigmasVectorsInSamples(double[][][] matrices)
        {
        	double[][] means = new double[matrices.length][], sigmas = new double[matrices.length][];
            for( int i = 0; i < matrices.length; i++ )
            {
                Object[] objects = MultivariateSample.getMeansAndSigmas(matrices[i]);
            	means[i] = (double[]) objects[0];
            	sigmas[i] = (double[]) objects[1];
            }
            return new Object[]{means, means};
        }

    }
    /****************** MultivariateSamples : end  ***************************/

    /****************** UnivariateSample : start  ****************************/
    public static class UnivariateSample
    {
        public static double[] getMeanAndVariance(double[] sample)
        {
            double[] result = PrimitiveOperations.getSumOfSquaresCentered(sample);
            if( sample.length == 1 ) return new double[]{result[0], 0.0};
            result[1] /= (double)(sample.length - 1);
            return result; 
        }
        
        public static double[] getMeanAndSigma(double[] sample)
        {
            double[] result = getMeanAndVariance(sample);
            result[1] = Math.sqrt(result[1]);
            return result;
        }
        
        public static double getMedian(double[] sample)
        {
            double[] array = ArrayUtils.clone(sample);
            UtilsForArray.sortInAscendingOrder(array);
            if( MathUtils.isOdd(sample.length) ) return array[(sample.length - 1) / 2];
            int i = sample.length / 2;
            return 0.5 * (array[i] + array[i - 1]);
        }
        
        // Outlier is checked among min and max values of sample.
        // Return Object[] objects : objects[0] = sample (or sample without outlier if outlier exists),
        //                           objects[1] = meanAndSigma (or meanAndSigma without outlier if outlier exists).
        public static Object[] checkForOutlier(double[] sample)
        {
            Object[] objsForMin = PrimitiveOperations.getMin(sample), objectsForMin = checkForOutlier(sample, (int)objsForMin[0]);
            Object[] objsForMax = PrimitiveOperations.getMax(sample), objectsForMax = checkForOutlier(sample, (int)objsForMax[0]);
            if( ! (boolean)objectsForMin[2] && ! (boolean)objectsForMax[2]) return new Object[]{sample, getMeanAndSigma(sample)};
            if( (boolean)objectsForMin[2] && ! (boolean)objectsForMax[2]) return new Object[]{objectsForMin[0], objectsForMin[1]};
            if( ! (boolean)objectsForMin[2] && (boolean)objectsForMax[2]) return new Object[]{objectsForMax[0], objectsForMax[1]};
            double differenceForMin = Math.abs(sample[(int)objsForMin[0]] - ((double[])objectsForMin[1])[0]);
            double differenceForMax = Math.abs(sample[(int)objsForMax[0]] - ((double[])objectsForMax[1])[0]);
            return differenceForMin > differenceForMax ? new Object[]{objectsForMin[0], objectsForMin[1]} : new Object[]{objectsForMax[0], objectsForMax[1]}; 
        }
        
        public static Object[] checkForOutlier(double[] sample, int index)
        {
            double[] sampleNew = ArrayUtils.remove(sample, index), meanAndSigma = getMeanAndSigma(sampleNew);
            boolean isOutlier = ! (Math.abs(sample[index] - meanAndSigma[0]) <= 3.0 * meanAndSigma[1]);
            return new Object[]{sampleNew, meanAndSigma, isOutlier};
        }
        
        /****************** DensityEstimation : start *****************************************/
        public static class DensityEstimation
        {
            public static final String WINDOW_WIDTH_01 = "0.1 x Abs(mean value)";
            public static final String WINDOW_WIDTH_02 = "0.1 x (maximal value - minimal value)";
            public static final String WINDOW_WIDTH_03 = "0.2 x (maximal value - minimal value)";
            public static final String WINDOW_WIDTH_04 = "Given smoothing window width";
            private double[] xValues, yValues;
            
            public DensityEstimation(double[] sample, double window, Boolean doAddTwoZeroPoints)
            {
                xValues = UtilsGeneral.getDistinctValues(sample);
                yValues = UtilsForArray.getConstantArray(xValues.length, 0.0);

                // 1. Calculation of smoothed density.
                // TODO: To improve and optimize codes.
                double constant = window * (double)sample.length;
                for( int i = 0; i < xValues.length; i++ )
                {
                    for( double x : sample )
                        yValues[i] += getEpanechninkovKernel((xValues[i] - x), window);
                    yValues[i] /= constant;
                }
                
                // 2. Add two zero points.
                if( doAddTwoZeroPoints )    
                {
                    double[] minAndMax = PrimitiveOperations.getMinAndMax(xValues);
                    double x1 = minAndMax[0] > 0.0 ? 0.999 * minAndMax[0] : 1.001 * minAndMax[0];
                    double x2 = minAndMax[1] > 0.0 ? 1.001 * minAndMax[1] : 0.999 * minAndMax[1];
                    xValues = ArrayUtils.addAll(xValues, new double[]{x1,x2});
                    yValues = ArrayUtils.addAll(yValues, new double[]{0.0, 0.0});
                }
            }
            
            public double[][] getCurve()
            {
                return new double[][]{xValues, yValues};
            }
            
            // TODO: To replace and remove
            private static double getEpanechninkovKernel(double x, double window)
            {
                double y = x / window;
                return Math.abs(y) >= 1.0 ? 0.0 : 0.75 * (1.0 - y * y);
            }
            
            /*************static methods *******************/
            
            public static Chart createChartWithSmoothedDensities(double[][] samples, String[] sampleNames, String commonNameOfSamples, Boolean doAddTwoZeroPoints, double[] multipliers, String windowSelector, Double givenWindow)
            {
                double[][] xValuesForCurves = new double[samples.length][], yValuesForCurves = new double[samples.length][];
                for( int i = 0; i < samples.length; i++ )
                {
                    double window = getWindow(samples[i], windowSelector, givenWindow);
                    DensityEstimation de = new DensityEstimation(samples[i], window, doAddTwoZeroPoints);
                    double[][] curve = de.getCurve();
                    xValuesForCurves[i] = curve[0];
                    yValuesForCurves[i] = curve[1];
                }
                return createChartWithSmoothedDensities(xValuesForCurves, yValuesForCurves, sampleNames, commonNameOfSamples,  multipliers);
            }
            
            public static Chart createChartWithSmoothedDensities(double[][] xValuesForCurves, double[][] yValuesForCurves, String[] sampleNames, String commonNameOfSamples,  double[] multipliers)
            {
                for( int i = 0; i < xValuesForCurves.length; i++ )
                    if( multipliers != null && ! Double.isNaN(multipliers[i]) )
                        yValuesForCurves[i] = VectorOperations.getProductOfVectorAndScalar(yValuesForCurves[i], multipliers[i]);
                double commonMultiplier = getCommonMultiplier(yValuesForCurves);
                if(commonMultiplier > 1.1 )
                    recalculateDensities(commonMultiplier, xValuesForCurves, yValuesForCurves);
                String newCommonName = commonMultiplier > 1.1 ?  (int)(commonMultiplier + 0.1) + " x (" + commonNameOfSamples + ")" : commonNameOfSamples;
                return ChartUtils.createChart(xValuesForCurves, yValuesForCurves, sampleNames, null, null, null, null, newCommonName, "Probability", true);
            }
            
            public static Double getWindow(double[] sample, String windowSelector, Double givenWindow)
            {
                switch( windowSelector )
                {
                    case WINDOW_WIDTH_01 : return 0.1 * Math.abs(PrimitiveOperations.getAverage(sample));
                    case WINDOW_WIDTH_02 : double[] minAndMax = PrimitiveOperations.getMinAndMax(sample);
                                           return 0.1 * (minAndMax[1] - minAndMax[0]);
                    case WINDOW_WIDTH_03 : minAndMax = PrimitiveOperations.getMinAndMax(sample);
                                           return 0.2 * (minAndMax[1] - minAndMax[0]);
                    case WINDOW_WIDTH_04 : return givenWindow;
                    default              : return null;
                }
            }
            
            private static double getCommonMultiplier(double[][] yValuesForCurves)
            {
                double max = yValuesForCurves[0][0];
                for( int i = 0; i < yValuesForCurves.length; i++ )
                    for( int j = 0; j < yValuesForCurves[i].length; j++ )
                        max = Math.max(max, yValuesForCurves[i][j]);
                if( max < 1.0 ) return 1.0;
                return Math.pow(10.0, Math.ceil(Math.log10(max)));
            }
            
            public static void recalculateDensities(double commonMultiplier, double[][]xValuesForCurves, double[][] yValuesForCurves)
            {
                double y = 1.0 / commonMultiplier;
                for( int i = 0; i < xValuesForCurves.length; i++ )
                {
                    xValuesForCurves[i] = VectorOperations.getProductOfVectorAndScalar(xValuesForCurves[i], commonMultiplier);
                    yValuesForCurves[i] = VectorOperations.getProductOfVectorAndScalar(yValuesForCurves[i], y);
                }
            }
        }
        /****************** DensityEstimation : end *******************************/
    }
    /****************** UnivariateSample : end ************************************/
    
    /****************** UnivariateSamples : start *********************************/
    public static class UnivariateSamples
    {
        private double [][] samples; // samples[i] = i-th sample; dim(sample[i]) != dim(sample[j])
        private String[] sampleNames; // sampleNames[i] = i-th sample name; samples.length = sampleNames.length; 
        
        public UnivariateSamples(String[] sampleNames, double [][] samples)
        {
            this.sampleNames = sampleNames;
            this.samples = samples;
        }
        
        // dim(samplesNames) = dim(samplesValues)
        public UnivariateSamples(String[] samplesNames, double[] samplesValues)
        {
            Object[] objects = UtilsForArray.splitIntoSubarrays(samplesNames, samplesValues);
            this.sampleNames = (String[])objects[0];
            this.samples = (double[][])objects[1];
        }
        
        public String[] getSampleNames()
        {
            return sampleNames;
        }
        
        public UnivariateSamples getPairOfSamples(int indexOfFirstSample, int indexOfSecondSample)
        {
            return new UnivariateSamples(new String[]{sampleNames[indexOfFirstSample], sampleNames[indexOfSecondSample]}, new double [][]{samples[indexOfFirstSample], samples[indexOfSecondSample]});
        }

        public DataMatrix getSimpleCharacteristicsOfSamples()
        {
            double[][] matrix = new double[samples.length][];
            for( int i = 0; i < samples.length; i++ )
            {
                double[] meanAndSigma = UnivariateSample.getMeanAndSigma(samples[i]), minAndMax = PrimitiveOperations.getMinAndMax(samples[i]);
                matrix[i] = new double[]{samples[i].length, meanAndSigma[0], meanAndSigma[1], minAndMax[0], minAndMax[1]};
            }
            return new DataMatrix(sampleNames, new String[]{"Size", "Mean", "Sigma", "Minimum", "Maximum"}, matrix);
        }
        
        public Chart createChartWithSmoothedDensities(String commonNameOfSamples, Boolean doAddTwoZeroPoints, String windowSelector, Double givenWindow)
        {
            return DensityEstimation.createChartWithSmoothedDensities(samples, sampleNames, commonNameOfSamples, doAddTwoZeroPoints, null, windowSelector, givenWindow);
        }
        
        public int getNumberOfSamples()
        {
            return samples.length;
        }
        
        public double getWithinSumOfSquares(double[] means)
        {
            double result= 0.0;
            for(int i = 0; i < samples.length; i++ )
                for(int j = 0; j < samples[i].length; j++ )
                {
                    double y = samples[i][j] - means[i];
                    result += y * y;
                }
            return result;
        }

        public double getBetweenSumOfSquares(double totalMean, double[] means)
        {
            double result= 0.0;
            for(int i = 0; i < samples.length; i++ )
            {
                double x = means[i] - totalMean;
                result += (double)samples[i].length * x * x;
            }
            return result;
        }

        public double getTotalMean(int totalSize)
        {
            double result = 0.0;
            for(int i = 0; i < samples.length; i++ )
                result += PrimitiveOperations.getSum(samples[i]);
            return result / (double)totalSize;
        }
        
        public int getTotalSize()
        {
            int result = 0;
            for(int i = 0; i < samples.length; i++ )
                result += samples[i].length;
            return result;
        }
        
        public int getSizeOfGivenSample(int sampleIndex)
        {
            return samples[sampleIndex].length;
        }

        public double[] getMeans()
        {
            double[] result = new double[samples.length];
            for(int i = 0; i < samples.length; i++ )
                result[i] = PrimitiveOperations.getAverage(samples[i]);
            return result;
        }
        
        public double[] getCombinedSample()
        {
            return MatrixUtils.concatinateRows(samples);
        }
        
        public Object[] getMeansAndVariances()
        {
            double[] means = new double[samples.length], variances = new double[samples.length];
            for(int i = 0; i < samples.length; i++ )
            {
                double[] meanAndVariance = UnivariateSample.getMeanAndVariance(samples[i]);
                means[i] = meanAndVariance[0];
                variances[i] = meanAndVariance[1];
            }
            return new Object[]{means, variances};
        }
    }
    /****************** UnivariateSamples : end ***********************************/

    /****************** Distributions : start *************************************/
    public static class Distributions
    {
        /****************** ChiSquaredDistribution : start ************************/
        public static class ChiSquaredDistribution
        {
            public static double getDistributionFunction(double x, int degrees, int niter)
            {
                return degrees == 1 ? 2.0 * NormalDistribution.getDistributionFunction(Math.sqrt(x)) - 1.0 : GammaDistribution.getDistributionFunction(0.5 * x, degrees * 0.5, niter);
            }
        }
        /****************** ChiSquaredDistribution : end **************************/
        
        /****************** GammaDistribution : start *****************************/
        // GammaDistribution is the incomplete gamma function. x > 0.0; it is from MAT074.
        public static class GammaDistribution
        {
            public static double getDistributionFunction(double x, double parameter, int niter)
            {
                if( x <= 0.0 ) return Double.NaN;
                double y = 0.0, z = parameter + 1.0, zz = Math.log(x);
                if( x < z )
                {
                    for( int j = 0; j < niter; j++ )
                    {
                        int jj = j * 2;
                        double zzz = z + (double)jj, z1 = Math.exp((double)jj * zz - MathUtils.gammaFunctionLn(zzz));
                        y += z1 * (1.0 + x / zzz);
                    }
                    y *= Math.exp(parameter * zz - x);
                }
                else
                {
                    double zzz = x + 1.0 - parameter;
                    for( int j = niter; j >= 1; j-- )
                    {
                        y = zzz + (double)(j * 2) - y;
                        if( y == 0.0 ) return Double.NaN;
                        y = (double)j * ((double)j - parameter) / y;
                    }
                    y = zzz - y;
                    if( y == 0.0 ) return Double.NaN;
                    y = 1.0 / y;
                    y *= Math.exp(parameter * zz - x - MathUtils.gammaFunctionLn(parameter));
                    y = 1.0 - y;
                }
                return Util.restrict(0.0, 1.0, y);
            }
        }
        /****************** GammaDistribution : end *******************************/
        
        /****************** NormalDistribution : start ****************************/
        public static class NormalDistribution
        {
            public static double getDensity(double x, double mean, double sigma)
            {
                double result = (x - mean) / sigma;
                return Math.exp(- result * result / 2.0) / (sigma * MathUtils.SQRT_OF_2PI);
            }
            
            // TODO: To test it.
            // It is standard normal distribution function.
            public static double getDistributionFunction(double x)
            {
                double p, pp; // p = F(x) is probability, where F is distribution function; pp = 1.0 - p;
                if( x == 0.0 ) return 0.5;
                double y = Math.abs(x) / Math.sqrt(2.0), yy = y * y;
                if( y <= 0.0 ) return 0.5;
                if( y >= 1.0 )
                {
                    p = 61.41751803655557 + 86.59183795230987 * y + 59.59357876640772 * yy;
                    pp=61.41751803908473 + 155.8940854441156 * y + 174.0837071857067 * yy;
                    double  yyy = y * yy;
                    p += 23.605679444219112 * yyy;
                    pp += 110.345448210573 * yyy;
                    y = yy * yy;
                    p += 5.3508823344203262 * y;
                    pp += 42.34202517388795 * y;
                    y = yy * yyy;
                    p += 0.5641881275911299 * y;
                    pp += 9.484083981245681 * y + yyy * yyy;
                }
                else
                {
                    double yyy = y * yy, y1 = yy * yyy;
                    p = 0.5641881275911299 * y1;
                    pp = yyy * yyy + 9.484083981245681 * y1;
                    y1 = yy * yy;
                    p += 5.3508823344203262 * y1 + 23.605679444219112 * yyy + 59.59357876640772 * yy + 86.59183795230987 * y + 61.41751803655557;
                    pp += 42.34202517388795 * y1 + 110.345448210573 * yyy + 174.0837071857067 * yy + 155.8940854441156 * y + 61.41751803908473;
                }
                p /= pp;
                p *= 0.5 * Math.exp(-yy);
                p = Util.restrict(0.0, 1.0, p);
                
                return x < 0.0 ? p : 1.0 - p;
            }
            
            /***
             * Calculation of quantile of standard normal distribution
             * quantile = F*(p), where p is given probability,
             * F*() is inverse function of F and F() is distribution function of standard normal distribution
             * @param p
             * @param eps
             * @param maxNumberOfIterations
             * @return quantile
             */
            public static double getStandartNormalQuantile(double p, double eps, int maxNumberOfIterations)
            {
                double quantile = 0.0, x =  p > 0.5 ? p : 1.0 - p;
                if( p == 0.5 ) return quantile;
                x = 2.0 * x  - 1.0;
                if( x <= 0.0 ) return quantile;
                if( x >= 1.0 ) return Double.MAX_VALUE;
                if( x >= 0.36 )
                {
                    double xx = 1.0 / Math.sqrt(-Math.log(1.0 - x)), xxx = xx * xx, x1 = xx * xxx, x2 = xxx * xxx, x3 = xxx + 0.268419889732864 * xx + 0.0111247992539409;
                    quantile = -0.02055509764649 *x2 * x2 + 0.151646346343608 * x1 * x2 - 0.460597144409907 * x1 * x1 + 0.715981570361426 * x1 * xxx - 0.446867040619259 * xxx * xxx - 0.377458582257143 * x1 + 0.973134220625464 * xxx + 0.268334204826738 * xx + 0.0111252183495389;
                    quantile /= (x3 * xx);
                }
                if ( x <= 0.9375 )
                {
                    double xx = 1.0 + x;
                    for ( int i = 0;  i < maxNumberOfIterations; i++ )
                    {
                        double xxx = getDistributionFunction(quantile * MathUtils.SQRT_OF_2);
                        xxx = 0.886226925452758013649083741671 * (2.0 * xxx - xx);
                        xxx *= Math.exp(quantile * quantile) * (1.0 - xxx);
                        if( Math.abs(xxx) <= eps ) break;
                        quantile -= xxx;
                    }
                }
                quantile *= MathUtils.SQRT_OF_2;
                if( p < 0.5 )
                    quantile = - quantile;
                return quantile;
            }
        }
        /****************** NormalDistribution : end ******************************/
        
        /****************** FisherDistribution : start ****************************/
        public static class FisherDistribution
        {
            public static double getDistributionFunction(double x, double degrees1, double degrees2, int niter)
            {
                double z = degrees1 * x;
                return BetaDistribution.getDistributionFunction(z / (z + degrees2), 0.5 * degrees1, 0.5 * degrees2, niter);
            }
        }
        /****************** FisherDistribution : end ******************************/
        
        /****************** StudentDistribution : start ***************************/
        public static class StudentDistribution
        {
            public static double getDistributionFunction(double x, double degrees, int niter)
            {
                double z = x * x;
                double prob = BetaDistribution.getDistributionFunction(z / (z + degrees), 0.5, 0.5 * degrees, niter);
                prob = (1.0 + prob) / 2.0;
                return x > 0.0 ? prob : 1.0 - prob;
            }
            
            // x >= 0.0
            public static double getPvalueForAbsStudent(double x, double degrees, int niter)
            {
                double z = x * x;
                return BetaDistribution.getDistributionFunctionAndPvalue(z / (z + degrees), 0.5, 0.5 * degrees, niter)[1];
            }
        }
        /****************** StudentDistribution : end **********************************/
     
        /****************** BinomialDistribution : start *******************************/
        public static class BinomialDistribution
        {
            // TODO: To test
            public static double getDistributionFunction(double x, double probability, int n)
            {
                return BetaDistribution.getDistributionFunction(1.0 - probability, (double)n - x, x + 1.0, 80);
            }
        }
        /****************** BinomialDistribution : end *******************************/
        
        /****************** BetaDistribution : start *********************************/
        public static class BetaDistribution
        {
            public static double[] getDistributionFunctionAndPvalue(double x, double a, double b, int niter)
            {
                double z = 1.0 - x, beta = 0.0;
                if( x <= 0.0 || z >= 1.0 ) return new double[]{0.0, 1.0};
                if( x >= 1.0 || z <= 0.0 ) return new double[]{1.0, 0.0};
                double aaa, aaa1, aaa2, aaa3, aaa4, zz, zzz, z1, z2, z3, z4, z5, z6, z7;
                boolean ind = false;
                if( x <= z )
                {
                    aaa = x;
                    aaa1 = 1.0 - x;
                    aaa2 = a;
                    aaa3 = b;
                }
                else
                {
                    ind = true;
                    aaa = z;
                    aaa1 = x;
                    aaa2 = b;
                    aaa3 = a;
                }
                zz = aaa2 + 1.0;
                zzz = aaa2 + aaa3;
                if( zz <= (zzz - 1.0) * Math.exp(1.2 * Math.log(aaa)) )
                {
                    aaa4 = aaa;
                    aaa = aaa1;
                    aaa1 = aaa4;
                    aaa4 = aaa2;
                    aaa2 = aaa3;
                    aaa3 = aaa4;
                    zz = aaa2 + 1.0;
                    ind = ! ind;
                }
                z1 = aaa2 < 1.0 ? zz : aaa2;
                beta = 1.0;
                z2 = aaa3 - 1.0;
                z3 = aaa / aaa1;
                z4 = z1 - 1.0;
                z5 = z1 - 2.0;
                z6 = z1 + z2;
                for( int jj = niter * 2, j = niter; j > 0L; j--, jj -= 2L )
                {
                    z7 = z4 + jj;
                    if( beta == 0.0 ) return new double[]{0.0, 1.0};
                    beta = 1.0 + z3 * ((j * (z6 + j)) / ((z1 + jj) * z7)) / beta;
                    if( beta == 0.0 ) return new double[]{0.0, 1.0};
                    beta = 1.0 - z3 * (((aaa3 - j) * (z4 + j)) / ((z5 + jj) * z7)) / beta;
                }
                if( beta == 0.0 ) return new double[]{0.0, 1.0};
                beta = 1d / beta;
                if( z1 != aaa2 )
                {
                    beta *= aaa * zzz / zz;
                    beta += aaa1;
                }
                zz = MathUtils.betaFunctionLn(aaa2, aaa3);
                zzz = aaa2 * Math.log(aaa) + z2 * Math.log(aaa1) - zz;
                beta *= Math.exp(zzz) / aaa2;
                if( beta < 0 )
                {
                    beta = - beta;
                    ind = ! ind;
                }
                double invbeta = 1.0 - beta;
                return ind ? new double[]{invbeta, beta} : new double[]{beta, invbeta};
            }

            public static double getDistributionFunction(double x, double a, double b, int niter)
            {
                return getDistributionFunctionAndPvalue(x, a, b, niter)[0];
            }
        }
        /****************** BetaDistribution : end  ***********************************/
    }
    /****************** Distributions : end  ******************************************/

    /****************** SimilaritiesAndDissimilarities : begin  ***********************/
    public static class SimilaritiesAndDissimilarities
    {
        public static class SimilaritiesForBinaryData
        {
        	public static String[] MEASURE_NAMES_FOR_ABSENT_N00 = {"Jaccard measure", "2 x Dice measure", "Sorensen measure", "Kulzinsky measure", "Sokal-Sneath-a measure", "Sokal-Sneath-b measure", "Ochiai measure", "Gower-Leegendre measure"};

        	public static double[] getSimilarityMeasures(String[] measureNames, int n00, int n01, int n10, int n11)
        	{
        		double[] result = new double[measureNames.length];
                for( int i = 0; i < measureNames.length; i++ )
                	switch(measureNames[i])
                	{
                		case "Jaccard measure"		   : result[i] = getJaccardMeasure(n01, n10, n11); break;
                		case "2 x Dice measure"		   : result[i] = getDiceMeasureNormed(n01, n10, n11); break;
                		case "Sorensen measure" 	   : result[i] = getSorensenMeasure(n01, n10, n11); break;
                		case "Kulzinsky measure"	   : result[i] = getKulzinskyMeasure(n01, n10, n11); break;
                		case "Sokal-Sneath-a measure"  : result[i] = getSokalSneathMeasureA(n01, n10, n11); break;
                		case "Sokal-Sneath-b measure"  : result[i] = getSokalSneathMeasureB(n01, n10, n11); break;
                		case "Ochiai measure"		   : result[i] = getOchiaiMeasure(n01, n10, n11); break;
                		case "Gower-Leegendre measure" : result[i] = getGowerLegendreMeasure(n01, n10, n11); break;
                	}
        		return result;
        	}
        	
        	// Object[0] = String[] measuresNames, Object[1] = double[] measures. 
        	public static Object[] getAllSimilarityMeasuresWhenN00IsAbsent(int n01, int n10, int n11)
        	{
        		double[] measures = getSimilarityMeasures(MEASURE_NAMES_FOR_ABSENT_N00, -1, n01, n10, n11);
        		return new Object[]{MEASURE_NAMES_FOR_ABSENT_N00, measures};
        	}
        	
        	// range = [0, 1]
        	private static double getJaccardMeasure(int n01, int n10, int n11)
        	{
        		int k = n01 + n10 + n11;
        		return k == 0 ? Double.NaN : (double)n11 / (double)k;
        	}
        	
        	// range = [0, 1/2]
        	private static double getDiceMeasure(int n01, int n10, int n11)
        	{
        		int k = n01 + n10 + 2 * n11;
        		return k == 0 ? Double.NaN : (double)n11 / (double)k;
        	}

        	// range = [0, 1]
        	private static double getDiceMeasureNormed(int n01, int n10, int n11)
        	{
        		return 2.0 * getDiceMeasure(n01, n10, n11);
        	}
        	
        	// range = [0, 1]
        	private static double getSorensenMeasure(int n01, int n10, int n11)
        	{
        		int k = n01 + n10 + 2 * n11;
        		return k == 0 ? Double.NaN : 2.0 * (double)n11 / (double)k;
        	}

        	// range = [0, infinity]
        	private static double getKulzinskyMeasure(int n01, int n10, int n11)
        	{
        		int k = n01 + n10;
        		return k == 0 ? Double.NaN : (double)n11 / (double)k;
        	}

        	// range = [0, 1]
        	private static double getSokalSneathMeasureA(int n01, int n10, int n11)
        	{
        		int k = n11 + n01, kk = n11 + n10;
        		return k == 0 || kk == 0 ? Double.NaN : 0.5 * (double)n11 * ((1.0 / (double)k + 1.0 / (double)kk));
        	}
        	
        	private static double getSokalSneathMeasureB(int n01, int n10, int n11)
        	{
        		int k = n11 + 2 * (n01 + n10);
        		return k == 0 ? Double.NaN : (double)n11 / (double)k;
        	}
        	
        	// range = [0, 1]
        	private static double getOchiaiMeasure(int n01, int n10, int n11)
        	{
        		int k = n11 + n01, kk = n11 + n10;
        		return k == 0 || kk == 0 ? Double.NaN : (double)n11 / Math.sqrt((double) k * (double)kk);
        	}
        	
        	private static double getGowerLegendreMeasure(int n01, int n10, int n11)
        	{
        		double x = (double)n11 + 0.5 * (double)(n01 + n10);
        		return x <= 0 ? Double.NaN : (double)n11 / x;
        	}
        }

        public static double getPearsonCorrelation(double[] sample1, double[] sample2)
        {
            double mean1 = PrimitiveOperations.getAverage(sample1), mean2 = PrimitiveOperations.getAverage(sample2), cov = 0.0, var1 = 0.0, var2 = 0.0;
            for( int i = 0; i < sample1.length; i++ )
            {
                double x = sample1[i] - mean1, y = sample2[i] - mean2;
                cov += x * y;
                var1 += x * x;
                var2 += y * y;
            }
            double x = Math.sqrt(var1 * var2);
            if( x <= 0.0 ) return Double.NaN;
            return cov / x;
        }
        
        public static double getPearsonCorrelation(boolean[] sample1, boolean[] sample2)
        {
        	double[] sampleOne = new double[sample1.length], sampleTwo = new double[sample2.length];
            for( int i = 0; i < sample1.length; i++ )
            {
            	sampleOne[i] = sample1[i] == true ? 1.0 : 0.0;
            	sampleTwo[i] = sample2[i] == true ? 1.0 : 0.0;
            }
            return getPearsonCorrelation(sampleOne, sampleTwo);
        }

        public static double getSpearmanCorrelation(double[] sample1, double[] sample2)
        {
            Object[] objects1 = VectorOperations.getRanksWithTieCorrections(sample1), objects2 = VectorOperations.getRanksWithTieCorrections(sample2);
            return getSpearmanCorrelation((double[])objects1[0], (double)objects1[2], (double[])objects2[0], (double)objects2[2]);
        }

        public static double getSpearmanCorrelation(double[] ranks1, double tieCorrection1, double[] ranks2, double tieCorrection2)
        {
            double result = Distance.getEuclideanSquared(ranks1, ranks2);
            double size = (double)ranks1.length, x = size * size * size - size;
            result = 1.0 - 6.0 * result / x;
            if( tieCorrection1 == 0.0 && tieCorrection2 == 0.0 ) return result;
            double y = (1.0 - tieCorrection1 / x) * (1.0 - tieCorrection2 / x);
            return (result - 0.5 * (tieCorrection1 + tieCorrection2) / x) / Math.sqrt(y);
        }

        public static double getMahalanobisSquaredDistance(double[] vector1, double[] vector2, double[][] inverseCovarianceMatrix)
        {
            double[] vector = VectorOperations.getSubtractionOfVectors(vector1, vector2);
            return MatrixUtils.getProductOfTransposedVectorAndSymmetricMatrixAndVector(inverseCovarianceMatrix, vector);
        }
        
        public static double getKendallCoefficientOfConcordance(double[][] ranks, double[] tieCorrections2, boolean areRanksTransposed)
        {
            double[] sumsOfRanks = areRanksTransposed ? MatrixUtils.getSumsOfColumns(ranks) : MatrixUtils.getSumsOfRows(ranks);
            double[] meanRankAndSumOfSquaresCented = PrimitiveOperations.getSumOfSquaresCentered(sumsOfRanks);
            double m = areRanksTransposed ? (double)ranks.length : (double)ranks[0].length, n = areRanksTransposed ? (double)ranks[0].length : (double)ranks.length;
            double x = m * n * (n * n - 1.0);
            double result = 12.0 * meanRankAndSumOfSquaresCented[1] / (m * x);
            return result / (1.0 - PrimitiveOperations.getSum(tieCorrections2) / x);
        }
        
        public static double[] comparePearsonCorrelations(double correlationCoefficient1, int sampleSize1, double correlationCoefficient2, int sampleSize2)
        {
            double z1 = MathUtils.argthFunction(correlationCoefficient1), z2 = MathUtils.argthFunction(correlationCoefficient2);
            double zScore = Math.abs(z1 - z2) / Math.sqrt(1.0 / (double)(sampleSize1 - 3) + 1.0 / (double)(sampleSize2 - 3));
            double pValue = 1.0 - NormalDistribution.getDistributionFunction(zScore);
            return new double[]{zScore, pValue};
        }
        
        // 0.0 < confidenceLevel < 1.0; Usually confidenceLevel = 0.95 or 0.99/ 
        public static double[] confidenceIntervalForPearsonCorrelation(double correlationCoefficient, int sampleSize, double confidenceLevel)
        {
            double quantile = NormalDistribution.getStandartNormalQuantile(0.5 * (1.0 + confidenceLevel), 0.0001, 100);
            quantile /= Math.sqrt((double)(sampleSize - 3));
            double x = MathUtils.argthFunction(correlationCoefficient);
            return new double[]{MathUtils.thFunction(x - quantile), MathUtils.thFunction(x + quantile)};
        }
    }
    /****************** SimilaritiesAndDissimilarities : end ******************************/
    
    /********************* PopulationSize : start *****************************************/
    public static class PopulationSize
    {
        // count1 := size of 1-st set; count2:= size of 2-nd set; count12 := size of overlaps of 1-st and 2-nd sets 
        public static double getPopulationSizeChapman(int count1, int count2, int count12)
        {
            return (double)(count1 + 1) * (double)(count2 + 1) / (double)(count12 + 1) - 1.0;
        }
        
        // Orphans are the individuals that observed exactly in one sample.
        // Return FPCM.
        public static double getFpcm(int f1, int f2, int f3)
        {
            if( f2 <= 0 ) return Double.NaN;
            return 1.5 * (double)f1 * (double)f3 / ((double)(f2) * (double)f2);
        }
        
        public static double getFpcm2(int f2, int f3, int f4)
        {
            if( f3 <= 0 ) return Double.NaN;
            return 4.0 * (double)f2 * (double)f4 / (3.0 * (double)(f3) * (double)f3);
        }
        
        // Input : f_i = number of distinct individuals that observed exactly in i samples, i = 1,...,m;
        //             n = number of all distinct individuals that observed at least in one sample
        public static double[] getPopulationSizeAndSigmaChao(int f1, int f2, int n)
        {
            if( f2 == 0 ) return new double[]{Double.NaN, Double.NaN};
            double x1 = (double)f1 * (double)f1, x2 = x1 * x1, x3 = x1 / (double)f2;
            return new double[]{(double)n + 0.5 * x3, Math.sqrt(0.25 * (1.0 - (double)f2 / (double)n) * x3 * x3 / (double)f2 + x3 * (0.5 + (double)f1 / (double)f2) - 0.5 * x2 / (f2 * (2.0 *(double)n * (double)f2 + x1)))};
        }

        // Input : f_i = number of distinct individuals that observed exactly in i samples, i = 1,...,m;
        //             n = number of all distinct individuals that observed at least in one sample
        public static double[] getPopulationSizeAndSigmaLanumteangBohning(int f1, int f2, int f3, int n)
        {
            if( f2 == 0 ) return new double[]{Double.NaN, Double.NaN};
            double x1 = (double)f1 * (double)f1 * (double)f1, x2 = (double)f2 * (double)f2 * (double)f2, x3 = x1 / x2, x4  = x3 * x3, x5 = 0.75 * x1 * (double)f3;
            return new double[]{(double)n + 0.75 * x3 * (double)f3, Math.sqrt(5.0625 * (1.0 + (double)f1 / (double)f2) * x4 *(double)f3 * (double)f3 / (double)f1 + 0.5625 * (1.0 - (double)f3 / (double)n) * x4 *(double)f3 + (double)n * x5 / ((double)n * x2 + x5))};
        }

        // Input : f_i = number of distinct individuals that observed exactly in i samples, i = 1,...,m;
        //             n = number of all distinct individuals that observed at least in one sample
        public static double[] getPopulationSizeAndSigmaZelterman(int f1, int f2, int n)
        {
            if( f1 == 0 ) return new double[]{Double.NaN, Double.NaN};
            double x0 = 2.0 * (double)f2 / (double)f1, x1 = Math.exp(-x0), x2 = 1.0 - x1, x3 = (double)n * x1 / (x2 * x2);
            return new double[]{(double)n / x2, Math.sqrt(x3 * (1.0 + x3 * x0 * x0 * (1.0 / (double)f1 + 1.0 / (double)f2)))};
        }
        
        // Input : int[] freq : freq[i] = number of distinct individuals that observed exactly in (i + 1) samples, i = 0,...,m - 1;
        //             n = number of all distinct individuals that observed at least in one sample
        public static double[] getPopulationSizeAndSigmaMaximumLikelihood(int[] freq, double populationSizeInitialApproximation, int maxNumberOfIterations, double epsilon)
        {
            // 1. Calculation of initial approximation
            double sum = 0.0, n = 0.0;
            for( int i = 0; i < freq.length; i++ )
            {
                sum += (double)(i + 1) * (double)freq[i];
                n += (double)freq[i];
            }
            double lambda = - Math.log(1.0 - n / populationSizeInitialApproximation);

            // 2. Newton's method for calculation of lambda 
            for( int i = 0; i < maxNumberOfIterations; i++ )
            {
                double x = Math.exp(-lambda), xx = 1.0 - x, y = sum / lambda, yy = n / xx, delta = (y - yy) / (- y / lambda + yy * x / xx); 
                if( Math.abs(delta) < epsilon ) break;
                lambda -= delta;
            }
            double populationSize = n / (1.0 - Math.exp(-lambda)), z = sum / (double)populationSize;
            return new double[]{populationSize, Math.sqrt((double)populationSize / (Math.exp(z) - z - 1.0))};
        }
        
        // Input : int[] freq : freq[i] = number of distinct individuals that observed exactly in (i + 1) samples, i = 0,...,m - 1;
        //             n = number of all distinct individuals that observed at least in one sample
        public static double[] getPopulationSizes(int[] freq, int n, int maxNumberOfIterations)
        {
            double populationSizeChao = getPopulationSizeAndSigmaChao(freq[0], freq[1], n)[0];
            return new double[]{populationSizeChao,
                                getPopulationSizeAndSigmaLanumteangBohning(freq[0], freq[1], freq[2], n)[0],
                                getPopulationSizeAndSigmaZelterman(freq[0], freq[1], n)[0],
                                getPopulationSizeAndSigmaMaximumLikelihood(freq, populationSizeChao, maxNumberOfIterations, 1.0e-5)[0]};
        }
    }
    /********************* PopulationSize : finish **********************/
    
    private static Logger log = Logger.getLogger(StatUtils.class.getName());

}
