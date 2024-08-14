package biouml.plugins.simulation.java._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class TestFastReaction extends AbstractBioUMLTest
{
    public TestFastReaction(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestFastReaction.class.getName() );
        suite.addTest( new TestFastReaction( "test" ) );
        return suite;
    }

    public static void test() throws Exception
    {
//        testSimpleModel("Euler", JavaSimulationEngine.TEMPLATE_LARGE_ONLY);
        testSimpleModel("Radau5", JavaSimulationEngine.TEMPLATE_LARGE_ONLY);
        testSimpleModel("JVode", JavaSimulationEngine.TEMPLATE_LARGE_ONLY);
        testSimpleModel("DormandPrince", JavaSimulationEngine.TEMPLATE_LARGE_ONLY);

//        testSimpleModel("Euler", JavaSimulationEngine.TEMPLATE_NORMAL_ONLY);
        testSimpleModel("Radau5", JavaSimulationEngine.TEMPLATE_NORMAL_ONLY);
        testSimpleModel("JVode", JavaSimulationEngine.TEMPLATE_NORMAL_ONLY);
        testSimpleModel("DormandPrince", JavaSimulationEngine.TEMPLATE_NORMAL_ONLY);
    }


    public static void testSimpleModel(String solverName, String templateType) throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator( "diagram" );

        Node a = generator.createSpecies( "A", 500 );
        Node b = generator.createSpecies( "B", 0 );

        SpecieReference refA = generator.createSpeciesReference( a, SpecieReference.REACTANT );
        SpecieReference refB1 = generator.createSpeciesReference( b, SpecieReference.PRODUCT );

        Node reaction = (Node)generator.createReaction( "1E-4*$A", refA, refB1 ).getElement( Util::isReaction );
        ( (Reaction)reaction.getKernel() ).setFast( true );

        Diagram diagram = generator.getDiagram();

        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setSolverName(solverName);
        engine.setDiagram( diagram );
        engine.setTemplateType(templateType);
        Model model = engine.createModel();
        SimulationResult result = new SimulationResult( null, "result" );
        engine.simulate( model, result );

        double[][] results = result.getValues(new String[] {"$A", "$B"});

        System.out.println("Testing: "+engine.getSolverName());
        assertEquals(results[0][0], 0.0, 1E-9);
        assertEquals(results[1][0], 500.0, 1E-9);
        System.out.println("A: "+ DoubleStreamEx.of(results[0]).joining("\t"));
    }
}
