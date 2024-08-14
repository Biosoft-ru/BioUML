package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.chipseq.PICSPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToPICSChipSeqPeak extends BedEntryToChIPSeqPeak<PICSPeak>
{
    
    public BedEntryToPICSChipSeqPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected PICSPeak createPeak()
    {
        return new PICSPeak();
    }

    @Override
    protected void updatePeakFromColumns(PICSPeak peak, String[] columns)
    {
        peak.setPicsScore( Float.parseFloat( columns[2] ) );
        peak.setId( Integer.parseInt( columns[3] ) );
    }

    @Override
    protected String createRestStringFromPeak(PICSPeak peak)
    {
        String res = super.createRestStringFromPeak(peak);
        return res + "\t" + peak.getPicsScore() + "\t" + peak.getId();
    }
}
