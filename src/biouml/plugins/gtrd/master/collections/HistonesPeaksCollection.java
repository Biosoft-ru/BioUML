package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.histones.HistonesPeak;
import ru.biosoft.access.core.DataCollection;

public class HistonesPeaksCollection<P extends HistonesPeak> extends PeaksCollection<HistonesExperiment, P>
{
    public HistonesPeaksCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }
    
    @Override
    protected Map<String, HistonesExperiment> getExperimentsCollectionFromMetadata(Metadata meta)
    {
        return meta.histoneExperiments;
    }
}
