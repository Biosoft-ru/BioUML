package biouml.plugins.gtrd.master.sites.chipexo;

import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.master.sites.Peak;

public abstract class ChIPexoPeak extends Peak<ChIPexoExperiment>
{
    @Override
    public String getType()
    {
        return exp.getTfTitle();
    }
}