package biouml.plugins.gtrd.master.sites.bedconv;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import ru.biosoft.bsa.track.big.BigBedTrack;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonMACS2FootprintCluster;

public class BedEntryToWMACS2FootprintCluster extends BedEntryToFootprintCluster<WellingtonMACS2FootprintCluster>
{
    public BedEntryToWMACS2FootprintCluster(BigBedTrack<?> origin, CellLine cell, Design design)
    {
        super( origin, cell, design );
    }
    
    @Override
    protected WellingtonMACS2FootprintCluster createCluster()
    {
        return new WellingtonMACS2FootprintCluster();
    }
}
