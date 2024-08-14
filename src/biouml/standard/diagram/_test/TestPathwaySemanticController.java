package biouml.standard.diagram._test;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model._test.DiagramChecker;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Stub;

public class TestPathwaySemanticController extends DiagramChecker
{
    public TestPathwaySemanticController( String name )
    {
        super(name);
    }

    public void testRemove() throws Exception
    {
        diagram = makeTestDiagram01();
        assertNotNull( "Diagram is null",diagram );
        SemanticController controller = diagram.getType().getSemanticController();
        assertNotNull( "Semantic Controller is null",controller );

        Node b = (Node)diagram.get("B");
        assertNotNull( "Node 'B' not found",b );
        boolean removed = controller.remove( b );
        assertTrue( "Semantic Controller report about failure on removing",removed );
        assertTrue( "Node 'B' not removed.",!diagram.contains("B") );

        checkDiagram();
    }

    private Diagram makeTestDiagram01() throws Exception
    {
        Diagram diagram = new Diagram(null,new Stub(null, "d2"),new PathwaySimulationDiagramType() );
        Node a = new Node(diagram,new Stub(null, "A"));
        diagram.put( a );
        Node b = new Node(diagram,new Stub(null, "B"));
        diagram.put( b );
        Node r = new Node(diagram,new Stub(null, "R"));
        diagram.put( r );
        Edge eAR = new Edge(diagram,new Stub(null, "eAR"),a,r);
        Edge eRB = new Edge(diagram,new Stub(null, "eRB"),r,b);
        diagram.put( eAR );
        diagram.put( eRB );
        return diagram;
    }
}