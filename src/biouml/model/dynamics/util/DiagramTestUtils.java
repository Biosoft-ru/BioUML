package biouml.model.dynamics.util;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Stub;

public class DiagramTestUtils
{
    public static Diagram createTestDiagramWithThreeCompartments() throws Exception
    {
        Diagram diagram = new Diagram(null, new Stub(null, "test_diagram"), null);
        diagram.setRole(new EModel(diagram));

        Compartment c_big = new Compartment(diagram, "c_big", new Stub(null, "cc_big"));
        Compartment c1 = new Compartment(c_big, "c1", new Stub(null, "cc1"));
        Compartment c2 = new Compartment(c_big, "c2", new Stub(null, "cc2"));

        diagram.put(c_big);
        c_big.put(c1);
        c_big.put(c2);

        Node xSubstanse = new Node(c1, new Stub(null, "x"));
        Node ySubstanse = new Node(c1, new Stub(null, "y"));
        Node uSubstanse = new Node(c2, new Stub(null, "u"));
        Node vSubstanse = new Node(c2, new Stub(null, "v"));

        VariableRole xVar = new VariableRole(c1, xSubstanse, 1);
        VariableRole yVar = new VariableRole(c1, ySubstanse, 2);
        VariableRole uVar = new VariableRole(c2, uSubstanse, 3);
        VariableRole vVar = new VariableRole(c2, vSubstanse, 4);

        xSubstanse.setRole(xVar);
        ySubstanse.setRole(yVar);
        uSubstanse.setRole(uVar);
        vSubstanse.setRole(vVar);

        c1.put(xSubstanse);
        c1.put(ySubstanse);
        c2.put(uSubstanse);
        c2.put(vSubstanse);

        // create equations in blocks
        // first block
        Equation equation11 = new Equation(c1, Equation.TYPE_RATE, "$x");
        Equation equation12 = new Equation(c1, Equation.TYPE_RATE, "$y");

        equation11.setFormula("$x + $y");
        equation12.setFormula("$x - $y");

        Node node11 = new Node(c1, new Stub(null, "node11"));
        Node node12 = new Node(c1, new Stub(null, "node12"));
        node11.setRole(equation11);
        node12.setRole(equation12);

        c1.put(node11);
        c1.put(node12);

        // second block
        Equation equation21 = new Equation(c2, Equation.TYPE_RATE, "$u");
        Equation equation22 = new Equation(c2, Equation.TYPE_RATE, "$v");

        equation21.setFormula("$v + 1");
        equation22.setFormula("2*$u");

        Node node21 = new Node(c2, new Stub(null, "node21"));
        Node node22 = new Node(c2, new Stub(null, "node22"));
        node21.setRole(equation11);
        node22.setRole(equation12);

        c2.put(node21);
        c2.put(node22);
        return diagram;
    }

    public static void addConnection(Connection connection, @Nonnull Compartment compartment1, @Nonnull Compartment compartment2, DataCollection origin)
            throws Exception
    {
        Edge edge = new Edge(origin, new Stub(null, "connection"), compartment1, compartment2);
        edge.setRole(connection);
        origin.put(edge);
    }

}
