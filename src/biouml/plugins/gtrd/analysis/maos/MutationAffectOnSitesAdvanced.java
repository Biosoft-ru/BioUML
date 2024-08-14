package biouml.plugins.gtrd.analysis.maos;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.analysis.maos.MutationEffectOnSites;
import ru.biosoft.bsa.analysis.maos.WholeGenomeTask;

public class MutationAffectOnSitesAdvanced extends MutationEffectOnSites
{
    public MutationAffectOnSitesAdvanced(DataCollection<?> origin, String name)
    {
        super( origin, name, new AdvancedParameters() );
    }
    
    @Override
    public AdvancedParameters getParameters()
    {
        return (AdvancedParameters)super.getParameters();
    }

    @Override
    protected WholeGenomeTask createTask()
    {
        return new WholeGenomeTaskAdvanced( getParameters(), log, jobControl );
    }
    
}
