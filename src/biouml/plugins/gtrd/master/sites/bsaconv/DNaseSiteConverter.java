package biouml.plugins.gtrd.master.sites.bsaconv;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;

public abstract class DNaseSiteConverter<T extends DNasePeak> extends SiteToPeakConverter<T, DNaseExperiment>
{
    public DNaseSiteConverter(DNaseExperiment exp)
    {
        super( exp );
    }
}