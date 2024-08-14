package biouml.plugins.gtrd.master.sites.bsaconv;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonHotspot2Footprint;

public class WellingtonHotspot2SiteConverter extends WellingtonSiteConverter<WellingtonHotspot2Footprint>
{
    public WellingtonHotspot2SiteConverter(DNaseExperiment exp)
    {
        super( exp );
    }

    @Override
    protected WellingtonHotspot2Footprint createPeak()
    {
        return new WellingtonHotspot2Footprint();
    }
}