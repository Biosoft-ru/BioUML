package ru.biosoft.bsa.analysis._test;

import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;
import ru.biosoft.bsa.analysis.OptimizeSiteSearchAnalysis;

/**
 * The per-cutoff site counts of {@link OptimizeSiteSearchAnalysis}: upper bounds and their suffix sums give exactly the
 * counts of the former loop over the cutoffs for every site.
 */
public class OptimizeSiteSearchCountsTest extends TestCase
{
    /** The former count: each site adds one at every cutoff from the lowest up to its weight. */
    private static int[] loopCounts(int[] scores, int[] weights)
    {
        int[] counts = new int[scores.length];
        for( int weight : weights )
            for( int scoreNum = 0; scoreNum < scores.length && weight >= scores[scoreNum]; scoreNum++ )
                counts[scoreNum]++;
        return counts;
    }

    private static int[] boundCounts(int[] scores, int[] weights)
    {
        int[] hitsByBound = new int[scores.length + 1];
        for( int weight : weights )
            hitsByBound[OptimizeSiteSearchAnalysis.upperBound( scores, weight )]++;
        return Arrays.copyOf( OptimizeSiteSearchAnalysis.toCountsAtCutoffs( hitsByBound ), scores.length );
    }

    public void testUpperBound()
    {
        int[] sorted = {1, 3, 3, 3, 7};
        assertEquals( 0, OptimizeSiteSearchAnalysis.upperBound( sorted, 0 ) );
        assertEquals( 1, OptimizeSiteSearchAnalysis.upperBound( sorted, 1 ) );
        assertEquals( 1, OptimizeSiteSearchAnalysis.upperBound( sorted, 2 ) );
        assertEquals( 4, OptimizeSiteSearchAnalysis.upperBound( sorted, 3 ) );
        assertEquals( 5, OptimizeSiteSearchAnalysis.upperBound( sorted, 7 ) );
        assertEquals( 5, OptimizeSiteSearchAnalysis.upperBound( sorted, 100 ) );
        assertEquals( 0, OptimizeSiteSearchAnalysis.upperBound( new int[0], 5 ) );
    }

    public void testCountsMatchTheLoop()
    {
        Random random = new Random( 42 );
        for( int round = 0; round < 500; round++ )
        {
            // the cutoffs are the models' own site weights, sorted, with repeats; sites on promoters are some of them,
            // and below or above every cutoff too
            int[] scores = new int[random.nextInt( 60 )];
            for( int i = 0; i < scores.length; i++ )
                scores[i] = random.nextInt( 40 ) * 25;
            Arrays.sort( scores );
            // with repeats (as the counting gets them in a test of the method alone) and distinct (as the analysis has them)
            if( round % 2 == 1 )
                scores = Arrays.stream( scores ).distinct().toArray();
            int[] weights = new int[random.nextInt( 200 )];
            for( int i = 0; i < weights.length; i++ )
                weights[i] = random.nextInt( 45 ) * 25 - 50;
            assertTrue( "round " + round, Arrays.equals( loopCounts( scores, weights ), boundCounts( scores, weights ) ) );
        }
    }

    /** Window optimization only: the one cutoff 0, which every site of non-negative weight reaches. */
    public void testSingleCutoff()
    {
        int[] scores = {0};
        int[] weights = {0, 5, 100, -1};
        assertTrue( Arrays.equals( loopCounts( scores, weights ), boundCounts( scores, weights ) ) );
        assertEquals( 3, boundCounts( scores, weights )[0] );
    }
}
