package ru.biosoft.bsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import ru.biosoft.bsa.BAMTrack.SitesCollection.BAMSite;
import ru.biosoft.util.Pair;

public class PileupElement
{
    private final Alphabet alphabet;

    private final double[] codeCounts;
    private int letterCount;

    private int deletionCount;

    private final Map<String, Integer> insertions = new HashMap<>();
    private int insertionCount;


    public PileupElement(Alphabet alphabet)
    {
        this.alphabet = alphabet;
        codeCounts = new double[alphabet.basicSize()];
    }

    public Alphabet getAlphabet()
    {
        return alphabet;
    }

    public double getCodeCount(byte code)
    {
        return codeCounts[code];
    }
    
    public double[] getCodeCounts()
    {
        return codeCounts;
    }

    /**
     * Most present letter or -1 if this pileup is empty
     */
    public byte getMostPresentCode()
    {
        double bestCount = 0;
        byte bestCode = -1;
        for( byte i = 0; i < codeCounts.length; i++ )
            if( codeCounts[i] > bestCount )
            {
                bestCode = i;
                bestCount = codeCounts[bestCode];
            }
        return bestCode;
    }
    
    /**
     * Array of all letters ordered by there counts in this pileup (Most present come first).
     */
    public byte[] getCodesOrderByCount()
    {
        Byte[] codes = ArrayUtils.toObject(alphabet.basicCodes());
        
        Arrays.sort( codes, (l1, l2) -> {
            int result = Double.compare( getCodeCount( l2 ), getCodeCount( l1 ) );
            if( result != 0) return result;
            return l2 - l1;
        } );

        return ArrayUtils.toPrimitive(codes);
    }
    
    public void addLetter(byte letter)
    {
        byte[] basicCodes = alphabet.basicCodes( alphabet.letterToCodeMatrix()[letter] );
        for( byte basicCode : basicCodes )
            codeCounts[basicCode] += ( 1.0 / basicCodes.length );
        letterCount++;
    }

    public int getDeletionCount()
    {
        return deletionCount;
    }

    public void addDeletion()
    {
        deletionCount++;
    }
    
    public byte getMostPresentLetterOrDeletion()
    {
        byte mostPresentCode = getMostPresentCode();
        if(deletionCount > 0 && (mostPresentCode == -1 || deletionCount > getCodeCount( mostPresentCode )))
            return '-';
        return mostPresentCode == -1 ? -1 : alphabet.codeToLetterMatrix()[mostPresentCode];
    }

    /**
     * The number of sequences overlapping this position
     */
    public int getSize()
    {
        return letterCount + deletionCount;
    }

    /**
     * The number of sequences with insertions just before this position
     */
    public int getInsertionCount()
    {
        return insertionCount;
    }

    /**
     * Insertions just before this position
     */
    public Set<String> getInsertions()
    {
        return insertions.keySet();
    }

    public int getInsertionCount(String insertion)
    {
        Integer result = insertions.get( insertion );
        return result == null ? 0 : result;
    }

    public String getMostPresentInsertion()
    {
        return EntryStream.of(insertions).max(Comparator.comparing(Entry::getValue)).map(Entry::getKey).orElse(null);
    }

    public void addInsertion(String insertion)
    {
        Integer prev = insertions.get( insertion );
        if( prev == null )
            prev = 0;
        insertions.put( insertion, prev + 1 );
        insertionCount++;
    }

    public String getDescription()
    {
        List<Pair<String, Double>> sorted = new ArrayList<>();
        for( byte code : getAlphabet().basicCodes() )
            if( getCodeCount( code ) > 0 )
                sorted.add( new Pair<>( getAlphabet().codeToLetters(code).toUpperCase(), getCodeCount( code ) ) );
        if( getDeletionCount() > 0 )
            sorted.add( new Pair<>( "-", (double)getDeletionCount() ) );
        Collections.sort( sorted, Comparator
                .comparingDouble( (Pair<String, Double> p) -> p.getSecond() ).reversed()
                .thenComparing( (Pair<String, Double> p) -> p.getFirst() ) );
        try (Formatter formatter = new Formatter())
        {
            for( Pair<String, Double> e : sorted )
                formatter.format("%s:%.2f ", e.getFirst(), e.getSecond());
            formatter.format("Total:%d", getSize());
            return formatter.toString();
        }
    }

    public String getInsertionDescription()
    {
        return EntryStream.of( insertions ).sortedBy( Entry::getValue )
            .mapKeys( String::toUpperCase ).join( ":" )
            .joining( " ", "Insertions-", " Total:" + getInsertionCount() );
    }

    public static PileupElement[] getElements(Iterable<BAMSite> alignments, int from, int to)
    {
        PileupElement[] result = StreamEx.constant( Nucleotide15LetterAlphabet.getInstance(), to - from + 1 ).map( PileupElement::new )
                .toArray( PileupElement[]::new );

        for( BAMSite site : alignments )
        {
            Sequence readSequence = site.getReadSequence();
            Cigar cigar = site.getCigar();
            int readOffset = 0;
            int refOffset = site.getFrom() - from;
            for( CigarElement e : cigar.getCigarElements() )
            {
                CigarOperator op = e.getOperator();
                switch( op )
                {
                    case M:
                    case X:
                    case EQ:
                    {
                        for( int i = 0; i < e.getLength(); i++ )
                        {
                            int pileupIndex = refOffset + i;
                            if( pileupIndex >= 0 && pileupIndex < result.length )
                            {
                                byte nucleotide = readSequence.getLetterAt(i + readOffset + 1);
                                result[pileupIndex].addLetter( nucleotide );
                            }
                        }
                        break;
                    }
                    case I:
                    {
                        int pileupIndex = refOffset;
                        if( pileupIndex >= 0 && pileupIndex < result.length )
                        {
                            String insertedSequence = readSequence.toString().substring(readOffset, readOffset+e.getLength());
                            result[pileupIndex].addInsertion( insertedSequence );
                        }
                        break;
                    }
                    case D:
                        for( int i = 0; i < e.getLength(); i++ )
                        {
                            int pileupIndex = refOffset + i;
                            if( pileupIndex >= 0 && pileupIndex < result.length )
                                result[pileupIndex].addDeletion();
                        }
                        break;
                    default:
                        break;
                }
                if( op.consumesReadBases() )
                    readOffset += e.getLength();
                if( op.consumesReferenceBases() )
                    refOffset += e.getLength();
            }
        }
        return result;
    }
}
