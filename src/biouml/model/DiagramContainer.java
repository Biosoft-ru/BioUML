package biouml.model;

import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public abstract class DiagramContainer extends Compartment
{
    protected Diagram diagram;
    public DiagramContainer(DataCollection origin, Diagram diagram, Base kernel)
    {
        super( origin, kernel );
        this.diagram = diagram;
    }
    
    public Diagram getDiagram()
    {
        return diagram;
    }
    
    public boolean isDiagramMutable()
    {
        return true;
    }
}
