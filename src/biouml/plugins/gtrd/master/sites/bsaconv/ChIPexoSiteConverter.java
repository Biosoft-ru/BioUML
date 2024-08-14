package biouml.plugins.gtrd.master.sites.bsaconv;

import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;

public abstract class ChIPexoSiteConverter<T extends ChIPexoPeak> extends SiteToPeakConverter<T, ChIPexoExperiment>
{
    public ChIPexoSiteConverter(ChIPexoExperiment exp)
    {
        super(exp);
    }
}