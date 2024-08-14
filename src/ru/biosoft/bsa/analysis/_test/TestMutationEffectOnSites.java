package ru.biosoft.bsa.analysis._test;

import java.util.Arrays;

import junit.framework.TestCase;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.analysis.maos.Variation;

public class TestMutationEffectOnSites extends TestCase
{
    public void testInvertVariations1()
    {
        Variation[] vars = new Variation[1];
        vars[0] = new Variation( "1", "", 10, 10, "C".getBytes(), "G".getBytes() );
        Variation[] inv = Variation.invertVariations( vars );
        assertEquals( 1, inv.length );
        assertEquals(10, inv[0].getFrom());
        assertEquals(10, inv[0].getTo());
        assertTrue(Arrays.equals( "C".getBytes(), inv[0].alt));
        assertTrue(Arrays.equals( "G".getBytes(), inv[0].ref));
    }
    
    public void testInvertVariations2()
    {
        Variation[] vars = new Variation[2];
        vars[0] = new Variation( "1", "", 10, 10, "C".getBytes(), "G".getBytes() );
        vars[1] = new Variation( "2", "", 11, 11, "A".getBytes(), "T".getBytes() );
        Variation[] inv = Variation.invertVariations( vars );
        assertEquals( 2, inv.length );

        assertEquals(10, inv[0].getFrom());
        assertEquals(10, inv[0].getTo());
        assertTrue(Arrays.equals( "G".getBytes(), inv[0].ref));
        assertTrue(Arrays.equals( "C".getBytes(), inv[0].alt));
        
        assertEquals(11, inv[1].getFrom());
        assertEquals(11, inv[1].getTo());
        assertTrue(Arrays.equals( "T".getBytes(), inv[1].ref));
        assertTrue(Arrays.equals( "A".getBytes(), inv[1].alt));
    }
    
    public void testInvertVariations3()
    {
        Variation[] vars = new Variation[2];
        vars[0] = new Variation( "1", "", 11, 10, "".getBytes(), "G".getBytes() );//insertion of G between 10 and 11
        vars[1] = new Variation( "2", "", 11, 11, "A".getBytes(), "T".getBytes() );
        Variation[] inv = Variation.invertVariations( vars );
        assertEquals( 2, inv.length );

        assertEquals(11, inv[0].getFrom());
        assertEquals(11, inv[0].getTo());
        assertTrue(Arrays.equals( "G".getBytes(), inv[0].ref));
        assertTrue(Arrays.equals( "".getBytes(), inv[0].alt));
        
        assertEquals(12, inv[1].getFrom());
        assertEquals(12, inv[1].getTo());
        assertTrue(Arrays.equals( "T".getBytes(), inv[1].ref));
        assertTrue(Arrays.equals( "A".getBytes(), inv[1].alt));
    }
    
    public void testMapToRC()
    {
        //ref: ..A.-......
        //alt: ..T.G......
        Variation[] vars = new Variation[2];
        vars[0] = new Variation( "1", "", 3, 3, "A".getBytes(), "T".getBytes() );
        vars[1] = new Variation( "2", "", 5, 4, "".getBytes(), "G".getBytes() );//insertion of G between 4 and 5
        
        Variation[] rc = Variation.mapToRC( new Interval(1,10), vars, 1, Nucleotide5LetterAlphabet.getInstance() );
        // refrc: ......-.T..
        // altrc: ......C.A..
        assertEquals( 2, rc.length );
        
        assertEquals( 7, rc[0].getFrom() );
        assertEquals( 6, rc[0].getTo() );
        assertEquals( 0, rc[0].ref.length );
        assertTrue( Arrays.equals( "C".getBytes(), rc[0].alt ) );
        
        assertEquals( 8, rc[1].getFrom() );
        assertEquals( 8, rc[1].getTo() );
        assertTrue( Arrays.equals( "T".getBytes(), rc[1].ref ) );
        assertTrue( Arrays.equals( "A".getBytes(), rc[1].alt ) );
    }
}
