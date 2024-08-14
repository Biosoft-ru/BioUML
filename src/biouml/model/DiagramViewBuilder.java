package biouml.model;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.swing.Icon;

import ru.biosoft.graph.PortFinder;
import ru.biosoft.graph.ShapeChanger;
import ru.biosoft.graphics.CompositeView;

public interface DiagramViewBuilder
{
    /**
     * Creates the icon to be used in toolbar for creation corresponding type of diagram element.
     */
    public Icon getIcon(Object type);

    //////////////////////////////////////////////////////////////////
    // Methods to build the diagram view
    //

    public @Nonnull CompositeView createDiagramView(Diagram diagram, Graphics g);

    public @Nonnull CompositeView createCompartmentView(Compartment compartment, DiagramViewOptions options, Graphics g);

    public @Nonnull CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g);

    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g);

    public DiagramViewOptions createDefaultDiagramViewOptions();

    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g);
    
    public void setBaseViewBuilder(DiagramViewBuilder baseViewBuilder);
    
    public void setBaseViewOptions(DiagramViewOptions baseViewOptions);
    
    public void setTypeMapping(Map<Object, String> typeMapping);
    
    public boolean calculateInOut(Edge edge, Point in, Point out);
    
    public PortFinder getPortFinder(Node node);
    
    /**
     * Some nodes may have change their shape during layout (e.g. orientation)
     */
    public ShapeChanger getShapeChanger(Node node);
    
    /**
     * Method is used for layout issues<br>
     * It rebuilds node view if necessary and returns it with location from node.getLoaction()
     * 
     * @param node
     * @return
     */
    public Rectangle getNodeBounds(Node node);

    /**
     * Calculates point closest to the desired point and located on node bounds
     */
    public Point getNearestNodePoint(Point desired, Node node);
    
    /**
     * If false then given node can be represented by custom image defined by user 
     */
    public boolean forbidCustomImage(Node node);
 
}
