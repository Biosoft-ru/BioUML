package biouml.plugins.gtrd.master.meta.json;

import biouml.plugins.gtrd.ChIPexoExperiment;

public class ChIPExoExperimentSerializer extends ChIPTFExperimentSerializer<ChIPexoExperiment>
{
    @Override
    protected ChIPexoExperiment createExperiment(String id)
    {
        return new ChIPexoExperiment( null, id );
    }
}
