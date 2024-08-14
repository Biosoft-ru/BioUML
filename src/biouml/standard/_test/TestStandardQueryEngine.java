package biouml.standard._test;

import java.util.Map;
import java.util.function.Function;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.StandardQueryEngine;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.RNA;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import biouml.workbench.graphsearch.QueryEngine;
import biouml.workbench.graphsearch.QueryOptions;
import biouml.workbench.graphsearch.SearchElement;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class TestStandardQueryEngine extends TestCase
{
    public TestStandardQueryEngine(String name)
    {
        super(name);
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestStandardQueryEngine.class.getName());
        suite.addTest(new TestStandardQueryEngine("testSearchLinkedSimpleBoth"));
        suite.addTest(new TestStandardQueryEngine("testSearchLinkedSimpleUp"));
        suite.addTest(new TestStandardQueryEngine("testSearchLinkedSimpleDown"));
        suite.addTest(new TestStandardQueryEngine("testSearchLinkedDeepBoth"));
        return suite;
    }

    @Override
    public void setUp() throws Exception
    {
        // create repository
        CollectionFactory.unregisterAllRoot();
        repository = CollectionFactory.createRepository("../data/test/biouml/standard");
        assertNotNull("Can't create repository", repository);
        module = (Module)repository.get("examples");
        assertNotNull("Can't find module 'examples'", module);
    }

    public void testSearchLinkedSimpleBoth() throws Exception
    {
        Diagram diagram = new Diagram(module.getDiagrams(), new DiagramInfo(module.getDiagrams(), "test01"), new PathwayDiagramType());
        Base nodeKernel = module.getKernel( RNA.class, "E" );
        assertNotNull("Can't find RNA 'E'", nodeKernel);
        QueryEngine engine = new StandardQueryEngine();
        SearchElement[] nodeInfosArray = engine.searchLinked(new SearchElement[] {new SearchElement(nodeKernel)}, new QueryOptions(1,
                BioHub.DIRECTION_BOTH), new TargetOptions(module.getCompletePath()), null);
        assertNotNull("'searchLink' returns null", nodeInfosArray);
        Map<String, SearchElement> nodes = toMap(nodeInfosArray);
        assertEquals("Wrong size.", 5, nodes.size());
        checkEdges(diagram.getNodes());

        for( String node : nodes.keySet() )
            System.out.println(node);

        assertTrue("Node 'B' not found.", nodes.containsKey("B"));
        assertTrue("Node 'D' not found.", nodes.containsKey("D"));
        assertTrue("Node 'E' not found.", nodes.containsKey("E"));
        assertTrue("Node 'G' not found.", nodes.containsKey("G"));
        assertTrue("Node 'R000001' not found.", nodes.containsKey("R000001"));
    }

    public void testSearchLinkedSimpleUp() throws Exception
    {
        Diagram diagram = new Diagram(module.getDiagrams(), new Stub(null, "test"), new PathwayDiagramType());
        Base nodeKernel = module.getKernel( RNA.class, "E" );
        assertNotNull("Can't find substance 'E'", nodeKernel);
        QueryEngine engine = new StandardQueryEngine();
        SearchElement[] nodeInfosArray = engine.searchLinked(new SearchElement[] {new SearchElement(nodeKernel)}, new QueryOptions(1,
                BioHub.DIRECTION_UP), new TargetOptions(module.getCompletePath()), null);
        assertNotNull("'searchLink' returns null", nodeInfosArray);
        Map<String, SearchElement> nodes = toMap(nodeInfosArray);
        assertEquals("Wrong size.", 1, nodes.size());
        checkEdges(diagram.getNodes());

        for( String node : nodes.keySet() )
            System.out.println(node);

        assertTrue("Node 'G' not found.", nodes.containsKey("G"));
    }

    public void testSearchLinkedSimpleDown() throws Exception
    {
        Diagram diagram = new Diagram(module.getDiagrams(), new Stub(null, "test"), new PathwayDiagramType());
        Base nodeKernel = module.getKernel( RNA.class, "E" );
        assertNotNull("Can't find RNA 'E'", nodeKernel);
        QueryEngine engine = new StandardQueryEngine();
        SearchElement[] nodeInfosArray = engine.searchLinked(new SearchElement[] {new SearchElement(nodeKernel)}, new QueryOptions(1,
                BioHub.DIRECTION_DOWN), new TargetOptions(module.getCompletePath()), null);
        assertNotNull("'searchLink' returns null", nodeInfosArray);
        Map<String, SearchElement> nodes = toMap(nodeInfosArray);
        assertEquals("Wrong size.", 2, nodes.size());
        checkEdges(diagram.getNodes());

        assertTrue("Node 'D' not found.", nodes.containsKey("D"));
        assertTrue("Node 'R000001' not found.", nodes.containsKey("R000001"));
    }

    public void testSearchLinkedDeepBoth() throws Exception
    {
        Diagram diagram = new Diagram(module.getDiagrams(), new Stub(null, "test"), new PathwayDiagramType());
        Base nodeKernel = module.getKernel( Substance.class, "B" );
        assertNotNull("Can't find substance 'B'", nodeKernel);
        QueryEngine engine = new StandardQueryEngine();
        SearchElement[] nodeInfosArray = engine.searchLinked(new SearchElement[] {new SearchElement(nodeKernel)}, new QueryOptions(2,
                BioHub.DIRECTION_BOTH), new TargetOptions(module.getCompletePath()), null);
        assertNotNull("'searchLink' returns null", nodeInfosArray);
        Map<String, SearchElement> nodes = toMap(nodeInfosArray);
        assertEquals("Wrong size.", 5, nodes.size());
        checkEdges(diagram.getNodes());

        assertTrue("Node 'E' not found.", nodes.containsKey("E"));
        assertTrue("Node 'B' not found.", nodes.containsKey("B"));
        assertTrue("Node 'D' not found.", nodes.containsKey("D"));
        assertTrue("Node 'G' not found.", nodes.containsKey("G"));
        assertTrue("Node 'R000001' not found.", nodes.containsKey("R000001"));
        
        checkEdges(diagram.getNodes());
    }

    protected void checkEdges(Node[] nodes) throws Exception
    {
        for( Node node : nodes )
        {
            for( Edge edge : node.edges() )
            {
                assertNotNull("input in Egde '" + edge.getName() + "' of node '" + node.getName() + "' can't be null", edge.getInput());
                assertNotNull("output in Egde '" + edge.getName() + "' of node '" + node.getName() + "' can't be null", edge.getOutput());
            }
            if( node instanceof Compartment )
                checkEdges( ( (Compartment)node ).getNodes());
        }
    }

    protected Map<String, SearchElement> toMap(SearchElement[] nodesArray)
    {
        return StreamEx.of( nodesArray ).toMap( SearchElement::getBaseName, Function.identity() );
    }

    protected int countEdges(Node node)
    {
        return (int)node.edges().count();
    }

    private DataCollection<?> repository = null;
    private Module module = null;
}