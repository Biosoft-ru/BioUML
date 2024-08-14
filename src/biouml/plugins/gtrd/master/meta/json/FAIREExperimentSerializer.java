package biouml.plugins.gtrd.master.meta.json;

import biouml.plugins.gtrd.FAIREExperiment;

public class FAIREExperimentSerializer extends ExperimentSerializer<FAIREExperiment>
{
    @Override
    protected FAIREExperiment createExperiment(String id)
    {
        return new FAIREExperiment( null, id );
    }
}
