package biouml.model;

import java.awt.Point;

import ru.biosoft.graphics.editor.ViewEditorPane;


/**
 * Properties returned by {@link biouml.model.SemanticController#getPropertiesByType(Compartment, Object, Point)} which can create element by themselves
 * @author lan
 */
public interface InitialElementProperties
{
    /**
     * Create elements and add them to diagram based on current properties
     * @param compartment compartment to add new elements
     * @param location point in the diagram coordinate system where to add new elements
     * @param viewPane view pane to use
     * @return DiagramElementGroup of created elements
     * @throws Exception
     */
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception;
}
