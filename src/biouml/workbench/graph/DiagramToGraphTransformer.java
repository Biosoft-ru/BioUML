package biouml.workbench.graph;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.graph.Edge;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.LabelLayouter;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.MapPortFinder;
import ru.biosoft.graph.ModHierarchicLayouter;
import ru.biosoft.graph.Util;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.util.TextUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.DiagramViewBuilder;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.standard.type.Base;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.application.ApplicationUtils;

/**
 * Converts BioUML diagram or its part into ru.biosoft.graph for usage by graph
 * layout algorithm. Produced layout is used to assign corresponding coordinates
 * to diagram components.
 *
 * @pending - compartment issues. It is suggested that the diagram has no
 *          compartments.
 */
public class DiagramToGraphTransformer
{
    protected static final Logger log = Logger.getLogger(DiagramToGraphTransformer.class.getName());

    private DiagramToGraphTransformer()
    {

    }

    public static void layoutIfNeeded(Diagram diagram)
    {
        if( diagram.needsRelayout() )
            layout(diagram);
    }

    protected static void setNodesFixed(Compartment compartment)
    {
        compartment.recursiveStream().select( Node.class ).forEach( n -> n.setFixed( true ) );
    }

    public static void layout(Diagram diagram)
    {
        layout(diagram, diagram.getPathLayouter());
    }

    public static void layout(Compartment compartment, Layouter layouter)
    {
        layout(compartment, layouter, ApplicationUtils.getGraphics());
    }

    public static void layoutEdges(Diagram diagram)
    {
        if( diagram == null )
            return;

        Graph graph = generateGraph(diagram, null);
        Layouter layouter = diagram.getPathLayouter();
        if( layouter == null )
            layouter = diagram.getViewOptions().getPathLayouter();
        if( layouter != null )
        {
            layouter.layoutEdges(graph, null);
            applyLayout(graph);
        }
    }

    public static void layoutLabels(Diagram diagram)
    {
        if( diagram == null || diagram.getLabelLayouter() == null )
            return;

        Graph graph = generateGraph(diagram, null);
        diagram.getLabelLayouter().layoutEdges(graph, null);
        applyLayout(graph);
    }

    private static void layout(Compartment compartment, Layouter layouter, Graphics g)
    {
        if( compartment == null || layouter == null )
            return;

        // Initialize nodes size by generating diagramView
        // TODO: Get rid of total view recreation
        Diagram diagram = Diagram.getDiagram(compartment);
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, g);

        // layout
        if( diagram.getSize() > 0 )
        {
            int k = 0;

            try
            {
                while( diagram.get("level." + k) != null )
                {
                    diagram.remove("level." + k);
                    k++;
                }
            }
            catch( Exception e )
            {

            }

            Graph graph = generateGraph(compartment, diagram.getType().getSemanticController().getFilter());

            int maxLayer = 0;
            if( layouter instanceof ModHierarchicLayouter )
            {
                biouml.model.Node[] Nodes = diagram.getNodes();

                for( Node node : Nodes )
                {
                    String str = node.getAttributes().getValue("level").toString();
                    int layer = Integer.parseInt(str);
                    maxLayer = Math.max(layer, maxLayer);
                }

                Object[] labelesData = new Object[maxLayer];
                for( int j = 0; j < maxLayer; j++ )
                {
                    biouml.model.Node modelNode = new biouml.model.Node(diagram, "level." + j, Nodes[0].getKernel());
                    modelNode.setTitle("level." + j);
                    modelNode.setShapeSize(Nodes[0].getShapeSize());
                    modelNode.setView(Nodes[0].getView());
                    try
                    {
                        diagram.put(modelNode);
                    }
                    catch( Exception e )
                    {

                    }
                    labelesData[j] = modelNode;
                }

                Rectangle r = Nodes[0].getView().getBounds();
                graph.addLables(labelesData, maxLayer, r.width, r.height);
            }
            layouter.doLayout(graph, null);
            applyLayout(graph);
        }
    }

    public static boolean layoutSingleEdge(biouml.model.Edge edge, Layouter layouter, Filter<DiagramElement> filter)
    {
        Option parent = edge.getParent();
        Compartment compartment = ( parent instanceof Compartment ) ? (Compartment)parent : Diagram.getDiagram(edge);
        Graph graph = generateGraph(compartment, filter, true);
        String inputCompleteName = edge.getInput().getCompleteNameInDiagram();
        String outputCompleteName = edge.getOutput().getCompleteNameInDiagram();
        ru.biosoft.graph.Node input = graph.getNode(inputCompleteName);
        ru.biosoft.graph.Node output = graph.getNode(outputCompleteName);

        Edge e = graph.getEdge(input, output);

        //try to get redirected edge
        if( e == null )
        {
            for( Edge graphEdge : graph.getEdges() )
            {
                if( graphEdge.getAttribute("inputPortName").equals(inputCompleteName)
                        && graphEdge.getAttribute("outputPortName").equals(outputCompleteName) )
                {
                    e = graphEdge;
                    break;
                }
            }
        }

        if(e != null)
        {
            layouter.layoutPath(graph, e, null);
            edge.setPath(e.getPath());
            return true;
        }
        return false;
    }

    public static void layoutSingleNodeEdges(biouml.model.Node node, Layouter layouter)
    {
        // TODO: Extra validation
        Graph graph = generateGraph(Diagram.getDiagram(node), null);
        layoutSingleNodeEdges(node, layouter, graph);
    }

    public static void layoutSingleNodeEdges(biouml.model.Node node, Layouter layouter, Graph graph)
    {
        if( layouter == null )
            return;

        // Find graph node correspondent to model node
        ru.biosoft.graph.Node movedGraphNode = null;
        for(ru.biosoft.graph.Node gNode: graph.getNodes() )
        {
            if( gNode.applicationData == node )
            {
                movedGraphNode = gNode;
                break;
            }
        }

        if( movedGraphNode == null )
        {
            // TODO: replace with error after compartment issues resolve
            log.warning("Could not recover graph node after transform");
            return;
        }

        List<ru.biosoft.graph.Edge> nodeEdges = graph.getEdges(movedGraphNode);
        if( nodeEdges != null )
        {
            for( ru.biosoft.graph.Edge graphEdge : nodeEdges )
            {
                layouter.layoutPath(graph, graphEdge, null);

                biouml.model.Edge modelEdge = (biouml.model.Edge)graphEdge.applicationData;
                if( modelEdge == null )
                    continue;

                modelEdge.nodes().forEach( n -> n.addEdge( modelEdge ) );
                modelEdge.setPath(graphEdge.getPath());
            }
        }

        if( node.getClass().equals(Compartment.class) && Diagram.getDiagram(node).getType().needLayout(node) )
        {
            for(DiagramElement de : (Compartment)node)
            {
                if( de instanceof Node )
                    layoutSingleNodeEdges((Node)de, layouter, graph);
            }
        }
    }

    /**
     * Generates graph for BioUML diagram or its part.
     *
     * @param daigram -
     *            diagram or compartment for which graph should be generated.
     * @param filter -
     *            allows filter nodes and edges to be included in the graph.
     *
     * @pending - compartment issues. Compartments are treated as usual nodes,
     *          their internal elements are ignored, due to this reason some
     *          edges can be skipped.
     */
    public static @Nonnull Graph generateGraph(Compartment compartment, Filter<DiagramElement> filter, boolean includeFixedCompartments)
    {
        Graph graph = new Graph();

        // first iteration - create nodes
        addNodesToGraph(graph, compartment, filter, includeFixedCompartments);

        // second iteration - create edges
        addEdgesToGraph(graph, compartment, filter);

        return graph;
    }

    public static @Nonnull Graph generateGraph(Compartment compartment, Filter<DiagramElement> filter)
    {
        return generateGraph(compartment, filter, false);
    }
    
    private static void addNodesToGraph(Graph graph, Compartment compartment, Filter<DiagramElement> filter, boolean includeFixedCompartments)
    {
        DiagramType diagramType = Diagram.getDiagram(compartment).getType();
        Predicate<Node> pred = Node::isVisible;
        if(filter != null && filter.isEnabled())
            pred = pred.and( filter::isAcceptable );
        for(Node obj : compartment.stream( Node.class ).filter( pred ))
        {
            ru.biosoft.graph.Node graphNode = createGraphNode(obj, pred);

            if( obj instanceof Compartment && diagramType.needLayout( obj ) )
            {
//                TODO: do not layout completely fixed compartments, instead treat them as one large node. 
//                      after layout manually layout their edges. BUT do not do this if layouter supports compartments (e.g. FastGridLayouter)
//                if( !includeFixedCompartments && isCompletelyFixed( (Compartment)obj ) )
//                    continue;

                graphNode.setAttribute( "isCompartment", "true" );
                addNodesToGraph( graph, (Compartment)obj, filter, includeFixedCompartments );
            }

            if( graph.getNode(graphNode.getName()) == null )
                graph.addNode(graphNode);
        }
    }

    private static ru.biosoft.graph.Node createGraphNode(Node modelNode, Predicate<Node> filter)
    {
        Diagram diagram = Diagram.getDiagram(modelNode);
        DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();

        Rectangle r = viewBuilder.getNodeBounds(modelNode);
        View view = modelNode.getView();

        // special case: we would like to skip reaction titles
        if( view != null && modelNode.getKernel() instanceof biouml.standard.type.Reaction )
        {
            CompositeView cv = (CompositeView)view;
            if( cv.size() > 1 && cv.elementAt(1) instanceof TextView)
                r = cv.elementAt(0).getBounds();
        }

        ru.biosoft.graph.Node graphNode = new ru.biosoft.graph.Node(modelNode.getCompleteNameInDiagram(), r.x, r.y, r.width, r.height,
                modelNode.isFixed());

        DynamicProperty dp = modelNode.getAttributes().getProperty("orientation");
        if (dp != null)
            graphNode.setAttribute("orientation", dp.getValue().toString());

        try
        {
            graphNode.setAttribute("label", "false");
            // set level to graph node
            if( modelNode.getAttributes().getProperty("level") != null )
                graphNode.setAttribute("level", modelNode.getAttributes().getProperty("level").getValue().toString());

            // set score to graph node (for color arrangement)
            Object values = modelNode.getAttributes().getValue("probe");
            double value = 0;
            if( values != null )
            {
                String[] valuesArray = TextUtil.split( values.toString(), ';' );
                for( String valueStr : valuesArray )
                {
                    if( valueStr.trim().length() == 0 )
                        continue;

                    try
                    {
                        value += Double.parseDouble(valueStr.trim());
                    }
                    catch( NumberFormatException e )
                    {

                    }
                }
                value /= valuesArray.length;
            }
            graphNode.setAttribute("score", value + "");
        }
        catch( Exception ex )
        {

        }

        if( modelNode.getTitleView() != null )
        {
            Rectangle bounds = modelNode.getTitleView().getBounds();
            graphNode.setAttribute(LabelLayouter.LABEL_SIZE, bounds.width + ";" + bounds.height);
        }

        //add compartment info
        Compartment parentCompartment = modelNode.getCompartment();
        if( ! ( parentCompartment instanceof Diagram ) )
            graphNode.setAttribute("compartmentName", parentCompartment.getCompleteNameInDiagram());

        Base kernel = modelNode.getKernel();

        graphNode.setShapeChanger(viewBuilder.getShapeChanger(modelNode));

        if( kernel != null )
        {
            graphNode.setPortFinder( viewBuilder.getPortFinder(modelNode) );
            String type = kernel.getType();
            if( type.equals(Type.MATH_EVENT) )
            {
                graphNode.setAttribute("Type", "Event");
            }
            else if( type.equals(Type.MATH_EQUATION) )
            {
                graphNode.setAttribute("Type", "Equation");
            }
            else if( type.equals(Type.MATH_FUNCTION) )
            {
                graphNode.setAttribute("Type", "Function");
            }
            else if( modelNode.getKernel() instanceof biouml.standard.type.Reaction || type.equals(Type.TYPE_REACTION))
            {
                graphNode.setAttribute("Type", "Reaction");
            }
            //temporal for old version diagrams compatibility
            else if( type.equals(Type.ANALYSIS_METHOD) || modelNode instanceof SubDiagram )
            {
                dp = modelNode.getAttributes().getProperty(Node.INNER_NODES_PORT_FINDER_ATTR);
                if( dp == null )
                {
                    dp = new DynamicProperty(Node.INNER_NODES_PORT_FINDER_ATTR, Boolean.class, true);
                    dp.setHidden(true);
                    modelNode.getAttributes().add(dp);
                }
            }

            dp = modelNode.getAttributes().getProperty(Node.INNER_NODES_PORT_FINDER_ATTR);
            if( dp != null && dp.getValue().equals(true) && modelNode instanceof Compartment )
            {
                MapPortFinder porFinder = new MapPortFinder();
                Compartment compartment = (Compartment)modelNode;
                Rectangle compartmentRectangle = viewBuilder.getNodeBounds(compartment);
                for( Node node : compartment.stream( Node.class ).filter( filter ) )
                {
                    Rectangle nodeRectangle = viewBuilder.getNodeBounds(node);
                    Point portPoint = getNearestFringePoint(nodeRectangle, compartmentRectangle);
                    porFinder.addPort(node.getCompleteNameInDiagram(), portPoint);
                }
                graphNode.setPortFinder(porFinder);
            }
        }

        if( modelNode instanceof Compartment && modelNode.isNotResizable() )
            graphNode.setAttribute("isNotResizable", "true");

        graphNode.applicationData = modelNode;
        return graphNode;
    }

    private static Point getNearestFringePoint(Rectangle innerRec, Rectangle outerRec)
    {
        //stores correspondence distance => point
        TreeMap<Double, Point> points = new TreeMap<>();

        //left point
        points.put(innerRec.getX() - outerRec.getX(), new Point(0, (int)innerRec.getCenterY() - outerRec.y));

        //right point
        points.put(outerRec.getMaxX() - innerRec.getMaxX(), new Point(outerRec.width, (int)innerRec.getCenterY() - outerRec.y));

        //bottom point
        points.put(outerRec.getMaxY() - innerRec.getMaxY(), new Point((int)innerRec.getCenterX() - outerRec.x, outerRec.height));

        //top point
        points.put(innerRec.getY() - outerRec.getY(), new Point((int)innerRec.getCenterX() - outerRec.x, 0));

        return points.firstEntry().getValue();
    }

    private static void addEdgesToGraph(Graph graph, Compartment compartment, Filter<DiagramElement> filter)
    {
        Predicate<biouml.model.Edge> pred = edge -> edge.getInput().isVisible() && edge.getOutput().isVisible();
        if(filter != null && filter.isEnabled())
            pred = pred.and( filter::isAcceptable );
        for(DiagramElement obj : compartment)
        {
            if( obj instanceof biouml.model.Edge )
            {
                // try to find edge nodes
                biouml.model.Edge modelEdge = (biouml.model.Edge)obj;
                if(!pred.test( modelEdge ))
                    continue;

                ru.biosoft.graph.Node graphInNode = null;
                ru.biosoft.graph.Node graphOutNode = null;

                //redirecting edge to compartment if needed
                Compartment inputCompartment = modelEdge.getInput().getCompartment();
                if(!inputCompartment.isVisible())
                    continue;
                DynamicProperty dp = inputCompartment.getAttributes().getProperty(Node.INNER_NODES_PORT_FINDER_ATTR);
                if( dp != null && dp.getValue().equals(true) )
                    graphInNode = graph.getNode(inputCompartment.getCompleteNameInDiagram());

                //redirecting edge to compartment if needed
                Compartment outputCompartment = modelEdge.getOutput().getCompartment();
                if(!outputCompartment.isVisible())
                    continue;
                dp = outputCompartment.getAttributes().getProperty(Node.INNER_NODES_PORT_FINDER_ATTR);
                if( dp != null && dp.getValue().equals(true) )
                    graphOutNode = graph.getNode(outputCompartment.getCompleteNameInDiagram());

                if( graphInNode == null )
                    graphInNode = graph.getNode(modelEdge.getInput().getCompleteNameInDiagram());

                if( graphOutNode == null )
                    graphOutNode = graph.getNode(modelEdge.getOutput().getCompleteNameInDiagram());

                if( graphInNode == null || graphOutNode == null )
                {
                    String msg = graphInNode == null ? "input node is not found, name = " + modelEdge.getInput().getName()
                            : "output node is not found, name = " + modelEdge.getOutput().getName();

                    //System.err.println ( "Edge " + modelEdge.getName ( ) + " was skipped for layout, " + msg + "." );
                    log.warning("Edge " + modelEdge.getName() + " was skipped for layout, " + msg + ".");
                    continue;
                }

                ru.biosoft.graph.Edge graphEdge = new ru.biosoft.graph.Edge(graphInNode, graphOutNode);
                graphEdge.fixed = modelEdge.isFixed();
                graphEdge.setAttribute("inputPortName", modelEdge.getInput().getCompleteNameInDiagram());
                graphEdge.setAttribute("outputPortName", modelEdge.getOutput().getCompleteNameInDiagram());
                if( modelEdge.getPath() != null )
                {
                    graphEdge.setPath( modelEdge.getPath().clone() );
                }
                else if( modelEdge.getInPort() != null && modelEdge.getOutPort() != null )
                {
                    graphEdge.createPath(modelEdge.getInPort().x, modelEdge.getInPort().y, modelEdge.getOutPort().x,
                            modelEdge.getOutPort().y);
                }

                graphEdge.applicationData = modelEdge;

                graph.addEdge(graphEdge);
            }
            else if( obj instanceof biouml.model.Compartment
                    && Diagram.getDiagram(obj).getType().needLayout((biouml.model.Node)obj) )
            {
                addEdgesToGraph(graph, (biouml.model.Compartment)obj, filter);
            }
        }
    }

    /**
     * Applies layout generated for given graph to the underlying diagram
     */
    public static void applyLayout(Graph graph)
    {
        if( graph.getNodes().size() == 0 )
            return;
        applyLayout(graph, Diagram.getDiagram((biouml.model.Node)graph.getNodes().get(0).applicationData));
    }

    public static void applyLayout(Graph graph, Diagram newDiagram)
    {
        applyLayout(graph, newDiagram, false);
    }
    /**
     * Applies layout generated for given graph to selected diagram
     * if simpleApply is true then semantic controller is not used - all nodes are simply set to given coordinates
     * use it only if nothing should be done to nodes during layout application
     * E.g.: when specie is moving its inside nodes (variable, unit of info) should also be moved to the border of specie node
     */
    public static void applyLayout(Graph graph, Diagram newDiagram, boolean simpleApply)
    {
        // apply nodes layout
        for(ru.biosoft.graph.Node graphNode: graph.getNodes() )
        {
            biouml.model.Node modelNode = (biouml.model.Node)getElementInSelectedDiagram((biouml.model.Node)graphNode.applicationData,
                    newDiagram);
            if( modelNode == null )
                continue;

            //apply node orientationClockwise

            if (graphNode.hasAttribute("orientation"))
            {
                DynamicProperty orientation = modelNode.getAttributes().getProperty("orientation");
                if (orientation != null)
                {
                    PortOrientation newOrientation = PortOrientation.createInstance(graphNode.getAttribute("orientation"));
                    if( !newOrientation.equals( orientation.getValue() ) )
                    {
                        orientation.setValue( newOrientation );
                        modelNode.setShapeSize(new Dimension(graphNode.width, graphNode.height));
                    }
                }
            }

            Point location = modelNode.getLocation();
            if( graphNode.x != location.x || graphNode.y != location.y )
            {
                if( simpleApply || Util.isCompartment(graphNode) )
                {
                    modelNode.setLocation(graphNode.x, graphNode.y);
                }
                else
                {
                    Dimension offset = new Dimension(graphNode.x - location.x, graphNode.y - location.y);
                    SemanticController semanticController = newDiagram.getType().getSemanticController();
                    try
                    {
                        if( semanticController instanceof XmlDiagramSemanticController )
                            ( (XmlDiagramSemanticController)semanticController ).disableMoveFunction();

                        modelNode.setLocation( graphNode.x, graphNode.y );
                        
                        if( modelNode instanceof Compartment && !newDiagram.getType().needLayout( modelNode ) )
                        {
                            for (Node node: modelNode.recursiveStream().select( Node.class ).without( modelNode ))
                            {
                                Point nodeLocation = node.getLocation();
                                nodeLocation.translate( offset.width, offset.height );
                                node.setLocation( nodeLocation );
                            }
                        }
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Can not move node", e);
                    }
                    finally
                    {
                        if( semanticController instanceof XmlDiagramSemanticController )
                            ( (XmlDiagramSemanticController)semanticController ).enableMoveFunction();
                    }
                }
                if( modelNode.isFixed() ) // layouter moved fixed node - situation is possible though unlikely
                    log.warning("Fixed node " + modelNode.getName() + " was moved after layouting");
            }

            Dimension dimension = modelNode.getShapeSize();
            if( dimension != null && newDiagram.getType().needLayout( modelNode )) //if not need layout then shape size was not changed by layout (but location was)
            {
                if( graphNode.width != dimension.width || graphNode.height != dimension.height )
                    modelNode.setShapeSize(new Dimension(graphNode.width, graphNode.height));
            }

            if( graphNode.hasAttribute(LabelLayouter.LABEL_OFFSET) )
                modelNode.setTitleOffset(Integer.parseInt(graphNode.getAttribute(LabelLayouter.LABEL_OFFSET)));

            if( graphNode.hasAttribute(LabelLayouter.LABEL_ANGLE) )
                modelNode.setTitleAngle(Double.parseDouble(graphNode.getAttribute(LabelLayouter.LABEL_ANGLE)));
//            modelNode.setFixed(true);
        }

        // apply edges layout
        for( ru.biosoft.graph.Edge graphEdge: graph.getEdges() )
        {
            if (graphEdge.fixed)
                continue;
                
            biouml.model.Edge modelEdge = (biouml.model.Edge)getElementInSelectedDiagram((biouml.model.Edge)graphEdge.applicationData,
                    newDiagram);

            if( modelEdge == null )
                continue;

            modelEdge.getInput().addEdge(modelEdge);
            modelEdge.getOutput().addEdge(modelEdge);
            modelEdge.setPath(graphEdge.getPath());
        }

        //layout edges from inside of fixed compartments
//        for( biouml.model.Edge e : newDiagram.recursiveStream().select( Compartment.class )
//                .filter( n -> isCompletelyFixed( n )).flatMap( n -> n.edges() ) )
//        {
//            newDiagram.getType().getSemanticController().recalculateEdgePath( e );
//        }
    }

    protected static DiagramElement getElementInSelectedDiagram(DiagramElement de, Diagram diagram)
    {
        String fullDiagramName = null;
        if( de != null )
        {
            if( de instanceof Compartment )
            {
                fullDiagramName = ( (Compartment)de ).getCompleteNameInDiagram();
            }
            else
            {
                fullDiagramName = de.getCompleteNameInDiagram();
            }
            try
            {
                return diagram.findDiagramElement(fullDiagramName);
            }
            catch( Exception e )
            {
            }
        }
        return null;
    }

    public static boolean reApplyLayout(Graph graphFrom, Graph graphTo)
    {
        boolean extraNodes = false;
        for(ru.biosoft.graph.Node nodeTo: graphTo.getNodes() )
        {
            ru.biosoft.graph.Node nodeFrom = graphFrom.getNode(nodeTo.getName());
            if(nodeFrom == null)
            {
                nodeFrom = graphFrom.getNode(nodeTo.getName().replace("_", "-"));
            }
            if( nodeFrom != null )
            {
                nodeTo.x = nodeFrom.x;
                nodeTo.y = nodeFrom.y;
                nodeTo.width = nodeFrom.width;
                nodeTo.height = nodeFrom.height;
                nodeTo.fixed = true;

                ru.biosoft.graph.Node.copyAttributes( nodeFrom, nodeTo );
            }
            else
                extraNodes = true;
        }

        for( Edge edgeTo : graphTo.getEdges() )
        {
            Edge edgeFrom = findCorrepsondingEdge(edgeTo, graphTo, graphFrom);
            if (edgeFrom != null)
                edgeTo.setPath(edgeFrom.getPath());
        }
        return extraNodes;
    }

    /**
     * Tries to find edge in <b>targetGraph</b> which is 'the same' as <b>edge</b> in <b>graph</b>
     * The same means that it connects nodes with same names as does <b>edge</b><br>
     * If there are more then one such edge - we check edges applicationData which should store biouml.model.Edge from the diagram (if graph was generated from diagram)
     * @return found edge or null if there no such edge in targetGraph
     */
    public static Edge findCorrepsondingEdge(Edge edge, Graph graph, Graph targetGraph)
    {
        //we expect that node names are the same for two graphs
        ru.biosoft.graph.Node in = targetGraph.getNode(edge.getFrom().getName());
        ru.biosoft.graph.Node out = targetGraph.getNode(edge.getTo().getName());

        Edge result = null;

        if( in != null && out != null )
        {
            result = targetGraph.getEdge(in, out);

            //in this case in and out nodes are not enough to unambiguously define edge - need to check all slave edges as well - we try to do this using data from coresponding diagram
            if( result != null && result.master && result.slaves != null )
            {
                biouml.model.Edge originalEdge = getDiagramEdge(edge);
                if( originalEdge == null )
                    return result;
                String originalName = originalEdge.getCompleteNameInDiagram();
                biouml.model.Edge resultOriginalEdge = getDiagramEdge(result);
                if( resultOriginalEdge == null || !resultOriginalEdge.getCompleteNameInDiagram().equals(originalName) )
                {
                    for( Edge slaveEdge : result.slaves )
                    {
                        biouml.model.Edge originalSlaveEdge = getDiagramEdge(slaveEdge);
                        if( originalSlaveEdge != null && originalSlaveEdge.getCompleteNameInDiagram().equals(originalName) )
                            return slaveEdge;
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return edge from Diagram which is correspondent to this Graph's edge <b>e</b> or null if this data is unavailable
     */
    public static biouml.model.Edge getDiagramEdge(Edge e)
    {
        if (e.applicationData == null || !(e.applicationData instanceof biouml.model.Edge))
                return null;
        return (biouml.model.Edge)e.applicationData;
    }
    
//    private static boolean isCompletelyFixed(Compartment compartment)
//    {
//        return compartment.isFixed() && compartment.recursiveStream().allMatch( n -> n.isFixed() );
//    }
}
