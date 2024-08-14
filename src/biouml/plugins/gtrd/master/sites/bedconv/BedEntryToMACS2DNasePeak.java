package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.master.sites.dnase.MACS2DNasePeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

/**
 * Example default.dat
ID      DPEAKS000352_2.bb
name    DPEAKS000352_2.bb
class   biouml.plugins.gtrd.master.BigBedTrack
SequencesCollection     databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38
file    DPEAKS000352_2.bb
plugins biouml.plugins.research;ru.biosoft.access;biouml.plugins.gtrd
ConvertChrNamesToEnsembl        true
BigBedConverter.class   biouml.plugins.gtrd.master.sites.bedconv.BedEntryToMACS2DNasePeak
BigBedConverter.ExperimentId    DEXP000352
BigBedConverter.ExperimentsPath databases/GTRD/Data/DNase experiments
BigBedConverter.Replicate       2
parent-collecion        data/Collaboration/testtest/Data/master_track/dnase peaks/macs2
configFile      DPEAKS000352_2.bb.node.config
modifiedDate    1600767299878
createdDate     1600767299878
driver  ru.biosoft.access.generic.RepositoryTypeDriver
//
 */
public class BedEntryToMACS2DNasePeak extends BedEntryToDNasePeak<MACS2DNasePeak>
{
    
    public BedEntryToMACS2DNasePeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected MACS2DNasePeak createPeak()
    {
        return new MACS2DNasePeak();
    }

    @Override
    protected void updatePeakFromColumns(MACS2DNasePeak peak, String[] columns)
    {
        peak.setSummit( Integer.parseInt( columns[0] ) - peak.getFrom() + 1 );
        peak.setPileup( Float.parseFloat( columns[1] ) );
        peak.setMLog10PValue( Float.parseFloat( columns[2] ) );
        peak.setFoldEnrichment( Float.parseFloat( columns[3] ) );
        peak.setMLog10QValue( Float.parseFloat( columns[4] ) );
        peak.setId( Integer.parseInt( columns[5] ) );
    }

    @Override
    protected String createRestStringFromPeak(MACS2DNasePeak peak)
    {
        return (peak.getSummit() + peak.getFrom() - 1) + "\t" +
                peak.getPileup() + "\t" +
                peak.getMLog10PValue() + "\t" +
                peak.getFoldEnrichment() + "\t" +
                peak.getMLog10QValue() + "\t" +
                peak.getId();
    }
}
