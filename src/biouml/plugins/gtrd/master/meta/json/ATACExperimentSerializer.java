package biouml.plugins.gtrd.master.meta.json;

import biouml.plugins.gtrd.ATACExperiment;

public class ATACExperimentSerializer extends ExperimentSerializer<ATACExperiment>
{
    @Override
    protected ATACExperiment createExperiment(String id)
    {
        return new ATACExperiment( null, id );
    }
}
