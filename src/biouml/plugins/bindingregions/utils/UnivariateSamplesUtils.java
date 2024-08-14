package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ru.biosoft.analysis.Stat;

/**
 * @author yura
 *
 */

public class UnivariateSamplesUtils
{
    public static Map<String, double[]> getSampleNameAndSample(String[] sampleNamesForEachMeasurement, double[] samples)
    {
        Map<String, List<Double>> preResult = new HashMap<>();
        Map<String, double[]> result = new HashMap<>();
        for( int i = 0; i < samples.length; i++ )
            preResult.computeIfAbsent(sampleNamesForEachMeasurement[i], key -> new ArrayList<>()).add(samples[i]);
        for( Entry<String, List<Double>> entry : preResult.entrySet() )
            result.put(entry.getKey(), MatrixUtils.fromListToArray(entry.getValue()));
        return result;
    }
    
    // Interesting !!! This test coincided with Wilcoxon two-sample rank test (normal approximation) StatUtil.getWilcoxonTwoSampleRankTest() !!!
    public static double[] compareTwoFrequenciesByNormalApproximation(int frequency1, int sampleSize1, int frequency2, int sampleSize2)
    {
        double statistic = frequency1 + frequency2, n = sampleSize1 + sampleSize2;
        double probability1 = (double)frequency1 / (double)sampleSize1, probability2 = (double)frequency2 / (double)sampleSize2;
        statistic = statistic * ( n - statistic);
        statistic /= (n - 1.0) * sampleSize1 * sampleSize2;
        statistic = (probability1 - probability2) / Math.sqrt(statistic);
        double pValue = 2.0 * (1.0 - Stat.standartNormalDistribution(Math.abs(statistic)));
        return new double[]{probability1, probability2, statistic, pValue};
    }
    
    /*************** NonParametricAnova : start ******************/
    public static class NonParametricAnova
    {
        public static double[] getKruskalWallisTest(String[] sampleNamesForEachMeasurement, double[] samples) throws Exception
        {
            Object[] objects = Stat.getRanks(samples);
            double[] ranks = (double[])objects[0], tieCorrections = (double[])objects[1];
            Map<String, double[]> sampleNameAndSample = getSampleNameAndSample(sampleNamesForEachMeasurement, ranks);
            double statistic = 0.0, mean = (samples.length + 1.0) / 2.0;
            for( double[] sample : sampleNameAndSample.values() )
            {
                double x = Stat.mean(sample) - mean;
                statistic += x * x * sample.length;
            }
            statistic *= 12.0 / ( samples.length * ( samples.length + 1.0 ) );
            if( tieCorrections[1] > 0.0 )
                statistic /= 1.0 - tieCorrections[1] / ( (double)samples.length * samples.length * samples.length - samples.length );
            double pValue = 1.0 - Stat.chiDistribution(statistic, sampleNameAndSample.size() - 1.0);
            return new double[]{statistic, pValue};
        }
    }
    /*************** NonParametricAnova : finish ******************/
}
