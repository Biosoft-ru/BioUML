
package biouml.plugins.bindingregions.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import one.util.streamex.StreamEx;
import biouml.plugins.bindingregions.utils.LinearRegression.LSregression;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.graphics.chart.Chart;

/**
 * @author yura
 *
 */
public class SampleComparison
{
    public static final String Z_SCORE = "Z-score (statistic of normal approximation)";
    
    private final Map<String, double[]> nameAndSample;
    public String commonName;
    private boolean[] firstSubsampleIndicators;
    
    public SampleComparison(Map<String, double[]> nameAndSample, String commonName, boolean[] firstSubsampleIndicators)
    {
        this.nameAndSample = new TreeMap<>(nameAndSample);
        this.commonName = commonName;
        this.firstSubsampleIndicators = firstSubsampleIndicators;
    }
    
    public SampleComparison(Map<String, double[]> nameAndSample, String commonName)
    {
        this(nameAndSample, commonName, null);
    }
    
    public SampleComparison(String sampleName1, double[] sample1, String sampleName2, double[] sample2, String commonName)
    {
        Map<String, double[]> nameAndSample = new TreeMap<>();
        nameAndSample.put(sampleName1, sample1);
        nameAndSample.put(sampleName2, sample2);
        this.nameAndSample = nameAndSample;
        this.commonName = commonName;
    }
    
    public String getCommonName()
    {
        return commonName;
    }
    
    private int getSizeOfEachFirstSubsample()
    {
        int size = 0;
        for( boolean indicator : firstSubsampleIndicators )
            if( indicator )
                size++;
        return size;
    }
    
    private SampleComparison getSubsamples(boolean isFirstSubsamples)
    {
        int n = getSizeOfEachFirstSubsample();
        if( ! isFirstSubsamples )
            n = firstSubsampleIndicators.length - n;
        Map<String, double[]> result = new HashMap<>();
        for(Entry<String, double[]> entry : nameAndSample.entrySet())
        {
            String name = entry.getKey();
            double[] sample = entry.getValue(), subsample = new double[n];
            int index = 0;
            for( int i = 0; i < firstSubsampleIndicators.length; i++ )
                if( (isFirstSubsamples && firstSubsampleIndicators[i]) || (! isFirstSubsamples && ! firstSubsampleIndicators[i] )  )
                    subsample[index++] = sample[i];
            result.put(name, subsample);
        }
        return new SampleComparison(result, commonName);
    }
    
    public SampleComparison getFirstSubsamples()
    {
        return getSubsamples(true);
    }
    
    public SampleComparison getSecondSubsamples()
    {
        return getSubsamples(false);
    }

    private String[] getSampleNames()
    {
       String[] result = new String[nameAndSample.size()];
       int index = 0;
       for( String name : nameAndSample.keySet() )
           result[index++] = name;
       return result;
    }

    // TODO: to remove (after studying 'StreamEx.of') 
    private static double getCommonMultiplier(List<double[]> yValuesForCurves)
    {
        double max = StreamEx.of(yValuesForCurves).flatMapToDouble( Arrays::stream ).max().orElse( Double.MIN_VALUE );
        if( max < 1.0 ) return 1.0;
        return Math.pow(10.0, Math.ceil(Math.log10(max)));
    }

    public Chart chartWithSmoothedDensities(Boolean doAddTwoZeroPoints, Map<String, Double> nameAndMultipliers, String windowSelector, Double givenWindow)
    {
        return DensityEstimation.chartWithSmoothedDensities(nameAndSample, commonName, doAddTwoZeroPoints, nameAndMultipliers, windowSelector, givenWindow);
    }


    public void writeChartsWithSmoothedDensitiesOfDifferences(Boolean doAddTwoZeroPoints, DataElementPath pathToTable)
    {
        for( Entry<String, double[]> entry1 : nameAndSample.entrySet() )
        {
            String name1 = entry1.getKey();
            double[] sample1 = entry1.getValue();
            Map<String, double[]> nameAndDiferencesOfSamples = new HashMap<>();
            for( Entry<String, double[]> entry2 : nameAndSample.entrySet() )
            {
                String name2 = entry2.getKey();
                double[] sample2 = entry2.getValue();
                if( name1.equals(name2) ) continue;
                nameAndDiferencesOfSamples.put("(" + name1 + ") - (" + name2 + ")", MatrixUtils.getSubtractionOfVectors(sample1, sample2));
            }
            SampleComparison sc = new SampleComparison(nameAndDiferencesOfSamples, "Differences between " + commonName + "s");
            Chart chart = sc.chartWithSmoothedDensities(doAddTwoZeroPoints, null, DensityEstimation.WINDOW_WIDTH_02, null);
            TableUtils.addChartToTable("Differences of " + name1, chart, pathToTable);
        }
    }

    //TODO: To reduce to 'MultivariateSample.writeIndividualChartsWithSmoothedDensities()'
    public static void writeIndividualChartsWithSmoothedDensities(Map<String, double[]> nameAndSample, Boolean doAddTwoZeroPoints, Map<String, Double> nameAndMultipliers, String windowSelector, Double givenWindow, DataElementPath pathToTable)
    {
        for( Entry<String, double[]> entry : nameAndSample.entrySet() )
        {
            String name = entry.getKey();
            Map<String, double[]> map = new HashMap<>();
            map.put("", entry.getValue());
            Chart chart = DensityEstimation.chartWithSmoothedDensities(map, name, doAddTwoZeroPoints, nameAndMultipliers, windowSelector, givenWindow);
            TableUtils.addChartToTable(name, chart, pathToTable);
        }
    }

    // TODO: To reduce to MultivariateSample.writeTableWithMeanAndSigma()
    public void writeTableWithMeanAndSigma(DataElementPath pathToTables, String tableName)
    {
        double[][] data = new double[nameAndSample.size()][];
        int i = 0;
        for( double[] sample : nameAndSample.values() )
        {
            double[] meanAndSigma = Stat.getMeanAndSigma(sample);
            data[i++] = new double[]{sample.length, meanAndSigma[0], meanAndSigma[1]};
        }
        TableUtils.writeDoubleTable(data, getSampleNames(), new String[] {"Sample size", "Mean", "Sigma"}, pathToTables, tableName);
    }

    public void writeTableWithWilcoxonPaired(DataElementPath pathToTables, String tableName) throws Exception
    {
        int i = 0, n = nameAndSample.size();
        n = (n * (n - 1)) / 2;
        double[][] data = new double[n][];
        String[] pairedNames = new String[n];
        for( Entry<String, double[]> entry1 : nameAndSample.entrySet() )
        {
            String name1 = entry1.getKey();
            double[] sample1 = entry1.getValue();
            for( Entry<String, double[]> entry2 : nameAndSample.entrySet() )
            {
                String name2 = entry2.getKey();
                double[] sample2 = entry2.getValue();
                if( name1.equals(name2) ) break;
                pairedNames[i] = name1 + " <=> " + name2;
                data[i++] = Stat.wilcoxonSignedRank(sample1, sample2);
            }
        }
        TableUtils.writeDoubleTable(data, pairedNames, new String[] {"Wilcoxon signed-rank statistic", Z_SCORE, "p-value"}, pathToTables, tableName);
    }
    
    public void writeTableWithBinomialTest(DataElementPath pathToTables, String tableName) throws Exception
    {
        int n = nameAndSample.size();
        n = (n * (n - 1)) / 2;
        double[][] data = new double[n][2];
        String[] pairedNames = new String[n];
        int i = 0;
        for( Entry<String, double[]> entry1 : nameAndSample.entrySet() )
        {
            String name1 = entry1.getKey();
            double[] sample1 = entry1.getValue();
            for( Entry<String, double[]> entry2 : nameAndSample.entrySet() )
            {
                String name2 = entry2.getKey();
                double[] sample2 = entry2.getValue();
                if( name1.equals(name2) ) break;
                pairedNames[i] = "(" + name1 + ") - (" + name2 + ")";
                data[i++] = Stat.binomialTestTwoSided(sample1, sample2);
            }
        }
        TableUtils.writeDoubleTable(data, pairedNames, new String[] {"Number of positive differences", "p-value"}, pathToTables, tableName);
    }
    
    /***
     * 
     * @return samples as array[][]: j-th column of array contains j-th sample;
     */
    private double[][] convertSamplesIntoMatrix()
    {
        double[][] samples = null;
        int j = 0;
        for( double[] sample : nameAndSample.values() )
        {
            if( samples == null )
                samples = new double[sample.length][nameAndSample.size()];
            for( int i = 0; i < sample.length; i++ )
                samples[i][j] = sample[i];
            j++;
        }
        return samples;
    }
    
    public void writeTableWithFriedmanRankTest(DataElementPath pathToTables, String tableName) throws Exception
    {
        double data[][] = new double[1][];
        double[][] samples = convertSamplesIntoMatrix();
        data[0] = Stat.friedmanTest(samples);
        TableUtils.writeDoubleTable(data, new String[]{"Friedman rank test"}, new String[] {"Friedman test statistic", "p-value"}, pathToTables, tableName);
    }
    
    /***
     * There are exactly 2 samples in nameAndSample
     * @param pathToTables
     * @param tableName
     * @throws Exception
     */
    public void writeTableWithTwoCorrelations(DataElementPath pathToTables, String tableName) throws Exception
    {
        double data[][] = new double[1][2];
        Object[] objects = nameAndSample.values().toArray();
        data[0][0] = Stat.pearsonCorrelation((double[])objects[0], (double[])objects[1]);
        data[0][1] = StatUtil.getSpearmanCorrelation((double[])objects[0], (double[])objects[1]);
        TableUtils.writeDoubleTable(data, new String[]{"Correlation coefficient"}, new String[] {"Pearson correlation", "Spearman correlation"}, pathToTables, tableName);
    }
    
    // TODO: to replace it by 'MultivariateSample.writeTableWithPearsonCorrelationMatrix()'
    public void writeTableWithPearsonCorrelationMatrix(DataElementPath pathToOutputs, String tableName)
    {
        double[][] matrix = convertSamplesIntoMatrix();
        double[][] correlations = MatrixUtils.transformSymmetricMatrixToSquareMatrix(MultivariateSample.getCorrelationMatrix(matrix));
        String[]  sampleNames = getSampleNames();
        TableUtils.writeDoubleTable(correlations, sampleNames, sampleNames, pathToOutputs, tableName);
    }

    public void writeTableWithAllClouds(DataElementPath pathToOutputs, String tableName)
    {
        for( Entry<String, double[]> entry1 : nameAndSample.entrySet() )
        {
            String name1 = entry1.getKey();
            double[] sample1 = entry1.getValue();
            for( Entry<String, double[]> entry2 : nameAndSample.entrySet() )
            {
                String name2 = entry2.getKey();
                double[] sample2 = entry2.getValue();
                if( name1.equals(name2) ) break;
                Chart chart = LSregression.createChartWithLineAndCloud(sample1, sample2, null, null, null, null, name1, name2);
                TableUtils.addChartToTable(name1 + "_and_" + name2, chart, pathToOutputs.getChildPath(tableName));
            }
        }
    }

    // it is necessary to test
    /***
     * 
     * @return Map(String, double[]); String - combined name of two sample names;
     * double[] - dimension = 2; contains 2 values: value of statistic and p-value;
     * @throws Exception
     */
    public Map<String, double[]> compareByStudent() throws Exception
    {
        Map<String, double[]> result = new HashMap<>();
        for( Entry<String, double[]> entry1 : nameAndSample.entrySet() )
        {
            String name1 = entry1.getKey();
            double[] sample1 = entry1.getValue();
            for( Entry<String, double[]> entry2 : nameAndSample.entrySet() )
            {
                String name2 = entry2.getKey();
                double[] sample2 = entry2.getValue();
                if( name1.equals(name2) ) break;
                double[] statisticAndPvalue = new double[2];
                statisticAndPvalue[0] = Stat.studentTest(sample1, sample2);
                double[] values = Stat.studentDistribution(statisticAndPvalue[0], sample1.length + sample2.length - 2);
                statisticAndPvalue[1] = values[1];
                result.put(name1 + " <=> " + name2, statisticAndPvalue);
            }
        }
        return result;
    }
}
