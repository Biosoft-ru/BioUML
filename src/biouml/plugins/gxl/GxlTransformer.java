package biouml.plugins.gxl;

import java.io.File;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;

public class GxlTransformer extends AbstractFileTransformer<Diagram>
{
    /**
     * Return class of output data element.
     * Output data element stored in transformed data collection.
     * @return Class of output data element.
     */
    @Override
    public Class<Diagram> getOutputType()
    {
        return Diagram.class;
    }

    @Override
    public Diagram load(File input, String name, DataCollection<Diagram> origin) throws Exception
    {
        return new GxlReader().readDiagram(input, origin);
    }

    @Override
    public void save(File output, Diagram element) throws Exception
    {
        new GxlWriter().writeDiagram(output, element);
    }
}
