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
import ru.biosoft.bsa.analysis.SequenceAccessor.CachedSequenceRegion;
import ru.biosoft.bsa.analysis.maos.Variation;

/**
 * The site gain/loss scores of {@link CompareTFBSMutationsWithVCF}, computed in parallel. Against the former
 * sequential loop, two things changed: the loop (now TaskPool.iterate) and how the reference region is copied (now
 * through a SequenceAccessor). The scores computed in parallel are bit-identical to those of one thread in the former
 * order and to each variation scored alone, with weight matrix, MATCH and IPS models (the IPS one keeps per-call state
 * in ThreadLocals); the accessor's copy is the former region exactly; and one model is entered by two threads at once.
 */
public class CompareTFBSMutationsParallelTest extends AbstractBioUMLTest
{
    private static final String LETTERS = "ACGT";
    private static final Set<Thread> scoringThreads = ConcurrentHashMap.newKeySet();

    private Preferences saved;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        saved = Application.getPreferences();
        threads( 4 );
        scoringThreads.clear();
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
        // and the pool back to the restored threadsNumber (it takes the size at its next lookup)
        ru.biosoft.access.task.TaskPool.getInstance().getAvailableThreadsCount();
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
            return super.getScore( sequence, position );
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

    /**
     * The reference region as the accessor copies it is the region the former loop made with new CachedSequenceRegion:
     * same start, length and letters, and so the reverse complement, also where the region is cut at a chromosome end.
     */
    public void testAccessorCopyIsTheFormerRegion()
    {
        Random random = new Random( 11 );
        String dna = randomDna( random, 3000 );
        Sequence chromosome = new LinearSequence( "chr1", dna, Nucleotide5LetterAlphabet.getInstance() );
        ru.biosoft.bsa.analysis.SequenceAccessor accessor = new ru.biosoft.bsa.analysis.SequenceAccessor( chromosome );
        for( int i = 0; i < 300; i++ )
        {
            int length = 1 + random.nextInt( 60 );
            int from = chromosome.getStart() + random.nextInt( chromosome.getLength() - length + 1 );
            CachedSequenceRegion former = new CachedSequenceRegion( chromosome, from, length, false );
            CachedSequenceRegion copied = accessor.getSubSequence( from, length, false );
            assertRegionsEqual( from + "+" + length, former, copied );
            assertRegionsEqual( from + "+" + length + " RC", former.getReverseComplement(), copied.getReverseComplement() );
        }
    }

    private static void assertRegionsEqual(String message, Sequence expected, Sequence actual)
    {
        assertEquals( message, expected.getStart(), actual.getStart() );
        assertEquals( message, expected.getLength(), actual.getLength() );
        for( int pos = expected.getStart(); pos < expected.getStart() + expected.getLength(); pos++ )
            assertEquals( message + " @" + pos, expected.getLetterAt( pos ), actual.getLetterAt( pos ) );
    }

    /** A model that holds its first caller in each thread until a second thread is inside the model too. */
    private static class MeetingModel extends WeightMatrixModel
    {
        final java.util.concurrent.CountDownLatch inside = new java.util.concurrent.CountDownLatch( 2 );
        final ThreadLocal<Boolean> entered = ThreadLocal.withInitial( () -> false );
        volatile boolean met = true;

        MeetingModel(FrequencyMatrix matrix)
        {
            super( "meeting", null, matrix, 0.0 );
        }

        @Override
        public double getScore(Sequence sequence, int position)
        {
            if( !entered.get() )
            {
                entered.set( true );
                inside.countDown();
                try
                {
                    // returns at once when two threads are in this method of this instance; false: no second came
                    if( !inside.await( 20, java.util.concurrent.TimeUnit.SECONDS ) )
                        met = false;
                }
                catch( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                }
            }
            return super.getScore( sequence, position );
        }
    }

    /**
     * The variations are scored on several threads at once, with the same model instance: a latch inside the model
     * makes the first thread wait for a second one (not a timing measurement: it passes once two threads score).
     */
    public void testOneModelIsScoredByTwoThreadsAtOnce() throws Exception
    {
        Random random = new Random( 3 );
        MeetingModel model = new MeetingModel( matrix( random, "meeting", 6 ) );
        String dna = randomDna( random, 2000 );
        Map<String, Sequence> chromosomes = Collections.singletonMap( "chr1",
                new LinearSequence( "chr1", dna, Nucleotide5LetterAlphabet.getInstance() ) );
        Map<String, List<Variation>> variationsByChr = Collections.singletonMap( "chr1", variations( random, "chr1", dna, 50 ) );
        float[][] losses = new float[1][50], gains = new float[1][50];
        assertTrue( CompareTFBSMutationsWithVCF.computeDeltas( variationsByChr, chromosomes, new SiteModel[] {model}, model.getLength(),
                losses, gains, null ) );
        assertEquals( "two threads were never inside the model at once", 0, model.inside.getCount() );
        assertTrue( model.met );
    }
}
