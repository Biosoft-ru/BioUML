package biouml.plugins.reactome;

import biouml.standard.type.DiagramReference;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class ReactomeDiagramReference extends DiagramReference
{
    public ReactomeDiagramReference(DataCollection<?> origin, String name)
    {
        super( origin, name );
    }
    @Override
    public String getType()
    {
        return "submap";
    }
}
