package ru.biosoft.bsa.align._test;

import static ru.biosoft.bsa.align.Alignment.Element.G;
import static ru.biosoft.bsa.align.Alignment.Element.I;
import static ru.biosoft.bsa.align.Alignment.Element.M;

import java.nio.charset.StandardCharsets;

import junit.framework.TestCase;
import ru.biosoft.bsa.align.Alignment;
import ru.biosoft.bsa.align.Alignment.Element;
import ru.biosoft.bsa.align.RightEndFreeGap;
import ru.biosoft.bsa.align.ScoringScheme;
import ru.biosoft.bsa.align.SemiGlobalScoringScheme;
import ru.biosoft.bsa.align.SimpleScoringScheme;



public class AlignmentTest extends TestCase
{
    public void testToString()
    {
        Alignment alignment = new Alignment( dna( "ACGTAAT" ), dna( "AGGAAT" ), new Element[] {M, M, M, I, M, M, M} );
        String str = alignment.toString();
        assertEquals( "ACGTAAT\n"
                    + "| | |||\n"
                    + "AGG-AAT", str );
    }

    public void testSimpleAlignmentScore()
    {
        Alignment alignment = new Alignment( dna( "ACGTAAT" ), dna( "AGGAAT" ), new Element[] {M, M, M, I, M, M, M} );
        ScoringScheme scoringScheme = new SimpleScoringScheme();
        assertEquals( -2.0, alignment.getScore( scoringScheme ) );
    }
    
    public void testSemiGlobalAlignmentScore()
    {
        //CCAAGT-CAAGTCGG----
        //----GTTCAAATCGGGCTT
        Alignment alignment = new Alignment(dna( "CCAAGTCAAGTCGG" ), dna( "GTTCAAATCGGGCTT" ), new Element[] {I, I, I, I, M, M, G, M, M, M, M, M, M, M, M, G, G, G, G});
        assertEquals(-10.0, alignment.getScore( new SimpleScoringScheme() ));
        assertEquals(7.0, alignment.getScore( new SemiGlobalScoringScheme() ));
        
        /*
        32020123230202222221322330201030313112312
              ||  |   |                |||
        ------2300013023.3000101.3010.10310021.00
        */
        byte[] seq1 = dna("32020123230202222221322330201030313112312");
        byte[] seq2 = dna("2300013023.3000101.3010.10310021.00");
        alignment = new Alignment(seq1, seq2, new Element[] {I,I,I,I,I,I,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M});
        assertEquals(-21.0, alignment.getScore( new SemiGlobalScoringScheme() ) );
        
        //30010310310130312122102330201030313112312
        //|||||||||| |||||||||||||||||||||  | | |
        //3001031031.130312122102330201030--3-1-3--
        alignment = new Alignment( dna("30010310310130312122102330201030313112312"), dna("3001031031.130312122102330201030313"),
                new Element[] {M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,I,I,M,I,M,I,M,I,I} );
        assertEquals(29.0, alignment.getScore( new SemiGlobalScoringScheme() ) );
    }
    
    public void testRightEndFreeGap()
    {
        //0200212221002003010211121330201030313112312
        //|||||||||| ||||||||||||||||||||||||
        //0200212221.020030102111213302010303--------
        Alignment alignment = new Alignment( dna("0200212221002003010211121330201030313112312"), dna("0200212221.020030102111213302010303"),
                new Element[] {M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,M,I,I,I,I,I,I,I,I} );
        assertEquals(33.0, alignment.getScore( new RightEndFreeGap() ) );
        
        //2012103103320-20332310330330201-030313112-312
        //||| | || || | |||||  |  | ||| | ||| | |||  |
        //201-1231-33-0.20332--0--02302-12030-1-1120011
        alignment = new Alignment(dna("201210310332020332310330330201030313112312"), dna("2011231330.203320023021203011120011"),
                new Element[] {M,M,M,I,M,M,M,M,I,M,M,I,M,G,M,M,M,M,M,I,I,M,I,I,M,M,M,M,M,I,M,G,M,M,M,I,M,I,M,M,M,G,M,M,M});
        assertEquals( 11.0, alignment.getScore( new RightEndFreeGap() ) );
    }
    
    public void testEquals()
    {
        Alignment first = new Alignment( dna( "ACGTAAT" ), dna( "AGGAAT" ), new Element[] {M, M, M, I, M, M, M} );
        Alignment sameAsFirst = new Alignment( dna( "ACGTAAT" ), dna( "AGGAAT" ), new Element[] {M, M, M, I, M, M, M} );
        Alignment second = new Alignment( dna( "ACGTAAT" ), dna( "GGTAAT" ), new Element[] {I, M, M, M, M, M, M} );
        assertEquals( first, sameAsFirst );
        assertFalse( first.equals( second ) );
    }

    public static byte[] dna(String sequence)
    {
        return sequence.toUpperCase().getBytes(StandardCharsets.ISO_8859_1);
    }
}
