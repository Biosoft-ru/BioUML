package ru.biosoft.analysis;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;


/**
 * Different statistical functions
 * 
 * @author shadrin
 * 
 */
public class Stat
{
    public static final double SQRT_OF_2PI = 2.5066282746310007;
    private static final int LOG_GAMMA_CACHE_SIZE = 16384;

    private Stat()
    {
    }

    /**
     * Log of number of combinations by k elements from N
     * 
     * @param N -
     *            number of objects
     * @param k -
     *            number of retrieved objects
     * @return log of number of sample combinations with length k from N elements
     * 
     * @throws IllegalArgumentException
     *             if N < 0 or k < 0 or N < k
     */
    public static double logCnk(int N, int k) throws IllegalArgumentException
    {
        if( N < 0 || k < 0 || N < k )
            throw new IllegalArgumentException("Invalid Arguments for logCnk");
        return logGamma(N + 1) - ( logGamma(k + 1) + logGamma(N - k + 1) );
    }

    /**
     * Log of binomial distribution
     * (probability to get exactly M marked elements from N elements when probability of marking is prob)
     * 
     * @param N - total number of elements
     * @param M - number of marked elements
     * @param prob - probability for element to being marked (0..1)
     * @throws Exception
     */
    public static double logBinomialDistribution(int N, int M, double prob) throws Exception
    {
        if( N == 0 )
            return 0;
        if( M == 0 )
            return N * Math.log(1.0 - prob);
        return logCnk(N, M) + ( M * Math.log(prob) ) + ( ( N - M ) * Math.log(1.0 - prob) );
    }

    /**
     * Inverse cumulative binomial distribution (find M by N, prob and value)
     */
    public static int cumulativeBinomialInv(double cdf, int N, double prob) throws Exception
    {
        if( cdf < 0 || cdf > 1 )
            throw new Exception("CDF must >= 0 or <= 1");
        double cdf2 = 0;
        int x;
        for( x = 0; x <= N; x++ )// in xrange(0,a+1):
        {
            double pdf = Math.exp(logBinomialDistribution(N, x, prob));
            cdf2 += pdf;
            if( cdf < cdf2 )
                return x;
        }
        return x;
    }

    /**
     * Cumulative binomial distribution
     * 
     * @param N - total number of elements
     * @param M - number of marked elements
     * @param prob - probability for element to being marked (0..1)
     * 
     * @return array of two doubles
     * 1st: probability of having underrepresentation
     * 2nd: probability of having overrepresentation
     * @throws Exception
     * 
     * Note that sum of return values should always equals to 1, but if one of them is too small,
     * the other may experience rounding errors
     */
    public static double[] cumulativeBinomial(int N, int M, double prob) throws Exception
    {
        if( prob <= 0 )
            return new double[] {0, 1};
        if( prob >= 1 )
            return new double[] {1, 0};

        double expected = N * prob;
        double left = 0, right = 0;
        int bound = M;
        if( M < expected )
        {
            for( int i = 0; i <= bound; i++ )
            {
                double logBin = logBinomialDistribution(N, i, prob);
                if(logBin < -746)
                {
                    int left0 = i, right0 = bound;
                    while(right0 - left0 > 1)
                    {
                        i = (left0+right0)/2;
                        logBin = logBinomialDistribution(N, i, prob);
                        if(logBin < -746) left0 = i; else right0 = i;
                    }
                }
                left += Math.exp(logBin);
            }
            right = 1 - left;
        }
        else
        {
            for( int i = N; i >= bound; i-- )
            {
                double logBin = logBinomialDistribution(N, i, prob);
                if(logBin < -746)
                {
                    int left0 = bound, right0 = i;
                    while(right0 - left0 > 1)
                    {
                        i = (left0+right0+1)/2;
                        logBin = logBinomialDistribution(N, i, prob);
                        if(logBin < -746) right0 = i; else left0 = i;
                    }
                }
                right += Math.exp(logBin);
            }
            left = 1 - right;
        }
        return new double[] {left, right};
    }
    
    public static double[] cumulativeBinomialFast(int N, int M, double prob)
    {
        if( prob <= 0 )
            return new double[] {0, 1};
        if( prob >= 1 )
            return new double[] {1, 0};

        double expected = N * prob;
        if( M >= expected ) M--;
        double[] betaDistribution = betaDistribution(prob, M+1, N-M);
        double tmp = betaDistribution[0];
        betaDistribution[0] = betaDistribution[1];
        betaDistribution[1] = tmp;
        return betaDistribution;
    }
    
    /***
     * 
     * @param sample1
     * @param sample2
     * @return array: array[0] = test statistic (number of positive differences), array[1] = p-value;
     * @throws Exception
     */
    public static double[] binomialTestTwoSided(double[] sample1, double[] sample2) throws Exception
    {
        int n = 0;
        int m = 0;
        for( int i = 0; i < sample1.length; i++ )
            if( sample1[i] != sample2[i] )
            {
                n++;
                if( sample1[i] > sample2[i] )
                    m++;
            }
        int k = Math.max(m, n - m);
        double[] result = cumulativeBinomialFast(n, k, 0.5);
        result[0] = m;
        result[1] *= 2.0;
        return result;
    }

    /**
     * Log of hypergeometrical distribution
     * 
     * @param N -
     *            total number of elements,
     * @param M -
     *            number of marked elements,
     * @param m -
     *            number of pulled elements,
     * @param x -
     *            number of marked elements among pulled
     */
    public static double logHyperDistribution(int N, int M, int m, int x) throws Exception
    {
        try
        {
            return ( logCnk(M, x) + logCnk(N - M, m - x) ) - logCnk(N, m);
        }
        catch( Exception e )
        {
            throw new Exception("Wrong arguments for hypergeometrical distribution");
        }
    }

    /**
     * Cumulative hypergeometric distribution
     * 
     * @param N -
     *            total number of elements,
     * @param M -
     *            number of marked elements,
     * @param sN -
     *            number of pulled elements,
     * @param sM -
     *            number of marked elements among pulled
     */
    public static double[] cumulativeHypergeometric(int N, int M, int sN, int sM) throws Exception
    {
        int minN = Math.max(sN - ( N - M ), 0);
        int maxN = Math.min(sN, M);
        double left = 0, right = 0, sMval = Math.exp(logHyperDistribution(N, M, sN, sM));
        double expected = (double)sN * M / N;

        if( sM < expected )
        {
            for( int i = minN; i <= sM; i++ )
            {
                left += Math.exp(logHyperDistribution(N, M, sN, i));
            }
            right = 1 - left + sMval;
        }
        else
        {
            for( int i = maxN; i >= sM; i-- )
            {
                right += Math.exp(logHyperDistribution(N, M, sN, i));
            }
            left = 1 - right + sMval;
        }
        return new double[] {left, right};
    }
    
//---------------------------------------------------------------------------------------------
    /**
     * Adjust a list of P-values.
     * 
     * @param   pvals - P-values to be adjusted
     * @param   total - Total number of tests, in case the P-value list is cut off.
     * @return  Array of corresponding FDRs in same order as P-values of input array.
     */
    public static double[] adjustPvalues(double[] pvals,int total)
    {
        int N = pvals.length;
        int D = total-N;
        double[] fdrs = new double[N];
        int[] spos = sortIdx(pvals);
        double fdr = 1.0;
        double vp;
        for (int p = 0;p < N;++p)
        {
                vp = pvals[spos[p]];
                if (p == 0)
                {
                        fdr = Math.min(1.0,(total*vp)/(total-D));
                }
                else if (vp < pvals[spos[p-1]])
                {
                        fdr = Math.min(fdr,(total*vp)/(total-(D+p)));
                }
                fdrs[spos[p]] = fdr;
        }
        return fdrs;
    }
    
    /**
     * Indexes of list elements sorting them in descending order.
     * 
     * @param  V - values to sort
     * @return array of V element indexes sorting V in descending order
     */
    public static int[] sortIdx(double[] V)
    {
        int[] vpos = new int[V.length];
        for (int i = 0;i < V.length;++i)
        {
            vpos[i] = i;
        }
        int t = V.length;
        int nt, z;
        do
        {
            nt = 1;
            for (int i = 0;i < t-1;++i)
            {
                if (V[vpos[i]] < V[vpos[i+1]])
                {
                    z = vpos[i];
                    vpos[i]   = vpos[i+1];
                    vpos[i+1] = z;
                    nt = i+1;
                }
            }
            t = nt;
        }
        while (t > 1);
        return vpos;
    }
//---------------------------------------------------------------------------------------------
    
    
    /**
     * Sample mean
     */
    public static double mean(double[] sample)
    {
        double sum = 0;
        for( double s : sample )
            sum += s;
        return sum / sample.length;
    }
    
    /**
     * Sample mean
     */
    public static double mean(Double[] sample)
    {
        double sum = 0;
        for( double s : sample )
            sum += s;
        return sum / sample.length;
    }
    
    /**
     * Sample mean
     */
    public static double mean(List<Double> sample)
    {
        double sum = 0;
        for( double s : sample )
            sum += s;
        return sum / sample.size();
    }

    /**
     * Sample mean of any power
     */
    public static double mean(double[] sample, int power)
    {
        double sum = 0;
        for( double s : sample )
            sum += Math.pow(s, power);
        return sum / sample.length;
    }

    /**
     * Sample variance
     * 
     * @returns estimate for variance found with maximum likelihood method
     * @param isShifted:
     *            if true then will return shifted estimate
     */
    public static double variance(double[] sample, boolean isShifted)
    {
        double sum = 0;
        double mean = mean(sample);
        for( double s : sample )
        {
            double diff = s - mean;
            sum += diff * diff;
        }
        if( isShifted )
            return sum / sample.length;
        else
            return sum / ( sample.length - 1 );
    }
    
    public static double variance(double[] sample)
    {
        return variance(sample, false);
    }
    
    public static double covariance(double[] sample1, double[] sample2)
    {
        double sum = 0;
        double mean1 = mean(sample1);
        double mean2 = mean(sample2);
        for( int i = 0; i < sample1.length; i++ )
            sum += ( sample1[i] - mean1 ) * ( sample2[i] - mean2 );
        return sum / ( sample1.length - 1 );
    }

    public static double[][] variance(double[][] sample)
    {
        sample = Util.matrixConjugate(sample);
        int m = sample.length;
        double[][] result = new double[m][m];
        
        for( int i = 0; i < m; i++ )
            result[i][i] = variance(sample[i]);

        for( int i = 0; i < m; i++ )
            for( int j = i + 1; j < m; j++ )
                result[i][j] = result[j][i] = covariance(sample[i], sample[j]);

        return result;
    }

    /***
     * Calculation of mean value and sigma for given sample X[].
     * @param X
     * @return array 'result': result[0] = mean; result[1] = sigma
     */
    public static double[] getMeanAndSigma(double[] X)
    {
        double[] result = new double[2];
        result[0] = result[1] = 0.0;
        if( X.length < 1) return result;
        for( double x : X )
            result[0] += x;
        if( X.length < 2) return result;
        result[0] /= X.length;
        for( double x : X )
        {
           double y = x - result[0];
           result[1] += y * y;
        }
        result[1] /= X.length - 1;
        result[1] = Math.sqrt(result[1]);
        return result;
    }
    
    ///////////////////////////////O.K.
    // Calculation of mean value and sigma for given sample.
    public static double[] getMeanAndSigma1(List<Double> sample)
    {
        if( sample.isEmpty() ) return null;
        int n = sample.size();
        if( n < 2)  return null;
        double[] result = new double[]{0.0, 0.0};
        for( Double x : sample )
            result[0] += x;
        result[0] /= n;
        for( Double x : sample )
        {
           double y = x - result[0];
           result[1] += y * y;
        }
        result[1] /= n - 1;
        result[1] = Math.sqrt(result[1]);
        return result;
    }
    
    public static double[] getMinAndMax(double[] sample)
    {
        double[] result = new double[2];
        result[0] = result[1] = sample[0];
        for( int i = 1; i < sample.length; i++ )
        {
            result[0] = Math.min(result[0], sample[i]);
            result[1] = Math.max(result[1], sample[i]);
        }
        return result;
    }
    
    public static double[] getMinAndMax(List<Double> sample)
    {
        double[] result = new double[2];
        result[0] = result[1] = sample.get(0);
        for( double x : sample )
        {
            result[0] = Math.min(result[0], x);
            result[1] = Math.max(result[1], x);
        }
        return result;
    }
    
    /***
     * Calculation of mean value and sigma (standard deviation) for given sample List<Integer>.
     * @param X
     * @return double[]{mean, sigma}
     */
    public static double[] getMeanAndSigma(List<Integer> x)
    {
        double[] result = new double[2];
        result[0] = result[1] = 0.0;
        if( x.size() < 1) return result;
        for( Integer xx : x )
            result[0] += (double)xx;
        if( x.size() < 2) return result;
         result[0] /= (x.size());
        for( Integer xx : x )
        {
           double y = (double)xx - result[0];
           result[1] += y * y;
        }
        result[1] /= (x.size() - 1);
        result[1] = Math.sqrt(result[1]);
        return result;
    }
    
    public static double getMeanValue(List<Integer> sample)
    {
        double result = 0;
        if( sample == null || sample.size() < 1 ) return result;
        for( Integer i : sample )
            result += (double)i;
        result /= sample.size();
        return result;
    }
    
    /***
     * Birth-and-Death process.
     * Estimation of birth rates and death rates.
     * @param populationSizesAndTimesWaitingForBirth : "Map(Integer, List(Integer))" contains the population sizes Integer and corresponding samples List(Integer} of times waiting for birth.
     * @param populationSizesAndTimesWaitingForDeath : "Map(Integer, List(Integer))" contains the population sizes Integer and corresponding samples List(Integer} of times waiting for death.
     * @return "Map(Integer, double[])", where Integer is population size, corresponding double[0] contains the birth rate
     * and corresponding  double[1] contains the death rate.
     */
    public static Map<Integer, double[]> getPopulationSizeAndBirthAndDeathRates(Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth, Map<Integer, List<Integer>> populationSizesAndTimesWaitingForDeath)
    {
        Map<Integer, double[]> result = new HashMap<>();
        int maxPopulationSize = -1;
        for( int populationSize : populationSizesAndTimesWaitingForDeath.keySet() )
        {
            if( populationSize > maxPopulationSize )
                maxPopulationSize = populationSize;
        }
        for( int j = 0; j <= maxPopulationSize; j++ )
        {
            Integer populationSize = j;
            double x[] = new double[2];
            double meanWaitingTime =  Stat.getMeanValue(populationSizesAndTimesWaitingForBirth.get(populationSize));
            if( meanWaitingTime > 0 )
                x[0] = 1.0 / meanWaitingTime;
            else
                x[0] = 0.0;
            meanWaitingTime = Stat.getMeanValue(populationSizesAndTimesWaitingForDeath.get(populationSize));
            if( meanWaitingTime > 0 )
                x[1] = 1.0 / meanWaitingTime;
            else
                x[1] = 0.0;
            result.put(populationSize, x);
        }
        return result;
    }

    /***
     * Birth-and-Death process.
     * Calculation of steady-state probabilities of population sizes (equally, probabilities that system is in given states)
     * @param populationSizeAndBirthAndDeathRates : this Map(Integer, double[]) contains the population size (Integer)
     * and corresponding birth rate (double[0]) and death rate (double[1])
     * @return "Map(Integer, Double)", where Integer is population size and corresponding Double is probability
     */
    public static Map<Integer, Double> getTheoreticalProbabilitiesOfPopulationSizes(Map<Integer, double[]> populationSizeAndBirthAndDeathRates)
    {
        Map<Integer, Double> result = new HashMap<>();
        double[] probabilities = new double[populationSizeAndBirthAndDeathRates.size()];
        double probability = 1.0;
        for( int i = 0; i < populationSizeAndBirthAndDeathRates.size() - 1; i++ )
        {
            Integer populationSize = i;
            double birthRate = populationSizeAndBirthAndDeathRates.get(populationSize)[0];
            populationSize = i + 1;
            double deathRate = populationSizeAndBirthAndDeathRates.get(populationSize)[1];
            probability *= birthRate / deathRate;
            probabilities[i + 1] = probability;
        }
        probabilities[0] = 1.0;
        for( int i = 1; i < populationSizeAndBirthAndDeathRates.size(); i++ )
            probabilities[0] += probabilities[i];
        probabilities[0] = 1.0 / probabilities[0];
        for( int i = 1; i < populationSizeAndBirthAndDeathRates.size(); i++ )
            probabilities[i] *= probabilities[0];
        for( int i = 0; i < populationSizeAndBirthAndDeathRates.size(); i++ )
            result.put(i, probabilities[i]);
        return result;
    }

    /***
     * Calculation of histogram for non-negative integer-valued sample.
     * The interval [0,maxValue) is divided on numberOfGroups sub-intervals; each of them has length width = maxValue / numberOfGroups.
     * [0, width), [width, 2*width), [2*width, 3*width},...,[(numberOfGroups-1)*width, numberOfGroups8width}.
     * Then histogram[i] is equal to the number of sample elements that belong to i-th sub-interval
     * [i*width, (i+1)*width), i=0,...,numberOfGroups-1.
     * @param sample
     * @param maxvalue
     * @param numberOfGroups
     * @return histogram[]>=0;
     */
    public static int[] getHistogram(List<Integer> sample, int maxValue, int numberOfGroups)
    {
        int[] histogram = new int[numberOfGroups];
        int width = maxValue / numberOfGroups;
        for( int i = 0; i < numberOfGroups; i++ )
            histogram[i] = 0;
        if( width <= 0 )
            return histogram;
        for( Integer integer : sample )
        {
            int i = integer;
            i /= width;
            if( i < numberOfGroups )
            histogram[i]++;
        }
        return histogram;
    }
    
    public static int[] getHistogram(List<Integer> sample, int numberOfGroups)
    {
        int maxValue = sample.get(0);
        for( Integer i : sample )
            if( i > maxValue )
                maxValue = i;
        return getHistogram(sample, maxValue, numberOfGroups);
    }
    
    /***
     * Calculation of statistic and p-value of Chi-squared test for exponentiality.
     * The interval [0,max] is divided on 'numberOfIntervals' intervals, where max is maximal element of sample.
     * @param sample: all sample elements have to be non-negative
     * @param numberOfIntervals: number of intervals >= 2
     * @return double [] = {chi-squared test statistic, p-value}
     * @throws Exception
     */
    public static double[] getStatisticAndPvalueOfChiSquaredTestForExponentiality(List<Integer> sample, int numberOfIntervals) throws Exception
    {
        double[] result = new double[2];
        result[0] = 0.0;
        result[1] = 1.0;
        if( sample == null || sample.size() < 1 || numberOfIntervals <= 2 )
            return result;
        int max = -1;
        for( Integer i : sample )
        {
            int sampleElement = i;
            if( sampleElement > max ) max = sampleElement;
        }
        int width = ++max / numberOfIntervals;
        if( width < 1 )
            return result;
        int[] histogram = new int[numberOfIntervals];
        for( int j = 0; j < numberOfIntervals; j++ )
            histogram[j] = 0;
        int correctedSizeOfSample = 0;
        for( Integer i : sample )
        {
            int sampleElement = i;
            int indexOfInterval = sampleElement / width;
            if( indexOfInterval >= numberOfIntervals ) continue;
            correctedSizeOfSample++;
            histogram[indexOfInterval]++;
        }
        double mean = getMeanValue(sample);
        if( mean == 0 )
            return result;
        double parameterOfExponentialDistribution = 1.0 / mean;
        double constant = 1.0 - Math.exp(-parameterOfExponentialDistribution * width);
        for( int j = 0; j < numberOfIntervals; j++ )
        {
            double probability = constant * Math.exp(-parameterOfExponentialDistribution * width * j);
            double x = probability * sample.size();
            double xx = x - histogram[j];
            result[0] += xx * xx / x;
        }
        if( correctedSizeOfSample < sample.size() )
        {
            double probability = Math.exp(-parameterOfExponentialDistribution * width * numberOfIntervals);
            double x = probability * sample.size();
            double xx = x - (sample.size() - correctedSizeOfSample);
            result[0] += xx * xx / x;
        }
        double degreesOfFredom = numberOfIntervals - 2;
        if( correctedSizeOfSample < sample.size() )
            degreesOfFredom += 1.0;
        result[1] = 1.0 - chiDistribution(result[0], degreesOfFredom);
        return result;
    }
    
    /***
     * Expectation Step in EM-algorithm for identification of k-component exponential mixture.
     * The probabilities Pij are defined as Pij = is the probability that i-th element of sample belongs to j-th component of exponential mixture.
     * 
     * @param exponentialParameters: dimension(exponentialParameters)=k;
     * @param probabilitiesOfMixtureComponents: dimension(probabilitiesOfMixtureComponents)=k;
     * @param sample
     * @return probabilities Pij
     */
    public static double[][] getProbabilitiesPijForExponentialMixture(double[] exponentialParameters, double[] probabilitiesOfMixtureComponents, List<Integer> sample)
    {
        double[][] probabilitiesPij = IntStreamEx.of( sample )
                .mapToObj( x -> DoubleStreamEx.zip( probabilitiesOfMixtureComponents, exponentialParameters,
                                (prob, exp) -> prob * exp * Math.exp( -exp * x ) ).toArray() )
                .toArray( double[][]::new );
        for( int i = 0; i < sample.size(); i++ )
        {
            double sum = DoubleStreamEx.of( probabilitiesPij[i] ).sum();
            for( int j = 0; j < exponentialParameters.length; j++ )
                probabilitiesPij[i][j] /= sum;
        }
        return probabilitiesPij;
    }

    /***
     * Expectation Step in EM-algorithm for identification of k-component mixture.
     * Mixture consists of k1 exponential and k2 normal distributions, k = k1 + k2
     * The probabilities Pij are defined as Pij = is the probability that i-th element of sample belongs to j-th component of mixture.
     * 
     * @param exponentialParameters
     * @param meansAndSigmas
     * @param probabilitiesOfMixtureComponents
     * @param sample
     * @return
     */
    public static double[][] getProbabilitiesPijForExponentialNormalMixture(double[] exponentialParameters, double[][] meansAndSigmas, double[] probabilitiesOfMixtureComponents, List<Integer> sample)
    {
        int k1 = exponentialParameters.length;
        double[][] probabilitiesPij = new double[sample.size()][ probabilitiesOfMixtureComponents.length ];
        for( int i = 0; i < sample.size(); i++ )
        {
            int ii = sample.get(i);
            double x = ii;
            for( int j = 0; j < k1; j++ )
                probabilitiesPij[i][j] = probabilitiesOfMixtureComponents[j] * getExponentialDensity(x, exponentialParameters[j]);
            for( int j = 0; j < meansAndSigmas.length; j++ )
                probabilitiesPij[i][j + k1] = probabilitiesOfMixtureComponents[j + k1] * getNormalDensity(x, meansAndSigmas[j][0], meansAndSigmas[j][1]);
        }
        for( int i = 0; i < sample.size(); i++ )
        {
            double sum = 0.0;
            for( int j = 0; j < probabilitiesOfMixtureComponents.length; j++ )
                sum += probabilitiesPij[i][j];
            for( int j = 0; j < probabilitiesOfMixtureComponents.length; j++ )
                probabilitiesPij[i][j] /= sum;
        }
        return probabilitiesPij;
    }

    /***
     * Maximization Step in EM-algorithm for identification of k-component exponential mixture.
     * 
     * @param probabilitesPij: Each probability Pij is the probability that i-th element of sample belongs to j-th component of exponential mixture
     * @param sample: consists of observations of exponential mixture
     * @return double[][] {probabilitiesOfMixtureComponents, exponentialParameters}
     */
    public static double[][] getParametersOfExponentialMixture(double[][] probabilitiesPij, List<Integer> sample)
    {
        int n = probabilitiesPij.length;
        int k = probabilitiesPij[0].length;
        double[] exponentialParameters = new double[k];
        double[] probabilitiesOfMixtureComponents = new double[k];
        for( int j = 0; j < k; j++ )
        {
            double sum1 = 0.0;
            double sum2 = 0.0;
            for( int i = 0; i < n; i++ )
            {
                sum1 += probabilitiesPij[i][j];
                sum2 += probabilitiesPij[i][j] * sample.get(i);
            }
            probabilitiesOfMixtureComponents[j] = sum1 / n;
            exponentialParameters[j] = sum1 / sum2;
        }
        return new double[][] {probabilitiesOfMixtureComponents, exponentialParameters};
    }

    /****
     * Maximization Step in EM-algorithm for identification of k-component mixture.
     * Mixture consists of k components: among them numberOfExponentialComponents components are  exponential
     * and k2 = k - numberOfExponentialComponents components are normal distributions.
     * 
     * @param probabilitiesPij
     * @param numberOfExponentialComponents
     * @param sample
     * @return Object[], dimension = 3;
     * Object[0] = double[numberOfExponentialComponents] exponentialParameters
     * Object[1] = double[k2][2] meansAndSigmas
     * Object[2] = double[k] probabilitiesOfMixtureComponents
     */
    public static Object[] getParametersOfExponentialNormalMixture(double[][] probabilitiesPij, int numberOfExponentialComponents, List<Integer> sample)
    {
        int n = probabilitiesPij.length;
        int k = probabilitiesPij[0].length;
        int k2 = k - numberOfExponentialComponents;
        double[] exponentialParameters = new double[numberOfExponentialComponents];
        double[][] meansAndSigmas = new double[k2][2];
        double[] probabilitiesOfMixtureComponents = new double[k];
        for( int j = 0; j < numberOfExponentialComponents; j++ )
        {
            double sum1 = 0.0;
            double sum2 = 0.0;
            for( int i = 0; i < n; i++ )
            {
                sum1 += probabilitiesPij[i][j];
                sum2 += probabilitiesPij[i][j] * sample.get(i);
            }
            probabilitiesOfMixtureComponents[j] = sum1 / n;
            exponentialParameters[j] = sum1 / sum2;
        }
        for( int j = 0; j < k2; j++ )
        {
            double sum1 = 0.0;
            double sum2 = 0.0;
            double sum3 = 0.0;
            int jj = numberOfExponentialComponents + j;
            for( int i = 0; i < n; i++ )
            {
                double x = sample.get(i);
                sum1 += probabilitiesPij[i][jj];
                double xx = probabilitiesPij[i][jj] * x;
                sum2 += xx;
                sum3 += xx * x;
            }
            probabilitiesOfMixtureComponents[jj] = sum1 / n;
            meansAndSigmas[j][0] = sum2 / sum1;
//          meansAndSigmas[j][1] = (sum3 - 2.0 * meansAndSigmas[j][0] * sum2 + meansAndSigmas[j][0] * meansAndSigmas[j][0] * sum3) / sum1;
            meansAndSigmas[j][1] = Math.sqrt((sum3 - 2.0 * meansAndSigmas[j][0] * sum2) / sum1 + meansAndSigmas[j][0] * meansAndSigmas[j][0]);
        }
        return new Object[] {exponentialParameters, meansAndSigmas, probabilitiesOfMixtureComponents};
    }

    /***
     * Full iterative EM-algorithm for identification of k-component exponential mixture.
     * Each iteration consists of 2 steps: Expectation Step and Maximization Step.
     * Input parameters exponentialParameters and probabilitiesOfMixtureComponents represent the initial approximations.
     * These parameters will be changed: in Output they will represent optimal estimations.
     * 
     * @param exponentialParameters initial approximations of k parameters of k exponential distributions; is changed in Output; new values are estimated parameters of k exponential distributions.
     * @param probabilitiesOfMixtureComponents: k probabilities of mixture components; is changed in Output;
     * @param sample: List<Integer>
     * @param maximalNumberOfIterations: is changed in Output: new value is actual number of performed iterations;
     * @return probabilitiesPij: Each probability Pij is the probability that i-th element of sample belongs to j-th component of exponential mixture;
     */
    public static double[][] estimateExponentialMixtureBy_EM_Algorithm(double[] exponentialParameters, double[] probabilitiesOfMixtureComponents, List<Integer> sample, int maximalNumberOfIterations)
    {
       double[][] probabilitiesPij = getProbabilitiesPijForExponentialMixture(exponentialParameters, probabilitiesOfMixtureComponents, sample);
       for( int numberOfIterations = 1; numberOfIterations <= maximalNumberOfIterations; numberOfIterations++ )
       {
           if( numberOfIterations > 1 )
               probabilitiesPij = getProbabilitiesPijForExponentialMixture(exponentialParameters, probabilitiesOfMixtureComponents, sample);
           double[][] probabilitiesAndParametersOfMixtureComponents = getParametersOfExponentialMixture(probabilitiesPij, sample);
           double[] newProbabilitiesOfMixtureComponens = probabilitiesAndParametersOfMixtureComponents[0];
           double[] newExponentialParameters = probabilitiesAndParametersOfMixtureComponents[1];
           boolean isEqual = true;
           for( int j = 0; j < exponentialParameters.length; j++ )
               if( exponentialParameters[j] != newExponentialParameters[j] || probabilitiesOfMixtureComponents[j] != newProbabilitiesOfMixtureComponens[j])
               {
                   isEqual = false;
                   break;
               }
           if( isEqual )
           {
               return probabilitiesPij;
           }
           for( int j = 0; j < exponentialParameters.length; j++ )
           {
               exponentialParameters[j] = newExponentialParameters[j];
               probabilitiesOfMixtureComponents[j] = newProbabilitiesOfMixtureComponens[j];
           }
       }
       probabilitiesPij = getProbabilitiesPijForExponentialMixture(exponentialParameters, probabilitiesOfMixtureComponents, sample);
       return probabilitiesPij;
    }

    /***
     * Full iterative EM-algorithm for identification of k-component mixture.
     * Mixture consists of k components: among them numberOfExponentialComponents (or k1) components are  exponential
     * and k2 = k - numberOfExponentialComponents components are normal distributions.
     * Each iteration consists of 2 steps: Expectation Step and Maximization Step.
     * 
     * @param exponentialParameters contain initial approximations of k1 parameters of k1 exponential distributions.
     * @param meansAndSigmas contain contain initial approximations of means and sigmas of k2 normal distributions.
     * @param probabilitiesOfMixtureComponents: k probabilities of mixture components.
     * @param sample: List<Integer>
     * @param maximalNumberOfIterations
     * @return Object[];
     * Object[0] = newExponentialParameters;
     * Object[1] = newMeansAndSigmas;
     * Object[2] = newProbabilitiesOfMixtureComponents;
     * Object[3] = numberOfIterations;
     * Object[4] = probabilitiesPij; Each probability Pij is the probability that i-th element of sample belongs to j-th component of mixture;
     */
    public static Object[] estimateExponentialNormalMixtureBy_EM_Algorithm(double[] exponentialParameters, double[][] meansAndSigmas, double[] probabilitiesOfMixtureComponents, List<Integer> sample, int maximalNumberOfIterations)
    {
       double[][] probabilitiesPij = getProbabilitiesPijForExponentialNormalMixture(exponentialParameters, meansAndSigmas, probabilitiesOfMixtureComponents, sample);
       double[] newExponentialParameters = null;
       double[][] newMeansAndSigmas = null;
       double[] newProbabilitiesOfMixtureComponents = null;
       double[] oldProbabilitiesOfMixtureComponents = probabilitiesOfMixtureComponents.clone();
       int numberOfIterations;
       for( numberOfIterations = 1; numberOfIterations <= maximalNumberOfIterations; numberOfIterations++ )
       {
           if( numberOfIterations > 1 )
               probabilitiesPij = getProbabilitiesPijForExponentialNormalMixture(newExponentialParameters, newMeansAndSigmas, newProbabilitiesOfMixtureComponents, sample);
           Object[]  objects = getParametersOfExponentialNormalMixture(probabilitiesPij, exponentialParameters.length, sample);
           newExponentialParameters = (double[])objects[0];
           newMeansAndSigmas = (double[][])objects[1];
           newProbabilitiesOfMixtureComponents = (double[])objects[2];
           boolean isEqual = true;
           for( int j = 0; j < probabilitiesOfMixtureComponents.length; j++ )
               if( newProbabilitiesOfMixtureComponents[j] != oldProbabilitiesOfMixtureComponents[j])
               {
                   isEqual = false;
                   break;
               }
           if( isEqual ) break;
           oldProbabilitiesOfMixtureComponents = newProbabilitiesOfMixtureComponents.clone();
       }
       if( numberOfIterations > maximalNumberOfIterations )
           numberOfIterations = maximalNumberOfIterations;
       return new Object[] {newExponentialParameters, newMeansAndSigmas, newProbabilitiesOfMixtureComponents, numberOfIterations, probabilitiesPij};
    }

    /***
     * Divide the sample (List<Integer>) into k sub-samples by using random value generator and probabilities Pij,
     * where Pij = the probability that i-th element of sample belongs to j-th sub-sample, j=0,...,k-1.
     * @param probabilitiesPij
     * @param sample
     * @return subsamples (Map<Integer, List<Integer>>)
     */
    public static List<List<Integer>> getSubsamplesSimulatedByProbabilitiesPij(double[][] probabilitiesPij, List<Integer> sample)
    {
        int numberOfSubsamples = probabilitiesPij[0].length;
        List<List<Integer>> result = IntStreamEx.range( numberOfSubsamples ).<List<Integer>>mapToObj( j -> new ArrayList<>() ).toList();
        for( int i = 0; i < sample.size(); i++ )
        {
            double randomValue = Math.random();
            double sum = 0.0;
            for( int j = 0; j < numberOfSubsamples; j++ )
            {
                sum += probabilitiesPij[i][j];
                if( sum >= randomValue )
                {
                    result.get(j).add(sample.get(i));
                    break;
                }
            }
        }
        return result;
    }
    
    /***
     * 
     * @param sample
     * @param numberOfMixtureComponents >=1
     * @param numberOfIntervals
     * @param maximalNumberOfIterations (it is needed for for iterative iterative EM-algorithm for identification of k-component exponential mixture)
     * @param populationSizesAndTimesWaitingForBirth
     * @return double[] result, where dimension(result) = 5 * numberOfMixtureComponents
     * result[0],...,result[numberOfMixtureComponents-1] are mean values in mixture components 0,...,numberOfMixtureComponents-1
     * result[1 * numberOfMixtureComponents],...,result[2 * numberOfMixtureComponents - 1] are sizes of mixture components 0,...,numberOfMixtureComponents-1
     * result[2 * numberOfMixtureComponents],...,result[3 * numberOfMixtureComponents - 1] are simulated mean values in mixture components  0,...,numberOfMixtureComponents-1
     * result[3 * numberOfMixtureComponents],...,result[4 * numberOfMixtureComponents - 1] are simulated sigmas in mixture components  0,...,numberOfMixtureComponents-1
     * result[4 * numberOfMixtureComponents],...,result[5 * numberOfMixtureComponents - 1] are p-values of exponentialities of mixture components  0,...,numberOfMixtureComponents-1
     * 
     * @throws Exception
     */
    public static double[] getExponentialMixtureForSample(List<Integer> sample, int numberOfMixtureComponents, int numberOfIntervals, int maximalNumberOfIterations) throws Exception
    {
        double[] result = new double[5 * numberOfMixtureComponents];
        double[] exponentialParameters = new double[numberOfMixtureComponents];
        double[] probabilitiesOfMixtureComponents = new double[numberOfMixtureComponents];
        if( sample != null && sample.size() > 10 && sample.size() > 3 * numberOfMixtureComponents)
        {
            int max = -1;
            for( Integer i : sample )
            {
                int ii = i;
                if( max < ii )
                    max = ii;
            }
            int h = max / (numberOfMixtureComponents + 1);
            for( int i = 1; i <= numberOfMixtureComponents; i++ )
            {
                exponentialParameters[i - 1] = 1.0 / (i * h);
                probabilitiesOfMixtureComponents[i - 1] = 1.0 / numberOfMixtureComponents;
            }
            int numberOfIterations = maximalNumberOfIterations;
            double[][] probabilitiesPij = estimateExponentialMixtureBy_EM_Algorithm(exponentialParameters, probabilitiesOfMixtureComponents, sample, numberOfIterations);
            for( int i = 0; i < numberOfMixtureComponents; i++ )
                result[0 + i * 5] = 1.0 / exponentialParameters[i];
            List<List<Integer>> indexOfSubsampleAndSubsample = getSubsamplesSimulatedByProbabilitiesPij(probabilitiesPij, sample);
            for( int indexOfSubsample = 0; indexOfSubsample < indexOfSubsampleAndSubsample.size(); indexOfSubsample++ )
            {
                List<Integer> subSample = indexOfSubsampleAndSubsample.get( indexOfSubsample );
                result[1 + indexOfSubsample * 5] = subSample.size();
                double[] meanAndSigma = getMeanAndSigma(subSample);
                result[2 + indexOfSubsample * 5] = meanAndSigma[0];
                result[3 + indexOfSubsample * 5] = meanAndSigma[1];
                double[] chiSquaredStatisticAndPvalue = getStatisticAndPvalueOfChiSquaredTestForExponentiality(subSample,
                        numberOfIntervals);
                result[4 + indexOfSubsample * 5] = chiSquaredStatisticAndPvalue[1];
            }
        }
        else
            for( int j = 0; j < 5; j++ )
                result[j] = 0.0;
        return result;
    }
    
    /***
     * Calculation of Observed and Predicted Densities of Sub-samples
     * Sub-samples are identified as components of exponential mixture
     * @param sample
     * @param numberOfMixtureComponents
     * @param numberOfIntervals
     * @param maximalNumberOfIterations
     * @return
     * @throws Exception
     */
    public static Map<Integer, Map<Integer, int[]>> getObservedAndPredictedDensitiesOfExponentialMixture(List<Integer> sample, int numberOfMixtureComponents, int numberOfIntervals, int maximalNumberOfIterations) throws Exception
    {
        Map<Integer, Map<Integer, int[]>> result = new HashMap<>();
        double[] exponentialParameters = new double[numberOfMixtureComponents];
        double[] probabilitiesOfMixtureComponents = new double[numberOfMixtureComponents];
        if( sample != null && sample.size() > 10 && sample.size() > 3 * numberOfMixtureComponents)
        {
            int max = -1;
            for( Integer i : sample )
            {
                int ii = i;
                if( max < ii )
                    max = ii;
            }
            int h = max / (numberOfMixtureComponents + 1);
            for( int i = 1; i <= numberOfMixtureComponents; i++ )
            {
                exponentialParameters[i - 1] = 1.0 / (i * h);
                probabilitiesOfMixtureComponents[i - 1] = 1.0 / numberOfMixtureComponents;
            }
            int numberOfIterations = maximalNumberOfIterations;
            double[][] probabilitiesPij = estimateExponentialMixtureBy_EM_Algorithm(exponentialParameters, probabilitiesOfMixtureComponents, sample, numberOfIterations);
            List<List<Integer>> indexOfSubsampleAndSubsample = getSubsamplesSimulatedByProbabilitiesPij(probabilitiesPij, sample);
            for( int indexOfSubsample = 0; indexOfSubsample < indexOfSubsampleAndSubsample.size(); indexOfSubsample++ )
            {
                List<Integer> subSample = indexOfSubsampleAndSubsample.get( indexOfSubsample );
                Map<Integer, Integer> valueOfSubsampleAndItsFrequency = getDensityForIntegerValuedSample(subSample);
                double[] meanAndSigma = getMeanAndSigma(subSample);
                Map<Integer, int[]> valueOfSubsampleAndItsFrequencyAndPredictedFrequency = EntryStream.of( valueOfSubsampleAndItsFrequency )
                        .mapToValue( (valueInSubsample, observed) -> {
                    double x = 1.0 / meanAndSigma[0];
                    int i = (int)( x * Math.exp(-x * valueInSubsample) * subSample.size() );
                    return new int[] {observed, i};
                }).toMap();
                result.put(indexOfSubsample, valueOfSubsampleAndItsFrequencyAndPredictedFrequency);
            }
        }
        else
            return null;
        return result;
    }

    /***
     * There is 2x2-contingency table {n00, n01, n10, n11}
     * The statistic of chi-squared test for independence is calculated by using the given 2x2-contingency table
     * @param contingencyTable
     * @return statistic of chi-squared test for independence
     */
    public static double getStatisticOfChiSquared_2x2_testForIndependence(int[] contingencyTable)
    {
        double result = ((double)contingencyTable[0]) * ((double)contingencyTable[3]);
        result -= ((double)contingencyTable[1]) * ((double)contingencyTable[2]);
        result = Math.abs(result);
        int totalNumber = 0;
        for( int i = 0; i < 4; i++ )
            totalNumber += contingencyTable[i];
        if( totalNumber == 0 ) return 0.0;
        result -= 0.5 * totalNumber;
        result *= result;
        result *= totalNumber;
        int number = contingencyTable[0] + contingencyTable[1];
        if (number == 0 ) return 0.0;
        result /= number;
        number = contingencyTable[1] + contingencyTable[2];
        if (number == 0 ) return 0.0;
        result /= number;
        number = contingencyTable[0] + contingencyTable[2];
        if (number == 0 ) return 0.0;
        result /= number;
        number = contingencyTable[1] + contingencyTable[3];
        if (number == 0 ) return 0.0;
        result /= number;
        return result;
    }
    
    public static double getStatisticOfChiSquared_2x2_testForIndependence(long[] contingencyTable)
    {
        double result = ((double)contingencyTable[0]) * ((double)contingencyTable[3]);
        result -= ((double)contingencyTable[1]) * ((double)contingencyTable[2]);
        result = Math.abs(result);
        long totalNumber = 0;
        for( int i = 0; i < 4; i++ )
            totalNumber += contingencyTable[i];
        if( totalNumber == 0 ) return 0.0;
        result -= 0.5 * totalNumber;
        result *= result;
        result *= totalNumber;
        long number = contingencyTable[0] + contingencyTable[1];
        if ( number == 0 ) return 0.0;
        result /= number;
        number = contingencyTable[1] + contingencyTable[2];
        if (number == 0 ) return 0.0;
        result /= number;
        number = contingencyTable[0] + contingencyTable[2];
        if (number == 0 ) return 0.0;
        result /= number;
        number = contingencyTable[1] + contingencyTable[3];
        if (number == 0 ) return 0.0;
        result /= number;
        return result;
    }

    //////////////////////////////////// O.K.
    /***
     * Density is represented by Map<i,freq(i)>, where i is sample element and freq(i) is absolute frequency of element i in sample
     * @param sample
     * @return
     */
    public static Map<Integer, Integer> getDensityForIntegerValuedSample(List<Integer> sample)
    {
        Map<Integer, Integer> result = new HashMap<>();
        for( Integer sampleElement : sample )
        {
            if( ! result.containsKey(sampleElement) )
                result.put(sampleElement, 1);
            else
            {
                int freq = result.get(sampleElement);
                result.put(sampleElement, ++freq);
            }
        }
        return result;
    }

    /***
     * Density is represented by Map<i,freq(i)>, where i is sample element and freq(i) is absolute frequency of element i in sample
     * and key is ordered.
     * @param sample
     * @return
     */
    public static Map<Integer, Integer> getOrderedDensityForIntegerValuedSample(List<Integer> sample)
    {
        return StreamEx.of(sample).sorted().mapToEntry( x -> 1 ).toCustomMap( Integer::sum, LinkedHashMap::new );
    }
    

    //////////////////////////O.K.
    /***
     * 
     * @param sample
     * @param min
     * @param max
     * @param numberOfIntervals
     * @return Map(Double, Double) = Map(sample value, frequency)
     */
    public static Map<Double, Double> getEmpiricalDensity(List<Double> sample, double min, double max, int numberOfIntervals)
    {
        Map<Double, Double> result = new HashMap<>();
        if( min >= max || numberOfIntervals <= 0 ) return null;
        int[] freq = new int[numberOfIntervals];
        double[] values = new double[numberOfIntervals];
        for( int i = 0; i < numberOfIntervals; i++ )
        {
            freq[i] = 0;
            values[i] = 0.0;
        }
        int n = 0;
        double h = (max - min) / numberOfIntervals;
        for( Double sampleElement : sample )
        {
            int i = (int) ( (sampleElement - min) / h );
            if( i < 0 || i >= numberOfIntervals ) continue;
            freq[i]++;
            values[i] += sampleElement;
            n++;
        }
        if( n == 0 ) return null;
        for( int i = 0; i < numberOfIntervals; i++ )
        {
            double x = min + h / 2.0 + h * i;
            if( freq[i] > 0 )
                x = values[i] / freq[i];
            double y = (double)freq[i] / (double)n;
            result.put(x, y);
        }
        return result;
    }
    
    public static Map<Double, Double> getEmpiricalDensity(double[] sample, int numberOfIntervals)
    {
        double min = sample[0];
        double max = sample[0];
        List<Double> list = new ArrayList<>();
        for( double element : sample )
        {
            if( element < min )
                min = element;
            if( element > max )
                max = element;
            list.add(element);
        }
        return getEmpiricalDensity(list, min, max, numberOfIntervals);
    }

    /***
     * 
     * @param sample
     * @param w smoothing window
     * @param doAddTwoZerroPoints
     * @return List of 2 arrays; 1-st array = smoothed x-values (distinct values of sample); 2-nd array = smoothed y-values (probabilities)
     */
    public static List<double[]> getEmpiricalDensitySmoothedByEpanechninkov(double[] sample, double w, Boolean doAddTwoZeroPoints)
    {
        List<double[]> result = new ArrayList<>();
        double constant = w * sample.length;
        List<Double> distinctXvalues = new ArrayList<>();
        for( double x : sample )
            if( ! distinctXvalues.contains(x) )
                distinctXvalues.add(x);
        int n = distinctXvalues.size(), dim = n;
        if( doAddTwoZeroPoints )
            dim += 2;
        double[] xValues = new double[dim], yValues = new double[dim];
        for( int i = 0; i < n; i++ )    // calculation of smoothed density
        {
            xValues[i] = distinctXvalues.get(i);
            yValues[i] = 0;
            for( double x : sample )
                yValues[i] += Util.epanechninkovKernel((xValues[i] - x), w);
            yValues[i] /= constant;
        }
        if( doAddTwoZeroPoints)    // add two zero points
        {
            double[] minAndMax = getMinAndMax(sample);
            if( minAndMax[0] > 0.0 )
                xValues[n] = 0.999 * minAndMax[0];
            else
                xValues[n] = 1.001 * minAndMax[0];
            if ( minAndMax[1] > 0.0 )
                xValues[n + 1] = 1.001 * minAndMax[1];
            else
                xValues[n + 1] = 0.999 * minAndMax[1];
            yValues[n] = yValues[n + 1] = 0.0;
        }
        result.add(0, xValues);
        result.add(1, yValues);
        return result;
    }

    /**
     * Pearson Correlation
     * 
     * @throws Exception
     *             if lengths of arguments differs
     */
    public static double pearsonCorrelation(double[] sample1, double[] sample2) throws Exception
    {
        if( sample1.length != sample2.length )
            throw new Exception("Samples for correlation are not with the same length!");
        int n = sample1.length;
        double sum12 = 0;
        double sum1 = 0;
        double sum2 = 0;
        double sum11 = 0;
        double sum22 = 0;

        boolean constant1 = true;
        boolean constant2 = true;

        for( int i = 0; i < n; i++ )
        {
            if( i < n - 1 && sample1[i] != sample1[i + 1] )
                constant1 = false;

            if( i < n - 1 && sample2[i] != sample2[i + 1] )
                constant2 = false;

            double val1 = sample1[i];
            double val2 = sample2[i];
            sum12 += val1 * val2;
            sum1 += val1;
            sum2 += val2;
            sum11 += val1 * val1;
            sum22 += val2 * val2;
        }

        if( constant1 || constant2 )
        {
            return constant1 == constant2 ? 1 : 0;
        }

        double numerator = n * sum12 - sum1 * sum2;
        double var1 = n * sum11 - sum1 * sum1;
        double var2 = n * sum22 - sum2 * sum2;

        return numerator / ( Math.sqrt(var1 * var2) );
    }

    /***
     * 
     * @param cTable  is contingency table, i.e. it is array containing four frequencies: {n00, n01, n10, n11}
     * @return
     * @throws Exception
     */
    public static double pearsonCorrelation(int[] cTable) throws Exception
    {
        double x = (double)(cTable[1] + cTable[3]) * (double)(cTable[2] + cTable[3]);
        double result = x * (cTable[0] + cTable[1]) * (cTable[0] + cTable[2]);
        if(  result <= 0.0 )
            throw new Exception("Variation is zero!");
        int sum = 0;
        for( int nij : cTable )
            sum += nij;
        result = ((double)sum * (double)cTable[3] - x) / Math.sqrt(result);
        return result;
    }
    
    public static double pearsonSignificance(double correlation, int n)
    {
        if( Math.abs(correlation) == 1 )
            return 0;
        double statistic = correlation * Math.sqrt(n - 2) / Math.sqrt(1 - correlation * correlation);
        double pvalue = Stat.studentDistribution(Math.abs(statistic), n - 2, 80)[1];
        return 2 * pvalue;
    }


    /**
     * Spearman rank Correlation
     * 
     * @throws Exception
     *             if lengths of arguments differs
     */
    
    /**** Sometimes it don't work correctly !!!! ****/
    /**** Sometimes it don't work correctly !!!! ****/
    /**** Sometimes it don't work correctly !!!! ****/
    public static double spearmanCorrelationFast(double[] sample1, double[] sample2) throws Exception
    {
        if( sample1.length != sample2.length )
            throw new Exception("Samples for correlation are not with the same length!");
        int n = sample1.length;

        double[] r1 = new double[n];
        double[] r2 = new double[n];

        //calculate ranks for all values in both samples
        double tieCorrection1 = getRanks(sample1, r1);
        double tieCorrection2 = getRanks(sample2, r2);

        double result = 0;
        double nn = ( n + 1 ) / (double)2;
        for( int i = 0; i < n; i++ )
        {
            result += ( r1[i] - nn ) * ( r2[i] - nn );
        }

        double tieCorrection = tieCorrection1 + tieCorrection2;

        result = 12 * result / ( n * ( n * n - 1 ) - tieCorrection * 6 );

        return result;
    }

    public static double spearmanSignificance(double correlation, int n)
    {
        return 2 * ( 1 - Stat.normalDistribution(Math.abs(correlation), 0, 1d / ( n - 1 )) );
    }

    /**
     * Spearman rank Correlation
     * 
     * @throws Exception
     *             if lengths of arguments differs
     */
    public static double spearmanCorrelationPearson(double[] sample1, double[] sample2) throws Exception
    {
        if( sample1.length != sample2.length )
            throw new Exception("Samples for correlation are not with the same length!");
        int n = sample1.length;

        double[] r1 = new double[n];
        double[] r2 = new double[n];
        getRanks(sample1, r1);
        getRanks(sample2, r2);
        return pearsonCorrelation(r1, r2);
    }


    /**
     * Get ranks for sample and save them into array <b>ranks</b><br>
     * For equal values ranks are averaged:<br>
     * (1,0,0,0,5) -> (4,1,2,3,5) -> (4,2,2,2,5) - result<br>
     * Also returns tieCorrection value: 1/12 * sum((A_j)^3 - A_j) where A_j - number of elements int jth tie<br>
     * (4,2,2,2,5) -> A_1 = 1, A_3 = 1, A_2 = 3 => A = 1/12 *(81 - 3) =  6.5;
     * @param sample
     * @param ranks
     * @return A
     */
    private static double getRanks(double[] sample, double[] ranks)
    {
        int n = sample.length;
        if( ranks.length != n )
            throw new IllegalArgumentException("sample and ranks length must agree");

        //arranging sample ascending and set initial ranks
        double[] data = Arrays.copyOf(sample, n);
        double[] result = new double[n];
        int[] pos = Util.sort(data);
        for( int i = 0; i < n; i++ )
            result[i] = i + 1;

        double tieCorrection = 0;// correction for equal ranks

        //in the case of equal ranks (ties)
        for( int i = 1; i < n; i++ )
        {
            int k = i - 1;
            double averagedRank = result[k];

            //for all elements in those tie summarazing its ranks and then averaging it
            while( k < n - 1 && data[k] == data[k + 1] )
            {
                k++;
                averagedRank += result[k];
            }
            int tieSize = k - i + 2;

            //if more than one element in tie
            if( tieSize != 1 )
            {
                averagedRank /= tieSize;
                tieCorrection += ( tieSize * ( tieSize * tieSize - 1 ) );
            }

            //setting equal ranks to all elements in tie
            for( int j = i - 1; j <= k; j++ )
            {
                result[j] = averagedRank;
            }

            i = k + 1; //get to the next element after tie
        }

        //rearranging ranks to fit sample initial arrangement
        for( int i = 0; i < n; i++ )
            ranks[pos[i]] = result[i];

        return tieCorrection / 12;
    }


    /**
     * 
     * @param matrix
     * @return
     */
    public static double[] permutationVector(double[] vector)
    {
        int n = vector.length;
        double[] rand = new double[n];

        for( int i = 0; i < n; i++ )
            rand[i] = Math.random();

        int[] pos = Util.sortHeap(rand);

        double[] perVector = new double[n];
        for( int i = 0; i < n; i++ )
            perVector[i] = vector[pos[i]];

        return perVector;
    }

    /**
     * Matrix permutation based on permutationVector can be applied to matrices with unequal row lengths
     * @param matrix
     * @return
     */
    public static double[][] permutationComplicatedMatrix(double[][] matrix)
    {
        int rowCount = matrix.length;
        int[] rowLength = new int[rowCount];
        for( int i = 0; i < rowCount; i++ )
            rowLength[i] = matrix[i].length;
        double[] vector = Util.getVectorByMatrix(matrix);
        vector = permutationVector(vector);
        return Util.getMatrixByVector(vector, matrix.length, rowLength);
    }

    /**
     * Random permutation matrix
     */
    public static double[][] permutationMatrix(double[][] matrix)
    {
        int n = matrix.length;
        int m = matrix[0].length;
        double[] rand = new double[n * m];

        for( int i = 0; i < rand.length; i++ )
            rand[i] = Math.random();

        int[] pos = Util.sortHeap(rand);

        double[][] perMatrix = new double[n][m];
        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < m; j++ )
            {
                int u = pos[i * m + j] / m;
                int v = pos[i * m + j] - u * m;
                perMatrix[i][j] = matrix[u][v];
            }
        }
        return perMatrix;
    }

    /**
     * Another random permutation matrix
     */
    public static double[][] permutation(double[][] matrix)
    {
        int n = matrix.length;
        int m = matrix[0].length;
        double[][] result = matrix.clone();
        int u = 0;
        int v = 0;
        int tempi = 0;
        double tempd = 0;

        for( int i = n - 1; i >= 0; i-- )
        {
            for( int j = m - 1; j >= 0; j-- )
            {
                tempi = randInt(i * m + j);
                u = tempi / m;
                v = tempi - u * m;
                tempd = result[i][j];
                result[i][j] = result[u][v];
                result[u][v] = tempd;
            }
        }
        return result;
    }


    /**
     * Student`s distribution function;
     * 
     * @param degrees:
     *            number of freedom degrees;
     */
    public static double[] studentDistribution(double x, double degrees) throws Exception
    {
        return studentDistribution(x, degrees, 100);
    }

    /**
     * Student`s distribution function;
     * 
     * @param degrees:
     *            number of freedom degrees;
     * @param niter:
     *            number of iterations for calculating
     */
    public static double[] studentDistribution(double x, double degrees, long niter)
    {
        if( niter <= 0 )
            throw new IllegalArgumentException("Wrong iteration number");
        if( degrees <= 0 )
            throw new IllegalArgumentException("Wrong freedom degrees");
        double[] probs = betaDistribution(x * x / ( x * x + degrees ), 0.5, 0.5 * degrees, niter);
        double prob = probs[1] / 2;
        //double prob = (1.0 + probs[1]) / 2.0;
        return x < 0 ? new double[] {prob, 1 - prob} : new double[] {1 - prob, prob};
    }
    
    /**
     * Student statistics
     * 
     * @return value of Student statistics with sign
     */
    public static double studentTest(double[] sample1, double[] sample2)
    {
        double n1 = sample1.length, n2 = sample2.length, n = 1f / n1 + 1f / n2;
        double m1 = mean(sample1), m2 = mean(sample2), v1 = 0.0, v2 = 0.0;
        for( double s : sample1 )
        {
            double diff = s - m1;
            v1 += diff * diff;
        }
        for( double s : sample2 )
        {
            double diff = s - m2;
            v2 += diff * diff;
        }
        if( v1 == 0 && v2 == 0 ) return 0.0;
        double s = ( m1 - m2 ) * Math.sqrt(n1 + n2 - 2);
        s /= Math.sqrt( ( v1 + v2 ) * n);
        return s;
    }

    /**
     * Two-group paired Student test
     */
    public static double pairedStudentTest(double[] sample1, double[] sample2)
    {
        int n = Math.min(sample1.length, sample2.length);
        double t = 0, tt = 0;
        for( int i = 0; i < n; i++ )
        {
            double diff = sample1[i] - sample2[i];
            t += diff;//sample1[i] - sample2[i];
            tt += diff * diff;//Math.pow(sample1[i] - sample2[i], 2);
        }
        double s = t / Math.sqrt(n * tt - t * t);
        s *= Math.sqrt(n - 1);
        return s;
    }

    /**
     * Gamma - function
     */

    public static double gammaFunc(double x) throws Exception
    {
        if( x < 0 )
            throw new Exception("Argument for Gamma-function must not be negative!");
        int c = (int)x;
        double y = x - c;
        if( y <= 0 )
        {
            double result = 1;
            for( int i = 2; i < c; i++ )
                result *= i;
            return result;
        }
        double y1 = y * y * y * y * y * y;//Math.pow(y, 6);
        y1 -= 13.400414785781348263 * y * y * y * y * y;//Math.pow(y, 5);
        y1 += 50.788475328895409737 * y * y * y * y;//Math.pow(y, 4);
        y1 += 83.550058667919769575 * y * y * y;//Math.pow(y, 3);
        y1 -= 867.23098753110299446 * y * y;//Math.pow(y, 2);
        y1 += 476.79386050368791516 * y;
        y1 += 3786.0105034825718726;

        double result = 0.7780795856133005759 * y * y * y * y * y * y;//Math.pow(y, 6);
        result += 6.1260674503360842988 * y * y * y * y * y;//Math.pow(y, 5);
        result += 48.954346227909938052 * y * y * y * y;//Math.pow(y, 4);
        result += 222.11239616801179484 * y * y * y;//Math.pow(y, 3);
        result += 893.58180452374981424 * y * y;//Math.pow(y, 2);
        result += 2077.4597938941873210 * y;
        result += 3786.0105034825724548;
        result /= y1;


        switch( c )
        {
            case 2:
                break;
            case 1:
            {
                result /= x;
                break;
            }
            case 0:
            {
                result /= x * ( 1.0 + x );
                break;
            }
            default:
            {
                for( int i = 2; i < c; i++ )
                    result *= i + y;
                break;
            }
        }

        return result;
    }

    private static double[] logGammaCached = new double[LOG_GAMMA_CACHE_SIZE];
    static
    {
        for(int i=0; i<logGammaCached.length; i++) logGammaCached[i] = logGamma((double)i);
    }
    
    /**
     * Gamma - function for int values of x. Uses cache for values from 0 to 1023
     * @param x
     * @return
     */
    public static double logGamma(int x)
    {
        if(x < LOG_GAMMA_CACHE_SIZE) return logGammaCached[x];
        return logGamma((double)x);
    }
    
    /**
     * Gamma - function (MAT005) Returns logarithm if x>=8 It is recommended to
     * use GammaLog function which always returns logarithm
     * 
     * @param double
     *            x>0
     * @return LogarithmGamma(x) if 0 < x < 8
     * @throws Exception
     *             if argument x < 0
     */
    public static double logGamma(double x)
    {
        if( x < 0 )
            throw new IllegalArgumentException("Argument for Gamma-function must not be negative!");

        if( x >= 8 )
        {
            double y = 1.0 / x;
            double g = 7.66345188e-04 * y * y * y * y * y * y * y * y * y;//Math.pow(y, 9);
            g -= 5.9409561052e-04 * y * y * y * y * y * y * y;//Math.pow(y, 7);
            g += 7.936431104845e-04 * y * y * y * y * y;//Math.pow(y, 5);
            g -= 2.77777775657725e-03 * y * y * y;
            g += 8.3333333333316923e-02 * y;
            g += 0.918938533204672741780329739905;
            g += ( x - 0.5 ) * Math.log(x) - x;

            return g;
        }
        int c = (int)x;
        double y = x - ( (char)x );
        if( y < 0.0 )
        {
            y = 0.0;
        }
        double y1 = y * y * y * y * y * y;//Math.pow(y, 6);
        y1 -= 13.400414785781348263 * y * y * y * y * y;//Math.pow(y, 5);
        y1 += 50.788475328895409737 * y * y * y * y;
        y1 += 83.550058667919769575 * y * y * y;
        y1 -= 867.23098753110299446 * y * y;
        y1 += 476.79386050368791516 * y;
        y1 += 3786.0105034825718726;

        double g = 0.7780795856133005759 * y * y * y * y * y * y;//Math.pow(y, 6);
        g += 6.1260674503360842988 * y * y * y * y * y;//Math.pow(y, 5);
        g += 48.954346227909938052 * y * y * y * y;
        g += 222.11239616801179484 * y * y * y;
        g += 893.58180452374981424 * y * y;
        g += 2077.4597938941873210 * y;
        g += 3786.0105034825724548;
        g /= y1;

        switch( c )
        {
            case 2:
                break;
            case 1:
            {
                g /= x;
                break;
            }
            case 0:
            {
                g /= x * ( 1.0 + x );
                break;
            }
            default:
            {
                for( int i = 2; i < c; i++ )
                    g *= i + y;
                break;
            }
        }
        return Math.log(g);
    }

    /**
     * Logarithm of Beta function
     * 
     * @throws Exception
     *             if x < 0 or y < 0
     */
    public static double logBeta(double x, double y)
    {
        if( x < 0 || y < 0 )
            throw new IllegalArgumentException("Both arguments for Beta function must not be negative!");
        return logGamma(x) + logGamma(y) - logGamma(x + y);
    }

    /**
     * B-distribution
     * 
     * @return value of beta Distribution function
     * @throws Exception
     *             if a < 0 or b < 0 or niter < 0
     */
    public static double[] betaDistribution(double x, double a, double b)
    {
        return betaDistribution(x, a, b, 100);
    }

    /**
     * B-distribution
     * 
     * @param niter:
     *            number of iteration for integral solving
     * @return value of beta Distribution function and 1-beta Distribution function
     * @throws Exception
     *             if a < 0 or b < 0 or niter < 0
     */
    public static double[] betaDistribution(double x, double a, double b, long niter)
    {
        if( a <= 0 || b <= 0 )
            throw new IllegalArgumentException("Parameters for Beta Distribution must be >=0.5");
        if( niter <= 0 )
            throw new IllegalArgumentException("Number of iterations must be positive");
        double beta;
        if( x <= 0 || x >= 1 )
        {
            beta = Util.restrict(0, 1, x);
            return new double[] {beta, 1-beta};
        }

        double aaa, aaa1, aaa2, aaa3, aaa4;
        double zz, zzz, z1, z2, z3, z4, z5, z6, z7;

        boolean ind = false;

        if( a > ( a + b ) * x )
        {
            aaa = x;
            aaa1 = 1 - x;
            aaa2 = a;
            aaa3 = b;
        }
        else
        {
            ind = true;
            aaa = 1 - x;
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

        if( aaa2 < 1.0 )
            z1 = zz;
        else
            z1 = aaa2;
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
            {
                return new double[] {0,1};
            }
            beta = 1.0 + z3 * ( ( j * ( z6 + j ) ) / ( ( z1 + jj ) * z7 ) ) / beta;
            if( beta == 0.0 )
            {
                return new double[] {0,1};
            }
            beta = 1.0 - z3 * ( ( ( aaa3 - j ) * ( z4 + j ) ) / ( ( z5 + jj ) * z7 ) ) / beta;
        }
        if( beta == 0.0 )
        {
            return new double[] {0,1};
        }
        beta = 1d / beta;
        if( z1 != aaa2 )
        {
            beta *= aaa * zzz / zz;
            beta += aaa1;
        }
        zz = logBeta(aaa2, aaa3);
        zzz = aaa2 * Math.log(aaa) + z2 * Math.log(aaa1) - zz;
        beta *= Math.exp(zzz) / aaa2;
        if(beta < 0)
        {
            beta = -beta;
            ind = !ind;
        }
        double invbeta = 1.0 - beta;

        return ind ? new double[] {invbeta, beta} : new double[] {beta, invbeta};
    }

    /**
     * Normal distribution
     */
    public static double standartNormalDistribution(double x)
    {
        double[] p = {0, 0};
        double y, y1, yy, yyy;
        if( x == 0.0 )
            return 0.5;

        y = Math.abs(x);
        y /= 1.414213562373095048801688724210;
        if( y <= 0.0 )
            return 0.5;

        yy = y * y;
        if( y >= 1.0 )
        {
            p[0] = 61.41751803655557 + 86.59183795230987 * y;
            p[0] += 59.59357876640772 * yy;
            p[1] = 61.41751803908473 + 155.8940854441156 * y;
            p[1] += 174.0837071857067 * yy;
            yyy = y * yy;
            p[0] += 23.605679444219112 * yyy;
            p[1] += 110.345448210573 * yyy;
            y = yy * yy;
            p[0] += 5.3508823344203262 * y;
            p[1] += 42.34202517388795 * y;
            y = yy * yyy;
            p[0] += 0.5641881275911299 * y;
            p[1] += 9.484083981245681 * y;
            p[1] += yyy * yyy;
        }
        else
        {
            yyy = y * yy;
            p[1] = yyy * yyy;
            y1 = yy * yyy;
            p[0] = 0.5641881275911299 * y1;
            p[1] += 9.484083981245681 * y1;
            y1 = yy * yy;
            p[0] += 5.3508823344203262 * y1;
            p[0] += 23.605679444219112 * yyy;
            p[0] += 59.59357876640772 * yy;
            p[0] += 86.59183795230987 * y;
            p[0] += 61.41751803655557;
            p[1] += 42.34202517388795 * y1;
            p[1] += 110.345448210573 * yyy;
            p[1] += 174.0837071857067 * yy;
            p[1] += 155.8940854441156 * y;
            p[1] += 61.41751803908473;
        }
        p[0] /= p[1];
        p[0] *= 0.5 * Math.exp( -yy);
        if( x < 0.0 )
            p[1] = 1.0 - p[0];
        else
        {
            p[1] = p[0];
            p[0] = 1.0 - p[0];
        }
        try
        {
            Util.restrict(0.0, 1.0, p[0]);
            Util.restrict(0.0, 1.0, p[1]);
        }
        catch( Exception e )
        {
        }
        return p[0];
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
        double quantile = 0.0;
        if( p == 0.5 ) return quantile;
        double x = 1.0 - p;
        if( p > 0.5) x = p;
        x*=2.0;
        x -= 1.0;
        if( x <= 0.0 ) return quantile;
        if( x >= 1.0 ) return Double.MAX_VALUE;
        if( x >= 0.36 )
        {
            double xx = 1.0 - x;
            xx = - Math.log(xx);
            xx = 1.0 / Math.sqrt(xx);
            double xxx = xx * xx, x1 = xx * xxx, x2 = xxx * xxx, x3 = xxx + 0.268419889732864 * xx;
            x3 += 0.0111247992539409;
            quantile = -0.02055509764649 *x2 * x2;
            quantile += 0.151646346343608 * x1 * x2;
            quantile -= 0.460597144409907 * x1 * x1;
            quantile += 0.715981570361426 * x1 * xxx;
            quantile -= 0.446867040619259 * xxx * xxx;
            quantile -= 0.377458582257143 * x1;
            quantile += 0.973134220625464 * xxx;
            quantile += 0.268334204826738 * xx;
            quantile += 0.0111252183495389;
            quantile /= x3;
            quantile /= xx;
        }
        if ( x <= 0.9375 )
        {
            double xx = 1.0 + x;
            for ( int i = 0;  i < maxNumberOfIterations; i++ )
            {
                double xxx = standartNormalDistribution(quantile * 1.414213562373095048801688724210);
                xxx *= 2.0;
                xxx -= xx;
                xxx *= 0.886226925452758013649083741671;
                xxx *= Math.exp(quantile * quantile);
                xxx *= 1.0 - xxx;
                double x1 = Math.abs(xxx);
                if( x1 <= eps ) break;
                quantile -= xxx;
            }
        }
        quantile *= 1.414213562373095048801688724210;
        if( p < 0.5 )
            quantile = - quantile;
        return quantile;
    }
    
    /**
     * 
     * @param x
     * @param mean
     * @param variance
     * @return
     */

    public static double normalDistribution(double x, double mean, double variance)
    {
        return standartNormalDistribution( ( x - mean ) / Math.sqrt(variance));
    }

    /**
     * MAT074 - Calculation of the incomplete gamma function.
     * 
     * @throws Exception
     *             if x <= 0;
     */
    private static double iGammaFunction(double x, double par, long niter) throws Exception
    {
        double y = 0;

        long j, jj;
        double z, z1, zzz;
        double zz;

        if( x <= 0.0 )
        {
            throw new Exception( ( "Argument for incomplete gamma function must be postive" ));
        }
        z = par + 1.0;
        zz = Math.log(x);

        if( x < z )
        {

            for( j = 0L; j < niter; j++ )
            {
                jj = j * 2L;
                zzz = z + jj;
                z1 = logGamma(zzz);
                z1 = jj * zz - z1;
                z1 = Math.exp(z1);
                z1 *= ( 1.0 + x / zzz );
                y += z1;

            }
            y *= Math.exp(par * zz - x);

        }
        else
        {
            for( zzz = x + 1.0 - par, j = niter; j >= 1L; j-- )
            {
                y = zzz + j * 2L - y;
                if( y == 0.0 )
                {
                    throw new Exception( ( "Error while caluclating incomplete Gamma function" ));
                }
                y = j * ( j - par ) / y;
            }
            y = zzz - y;
            if( y == 0.0 )
            {
                throw new Exception( ( "Error while caluclating incomplete Gamma function" ));
            }
            y = 1.0 / y;
            z = par * zz - x;
            zz = logGamma(par);
            z -= zz;
            y *= Math.exp(z);
            y = 1.0 - y;
        }
        Util.restrict(0, 1, y);

        return y;

    }

    /**
     * Calculation of the chi-squared distribution function.
     * 
     * @param x
     * @param degrees:
     *            number of freedom degrees
     * @niter: number of iteration for estimating
     */
    public static double chiDistribution(double x, double degrees, long niter) throws Exception
    {
        if( degrees == 1 )
            return 2 * standartNormalDistribution(Math.sqrt(x)) - 1;
        else
            return iGammaFunction(0.5 * x, degrees * 0.5, niter);
    }

    // old version
    /**
     * Calculation of the chi-squared distribution function.
     * 
     * @param degrees:
     *            number of freedom degrees
     */
    /***
    public static double chiDistribution(double x, double degrees) throws Exception
    {
        if( degrees == 1 )
            return 2 * standartNormalDistribution(Math.sqrt(x)) - 1;
        else
            return iGammaFunction(0.5 * x, degrees * 0.5, 100);
    }
    ***/
    
    // new version
    /**
     * Calculation of the chi-squared distribution function.
     * 
     * @param degrees:
     *            number of freedom degrees
     */
    public static double chiDistribution(double x, double degrees) throws Exception
    {
        return chiDistribution(x, degrees, 100);
    }

    /**
     * @return random integer value between 0 and k inclusively
     */
    public static int randInt(int k)
    {
        return (int) ( Math.random() * ( k + 1 ) );
    }

    /**
     * Simple Linear regression with two parameters: Y = a+X*b
     * 
     * @return array of two regression coefficients: a and b and Pvalue;
     *         LinearRegression[0] = a; LinearRegression[1] = b;
     *         LinearRegression[2] = Pvalue for Hypothesis {b = 0}
     * @throws Exception
     *             if arguments length is smaller then 3 or their lengths are
     *             different
     */
    public static double[] linearRegression(double[] Y, double[] X) throws Exception
    {
        if( Y.length != X.length )
            throw new Exception("Samples lengths don't match");
        double a = 0, b = 0, corrXY = 0, varianceX = 0, pv = 0;
        double meanX = mean(X);
        int length = X.length;
        if( length < 3 )
            throw new Exception("Samples are too short (length < 3)");
        for( int i = 0; i < length; i++ )
        {
            double diff = X[i] - meanX;
            corrXY += diff * Y[i];
            varianceX += diff * diff;
        }
        if( varianceX == 0 )
            throw new Exception("Probably bad time points");
        b = corrXY / varianceX;
        a = mean(Y) - b * meanX;

        // Evaluation of P-value
        double ss = 0;
        for( int i = 0; i < length; i++ )
        {
            double diff = Y[i] - a - b * X[i];
            ss += diff * diff;//Math.pow(Y[i] - a - b * X[i], 2);
        }
        ss = ss / ( length - 2 );
        if( ss != 0 )
        {
            double statistic = corrXY / Math.sqrt(varianceX * ss);
            pv = Stat.studentDistribution(Math.abs(statistic), length - 2, 80)[1];
        }
        else
            pv = 0;
        double[] Regpars = new double[3];
        Regpars[0] = a;
        Regpars[1] = b;
        Regpars[2] = pv;
        return Regpars;
    }

    /***
     * 
     * @param samples is a matrix with n rows and k columns (samples)
     * @return array: array[0] = Friedman statistic, array[1] - p-value;
     * @throws Exception
     */
    public static double[] friedmanTest(double[][] samples) throws Exception
    {
        double[] result = new double[2];
        double tempi;
        int n = samples.length;
        int m = samples[0].length;
        double[] tieCorrections = new double[2];
        double[][] ranks = new double[n][m];
        double correctionOnTies = 0.0;
        for( int i = 0; i < n; i++ )
        {
            getRanks(samples[i], ranks[i], tieCorrections);
            correctionOnTies += tieCorrections[1];
        }
        double friedmanStat = 0.0;
        double constant = (double)n * (m + 1);
        for( int j = 0; j < m; j++ )
        {
            tempi = 0;
            for( int i = 0; i < n; i++ )
                tempi += ranks[i][j];
            double x = tempi - 0.5 * constant;
            friedmanStat += x * x;
        }
        result[0] = 12.0 * friedmanStat / (constant * m - correctionOnTies / (m - 1));
        result[1] = 1.0 - Stat.chiDistribution(result[0], m - 1);
        return result;
    }

    /**
     * Parametric test. Tests null hypothesis, that some data has normal
     * distribution.
     * 
     * @param matrix
     * @return P-value
     * @throws Exception
     *             Close to 1 P-value means that testing data set is
     *             significantly normal distributed
     */
    public static double chisSquareTest(double[] vector) throws Exception
    {
        int length = vector.length;
        double[] x = new double[6]; // x[] - split points
        int[] n = new int[x.length + 1];
        double[] p = new double[x.length + 1];
        int tempi = 0;
        int s = 0;
        int k = 0;
        double mean = mean(vector);
        double variance = variance(vector, false);
        double chiStat = 0;
        Arrays.sort(vector);
        double step = ( vector[length - 1] - vector[0] ) / ( x.length + 1 );
        x[0] = vector[0] + step;
        p[0] = normalDistribution(x[0], mean, variance);
        for( int i = 1; i < x.length; i++ )
        {
            x[i] = x[i - 1] + step;
            p[i] = normalDistribution(x[i], mean, variance) - normalDistribution(x[i - 1], mean, variance);
        }
        p[x.length] = 1 - normalDistribution(x[x.length - 1], mean, variance);

        do
        {
            k++;
            if( ( vector[k] > x[s] ) && ( x[s] > vector[k - 1] ) )
            {
                n[s] = k - tempi;
                tempi = k;
                s++;
            }
        }
        while( vector[k] < x[x.length - 1] );
        n[x.length] = length - tempi;
        for( int i = 0; i < x.length + 1; i++ )
        {
            chiStat += ( ( n[i] - length * p[i] ) * ( n[i] - length * p[i] ) ) / ( length * p[i] );
        }
        double pval = 2;
        for( int i = 0; i < 3; i++ )
        {
            pval = 1 - Math.min(Stat.chiDistribution(chiStat, x.length - i), pval);
        }

        return pval;
    }

    /**
     * Inverse Geary distribution function
     * 
     * @param matrix
     * @return
     * @throws Exception
     */
    public static double inverseGearyDistribution(int n, double p)
    {
        double a, b;
        if( n <= 101 )
        {
            if( 0.95 < p )
            {
                a = 1.226575 * p - 0.183259;
                b = -0.185425 * p + 0.139487;
            }
            else if( 0.9 <= p && p <= 0.95 )
            {
                a = 0.57132 * p + 0.439233;
                b = -0.09188 * p + 0.050619;
            }
            else if( 0.1 < p && p < 0.9 )
            {
                a = 0.308114 * p + 0.676119;
                b = -0.064391 * p + 0.025879;
            }
            else if( 0.05 <= p && p <= 0.1 )
            {
                a = 0.81062 * p + 0.625868;
                b = -0.21708 * p + 0.041148;
            }
            else
            {
                a = 1.813025 * p + 0.575748;
                b = -0.528675 * p + 0.056728;
            }
        }
        else
        {
            if( 0.95 < p )
            {
                a = 0.673075 * p + 0.243055;
                b = -0.089525 * p + 0.0721284;
            }
            else if( 0.9 <= p && p <= 0.95 )
            {
                a = 0.37074 * p + 0.530273;
                b = -0.05426 * p + 0.038682;
            }
            else if( 0.1 < p && p < 0.9 )
            {
                a = 0.153544 * p + 0.72575;
                b = -0.024369 * p + 0.01178;
            }
            else if( 0.05 <= p && p <= 0.1 )
            {
                a = 0.33552 * p + 0.707552;
                b = -0.05804 * p + 0.015147;
            }
            else
            {
                a = 0.838325 * p + 0.682412;
                b = -0.1531 * p + 0.0199;
            }
        }
        return a * Math.exp(b * Math.log(n));
    }

    /**
     * Distribution function of Geary statistics
     * 
     * @param n
     * @param x
     * @return
     */
    public static double gearyDistribution(int n, double x)
    {

        double c = 0.01, d = 1, y, cx;
        do
        {
            cx = x - inverseGearyDistribution(n, c);
            y = ( c + d ) / 2;
            cx *= x - inverseGearyDistribution(n, y);
            if( cx == 0 )
            {
                return y;
            }
            else if( cx > 0 )
                c = y;
            else
                d = y;
        }
        while( Math.abs(c - d) > 0.00000001 );
        return y;
    }

    /**
     * Geary statistics
     * 
     * @param vector
     * @param n
     * @return
     */
    public static double gearyStat(double[] vector)
    {
        double mean = mean(vector);
        double variance = variance(vector, false);
        double gStat = 0;
        for( double element : vector )
        {
            gStat += Math.abs(element - mean);
        }
        return gStat / ( vector.length * Math.sqrt(variance) );
    }

    /**
     * Non-parametric test. Tests null hypothesis, that some data has normal
     * distribution.
     * 
     * @param vector
     * @return P-value. Large P-value (close to 1)means that distribution of data
     *         close to normal.
     */
    public static double gearyTest(double[] vector)
    {
        double pval = 0;
        if( vector.length > 1001 )
        {
            double[] tempVector = new double[1001];
            vector = permutationVector(vector);
            for( int i = 0; i < 1001; i++ )
            {
                tempVector[i] = vector[i];
            }
            pval = 1 - gearyDistribution(tempVector.length, gearyStat(tempVector));
        }
        else
        {
            double[] tempVector = vector;
            pval = 1 - gearyDistribution(tempVector.length, gearyStat(tempVector));
        }

        return pval;
    }

    /**
     * Asymmetry statistics
     * 
     * @param vector
     * @return
     */
    public static double asSymetryStat(double[] vector)
    {
        double asStat = 0;
        double mean = mean(vector);
        double variance = variance(vector, false);
        variance = variance * variance * variance;//Math.pow(variance, 3);
        for( double element : vector )
        {
            double diff = element - mean;
            asStat += diff * diff * diff;//Math.pow(vector[i] - mean, 3);
        }
        return asStat / ( vector.length * variance );
    }

    /**
     * Non-parametric test. Null hypothesis is that the input vector is some
     * random data set (hypothetic distribution of vector should be
     * continuous!!!)
     * 
     * @param vector
     * @return P-value close to 1  means that testing data set is
     *         significantly random
     */
    public static double seriesCrit(double[] vector)
    {
        double[] tempVector = new double[vector.length];
        System.arraycopy(vector, 0, tempVector, 0, vector.length);
        double med = (int) ( (double) ( vector.length ) / 2 );
        double mean = 1 + med;
        double variance = med * ( med - 1 ) / ( 2 * med - 1 );
        Arrays.sort(tempVector);
        double medVal = tempVector[(int)med];
        int seriesCount = 0;
        double sign = 0;

        if( vector[0] != medVal )
            seriesCount = 1;
        for( int i = 1; i < vector.length; i++ )
        {
            if( vector[i - 1] != medVal )
                sign = Math.signum(vector[i - 1] - medVal);
            if( vector[i] != medVal )
            {
                if( sign != Math.signum(vector[i] - medVal) )
                    seriesCount++;
            }
        }

        double seriesStat = ( seriesCount - mean + 0.5 ) / Math.sqrt(variance);
        double pval = 2 * ( 1 - standartNormalDistribution(Math.abs(seriesStat)) );
        return pval;
    }

    private static final int WILCOX_MAX=20;
    private static volatile double[][][] wilcoxCache;
    
    // Ported from R: wilcox.c:w_init_maybe
    private static void wilcoxInit()
    {
        if( wilcoxCache == null )
        { /* initialize w[][] */
            synchronized( Stat.class )
            {
                if( wilcoxCache == null )
                {
                    int i;
                    int m = WILCOX_MAX;
                    int n = WILCOX_MAX;

                    if( m > n )
                    {
                        i = n;
                        n = m;
                        m = i;
                    }

                    double[][][] localWilcoxCache = new double[m + 1][][];
                    for( i = 0; i <= m; i++ )
                    {
                        localWilcoxCache[i] = new double[n + 1][];
                    }
                    wilcoxCache = localWilcoxCache;
                }
            }
        }
    }

    // Ported from R: wilcox.c
    private static double cwilcox(int k, int m, int n)
    {
        int c, u, i, j;

        u = m * n;
        if( k < 0 || k > u )
            return 0;
        c = u / 2;
        if( k > c )
            k = u - k; /* hence  k <= floor(u / 2) */
        if( m < n )
        {
            i = m;
            j = n;
        }
        else
        {
            i = n;
            j = m;
        } /* hence  i <= j */

        if( j == 0 /* and hence i == 0 */)
            return ( k == 0 ) ? 1 : 0;

        /* We can simplify things if k is small.  Consider the Mann-Whitney
           definition, and sort y.  Then if the statistic is k, no more
           than k of the y's can be <= any x[i], and since they are sorted
           these can only be in the first k.  So the count is the same as
           if there were just k y's.
        */
        if( j > 0 && k < j )
            return cwilcox(k, i, k);

        double[][] wi = wilcoxCache[i];
        if( wi[j] == null )
        {
            wi[j] = new double[c + 1];
            Arrays.fill( wi[j], -1 );
        }
        if( wi[j][k] < 0 )
        {
            wi[j][k] = cwilcox(k - j, i - 1, j) + cwilcox(k, i, j - 1);
        }
        return ( wi[j][k] );
    }

    // Ported from R: wilcox.c
    private static double pwilcox(double q, int m, int n, boolean lowerTail)
    {
        int i;
        double c, p;

        q = Math.floor(q + 1e-7);

        wilcoxInit();
        c = Math.exp(logCnk(m + n, n));
        p = 0;
        /* Use summation of probs over the shorter range */
        if( q <= ( m * n / 2 ) )
        {
            for( i = 0; i <= q; i++ )
                p += cwilcox(i, m, n) / c;
        }
        else
        {
            q = m * n - q;
            for( i = 0; i < q; i++ )
                p += cwilcox(i, m, n) / c;
            lowerTail = !lowerTail; /* p = 1 - p; */
        }
        return lowerTail?p:1-p;
    }

    /**
     * @param total = M+N
     * @param part = N
     * @param x
     * @param upTail
     * @return
     */
    public static double wilcoxonDistributionFast(int total, int part, double x, boolean upTail)
    {
        int minsum = part*(part+1)/2, maxsum = (2*total-part+1)*part/2;
        if(x < minsum) return upTail?1:0;
        if(x > maxsum) return upTail?0:1;
        if(part < 20 && total-part < 20) return pwilcox(upTail?maxsum-x:x-minsum, total-part, part, true);
        // For big totals calculate approximate value
        double meanU = (double)part*(total-part)/2;
        double varU = (double)part*(total-part)*(total+1)/12.;
        double u = part*(total-part)+part*(part+1)/2-x;
        double z = (u-meanU)/Math.sqrt(varU);
        double pValue = normalDistribution(upTail?z:-z, 0, 1);
        return pValue;
    }

    /**
     * Random uniform-distributed value
     */
    public static class RandomUniform
    {
        private int numb1;
        private int numb2;
        private int numb3;
        private double rand;

        public RandomUniform(int n1, int n2, int n3)
        {
            numb1 = n1;
            numb2 = n2;
            numb3 = n3;
        }

        public double getNextRandom()
        {
            rand = ( ( numb1 ) ) / 30269.0 + ( ( numb2 ) ) / 30307.0 + ( ( numb3 ) ) / 30323.0;
            int j = (int) ( rand );
            rand -= j;
            try
            {
                rand = Util.restrict(0, 1, rand);
            }
            catch( Exception ex )
            {
            }
            j = ( 2 * ( numb1 ) ) / 177;
            j -= 171 * ( ( numb1 ) % 177 );
            numb1 = Math.abs(j);
            if( numb1 == 0 )
                numb1 = 1 + (int) ( 97.0 * ( rand ) );

            j = ( 35 * numb2 ) / 176;
            j -= 172L * ( ( numb2 ) % 176L );
            numb2 = Math.abs(j);
            if( numb2 == 0 )
                ( numb2 ) = 10 + (int) ( 977.0 * rand );


            j = ( 63 * numb3 ) / 178;
            j -= 170 * ( ( numb3 ) % 178L );
            numb3 = Math.abs(j);
            if( numb3 == 0 )
                ( numb3 ) = 100 + (int) ( 9777.0 * rand );
            return rand;
        }
    }

    /**
     * Calculation of wilcoxon statistic
     * sample1 represents random values e(i)
     * sample2 represents random values e(i)+d
     * testing hypothesis H: d=0 vs H': d!=0
     * If P-value  close to 0.5 we accept H (P-value here can't be greater then 0.5)
     * P-value close to 0 means d>0.
     * P-value close to 1 means d<0.
     */
    public static double wilcoxonTest(double[] sample1, double[] sample2)
    {
        boolean equaltonext = false;

        int n = sample1.length + sample2.length;
        int m = sample1.length;
        int i, tempi = 0, count = 1;
        double wilkoksonStat = 0, rang;
        double[] apparr = new double[n];
        for( i = 0; i < m; i++ )
        {
            apparr[i] = sample1[i];
        }
        for( i = 0; i < sample2.length; i++ )
        {
            apparr[m + i] = sample2[i];
        }
        double[] rangarr = new double[n];
        int[] pos = Util.sortHeap(apparr);

        for( i = 0; i < n - 1; i++ )
        {
            if( apparr[i] == apparr[i + 1] )
            {
                tempi += i + 1;
                count++;
                equaltonext = true;
            }
            else
            {
                if( equaltonext )
                {
                    rang = ( (double) ( tempi + i + 1 ) ) / count;
                    for( int j = i - count + 1; j <= i; j++ )
                        rangarr[pos[j]] = rang;
                    equaltonext = false;
                    count = 1;
                    tempi = 0;
                }
                else
                    rangarr[pos[i]] = i + 1;
            }
        }

        if( equaltonext )
        {
            rang = ( (double) ( tempi + n ) ) / count;
            for( int j = n - count; j <= n - 1; j++ )
                rangarr[pos[j]] = rang;
        }
        else
            rangarr[pos[n - 1]] = n;

        for( i = 0; i < m; i++ )
        {
            wilkoksonStat += rangarr[i];
        }
        return wilkoksonStat;
    }
    
    /***
     * Wilcoxon signed-rank test
     * @param sample1 - 1-st sample
     * @param sample2 - 2-nd sample; dim(sample1) = dim(sample2)
     * @return array; array[0] = Wilcoxon statistic; array[1] = statistic for normal approximation, array[2] = p-value;
     */
    public static double[] wilcoxonSignedRank(double[] sample1, double[] sample2)
    {
        double result[] = new double[3];
        int n = sample1.length;
        double ranks[] = new double[n];
        double tieCorrections[] = new double[2];

        if( n != sample2.length )
            throw new IllegalArgumentException("Samples must have the equal sizes");
        double[] absDifferences = new double[n];
        for( int i = 0; i < n; i++ )
            absDifferences[i] = Math.abs(sample1[i] - sample2[i]);
        getRanks(absDifferences, ranks, tieCorrections);
        result[0] = 0.0;
        for( int i = 0; i < n; i++ )
            if( sample2[i] > sample1[i] )
                result[0] += ranks[i];
        double x = (double) n * (n + 1);
        result[1] = result[0] - 0.25 * x;
        x *= 2 * n + 1.0;
        x -= 0.5 * tieCorrections[1];
        result[1] /= Math.sqrt(x / 24.0);
        result[2] = 2.0 * ( 1.0 - standartNormalDistribution(Math.abs(result[1])));
        return result;
    }

    /****
     * 
     * @param sample
     * @param ranks - ranks
     * @param tieCorrections: dim(tieCorrections) = 2; in output tieCorrections[0] = sum (ki(ki-1)), where ki is the size of i-th tie;
     * tieCorrections[1] = sum (ki(ki-1)(ki+1));
     * 
     */
    public static void getRanks(double[] sample, double[] ranks, double[] tieCorrections)
    {
        int n = sample.length;
        double[] preRanks = new double[n];
        for( int i = 0; i < n; i++ )
            preRanks[i] = i + 1;
        // double[] x = ArrayUtils.clone(sample);
        double[] x = new double[n];
        for( int i = 0; i < n; i++ )
            x[i] = sample[i];
        int[] pos = Util.sortHeap(x);
        tieCorrections[0] = tieCorrections[1] = 0.0;
        for( int i = 0; i < n; i++ )
        {
            int subSize = 1, j = i;
            double sum = preRanks[i];
            while( ++j < n )
            {
                if( x[i] != x[j] ) break;
                subSize++;
                sum += preRanks[j];
            }
            if( subSize == 1 ) continue;
            double average = sum / subSize;
            for( j = 0; j < subSize; j++ )
                preRanks[i + j] = average;
            double y = (double)subSize * (subSize - 1);
            tieCorrections[0] += y;
            tieCorrections[1] += y * (subSize + 1);
            i += subSize - 1;
        }
        for( int i = 0; i < n; i++ )
            ranks[pos[i]] = preRanks[i];
    }
    
    public static Object[] getRanks(double[] sample)
    {
        double[] ranks = new double[sample.length], tieCorrections = new double[2];
        getRanks(sample, ranks, tieCorrections);
        return new Object[]{ranks, tieCorrections};
    }


    /**
     * Calculation of Kolmogorov statistic suitable for short samples
     */
    public static double kolmogorovShortSampleTest(double[] sample1, double[] sample2)
    {
        int n = sample1.length + sample2.length;
        int m = sample1.length;
        int i;
        double kolmogorovStat = 0, tempCurrent = 0, tempMax = 0;
        double[] apparr = new double[n];
        for( i = 0; i < m; i++ )
        {
            apparr[i] = sample1[i];
        }
        for( i = 0; i < sample2.length; i++ )
        {
            apparr[m + i] = sample2[i];
        }
        double[] deltaarr = new double[n];
        int[] pos = Util.sortHeap(apparr);

        for( i = 0; i < n; i++ )
        {
            if( pos[i] < m )
                deltaarr[i] = 1;
            else
                deltaarr[i] = 0;
        }

        for( i = 0; i < n; i++ )
        {
            deltaarr[pos[i]] = 1;
        }

        for( i = 0; i < n; i++ )
        {
            tempCurrent += ( (double)m ) / n - deltaarr[i];
            if( Math.abs(tempCurrent) > tempMax )
            {
                tempMax = Math.abs(tempCurrent);
            }
        }
        kolmogorovStat = tempMax * n / Util.getGCF(m, n - m);

        return kolmogorovStat;
    }
    /**
     * Calculation of Kolmogorov statistic suitable for long samples with shift correction
     */
    public static double kolmogorovLongSampleTest(double[] sample1, double[] sample2)
    {
        int n = sample1.length + sample2.length;
        int m = sample1.length;
        int i;
        double kolmogorovStat = 0, tempCurrent = 0, tempMax = 0;
        double[] apparr = new double[n];
        for( i = 0; i < m; i++ )
        {
            apparr[i] = sample1[i];
        }
        for( i = 0; i < sample2.length; i++ )
        {
            apparr[m + i] = sample2[i];
        }
        double[] deltaarr = new double[n];
        int[] pos = Util.sortHeap(apparr);

        for( i = 0; i < n; i++ )
        {
            if( pos[i] < m )
                deltaarr[i] = 1;
            else
                deltaarr[i] = 0;
        }

        for( i = 0; i < n; i++ )
        {
            tempCurrent += ( (double)m ) / n - deltaarr[i];
            if( Math.abs(tempCurrent) > tempMax )
            {
                tempMax = Math.abs(tempCurrent);
            }
        }
        kolmogorovStat = ( n / ( 4.6 * ( m * ( n - m ) ) ) + tempMax ) * Math.sqrt((double) ( m * ( n - m ) ) / n);

        return kolmogorovStat;
    }

    /**
     * Calculation of asymptotic Kolmogorov-Smirnov test suitable for long samples.
     * Number of summands is given
     */
    public static double kolmogorovDistr(double x)
    {
        if( x <= 0 )
            return 0;
        double result = 1;
        int coeff = -1;
        for( int k = 1; k < 15; k++ )
        {
            result += 2 * coeff * Math.exp( -2 * k * k * x * x);
            coeff *= -1;
        }
        return result;
    }

    /**
     * Calculation of asymptotic Kolmogorov-Smirnov distribution function.
     * Number of summands and the sample size are the input parameters.
     */
    public static double kolmogorovSmirnovDistributionFunction(double x, int numberOfSummands, int sampleSize)
    {
        if( x <= 0 )
            return 0;
        double xx = Math.sqrt(sampleSize);
        xx = x * (xx + 0.12 + 0.11 / xx);
        double result = 0;
        int coeff = 1;
        for( int k = 1; k <= numberOfSummands; k++ )
        {
            result += coeff * Math.exp( -2 * k * k * xx * xx);
            coeff *= -1;
        }
        result *= 2;
        return 1 - result;
    }
    
    /**
     * Calculation of Kolmogorov-Smirnov statistic for testing uniformity.
     * Conditions on input parameters:
     * X[0] <= X[1] <= X[2] <=... ;
     * 0 < X[i] < 'maximum' for all i;
     * 
     */
    public static double calcKolmogorovSmirnovUniformityStatistic(int X[], int maximum)
    {
     int n = X.length;
     if( n == 0 )
         return 0;
     double constant = 1.0 / (n);
     double max1 = -2.0;
     double max2 = -2.0;
     for( int i = 1; i <= n; i++ )
     {
         double f = ((double)X[i-1]) / maximum;
         double fn = ((double)i) / ((double)n);
         double x = fn - f;
         if( x > max1 ) max1 = x;
         double xx = constant - x;
         if( xx > max2 ) max2 = xx;
     }
     double result = max1 >= max2 ? max1 : max2;
     return result;
    }
    
    /**
     * Calculation of Kolmogorov-Smirnov statistic for testing the exponential distribution.
     * Conditions on input parameters:
     * X[0] <= X[1] <= X[2] <=... ;
     * 
     */
    public static double calcKolmogorovSmirnovExponentialStatistic(int X[])
    {
     int n = X.length;
     if( n == 0 )
         return 0;
     int sum = 0;
     for( int j = 0; j < n; j++ )
         sum += X[j];
     double mean = ((double)sum) / ((double)n);
     double constant = 1.0 / (n);
     double max1 = -2.0;
     double max2 = -2.0;
     for( int i = 1; i <= n; i++ )
     {
         double f = 1.0 - Math.exp(-((double)X[i-1]) / mean);
         double fn = ((double)i) / ((double)n);
         double x = fn - f;
         if( x > max1 ) max1 = x;
         double xx = constant - x;
         if( xx > max2 ) max2 = xx;
     }
     double result = max1 >= max2 ? max1 : max2;
     return result;
    }

    /**
     * Calculation of Kolmogorov-Smirnov statistic for testing the exponential distribution.
     */
    public static double calcKolmogorovSmirnovExponentialStatistic(List<Integer> sample)
    {
     int n = sample.size();
     if( n < 3 )
         return -1.0;
     double mean = getMeanValue(sample);
     Map<Integer, Integer> sampleValueAndFrequency = getOrderedDensityForIntegerValuedSample(sample);
     if( sampleValueAndFrequency.size() < 3 )
         return -1.0;
     double max1 = -2.0;
     double max2 = -2.0;
     int sumOfFrequencies = 0;
     for( Map.Entry<Integer, Integer> entry : sampleValueAndFrequency.entrySet() )
     {
         int value = entry.getKey();
         int frequency = entry.getValue();
         double f = 1.0 - Math.exp(-((double)value) / mean);
         double xx = f - (double)sumOfFrequencies / (double)n;
         if( xx > max2 ) max2 = xx;
         sumOfFrequencies += frequency;
         double fn = (double)sumOfFrequencies / (double)n;
         double x = fn - f;
         if( x > max1 ) max1 = x;
     }
     double result = max1 >= max2 ? max1 : max2;
     return result;
    }
    
    /**
     * Calculation of Kolmogorov-Smirnov statistic for testing the exponential distribution.
     * Output: Number[0] = Kolmogorov-Smirnov statistic (double)
     *         Number[1] = element of sample for which the maximal difference between empirical
     *                     and theoretical distribution functions was observed (Integer)
     */
    public static Number[] calcKolmogorovSmirnovExponentialStatistic1(List<Integer> sample)
    {
     int n = sample.size();
     if( n < 3 )
         return null;
     double mean = getMeanValue(sample);
     Map<Integer, Integer> sampleValueAndFrequency = getOrderedDensityForIntegerValuedSample(sample);
     if( sampleValueAndFrequency.size() < 3 )
         return null;
     double max1 = -2.0;
     double max2 = -2.0;
     Integer value1 = null;
     Integer value2 = null;
     int sumOfFrequencies = 0;
     for( Map.Entry<Integer, Integer> entry : sampleValueAndFrequency.entrySet() )
     {
         int value = entry.getKey();
         int frequency = entry.getValue();
         double f = 1.0 - Math.exp(-((double)value) / mean);
         double xx = f - (double)sumOfFrequencies / (double)n;
         if( xx >= max2 )
         {
             max2 = xx;
             value2 = value;
         }
         sumOfFrequencies += frequency;
         double fn = (double)sumOfFrequencies / (double)n;
         double x = fn - f;
         if( x >= max1 )
         {
             max1 = x;
             value1 = value;
         }
     }
     double statistic = max1;
     Integer valueOfMaximaldevirgence = value1;
     if( max2 < max1 )
     {
         statistic = max2;
         valueOfMaximaldevirgence = value2;
     }
     return new Number[] {statistic, valueOfMaximaldevirgence};
    }

    /***
     * Initial sample is divided step by step into 2 subsamples.
     * 1-st subsample (exponentialSubsample) represent the sample that has exponential distribution.
     * 2-nd subsample (outliers) is the residual part of initial sample.
     * Exponentiality is estimated by Kolmogorov-Smirnov test for exponentiality
     * @param sample
     * @param pValue
     * @return
     */
    public static Map<String, List<Integer>> getKolmogorovSmirnovExponentialSubsample(List<Integer> sample, double pValue)
    {
        Map<String, List<Integer>> result = new HashMap<>();
        List<Integer> exponentialSubsample = new ArrayList<>(sample);
        List<Integer> outliers = new ArrayList<>();
        result.put("exponentialSubsample", exponentialSubsample);
        result.put("outliers", outliers);
        while ( exponentialSubsample.size() >= 3 )
        {
            Number[] statisticAndValueOfMaximaldevirgence = calcKolmogorovSmirnovExponentialStatistic1(exponentialSubsample);
            if( statisticAndValueOfMaximaldevirgence == null )
                return result;
            double statistic = statisticAndValueOfMaximaldevirgence[0].doubleValue();
            double pvalue = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(statistic, 25, exponentialSubsample.size());
            if( pvalue > pValue )
                return result;
            int valueOfMaximaldevirgence = statisticAndValueOfMaximaldevirgence[1].intValue();
            exponentialSubsample.remove(valueOfMaximaldevirgence);
            outliers.add(valueOfMaximaldevirgence);
        }
        return result;
    }
    
    public static double getExponentialDensity(double x, double parameter)
    {
        if( x <= 0.0 )
            return 0.0;
        return(parameter * Math.exp(-parameter * x));
    }

    public static double getNormalDensity(double x, double mean, double sigma)
    {
        double result = (x - mean) / sigma;
        result *= result;
        result = Math.exp(-result / 2.0);
        result /= sigma;
        result /= SQRT_OF_2PI;
        return result;
    }
    
    /**
     * Calculation of von Mises distribution
     */
    public static double misesDistr(double x) throws Exception
    {
        if( x <= 0 )
            return 0;
        double result = 0;
        double arg = 0;
        for( int k = 0; k < 6; k++ )
        {
            arg = ( 4 * k + 1 ) * ( 4 * k + 1 ) / ( 16 * x );
            result += ( gammaFunc(k + 0.5) * Math.sqrt(4 * k + 1) * Math.exp( -arg) * ( Util.bessel(arg, -0.25) - Util.bessel(arg, 0.25) ) )
                    / ( gammaFunc(0.5) * gammaFunc(k + 1) );
        }
        result /= Math.sqrt(2 * x);
        return result;
    }


    /**
     * Calculation of von Mises distribution
     */
    public static double tabledMisesDistr(double x)
    {
        int i;
        double result = 0;
        double[] points = new double[150];
        double[] values = new double[150];
        points[0] = 0;
        for( i = 1; i < 150; i++ )
            points[i] = points[i - 1] + 0.01;
        values[0] = 0;
        values[1] = 0.00001;
        values[2] = 0.003;
        values[3] = 0.02568;
        values[4] = 0.06685;
        values[5] = 0.12372;
        values[6] = 0.18602;
        values[7] = 0.24844;
        values[8] = 0.30815;
        values[9] = 0.36386;
        values[10] = 0.41513;
        values[11] = 0.46196;
        values[12] = 0.50457;
        values[13] = 0.54329;
        values[14] = 0.57846;
        values[15] = 0.61042;
        values[16] = 0.63951;
        values[17] = 0.666;
        values[18] = 0.69019;
        values[19] = 0.71229;
        values[20] = 0.73253;
        values[21] = 0.75109;
        values[22] = 0.76814;
        values[23] = 0.78383;
        values[24] = 0.79829;
        values[25] = 0.81163;
        values[26] = 0.82396;
        values[27] = 0.83536;
        values[28] = 0.84593;
        values[29] = 0.85573;
        values[30] = 0.86483;
        values[31] = 0.87329;
        values[32] = 0.88115;
        values[33] = 0.88848;
        values[34] = 0.89531;
        values[35] = 0.90167;
        values[36] = 0.90762;
        values[37] = 0.91317;
        values[38] = 0.91836;
        values[39] = 0.92321;
        values[40] = 0.92775;
        values[41] = 0.93201;
        values[42] = 0.93599;
        values[43] = 0.93972;
        values[44] = 0.94323;
        values[45] = 0.94651;
        values[46] = 0.9496;
        values[47] = 0.95249;
        values[48] = 0.95521;
        values[49] = 0.95777;
        values[50] = 0.96017;
        values[51] = 0.96242;
        values[52] = 0.96455;
        values[53] = 0.96655;
        values[54] = 0.96843;
        values[55] = 0.9702;
        values[56] = 0.97186;
        values[57] = 0.97343;
        values[58] = 0.97491;
        values[59] = 0.9763;
        values[60] = 0.97762;
        values[61] = 0.97886;
        values[62] = 0.98002;
        values[63] = 0.98112;
        values[64] = 0.98216;
        values[65] = 0.98314;
        values[66] = 0.98406;
        values[67] = 0.98493;
        values[68] = 0.98575;
        values[69] = 0.98653;
        values[70] = 0.98726;
        values[71] = 0.98795;
        values[72] = 0.98861;
        values[73] = 0.98922;
        values[74] = 0.98981;
        values[75] = 0.99036;
        values[76] = 0.99088;
        values[77] = 0.99137;
        values[78] = 0.99183;
        values[79] = 0.99227;
        values[80] = 0.99268;
        values[81] = 0.99308;
        values[82] = 0.99345;
        values[83] = 0.9938;
        values[84] = 0.99413;
        values[85] = 0.99444;
        values[86] = 0.99474;
        values[87] = 0.99502;
        values[88] = 0.99528;
        values[89] = 0.99553;
        values[90] = 0.99577;
        values[91] = 0.99599;
        values[92] = 0.99621;
        values[93] = 0.99641;
        values[94] = 0.9966;
        values[95] = 0.99678;
        values[96] = 0.99695;
        values[97] = 0.99711;
        values[98] = 0.99726;
        values[99] = 0.9974;
        values[100] = 0.99754;
        values[101] = 0.99764;
        values[102] = 0.99776;
        values[103] = 0.99787;
        values[104] = 0.99799;
        values[105] = 0.99812;
        values[106] = 0.9982;
        values[107] = 0.99828;
        values[108] = 0.99837;
        values[109] = 0.99847;
        values[110] = 0.99856;
        values[111] = 0.99862;
        values[112] = 0.99869;
        values[113] = 0.99876;
        values[114] = 0.99883;
        values[115] = 0.9989;
        values[116] = 0.99895;
        values[117] = 0.999;
        values[118] = 0.99905;
        values[119] = 0.9991;
        values[120] = 0.99916;
        values[121] = 0.99919;
        values[122] = 0.99923;
        values[123] = 0.99927;
        values[124] = 0.99931;
        values[125] = 0.99935;
        values[126] = 0.99938;
        values[127] = 0.99941;
        values[128] = 0.99944;
        values[129] = 0.99947;
        values[130] = 0.9995;
        values[131] = 0.99953;
        values[132] = 0.99955;
        values[133] = 0.99957;
        values[134] = 0.99959;
        values[135] = 0.99962;
        values[136] = 0.99964;
        values[137] = 0.99965;
        values[138] = 0.99967;
        values[139] = 0.99969;
        values[140] = 0.99971;
        values[141] = 0.99972;
        values[142] = 0.99973;
        values[143] = 0.99975;
        values[144] = 0.99976;
        values[145] = 0.99978;
        values[146] = 0.99978;
        values[147] = 0.99979;
        values[148] = 0.9998;
        values[149] = 0.9998;

        if( x <= points[149] && x > 0 )
        {
            for( i = 149; i > 0; i-- )
            {
                if( x <= points[i] && x > points[i - 1] )
                {
                    result = ( points[i] - x ) * values[i - 1] / ( points[i] - points[i - 1] ) + ( points[i - 1] - x ) * values[i]
                            / ( points[i - 1] - points[i] );
                    break;
                }
            }
        }
        else if( x > points[149] )
            result = 1;
        else if( x <= points[0] )
            result = 0;
        return result;
    }

    /**
     * Calculation of Lehmann-Rosenblatt statistics
     */
    public static double lehmannRosenblattTest(double[] sample1, double[] sample2)
    {
        int i, tempi = 0, count = 1;
        boolean equaltonext = false;
        double lrStatistic = 0, rang;
        double temp1 = 0, temp2 = 0;
        int m = sample1.length;
        int n = sample2.length;
        Util.sortShell(sample1);
        Util.sortShell(sample2);
        double[] apparr = DoubleStreamEx.of( sample1 ).append( sample2 ).toArray();

        double[] rangarr = new double[m + n];
        int[] pos = Util.sortHeap(apparr);

        for( i = 0; i < m + n - 1; i++ )
        {
            if( apparr[i] == apparr[i + 1] )
            {
                tempi += i + 1;
                count++;
                equaltonext = true;
            }
            else
            {
                if( equaltonext )
                {
                    rang = ( (double) ( tempi + i + 1 ) ) / count;
                    for( int j = i - count + 1; j <= i; j++ )
                        rangarr[pos[j]] = rang;
                    equaltonext = false;
                    count = 1;
                    tempi = 0;
                }
                else
                    rangarr[pos[i]] = i + 1;
            }
        }

        if( equaltonext )
        {
            rang = ( (double) ( tempi + m + n ) ) / count;
            for( int j = m + n - count; j <= m + n - 1; j++ )
                rangarr[pos[j]] = rang;
        }
        else
            rangarr[pos[m + n - 1]] = m + n;


        for( i = 0; i < m; i++ )
        {
            temp1 += ( rangarr[i] - ( i + 1 ) ) * ( rangarr[i] - ( i + 1 ) );
        }
        temp1 *= m;
        for( i = m; i < m + n; i++ )
        {
            temp2 += ( rangarr[i] - ( i + 1 - m ) ) * ( rangarr[i] - ( i + 1 - m ) );
        }
        temp2 *= n;

        lrStatistic = ( temp1 + temp2 ) / ( ( m + n ) * m * n ) - ( (double) ( 4 * m * n - 1 ) ) / ( 6 * ( m + n ) );

        return lrStatistic;
    }

    /**
     * Estimating of observational distribution function of some sample
     */
    public static double observationalDistr(double[] values, double t)
    {
        int n = values.length;
        return IntStreamEx.ofIndices( values, val -> val > t ).findFirst().orElse( n ) / (double)n;
    }

    /**
     * Calculation of Kolmogorov statistic with shift correction
     */
    public static double kolmogorovStatistic(double[] sample1, double[] sample2)
    {
        double d = 0, kolmogorovStat = 0, tempd = 0;
        int i;
        int m = sample1.length;
        int n = sample2.length;
        Util.sortShell(sample1);
        Util.sortShell(sample2);
        for( i = 0; i < m; i++ )
        {
            tempd = (double) ( i + 1 ) / m - observationalDistr(sample2, sample1[i]);
            if( tempd > d )
                d = tempd;
        }

        for( i = 0; i < n; i++ )
        {
            tempd = (double) ( i + 1 ) / n - observationalDistr(sample1, sample2[i]);
            if( tempd > d )
                d = tempd;
        }
        kolmogorovStat = Math.sqrt((double)m * n / ( m + n )) * ( d + ( m + n ) / ( 4.6 * m * n ) );

        return kolmogorovStat;
    }

    /**
     * Poisson distribution value
     * @param n - your observation
     * @param lambda - lambda of poisson distribution
     * @param lower - if false, calculate the upper tail CDF
     */
    public static double poissonDistribution(int n, double lambda, boolean lower)
    {
        if( lambda < 0 )
            throw new InvalidParameterException("Lambda must be > 0");
        if( lower )
            if( lambda > 700 )
                return poissonDistributionLargeLambda(n, lambda);
            else
                return poissonDistributionSmallLambda(n, lambda);
        else if( lambda > 700 )
            return poissonDistributionQLargeLambda(n, lambda);
        else
            return poissonDistributionQSmallLambda(n, lambda);
    }

    /**
     * Works for lambda < 745
     * @see poissonDistribution
     */
    private static double poissonDistributionSmallLambda(int n, double lambda)
    {
        if( n < 0 )
            return 0;
        double next = Math.exp( -lambda);
        double cdf = next;
        for( int i = 1; i <= n; i++ )
        {
            next = next * lambda / i;
            cdf += next;
        }
        return cdf > 1 ? 1 : cdf;
    }

    /**
     * Works for large lambda, but slower than poissonDistributionSmallLambda
     * @see poissonDistribution
     */
    private static final int LSTEP = 200;
    private static final double EXPTHRES = Math.exp(LSTEP);
    private static final double EXPSTEP = Math.exp( -LSTEP);
    private static double poissonDistributionLargeLambda(int n, double lambda)
    {
        if( n < 0 )
            return 0;
        int numParts = (int)Math.floor(lambda / LSTEP);
        double lastPart = lambda - numParts * LSTEP;
        double lastExp = Math.exp( -lastPart);
        double next = EXPSTEP;
        numParts--;
        double cdf = next;
        for( int i = 1; i <= n; i++ )
        {
            next = next * lambda / i;
            cdf += next;
            if( next > EXPTHRES || cdf > EXPTHRES )
            {
                if( numParts >= 1 )
                {
                    cdf *= EXPSTEP;
                    next *= EXPSTEP;
                    numParts--;
                }
                else
                {
                    cdf *= lastExp;
                    lastExp = 1;
                }
            }
        }
        for( int i = 0; i < numParts; i++ )
            cdf *= EXPSTEP;
        cdf *= lastExp;
        return cdf;
    }

    /**
     * Works for lambda < 745
     * @see poissonDistribution
     */
    private static double poissonDistributionQSmallLambda(int n, double lambda)
    {
        if( n < 0 )
            return 1;
        double next = Math.exp( -lambda);
        int i;
        for( i = 1; i <= n; i++ )
            next = next * lambda / i;
        double cdf = 0;
        i = n + 1;
        while( next > 0 )
        {
            next = next * lambda / i;
            cdf += next;
            i++;
        }
        return cdf;
    }

    /**
     * Works for large lambda, but slower than poissonDistributionSmallLambda
     * @see poissonDistribution
     */
    private static double poissonDistributionQLargeLambda(int n, double lambda)
    {
        if( n < 0 )
            return 1;
        int numParts = (int)Math.floor(lambda / LSTEP);
        double lastPart = lambda - numParts * LSTEP;
        double lastExp = Math.exp( -lastPart);
        double next = EXPSTEP;
        numParts--;
        int i;
        for( i = 1; i <= n; i++ )
        {
            next = next * lambda / i;
            if( next > EXPTHRES )
            {
                if( numParts >= 1 )
                {
                    next *= EXPSTEP;
                    numParts--;
                }
                else
                    lastExp = 1;
            }
        }
        double cdf = 0;
        i = n + 1;
        while( next > 0 )
        {
            next = next * lambda / i;
            cdf += next;
            i++;
            if( next > EXPTHRES || cdf > EXPTHRES )
            {
                if( numParts >= 1 )
                {
                    cdf *= EXPSTEP;
                    next *= EXPSTEP;
                    numParts--;
                }
                else
                {
                    cdf *= lastExp;
                    lastExp = 1;
                }
            }
        }
        for( i = 0; i < numParts; i++ )
            cdf *= EXPSTEP;
        cdf *= lastExp;
        return cdf;
    }

    /**
     * Inverse Poisson distribution
     * @param cdf - Poisson distribution value
     * @param lambda - lambda of Poisson distribution
     * @return minimal number of trials for which distribution value exceeds cdf
     * Note that if result value > 1000, then 1000 is returned
     * Also it doesn't work for lambda > 740
     */
    public static int poissonDistributionInv(double cdf, double lambda)
    {
        return poissonDistributionInv(cdf, lambda, 1000);
    }

    public static int poissonDistributionInv(double cdf, double lambda, int maximum)
    {
        if( lambda > 740 )
            throw new UnsupportedOperationException(
                    "poissonDistributionInv: current implementation doesn't support lambda > 740 (supplied lambda is "
                            + String.valueOf(lambda) + ")");
        if( cdf < 0 || cdf > 1 )
            throw new InvalidParameterException("CDF must be >=0 and <= 1 (supplied value is " + String.valueOf(cdf) + ")");
        if( cdf == 0 )
            return 0;
        double newVal = Math.exp( -lambda);
        double sum2 = newVal;
        for( int i = 1; i <= maximum; i++ )
        {
            double sumold = sum2;
            newVal = newVal * lambda / i;
            sum2 += newVal;
            if( cdf >= sumold && cdf <= sum2 )
                return i;
        }
        return maximum;
    }

/********************////////////////////////////////////////////************************/
    
    //////////  class KolmogorovSmirnovTests will be moved out of class Stat after its construction
    public static class KolmogorovSmirnovTests
    {
        /***
         * condition: sample has to be sorted  (in increasing order), for example, Collections.sort(sample);
         * @param sample
         * @return statistics
         */
        public static double getStatisticsOfNormalityTest(List<Double> sample)
        {
            if( sample.isEmpty() ) return -1.0;
            int n = sample.size();
            if( n < 5 ) return -1.0;
            double[] meanAndSigma = Stat.getMeanAndSigma1(sample);
            double variance = meanAndSigma[1] * meanAndSigma[1];
            double max1 = -2.0, max2 = -2.0, constant = 1.0 / n;
            for( int m = 1; m <= n; m++ )
            {
                double x = sample.get(m - 1);
                double f = Stat.normalDistribution(x, meanAndSigma[0], variance);
                double fn = (double)m / (double)n;
                double y = fn - f;
                if( y > max1 )
                    max1 = y;
                y = constant - y;
                if( y > max2 )
                    max2 = y;
            }
            return max1 >= max2 ? max1 : max2;
        }
        
        /***
         * condition: sample has to be sorted, for example, Collections.sort(sample);
         * @param sample
         * @return Object[0] is statistics of Kolmogorov-Smirnov test (double), Object[1] is index of maximal divergence from normality (int)
         */
        public static Object[] getMaximalDivergenceFromNormality(List<Double> sample)
        {
            int n = sample.size();
            double[] meanAndSigma = Stat.getMeanAndSigma1(sample);
            double variance = meanAndSigma[1] * meanAndSigma[1], constant = 1.0 / n;
            return EntryStream.of(sample)
                .mapValues( x -> Stat.normalDistribution(x, meanAndSigma[0], variance) )
                .mapToValue( (mm, f) -> (mm + 1.0)/n - f )
                .mapValues( y -> Math.max(y, constant - y) )
                .maxBy( Entry::getValue )
                .map( entry -> new Object[] {entry.getValue(), entry.getKey()} )
                .orElseGet( () -> new Object[] {-2.0, 0} );
        }
        
        public static Map<String, List<Double>> splitIntoNormalSubsampleAndOutliers(List<Double> sample, double pValueThreshold)
        {
            if( sample.size() < 5 ) return null;
            Map<String, List<Double>> result = new HashMap<>();
            List<Double> outliers = new ArrayList<>();
            List<Double> normalSubsample = StreamEx.of(sample).sorted().toList();
            while ( normalSubsample.size() >= 4 )
            {
                Object[] object = getMaximalDivergenceFromNormality(normalSubsample);
                double x = (Double)object[0];
                int i = (Integer)object[1];
                double pvalue = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(x, 25, normalSubsample.size());
                if( pvalue >= pValueThreshold ) break;
                outliers.add(normalSubsample.remove(i));
            }
            result.put("normalSubsample", normalSubsample);
            result.put("outliers", outliers);
            return result;
        }
    }

/********************* DistributionMixture : start **********************/
    public static class DistributionMixture
    {
        /***
         * Expectation Step in EM-algorithm for identification of k-component normal mixture.
         * The probabilities Pij are defined as Pij = is the probability that i-th element of sample belongs to j-th component of mixture.
         * 
         * @param meansAndSigmas
         * @param probabilitiesOfMixtureComponents
         * @param sample
         * @return probabilitiesPij[][]; probabilitiesPij[i][j] = Pij;
         ***/
        public static double[][] getProbabilitiesPijForNormalMixture(double[][] meansAndSigmas, double[] probabilitiesOfMixtureComponents, List<Double> sample)
        {
            int n = sample.size(), k = probabilitiesOfMixtureComponents.length;
            double[][] probabilitiesPij = new double[n][k];
            for( int i = 0; i < n; i++ )
                for( int j = 0; j < k; j++ )
                    probabilitiesPij[i][j] = probabilitiesOfMixtureComponents[j] * Stat.getNormalDensity(sample.get(i), meansAndSigmas[j][0], meansAndSigmas[j][1]);
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
        public static Object[] getParametersOfNormalMixture(double[][] probabilitiesPij, List<Double> sample)
        {
            int n = probabilitiesPij.length, k = probabilitiesPij[0].length;
            double[][] meansAndSigmas = new double[k][2];
            double[] probabilitiesOfMixtureComponents = new double[k];
            for( int j = 0; j < k; j++ )
            {
                double sum1 = 0.0, sum2 = 0.0, sum3 = 0.0;
                for( int i = 0; i < n; i++ )
                {
                    double x = sample.get(i);
                    sum1 += probabilitiesPij[i][j];
                    double xx = probabilitiesPij[i][j] * x;
                    sum2 += xx;
                    sum3 += xx * x;
                }
                probabilitiesOfMixtureComponents[j] = sum1 / n;
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
        public static Object[] estimateNormalMixtureByEmAlgorithm(double[][] meansAndSigmas, double[] probabilitiesOfMixtureComponents, List<Double> sample, int maximalNumberOfIterations)
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
               for( int j = 0; j < probabilitiesOfMixtureComponents.length; j++ )
                   if( newProbabilitiesOfMixtureComponents[j] != oldProbabilitiesOfMixtureComponents[j] )
                   {
                       isEqual = false;
                       break;
                   }
               if( isEqual ) break;
               oldProbabilitiesOfMixtureComponents = newProbabilitiesOfMixtureComponents.clone();
           }
           if( numberOfIterations > maximalNumberOfIterations )
               numberOfIterations = maximalNumberOfIterations;
           return new Object[]{newMeansAndSigmas, newProbabilitiesOfMixtureComponents, numberOfIterations, probabilitiesPij};
        }
        
        /***
         * Divide the sample (List<Double>) into k sub-samples by using random value generator and probabilities Pij,
         * where Pij = the probability that i-th element of sample belongs to j-th sub-sample, j=0,...,k-1.
         * @param probabilitiesPij
         * @param sample
         * @param random TODO
         * @return subsamples (List<List<Double>>)
         */
        public static List<List<Double>> getSubsamplesSimulatedByProbabilitiesPij(double[][] probabilitiesPij, List<Double> sample, Random random)
        {
            List<List<Double>> result = new ArrayList<>();
            int numberOfSubsamples = probabilitiesPij[0].length;
            for( int j = 0; j < numberOfSubsamples; j++ )
                result.add(new ArrayList<Double>());
            for( int i = 0; i < sample.size(); i++ )
            {
                double randomValue = random.nextDouble(), sum = 0.0;
                for( int j = 0; j < numberOfSubsamples; j++ )
                {
                    sum += probabilitiesPij[i][j];
                    if( sum >= randomValue )
                    {
                        result.get(j).add(sample.get(i));
                        break;
                    }
                }
            }
            return result;
        }

        //////////////////////////////O.K.
        /***
         * @param sample
         * @param numberOfMixtureComponents
         * @param maximalNumberOfIterations
         * @param random
         * @return Map<Integer, Object[]> : Integer - index of mixture component, 0 <= Integer <= numberOfMixtureComponents;
         * When 0 <= Integer <= numberOfMixtureComponents - 1, dim(Object[]) = 4,
         * objects[0] = probability of Integer-th MixtureComponent,
         * objects[1] = double[2] = meanAndSigmaFromMixture for Integer-th MixtureComponent,
         * objects[2] = double[2] = meanAndSigmaFromSimulation for Integer-th MixtureComponent,
         * objects[3] = List<Double> = Integer-th subsample;
         * If Integer == numberOfMixtureComponents, then dim(Object[]) = 1, Object[0] = numberOfIterations
         */
        public static Map<Integer, Object[]> getNormalMixture(List<Double> sample, int numberOfMixtureComponents, int maximalNumberOfIterations, Random random)
        {
            Map<Integer, Object[]> result = new HashMap<>();
            if( sample.isEmpty() || sample.size() < 3 * numberOfMixtureComponents) return null;
            double[][] meansAndSigmas = new double[numberOfMixtureComponents][];
            double[] probabilitiesOfMixtureComponents = new double[numberOfMixtureComponents];
            double[] meanAndSigma = Stat.getMeanAndSigma1(sample);
            Double min = sample.get(0), max = min;
            for( Double x : sample )
            {
                min = Math.min(x, min);
                max = Math.max(x, max);
            }
            double h = (max - min) / (numberOfMixtureComponents + 1);
            for( int i = 0; i < numberOfMixtureComponents; i++ )
            {
                probabilitiesOfMixtureComponents[i] = 1.0 / numberOfMixtureComponents;
                meansAndSigmas[i] = new double[]{(i + 1) * h, meanAndSigma[1]};
            }
            Object[] objects = estimateNormalMixtureByEmAlgorithm(meansAndSigmas, probabilitiesOfMixtureComponents, sample, maximalNumberOfIterations);
            double[][] meansAndSigmasFromMixture = (double[][])objects[0];
            double[] newProbabilitiesOfMixtureComponents = (double[])objects[1];
            Integer numberOfIterations = (Integer)objects[2];
            double[][] probabilitiesPij = (double[][])objects[3];
            List<List<Double>> indexAndSubsample = getSubsamplesSimulatedByProbabilitiesPij(probabilitiesPij, sample, random);
            double[] meanAndSigmaFromSimulation;
            for( int i = 0; i < numberOfMixtureComponents; i++ )
            {
                List<Double> subsample = indexAndSubsample.get(i);
                if( ! subsample.isEmpty() && subsample.size() > 1 )
                    meanAndSigmaFromSimulation = Stat.getMeanAndSigma1(subsample);
                else
                    meanAndSigmaFromSimulation = new double[]{0.0, 0.0};
                result.put(i, new Object[]{newProbabilitiesOfMixtureComponents[i], meansAndSigmasFromMixture[i], meanAndSigmaFromSimulation, subsample});
            }
            result.put(numberOfMixtureComponents, new Object[]{numberOfIterations});
            return result;
        }
    }
    /********************* DistributionMixture : finish **********************/
    

    /**
     * Algorithm Algorithm AS 91: The Percentage Points of the χ2 Distribution
     * D. J. Best, D. E. Roberts
     * Journal of the Royal Statistical Society. Series C (Applied Statistics), Vol. 24, No. 3 (1975), pp. 385-388 (4 pages)
     * https://doi.org/10.2307/2347113
     * @param df - degrees of freedom
     * @param p - probability
     * @return quantile
     * @throws Exception
     */
    public static double quantileChiSquare(int df, double p) throws Exception
    {
        if( p < 0.000002 || p > 0.999998 )
            throw new IllegalArgumentException();
        double xx = 0.5 * df;
        double c = xx - 1;
        double g = logGamma( df / 2.0 );
        double e = 0.5E-6;
        double aa = 0.6931471805;
        double result;

        if( df >= -1.24 * Math.log( p ) )
        {
            if( df > 0.32 )
            {
                double x = getStandartNormalQuantile( p, 0.0001, 100 );
                double p1 = 0.222222 / df;
                result = df * Math.pow( ( x * Math.sqrt( p1 ) + 1 - p1 ), 3 );
                if( result > 2.2 * df + 6 )
                    result = -2 * Math.log( 1 - p ) - c * Math.log( result / 2 ) + g;
            }
            else
            {
                result = 0.4;
                double a = Math.log( 1 - p );
                double q;
                do
                {
                    q = result;
                    double p1 = 2 + result * ( 4.67 + result );
                    double p2 = result * ( 6.73 + result * ( 6.66 + result ) );
                    double t = -0.5 + ( 4.67 + 2 * result ) / p1 - ( 6.73 + result * ( 13.32 + 3 * result ) ) / p2;
                    result = result - ( 1 - Math.exp( a + g + 0.5 * result + c * aa ) * p2 / p1 ) / t;
                }
                while( Math.abs( q / result - 1 ) > 0.01 );
            }
        }
        else
        {
            result = Math.pow( p * xx * Math.exp( g + xx * aa ), 1 / xx );
            if( result < e )
                return result;
        }
        double q = result;
        do
        {
            q = result;
            double p1 = 0.5 * result;
            double p2 = p - iGammaFunction( p1, xx, 50 );// GAMAIN( P1, XX, G );
            double t = p2 * Math.exp( xx * aa + g + p1 - c * Math.log( result ) );
            double b = t / result;
            double a = 0.5 * t - b * c;
            double s1 = ( 210 + a * ( 140 + a * ( 105 + a * ( 84 + a * ( 70 + 60 * a ) ) ) ) ) / 420;
            double s2 = ( 420 + a * ( 735 + a * ( 966 + a * 1141 + 1278 * a ) ) ) / 2520;
            double s3 = ( 210 + a * ( 462 + a * ( 707 + 932 * a ) ) ) / 2520;
            double s4 = ( 252 + a * ( 672 + 1182 * a ) + c * ( 294 + a * ( 889 + 1740 * a ) ) ) / 5040;
            double s5 = ( 84 + 264 * a + c * ( 175 + 606 * a ) ) / 2520;
            double s6 = ( 120 + c * ( 346 + 127 * c ) ) / 5040;
            result = result + t * ( 1 + 0.5 * t * s1 - b * c * ( s1 - b * ( s2 - b * ( s3 - b * ( s4 - b * ( s5 - b * s6 ) ) ) ) ) );
        }
        while( Math.abs( q / result - 1 ) > e );
        return result;
    }
}