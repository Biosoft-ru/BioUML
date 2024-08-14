package biouml.model;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.annotation.Nonnull;

import biouml.standard.type.Base;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.graphics.editor.ViewEditorPane;

public interface SemanticController
{
    /**
     * Return <code>true</code> only if this compartment can accept
     * this <code>DiagramElement</code> (depends on kernel's type)
     */
    public boolean canAccept(Compartment compartment, DiagramElement de);

    /**
     * Moves the specified node to the specified position. If necessary compartment can be changed.
     * @param node the diagram node
     * @param compartment compartment where node should be moved
     * @param offset distance for which node should be moved relative its initial location.
     * @returns the actual distance on which the node was moved.
     */
    public Dimension move(DiagramElement de, Compartment parent, Dimension offset, Rectangle oldBounds) throws Exception;

    /** Removes the diagram element and all related edges. */
    public boolean remove(DiagramElement de) throws Exception;

    /**
     * @returns whether a specified diagram element can be resized.
     */
    public boolean isResizable(DiagramElement diagramElement);

    /**
     *
     * @param de
     * @return
     */
    public Dimension resize(DiagramElement de, Dimension sizeChange);

    /**
    * Check if specified de element can be resized by sizeChange and moved by offset
    * @param de diagram element
    * @param sizeChange relative to initial element size
    * @param offset distance for which de should be moved relative to its location 
    * @return the actual size on which element can be resized
    */
    public Dimension resize(DiagramElement de, Dimension sizeChange, Dimension offset);

    /**
     * Creates new instance of <code>DiagramElement</code> with specific
     * kernel type
     */
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, Point point, ViewEditorPane viewEditor);

    /**
     * Creates new instance of <code>DiagramElement</code> with specific kernel type and properties
     * without using any interface dialogs.
     */
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, Point point, Object properties);

    /**
     * Gets the default properties of <code>DiagramElement</code> by its type for the correct element creation
     * by createInstance method
     */
    public Object getPropertiesByType(Compartment compartment, Object type, Point point);

    /**
     * Creates new instance of <code>DiagramElement</code> by specified ru.biosoft.access.core.DataElement (if applicable), adds it on diagram and returns it
     * Additional linked elements may be created as well.
     * It's prohibited to use any UI code in this method.
     * @return created DiagramElement  and all additional elements or empty DiagramElementGroup if specified path cannot yield any DiagramElement
     * @throws Exception if DiagramElement cannot be created and method has something to say to the user (i.e. such element already exists,
     *  it can be added but into another compartment, etc.)
     */
    public DiagramElementGroup addInstanceFromElement(Compartment compartment, DataElement dataElement, Point point, ViewEditorPane viewEditor)
            throws Exception;

    /**
     * Creates new path for the edge. This method is useful for notation
     * changing when node sizes changed.
     */
    public void recalculateEdgePath(Edge edge);

    /**
     * Validate create/change/delete event for diagram element.
     * Designed to call from base {@link SemanticController} when current controller is used as prototype.
     */
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception;

    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de) throws Exception;

    public Filter<DiagramElement> getFilter();

    /**
     *  Create clone of existing diagram node.
     *  The new node represents the same entity on the diagram. 
     *  Thus it should have the same kernel object, same role object, same compartment and be synchronized with initial node.
     */
    public Node cloneNode(Node element, String newName, Point location);
    
    /**
     * Create copy of existing diagram node. 
     * The new node is completely separate entity but copies attributes and properties of the initial node.
     */
    public Node copyNode(Node element, String newName, Compartment newParent, Point location);

    /**
     * Search for edge between nodes with the same kernel type
     * Is used to check whether edge already exists
     */
    public Edge findEdge(Node from, Node to, Base kernel);


    /**
     * Create edge element
     */
    public Edge createEdge(@Nonnull Node fromNode, @Nonnull Node toNode, String edgeType, Compartment compartment);

    /**
     * Check name and replace incorrect symbols
     * @param name
     * @return
     */
    public String validateName(String name);
    
    
    /**
     * Returns true if selected node can be reaction participant
     */
    public boolean isAcceptableForReaction(Node node);
}
