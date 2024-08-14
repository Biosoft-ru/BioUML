package biouml.plugins.gtrd.master.sites.bsaconv;

import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.master.sites.Peak;
import ru.biosoft.bsa.Site;

public abstract class SiteToPeakConverter<T extends Peak<E>, E extends Experiment> extends SiteToGenomeLocationConverter<T>
{
    protected E exp;
    
    public SiteToPeakConverter(E exp)
    {
        this.exp = exp;
    }
    
    @Override
    protected void updatePeak(T peak, Site site)
    {
        super.updatePeak( peak, site );
        peak.setId( Integer.parseInt( site.getName() ) );
        peak.setExp( exp );
    }
}
