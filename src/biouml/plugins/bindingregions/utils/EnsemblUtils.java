package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;

/**
 * @author yura
 *
 */
public class EnsemblUtils
{
    public static final String SEQUENCE_SAMPLE = "Sequence sample";

    public static class Gap
    {
        Interval interval;
        
        public Gap(int gapStartPosition, int gapLength)
        {
            this.interval = new Interval(gapStartPosition, gapStartPosition + gapLength-1);
        }
        
        public Interval getInterval()
        {
            return interval;
        }
        
        public int correct(int position)
        {
            return position > interval.getFrom() ? position - getInterval().getLength() : position;
        }
        
        public static int getPositionCorrectedByGaps(int x, List<Gap> gaps)
        {
            return StreamEx.of( gaps ).foldLeft( x, (i, gap) -> gap.correct( i ) );
        }
    }
    
    /*****************************/////////////////////////**********************************/
    
    public static class Gene implements Comparable<Gene>
    {
        public static final String PROTEIN_CODING = "protein_coding";
        public static final String LINC_RNA = "lincRNA";

        String chromosome;
        Interval interval;  // chromosomal 'from' and 'to' of transcript
        String ensemblId;
        String geneType;
        String geneName;
        int[] transcriptionStarts; // to remove this field after refactoring 'GatheringGenomeStatistics' analysis
        int[] transcriptionEnds; // to remove this field after refactoring 'GatheringGenomeStatistics' analysis
        
        public Gene(String chromosome, Interval interval, String ensemblId, String geneType, String geneName, int[] transcriptionStarts, int[] transcriptionEnds)
        {
            this.chromosome = chromosome;
            this.interval = interval;
            this.ensemblId = ensemblId;
            this.geneType = geneType;
            this.geneName = geneName;
            this.transcriptionStarts = transcriptionStarts;
            this.transcriptionEnds = transcriptionEnds;
        }
        
        public Gene(Site site)
        {
            this( site.getSequence().getName(), site.getInterval(), (String)site.getProperties().getValue( "id" ), site.getType(),
                    (String)site.getProperties().getValue( "symbol" ), null, null );
        }
        
        public Gene(String ensemblId, String geneType, int[] transcriptionStarts, int[] transcriptionEnds)
        {
            this(null, null, ensemblId, geneType, null, transcriptionStarts, transcriptionEnds);
        }
        
        public String getChromosome()
        {
            return chromosome;
        }
        
        public Interval getInterval()
        {
            return interval;
        }
        
        public String getEnsemblId()
        {
            return ensemblId;
        }
        
        public String getGeneType()
        {
            return geneType;
        }
        
        public String getGeneName()
        {
            return geneName;
        }

        public int[] getTranscriptionStarts()
        {
            return transcriptionStarts;
        }
        
        public int[] getTranscriptionEnds()
        {
            return transcriptionEnds;
        }
    
        // to replace in future by getInterval()
        public int [] getStartAndEndOfGene()
        {
            int[] result = new int[2];
//          result[0] = transcriptionStarts[0] < transcriptionEnds[0] ? transcriptionStarts[0] : transcriptionEnds[0];
            result[0] = Math.min(transcriptionStarts[0], transcriptionEnds[0]);
            int j = transcriptionStarts[transcriptionStarts.length -1];
            int jj = transcriptionEnds[transcriptionEnds.length -1];
            result[1] = j < jj ? jj : j;
            return result;
        }
        
        @Override
        public int compareTo(Gene o)
        {
            int position1 = transcriptionStarts[0] <= transcriptionEnds[0] ? transcriptionStarts[0] : transcriptionEnds[0];
            int i = o.getTranscriptionStarts()[0];
            int ii = o.getTranscriptionEnds()[0];
            int position2 = i <= ii ? i : ii;
            return position1 - position2;
        }
        
        ///////////////////////////////// O.K.
        public static void removeAllNonProteinCodingGenes(Map<String, List<Gene>> chromosomesAndGenes)
        {
            for( List<Gene> genes: chromosomesAndGenes.values() )
            {
                Iterator<Gene> it = genes.iterator();
                while( it.hasNext() )
                {
                    Gene gene = it.next();
                    if( ! gene.getGeneType().equals("protein_coding") )
                        it.remove();
                }
            }
        }

        /***
         * 
         * @param pathToTable
         * @return genes as objects of class <Gene>
         */
        public static Map<String, List<Gene>> readGenesInTable(DataElementPath pathToTable)
        {
            Map<String, List<Gene>> result = new HashMap<>();
            TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
            for( int i = 0; i < table.getSize(); i++ )
            {
                Object[] objects = table.getAt(i).getValues();
                String chromosome = (String)objects[2];
                String ensemblId = (String)objects[0];
                String geneType = (String)objects[1];
                int[] starts = ( (StringSet)objects[3] ).stream().mapToInt( Integer::parseInt ).toArray();
                int[] ends = ( (StringSet)objects[4] ).stream().mapToInt( Integer::parseInt ).toArray();
                result.computeIfAbsent( chromosome, k -> new ArrayList<>() )
                    .add(new Gene(ensemblId, geneType, starts, ends));
            }
            return result;
         }
        
        // in future to replace by 'getGivenGenes(DataElementPath pathToSequences, List<String> idsOfGivenGenes)'
        public static Map<String, List<Gene>> getGivenGenes(List<String> idsOfGivenGenes, Map<String, List<Gene>> chromosomesAndGenes)
        {
            return EntryStream.of(chromosomesAndGenes)
                .mapValues(genes -> StreamEx.of(genes).filter(gene -> idsOfGivenGenes.contains(gene.getEnsemblId())).toList())
                .removeValues(List::isEmpty).toMap();
        }
        
        public static List<Gene> getGivenGenes(DataElementPath pathToSequences, List<String> idsOfGivenGenes) throws Exception
        {
            Track geneTrack = pathToSequences.getRelativePath("../../Tracks/Genes").getDataElement(Track.class);
            DataElementPathSet pathSet = pathToSequences.getChildren();
            return pathSet.stream().flatMap( chromosomePath -> geneTrack.getSites(chromosomePath.toString(), 0, Integer.MAX_VALUE).stream() )
                .filter( site -> idsOfGivenGenes.contains( site.getProperties().getValue("id") ) ).map( Gene::new ).collect(Collectors.toList());
        }

        public static Map<String, List<Gene>> getGenesFromWholeGenome(DataElementPath pathToSequences, List<String> givenGeneTypes, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            Map <String, List<Gene>> result = new HashMap<>();
            Track geneTrack = pathToSequences.getRelativePath("../../Tracks/Genes").getDataElement(Track.class);
            int difference = to - from, index = 0;
            DataElementPathSet pathSet = pathToSequences.getChildren();
            for( DataElementPath chromosomePath : pathSet )
            {
                String chromosomeName = chromosomePath.getName();
                List<Gene> genes = geneTrack.getSites(chromosomePath.toString(), 0, Integer.MAX_VALUE).stream()
                    .filter( site -> givenGeneTypes.contains(site.getType()) )
                    .map( Gene::new ).collect(Collectors.toList());
                result.put(chromosomeName, genes);
                if( jobControl != null )
                    jobControl.setPreparedness(from + ++index * difference / pathSet.size());
            }
            return result;
        }
    }

    /*****************************/////////////////////////**********************************/

    ///////////////////////////////////////////// O.K.
    public static Map<String, Interval> getChromosomeIntervals(DataElementPath pathToSequences)
    {
        return sequences(pathToSequences).toMap( Sequence::getName, Sequence::getInterval );
    }
    
    public static Map<String, Integer> getChromosomeLengths(DataElementPath pathToSequences)
    {
        return sequences(pathToSequences).toMap( Sequence::getName, Sequence::getLength );
    }
    
    public static Map<String, Sequence> getChromosomeAndSequence(DataElementPath pathToSequences)
    {
        return annotatedSequences( pathToSequences ).toMap( AnnotatedSequence::getName, AnnotatedSequence::getSequence );
    }
    
    ///////////////////////////////////////////// O.K.
    public static List<Sequence> getSequences(DataElementPath pathToSequences)
    {
        return sequences( pathToSequences ).toList();
    }

    public static StreamEx<Sequence> sequences(DataElementPath pathToSequences)
    {
        return annotatedSequences( pathToSequences ).map( AnnotatedSequence::getSequence );
    }
    
    private static StreamEx<AnnotatedSequence> annotatedSequences(DataElementPath pathToSequences)
    {
        return StreamEx.of( pathToSequences.getDataCollection( AnnotatedSequence.class ).stream() )
                .remove( aseq -> aseq.getName().equals( "MT" ) );
    }

    public static StreamEx<String> sequenceNames(DataElementPath pathToSequences)
    {
        return annotatedSequences( pathToSequences ).map( AnnotatedSequence::getName );
    }
    
    public static String[] getStandardSequencesNames(DataElementPath pathToSequences)
    {
        List<String> result = new ArrayList<>();
        DataCollection<AnnotatedSequence> sequencesPaths = pathToSequences.getDataCollection(AnnotatedSequence.class);
        for( AnnotatedSequence as : sequencesPaths )
        {
            String name = as.getName();
            if( ! name.equals("MT") )
                result.add(name);
        }
        return result.toArray(new String[0]);
    }
    
    ///////////////////////////////////////////// O.K.
    public static Map<String, List<Gap>> readChromosomeNameAndGapsInTable(DataElementPath dataElementPath)
    {
        Map<String, List<Gap>> result = new HashMap<>();
        TableDataCollection table = dataElementPath.getDataElement(TableDataCollection.class);
        for( RowDataElement row : table )
        {
            Object[] objects = row.getValues();
            String chromosome = (String)objects[0];
            /***
            int gapStartPosition = (Integer)objects[1];
            int gapLength = (Integer)objects[2];
            if( ! result.containsKey(chromosome) )
            {
                List<Gap> gaps = new ArrayList<Gap>();
                result.put(chromosome, gaps);
            }
            List<Gap> gaps = result.get(chromosome);
            gaps.add(new Gap(gapStartPosition, gapLength));
            result.put(chromosome, gaps);
            ***/
            result.computeIfAbsent( chromosome, k -> new ArrayList<>() ).add(new Gap((Integer)objects[1], (Integer)objects[2]));
        }
        return result;
    }

    ///////////////////////////////////////////// O.K.
    public static Map<String, List<Gap>> readChromosomeNameAndGapsInTable(DataElementPath dataElementPath, String nameOfTable)
    {
        DataElementPath dep = DataElementPath.create(dataElementPath.optParentCollection(), nameOfTable);
        return readChromosomeNameAndGapsInTable(dep);
    }

    ///////////////////////////////////////////// O.K.
    public static Sequence getSequenceRegion(Sequence sequence, int regionStart, int regionLength)
    {
        int start = Math.max(1, regionStart);
        int n = sequence.getLength();
        if( start + regionLength - 1 > n )
            start = n - regionLength + 1;
        return new SequenceRegion(sequence, start, regionLength, false, false);
    }

    ///////////////////////////////////////////// O.K.
    public static Sequence getSequenceRegion(String chromosome, int regionStart, int regionLength, DataElementPath pathToSequences)
    {
        return getSequenceRegion(getSequence(pathToSequences, chromosome), regionStart, regionLength);
    }

    public static Sequence getSequence(DataElementPath pathToSequences, String chromosome)
    {
        return pathToSequences.getChildPath(chromosome).getDataElement(AnnotatedSequence.class).getSequence();
    }
    
    public static Map<String, byte[]> getSequencesFromFastaTrack(DataElementPath pathToMrnaSequences, StringSet sequenceNames) throws Exception
    {
        return sequenceNames.stream().toMap( name -> getSequenceFromFastaTrack( pathToMrnaSequences, name ) );
    }
    /////
    public static byte[] getSequenceFromFastaTrack(DataElementPath pathToFastaMrnaSequences, String sequenceName)
    {
        AnnotatedSequence seq = pathToFastaMrnaSequences.getChildPath( sequenceName ).optDataElement(AnnotatedSequence.class);
        if( seq == null ) return null;
        byte[] mrnaSeq = seq.getSequence().getBytes();
        return new String(mrnaSeq).toLowerCase().getBytes();
    }
}