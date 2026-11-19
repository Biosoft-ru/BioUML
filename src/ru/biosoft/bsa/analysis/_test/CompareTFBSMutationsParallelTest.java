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
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.bsa.analysis.maos.Variation;

/**
 * The site gain/loss scores of {@link CompareTFBSMutationsWithVCF}, computed in parallel: each variation gets exactly
 * the scores it gets when scored alone, in its own place of the result.
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
        Preferences preferences = new Preferences();
        preferences.add( new DynamicProperty( "threadsNumber", Integer.class, 4 ) );
        Application.setPreferences( preferences );
        scoringThreads.clear();
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
            return super.getScore( sequence, position );
        }
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

    /** SNVs and a few two-base substitutions, away from the chromosome ends, with the reference letters of the genome. */
    private static List<Variation> variations(Random random, String chr, String dna, int count)
    {
        List<Variation> result = new ArrayList<>();
        for( int i = 0; i < count; i++ )
        {
            int length = i % 10 == 0 ? 2 : 1;
            int from = 40 + random.nextInt( dna.length() - 80 ); // 1-based
            String ref = dna.substring( from - 1, from - 1 + length );
            StringBuilder alt = new StringBuilder();
            for( char c : ref.toCharArray() )
                alt.append( LETTERS.charAt( ( LETTERS.indexOf( c ) + 1 + random.nextInt( 3 ) ) % 4 ) );
            result.add( new Variation( chr + "_" + i, chr + "_" + i, from, from + length - 1, ref.getBytes(), alt.toString().getBytes() ) );
        }
        return result;
    }

    public void testParallelScoresEqualScoringEachAlone() throws Exception
    {
        Random random = new Random( 7 );
        SiteModel[] models = {model( random, "m5", 5 ), model( random, "m8", 8 ), model( random, "m10", 10 ), model( random, "m6", 6 )};
        int maxLength = 10;

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
