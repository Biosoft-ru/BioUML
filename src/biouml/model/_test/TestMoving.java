package biouml.model._test;

import java.awt.Dimension;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

public class TestMoving extends DiagramChecker
{
    protected static final DefaultSemanticController semanticController = new DefaultSemanticController();

    public TestMoving(String name)
    {
        super(name);
    }

    public void testMoveBetweenSiblings() throws Exception
    {
        makeTestDiagram01();
        assertNotNull("Can't create diagram", diagram);
        Compartment left = (Compartment)diagram.get("left");
        assertNotNull("'left' not found.", left);
        Compartment right = (Compartment)diagram.get("right");
        assertNotNull("'right' not found.", right);

        // make cloning (move 'A' from 'right' to 'left')
        Node a = (Node)right.get("A");
        assertNotNull("Node 'A' in '" + right.getName() + "' not found.", a);
        Node oldNode = a;
        semanticController.move(oldNode, left, new Dimension( -100, 0), null);
        checkDiagram();

        // make cloning back (move 'A' from 'left' to 'right')
        a = (Node)left.get("A");
        assertNotNull("Node 'A' in '" + left.getName() + "' not found.", a);
        oldNode = a;
        semanticController.move(oldNode, right, new Dimension( -100, 0), null);
        checkDiagram();
    }

    public void testMoveCompartmentBetweenSublings() throws Exception
    {
        makeTestDiagram02();
        assertNotNull("Can't create diagram", diagram);
        Compartment left = (Compartment)diagram.get("left");
        assertNotNull("'left' not found.", left);
        Compartment right = (Compartment)diagram.get("right");
        assertNotNull("'right' not found.", right);
        checkDiagram();

        // make cloning (move 'inner' from 'right' to 'left')
        Compartment inner = (Compartment)right.get("inner");
        assertNotNull("Compartment 'inner' in '" + right.getName() + "' not found.", inner);
        Node oldNode = inner;
        semanticController.move(oldNode, left, new Dimension( -100, 0), null);
        checkDiagram();
    }

    public void testMoveNestedCompartmentIntoCompartment() throws Exception
    {
        makeTestDiagram02();
        assertNotNull("Can't create diagram", diagram);
        Compartment left = (Compartment)diagram.get("left");
        assertNotNull("'left' not found.", left);
        Compartment right = (Compartment)diagram.get("right");
        assertNotNull("'right' not found.", right);
        checkDiagram();

        // make cloning (move 'right' into 'left')
        Node oldNode = right;
        semanticController.move(oldNode, left, new Dimension( -100, 0), null);
        checkDiagram();
        countEdges(2);
        assertEquals("Too many nodes in diagram", 1, diagram.getSize());
    }

    private void makeTestDiagram01() throws Exception
    {
        diagram = new Diagram(null, new Stub(null, "subling2"), new PathwaySimulationDiagramType());
        Compartment left = new Compartment(diagram, new Stub(null, "left"));
        diagram.put(left);
        Compartment right = new Compartment(diagram, new Stub(null, "right"));
        diagram.put(right);
        Node a = new Node(right, new Substance(null, "A"));
        right.put(a);
        Node b = new Node(right, new Substance(null, "B"));
        right.put(b);
        Node r = new Node(right, new Reaction(null, "R"));
        right.put(r);
        Edge eAR = new Edge(right, new SpecieReference(null, "eAR"), a, r);
        Edge eRB = new Edge(right, new SpecieReference(null, "eRB"), r, b);
        right.put(eAR);
        right.put(eRB);
    }

    private void makeTestDiagram02() throws Exception
    {
        diagram = new Diagram(null, new Stub(null, "subling2"), new PathwaySimulationDiagramType());
        Compartment left = new Compartment(diagram, new biouml.standard.type.Compartment(null, "left"));
        diagram.put(left);
        Compartment right = new Compartment(diagram, new biouml.standard.type.Compartment(null, "right"));
        diagram.put(right);
        Node a = new Node(right, new Substance(null, "A"));
        right.put(a);
        Node b = new Node(right, new Substance(null, "B"));
        right.put(b);
        Compartment inner = new Compartment(right, new biouml.standard.type.Compartment(null, "inner"));
        Node r = new Node(inner, new Reaction(null, "R"));
        inner.put(r);
        right.put(inner);
        Edge eAR = new Edge(right, new SpecieReference(null, "eAR"), a, r);
        Edge eRB = new Edge(right, new SpecieReference(null, "eRB"), r, b);
        right.put(eAR);
        right.put(eRB);
    }
}