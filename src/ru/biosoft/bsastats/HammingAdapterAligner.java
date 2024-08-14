package ru.biosoft.bsastats;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName( "Hamming aligner" )
@PropertyDescription( "The adapter sequence is aligned without gaps to 5' or 3' end of read with given error rate and matched part is removed."
+ "If there are many alignments passing error rate threshold, the one with more matches is selected." )
public class HammingAdapterAligner extends AdapterAligner 
{
    private int minMatchLength = 10;
    private double errorRate = 0.1;
    private boolean leftMost = false;
    
    @Override
    public String getName()
    {
        return "Hamming aligner";
    }
    
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
    
    @PropertyName( "Left most" )
    @PropertyDescription( "Choose left most alignment among equivalent alignments" )
    public boolean isLeftMost()
    {
        return leftMost;
    }

    public void setLeftMost(boolean leftMost)
    {
        boolean oldValue = this.leftMost;
        this.leftMost = leftMost;
        firePropertyChange( "leftMost", oldValue, leftMost );
    }

    @Override
    public AdapterMatch alignAdapter(byte[] adapter, byte[] sequence)
    {
        int bestAt = sequence.length;
        int bestMatches = -1;
        for( int i = 0; i <= sequence.length - minMatchLength ; i++ )
        {
            int matchLength = Math.min( sequence.length - i, adapter.length );
            int errorsAllowed = (int) ( matchLength * errorRate );
            int errors = 0;
            for( int j = 0; j < matchLength && errors <= errorsAllowed; j++ )
                if( adapter[j] != 'n' && adapter[j] != 'N' && sequence[i + j] != adapter[j] )
                    errors++;
            int matches = matchLength - errors;
            if(errors <= errorsAllowed && (bestMatches < matches || (bestMatches == matches && !leftMost)))
            {
                bestMatches = matches;
                bestAt = i;
            }
        }
        return new AdapterMatch( bestAt, bestMatches );
    }

}
