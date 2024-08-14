package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.atac.MACS2ATACPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToMACS2ATACPeak extends BedEntryToATACPeak<MACS2ATACPeak>
{
    
    public BedEntryToMACS2ATACPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected MACS2ATACPeak createPeak()
    {
        return new MACS2ATACPeak();
    }

    @Override
    protected void updatePeakFromColumns(MACS2ATACPeak peak, String[] columns)
    {
        peak.setSummit( Integer.parseInt( columns[0] ) - peak.getFrom() + 1 );
        peak.setPileup( Float.parseFloat( columns[1] ) );
        peak.setMLog10PValue( Float.parseFloat( columns[2] ) );
        peak.setFoldEnrichment( Float.parseFloat( columns[3] ) );
        peak.setMLog10QValue( Float.parseFloat( columns[4] ) );
        peak.setId( Integer.parseInt( columns[5] ) );
    }

    @Override
    protected String createRestStringFromPeak(MACS2ATACPeak peak)
    {
        return (peak.getSummit() + peak.getFrom() - 1) + "\t" +
                peak.getPileup() + "\t" +
                peak.getMLog10PValue() + "\t" +
                peak.getFoldEnrichment() + "\t" +
                peak.getMLog10QValue() + "\t" +
                peak.getId();
    }
}
