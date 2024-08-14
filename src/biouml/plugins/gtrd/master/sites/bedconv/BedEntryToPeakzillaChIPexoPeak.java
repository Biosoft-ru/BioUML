package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeakzillaPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToPeakzillaChIPexoPeak extends BedEntryToChIPexoPeak<ChIPexoPeakzillaPeak>
{
    public BedEntryToPeakzillaChIPexoPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected ChIPexoPeakzillaPeak createPeak()
    {
        return new ChIPexoPeakzillaPeak();
    }

    @Override
    protected void updatePeakFromColumns(ChIPexoPeakzillaPeak peak, String[] columns)
    {
        peak.setSummit( Integer.parseInt( columns[2] ) - peak.getFrom() + 1 );
        peak.setPeakZillaScore( Float.parseFloat( columns[3] ) );
        peak.setChip( Float.parseFloat( columns[4] ) );
        peak.setControl( Float.parseFloat( columns[5] ) );
        peak.setFoldEnrichment( Float.parseFloat( columns[6] ) );
        peak.setDistributionScore( Float.parseFloat( columns[7] ) );
        peak.setFdr( Float.parseFloat( columns[8] ) );
        peak.setId( Integer.parseInt( columns[9] ) );
    }

    @Override
    protected String createRestStringFromPeak(ChIPexoPeakzillaPeak peak)
    {
        String res = super.createRestStringFromPeak(peak);
        
        return res + "\t" + (peak.getSummit() + peak.getFrom() - 1)//-1 to make it zero based
                   + "\t" + peak.getPeakZillaScore()
                   + "\t" + peak.getChip()
                   + "\t" + peak.getControl()
                   + "\t" + peak.getFoldEnrichment()
                   + "\t" + peak.getDistributionScore()
                   + "\t" + peak.getFdr()
                   + "\t" + peak.getId();
    }
}
