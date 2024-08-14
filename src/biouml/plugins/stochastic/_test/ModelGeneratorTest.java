package biouml.plugins.stochastic._test;

import biouml.model.Diagram;
import biouml.plugins.simulation.Model;
import biouml.plugins.stochastic.StochasticSimulationEngine;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class ModelGeneratorTest extends TestCase
{
    static String repositoryPath = "../data";

    public ModelGeneratorTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ModelGeneratorTest.class.getName());

        suite.addTest(new ModelGeneratorTest("testGenerator"));

        return suite;
    }

    public void testGenerator() throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        Diagram diagram = DataElementPath.create("databases/my/Diagrams/third").getDataElement(Diagram.class);
        StochasticSimulationEngine engine = new StochasticSimulationEngine();
        engine.setDiagram(diagram);
        engine.setOutputDir("../out");
        Model model = engine.createModel();

        assertNotNull("Can not load model class", model);

        model.init();
    }
}
