
package biouml.plugins.bindingregions.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.SmoothingParameters;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.VariableNamesSelector;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.StatUtil;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.UnivariateSamplesUtils;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.UnivariateSamplesUtils.NonParametricAnova;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 * Every variable represents the set of univariate samples (clusters, groups, classes).
 * Every variable is represented as a column in the input data matrix (TableDataCollection)
 * All variables are treated independently
 */
public class UnivariateSamplesAnalysis extends AnalysisMethodSupport<UnivariateSamplesAnalysis.UnivariateSamplesAnalysisParameters>
{
    private static final String OUTPUT_01_KRUSKAL_WALLIS_TEST = "Kruskal-Wallis test";
    private static final String OUTPUT_02_WILCOXON_FOR_ALL_PAIRS_OF_SAMPLES = "Wilcoxon two-sample rank test for all pairs of samples";
    private static final String OUTPUT_03_WRITE_SAMPLE_DENSITIES = "Write charts with sample densities";
    private static final String OUTPUT_04_TWO_FREQUENCIES_COMPARISON_BY_NORMALITY = "Comparison of two observed frequencies with the help of normal approximation";

    public UnivariateSamplesAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new UnivariateSamplesAnalysisParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Univariate samples analysis: every variable represents the set of univariate samples (clusters, groups, classes");
        log.info("Every variable is represented as a column in the input data matrix");
        log.info("All variables are treated independently");
        String[] outputOptions = parameters.getOutputOptions();
        DataElementPath pathToTableWithDataMatrix = parameters.getPathToTableWithDataMatrix();
        String[] variableNames = parameters.getVariableNames();
        String classifierName = parameters.getClassifierName();
        SmoothingParameters smoothingParameters = parameters.getSmoothingParameters(); 
        DataElementPath pathToOutputs = parameters.getOutputPath();

        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        implementAnalysis(pathToTableWithDataMatrix, outputOptions, variableNames, classifierName, smoothingParameters.getSmoothingWindowWidthType(), smoothingParameters.getGivenSmoothingWindowWidth(), pathToOutputs, jobControl, 0, 100);
        return pathToOutputs.getDataCollection();
    }
    
    private void implementAnalysis(DataElementPath pathToTableWithDataMatrix, String[] outputOptions, String[] variableNames, String classifierName, String smoothingWindowWidthType, double givenWindow, DataElementPath pathToOutputs, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        TableDataCollection table = pathToTableWithDataMatrix.getDataElement(TableDataCollection.class);
        String[] allSampleNamesForEachMeasurement = TableUtils.readGivenColumnInStringTable(table, classifierName);
        int difference = to - from, totalNumberOfSteps = variableNames.length * outputOptions.length; 
        for( int i = 0; i < variableNames.length; i++ )
        {
            double[] samples = TableUtils.readGivenColumnInDoubleTableAsArray(table, variableNames[i]);
            Object[] objects = MatrixUtils.removeObjectsWithMissingData(allSampleNamesForEachMeasurement, samples);
            samples = (double[])objects[1];
            String[] sampleNamesForEachMeasurement = (String[])objects[0]; 
            //objects = Classification.getIndicesOfClassesAndNamesOfClasses(sampleNamesForEachMeasurement);
            //int[] indicesOfSamples = (int[])objects[0];
            //String[] distinctNamesOfSamples = (String[])objects[1];
            Map<String, double[]> sampleNameAndSample = UnivariateSamplesUtils.getSampleNameAndSample(sampleNamesForEachMeasurement, samples);
            for( int j = 0; j < outputOptions.length; j++ )
            {
                if( jobControl != null )
                    jobControl.setPreparedness(from + (i * outputOptions.length + j + 1) * difference / totalNumberOfSteps);
                switch( outputOptions[j] )
                {
                    case OUTPUT_01_KRUSKAL_WALLIS_TEST                     : double[] statisticAndPvalue = NonParametricAnova.getKruskalWallisTest(sampleNamesForEachMeasurement, samples);
                                                                             TableUtils.addRowToDoubleTable(statisticAndPvalue, variableNames[i], new String[]{"Kruskal-Wallis statistic", "p-value"}, pathToOutputs, "KruskalWallisTest"); break;
                    case OUTPUT_02_WILCOXON_FOR_ALL_PAIRS_OF_SAMPLES       : writeTableWithWilcoxonTestForAllPairsOfSamples(sampleNameAndSample, variableNames[i], pathToOutputs, "WilcoxonRunkSumTestForAllPairsOfSamples"); break;
                    case OUTPUT_03_WRITE_SAMPLE_DENSITIES                  : writeChartsWithSampleDensities(variableNames[i], sampleNamesForEachMeasurement, samples, true, smoothingWindowWidthType, givenWindow, pathToOutputs.getChildPath("chart_sampleDensities")); break;
                    case OUTPUT_04_TWO_FREQUENCIES_COMPARISON_BY_NORMALITY : //Comparison of 2 frequencies for binary {0,1}-data; Principal of dichotomy for each sample {xi}: {1, if xi > 0; 0, if vi <= 0}
                                                                             writeTableWithFrequenciesComparisonByNormality(sampleNameAndSample, variableNames[i], pathToOutputs, "frequenciesComparedByNormality"); break;
                    default                                                : throw new Exception(outputOptions[j] + " : this treatment of univariate samples is not supported in our analysis currently");
                }
            }
        }
    }
    
    // TODO: to add the quality of normal approximation - see BrownleeKA65;
    // TODO: also consider the exact Fisher test for comparison without approximations but using hypergeometrical distribution
    // Interesting !!! This test coincided with Wilcoxon two-sample rank test (normal approximation), StatUtil.getWilcoxonTwoSampleRankTest() !!!
    private void writeTableWithFrequenciesComparisonByNormality(Map<String, double[]> sampleNameAndSample, String variableName, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        Map<String, int[]> nameAndFrequencyAndSize = getFrequencyAndSampleSize(sampleNameAndSample);         
        for( Entry<String, int[]> entry1 : nameAndFrequencyAndSize.entrySet() )
        {
            String name1 = entry1.getKey();
            int[] frequencyAndSize1 = entry1.getValue();
            for( Entry<String, int[]> entry2 : nameAndFrequencyAndSize.entrySet() )
            {
                String name2 = entry2.getKey();
                int[] frequencyAndSize2 = entry2.getValue();
                if( name1.equals(name2) ) break;
                double[] test = UnivariateSamplesUtils.compareTwoFrequenciesByNormalApproximation(frequencyAndSize1[0], frequencyAndSize1[1], frequencyAndSize2[0], frequencyAndSize2[1]);
                Object[] objects = new Object[]{variableName, name1, frequencyAndSize1[1], test[0], name2, frequencyAndSize2[1], test[1], test[0] - test[1], test[2], test[3]};
                String[] typesOfColumns = new String[]{"String", "String", "Integer", "Double", "String", "Integer", "Double", "Double", "Double", "Double"};
                String[] namesOfColumns = new String[]{"variable name", "1-st sample name", "1-st sample size", "relative frequency of positive elements in 1-st sample", "2-nd sample name", "2-nd sample size", "relative frequency of positive elements in 2-st sample", "difference between frequencies", "test statistic (Z-score)", "p-value"};
                TableUtils.addRowToTable(typesOfColumns, namesOfColumns, null, objects, pathToOutputs, tableName);
            }
        }
    }

    private Map<String, int[]> getFrequencyAndSampleSize(Map<String, double[]> sampleNameAndSample)
    {
        Map<String, int[]> sampleNameAndFrequencyAndSize = new HashMap<>();
        for( Entry<String, double[]> entry : sampleNameAndSample.entrySet() )
        {
            double[] sample = entry.getValue();
            int frequency = 0;
            for( double x : sample )
                if( x > 0.0 )
                    frequency++;
            sampleNameAndFrequencyAndSize.put(entry.getKey(), new int[]{frequency, sample.length});
        }
        return sampleNameAndFrequencyAndSize;
    }
    
    private void writeChartsWithSampleDensities(String variableName, String[] sampleNamesForEachMeasurement, double[] samples, Boolean doAddTwoZeroPoints, String windowSelector, Double givenWindow, DataElementPath pathToChartTable)
    {
        Map<String, double[]> sampleNameAndSample = UnivariateSamplesUtils.getSampleNameAndSample(sampleNamesForEachMeasurement, samples);
        
// !!!! Temporary remove the union of sample and nameAndMultipliers (needed for normalization of components-samples)
//        sampleNameAndSample.put(variableName + " : union of samples", samples);
//        Map<String, Double> nameAndMultipliers = new HashMap<>();
//        for( Entry<String, double[]> entry : sampleNameAndSample.entrySet() )
//            nameAndMultipliers.put(entry.getKey(), (double)entry.getValue().length / samples.length);
        Map<String, Double> nameAndMultipliers = null;

        Chart chart = DensityEstimation.chartWithSmoothedDensities(sampleNameAndSample, variableName, doAddTwoZeroPoints, nameAndMultipliers, windowSelector, givenWindow);
        TableUtils.addChartToTable(variableName, chart, pathToChartTable);
    }

    private void writeTableWithWilcoxonTestForAllPairsOfSamples(Map<String, double[]> sampleNameAndSample, String variableName, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        for( Entry<String, double[]> entry1 : sampleNameAndSample.entrySet() )
        {
            String name1 = entry1.getKey();
            double[] sample1 = entry1.getValue();
            for( Entry<String, double[]> entry2 : sampleNameAndSample.entrySet() )
            {
                String name2 = entry2.getKey();
                double[] sample2 = entry2.getValue();
                if( name1.equals(name2) ) break;
                double[] meanAndSigma1 = Stat.getMeanAndSigma(sample1), meanAndSigma2 = Stat.getMeanAndSigma(sample2);
                double[] statisticAndPvalue = StatUtil.getWilcoxonTwoSampleRankTest(sample1, sample2);
                Object[] rowElements = new Object[]{variableName, name1, sample1.length, meanAndSigma1[0], meanAndSigma1[1], name2, sample2.length, meanAndSigma2[0], meanAndSigma2[1], statisticAndPvalue[0], statisticAndPvalue[1]};
                String[] typesOfColumns = new String[]{"String", "String", "Integer", "Double", "Double", "String", "Integer", "Double", "Double", "Double", "Double"};
                String[] namesOfColumns = new String[]{"Variable name", "1-st sample name", "1-st sample size", "1-st sample mean", "1-st sample sigma", "2-nd sample name", "2-nd sample size", "2-nd sample mean", "2-nd sample sigma", "Wilcoxon two-sample rank test statistic (nomal approximation)", "p-value"};
                TableUtils.addRowToTable(typesOfColumns, namesOfColumns, null, rowElements, pathToOutputs, tableName);
            }
        }
    }
    
    public static class UnivariateSamplesAnalysisParameters extends AbstractStatisticalAnalysisParameters
    {
        public UnivariateSamplesAnalysisParameters()
        {
            setSmoothingParameters(new SmoothingParameters());
        }
  
        public boolean isSmoothingParametersHidden()
        {
            return ! ArrayUtils.contains(getOutputOptions(), OUTPUT_03_WRITE_SAMPLE_DENSITIES);
        }
    }
    
    public static class OutputOptionsSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            return new String[]{OUTPUT_01_KRUSKAL_WALLIS_TEST, OUTPUT_02_WILCOXON_FOR_ALL_PAIRS_OF_SAMPLES, OUTPUT_03_WRITE_SAMPLE_DENSITIES, OUTPUT_04_TWO_FREQUENCIES_COMPARISON_BY_NORMALITY};
        }
    }

    public static class UnivariateSamplesAnalysisParametersBeanInfo extends BeanInfoEx2<UnivariateSamplesAnalysisParameters>
    {
        public UnivariateSamplesAnalysisParametersBeanInfo()
        {
            super(UnivariateSamplesAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("outputOptions", OutputOptionsSelector.class);
            add(DataElementPathEditor.registerInput("pathToTableWithDataMatrix", beanClass, TableDataCollection.class));
            add("variableNames", VariableNamesSelector.class);
            add(ColumnNameSelector.registerSelector("classifierName", beanClass, "pathToTableWithDataMatrix", false));
            addHidden("smoothingParameters", "isSmoothingParametersHidden");
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class));
        }
    }
}
