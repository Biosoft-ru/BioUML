package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoGEMPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class BedEntryToGEMChIPexoPeak extends BedEntryToChIPexoPeak<ChIPexoGEMPeak>
{
    public BedEntryToGEMChIPexoPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected ChIPexoGEMPeak createPeak()
    {
        return new ChIPexoGEMPeak();
    }

    @Override
    protected void updatePeakFromColumns(ChIPexoGEMPeak peak, String[] columns)
    {
        peak.setIp( Float.parseFloat( columns[2] ) );
        peak.setControl( Float.parseFloat( columns[3] ) );
        peak.setFold( Float.parseFloat( columns[4] ) );
        peak.setExpected( Float.parseFloat( columns[5] ) );
        peak.setPMLog10( Float.parseFloat( columns[6] ) );
        peak.setQMLog10( Float.parseFloat( columns[7] ) );
        peak.setPPoiss( Float.parseFloat( columns[8] ) );
        peak.setIpVsEmp( Float.parseFloat( columns[9] ) );
        peak.setNoise( Float.parseFloat( columns[10] ) );
        peak.setId( Integer.parseInt( columns[11] ) );
    }

    @Override
    protected String createRestStringFromPeak(ChIPexoGEMPeak peak)
    {
        String res = super.createRestStringFromPeak(peak);
        
        return res + "\t" + peak.getIp()
                   + "\t" + peak.getControl()
                   + "\t" + peak.getFold()
                   + "\t" + peak.getExpected()
                   + "\t" + peak.getPMLog10()
                   + "\t" + peak.getQMLog10()
                   + "\t" + peak.getPPoiss()
                   + "\t" + peak.getIpVsEmp()
                   + "\t" + peak.getNoise()
                   + "\t" + peak.getId();
    }
}
