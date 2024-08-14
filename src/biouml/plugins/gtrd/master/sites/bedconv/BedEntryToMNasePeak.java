package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.mnase.MNasePeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class BedEntryToMNasePeak<P extends MNasePeak> extends BedEntryToPeak<P, MNaseExperiment>
{
    
    protected BedEntryToMNasePeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected MNaseExperiment getExpFromMetadata(Metadata meta, String expId)
    {
        return meta.mnaseExperiments.get( experimentId );
    }
    
    @Override
    protected MNaseExperiment createStubExp()
    {
        String name = experimentId == null ? "MEXP??????" : experimentId;
        return new MNaseExperiment( null, name );
    }
}
