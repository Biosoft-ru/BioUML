package biouml.model.dynamics._test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.LogManager;

import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model._test.ViewTestCase;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;

/** Batch unit test for biouml.model package. */
public class SimpleEventTest extends ViewTestCase
{
    /** Standart JUnit constructor */
    public SimpleEventTest(String name)
    {
        super(name);
        File configFile = new File( "./biouml/model/dynamics/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(SimpleEventTest.class.getName());

        suite.addTest(new SimpleEventTest("testLoadModule"));
        suite.addTest(new SimpleEventTest("testCreateDiagram") );
        suite.addTest(new SimpleEventTest("testDiagramView") );
        suite.addTest(new SimpleEventTest("testWriteDiagram") );
        suite.addTest(new SimpleEventTest("testReadDiagram") );
        suite.addTest(new SimpleEventTest("testWriteAgainDiagram") );

        return suite;
    }

    static Module module;
    public void testLoadModule() throws Exception
    {
        DataCollection repository = CollectionFactory.createRepository("../data");
        module = (Module)repository.get("test");
        assertNotNull("Can not load module", module);
    }

    protected void addState(String name, String var, String math, int x, int y) throws Exception
    {
        Node node = new Node(diagram, new Stub(null, name, Type.MATH_STATE));
        State state = new State(node);
        state.addOnEntryAssignment(new Assignment(var, math), false);
        node.setRole(state);
        node.setLocation(x, y);
        diagram.put(node);
    }

    protected void addTransition(String from, String to, String when, String after) throws Exception
    {
        Edge edge = new Edge( diagram, new Stub(null, from + " -> " + to, Type.MATH_TRANSITION),
                              (Node)diagram.get(from), (Node)diagram.get(to));
        Transition transition = new Transition(edge);
        if( when != null )
            transition.setWhen(when);
        else
            transition.setAfter(after);

        edge.setRole(transition);
        diagram.put(edge);
    }

    static Diagram diagram;
    public void testCreateDiagram() throws Exception
    {
        diagram = new Diagram(null, new Stub(null, "diagram"),
                              new PathwaySimulationDiagramType());

        EModel model = new EModel(diagram);

        // create stubs
        Node n1 = new Node(diagram, new Stub(null, "s1", Type.TYPE_SUBSTANCE));
        n1.setRole(new VariableRole(null, n1, 1));
        n1.setLocation(10, 10);
        diagram.put(n1);

        Node n2 = new Node(diagram, new Stub(null, "s2", Type.TYPE_SUBSTANCE));
        n2.setRole(new VariableRole(null, n2, 2));
        n2.setLocation(10, 100);
        diagram.put(n2);

        // create events
        Node ne1 = new Node(diagram, new Stub(null, "e1"));
        Event e1 = new Event(ne1);
        e1.clearAssignments(false);
        e1.setTrigger("$s1 > $s2");
        e1.setDelay("10");
        e1.addEventAssignment( new Assignment("$s1", "a+b/c"), false);
        e1.addEventAssignment( new Assignment("$s2", "a+b/c"), false);
        ne1.setRole(e1);
        ne1.setLocation(150, 10);
        diagram.put(ne1);

        addState("G1", "k_growth", "0.01", 400, 200);
        addState("S",  "k_growth", "0.005", 200, 400);
        addState("G2", "k_growth", "0.005",   0, 200);
        addState("M",  "k_growth", "0",   200, 0);

        State m = ((Node)diagram.get("M")).getRole(State.class);
        m.addOnExitAssignment(new Assignment("$cytoplasm", "$cytoplasm/2"), false);

        // create transitions
        addTransition("G1", "S",  "$cytoplasm >= 1.8", null);
        addTransition("S",  "G2", null, "120");
        addTransition("G2", "M",  null,  "60");
        addTransition("M",  "G1", null, "120");
        model.setInitialState((State)((Node)diagram.get("G1")).getRole());

        diagram.setRole(model);
    }

    public void testDiagramView()
    {

        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView( (CompositeView)diagram.getView() );
        assertView(pane, diagram.getName());
    }

    public void testWriteDiagram() throws Exception
    {
        File fileResult = new File("../data/test/Diagrams/test_event_1.result");
        DiagramXmlWriter writer = new DiagramXmlWriter(new FileOutputStream(fileResult));
        writer.write(diagram);
    }

    public void testReadDiagram() throws Exception
    {
        File fileOrig = new File("../data/test/Diagrams/test_event_1.result");
        diagram = DiagramXmlReader.readDiagram(fileOrig.getName(), new FileInputStream(fileOrig), null, module.getDiagrams(), module);
    }

    public void testWriteAgainDiagram() throws Exception
    {
        File fileResult = new File("../data/test/Diagrams/test_event_2.result");
        DiagramXmlWriter writer = new DiagramXmlWriter(new FileOutputStream(fileResult));
        writer.write(diagram);
    }



}
