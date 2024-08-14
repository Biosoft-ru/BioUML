/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix.DataMatrixConstructor;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixStringConstructor;
import biouml.plugins.machinelearning.utils.StatUtils.RandomUtils;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.ChiSquaredDistribution;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.FisherDistribution;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSamples;
import biouml.plugins.machinelearning.utils.StochasticProcesses.MarkovChains;
import biouml.plugins.machinelearning.utils.UtilsGeneral.MathUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.Norm;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;

/**
 * @author yura
 *
 */
public class MetaAnalysis
{
    /****************** RankAggregation : start  ****************************/
    public static class RankAggregation
    {
    	// 23.03.22
        //public static final String RA_SCORE = "RA_score";
        public static final String RA_SCORE = "RA_score";
        
        public static final String METHOD_AR_MEAN  = "Borda : Arithmetic mean";
        public static final String METHOD_GEO_MEAN = "Borda : Geometric mean";
        public static final String METHOD_MEDIAN   = "Borda : Median";
        public static final String METHOD_L1_NORM  = "Borda : L1-norm";
        public static final String METHOD_L2_NORM  = "Borda : L2-norm";
        public static final String METHOD_MC1      = "Markov chain 1";
        public static final String METHOD_MC2      = "Markov chain 2";
        public static final String METHOD_MC3      = "Markov chain 3";
        
        public static final int MAX_NUMBER_OF_ITERATIONS = 10000;
        public static final int NUMBER_OF_PERMUTATIONS = 1;
        public static final double EPS = 1.0E-4;

        private String[] objectNames, listNames;
        private double[][] ranks; // columns of ranks are rank lists
        
        public RankAggregation(double[][] ranks, String[] objectNames, String[] listNames)
        {
            this.ranks = ranks;
            this.objectNames = objectNames;
            this.listNames = listNames;
        }
        
        public RankAggregation(DataMatrix dataMatrix, boolean[] doSortInIncreasingOrder)
        {
            this(MatrixUtils.getRanks(dataMatrix.getMatrix(), doSortInIncreasingOrder), dataMatrix.getRowNames(), dataMatrix.getColumnNames());
        }
        
        public RankAggregation(DataMatrix dataMatrixWithRanks)
        {
            this(dataMatrixWithRanks.getMatrix(), dataMatrixWithRanks.getRowNames(), dataMatrixWithRanks.getColumnNames());
        }
        
        public DataMatrix getRanks()
        {
            return new DataMatrix(objectNames, listNames, ranks);
        }
        
        public double[] getScoresTransformed(String methodName, Double tuningParameter, Integer maxNumberOfIterations, Double eps)
        {
            // return VectorTransformation.toZeroAndOneRange(getScores(methodName, tuningParameter, maxNumberOfIterations, eps));
            double[] scores = getScores(methodName, tuningParameter, maxNumberOfIterations, eps);
            return VectorOperations.getProductOfVectorAndScalar(scores, 1.0 / (double)scores.length);
        }
        
        public double[] getScores(String methodName, Double tuningParameter, Integer maxNumberOfIterations, Double eps)
        {
            double[] result = new double[ranks.length];
            switch( methodName )
            {
                case METHOD_AR_MEAN : for( int i = 0; i < ranks.length; i++ )
                                          result[i] = PrimitiveOperations.getAverage(ranks[i]);
                                       return result;
                case METHOD_GEO_MEAN : for( int i = 0; i < ranks.length; i++ )
                                           result[i] = PrimitiveOperations.getGeometricMean(ranks[i]);
                                       return result;
                case METHOD_MEDIAN   : for( int i = 0; i < ranks.length; i++ )
                                           result[i] = UnivariateSample.getMedian(ranks[i]);
                                       return result;
                case METHOD_L1_NORM  : for( int i = 0; i < ranks.length; i++ )
                                           result[i] = Norm.getManhattanL1(ranks[i]);
                                       return result;
                case METHOD_L2_NORM  : for( int i = 0; i < ranks.length; i++ )
                                           result[i] = Norm.getEuclideanL2(ranks[i]);
                                       return result;
                case METHOD_MC1      :
                case METHOD_MC2      :
                case METHOD_MC3      : double tunParameter = tuningParameter == null ? 0.2 / (double)ranks.length : tuningParameter;
                                       int maxNumIter = maxNumberOfIterations == null ? MAX_NUMBER_OF_ITERATIONS : maxNumberOfIterations;
                                       double epsilon = eps == null ? EPS : eps;
                                       double[][] transitionMatrix = getTransitionMatrix(methodName);
                                       MarkovChains.modifyTransitionMatrix(transitionMatrix, tunParameter);
                                       Object[] objects = MarkovChains.getStationaryProbabilities(transitionMatrix, null, maxNumIter, epsilon);
                                       return (double[])objects[0];
            }
            return null;
        }
        
        // TODO: To modify and remove to appropriate Class.
        public double[] getRandomScoresTransformed(String methodName, Integer numberOfPermutations, Double tuningParameter, Integer maxNumberOfIterations, Double eps)
        {
            double[] result = new double[0];
            int seed = 0, numPermut = numberOfPermutations == null ? NUMBER_OF_PERMUTATIONS : numberOfPermutations;
            Random randomNumberGenerator = new Random(seed);
            for( int i = 0; i < numPermut; i++ )
            {
                double[][] matrix = RandomUtils.permuteMatrixColumns(ranks, randomNumberGenerator);
                RankAggregation ra = new RankAggregation(matrix, null, null);
                double[] randomScores = ra.getScoresTransformed(methodName, tuningParameter, maxNumberOfIterations, eps);
                result = ArrayUtils.addAll(result, randomScores);
            }
            return result;
        }
        
        // TODO: To remove or to move to appropriate Class ???
        // The randomScores must be sorted (in increasing order).  
        public static double[] getEmpiricalPvalues(double[] scores, double[] randomScores)
        {
            double[] result = new double[scores.length];
            for( int i = 0; i < scores.length; i++ )
            {
                if( scores[i] <= randomScores[0] )
                    result[i] = 1.0 / (double)randomScores.length;
                else if( scores[i] >= randomScores[randomScores.length - 1] )
                    result[i] = 1.0;
                else
                    for( int j = 1; j < randomScores.length; j++ )
                        if( scores[i] <= randomScores[j] )
                        {
                            result[i] = (double)(j + 1) / (double)randomScores.length;
                            break;
                        }
            }
            return result;
        }

        // It return particular transition matrices that are used in three Markov chain models.
        public double[][] getTransitionMatrix(String markovModelType)
        {
            double[][] result = new double[ranks.length][ranks.length];
            double x = 1.0 / (double)ranks.length, xx = 1.0 / ((double)ranks.length * (double)ranks[0].length);
            int half = MathUtils.isOdd(ranks[0].length) ? (ranks[0].length + 1) / 2 : ranks[0].length / 2;
            for( int i = 0; i < ranks.length; i++ )
            {
                for( int ii = 0; ii < ranks.length; ii++ )
                {
                    if( i == ii ) continue;
                    switch( markovModelType )
                    {
                        case METHOD_MC1 : for( int j = 0; j < ranks[0].length; j++ )
                                              if( ranks[i][j] > ranks[ii][j] )
                                              {
                                                  result[i][ii] = x;
                                                  break;
                                              }
                                          break;
                        case METHOD_MC2 : int count = 0;
                                          for( int j = 0; j < ranks[0].length; j++ )
                                              if( ranks[i][j] > ranks[ii][j] )
                                              {
                                                  if( ++count < half ) continue;
                                                  result[i][ii] = x;
                                                  break;
                                              }
                                          break;
                        case METHOD_MC3 : count = 0;
                                          for( int j = 0; j < ranks[0].length; j++ )
                                              if( ranks[i][j] > ranks[ii][j] )
                                                  count++;
                                          result[i][ii] = (double)count * xx;
                                          break;
                        default         : return null;
                    }
                }
                result[i][i] = 1.0 - PrimitiveOperations.getSum(result[i]);
            }
            return result;
        }
        
        /******************** static methods *****************************/
        public static String[] getAvailableMethodNames()
        {
            return new String[]{METHOD_AR_MEAN, METHOD_GEO_MEAN, METHOD_MEDIAN, METHOD_L1_NORM, METHOD_L2_NORM, METHOD_MC1, METHOD_MC2, METHOD_MC3};
        }
    }
    /****************** RankAggregation : end **********************************/
    
    /****************** Homogeneity : start ************************************/
    public static class Homogeneity
    {
        public static final String KRUSKAL_WALLIS_TEST        = "Kruskal-Wallis test";
        public static final String ANOVA_TEST                 = "ANOVA test";
        public static final String COCHRAN_TEST               = "Cochran test";
        public static final String WELCH_TEST                 = "Welch test";
        public static final String BROWN_FORSYTHE_TEST        = "Brown-Forsythe test";
        public static final String MEHROTRA_TEST              = "Mehrotra test";
        public static final String HARTUNG_ARGAC_MAKAMBI_TEST = "Hartung-Argac-Makambi test";
        
        UnivariateSamples univariateSamples;
        int numberOfSamples = 0;
        
        public Homogeneity(UnivariateSamples univariateSamples)
        {
            this.univariateSamples = univariateSamples;
            numberOfSamples = univariateSamples.getNumberOfSamples();
        }
        
        public Object[] performPairwiseComparisonOfSamples(String[] testNames)
        {
            DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Statistic", "p-value"});
            DataMatrixStringConstructor dmcs = new DataMatrixStringConstructor(new String[]{"1_st sample", "2_nd sample", "Test"});
            for( int index = 0, i = 0; i < numberOfSamples - 1; i++ )
                for( int j = i + 1; j < numberOfSamples; j++ )
                {
                    UnivariateSamples us = univariateSamples.getPairOfSamples(i, j);
                    Homogeneity homogeneity = new Homogeneity(us);
                    DataMatrix dm = homogeneity.performTestsOfHomogeneity(testNames);
                    String[] sampleNames = us.getSampleNames(), testNamesNew = dm.getRowNames();
                    double[][] matrix = dm.getMatrix();
                    for( int ii = 0; ii < testNamesNew.length; ii++ )
                    {
                        String rowName = Integer.toString(index++);
                        dmcs.addRow(rowName, new String[]{sampleNames[0], sampleNames[1], testNamesNew[ii]});
                        dmc.addRow(rowName, matrix[ii]);
                    }
                }
            return new Object[]{dmc.getDataMatrix(), dmcs.getDataMatrixString()};
        }
        
        public void performPairwiseComparisonOfSamples(String[] testNames, DataElementPath pathToOutputFolder, String tableName)
        {
            Object[] objects = performPairwiseComparisonOfSamples(testNames);
            DataMatrix dm = (DataMatrix)objects[0];
            dm.writeDataMatrix(false, (DataMatrixString)objects[1], pathToOutputFolder, tableName, log);
        }
        
        public DataMatrix performTestsOfHomogeneity(String[] testNames)
        {
            String[] names = testNames == null ? getAvailableTestNames() : testNames;
            double[][] matrix = new double[names.length][];
            for( int i = 0; i < names.length; i++ )
                switch( names[i] )
                {
                    case KRUSKAL_WALLIS_TEST        : matrix[i] = performKruskalWallisTest(); break;
                    case ANOVA_TEST                 : matrix[i] = performAnovaTest(); break;
                    case COCHRAN_TEST               : matrix[i] = performCochranTest(); break;
                    case WELCH_TEST                 : matrix[i] = performWelchTest(); break;
                    case BROWN_FORSYTHE_TEST        : matrix[i] = performBrownForsytheTest(); break;
                    case MEHROTRA_TEST              : matrix[i] = performMehrotraTest(); break;
                    case HARTUNG_ARGAC_MAKAMBI_TEST : matrix[i] = performHartungArgacMakambiTest(); break;
                }
            return new DataMatrix(names, new String[]{"Statistic", "p-value"}, matrix);
        }
        
        public static String[] getAvailableTestNames()
        {
            return new String[]{KRUSKAL_WALLIS_TEST, ANOVA_TEST, COCHRAN_TEST, WELCH_TEST, BROWN_FORSYTHE_TEST, MEHROTRA_TEST, HARTUNG_ARGAC_MAKAMBI_TEST};
        }
        
        private double[] performKruskalWallisTest()
        {
            double[] combinedSample = univariateSamples.getCombinedSample();
            Object[] objects = VectorOperations.getRanksWithTieCorrections(combinedSample);
            double[] ranks = (double[])objects[0];
            double[][] sampleRanks = new double[numberOfSamples][];
            for( int i = 0, index = 0; i < numberOfSamples; i++ )
            {
                int size = univariateSamples.getSizeOfGivenSample(i);
                sampleRanks[i] = UtilsForArray.copySubarray(ranks, index, size);
                index += size;
            }
            double tieCorrections = (double)objects[2], statistic = 0.0, mean = (double)(ranks.length + 1) / 2.0;
            for( int i = 0; i < sampleRanks.length; i++ )
            {
                double x = PrimitiveOperations.getAverage(sampleRanks[i]) - mean;
                statistic += x * x * (double)sampleRanks[i].length;
            }
            statistic *= 12.0 / ((double)ranks.length * (double)(ranks.length + 1));
            if( tieCorrections > 0.0 )
                statistic /= 1.0 - tieCorrections / ((double)combinedSample.length * (double)combinedSample.length * (double)combinedSample.length - (double)combinedSample.length);
            double pValue = 1.0 - ChiSquaredDistribution.getDistributionFunction(statistic, sampleRanks.length - 1, 50);
            return new double[]{statistic, pValue};
        }
        
        private double[] performMehrotraTest()
        {
            Object[] objects = getBrownForsytheStatisticAndSums();
            double statistic = (double)objects[0], sum1 = (double)objects[1], sum2 = (double)objects[2], sum3 = 0.0, sum4 = 0.0, sum5 = 0.0;
            double[] variances = (double[])objects[3];
            int totalSize = (int)objects[4];
            for(int i = 0; i < numberOfSamples; i++ )
            {
                double x = variances[i] * variances[i], size = (double)univariateSamples.getSizeOfGivenSample(i);
                sum3 += x;
                sum4 += size * variances[i];
                sum5 += size * x;
            }
            double degrees = sum1 * sum1 / (sum3 + sum4 * sum4 / ((double)totalSize * (double)totalSize) - 2.0 * sum5 / (double)totalSize);
            double pValue = 1.0 - FisherDistribution.getDistributionFunction(statistic, degrees, sum1 * sum1 / sum2, 100);
            return new double[]{statistic, pValue};
        }
        
        private double[] performBrownForsytheTest()
        {
            Object[] objects = getBrownForsytheStatisticAndSums();
            double statistic = (double)objects[0], sum1 = (double)objects[1], sum2 = (double)objects[2], pValue = 1.0 - FisherDistribution.getDistributionFunction(statistic, (double)(numberOfSamples - 1), sum1 * sum1 / sum2, 100);
            return new double[]{statistic, pValue};
        }
        
        private Object[] getBrownForsytheStatisticAndSums()
        {
            Object[] objects = univariateSamples.getMeansAndVariances();
            double[] means = (double[])objects[0], variances = (double[])objects[1];
            int totalSize = univariateSamples.getTotalSize();
            double totalMean = univariateSamples.getTotalMean(totalSize), between = univariateSamples.getBetweenSumOfSquares(totalMean, means);
            double sum1 = 0.0, sum2 = 0.0;
            for(int i = 0; i < numberOfSamples; i++ )
            {
                int size = univariateSamples.getSizeOfGivenSample(i);
                double x = variances[i] * (1.0 - (double)size / (double)totalSize);
                sum1 += x;
                sum2 += x * x / (double)(size - 1);
            }
            return new Object[]{between / sum1, sum1, sum2, variances, totalSize};
        }

        private double[] performAnovaTest()
        {
            double[] means = univariateSamples.getMeans();
            int totalSize = univariateSamples.getTotalSize(), n1 = numberOfSamples - 1, n2 = totalSize - numberOfSamples;
            double totalMean = univariateSamples.getTotalMean(totalSize), between = univariateSamples.getBetweenSumOfSquares(totalMean, means), within = univariateSamples.getWithinSumOfSquares(means);
            if( within == 0.0 ) return new double[]{Double.NaN, Double.NaN};
            double statistic = (double)n2 * between / ((double)n1 * within), pValue = 1.0 - FisherDistribution.getDistributionFunction(statistic, (double)n1, (double)n2, 100);
            return new double[]{statistic, pValue};
        }
        
        private double[] performHartungArgacMakambiTest()
        {
            return performWelchTest(false);
        }
        
        private double[] performWelchTest()
        {
            return performWelchTest(true);
        }

        private double[] performWelchTest(boolean doWeightsForNotModifiedCochranTest)
        {
            Object[] objects = getCochranStatisticAndHweights(doWeightsForNotModifiedCochranTest);
            if( objects == null ) return new double[]{Double.NaN, Double.NaN};
            double[] hWeights = (double[])objects[1];
            double statistic = (double)objects[0], degrees1 = (double)(numberOfSamples - 1), sum = 0.0;
            for(int i = 0; i < hWeights.length; i++ )
            {
                double x = 1.0 - hWeights[i];
                sum += x * x / (double)(univariateSamples.getSizeOfGivenSample(i) - 1);
            }
            statistic /= degrees1 + 2.0 * (double)(numberOfSamples - 2) * sum / (double)(numberOfSamples + 1);
            double degrees2 = ((double)numberOfSamples * (double)numberOfSamples - 1.0) / (3.0 * sum);
            return new double[]{statistic, 1.0 - FisherDistribution.getDistributionFunction(statistic, degrees1, degrees2, 100)};
        }
        
        private double[] performCochranTest()
        {
            Object[] objects = getCochranStatisticAndHweights(true);
            if( objects == null ) return new double[]{Double.NaN, Double.NaN};
            return new double[]{(double)objects[0], 1.0 - ChiSquaredDistribution.getDistributionFunction((double)objects[0], numberOfSamples - 1, 100)};
        }
        
        private Object[] getCochranStatisticAndHweights(boolean doWeightsForNotModifiedCochranTest)
        {
            Object[] objects = univariateSamples.getMeansAndVariances();
            double[] means = (double[])objects[0], variances = (double[])objects[1];
            if( ArrayUtils.contains(variances, 0.0) ) return null;
            objects = getWweightsAndHweights(variances, doWeightsForNotModifiedCochranTest);
            double[] wWeights = (double[])objects[0], hWeights = (double[])objects[1];
            double x = VectorOperations.getInnerProduct(hWeights, means), statistic = 0.0;
            for(int i = 0; i < numberOfSamples; i++ )
            {
                double y = means[i] - x;
                statistic += wWeights[i] * y * y;
            }
            return new Object[]{statistic, hWeights};
        }
        
        private Object[] getWweightsAndHweights(double[] variances, boolean doWeightsForNotModifiedCochranTest)
        {
            double[] wWeights = new double[numberOfSamples];
            if( doWeightsForNotModifiedCochranTest )
                for(int i = 0; i < numberOfSamples; i++ )
                    wWeights[i] = (double)univariateSamples.getSizeOfGivenSample(i) / variances[i];
            else
                for(int i = 0; i < numberOfSamples; i++ )
                {
                    double size = (double)univariateSamples.getSizeOfGivenSample(i);
                    wWeights[i] = size * (size - 3.0) / (variances[i] * (size - 1.0));
                }
            double[] hWeights = VectorOperations.getProductOfVectorAndScalar(wWeights, 1.0 / PrimitiveOperations.getSum(wWeights));
            return new Object[]{wWeights, hWeights};
        }
    }
    /****************** Homogeneity : end **************************************/
    
    private static Logger log = Logger.getLogger(MetaAnalysis.class.getName());
}
