package ru.biosoft.bsa.align;

import java.util.Arrays;

public class Alignment
{
    public static enum Element
    {
        M, //match or mismatch
        G, //gap in first sequence
        I; //insert in first sequence (gap in second)
    }

    private byte[] seq1, seq2;
    private Element[] path;
    private double score;

    public Alignment(byte[] seq1, byte[] seq2, Element[] path) throws IllegalArgumentException
    {
        this.seq1 = seq1;
        this.seq2 = seq2;
        this.path = path;

        int mCount, gCount, iCount;
        mCount = gCount = iCount = 0;
        for( Element e : path )
            switch( e )
            {
                case M:
                    mCount++;
                    break;
                case G:
                    gCount++;
                    break;
                case I:
                    iCount++;
                    break;
            }
        if( mCount + gCount != seq2.length || mCount + iCount != seq1.length )
            throw new IllegalArgumentException( "Invalid alignment path: M:" + mCount + " G:" + gCount + " I:" + iCount + " S1:"
                    + seq1.length + " S2:" + seq2.length );
    }

    public byte[] getSeq1()
    {
        return seq1;
    }

    public byte[] getSeq2()
    {
        return seq2;
    }

    public Element[] getPath()
    {
        return path;
    }
    
    public double getScore()
    {
        return score;
    }

    public void setScore(double score)
    {
        this.score = score;
    }

    public double getScore(ScoringScheme scoringScheme)
    {
        double result = 0;
        int i = - 1;
        int j = - 1;
        for( Element e : path )
        {
            switch( e )
            {
                case M: i++; j++; break;
                case G: j++; break;
                case I: i++; break;
            }
            result += scoringScheme.getScore( seq1, i, seq2, j, e);
        }
        return result;
    }
    
    public double getErrorRate()
    {
        int matches = 0;
        int i = -1, j = -1;
        for( Element e : path )
        {
            switch( e )
            {
                case M:
                {
                    i++;
                    j++;
                    if( seq1[i] == seq2[j] )
                        matches++;
                    break;
                }
                case G:
                    j++;
                    break;
                case I:
                    i++;
                    break;
            }
        }
        return ( (double) ( path.length - matches ) ) / path.length;
    }

    @Override
    public String toString()
    {
        StringBuilder seq1Builder = new StringBuilder();
        StringBuilder seq2Builder = new StringBuilder();
        StringBuilder matchBuilder = new StringBuilder();

        int pos1 = 0, pos2 = 0;
        for( Element e : path )
        {
            switch( e )
            {
                case M:
                    char l1 = (char)seq1[pos1++];
                    char l2 = (char)seq2[pos2++];
                    seq1Builder.append( l1 );
                    seq2Builder.append( l2 );
                    matchBuilder.append( l1 == l2 ? '|' : ' ' );
                    break;
                case G:
                    seq1Builder.append( '-' );
                    seq2Builder.append( (char)seq2[pos2++] );
                    matchBuilder.append( ' ' );
                    break;
                case I:
                    seq1Builder.append( (char)seq1[pos1++] );
                    seq2Builder.append( '-' );
                    matchBuilder.append( ' ' );
                    break;
            }
        }
        return seq1Builder.toString() + "\n" + matchBuilder.toString() + "\n" + seq2Builder.toString();
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( !(obj instanceof Alignment) )
            return false;
        Alignment other = (Alignment)obj;
        return Arrays.equals( path, other.path ) && Arrays.equals( seq1, other.seq1 ) && Arrays.equals( seq2, other.seq2 );
    }
    
}
