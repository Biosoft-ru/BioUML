package biouml.plugins.gtrd;

public class MNaseExperimentSQLTransformer extends DNaseLikeExperimentSQLTransformer<MNaseExperiment>
{
    public MNaseExperimentSQLTransformer()
    {
        super("mnase_experiments", "MNase");
    }

    @Override
    public Class<MNaseExperiment> getTemplateClass()
    {
        return MNaseExperiment.class;
    }
    
    @Override
    protected MNaseExperiment createExperiment(String id)
    {
        return new MNaseExperiment( owner, id );
    }
}
