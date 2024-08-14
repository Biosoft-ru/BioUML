package biouml.plugins.gtrd.master.sites.bsaconv;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.master.sites.GenomeLocation;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.bsa.Site;

public abstract class SiteToGenomeLocationConverter<T extends GenomeLocation> implements SiteConverter<T>
{
    protected abstract T createPeak();
    
    protected void updatePeak(T peak, Site site)
    {
        peak.setChr( StringPool.get(site.getOriginalSequence().getName()) );
        peak.setFrom( site.getFrom() );
        peak.setTo( site.getTo() );
        updatePeakFromSiteProperties( peak, site.getProperties() );
    }
    
    protected abstract void updatePeakFromSiteProperties(T peak, DynamicPropertySet properties);

    @Override
    public T createFromSite(Site site) {
        T peak = createPeak();
        updatePeak( peak, site );
        return peak;
    }
}
