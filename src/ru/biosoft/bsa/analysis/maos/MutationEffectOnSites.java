package ru.biosoft.bsa.analysis.maos;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;

@ClassIcon ( "resources/mutation-effect.gif" )
public class MutationEffectOnSites extends AnalysisMethodSupport<Parameters>
{
    public MutationEffectOnSites(DataCollection<?> origin, String name)
    {
        this( origin, name, new Parameters() );
    }
    
    protected MutationEffectOnSites(DataCollection<?> origin, String name, Parameters parameters)
    {
        super( origin, name, parameters );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        WholeGenomeTask task = createTask();
        return task.run();
    }

    protected WholeGenomeTask createTask()
    {
        return new WholeGenomeTask( parameters, log, jobControl );
    }
}
