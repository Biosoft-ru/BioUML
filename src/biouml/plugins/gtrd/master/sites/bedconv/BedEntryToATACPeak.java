package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.atac.ATACPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class BedEntryToATACPeak<P extends ATACPeak> extends BedEntryToPeak<P, ATACExperiment>
{
    protected BedEntryToATACPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected ATACExperiment getExpFromMetadata(Metadata meta, String expId)
    {
        return meta.atacExperiments.get( experimentId );
    }
    
    @Override
    protected ATACExperiment createStubExp()
    {
        String name = experimentId == null ? "AEXP??????" : experimentId;
        return new ATACExperiment( null, name );
    }
}
