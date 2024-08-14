package biouml.plugins.gtrd.master.sites.histones;

import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.master.sites.Peak;

public abstract class HistonesPeak extends Peak<HistonesExperiment>
{
    @Override
    public String getType()
    {
        return exp.getTarget();
    }
}
