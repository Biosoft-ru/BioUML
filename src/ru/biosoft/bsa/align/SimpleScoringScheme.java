package ru.biosoft.bsa.align;

import static ru.biosoft.bsa.align.Alignment.Element.M;
import ru.biosoft.bsa.align.Alignment.Element;

public class SimpleScoringScheme implements ScoringScheme
{

    protected double match, mismatch, gap;


    public SimpleScoringScheme()
    {
        this( 0, -1, -1 );
    }

    public SimpleScoringScheme(double match, double mismatch, double gap)
    {
        this.match = match;
        this.mismatch = mismatch;
        this.gap = gap;
        if( match <= mismatch )
            throw new IllegalArgumentException( "match <= mismatch" );
    }

    @Override
    public double getScore(byte[] seq1, int pos1, byte[] seq2, int pos2, Element elem)
    {
        if( elem == M )
            return seq1[pos1] == seq2[pos2] ? match : mismatch;
        return gap;
    }

    @Override
    public double getMaxScore(int seqLen)
    {
        return seqLen * match;
    }
}
