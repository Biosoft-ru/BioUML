package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.dnase.WellingtonMACS2Footprint;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToWellingtonMACS2Footprint extends BedEntryToWellingtonFootprint<WellingtonMACS2Footprint>
{
    public BedEntryToWellingtonMACS2Footprint(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }

    @Override
    protected WellingtonMACS2Footprint createPeak()
    {
        return new WellingtonMACS2Footprint();
    }

}
