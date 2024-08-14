package ru.biosoft.plugins.graph;

import java.awt.Graphics;
import java.util.Iterator;

import javax.annotation.Nonnull;

import ru.biosoft.graph.Edge;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.Node;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.PolylineView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;

/**
 * Routines for graph view generation.
 */
public class GraphViewBuilder
{
    //////////////////////////////////////////////////////////////////
    // Methods to build the diagram view
    //

    public CompositeView createGraphView(Graph[] graphs, GraphViewOptions options, Graphics g)
    {
        CompositeView view = new CompositeView();

        for( Graph graph : graphs )
            view.add(createGraphView(graph, options, g));

        return view;
    }

    /**
     * Creates the graph view
     */
    public @Nonnull CompositeView createGraphView(Graph graph, GraphViewOptions options, Graphics g)
    {
        CompositeView graphView = new CompositeView();

        // build all nodes
        Iterator<Node> nodes = graph.nodeIterator();
        while( nodes.hasNext() )
        {
            View view = createNodeView(nodes.next(), options, g);
            graphView.add(view);
        }

        // build all edges
        Iterator<Edge> edges = graph.edgeIterator();
        while( edges.hasNext() )
        {
            View view = createEdgeView(edges.next(), options);
            graphView.add(view);
        }

        if( options.isDebugMode() )
            graphView.add(new BoxView(options.getGraphBorderPen(), null, graphView.getBounds()));

        return graphView;
    }

    /**
     * Creates node view that is node title and rectangle.
     */
    public @Nonnull View createNodeView(Node node, GraphViewOptions options, Graphics g)
    {
        CompositeView view = new CompositeView();

        View bound = null;
        String shape = node.getAttribute("shape");
        if( "diamond".equals(shape) )
        {
            PolygonView diamond = new PolygonView(options.getNodePen(), null);
            diamond.addPoint(node.x + node.width / 2, node.y);
            diamond.addPoint(node.x + node.width, node.y + node.height / 2);
            diamond.addPoint(node.x + node.width / 2, node.y + node.height);
            diamond.addPoint(node.x, node.y + node.height / 2);

            bound = diamond;
        }
        else
        {
            bound = new BoxView(options.getNodePen(), null, node.x, node.y, node.width, node.height);
        }
        view.add(bound);

        if( !node.getName().startsWith("dummy") )
        {
            String name = node.getName();
            View title = new TextView(name, options.getNodeTitleFont(), g);
            view.add(title, CompositeView.X_CC | CompositeView.Y_CC, null);
        }

        return view;
    }

    public @Nonnull CompositeView createEdgeView(Edge edge, GraphViewOptions options)
    {
        CompositeView view = new CompositeView();

        // create tip for directed graph view
        ArrowView.Tip tip = ArrowView.createSimpleTip(options.getEdgePen(), 7, 5);

        // TODO: Throw exception
        if( edge.getPath().npoints < 2 )
            return view;

        int x1 = edge.getPath().xpoints[edge.getPath().npoints - 2];
        int y1 = edge.getPath().ypoints[edge.getPath().npoints - 2];
        int x2 = edge.getPath().xpoints[edge.getPath().npoints - 1];
        int y2 = edge.getPath().ypoints[edge.getPath().npoints - 1];

        int dx = x2 - x1;
        int dy = y2 - y1;
        double l = Math.sqrt(dx * dx + dy * dy);
        double alpha = Math.asin(dy / l);
        if( dx < 0 )
            alpha = Math.PI - alpha;

        ArrowView.locateTip(tip, alpha, x2, y2);

        view.add(new PolylineView(options.getEdgePen(), edge.getPath()));
        view.add(tip.view);

        return view;
    }
}
