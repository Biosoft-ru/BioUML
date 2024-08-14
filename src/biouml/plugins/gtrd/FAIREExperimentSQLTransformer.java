package biouml.plugins.gtrd;

public class FAIREExperimentSQLTransformer extends DNaseLikeExperimentSQLTransformer<FAIREExperiment>
{
    public FAIREExperimentSQLTransformer()
    {
        super( "faire_experiments", "FAIREseq" );
    }
    
    @Override
    public Class<FAIREExperiment> getTemplateClass()
    {
        return FAIREExperiment.class;
    }
    
    @Override
    protected FAIREExperiment createExperiment(String id)
    {
        return new FAIREExperiment( owner, id );
    }
}
