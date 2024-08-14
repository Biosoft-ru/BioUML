package biouml.plugins.simulation.java._test;

import biouml.model.Diagram;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

/**
 *
 */
public class TestLargeModel extends TestCase
{
    public TestLargeModel(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestLargeModel.class.getName());
        suite.addTest(new TestLargeModel("testLargeModel"));
        return suite;
    }

    public void testLargeModel() throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataCollection<?> collection = CollectionFactory.getDataCollection(collectionPath);
        assertNotNull(collection);
        System.out.println(collection.getSize());

        Diagram diagram = (Diagram)collection.get(diagramName);
        assertNotNull(diagram);

        System.out.println("Diagram was read successfully");
        SimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram(diagram);

        Model model = engine.createModel();
        assertNotNull(model);
    }

    private String repositoryPath = "../data";
    private String collectionPath = "databases/Glycan_structures/Diagrams/";
    private String diagramName = "glycobiology_Fred";

}
