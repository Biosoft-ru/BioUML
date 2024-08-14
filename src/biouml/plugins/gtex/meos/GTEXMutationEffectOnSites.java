package biouml.plugins.gtex.meos;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.analysis.maos.MutationEffectOnSites;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.WholeGenomeTask;

public class GTEXMutationEffectOnSites extends MutationEffectOnSites
{
    public GTEXMutationEffectOnSites(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    protected WholeGenomeTask createTask()
    {
        return new GTEXWholeGenomeTask( getParameters(), log, jobControl );
    }
    
}
