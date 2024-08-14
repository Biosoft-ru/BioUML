package biouml.plugins.simulation.java._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Diagram;
import biouml.model.dynamics.Equation;
import biouml.plugins.simulation.ConstraintPreprocessor;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.SimulationResult;

public class TestConstraintsModel extends AbstractBioUMLTest
{
    public TestConstraintsModel(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestConstraintsModel.class.getName());
        suite.addTest(new TestConstraintsModel("test"));
        return suite;
    }


    public void test() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("diagram_event");
        generator.createSpecies("x", 0);
        generator.createEquation("$x", "1", Equation.TYPE_RATE_BY_RULE);
        generator.createConstraint("constraint", "$x<5", "X exceeded 10");
        Diagram diagram = generator.getDiagram();

        test(diagram, JavaSimulationEngine.TEMPLATE_LARGE_ONLY, ConstraintPreprocessor.CONSTRAINTS_STOP,
                new double[] {0.0, 1.0, 2.0, 3.0, 4.0});
        test(diagram, JavaSimulationEngine.TEMPLATE_NORMAL_ONLY, ConstraintPreprocessor.CONSTRAINTS_IGNORE,
                new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0});
    }

    public void test(Diagram diagram, String template, String constraintsHandling, double[] expectedResult) throws Exception
    {
        SimulationResult sr1 = new SimulationResult(null, "tmp1");
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setCompletionTime(6);
        engine.setDiagram(diagram);
        engine.setConstraintsViolation(constraintsHandling);
        engine.setTemplateType(template);
        engine.simulate(sr1);
        double[] results = sr1.getValues(new String[] {"$x"})[0];
        assertArrayEquals("Results differs at first step", expectedResult, results, 0);
    }
}
