package biouml.plugins.gtrd.analysis.maos;

public class TfCellTreatment
{
    public final String tf,cell,treatment;

    public TfCellTreatment(String tf, String cell, String treatment)
    {
        this.tf = tf;
        this.cell = cell;
        this.treatment = treatment;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( cell == null ) ? 0 : cell.hashCode() );
        result = prime * result + ( ( tf == null ) ? 0 : tf.hashCode() );
        result = prime * result + ( ( treatment == null ) ? 0 : treatment.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        TfCellTreatment other = (TfCellTreatment)obj;
        if( cell == null )
        {
            if( other.cell != null )
                return false;
        }
        else if( !cell.equals( other.cell ) )
            return false;
        if( tf == null )
        {
            if( other.tf != null )
                return false;
        }
        else if( !tf.equals( other.tf ) )
            return false;
        if( treatment == null )
        {
            if( other.treatment != null )
                return false;
        }
        else if( !treatment.equals( other.treatment ) )
            return false;
        return true;
    }
}
