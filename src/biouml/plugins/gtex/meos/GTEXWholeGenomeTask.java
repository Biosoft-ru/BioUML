package biouml.plugins.gtex.meos;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.analysis.maos.ChrTask;
import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.WholeGenomeTask;

public class GTEXWholeGenomeTask extends WholeGenomeTask
{

    public GTEXWholeGenomeTask(Parameters parameters, Logger analysisLog, AnalysisJobControl progress)
    {
        super( parameters, analysisLog, progress );
    }
    
    @Override
    protected ChrTask createChrTask(DataElementPath chrPath)
    {
        return new GTEXChrTask( chrPath, parameters, resultHandler, analysisLog );
    }

    @Override
    protected IResultHandler createResultHandler()
    {
        return new GTEXResultHandler( parameters, analysisLog );
    }
}
