package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.dnase.WellingtonFootprint;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class BedEntryToWellingtonFootprint<T extends WellingtonFootprint> extends BedEntryToDNasePeak<T>
{
    public BedEntryToWellingtonFootprint(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }

    @Override
    protected void updatePeakFromColumns(T peak, String[] columns)
    {
        peak.setWellingtonScore( Float.parseFloat( columns[0] ) );
        peak.setId( Integer.parseInt( columns[1] ) );
    }

    @Override
    protected String createRestStringFromPeak(T peak)
    {
        return  peak.getWellingtonScore() + "\t" +
                peak.getId();
    }
}
