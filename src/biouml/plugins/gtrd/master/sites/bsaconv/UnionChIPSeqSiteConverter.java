package biouml.plugins.gtrd.master.sites.bsaconv;

import java.util.function.Function;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;

public abstract class UnionChIPSeqSiteConverter<T extends ChIPSeqPeak> extends UnionSiteToPeakConverter<T, ChIPseqExperiment>
{
    protected UnionChIPSeqSiteConverter(Function<String, ChIPseqExperiment> expSupplier)
    {
        super( expSupplier );
    }
}
