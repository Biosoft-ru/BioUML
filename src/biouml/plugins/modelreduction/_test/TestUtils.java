package biouml.plugins.modelreduction._test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Substance;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

public class TestUtils
{
    protected static Diagram createTestDiagram_1() throws Exception
    {
        Diagram diagram = new Diagram(null, new Stub(null, "d1"), new PathwaySimulationDiagramType());

        EModel model = new EModel(diagram);
        diagram.setRole(model);

        //------------------------------ Create variables ------------------------------
        createVariable(diagram, "e", 1.0);
        createVariable(diagram, "s", 100.0);
        createVariable(diagram, "c1", 0.0);
        createVariable(diagram, "c2", 0.0);
        createVariable(diagram, "p", 0.0);

        //------------------------------ Create reactions ------------------------------
        List<SpecieReference> speciesReferences;

        //e + s -> c1
        speciesReferences = new ArrayList<>();
        speciesReferences.add(createSpeciesReference("e", SpecieReference.REACTANT));
        speciesReferences.add(createSpeciesReference("s", SpecieReference.REACTANT));
        speciesReferences.add(createSpeciesReference("c1", SpecieReference.PRODUCT));
        createReaction(diagram, speciesReferences, "k1 * $e * $s");

        //c1 -> e + s
        speciesReferences = new ArrayList<>();
        speciesReferences.add(createSpeciesReference("c1", SpecieReference.REACTANT));
        speciesReferences.add(createSpeciesReference("e", SpecieReference.PRODUCT));
        speciesReferences.add(createSpeciesReference("s", SpecieReference.PRODUCT));
        createReaction(diagram, speciesReferences, "k2 * $c1");

        //c1 -> c2
        speciesReferences = new ArrayList<>();
        speciesReferences.add(createSpeciesReference("c1", SpecieReference.REACTANT));
        speciesReferences.add(createSpeciesReference("c2", SpecieReference.PRODUCT));
        createReaction(diagram, speciesReferences, "k3 * $c1");

        //c2 -> c1
        speciesReferences = new ArrayList<>();
        speciesReferences.add(createSpeciesReference("c2", SpecieReference.REACTANT));
        speciesReferences.add(createSpeciesReference("c1", SpecieReference.PRODUCT));
        createReaction(diagram, speciesReferences, "k4 * $c2");

        //c2 -> p + e
        speciesReferences = new ArrayList<>();
        speciesReferences.add(createSpeciesReference("c2", SpecieReference.REACTANT));
        speciesReferences.add(createSpeciesReference("p", SpecieReference.PRODUCT));
        speciesReferences.add(createSpeciesReference("e", SpecieReference.PRODUCT));
        createReaction(diagram, speciesReferences, "k5 * $c2");

        //------------------------------ Set initial values ------------------------------
        model.getVariable("k1").setInitialValue(500000.0);
        model.getVariable("k2").setInitialValue(5.0);
        model.getVariable("k3").setInitialValue(1000.0);
        model.getVariable("k4").setInitialValue(100.0);
        model.getVariable("k5").setInitialValue(0.16);

        return diagram;
    }

    protected static Diagram createTestDiagram_2() throws Exception
    {
        Diagram diagram = new Diagram(null, new Stub(null, "d2"), new PathwaySimulationDiagramType());

        EModel model = new EModel(diagram);
        diagram.setRole(model);

        //------------------------------ Create variables ------------------------------
        TestUtils.createVariable(diagram, "A", 0.0);
        TestUtils.createVariable(diagram, "B", 0.0);
        TestUtils.createVariable(diagram, "C", 0.0);
        TestUtils.createVariable(diagram, "D", 0.0);

        //------------------------------ Create reactions ------------------------------
        List<SpecieReference> speciesReferences;

        // -> A
        speciesReferences = new ArrayList<>();
        speciesReferences.add(TestUtils.createSpeciesReference("A", SpecieReference.PRODUCT));
        TestUtils.createReaction(diagram, speciesReferences, "k0");

        // A -> B
        speciesReferences = new ArrayList<>();
        speciesReferences.add(TestUtils.createSpeciesReference("A", SpecieReference.REACTANT));
        speciesReferences.add(TestUtils.createSpeciesReference("B", SpecieReference.PRODUCT));
        TestUtils.createReaction(diagram, speciesReferences, "k1 * $A");

        // B <--> C
        speciesReferences = new ArrayList<>();
        speciesReferences.add(TestUtils.createSpeciesReference("B", SpecieReference.REACTANT));
        speciesReferences.add(TestUtils.createSpeciesReference("C", SpecieReference.PRODUCT));
        TestUtils.createReaction(diagram, speciesReferences, "k2 * $B - k_2 * $C");

        // B <--> D
        speciesReferences = new ArrayList<>();
        speciesReferences.add(TestUtils.createSpeciesReference("B", SpecieReference.REACTANT));
        speciesReferences.add(TestUtils.createSpeciesReference("D", SpecieReference.PRODUCT));
        TestUtils.createReaction(diagram, speciesReferences, "k3 * $B - k_3 * $D");

        // C ->
        speciesReferences = new ArrayList<>();
        speciesReferences.add(TestUtils.createSpeciesReference("C", SpecieReference.REACTANT));
        TestUtils.createReaction(diagram, speciesReferences, "k4 * $C");

        // D ->
        speciesReferences = new ArrayList<>();
        speciesReferences.add(TestUtils.createSpeciesReference("D", SpecieReference.REACTANT));
        TestUtils.createReaction(diagram, speciesReferences, "k5 * $D");

        //------------------------------ Set initial values ------------------------------
        model.getVariable("k0").setInitialValue(0.1);
        model.getVariable("k1").setInitialValue(0.1);
        model.getVariable("k2").setInitialValue(0.05);
        model.getVariable("k_2").setInitialValue(0.05);
        model.getVariable("k3").setInitialValue(0.05);
        model.getVariable("k_3").setInitialValue(0.05);
        model.getVariable("k4").setInitialValue(0.05);
        model.getVariable("k5").setInitialValue(0.05);

        return diagram;
    }

    protected static Diagram createTestDiagram_3() throws Exception
    {
        Diagram diagram = new Diagram(null, new Stub(null, "d3"), new PathwaySimulationDiagramType());

        EModel model = new EModel(diagram);
        diagram.setRole(model);

        //------------------------------ Create variables ------------------------------
        TestUtils.createVariable(diagram, "A", 5.0);
        TestUtils.createVariable(diagram, "B", 13.0);
        TestUtils.createVariable(diagram, "C", 2.0);

        //------------------------------ Create reactions ------------------------------
        List<SpecieReference> speciesReferences;

        // A + B -> C
        speciesReferences = new ArrayList<>();
        speciesReferences.add(TestUtils.createSpeciesReference("A", SpecieReference.REACTANT));
        speciesReferences.add(TestUtils.createSpeciesReference("B", SpecieReference.REACTANT));
        speciesReferences.add(TestUtils.createSpeciesReference("C", SpecieReference.PRODUCT));
        TestUtils.createReaction(diagram, speciesReferences, "k0 * $A * $B");

        //------------------------------ Set initial values ------------------------------
        model.getVariable("k0").setInitialValue(0.05);

        return diagram;
    }

    protected static void createVariable(Diagram diagram, String name, double value) throws Exception
    {
        Node node = new Node(diagram, new Substance(null, name));
        node.setRole(new VariableRole(null, node, value));
        diagram.put(node);
    }

    protected static void createReaction(Diagram diagram, List<SpecieReference> components, String formula) throws Exception
    {
        DiagramUtility.createReactionNode(diagram, diagram, null, components, formula, new Point(0, 0), "reaction");
    }

    protected static SpecieReference createSpeciesReference(String variable, String role)
    {
        SpecieReference specieReference = new SpecieReference(null, variable, role);
        specieReference.setSpecie(variable);
        return specieReference;
    }

    public static boolean compare(double[][] m1, double[][] m2, double accuracy)
    {
        if( m1 == null || m2 == null )
            return false;

        if( m1.length != m2.length || m1[0].length != m2[0].length )
            return false;

        for( int i = 0; i < m1.length; ++i )
        {
            for( int j = 0; j < m1[0].length; ++j )
            {
                if( Math.abs(m1[i][j] - m2[i][j]) > accuracy )
                    return false;
            }
        }
        return true;
    }
}
