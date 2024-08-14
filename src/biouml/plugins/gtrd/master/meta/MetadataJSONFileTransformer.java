package biouml.plugins.gtrd.master.meta;

import java.io.File;

import biouml.plugins.gtrd.master.meta.json.MetadataSerializer;
import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;

public class MetadataJSONFileTransformer extends AbstractFileTransformer<Metadata>
{
    @Override
    public Class<Metadata> getOutputType()
    {
        return Metadata.class;
    }

    @Override
    public Metadata load(File input, String name, DataCollection<Metadata> origin) throws Exception
    {
       Metadata result = MetadataSerializer.readMetadata( input.toPath() );
       result.setOrigin( origin );
       result.setName(name);
       return result;
    }

    @Override
    public void save(File output, Metadata metadata) throws Exception
    {
        MetadataSerializer.writeMetadata( metadata, output.toPath() );
    }
}
