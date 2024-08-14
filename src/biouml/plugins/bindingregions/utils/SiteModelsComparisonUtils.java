package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.ListUtil;

/**
 * @author yura
 *
 */
public class SiteModelsComparisonUtils
{
    public static final String PERCENTAGE_OF_BEST_SITES = "Percentage of best sites";
    public static final String AUC = "AUC";
    public static final String AUC_FOR = "AUC for ";
    public static final String SIZE_OF_SEQUENCE_SET = "Size of sequence set";
    public static final String AUCS = "AUCs";
    public static final String AUCS_REVISED = "AUCs_revised";
    public static final String REVISED = "revised";

    public static void writeChartsIntoTable(String chartName, Chart chart, String nameOfColumn, DataElementPath pathToTables, String tableName) throws Exception
    {
        TableUtils.addChartToTable(chartName, chart, pathToTables.getChildPath(tableName));
    }

    public static Site findBestSite(Sequence sequence, SiteModel siteModel)
    {
        // return SequenceRegion.withReversed( sequence ).map( siteModel::findBestSite ).maxByDouble( Site::getScore ).get();
        Site bestSiteInPositiveStrand = siteModel.findBestSite(sequence);
        SequenceRegion reverseSeq = SequenceRegion.getReversedSequence(sequence);
        Site site = siteModel.findBestSite(reverseSeq);
        // return site.getScore() > bestSiteInPositiveStrand.getScore() ? bestSiteInPositiveStrand : site;
        return site.getScore() > bestSiteInPositiveStrand.getScore() ? site : bestSiteInPositiveStrand;
    }
    
    public static Site findBestSite(Sequence sequence, boolean areBothStrands, SiteModel siteModel)
    {
        return areBothStrands ? findBestSite(sequence, siteModel) : siteModel.findBestSite(sequence);
    }

    // It's modification is moved to class FrequencyMatrixUtils
    public static void updateMatrix(List<Sequence> sequences, FrequencyMatrix frequencyMatrix, String siteModelType, SiteModel siteModel, int maxIterations, boolean areBothStrands)
    {
        int w = SiteModelsComparison.getWindow(siteModel);
        Integer window = w == 0 ? null : w;
        SiteModel newSiteModel = siteModel;
        for( int iter = 0; iter < maxIterations; iter++ )
        {
            List<Sequence> siteSequences = new ArrayList<>();
            for( Sequence seq : sequences )
                siteSequences.add(findBestSite(seq, areBothStrands, newSiteModel).getSequence());
            frequencyMatrix.updateFromSequences(siteSequences);
            newSiteModel = SiteModelsComparison.getSiteModel(siteModelType, frequencyMatrix, 0.01, window);
        }
    }
    
    /***
     * aim of recalculation: to average sensitivity values that correspond to same FDR value
     * @param rocCurve : list of two arrays; 1-st array = FDRs; 2-nd array = sensitivities;
     * @return list of two arrays; 1-st array = new FDRs; 2-nd array = new sensitivities;
     */
    /***
    public static List<double[]> recalculateRocCurve(List<double[]> rocCurve)
    {
        double x[] = rocCurve.get(0), y[] = rocCurve.get(1);
        Map<Double, List<Double>> map = IntStreamEx.ofIndices( x ).mapToEntry( i -> x[i], i -> y[i] ).grouping( TreeMap::new );
        double x1[] = new double[map.size()], y1[] = new double[map.size()];
        int i = 0;
        for( Entry<Double, List<Double>> entry : map.entrySet() )
        {
            x1[i] = entry.getKey();
            y1[i++] = Stat.mean(entry.getValue());
        }
        return Arrays.asList(x1, y1);
    }
    ***/
    
    /***
     * 
     * @param rocCurve : 1-st array = FDRs, 2-nd array = sensitivities;
     * @return AUC-value (Area Under Curve)
     */
    public static double getAUC(List<double[]> rocCurve)
    {
        double[] fdrs = rocCurve.get(0), sensitivities = rocCurve.get(1);
        double result = 0.0;
        for( int i = 0; i < fdrs.length - 1; i++ )
            result += (fdrs[i + 1] - fdrs[i]) * (sensitivities[i] + sensitivities[i + 1]) / 2.0;
        return result;
    }
    
    public static void writeROCcurvesAndAUCsForGroupedPeaks(String[] characteristicNames, Map<String, Map<String, List<ChipSeqPeak>>> nameOfGroupAndPeaksOfGroup, boolean isAroundSummit, DataElementPath pathToSequences, boolean areBothStrands, int minimalLengthOfSequenceRegion, SiteModelsComparison smc, DataElementPath pathToOutputs, String commonSubNameOfTables, AnalysisJobControl jobControl, int from, int to)
    {
        int siteModelsNumber = smc.getNumberOfSiteModels(), index = 0, difference = to - from;
        double[][] data = new double[nameOfGroupAndPeaksOfGroup.size()][siteModelsNumber + 1 + characteristicNames.length];
        String[] tableRowNames = new String[nameOfGroupAndPeaksOfGroup.size()];
        Map<String, Double> siteModelTypeAndAUC = null;
        for( Entry<String, Map<String, List<ChipSeqPeak>>> entry : nameOfGroupAndPeaksOfGroup.entrySet() )
        {
            String groupName = entry.getKey();
            Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = entry.getValue();
            Map<String, List<ChipSeqPeak>> peaksGroup = ! isAroundSummit ? chromosomeAndPeaks : ChipSeqPeak.getPeaksWithSummitsInCenter(chromosomeAndPeaks, minimalLengthOfSequenceRegion, EnsemblUtils.getChromosomeIntervals(pathToSequences));
            Sequence[] seqs = ChipSeqPeak.getLinearSequencesForChipSeqPeaks(peaksGroup, pathToSequences, minimalLengthOfSequenceRegion, null, 0, 0);
            Object[] objects = smc.getChartWithROCcurves(seqs, areBothStrands, true, false);
            TableUtils.addChartToTable(groupName, (Chart)objects[0], pathToOutputs.getChildPath("ROCcurve" + commonSubNameOfTables));
            siteModelTypeAndAUC = (Map<String, Double>)objects[1];
            int i = 0;
            for( double x : siteModelTypeAndAUC.values() )
                data[index][i++] = x;
            for( i = 0; i < characteristicNames.length; i++ )
                data[index][siteModelsNumber + i] = Stat.mean(ChipSeqPeak.getValuesOfGivenCharacteristic(chromosomeAndPeaks, characteristicNames[i]));
            data[index][siteModelsNumber + characteristicNames.length] = ListUtil.sumTotalSize(chromosomeAndPeaks);
            tableRowNames[index++] = groupName;
            if( jobControl != null )
                jobControl.setPreparedness(from + index * difference / nameOfGroupAndPeaksOfGroup.size());
        }
        String[] tableColumnNames = new String[siteModelsNumber + 1 + characteristicNames.length];
        index = 0;
        for( String name : siteModelTypeAndAUC.keySet() )
            tableColumnNames[index++] = SiteModelsComparisonUtils.AUC_FOR + name;
        for( int i = 0; i < characteristicNames.length; i++ )
            tableColumnNames[siteModelsNumber + i] = "Mean value of " + characteristicNames[i];
        tableColumnNames[siteModelsNumber + characteristicNames.length] = "Size of group";
        TableUtils.writeDoubleTable(data, tableRowNames, tableColumnNames, pathToOutputs, "AUCs" + commonSubNameOfTables);
    }
    static Logger log = Logger.getLogger(SiteModelsComparisonUtils.class.getName());
}

