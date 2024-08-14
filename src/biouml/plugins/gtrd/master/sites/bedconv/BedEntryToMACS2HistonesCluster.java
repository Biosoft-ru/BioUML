package biouml.plugins.gtrd.master.sites.bedconv;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.histones.MACS2HistonesCluster;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToMACS2HistonesCluster extends BedEntryToHistonesCluster<MACS2HistonesCluster>
{

    public BedEntryToMACS2HistonesCluster(BigBedTrack<?> origin, CellLine cell, String target)
    {
        super( origin, cell, target );
    }

    @Override
    protected MACS2HistonesCluster createCluster()
    {
        return new MACS2HistonesCluster();
    }

    @Override
    protected void parseRestString(MACS2HistonesCluster c, String[] parts)
    {
        int i = 0;
        
        c.setPeakCount( Integer.parseInt( parts[i++] ) );
        
        if(parts.length > 6)
        {
            c.setMeanAbsSummit( Float.parseFloat( parts[i++] ) );
            c.setMedianAbsSummit( Float.parseFloat( parts[i++] ) );
        }
        
        c.setMeanPileup( Float.parseFloat( parts[i++] ) );
        c.setMedianPileup( Float.parseFloat( parts[i++] ) );
        c.setMeanFoldEnrichment( Float.parseFloat( parts[i++] ) );
        c.setMedianFoldEnrichment( Float.parseFloat( parts[i++] ) );
        c.setId( Integer.parseInt( parts[i++] ) );
    }

    @Override
    protected String getRestString(MACS2HistonesCluster c)
    {
        StringBuilder sb = new StringBuilder();
        sb.append( c.getPeakCount() );
        
        if(c.hasSummit())
            sb.append( '\t' ).append( c.getMeanAbsSummit() )
            .append( '\t' ).append( c.getMedianAbsSummit() );
        
        sb
        .append( '\t' ).append( c.getMeanPileup() )
        .append( '\t' ).append( c.getMedianPileup() )
        .append( '\t' ).append( c.getMeanFoldEnrichment() )
        .append( '\t' ).append( c.getMedianFoldEnrichment() )
        .append( '\t' ).append( c.getId() );
        
        return sb.toString();
    }

}