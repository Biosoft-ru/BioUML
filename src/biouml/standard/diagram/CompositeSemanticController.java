package biouml.standard.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.EModel;
import biouml.standard.diagram.properties.ConstantElementProperties;
import biouml.standard.diagram.properties.SwitchElementProperties;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.standard.type.Stub.ConnectionPort;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class CompositeSemanticController extends PathwaySimulationSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled(false);

        try
        {

            if( type instanceof Class )
            {
                Class<?> typeClass = (Class<?>)type;
         
                if( typeClass == Stub.DirectedConnection.class || typeClass == Stub.UndirectedConnection.class )
                {
                    parent.setNotificationEnabled(true);
                    CreateEdgeDialog dialog = CreateEdgeDialog.getConnectionDialog(Module.getModule(parent), pt, viewEditor, typeClass,
                            parent);
                    dialog.setVisible(true);
                    return DiagramElementGroup.EMPTY_EG;
                }
            }
            return super.createInstance(parent, type, pt, viewEditor);
        }
        finally
        {
            parent.setNotificationEnabled(isNotificationEnabled);
        }
    }
    
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, Point point, Object properties)
    {
        try
        {
            if( type.equals( ConnectionPort.class ) && properties instanceof PortProperties )
            {
                PortProperties portProperties = (PortProperties)properties;
                return portProperties.createElements( compartment, point, null );
            }
            else if( type.equals( SubDiagram.class ) && properties instanceof SubDiagramProperties )
            {
                SubDiagramProperties subDiagramProperties = (SubDiagramProperties)properties;
                return  subDiagramProperties.createElements( compartment, point, null );
            }
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "While creating instance of type " + type.toString(), e );
        }
        return super.createInstance( compartment, type, point, properties );
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if( compartment instanceof SubDiagram )
        {
            return false;
        }
        if( de instanceof Node && ( Util.isBus( de ) || Util.isPlot( de ) || Util.isSwitch( de ) || de instanceof SubDiagram ) )
        {
            return true;
        }
        return super.canAccept(compartment, de);
    }

    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( de instanceof Diagram )
            return false;

        if( de instanceof SubDiagram )
        {
            for( Edge edge : getEdges( (Node)de ) )
                edge.getCompartment().remove( edge.getName() ); //force remove
            de.getOrigin().remove(de.getName());
            return true;
        }
        if( de instanceof Edge )
        {
            Edge edge = (Edge)de;
            if( edge.getKernel().getType().equals(Type.TYPE_DIRECTED_LINK) || edge.getKernel().getType().equals(Type.TYPE_UNDIRECTED_LINK) )
            {

                if( ( Util.isModulePort(edge.getInput()) && Util.isPropagatedPort(edge.getOutput()) )
                        || ( Util.isModulePort(edge.getOutput()) && Util.isPropagatedPort(edge.getInput()) ) )
                    return false;

                edge.getOrigin().remove(edge.getName());
                return true;
            }
        }
        if( Util.isModulePort(de) )
        {
            return false;
        }
        return super.remove(de);
    }

    /**
     * Moves the specified diagram element to the specified position. If neccessary
     * compartment can be changed.
     *
     * @returns the actual distance on which the diagram node was moved.
     */
    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if( Util.isModulePort(de) )
        {
            Compartment compartment = (Compartment)de.getOrigin();
            boolean keepOrientation = offset.width == 0 && offset.height == 0;
            movePortToEdge( (Node)de, compartment, offset, keepOrientation );
            for( Edge edge : ( (Node)de ).getEdges() )
                recalculateEdgePath(edge);
            return offset;
        }
        else if( de instanceof DiagramContainer )
        {
            Compartment compartment = (Compartment)de;
            Point location = compartment.getLocation();
            location.translate(offset.width, offset.height);

            boolean notification = compartment.isNotificationEnabled();
            compartment.setNotificationEnabled(false);//to avoid updating subDiagram

            compartment.setLocation(location);

            compartment.setNotificationEnabled(notification);

            for( Node node : compartment.getNodes() )
            {
                Point nodeLocation = node.getLocation();
                nodeLocation.translate(offset.width, offset.height);
                node.setLocation(nodeLocation);
            }

            for( Edge edge : getEdges(compartment) )
            {
                this.recalculateEdgePath(edge);
            }
            return offset;
        }

        return super.move(de, newParent, offset, oldBounds);
    }

    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        if( diagramElement instanceof SubDiagram || diagramElement instanceof Node && Util.isAverager( diagramElement ) )
        {
            return true;
        }
        return super.isResizable(diagramElement);
    }

    /**
     * Move port to compartment border and set correctly orientation
     */
    public static Dimension movePortToEdge(Node node, Compartment compartment, Dimension offset, boolean keepOrientation)
    {
        Point oldLocation = node.getLocation();
        Point newLocation = new Point(oldLocation);
        newLocation.translate(offset.width, offset.height);
        movePortToEdge(node, compartment, newLocation, keepOrientation);
        node.setLocation(newLocation);
        return new Dimension(oldLocation.x - newLocation.x, oldLocation.y - newLocation.y);
    }

    public static void movePortToEdge(Node node, Compartment compartment, Point location, boolean keepOrientation)
    {
        Rectangle domain = new Rectangle( ( compartment instanceof Diagram ) ? new Point(0, 0): compartment.getLocation(), compartment.getShapeSize());
        movePortToEdge(node, domain, location, keepOrientation);
    }

    public static void movePortToEdge(Node node, Rectangle domain, Point location, boolean keepOrientation)
    {
        Dimension nodeSize;
        if( node.getView() == null )
            nodeSize = node.getShapeSize();
        else
            nodeSize = node.getView().getBounds().getSize();

        PortOrientation orientation = Util.getPortOrientation(node);

        if( !keepOrientation )
        {
            double leftDistance = location.x - domain.x; //distance to the left compartment boundary
            double rightDistance = ( domain.x + domain.width ) - ( location.x + nodeSize.width );//distance to the right compartment boundary
            double topDistance = location.y - domain.y;//distance to the top compartment boundary
            double bottomDistance = ( domain.y + domain.height ) - ( location.y + nodeSize.height );//distance to the boundary compartment boundary

            double distance = Double.MAX_VALUE;

            if( leftDistance < distance )
            {
                distance = leftDistance;
                orientation = PortOrientation.LEFT;
            }
            if( rightDistance < distance )
            {
                distance = rightDistance;
                orientation = PortOrientation.RIGHT;
            }
            if( topDistance < distance )
            {
                distance = topDistance;
                orientation = PortOrientation.TOP;
            }
            if( bottomDistance < distance )
            {
                orientation = PortOrientation.BOTTOM;
            }

            if( !orientation.equals(Util.getPortOrientation(node)) )
            {
                node.getAttributes().setValue(PortOrientation.ORIENTATION_ATTR, orientation);

                //updating node size after orientation changing
                Diagram diagram = Diagram.getDiagram(node);
                DiagramViewOptions viewOptions = diagram.getViewOptions();
                DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
                View view = viewBuilder.createNodeView(node, viewOptions, ApplicationUtils.getGraphics());

                nodeSize = view.getBounds().getSize();
            }
        }

        switch( orientation )
        {
            case TOP:
                location.y = domain.y;
                location.x = Math.min( Math.max(domain.x, location.x), domain.x + domain.width- nodeSize.width);
                break;
            case RIGHT:
                location.x = domain.x + domain.width - nodeSize.width;
                location.y = Math.min( Math.max(domain.y, location.y), domain.y + domain.height- nodeSize.height);
                break;
            case BOTTOM:
                location.y = domain.y + domain.height - nodeSize.height;
                location.x = Math.min( Math.max(domain.x, location.x), domain.x + domain.width- nodeSize.width);
                break;
            case LEFT:
                location.x = domain.x;
                location.y = Math.min( Math.max(domain.y, location.y), domain.y + domain.height- nodeSize.height);
                break;
        }
    }

    /**
     * Method returns all edges connected to node or its ports (and only ports!)
     * @param node - SubDiagram or port node
     * @return
     */
    public static List<Edge> getEdges(Node node)
    {
        return node.recursiveStream().select( Node.class ).filter( n -> Util.isPort( n ) || n instanceof Compartment )
                .flatMap( Node::edges ).toList();
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type.equals(Stub.Bus.class.getName()) || type instanceof Class && Stub.Bus.class.isAssignableFrom((Class<?>)type) )
        {
            //            String name = generateUniqueNodeName(compartment, "bus");
            return new SimpleBusProperties( Diagram.getDiagram( compartment ) );
            //            return new BusProperties((EModel)Diagram.getDiagram(compartment).getRole(), name);
        }
        else if( ( type instanceof Class && SubDiagram.class.isAssignableFrom( (Class<?>)type ) )
                || type.equals( SubDiagram.class.getName() ) )
        {
            return new SubDiagramProperties(Diagram.getDiagram(compartment));
        }
        else if( type.equals( Stub.SwitchElement.class.getName() ) || type.equals( Stub.SwitchElement.class ) )
        {
            return new SwitchElementProperties(generateUniqueNodeName(Diagram.getDiagram(compartment), "Switcher"));
        }
        else if( type.equals( Stub.Constant.class.getName() ) || type.equals( Stub.Constant.class ) )
        {
            return new ConstantElementProperties(generateUniqueNodeName(Diagram.getDiagram(compartment), "Constant"));
        }
        return super.getPropertiesByType(compartment, type, point);
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de) throws Exception
    {
        if( de.getKernel() instanceof biouml.standard.type.Stub.Bus && de.getRole() == null )
            de.setRole( new Bus( de.getName(), false ) );
        return de;
    }
}
