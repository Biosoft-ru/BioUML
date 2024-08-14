package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;

public class DiagramReference extends Concept
{
    public DiagramReference(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_DIAGRAM_REFERENCE;
    }
    
    public String getDisplayName()
    {
        return getTitle();
    }
}
