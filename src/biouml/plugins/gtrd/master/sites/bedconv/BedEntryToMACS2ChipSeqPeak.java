package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.chipseq.MACS2ChIPSeqPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToMACS2ChipSeqPeak extends BedEntryToChIPSeqPeak<MACS2ChIPSeqPeak>
{
    
    public BedEntryToMACS2ChipSeqPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected MACS2ChIPSeqPeak createPeak()
    {
        return new MACS2ChIPSeqPeak();
    }

    @Override
    protected void updatePeakFromColumns(MACS2ChIPSeqPeak peak, String[] columns)
    {
        peak.setSummit( Integer.parseInt( columns[2] ) - peak.getFrom() + 1 );
        peak.setPileup( Float.parseFloat( columns[3] ) );
        peak.setMLog10PValue( Float.parseFloat( columns[4] ) );
        peak.setFoldEnrichment( Float.parseFloat( columns[5] ) );
        peak.setMLog10QValue( Float.parseFloat( columns[6] ) );
        peak.setId( Integer.parseInt( columns[7] ) );
    }

    @Override
    protected String createRestStringFromPeak(MACS2ChIPSeqPeak peak)
    {
        return super.createRestStringFromPeak( peak ) + "\t" +
                (peak.getSummit() + peak.getFrom() - 1) + "\t" +
                peak.getPileup() + "\t" +
                peak.getMLog10PValue() + "\t" +
                peak.getFoldEnrichment() + "\t" +
                peak.getMLog10QValue() + "\t" +
                peak.getId();
    }
}
