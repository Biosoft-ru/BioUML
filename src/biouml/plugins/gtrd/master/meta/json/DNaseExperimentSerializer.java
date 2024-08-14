package biouml.plugins.gtrd.master.meta.json;

import biouml.plugins.gtrd.DNaseExperiment;

public class DNaseExperimentSerializer extends ExperimentSerializer<DNaseExperiment>
{
    @Override
    protected DNaseExperiment createExperiment(String id)
    {
        return new DNaseExperiment( null, id );
    }
}
