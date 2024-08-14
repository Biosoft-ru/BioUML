package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.faire.MACS2FAIREPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToMACS2FAIREPeak extends BedEntryToFAIREPeak<MACS2FAIREPeak>
{
    
    public BedEntryToMACS2FAIREPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected MACS2FAIREPeak createPeak()
    {
        return new MACS2FAIREPeak();
    }

    @Override
    protected void updatePeakFromColumns(MACS2FAIREPeak peak, String[] columns)
    {
        peak.setSummit( Integer.parseInt( columns[0] ) - peak.getFrom() + 1 );
        peak.setPileup( Float.parseFloat( columns[1] ) );
        peak.setMLog10PValue( Float.parseFloat( columns[2] ) );
        peak.setFoldEnrichment( Float.parseFloat( columns[3] ) );
        peak.setMLog10QValue( Float.parseFloat( columns[4] ) );
        peak.setId( Integer.parseInt( columns[5] ) );
    }

    @Override
    protected String createRestStringFromPeak(MACS2FAIREPeak peak)
    {
        return (peak.getSummit() + peak.getFrom() - 1) + "\t" +
                peak.getPileup() + "\t" +
                peak.getMLog10PValue() + "\t" +
                peak.getFoldEnrichment() + "\t" +
                peak.getMLog10QValue() + "\t" +
                peak.getId();
    }
}
