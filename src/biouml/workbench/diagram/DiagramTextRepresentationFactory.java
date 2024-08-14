package biouml.workbench.diagram;

import java.util.Objects;

import biouml.model.Diagram;
import ru.biosoft.access.exception.DataElementNotAcceptableException;
import ru.biosoft.util.ObjectExtensionRegistry;

public abstract class DiagramTextRepresentationFactory
{
    private static final ObjectExtensionRegistry<DiagramTextRepresentationFactory> registry = new
            ObjectExtensionRegistry<>( "biouml.workbench.diagramText", DiagramTextRepresentationFactory.class );
    
    /**
     * @param diagram diagram to create DiagramTextRepresentation for
     * @return DiagramTextRepresentation for diagram or null if this factory cannot work with given diagram
     */
    protected abstract DiagramTextRepresentation create(Diagram diagram);
    
    public static boolean hasDiagramTextRepresentation(Diagram d)
    {
        return registry.stream().map( factory -> factory.create( d ) ).anyMatch( Objects::nonNull );
    }

    /**
     * @param d diagram to get DiagramTextRepresentation object
     * @return DiagramTextRepresentation object bound to given diagram
     * @throws DataElementNotAcceptableException if no DiagramTextRepresentationFactory wants to work with this diagram
     * @throws NullPointerException if d is null
     */
    public static DiagramTextRepresentation getDiagramTextRepresentation(Diagram d) throws DataElementNotAcceptableException
    {
        return registry.stream().map( factory -> factory.create( d ) ).nonNull().findAny()
                .orElseThrow( () -> new DataElementNotAcceptableException( d.getCompletePath(), "No text representation on this diagram" ) );
    }
    
}
