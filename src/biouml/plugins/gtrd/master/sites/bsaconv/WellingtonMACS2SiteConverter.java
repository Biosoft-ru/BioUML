package biouml.plugins.gtrd.master.sites.bsaconv;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonMACS2Footprint;

public class WellingtonMACS2SiteConverter extends WellingtonSiteConverter<WellingtonMACS2Footprint>
{
    public WellingtonMACS2SiteConverter(DNaseExperiment exp)
    {
        super( exp );
    }

    @Override
    protected WellingtonMACS2Footprint createPeak()
    {
        return new WellingtonMACS2Footprint();
    }
}