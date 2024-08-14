package biouml.plugins.gtrd.master.sites.bedconv;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import ru.biosoft.bsa.track.big.BigBedTrack;
import biouml.plugins.gtrd.master.sites.dnase.MACS2DNaseCluster;

public class BedEntryToMACS2DNaseCluster extends BedEntryToDNaseCluster<MACS2DNaseCluster>
{
    public BedEntryToMACS2DNaseCluster(BigBedTrack<?> origin, CellLine cell, Design design)
    {
        super( origin, cell, design );
    }
    
    @Override
    protected MACS2DNaseCluster createCluster()
    {
        return new MACS2DNaseCluster();
    }
    
    @Override
    protected void parseRestString(MACS2DNaseCluster c, String[] parts)
    {
        c.setPeakCount( Integer.parseInt( parts[0] ) );
        c.setMeanAbsSummit( Float.parseFloat( parts[1] ) );
        c.setMedianAbsSummit( Float.parseFloat( parts[2] ) );
        c.setMeanPileup( Float.parseFloat( parts[3] ) );
        c.setMedianPileup( Float.parseFloat( parts[4] ) );
        c.setMeanFoldEnrichment( Float.parseFloat( parts[5] ) );
        c.setMedianFoldEnrichment( Float.parseFloat( parts[6] ) );
        c.setId( Integer.parseInt( parts[7] ) );
    }
    
    @Override
    protected String getRestString(MACS2DNaseCluster c)
    {
        StringBuilder sb = new StringBuilder();
        sb.append( c.getPeakCount() ).append( '\t' )
          .append( c.getMeanAbsSummit() ).append( '\t' )
          .append( c.getMedianAbsSummit() ).append( '\t' )
          .append( c.getMeanPileup() ).append( '\t' )
          .append( c.getMedianPileup() ).append( '\t' )
          .append( c.getMeanFoldEnrichment() ).append( '\t' )
          .append( c.getMedianFoldEnrichment() ).append( '\t' )
          .append( c.getId() );
        return sb.toString();
    }
}
