package biouml.plugins.gtrd;

public class ATACExperimentSQLTransformer extends DNaseLikeExperimentSQLTransformer<ATACExperiment>
{
    public ATACExperimentSQLTransformer()
    {
        super( "atac_experiments", "ATACseq" );
    }
    
    @Override
    public Class<ATACExperiment> getTemplateClass()
    {
        return ATACExperiment.class;
    }
    
    @Override
    protected ATACExperiment createExperiment(String id)
    {
        return new ATACExperiment( owner, id );
    }
}
