package biouml.plugins.gtrd.master.meta.json;

import biouml.plugins.gtrd.MNaseExperiment;

public class MNaseExperimentSerializer extends ExperimentSerializer<MNaseExperiment>
{
    @Override
    protected MNaseExperiment createExperiment(String id)
    {
        return new MNaseExperiment( null, id );
    }
}
