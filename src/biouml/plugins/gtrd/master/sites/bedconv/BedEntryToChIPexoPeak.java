package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class BedEntryToChIPexoPeak<P extends ChIPexoPeak> extends BedEntryToPeak<P, ChIPexoExperiment>
{
    protected BedEntryToChIPexoPeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
    }

    @Override
    protected String createRestStringFromPeak(P peak)
    {
        String tfTitle = exp != null ? exp.getTfTitle() : "";
        String uniprotId = exp != null ? exp.getTfUniprotId() : "";
        return tfTitle + "\t" + uniprotId;
    }
    
    @Override
    protected ChIPexoExperiment getExpFromMetadata(Metadata meta, String expId)
    {
        return meta.chipExoExperiments.get( experimentId );
    }
    
    @Override
    protected ChIPexoExperiment createStubExp()
    {
        String name = experimentId == null ? "EEXP??????" : experimentId;
        return new ChIPexoExperiment( null, name );
    }
}
