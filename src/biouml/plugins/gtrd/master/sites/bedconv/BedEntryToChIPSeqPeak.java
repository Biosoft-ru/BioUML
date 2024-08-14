package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class BedEntryToChIPSeqPeak<P extends ChIPSeqPeak> extends BedEntryToPeak<P, ChIPseqExperiment>
{
    protected BedEntryToChIPSeqPeak(BigBedTrack<?> origin, Properties props)
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
    protected ChIPseqExperiment getExpFromMetadata(Metadata meta, String expId)
    {
        return meta.chipSeqExperiments.get( experimentId );
    }
    
    @Override
    protected ChIPseqExperiment createStubExp()
    {
        String name = experimentId == null ? "EXP??????" : experimentId;
        return new ChIPseqExperiment( null, name );
    }
}
