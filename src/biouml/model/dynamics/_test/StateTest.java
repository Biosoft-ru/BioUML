package biouml.model.dynamics._test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.LogManager;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model._test.ViewTestCase;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.simulation.ResultListener;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.jobcontrol.FunctionJobControl;

/** Batch unit test for biouml.model package. */
public class StateTest extends ViewTestCase
{
    public StateTest(String name)
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
        TestSuite suite = new TestSuite(StateTest.class.getName());

        suite.addTest(new StateTest("testCreateDiagram"));
        //        suite.addTest(new StateTest("testWriteDiagram") );
        suite.addTest(new StateTest("testDiagramView"));
        //        suite.addTest(new StateTest("testMatlabSimulate") );
        suite.addTest(new StateTest("testJavaSimulate"));

        return suite;
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
        Edge edge = new Edge( diagram, new Stub( null, from + " -> " + to, Type.MATH_TRANSITION ), (Node)diagram.get( from ),
                (Node)diagram.get( to ) );
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
        diagram = new Diagram(null, new Stub(null, "diagram_cytoplasm_division"), new PathwaySimulationDiagramType());

        EModel model = new EModel(diagram);
        diagram.setRole(model);

        // create cytoplasm and rule for its growth
        Compartment c = new Compartment(diagram, new Stub(null, "cytoplasm", Type.TYPE_COMPARTMENT));
        c.setRole(new VariableRole(c, 1));
        c.setLocation(0, -100);
        c.setShapeSize(new java.awt.Dimension(200, 100));
        c.setShapeType(Compartment.SHAPE_ELLIPSE);
        diagram.put(c);

        Node rule = new Node(null, new Stub(null, "growth rule", Type.MATH_EQUATION));
        Equation eq = new Equation(rule, Equation.TYPE_RATE, "$cytoplasm", "k_growth");
        rule.setRole(eq);
        diagram.put(rule);
        rule.setLocation(250, -100);

        // create states
        addState("G1", "k_growth", "0.01", 400, 200);
        addState("S", "k_growth", "0.005", 200, 400);
        addState("G2", "k_growth", "0.005", 0, 200);
        addState("M", "k_growth", "0", 200, 0);

        State m = ( (Node)diagram.get("M") ).getRole(State.class);
        m.addOnExitAssignment(new Assignment("$cytoplasm", "$cytoplasm/2"), false);

        // create transitions
        addTransition("G1", "S", "$cytoplasm >= 1.8", null);
        addTransition("S", "G2", null, "120");
        addTransition("G2", "M", null, "60");
        addTransition("M", "G1", null, "120");
        model.setInitialState((State) ( (Node)diagram.get("G1") ).getRole());
    }

    Module module;
    public void testWriteDiagram() throws Exception
    {
        assertNotNull("Diagram is not initialized", diagram);

        if( module == null )
        {
            DataCollection<?> repository = CollectionFactory.createRepository( "../data" );
            module = (Module)repository.get("test");
            assertNotNull("Can not load module", module);
        }

        File fileResult = new File("../data/test/Diagrams/test_state");
        DiagramXmlWriter writer = new DiagramXmlWriter(new FileOutputStream(fileResult));
        writer.write(diagram);
    }

    public void testDiagramView()
    {
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView((CompositeView)diagram.getView());
        assertView(pane, diagram.getName());
    }

    public void testJavaSimulate() throws Exception
    {
        JavaSimulationEngine java = new JavaSimulationEngine();
        java.setOutputDir("./out");
        java.setDiagram(diagram);
        java.setInitialTime(0);
        java.setCompletionTime(1500);
        java.setTimeIncrement(1);
        java.setJobControl(new FunctionJobControl(null));
        File[] files = java.generateModel(true);
        String s = java.simulate(files, (ResultListener[])null);
        assertNull(s);
    }
}
