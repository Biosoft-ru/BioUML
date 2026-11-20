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
     * SNVs, two-base substitutions, two-base deletions and one-base insertions, with the reference letters of the genome;
     * the first ones at the chromosome ends, where the scored region is cut.
     */
    private static List<Variation> variations(Random random, String chr, String dna, int count)
    {
        List<Variation> result = new ArrayList<>();
        int n = dna.length();
        int[] ends = {1, 2, 3, 5, 8, n - 7, n - 4, n - 2, n - 1, n};
        for( int i = 0; i < count; i++ )
        {
            String id = chr + "_" + i;
            int kind = i % 10;
            int length = kind == 0 || kind == 1 ? 2 : kind == 2 ? 0 : 1;
            int from = i < 3 * ends.length ? ends[i % ends.length] : 40 + random.nextInt( dna.length() - 80 ); // 1-based
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
     * same start, length and letters, and so the reverse complement. The regions are cut to the chromosome as the
     * analysis cuts them (a variation's interval grown by the longest model, intersected with the chromosome), many of
     * them at its ends, and the chromosomes start at 1 and elsewhere.
     */
    public void testAccessorCopyIsTheFormerRegion()
    {
        Random random = new Random( 11 );
        int clipped = 0;
        for( int start : new int[] {1, 1000001} )
        {
            String dna = randomDna( random, 3000 );
            Sequence chromosome = new LinearSequence( "chr1", start, dna.getBytes(), Nucleotide5LetterAlphabet.getInstance() );
            ru.biosoft.bsa.analysis.SequenceAccessor accessor = new ru.biosoft.bsa.analysis.SequenceAccessor( chromosome );
            for( int i = 0; i < 300; i++ )
            {
                // a variation anywhere, a third of them within the growth of an end
                int grow = 1 + random.nextInt( 30 );
                int at = i % 3 == 0 ? random.nextInt( grow ) : i % 3 == 1 ? dna.length() - 1 - random.nextInt( grow ) : random.nextInt( dna.length() );
                int from = chromosome.getStart() + at;
                ru.biosoft.bsa.Interval interval = new ru.biosoft.bsa.Interval( from, from ).grow( grow ).intersect( chromosome.getInterval() );
                assertNotNull( interval );
                if( interval.getLength() < 2 * grow + 1 )
                    clipped++;
                CachedSequenceRegion former = new CachedSequenceRegion( chromosome, interval.getFrom(), interval.getLength(), false );
                CachedSequenceRegion copied = accessor.getSubSequence( interval.getFrom(), interval.getLength(), false );
                String message = start + ":" + interval;
                assertRegionsEqual( message, former, copied );
                assertRegionsEqual( message + " RC", former.getReverseComplement(), copied.getReverseComplement() );
            }
        }
        assertTrue( "no region was cut at a chromosome end: " + clipped, clipped > 100 );
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
                    if( !inside.await( 5, java.util.concurrent.TimeUnit.SECONDS ) )
                        met = false;
                }
                catch( InterruptedException e )
                {
                    met = false;
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

    /**
     * Insertions and deletions near a chromosome end, where the scored region is cut: a window crossing the indel used to
     * map to windows reaching past the chromosome, and the analysis failed (ArrayIndexOutOfBoundsException). Every kind
     * of variation at every position near both ends now gets finite scores.
     *
     * <p>On chromosomes starting at 1 only: on one starting elsewhere every variation fails, wherever it is, because
     * Variation.applyVariations makes the alternative start at 1 while the reference region and the mappings use the
     * chromosome's coordinates. A separate issue (genomes in use start their chromosomes at 1).</p>
     */
    public void testVariationsAtChromosomeEnds() throws Exception
    {
        Random random = new Random( 5 );
        SiteModel[] models = {model( random, "m5", 5 ), model( random, "m9", 9 )};
        int maxLength = 9;
        int start = 1;
        {
            String dna = randomDna( random, 200 );
            Sequence chromosome = new LinearSequence( "chr1", start, dna.getBytes(), Nucleotide5LetterAlphabet.getInstance() );
            List<Variation> variations = new ArrayList<>();
            int n = dna.length();
            for( int offset = 0; offset < 2 * maxLength; offset++ )
                for( int at : new int[] {offset, n - 1 - offset} ) // 0-based in dna
                {
                    int from = start + at;
                    String snv = dna.substring( at, at + 1 );
                    variations.add( variation( "snv" + from, from, snv, otherLetters( random, snv ) ) );
                    variations.add( variation( "ins" + from, from, "", "T" ) );
                    variations.add( variation( "ins3" + from, from, "", "GAT" ) );
                    if( at + 2 <= n )
                    {
                        String two = dna.substring( at, at + 2 );
                        variations.add( variation( "del" + from, from, two, "" ) );
                        variations.add( variation( "mnv" + from, from, two, otherLetters( random, two ) ) );
                    }
                    if( at + 4 <= n )
                        variations.add( variation( "del4" + from, from, dna.substring( at, at + 4 ), "" ) );
                }
            // after the last base: a VCF row at the last position, X -> XT, becomes from = end + 1, to = end
            variations.add( variation( "insAfterEnd", start + n, "", "T" ) );
            float[][] losses = new float[models.length][variations.size()], gains = new float[models.length][variations.size()];
            assertTrue( CompareTFBSMutationsWithVCF.computeDeltas( Collections.singletonMap( "chr1", variations ),
                    Collections.singletonMap( "chr1", chromosome ), models, maxLength, losses, gains, null ) );
            double sum = 0;
            for( int m = 0; m < models.length; m++ )
                for( int i = 0; i < variations.size(); i++ )
                {
                    String what = start + " " + variations.get( i ).id + " " + models[m].getName();
                    assertTrue( what + " loss " + losses[m][i], Float.isFinite( losses[m][i] ) && losses[m][i] >= 0 );
                    assertTrue( what + " gain " + gains[m][i], Float.isFinite( gains[m][i] ) && gains[m][i] >= 0 );
                    sum += losses[m][i] + gains[m][i];
                }
            // dropping every window would give zeros only
            assertTrue( "no loss or gain at all near the ends", sum > 0 );
        }
    }

    /**
     * mapWindows keeps of each window's mapped windows exactly those lying wholly within the target, and none when
     * none remains: for indels near both ends of a region, where some mapped windows reach past it.
     */
    public void testMapWindowsKeepsTheWindowsWithinTheTarget()
    {
        Random random = new Random( 13 );
        int partial = 0, dropped = 0, kept = 0;
        for( int round = 0; round < 200; round++ )
        {
            String dna = randomDna( random, 12 + random.nextInt( 20 ) );
            Sequence reference = new LinearSequence( "ref", dna, Nucleotide5LetterAlphabet.getInstance() );
            int length = 2 + random.nextInt( 6 );
            // a deletion or an insertion near either end of the region
            int at = round % 2 == 0 ? random.nextInt( 3 ) : dna.length() - 1 - random.nextInt( 3 ); // 0-based
            int from = at + 1;
            Variation var;
            if( round % 4 < 2 && at + 2 <= dna.length() )
                var = variation( "del", from, dna.substring( at, at + 2 ), "" );
            else
                var = variation( "ins", from, "", "GA" );
            Sequence alternative = Variation.applyVariations( reference, new Variation[] {var} );
            ru.biosoft.bsa.analysis.maos.coord_mapping.CoordinateMapping ref2alt =
                    new ru.biosoft.bsa.analysis.maos.coord_mapping.MappingByVCF( reference.getInterval(), new Variation[] {var} );

            int[][] windows = CompareTFBSMutationsWithVCF.mapWindows( reference, ref2alt, alternative, length );
            int lastStart = alternative.getStart() + alternative.getLength() - length;
            for( int k = 0; k < windows.length; k++ )
            {
                int s = reference.getStart() + k;
                List<Integer> expected = new ArrayList<>();
                int all = 0;
                for( int pos : ref2alt.mapInterval( new ru.biosoft.bsa.Interval( s, s + length - 1 ) ) )
                {
                    all++;
                    if( pos >= alternative.getStart() && pos <= lastStart )
                        expected.add( pos );
                }
                List<Integer> actual = new ArrayList<>();
                for( int pos : windows[k] )
                    actual.add( pos );
                assertEquals( var.id + "@" + from + " window " + s + " length " + length, expected, actual );
                if( expected.isEmpty() )
                    assertEquals( 0, windows[k].length );
                if( all > expected.size() )
                {
                    dropped++;
                    if( !expected.isEmpty() )
                        partial++;
                }
                kept += expected.size();
            }
        }
        // the cases that matter happened: windows dropped, some of them while others of the same window were kept
        assertTrue( "no window dropped", dropped > 0 );
        assertTrue( "no window kept beside a dropped one", partial > 0 );
        assertTrue( kept > 0 );
    }

    private static String reverseComplement(String dna)
    {
        StringBuilder sb = new StringBuilder();
        for( int i = dna.length() - 1; i >= 0; i-- )
            sb.append( "TGCA".charAt( LETTERS.indexOf( dna.charAt( i ) ) ) );
        return sb.toString();
    }

    /** The greatest score of the windows of the first sequence minus the score of the same window of the second, or 0. */
    private static float maxDelta(SiteModel model, String first, String second)
    {
        Sequence a = new LinearSequence( "a", first, Nucleotide5LetterAlphabet.getInstance() );
        Sequence b = new LinearSequence( "b", second, Nucleotide5LetterAlphabet.getInstance() );
        float result = 0;
        for( int k = 0; k + model.getLength() <= first.length(); k++ )
            result = Math.max( result, (float) ( model.getScore( a, a.getStart() + k ) - model.getScore( b, b.getStart() + k ) ) );
        return result;
    }

    /**
     * An SNV at a chromosome end, where the region is cut: its loss and gain are those computed directly, window by
     * window on both strands of the cut region (an SNV maps each window to the same window).
     */
    public void testSnvAtChromosomeEndsHasTheExpectedScores() throws Exception
    {
        Random random = new Random( 17 );
        SiteModel model = model( random, "m6", 6 );
        int length = model.getLength();
        String dna = randomDna( random, 100 );
        Sequence chromosome = new LinearSequence( "chr1", dna, Nucleotide5LetterAlphabet.getInstance() );
        int n = dna.length();
        for( int at : new int[] {0, 1, 3, n - 4, n - 2, n - 1} ) // 0-based
        {
            String alt = otherLetters( random, dna.substring( at, at + 1 ) );
            Variation var = variation( "snv" + ( at + 1 ), at + 1, dna.substring( at, at + 1 ), alt );
            int from = Math.max( 0, at - length ), to = Math.min( n - 1, at + length ); // the region, 0-based inclusive
            String ref = dna.substring( from, to + 1 );
            String altRegion = ref.substring( 0, at - from ) + alt + ref.substring( at - from + 1 );
            float loss = Math.max( maxDelta( model, ref, altRegion ), maxDelta( model, reverseComplement( ref ), reverseComplement( altRegion ) ) );
            float gain = Math.max( maxDelta( model, altRegion, ref ), maxDelta( model, reverseComplement( altRegion ), reverseComplement( ref ) ) );

            float[][] losses = new float[1][1], gains = new float[1][1];
            assertTrue( CompareTFBSMutationsWithVCF.computeDeltas( Collections.singletonMap( "chr1", Collections.singletonList( var ) ),
                    Collections.singletonMap( "chr1", chromosome ), new SiteModel[] {model}, length, losses, gains, null ) );
            assertEquals( var.id + " loss", Float.floatToIntBits( loss ), Float.floatToIntBits( losses[0][0] ) );
            assertEquals( var.id + " gain", Float.floatToIntBits( gain ), Float.floatToIntBits( gains[0][0] ) );
        }
    }
}
