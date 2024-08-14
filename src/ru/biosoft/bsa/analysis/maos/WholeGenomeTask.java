package ru.biosoft.bsa.analysis.maos;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AnalysisJobControl;

public class WholeGenomeTask
{
    protected Parameters parameters;
    protected Logger analysisLog;
    protected AnalysisJobControl progress;
    protected IResultHandler resultHandler;
    
    public WholeGenomeTask(Parameters parameters, Logger analysisLog, AnalysisJobControl progress)
    {
        this.parameters = parameters;
        this.analysisLog = analysisLog;
        this.progress = progress;
        resultHandler = createResultHandler();
        resultHandler.init();
    }
    
    public Object[] run() throws Exception
    {
        DataElementPathSet chromosomes = parameters.getChromosomesPath().getChildren();
        progress.forCollection( chromosomes,  (chrPath) -> {
            analysisLog.info( "Processing chromosome " + chrPath.getName() );
            ChrTask chrTask = createChrTask( chrPath );
            chrTask.run();
            return true;
        });
        resultHandler.finish();
        return resultHandler.getResults();
    }

    
    protected IResultHandler createResultHandler()
    {
        return new ResultHandler( parameters, analysisLog );
    }
    
    protected ChrTask createChrTask(DataElementPath chrPath)
    {
        return new ChrTask(chrPath, parameters, resultHandler, analysisLog);
    }
}
