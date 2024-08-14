package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.dnase.WellingtonHotspot2Footprint;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToWellingtonHotspot2Footprint extends BedEntryToWellingtonFootprint<WellingtonHotspot2Footprint>
{
    public BedEntryToWellingtonHotspot2Footprint(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }

    @Override
    protected WellingtonHotspot2Footprint createPeak()
    {
        return new WellingtonHotspot2Footprint();
    }

}
