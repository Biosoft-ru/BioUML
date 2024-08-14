package biouml.model._test;

import junit.framework.TestCase;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Stub;

/**
 * Variable name MODE testing.
 * Modes:
 *  EMaodel.VARIABLE_NAME_BY_ID         - testIdMode;
 *  EMaodel.VARIABLE_NAME_BY_TITLE      - testTitleMode;
 *  EMaodel.VARIABLE_NAME_BY_TITLE_BRIEF- testBriafTitleMode ;
 */
public class TestVariableNameMode extends TestCase
{
    public TestVariableNameMode(String name)
    {
        super(name);
    }

    private Diagram makeTestDiagram() throws Exception
    {
        PathwaySimulationDiagramType diagramType = new PathwaySimulationDiagramType();
        Diagram diagram = diagramType.createDiagram(null, "test diagram", null);

        Node a = new Node(diagram, new Stub(null, "a"));
        a.setTitle("Node1");
        a.setRole(new VariableRole(null, a, 10.0));
        diagram.put(a);

        Compartment comp1 = new Compartment(diagram, new Stub(null, "comp1"));
        comp1.setTitle("First_compartment");
        comp1.setRole(new VariableRole(null, (Node)comp1, 1.0));
        Node b1 = new Node(comp1, new Stub(null, "b"));
        b1.setTitle("Node2");
        b1.setRole(new VariableRole(null, b1, 5.0));
        comp1.put(b1);
        diagram.put(comp1);

        Compartment comp2 = new Compartment(diagram, new Stub(null, "comp2"));
        comp2.setTitle("Second compartment");
        comp2.setRole(new VariableRole(null, (Node)comp2, 1.0));
        Node b2 = new Node(comp2, new Stub(null, "c"));
        b2.setTitle("Node2");
        b2.setRole(new VariableRole(null, b2, 7.0));
        comp2.put(b2);
        diagram.put(comp2);

        return diagram;
    }

    public void testIdMode() throws Exception
    {
        Diagram diagram = makeTestDiagram();
        assertNotNull(diagram);
        EModel model = diagram.getRole(EModel.class);

        String vName = model.getQualifiedName("$a", diagram.get("a"), EModel.VARIABLE_NAME_BY_ID);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'a' name", "$a", vName);
        vName = model.getQualifiedName("$comp1.b", ( (Compartment)diagram.get("comp1") ).get("b"),
                EModel.VARIABLE_NAME_BY_ID);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'b' name", "$comp1.b", vName);
        vName = model.getQualifiedName("$comp2.c", ( (Compartment)diagram.get("comp2") ).get("c"),
                EModel.VARIABLE_NAME_BY_ID);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'c' name", "$comp2.c", vName);
    }

    public void testTitleMode() throws Exception
    {
        Diagram diagram = makeTestDiagram();
        assertNotNull(diagram);
        EModel model = diagram.getRole(EModel.class);

        String vName = model.getQualifiedName("$a", diagram.get("a"), EModel.VARIABLE_NAME_BY_TITLE);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'a' name", "$Node1", vName);
        vName = model.getQualifiedName("$comp1.b", ( (Compartment)diagram.get("comp1") ).get("b"),
                EModel.VARIABLE_NAME_BY_TITLE);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'b' name", "$\"First_compartment.Node2\"", vName);
        vName = model.getQualifiedName("$comp2.c", ( (Compartment)diagram.get("comp2") ).get("c"),
                EModel.VARIABLE_NAME_BY_TITLE);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'c' name", "$\"Second compartment.Node2\"", vName);
    }

    public void testBriafTitleMode() throws Exception
    {
        Diagram diagram = makeTestDiagram();
        assertNotNull(diagram);
        EModel model = diagram.getRole(EModel.class);

        String vName = model.getQualifiedName("$a", diagram.get("a"), EModel.VARIABLE_NAME_BY_TITLE_BRIEF);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'a' name", "$Node1", vName);
        vName = model.getQualifiedName("$comp1.b", ( (Compartment)diagram.get("comp1") ).get("b"),
                EModel.VARIABLE_NAME_BY_TITLE_BRIEF);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'b' name", "$Node2", vName);
        vName = model.getQualifiedName("$comp2.c", ( (Compartment)diagram.get("comp2") ).get("c"),
                EModel.VARIABLE_NAME_BY_TITLE_BRIEF);
        assertNotNull("Cannot get qualified name", vName);
        assertEquals("Incorrect 'c' name", "$Node2", vName);
    }
}