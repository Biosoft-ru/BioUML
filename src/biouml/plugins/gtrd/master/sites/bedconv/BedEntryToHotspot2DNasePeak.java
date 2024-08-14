package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.dnase.Hotspot2DNasePeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToHotspot2DNasePeak extends BedEntryToDNasePeak<Hotspot2DNasePeak>
{
    
    public BedEntryToHotspot2DNasePeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected Hotspot2DNasePeak createPeak()
    {
        return new Hotspot2DNasePeak();
    }

    @Override
    protected void updatePeakFromColumns(Hotspot2DNasePeak peak, String[] columns)
    {
        peak.setScore1( Float.parseFloat( columns[0] ) );
        peak.setScore2( Float.parseFloat( columns[1] ) );
        peak.setId( Integer.parseInt( columns[2] ) );
    }

    @Override
    protected String createRestStringFromPeak(Hotspot2DNasePeak peak)
    {
        return peak.getScore1() + "\t" +
               peak.getScore2() + "\t" +
               peak.getId();
    }
}
