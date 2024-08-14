package biouml.model._test;

import java.awt.Dimension;
import junit.framework.TestCase;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Stub;

public class TestNode extends TestCase
{
    public TestNode(String name)
    {
        super(name);
    }

    public void testClone() throws Exception
    {
        Diagram diagram = makeTestDiagram();
        assertNotNull(diagram);

        Node a = (Node)diagram.get("A");
        assertNotNull("'A' not found.", a);
        Compartment outer = (Compartment)diagram.get("outer");
        assertNotNull("'outer' not found.", outer);
        Compartment inner = (Compartment)outer.get("inner");
        assertNotNull("'inner' not found.", inner);

        // make cloning (move 'A' from diagram to 'outer')
        Node oldNode = a;

        DefaultSemanticController semanticController = new DefaultSemanticController();
        semanticController.move(oldNode, outer, new Dimension(200, 200), null);

        // check diagram
        assertTrue("Node 'A' should be moved from diagram", !diagram.contains("A"));
        assertEquals("Count of edges and nodes in diagram is wrong", 5, diagram.getSize());
        assertEquals("Diagram contains wrong edge count", 2, countEdges(diagram));
        assertEquals("Diagram contains wrong node count", 3, countNodes(diagram));
        assertEquals("'outer' contains wrong edge count", 0, countEdges(outer));
        assertEquals("'outer' contains wrong node count", 2, countNodes(outer));

        // make next cloning (move 'A' from 'outer' to 'inner')
        a = (Node)outer.get("A");
        assertNotNull("'A' not found in 'outer'.", a);
        oldNode = a;
        semanticController.move(oldNode, inner, new Dimension( -200, -200), null);

        // check diagram
        assertTrue("Node 'A' should be moved from 'outer'", !outer.contains("A"));
        assertEquals("Count of edges and nodes in diagram is wrong", 5, diagram.getSize());
        assertEquals("Diagram contains wrong edge count", 2, countEdges(diagram));
    }

    private Diagram makeTestDiagram() throws Exception
    {
        Diagram diagram = new Diagram(null, new Stub(null, "test diagram"), null);
        Node a = new Node(diagram, new Stub(null, "A"));
        Node b = new Node(diagram, new Stub(null, "B"));
        Node r = new Node(diagram, new Stub(null, "R"));
        diagram.put(a);
        diagram.put(b);
        diagram.put(r);
        Compartment outer = new Compartment(diagram, new Stub(null, "outer"));
        diagram.put(outer);
        Compartment inner = new Compartment(outer, new Stub(null, "inner"));
        outer.put(inner);
        Edge eAR = new Edge(diagram, new Stub(null, "eAR"), a, r);
        Edge eRB = new Edge(diagram, new Stub(null, "eRB"), r, b);
        diagram.put(eAR);
        diagram.put(eRB);
        return diagram;
    }

    private int countEdges(Compartment compartment)
    {
        int count = 0;
        for(Edge edge : compartment.stream( Edge.class ))
        {
            assertNotNull("Input of " + edge.getName() + " can't be null", edge.getInput());
            assertNotNull("Output of " + edge.getName() + " can't be null", edge.getOutput());
            assertSame("Wrong origin of edge " + edge.getName() + "", compartment, edge.getOrigin());
            count++;
        }
        return count;
    }

    private int countNodes(Compartment compartment)
    {
        int count = 0;
        for(Node node : compartment.stream( Node.class ))
        {
            assertSame("Wrong origin of node " + node.getName() + "", compartment, node.getOrigin());
            count++;
        }
        return count;
    }
}