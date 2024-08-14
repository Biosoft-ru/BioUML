package biouml.plugins.bindingregions.cisregmodule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.CisModule;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gap;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gene;
import biouml.plugins.bindingregions.utils.IPSPrediction;
import biouml.plugins.bindingregions.utils.TrackInfo;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.procedure.TIntLongProcedure;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.LongStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.Maps;
import ru.biosoft.util.ObjectCache;

/**
 * @author yura
 *
 */
public class CisRegModule extends AnalysisMethodSupport<CisRegModuleParameters>
{
    public CisRegModule(DataCollection origin, String name)
    {
        super(origin, name, new CisRegModuleParameters());
    }

/***********************************/
// revised up to here
/***********************************/

    private double[] getKolmogorovSmirnovUniformityPvalueForOneTfClassAndEveryChromosome(String tfClass, Map<String, Integer> chromosomeNameAndLength, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps)
    {
        double[] pvalue = new double[chromosomeNameAndLength.size()];
        int i = 0;
        for( Map.Entry<String, Integer> entry : chromosomeNameAndLength.entrySet() )
        {
            String chromosome = entry.getKey();
            List<BindingRegion> brs = allBindingRegions.get(chromosome);
            List<Gap> gaps = chromosomeNameAndGaps.get(chromosome);
            int[] positions = StreamEx.of(brs).filter( br -> br.getTfClass().equals( tfClass ) )
                .mapToInt( br -> (br.getStartPosition() + br.getFinishPosition()) / 2 )
                .map( x -> Gap.getPositionCorrectedByGaps( x, gaps ) )
                .sorted().toArray();
            double x = Stat.calcKolmogorovSmirnovUniformityStatistic(positions, entry.getValue());
            pvalue[i++] = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(x, 25, positions.length);
        }
         return pvalue;
    }

    private double[][] getKolmogorovSmirnovUniformityPvaluesForEveryTfClassAndChromosome(Map<String, Integer> chromosomeNameAndLength, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps, List<String> distinctTfClasses)
    {
        double[][] pvalues = new double[distinctTfClasses.size()][];
        int i = 0;
        for( String tfClass : distinctTfClasses )
        {
            log.info("tfClass = " + tfClass);
            double[] pvalue = getKolmogorovSmirnovUniformityPvalueForOneTfClassAndEveryChromosome(tfClass, chromosomeNameAndLength, allBindingRegions, chromosomeNameAndGaps);
            pvalues[i] = pvalue;
            getJobControl().setPreparedness((++i) * 100 / distinctTfClasses.size());
        }
        return pvalues;
    }
    
    private double[] getKolmogorovSmirnovUniformityPvalueForAllTfClassesAndEveryChromosome(Map<String, Integer> chromosomeNameAndLength, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps)
    {
        double[] pvalues = new double[chromosomeNameAndLength.size()];
        int i = 0;
        for( Map.Entry<String, Integer> entry : chromosomeNameAndLength.entrySet() )
        {
            String chromosome = entry.getKey();
            List<BindingRegion> brs = allBindingRegions.get(chromosome);
            List<Gap> gaps = chromosomeNameAndGaps.get(chromosome);
            int[] pos = StreamEx.of(brs).mapToInt( BindingRegion::getCenterPosition )
                    .map( x -> Gap.getPositionCorrectedByGaps( x, gaps ) ).sorted().toArray();
            double x = Stat.calcKolmogorovSmirnovUniformityStatistic(pos, entry.getValue());
            pvalues[i] = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(x, 25, pos.length);
            getJobControl().setPreparedness((++i) * 100 / chromosomeNameAndLength.size());
        }
        return pvalues;
    }
    
    /***
     * Calculation of Table that contains the p-values for uniformity of binding regions.
     * The uniformity is tested by Kolmogorov-Smirnov test of uniformity.
     * @param chromosomeNameAndCorrectedLength
     * @param allBindingRegions
     * @param chromosomeNameAndGaps
     * @param distinctTfClasses
     * @return
     */
    private TableDataCollection getTable_KolmogorovSmirnovUniformityPvalues(Map<String, Integer> chromosomeNameAndCorrectedLength, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps, List<String> distinctTfClasses)
    {
        log.info("Kolmogorov-Smirnov uniformity test is started");
        double[][] pvalues = getKolmogorovSmirnovUniformityPvaluesForEveryTfClassAndChromosome(chromosomeNameAndCorrectedLength, allBindingRegions, chromosomeNameAndGaps, distinctTfClasses);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getKolmogorovSmirnovUniformityPvaluesTablePath());
        for( String chr : chromosomeNameAndCorrectedLength.keySet() )
        {
            table.getColumnModel().addColumn(chr, Double.class);
        }
        int j = 0;
        for( String tfClass : distinctTfClasses )
        {
            TableDataCollectionUtils.addRow(table, tfClass, DoubleStreamEx.of(pvalues[j]).boxed().toArray());
            j++;
        }
        log.info("Kolmogorov-Smirnov uniformity test for all tfClasses is started");
        double[] pValuesForAllTfClasses = getKolmogorovSmirnovUniformityPvalueForAllTfClassesAndEveryChromosome(chromosomeNameAndCorrectedLength, allBindingRegions, chromosomeNameAndGaps);
        TableDataCollectionUtils.addRow(table, "allTfClasses", DoubleStreamEx.of(pValuesForAllTfClasses).boxed().toArray());
        return table;
    }

/////////////////////////////////////

    public  static class CisModule3 implements Comparable<CisModule3>
    {
        int position;
        List<String> pivotalTfClasses;
        List<String> additionalTfClasses;

        public CisModule3(int position, List<String> pivotalTfClasses, List<String> additionalTfClasses)
        {
            this.position = position;
            this.pivotalTfClasses = pivotalTfClasses;
            this.additionalTfClasses = additionalTfClasses;
        }

        public int getPosition()
        {
            return position;
        }

        public List<String> getpivotalTfClasses()
        {
            return pivotalTfClasses;
        }

        public List<String> getAdditionalTfClasses()
        {
            return additionalTfClasses;
        }
        
        @Override
        public int compareTo(CisModule3 o)
        {
            return position - o.getPosition();
        }
    }

    private double[] getKolmogorovSmirnovExponentialityPvalueForOneTfClassAndEveryChromosome(String tfClass, Map<String, Integer> chromosomeNameAndLength, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps)
    {
        double[] pvalue = new double[chromosomeNameAndLength.size()];
        int i = 0;
        for( String chromosome : chromosomeNameAndLength.keySet() )
        {
            List<BindingRegion> brs = allBindingRegions.get(chromosome);
            List<Gap> gaps = chromosomeNameAndGaps.get(chromosome);
            int[] waitingTime = StreamEx.of(brs).filter(br -> br.getTfClass().equals(tfClass))
                    .mapToInt(BindingRegion::getStartPosition)
                    .map( x -> Gap.getPositionCorrectedByGaps( x, gaps ) )
                    .pairMap( (a, b) -> b - a).sorted().toArray();
            if( waitingTime.length == 0 )
            {
                pvalue[i++] = 1.0;
            } else
            {
                double x = Stat.calcKolmogorovSmirnovExponentialStatistic(waitingTime);
                pvalue[i++] = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(x, 25, waitingTime.length);
            }
        }
         return pvalue;
    }

    private double[][] getKolmogorovSmirnovExponentialityPvaluesForEveryTfClassAndChromosome(Map<String, Integer> chromosomeNameAndLength, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps, List<String> distinctTfClasses)
    {
        double[][] pvalues = new double[distinctTfClasses.size()][];
        int i = 0;
        for( String tfClass : distinctTfClasses )
        {
            log.info("tfClass = " + tfClass);
            double[] pvalue = getKolmogorovSmirnovExponentialityPvalueForOneTfClassAndEveryChromosome(tfClass, chromosomeNameAndLength, allBindingRegions, chromosomeNameAndGaps);
            pvalues[i] = pvalue;
            getJobControl().setPreparedness((++i) * 100 / distinctTfClasses.size());
        }
        return pvalues;
    }

    private double[] getKolmogorovSmirnovExponentialityPvalueForAllTfClassesAndEveryChromosome(Map<String, Integer> chromosomeNameAndLength, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps)
    {
        double[] pvalues = new double[chromosomeNameAndLength.size()];
        int i = 0;
        for( String chromosome : chromosomeNameAndLength.keySet() )
        {
            List<BindingRegion> brs = allBindingRegions.get(chromosome);
            if( brs.size() < 2 )
            {
                pvalues[i++] = 1.0;
                continue;
            }
            List<Gap> gaps = chromosomeNameAndGaps.get(chromosome);
            int[] waitingTime = StreamEx.of(brs).mapToInt(BindingRegion::getStartPosition)
                    .map( x -> Gap.getPositionCorrectedByGaps( x, gaps ) )
                    .pairMap( (a, b) -> (b - a) ).sorted().toArray();
            double x = Stat.calcKolmogorovSmirnovExponentialStatistic(waitingTime);
            pvalues[i] = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(x, 25, waitingTime.length);
            getJobControl().setPreparedness((++i) * 100 / chromosomeNameAndLength.size());
        }
        return pvalues;
    }
    
    /***
     * Calculation of Table that contains the p-values for exponentiality of the waiting times (distances
     * between binding regions).
     * The exponentiality is tested by Kolmogorov-Smirnov test of uniformity.
     * @param chromosomeNameAndLength
     * @param allBindingRegions
     * @param chromosomeNameAndGaps
     * @param distinctTfClasses
     * @return
     */
    private TableDataCollection getTable_KolmogorovSmirnovExponentialityPvalues(Map<String, Integer> chromosomeNameAndLength, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps, List<String> distinctTfClasses)
    {
        log.info("Kolmogorov-Smirnov Exponentiality test is started");
        double[][] pvaluesExponentiality = getKolmogorovSmirnovExponentialityPvaluesForEveryTfClassAndChromosome(chromosomeNameAndLength, allBindingRegions, chromosomeNameAndGaps, distinctTfClasses);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getKolmogorovSmirnovExponentialityPvaluesTablePath());
        for( String chr : chromosomeNameAndLength.keySet() )
        {
            table.getColumnModel().addColumn(chr, Double.class);
        }
        int j = 0;
        for( String tfClass : distinctTfClasses )
        {
            TableDataCollectionUtils.addRow(table, tfClass, DoubleStreamEx.of(pvaluesExponentiality[j]).boxed().toArray());
            j++;
            getJobControl().setPreparedness(j * 100 / distinctTfClasses.size());
        }
        log.info("Kolmogorov-Smirnov Exponentiality test for all tfClasses is started");
        double[] pValuesForAllTfClasses1  = getKolmogorovSmirnovExponentialityPvalueForAllTfClassesAndEveryChromosome(chromosomeNameAndLength, allBindingRegions, chromosomeNameAndGaps);
        TableDataCollectionUtils.addRow( table, "allTfClasses", DoubleStreamEx.of( pValuesForAllTfClasses1 ).boxed().toArray() );
        return table;
    }
    
    private boolean areOverlappedTwoSites(int beginOfFirstSite, int endOfFirstSite, int beginOfSecondSite, int endOfSecondtSite)
    {
        return endOfFirstSite >= beginOfSecondSite && endOfSecondtSite >= beginOfFirstSite;
    }


    public int[] getOverlapOfTwoSites(int beginOfFirstSite, int endOfFirstSite, int beginOfSecondSite, int endOfSecondtSite)
    {
        int[] result = new int[2];
        if( ! areOverlappedTwoSites(beginOfFirstSite, endOfFirstSite, beginOfSecondSite, endOfSecondtSite) )
            return null;
        result[0] = beginOfFirstSite <= beginOfSecondSite ? beginOfSecondSite : beginOfFirstSite;
        result[1] = endOfFirstSite <= endOfSecondtSite ? endOfFirstSite : endOfSecondtSite;
        return result;
    }

    private Map<String, Integer> toCountGenesOverlappedWithCisModules(Map<String, List<CisModule>> allCisModules, Map<String, List<Gene>> chromosomesAndGenes, List<String> distinctGeneTypes)
    {
        Map<String, Integer> result = new HashMap<>();
        for( String geneType : distinctGeneTypes )
            result.put(geneType, 0);
        for( Map.Entry<String, List<CisModule>> entry : allCisModules.entrySet() )
        {
            List <CisModule> cisModules = entry.getValue();
            List <Gene> genes = chromosomesAndGenes.get(entry.getKey());
            int iCisModule = 0;
            for( Gene gene : genes )
            {
                int[] startAndEndOfGene = gene.getStartAndEndOfGene();
                boolean indicatorOfOverlap = false;
                while( iCisModule < cisModules.size() )
                {
                    CisModule cisModule = cisModules.get(iCisModule);
                    indicatorOfOverlap = areOverlappedTwoSites(startAndEndOfGene[0], startAndEndOfGene[1], cisModule.getStartPosition(), cisModule.getFinishPosition());
                    if( indicatorOfOverlap == true ) break;
                    if( cisModule.getFinishPosition() < startAndEndOfGene[0] )
                        iCisModule++;
                    else break;
                }
                if( indicatorOfOverlap == false ) continue;
                Integer count = result.get(gene.getGeneType());
                if( count == null ) continue;
                int newCount = count + 1;
                result.put(gene.getGeneType(), newCount);
            }
        }
        return result;
    }

    /***
     * Calculate the table that contains the summary information on genes overlapped by cis-regulatory modules.
     * Namely, for each gene type the following two frequencies are calculated: f1 and f2,
     * where f1 - is absolute frequency of genes overlapped with cis-regulatory modules
     * and f2 - is relative frequency f1/f3, where f3 is total number of genes of given type.
     * 
     * @param overlapedGenesTypesAndCounts
     * @param GeneTypesAndCounts
     * @return
     */
    private TableDataCollection getTable_summaryOnGenesOverlappedWithCisModules(Map<String, Integer> overlappedGenesTypesAndCounts, Map<String, Integer> GeneTypesAndCounts)
    {
        int numberOfOverlappedGenes = 0;
        for( int number : overlappedGenesTypesAndCounts.values() )
            numberOfOverlappedGenes += number;
        log.info("Number of all genes overlapped with cis-regulatory modules = " + numberOfOverlappedGenes);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getSummaryOnGenesOverlappedWithCisModulesPath());
        table.getColumnModel().addColumn("GeneTypes", String.class);
        table.getColumnModel().addColumn("CountsOfOverlappedGenes", Integer.class);
        table.getColumnModel().addColumn("PercentageOfOverlappedGenes", Double.class);
        int iRow = 0;
        for( Map.Entry<String, Integer> entry : overlappedGenesTypesAndCounts.entrySet() )
        {
            String geneType = entry.getKey();
            numberOfOverlappedGenes = entry.getValue();
            int numberOfGenes = GeneTypesAndCounts.get(geneType);
            float percentage = numberOfGenes == 0 ? 0 : (numberOfOverlappedGenes) * ((float)100.0) / (numberOfGenes);
            TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {geneType, numberOfOverlappedGenes, percentage});
        }
        return table;
    }
    
    /***
     * Classification of cis-regulatory modules  on the basis of their locations with respect to protein_coding genes
     * @param allCisModules
     * @param chromosomesAndGenes
     * @return Map<String, List<CisModule>> where String is type of cis-regulatory modules
     *          and List<CisModule> is the list of cis-regulatory modules of specified type.
     */
    private Map<String, List<CisModule>> getClassificationOfCisModules(Map<String, List<CisModule>> allCisModules, Map<String, List<Gene>> chromosomesAndGenes)
    {
        Map<String, List<CisModule>> result = StreamEx.of("promoter", "3'flank", "insideGene", "outsideGene")
                .toMap( k -> new ArrayList<>() );
        int iJobControl = 0;
        for( Map.Entry<String, List<CisModule>> entry : allCisModules.entrySet() )
        {
            List<CisModule> cisModules = entry.getValue();
            List<Gene> genes = chromosomesAndGenes.get(entry.getKey());
            int iGene0 = 0;
            for( CisModule cisModule : cisModules )
            {
                int cisModuleStart = cisModule.getStartPosition();
                int cisModuleEnd = cisModule.getFinishPosition();
                int classifier = 4;
                for( int iGene = iGene0; iGene < genes.size(); iGene++ )
                {
                    Gene gene = genes.get(iGene);
                    int[] StartAndEndOfGene = gene.getStartAndEndOfGene();
                    if( StartAndEndOfGene[1] < cisModuleStart )
                    {
                        iGene0 = iGene;
                        continue;
                    }
                    if( cisModuleEnd < StartAndEndOfGene[0] ) break;
                    if( areOverlappedTwoSites(cisModuleStart, cisModuleEnd, StartAndEndOfGene[0], StartAndEndOfGene[1]) )
                    {
                        if( classifier == 4 ) classifier = 3;
                        int[] transcriptionEnds = gene.getTranscriptionEnds();
                        for( int transcriptionEnd : transcriptionEnds )
                        {
                            if( areOverlappedTwoSites(transcriptionEnd, transcriptionEnd, cisModuleStart, cisModuleEnd)
                                && classifier > 2 )
                            {
                                classifier = 2;
                                break;
                            }
                        }
                        int [] transcriptionStarts = gene.getTranscriptionStarts();
                        for( int transcriptionStart : transcriptionStarts )
                        {
                            if( areOverlappedTwoSites(transcriptionStart, transcriptionStart, cisModuleStart, cisModuleEnd) )
                            {
                                classifier = 1;
                                break;
                            }
                        }
                    }
                    if( classifier == 1 ) break;
                }
                String cisModuleType = null;
                switch( classifier )
                {
                    case 1: cisModuleType = "promoter"; break;
                    case 2: cisModuleType = "3'flank"; break;
                    case 3: cisModuleType = "insideGene"; break;
                    case 4: cisModuleType = "outsideGene"; break;
                }
                result.get(cisModuleType).add(cisModule);
            }
            getJobControl().setPreparedness((++iJobControl) * 100 / allCisModules.size());
        }
        return result;
    }
    
    private Map<String, Integer> toCountTypesOfCisModules(Map<String, List<CisModule>> typesAndCisModules)
    {
        return Maps.transformValues( typesAndCisModules, List::size );
    }
    
    private TableDataCollection getTable_summaryOnCisModulesOverlappedWithGenes(Map<String, Integer> cisModuleTypesAndTheirCounts)
    {
        int totalNumber = 0;
        for( int number : cisModuleTypesAndTheirCounts.values() )
            totalNumber += number;
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getSummaryOnCisModulesOverlappedWithGenesPath());
        table.getColumnModel().addColumn("CisModuleLocations", String.class);
        table.getColumnModel().addColumn("CountsOfCisModules", Integer.class);
        table.getColumnModel().addColumn("PercentageOfCisModules", Double.class);
        int iRow = 0;
        for( Map.Entry<String, Integer> entry : cisModuleTypesAndTheirCounts.entrySet() )
        {
            int numberOfCisModules = entry.getValue();
            float percentage = (numberOfCisModules) * ((float)100.0) / (totalNumber);
            TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {entry.getKey(), numberOfCisModules, percentage});
        }
        return table;
    }
    
    private Map<String, double[]> getMeansAndSigmasForCisModules(Map<String, List<CisModule>> typesAndCisModules)
    {
        List<ToDoubleFunction<CisModule>> functions = Arrays.asList(
                CisModule::getLength,
                CisModule::getNumberOfTfClasses,
                CisModule::getNumberOfDistinctTfClasses);
        
        return Maps.transformValues(
                typesAndCisModules,
                cisModules -> StreamEx.of( functions ).map( fn -> StreamEx.of( cisModules ).mapToDouble( fn ).toArray() )
                        .map( Stat::getMeanAndSigma ).flatMapToDouble( Arrays::stream ).toArray() );
    }
    
    /***
     * Calculate the table that contains the mean values and sigma values
     * of the characteristics (length of cis-regulatory module, number of tfClasses in cis-regulatory module
     * and number of distinct tfClasses in cis-regulatory module.
     * These characteristics are calculeted for different types of cis-regulatory module.
     * @param cisModuleTypesAndMeansSigmas
     * @return
     * @throws Exception
     */
    private TableDataCollection getTable_tfClassesInCisModulesOfDifferentTypes(Map<String, double[]> cisModuleTypesAndMeansSigmas) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getTfClassesInCisModulesOfDifferentTypesPath());
        table.getColumnModel().addColumn("MeanNumberOfLengthOfCisModule", Double.class);
        table.getColumnModel().addColumn("Sigma1", Double.class);
        table.getColumnModel().addColumn("MeanNumberOfTfClassesInCisModule", Double.class);
        table.getColumnModel().addColumn("Sigma2", Double.class);
        table.getColumnModel().addColumn("MeanNumberOfDistinctTfClassesInCisModule", Double.class);
        table.getColumnModel().addColumn("Sigma3", Double.class);
        for( Map.Entry<String, double[]> entry : cisModuleTypesAndMeansSigmas.entrySet() )
        {
            TableDataCollectionUtils.addRow(table, entry.getKey(), DoubleStreamEx.of( entry.getValue() ).boxed().toArray(), true);
        }
        table.finalizeAddition();
        return table;
    }
    
     private Map<String, Map<String, Double>> getCisModulesTypesAndDistinctTfClassesAndCountsOfDistinctTfClasses(Map<String, List<CisModule>> cisModuleTypesAndCisModules, List<String> distinctTfClasses)
    {
        Map<String, Map<String, Double>> result = new HashMap<>();
        int iJobControl = 0;
        for( Map.Entry<String, List<CisModule>> entry : cisModuleTypesAndCisModules.entrySet() )
        {
            Map<String, Integer> result1 = new HashMap<>();
            for( String distinctTfClass : distinctTfClasses )
                result1.put(distinctTfClass, 0);
            List<CisModule> cisModules = entry.getValue();
            for( CisModule cisModule : cisModules )
            {
                Map<String, Boolean> indicatorVector = cisModule.getIndicatorVectorOfTfClasses(distinctTfClasses);
                for( Map.Entry<String, Boolean> indicatorEntry : indicatorVector.entrySet() )
                {
                    String tfClass = indicatorEntry.getKey();
                    boolean indicator = indicatorEntry.getValue();
                    int frequency = result1.get(tfClass);
                    if( indicator )
                        frequency++;
                    result1.put(tfClass, frequency);
                }
            }
            Map<String, Double> result2 = EntryStream.of(result1).mapValues(val -> val / (double)cisModules.size()).toMap();
            result.put(entry.getKey(), result2);
            getJobControl().setPreparedness((++iJobControl) * 100 / cisModuleTypesAndCisModules.size());
        }
        return result;
    }
 
     private TableDataCollection getTable_frequenciesOfTfClassesInCisModulesOfDifferentTypes(Map<String, Map<String, Double>> cisModulesTypesAndDistinctTfClassesAndVectorOfCountsOfDistinctTfClasses) throws Exception
     {
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getFrequenciesOfTfClassesInCisModulesPath());
         for( String name : cisModulesTypesAndDistinctTfClassesAndVectorOfCountsOfDistinctTfClasses.keySet() )
             table.getColumnModel().addColumn(name, Double.class);
         Map<String, Double> var = cisModulesTypesAndDistinctTfClassesAndVectorOfCountsOfDistinctTfClasses.values().iterator().next();
         for( String tfClass : var.keySet() )
         {
             Float[] frequencies = new Float[cisModulesTypesAndDistinctTfClassesAndVectorOfCountsOfDistinctTfClasses.size()];
             int  i = 0;
             for( Map<String, Double> var1 : cisModulesTypesAndDistinctTfClassesAndVectorOfCountsOfDistinctTfClasses.values() )
             {
                 double x = var1.get(tfClass);
                 float xx = (float)x;
                 frequencies[i++] = xx;
             }
             TableDataCollectionUtils.addRow(table, tfClass, frequencies, true);
         }
         table.finalizeAddition();
         return table;
     }

     private Map<String, Map<String, int[]>> getSetOf_2x2_ContingencyTables(List<CisModule> cisModules, List<String> distinctTfClasses)
     {
         Map<String, Map<String, int[]>> result = new HashMap<>();
         for( String tfClass1 : distinctTfClasses )
         {
             Map<String, int[]> result1 = new HashMap<>();
             for( String tfClass2 : distinctTfClasses )
             {
                 if( tfClass2.equals(tfClass1) ) break;
                 int[] contingencyTable = new int[4];
                 for( CisModule cisModule : cisModules )
                 {
                     boolean indicator1 = cisModule.isBelongToCisModule(tfClass1);
                     boolean indicator2 = cisModule.isBelongToCisModule(tfClass2);
                     int hashIndex = indicator1 == false ? 0 : 2;
                     hashIndex += indicator2 == false ? 0 : 1;
                     contingencyTable[hashIndex]++;
                 }
                 result1.put(tfClass2, contingencyTable);
             }
             result.put(tfClass1, result1);
         }
         return result;
     }
     
     private Map<String, Map<String, double[]>> getChiSquaredIndependenceStatisticsAndPvalues(Map<String, List<CisModule>> cisModuleTypesAndCisModules, List<String> distinctTfClasses) throws Exception
     {
         Map<String, Map<String, double[]>> result = new HashMap<>();
         for( String tfClass1 : distinctTfClasses )
         {
             Map<String, double[]> result1 = new HashMap<>();
             for( String tfClass2 : distinctTfClasses )
             {
                 if( tfClass2.equals(tfClass1) ) break;
                 double[] StatisticsAndPvalues = new double[2 * cisModuleTypesAndCisModules.size()];
                 result1.put(tfClass2, StatisticsAndPvalues);
             }
             result.put(tfClass1, result1);
         }
         int indexForArray = 0;
         for( List<CisModule> cisModules : cisModuleTypesAndCisModules.values() )
         {
             Map<String, Map<String, int[]>> tfClass1AndtfClass2AndContingencyTables = getSetOf_2x2_ContingencyTables(cisModules, distinctTfClasses);
             int iJobControl = 0;
             for( Map.Entry<String, Map<String, int[]>> entry1 : tfClass1AndtfClass2AndContingencyTables.entrySet() )
             {
                 String tfclass1 = entry1.getKey();
                 Map<String, int[]> tfClasses2AndContingencyTables = entry1.getValue();
                 for( Map.Entry<String, int[]> entry2 : tfClasses2AndContingencyTables.entrySet() )
                 {
                     String tfClass2 = entry2.getKey();
                     int[] contingencyTable = entry2.getValue();
                     double statistic = Stat.getStatisticOfChiSquared_2x2_testForIndependence(contingencyTable);
                     double pValue = 1.0 - Stat.chiDistribution(statistic, 1.0);
                     double[] StatisticsAndPvalues = result.get(tfclass1).get(tfClass2);
                     StatisticsAndPvalues[indexForArray] = statistic;
                     StatisticsAndPvalues[indexForArray + 1] = pValue;
                 }
             }
             indexForArray += 2;
             getJobControl().setPreparedness((++iJobControl) * 100 / cisModuleTypesAndCisModules.size());
         }
         return result;
     }
     
     /***
      * Calculate the table that contains chi-squared statistics of tfClasses independence tests and corresponding p-values
      * @param cisModuleTypesAndCisModules
      * @param tfClass1AndtfClass2AndStatisticsPvalues
      * @return
      * @throws Exception
      */
     private TableDataCollection getTable_ChisquaredTestForTfClassesIndependence(Map<String, List<CisModule>> cisModuleTypesAndCisModules, Map<String, Map<String, double[]>> tfClass1AndtfClass2AndStatisticsPvalues) throws Exception
     {
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getChisquaredTestForTfClassesIndependencePath());
         for( String cisModuleType : cisModuleTypesAndCisModules.keySet() )
         {
             String columnName1 = "chiSquaredStatisticFor_" + cisModuleType;
             table.getColumnModel().addColumn(columnName1, Double.class);
             String columnName2 = "pValueFor_" + cisModuleType;
             table.getColumnModel().addColumn(columnName2, Double.class);
         }
         for( Map.Entry<String, Map<String, double[]>> entry : tfClass1AndtfClass2AndStatisticsPvalues.entrySet() )
         {
             String tfClass1 = entry.getKey();
             Map<String, double[]> var = entry.getValue();
             for( Map.Entry<String, double[]> varEntry : var.entrySet() )
             {
                 String tfClass2 = varEntry.getKey();
                 double[] values = varEntry.getValue();
                 Float[] floatValues = new Float[values.length];
                 for( int i = 0; i < values.length; i++ )
                 {
                     double value = values[i];
                     float floatValue = (float)value;
                     floatValues[i] = floatValue;
                 }
                 String rowName = tfClass1 + "___" + tfClass2;
                 TableDataCollectionUtils.addRow(table, rowName, floatValues, true);
             }
         }
         table.finalizeAddition();
         return table;
     }

     private void getTimesWaitingForBirthOrDeath(Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps, Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth, Map<Integer, List<Integer>> populationSizesAndTimesWaitingForDeath)
     {
         int iJobControl = 0;
         for( Map.Entry<String, List<BindingRegion>> entry : allBindingRegions.entrySet() )
         {
             List<Gap> gaps = chromosomeNameAndGaps.get(entry.getKey());
             List<BindingRegion> bindingRegions = entry.getValue();
             List<BindingRegion> population = new ArrayList<>();
             int iBindingRegion = 0;
             int waitingTime;
             int[] populationBeginAndEnd = new int[2];
             populationBeginAndEnd[0] = populationBeginAndEnd[1] = 0;
             while( iBindingRegion < bindingRegions.size() )
             {
                 Integer populationSize = population.size();
                 boolean isBirth = false;
                 if( population.size() == 0 )
                 {
                     BindingRegion br = bindingRegions.get(iBindingRegion++);
                     population.add(br);
                     waitingTime = br.getStartPositionCorrectedOnGaps(gaps) - populationBeginAndEnd[1];
                     isBirth = true;
                 }
                 else
                 {
                     Iterator<BindingRegion> iterator = population.iterator();
                     BindingRegion br = iterator.next();
                     populationBeginAndEnd[0] = br.getStartPositionCorrectedOnGaps(gaps);
                     populationBeginAndEnd[1] = br.getFinishPositionCorrectedOnGaps(gaps);
                     while( iterator.hasNext() )
                     {
                         br = iterator.next();
                         populationBeginAndEnd = getOverlapOfTwoSites(populationBeginAndEnd[0], populationBeginAndEnd[1], br.getStartPositionCorrectedOnGaps(gaps), br.getFinishPositionCorrectedOnGaps(gaps));
                     }
                     int timeWaitingForBirth = Integer.MAX_VALUE;
                     br = bindingRegions.get(iBindingRegion);
                     if( populationBeginAndEnd[0] <= br.getStartPositionCorrectedOnGaps(gaps) && br.getStartPositionCorrectedOnGaps(gaps) <= populationBeginAndEnd[1])
                         timeWaitingForBirth = br.getStartPositionCorrectedOnGaps(gaps) - populationBeginAndEnd[0];
                     int firstEnd = Integer.MAX_VALUE;
                     int indexOfCandidateForDeath = 0;
                     for( BindingRegion br1 : population )
                     {
                         if( br1.getFinishPositionCorrectedOnGaps(gaps) < firstEnd )
                         {
                             firstEnd = br1.getFinishPositionCorrectedOnGaps(gaps);
                             indexOfCandidateForDeath = population.indexOf(br1);
                         }
                     }
                     int timeWaitingForDeath = firstEnd - populationBeginAndEnd[0];
                     if( timeWaitingForBirth <= timeWaitingForDeath )
                     {
                         waitingTime = timeWaitingForBirth;
                         isBirth = true;
                         population.add(bindingRegions.get(iBindingRegion++));
                     }
                     else
                     {
                         waitingTime = timeWaitingForDeath;
                         population.remove(indexOfCandidateForDeath);
                     }
                 }
                 Integer wTime = waitingTime;
                 if( isBirth == true )
                 {
                     if( ! populationSizesAndTimesWaitingForBirth.containsKey(populationSize) )
                     {
                         List<Integer> newList = new ArrayList<>();
                         newList.add(wTime);
                         populationSizesAndTimesWaitingForBirth.put(populationSize, newList);
                     }
                     else
                         populationSizesAndTimesWaitingForBirth.get(populationSize).add(wTime);
                 }
                 else
                 {
                     if( ! populationSizesAndTimesWaitingForDeath.containsKey(populationSize) )
                     {
                         List<Integer> newList = new ArrayList<>();
                         newList.add(wTime);
                         populationSizesAndTimesWaitingForDeath.put(populationSize, newList);
                     }
                     else
                         populationSizesAndTimesWaitingForDeath.get(populationSize).add(wTime);
                 }
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / allBindingRegions.size());
         }
     }

     /***
      * Calculate the table that contains mean values and sigmas for times
      * waiting for new birth or death
      * @param populationSizesAndTimesWaitingForBirth
      * @param populationSizesAndTimesWaitingForDeath
      * @return
      */
     TableDataCollection getTable_timesWaitingForBirthOrDeath(Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth, Map<Integer, List<Integer>> populationSizesAndTimesWaitingForDeath)
     {
         List<Integer> newList = new ArrayList<>();
         Integer zero = 0;
         populationSizesAndTimesWaitingForDeath.put(zero, newList);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getTimesWaitingForBithOrDeathPath());
         int maxPopulationSize = -1;
         for( int j :  populationSizesAndTimesWaitingForBirth.keySet() )
         {
             if( j > maxPopulationSize )
                 maxPopulationSize = j;
         }
         for( int j : populationSizesAndTimesWaitingForDeath.keySet() )
         {
             if( j > maxPopulationSize )
                 maxPopulationSize = j;
         }
         List<Integer> newList0 = new ArrayList<>();
         Integer zero1 = maxPopulationSize;
         populationSizesAndTimesWaitingForBirth.put(zero1, newList0);
         table.getColumnModel().addColumn("PopulationSize", Integer.class);
         table.getColumnModel().addColumn("NumberOfPopulationsWaitingForNewBirth", Integer.class);
         table.getColumnModel().addColumn("NumberOfPopulationsWaitingForNewDeath", Integer.class);
         table.getColumnModel().addColumn("TimeWaitingForBirth_mean", Double.class);
         table.getColumnModel().addColumn("TimeWaitingForBirth_sigma", Double.class);
         table.getColumnModel().addColumn("TimeWaitingForDeat_mean", Double.class);
         table.getColumnModel().addColumn("TimeWaitingForDeat_sigma", Double.class);
         int iRow = 0;
         for( int j = 0; j <= maxPopulationSize; j++ )
         {
             Integer populationSize = j;
             List<Integer> timesWaitingForBirth = populationSizesAndTimesWaitingForBirth.get(populationSize);
             Integer numberOfNextBirth = timesWaitingForBirth.size();
             double[] meanAndSigmaForTimesWaitingForBirth =  Stat.getMeanAndSigma(timesWaitingForBirth);
             float x = (float)meanAndSigmaForTimesWaitingForBirth[0];
             Float meanTimeWaitingForBirth = x;
             x = (float)meanAndSigmaForTimesWaitingForBirth[1];
             Float sigmaOfTimesWaitingForBirth = x;
             List<Integer> timesWaitingForDeath = populationSizesAndTimesWaitingForDeath.get(populationSize);
             Integer numberOfNextDeath = timesWaitingForDeath.size();
             double[] meanAndSigmaForTimesWaitingForDeath = Stat.getMeanAndSigma(timesWaitingForDeath);
             x = (float)meanAndSigmaForTimesWaitingForDeath[0];
             Float meanTimeWaitingForDeath = x;
             x = (float)meanAndSigmaForTimesWaitingForDeath[1];
             Float sigmaOtTimesWaitingForDeath = x;
             TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {populationSize, numberOfNextBirth, numberOfNextDeath, meanTimeWaitingForBirth, sigmaOfTimesWaitingForBirth, meanTimeWaitingForDeath, sigmaOtTimesWaitingForDeath});
         }
         return table;
     }

     /***
      * this table contains the histograms for times waiting for birth for distinct population size.
      * @param populationSizesAndTimesWaitingForBirth
      * @return
      * @throws Exception
      */
     TableDataCollection getTable_histogramsOfTimesWaitingForBirth(Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth) throws Exception
     {
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.histogramsOfTimesWaitingForBithPath());
         int numberOfGroups = 20;
         for( int i = 0; i < numberOfGroups; i++ )
         {
             String name = "group_" + i;
             table.getColumnModel().addColumn(name, Integer.class);
         }
         for( Map.Entry<Integer, List<Integer>> entry : populationSizesAndTimesWaitingForBirth.entrySet() )
         {
             int populationSize = entry.getKey();
             List<Integer> timesWaitingForBirth = entry.getValue();
             if( timesWaitingForBirth == null || timesWaitingForBirth.size() < 30 ) continue;
             int maxValue = -1;
             for( Integer integer : timesWaitingForBirth )
             {
                 int timeWaitingForBirth = integer;
                 if( maxValue < timeWaitingForBirth )
                     maxValue = timeWaitingForBirth;
                 if( timeWaitingForBirth < 0 )
                     log.info("Is negative!!!! timeWaitingForBirth = " + timeWaitingForBirth + " populationSize = " + populationSize);
             }
             maxValue += 2;
             int[] histogram = Stat.getHistogram(timesWaitingForBirth, maxValue, numberOfGroups);
             String rowName = "populationSize" + populationSize;
             Integer[] integerValues = new Integer[histogram.length];
             for( int i = 0; i < histogram.length; i++ )
                 integerValues[i] = histogram[i] ;
             TableDataCollectionUtils.addRow(table, rowName, integerValues, true);
         }
         table.finalizeAddition();
         return table;
     }
     
     private Map<Integer, double[]> getKolmogorovSmirnovExponentialityStatisticsAndPvaluesForWaitingTimes(Map<Integer, List<Integer>> populationSizesAndWaitingTimes)
     {
         Map<Integer, double[]> result = new HashMap<>();
         int iJobControl = 0;
         for( Map.Entry<Integer, List<Integer>> entry : populationSizesAndWaitingTimes.entrySet() )
         {
             List<Integer> waitingTimes = entry.getValue();
             if( waitingTimes != null && waitingTimes.size() >= 10 )
             {
                 int[] waitingTimesArray = IntStreamEx.of(waitingTimes).sorted().toArray();
                 double [] statisticAndPvalue = new double[2];
                 statisticAndPvalue[0] = Stat.calcKolmogorovSmirnovExponentialStatistic(waitingTimesArray);
                 statisticAndPvalue[1] = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(statisticAndPvalue[0], 25, waitingTimesArray.length);
                 result.put(entry.getKey(), statisticAndPvalue);
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / populationSizesAndWaitingTimes.size());
         }
         return result;
     }

     /***
      * Calculate two tables.
      * 1-st table contains statistics and p-values for Kolmogorov-Smirnov tests for exponentiality
      * of times waiting for birth.
      * 2-nd table contains statistics and p-values for Kolmogorov-Smirnov tests for exponentiality
      * of times waiting for death.
      * @param populationSizesAndTimesWaitingForBirth
      * @param populationSizesAndTimesWaitingForDeath
      * @return
      * @throws Exception
      */
     TableDataCollection[] getTable_exponentialityKolmogorovSmirnovStatisticsAndPvaluesForWaitingTimes(Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth, Map<Integer, List<Integer>> populationSizesAndTimesWaitingForDeath)
     {
         TableDataCollection tableBirth = TableDataCollectionUtils.createTableDataCollection(parameters.getExponentiality_KS_OfTimesWaitingForBirthPath());
         tableBirth.getColumnModel().addColumn("populationSize", Integer.class);
         tableBirth.getColumnModel().addColumn("exponentialityKolmogorovSmirnovStatistic", Double.class);
         tableBirth.getColumnModel().addColumn("exponentialityKolmogorovSmirnovPvalue", Double.class);
         Map<Integer, double[]> statisticsAndPvaluesForTimesWaitingForBirth = getKolmogorovSmirnovExponentialityStatisticsAndPvaluesForWaitingTimes(populationSizesAndTimesWaitingForBirth);
         int iRow = 0;
         for( Map.Entry<Integer, double[]> entry : statisticsAndPvaluesForTimesWaitingForBirth.entrySet() )
         {
             double[] values = entry.getValue();
             float x = (float)values[0];
             Float value1 = x;
             x = (float)values[1];
             Float value2 = x;
             TableDataCollectionUtils.addRow(tableBirth, String.valueOf(++iRow), new Object[] {entry.getKey(), value1, value2});
         }
         TableDataCollection tableDeath = TableDataCollectionUtils.createTableDataCollection(parameters.getExponentiality_KS_OfTimesWaitingForDeathPath());
         tableDeath.getColumnModel().addColumn("populationSize", Integer.class);
         tableDeath.getColumnModel().addColumn("exponentialityKolmogorovSmirnovStatistic", Double.class);
         tableDeath.getColumnModel().addColumn("exponentialityKolmogorovSmirnovPvalue", Double.class);
         Map<Integer, double[]> statisticsAndPvaluesForTimesWaitingForDeath = getKolmogorovSmirnovExponentialityStatisticsAndPvaluesForWaitingTimes(populationSizesAndTimesWaitingForDeath);
         iRow = 0;
         for( Map.Entry<Integer, double[]> entry : statisticsAndPvaluesForTimesWaitingForDeath.entrySet() )
         {
             double[] values = entry.getValue();
             float x = (float)values[0];
             Float value1 = x;
             x = (float)values[1];
             Float value2 = x;
             TableDataCollectionUtils.addRow(tableDeath, String.valueOf(++iRow), new Object[] {entry.getKey(), value1, value2});
         }
         return new TableDataCollection[] {tableBirth, tableDeath};
     }

     private Map<Integer, double[]> getChiSquaredExponentialityStatisticsAndPvaluesForWaitingTimes(Map<Integer, List<Integer>> populationSizesAndWaitingTimes) throws Exception
     {
         int numberOfIntervals = 20;
         Map<Integer, double[]> result = new HashMap<>();
         int iJobControl = 0;
         for( Map.Entry<Integer, List<Integer>> entry : populationSizesAndWaitingTimes.entrySet() )
         {
             List<Integer> waitingTimes = entry.getValue();
             if( waitingTimes != null && waitingTimes.size() > 10 )
             {
                 double[] statisticAndPvalue = Stat.getStatisticAndPvalueOfChiSquaredTestForExponentiality(waitingTimes, numberOfIntervals);
                 result.put(entry.getKey(), statisticAndPvalue);
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / populationSizesAndWaitingTimes.size());
         }
         return result;
     }

     /***
      * Calculate two tables.
      * 1-st table contains statistics and p-values for Chi-squared tests for exponentiality
      * of times waiting for birth.
      * 2-nd table contains statistics and p-values for Chi-squared tests for exponentiality
      * of times waiting for death.
      * @param populationSizesAndTimesWaitingForBirth
      * @param populationSizesAndTimesWaitingForDeath
      * @return
      * @throws Exception
      */
     TableDataCollection[] getTable_exponentialityChiSquaredStatisticsAndPvaluesForWaitingTimes(Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth, Map<Integer, List<Integer>> populationSizesAndTimesWaitingForDeath) throws Exception
     {
         TableDataCollection tableBirth = TableDataCollectionUtils.createTableDataCollection(parameters.getChiSquaredExponentialityOfTimesWaitingForBirthPath());
         tableBirth.getColumnModel().addColumn("populationSize", Integer.class);
         tableBirth.getColumnModel().addColumn("exponentialityChiSquaredStatistic", Double.class);
         tableBirth.getColumnModel().addColumn("exponentialityChiSquaredPvalue", Double.class);
         Map<Integer, double[]> statisticsAndPvaluesForTimesWaitingForBirth = getChiSquaredExponentialityStatisticsAndPvaluesForWaitingTimes(populationSizesAndTimesWaitingForBirth);
         int iRow = 0;
         for( Map.Entry<Integer, double[]> entry : statisticsAndPvaluesForTimesWaitingForBirth.entrySet() )
         {
             Integer populationSize = entry.getKey();
             double[] values = entry.getValue();
             float x = (float)values[0];
             Float value1 = x;
             x = (float)values[1];
             Float value2 = x;
             TableDataCollectionUtils.addRow(tableBirth, String.valueOf(++iRow), new Object[] {populationSize, value1, value2});
         }
         TableDataCollection tableDeath = TableDataCollectionUtils.createTableDataCollection(parameters.getChiSquaredExponentialityOfTimesWaitingForDeathPath());
         tableDeath.getColumnModel().addColumn("populationSize", Integer.class);
         tableDeath.getColumnModel().addColumn("exponentialityChiSquaredStatistic", Double.class);
         tableDeath.getColumnModel().addColumn("exponentialityChiSquaredPvalue", Double.class);
         Map<Integer, double[]> statisticsAndPvaluesForTimesWaitingForDeath = getChiSquaredExponentialityStatisticsAndPvaluesForWaitingTimes(populationSizesAndTimesWaitingForDeath);
         iRow = 0;
         for( Map.Entry<Integer, double[]> entry : statisticsAndPvaluesForTimesWaitingForDeath.entrySet() )
         {
             double[] values = entry.getValue();
             float x = (float)values[0];
             Float value1 = x;
             x = (float)values[1];
             Float value2 = x;
             TableDataCollectionUtils.addRow(tableDeath, String.valueOf(++iRow), new Object[] {entry.getKey(), value1, value2});
         }
         return new TableDataCollection[] {tableBirth, tableDeath};
     }

     /***
      * The mixture of exponential distributions is estimated by EM algorithm.
      * Calculated table contains the estimated parameters of exponential distributions (namely mean values and sigmas
      * and p-values of chi-squared test for exponentiality.
      * @param populationSizesAndTimesWaitingForBirth
      * @param populationSizesAndTimesWaitingForDeath
      * @return
      * @throws Exception
      */
     private double[][] getExponentialMixtureForTimesWaitingForBirthAndDeath(Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth, Map<Integer, List<Integer>> populationSizesAndTimesWaitingForDeath) throws Exception
     {
         int numberOfIntervals = 20;
         int maximalNumberOfIterations = 35;
         int numberOfMixtureComponents = 5;
         double[][] result = new double[populationSizesAndTimesWaitingForDeath.size()][2 * 5 * numberOfMixtureComponents + 1];
         double[] exponentialParameters = new double[numberOfMixtureComponents];
         double[] probabilitiesOfMixtureComponents = new double[numberOfMixtureComponents];
         int index = 0;
         for( Map.Entry<Integer, List<Integer>> populationForDeathEntry : populationSizesAndTimesWaitingForDeath.entrySet() )
         {
             int populationSize = populationForDeathEntry.getKey();
             result[index][0] = populationSize;
             List<Integer> timesWaitingForBirth = populationSizesAndTimesWaitingForBirth.get(populationSize);
             if( timesWaitingForBirth != null && timesWaitingForBirth.size() > 10 && timesWaitingForBirth.size() > 3 * numberOfMixtureComponents)
             {
                 int max = -1;
                 for( Integer i: timesWaitingForBirth )
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
                 double[][] probabilitiesPij = Stat.estimateExponentialMixtureBy_EM_Algorithm(exponentialParameters, probabilitiesOfMixtureComponents, timesWaitingForBirth, numberOfIterations);
                 for( int i = 0; i < numberOfMixtureComponents; i++ )
                     result[index][1 + i * 5] = 1.0 / exponentialParameters[i];
                 List<List<Integer>> IndexOfSubsampleAndSubsample = Stat.getSubsamplesSimulatedByProbabilitiesPij(probabilitiesPij, timesWaitingForBirth);
                 for( int i = 0; i<IndexOfSubsampleAndSubsample.size(); i++ )
                 {
                     List<Integer> subsample = IndexOfSubsampleAndSubsample.get( i );
                     result[index][2 + i * 5] = subsample.size();
                     double[] meanAndSigma = Stat.getMeanAndSigma(subsample);
                     result[index][3 + i * 5] = meanAndSigma[0];
                     result[index][4 + i * 5] = meanAndSigma[1];
                     double[] chiSquaredStatisticAndPvalue = Stat.getStatisticAndPvalueOfChiSquaredTestForExponentiality(subsample, numberOfIntervals);
                     result[index][5 + i * 5] = chiSquaredStatisticAndPvalue[1];
                 }
             }
             else
                 for( int j = 1; j <= 5 * numberOfMixtureComponents; j++ )
                     result[index][j] = 0.0;
             List<Integer> timesWaitingForDeath = populationForDeathEntry.getValue();
             if( timesWaitingForDeath != null && timesWaitingForDeath.size() > 10 && timesWaitingForDeath.size() > 3 * numberOfMixtureComponents)
             {
                 int max = -1;
                 for( Integer i: timesWaitingForDeath )
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
                 double[][] probabilitiesPij = Stat.estimateExponentialMixtureBy_EM_Algorithm(exponentialParameters, probabilitiesOfMixtureComponents, timesWaitingForDeath, numberOfIterations);
                 for( int i = 0; i < numberOfMixtureComponents; i++ )
                     result[index][1 + 5 * numberOfMixtureComponents + i * 5] = 1.0 / exponentialParameters[i];
                 List<List<Integer>> IndexOfSubsampleAndSubsample = Stat.getSubsamplesSimulatedByProbabilitiesPij(probabilitiesPij, timesWaitingForDeath);
                 for( int i = 0; i<IndexOfSubsampleAndSubsample.size(); i++ )
                 {
                     List<Integer> subsample = IndexOfSubsampleAndSubsample.get( i );
                     result[index][2 + 5 * numberOfMixtureComponents + i * 5] = subsample.size();
                     double[] meanAndSigma = Stat.getMeanAndSigma(subsample);
                     result[index][3 + 5 * numberOfMixtureComponents + i * 5] = meanAndSigma[0];
                     result[index][4 + 5 * numberOfMixtureComponents + i * 5] = meanAndSigma[1];
                     double[] chiSquaredStatisticAndPvalue = Stat.getStatisticAndPvalueOfChiSquaredTestForExponentiality(subsample, numberOfIntervals);
                     result[index][5 + 5 * numberOfMixtureComponents + i * 5] = chiSquaredStatisticAndPvalue[1];
                 }
             }
             else
                 for( int j = 0; j < 5 * numberOfMixtureComponents; j++ )
                     result[index][1 + 5 * numberOfMixtureComponents + j] = 0.0;
             getJobControl().setPreparedness((++index) * 100 / populationSizesAndTimesWaitingForDeath.size());
         }
         return result;
     }

     private TableDataCollection getTable_exponentialMixtureForWaitingTimes(Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth, Map<Integer, List<Integer>> populationSizesAndTimesWaitingForDeath) throws Exception
     {
         double[][] informationTable = getExponentialMixtureForTimesWaitingForBirthAndDeath(populationSizesAndTimesWaitingForBirth, populationSizesAndTimesWaitingForDeath);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.exponentialMixtureForWaitingTimesPath());
         table.getColumnModel().addColumn("populationSize", Double.class);
         int numberOfMixtureComponents = (informationTable[0].length - 1) / 10;
         for( int i = 1; i <= numberOfMixtureComponents; i++ )
         {
             String s1 = "meanTimeWaitingForBirthFrom_EM_inComponent_" + i;
             table.getColumnModel().addColumn(s1, Double.class);
             String s2 = "sizeOfBirthSubsampleInComponent_" + i;
             table.getColumnModel().addColumn(s2, Double.class);
             String s3 = "meanTimeWaitingForBirthFromSimulationInComponent_" + i;
             table.getColumnModel().addColumn(s3, Double.class);
             String s4 = "sigmaOfTimeWaitingForBirthFromSimulationInComponent_" + i;
             table.getColumnModel().addColumn(s4, Double.class);
             String s5 = "pValueOfChiSquaredTestForBirthExponentialityInComponent_" + i;
             table.getColumnModel().addColumn(s5, Double.class);
         }
         for( int i = 1; i <= numberOfMixtureComponents; i++ )
         {
             String s1 = "meanTimeWaitingForDeathFrom_EM_inComponent_" + i;
             table.getColumnModel().addColumn(s1, Double.class);
             String s2 = "sizeOfDeathSubsampleInComponent_" + i;
             table.getColumnModel().addColumn(s2, Double.class);
             String s3 = "meanTimeWaitingForDeathFromSimulationInComponent_" + i;
             table.getColumnModel().addColumn(s3, Double.class);
             String s4 = "sigmaOfTimeWaitingForDeathFromSimulationInComponent_" + i;
             table.getColumnModel().addColumn(s4, Double.class);
             String s5 = "pValueOfChiSquaredTestForDeathExponentialityInComponent_" + i;
             table.getColumnModel().addColumn(s5, Double.class);
         }
         for( int iRow = 0; iRow < informationTable.length; iRow++ )
         {
             Float[] row = new Float[informationTable[0].length];
             for( int j = 0; j < informationTable[0].length; j++ )
             {
                 float x = (float)informationTable[iRow][j];
                 row[j] = x;
             }
             TableDataCollectionUtils.addRow(table, String.valueOf(iRow), row);
         }
         return table;
     }

     /***
      * Map<Integer, Integer> = <numberOfOverlaps, LengthOfOverlaps>; numberOfOverlaps >= 1;
      * Lengths of overlaps are used for calculation of observed probabilities of distinct states
      * in birth-and-death process.
      * @param bindingRegions
      * @return
      */
     private Map<Integer, Integer> getNumberOfOverlapsAndLengthOfOverlapsInOneChromosome(List<BindingRegion> bindingRegions)
     {
         Map<Integer, Integer> numberOfOverlapsAndLength = new HashMap<>();
         int startOfOverlaps = 0;
         int endOfOverlaps = 0;
         int indexOfNewBindingRegion = 0;
         List<BindingRegion> overlappedBindingRegions = new ArrayList<>();
         while( indexOfNewBindingRegion < bindingRegions.size() )
         {
             if( overlappedBindingRegions.size() == 0 )
             {
                 BindingRegion bindingRegion = bindingRegions.get(indexOfNewBindingRegion);
                 overlappedBindingRegions.add(bindingRegion);
                 indexOfNewBindingRegion++;
                 startOfOverlaps = bindingRegion.getStartPosition();
                 endOfOverlaps = bindingRegion.getFinishPosition();
                 if( indexOfNewBindingRegion < bindingRegions.size() )
                 {
                     int candidateForEndOfOverlaps = bindingRegions.get(indexOfNewBindingRegion).getStartPosition();
                     if( candidateForEndOfOverlaps < endOfOverlaps )
                         endOfOverlaps = candidateForEndOfOverlaps;
                 }
             }
             else
             {
                 BindingRegion bindingRegion = bindingRegions.get(indexOfNewBindingRegion);
                 int timeWaitingForBirth = bindingRegion.getStartPosition() - endOfOverlaps;
                 int timeWaitingForDeath = Integer.MAX_VALUE;
                 int indexForNewDeath = 0;
                 for( int i = 0; i < overlappedBindingRegions.size(); i++ )
                 {
                     BindingRegion br = overlappedBindingRegions.get(i);
                     int time = br.getFinishPosition() - endOfOverlaps;
                     if( time < timeWaitingForDeath )
                     {
                         timeWaitingForDeath = time;
                         indexForNewDeath = i;
                     }
                 }
                 if( timeWaitingForBirth <= timeWaitingForDeath )
                 {
                     overlappedBindingRegions.add(bindingRegion);
                     indexOfNewBindingRegion++;
                 }
                 else
                     overlappedBindingRegions.remove(indexForNewDeath);
                 startOfOverlaps = endOfOverlaps;
                 endOfOverlaps = Integer.MAX_VALUE;
                 if( indexOfNewBindingRegion < bindingRegions.size() )
                     endOfOverlaps = bindingRegions.get(indexOfNewBindingRegion).getStartPosition();
                 for( BindingRegion br : overlappedBindingRegions )
                     if( br.getFinishPosition() < endOfOverlaps )
                         endOfOverlaps = br.getFinishPosition();
             }
             if( overlappedBindingRegions.size() > 0 )
             {
                 int lengthOfOverlaps = endOfOverlaps - startOfOverlaps;
                 Integer integer = overlappedBindingRegions.size();
                 if( numberOfOverlapsAndLength.containsKey(integer) )
                     lengthOfOverlaps += numberOfOverlapsAndLength.get(integer);
                 numberOfOverlapsAndLength.put(overlappedBindingRegions.size(), lengthOfOverlaps);
             }
         }
         return numberOfOverlapsAndLength;
     }
     
     private Map<Integer, Double> getObservedProbabilitiesOfOverlaps(Map<String, List<BindingRegion>> allBindingRegions, Map<String, Integer> chromosomeNameAndCorrectedLength)
     {
         Map<Integer, Long> numberOfOverlapsAndLengthOfOverlaps = new HashMap<>();
         int iJobControl = 0;
         for( List<BindingRegion> bindingRegions : allBindingRegions.values() )
         {
             Map<Integer, Integer> numberOfOverlapsAndLengthOfOverlapsInOneChromosome = getNumberOfOverlapsAndLengthOfOverlapsInOneChromosome(bindingRegions);
             for( Map.Entry<Integer, Integer> overlapsEntry : numberOfOverlapsAndLengthOfOverlapsInOneChromosome.entrySet() )
             {
                 Integer numberOfOverlaps = overlapsEntry.getKey();
                 numberOfOverlapsAndLengthOfOverlaps.merge(numberOfOverlaps, (long) overlapsEntry.getValue(),
                        (a, b) -> a + b);
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / allBindingRegions.size());
         }
         long totalLengthOfGenome = IntStreamEx.of(chromosomeNameAndCorrectedLength.values()).asLongStream().sum();
         long totalLengthOfZeroOverlaps = totalLengthOfGenome
                - LongStreamEx.of(numberOfOverlapsAndLengthOfOverlaps.values()).sum();
         numberOfOverlapsAndLengthOfOverlaps.put(0, totalLengthOfZeroOverlaps);
         return Maps.transformValues(numberOfOverlapsAndLengthOfOverlaps,
                length -> ((double) length) / totalLengthOfGenome);
     }
     /***
      * Table contains theoretical and observed probabilities of populations of distinct sizes.
      * Theoretical probabilities were calculated with the help of estimated birth and death rates.
      * @param populationSizeAndBirthAndDeathRates
      * @param allBindingRegions
      * @param chromosomeNameAndCorrectedLength
      * @return
      */
     TableDataCollection getTable_populationSizesAndTheirProbabilities(Map<Integer, double[]> populationSizeAndBirthAndDeathRates, Map<String, List<BindingRegion>> allBindingRegions, Map<String, Integer> chromosomeNameAndCorrectedLength)
     {
         Map<Integer, Double> populationSizeAndTheoreticalProbability = Stat.getTheoreticalProbabilitiesOfPopulationSizes(populationSizeAndBirthAndDeathRates);
         Map<Integer, Double> populationSizeAndObservedProbability = getObservedProbabilitiesOfOverlaps(allBindingRegions, chromosomeNameAndCorrectedLength);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.populationSizesAndTheirProbabilitiesPath());
         table.getColumnModel().addColumn("populationSize", Integer.class);
         table.getColumnModel().addColumn("theoreticalProbability", Double.class);
         table.getColumnModel().addColumn("observedProbability", Double.class);
         int iRow = 0;
         for( Map.Entry<Integer, Double> entry : populationSizeAndObservedProbability.entrySet() )
         {
             Integer populationSize = entry.getKey();
             double x = entry.getValue();
             Float observedProbability = (float)x;
             Float theoreticalProbability = (float)0;
             if( populationSizeAndTheoreticalProbability.containsKey(populationSize) )
             {
                 x = populationSizeAndTheoreticalProbability.get(populationSize);
                 theoreticalProbability = (float)x;
             }
             TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {populationSize, theoreticalProbability, observedProbability});
         }
         return table;
     }

     private TIntLongMap getHistogramOfBindingRegionLength(Map<String, List<BindingRegion>> allBindingRegions)
     {
         TIntLongMap result = new TIntLongHashMap();
         int iJobControl = 0;
         for( List<BindingRegion> bindingRegions : allBindingRegions.values() )
         {
             for( BindingRegion bindingRegion : bindingRegions)
             {
                 result.adjustOrPutValue( bindingRegion.getLengthOfBindingRegion(), 1, 1 );
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / allBindingRegions.size());
         }
         return result;
     }
     
    TableDataCollection getTable_histogramOfBindingRegionLength(Map<String, List<BindingRegion>> allBindingRegions)
    {
        DataElementPath dep = parameters.getCisRegModuleTable();
        DataElementPath dep1 = DataElementPath.create( dep.optParentCollection(), dep.getName() + "histogramOfBindingRegionLength" );
        final TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( dep1 );
        table.getColumnModel().addColumn( "BindingRegionLength", Integer.class );
        table.getColumnModel().addColumn( "NumberOfBindingRegions", Integer.class );
        getHistogramOfBindingRegionLength( allBindingRegions ).forEachEntry( new TIntLongProcedure()
        {
            int iRow = 0;
    
            @Override
            public boolean execute(int length, long i)
            {
                int ii = (int)i;
                Integer frequency = ii;
                TableDataCollectionUtils.addRow( table, String.valueOf( iRow++ ), new Object[] {length, frequency} );
                return true;
            }
        } );
        return table;
    }

     private Map<Integer, long[]> getSummaryOnCisModule2(Map<String, List<CisModule>> allCisModules2)
     {
         Map<Integer, long[]> result = new HashMap<>();
         for( List<CisModule> cisModules2 : allCisModules2.values() )
         {
             for( CisModule cisModule : cisModules2 )
             {
                 int numberOfTfClasses = cisModule.getNumberOfTfClasses();
                 if( result.containsKey(numberOfTfClasses) )
                 {
                     long[] numberOfTfClassesAndMeanLengthOfCisModule = new long[2];
                     numberOfTfClassesAndMeanLengthOfCisModule[0] = result.get(numberOfTfClasses)[0];
                     numberOfTfClassesAndMeanLengthOfCisModule[0]++;
                     numberOfTfClassesAndMeanLengthOfCisModule[1] = result.get(numberOfTfClasses)[1] + cisModule.getLength();
                     result.put(numberOfTfClasses, numberOfTfClassesAndMeanLengthOfCisModule);
                 }
                 else
                     result.put(numberOfTfClasses, new long[] {1, cisModule.getLength()});
             }
         }
         for( Map.Entry<Integer, long[]> entry : result.entrySet() )
         {
             long[] array = new long[2];
             array[0] = entry.getValue()[0];
             array[1] = entry.getValue()[1];
             array[1] /= array[0];
             entry.setValue(array);
         }
         return result;
     }
     
     private TableDataCollection getTable_summaryOnCisModules2(Map<String, List<CisModule>> allCisModules2)
     {
         DataElementPath dep = parameters.getCisRegModuleTable();
         DataElementPath dep1 = DataElementPath.create(dep.optParentCollection(), dep.getName() + "summaryOnCisModules2");
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep1);
         table.getColumnModel().addColumn("numberOfTfClassesInCisModules2", Integer.class);
         table.getColumnModel().addColumn("numberOfCisModules2", Integer.class);
         table.getColumnModel().addColumn("meanLengthOfCisModules2", Integer.class);
         Map<Integer, long[]> numberOfTfClassesAndNumberOfCisModules2AndMeanLengthOfCisModule = getSummaryOnCisModule2(allCisModules2);
         int iRow = 0;
         long numberOfAllCisModules = 0;
         for( Map.Entry<Integer, long[]> entry : numberOfTfClassesAndNumberOfCisModules2AndMeanLengthOfCisModule.entrySet() )
         {
             long numberOfCisModules = entry.getValue()[0];
             numberOfAllCisModules += numberOfCisModules;
             int i = (int)numberOfCisModules;
             Integer frequency = i;
             long meanLengthOfCisModule = entry.getValue()[1];
             i = (int)meanLengthOfCisModule;
             Integer length = i;
             TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {entry.getKey(), frequency, length});
         }
         log.info("number of all CisModules identified = " + numberOfAllCisModules);
         return table;
     }
     
     private void writeCisModules3IntoTable(Map<String, List<CisModule3>> allCisModules3, DataElementPath dataElementPath, String nameOfTable) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), nameOfTable);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
         table.getColumnModel().addColumn("chromosome", String.class);
         table.getColumnModel().addColumn("position1", Integer.class);
         table.getColumnModel().addColumn("position2", Integer.class);
         table.getColumnModel().addColumn("numberOfPivotalTfClasses", Integer.class);
         table.getColumnModel().addColumn("pivotalTfClasses", StringSet.class);
         table.getColumnModel().addColumn("numberOfAdditionalTfClasses", Integer.class);
         table.getColumnModel().addColumn("additionalTfClasses", StringSet.class);
         int iRow = 0;
         List<CisModule3> cisModules0 = allCisModules3.get("X");
         if( cisModules0 != null )
         for( CisModule3 cisModule : cisModules0 )
         {
             Object[] values = new Object[7];
             values[0] = "X";
             int position = cisModule.getPosition();
             values[1] = position - 1;
             values[2] = position + 1;
             List<String> pivotalTfClasses = cisModule.getpivotalTfClasses();
             values[3] = pivotalTfClasses.size();
             StringSet tfClassesSet = new StringSet();
             for( String tfClass : pivotalTfClasses )
                 tfClassesSet.add(tfClass);
             values[4] = tfClassesSet;
             List<String> additionalTfClasses = cisModule.getAdditionalTfClasses();
             Integer numberOfAdditionalTfClasses = additionalTfClasses.size();
             values[5] = numberOfAdditionalTfClasses;
             StringSet tfClassesSet1 = new StringSet();
             for( String tfClass : additionalTfClasses )
                 tfClassesSet1.add(tfClass);
             values[6] = tfClassesSet1;
             TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values, true);
         }
         for( Map.Entry<String, List<CisModule3>> entry : allCisModules3.entrySet() )
         {
             String chromosome = entry.getKey();
             if( chromosome.equals("X") ) continue;
             List<CisModule3> cisModules = entry.getValue();
             for( CisModule3 cisModule : cisModules )
             {
                 Object[] values = new Object[7];
                 values[0] = chromosome;
                 int position = cisModule.getPosition();
                 values[1] = position - 1;
                 values[2] = position + 1;
                 List<String> pivotalTfClasses = cisModule.getpivotalTfClasses();
                 values[3] = pivotalTfClasses.size();
                 StringSet tfClassesSet = new StringSet();
                 for( String tfClass : pivotalTfClasses )
                     tfClassesSet.add(tfClass);
                 values[4] = tfClassesSet;
                 List<String> additionalTfClasses = cisModule.getAdditionalTfClasses();
                 Integer numberOfAdditionalTfClasses = additionalTfClasses.size();
                 values[5] = numberOfAdditionalTfClasses;
                 StringSet tfClassesSet1 = new StringSet();
                 for( String tfClass : additionalTfClasses )
                     tfClassesSet1.add(tfClass);
                 values[6] = tfClassesSet1;
                 TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values, true);
             }
         }
         table.finalizeAddition();
         CollectionFactoryUtils.save(table);
      }
     
     private int getMaximalNumberOfTFClassesInCisModules(Map<String, List<CisModule>> allCisModules)
     {
        return StreamEx.ofValues(allCisModules).flatMap(List::stream).mapToInt(CisModule::getNumberOfTfClasses).max()
                .orElse(0);
     }

     private Map<String, Long> getTfClassesAndNumberOfBindingRegions(List<String> distinctTfClasses, Map<String, List<BindingRegion>> allBindingRegions)
     {
         Map<String, Long> result = StreamEx.of(distinctTfClasses).toMap(k -> 0L);
         StreamEx.ofValues(allBindingRegions).flatMap(List::stream)
                .forEach(br -> result.compute(br.getTfClass(), (k, v) -> v + 1));
         return result;
     }

     private long getSumOfLongValues(Map<String, Long> map)
     {
         return StreamEx.ofValues(map).mapToLong(l -> l).sum();
     }
     
     private Map<Integer, Long> getNumberOfTfClassesAndNumberOfCisModules(Map<String, List<CisModule>> allCisModules)
     {
        return StreamEx.ofValues(allCisModules).flatMap(List::stream)
                .groupingBy(CisModule::getNumberOfDistinctTfClasses, Collectors.counting());
     }

     private Map<String, double[]> getRatiosOfObservedToExpectedProbabilitiesOfTFClassesInCisModulesSubSets(List<String> distinctTfClasses, Map<String, List<CisModule>> allCisModules, Map<String, List<BindingRegion>> allBindingRegions)
     {
         Map<String, double[]> result = new HashMap<>();
         Map<String, long[]> distinctTfClassAndNumbersOfTFClasses = new HashMap <>();
         int maximalNumberOfTFClassesInCisModules = getMaximalNumberOfTFClassesInCisModules(allCisModules);
         for( String distinctTfClass : distinctTfClasses )
         {
             long[] numbersOfTFClasses = new long[maximalNumberOfTFClassesInCisModules + 1];
             for( int i = 0; i <= maximalNumberOfTFClassesInCisModules; i++ )
                 numbersOfTFClasses[i] = 0;
             distinctTfClassAndNumbersOfTFClasses.put(distinctTfClass, numbersOfTFClasses);
         }
         int iJobControl = 0;
         for( List<CisModule> cisModules : allCisModules.values() )
         {
             for( CisModule cisModule : cisModules )
             {
                 List<String> tfClasses = cisModule.getDifferentTfClasses();
                 int numberOfTfClasses = tfClasses.size();
                 for( String tfClass : tfClasses )
                 {
                     long[] numbersOfTFClasses = distinctTfClassAndNumbersOfTFClasses.get(tfClass);
                     numbersOfTFClasses[numberOfTfClasses]++;
                     distinctTfClassAndNumbersOfTFClasses.put(tfClass, numbersOfTFClasses);
                 }
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / allBindingRegions.size());
         }
         Map<String, Long> tfClassesAndNumberOfBindingRegions = getTfClassesAndNumberOfBindingRegions(distinctTfClasses, allBindingRegions);
         long numberOfAllBindingRegions = getSumOfLongValues(tfClassesAndNumberOfBindingRegions);
         Map<Integer, Long> numberOfTfClassesAndNumberOfCisModules = getNumberOfTfClassesAndNumberOfCisModules(allCisModules);
         for( Map.Entry<String, long[]> entry : distinctTfClassAndNumbersOfTFClasses.entrySet() )
         {
             String distinctTfClass = entry.getKey();
             double expectedProbability = (double)tfClassesAndNumberOfBindingRegions.get(distinctTfClass) / (double)numberOfAllBindingRegions;
             long frequencies[] = entry.getValue();
             double ratios[] = new double[frequencies.length];
             for( int i = 0; i < frequencies.length; i++ )
             {
                 if( frequencies[i] == 0 || i == 0 )
                     ratios[i] = 0;
                 else
                 {
                     Integer ii = i;
                     long numberOfCisModules = 0;
                     if( numberOfTfClassesAndNumberOfCisModules.containsKey(ii) )
                         numberOfCisModules = numberOfTfClassesAndNumberOfCisModules.get(ii);
                     double observedProbability = 0.0;
                     if( numberOfCisModules > 0 )
                         observedProbability = (double)frequencies[i] / (double)numberOfCisModules;
                     ratios[i] = observedProbability / expectedProbability;
                 }
             }
             result.put(distinctTfClass, ratios);
         }
         return result;
     }

     TableDataCollection getTable_ratiosOfObservedToExpectedProbabilitiesOfTFClassesInCisModulesSubSets(List<String> distinctTfClasses, Map<String, List<CisModule>> allCisModules, Map<String, List<BindingRegion>> allBindingRegions)
     {
         DataElementPath dep = parameters.getCisRegModuleTable();
         DataElementPath dep1 = DataElementPath.create(dep.optParentCollection(), dep.getName() + "ratiosOfObservedToExpectedProbabilitiesOfTFClassesInCisModulesSubSets");
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep1);
         Map<String, double[]> tfclassAndRatios = getRatiosOfObservedToExpectedProbabilitiesOfTFClassesInCisModulesSubSets(distinctTfClasses, allCisModules, allBindingRegions);
         int numberOfTfClasses = tfclassAndRatios.values().iterator().next().length;
         for( int i = 0; i < numberOfTfClasses; i++ )
         {
             String s = " cisModulesContained_" + i + "_tfClasses";
             table.getColumnModel().addColumn(s, Double.class);
         }
         for( Map.Entry<String, double[]> entry : tfclassAndRatios.entrySet() )
         {
             double[] doubleRatios = entry.getValue();
             Float[] ratios = new Float[numberOfTfClasses];
             for( int i = 0; i < numberOfTfClasses; i++ )
             {
                 float x = (float)doubleRatios[i];
                 ratios[i] = x;
             }
             TableDataCollectionUtils.addRow(table, entry.getKey(), ratios);
         }
         return table;
     }

     /*** exploratory analysis: co-occurrence of tfClasses after division
      * 
      * @param allCisModules
      */
     private void getFriendsOfPivotalTfClasess(List<String> distinctTfClasses, Map<String, List<CisModule>> allCisModules)
     {
         int minimalNumberOfTfClassesInCisModules = 22;
         int iJobControl = 0;
         for( String distinctTfClass : distinctTfClasses )
         {
             int sizeYes = 0;
             int sizeNo = 0;
             Map<String, Integer> tfClassAndFrequencyYes = new HashMap<>();
             Map<String, Integer> tfClassAndFrequencyNo = new HashMap<>();
             Map<String, Integer> tfClassAndFrequency = null;
             for( String tfClass : distinctTfClasses )
             {
                 tfClassAndFrequencyYes.put(tfClass, 0);
                 tfClassAndFrequencyNo.put(tfClass, 0);
             }
             for( List<CisModule> cisModules : allCisModules.values() )
             {
                 for( CisModule cisModule : cisModules )
                 {
                     if( cisModule.getNumberOfDistinctTfClasses() < minimalNumberOfTfClassesInCisModules ) continue;
                     if( cisModule.isBelongToCisModule(distinctTfClass) )
                     {
                         tfClassAndFrequency = tfClassAndFrequencyYes;
                         sizeYes++;
                     }
                     else
                     {
                         tfClassAndFrequency = tfClassAndFrequencyNo;
                         sizeNo++;
                     }
                     List<String> tfClasses = cisModule.getDifferentTfClasses();
                     for( String tfClass : tfClasses )
                     {
                         int i = tfClassAndFrequency.get(tfClass);
                         tfClassAndFrequency.put(tfClass, ++i);
                     }
                 }
             }
             log.info("pivotal tfClass = " + distinctTfClass + " sizeYes = " + sizeYes + " sizeNo = " + sizeNo);
             for( Map.Entry<String, Integer> entry : tfClassAndFrequencyYes.entrySet() )
             {
                 String tfClass = entry.getKey();
                 int frequencyYes = entry.getValue();
                 int frequencyNo = tfClassAndFrequencyNo.get(tfClass);
                 if( frequencyYes > 0 && frequencyNo > 0 )
                 {
                     double probabilityYes = 0.0;
                     double probabilityNo = 0.0;
                     if ( sizeYes > 0 )
                         probabilityYes = (double)frequencyYes / (double)sizeYes;
                     if( sizeNo > 0 )
                         probabilityNo = (double) frequencyNo / (double)sizeNo;
                     double ratio = 0.0;
                     if( probabilityNo > 0.0 )
                         ratio = probabilityYes / probabilityNo;
                     if (probabilityYes > probabilityNo )
                         log.info("his friend = " + tfClass + " with probability in Yes Sample = " + probabilityYes + " with probability in No Sample = " + probabilityNo + " ratio = " + ratio);
                 }
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / distinctTfClasses.size());
         }
     }

     private static class IdenticalCisModules
     {
         List<String> tfClasses;
         Map<String, List<int[]>> chromosomeAndStartAndFinish;
         
         IdenticalCisModules(List<String> tfClasses, String chromosome, int start, int finish)
         {
             this.tfClasses = tfClasses;
             int[] startAndFinish = new int[2];
             startAndFinish[0] = start;
             startAndFinish[1] = finish;
             List<int[]> listOfStartAndFinish = new ArrayList<>();
             listOfStartAndFinish.add(startAndFinish);
             Map<String, List<int[]>> chromosomeAndStartAndFinish = new HashMap<>();
             chromosomeAndStartAndFinish.put(chromosome, listOfStartAndFinish);
             this.chromosomeAndStartAndFinish = chromosomeAndStartAndFinish;
         }
         
         public List<String> getTfClasses()
         {
             return tfClasses;
         }
         
         public Map<String, List<int[]>> getChromosomeAndStartAndFinish()
         {
             return chromosomeAndStartAndFinish;
         }

         public int getNumberOfIdenticalCisModules()
         {
             int result = 0;
             for( List<int[]> listOfIdenticalCisModules : chromosomeAndStartAndFinish.values() )
             {
                 result += listOfIdenticalCisModules.size();
             }
             return result;
         }
         
         public boolean areTfClassesTheSame(List<String> tfClasses)
         {
             for( String tfClass : this.tfClasses )
                 if( ! tfClasses.contains(tfClass) ) return false;
             return true;
         }
         
         public void addCoordinatesOfNewCisModule(String chromosome, int start, int finish)
         {
             int[] startAndFinish = new int[2];
             startAndFinish[0] = start;
             startAndFinish[1] = finish;
             chromosomeAndStartAndFinish.computeIfAbsent(chromosome, k -> new ArrayList<>())
                 .add(startAndFinish);
         }
     }
     
     private Map<Integer, List<IdenticalCisModules>> getNumberOfTfClassesAndIdenticalCisModules(Map<String, List<CisModule>> allCisModules)
     {
         Map<Integer, List<IdenticalCisModules>> result = new HashMap<>();
         int iJobControl = 0;
         for( Map.Entry<String, List<CisModule>> entry : allCisModules.entrySet() )
         {
             String chromosome = entry.getKey();
             List<CisModule> cisModules = entry.getValue();
             for( CisModule cisModule : cisModules )
             {
                 int start = cisModule.getStartPosition();
                 int finish = cisModule.getFinishPosition();
                 int numberOfTfClasses = cisModule.getNumberOfTfClasses();
                 Integer integerNumberOfTfClasses = numberOfTfClasses;
                 List<String> tfClasses = cisModule.getTfClasses();
                 if( ! result.containsKey(integerNumberOfTfClasses) )
                 {
                     IdenticalCisModules identicalCisModules = new IdenticalCisModules(tfClasses, chromosome, start, finish);
                     List<IdenticalCisModules> result1 = new ArrayList<>();
                     result1.add(identicalCisModules);
                     result.put(integerNumberOfTfClasses, result1);
                 }
                 else
                 {
                     boolean cisModuleExist = false;
                     List<IdenticalCisModules> listOfIdenticalCisModules = result.get(integerNumberOfTfClasses);
                     for( IdenticalCisModules identicalCisModules : listOfIdenticalCisModules )
                         if( identicalCisModules.areTfClassesTheSame(tfClasses) )
                         {
                             identicalCisModules.addCoordinatesOfNewCisModule(chromosome, start, finish);
                             cisModuleExist = true;
                             break;
                         }
                     if( cisModuleExist == false )
                     {
                         IdenticalCisModules identicalCisModules = new IdenticalCisModules(tfClasses, chromosome, start, finish);
                         listOfIdenticalCisModules.add(identicalCisModules);
                         result.put(integerNumberOfTfClasses, listOfIdenticalCisModules);
                     }
                 }
              }
             getJobControl().setPreparedness((++iJobControl) * 100 / allCisModules.size());
         }
         return result;
     }
     
     /*** exploratory analysis: identification of identical CisModules **/
     private void printInformationOnIdenticalCisModules(Map<Integer, List<IdenticalCisModules>> numberOfTfClassesAndIdenticalCisModules)
     {
         log.info("distinctNumbersOfTfClasses = " + numberOfTfClassesAndIdenticalCisModules.size());
         for( Map.Entry<Integer, List<IdenticalCisModules>> entry : numberOfTfClassesAndIdenticalCisModules.entrySet() )
         {
             List<IdenticalCisModules> listOfIdenticalCisModules = entry.getValue();
             for( IdenticalCisModules identicalCisModules : listOfIdenticalCisModules )
             {
                 int numberOfIdenticalCisModules = identicalCisModules.getNumberOfIdenticalCisModules();
                 if( numberOfIdenticalCisModules < 2 ) continue;
                 log.info(" numberOfTfClasses = " + entry.getKey() + " numberOfIdenticalCisModules = " + numberOfIdenticalCisModules);
                 List<String> tfClasses = identicalCisModules.getTfClasses();
                 for( String tfClass : tfClasses)
                     log.info("tfClass = " + tfClass);
                 Map<String, List<int[]>> chromosomeAndStartAndEnd = identicalCisModules.getChromosomeAndStartAndFinish();
                 for( Map.Entry<String, List<int[]>> chromosomeAndStartAndEndEntry : chromosomeAndStartAndEnd.entrySet() )
                 {
                     String chr = chromosomeAndStartAndEndEntry.getKey();
                     List<int[]> allPos = chromosomeAndStartAndEndEntry.getValue();
                     for( int[] pos : allPos )
                         log.info("chromosome = " + chr + " start = " + pos[0] + " end = " + pos[1]);
                 }
             }
         }
     }

     TableDataCollection getTable_identicalCisModules(Map<Integer, List<IdenticalCisModules>> numberOfTfClassesAndIdenticalCisModules)
     {
         DataElementPath dep = parameters.getCisRegModuleTable();
         DataElementPath dep1 = DataElementPath.create(dep.optParentCollection(), dep.getName() + "identicalCisModules");
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep1);
         int numberOfDistinctCisModules = 0;
         for( List<IdenticalCisModules> listOfIdenticalCisModules : numberOfTfClassesAndIdenticalCisModules.values() )
         {
             numberOfDistinctCisModules += listOfIdenticalCisModules.size();
         }
         log.info("numberOfDistinctCisModules = " + numberOfDistinctCisModules);
         int maxNumberOfTfClasses = 0;
         for( int i : numberOfTfClassesAndIdenticalCisModules.keySet() )
         {
             if (i > maxNumberOfTfClasses )
                 maxNumberOfTfClasses = i;
         }
         table.getColumnModel().addColumn("numberOfIdenticalCisModules", Integer.class);
         table.getColumnModel().addColumn("numberOfTfClasses", Integer.class);
         table.getColumnModel().addColumn("tfClasses", StringSet.class);
         int iRow = 0;
         for( Map.Entry<Integer, List<IdenticalCisModules>> entry : numberOfTfClassesAndIdenticalCisModules.entrySet() )
         {
             List<IdenticalCisModules> listOfIdenticalCisModules = entry.getValue();
             for( IdenticalCisModules identicalCisModules : listOfIdenticalCisModules )
             {
                 Integer numberOfIdenticalCisModules = identicalCisModules.getNumberOfIdenticalCisModules();
                 if( numberOfIdenticalCisModules < 2 ) continue;
                 List<String> tfClasses = identicalCisModules.getTfClasses();
                 Object[] values = new Object[3];
                 StringSet factors = new StringSet();
                 for( String tfClass : tfClasses )
                     factors.add(tfClass);
                 values[0] = numberOfIdenticalCisModules;
                 values[1] = entry.getKey();
                 values[2] = factors;
                 TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values);
             }
         }
         return table;
     }
     
     public static Map<String, Map<String, List<BindingRegion>>> readTfClassAndChromosomeAndBindingRegionsInTable(DataElementPath dataElementPath, String nameOfTable) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), nameOfTable);
         Map<String, Map<String, List<BindingRegion>>> result = new HashMap<>();
         TableDataCollection table = dep.getDataElement(TableDataCollection.class);
//       int numberOfRows = table.getSize();
         ObjectCache<String> tfClassNames = new ObjectCache<>();        //
//       int numberOfColumns = table.getColumnModel().getColumnCount();
         for( RowDataElement row : table )
         {
             Object[] objects = row.getValues();
             String chromosome = (String)objects[0];
             Integer j = (Integer)objects[1];
             int startPosition = j;
             j = (Integer)objects[2];
             int finishPosition = j;
             String tfClass = tfClassNames.get( (String)objects[3] );
             j = (Integer)row.getValues()[4];
             int numberOfOverlaps = j;
             BindingRegion bindingRegion = new BindingRegion(tfClass, startPosition, finishPosition, numberOfOverlaps);
             result
                 .computeIfAbsent( tfClass, k -> new HashMap<>() )
                 .computeIfAbsent( chromosome, k -> new ArrayList<>() )
                 .add( bindingRegion );
         }
         return result;
     }
     
     /***
      * Read cis-modules patterns in table "cisModulePatterns"
      * @param
      * @return
      */
     private Map<String, List<String>> readPatternIdsAndCisModulePatternsInTable(DataElementPath dataElementPath, String nameOfTable)
     {
         Map<String, List<String>> result = new HashMap<>();
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), nameOfTable);
         TableDataCollection table = dep.getDataElement(TableDataCollection.class);
         for( RowDataElement row : table )
         {
             String patternId = row.getName();
             Object[] objects = row.getValues();
             int numberOfTfClasses = (Integer)objects[0];
             List<String> tfClasses = new ArrayList<>();
             for( int jj = 0; jj < numberOfTfClasses; jj++ )
                 tfClasses.add((String)objects[jj + 1]);
             result.put(patternId, tfClasses);
         }
         return result;
     }
     
     private Map<String, List<BindingRegion>> selectBindingRegions(Map<String, List<BindingRegion>> allBindingRegions, List<String> selectedTfClasses)
     {
         return Maps.transformValues( allBindingRegions, bindingRegions ->
                 StreamEx.of(bindingRegions).filter(br -> selectedTfClasses.contains(br.getTfClass())).toList());
     }
     
     private Map<String, List<CisModule>> getCisModules2WithGivenPattern(Map<String, List<BindingRegion>> allBindingRegions, List<String> selectedTfClasses, int minimalNumberOfOverlaps)
     {
         Map<String, List<BindingRegion>> selectedBindingRegions = selectBindingRegions(allBindingRegions, selectedTfClasses);
         ListUtil.sortAll(selectedBindingRegions);
         Map<String, List<CisModule>> result = CisModule.getAllCisModules2(selectedBindingRegions, minimalNumberOfOverlaps);
         return result;
     }

    public static Map<String, List<CisModule>> readChromosomeAndCisModules1(DataElementPath dataElementPath, String nameOfTable)
            throws Exception
    {
        DataElementPath dep = DataElementPath.create( dataElementPath.optParentCollection(), nameOfTable );
        Map<String, List<CisModule>> result = new HashMap<>();
        TableDataCollection table = dep.getDataElement( TableDataCollection.class );
        int numberOfRows = table.getSize();
        int numberOfColumns = table.getColumnModel().getColumnCount();
        String[] distinctTfClasses = new String[numberOfColumns - 4];
        for( int j = 4; j < numberOfColumns; j++ )
            distinctTfClasses[j - 4] = table.getColumnModel().getColumn( j ).getName();
        for( int i = 0; i < numberOfRows; i++ )
        {
            RowDataElement row = table.getAt( i );
            Object[] objects = row.getValues();
            String chromosome = (String)objects[0];
            String s = (String)objects[1];
            int startPosition = Integer.parseInt( s );
            s = (String)objects[2];
            int finishPosition = Integer.parseInt( s );
            List<String> tfClasses = new ArrayList<>();
            for( int j = 4; j < numberOfColumns; j++ )
            {
                String ss = (String)objects[j];
                int frequency = Integer.parseInt( ss );
                if( frequency > 0 )
                    for( int jj = 0; jj < frequency; jj++ )
                        tfClasses.add( distinctTfClasses[j - 4] );
            }
            result.computeIfAbsent( chromosome, k -> new ArrayList<>() ).add( new CisModule( startPosition, finishPosition, tfClasses ) );
        }
        return result;
    }

    private Map<String, List<Gene>> readGenesInTable(DataElementPath dataElementPath, String nameOfTable) throws Exception
    {
        DataElementPath dep = dataElementPath.getSiblingPath( nameOfTable );
        Map<String, List<Gene>> result = new HashMap<>();
        TableDataCollection table = dep.getDataElement( TableDataCollection.class );
        for( RowDataElement row : table )
        {
            Object[] objects = row.getValues();
            String ensemblId = (String)objects[0];
            String geneType = (String)objects[1];
            String chromosome = (String)objects[2];
            StringSet startsSet = (StringSet)objects[3];
            StringSet endsSet = (StringSet)objects[4];
            int[] starts = startsSet.stream().mapToInt( Integer::parseInt ).toArray();
            int[] ends = endsSet.stream().mapToInt( Integer::parseInt ).toArray();
            result.computeIfAbsent( chromosome, k -> new ArrayList<>() ).add( new Gene( ensemblId, geneType, starts, ends ) );
        }
        return result;
    }
     
// need to improve
    public static Map<String, Integer> readMap_String_IntegerInTable(DataElementPath dataElementPath, String nameOfTable)
    {
        DataElementPath dep = dataElementPath.getChildPath( nameOfTable );
        TableDataCollection table = dep.getDataElement( TableDataCollection.class );
        return table.stream().map( RowDataElement::getValues )
                .collect( Collectors.toMap( vals -> (String)vals[0], vals -> (Integer)vals[1] ) );
    }

    private Map<String, List<Gene>> selectGivenGenes(List<String> ensemblIdsOfGivenGenes, Map<String, List<Gene>> chromosomesAndGenes)
            throws Exception
    {
        return EntryStream.of(chromosomesAndGenes)
            .mapValues(val -> StreamEx.of(val).filter(gene -> ensemblIdsOfGivenGenes.contains(gene.getEnsemblId())).toList())
            .removeValues(List::isEmpty).toMap();
    }

    private List<String> readListOfStringsInSecondColumnOfTable(DataElementPath dataElementPath, String nameOfTable) throws Exception
    {
        DataElementPath dep = dataElementPath.getSiblingPath( nameOfTable );
        TableDataCollection table = dep.getDataElement( TableDataCollection.class );
        return table.stream().map( row -> (String)row.getValues()[0] ).collect( Collectors.toList() );
    }
     
    private void writeCisModules2IntoTable(Map<String, List<CisModule>> allCisModules2, DataElementPath pathToTables, String nameOfTable)
            throws Exception
    {
        DataElementPath dep = pathToTables.getChildPath( nameOfTable );
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( dep );
        table.getColumnModel().addColumn( "chromosome", String.class );
        table.getColumnModel().addColumn( "startPosition", Integer.class );
        table.getColumnModel().addColumn( "endPosition", Integer.class );
        table.getColumnModel().addColumn( "numberOfTfClasses", Integer.class );
        table.getColumnModel().addColumn( "tfClasses", StringSet.class );
        int iRow = 0;
        List<CisModule> cisModules0 = allCisModules2.get( "X" );
        if( cisModules0 != null )
            for( CisModule cisModule : cisModules0 )
            {
                Object[] values = new Object[5];
                values[0] = "X";
                Integer startPosition = cisModule.getStartPosition();
                values[1] = startPosition;
                Integer endPosition = cisModule.getFinishPosition();
                values[2] = endPosition;
                Integer numberOfTfClasses = cisModule.getNumberOfTfClasses();
                values[3] = numberOfTfClasses;
                List<String> tfClasses = cisModule.getTfClasses();
                StringSet tfClassesSet = new StringSet();
                for( String tfClass : tfClasses )
                    tfClassesSet.add( tfClass );
                values[4] = tfClassesSet;
                TableDataCollectionUtils.addRow( table, String.valueOf( iRow++ ), values, true );
            }
        for( Map.Entry<String, List<CisModule>> entry : allCisModules2.entrySet() )
        {
            String chromosome = entry.getKey();
            if( chromosome.equals( "X" ) )
                continue;
            List<CisModule> cisModules = entry.getValue();
            for( CisModule cisModule : cisModules )
            {
                Object[] values = new Object[5];
                values[0] = chromosome;
                Integer startPosition = cisModule.getStartPosition();
                values[1] = startPosition;
                Integer endPosition = cisModule.getFinishPosition();
                values[2] = endPosition;
                Integer numberOfTfClasses = cisModule.getNumberOfTfClasses();
                values[3] = numberOfTfClasses;
                List<String> tfClasses = cisModule.getTfClasses();
                StringSet tfClassesSet = new StringSet();
                for( String tfClass : tfClasses )
                    tfClassesSet.add( tfClass );
                values[4] = tfClassesSet;
                TableDataCollectionUtils.addRow( table, String.valueOf( iRow++ ), values, true );
            }
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save( table );
    }

    private Map<String, Float> calculateFrequenciesOfDistinctTfClassesInCisModules(int minimalSizeOfCisModule,
            Map<String, List<CisModule>> chromosomeAndCisModules)
    {
        Map<String, Integer> result0 = new HashMap<>();
        for( List<CisModule> cisModules : chromosomeAndCisModules.values() )
        {
            for( CisModule cisModule : cisModules )
            {
                if( cisModule.getNumberOfTfClasses() < minimalSizeOfCisModule )
                    continue;
                List<String> tfClasses = cisModule.getDifferentTfClasses();
                for( String tfClass : tfClasses )
                {
                    result0.merge( tfClass, 1, (a, b) -> a + b );
                }
            }
        }
        float numberOfCisModules = StreamEx.ofValues( result0 ).mapToInt( x -> x ).sum();
        return Maps.transformValues( result0, val -> val / numberOfCisModules );
    }

     private StreamEx<BindingRegion> selectBindingRegions(List<BindingRegion> bindingRegions, String tfClass1, String tfClass2)
     {
        return StreamEx.of( bindingRegions ).filter( br -> br.getTfClass().equals( tfClass1 ) || br.getTfClass().equals( tfClass2 ) );
     }
     
     private StreamEx<BindingRegion> selectBindingRegions(List<BindingRegion> bindingRegions, String tfClass)
     {
         return StreamEx.of(bindingRegions).filter( br -> br.getTfClass().equals( tfClass ) );
     }

     private long[] calculateContingencyTableOfOccurenceOfMatchedTfClasses(String tfClass1, String tfClass2, Map<String, List<BindingRegion>> allBindingRegions, Map<String, Integer> chromosomeNameAndCorrectedLength)
     {
         long result[] = new long[4];
         for( int i = 0;  i < result.length; i++ )
             result[i] = 0;
         for( List<BindingRegion> bindingRegions : allBindingRegions.values() )
         {
             int n01 = 0;
             int n10 = 0;
             int n11 = 0;
             List<BindingRegion> selectedBindingRegions = selectBindingRegions(bindingRegions, tfClass1, tfClass2).sorted().toList();
             List<CisModule> cisModules2 = CisModule.getCisModules2InOneChromosome(selectedBindingRegions, 2);
             Collections.sort(cisModules2);
             for( CisModule cisModule : cisModules2 )
                 n11 += cisModule.getLength();
             int iCisModule = 0;
             for (int iBindingRegion = 0; iBindingRegion < selectedBindingRegions.size(); iBindingRegion++ )
             {
                 BindingRegion bindingRegion = selectedBindingRegions.get(iBindingRegion);
                 boolean indicatorOfOverlap = false;
                 while( iCisModule < cisModules2.size() )
                 {
                     CisModule cisModule = cisModules2.get(iCisModule);
                     indicatorOfOverlap = areOverlappedTwoSites(bindingRegion.getStartPosition(), bindingRegion.getFinishPosition(), cisModule.getStartPosition(), cisModule.getFinishPosition());
                     if( indicatorOfOverlap == true ) break;
                     if( cisModule.getFinishPosition() < bindingRegion.getStartPosition() )
                         iCisModule++;
                     else break;
                 }
                 if( indicatorOfOverlap == false ) continue;
                 if( bindingRegion.getTfClass().equals(tfClass1) )
                     n10 += bindingRegion.getLengthOfBindingRegion() - cisModules2.get(iCisModule).getLength();
                 else
                     n01 += bindingRegion.getLengthOfBindingRegion() - cisModules2.get(iCisModule).getLength();
             }
             result[1] += n01;
             result[2] += n10;
             result[3] += n11;
         }
         result[0] = 0;
         for( int correctLength : chromosomeNameAndCorrectedLength.values() )
             result[0] += correctLength;
         result[0] -= result[1] + result[2] + result[3];
         return result;
     }

     private void getTable_chiSquaredDependenceOfPairedTfClasses(List<String> distinctTfClasses, Map<String, List<BindingRegion>> allBindingRegions, Map<String, Integer> chromosomeNameAndCorrectedLength, DataElementPath dataElementPath, String nameOfTable) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), dataElementPath.getName() + nameOfTable);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
         table.getColumnModel().addColumn("1stTfClass", String.class);
         table.getColumnModel().addColumn("2ndTfClass", String.class);
         table.getColumnModel().addColumn("chiSquaredStatistic", Double.class);
         table.getColumnModel().addColumn("pvalue", Double.class);
         table.getColumnModel().addColumn("ratio=p12<>(p1*p2)", Double.class);
         int iRow = 0;
         int iJobControl = 0;
         for (int iTfClass1 = 0; iTfClass1 < distinctTfClasses.size(); iTfClass1++ )
         {
             String tfClass1 = distinctTfClasses.get(iTfClass1);
             for (int iTfClass2 = iTfClass1 + 1; iTfClass2 < distinctTfClasses.size(); iTfClass2++ )
             {
                 String tfClass2 = distinctTfClasses.get(iTfClass2);
                 long[] contingencyTable = calculateContingencyTableOfOccurenceOfMatchedTfClasses(tfClass1, tfClass2, allBindingRegions, chromosomeNameAndCorrectedLength);
                 if ( contingencyTable[2] + contingencyTable[3] == 0 || contingencyTable[1] + contingencyTable[3] == 0 ) continue;
                 double statistic = Stat.getStatisticOfChiSquared_2x2_testForIndependence(contingencyTable);
                 double pValue = 1.0 - Stat.chiDistribution(statistic, 1.0);
                 long j = 0;
                 for( long element : contingencyTable )
                    j += element;
                 double ratio = (double)contingencyTable[3] / (double)j;
                 ratio /= (double)(contingencyTable[2] + contingencyTable[3]) / (double)j;
                 ratio /= (double)(contingencyTable[1] + contingencyTable[3]) / (double)j;
                 Float x = (float)statistic;
                 Float xx = (float)pValue;
                 Float xxx = (float)ratio;
                 TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {tfClass1, tfClass2, x, xx, xxx}, true);
                 log.info("tfClass1 = " + tfClass1 + " tfClass2 = " + tfClass2 + " statistic = " + statistic + " pValue = " + pValue + " ratio = " + ratio + " table = " + contingencyTable[0] + " " + contingencyTable[1] + " " + contingencyTable[2] + " " + contingencyTable[3]);
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / distinctTfClasses.size());
         }
         table.finalizeAddition();
         CollectionFactoryUtils.save(table);
     }

     private void writeTablesForMode_8() throws Exception
     {
         log.info("Chi-squared test of independence of paired tfClasses (on the base of 2x2-contingency tables) by using all binding regions");
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read binding regions in table 'bindingRegions'");
         Map<String, List<BindingRegion>> allBindingRegions = BindingRegion.readBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("All binding regions are sorting");
         ListUtil.sortAll(allBindingRegions);
         log.info("Calculation of resulted table");
         Set<String> distinctTfClasses = BindingRegion.getDistinctTfClasses(allBindingRegions);
         log.info("Number of distinct tfClasses = " + distinctTfClasses.size());
         Map<String, Integer> chromosomeNameAndCorrectedLength = readMap_String_IntegerInTable(dataElementPath, "correctedChromosomeLengths");
         getTable_chiSquaredDependenceOfPairedTfClasses(new ArrayList<>(distinctTfClasses), allBindingRegions, chromosomeNameAndCorrectedLength, dataElementPath, "_chiSquaredDependenceOfPairedTfClasses");
     }

//     if two genes are located closed to each other and distanceThreshold is large
//     then some selected binding regions may be duplicated. It is not good
//     More correct version of 'selectBindingRegionsLocatedNearGivenGenes' is 'selectBindingRegionsLocatedNearGivenGenes1'
     private Map<String, List<BindingRegion>> selectBindingRegionsLocatedNearGivenGenes(Map<String, List<Gene>> chromosomesAndGivenGenes, Map<String, List<BindingRegion>> allBindingRegions, int distanceThreshold)
     {
         Map<String, List<BindingRegion>> result = new HashMap<>();
         for( Map.Entry<String, List<Gene>> entry : chromosomesAndGivenGenes.entrySet() )
         {
             String chromosome = entry.getKey();
             List<Gene> genes = entry.getValue();
             List<BindingRegion> bindingRegions = allBindingRegions.get(chromosome);
             for( Gene gene : genes )
             {
                 int[] geneBoundaries = gene.getStartAndEndOfGene();
                 geneBoundaries[0] -= distanceThreshold;
                 if( geneBoundaries[0] < 1) geneBoundaries[0] = 1;
                 geneBoundaries[1] += distanceThreshold;
                 for( BindingRegion bindingRegion : bindingRegions )
                 {
                     if( bindingRegion.getStartPosition() > geneBoundaries[1] ) break;
                     if( bindingRegion.getFinishPosition() < geneBoundaries[0] ) continue;
                     result.computeIfAbsent( chromosome, k -> new ArrayList<>() ).add( bindingRegion );
                 }
             }
         }
         return result;
     }

// more correct version of 'selectBindingRegionsLocatedNearGivenGenes'
     private Map<String, List<BindingRegion>> selectBindingRegionsLocatedNearGivenGenes1(Map<String, List<Gene>> chromosomesAndGivenGenes, Map<String, List<BindingRegion>> allBindingRegions, int distanceThreshold)
     {
         Map<String, List<BindingRegion>> result = new HashMap<>();
         for( Map.Entry<String, List<Gene>> entry : chromosomesAndGivenGenes.entrySet() )
         {
             String chromosome = entry.getKey();
             List<Gene> genes = entry.getValue();
             List<BindingRegion> bindingRegions = allBindingRegions.get(chromosome);
             int[] starts = new int[genes.size()];
             int[] ends = new int[genes.size()];
             int i = -1;
             for( Gene gene : genes )
             {
                 int[] geneBoundaries = gene.getStartAndEndOfGene();
                 starts[++i] = geneBoundaries[0] - distanceThreshold;
                 ends[i] = geneBoundaries[1] + distanceThreshold;
             }
             for( BindingRegion bindingRegion : bindingRegions )
             {
                 for( i = 0; i < starts.length; i++ )
                 {
                     if( starts[i] > bindingRegion.getFinishPosition() ) break;
                     if( ends[i] < bindingRegion.getStartPosition() ) continue;
                     result.computeIfAbsent( chromosome, k -> new ArrayList<>() ).add( bindingRegion );
                     break;
                 }
             }
         }
         return result;
     }
     
     private Map<String, List<int[]>> getSelectedGeneRegions(Map<String, List<Gene>> chromosomesAndGivenGenes, int distanceThreshold)
     {
         return Maps.transformValues( chromosomesAndGivenGenes, genes ->
                 StreamEx.of(genes).map( gene -> {
                     int[] selectedGeneRegion = gene.getStartAndEndOfGene();
                     selectedGeneRegion[0] -= distanceThreshold;
                     if( selectedGeneRegion[0] < 1) selectedGeneRegion[0] = 1;
                     selectedGeneRegion[1] += distanceThreshold;
                     return selectedGeneRegion;
                 }).toList());
     }

     private long[] calculateContingencyTableOfOccurenceOfMatchedTfClasses1(String tfClass1, String tfClass2, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<int[]>> selectedGeneRegions)
     {
         long result[] = new long[4];
         for( int i = 0;  i < result.length; i++ )
             result[i] = 0;
         for( List<BindingRegion> bindingRegions : allBindingRegions.values() )
         {
             int n01 = 0;
             int n10 = 0;
             int n11 = 0;
             List<BindingRegion> selectedBindingRegions = selectBindingRegions(bindingRegions, tfClass1, tfClass2).sorted().toList();
             List<CisModule> cisModules2 = CisModule.getCisModules2InOneChromosome(selectedBindingRegions, 2);
             Collections.sort(cisModules2);
             for( CisModule cisModule : cisModules2 )
                 n11 += cisModule.getLength();
             int iCisModule = 0;
             for (int iBindingRegion = 0; iBindingRegion < selectedBindingRegions.size(); iBindingRegion++ )
             {
                 BindingRegion bindingRegion = selectedBindingRegions.get(iBindingRegion);
                 boolean indicatorOfOverlap = false;
                 while( iCisModule < cisModules2.size() )
                 {
                     CisModule cisModule = cisModules2.get(iCisModule);
                     indicatorOfOverlap = areOverlappedTwoSites(bindingRegion.getStartPosition(), bindingRegion.getFinishPosition(), cisModule.getStartPosition(), cisModule.getFinishPosition());
                     if( indicatorOfOverlap == true ) break;
                     if( cisModule.getFinishPosition() < bindingRegion.getStartPosition() )
                         iCisModule++;
                     else break;
                 }
                 if( indicatorOfOverlap == false ) continue;
                 if( bindingRegion.getTfClass().equals(tfClass1) )
                     n10 += bindingRegion.getLengthOfBindingRegion() - cisModules2.get(iCisModule).getLength();
                 else
                     n01 += bindingRegion.getLengthOfBindingRegion() - cisModules2.get(iCisModule).getLength();
             }
             result[1] += n01;
             result[2] += n10;
             result[3] += n11;
         }
         result[0] = 0;
         for( List<int[]> list : selectedGeneRegions.values() )
         {
             for( int[] region : list )
                 result[0] += region[1] - region[0] + 1;
         }
         result[0] -= result[1] + result[2] + result[3];
         return result;
     }

     private void getTable_chiSquaredDependenceOfPairedTfClassesInGivenGenes(List<String> distinctTfClasses, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<int[]>> selectedGeneRegions, DataElementPath dataElementPath, String nameOfTable) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), dataElementPath.getName() + nameOfTable);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
         table.getColumnModel().addColumn("1stTfClass", String.class);
         table.getColumnModel().addColumn("2ndTfClass", String.class);
         table.getColumnModel().addColumn("chiSquaredStatistic", Double.class);
         table.getColumnModel().addColumn("pvalue", Double.class);
         table.getColumnModel().addColumn("ratio=p12<>(p1*p2)", Double.class);
         int iRow = 0;
         int iJobControl = 0;
         for (int iTfClass1 = 0; iTfClass1 < distinctTfClasses.size(); iTfClass1++ )
         {
             String tfClass1 = distinctTfClasses.get(iTfClass1);
             for (int iTfClass2 = iTfClass1 + 1; iTfClass2 < distinctTfClasses.size(); iTfClass2++ )
             {
                 String tfClass2 = distinctTfClasses.get(iTfClass2);
                 long[] contingencyTable = calculateContingencyTableOfOccurenceOfMatchedTfClasses1(tfClass1, tfClass2, allBindingRegions, selectedGeneRegions);
                 if ( contingencyTable[2] + contingencyTable[3] == 0 || contingencyTable[1] + contingencyTable[3] == 0 ) continue;
                 double statistic = Stat.getStatisticOfChiSquared_2x2_testForIndependence(contingencyTable);
                 double pValue = 1.0 - Stat.chiDistribution(statistic, 1.0);
                 long j = 0;
                 for( long element : contingencyTable )
                    j += element;
                 double ratio = (double)contingencyTable[3] / (double)j;
                 ratio /= (double)(contingencyTable[2] + contingencyTable[3]) / (double)j;
                 ratio /= (double)(contingencyTable[1] + contingencyTable[3]) / (double)j;
                 Float x = (float)statistic;
                 Float xx = (float)pValue;
                 Float xxx = (float)ratio;
                 TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {tfClass1, tfClass2, x, xx, xxx}, true);
                 log.info("tfClass1 = " + tfClass1 + " tfClass2 = " + tfClass2 + " statistic = " + statistic + " pValue = " + pValue + " ratio = " + ratio + " table = " + contingencyTable[0] + " " + contingencyTable[1] + " " + contingencyTable[2] + " " + contingencyTable[3]);
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / distinctTfClasses.size());
         }
         table.finalizeAddition();
         CollectionFactoryUtils.save(table);
     }

     private void writeTablesForMode_9() throws Exception
     {
         log.info("Chi-squared test of independence of paired tfClasses near given genes (on the base of 2x2-contingency tables) by using selected binding regions");
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read Ensembl IDs of given genes in table 'givenGenes'");
         List<String> ensemblIdsOfGivenGenes = readListOfStringsInSecondColumnOfTable(dataElementPath, "givenGenes");
         log.info("Read genes in table 'genes'");
         Map<String, List<Gene>> chromosomesAndGenes = readGenesInTable(dataElementPath, "genes");
         log.info("Remove all non-protein-coding genes");
         Gene.removeAllNonProteinCodingGenes(chromosomesAndGenes);
         log.info("Select the given genes");
         Map<String, List<Gene>> chromosomesAndGivenGenes = selectGivenGenes(ensemblIdsOfGivenGenes, chromosomesAndGenes);
         ListUtil.sortAll(chromosomesAndGivenGenes);
         log.info("Read binding regions in table 'bindingRegions'");
         Map<String, List<BindingRegion>> allBindingRegions = BindingRegion.readBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("All binding regions are sorting");
         ListUtil.sortAll(allBindingRegions);
         log.info("Select binding regions that located near given genes");
         int distanceThreshold = 20000;
         Map<String, List<BindingRegion>> selectedBindingRegions = selectBindingRegionsLocatedNearGivenGenes(chromosomesAndGivenGenes, allBindingRegions, distanceThreshold);
         log.info("Calculation of resulted table");
         Set<String> distinctTfClasses = BindingRegion.getDistinctTfClasses(selectedBindingRegions);
         log.info("Number of distinct tfClasses = " + distinctTfClasses.size());
         Map<String, List<int[]>> selectedGeneRegions = getSelectedGeneRegions(chromosomesAndGivenGenes, distanceThreshold);
         getTable_chiSquaredDependenceOfPairedTfClassesInGivenGenes(new ArrayList<>(distinctTfClasses), selectedBindingRegions, selectedGeneRegions, dataElementPath, "_chiSquaredDependenceOfPairedTfClassesInGivenGenes");
     }

     private List<Integer> getDistancesBetweenBindingRegionsOfGivenTfClass(String tfClass, Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<Gap>> chromosomeNameAndGaps)
     {
         List<Integer> result = new ArrayList<>();
         for( Map.Entry<String, List<BindingRegion>> entry : allBindingRegions.entrySet() )
         {
             List<Gap> gaps = chromosomeNameAndGaps.get(entry.getKey());
             List<BindingRegion> bindingRegions = entry.getValue();
             List<BindingRegion> selectedBindingRegions = selectBindingRegions(bindingRegions, tfClass).sorted().toList();
             int oldStartPosition = 0;
             for( BindingRegion br : selectedBindingRegions )
             {
                 int startPosition = br.getStartPositionCorrectedOnGaps(gaps);
                 int distance = startPosition - oldStartPosition;
                 if( distance < 0 )
                     distance = 0;
                 result.add(distance);
                 oldStartPosition = startPosition;
             }
         }
         return result;
     }

     private List<Integer> getDistancesBetweenBindingRegionsOfGivenTfClass(Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfSingleTfClass, Map<String, List<Gap>> chromosomeNameAndGaps)
     {
         List<Integer> result = new ArrayList<>();
         for( Map.Entry<String, List<BindingRegion>> entry : chromosomeAndBindingRegionsOfSingleTfClass.entrySet() )
         {
            List<Gap> gaps = chromosomeNameAndGaps.get(entry.getKey());
            List<BindingRegion> bindingRegions = entry.getValue();
            int oldStartPosition = 0;
            for( BindingRegion br : bindingRegions )
            {
                int startPosition = br.getStartPositionCorrectedOnGaps(gaps);
                int distance = startPosition - oldStartPosition;
                if( distance < 0 )
                    distance = 0;
                result.add(distance);
                oldStartPosition = startPosition;
            }
         }
         return result;
     }

     private void getTable_exponentialMixtureDistancesBetweenBindingRegionsOfSameTfClass(List<String> distinctTfClasses, Map<String, List<BindingRegion>> allBindingRegions, int numberOfMixtureComponents, int numberOfIntervals, int maximalNumberOfIterations, Map<String, List<Gap>> chromosomeNameAndGaps, DataElementPath dataElementPath, String nameOfTable) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), dataElementPath.getName() + nameOfTable);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
         for( int i = 1; i <= numberOfMixtureComponents; i++ )
         {
             table.getColumnModel().addColumn("meanValueInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("sizeOfMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("simulatedMeanValueInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("simulatedSigmaInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("p-valueOfExponentialityOfComponent_" + i, Double.class);
         }
         for( String tfClass : distinctTfClasses )
         {
             List<Integer> distancesBetweenBindingRegionsOfGivenTfClass = getDistancesBetweenBindingRegionsOfGivenTfClass(tfClass, allBindingRegions, chromosomeNameAndGaps);
             double[] mixtureInformation = Stat.getExponentialMixtureForSample(distancesBetweenBindingRegionsOfGivenTfClass, numberOfMixtureComponents, numberOfIntervals, maximalNumberOfIterations);
             Float[] row = new Float[mixtureInformation.length];
             for( int j = 0; j < mixtureInformation.length; j++ )
             {
                 float x = (float)mixtureInformation[j];
                 row[j] = x;
             }
             TableDataCollectionUtils.addRow(table, tfClass, row, true);
         }
         table.finalizeAddition();
         CollectionFactoryUtils.save(table);
     }

     private void getTable_exponentialMixtureDistancesBetweenBindingRegionsOfSameTfClass(Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions, int numberOfMixtureComponents, int numberOfIntervals, int maximalNumberOfIterations, Map<String, List<Gap>> chromosomeNameAndGaps, DataElementPath dataElementPath, String nameOfTable) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), dataElementPath.getName() + nameOfTable);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
         for( int i = 1; i <= numberOfMixtureComponents; i++ )
         {
             table.getColumnModel().addColumn("meanValueInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("sizeOfMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("simulatedMeanValueInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("simulatedSigmaInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("p-valueOfExponentialityOfComponent_" + i, Double.class);
         }
         for( Map.Entry<String, Map<String, List<BindingRegion>>> entry : tfClassAndChromosomeAndBindingRegions.entrySet() )
         {
             Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfSingleTfClass = entry.getValue();
             List<Integer> distancesBetweenBindingRegionsOfGivenTfClass = getDistancesBetweenBindingRegionsOfGivenTfClass(chromosomeAndBindingRegionsOfSingleTfClass, chromosomeNameAndGaps);
             double[] mixtureInformation = Stat.getExponentialMixtureForSample(distancesBetweenBindingRegionsOfGivenTfClass, numberOfMixtureComponents, numberOfIntervals, maximalNumberOfIterations);
             Float[] row = new Float[mixtureInformation.length];
             for( int j = 0; j < mixtureInformation.length; j++ )
             {
                 float x = (float)mixtureInformation[j];
                 row[j] = x;
             }
             TableDataCollectionUtils.addRow(table, entry.getKey(), row, true);
         }
         table.finalizeAddition();
         CollectionFactoryUtils.save(table);
     }
     
/*****
     private List<String> getDistinctTfClasses2(Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions)
     {
         List<String> result = new ArrayList<String>();
         Iterator<String> it = tfClassAndChromosomeAndBindingRegions.keySet().iterator();
         while( it.hasNext() )
         {
             String tfClass = it.next();
             result.add(tfClass);
         }
         return result;
     }
 *****/
     
     private void sortAllBindingRegions1(Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions)
     {
         for(Map<String, List<BindingRegion>> map: tfClassAndChromosomeAndBindingRegions.values())
         {
             ListUtil.sortAll(map);
         }
     }

     private void writeTablesForMode_10() throws Exception
     {
         log.info("Chi-squared test of exponentiality of mixture components for distances between binding regions of same tfClass");
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read binding regions in table 'bindingRegions'");
//       Map<String, List<BindingRegion>> allBindingRegions = readBindingRegionsInTable(dataElementPath, "bindingRegions");
         Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions  = readTfClassAndChromosomeAndBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("All binding regions are sorting");
//       sortAllBindingRegions(allBindingRegions);
         sortAllBindingRegions1(tfClassAndChromosomeAndBindingRegions);
//       log.info("Identification of distinct tfClasses");
//       List<String> distinctTfClasses = getDistinctTfClasses1(allBindingRegions);
//       List<String> distinctTfClasses = getDistinctTfClasses2(tfClassAndChromosomeAndBindingRegions);
//       log.info("Number of distinct tfClasses = " + distinctTfClasses.size());
         log.info("Read all gaps in table 'gaps'");
         Map<String, List<Gap>> chromosomeNameAndGaps = EnsemblUtils.readChromosomeNameAndGapsInTable(dataElementPath, "gaps");
         int maximalNumberOfIterations = 35;
         int numberOfIntervals = 20;
         int maximalNumberOfMixtureComponents = 5;
         for( int numberOfMixtureComponents = 1; numberOfMixtureComponents <= maximalNumberOfMixtureComponents; numberOfMixtureComponents++ )
         {
             log.info("Mixture with " + numberOfMixtureComponents + " components is analysed");
             String tableName = "_exponentialMixtureDistancesBetweenBindingRegionsOfSameTfClass_numberOfMixtureComponents_" + numberOfMixtureComponents;
//           getTable_exponentialMixtureDistancesBetweenBindingRegionsOfSameTfClass(distinctTfClasses, allBindingRegions, numberOfMixtureComponents, numberOfIntervals, maximalNumberOfIterations, chromosomeNameAndGaps, dataElementPath, tableName);
             getTable_exponentialMixtureDistancesBetweenBindingRegionsOfSameTfClass(tfClassAndChromosomeAndBindingRegions, numberOfMixtureComponents, numberOfIntervals, maximalNumberOfIterations, chromosomeNameAndGaps, dataElementPath, tableName);
             getJobControl().setPreparedness(numberOfMixtureComponents * 100 / maximalNumberOfMixtureComponents);
         }
     }
     
     private List<Integer> getDistancesBetweenBindingRegionsOfTwoTfClasses(Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass1, Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass2, Map<String, List<Gap>> chromosomeNameAndGaps)
     {
         List<Integer> result = new ArrayList<>();
         for( Map.Entry<String, List<BindingRegion>> entry : chromosomeAndBindingRegionsOfTfClass1.entrySet() )
         {
             String chromosome = entry.getKey();
             List<Gap> gaps = chromosomeNameAndGaps.get(chromosome);
             List<BindingRegion> bindingRegions1 = entry.getValue();
             List<BindingRegion> bindingRegions2 = chromosomeAndBindingRegionsOfTfClass2.get(chromosome);
             if( bindingRegions1 == null || bindingRegions2 == null || gaps == null )
                 continue;
             if( bindingRegions1.size() < 2 || bindingRegions2.size() < 2 )
                 continue;
             int iBindingRegions2 = 0;
             for( BindingRegion br1 : bindingRegions1 )
             {
                 int start1 = br1.getStartPositionCorrectedOnGaps(gaps);
                 int start2 = 0;
                 while( iBindingRegions2 < bindingRegions2.size() )
                 {
                     BindingRegion br2 = bindingRegions2.get(iBindingRegions2);
                     start2 = br2.getStartPositionCorrectedOnGaps(gaps);
                     if( start2 < start1 )
                         iBindingRegions2++;
                     else
                         break;
                 }
                 if( iBindingRegions2 >= bindingRegions2.size() )
                     break;
                 if( start2 >= start1 )
                     result.add(start2 - start1);
             }
         }
         return result;
     }

     private void getTable_exponentialMixtureDistancesBetweenBindingRegionsOfTwoTfClasses(Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions, int numberOfMixtureComponents, int numberOfIntervals, int maximalNumberOfIterations, Map<String, List<Gap>> chromosomeNameAndGaps, DataElementPath dataElementPath, String nameOfTable) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), dataElementPath.getName() + nameOfTable);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
         table.getColumnModel().addColumn("tfClass1", String.class);
         table.getColumnModel().addColumn("tfClass2", String.class);
         for( int i = 1; i <= numberOfMixtureComponents; i++ )
         {
             table.getColumnModel().addColumn("meanValueInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("sizeOfMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("simulatedMeanValueInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("simulatedSigmaInMixtureComponent_" + i, Double.class);
             table.getColumnModel().addColumn("p-valueOfExponentialityOfComponent_" + i, Double.class);
         }
         int iRow = 0;
         int iJobControl = 0;
         for( Map.Entry<String, Map<String, List<BindingRegion>>> entry1 : tfClassAndChromosomeAndBindingRegions.entrySet() )
         {
             String tfClass1 = entry1.getKey();
             Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass1 = entry1.getValue();
             for( Map.Entry<String, Map<String, List<BindingRegion>>> entry2 : tfClassAndChromosomeAndBindingRegions.entrySet() )
             {
                 String tfClass2 = entry2.getKey();
                 if( tfClass1.equals(tfClass2))
                     break;
                 log.info("tfClass1 = " + tfClass1 + " tfClass2 = " + tfClass2);
                 Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass2 = entry2.getValue();
                 List<Integer> distancesBetweenBindingRegionsOfTwoTfClasses = getDistancesBetweenBindingRegionsOfTwoTfClasses(chromosomeAndBindingRegionsOfTfClass1, chromosomeAndBindingRegionsOfTfClass2, chromosomeNameAndGaps);
                 log.info("distancesBetweenBindingRegionsOfTwoTfClasses.size() = " + distancesBetweenBindingRegionsOfTwoTfClasses.size());
                 double[] mixtureInformation = Stat.getExponentialMixtureForSample(distancesBetweenBindingRegionsOfTwoTfClasses, numberOfMixtureComponents, numberOfIntervals, maximalNumberOfIterations);
                 log.info("mixtureInformation:");
                 for( int j = 0; j < mixtureInformation.length; j++ )
                     log.info("mixtureInformation[" + j +"] = " + mixtureInformation[j]);
                 Object[] values = new Object[2 + 5 * numberOfMixtureComponents];
                 values[0] = tfClass1;
                 values[1] = tfClass2;
                 for( int j = 0; j < mixtureInformation.length; j++ )
                 {
                     float x = (float)mixtureInformation[j];
                     values[2 + j] = x;
                 }
                 TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values, true);
                 List<Integer> distancesBetweenBindingRegionsOfTwoTfClasses_ = getDistancesBetweenBindingRegionsOfTwoTfClasses(chromosomeAndBindingRegionsOfTfClass2, chromosomeAndBindingRegionsOfTfClass1, chromosomeNameAndGaps);
                 double[] mixtureInformation_ = Stat.getExponentialMixtureForSample(distancesBetweenBindingRegionsOfTwoTfClasses_, numberOfMixtureComponents, numberOfIntervals, maximalNumberOfIterations);
                 log.info("tfClass1 = " + tfClass2 + " tfClass2 = " + tfClass1);
                 log.info("mixtureInformation_:");
                 for( int j = 0; j < mixtureInformation_.length; j++ )
                     log.info("mixtureInformation_[" + j +"] = " + mixtureInformation_[j]);
                 Object[] values_ = new Object[2 + 5 * numberOfMixtureComponents];
                 values_[0] = tfClass2;
                 values_[1] = tfClass1;
                 for( int j = 0; j < mixtureInformation_.length; j++ )
                 {
                     float x = (float)mixtureInformation_[j];
                     values_[2 + j] = x;
                 }
                 TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values_, true);
             }
             getJobControl().setPreparedness((++iJobControl) * 100 / tfClassAndChromosomeAndBindingRegions.size());
         }
         table.finalizeAddition();
         CollectionFactoryUtils.save(table);
     }

     private void writeTablesForMode_11() throws Exception
     {
         log.info("Chi-squared test of exponentiality of mixture components for distances between binding regions of two distinct tfClasses");
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read binding regions in table 'bindingRegions'");
         Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions  = readTfClassAndChromosomeAndBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("All binding regions are sorting");
         sortAllBindingRegions1(tfClassAndChromosomeAndBindingRegions);
         log.info("Read all gaps in table 'gaps'");
         Map<String, List<Gap>> chromosomeNameAndGaps = EnsemblUtils.readChromosomeNameAndGapsInTable(dataElementPath, "gaps");
         int maximalNumberOfIterations = 35;
         int numberOfIntervals = 20;
         int maximalNumberOfMixtureComponents = 5;
         for( int numberOfMixtureComponents = 1; numberOfMixtureComponents <= maximalNumberOfMixtureComponents; numberOfMixtureComponents++ )
         {
             log.info("Mixture with " + numberOfMixtureComponents + " components is analysed");
             String tableName = "_exponentialMixtureDistancesBetweenBindingRegionsOfTwoTfClasses_numberOfMixtureComponents_" + numberOfMixtureComponents;
             getTable_exponentialMixtureDistancesBetweenBindingRegionsOfTwoTfClasses(tfClassAndChromosomeAndBindingRegions, numberOfMixtureComponents, numberOfIntervals, maximalNumberOfIterations, chromosomeNameAndGaps, dataElementPath, tableName);
             getJobControl().setPreparedness(numberOfMixtureComponents * 100 / maximalNumberOfMixtureComponents);
         }
     }

     private void getTable_exponentialMixtureDistancesBetweenBindingRegionsOfTwoGivenTfClasses(Map<Integer, int[]> valueInSabsampleAndObservedPredictedFrequencies, DataElementPath dataElementPath, String nameOfTable) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), dataElementPath.getName() + nameOfTable);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
         table.getColumnModel().addColumn("value", Integer.class);
         table.getColumnModel().addColumn("observedFrequency", Integer.class);
         table.getColumnModel().addColumn("predictedFrequency", Integer.class);
         int iRow = 0;
         for( Map.Entry<Integer, int[]> entry : valueInSabsampleAndObservedPredictedFrequencies.entrySet() )
         {
             int[] frequencies = entry.getValue();
             Integer i = frequencies[0];
             Integer ii = frequencies[1];
             TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[]{entry.getKey(), i, ii}, true);
         }
         table.finalizeAddition();
         CollectionFactoryUtils.save(table);
     }

     private void writeTablesForMode_12() throws Exception
     {
         log.info("Graph for distances between binding regions of two distinct tfClasses");
         String tfClass1 = "3.5.3.0.4";
         String tfClass2 = "3.5.2.5.1";
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read binding regions in table 'bindingRegions'");
         Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions  = readTfClassAndChromosomeAndBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("All binding regions are sorting");
         sortAllBindingRegions1(tfClassAndChromosomeAndBindingRegions);
         log.info("Read all gaps in table 'gaps'");
         Map<String, List<Gap>> chromosomeNameAndGaps = EnsemblUtils.readChromosomeNameAndGapsInTable(dataElementPath, "gaps");
         int maximalNumberOfIterations = 35;
         int numberOfIntervals = 20;
         int numberOfMixtureComponents = 5;
         Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass1 = tfClassAndChromosomeAndBindingRegions.get(tfClass1);
         Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass2 = tfClassAndChromosomeAndBindingRegions.get(tfClass2);
         List<Integer> distancesBetweenBindingRegionsOfTwoTfClasses = getDistancesBetweenBindingRegionsOfTwoTfClasses(chromosomeAndBindingRegionsOfTfClass1, chromosomeAndBindingRegionsOfTfClass2, chromosomeNameAndGaps);
         Map<Integer, Map<Integer, int[]>> indexOfSubsampleAndValueInSabsampleAndObservedPredictedFrequencies = Stat.getObservedAndPredictedDensitiesOfExponentialMixture(distancesBetweenBindingRegionsOfTwoTfClasses, numberOfMixtureComponents, numberOfIntervals, maximalNumberOfIterations);
         for( Map.Entry<Integer, Map<Integer, int[]>> entry : indexOfSubsampleAndValueInSabsampleAndObservedPredictedFrequencies.entrySet() )
         {
             Map<Integer, int[]> valueInSubsampleAndObservedPredictedFrequencies = entry.getValue();
             String tableName = "_exponentialMixtureDistancesBetweenBindingRegionsOfTwoGivenTfClasses_" + entry.getKey();
             getTable_exponentialMixtureDistancesBetweenBindingRegionsOfTwoGivenTfClasses(valueInSubsampleAndObservedPredictedFrequencies, dataElementPath, tableName);
         }
     }

// from class Stat for test
     /***
      * Initial sample is divided step by step into 2 subsamples.
      * 1-st subsample (exponentialSubsample) represent the sample that has exponential distribution.
      * 2-nd subsample (outliers) is the residual part of initial sample.
      * Exponentiality is estimated by Kolmogorov-Smirnov test for exponentiality
      * @param sample
      * @param pValue
      * @return
      */
     private Map<String, List<Integer>> getKolmogorovSmirnovExponentialSubsample(List<Integer> sample, double pValue)
     {
         Map<String, List<Integer>> result = new HashMap<>();
         List<Integer> exponentialSubsample = new ArrayList<>();
         List<Integer> outliers = new ArrayList<>();
         for ( Integer x : sample )
             exponentialSubsample.add(x);
         result.put("exponentialSubsample", exponentialSubsample);
         result.put("outliers", outliers);
         while ( exponentialSubsample.size() >= 3 )
         {
             Number[] statisticAndValueOfMaximaldevirgence = Stat.calcKolmogorovSmirnovExponentialStatistic1(exponentialSubsample);
             if( statisticAndValueOfMaximaldevirgence == null )
                 return result;
             Double x = (Double)statisticAndValueOfMaximaldevirgence[0];
             double statistic = x;
             double pvalue = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(statistic, 25, exponentialSubsample.size());
             if( pvalue > pValue )
                 return result;
             Integer valueOfMaximaldevirgence = (Integer)statisticAndValueOfMaximaldevirgence[1];

//test
log.info("valueOfMaximaldevirgence = " + valueOfMaximaldevirgence + " statistic = " + statistic + " pvalue = " + pvalue + " exponentialSubsampleSize = " + exponentialSubsample.size());
//test
             
             exponentialSubsample.remove(valueOfMaximaldevirgence);
             outliers.add(valueOfMaximaldevirgence);
             result.put("exponentialSubsample", exponentialSubsample);
             result.put("outliers", outliers);
         }
         return result;
     }

     
     
     
     
     //temporary
     private void RAB_writeTablesForMode_13() throws Exception
     {
         log.info("Mixtire of exponentials for distances between binding regions of two distinct tfClasses (or one tfClass)");
         String tfClass1 = "3.5.3.0.4";
         String tfClass2 = "3.5.3.0.4";
//         String tfClass1 = "3.5.3.0.4";
//         String tfClass2 = "2.3.1.1.1";
//         String tfClass2 = "2.1.2.4.1";
//         String tfClass2 = "1.2.1.0.3";
//         String tfClass2 = "3.5.2.5.1";
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read binding regions in table 'bindingRegions'");
         Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions  = readTfClassAndChromosomeAndBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("All binding regions are sorting");
         sortAllBindingRegions1(tfClassAndChromosomeAndBindingRegions);
         log.info("Read all gaps in table 'gaps'");
         Map<String, List<Gap>> chromosomeNameAndGaps = EnsemblUtils.readChromosomeNameAndGapsInTable(dataElementPath, "gaps");
         int maximalNumberOfIterations = 35;
         int maximalNumberOfMixtureComponents = 5;
         double pValue = 0.0501;
         List<Integer> distancesBetweenBindingRegionsOfTwoTfClasses = null;
         if( ! tfClass1.equals(tfClass2) )
         {
             Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass1 = tfClassAndChromosomeAndBindingRegions.get(tfClass1);
             Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass2 = tfClassAndChromosomeAndBindingRegions.get(tfClass2);
             distancesBetweenBindingRegionsOfTwoTfClasses = getDistancesBetweenBindingRegionsOfTwoTfClasses(chromosomeAndBindingRegionsOfTfClass1, chromosomeAndBindingRegionsOfTfClass2, chromosomeNameAndGaps);
         }
         else
         {
             Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass1 = tfClassAndChromosomeAndBindingRegions.get(tfClass1);
             distancesBetweenBindingRegionsOfTwoTfClasses = getDistancesBetweenBindingRegionsOfGivenTfClass(chromosomeAndBindingRegionsOfTfClass1, chromosomeNameAndGaps);
         }
log.info(distancesBetweenBindingRegionsOfTwoTfClasses.size() + " distances between tfClass1 = " + tfClass1 + " and tfClass2 = " + tfClass2 + " are calculated");

         
//         for( int numberOfMixtureComponents = 1; numberOfMixtureComponents <= maximalNumberOfMixtureComponents; numberOfMixtureComponents++ )
         for( int numberOfMixtureComponents = maximalNumberOfMixtureComponents; numberOfMixtureComponents <= maximalNumberOfMixtureComponents; numberOfMixtureComponents++ )
         {
             log.info("Mixture with " + numberOfMixtureComponents + " components is analysed");
             double[] exponentialParameters = new double[numberOfMixtureComponents];
             double[] probabilitiesOfMixtureComponents = new double[numberOfMixtureComponents];
             int max = -1;
             for( Integer i: distancesBetweenBindingRegionsOfTwoTfClasses )
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
             double[][] probabilitiesPij = Stat.estimateExponentialMixtureBy_EM_Algorithm(exponentialParameters, probabilitiesOfMixtureComponents, distancesBetweenBindingRegionsOfTwoTfClasses, numberOfIterations);
log.info("Probabilities Pij are calculated");
             List<List<Integer>> indexOfSubsampleAndSubsample = Stat.getSubsamplesSimulatedByProbabilitiesPij(probabilitiesPij, distancesBetweenBindingRegionsOfTwoTfClasses);
log.info("Mixture components are identified");
             for( int i = 0; i<indexOfSubsampleAndSubsample.size(); i++ )
             {
                 List<Integer> subSample = indexOfSubsampleAndSubsample.get( i );
                 Map<String, List<Integer>> typeOfSampleAndSample = getKolmogorovSmirnovExponentialSubsample(subSample, pValue);
                 double[] meanAndSigma = Stat.getMeanAndSigma(subSample);
                 log.info("numberOfMixtureComponents = " + numberOfMixtureComponents + " indexOfSubsample = " + i + " sizeOfSubsample = " + subSample.size() + " mean = " + meanAndSigma[0] + " sigma = " + meanAndSigma[1]);
                 List<Integer> exponentialSubsample = typeOfSampleAndSample.get("exponentialSubsample");
                 List<Integer> outliers = typeOfSampleAndSample.get("outliers");
                 double[] meanAndSigma1 = Stat.getMeanAndSigma(exponentialSubsample);
                 double[] meanAndSigma2 = Stat.getMeanAndSigma(outliers);
                 log.info("exponentialSubsample : size = " + exponentialSubsample.size() + " mean = " + meanAndSigma1[0] + " sigma = " + meanAndSigma1[1]);
                 log.info("outliers : size = " + outliers.size() + " mean = " + meanAndSigma2[0] + " sigma = " + meanAndSigma2[1]);
             }
             getJobControl().setPreparedness(numberOfMixtureComponents * 100 / maximalNumberOfMixtureComponents);
         }
     }
     
     private void writeTablesForMode_14() throws Exception
     {
         log.info("Mixtire of exponential and normal distributions for distances between binding regions of two tfClasses");
         String tfClass1 = "3.5.3.0.4";
         String tfClass2 = "3.5.3.0.4";
//         String tfClass2 = "2.3.1.1.1";
//         String tfClass2 = "2.1.2.4.1";
//         String tfClass2 = "1.2.1.0.3";
//         String tfClass2 = "3.5.2.5.1";
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read binding regions in table 'bindingRegions'");
         Map<String, Map<String, List<BindingRegion>>> tfClassAndChromosomeAndBindingRegions  = readTfClassAndChromosomeAndBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("All binding regions are sorting");
         sortAllBindingRegions1(tfClassAndChromosomeAndBindingRegions);
         log.info("Read all gaps in table 'gaps'");
         Map<String, List<Gap>> chromosomeNameAndGaps = EnsemblUtils.readChromosomeNameAndGapsInTable(dataElementPath, "gaps");
         int maximalNumberOfIterations = 35;
         List<Integer> distancesBetweenBindingRegionsOfTwoTfClasses = null;
         if( ! tfClass1.equals(tfClass2) )
         {
             Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass1 = tfClassAndChromosomeAndBindingRegions.get(tfClass1);
             Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass2 = tfClassAndChromosomeAndBindingRegions.get(tfClass2);
             distancesBetweenBindingRegionsOfTwoTfClasses = getDistancesBetweenBindingRegionsOfTwoTfClasses(chromosomeAndBindingRegionsOfTfClass1, chromosomeAndBindingRegionsOfTfClass2, chromosomeNameAndGaps);
         }
         else
         {
             Map<String, List<BindingRegion>> chromosomeAndBindingRegionsOfTfClass1 = tfClassAndChromosomeAndBindingRegions.get(tfClass1);
             distancesBetweenBindingRegionsOfTwoTfClasses = getDistancesBetweenBindingRegionsOfGivenTfClass(chromosomeAndBindingRegionsOfTfClass1, chromosomeNameAndGaps);
         }
         log.info(distancesBetweenBindingRegionsOfTwoTfClasses.size() + " distances between tfClass1 = " + tfClass1 + " and tfClass2 = " + tfClass2 + " are calculated");
         double[] exponentialParameters = {1.0 / 14996.0, 1.0 / 36580.0};
         double[][] meansAndSigmas = new double[3][2];
         meansAndSigmas[0][0] = 150.0;
         meansAndSigmas[0][1] = 50.0;
         meansAndSigmas[1][0] = 3800.0;
         meansAndSigmas[1][1] = 1000.0;
         meansAndSigmas[2][0] = 7500.0;
         meansAndSigmas[2][1] = 2000.0;
         double[] probabilitiesOfMixtureComponents = {0.2, 0.2, 0.2, 0.2, 0.2};
         Object[] objects = Stat.estimateExponentialNormalMixtureBy_EM_Algorithm(exponentialParameters, meansAndSigmas, probabilitiesOfMixtureComponents, distancesBetweenBindingRegionsOfTwoTfClasses, maximalNumberOfIterations);
log.info("Mixture: O.K.");
         double[] newExponentialParameters = (double[])objects[0];
         double[][] newMeansAndSigmas = (double[][])objects[1];
         double[] newProbabilitiesOfMixtureComponents = (double[])objects[2];
         Integer integer = (Integer)objects[3];
         int numberOfIterations = integer;
         double[][] probabilitiesPij = (double[][])objects[4];
         int k1 = exponentialParameters.length;
         int k2 = meansAndSigmas.length;
         for( int i = 0; i < k1; i++ )
             log.info("type of mixture component = exponential; probabilities of component = " + probabilitiesOfMixtureComponents[i] + ", " + newProbabilitiesOfMixtureComponents[i] + " initial mean distance = " + 1.0 / exponentialParameters[i] + " estimated mean distance = " + 1.0 / newExponentialParameters[i]);
         for( int i = 0; i < k2; i++ )
             log.info("type of mixture component = normal; probabilities of component = " + probabilitiesOfMixtureComponents[k1 + i] + ", " + newProbabilitiesOfMixtureComponents[k1 + i] + " initial meansAndSigmas = " + meansAndSigmas[i][0] + ", " + meansAndSigmas[i][1] + " estimated meansAndSigmas = " + newMeansAndSigmas[i][0] + ", " + newMeansAndSigmas[i][1]);
     }

     private List<CisModule> getCisModule2_2InOneChromosome(List<BindingRegion> bindingRegions, List<Integer> positions, int minimalNumberOfTfClasses, int distanceThreshold)
     {
         List<CisModule> result = new ArrayList<>();
         for( Integer position : positions )
         {
             List<String> tfClasses = new ArrayList<>();
             for( BindingRegion br : bindingRegions )
             {
                 String tfClass = br.getTfClass();
                 if( tfClasses.contains(tfClass) ) continue;
                 int start = br.getStartPosition();
                 int end = br.getFinishPosition();
                 int distance;
                 if( areOverlappedTwoSites(start, end, position, position) )
                     distance = 0;
                 else
                     if( position < start )
                         distance = start - position;
                     else
                         distance = position - end;
                 if( distance <= distanceThreshold )
                     tfClasses.add(tfClass);
             }
             if( tfClasses.size() >= minimalNumberOfTfClasses )
                 result.add(new CisModule(position, position, tfClasses));
         }
         return result;
     }
     
     private void writeTablesWithCisModules2_2WithGivenPatterns(Map<String, List<BindingRegion>> allBindingRegions, Map<String, List<String>> patternIdsAndCisModulePatterns, int distanceThreshold, DataElementPath dataElementPath) throws Exception
     {
         for( Map.Entry<String, List<String>> patternEntry : patternIdsAndCisModulePatterns.entrySet() )
         {
             List<String> selectedTfClasses = patternEntry.getValue();
             Map<String, List<BindingRegion>> selectedBindingRegions = selectBindingRegions(allBindingRegions, selectedTfClasses);
             ListUtil.sortAll(selectedBindingRegions);
             int minimalNumberOfOverlaps = 1;
             if( selectedTfClasses.size() > 1 )
                 minimalNumberOfOverlaps = selectedTfClasses.size() / 2;
             Map<String, List<CisModule>> chromosomeAndCisModules2_2 = new HashMap<>();
             Map<String, List<CisModule>> chromosomeAndCisModules2 = getCisModules2WithGivenPattern(selectedBindingRegions, selectedTfClasses, minimalNumberOfOverlaps);
             int iJobControl = 0;
             for( Map.Entry<String, List<CisModule>> entry2 : chromosomeAndCisModules2.entrySet() )
             {
                 String chromosome = entry2.getKey();
                 List<CisModule> cisModules = entry2.getValue();
                 List<Integer> positions = new ArrayList<>();
                 for( CisModule cisModule : cisModules )
                     positions.add((cisModule.getStartPosition() + cisModule.getFinishPosition()) / 2);
                 Collections.sort(positions);
                 List<BindingRegion> bindingRegions = selectedBindingRegions.get(chromosome);
                 List<CisModule> cisModules2_2 = getCisModule2_2InOneChromosome(bindingRegions, positions, selectedTfClasses.size(), distanceThreshold);
                 if( cisModules2_2.size() > 0 )
                     chromosomeAndCisModules2_2.put(chromosome, cisModules2_2);
                 log.info("chromosome = " + chromosome + " number of initial cisModules = " + cisModules.size() + " number of final cisModules = " + cisModules2_2);
                 getJobControl().setPreparedness(++iJobControl * 100 / chromosomeAndCisModules2.size());
             }
             writeCisModules2IntoTable(chromosomeAndCisModules2_2, dataElementPath, dataElementPath.getName() + "_cisModules2_2_with_pattern_" + patternEntry.getKey());
         }
     }

     private void writeTablesForMode_15() throws Exception
     {
         log.info("Identification of cis-modules2_2 with given patterns of tfClasses");
         log.info("cis-modules2_2: initially  weak cis-modules2 are identified");
         log.info("then distances instead of overlapping are used for creation of cis-modules2_2");
         int distanceThreshold = 500;
         log.info("distanceThreshold = " + distanceThreshold);
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read binding regions in table 'bindingRegions'");
         Map<String, List<BindingRegion>> allBindingRegions = BindingRegion.readBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("Read given patterns of cisModules2 in table 'cisModulePatterns'");
         Map<String, List<String>> patternIdsAndCisModulePatterns = readPatternIdsAndCisModulePatternsInTable(dataElementPath, "cisModulePatterns");
         log.info("Identify cis-modules2_2 and write them into tables");
         writeTablesWithCisModules2_2WithGivenPatterns(allBindingRegions, patternIdsAndCisModulePatterns, distanceThreshold, dataElementPath);
     }

     private List<CisModule3> getCisModules3(List<BindingRegion> bindingRegions, List<String> pivotalTfClasses, int minimalNumberOfPivotalTfClasses, int distanceThreshold)
     {
         List<CisModule3> result = new ArrayList<>();
         List<BindingRegion> selectedBindingRegions = BindingRegion.selectBindingRegions(bindingRegions, pivotalTfClasses);
         if( selectedBindingRegions.size() < minimalNumberOfPivotalTfClasses / 2) return null;
         Collections.sort(selectedBindingRegions);
         List<CisModule> cisModules2 = CisModule.getCisModules2InOneChromosome(selectedBindingRegions, minimalNumberOfPivotalTfClasses / 2);
         List<Integer> positions = new ArrayList<>();
         for( CisModule cisModule : cisModules2 )
             positions.add((cisModule.getStartPosition() + cisModule.getFinishPosition()) / 2);
         Collections.sort(positions);
         List<CisModule> cisModules2_2 = getCisModule2_2InOneChromosome(bindingRegions, positions, minimalNumberOfPivotalTfClasses, distanceThreshold);
         for( CisModule cisModule : cisModules2_2 )
         {
             List<String> pivotalList = new ArrayList<>();
             List<String> additionalList = new ArrayList<>();
             List<String> totalList = cisModule.getTfClasses();
             for( String tfClass : totalList )
                 if( pivotalTfClasses.contains(tfClass) )
                     pivotalList.add(tfClass);
                 else
                     additionalList.add(tfClass);
             if( pivotalList.size() >= minimalNumberOfPivotalTfClasses )
                 result.add(new CisModule3(cisModule.getMiddlePosition(), pivotalList, additionalList));
         }
         return result;
     }

     private Map<String, List<CisModule3>> getCisModules3(Map<String, List<BindingRegion>> allBindingRegions, List<String> pivotalTfClasses, int minimalNumberOfPivotalTfClasses, int distanceThreshold)
     {
         Map<String, List<CisModule3>> result = new HashMap<>();
         int iJobControl = 0;
         for( Map.Entry<String, List<BindingRegion>> entry : allBindingRegions.entrySet() )
         {
             String chromosome = entry.getKey();
             List<BindingRegion> bindingRegions = entry.getValue();
             if( bindingRegions == null || bindingRegions.size() < minimalNumberOfPivotalTfClasses ) continue;
             List<CisModule3> cisModules3 = getCisModules3(bindingRegions, pivotalTfClasses, minimalNumberOfPivotalTfClasses, distanceThreshold);
             if( ! cisModules3.isEmpty() )
                 result.put(chromosome, cisModules3);
             getJobControl().setPreparedness(++iJobControl * 100 / allBindingRegions.size());
             log.info(cisModules3.size() + " cisModules3 were identified in chromosome = " + chromosome + " !!!");
         }
         return result;
     }
     
     private List<SiteModel> getSiteModels(DataElementPath siteModelPath)
     {
         return siteModelPath.getChildren().elements( SiteModel.class ).collect( Collectors.toList() );
     }

     private void writeIpsScoresForCisModules3IntoTable(Map<String, List<CisModule3>> allCisModules3, DataElementPath dataElementPath, String nameOfTable, List<SiteModel> siteModels, int regionLength, DataElementPath pathToSequences) throws Exception
     {
         DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), nameOfTable);
         TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
         table.getColumnModel().addColumn("chromosome", String.class);
         table.getColumnModel().addColumn("position1", Integer.class);
         table.getColumnModel().addColumn("position2", Integer.class);
         for( SiteModel siteModel : siteModels )
         {
             String modelName = siteModel.getName();
             table.getColumnModel().addColumn("ipsScoreFor_" + modelName, Double.class);
         }
         int iRow = 0;
         int iJobControl = 0;
         List<CisModule3> cisModules0 = allCisModules3.get("X");
         if( cisModules0 != null )
         for( CisModule3 cisModule : cisModules0 )
         {
             Object[] values = new Object[3 + siteModels.size()];
             values[0] = "X";
             String chromosome = (String)values[0];
             Integer position = cisModule.getPosition();
             int start = position - regionLength / 2;
             Sequence sequenceRegion = EnsemblUtils.getSequenceRegion(chromosome, start, regionLength, pathToSequences);
             
log.info("chromosome = " + (String)values[0] + " position = " + position);

             values[1] = position - 1;
             values[2] = position + 1;
             int i = 2;
             for( SiteModel siteModel : siteModels )
             {
                 IPSSiteModel ipsSiteModel = (IPSSiteModel)siteModel;
                 values[++i] = IPSPrediction.getMaximalIpsScore(sequenceRegion, ipsSiteModel);
                 
log.info("chromosome = " + (String)values[0] + " position = " + position + " siteModel = " + siteModel.getName() + " IPSscore = " + values[i]);

             }
             TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values, true);
         }
         getJobControl().setPreparedness(++iJobControl * 100 / allCisModules3.size());
         for( Map.Entry<String, List<CisModule3>> entry : allCisModules3.entrySet() )
         {
             String chromosome = entry.getKey();
             if( chromosome.equals("X") ) continue;
             List<CisModule3> cisModules = entry.getValue();
             for( CisModule3 cisModule : cisModules )
             {
                 Object[] values = new Object[3 + siteModels.size()];
                 values[0] = chromosome;
                 Integer position = cisModule.getPosition();
                 int start = position - regionLength / 2;
                 Sequence sequenceRegion = EnsemblUtils.getSequenceRegion(chromosome, start, regionLength, pathToSequences);
                 Sequence reverseSequenceRegion = SequenceRegion.getReversedSequence(sequenceRegion);
log.info("chromosome = " + (String)values[0] + " position = " + position);
                 values[1] = position - 1;
                 values[2] = position + 1;
                 int i = 2;
                 for( SiteModel siteModel : siteModels )
                 {
                     IPSSiteModel ipsSiteModel = (IPSSiteModel)siteModel;
                     values[++i] = IPSPrediction.getMaximalIpsScore(sequenceRegion, ipsSiteModel);
log.info("chromosome = " + (String)values[0] + " position = " + position + " siteModel = " + siteModel.getName() + " IPSscore = " + values[i]);
                 }
                 TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values, true);
             }
             getJobControl().setPreparedness(++iJobControl * 100 / allCisModules3.size());
         }
         table.finalizeAddition();
         CollectionFactoryUtils.save(table);
      }
     
     private void writeTablesForMode_16() throws Exception
     {
         int distanceThreshold = 500;
         log.info("Identification of cis-modules3 near given genes");
         DataElementPath dataElementPath = parameters.getCisRegModuleTable();
         log.info("Read Ensembl IDs of given genes in table 'givenGenes'");
         List<String> ensemblIdsOfGivenGenes = readListOfStringsInSecondColumnOfTable(dataElementPath, "givenGenes");
         log.info("Read genes in table 'genes'");
         Map<String, List<Gene>> chromosomesAndGenes = readGenesInTable(dataElementPath, "genes");
         log.info("Remove all non-protein-coding genes");
         Gene.removeAllNonProteinCodingGenes(chromosomesAndGenes);
         log.info("Select the given genes");
         Map<String, List<Gene>> chromosomesAndGivenGenes = selectGivenGenes(ensemblIdsOfGivenGenes, chromosomesAndGenes);
         ListUtil.sortAll(chromosomesAndGivenGenes);
         log.info("Read binding regions in table 'bindingRegions'");
         Map<String, List<BindingRegion>> allBindingRegions = BindingRegion.readBindingRegionsInTable(dataElementPath, "bindingRegions");
         log.info("All binding regions are sorting");
         ListUtil.sortAll(allBindingRegions);
         log.info("Select binding regions that located near given genes");
         int distanceBetweenGenesAndBindingRegionsThreshold = 20000;
         Map<String, List<BindingRegion>> allBindingRegionsNearGivenGenes = selectBindingRegionsLocatedNearGivenGenes1(chromosomesAndGivenGenes, allBindingRegions, distanceBetweenGenesAndBindingRegionsThreshold);
         log.info("Read pivotal pattern of cisModules3 in table 'cisModulePatterns'");
         Map<String, List<String>> patternIdsAndCisModulePatterns = readPatternIdsAndCisModulePatternsInTable(dataElementPath, "cisModulePatterns");
         log.info("Calculation of cis-modules3");
         List<String> pivotalTfClasses = patternIdsAndCisModulePatterns.values().iterator().next();
         int minimalNumberOfPivotalTfClasses = parameters.getMinimalNumberOfOverlaps();
         Map<String, List<CisModule3>> allCisModules3 = getCisModules3(allBindingRegionsNearGivenGenes, pivotalTfClasses, minimalNumberOfPivotalTfClasses, distanceThreshold);
         writeCisModules3IntoTable(allCisModules3, dataElementPath, dataElementPath.getName() + "_cisModules3NearGivenGenes");
         log.info("Calculation of maximal IPss for identified cis-modules3");
         int regionLength = 400;
         DataElementPath siteModelPath = DataElementPath.create("databases/GTRD/Data/site models/moderate threshold");
         DataElementPath pathToSequences = parameters.getSequencePath();
         List<SiteModel> siteModels = getSiteModels(siteModelPath);
         writeIpsScoresForCisModules3IntoTable(allCisModules3, dataElementPath, dataElementPath.getName() + "_ipsScoresInCisModules3NearGivenGenes", siteModels, regionLength, pathToSequences);
     }

     private void writeTablesForMode_22_preGTRD_3() throws Exception
     {
         log.info("--- Analysis of lengths of binding regions ---");
         DataElementPath pathToBindingRegionsTracks = parameters.getChipSeqPeaksPath();
         DataCollection<Track> tracks = pathToBindingRegionsTracks.getDataCollection(Track.class);
         int iJobControl = 0;
         for(Track track: tracks)
         {
             log.info("Read binding regions in sql track");
             TrackInfo trackInfo = new TrackInfo(track);
             String cellLine = trackInfo.getCellLine();
             cellLine = cellLine.replaceAll("/","|");
             Map<String, List<BindingRegion>> allBindingRegions = BindingRegion.readBindingRegionsFromTrack(track);
             Set<String> distinctTfClasses = BindingRegion.getDistinctTfClasses(allBindingRegions);
             for( String tfClass : distinctTfClasses )
             {
                 List<Integer> lengths = BindingRegion.getLengthsOfBindingRegionsOfGivenTfClass(allBindingRegions, tfClass);
                 double statistic = Stat.calcKolmogorovSmirnovExponentialStatistic(lengths);
                 double pvalue = 1.0 - Stat.kolmogorovSmirnovDistributionFunction(statistic, 25, lengths.size());
                 double meanAndSigma[] = Stat.getMeanAndSigma(lengths);
                 log.info("tfClass = " + tfClass + " cellLine" + cellLine + " n = " + lengths.size() + " statistic = " + statistic + " pvalue = " + pvalue + " mean = " + meanAndSigma[0] + " sigma = " + meanAndSigma[1]);
                 Map<String, List<Integer>> twoSubsamples = getKolmogorovSmirnovExponentialSubsample(lengths, 0.05);
                 List<Integer> subsample = twoSubsamples.get("exponentialSubsample");
                 double meanAndSigma1[] = Stat.getMeanAndSigma(subsample);
                 int n1 = subsample.size();
                 subsample = twoSubsamples.get("outliers");
                 double meanAndSigma2[] = Stat.getMeanAndSigma(subsample);
                 int n2 = subsample.size();
                 log.info(" n1 = " + n1 + " mean1 = " + meanAndSigma1[0] + " sigma1 = " + meanAndSigma1[1] + " n2 = " + n2 + " mean2 = " + meanAndSigma2[0] + " sigma2 = " + meanAndSigma1[2]);
             }
             getJobControl().setPreparedness(++iJobControl * 100 / tracks.getSize());
         }
     }

/**********************/
    
    @Override
    public TableDataCollection[] justAnalyzeAndPut() throws Exception
    {
        log.info("*****************************************************");
        log.info("MODE = 8  <=> Analysis of dependence of paired tfClasses on the base of chi-squared test of independence of paired tfClasses and using all binding regions.");
        log.info("              The following operations will be done");
        log.info("              8.1.   All available paires of distinct tfClasses will be analysed");
        log.info("                     and table with extension '_chiSquaredDependenceOfPairedTfClasses' will be created");
        log.info("*****************************************************");
        log.info("MODE = 9  <=> Analysis of dependence of paired tfClasses in set of given genes on the base of chi-squared test of independence of paired tfClasses and using all binding regions.");
        log.info("              The following operations will be done");
        log.info("              9.1.   All available paires of distinct tfClasses in set of given genes will be analysed");
        log.info("                     and table with extension '_chiSquaredDependenceOfPairedTfClassesInGivenGenes' will be created");
        log.info("*****************************************************");
        log.info("MODE = 10  <=> Analysis of distances between binding regions of the same tfClass.");
        log.info("              The following operations will be done");
        log.info("              10.1.  Exponential mixture for distances between binding regions of same tfClass will be analysed");
        log.info("                     and tables with extensions '_exponentialMixtureDistancesBetweenBindingRegionsOfTwoTfClasses_numberOfMixtureComponents_' will be created");
        log.info("*****************************************************");
        log.info("MODE = 11  <=> Analysis of distances between binding regions of two distinct tfClasses.");
        log.info("              The following operations will be done");
        log.info("              11.1.  Exponential mixture for distances between binding regions of two distinct tfClasses will be analysed by Chi-squared test of exponentiality of mixture components");
        log.info("                     and tables with extensions '_exponentialMixtureDistancesBetweenBindingRegionsOfSameTfClass_numberOfMixtureComponents_' will be created");
        log.info("*****************************************************");

        int mode = parameters.getMode();
        switch( mode )
        {
            case 8 : writeTablesForMode_8(); return null;
            case 9 : writeTablesForMode_9(); return null;
            case 10 : writeTablesForMode_10(); return null;
            case 11 : writeTablesForMode_11(); return null;
            case 12 : writeTablesForMode_12(); return null;
            case 13 : RAB_writeTablesForMode_13(); return null;
            case 14 : writeTablesForMode_14(); return null;
            case 15 : writeTablesForMode_15(); return null;
            case 16 : writeTablesForMode_16(); return null;
            case 22 : writeTablesForMode_22_preGTRD_3(); return null;
            default : return null;
        }

        
//       Jama.Matrix
//        getFriensOfPivotalTfClasess(distinctTfClasses, allCisModules2);

        
/***********************************

//2-nd table: KolmogorovSmirnovUniformityPvalues
        log.info("Identification of gaps in chromosomes");
        Map<String, Integer> chromosomeNameAndCorrectedLength = getChromosomeNameAndCorrectedLength(chromosomeNameAndLength, chromosomeNameAndGaps);
        TableDataCollection table2_KolmogorovSmirnovUniformityPvalues = getTable_KolmogorovSmirnovUniformityPvalues(chromosomeNameAndCorrectedLength, allBindingRegions, chromosomeNameAndGaps, distinctTfClasses);
        table2_KolmogorovSmirnovUniformityPvalues.getOrigin().put(table2_KolmogorovSmirnovUniformityPvalues);

//5-th table: KolmogorovSmirnovExponentialityPvalues
        TableDataCollection  table5_KolmogorovSmirnovExponentialityPvalues = getTable_KolmogorovSmirnovExponentialityPvalues(chromosomeNameAndLength, allBindingRegions, chromosomeNameAndGaps, distinctTfClasses);
        table5_KolmogorovSmirnovExponentialityPvalues.getOrigin().put(table5_KolmogorovSmirnovExponentialityPvalues);

//7-th table: summary on genes overlapped with cis-regulatory modules
        log.info("Identification of genes overlapped with cis-regulatory modules is started");
        Map<String, Integer> overlappedGenesTypesAndCounts = toCountGenesOverlappedWithCisModules(allCisModules, chromosomesAndGenes, distinctGeneTypes);
        TableDataCollection table7_summaryOnGenesOverlappedWithCisModules = getTable_summaryOnGenesOverlappedWithCisModules(overlappedGenesTypesAndCounts, GeneTypesAndCounts);
        table7_summaryOnGenesOverlappedWithCisModules.getOrigin().put(table7_summaryOnGenesOverlappedWithCisModules);

//8-th table: summary on cis-regulatory modules overlapped with genes
        log.info("Classification of cis-regulatory modules with respect to their locations in genes");
        log.info("Selection of protein_coding genes");
        removeAllNonProteinCodingGenes(chromosomesAndGenes);
        Map<String, List<CisModule>> cisModuleTypesAndCisModules = getClassificationOfCisModules(allCisModules, chromosomesAndGenes);
        Map<String, Integer> cisModuleTypesAndTheirCounts = toCountTypesOfCisModules(cisModuleTypesAndCisModules);
        TableDataCollection table8_summaryOnCisModulesOverlappedWithGenes = getTable_summaryOnCisModulesOverlappedWithGenes(cisModuleTypesAndTheirCounts);
        table8_summaryOnCisModulesOverlappedWithGenes.getOrigin().put(table8_summaryOnCisModulesOverlappedWithGenes);

//9-th table: tfClasses in cis-regulatory modules of different types
        Map<String, double[]> cisModuleTypesAndMeansSigmas = getMeansAndSigmasForCisModules(cisModuleTypesAndCisModules);
        TableDataCollection table9_tfClassesInCisModulesOfDifferentTypes = getTable_tfClassesInCisModulesOfDifferentTypes(cisModuleTypesAndMeansSigmas);
        table9_tfClassesInCisModulesOfDifferentTypes.getOrigin().put(table9_tfClassesInCisModulesOfDifferentTypes);

//10-th table: Frequencies of tfClasses in cis-regulatory modules of different types
        log.info("Calculation of FrequenciesOfTfClassesInCisModulesOfDifferentTypes");
        Map<String, Map<String, Double>> cisModulesTypesAndDistinctTfClassesAndCountsOfDistinctTfClasses = getCisModulesTypesAndDistinctTfClassesAndCountsOfDistinctTfClasses(cisModuleTypesAndCisModules, distinctTfClasses);
        TableDataCollection table10_frequenciesOfTfClassesInCisModulesOfDifferentTypes = getTable_frequenciesOfTfClassesInCisModulesOfDifferentTypes(cisModulesTypesAndDistinctTfClassesAndCountsOfDistinctTfClasses);
        table10_frequenciesOfTfClassesInCisModulesOfDifferentTypes.getOrigin().put(table10_frequenciesOfTfClassesInCisModulesOfDifferentTypes);

//11-th table Chi-squared test for independence of tfClasses
        log.info("Chi-squared test for independence of tfClasses is started");
        Map<String, Map<String, double[]>> tfClass1AndtfClass2AndStatisticsPvalues = getChiSquaredIndependenceStatisticsAndPvalues(cisModuleTypesAndCisModules, distinctTfClasses);
        TableDataCollection table11_ChisquaredTestForTfClassesIndependence = getTable_ChisquaredTestForTfClassesIndependence(cisModuleTypesAndCisModules, tfClass1AndtfClass2AndStatisticsPvalues);
        table11_ChisquaredTestForTfClassesIndependence.getOrigin().put(table11_ChisquaredTestForTfClassesIndependence);

//12-th table: times waiting for birth and death
        log.info("Identification of times waiting for birth and death");
        Map<Integer, List<Integer>> populationSizesAndTimesWaitingForBirth = new HashMap<Integer, List<Integer>>();
        Map<Integer, List<Integer>> populationSizesAndTimesWaitingForDeath = new HashMap<Integer, List<Integer>>();
        getTimesWaitingForBirthOrDeath(allBindingRegions, chromosomeNameAndGaps, populationSizesAndTimesWaitingForBirth, populationSizesAndTimesWaitingForDeath);
        TableDataCollection table12_timesWaitingForBithOrDeath = getTable_timesWaitingForBirthOrDeath(populationSizesAndTimesWaitingForBirth, populationSizesAndTimesWaitingForDeath);
        log.info("times waiting for birth and death are identified");
        table12_timesWaitingForBithOrDeath.getOrigin().put(table12_timesWaitingForBithOrDeath);

//13-th table: histograms for times waiting for birth
        log.info("Identification of histograms for times waiting for birth");
        TableDataCollection table13_histogramsOfTimesWaitingForBith = getTable_histogramsOfTimesWaitingForBirth(populationSizesAndTimesWaitingForBirth);
        table13_histogramsOfTimesWaitingForBith.getOrigin().put(table13_histogramsOfTimesWaitingForBith);
        
//14-th and 15-th tables: statistics and p-values of Kolmogorov-Smirnov tests for exponentiality of times waiting for birth and death
        log.info("Calculation of statistics and p-values of Kolmogorov-Smirnov tests for exponentiality of times waiting for birth and death");
        TableDataCollection[] tables = getTable_exponentialityKolmogorovSmirnovStatisticsAndPvaluesForWaitingTimes(populationSizesAndTimesWaitingForBirth, populationSizesAndTimesWaitingForDeath);
        TableDataCollection table14_kolmogorovSmirnovExponentialityOfTimesWaitingForBirth = tables[0];
        TableDataCollection table15_kolmogorovSmirnovexponentialityOfTimesWaitingForDeath = tables[1];
        table14_kolmogorovSmirnovExponentialityOfTimesWaitingForBirth.getOrigin().put(table14_kolmogorovSmirnovExponentialityOfTimesWaitingForBirth);
        table15_kolmogorovSmirnovexponentialityOfTimesWaitingForDeath.getOrigin().put(table15_kolmogorovSmirnovexponentialityOfTimesWaitingForDeath);

//16-th and 17-th tables: statistics and p-values of Chi-squared tests for exponentiality of times waiting for birth and death
        log.info("Calculation of statistics and p-values of Chi-squared tests for exponentiality of times waiting for birth and death");
        TableDataCollection[] tables1 = getTable_exponentialityChiSquaredStatisticsAndPvaluesForWaitingTimes(populationSizesAndTimesWaitingForBirth, populationSizesAndTimesWaitingForDeath);
        TableDataCollection table16_chiSquaredExponentialityOfTimesWaitingForBirth = tables1[0];
        TableDataCollection table17_chiSquaredExponentialityOfTimesWaitingForDeath = tables1[1];
        table16_chiSquaredExponentialityOfTimesWaitingForBirth.getOrigin().put(table16_chiSquaredExponentialityOfTimesWaitingForBirth);
        table17_chiSquaredExponentialityOfTimesWaitingForDeath.getOrigin().put(table17_chiSquaredExponentialityOfTimesWaitingForDeath);
        
//18-th table: exponential mixture for waiting times
        log.info("Analysis of exponential mixture for waiting times is started");
        TableDataCollection table18_exponentialMixtureForWaitingTimes =  getTable_exponentialMixtureForWaitingTimes(populationSizesAndTimesWaitingForBirth, populationSizesAndTimesWaitingForDeath);
        table18_exponentialMixtureForWaitingTimes.getOrigin().put(table18_exponentialMixtureForWaitingTimes);

//20-th table: population sizes and their theoretical and observed probabilities
        log.info("Identification of population sizes and their theoretical and observed probabilities");
        Map<Integer, double[]> populationSizeAndBirthAndDeathRates = Stat.getPopulationSizeAndBirthAndDeathRates(populationSizesAndTimesWaitingForBirth, populationSizesAndTimesWaitingForDeath);
        TableDataCollection table20_populationSizesAndTheirProbabilities = getTable_populationSizesAndTheirProbabilities(populationSizeAndBirthAndDeathRates, allBindingRegions, chromosomeNameAndCorrectedLength);
        table20_populationSizesAndTheirProbabilities.getOrigin().put(table20_populationSizesAndTheirProbabilities);

//21-th histogram of length of binding regions
        log.info("Calculation  of histogram of length of binding regions");
        TableDataCollection table21_histogramOfBindingRegionLength = getTable_histogramOfBindingRegionLength(allBindingRegions);
        table21_histogramOfBindingRegionLength.getOrigin().put(table21_histogramOfBindingRegionLength);

//22-th and 23-th tables: cis-regulatory modules cisModules2 and summary on cisModules2
// 22 table is revised
        log.info("Identification of all cis-regulatory modules of 2-nd type (cisModules2)");
        Map<String, List<CisModule>> allCisModules2 = getAllCisModules2(allBindingRegions, parameters.getMinimalNumberOfOverlaps());
        TableDataCollection table22_cisModules2 = getTable_cisModules2(allCisModules2, "cisModules2");
        table22_cisModules2.getOrigin().put(table22_cisModules2);
        TableDataCollection table23_summaryOnCisModules2 = getTable_summaryOnCisModules2(allCisModules2);
        table23_summaryOnCisModules2.getOrigin().put(table23_summaryOnCisModules2);

//24-th table: ratios Of Observed To Expected Probabilities Of TFClasses In CisModules SubSets
        TableDataCollection table24_ratiosOfObservedToExpectedProbabilitiesOfTFClassesInCisModulesSubSets = getTable_ratiosOfObservedToExpectedProbabilitiesOfTFClassesInCisModulesSubSets(distinctTfClasses, allCisModules2, allBindingRegions);
        table24_ratiosOfObservedToExpectedProbabilitiesOfTFClassesInCisModulesSubSets.getOrigin().put(table24_ratiosOfObservedToExpectedProbabilitiesOfTFClassesInCisModulesSubSets);
        
//25-th table: identical CisModules
        log.info("Identification of all identical cis-regulatory modules among all cisModules2");
        Map<Integer, List<IdenticalCisModules>> numberOfTfClassesAndIdenticalCisModules = getNumberOfTfClassesAndIdenticalCisModules(allCisModules2);
        TableDataCollection table25_identicalCisModules = getTable_identicalCisModules(numberOfTfClassesAndIdenticalCisModules);
        table25_identicalCisModules.getOrigin().put(table25_identicalCisModules);
        printInformationOnIdenticalCisModules(numberOfTfClassesAndIdenticalCisModules);
        
*************/
        
    }
}