package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.chipseq.SISSRSPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToSISSRSChipSeqPeak extends BedEntryToChIPSeqPeak<SISSRSPeak>
{
    public BedEntryToSISSRSChipSeqPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected SISSRSPeak createPeak()
    {
        return new SISSRSPeak();
    }

    @Override
    protected void updatePeakFromColumns(SISSRSPeak peak, String[] columns)
    {
        peak.setNumTags( Integer.parseInt( columns[2] ) );
        int i = 3;
        if(columns.length >= 5)
        {
            peak.setFold( Float.parseFloat( columns[3] ) );
            peak.setPValue( Float.parseFloat( columns[4] ) );
            i += 2;
        }
        peak.setId( Integer.parseInt( columns[i] ) );
    }

    @Override
    protected String createRestStringFromPeak(SISSRSPeak peak)
    {
        String res = super.createRestStringFromPeak(peak);
        return res + "\t" + peak.getNumTags() 
                   + "\t" + peak.getFold()
                   + "\t" + peak.getPValue()
                   + "\t" + peak.getId();
    }
}
