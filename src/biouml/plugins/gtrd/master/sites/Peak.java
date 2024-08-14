package biouml.plugins.gtrd.master.sites;

import java.beans.PropertyDescriptor;
import java.util.Objects;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.Experiment;
import ru.biosoft.util.bean.StaticDescriptor;

public abstract class Peak<E extends Experiment> extends GenomeLocation
{
    protected E exp;

    public abstract String getPeakCaller();

    @Override
    public String getStableId()
    {
        return "p." + exp.getName() + "." + getPeakCaller() + "." + id;//p.EEXP003098.gem.10719
    }

    public E getExp()
    {
        return exp;
    }

    public void setExp(E exp)
    {
        this.exp = exp;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( !super.equals( obj ) )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        Peak other = (Peak)obj;
        if(id != other.id)
            return false;
        return Objects.equals( exp, other.exp );
    }
    
    protected static final PropertyDescriptor EXP_PD = StaticDescriptor.create( "experimentId", "Experiment id" );
    protected static final PropertyDescriptor PEAK_CALLER_PD = StaticDescriptor.create( "peakCaller", "Peak caller" );
    @Override
    public DynamicPropertySet getProperties()
    {
        DynamicPropertySet dps = super.getProperties();
        dps.add( new DynamicProperty( EXP_PD, String.class, getExp().getName() ) );
        dps.add( new DynamicProperty(PEAK_CALLER_PD, String.class, getPeakCaller()) );
        return dps;
    }
}
