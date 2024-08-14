package ru.biosoft.bsa.align;

import ru.biosoft.bsa.align.Alignment.Element;

public class RightEndFreeGap extends SimpleScoringScheme
{
    public RightEndFreeGap()
    {
        this(1, -1, -1);
    }
    
    public RightEndFreeGap(double match, double mismatch, double gap)
    {
        super( match, mismatch, gap );
    }

    @Override
    public double getScore(byte[] seq1, int pos1, byte[] seq2, int pos2, Element elem)
    {
        if( (elem == Element.G && pos1 == seq1.length - 1) || (elem == Element.I && pos2 == seq2.length - 1) )
            return 0;
        return super.getScore( seq1, pos1, seq2, pos2, elem );
    }

}