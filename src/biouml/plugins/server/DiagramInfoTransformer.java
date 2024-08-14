package biouml.plugins.server;

import ru.biosoft.access.support.BeanInfoEntryTransformer;
import biouml.standard.type.DiagramInfo;

/**
 * Utility class for DiagramClient
 */
public class DiagramInfoTransformer extends BeanInfoEntryTransformer<DiagramInfo>
{
    @Override
    public Class<DiagramInfo> getOutputType()
    {
        return DiagramInfo.class;
    }
}
