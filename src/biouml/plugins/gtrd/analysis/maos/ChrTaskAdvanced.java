package biouml.plugins.gtrd.analysis.maos;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.maos.ChrTask;
import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.IntervalData;
import ru.biosoft.bsa.analysis.maos.SiteModelTask;
import ru.biosoft.bsa.analysis.maos.Variation;

public class ChrTaskAdvanced extends ChrTask
{
    private GTRDMetadata gtrdMetadata;
    private Map<String, Set<String>> uniprot2pathway;
    private Map<String, Set<String>> uniprot2disease;

    public ChrTaskAdvanced(DataElementPath chrPath, AdvancedParameters parameters, IResultHandler resultHandler, Logger analysisLog,
            GTRDMetadata gtrdMetadata, Map<String, Set<String>> uniprot2pathway, Map<String, Set<String>> uniprot2disease)
    {
        super( chrPath, parameters, resultHandler, analysisLog );
        this.gtrdMetadata = gtrdMetadata;
        this.uniprot2pathway = uniprot2pathway;
        this.uniprot2disease = uniprot2disease;
    }
    
    private AdvancedParameters getParameters()
    {
        return (AdvancedParameters)parameters;
    }
    
    @Override
    protected IntervalDataAdvanced createIntervalData(Interval interval, Sequence chr, Variation[] variations, int variationStart)
    {
        IntervalData mainData = super.createIntervalData( interval, chr, variations, variationStart );
        
        String chrPath = chr.getOrigin().getCompletePath().toString();
        Map<String, GTRDDataForTFClass> gtrdDataByTFClass = GTRDDataForTFClass.load( chrPath, interval, getParameters().getTfClassDepthNumber() );
        for(GTRDDataForTFClass tfData : gtrdDataByTFClass.values())
            tfData.translateToRelativeAndTruncate( interval, mainData.getReference().getStart() );
        
        return new IntervalDataAdvanced( mainData, gtrdDataByTFClass );
    }
    
    @Override
    protected SiteModelTask createSiteModelTask(IntervalData data, SiteModel model)
    {
        Set<String> tfClasses = gtrdMetadata.siteModel2TfClass.get( model.getName() );
        return new SiteModelTaskAdvanced( model, (IntervalDataAdvanced)data, resultHandler, parameters, tfClasses, uniprot2pathway, uniprot2disease );
    }
}
