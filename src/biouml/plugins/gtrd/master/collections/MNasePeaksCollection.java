package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.mnase.MNasePeak;
import ru.biosoft.access.core.DataCollection;

public class MNasePeaksCollection<P extends MNasePeak> extends PeaksCollection<MNaseExperiment, P>
{
    public MNasePeaksCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }
    
    @Override
    protected Map<String, MNaseExperiment> getExperimentsCollectionFromMetadata(Metadata meta)
    {
        return meta.mnaseExperiments;
    }
    
    
}
