package biouml.model.xml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.StringReader;
import java.util.Map;
import one.util.streamex.StreamEx;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.parser.ParseException;
import ru.biosoft.math.parser.Parser;
import ru.biosoft.math.view.FormulaViewBuilder;

/**
 * Utility class for XML notations
 */
public class JSUtility
{
    public static int moveToEdge(Point location, Rectangle bounds)
    {
        int[] dist = new int[] {(int)Math.abs(location.x - bounds.getMinX()), (int)Math.abs(location.y - bounds.getMinY()),
                (int)Math.abs(location.x - bounds.getMaxX()), (int)Math.abs(location.y - bounds.getMaxY())};
        int minDist = dist[0], maxDir = 0;
        for( int i = 1; i < 4; i++ )
        {
            if( minDist > dist[i] )
            {
                minDist = dist[i];
                maxDir = i;
            }
        }
        switch( maxDir )
        {
            case 0:
                location.y = (int)Math.min(Math.max(location.y, bounds.getMinY()), bounds.getMaxY());
                location.x = (int)bounds.getMinX();
                break;
            case 1:
                location.y = (int)bounds.getMinY();
                location.x = (int)Math.min(Math.max(location.x, bounds.getMinX()), bounds.getMaxX());
                break;
            case 2:
                location.y = (int)Math.min(Math.max(location.y, bounds.getMinY()), bounds.getMaxY());
                location.x = (int)bounds.getMaxX();
                break;
            case 3:
                location.y = (int)bounds.getMaxY();
                location.x = (int)Math.min(Math.max(location.x, bounds.getMinX()), bounds.getMaxX());
                break;
        }
        return maxDir;
    }

    /**
     * Moves child node to the nearest edge
     * 
     * @return result side of parent:
     *     0 - west
     *     1 - north
     *     2 - east
     *     3 - south
     */
    public static int moveToEdge(Point location, DiagramElementJScriptWrapper node, DiagramElementJScriptWrapper parent)
    {
        return moveToEdge(location, node, parent, false);
    }

    /**
     * Moves child node to the nearest edge
     * 
     * @return result side of parent:
     *     0 - west
     *     1 - north
     *     2 - east
     *     3 - south
     */
    public static int moveToEdge(Point location, DiagramElementJScriptWrapper node, DiagramElementJScriptWrapper parent, boolean isInside)
    {
        Point parentLocation = (Point)parent.getValue("location", null);
        Dimension parentDimension = (Dimension)parent.getValue("shapeSize", null);
        Dimension nodeSize;
        View nodeView = (View)node.getValue("view", null);
        if( nodeView != null )
        {
            Rectangle nodeBounds = nodeView.getBounds();
            nodeSize = new Dimension(nodeBounds.width, nodeBounds.height);
        }
        else
        {
            //HACK: if we don't know bounds, it should be (40, 20)
            nodeSize = new Dimension(40, 20);
        }
        location.translate(nodeSize.width / 2, nodeSize.height / 2);
        Rectangle rect = new Rectangle(parentLocation, parentDimension);
        if( isInside )
            rect.grow( -nodeSize.width / 2, -nodeSize.height / 2);
        int result = moveToEdge(location, rect);
        location.translate( -nodeSize.width / 2, -nodeSize.height / 2);
        return result;
    }

    /**
     * Calculate inner node location by angle
     * @param location
     * @param parentLocation
     * @param parentDimension
     * @param nodeBounds
     * @param angle
     * @param isInside
     * @return
     */
    public static int fillLocationByAngle(Point location, Dimension parentDimension, Dimension nodeSize, double angle, boolean isInside)
    {
        int type = 0;
        double dWidth = parentDimension.width;
        double dHeight = parentDimension.height;

        double angleLimit = Math.atan(dHeight / dWidth);
        Point pos = new Point(0, parentDimension.height / 2);
        if( angle >= -angleLimit && angle <= angleLimit )
        {
            pos.y = (int) ( ( dHeight - dWidth * Math.tan(angle) ) / 2.0 );
        }
        else if( angle >= angleLimit && angle <= Math.PI - angleLimit )
        {
            pos.y = 0;
            pos.x = (int) ( ( dWidth - dHeight / Math.tan(angle) ) / 2.0 );
            type = 1;
        }
        else if( angle <= -angleLimit && angle >= -Math.PI + angleLimit )
        {
            pos.y = parentDimension.height;
            pos.x = (int) ( ( dWidth + dHeight / Math.tan(angle) ) / 2.0 );
            type = 3;
        }
        else if( ( angle >= Math.PI - angleLimit && angle <= Math.PI ) || ( angle <= -Math.PI + angleLimit && angle >= -Math.PI ) )
        {
            pos.y = (int) ( ( dHeight - dWidth * Math.tan(Math.PI - angle) ) / 2.0 );
            pos.x = parentDimension.width;
            type = 2;
        }

        if( isInside )
        {
            //child node should be inside of parent
            if( type == 0 )
            {
                location.x = pos.x;
                location.y = pos.y - nodeSize.height / 2;
            }
            else if( type == 1 )
            {
                location.x = pos.x - nodeSize.width / 2;
                location.y = pos.y;
            }
            else if( type == 2 )
            {
                location.x = pos.x - nodeSize.width;
                location.y = pos.y - nodeSize.height / 2;
            }
            else
            {
                location.x = pos.x - nodeSize.width / 2;
                location.y = pos.y - nodeSize.height;
            }
        }
        else
        {
            location.x = pos.x - nodeSize.width / 2;
            location.y = pos.y - nodeSize.height / 2;
        }
        return type;
    }

    /**
     * Get angle of title location for inner state nodes
     * @param nodeView
     * @param node
     * @return
     */
    public static double getTitleAngle(View nodeView, DiagramElementJScriptWrapper node)
    {
        Node n = (Node)node.getDiagramElement();
        Rectangle nBounds = nodeView.getBounds();
        Point nodeCenter = new Point(n.getLocation().x + nBounds.width / 2, n.getLocation().y + nBounds.height / 2);
        Compartment c = n.getCompartment();
        Point baseCenter = new Point(c.getLocation().x + c.getShapeSize().width / 2, c.getLocation().y + c.getShapeSize().height / 2);
        if( baseCenter.x > nodeCenter.x )
        {
            return Math.atan( ( (double) ( baseCenter.y - nodeCenter.y ) ) / ( (double) ( baseCenter.x - nodeCenter.x ) )) + Math.PI;
        }
        else if( baseCenter.x < nodeCenter.x )
        {
            return Math.atan( ( (double) ( baseCenter.y - nodeCenter.y ) ) / ( (double) ( baseCenter.x - nodeCenter.x ) ));
        }
        else
        {
            if( baseCenter.y >= nodeCenter.y )
                return Math.PI * 1.5;
            else
                return Math.PI * 0.5;
        }
    }

    /**
     * Returns full path for edge
     */
    public static SimplePath getPath(DiagramElementJScriptWrapper de)
    {
        if( ! ( de.getDiagramElement() instanceof Edge ) )
            return null;
        Edge edge = (Edge)de.getDiagramElement();
        if( edge.getPath() == null )
            Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);
        SimplePath path = edge.getSimplePath();
        if(path == null)
        {
            path = new SimplePath( new int[] {edge.getInPort().x, edge.getOutPort().x},
                    new int[] {edge.getInPort().y, edge.getOutPort().y}, 2);
        }
        return path;
    }

    /**
     * Get arc path for reaction edge
     */
    public static SimplePath getArcPath(DiagramElementJScriptWrapper de)
    {
        if( ! ( de.getDiagramElement() instanceof Edge ) )
            return null;

        Edge edge = (Edge)de.getDiagramElement();

        Path path = new Path();
        Point in = new Point();
        Point out = new Point();
        Diagram.getDiagram(edge).getType().getDiagramViewBuilder().calculateInOut(edge, in, out);

        Node reactionNode;
        Node elementNode;
        Point middlePoint = null;
        if( edge.getInput().getKernel() instanceof Reaction )
        {
            reactionNode = edge.getInput();
            elementNode = edge.getOutput();
            for( Edge e : reactionNode.getEdges() )
            {
                if( e.getKernel() instanceof SpecieReference )
                {
                    SpecieReference sr = (SpecieReference)e.getKernel();
                    if( sr.getRole().equals(SpecieReference.PRODUCT) )
                    {
                        if( e.getOutput() != elementNode )
                        {
                            Node specie = e.getOutput();
                            Rectangle rect = specie.getView().getBounds();
                            int x = rect.x + ( rect.width / 2 );
                            int y = rect.y + ( rect.height / 2 );
                            middlePoint = getProjection(out, in, new Point(x, y));
                            break;
                        }
                    }
                }
            }
        }
        else if( edge.getOutput().getKernel() instanceof Reaction )
        {
            reactionNode = edge.getOutput();
            elementNode = edge.getInput();
            for( Edge e : reactionNode.getEdges() )
            {
                if( e.getKernel() instanceof SpecieReference )
                {
                    SpecieReference sr = (SpecieReference)e.getKernel();
                    if( sr.getRole().equals(SpecieReference.REACTANT) )
                    {
                        if( e.getInput() != elementNode )
                        {
                            Node specie = e.getInput();
                            Rectangle rect = specie.getView().getBounds();
                            int x = rect.x + ( rect.width / 2 );
                            int y = rect.y + ( rect.height / 2 );
                            middlePoint = getProjection(in, out, new Point(x, y));
                            break;
                        }
                    }
                }
            }
        }

        path.addPoint(in.x, in.y, 0);
        if( middlePoint != null )
        {
            path.addPoint(middlePoint.x, middlePoint.y, 1);
        }
        path.addPoint(out.x, out.y, 0);

        edge.setPath(path);

        return edge.getSimplePath();
    }

    protected static Point getProjection(Point p, Point p1, Point p2)
    {
        double fDenominator = ( p2.x - p1.x ) * ( p2.x - p1.x ) + ( p2.y - p1.y ) * ( p2.y - p1.y );
        if( fDenominator == 0 ) // p1 and p2 are the same
            return new Point(p1.x, p1.y);
        double t = ( p.x * ( p2.x - p1.x ) - ( p2.x - p1.x ) * p1.x + p.y * ( p2.y - p1.y ) - ( p2.y - p1.y ) * p1.y )
                / fDenominator;
        return new Point((int) ( p1.x + ( p2.x - p1.x ) * t ), (int) ( p1.y + ( p2.y - p1.y ) * t ));
    }

    /**
     * Get nodes for reaction by role
     */
    public static DiagramElementJScriptWrapper[] getNodesByRole(Diagram diagram, Reaction reaction, String role)
    {
        return StreamEx.of( reaction.getSpecieReferences() ).filter( sr -> sr.getRole().equals( role ) )
                .map( sr -> Module.getModule( reaction ).getKernel( sr.getSpecie() ) ).select( Base.class )
                .flatMap( de -> diagram.getKernelNodes( de ) ).distinct()
                .map( DiagramElementJScriptWrapper::new )
                .toArray( DiagramElementJScriptWrapper[]::new );
    }

    /**
     * Check if node is a part of reaction
     */
    public static boolean isPartOfReaction(DiagramElementJScriptWrapper dew)
    {
        return true;
        //TODO: check for reaction corrected, current code doesn't work with KEGG/Diagrams/map04540.xml
        /*Node node = (Node)dew.getDiagramElement();
        for( Edge edge : node.getEdges() )
        {
            if( ( edge.getInput() == node ) && ( edge.getOutput().getKernel() instanceof Reaction ) )
            {
                return true;
            }
            if( ( edge.getOutput() == node ) && ( edge.getInput().getKernel() instanceof Reaction ) )
            {
                return true;
            }
        }
        return false;*/
    }

    /**
     * Creates formula view by formula string
     */
    public static CompositeView createFormulaView(String formula, Graphics g)
    {
        try
        {
            Parser parser = new Parser(new StringReader(formula));
            AstStart start = parser.Start();
            return ( new FormulaViewBuilder() ).createView(start, g);
        }
        catch( ParseException e )
        {
            //todo:
        }
        return null;
    }

    public static ColorFont getDefaultFont()
    {
        return ( new FormulaViewBuilder() ).getDefaultFont();
    }

    /**
     * Add and locate title for edge
     * @param edgeView edge view container
     * @param edge edge diagram element
     * @param title edge title
     */
    public static void addEdgeTitle(View edgeView, Edge edge, String title, ColorFont font, Map<String, ColorFont> fontRegistry, Graphics g)
    {
        if( edge.getOrigin() != null )
        {
            View parentView = ( (DiagramElement)edge.getOrigin() ).getView();
            if( parentView instanceof CompositeView )
            {
                Node node = null;
                Point point = null;
                if( edge.getInput().getKernel() instanceof Reaction )
                {
                    node = edge.getOutput();
                    point = edge.getOutPort();
                }
                else if( edge.getOutput().getKernel() instanceof Reaction )
                {
                    node = edge.getInput();
                    point = edge.getInPort();
                }
                if( node != null && point != null && point.x != 0 && point.y != 0 )
                {
                    ComplexTextView textView = new ComplexTextView(title, font, fontRegistry, ComplexTextView.TEXT_ALIGN_LEFT, 15, g);
                    Dimension size = new Dimension(textView.getBounds().width, textView.getBounds().height);

                    Point titleLocation = new Point();
                    int minPenalty = Integer.MAX_VALUE;
                    minPenalty = checkPosition(edgeView, node, size, new Point(point.x + 5, point.y), minPenalty, titleLocation);
                    minPenalty = checkPosition(edgeView, node, size, new Point(point.x - size.width / 2, point.y + 3), minPenalty,
                            titleLocation);
                    minPenalty = checkPosition(edgeView, node, size, new Point(point.x - size.width - 5, point.y), minPenalty,
                            titleLocation);
                    minPenalty = checkPosition(edgeView, node, size, new Point(point.x - size.width - 5, point.y - size.height),
                            minPenalty, titleLocation);
                    minPenalty = checkPosition(edgeView, node, size, new Point(point.x - size.width / 2, point.y - size.height - 3),
                            minPenalty, titleLocation);
                    checkPosition( edgeView, node, size, new Point( point.x + 5, point.y - size.height ), minPenalty, titleLocation );
                    ( (CompositeView)parentView ).add(textView, CompositeView.REL, titleLocation);
                }
            }
        }
    }

    protected static int checkPosition(View edgeView, Node node, Dimension size, Point location, int minPenalty, Point currentLocation)
    {
        int penalty = calculatePenalty(edgeView, node, size, location, 2);
        if( penalty < minPenalty )
        {
            currentLocation.x = location.x;
            currentLocation.y = location.y;
            return penalty;
        }
        return minPenalty;
    }

    protected static int calculatePenalty(View edgeView, Node node, Dimension size, Point location, int deep)
    {
        int result = 0;
        Rectangle titleRect = new Rectangle(location.x, location.y, size.width, size.height);
        if( node.getView() != null )
        {
            Rectangle bounds = node.getView().getBounds();
            if( bounds.intersects(titleRect) )
            {
                result += 20;
            }
        }
        if( edgeView != null && edgeView.intersects(titleRect) )
        {
            result += 10;
        }
        if( deep > 0 )
        {
            //look at nearest nodes
            for( Edge edge : node.edges() )
            {
                result += calculatePenalty(null, edge.getOtherEnd( node ), size, location, deep - 1);
            }
        }
        return result;
    }


//    public static void addPorts(CompositeView container, DiagramElementJScriptWrapper de, Graphics g)
//    {
//        Diagram sd = (Diagram)(de.getDiagramElement());
//        Dimension compartmentSize = container.getBounds().getSize();
//
//        for( PortInfo portInfo : sd.getPorts() )
//        {
//
//            Point portLocation = portInfo.getLocation();
//
//            //default location
//            if( portLocation == null )
//            {
//                if( portInfo.getPortNode().getKernel() instanceof Stub.InputConnectionPort )
//                {
//                    portInfo.setOrientation(PortOrientation.LEFT);
//                }
//                else if( portInfo.getPortNode().getKernel() instanceof Stub.OutputConnectionPort )
//                {
//                    portInfo.setOrientation(PortOrientation.RIGHT);
//                }
//                else
//                {
//                    portInfo.setOrientation(PortOrientation.BOTTOM);
//                }
//                portLocation = new Point(compartmentSize.width / 2, compartmentSize.height / 2);
//            }
//
//
//            CompositeView portView = createPortView(portInfo.getPortNode(), g);
//            Dimension portSize = portView.getBounds().getSize();
//            switch( portInfo.getOrientation() )
//            {
//                case TOP:
//                    portLocation.y = 0;
//                    break;
//                case RIGHT:
//                    portLocation.x = compartmentSize.width - portSize.width;
//                    break;
//                case BOTTOM:
//                    portLocation.y = compartmentSize.height - portSize.height;
//                    break;
//                case LEFT:
//                    portLocation.x = 0;
//                    break;
//            }
//            portInfo.setLocation(portLocation);
//            portView.setLocation(portLocation);
//            container.add(portView);
//        }
//    }


    protected Pen connectionPen = new Pen(1, Color.black);
    protected Pen connectionPortPen = new Pen(1, Color.black);
    protected Brush outputConnectionPortBrush = new Brush(Color.red);

//    protected static CompositeView createPortView(Node node, Graphics g)
//    {
//        CompositeView view = new CompositeView();
//        try
//        {
//            PortOrientation orientation = ( (SubDiagram)node.getOrigin() ).getPort(node).getOrientation();
//
//            String type = node.getKernel().getType();
//            if( type.equals(Type.TYPE_OUTPUT_CONNECTION_PORT) )
//            {
//                Polygon polygon = createPolygon(orientation, type);
//                view.add(new PolygonView(new Pen(1, Color.black), new Brush(Color.red), polygon));
//            }
//            else if( type.equals(Type.TYPE_INPUT_CONNECTION_PORT) )
//            {
//                Polygon polygon = createPolygon(orientation, type);
//                view.add(new PolygonView(new Pen(1, Color.black), new Brush(Color.green), polygon));
//            }
//            else if( type.equals(Type.TYPE_CONTACT_CONNECTION_PORT) )
//            {
//                view.add(new EllipseView(new Pen(1, Color.black), new Brush(Color.gray), 0, 0, PORT_SIZE, PORT_SIZE));
//            }
//
//            //add title
//            FormulaViewBuilder viewBuilder = new FormulaViewBuilder();
//            //            View title = viewBuilder.createTitleView(node.getTitle(),  viewBuilder.getDefaultFont(), g);
//            View title = viewBuilder.createTitleView(node.getTitle(), new ColorFont("Arial", Font.BOLD, 12, Color.black), g);
//
//            //            int x = options.getNodeTitleMargin().x;
//            //int y = options.getNodeTitleMargin().y;
//
//            switch( orientation )
//            {
//                case TOP:
//                    view.add(title, CompositeView.X_CC | CompositeView.Y_BT, new Point(0, 0));
//                    break;
//                case RIGHT:
//                    view.add(title, CompositeView.X_LR | CompositeView.Y_CC, new Point(0, 0));
//                    break;
//                case BOTTOM:
//                    view.add(title, CompositeView.X_CC | CompositeView.Y_TB, new Point(0, 0));
//                    break;
//                case LEFT:
//                    view.add(title, CompositeView.X_RL | CompositeView.Y_CC, new Point(0, 0));
//                    break;
//            }
//        }
//        catch( Exception e )
//        {
//
//        }
//        view.setModel(node);
//        view.setActive(true);
//        return view;
//    }

//    public static final int PORT_SIZE = 12;
//    protected static Polygon createPolygon(PortOrientation orientation, String type)
//    {
//        Polygon polygon = new Polygon();
//
//        boolean isOutput = true;
//        if( type.equals(Type.TYPE_INPUT_CONNECTION_PORT) )
//        {
//            isOutput = false;
//        }
//        if( ( orientation == PortOrientation.TOP && isOutput ) || ( orientation == PortOrientation.BOTTOM && !isOutput ) )
//        {
//            polygon.addPoint(0, 0);
//            polygon.addPoint(PORT_SIZE, 0);
//            polygon.addPoint(PORT_SIZE / 2, -PORT_SIZE);
//        }
//        else if( ( orientation == PortOrientation.BOTTOM && isOutput ) || ( orientation == PortOrientation.TOP && !isOutput ) )
//        {
//            polygon.addPoint(0, 0);
//            polygon.addPoint(PORT_SIZE, 0);
//            polygon.addPoint(PORT_SIZE / 2, PORT_SIZE);
//        }
//        else if( ( orientation == PortOrientation.RIGHT && isOutput ) || ( orientation == PortOrientation.LEFT && !isOutput ) )
//        {
//            polygon.addPoint(0, 0);
//            polygon.addPoint(0, PORT_SIZE);
//            polygon.addPoint(PORT_SIZE, PORT_SIZE / 2);
//        }
//        else if( ( orientation == PortOrientation.LEFT && isOutput ) || ( orientation == PortOrientation.RIGHT && !isOutput ) )
//        {
//            polygon.addPoint(0, 0);
//            polygon.addPoint(0, PORT_SIZE);
//            polygon.addPoint( -PORT_SIZE, PORT_SIZE / 2);
//        }
//        return polygon;
//    }

    /**
     * Move port to compartment border and set correctly orientation
     */
    public static void movePortToEdge(DiagramElement nodeDE, DiagramElement compartmentDE, Point newLocation)
    {
        CompositeSemanticController.movePortToEdge( (Node)nodeDE, (Compartment)compartmentDE, newLocation, false );
    }
    
    public static boolean isSubDiagramPort(String type, String deType)
    {
       return (deType.equals( "port" ) && type.equals( "subDiagram" )) ;
    }
}
