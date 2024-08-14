package biouml.plugins.ensembl.analysis.mutationeffect;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.analysis.maos.ChrTask;
import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.WholeGenomeTask;

public class WholeGenomeTaskBySNP extends WholeGenomeTask
{
    private Map<String, List<Site>> sitesByChr;
    public WholeGenomeTaskBySNP(Parameters parameters, Logger analysisLog, AnalysisJobControl progress, Map<String, List<Site>> sites)
    {
        super( parameters, analysisLog, progress );
        sitesByChr = sites;
        ( (ResultHandlerBySNP)resultHandler ).setSites( sites );

    }

    @Override
    protected ChrTask createChrTask(DataElementPath chrPath)
    {
        List<Site> sites = sitesByChr.get( chrPath.getName() );
        return new ChrTaskBySNP( chrPath, parameters, resultHandler, analysisLog, sites );
    }

    @Override
    protected IResultHandler createResultHandler()
    {
        return new ResultHandlerBySNP( parameters, analysisLog );
    }

}
