package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.histones.MACS2HistonesPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToMACS2HistonesPeak extends BedEntryToHistonesPeak<MACS2HistonesPeak>
{
    
    public BedEntryToMACS2HistonesPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected MACS2HistonesPeak createPeak()
    {
        return new MACS2HistonesPeak();
    }

    @Override
    protected void updatePeakFromColumns(MACS2HistonesPeak peak, String[] columns)
    {
        int i = 0;
        //columns[0] is a target histone modification name, skip it
        i++;
        
        boolean hasSummit = columns.length > 6;
        if(hasSummit)
            peak.setSummit(  Integer.parseInt( columns[i++] ) - peak.getFrom() + 1 );
        
        peak.setPileup( Float.parseFloat( columns[i++] ) );
        peak.setMLog10PValue( Float.parseFloat( columns[i++] ) );
        peak.setFoldEnrichment( Float.parseFloat( columns[i++] ) );
        peak.setMLog10QValue( Float.parseFloat( columns[i++] ) );
        peak.setId( Integer.parseInt( columns[i++] ) );
    }

    @Override
    protected String createRestStringFromPeak(MACS2HistonesPeak peak)
    {
        StringBuilder result = new StringBuilder();
        result.append( super.createRestStringFromPeak( peak ) );
        if(peak.hasSummit())
            result.append( '\t' ).append( String.valueOf(peak.getSummit() + peak.getFrom() - 1) );
        result.append( '\t' ).append( String.valueOf( peak.getPileup() ) );
        result.append( '\t' ).append( String.valueOf( peak.getMLog10PValue() ) );
        result.append( '\t' ).append( String.valueOf( peak.getFoldEnrichment() ) );
        result.append( '\t' ).append( String.valueOf( peak.getMLog10QValue() ) );
        result.append( '\t' ).append( String.valueOf( peak.getId() ) );
        return result.toString();
        
    }
}
