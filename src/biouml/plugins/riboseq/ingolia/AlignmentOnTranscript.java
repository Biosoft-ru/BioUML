package biouml.plugins.riboseq.ingolia;

import ru.biosoft.bsa.Interval;

public class AlignmentOnTranscript extends Interval
{
    private boolean isPositiveStrand;

    public AlignmentOnTranscript(int from, int to, boolean isPositiveStrand)
    {
        super( from, to );
        this.isPositiveStrand = isPositiveStrand;
    }

    public boolean isPositiveStrand()
    {
        return isPositiveStrand;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( obj == null || !(obj instanceof AlignmentOnTranscript) )
            return false;
        if( obj == this )
            return true;
        if( !super.equals( obj ) )
            return false;
        final AlignmentOnTranscript aot = (AlignmentOnTranscript) obj;
        return isPositiveStrand == aot.isPositiveStrand;
    }
}
