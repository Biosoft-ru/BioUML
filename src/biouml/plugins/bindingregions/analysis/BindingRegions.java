package biouml.plugins.bindingregions.analysis;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.IPSPrediction;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;

/**
 * @author yura
 *
 */
public class BindingRegions extends AnalysisMethodSupport<BindingRegionsParameters>
{
    public BindingRegions(DataCollection<?> origin, String name)
    {
        super(origin, name, new BindingRegionsParameters());
    }
    
///////////////////////
    
    private void treatmentOfMode_4() throws Exception
    {
        log.info("Relationship between maximal IpsScores and {numbers of overlaps, length of binding regions");
        DataElementPath pathToOutputs = parameters.getExpParameters2().getPathToOutputs();
        DataElementPath pathToSingleTrack = parameters.getExpParameters6().getPathToSingleTrack();
        String givenTfClass = parameters.getExpParameters7().getTfClass();
        DataElementPath pathToMatrix = parameters.getExpParameters11().getPathToMatrix();
        
        DataElementPath pathToSequences = TrackInfo.getPathToSequences(pathToSingleTrack);
        log.info("Read binding regions of tfClass = " + givenTfClass + " in SQL track");
        Map<String, List<BindingRegion>> selectedBindingRegions = BindingRegion.readBindingRegionsFromTrack(pathToSingleTrack, givenTfClass);
        log.info("Calculate charts and write them into table 'charts_densitiesOfMaximalIpsScores'");
        IPSSiteModel ipsSiteModel = IPSPrediction.getIpsSiteModel(pathToMatrix);
        Map<Integer, List<Double>> numberOfOverlapsAndIpsScores = new HashMap<>();
        Map<Integer, List<Double>> numberOfOverlapsAndLength = new HashMap<>();
        Map<Integer, List<Double>> bindingRegionLengthAndIpsScores = new HashMap<>();
        for( Map.Entry<String, List<BindingRegion>> entry : selectedBindingRegions.entrySet() )
        {
            for( BindingRegion br : entry.getValue() )
            {
                int numberOfOverlaps = br.getNumberOfOverlap();
                int bindingRegionLength = br.getLengthOfBindingRegion();
                Sequence sequence = BindingRegion.getSequenceForBindingRegion(EnsemblUtils.getSequence(pathToSequences, entry.getKey()), br, 300);
                double ipsScore = IPSPrediction.getMaximalIpsScore(sequence, ipsSiteModel);
                numberOfOverlapsAndIpsScores.computeIfAbsent( numberOfOverlaps, k -> new ArrayList<>() ).add( ipsScore );
                bindingRegionLengthAndIpsScores.computeIfAbsent( bindingRegionLength, k -> new ArrayList<>() ).add( ipsScore );
                numberOfOverlapsAndLength.computeIfAbsent( numberOfOverlaps, k -> new ArrayList<>() ).add( (double)bindingRegionLength );
            }
        }
        Chart chart1 = TableUtils.createChart1(numberOfOverlapsAndIpsScores, "Number of binding region overlaps", "Mean value of maximal IpsScore", "Relationship between binding region overlaps and mean maximal IpsScores in binding regions", Color.BLUE);
        Chart chart2 = TableUtils.createChart1(bindingRegionLengthAndIpsScores, "Binding region length", "Mean value of maximal IpsScore", "Relationship between binding region length and mean maximal IpsScores in binding regions", Color.BLUE);
        Chart chart3 = TableUtils.createChart1(numberOfOverlapsAndLength, "Number of binding region overlaps", "Mean length of binding regions", "Relationship between binding region overlaps and mean length of binding regions", Color.BLUE);
        Map<String, Chart> namesAndCharts = new HashMap<>();
        namesAndCharts.put("bindingRegionOverlapsAndMeanIpsScores", chart1);
        namesAndCharts.put("bindingRegionLengthAndMeanIpsScores", chart2);
        namesAndCharts.put("bindingRegionOverlapsAndMeanLength", chart3);
        int lengththreshold = 1000;
        Map<Integer, List<Double>> bindingRegionLengthAndIpsScores1 = EntryStream.of( bindingRegionLengthAndIpsScores )
                .filterKeys( key -> key <= lengththreshold ).toMap();
        Chart chart4 = TableUtils.createChart1(bindingRegionLengthAndIpsScores1, "Binding region length", "Mean value of maximal IpsScore", "Relationship between binding region length and mean maximal IpsScores in binding regions", Color.BLUE);
        namesAndCharts.put("bindingRegionLengthAndMeanIpsScores_short", chart4);
        TableUtils.writeChartsIntoTable(namesAndCharts, "densitiesOfMaximalIpsScores", pathToOutputs.getChildPath("charts_densitiesOfMaximalIpsScores"));
    }
 
    private void treatmentOfMode_13_forMaxim() throws Exception
    {
        log.info("print sequences for Maxim");
        DataElementPath pathToSequences = parameters.getExpParameters1().getPathToGenomeSequences();
        String[] pathsToMatrices  = new String [] {"databases/GTRD/Data/matrices/ChIP-seq/IRF-4.1", "databases/GTRD/Data/matrices/factorbook/GATA1", "databases/GTRD/Data/matrices/ChIP-seq/VDR.1", "databases/GTRD/Data/matrices/ChIP-seq/STAT3.1", "databases/GTRD/Data/matrices/ChIP-seq/VDR;RXRalpha.1"};
        String[] chromosomes = new String []{"20", "20", "20", "20", "20"};
        int[] middlePosition = new int[]{44737212, 44746858, 44746914, 44747104, 44746738};
        int length = 150;
       
        for( int i = 0; i < chromosomes.length; i++ )
        {
            String chromosome = chromosomes[i];
            int regionStart = middlePosition[i] - length / 2;
            Sequence sequence = EnsemblUtils.getSequenceRegion(chromosome, regionStart, length, pathToSequences);
            String seq = sequence.toString();
            log.info("chromosome = " + chromosome + " middlePosition = " + middlePosition[i] + " sequence = ");
            log.info(seq);
            String path = pathsToMatrices[i];
            DataElementPath pathToMatrix = DataElementPath.create(path);
            FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
            FrequencyMatrix[] matrixArray = new FrequencyMatrix[] {matrix};
            IPSSiteModel ipsSiteModel = new IPSSiteModel("I dont use this name", null, matrixArray, 3.5, IPSSiteModel.DEFAULT_DIST_MIN, IPSSiteModel.DEFAULT_WINDOW);
            Site site = ipsSiteModel.findBestSite(sequence);
            double score = site.getScore();
            int pos1 = site.getFrom();
            int pos2 = site.getTo();
            String matrixName = ipsSiteModel.getMatrices()[0].getMatrixPath().getName();
            log.info(" name = " + matrixName + " score = " + score + " positions = " + pos1 + " " + pos2);
            Sequence reverseSequenceRegion = SequenceRegion.getReversedSequence(sequence);
            site = ipsSiteModel.findBestSite(reverseSequenceRegion);
            score = site.getScore();
            pos1 = site.getFrom();
            pos2 = site.getTo();
            log.info(" reverse sequence  : score = " + score + " positions = " + pos1 + " " + pos2);
        }
    }

    // must be removed when 'BestSitesUnionROCCurves.java' will be extended on binding regions
    private void treatmentOfMode_17() throws Exception
    {
        log.info("ROC-curves: 5 siteModels; merged binding regions for single tfClass; union of sequences with best sites");
        int minimalLengthOfSequenceRegion = 300;
        DataElementPath pathToOutputs = parameters.getExpParameters2().getPathToOutputs();
        DataElementPath pathToSingleTrack = parameters.getExpParameters6().getPathToSingleTrack();
        String givenTfClass = parameters.getExpParameters7().getTfClass();
        log.info("givenTfClass = " + givenTfClass);
        DataElementPath pathToMatrix = parameters.getExpParameters11().getPathToMatrix();
        int percentage = parameters.getExpParameters23().getPercentageOfBestSites();
        boolean toRemoveAlu = parameters.getExpParameters24().getToRemoveAlu();
        boolean areBothStrands = true;
        
        DataElementPath pathToSequences = TrackInfo.getPathToSequences(pathToSingleTrack);
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        log.info("Read binding regions in sql track and read sequence set");
        Map<String, List<BindingRegion>> bindingRegions = BindingRegion.readBindingRegionsFromTrack(pathToSingleTrack, givenTfClass);
        if( toRemoveAlu )
            bindingRegions = BindingRegion.selectBindingRegionsWithoutAlu(bindingRegions, pathToSequences);
        List<Sequence> sequences = BindingRegion.getLinearSequencesForBindingRegions(bindingRegions, pathToSequences, minimalLengthOfSequenceRegion);
        Sequence[] sequences1 = new Sequence[sequences.size()];
        sequences.toArray(sequences1);

        log.info("Select sequences with best sites and calculate ROC-curves");
        SiteModelsComparison smc = new SiteModelsComparison(matrix);
        Sequence[] seqs = smc.getUnionOfBestSequences(sequences1, true, percentage);
        if( seqs == null ) return;
        log.info("size of initial set of sequences = " + sequences1.length + " size of selected set of sequences = " + seqs.length);
        Chart chart = (Chart)smc.getChartWithROCcurves(seqs, areBothStrands, false, false)[0];
        String tableName = "ROCcurve" + "_for_" + pathToMatrix.getName() + "_in_" + givenTfClass + "_percentage_" + percentage + "_size_" + seqs.length;
        TableUtils.addChartToTable(tableName, chart, pathToOutputs.getChildPath(tableName));
    }
    
/************************** test ********************/
    
//  Stat.wilcoxonDistributionFast(total, part, x, upTail).wilcoxonTest(sample1, sample2);
/***
Map<String, double[]> statAndPvalue = sc.compareByStudent();
for(Entry<String, double[]> entry : nameAndSample.entrySet())
{
    String name = entry.getKey();
    String[] names = TextUtil.split( name, ',' );
    double[] values = entry.getValue();
}
***/
    
    @Override
    public TableDataCollection[] justAnalyzeAndPut() throws Exception
    {
        int mode = parameters.getModeIndex();
        switch( mode )
        {
            case 4 : treatmentOfMode_4(); return null;
 
            case 13 : treatmentOfMode_13_forMaxim(); return null;
            case 17 : treatmentOfMode_17(); return null;
            default : return null;
        }
    }
}