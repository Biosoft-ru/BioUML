package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.faire.FAIREPeak;
import ru.biosoft.access.core.DataCollection;

public class FAIREPeaksCollection<P extends FAIREPeak> extends PeaksCollection<FAIREExperiment, P>
{
    public FAIREPeaksCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }
    
    @Override
    protected Map<String, FAIREExperiment> getExperimentsCollectionFromMetadata(Metadata meta)
    {
        return meta.faireExperiments;
    }
    
}
