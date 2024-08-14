package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gap;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gene;
import biouml.plugins.bindingregions.utils.TableUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 * Extracted from BindingRegions/mode0
 * "0. Preliminary identification of the auxiliary information about genome (creation of tables: '_chromosomeGaps', '_chromosomeLengths', '_genes', 'summaryOfGenes')"
 */
public class GatheringGenomeStatistics extends AnalysisMethodSupport<GatheringGenomeStatistics.GatheringGenomeStatisticsParameters>
{
    private static final int MINIMAL_GAP_LENGTH = 1000;

    public GatheringGenomeStatistics(DataCollection<?> origin, String name)
    {
        super(origin, name, new GatheringGenomeStatisticsParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        
        // 1.
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        log.info("Identify all chromosome gaps in genome and write them into table '_chromosomeGaps'");
        jobControl.pushProgress(0, 70);
        Map<String, Integer> gapLengths = getGapsInAllChromosomes(pathToSequences, pathToOutputs.getChildPath("_chromosomeGaps"));
        jobControl.popProgress();
        if( jobControl.isStopped() ) return null;
        
        // 2.
        log.info("Calculate the lengths of chromosomes and write them into table '_chromosomeLengths'");
        jobControl.pushProgress(70, 80);
        Object[] objects = GatheringGenomeStatistics.getNameAndLengthOfChromosomes(pathToSequences, gapLengths );
        TableUtils.writeIntegerTable((int[][])objects[0], (String[])objects[1], (String[])objects[2], pathToOutputs, "_chromosomeLengths");
        jobControl.popProgress();
        if( jobControl.isStopped() ) return null;
        
        // 3.
        log.info("Identify all genes in genome and write them into table '_genes'");
        jobControl.pushProgress(80, 90);
        Map<String, List<Gene>> chromosomesAndGenes = getGenesFromWholeGenome(pathToSequences);
        writeGenesTable(chromosomesAndGenes, pathToOutputs.getChildPath("_genes"));
        jobControl.popProgress();
        if(jobControl.isStopped()) return null;

        // 4.
        log.info("Calculate summary of genes and write it into table 'summaryOfGenes'");
        jobControl.pushProgress(90, 100);
        TObjectIntMap<String> geneTypesAndCounts = getSummaryOfGenes(chromosomesAndGenes);
        writeGenesSummaryTable(geneTypesAndCounts, pathToOutputs.getChildPath("summaryOfGenes"));
        jobControl.popProgress();
        return pathToOutputs.getDataCollection();
    }

    public static Map<String, List<Gene>> getGenesFromWholeGenome(DataElementPath pathToSequences) throws Exception
    {
        Map<String, List<Gene>> result = new HashMap<>();
        Track geneTrack = pathToSequences.getRelativePath("../../Tracks/Genes").getDataElement(Track.class);
        for( DataElementPath chromosomePath : pathToSequences.getChildren() )
        {
            String chromosomeName = chromosomePath.getName();
            List<Gene> genes = new ArrayList<>();
            for( Site site : geneTrack.getSites(chromosomePath.toString(), 0, Integer.MAX_VALUE) )
            {
                String geneType = site.getType();
                String ensemblId = (String)site.getProperties().getValue("id");
                if( geneType.equals("protein_coding") || geneType.equals("processed_transcript") || geneType.equals("lincRNA") )
                {
                    TIntSet startSet = new TIntHashSet();
                    TIntSet endSet = new TIntHashSet();
                    for( Interval interval : (Interval[])site.getProperties().getValue("transcripts") )
                    {
                        startSet.add(interval.getFrom());
                        endSet.add(interval.getTo());
                    }
                    int[] arrayOfTranscriptionStarts = startSet.toArray();
                    Arrays.sort(arrayOfTranscriptionStarts);
                    int[] arrayOfTranscriptionEnds = endSet.toArray();
                    Arrays.sort(arrayOfTranscriptionEnds);
                    genes.add(new Gene(ensemblId, geneType, arrayOfTranscriptionStarts, arrayOfTranscriptionEnds));
                }
                else
                    genes.add(new Gene(ensemblId, geneType, new int[]{site.getFrom()}, new int[] {site.getTo()}));
            }
            result.put(chromosomeName, genes);
        }
        return result;
    }

    private static TObjectIntMap<String> getSummaryOfGenes(Map<String, List<Gene>> allGenes)
    {
        TObjectIntMap<String> result = new TObjectIntHashMap<>();
        for( List<Gene> genes: allGenes.values() )
            for( Gene gene : genes )
                result.adjustOrPutValue(gene.getGeneType(), 1, 1);
        return result;
    }

    private static void writeGenesSummaryTable(TObjectIntMap<String> geneTypesAndCounts, DataElementPath path) throws Exception
    {
        int numberOfAllGenes = 0;
        for( int num: geneTypesAndCounts.values() )
            numberOfAllGenes += num;
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(path);
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        table.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
        table.getColumnModel().addColumn("geneTypes", String.class);
        table.getColumnModel().addColumn("countsOfGeneTypes", Integer.class);
        table.getColumnModel().addColumn("percentageOfGeneTypes", Double.class);
        int iRow = 0;
        for( String geneType: geneTypesAndCounts.keySet() )
        {
            int numberOfGenes = geneTypesAndCounts.get(geneType);
            float percentage = (numberOfGenes) * ((float)100.0) / (numberOfAllGenes);
            TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {geneType, numberOfGenes, percentage}, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    private static void writeGenesTable(Map<String, List<Gene>> chromosomesAndGenes, DataElementPath dataElementPath) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dataElementPath);
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        table.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
        table.getColumnModel().addColumn("ensemblId", String.class);
        table.getColumnModel().addColumn("geneType", String.class);
        table.getColumnModel().addColumn("chromosome", String.class);
        table.getColumnModel().addColumn("transcriptionStarts", StringSet.class);
        table.getColumnModel().addColumn("transcriptionEnds", StringSet.class);
        List<String> chromosomes = new ArrayList<>( chromosomesAndGenes.keySet() );
        if( chromosomes.remove("X") )
            chromosomes.add(0, "X");
        int iRow = 0;
        for( Entry<String, List<Gene>> entry : chromosomesAndGenes.entrySet() )
        {
            String chromosome = entry.getKey();
            List<Gene> genes = entry.getValue();
            for( Gene gene : genes )
            {
                int[] starts = gene.getTranscriptionStarts();
                StringSet startSet = new StringSet();
                for( int i : starts )
                    startSet.add(Integer.toString(i));
                int[] ends = gene.getTranscriptionEnds();
                StringSet endSet = new StringSet();
                for( int i : ends )
                    endSet.add(Integer.toString(i));
                Object[] values = new Object[]{gene.getEnsemblId(), gene.getGeneType(), chromosome, startSet, endSet};
                TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values, true);
            }
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
     }

    public static List<Gap> getChromosomeGaps(Sequence sequence)
    {
        List<Gap> result = new ArrayList<>();
        int chromosomeLength = sequence.getLength(), gapStartPosition;
        for( int i = 1; i <= chromosomeLength; i += MINIMAL_GAP_LENGTH )
            if( isN(sequence.getLetterAt(i)) )
            {
                int lastPos = Math.max(0, i - MINIMAL_GAP_LENGTH);
                for( gapStartPosition = i - 1; gapStartPosition > lastPos; gapStartPosition-- )
                    if( !isN(sequence.getLetterAt(gapStartPosition)) ) break;
                gapStartPosition++;
                for( ; i <= chromosomeLength; i++ )
                    if(!isN(sequence.getLetterAt(i))) break;
                if( i - gapStartPosition >= MINIMAL_GAP_LENGTH )
                    result.add(new Gap(gapStartPosition, i-gapStartPosition));
            }
        return result;
    }

    private static boolean isN(byte sequenceElement)
    {
        return sequenceElement == (byte)'N' || sequenceElement == (byte)'n';
    }

    private Map<String, Integer> getGapsInAllChromosomes(DataElementPath pathToSequences, DataElementPath dataElementPath) throws Exception
    {
        final Map<String, Integer> gapLengths = new HashMap<>();
        final TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dataElementPath);
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        table.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
        table.getColumnModel().addColumn("chromosome", String.class);
        table.getColumnModel().addColumn("startOfGap", Integer.class);
        table.getColumnModel().addColumn("lengthOfGap", Integer.class);
        jobControl.forCollection(pathToSequences.getChildren(), new Iteration<ru.biosoft.access.core.DataElementPath>()
        {
            int iRow = 0;

            @Override
            public boolean run(DataElementPath path)
            {
                String chromosome = path.getName();
                Sequence sequence = path.getDataElement(AnnotatedSequence.class).getSequence();
                List<Gap> gaps = getChromosomeGaps(sequence);
                int totalLength = 0;
                for( Gap gap : gaps )
                {
                    TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[]{chromosome, gap.getInterval().getFrom(), gap.getInterval().getLength()}, true);
                    totalLength += gap.getInterval().getLength();
                }
                gapLengths.put(chromosome, totalLength);
                return true;
            }
        });
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return gapLengths;
    }

    // old
    /***
     * Calculation of lengths of chromosomes and lengths corrected on chromosome gaps (chromosomeLength, chromosomeLengthCorrectedOnGaps).
     * @param pathToSequences
     * @param gapLengths
     * @return Object[]: Object[0] = (int) dataTable[][]; Object[1] = (String[]) names of rows (names of chromosomes); Object[2] = (String[]) names of columns.
     */
    /***
    private static Object[] getNameAndLengthOfChromosomes(DataElementPath pathToSequences, Map<String, Integer> gapLengths)
    {
        Object[] result = new Object[3];
        Map<String, Interval> nameAndLength = EnsemblUtils.getChromosomeIntervals(pathToSequences);
        int n = nameAndLength.size();
        int m = 2;
        int[][] dataTable = new int[n][m];
        String[] namesOfChromosomes = new String[n];
        int i = 0;
        for( String chromosome: nameAndLength.keySet() )
        {
            dataTable[i][0] = nameAndLength.get(chromosome).getLength();
            dataTable[i][1] = nameAndLength.get(chromosome).getLength() - ( gapLengths.containsKey(chromosome) ? gapLengths.get(chromosome) : 0 );
            namesOfChromosomes[i++] = chromosome;
        }
        
        String[] namesOfColumns = new String[m];
        namesOfColumns[0] = "chromosomeLength";
        namesOfColumns[1] = "chromosomeLengthCorrectedOnGaps";
        result[0] = dataTable;
        result[1] = namesOfChromosomes;
        result[2] = namesOfColumns;
        return result;
    }
    ***/
    
    // new
    /***
     * Calculation of lengths of chromosomes and lengths corrected on chromosome gaps (chromosomeLength, chromosomeLengthCorrectedOnGaps).
     * @param pathToSequences
     * @param gapLengths
     * @return Object[]: Object[0] = (int) dataTable[][]; Object[1] = (String[]) names of rows (names of chromosomes); Object[2] = (String[]) names of columns.
     */
    private static Object[] getNameAndLengthOfChromosomes(DataElementPath pathToSequences, Map<String, Integer> gapLengths)
    {
        Map<String, Interval> nameAndLength = EnsemblUtils.getChromosomeIntervals(pathToSequences);
        int n = nameAndLength.size(), i = 0;
        int[][] dataTable = new int[n][];
        String[] namesOfChromosomes = new String[n];
        for( Map.Entry<String, Interval> entry : nameAndLength.entrySet() )
        {
            int length = entry.getValue().getLength();
            String chromosome = entry.getKey();
            dataTable[i] = new int[]{length, length - (gapLengths.containsKey(chromosome) ? gapLengths.get(chromosome) : 0)};
            namesOfChromosomes[i++] = chromosome;
        }
        String[] namesOfColumns = new String[]{"chromosomeLength", "chromosomeLengthCorrectedOnGaps"};
        return new Object[]{dataTable, namesOfChromosomes, namesOfColumns};
    }


    public static class GatheringGenomeStatisticsParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private DataElementPath outputPath;
        
        public GatheringGenomeStatisticsParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }
        
        @PropertyName(MessageBundle.PN_OUTPUT_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_PATH)
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange("outputPath", oldValue, outputPath);
        }
        
        @PropertyName(MessageBundle.PN_DB_SELECTOR)
        @PropertyDescription(MessageBundle.PD_DB_SELECTOR)
        public BasicGenomeSelector getDbSelector()
        {
            return dbSelector;
        }
        public void setDbSelector(BasicGenomeSelector dbSelector)
        {
            Object oldValue = this.dbSelector;
            this.dbSelector = dbSelector;
            dbSelector.setParent(this);
            firePropertyChange("dbSelector", oldValue, dbSelector);
        }
    }
    
    public static class GatheringGenomeStatisticsParametersBeanInfo extends BeanInfoEx2<GatheringGenomeStatisticsParameters>
    {
        public GatheringGenomeStatisticsParametersBeanInfo()
        {
            super(GatheringGenomeStatisticsParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
