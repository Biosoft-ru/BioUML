package biouml.plugins.gtrd.master.sites.bsaconv;

import java.util.function.Function;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.master.sites.Peak;

//For sites that stored in union tracks
public abstract class UnionSiteToPeakConverter<T extends Peak<E>, E extends Experiment> extends SiteToGenomeLocationConverter<T>
{
    protected Function<String, E> expSupplier;
    protected UnionSiteToPeakConverter(Function<String, E> expSupplier)
    {
        this.expSupplier = expSupplier;
    }
    
    @Override
    protected void updatePeakFromSiteProperties(T peak, DynamicPropertySet dps)
    {
        String peakId = (String)dps.getValue( "id" );
        String[] parts = peakId.split( "[.]", 2 );
        peak.setId( Integer.parseInt( parts[1] ) );
        
        String expId = (String)dps.getValue( "exp" );
        peak.setExp( expSupplier.apply( expId ) );
    }
}
