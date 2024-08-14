package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.histones.HistonesPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class BedEntryToHistonesPeak<P extends HistonesPeak> extends BedEntryToPeak<P, HistonesExperiment>
{
    protected BedEntryToHistonesPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }

    @Override
    protected String createRestStringFromPeak(P peak)
    {
        return peak.getExp().getTarget();
    }
    
    @Override
    protected HistonesExperiment getExpFromMetadata(Metadata meta, String expId)
    {
        return meta.histoneExperiments.get( experimentId );
    }
    
    @Override
    protected HistonesExperiment createStubExp()
    {
        String name = experimentId == null ? "HEXP??????" : experimentId;
        return new HistonesExperiment( null, name );
    }
}
