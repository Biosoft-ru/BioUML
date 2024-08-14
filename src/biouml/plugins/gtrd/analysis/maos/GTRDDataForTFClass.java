package biouml.plugins.gtrd.analysis.maos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;

public class GTRDDataForTFClass
{
    public final String tfClass;
    public final Map<TfCellTreatment, List<GTRDPeak>> groups = new HashMap<>();
    public GTRDDataForTFClass(String tfClass)
    {
        this.tfClass = tfClass;
    }
    
    public static Map<String, GTRDDataForTFClass> load(String chrPath, Interval interval, int depth)
    {
        Map<String, GTRDDataForTFClass> result = new HashMap<>();
        for( String peakCaller : new String[] {"macs", "macs2", "gem", "pics", "sissrs"} )
        {
            DataElementPath unionTrackPath = DataElementPath.create( "databases/GTRD/Data/peaks/union/Homo sapiens " + peakCaller );
            if(!unionTrackPath.exists())
                continue;
            Track unionTrack = unionTrackPath.getDataElement( Track.class );
            DataCollection<Site> sites = unionTrack.getSites( chrPath, interval.getFrom(), interval.getTo() );
            for(Site s : sites)
            {
                String tfClass5 = s.getProperties().getValueAsString( "tfClassId" );
                if(tfClass5 == null || tfClass5.isEmpty())
                    continue;//not mapper to tfClass (cofactors)
                String cell = s.getProperties().getValueAsString( "cellLine" );
                if(cell == null)
                    throw new AssertionError();
                String treatment = s.getProperties().getValueAsString( "treatment" );
                if(treatment == null)
                    treatment = "";
                
                String tfClass = GTRDMetadata.toLowerDepth( tfClass5, depth );
                if(tfClass == null)
                    continue;//level of tfClass5 was lower then depth
                
                TfCellTreatment key = new TfCellTreatment( tfClass, cell, treatment );
                GTRDPeak peak = createPeakFromSite(unionTrack, s, peakCaller );
                
                
                result.computeIfAbsent( tfClass, GTRDDataForTFClass::new )
                    .groups.computeIfAbsent( key, k->new ArrayList<>() )
                    .add( peak );
            }
        }
        
        return result;
    }
    
    private static GTRDPeak createPeakFromSite(Track track, Site site, String peakCaller)
    {
        Interval interval = site.getInterval();
        double score;
        final double DEFAULT_SCORE = 1;
        if(peakCaller.equals( "macs" ))
        {
            String strScore = site.getProperties().getValueAsString( "-10*log10(pvalue)" );
            score = Double.parseDouble( strScore ) / 10;
            //score in range 5-330            
            score /= 33;
        } else if(peakCaller.equals( "macs2" ))
        {
            String strScore = site.getProperties().getValueAsString( "-log10(pvalue)" );
            score = Double.parseDouble( strScore ) / 175;
        } else if(peakCaller.equals( "gem" ))
        {
            String strScore = site.getProperties().getValueAsString( "P_-lg10" );
            score = Double.parseDouble( strScore );//[2-999], but when no control always zero
            score /= 99.9;
            if(score == 0)//case of no control
                score = DEFAULT_SCORE;
        } else if(peakCaller.equals( "pics" ))
        {
            score = DEFAULT_SCORE;//just a constant
        } else if(peakCaller.equals( "sissrs" ))
        {
            String strScore = site.getProperties().getValueAsString( "p-value" );
            if(strScore == null)//for experiments without control
            {
                score = DEFAULT_SCORE;
            } else
            {
                score = Double.parseDouble( strScore );
                if(score == 0)
                    score = 7;//for sissrs the minimal p-value greater then zero = 1e-6
                else
                    score = -Math.log10( score );
            }
        }
        else
            throw new IllegalArgumentException();
        
        if(score > 10)
            score = 10;
        if(score < 0)
            score = 0;
        
        SiteReference siteReference = new SiteReference( track, site.getName() );
        
        String uniprotId = site.getProperties().getValueAsString( "uniprotId" );
        
        return new GTRDPeak( interval.getFrom(), interval.getTo(), score, siteReference, uniprotId );
    }
    
    public void translateToRelativeAndTruncate(Interval interval, int start)
    {
        for(List<GTRDPeak> peaks : groups.values())
        {
            for(int i = 0; i < peaks.size(); i++)
            {
                GTRDPeak peak = peaks.get( i );
                int from = peak.getFrom() - interval.getFrom();
                if(from < 0)
                    from = 0;
                from += start;//make it one-based
                int to = peak.getTo() - interval.getFrom();
                if(to >= interval.getLength())
                    to = interval.getLength() - 1;
                to += start;//make it one-based
                GTRDPeak newPeak = new GTRDPeak( from , to, peak.getScore(), peak.getOriginalPeak(), peak.getTfUniprotId() );
                peaks.set( i, newPeak );
            }
            Collections.sort(peaks);
        }
    }
    
    public GTRDDataForTFClass getRevrseComplement(Interval interval, int start)
    {
        GTRDDataForTFClass result = new GTRDDataForTFClass( this.tfClass );
        groups.forEach( (key, peaks) ->{
            List<GTRDPeak> newPeaks = new ArrayList<>(peaks.size());
            for(int i = 0; i < peaks.size(); i++)
            {
                GTRDPeak peak = peaks.get( i );
                
                int from = interval.getTo() - peak.getTo();
                if(from < 0)
                    from = 0;
                from += start;//make it one-based
                
                int to = interval.getTo() - peak.getFrom();
                if(to >= interval.getLength())
                    to = interval.getLength() - 1;
                to += start;//make it one-based
                
                GTRDPeak newPeak = new GTRDPeak( from , to, peak.getScore(), peak.getOriginalPeak(), peak.getTfUniprotId() );
                newPeaks.add( newPeak );
            }
            Collections.sort(newPeaks);
            result.groups.put( key, newPeaks );
        } );
        return result;
    }
}
