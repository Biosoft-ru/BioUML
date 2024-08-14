package ru.biosoft.server.servlets.webservices;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import biouml.model.Diagram;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.graph.Edge;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.LayoutJobControlImpl;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.Node;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;

public class LayoutContext
{
    public static final String LAYOUT_INFO_PATH = "beans/layoutGraph";

    protected Graph graph;
    protected Diagram diagram;

    public static LayoutContext getLayoutContext()
    {
        LayoutContext result = null;
        String processName = LAYOUT_INFO_PATH;
        Object layoutObj = WebServicesServlet.getSessionCache().getObject(processName);
        if( ( layoutObj == null ) || ! ( layoutObj instanceof LayoutContext ) )
        {
            result = new LayoutContext();
            WebServicesServlet.getSessionCache().addObject(processName, result, true);
        }
        else
        {
            result = (LayoutContext)layoutObj;
        }
        return result;
    }

    public Graph getGraph()
    {
        return graph;
    }

    public void setGraph(Graph graph)
    {
        this.graph = graph;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
    }

    public void applyLayout(Diagram newDiagram, Layouter layouter) throws Exception
    {
        Graph newGraph = DiagramToGraphTransformer.generateGraph(diagram, diagram.getType().getSemanticController().getFilter());

        if( newGraph != null && graph != null )
        {
            int nodeToLayoutCount = reApplyLayout(graph, newGraph);
            if( nodeToLayoutCount == 0 )
            {
                DiagramToGraphTransformer.applyLayout(graph, newDiagram);
            }
            else
            {
                DiagramToGraphTransformer.applyLayout(newGraph, newDiagram);
                PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
                LayoutJobControl jobControl = new LayoutJobControlImpl(pathwayLayouter.estimate(newGraph, 0));
                pathwayLayouter.doLayout(newGraph, jobControl);
                DiagramToGraphTransformer.applyLayout(newGraph, newDiagram);
            }
        }
    }

    protected int reApplyLayout(Graph graphFrom, Graph graphTo) throws Exception
    {
        int nodeCount = 0;
        Iterator<Node> nodeIterator = graphFrom.nodeIterator();
        while( nodeIterator.hasNext() )
        {
            Node nodeFrom = nodeIterator.next();
            Node nodeTo = graphTo.getNode(nodeFrom.getName());
            if( nodeTo != null )
            {
                nodeTo.x = nodeFrom.x;
                nodeTo.y = nodeFrom.y;
                nodeTo.width = nodeFrom.width;
                nodeTo.height = nodeFrom.height;
                nodeTo.fixed = true;
            }
            else
                nodeCount++;
        }

        Iterator<Edge> edgeIterator = graphFrom.edgeIterator();
        while( edgeIterator.hasNext() )
        {
            Edge edgeFrom = edgeIterator.next();
            Node in = edgeFrom.getFrom();
            Node out = edgeFrom.getTo();
            Edge edgeTo = graphTo.getEdge(in, out);
            if( edgeTo != null )
            {
                edgeTo.setPath(edgeFrom.getPath());
            }
        }
        return nodeCount;
    }
    
    public BufferedImage generatePreviewImage()
    {
        return generatePreviewImage(true);
    }

    public BufferedImage generatePreviewImage(boolean applyLayout)
    {
        if(applyLayout) DiagramToGraphTransformer.applyLayout(graph);

        Rectangle bounds = WebDiagramsProvider.createView(diagram).getBounds();
        TileInfo tile = new TileInfo();
        tile.setX(0);
        tile.setY(0);
        tile.setWidth(bounds.width + 30);
        tile.setHeight(bounds.height + 30);
        tile.setScale(Math.min(200.0 / bounds.height, 400.0 / bounds.width));
        return WebDiagramsProvider.createImage(tile, diagram);
    }
}
