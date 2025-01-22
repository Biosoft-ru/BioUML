package biouml.model;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RectangularShape;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.CreateEdgeAction;
import biouml.standard.type.Base;
import biouml.standard.type.ImageDescriptor;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.Path;
import ru.biosoft.graph.PortFinder;
import ru.biosoft.graph.ShapeChanger;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.HtmlView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.PolylineView;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;

/**
 * Default implementation of <code>DiagramViewBuilder</code>.
 */
public class DefaultDiagramViewBuilder implements DiagramViewBuilder
{
    protected Logger log = Logger.getLogger(DefaultDiagramViewBuilder.class.getName());

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new DiagramViewOptions(null);
    }

    /**
     * Default implementation of the method.
     *
     * The following convention is used: icon images located in the subdirectory
     * 'resources' relative the specific DiagramViewBuilder. Icon file name is
     * class name with suffix ".gif", first letter is lower case.
     */
    @Override
    public Icon getIcon(Object type)
    {
        if( type instanceof Class )
        {
            Icon icon = getIcon((Class<?>)type, getResourcesRoot());
            if( icon != null )
                return icon;
            log.log(Level.SEVERE, "Image not found for type: " + ( (Class<?>)type ).getName());
        }
        if( type instanceof String )
        {
            String imageFile = "resources/" + ( (String)type ).toLowerCase() + ".gif";
            URL url = getIconURL(getResourcesRoot(), imageFile);

            if( url != null )
                return new ImageIcon(url);
            log.log(Level.SEVERE, "Image not found for type: " + type);
        }
        return null;
    }

    protected Class<? extends DefaultDiagramViewBuilder> getResourcesRoot()
    {
        return getClass();
    }

    /**
     * Obtaining icon using specific root
     */
    protected Icon getIcon(Class<?> type, Class<? extends DefaultDiagramViewBuilder> resourceRoot)
    {
        String name = type.getName();
        name = name.substring(name.lastIndexOf(".") + 1);
        if( name.indexOf('$') > 0 )
            name = name.substring(name.indexOf('$') + 1);

        String imageFile = "resources/" + name.toLowerCase() + ".gif";
        URL url = getIconURL(resourceRoot, imageFile);

        if( url != null )
            return new ImageIcon(url);

        return null;
    }

    /**
     * Build resource full path
     */
    protected URL getIconURL(Class<? extends DefaultDiagramViewBuilder> resourceRoot, String relativePath)
    {
        return resourceRoot.getResource(relativePath);
    }

    ///////////////////////////////////////////////////////////////////
    // Methods to build the diagram view
    //

    /**
     * Creates the diagram view
     */
    @Override
    public @Nonnull CompositeView createDiagramView(Diagram diagram, Graphics g)
    {
        return createCompartmentView(diagram, diagram.getViewOptions(), g);
    }

    @Override
    public @Nonnull CompositeView createCompartmentView(Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        boolean notificationEnabled = false;
        if( compartment.isNotificationEnabled() )
        {
            notificationEnabled = true;
            compartment.setNotificationEnabled(false);
        }

        CompositeView result = null;
        
        try
        {
            if( compartment.isUseCustomImage() )
            {
                result = createImageView( compartment, g );
                if( result != null )
                    buildNodes( compartment, options, g );
            }
        }
        catch( Exception ex )
        {
            log.info( "Error during compartment image creation: name = " + compartment.getCompleteNameInDiagram() + ", error: "
                    + ExceptionRegistry.translateException( ex ) );
        }

        if( result == null )
            result = doCreateCompartmentView(compartment, options, g);

        if( notificationEnabled )
            compartment.setNotificationEnabled(true);

        return result;
    }

    /**
     * Creates compartment view.
     */
    protected @Nonnull CompositeView doCreateCompartmentView(Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        CompositeView compartmentView = new CompositeView();
        compartment.setView(compartmentView);

        boolean showTitle = true;

        if( compartment instanceof Diagram ) // Diagram view has no shape
        {
            View stub = new BoxView(options.getDefaultPen(), null, 0, 0, 0, 0);
            stub.setVisible(false);
            stub.setModel(compartment);
            compartmentView.add(stub);

            compartmentView.setModel(compartment);
            compartmentView.setActive(true);
        }
        else
        {
            CompositeView view = createSpecialView(compartment, options, g);
            if( view != null )
                return view;
            showTitle = createCompartmentCoreView(compartmentView, compartment, options, g);
        }

        buildNodes(compartment, options, g);

        if( ( compartment instanceof Diagram && options.isDiagramTitleVisible() ) || ( ! ( compartment instanceof Diagram ) && showTitle ) )
        {
            View title = new ComplexTextView(compartment.getTitle(), getTitleFont(compartment, options.getDiagramTitleFont()),
                    options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
            compartmentView.add(title, options.getDiagramTitleAlignment(), new Point(5, 5));
        }
        return compartmentView;
    }
    /**
     * Creates view for equivalent node group.
     *
     * In view mode they are represented by one node image, in design mode they
     * look likes compartment.
     *
     * @pending design mode
     */
    public @Nonnull CompositeView createEquivalentNodeGroupView(EquivalentNodeGroup group, DiagramViewOptions options, Graphics g)
    {
        CompositeView groupView = new CompositeView();
        if( !options.isDesignMode() )
        {
            Node node = group.getRepresentative();

            if( node != null )
            {
                createNodeCoreView(groupView, node, options, g);
                View title = new ComplexTextView(node.getTitle(), options.getNodeTitleFont(), options.getFontRegistry(),
                        ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
                groupView.add(title, CompositeView.X_CC | CompositeView.Y_BT, options.getNodeTitleMargin());
            }

            Iterator<DiagramElement> i = group.iterator();
            while( i.hasNext() )
            {
                Node n = (Node)i.next();
                n.setView(groupView);
                n.setLocation(group.getLocation());
            }
        }

        group.setView(groupView);
        groupView.setLocation(group.getLocation());

        groupView.setModel(group);
        groupView.setActive(true);

        return groupView;
    }

    // /////////////////////////////////////////////////////////////////
    // Creating node issues
    //

    /**
     * Built all nodes and add them into compartment
     */
    protected void buildNodes(Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        Diagram d = Diagram.getDiagram(compartment);
        CompositeView compartmentView = (CompositeView)compartment.getView();

        // create list of compartments which has edges started or ended inside them
        Set<Compartment> edgeCompartments = compartment.stream(Edge.class).flatMap(Edge::nodes).map(Node::getCompartment).distinct()
                .map(cmp -> traverseToChild(compartment, cmp)).nonNull().toSet();

        // draw compartments where edges begin or end
        for( Compartment c : edgeCompartments )
        {
            View view = createCompartmentView(c, options, g);
            c.setView(view);
            view.setVisible(c.isVisible() || !d.isHideInvisibleElements());
            compartmentView.add(view);
        }

        int edgeInsertPos = compartmentView.size();

        Set<Compartment> otherCompartments = new HashSet<Compartment>();
        //draw compartments with Compartment kernel
        for( DiagramElement obj : compartment )
        {
            if( obj instanceof Compartment && obj.getKernel() != null && Type.TYPE_COMPARTMENT.equals( obj.getKernel().getType() )
                    && !edgeCompartments.contains( obj ) )
            {
                Compartment c = (Compartment)obj;
                View view = createCompartmentView( c, options, g );
                c.setView( view );
                view.setVisible( c.isVisible() || !d.isHideInvisibleElements() );
                compartmentView.add( view );
                otherCompartments.add( c );
            }
        }
        edgeInsertPos += otherCompartments.size();

        // draw other compartments
        for( DiagramElement obj : compartment )
        {
            if( obj instanceof Compartment && ! ( obj instanceof EquivalentNodeGroup ) && !edgeCompartments.contains( obj )
                    && !otherCompartments.contains( obj ) )
            {
                Compartment c = (Compartment)obj;
                View view = createCompartmentView(c, options, g);
                c.setView(view);
                view.setVisible(c.isVisible() || !d.isHideInvisibleElements());
                compartmentView.add(view);
            }
        }

        // draw usual nodes and skip reactions
        for( DiagramElement obj : compartment )
        {
            if( obj instanceof Node )
            {
                Node node = (Node)obj;
                if( isReaction(node.getKernel()) || ( node.getKernel() != null && node.getKernel() instanceof Stub.ConnectionPort ) )
                    continue;

                View view;
                if( node instanceof EquivalentNodeGroup )
                    view = createEquivalentNodeGroupView((EquivalentNodeGroup)node, options, g);
                else if( node instanceof Compartment )
                    continue;
                else
                    view = createNodeView(node, options, g);
                view.setVisible(node.isVisible() || !d.isHideInvisibleElements());
                compartmentView.add(view);
            }
        }

        // draw reactions and connection ports
        for( DiagramElement obj : compartment )
        {
            if( obj instanceof Node )
            {
                Node node = (Node)obj;
                if( isReaction(node.getKernel()) || node.getKernel() instanceof Stub.ConnectionPort )
                {
                    View view = createNodeView(node, options, g);
                    view.setVisible(node.isVisible() || !d.isHideInvisibleElements());
                    compartmentView.add(view);
                }
            }
        }

        // draw all edges
        for( Edge edge : compartment.stream(Edge.class) )
        {
            View view = createEdgeView(edge, options, g);
            edge.setView(view);
            view.setVisible( !d.isHideInvisibleElements() || edge.nodes().allMatch(Node::isVisible));
            compartmentView.insert(view, edgeInsertPos);
        }
    }

    private Compartment traverseToChild(Compartment compartment, Compartment child)
    {
        if( child == compartment )
            return null;
        try
        {
            while( child.getCompartment() != compartment )
                child = child.getCompartment();
            return child;
        }
        catch( Exception e )
        {
            return null;
        }
    }

    /**
     * Check if this kernel is reaction
     */
    protected boolean isReaction(Base kernel)
    {
        return ( kernel == null ) ? false : Base.TYPE_REACTION.equals(kernel.getType());
    }

    public CompositeView createImageView(Node node, Graphics g)
    {
        CompositeView cView = null;

        cView = node.getImage().getImageView((Graphics2D)g);
        if( cView != null )
            return cView;
        else
        {
            ImageDescriptor imageDescr = node.getImage();
            Image image = imageDescr.getImage();

            if( image == null )
                return null;

            Dimension size = node.getShapeSize();//imageDescr.getSize();
            if (size.width == 0 && size.height == 0)
                size = imageDescr.getOriginalSize();
            
            ImageView imageView = new ImageView(image, node.getLocation().x, node.getLocation().y, size.width, size.height);
            imageView.setPath( imageDescr.getPath().toString() );

            if( !size.equals(imageDescr.getOriginalSize()) )
                imageView.setToScale((float)size.width / imageDescr.getOriginalSize().width,
                        (float)size.height / imageDescr.getOriginalSize().height);


            cView = new CompositeView();
            cView.add(imageView);
            cView.setModel(node);
            cView.setActive(true);
            cView.setLocation(node.getLocation());
            node.setView(cView);
        }
        return cView;
    }
    
    public boolean needAddTitle(Node node)
    {
        return true;
    }

    @Override
    public @Nonnull CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = createSpecialView(node, options, g);
        if( view != null )
            return view;     
        boolean addTitle = false;

        try
        {
            // if node has image it will be used with high priority
            if( node.isUseCustomImage() )
            {
                view = createImageView( node, g );
                addTitle = needAddTitle( node );
            }
        }
        catch( Exception ex )
        {
            log.info( "Error during node image creation: name = " + node.getCompleteNameInDiagram() + ", error: "
                    + ExceptionRegistry.translateException( ex ) );
        }

        if( view == null )
        {
            view = new CompositeView();
            addTitle = createNodeCoreView(view, node, options, g);
        }

        if( addTitle )
            createNodeTitle(view, node, options, g);

        view.setModel(node);
        view.setActive(true);
        view.setLocation(node.getLocation());
        node.setView(view);
        return view;
    }

    private CompositeView createSpecialView(Node node, DiagramViewOptions options, Graphics g)
    {
        DynamicProperty dp = node.getAttributes().getProperty(NodeViewBuilder.NODE_VIEW_BUILDER);
        if( dp != null )
        {
            try
            {
                return ( (NodeViewBuilder)dp.getValue() ).createNodeView(node, options, g);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return null;
    }

    protected void createNodeTitle(CompositeView view, Node node, DiagramViewOptions options, Graphics g)
    {
        View title = new ComplexTextView(node.getTitle(), getTitleFont(node, options.getNodeTitleFont()), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );

        Diagram diagram = Diagram.optDiagram(node);
        if( ( diagram != null ) && ( diagram.getLabelLayouter() == null ) && Double.isNaN(node.getTitleAngle()) )
        {
            view.add(title, CompositeView.X_CC | CompositeView.Y_BT, options.getNodeTitleMargin());
        }
        else
        {
            CompositeView parentView = (CompositeView)node.getCompartment().getView();
            if( parentView != null )
            {
                node.setTitleView(title);

                Rectangle titleBounds = title.getBounds();
                Rectangle nodeBounds = view.getBounds();
                Point point = new Point();
                point.x = node.getLocation().x - titleBounds.x + ( nodeBounds.width - titleBounds.width ) / 2
                        - (int) ( node.getTitleOffset() * Math.cos(node.getTitleAngle()) );
                point.y = node.getLocation().y - titleBounds.y + ( nodeBounds.height - titleBounds.height ) / 2
                        - (int) ( node.getTitleOffset() * Math.sin(node.getTitleAngle()) );

                parentView.add(title, CompositeView.REL, point);
            }
        }
    }

    /** @return indicates whether a node title should be added to node core view */
    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( node.getKernel() instanceof Stub.Note )
            return createNoteView(container, node, options);

        if( CreateEdgeAction.EDGE_END_STUB.equals(node.getKernel().getType()) )
        {
            container.add(new BoxView(null, null, 0, 0, 1, 1));
            return false;
        }

        String name = node.getKernel().getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        View type = new TextView(name, options.getDefaultFont(), g);
        type.setLocation(5, 5);

        View bound = new BoxView(getBorderPen(node, options.getDefaultPen()), null, 0, 0, type.getBounds().width + 10,
                type.getBounds().height + 10);
        container.add(bound);
        container.add(type);

        return true;
    }

    public boolean createNoteView(CompositeView container, Node node, DiagramViewOptions options)
    {
        Stub.Note note = (Stub.Note)node.getKernel();

        // take into account that shape size also includes border
        Dimension shapeSize = node.getShapeSize();
        if( shapeSize != null && note.isBackgroundVisible() )
        {
            shapeSize = (Dimension)node.getShapeSize().clone();
            shapeSize.width -= options.getNoteMargin().x * 2;
            shapeSize.height -= options.getNoteMargin().y * 2;
        }

        HtmlView view = new HtmlView(node.getTitle(), getTitleFont(node, options.getNodeTitleFont()), new Point(0, 0), shapeSize);
        if( !note.isBackgroundVisible() )
        {
            container.add(view);
            return false;
        }

        int height = view.getBounds().height + options.getNoteMargin().y * 2;
        int delta = Math.min(15, height / 2);
        int width = view.getBounds().width + options.getNoteMargin().x * 2 + delta;

        Brush brush = getBrush(node, options.getNoteBrush());
        Pen pen = getBorderPen(node, options.getNodePen());

        PolygonView border = new PolygonView(pen, brush);
        border.addPoint(0, 0);
        border.addPoint(width - delta, 0);
        border.addPoint(width, delta);
        border.addPoint(width, height);
        border.addPoint(0, height);
        container.add(border);

        container.add(view, CompositeView.X_LL | CompositeView.Y_TT, options.getNoteMargin());

        PolygonView edge = new PolygonView(null, brush);
        edge.addPoint(width - delta, 0);
        edge.addPoint(width - delta, delta);
        edge.addPoint(width, delta);
        container.add(edge);

        PolylineView edgeLine = new PolylineView(pen);
        edgeLine.addPoint(width - delta, 0);
        edgeLine.addPoint(width - delta, delta);
        edgeLine.addPoint(width, delta);
        container.add(edgeLine);

        return false;
    }

    // /////////////////////////////////////////////////////////////////
    // Creating node issues
    //

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = new CompositeView();
        Pen pen = getBorderPen(edge, options.getDefaultPen());

        if( edge.getPath() == null )
            Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);

        SimplePath path = edge.getSimplePath();

        View arrow = new ArrowView(pen, null, path, 0, 1);
        arrow.setModel(edge);
        arrow.setActive(true);
        view.add(arrow);

        view.setModel(edge);
        view.setActive(false);

        return view;
    }

    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        // create compartment shape view
        BoxView shapeView = null;

        Dimension shapeSize = compartment.getShapeSize();

        RectangularShape shape = null;
        switch( compartment.getShapeType() )
        {
            case Compartment.SHAPE_ROUND_RECTANGLE:
                float arc = Math.min(shapeSize.width, shapeSize.height) * 20 / 100.0f;
                shape = new java.awt.geom.RoundRectangle2D.Float(0, 0, shapeSize.width, shapeSize.height, arc, arc);
                break;

            case Compartment.SHAPE_ELLIPSE:
                shape = new java.awt.geom.Ellipse2D.Float(0, 0, shapeSize.width, shapeSize.height);
                break;

            default:
                shape = new Rectangle(0, 0, shapeSize.width, shapeSize.height);
        }

        shapeView = new BoxView(getBorderPen(compartment, new Pen(1, Color.black)), getBrush(compartment, new Brush(Color.white)), shape);
        shapeView.setLocation(compartment.getLocation());
        container.add(shapeView);
        container.setModel(compartment);

        shapeView.setModel(compartment);
        shapeView.setActive(true);
        return true;
    }

    @Override
    public boolean calculateInOut(Edge edge, Point in, Point out)
    {
        return calculateInOut(edge, in, out, 1, 3);
    }

    protected boolean calculateInOut(Edge edge, Point in, Point out, int offsetStart, int offsetEnd)
    {
        Rectangle inputBounds = getNodeBounds(edge.getInput());
        Rectangle outputBounds = getNodeBounds(edge.getOutput());

        if( ( ( inputBounds.x >= outputBounds.x ) && ( inputBounds.x <= outputBounds.x + outputBounds.width )
                && ( inputBounds.y >= outputBounds.y ) && ( inputBounds.y <= outputBounds.y + outputBounds.height ) )
                || ( ( outputBounds.x >= inputBounds.x ) && ( outputBounds.x <= inputBounds.x + inputBounds.width )
                        && ( outputBounds.y >= inputBounds.y ) && ( outputBounds.y <= inputBounds.y + inputBounds.height ) ) )
        {
            //there is intersection between node bounds
            //TODO: find correct ports
            in.x = inputBounds.x + inputBounds.width / 2;
            in.y = inputBounds.y + inputBounds.height / 2;
            out.x = outputBounds.x + outputBounds.width / 2;
            out.y = outputBounds.y + outputBounds.height / 2;
            return false;
        }

        Path path = edge.getPath();
        if( path == null || path.npoints <= 2 )
        {
            calcAttachmentPoints(inputBounds, outputBounds, in, out, offsetStart, offsetEnd);
        }
        else
        {
            calcAttachmentPoints(inputBounds, new Rectangle(path.xpoints[1], path.ypoints[1], 1, 1), in, new Point(), 1, 1);
            calcAttachmentPoints(new Rectangle(path.xpoints[path.npoints - 2], path.ypoints[path.npoints - 2], 1, 1), outputBounds,
                    new Point(), out, 1, 3);
        }

        return true;
    }

    @Override
    public Rectangle getNodeBounds(Node node)
    {
        Diagram diagram = Diagram.getDiagram(node);
        DiagramViewOptions viewOptions = diagram.getViewOptions();
        Graphics graphics = ApplicationUtils.getGraphics();
        Dimension shapeSize = null;
        View view;
        if( node instanceof Compartment )
        {
            view = new CompositeView();
            createCompartmentCoreView((CompositeView)view, (Compartment)node, viewOptions, graphics);
        }
        else
        {
            view = createNodeView(node, viewOptions, graphics);
        }
        shapeSize = view.getBounds().getSize();

        return new Rectangle(node.getLocation(), shapeSize);
    }

    // ////////////////////////////////////////////////////////////////
    // Utilites
    //

    private static int proportion(int L, int l)
    {
        return (int)Math.round( ( L - l ) * Math.log(L) * l / L / 30);
    }

    private static int center(int start1, int end1, int start2, int end2)
    {
        if( start1 < end2 && start2 < end1 )
        {
            return ( Math.max(start1, start2) + Math.min(end1, end2) ) / 2;
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Calculates the attachment points to paint relation between two images
     * described by the specified rectangles.
     *
     * Two situations are possible:
     *
     * <pre>
     *  1 situation                           2 situation
     *   ________                     ______                 _______
     *  |        |                   |      |               |       |
     *  |        |    _______        |      |               |       |
     *  |        |___|       |       |______|               |_______|
     *  |________|   |       |               \                    |
     *               |_______|                \ _______         __|_____
     *                                         |       |       |        |
     *                                         |       |       |        |
     *                                         |_______|       |________|
     *
     *
     * </pre>
     *
     * @param r1 the minimal rectangle containing the first image
     * @param r2 the minimal rectangle containing the second image
     * @param p1 the attachment point for the first image
     * @param p2 the attachment point for the second image
     * @param offset the distance between the attachment point and minimal
     * rectangle     */
    protected static void calcAttachmentPoints(Rectangle r1, Rectangle r2, Point p1, Point p2, int offsetStart, int offsetEnd)
    {
        int x, L;

        // 1 situation
        x = r2.x;
        r2.x = r1.x;
        if( r1.intersects(r2) )
        {
            // set up x coordinates
            r2.x = x;
            if( r1.x < r2.x )
            {
                p1.x = r1.x + r1.width + offsetStart;
                p2.x = r2.x - offsetEnd;
            }
            else
            {
                p2.x = r2.x + r2.width + offsetEnd;
                p1.x = r1.x - offsetStart;
            }

            // set up y coordinates
            int center = center(r1.y, r1.y + r1.height, r2.y, r2.y + r2.height);
            if( center != Integer.MIN_VALUE )
            {
                p1.y = center;
                p2.y = center;
            }
            else
            {
                p1.y = r1.y + r1.height / 2;
                p2.y = r2.y + r2.height / 2;
                if( r1.y < r2.y )
                {
                    L = Math.max(r1.y + r1.height, r2.y + r2.height) - r1.y;
                    p1.y += proportion(L, r1.height);
                    p2.y -= proportion(L, r2.height);
                }
                else
                {
                    L = Math.max(r1.y + r1.height, r2.y + r2.height) - r2.y;
                    p1.y -= proportion(L, r1.height);
                    p2.y += proportion(L, r2.height);
                }
            }
        }

        // 2 situation
        else
        {
            r2.x = x;
            if( r1.y < r2.y )
            {
                p1.y = r1.y + r1.height + offsetStart;
                p2.y = r2.y - offsetEnd;
            }
            else
            {
                p2.y = r2.y + r2.height + offsetEnd;
                p1.y = r1.y - offsetStart;
            }

            // set up x coordinates
            int center = center(r1.x, r1.x + r1.width, r2.x, r2.x + r2.width);
            if( center != Integer.MIN_VALUE )
            {
                p1.x = center;
                p2.x = center;
            }
            else
            {
                p1.x = r1.x + r1.width / 2;
                p2.x = r2.x + r2.width / 2;

                if( r1.x < r2.x )
                {
                    L = Math.max(r1.x + r1.width, r2.x + r2.width) - r1.x;
                    p1.x += proportion(L, r1.width);
                    p2.x -= proportion(L, r2.width);
                }
                else
                {
                    L = Math.max(r1.x + r1.width, r2.x + r2.width) - r2.x;
                    p1.x -= proportion(L, r1.width);
                    p2.x += proportion(L, r2.width);
                }
            }
        }
    }

    protected DiagramViewBuilder baseViewBuilder = null;
    @Override
    public void setBaseViewBuilder(DiagramViewBuilder baseViewBuilder)
    {
        this.baseViewBuilder = baseViewBuilder;
    }

    protected DiagramViewOptions baseViewOptions = new DiagramViewOptions(null);
    @Override
    public void setBaseViewOptions(DiagramViewOptions baseViewOptions)
    {
        this.baseViewOptions = baseViewOptions;
    }

    protected Map<Object, String> typeMapping = null;
    @Override
    public void setTypeMapping(Map<Object, String> typeMapping)
    {
        this.typeMapping = typeMapping;
    }


    public static boolean hasTitle(Node node)
    {
        return node.getTitle() != null && !node.getTitle().isEmpty();
    }

    @Override
    public PortFinder getPortFinder(Node node)
    {
        return null;
    }

    @Override
    public ShapeChanger getShapeChanger(Node node)
    {
        return null;
    }

    public static boolean hasDefaultStyle(DiagramElement de)
    {
        return de.getPredefinedStyle().equals(DiagramElementStyle.STYLE_DEFAULT);
    }

    public static ColorFont getTitleFont(DiagramElement de, ColorFont defaultFont)
    {
        return hasDefaultStyle(de) ? defaultFont : de.getCustomStyle().getFont();
    }

    public static Pen getBorderPen(DiagramElement de, Pen defaultPen)
    {
        return hasDefaultStyle( de ) ? defaultPen : de.getCustomStyle().getPen();
    }

    public static Brush getBrush(DiagramElement de, Brush defaultBrush)
    {
        return hasDefaultStyle(de) ? new Brush(defaultBrush.getPaint()) : de.getCustomStyle().getBrush();
    }

    /**
     * This view is added to base view to indicate that this node is fixed
     */
    public View createPinView()
    {
        int[] xPoints = new int[] {0, 2, 1, 2, 6, 3, 2, 0};
        int[] yPoints = new int[] {6, 4, 3, 0, 4, 5, 4, 6};
        PolygonView polygonView = new PolygonView(new Pen(), new Brush(Color.BLACK), xPoints, yPoints);
        return polygonView;
    }

    @Override
    public Point getNearestNodePoint(Point p, Node node)
    {
        //TODO: process reaction and other port-related nodes
        Rectangle r = getNodeBounds( node );
        double w = r.getWidth();
        double h = r.getHeight();
        int x = r.x;
        int y = r.y;
        Point[] np = new Point[4];
        if( p.y < y )
        {
            np[0] = new Point( x, y );
            np[2] = new Point( (int) ( x + w ), y );
        }
        else if( p.y > y + h )
        {
            np[0] = new Point( x, (int) ( y + h ) );
            np[2] = new Point( (int) ( x + w ), (int) ( y + h ) );
        }
        else
        {
            np[0] = new Point( x, p.y );
            np[2] = new Point( (int) ( x + w ), p.y );
        }
        if( p.x < x )
        {
            np[1] = new Point( x, y );
            np[3] = new Point( x, (int) ( y + h ) );
        }
        else if( p.x > x + w )
        {
            np[1] = new Point( (int) ( x + w ), y );
            np[3] = new Point( (int) ( x + w ), (int) ( y + h ) );
        }
        else
        {
            np[1] = new Point( p.x, y );
            np[3] = new Point( p.x, (int) ( y + h ) );
        }
        Point nearest = p;
        double dist = Double.MAX_VALUE;
        for( int i = 0; i < 4; i++ )
        {
            double di = ( np[i].x - p.x ) * ( np[i].x - p.x ) + ( np[i].y - p.y ) * ( np[i].y - p.y );
            if( dist > di )
            {
                dist = di;
                nearest = np[i];
            }
        }
        return nearest;
    }

    @Override
    public boolean forbidCustomImage(Node node)
    {
        return !(node.getRole() instanceof VariableRole);
    }
}
