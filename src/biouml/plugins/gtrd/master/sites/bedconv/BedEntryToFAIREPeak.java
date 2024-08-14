package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.faire.FAIREPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class BedEntryToFAIREPeak<P extends FAIREPeak> extends BedEntryToPeak<P, FAIREExperiment>
{
    protected BedEntryToFAIREPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }
    
    @Override
    protected FAIREExperiment getExpFromMetadata(Metadata meta, String expId)
    {
        return meta.faireExperiments.get( experimentId );
    }
    
    @Override
    protected FAIREExperiment createStubExp()
    {
        String name = experimentId == null ? "FEXP??????" : experimentId;
        return new FAIREExperiment( null, name );
    }
}
