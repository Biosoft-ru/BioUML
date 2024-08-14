package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.logging.Logger;

import one.util.streamex.StreamEx;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysis.Util;
import ru.biosoft.graphics.chart.Chart;

/**
 * @author yura
 *
 */
public class StatUtil
{
    // It is copied
    /***
     * B-distribution
     * @param x
     * @param a
     * @param b
     * @param niter number of iteration for integral solving
     * @return value of beta Distribution function and 1- beta Distribution function
     */
    public static double[] betaDistribution(double x, double a, double b, long niter)
    {
        double z = 1.0 - x, beta = 0.0;
        if( x <= 0.0 || z >= 1.0 )
            return new double[] {0.0, 1.0};
        if( x >= 1.0 || z <= 0.0 )
            return new double[] {1.0, 0.0};
        double aaa, aaa1, aaa2, aaa3, aaa4;
        double zz, zzz, z1, z2, z3, z4, z5, z6, z7;
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
        if( zz <= ( zzz - 1.0 ) * Math.exp(1.2 * Math.log(aaa)) )
        {
            aaa4 = aaa;
            aaa = aaa1;
            aaa1 = aaa4;
            aaa4 = aaa2;
            aaa2 = aaa3;
            aaa3 = aaa4;
            zz = aaa2 + 1.0;
            ind = !ind;
        }
        z1 = aaa2 < 1.0 ? zz : aaa2;
        beta = 1.0;
        z2 = aaa3 - 1.0;
        z3 = aaa / aaa1;
        z4 = z1 - 1.0;
        z5 = z1 - 2.0;
        z6 = z1 + z2;
        for( long jj = niter * 2L, j = niter; j > 0L; j--, jj -= 2L )
        {
            z7 = z4 + jj;
            if( beta == 0.0 )
                return new double[] {0.0, 1.0};
            beta = 1.0 + z3 * ( ( j * ( z6 + j ) ) / ( ( z1 + jj ) * z7 ) ) / beta;
            if( beta == 0.0 )
                return new double[] {0.0, 1.0};
            beta = 1.0 - z3 * ( ( ( aaa3 - j ) * ( z4 + j ) ) / ( ( z5 + jj ) * z7 ) ) / beta;
        }
        if( beta == 0.0 )
            return new double[] {0.0, 1.0};
        beta = 1d / beta;
        if( z1 != aaa2 )
        {
            beta *= aaa * zzz / zz;
            beta += aaa1;
        }
        zz = Stat.logBeta(aaa2, aaa3);
        zzz = aaa2 * Math.log(aaa) + z2 * Math.log(aaa1) - zz;
        beta *= Math.exp(zzz) / aaa2;
        if( beta < 0 )
        {
            beta = - beta;
            ind = ! ind;
        }
        double invbeta = 1.0 - beta;
        return ind ? new double[] {invbeta, beta} : new double[] {beta, invbeta};
    }

    /// it is copied
    /***
     * Calculation of Student`s distribution function;
     * @param x
     * @param degrees : degrees of freedom
     * @param niter : number of iterations for calculating the beta distribution function
     * @return array: array[0] = value of distribution function, F(x); array[1] = 1 - F(x);
     */
    public static double[] studentDistribution(double x, double degrees, long niter)
    {
        if( niter <= 0 )
            throw new IllegalArgumentException("Wrong iteration number");
        if( degrees <= 0 )
            throw new IllegalArgumentException("Wrong freedom degrees");
        double z = x * x;
        double[] probs = betaDistribution(z / ( z + degrees ), 0.5, 0.5 * degrees, niter);
        double prob = ( 1.0 + probs[0] ) / 2.0;
        return x > 0.0 ? new double[]{prob, 1.0 - prob} : new double[]{1.0 - prob, prob};
    }
    
    public static double getStudentQuantileApproximation(double quantileNormalDistribution, double degrees)
    {
        double y = quantileNormalDistribution * quantileNormalDistribution, quantile = 79.0 * y + 776.0, yy = 3.0 * y + 19.0;
        quantile *= y;
        quantile += 1482.0;
        quantile *= y;
        quantile -= 1920.0;
        quantile *= y;
        quantile -= 945.0;
        quantile *= quantileNormalDistribution;
        quantile /= 92160.0;
        quantile /= degrees;
        yy *= y;
        yy += 17.0;
        yy *= y;
        yy -= 15.0;
        yy *= quantileNormalDistribution;
        yy /= 384.0;
        quantile += yy;
        quantile /= degrees;
        yy = 5.0 * y + 16.0;
        yy *= y;
        yy += 3.0;
        yy *= quantileNormalDistribution;
        yy /= 96.0;
        quantile += yy;
        quantile /= degrees;
        yy = y + 1.0;
        yy *= quantileNormalDistribution;
        yy *= 0.25;
        quantile += yy;
        quantile /= degrees;
        quantile += quantileNormalDistribution;
        return quantile; 
    }

    // it is copied
    // Sometimes Stat.spearmanCorrelationFast() don't work correctly
    public static double getSpearmanCorrelation(double[] sample1, double[] sample2)
    {
        int n = sample1.length;
        double[] ranks1 = new double[n], ranks2 = new double[n], tieCorrections1 = new double[2], tieCorrections2 = new double[2];
        Stat.getRanks(sample1, ranks1, tieCorrections1);
        Stat.getRanks(sample2, ranks2, tieCorrections2);
        return getSpearmanCorrelation(ranks1, tieCorrections1[1], ranks2, tieCorrections2[1]);
    }

    //// it is copied
    public static double getSpearmanCorrelation(double[] ranks1, double tieCorrection1, double[] ranks2, double tieCorrection2)
    {
        double result = MatrixUtils.Distance.getEuclideanSquared(ranks1, ranks2);
        double size = (double)ranks1.length, x = size * size * size - size;
        result = 1.0 - 6.0 * result / x;
        if( tieCorrection1 == 0.0 && tieCorrection2 == 0.0 ) return result;
        double y = (1.0 - tieCorrection1 / x) * (1.0 - tieCorrection2 / x);
        return (result - 0.5 * (tieCorrection1 + tieCorrection2) / x) / Math.sqrt(y);
    }

    /***
     * 
     * @param sample1; in general, dim(sample)  !=  dim(sample2);
     * @param sample2
     * @return array; array[0] = z-score (normal approximation of Wilcoxon statistic); array[1] = p-value;
     */
    public static double[] getWilcoxonTwoSampleRankTest(double[] sample1, double[] sample2)
    {
        double[] unifiedSample = Util.append(sample1, sample2), ranks = new double[unifiedSample.length], tieCorrections = new double[2];
        Stat.getRanks(unifiedSample, ranks, tieCorrections);
        double statistic = 0.0;
        for( int i = 0; i < sample1.length; i++ )
            statistic += ranks[i];
        double nm = (double)(sample1.length + sample2.length);
        double x = 0.5 * (double)sample1.length * (nm + 1.0);
        statistic -= x;
        double var = x * (double)sample2.length / 6.0;
        if( tieCorrections[1] > 0.0 )
            var *= 1.0 - tieCorrections[1] / (nm * (nm * nm - 1.0));
        statistic /= Math.sqrt(var);
        double pValue = 2.0 * (1.0 - Stat.standartNormalDistribution(Math.abs(statistic)));
        return new double[] {statistic, pValue};
    }

    // it is copied
    /***
     * Shuffle vector
     * @param vector
     * @param seed
     */
    public static void shuffleVector(int[] vector, int seed)
    {
        Random randomNumberGenerator = new Random(seed);
        for( int i = vector.length - 1; i > 0; i-- )
        {
            int j = randomNumberGenerator.nextInt(i + 1), tmp = vector[i];
            vector[i] = vector[j];
            vector[j] = tmp;
        }
    }
    
    /******************** DensityEstimation : start *********************/
    public static class DensityEstimation
    {
        public static final String WINDOW_WIDTH_01 = "0.1 x Abs(mean value)";
        public static final String WINDOW_WIDTH_02 = "0.1 x (maximal value - minimal value)";
        public static final String WINDOW_WIDTH_03 = "0.2 x (maximal value - minimal value)";
        public static final String WINDOW_WIDTH_04 = "Given smoothing window width";
        
        public static String[] getSmoothingWindowWidthTypesAvailable()
        {
            return new String[]{WINDOW_WIDTH_04, WINDOW_WIDTH_01, WINDOW_WIDTH_02, WINDOW_WIDTH_03};
        }
        
        public static Chart chartWithSmoothedDensities(Map<String, double[]> sampleNameAndSample, String commonNameOfSamples, Boolean doAddTwoZeroPoints, Map<String, Double> nameAndMultipliers, String windowSelector, Double givenWindow)
        {
            List<double[]> xValuesForCurves = new ArrayList<>(), yValuesForCurves = new ArrayList<>();
            for( Entry<String, double[]> entry : sampleNameAndSample.entrySet() )
            {
                String name = entry.getKey();
                double[] sample = entry.getValue();
                double window = getWindow(sample, windowSelector, givenWindow);
                List<double[]> curve = Stat.getEmpiricalDensitySmoothedByEpanechninkov(sample, window, doAddTwoZeroPoints);
                xValuesForCurves.add(curve.get(0));
                yValuesForCurves.add(curve.get(1));
                if( nameAndMultipliers != null && nameAndMultipliers.containsKey(name) )
                {
                    double multiplier = nameAndMultipliers.get(name);
                    double[] array = curve.get(1);
                    for( int j = 0; j < array.length; j++ )
                        array[j] *= multiplier;
                }
            }
            double commonMultiplier = getCommonMultiplier(yValuesForCurves);
            if(commonMultiplier > 1.1 )
                recalculateDensities(commonMultiplier, xValuesForCurves, yValuesForCurves);
            String newCommonName = commonMultiplier > 1.1 ?  (int)(commonMultiplier + 0.1) + " x (" + commonNameOfSamples + ")" : commonNameOfSamples;
            return TableUtils.createChart(xValuesForCurves, yValuesForCurves, getSampleNames(sampleNameAndSample), null, null, null, null, null, null, null, newCommonName, "Probability");
        }
        
        public static Double getWindow(double[] sample, String windowSelector, Double givenWindow)
        {
            switch( windowSelector )
            {
                case WINDOW_WIDTH_01 : return 0.1 * Math.abs(Stat.mean(sample));
                case WINDOW_WIDTH_02 : double[] minAndMax1 = Stat.getMinAndMax(sample);
                                       return 0.1 * (minAndMax1[1] - minAndMax1[0]);
                case WINDOW_WIDTH_03 : double[] minAndMax2 = Stat.getMinAndMax(sample);
                                       return 0.2 * (minAndMax2[1] - minAndMax2[0]);
                case WINDOW_WIDTH_04 : return givenWindow;
                default              : return null;
            }
        }
        
        private static double getCommonMultiplier(List<double[]> yValuesForCurves)
        {
            double max = StreamEx.of(yValuesForCurves).flatMapToDouble( Arrays::stream ).max().orElse( Double.MIN_VALUE );
            if( max < 1.0 ) return 1.0;
            return Math.pow(10.0, Math.ceil(Math.log10(max)));
        }

        private static void recalculateDensities(double commonMultiplier, List<double[]> xValuesForCurves, List<double[]> yValuesForCurves)
        {
            double y = 1.0 / commonMultiplier;
            for( int i = 0; i < xValuesForCurves.size(); i++ )
            {
                double[] xValues = xValuesForCurves.get(i);
                xValuesForCurves.set(i, MatrixUtils.getProductOfVectorAndScalar(xValues, commonMultiplier));
                double[] yValues = yValuesForCurves.get(i);
                yValuesForCurves.set(i, MatrixUtils.getProductOfVectorAndScalar(yValues, y));
            }
        }
        
        // TODO: to find compact codes for this method
        private static String[] getSampleNames(Map<String, double[]> sampleNameAndSample)
        {
           String[] result = new String[sampleNameAndSample.size()];
           int index = 0;
           for( String name : sampleNameAndSample.keySet() )
               result[index++] = name;
           return result;
        }
    }
    /******************** DensityEstimation : finish *********************/
    
    /********************* DistributionMixture : start **********************/
    // TODO: class 'DistributionMixture' is under construction !!!!
    public static class DistributionMixture
    {
        
        private final double[] sample; // dim = n
        private final int[] indicesOfComponents; // dim = n; indicesOfComponents[i] = j <=> i-th sample element belongs to j-th mixture component
        private final Map<Integer, double[]> componentIndexAndSubsample;
        
        public DistributionMixture(double[] sample, int[] indicesOfComponents, int numberOfClusters, String distanceType)
        {
            this.sample = sample;
            this.indicesOfComponents = indicesOfComponents != null ? indicesOfComponents : Clusterization.initializeInitialIndicesOfClustersRandomly(numberOfClusters, sample.length);
            this.componentIndexAndSubsample = createComponentIndexAndSubsample(sample, this.indicesOfComponents);
        }
        
        public static Map<Integer, double[]> createComponentIndexAndSubsample(double[] sample, int[] indicesOfComponents)
        {
            Map<Integer, List<Double>> preResult = new HashMap<>();
            for( int i = 0; i < sample.length; i++ )
                preResult.computeIfAbsent(indicesOfComponents[i], key -> new ArrayList<>()).add(sample[i]);
            Map<Integer, double[]> result = new HashMap<>();
            for( Entry<Integer, List<Double>> entry : preResult.entrySet() )
                result.put(entry.getKey(), MatrixUtils.fromListToArray(entry.getValue()));
            return result;
        }
        
        /***
         * Expectation Step in EM-algorithm for identification of k-component normal mixture.
         * The probabilities Pij are determined as Pij = P(i-th element of sample belongs to j-th component of mixture).
         * 
         * @param meansAndSigmas
         * @param probabilitiesOfMixtureComponents
         * @param sample
         * @return probabilitiesPij[][] = probabilities Pij;
         ***/
        public static double[][] getProbabilitiesPijForNormalMixture(double[][] meansAndSigmas, double[] probabilitiesOfMixtureComponents, double[] sample)
        {
            int n = sample.length, k = probabilitiesOfMixtureComponents.length;
            double[][] probabilitiesPij = new double[n][k];
            for( int i = 0; i < n; i++ )
                for( int j = 0; j < k; j++ )
                    probabilitiesPij[i][j] = probabilitiesOfMixtureComponents[j] * Stat.getNormalDensity(sample[i], meansAndSigmas[j][0], meansAndSigmas[j][1]);
            for( int i = 0; i < n; i++ )
            {
                double sum = 0.0;
                for( int j = 0; j < k; j++ )
                    sum += probabilitiesPij[i][j];
                for( int j = 0; j < k; j++ )
                    probabilitiesPij[i][j] /= sum;
            }
            return probabilitiesPij;
        }
        
        /****
         * Maximization Step in EM-algorithm for identification of k-component normal mixture.
         * 
         * @param probabilitiesPij
         * @param sample
         * @return Object[]
         * Object[0] = double[k][2] - meansAndSigmas for each mixture component
         * Object[1] = double[k]  - probabilitiesOfMixtureComponents
         */
        public static Object[] getParametersOfNormalMixture(double[][] probabilitiesPij, double[] sample)
        {
            int n = probabilitiesPij.length, k = probabilitiesPij[0].length;
            double[][] meansAndSigmas = new double[k][2];
            double[] probabilitiesOfMixtureComponents = new double[k];
            for( int j = 0; j < k; j++ )
            {
                double sum1 = 0.0, sum2 = 0.0, sum3 = 0.0;
                for( int i = 0; i < n; i++ )
                {
                    sum1 += probabilitiesPij[i][j];
                    double xx = probabilitiesPij[i][j] * sample[i];
                    sum2 += xx;
                    sum3 += xx * sample[i];
                }
                probabilitiesOfMixtureComponents[j] = sum1 / (double)n;
                meansAndSigmas[j][0] = sum2 / sum1;
                meansAndSigmas[j][1] = Math.sqrt((sum3 - 2.0 * meansAndSigmas[j][0] * sum2) / sum1 + meansAndSigmas[j][0] * meansAndSigmas[j][0]);
            }
            return new Object[] {meansAndSigmas, probabilitiesOfMixtureComponents};
        }
        
        /***
         * Full iterative EM-algorithm for identification of k-component normal mixture.
         * Each iteration consists of 2 steps: Expectation Step and Maximization Step.
         * 
         * @param meansAndSigmas contains initial approximations of means and sigmas of k normal distributions.
         * @param probabilitiesOfMixtureComponents: k probabilities of mixture components.
         * @param sample: List<Integer>
         * @param maximalNumberOfIterations
         * @return Object[];
         * Object[0] = newMeansAndSigmas;
         * Object[1] = newProbabilitiesOfMixtureComponents;
         * Object[2] = numberOfIterations;
         * Object[3] = probabilitiesPij; Each probability Pij is the probability that i-th element of sample belongs to j-th component of mixture;
         */
        private static Object[] estimateNormalMixtureByEmAlgorithm(double[][] meansAndSigmas, double[] probabilitiesOfMixtureComponents, double[] sample, int maximalNumberOfIterations)
        {
           double[][] probabilitiesPij = getProbabilitiesPijForNormalMixture(meansAndSigmas, probabilitiesOfMixtureComponents, sample);
           double[][] newMeansAndSigmas = null;
           double[] newProbabilitiesOfMixtureComponents = null;
           double[] oldProbabilitiesOfMixtureComponents = probabilitiesOfMixtureComponents.clone();
           int numberOfIterations = 1;
           for( ; numberOfIterations <= maximalNumberOfIterations; numberOfIterations++ )
           {
               if( numberOfIterations > 1 )
                   probabilitiesPij = getProbabilitiesPijForNormalMixture(newMeansAndSigmas, newProbabilitiesOfMixtureComponents, sample);
               Object[]  objects = getParametersOfNormalMixture(probabilitiesPij, sample);
               newMeansAndSigmas = (double[][])objects[0];
               newProbabilitiesOfMixtureComponents = (double[])objects[1];
               boolean isEqual = true;
               // for( int j = 0; j < probabilitiesOfMixtureComponents.length; j++ )
               for( int j = 0; j < oldProbabilitiesOfMixtureComponents.length; j++ )
                   if( newProbabilitiesOfMixtureComponents[j] != oldProbabilitiesOfMixtureComponents[j] )
                   {
                       isEqual = false;
                       break;
                   }
               if( isEqual ) break;
               List<Integer> listOfDegenerateComponents = determineDegenerateComponents(newMeansAndSigmas);
               if( listOfDegenerateComponents != null )
               {
                   objects = changeArrays(listOfDegenerateComponents, newProbabilitiesOfMixtureComponents, newMeansAndSigmas, probabilitiesPij);
                   newProbabilitiesOfMixtureComponents = (double[])objects[0];
                   newMeansAndSigmas = (double[][])objects[1];
                   probabilitiesPij = (double[][])objects[2];
               }
               oldProbabilitiesOfMixtureComponents = newProbabilitiesOfMixtureComponents.clone();
           }
           if( numberOfIterations > maximalNumberOfIterations )
               numberOfIterations = maximalNumberOfIterations;
           return new Object[]{newMeansAndSigmas, newProbabilitiesOfMixtureComponents, numberOfIterations, probabilitiesPij};
        }
        
        private static Object[] changeArrays(List<Integer> listOfDegenerateComponents, double[] probabilitiesOfMixtureComponents, double[][] meansAndSigmas, double[][] probabilitiesPij)
        {
            boolean doRemove[] = new boolean[probabilitiesOfMixtureComponents.length];
            for( int i = 0; i < doRemove.length; i++ )
                doRemove[i] = false;
            for( int x : listOfDegenerateComponents )
                doRemove[x] = true;
            double[] newProbabilitiesOfMixtureComponents = new double[probabilitiesOfMixtureComponents.length - listOfDegenerateComponents.size()];
            double[][] newMeansAndSigmas = new double[probabilitiesOfMixtureComponents.length - listOfDegenerateComponents.size()][];
            double[][] newProbabilitiesPij = new double[probabilitiesPij.length][probabilitiesOfMixtureComponents.length - listOfDegenerateComponents.size()];
            int index = 0;
            for( int i = 0; i < probabilitiesOfMixtureComponents.length; i++ )
                if( ! doRemove[i] )
                {
                    newMeansAndSigmas[index] = meansAndSigmas[i];
                    for(int j = 0; j < probabilitiesPij.length; j++ )
                        newProbabilitiesPij[j][index] = probabilitiesPij[j][i];
                    newProbabilitiesOfMixtureComponents[index++] = probabilitiesOfMixtureComponents[i];
                }
            return new Object[]{newProbabilitiesOfMixtureComponents, newMeansAndSigmas, newProbabilitiesPij};
        }
        
        private static List<Integer> determineDegenerateComponents(double[][] meansAndSigmas)
        {
            List<Integer> result = new ArrayList<>();
            for( int i = 0; i < meansAndSigmas.length; i++ )
                if( Double.isNaN(meansAndSigmas[i][1]) || meansAndSigmas[i][1] <= 0.0 )
                    result.add(i);
            return result.isEmpty() ? null : result;
        }

        private static Map<Integer, int[]> getSubsamplesSimulatedByProbabilitiesPij(double[][] probabilitiesPij, Random random)
        {
            Map<Integer, List<Integer>> preResult = new HashMap<>();
            for( int i = 0; i < probabilitiesPij.length; i++ )
            {
                double randomValue = random.nextDouble(), sum = 0.0;
                for( int j = 0; j < probabilitiesPij[0].length; j++ )
                {
                    sum += probabilitiesPij[i][j];
                    if( sum >= randomValue )
                    {
                        preResult.computeIfAbsent(j, key -> new ArrayList<>()).add(i);
                        break;
                    }
                }
            }
            Map<Integer, int[]> result = new HashMap<>();
            for( Entry<Integer, List<Integer>> entry : preResult.entrySet() )
                result.put(entry.getKey(), MatrixUtils.fromIntegerListToArray(entry.getValue()));
            return result;
        }

        /***
         * @param sample
         * @param numberOfMixtureComponents
         * @param maximalNumberOfIterations
         * @param random
         * @return Map<Integer, Object[]> : Integer - index of mixture component, 0 <= Integer <= numberOfMixtureComponents;
         * When 0 <= Integer <= numberOfMixtureComponents - 1, dim(Object[]) = 5,
         * objects[0] = probability of Integer-th MixtureComponent,
         * objects[1] = double[2] = meanAndSigmaFromMixture for Integer-th MixtureComponent,
         * objects[2] = double[2] = meanAndSigmaFromSimulation for Integer-th MixtureComponent,
         * objects[3] = double[] = Integer-th subsample;
         * objects[4] = int[] elementIndices for Integer-th subsample;
         * If Integer == numberOfMixtureComponents, then dim(Object[]) = 1, Object[0] = numberOfIterations
         */
        public static Map<Integer, Object[]> getNormalMixture(double[] sample, int numberOfMixtureComponents, int maximalNumberOfIterations, Random random)
        {
            Map<Integer, Object[]> result = new HashMap<>();
            
            // 1. Determination of initial approximations for input parameters
            if( sample == null || sample.length < 3 * numberOfMixtureComponents ) return null;
            double[] meanAndSigma = Stat.getMeanAndSigma(sample), minAndMax = Stat.getMinAndMax(sample);
            double h = (minAndMax[1] - minAndMax[0]) / (numberOfMixtureComponents + 1);
            double[] probabilitiesOfMixtureComponents = MatrixUtils.getConstantVector(numberOfMixtureComponents, 1.0 / numberOfMixtureComponents);
            double[][] meansAndSigmas = new double[numberOfMixtureComponents][];
            for( int i = 0; i < numberOfMixtureComponents; i++ )
                meansAndSigmas[i] = new double[]{(i + 1) * h, meanAndSigma[1]};
            
            // 2. Normal mixture estimation
            Object[] objects = estimateNormalMixtureByEmAlgorithm(meansAndSigmas, probabilitiesOfMixtureComponents, sample, maximalNumberOfIterations);
            double[][] meansAndSigmasFromMixture = (double[][])objects[0];
            double[] newProbabilitiesOfMixtureComponents = (double[])objects[1];
            Integer numberOfIterations = (Integer)objects[2];
            double[][] probabilitiesPij = (double[][])objects[3];
            
            // 3. Calculation additional outputs
            Map<Integer, int[]> subsampleIndexAndElementIndices = getSubsamplesSimulatedByProbabilitiesPij(probabilitiesPij, random);
            Map<Integer, double[]> indexAndSubsample = getSubsamples(subsampleIndexAndElementIndices, sample);
            for( Entry<Integer, double[]> entry : indexAndSubsample.entrySet() )
            {
                int componentIndex = entry.getKey();
                double[] subsample = entry.getValue();
                if( subsample != null )
                {
                    double[] meanAndSigmaFromSimulation = subsample.length > 1 ? Stat.getMeanAndSigma(subsample) : new double[]{subsample[0], 0.0};
                    result.put(componentIndex, new Object[]{newProbabilitiesOfMixtureComponents[componentIndex], meansAndSigmasFromMixture[componentIndex], meanAndSigmaFromSimulation, subsample, subsampleIndexAndElementIndices.get(componentIndex)});
                }
            }
            result.put(indexAndSubsample.size(), new Object[]{numberOfIterations});
            return result;
        }
        
        private static Map<Integer, double[]> getSubsamples(Map<Integer, int[]> subsampleIndexAndElementIndices, double[] sample)
        {
            Map<Integer, double[]> result = new HashMap<>();
            for( Entry<Integer, int[]> entry : subsampleIndexAndElementIndices.entrySet() )
            {
                Integer subsampleIndex = entry.getKey();
                int[] elementIndices = entry.getValue();
                double[] subSample = new double[elementIndices.length];
                for( int i = 0; i < elementIndices.length; i++ )
                    subSample[i] = sample[elementIndices[i]];
                result.put(subsampleIndex, subSample);
            }
            return result;
        }
    }
    /********************* DistributionMixture : finish **********************/
    
    /********************* TestsForExponentiality : start **********************/
    public static class TestsForExponentiality
    {
        public static double[] getGiniTest(double[] sample)
        {
            double mean = Stat.mean(sample);
            double[] sampleNormalized = MatrixUtils.getProductOfVectorAndScalar(sample, 1.0 / mean);
            
            // 1. Long way to calculate statistic.
//            double statistic = 0.0;
//            for( int i = 0; i < sample.length; i++ )
//                for( int j = 0; j < sample.length; j++ )
//                    statistic += Math.abs(sampleNormalized[i] - sampleNormalized[j]);
//            statistic /= (2.0 * (double)sample.length * (double)(sample.length - 1));
            
            // 2. Fast way to calculate statistic.
            double[] sampleNormalizedSorted = MatrixUtils.sortInAscendingOrder(sampleNormalized);
            double statistic = 0.0;
            for( int i = 0; i < sampleNormalizedSorted.length; i++ )
                statistic += (double)(i + 1) * sampleNormalizedSorted[i];
            statistic = 2.0 * (double)sampleNormalizedSorted.length - 2.0 * statistic / (double)sampleNormalizedSorted.length;
            statistic = 1.0 - statistic / (double)(sampleNormalizedSorted.length - 1);

            // 2. Calculate statisticAsymptotic and pValue
            double statisticAsymptotic = (statistic - 0.5) * Math.sqrt(12.0 * (double)(sample.length - 1));
            double pValue = 2.0 * (1.0 - Stat.standartNormalDistribution(Math.abs(statisticAsymptotic)));
            return new double[]{statistic, statisticAsymptotic, pValue};
        }
        
        public static double[] getCoxAndOakesTest(double[] sample)
        {
            double mean = Stat.mean(sample), statistic = (double)sample.length;
            double[] sampleNormalized = MatrixUtils.getProductOfVectorAndScalar(sample, 1.0 / mean);
            for( double x : sampleNormalized )
                statistic += (1.0 - x) * Math.log(x);
            double statisticAsymptotic = statistic * Math.sqrt(6.0 / (double)sample.length) / Math.PI;
            double pValue = 2.0 * (1.0 - Stat.standartNormalDistribution(Math.abs(statisticAsymptotic)));
            return new double[]{statistic, statisticAsymptotic, pValue};
        }
    }
    /********************* TestsForExponentiality : finish **********************/
    
    /********************* PopulationSize : start **********************/
    //////// it is copies !!!!!!!
    public static class PopulationSize
    {
        // count1 := size of 1-st set; count2:= size of 2-nd set; count12 := size of overlaps of 1-st and 2-nd sets 
        public static double getPopulationSizeChapman(int count1, int count2, int count12)
        {
            return (double)(count1 + 1) * (double)(count2 + 1) / (double)(count12 + 1) - 1.0;
        }
        
        // Orphans are the individuals that observed exactly in one sample.
        public static double getRatioOfObservedToExpectedNumberOfOrphans(int f1, int f2, int f3)
        {
            if( f2 <= 0 ) return Double.NaN;
            return 1.5 * (double)f1 * (double)f3 / ((double)(f2) * (double)f2);
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
                sum += (double)(i + 1) * freq[i];
                n += (double)freq[i];
            }
            double lambda = -Math.log(1.0 - n / populationSizeInitialApproximation);

            // 2. Newton's method for calculation of lambda 
            for( int i = 0; i < maxNumberOfIterations; i++ )
            {
                double x = Math.exp(-lambda), xx = 1.0 - x, y = sum / lambda, yy = n / xx, delta = (y - yy) / (-y / lambda + yy * x / xx); 
                if( Math.abs(delta) < epsilon ) break;
                lambda -= delta;
            }
            double populationSize = n / (1.0 - Math.exp(-lambda)), z = sum / (double)populationSize;
            return new double[]{populationSize, Math.sqrt((double)populationSize / (Math.exp(z) - z - 1.0))};
        }
        
        public static double[] getPopulationSizes(int[] freq, int numberOfFunSites, int maxNumberOfIterations)
        {
            double populationSizeChao = getPopulationSizeAndSigmaChao(freq[0], freq[1], numberOfFunSites)[0];
            return new double[]{populationSizeChao,
                                getPopulationSizeAndSigmaLanumteangBohning(freq[0], freq[1], freq[2], numberOfFunSites)[0],
                                getPopulationSizeAndSigmaZelterman(freq[0], freq[1], numberOfFunSites)[0],
                                getPopulationSizeAndSigmaMaximumLikelihood(freq, populationSizeChao, maxNumberOfIterations, 1.0e-5)[0]};
        }

    }
    /********************* PopulationSize : finish **********************/
    
    private static Logger log = Logger.getLogger(StatUtil.class.getName());
}
