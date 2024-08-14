package ru.biosoft.bsa.align;

import java.util.LinkedList;

import ru.biosoft.bsa.align.Alignment.Element;
import ru.biosoft.util.Util;

public class NeedlemanWunsch
{
    private final ScoringScheme scoringScheme;

    public NeedlemanWunsch(ScoringScheme scoringScheme)
    {
        this.scoringScheme = scoringScheme;
    }

    /**
     * Find best global alignment between two sequences.
     * If the best score is less then scoreThreshold return null.
     */
    public Alignment findBestAlignment(byte[] seq1, byte[] seq2)
    {
        double[][] m = new double[seq1.length + 1][seq2.length + 1];
        m[0][0] = 0;
        for( int i = 1; i <= seq1.length; i++ )
            m[i][0] = m[i - 1][0] + scoringScheme.getScore( seq1, i - 1, seq2, -1, Element.I );
        for( int i = 1; i <= seq2.length; i++ )
            m[0][i] = m[0][i - 1] + scoringScheme.getScore( seq1, -1, seq2, i - 1, Element.G );

        for( int i = 1; i <= seq1.length; i++ )
        {
            for( int j = 1; j <= seq2.length; j++ )
            {
                int pos1 = i - 1;
                int pos2 = j - 1;
                double match = m[i - 1][j - 1] + scoringScheme.getScore( seq1, pos1, seq2, pos2, Element.M );
                double ins = m[i - 1][j] + scoringScheme.getScore( seq1, pos1, seq2, pos2, Element.I );
                double gap = m[i][j - 1] + scoringScheme.getScore( seq1, pos1, seq2, pos2, Element.G );
                m[i][j] = Util.max( match, ins, gap );
            }
        }

        int i = seq1.length;
        int j = seq2.length;

        LinkedList<Element> path = new LinkedList<>();
        while( i != 0 || j != 0 )
        {
            int pos1 = i - 1;
            int pos2 = j - 1;
            Element e;

            if( i > 0 && j > 0 && m[i][j] == m[i - 1][j - 1] + scoringScheme.getScore( seq1, pos1, seq2, pos2, Element.M ) )
            {
                --i;
                --j;
                e = Element.M;
            }
            else if( i > 0 && m[i][j] == m[i - 1][j] + scoringScheme.getScore( seq1, pos1, seq2, pos2, Element.I ) )
            {
                --i;
                e = Element.I;
            }
            else
            {
                --j;
                e = Element.G;
            }
            path.addFirst( e );
        }

        Alignment alignment = new Alignment( seq1, seq2, path.toArray( new Element[path.size()] ) );
        alignment.setScore( m[seq1.length][seq2.length] );
        return alignment;
    }
    
    public boolean isScoreBetter(byte[] seq1, byte[] seq2, double scoreThreshold)
    {
        return findBestScore( seq1, seq2, scoreThreshold ) >= scoreThreshold;
    }

    public double findBestScore(byte[] seq1, byte[] seq2, double scoreThreshold)
    {
        double[] v = new double[seq1.length + 1];

        v[0] = 0;
        for( int i = 1; i < v.length; i++ )
            v[i] = v[i - 1] + scoringScheme.getScore( seq1, i - 1, seq2, -1, Element.I );

        for( int i = 1; i <= seq2.length; i++ )
        {
            double prev = v[0] + scoringScheme.getScore( seq1, -1, seq2, i - 1, Element.G );
            double best = prev;
            for( int j = 1; j < v.length; j++ )
            {
                int pos1 = j - 1;
                int pos2 = i - 1;
                double match = v[j - 1] + scoringScheme.getScore( seq1, pos1, seq2, pos2, Element.M );
                double ins = v[j] + scoringScheme.getScore( seq1, pos1, seq2, pos2, Element.G );
                double gap = prev + scoringScheme.getScore( seq1, pos1, seq2, pos2, Element.I );

                v[j - 1] = prev;
                prev = Util.max( match, ins, gap );

                if( best < prev )
                    best = prev;
            }
            v[v.length - 1] = prev;
            if( best + scoringScheme.getMaxScore( seq2.length - i ) < scoreThreshold )
                return -Double.MAX_VALUE;
        }

        return v[v.length - 1];
    }

}
