/* $Id$ */

package biouml.plugins.gtrd.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import biouml.plugins.gtrd.ProteinGTRDType;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.standard.type.Species;
import biouml.standard.type.Transcript;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Util;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.TextUtil2;

/**
 * @author yura
 *
 */

public class EnsemblUtils
{
	// 04.04.22
    public static final String TF_CLASSIFICATION_TYPE_UNIPROT = "UniProt ID";
    public static final String TF_CLASSIFICATION_TYPE_TF_CLASS = "Wingender TF-class";

    /******************* Gene: start *************************/
    public static class Gene
    {
        GeneTranscript[] geneTranscripts;
        public Gene(GeneTranscript[] geneTranscripts)
        {
            this.geneTranscripts = geneTranscripts;
        }
    }
    /******************* Gene: end ***************************/
    
    /************ GeneTranscript : start ********************/
    public static class GeneTranscript
    {
        private String transcriptName, chromosome, geneID, geneName, transcriptType;
        private Interval fromAndTo;  // positions 'from' and 'to' of transcript on chromosome
        private int strand;
        private Interval cdsFromAndTo;  // 'from' = relative 1-st position of coding region (within mRNA); 'from' >= 0; 'to' = relative last position of coding region; 'to' > 'from'
        private Interval[] exonPositions; // chromosomal positions of exons;
        
//        public GeneTranscript(String transcriptName, String chromosome, Interval fromAndTo, String geneID, String geneType, int strand, Interval cdsFromAndTo, Interval[] exonPositions)
//        {
//            this.transcriptName = transcriptName;
//            this.chromosome = chromosome;
//            this.fromAndTo = fromAndTo;
//            this.geneID = geneID;
//            this.geneType = geneType;
//            this.strand = strand;
//            this.cdsFromAndTo = cdsFromAndTo;
//            this.exonPositions = exonPositions;
//        }
        
        // details about parsing all transcripts see in biouml.plugins.ensembl.access.TranscriptTransformer.java
        public GeneTranscript(Transcript transcript)
        {
            // 1. Determine some fields from transcript attributes.
            transcriptName = transcript.getName();
            DynamicPropertySet attributes = transcript.getAttributes();
            chromosome = (String)attributes.getValue("chr");
            geneID = (String)attributes.getValue("gene");
            transcriptType = (String)attributes.getValue("biotype");
            geneName = (String)attributes.getValue("gene_symbol");
            strand = ((String)attributes.getValue("strand")).equals("+") ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;
            int start = ((Number)attributes.getValue("start")).intValue(), end = ((Number)attributes.getValue("end")).intValue();
            fromAndTo = new Interval(start, end);
            
            // 2. Parsing exonPositions by parsing such strings as "ENSE00002024145:11394774-11401737;2" 
            String[] exons = (String[])transcript.getAttributes().getValue("exons");
            exonPositions = new Interval[exons.length];
            String[] exonNames = new String[exons.length];
            for( String exon : exons )
            {
                String[] array = TextUtil2.split(exon, ';');
                int index = Integer.parseInt(array[1]) - 1;
                array = TextUtil2.split(array[0], ':');
                exonNames[index] = array[0];
                array = TextUtil2.split(array[1], '-');
                exonPositions[index] = new Interval(Integer.parseInt(array[0]), Integer.parseInt(array[1]));
            }
            
            // 3. Parsing cdsFromAndTo by parsing such strings as "ENSE00002024145:109 ENSE00002024145:1032"
            String cds = (String)transcript.getAttributes().getValue("cds");
            if( cds != null )
            {
                String[] array = TextUtil2.split(cds, ' '), subarray1 = TextUtil2.split(array[0], ':'), subarray2 = TextUtil2.split(array[1], ':');
                String name1 = subarray1[0], name2 = subarray2[0];
                int pos1 = Integer.parseInt(subarray1[1]), pos2 = Integer.parseInt(subarray2[1]);
                for( int i = 0; i < exonPositions.length; i++ )
                    if( ! name1.equals(exonNames[i]) )
                        pos1 += exonPositions[i].getLength();
                    else break;
                for( int i = 0; i < exonPositions.length; i++ )
                    if( ! name2.equals(exonNames[i]) )
                        pos2 += exonPositions[i].getLength();
                    else break;
                cdsFromAndTo = new Interval(--pos1, --pos2);
            }
        }
        
        public String getTranscriptName()
        {
            return transcriptName;
        }
        
        public String getChromosome()
        {
            return chromosome;
        }
        
        public String getGeneID()
        {
            return geneID;
        }
        
        public String getGeneName()
        {
            return geneName;
        }
        
        public String getTranscriptType()
        {
            return transcriptType;
        }
        
        public int getStrand()
        {
            return strand;
        }
        
        public Interval getFromAndTo()
        {
            return fromAndTo;
        }
        
        public Interval getCdsFromAndTo()
        {
            return cdsFromAndTo;
        }
        
        public Interval[] getExonPositions()
        {
            return exonPositions;
        }
        
        public int getTss()
        {
            return strand == StrandType.STRAND_PLUS ? fromAndTo.getFrom() : fromAndTo.getTo();
        }
        
        public int getTranscriptEnd()
        {
            return strand == StrandType.STRAND_PLUS ? fromAndTo.getTo() : fromAndTo.getFrom();
        }
        
        public int getTranscriptLength()
        {
            if( exonPositions == null ) return 0;
            int result = 0;
            for( Interval interval : exonPositions )
                result += interval.getLength();
            return result;
        }
        
        
        /**************** static methods ********************/
        
        public static GeneTranscript[] getGeneTranscripts(String[] transcriptIds, DataElementPath pathToTranscriptsCollection)
        {
            GeneTranscript[] result = new GeneTranscript[transcriptIds.length];
            // DataCollection<Transcript> dc = pathToSequences.getRelativePath("../../Data/transcript").getDataCollection(Transcript.class);
            DataCollection<Transcript> dc = pathToTranscriptsCollection.getDataCollection(Transcript.class);
            String[] names = transcriptIds != null ? transcriptIds : dc.getNameList().toArray(new String[0]);
            for( int i = 0; i < names.length; i++ )
            {
                Transcript transcript = null;;
                try
                {
                    transcript = dc.get(names[i]);
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
                result[i] = transcript == null ? null : new GeneTranscript(transcript);
            }
            return result;
        }
    }
    /************ GeneTranscript : end ********************/

    /**********************************************************************/
    /****************************** OligUtils : start *********************/
    /**********************************************************************/

    public static class OligUtils
    {
        private static final char[] NUCLEOTIDES = {'T', 'A', 'G', 'C'};

    	public static Object[] selectNotZeroOligsAndSortThem(Sequence[] sequences, int oligLength, int freqThreshold)
    	{
        	Object[] objects = getOligFrequencies(sequences, oligLength);
        	int[] oligFrequencies = (int[])objects[0];
        	// int numberOfReallyUsedSequences = (int)objects[1];
        	return selectNotZeroOligsAndSortThem(oligFrequencies, freqThreshold, oligLength);
    	}
    	
    	// oligFrequencies[] = oligFrequencies[hashValue] = frequency of olig with hashvalue. 
    	public static Object[] selectNotZeroOligsAndSortThem(int[] oligFrequencies, int freqThreshold, int oligLength)
    	{
    		List<Integer> listForFrequencies = new ArrayList<>(), listForHashValues = new ArrayList<>();
            for( int hashValue = 0; hashValue < oligFrequencies.length; hashValue++ )
            	if( oligFrequencies[hashValue] >= freqThreshold )
            	{
            		listForHashValues.add(hashValue);
            		listForFrequencies.add(oligFrequencies[hashValue]);
            	}
            int[] frequencies = UtilsGeneral.fromListIntegerToArray(listForFrequencies), hashValues = UtilsGeneral.fromListIntegerToArray(listForHashValues);
            int[] frequenciesNew = new int[frequencies.length], hashValuesNew = new int[frequencies.length];
            String[] oligs = new String[frequencies.length];
            int pos[] = Util.sortHeap(frequencies);
            for( int i = 0; i < frequenciesNew.length; i++ )
            {
            	hashValuesNew[i] = hashValues[pos[i]];
            	frequenciesNew[i] = frequencies[i];
            	oligs[i] = getOlig(hashValuesNew[i], oligLength);
            }
            return new Object[]{oligs, frequenciesNew};
    	}

    	// Returned freq[] = freq[hashValue] = frequency of olig with hashvalue. 
        public static Object[] getOligFrequencies(Sequence[] sequences, int oligLength)
        {
            int numberOfDistinctOligs = getNumberOfDistinctOligs(oligLength), numberOfReallyUsedSequences = 0;
            int[] frequencies = new int[numberOfDistinctOligs];
            for( Sequence sequence : sequences )
            {
                if( sequence.getLength() < oligLength ) continue;
                int[] oligFrequenciesInSequence = getOligFrequencies(sequence, oligLength);
                if( oligFrequenciesInSequence == null ) continue;
                for( int i = 0; i < numberOfDistinctOligs; i++ )
                    if( oligFrequenciesInSequence[i] > 0 )
                    	frequencies[i]++;
                numberOfReallyUsedSequences++;
            }
            
            // To set to zero the frequencies of complementary-matched oligs
            for( int hash = 0; hash < numberOfDistinctOligs; hash++ )
            {
                if( frequencies[hash] == 0 ) continue;
                long complementaryHash = getComplementOligHash(hash, oligLength);
                if( complementaryHash == hash ) continue;
                if( complementaryHash > hash )
                	frequencies[(int)complementaryHash] = 0;
                else
                	frequencies[hash] = 0;
            }
            return new Object[]{frequencies, numberOfReallyUsedSequences};
        }
        
        private static long getComplementOligHash(long hashCode, int oligLength)
        {
        	String olig = getOlig(hashCode, oligLength);
        	String complementaryOlig = getComplementOlig(olig);
            int[] intSequence = getIntSequence(complementaryOlig);
            return getOligHashCode(intSequence, 0, oligLength);
        }
        
        public static int[] getIntSequence(String olig)
        {
        	int[] result = new int[olig.length()];
            int n = olig.length();
            for( int i = 0; i < n; i++ )
                switch( olig.charAt(i) )
                {
                    case 'T':
                    case 't': result[i] = 0; break;
                    case 'A':
                    case 'a': result[i] = 1; break;
                    case 'G':
                    case 'g': result[i] = 2; break;
                    case 'C':
                    case 'c': result[i] = 3; break;
                    default : return null;
                }
            return result;
        }

        public static String getComplementOlig(String olig)
        {
            StringBuilder sb = new StringBuilder();
            int n = olig.length();
            for( int i = 0; i < n; i++ )
                switch( olig.charAt(n - 1 - i) )
                {
                    case 'T':
                    case 't': sb.append('A'); break;
                    case 'A':
                    case 'a': sb.append('T'); break;
                    case 'G':
                    case 'g': sb.append('C'); break;
                    case 'C':
                    case 'c': sb.append('G'); break;
                    default : return null;
                }
            return sb.toString();
        }
        
        private static String getOlig(long hashCode, int oligLength)
        {
            int[] intSequence = getIntSequence(hashCode, oligLength);
            return getOlig(intSequence);
        }
        
        // TODO: To replace IntStreamEx
        private static String getOlig(int[] intSequence)
        {
            return IntStreamEx.of(intSequence).map(i -> NUCLEOTIDES[i]).charsToString();
        }

        private static int[] getIntSequence(long hashCode, int oligLength)
        {
            int[] result = new int[oligLength];
            int oligLength1 = oligLength - 1;
            long j = getNumberOfDistinctOligs(oligLength1);
            long jj = hashCode;
            for( int i = 0; i < oligLength1; i++ )
            {
                result[i] = (int)(jj / j);
                jj %= j;
                j /= 4;
            }
            result[oligLength1] = (int)jj;
            return result;
        }

        // olig frequencies are calculated on both strands of sequence
        private static int[] getOligFrequencies(Sequence sequence, int oligLength)
        {
            int[] result = UtilsForArray.getConstantArray(getNumberOfDistinctOligs(oligLength), 0);
            int[] intSequence = getIntSequence(sequence);
            if( ! isIntSequencePositive(intSequence) ) return null;
            int ii = sequence.getLength() - oligLength;
            for( int i = 0; i <= ii; i++ )
            {
                long hash = getOligHashCode(intSequence, i, oligLength);
                result[(int)hash] = 1;
            }
            int[] complementIntSequence = getComplementIntSequence(sequence);
            for( int i = 0; i <= ii; i++ )
            {
                long hash = getOligHashCode(complementIntSequence, i, oligLength);
                result[(int)hash] = 1;
            }
            return result;
        }
        
        private static int[] getComplementIntSequence(Sequence sequence)
        {
            return getIntSequence(SequenceRegion.getReversedSequence(sequence));
        }
        
        private static long getOligHashCode(int[] intSequence, int oligStart, int oligLength)
        {
            long hash = intSequence[oligStart];
            int index = oligStart;
            for( int i = 1; i < oligLength; i++ )
            {
                hash *= 4;
                hash += intSequence[++index];
            }
            return hash;
        }
        
        private static boolean isIntSequencePositive(int[] intSequence)
        {
            for( int code: intSequence )
                if( code < 0 ) return false;
            return true;
        }
        
        private static int[] getIntSequence(Sequence sequence)
        {
            return getIntSequence(sequence.getBytes());
        }
        
        private static int[] getIntSequence(byte[] byteSequence)
        {
        	int n = byteSequence.length;
            int[] result = new int[n];
            for( int i = 0; i < n; i++ )
                switch (byteSequence[i])
                {
                    case 'T' :
                    case 't' : result[i] = 0; break;
                    case 'A' :
                    case 'a' : result[i] = 1; break;
                    case 'G' :
                    case 'g' : result[i] = 2; break;
                    case 'C' :
                    case 'c' : result[i] = 3; break;
                    default  : result[i] = -1;
                }
            return result;
        }
        
        private static int getNumberOfDistinctOligs(int oligLength)
        {
            int result = 1;
            for( int i = 0; i < oligLength; i++ )
                result *= 4;
            return result;
        }
    }

    /**********************************************************************/
    /****************************** OligUtils : end ***********************/
    /**********************************************************************/

    // Output: String[i][] = (String[]) uniprot IDs for tfClasses[i];
    public static String[][] getCorrespondenceBetweenTfClassesAndUniprotIDs(Species givenSpecie, String[] tfClasses)
    {
        String[][] result = new String[tfClasses.length][];
        Properties input = BioHubSupport.createProperties(givenSpecie, ReferenceTypeRegistry.getReferenceType(ProteinGTRDType.class));
        Properties output = BioHubSupport.createProperties(givenSpecie, ReferenceTypeRegistry.getReferenceType(UniprotProteinTableType.class));
        Map<String, String[]> references = BioHubRegistry.getReferences(UtilsGeneral.getDistinctValues(tfClasses), input, output, null);
        for( int i = 0; i < tfClasses.length; i++ )
            result[i] = references.get(tfClasses[i]);
        return result;
    }

    public static String[] getStandardSequencesNames(DataElementPath pathToSequences)
    {
        List<String> result = new ArrayList<>();
        for( AnnotatedSequence as : pathToSequences.getDataCollection(AnnotatedSequence.class) )
            result.add(as.getName());
        return (String[])ArrayUtils.removeElement(result.toArray(new String[0]), "MT");
    }
    
    public static Sequence[] getAnnotatedSequences(DataElementPath pathToSequences)
    {
        List<Sequence> result = new ArrayList<>();
        for( AnnotatedSequence as : pathToSequences.getDataCollection(AnnotatedSequence.class) )
            if( ! as.getName().equals("MT") )
                result.add(as.getSequence());
        return result.toArray(new Sequence[0]);
    }
    
    public static Sequence getAnnotatedSequence(DataElementPath pathToSequences, String chromosomeName)
    {
        return pathToSequences.getChildPath(chromosomeName).getDataElement(AnnotatedSequence.class).getSequence();
    }
    
    public static Sequence getSequenceRegion(Sequence fullChromosome, int regionStart, int regionLength)
    {
        int start = Math.max(1, regionStart);
        int n = fullChromosome.getLength();
        if( start + regionLength - 1 > n )
            start = n - regionLength + 1;
        return new SequenceRegion(fullChromosome, start, regionLength, false, false);
    }
    
    public static Sequence getSequenceRegion(DataElementPath pathToSequences, String chromosomeName, int regionStart, int regionLength)
    {
        return getSequenceRegion(getAnnotatedSequence(pathToSequences, chromosomeName), regionStart, regionLength);
    }

    public static Sequence[] getLinearSequencesWithGivenLengthForBestSites(DataElementPath pathToTrack, int numberOfBestSites, int lengthOfSequenceRegion, DataElementPath pathToSequences)
    {
    	FunSite[] funSites = TrackUtils.readBestFunSitesInTrack(pathToTrack, numberOfBestSites, lengthOfSequenceRegion, lengthOfSequenceRegion);
    	funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
    	return FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
    }
    
    private static Logger log = Logger.getLogger(EnsemblUtils.class.getName());
}