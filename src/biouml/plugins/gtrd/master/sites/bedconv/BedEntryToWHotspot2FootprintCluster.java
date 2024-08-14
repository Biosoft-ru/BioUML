package biouml.plugins.gtrd.master.sites.bedconv;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import ru.biosoft.bsa.track.big.BigBedTrack;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonHotspot2FootprintCluster;

public class BedEntryToWHotspot2FootprintCluster extends BedEntryToFootprintCluster<WellingtonHotspot2FootprintCluster>
{
    public BedEntryToWHotspot2FootprintCluster(BigBedTrack<?> origin, CellLine cell, Design design)
    {
        super( origin, cell, design );
    }
    
    @Override
    protected WellingtonHotspot2FootprintCluster createCluster()
    {
        return new WellingtonHotspot2FootprintCluster();
    }
}
