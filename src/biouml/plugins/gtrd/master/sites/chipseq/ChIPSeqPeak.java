package biouml.plugins.gtrd.master.sites.chipseq;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.sites.Peak;

public abstract class ChIPSeqPeak extends Peak<ChIPseqExperiment>
{
    @Override
    public String getType()
    {
        return exp.getTfTitle();
    }
}
