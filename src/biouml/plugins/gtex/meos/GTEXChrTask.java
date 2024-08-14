package biouml.plugins.gtex.meos;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.maos.ChrTask;
import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.IntervalData;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.SiteModelTask;

public class GTEXChrTask extends ChrTask
{
    public GTEXChrTask(DataElementPath chrPath, Parameters parameters, IResultHandler resultHandler, Logger analysisLog)
    {
        super( chrPath, parameters, resultHandler, analysisLog );
    }

    @Override
    protected void searchAllSiteModels(Sequence chr, Interval interval, IntervalData data)
    {
        IntervalData dataRC = data.getReverseComplement();
        for( SiteModel model : siteModels )
        {
            RCAggregator aggregator = new RCAggregator(resultHandler);
            
            SiteModelTask task = createSiteModelTask( data, model, aggregator );
            task.run();
            
            task = createSiteModelTask( dataRC, model, aggregator );
            task.run();
            
            aggregator.finish();
        }
    }
    
    protected SiteModelTask createSiteModelTask(IntervalData data, SiteModel model, IResultHandler resultHandler)
    {
        return new GTEXSiteModelTask( model, data, resultHandler, parameters );
    }
}
