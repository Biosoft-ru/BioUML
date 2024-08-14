package ru.biosoft.bsa.align._test;

import static ru.biosoft.bsa.align.Alignment.Element.G;
import static ru.biosoft.bsa.align.Alignment.Element.I;
import static ru.biosoft.bsa.align.Alignment.Element.M;
import static ru.biosoft.bsa.align._test.AlignmentTest.dna;
import junit.framework.TestCase;
import ru.biosoft.bsa.align.Alignment;
import ru.biosoft.bsa.align.Alignment.Element;
import ru.biosoft.bsa.align.NeedlemanWunsch;
import ru.biosoft.bsa.align.RightEndFreeGap;
import ru.biosoft.bsa.align.ScoringScheme;
import ru.biosoft.bsa.align.SemiGlobalScoringScheme;
import ru.biosoft.bsa.align.SimpleScoringScheme;



public class NeedlemanWunschTest extends TestCase
{

    public void testFindBestAlignment()
    {
        byte[] seq1 = dna( "ACGTAAT" );
        byte[]  seq2 = dna( "GGTAAT" );
        Alignment expected = new Alignment( seq1, seq2, new Element[] {I, M, M, M, M, M, M} );

        SimpleScoringScheme scoringScheme = new SimpleScoringScheme();
        NeedlemanWunsch nw = new NeedlemanWunsch( scoringScheme );
        Alignment alignment = nw.findBestAlignment( seq1, seq2 );

        assertEquals( -2.0, expected.getScore( scoringScheme ) );
        assertEquals( -2.0, alignment.getScore() );
        assertEquals( -2.0, alignment.getScore( scoringScheme ) );
        assertEquals( expected, alignment );
    }

    public void testFindBestScore()
    {
        byte[] seq1 = dna( "ACGTAAT" );
        byte[] seq2 = dna( "GGTAAT" );

        SimpleScoringScheme scoringScheme = new SimpleScoringScheme();
        NeedlemanWunsch nw = new NeedlemanWunsch( scoringScheme );
        assertEquals( -2.0, nw.findBestScore( seq1, seq2, -2.5 ) );
    }

    public void testSemiGlobalAlignment()
    {
        //CCAAG-TCAAGTCGG----
        //----GTTCAAATCGGGCTT
        byte[] seq1 = dna( "CCAAGTCAAGTCGG" );
        byte[] seq2 = dna( "GTTCAAATCGGGCTT" );
        Alignment expected = new Alignment( seq1, seq2, new Element[] {I, I, I, I, M, G, M, M, M, M, M, M, M, M, M, G, G, G, G} );

        SemiGlobalScoringScheme semiglobalScheme = new SemiGlobalScoringScheme();
        assertEquals( 7.0, expected.getScore( semiglobalScheme ) );

        NeedlemanWunsch nw = new NeedlemanWunsch( semiglobalScheme );
        Alignment alignment = nw.findBestAlignment( seq1, seq2 );

        assertEquals( 7.0, alignment.getScore() );
        assertEquals( 7.0, alignment.getScore( semiglobalScheme ) );
        assertEquals( expected, alignment );
    }
    
    public void testRightEndFreeGap()
    {
        //2012103103320-20332310330330201-030313112-312
        //||| | || || | |||||  |  | ||| | ||| | |||  |
        //201-1231-33-0.20332--0--02302-12030-1-1120011
        byte[] seq1 = dna("201210310332020332310330330201030313112312");
        byte[] seq2 = dna("2011231330.203320023021203011120011");
        Alignment expected = new Alignment(seq1,seq2,
                new Element[] {M,M,M,I,M,M,M,M,I,M,M,I,M,G,M,M,M,M,M,I,I,M,I,I,M,M,M,M,M,I,M,G,M,M,M,I,M,I,M,M,M,G,M,M,M});
        

        ScoringScheme scoringScheme = new RightEndFreeGap();
        NeedlemanWunsch nw = new NeedlemanWunsch( scoringScheme );

        Alignment alignment = nw.findBestAlignment( seq1, seq2 );
        assertEquals( expected, alignment );
        assertEquals( 11.0, alignment.getScore() );
        assertEquals( 11.0, alignment.getScore( scoringScheme ) );
        assertEquals( 11.0, nw.findBestScore( seq1, seq2, -Double.MAX_VALUE ) );
    }

    public void testAlignmentWithTreshold()
    {
        byte[] seq1 = dna( "ACGTAAT" );
        byte[] seq2 = dna( "GGTAAT" );

        SimpleScoringScheme scoringScheme = new SimpleScoringScheme();
        NeedlemanWunsch nw = new NeedlemanWunsch( scoringScheme );

        assertEquals( -2.0, nw.findBestScore( seq1, seq2, -2 ) );

        assertEquals( -2.0, nw.findBestScore( seq1, seq2, -3 ) );

        assertEquals( -Double.MAX_VALUE, nw.findBestScore( seq1, seq2, -1 ) );
    }

}
