package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.atac.ATACPeak;
import ru.biosoft.access.core.DataCollection;

public class ATACPeaksCollection<P extends ATACPeak> extends PeaksCollection<ATACExperiment, P>
{
    public ATACPeaksCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }
    
    @Override
    protected Map<String, ATACExperiment> getExperimentsCollectionFromMetadata(Metadata meta)
    {
        return meta.atacExperiments;
    }
}
