package biouml.plugins.gtrd.master.meta.json;

import biouml.plugins.gtrd.ChIPseqExperiment;

public class ChIPSeqExperimentSerializer extends ChIPTFExperimentSerializer<ChIPseqExperiment>
{
    @Override
    protected ChIPseqExperiment createExperiment(String id)
    {
        return new ChIPseqExperiment( null, id );
    }
}
