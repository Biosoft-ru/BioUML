package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.mnase.Danpos2MNasePeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToDanpos2MNasePeak extends BedEntryToMNasePeak<Danpos2MNasePeak>
{
    public BedEntryToDanpos2MNasePeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected Danpos2MNasePeak createPeak()
    {
        return new Danpos2MNasePeak();
    }

    @Override
    protected void updatePeakFromColumns(Danpos2MNasePeak peak, String[] columns)
    {
        peak.setSummit( Integer.parseInt( columns[0] ) + 1 - peak.getFrom() );
        peak.setSummitValue( Float.parseFloat( columns[1] ) );
        peak.setFuzzinessScore( Float.parseFloat( columns[2] ) );
        peak.setId( Integer.parseInt( columns[3] ) );
    }

    @Override
    protected String createRestStringFromPeak(Danpos2MNasePeak peak)
    {
        return (peak.getSummit() + peak.getFrom() - 1)
                   + "\t" + peak.getSummitValue()
                   + "\t" + peak.getFuzzinessScore()
                   + "\t" + peak.getId();
    }
}
