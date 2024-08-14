package biouml.plugins.gtrd;

public class DNaseExperimentSQLTransformer extends DNaseLikeExperimentSQLTransformer<DNaseExperiment>
{
    public DNaseExperimentSQLTransformer()
    {
        super( "dnase_experiments", "DNase" );
    }

    @Override
    public Class<DNaseExperiment> getTemplateClass()
    {
        return DNaseExperiment.class;
    }
    
    @Override
    protected DNaseExperiment createExperiment(String id)
    {
        return new DNaseExperiment( owner, id );
    }
}
