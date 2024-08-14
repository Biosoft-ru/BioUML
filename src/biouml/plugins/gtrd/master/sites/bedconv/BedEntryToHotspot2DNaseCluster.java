package biouml.plugins.gtrd.master.sites.bedconv;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import ru.biosoft.bsa.track.big.BigBedTrack;
import biouml.plugins.gtrd.master.sites.dnase.Hotspot2DNaseCluster;

public class BedEntryToHotspot2DNaseCluster extends BedEntryToDNaseCluster<Hotspot2DNaseCluster>
{
    public BedEntryToHotspot2DNaseCluster(BigBedTrack<?> origin, CellLine cell, Design design)
    {
        super( origin, cell, design );
    }
    
    @Override
    protected Hotspot2DNaseCluster createCluster()
    {
        return new Hotspot2DNaseCluster();
    }
    
    @Override
    protected void parseRestString(Hotspot2DNaseCluster c, String[] parts)
    {
        c.setPeakCount( Integer.parseInt( parts[0] ) );
        c.setId( Integer.parseInt( parts[1] ) );
    }
    
    @Override
    protected String getRestString(Hotspot2DNaseCluster c)
    {
        StringBuilder sb = new StringBuilder();
        sb.append( c.getPeakCount() ).append( '\t' )
          .append( c.getId() );
        return sb.toString();
    }
}
