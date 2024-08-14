package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import ru.biosoft.access.core.DataCollection;

public class ChIPSeqPeaksCollection<P extends ChIPSeqPeak> extends PeaksCollection<ChIPseqExperiment, P>
{
    public ChIPSeqPeaksCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }
    
    @Override
    protected Map<String, ChIPseqExperiment> getExperimentsCollectionFromMetadata(Metadata meta)
    {
        return meta.chipSeqExperiments;
    }
    
    
}
