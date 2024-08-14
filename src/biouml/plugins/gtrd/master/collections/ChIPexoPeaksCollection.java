package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import ru.biosoft.access.core.DataCollection;

public class ChIPexoPeaksCollection<P extends ChIPexoPeak> extends PeaksCollection<ChIPexoExperiment, P>
{
    public ChIPexoPeaksCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }
    
    @Override
    protected Map<String, ChIPexoExperiment> getExperimentsCollectionFromMetadata(Metadata meta)
    {
        return meta.chipExoExperiments;
    }
    
    
}
