package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.InternalException;
import ru.biosoft.analysis.Stat;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;

/**
 * @author yura
 *
 */


public class Olig
{
    public static final String OLIGONUCLEOTIDE = "Oligonucleotide_";
    private final String olig;

    public Olig(String olig)
    {
        this.olig = olig;
    }

    public Olig(long hashCode, int oligLength)
    {
        int[] intSequence = getIntSequence(hashCode, oligLength);
        this.olig = getOlig(intSequence);
    }
    
    private static final char[] NUCLEOTIDES = {'T', 'A', 'G', 'C'};
    
    private String getOlig(int[] intSequence)
    {
        return IntStreamEx.of( intSequence ).map( i -> NUCLEOTIDES[i] ).charsToString();
    }
    
    @Override
    public String toString()
    {
        return olig;
    }
    
    public Olig getComplementOlig()
    {
        String stringOlig = olig;
        StringBuilder sb = new StringBuilder();
        int n = stringOlig.length();
        for( int i = 0; i < n; i++ )
            switch( stringOlig.charAt(n - 1 - i) )
            {
                case 'T':
                case 't': sb.append('A'); break;
                case 'A':
                case 'a': sb.append('T'); break;
                case 'G':
                case 'g': sb.append('C'); break;
                case 'C':
                case 'c': sb.append('G'); break;
                default:
                    throw new IllegalArgumentException( "stringOlig[" + ( n - 1 - i ) + "] = " + stringOlig.charAt( n - 1 - i ) );
            }
        return new Olig(sb.toString());
    }
    
    private static String getOlig(byte[] sequence, int from, int oligLength)
    {
        return IntStreamEx.of( sequence, from, from+oligLength ).map( Character::toUpperCase ).charsToString();
    }

    private static int[] getIntSequence(byte[] sequence, int from, int to)
    {
        int n = to - from + 1;
        int[] result = new int[n];
        for( int i = 0; i < n; i++ )
            switch ( sequence[i + from] )
            {
                case 'T' :
                case 't' : result[i] = 0; break;
                case 'A' :
                case 'a' : result[i] = 1; break;
                case 'G' :
                case 'g' : result[i] = 2; break;
                case 'C' :
                case 'c' : result[i] = 3; break;
                default: result[i] = -1;
            }
        return result;
    }
    
    private static int[] getIntSequence(Sequence sequence)
    {
        byte[] letters = sequence.getBytes();
        return getIntSequence(letters, 0, letters.length - 1);
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
    
    private static boolean isIntSequencePositive(int[] intSequence)
    {
        for( int code: intSequence )
            if( code < 0 ) return false;
        return true;
    }
    
    private static int[] getComplementIntSequence(Sequence sequence)
    {
        Sequence reversedSequence = SequenceRegion.getReversedSequence(sequence);
        return getIntSequence(reversedSequence);
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
    
    public static int getNumberOfDistinctOligs(int oligLength)
    {
        int result = 1;
        for( int i = 0; i < oligLength; i++ )
            result *= 4;
        return result;
    }
    
    // olig frequencies are calculated on both strands of sequence
    private static int[] getOligFrequencies(Sequence sequence, int oligLength)
    {
        int[] result = new int[getNumberOfDistinctOligs(oligLength)];
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

    // olig frequencies are calculated only on positive strand of sequence
    private static int[] getOligFrequencies(int[] intSequence, int oligLength)
    {
        if( ! isIntSequencePositive(intSequence) ) return null;
        int[] result = new int[getNumberOfDistinctOligs(oligLength)];
        for( int i = 0; i <= intSequence.length - oligLength; i++ )
        {
            long hash = getOligHashCode(intSequence, i, oligLength);
            result[(int)hash]++;
        }
        return result;
    }

    private int[] getIntSequence()
    {
        int n = olig.length();
        int[] result = new int[n];
        for( int i = 0; i < n; i++ )
            switch( olig.charAt(i) )
            {
                case 'U':
                case 'u':
                case 'T':
                case 't': result[i] = 0; break;
                case 'A':
                case 'a': result[i] = 1; break;
                case 'G':
                case 'g': result[i] = 2; break;
                case 'C':
                case 'c': result[i] = 3; break;
                default:
                    throw new InternalException( "olig[" + i + "]=" + ( olig.charAt( i ) ) );
            }
        return result;
    }
    
    private long getHashCode()
    {
        return getOligHashCode(getIntSequence(), 0, olig.length());
    }
    
    private static long getComplementOligHash(long hashCode, int oligLength)
    {
        Olig olig = new Olig(hashCode, oligLength);
        Olig complementaryOlig = olig.getComplementOlig();
        int[] intSequence = complementaryOlig.getIntSequence();
        return getOligHashCode(intSequence, 0, oligLength);
    }

    public static Object[] getOligFrequencies(Map<String, List<BindingRegion>> bindingRegions, DataElementPath pathToSequences, int minimalLengthOfSequenceRegion, int oligLength)
    {
        int numberOfDistinctOligs = getNumberOfDistinctOligs(oligLength);
        int[] freq = new int[numberOfDistinctOligs];
        int numberOfUsedBindingRegions = 0;
        for( Sequence sequence : BindingRegion.sequencesForBindingRegions(bindingRegions, pathToSequences, minimalLengthOfSequenceRegion) )
        {
            if( sequence.getLength() < oligLength ) continue;
            int[] oligFrequenciesInSequence = getOligFrequencies(sequence, oligLength);
            if( oligFrequenciesInSequence == null ) continue;
            numberOfUsedBindingRegions++;
            for( int i = 0; i < numberOfDistinctOligs; i++ )
                if( oligFrequenciesInSequence[i] > 0 )
                    freq[i]++;
        }
        // To set to zero the frequencies of complementary-matched oligs
        for( int hash = 0; hash < numberOfDistinctOligs; hash++ )
        {
            if( freq[hash] == 0 ) continue;
            long complementaryHash = getComplementOligHash(hash, oligLength);
            if( complementaryHash == hash ) continue;
            if( complementaryHash > hash )
                freq[(int)complementaryHash] = 0;
            else
                freq[hash] = 0;
        }
        return new Object[] {numberOfUsedBindingRegions, freq};
    }

    /***
     * olig frequencies are calculated only on positive strand of sequence
     * if there are equal oligs in one sequence, then they contribute unit only
     * @param sequences
     * @param oligLength
     * @return
     */
    public static int[] getOligFrequencies(List<byte[]> sequences, int oligLength)
    {
        int numberOfDistinctOligs = getNumberOfDistinctOligs(oligLength);
        int[] frequences = new int[numberOfDistinctOligs];
        for( byte[] sequence : sequences )
        {
            int jj = sequence.length - oligLength;
            Set<String> distinctOligs = new HashSet<>();
            for( int j = 0; j <= jj; j++ )
            {
                String olig = getOlig(sequence, j, oligLength);
                if( olig != null )
                    distinctOligs.add(olig);
            }
            for( String s : distinctOligs )
                frequences[(int)(new Olig(s)).getHashCode()]++;
        }
        return frequences;
    }
    
    public static Double getComplementaryIndex(byte[] sequence, int from, int to, int oligLength)
    {
        double result = 0.0;
        int[] seq = getIntSequence(sequence, from, to);
        int[] frequencies = getOligFrequencies(seq, oligLength);
        if( frequencies == null ) return null;
        for( int i = 0; i < frequencies.length; i++ )
        {
            if( frequencies[i] < 0 ) continue;
            int complementHash = (int)getComplementOligHash(i, oligLength);
            if( complementHash == i ) continue;
            result += Math.abs(frequencies[i] - frequencies[complementHash]);
            frequencies[complementHash] = -1;
        }
        return 1.0 - result / (seq.length - oligLength + 1);
    }
    
    public static Double getCorrelationBasedComplementaryIndex(byte[] sequence, int from, int to, int oligLength) throws Exception
    {
        int[] seq = getIntSequence(sequence, from, to);
        int[] frequencies = getOligFrequencies(seq, oligLength);
        if( frequencies == null ) return null;
        List<Double> frequenciesInPlusStrand = new ArrayList<>(), frequenciesInMinusStrand = new ArrayList<>();
        for( int i = 0; i < frequencies.length; i++ )
        {
            int complementHash = (int)getComplementOligHash(i, oligLength);
            if( complementHash == i ) continue;
            frequenciesInPlusStrand.add((double)frequencies[i]);
            frequenciesInMinusStrand.add((double)frequencies[complementHash]);
        }
        return Stat.pearsonCorrelation(MatrixUtils.fromListToArray(frequenciesInPlusStrand), MatrixUtils.fromListToArray(frequenciesInMinusStrand));
    }

    
    public static boolean areTwoSequenceElementsComplement(byte firstElement, byte secondElement)
    {
        switch(firstElement)
        {
            case 'a' : return secondElement == 't';
            case 't' : return secondElement == 'a';
            case 'g' : return secondElement == 'c';
            case 'c' : return secondElement == 'g';
            default  : return false;
        }
    }
    
    public static byte[] getReversedSequence(byte[] sequence)
    {
        int n = sequence.length;
        byte[] result = new byte[n];
        for( int i = 0; i < n; i++ )
            switch( sequence[i] )
            {
                case 'T' :
                case 't' : result[n - 1 - i] = 'a'; break;
                case 'A' :
                case 'a' : result[n - 1 - i] = 't'; break;
                case 'G' :
                case 'g' : result[n - 1 - i] = 'c'; break;
                case 'C' :
                case 'c' : result[n - 1 - i] = 'g'; break;
                default  : throw new IllegalArgumentException("Sequence contains the forbidden elements: sequence[" + i + "] = " + (sequence[i]));
            }
        return result;
    }
    

    public static Integer getFirstPositionOfOlig(byte[] sequence, byte[] olig)
    {
        for( int i = 0; i < sequence.length - olig.length; i++ )
            if( isGivenOlig(sequence, i, olig) ) return i;
        return null;
    }

    public static void toLowerCase(byte[] sequence)
    {
        for( int i = 0; i < sequence.length; i++ )
            switch( sequence[i] )
            {
                case 't' :
                case 'T' : sequence[i] = 't'; break;
                case 'a' :
                case 'A' : sequence[i] = 'a'; break;
                case 'g' :
                case 'G' : sequence[i] = 'g'; break;
                case 'c' :
                case 'C' : sequence[i] = 'c'; break;
                default  : throw new IllegalArgumentException( "Sequence contains the forbidden elements: sequence[" + i + "] = " + sequence[i] );
            }
    }
    
    // Why does it return sometimes null ???
    public static String getStopCodon(Interval cdsFromAndTo, byte[] transcriptSequence)
    {
        int pos = cdsFromAndTo.getTo();
        byte[] seq = new byte[]{transcriptSequence[pos - 2], transcriptSequence[pos - 1 ], transcriptSequence[pos]};
        toLowerCase(seq);
        return new String(seq);
    }
    
    //O.K.\\
    public static byte[] getSubByteArray(byte[] array, int firstPosition, int length)
    {
        if( firstPosition < 0 || firstPosition + length > array.length ) return null;
        byte[] subArray = new byte[length];
        for( int i = 0; i < length; i++ )
            subArray[i] = array[i + firstPosition];
        toLowerCase(subArray);
        return subArray;
    }
    
    // O.K.\\
    public static String getStringFromByteInerval(Interval interval, byte[] sequence)
    {
        if( interval == null) return null;
        byte[] seq = getSubByteArray(sequence, interval.getFrom(), interval.getLength());
        return new String(seq);
    }
    
/////////////////////////////////////// tested /////////////////////////////////
    
    //O.K.\\
    public static boolean isGivenOlig(byte[] sequence, int startPosision, byte[] olig)
    {
        for( int i = 0; i < olig.length; i++ )
            if( olig[i] != sequence[startPosision + i] ) return false;
        return true;
    }
    
    public static class ScoredSequenceFragment
    {
        double score;
        byte[] sequenceFragment;
        
        public ScoredSequenceFragment(double score, byte[] sequenceFragment)
        {
            this.score = score;
            this.sequenceFragment = sequenceFragment;
        }
        
        public double getScore()
        {
            return score;
        }
        
        public byte[] getSequenceFragment()
        {
            return sequenceFragment;
        }
        
        public static void sortScoredSequences(List<ScoredSequenceFragment> scoredSequenceFragments)
        {
            Collections.sort(scoredSequenceFragments, Comparator.comparingDouble(ScoredSequenceFragment::getScore));
        }
        
        public static List<byte[]> getSubsample(List<ScoredSequenceFragment> scoredSequenceFragments, int firstIndex, int size)
        {
            if( firstIndex + size > scoredSequenceFragments.size()) return null;
            return IntStreamEx.range(size).mapToObj(i -> scoredSequenceFragments.get(i + firstIndex).getSequenceFragment()).nonNull().toList();
        }
    }
}
