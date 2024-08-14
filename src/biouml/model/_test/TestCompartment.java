package biouml.model._test;

import java.awt.Dimension;
import java.awt.Point;
import junit.framework.TestCase;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Stub;

public class TestCompartment extends TestCase
{
    protected static final DefaultSemanticController semanticController = new DefaultSemanticController ( );

    public TestCompartment ( String name )
    {
        super ( name );
    }

    public void testClone ( ) throws Exception
    {
        Diagram diagram = makeTestDiagram ( );
        assertNotNull ( diagram );

        // make cloning
        Compartment c1 = ( Compartment ) diagram.get ( "c1" );
        Compartment c2 = ( Compartment ) diagram.get ( "c2" );
        Compartment newC1 = c1.clone ( c2, c1.getName ( ) );
        diagram.remove ( c1.getName ( ) );
        c2.put ( newC1 );

        // checking cloned diagram
        c2 = ( Compartment ) diagram.get ( "c2" );
        assertNotNull ( "Compartment 'c2' not found.", c2 );
        c1 = ( Compartment ) c2.get ( "c1" );
        assertNotNull ( "Inserted compartment not found", c1 );
        assertEquals ( "Diagram contains wrong edge count", 2,
                countEdges ( diagram ) );
        assertEquals ( "'c1' contains wrong edge count", 0, countEdges ( c1 ) );
        assertEquals ( "'c2' contains wrong edge count", 0, countEdges ( c2 ) );
        assertEquals ( "Diagram contains wrong node count", 2,
                countNodes ( diagram ) );
        assertEquals ( "'c2' contains wrong node count", 2, countNodes ( c2 ) );
        assertEquals ( "'c1' contains wrong node count", 1, countNodes ( c1 ) );
        Node a = ( Node ) c1.get ( "A" );
        assertNotNull ( "'A' not found in nested compartment", a );
        assertEquals ( "'A' contains wrong edge count", 0, countEdges ( a ) );

        // check variable name changing
        VariableRole aVar = a.getRole ( VariableRole.class );
        assertEquals ( "Name of variable is wrong", "$c1.A", aVar.getName ( ) ); // name of inner variable now do not change (to $c2.c1.a) after cloning

        // check location
        Point p = a.getLocation ( );
        assertEquals ( "x coordinate of 'A' wrong", 10, ( int ) p.getX ( ) );
        assertEquals ( "y coordinate of 'A' wrong", 10, ( int ) p.getY ( ) );
    }

    public void testMoveNodeToParent ( ) throws Exception
    {
        Diagram diagram = makeTestDiagram02 ( );
        assertNotNull ( diagram );
        Compartment outer = ( Compartment ) diagram.get ( "outer" );
        assertNotNull ( outer );
        // make moving
        Node oldNode = ( Node ) outer.get ( "A" );
        
        semanticController.move ( oldNode, diagram, new Dimension ( 200, 200 ) , null);
        
        // check
        assertEquals ( "Wrong childs in diagram", 3, diagram.getSize ( ) );
        assertEquals ( "diagram contains wrong edge count", 1, countEdges ( diagram ) );
        Node a = ( Node ) diagram.get ( "A" );
        assertNotNull ( "Node 'A' not found in diagram", a );
    }

    private Diagram makeTestDiagram ( ) throws Exception
    {
        Diagram diagram = new Diagram ( null, new Stub ( null, "d1" ), null );

        Node r = new Node ( diagram, new Stub ( null, "R" ) );

        Compartment c1 = new Compartment ( diagram, new Stub ( null, "c1" ) );
        Node a = new Node ( c1, new Stub ( null, "A" ) );
        a.setRole ( new VariableRole ( a, 1.0 ) );
        a.setLocation ( new Point ( 10, 10 ) );
        c1.put ( a );

        Compartment c2 = new Compartment ( diagram, new Stub ( null, "c2" ) );
        Node b = new Node ( c2, new Stub ( null, "B" ) );
        c2.put ( b );

        diagram.put ( r );
        diagram.put ( c1 );
        diagram.put ( c2 );

        Edge eAR = new Edge ( diagram, new Stub ( null, "eAR" ), a, r );
        Edge eRB = new Edge ( diagram, new Stub ( null, "eRB" ), r, b );
        diagram.put ( eAR );
        diagram.put ( eRB );

        return diagram;
    }

    private Diagram makeTestDiagram02 ( ) throws Exception
    {
        Diagram diagram = new Diagram ( null, new Stub ( null, "d2" ), null );

        Compartment outer = new Compartment ( diagram,
                new Stub ( null, "outer" ) );
        Node a = new Node ( outer, new Stub ( null, "A" ) );
        a.setRole ( new VariableRole ( a, 1.0 ) );
        a.setLocation ( new Point ( 10, 10 ) );
        outer.put ( a );
        Node b = new Node ( outer, new Stub ( null, "B" ) );
        b.setRole ( new VariableRole ( b, 2.0 ) );
        b.setLocation ( new Point ( 60, 10 ) );
        outer.put ( b );
        Node r = new Node ( outer, new Stub ( null, "R" ) );
        Edge eAR = new Edge ( outer, new Stub ( null, "eAR" ), a, r );
        Edge eRB = new Edge ( outer, new Stub ( null, "eRB" ), r, b );
        outer.put ( eAR );
        outer.put ( eRB );
        diagram.put ( outer );
        return diagram;
    }

    public static int countEdges ( Compartment compartment )
    {
        int count = 0;
        for(DiagramElement obj : compartment)
        {
            if ( obj instanceof Edge )
            {
                Edge edge = ( Edge ) obj;
                assertNotNull ( "Input of " + edge.getName ( )
                        + " can't be null", edge.getInput ( ) );
                assertNotNull ( "Output of " + edge.getName ( )
                        + " can't be null", edge.getOutput ( ) );
                assertSame ( "Wrong origin of edge " + edge.getName ( ) + "",
                        compartment, edge.getOrigin ( ) );
                count++;
            }
        }
        return count;
    }

    public static int countEdges ( Node node )
    {
        int count = 0;
        for(Edge edge : node.edges())
        {
            assertTrue ( "Input or Output of " + edge.getName ( )
                    + " should point to this node",
                    edge.getInput ( ) == node || edge.getOutput ( ) == node );
            count++;
        }
        return count;
    }

    public int countNodes ( Compartment compartment )
    {
        int count = 0;
        for(Node node : compartment.stream( Node.class ))
        {
            assertSame ( "Wrong origin of node " + node.getName ( ) + "",
                    compartment, node.getOrigin ( ) );
            count++;
        }
        return count;
    }
}