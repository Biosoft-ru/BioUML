package ru.biosoft.plugins.graph._test;

import java.awt.Point;

import ru.biosoft.graph.Edge;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HasCyclesException;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.LayoutJobControlImpl;
import ru.biosoft.graph.Node;
import ru.biosoft.graph.Path;
import junit.framework.TestCase;

public class TestHierarchicLayouter extends TestCase
{
    public void testNodeCoordinatesChanging()
    {
        Graph graph = new Graph();
        Node n1 = new Node("n1", 0, 0, 30, 30);
        Node n2 = new Node("n2", 0, 0, 30, 30);
        
        graph.addNode(n1);
        graph.addNode(n2);
        graph.addEdge(new Edge(n1, n2));
        
        HierarchicLayouter layouter = new HierarchicLayouter();
        layouter.setLayerDeltaX( 100 );
        layouter.doLayout(graph, new LayoutJobControlImpl( layouter.estimate( graph, 0 )));
        
        Node newN1 = graph.getNode("n1");
        Node newN2 = graph.getNode("n2");
        
        assertNotNull(newN1);
        assertNotNull(newN2);
        assertFalse( new Point(newN1.x, newN1.y).
                equals(new Point(newN2.x,newN2.y)));
    }
    
    public void testGraphConnectivity() throws HasCyclesException
    {
        Graph connectedGraph = new Graph();
        Graph unconnectedGraph = new Graph();
        Node n1 = new Node("n1", 0, 0, 30, 30);
        Node n2 = new Node("n2", 0, 0, 30, 30);
        
        connectedGraph.addNode(n1);
        connectedGraph.addNode(n2);
        connectedGraph.addEdge(new Edge(n1, n2));
        
        unconnectedGraph.addNode(n1);
        unconnectedGraph.addNode(n2);
        
        HierarchicLayouter layouter1 = new HierarchicLayouter();
        layouter1.setLayerDeltaX( 100 );
        layouter1.doLayout(connectedGraph, new LayoutJobControlImpl( layouter1.estimate( connectedGraph, 0 )));
        
        HierarchicLayouter layouter2 = new HierarchicLayouter();
        layouter2.setLayerDeltaX( 100 );
        layouter2.doLayout(unconnectedGraph, new LayoutJobControlImpl( layouter2.estimate( connectedGraph, 0 )));
        
        assertEquals( 1, connectedGraph.getRoots().size() );
        // don't have roots because unconnected
        assertNull( unconnectedGraph.getRoots() );
    }
    
    public void testLongEdge()
    {
        Graph graph = new Graph();
        Node n1 = new Node("n1", 0, 0, 30, 30);
        Node n2 = new Node("n2", 0, 0, 30, 30);
        Node n3 = new Node("n3", 0, 0, 30, 30);
        Node n4 = new Node("n4", 0, 0, 30, 30);
        
        graph.addNode(n1);
        graph.addNode(n2);
        graph.addNode(n3);
        graph.addNode(n4);
        graph.addEdge(new Edge(n1, n2));
        graph.addEdge(new Edge(n2, n3));
        graph.addEdge(new Edge(n3, n4));
        graph.addEdge(new Edge(n4, n1));
        
        HierarchicLayouter layouter = new HierarchicLayouter();
        layouter.setLayerDeltaX( 100 );
        layouter.doLayout(graph, new LayoutJobControlImpl( layouter.estimate( graph, 0 )));
        
        assertEquals(4, graph.edgeCount());
        assertEquals(4, graph.nodeCount());
        
        Edge bezierEdge = graph.getEdge(n1, n2);
        Path bezierPath = bezierEdge.getPath();
        
        Edge longestEdge = graph.getEdge(n4, n1);
        Path longestPath = longestEdge.getPath();
        
        assertEquals(6, bezierPath.npoints);
        assertEquals(10, longestPath.npoints);
    }
}
