package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.logging.Level;
import javax.annotation.Nonnull;

import ru.biosoft.graph.OrientedPortFinder;
import ru.biosoft.graph.PortFinder;
import ru.biosoft.graph.OrthogonalPathLayouter.Orientation;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.DirectedConnection;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.workbench.graph.OnePointFinder;

public class CompositeDiagramViewBuilder extends PathwaySimulationDiagramViewBuilder
{
    protected static final int BORDER = 10;
    protected static final int PORTS_DISTANCE = 15;

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new CompositeDiagramViewOptions(null);
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( Type.TYPE_SWITCH.equals(compartment.getKernel().getType()) )
            return createSwitchCoreView(container, compartment, (CompositeDiagramViewOptions)options, g);
        else if( compartment instanceof SubDiagram )
            return createSubDiagramView(container, (SubDiagram)compartment, (CompositeDiagramViewOptions)options, g);
        else
            return super.createCompartmentCoreView(container, compartment, options, g);
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( Type.TYPE_CONNECTION_BUS.equals(node.getKernel().getType()) )
            return createBusView(container, node, (CompositeDiagramViewOptions)options, g);

        if( Type.TYPE_CONSTANT.equals(node.getKernel().getType()) )
            return createConstantView(container, node, (CompositeDiagramViewOptions)options, g);

        return super.createNodeCoreView(container, node, options, g);
    }
    
    /**
     * Constant element on diagram
     */
    protected boolean createConstantView(CompositeView constantView, Node node, CompositeDiagramViewOptions options, Graphics g)
    {
        constantView.add(new BoxView(getBorderPen(node, options.getModulePen()), getBrush(node, options.getConstantBrush()), 0, 0, 60, 40));
        String value = node.getAttributes().getProperty(Util.INITIAL_VALUE).getValue().toString();
        constantView.add(new TextView(value, getTitleFont(node, options.getNodeTitleFont()), g), CompositeView.X_CC | CompositeView.Y_CC);
        return false;
    }

    public boolean createSwitchCoreView(CompositeView container, Compartment compartment, CompositeDiagramViewOptions options, Graphics g)
    {
        try
        {
            ComplexTextView titleView = null;
            if( compartment.isShowTitle() )
                titleView = new ComplexTextView( compartment.getTitle(), getTitleFont( compartment, options.getCompartmentTitleFont() ),
                        options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );

            Dimension size = compartment.getShapeSize();
            if( size == null )
                size = new Dimension(100, 80);

            container.add(new BoxView(getBorderPen(compartment, options.getModulePen()), getBrush(compartment, options.getSwitchBrush()), 0, 0, size.width, size.height));
            container.add(titleView, CompositeView.X_CC | CompositeView.Y_CC);
            container.setModel(compartment);
            container.setActive(true);
            container.setLocation(compartment.getLocation());
            compartment.setView(container);

        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return false;
    }

    protected boolean createBusView(CompositeView busView, Node node, CompositeDiagramViewOptions options, Graphics g)
    {
        Bus bus = node.getRole( Bus.class );
        Color color = bus.getColor();
        Pen pen = getBorderPen( node, options.getNodePen() );
        if( bus.isDirected() )
            busView.add( new BoxView( pen, new Brush( color ), 0, 0, 20, 20 ) );
        else
            busView.add( new EllipseView( pen, new Brush( color ), 0, 0, 20, 20 ) );
        return true;
    }

    protected boolean createSubDiagramView(CompositeView container, SubDiagram compartment, CompositeDiagramViewOptions options, Graphics g)
    {
        String title = compartment.getTitle();
        if( title == null || title.isEmpty() )
            title = compartment.getName();
        ComplexTextView titleView = new ComplexTextView(title, getTitleFont(compartment, options.getCompartmentTitleFont()), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );

        Dimension size = compartment.getShapeSize();
        if( size == null )
            size = new Dimension(50, 50);

        container.add(new BoxView(getBorderPen(compartment, options.getModulePen()),
                getBrush(compartment, options.getModuleBrush()), 0, 0, size.width, size.height));
        container.add(titleView, CompositeView.X_CC | CompositeView.Y_CC);

        Diagram diagram = compartment.getDiagram();
        ComplexTextView expView = null;
        if( diagram.getCurrentState() != null )
        {
            expView = new ComplexTextView(diagram.getCurrentStateName(), options.getStateTitleFont(), options.getFontRegistry(),
                    ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
            Point titleLocation = titleView.getLocation();
            int newY = titleLocation.y - ( expView.getBounds().height );
            expView.setLocation(expView.getLocation().x, newY + expView.getBounds().height);
            titleView.setLocation(titleLocation.x, newY);
            container.add(expView, CompositeView.X_CC | CompositeView.Y_CC);
        }

        container.setModel(compartment);
        container.setActive(true);
        container.setLocation(compartment.getLocation());
        compartment.setView(container);
        return false;
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        CompositeDiagramViewOptions options = (CompositeDiagramViewOptions)viewOptions;

        String type = edge.getKernel().getType();
        if( type.equals(Type.TYPE_DIRECTED_LINK) || type.equals(Type.TYPE_UNDIRECTED_LINK) || type.equals(Type.TYPE_NOTE_LINK) )
        {
            CompositeView view = new CompositeView();

            if( edge.getPath() == null )
                Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);

            SimplePath path = edge.getSimplePath();
            Pen pen = getBorderPen(edge, options.getConnectionPen());
            Brush brush = getBrush(edge, options.getConnectionBrush());

            ArrowView arrow = null;
            if( type.equals(Type.TYPE_DIRECTED_LINK) )
            {
                int w1 = 5;
                int w2 = 10;
                int h = 5;
                if( !options.isCollapsed() )
                {
                    w1 = 20;
                    w2 = 30;
                    h = 10;
                }
                arrow = new ArrowView(pen, brush, path, null, options.arrows? ArrowView.createArrowTip(pen, brush, w1, w2, h): null);

                // create function label
                Role edgeRole = edge.getRole();
                if( edgeRole instanceof DirectedConnection )
                {
                    DirectedConnection role = (DirectedConnection)edgeRole;
                    if( role.getFunction() != null )
                    {
                        View function = new ComplexTextView(role.getFunction(), getTitleFont(edge, options.getDefaultFont()), options.getFontRegistry(),
                                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
                        Point location = new Point(path.xpoints[path.npoints / 2] - function.getBounds().width / 2,
                                path.ypoints[path.npoints / 2] + 3);
                        function.setLocation(location);
                        function.setActive(true);
                        function.setModel(edge);
                        view.add(function);
                    }
                }
            }
            else if( type.equals(Base.TYPE_UNDIRECTED_LINK) )
            {
                arrow = new ArrowView(pen, brush, path, options.arrows? ArrowView.createEllipseTip(pen, brush, 2): null,
                        options.arrows? ArrowView.createEllipseTip(pen, brush, 2): null);
            }
            else if( type.equals(Base.TYPE_NOTE_LINK) )
            {
                arrow = new ArrowView(options.getNoteLinkPen(), null, path, 0, 0);
            }

            if( arrow != null )
            {
                view.add( arrow );
                arrow.setActive( true );
                arrow.setModel( edge );
            }

            // create title (if connection is between ports we do not need to create title
            if( edge.nodes().noneMatch( n -> n.getKernel() instanceof Stub.ConnectionPort ) )
            {
                View titleView = null;
                if( arrow != null && edge.getTitle() != null && edge.getTitle().length() > 0 )
                {
                    int alignment = 0;
                    float a = ( edge.getOutPort().y - edge.getInPort().y ) / ( edge.getOutPort().x - edge.getInPort().x + 0.1f );
                    if( Math.abs(a) < 0.2 )
                        alignment = CompositeView.X_CC | CompositeView.Y_TB;
                    else
                        alignment = CompositeView.X_RL | CompositeView.Y_CC;

                    Point middlePoint = arrow.getPathView().getMiddlePoint();
                    titleView = new TextView(edge.getTitle(), new Point(middlePoint.x, middlePoint.y), alignment,
                            getTitleFont(edge, options.connectionTitleFont), g);
                    titleView.move(titleView.getBounds().width / 2, 0);

                    if( edge.getPath().getBounds().intersects(titleView.getBounds()) )
                    {
                        titleView.move(0, -titleView.getBounds().height);
                    }

                    titleView.setActive(true);
                    titleView.setModel(edge);
                }
                if( titleView != null )
                    view.add(titleView);
            }
            view.setModel(edge);
            view.setActive(false);

            return view;
        }
        return super.createEdgeView(edge, viewOptions, g);
    }

    @Override
    public PortFinder getPortFinder(Node node)
    {
        if( Util.isModulePort( node ))
        {
            return new OnePointFinder(Util.getPortOrientation(node), getNodeBounds(node));
        }
        else if (Util.isPublicPort( node ) || Util.isPropagatedPort( node ))
        {
            return new OnePointFinder(Util.getPortOrientation(node).opposite(), getNodeBounds(node));
        }
        else if (Util.isPrivatePort( node ))
        {
            OrientedPortFinder portFinder = new OrientedPortFinder();
            for( Edge e : node.edges() )
            {
                if( Util.isConnection( e) )
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.TOP);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.RIGHT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.LEFT);
                }
                else 
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.LEFT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.TOP);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.RIGHT);
                }
            }
            return portFinder;
        }
        return super.getPortFinder(node);
    }
}
