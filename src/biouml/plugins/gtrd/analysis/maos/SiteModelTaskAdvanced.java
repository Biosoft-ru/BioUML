package biouml.plugins.gtrd.analysis.maos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.SiteModelTask;
import ru.biosoft.bsa.analysis.maos.SiteMutation;

public class SiteModelTaskAdvanced extends SiteModelTask
{
    protected static final Logger log = Logger.getLogger( SiteModelTaskAdvanced.class.getName() );
    protected Set<String> tfClasses;
    private Map<String, Set<String>> uniprot2pathway;
    private Map<String, Set<String>> uniprot2disease;
    
    public SiteModelTaskAdvanced(SiteModel siteModel, IntervalDataAdvanced data, IResultHandler resultHandler, Parameters parameters,
            Set<String> tfClasses,  Map<String, Set<String>> uniprot2pathway, Map<String, Set<String>> uniprot2disease)
    {
        super( siteModel, data, resultHandler, parameters );
        this.uniprot2pathway = uniprot2pathway;
        this.uniprot2disease = uniprot2disease;
        this.tfClasses = tfClasses;
    }
    
    protected IntervalDataAdvanced getIntervalData()
    {
        return (IntervalDataAdvanced)data;
    }
    
    @Override
    public void run()
    {
        if(!hasGTRDData())
            return;
        super.run();
    }
    
    private boolean hasGTRDData()
    {
        for(String tfClass : tfClasses)
        {
            GTRDDataForTFClass tfData = getIntervalData().gtrdDataByTFClass.get( tfClass );
            if(tfData == null)
                continue;
            for(List<GTRDPeak> peaks : tfData.groups.values())
                if(!peaks.isEmpty())
                    return true;
        }
        return false;
    }

    @Override
    protected void siteMutationFound(SiteMutation siteMutation)
    {
        Interval siteInterval = siteMutation.getSiteInterval();
        if(siteInterval == null)
        {
            log.severe( "Can not find insertion point, result will be skipped"  );
            return;
        }
        
        for(String tfClass : tfClasses)
        {
            //Independent handling of cases when siteModel maps to several tfClasses.
            
            GTRDDataForTFClass tfData = getIntervalData().gtrdDataByTFClass.get( tfClass );
            if(tfData == null)
                continue;
            
            tfData.groups.forEach( (key,value) -> {
                GTRDPeak[] peaks = findOverlapping(value, siteInterval);
                if(peaks.length == 0)
                    return;
                
                Set<String> uniprots = new HashSet<>();
                for(GTRDPeak peak : peaks)
                    uniprots.add(peak.getTfUniprotId());
                
                Set<String> pathways = new HashSet<>();
                Set<String> diseaseList = new HashSet<>();
                for(String tf : uniprots)
                {
                    Set<String> pathwaysForTf = uniprot2pathway.get( tf );
                    if(pathwaysForTf != null)
                        pathways.addAll( pathwaysForTf );
                    Set<String> diseaseListForTf = uniprot2disease.get(tf);
                    if(diseaseListForTf != null)
                        diseaseList.addAll( diseaseListForTf );
                }
                
                SiteMutation advancedMutation = new SiteMutationAdvanced( siteMutation, key.tf, uniprots.toArray(new String[0]),
                        key.cell, key.treatment, peaks,
                        pathways.toArray( new String[0] ),
                        diseaseList.toArray( new String[0] ));
                
                super.siteMutationFound( advancedMutation );
            });
        }
        
    }

    private GTRDPeak[] findOverlapping(List<GTRDPeak> peaks, Interval interval)
    {
        List<GTRDPeak> result = new ArrayList<>();
        for(GTRDPeak p : peaks)
            if(p.intersects( interval ))
                result.add( p );
        return result.toArray(new GTRDPeak[0]);
    }
}
