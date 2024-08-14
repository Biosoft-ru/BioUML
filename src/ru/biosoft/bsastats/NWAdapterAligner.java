package ru.biosoft.bsastats;

import ru.biosoft.bsa.align.Alignment;
import ru.biosoft.bsa.align.Alignment.Element;
import ru.biosoft.bsa.align.NeedlemanWunsch;
import ru.biosoft.bsa.align.ScoringScheme;
import ru.biosoft.bsa.align.SemiGlobalScoringScheme;
import ru.biosoft.bsa.align.SimpleScoringScheme;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;


@PropertyName( "Needleman Wunsch aligner" )
@PropertyDescription( "Adapter and read aligned with Needleman Wunsch algorithm allowing free gaps in the adapter start and read end."
        + "The match gives +2 to alignment score, mismatch -1 and gap -2. If resulting alignment pass threshold then matching part of read is removed." )
public class NWAdapterAligner extends AdapterAligner
{
    @Override
    public String getName()
    {
        return "Needleman Wunsch";
    }

    private int minMatchLength = 10;
    private double errorRate = 0.1;
    
    @PropertyName ( "Minimal match" )
    @PropertyDescription ( "Minimal lenght of match between read and adapter" )
    public int getMinMatchLength()
    {
        return minMatchLength;
    }

    public void setMinMatchLength(int minMatchLength)
    {
        int oldValue = this.minMatchLength;
        this.minMatchLength = minMatchLength;
        firePropertyChange( "minMatchLength", oldValue, minMatchLength );
    }

    @PropertyName ( "Error rate" )
    @PropertyDescription ( "Allowed fraction of mismatches between read and adapter" )
    public double getErrorRate()
    {
        return errorRate;
    }

    public void setErrorRate(double errorRate)
    {
        double oldValue = this.errorRate;
        this.errorRate = errorRate;
        firePropertyChange( "errorRate", oldValue, errorRate );
    }

    @Override
    public AdapterMatch alignAdapter(byte[] adapter, byte[] sequence)
    {
        ScoringScheme scheme = new SemiGlobalScoringScheme();
        NeedlemanWunsch nw = new NeedlemanWunsch( scheme );
        Alignment alignment = nw.findBestAlignment( adapter, sequence );
        
        int prefixGaps = 0;
        Element[] path = alignment.getPath();
        for( Element e : path )
        {
            if( e == Element.G )
                prefixGaps++;
            else
                break;
        }
        int suffixInserts = 0;
        for( int k = path.length - 1; k >= 0; k--)
        {
            Element e = path[k];
            if( e == Element.I)
                suffixInserts++;
        }
        int matchLength = path.length - prefixGaps - suffixInserts;
        if(matchLength < minMatchLength)
            return new AdapterMatch( sequence.length, -Double.MAX_VALUE );
        
        int matches = 0;
        int i = -1, j = prefixGaps - 1;
        for( int k = prefixGaps; k < path.length - suffixInserts; k++ )
        {
            Element e = path[k];
            switch( e )
            {
                case M:
                {
                    i++;
                    j++;
                    if( adapter[i] == sequence[j] )
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
        
        double errorRate = ((double)(matchLength - matches)) / ((double)matchLength);
        if( errorRate > this.errorRate )
            return new AdapterMatch( sequence.length, -Double.MAX_VALUE);
            
        return new AdapterMatch( prefixGaps, alignment.getScore() );
    }

    private static class AdapterAlignmentScheme extends SimpleScoringScheme
    {
        public AdapterAlignmentScheme()
        {
            super( 2, -1, -2 );
        }
        
        @Override
        public double getScore(byte[] adapter, int adapterPos, byte[] sequence, int sequencePos, Element elem)
        {
            if( ( elem == Element.G && adapterPos == -1 ) || ( elem == Element.I && sequencePos == sequence.length - 1 ) )
                return 0;
            return super.getScore( adapter, adapterPos, sequence, sequencePos, elem );
        }
    }

}
