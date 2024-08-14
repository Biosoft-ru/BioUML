package biouml.plugins.gtrd.master.sites.bedconv;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import ru.biosoft.bsa.track.big.BigBedTrack;
import biouml.plugins.gtrd.master.sites.dnase.FootprintCluster;

public abstract class BedEntryToFootprintCluster<T extends FootprintCluster> extends BedEntryToDNaseCluster<T>
{
    public BedEntryToFootprintCluster(BigBedTrack<?> origin, CellLine cell, Design design)
    {
        super( origin, cell, design );
    }
    
    @Override
    protected void parseRestString(T c, String[] parts)
    {
        c.setPeakCount( Integer.parseInt( parts[0] ) );
        c.setId( Integer.parseInt( parts[1] ) );
    }
    
    @Override
    protected String getRestString(T c)
    {
        StringBuilder sb = new StringBuilder();
        sb.append( c.getPeakCount() ).append( '\t' )
          .append( c.getId() );
        return sb.toString();
    }
}
