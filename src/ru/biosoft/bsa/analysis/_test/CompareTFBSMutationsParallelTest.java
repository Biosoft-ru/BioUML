package ru.biosoft.bsa.analysis._test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.CompareTFBSMutationsWithVCF;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.MatchSiteModel;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.bsa.analysis.maos.Variation;

/**
 * The site gain/loss scores of {@link CompareTFBSMutationsWithVCF}, computed in parallel: bit-identical to the former
 * sequential loop (the same variations in the same order on one thread), and to scoring each variation alone, with
 * weight matrix, MATCH and IPS models (the IPS one keeps per-call state in ThreadLocals) used by several threads at once.
 */
public class CompareTFBSMutationsParallelTest extends AbstractBioUMLTest
{
    private static final String LETTERS = "ACGT";
    private static final Set<Thread> scoringThreads = ConcurrentHashMap.newKeySet();
    /** Calls to one recording model in progress at once, and the most seen: above 1, the model is shared concurrently. */
    private static final java.util.concurrent.atomic.AtomicInteger active = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger maxActive = new java.util.concurrent.atomic.AtomicInteger();

    private Preferences saved;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        saved = Application.getPreferences();
        threads( 4 );
        scoringThreads.clear();
        maxActive.set( 0 );
    }

    private static void threads(int n)
    {
        Preferences preferences = new Preferences();
        preferences.add( new DynamicProperty( "threadsNumber", Integer.class, n ) );
        Application.setPreferences( preferences );
        // the pool takes the new size at its next lookup
        ru.biosoft.access.task.TaskPool.getInstance().getAvailableThreadsCount();
    }

    @Override
    protected void tearDown() throws Exception
    {
        Application.setPreferences( saved );
        super.tearDown();
    }

    /** A weight matrix model that records the threads scoring with it. */
    private static class RecordingModel extends WeightMatrixModel
    {
        RecordingModel(String name, FrequencyMatrix matrix)
        {
            super( name, null, matrix, 0.0 );
        }

        @Override
        public double getScore(Sequence sequence, int position)
        {
            scoringThreads.add( Thread.currentThread() );
            maxActive.accumulateAndGet( active.incrementAndGet(), Math::max );
            try
            {
                return super.getScore( sequence, position );
            }
            finally
            {
                active.decrementAndGet();
            }
        }
    }

    private static FrequencyMatrix matrix(Random random, String name, int length)
    {
        double[][] weights = new double[length][4];
        for( double[] row : weights )
        {
            double sum = 0;
            for( int j = 0; j < 4; j++ )
                sum += row[j] = 0.05 + random.nextDouble();
            for( int j = 0; j < 4; j++ )
                row[j] /= sum;
        }
        return new FrequencyMatrix( null, name, Nucleotide5LetterAlphabet.getInstance(), null, weights, false );
    }

    private static SiteModel model(Random random, String name, int length)
    {
        double[][] weights = new double[length][4];
        for( double[] row : weights )
        {
            double sum = 0;
            for( int j = 0; j < 4; j++ )
                sum += row[j] = 0.05 + random.nextDouble();
            for( int j = 0; j < 4; j++ )
                row[j] /= sum;
        }
        FrequencyMatrix matrix = new FrequencyMatrix( null, name, Nucleotide5LetterAlphabet.getInstance(), null, weights, false );
        return new RecordingModel( name, matrix );
    }

    private static String randomDna(Random random, int length)
    {
        StringBuilder sb = new StringBuilder( length );
        for( int i = 0; i < length; i++ )
            sb.append( LETTERS.charAt( random.nextInt( 4 ) ) );
        return sb.toString();
    }

    private static Variation variation(String id, int from, String ref, String alt)
    {
        // as Variation.createFromSite makes them: to = from + ref length - 1, so an insertion has to = from - 1
        return new Variation( id, id, from, from + ref.length() - 1, ref.getBytes(), alt.getBytes() );
    }

    private static String otherLetters(Random random, String ref)
    {
        StringBuilder alt = new StringBuilder();
        for( char c : ref.toCharArray() )
            alt.append( LETTERS.charAt( ( LETTERS.indexOf( c ) + 1 + random.nextInt( 3 ) ) % 4 ) );
        return alt.toString();
    }

    /**
     * SNVs, two-base substitutions, two-base deletions and one-base insertions, with the reference letters of the genome,
     * at least 40 bases from the chromosome ends. Closer to an end, where the scored region is cut, the analysis fails
     * (ArrayIndexOutOfBoundsException in maxSiteLoss, scoring the alternative sequence outside it), sequential or not:
     * a separate bug, not covered here.
     */
    private static List<Variation> variations(Random random, String chr, String dna, int count)
    {
        List<Variation> result = new ArrayList<>();
        for( int i = 0; i < count; i++ )
        {
            String id = chr + "_" + i;
            int kind = i % 10;
            int length = kind == 0 || kind == 1 ? 2 : kind == 2 ? 0 : 1;
            int from = 40 + random.nextInt( dna.length() - 80 ); // 1-based
            if( from + length - 1 > dna.length() )
                length = 1;
            String ref = dna.substring( from - 1, from - 1 + length );
            if( kind == 1 )
                result.add( variation( id, from, ref, "" ) ); // deletion
            else if( length == 0 )
                result.add( variation( id, from, "", String.valueOf( LETTERS.charAt( random.nextInt( 4 ) ) ) ) ); // insertion
            else
                result.add( variation( id, from, ref, otherLetters( random, ref ) ) );
        }
        return result;
    }

    public void testParallelScoresEqualScoringEachAlone() throws Exception
    {
        Random random = new Random( 7 );
        WeightMatrixModel[] ipsMatrices = {new WeightMatrixModel( "i1", null, matrix( random, "i1", 6 ), 0.0 ),
                new WeightMatrixModel( "i2", null, matrix( random, "i2", 8 ), 0.0 )};
        SiteModel[] models = {model( random, "m5", 5 ), model( random, "m8", 8 ), model( random, "m10", 10 ), model( random, "m6", 6 ),
                new MatchSiteModel( "match", null, matrix( random, "match", 9 ), 0.0, 0.0 ),
                new IPSSiteModel( "ips", null, ipsMatrices, 0.0, 1, 16 )};
        int maxLength = 0;
        for( SiteModel model : models )
            maxLength = Math.max( maxLength, model.getLength() );

        Map<String, Sequence> chromosomes = new LinkedHashMap<>();
        Map<String, List<Variation>> variationsByChr = new LinkedHashMap<>();
        for( String chr : new String[] {"chr1", "chr2"} )
        {
            String dna = randomDna( random, 4000 );
            chromosomes.put( chr, new LinearSequence( chr, dna, Nucleotide5LetterAlphabet.getInstance() ) );
            variationsByChr.put( chr, variations( random, chr, dna, 300 ) );
        }
        int total = variationsByChr.values().stream().mapToInt( List::size ).sum();

        float[][] losses = new float[models.length][total], gains = new float[models.length][total];
        assertTrue( CompareTFBSMutationsWithVCF.computeDeltas( variationsByChr, chromosomes, models, maxLength, losses, gains, null ) );
        assertTrue( "scored on one thread only: " + scoringThreads.size(), scoringThreads.size() > 1 );
        assertTrue( "a model was never scored by two threads at once", maxActive.get() > 1 );

        // the former sequential loop: one thread, the variations in the map's order
        threads( 1 );
        scoringThreads.clear();
        float[][] seqLosses = new float[models.length][total], seqGains = new float[models.length][total];
        assertTrue( CompareTFBSMutationsWithVCF.computeDeltas( variationsByChr, chromosomes, models, maxLength, seqLosses, seqGains, null ) );
        assertEquals( "the sequential run used one thread", 1, scoringThreads.size() );
        for( int m = 0; m < models.length; m++ )
        {
            assertTrue( models[m].getName() + " losses", java.util.Arrays.equals( seqLosses[m], losses[m] ) );
            assertTrue( models[m].getName() + " gains", java.util.Arrays.equals( seqGains[m], gains[m] ) );
        }
        boolean anyScore = false;
        for( float[] row : losses )
            for( float v : row )
                anyScore |= v != 0;
        assertTrue( "no loss scored at all: the test would prove nothing", anyScore );
        threads( 4 );

        // each variation alone, in the order of the map: the same scores, at the same index
        int index = 0;
        for( Map.Entry<String, List<Variation>> e : variationsByChr.entrySet() )
        {
            for( Variation var : e.getValue() )
            {
                float[][] oneLoss = new float[models.length][1], oneGain = new float[models.length][1];
                Map<String, List<Variation>> one = Collections.singletonMap( e.getKey(), Collections.singletonList( var ) );
                assertTrue( CompareTFBSMutationsWithVCF.computeDeltas( one, chromosomes, models, maxLength, oneLoss, oneGain, null ) );
                for( int m = 0; m < models.length; m++ )
                {
                    assertEquals( var.id + " loss " + m, Float.floatToIntBits( oneLoss[m][0] ), Float.floatToIntBits( losses[m][index] ) );
                    assertEquals( var.id + " gain " + m, Float.floatToIntBits( oneGain[m][0] ), Float.floatToIntBits( gains[m][index] ) );
                }
                index++;
            }
        }
        assertEquals( total, index );

        // and a second parallel run gives the same
        float[][] losses2 = new float[models.length][total], gains2 = new float[models.length][total];
        assertTrue( CompareTFBSMutationsWithVCF.computeDeltas( variationsByChr, chromosomes, models, maxLength, losses2, gains2, null ) );
        for( int m = 0; m < models.length; m++ )
        {
            assertTrue( java.util.Arrays.equals( losses[m], losses2[m] ) );
            assertTrue( java.util.Arrays.equals( gains[m], gains2[m] ) );
        }
    }
}
