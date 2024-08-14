package biouml.plugins.simulation_test._test;

import biouml.model.Diagram;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineRegistry;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;

public class TestVirtualHuman extends AbstractBioUMLTest
{
    public TestVirtualHuman(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestVirtualHuman.class.getName() );
        suite.addTest( new TestVirtualHuman( "testSimple" ) );
        return suite;
    }
    
    public void testSimple() throws Exception
    { 
        for (String diagramName: diagramsToTest)
        {
            testSimpleDiagram(diagramName);
        }
    }
    
    public void testComposite()
    {
        
    }

    public void testSimpleDiagram(String diagramName) throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        Diagram diagram = (Diagram)CollectionFactory.getDataElement(collectionPath + diagramName);
        assertNotNull("Diagram " + diagramName + " can not be found ", diagram);
        System.out.println("Diagram " + diagram.getName() + " loaded successfully");
        SimulationEngine engine = SimulationEngineRegistry.getSimulationEngine(diagram);
        System.out.println("Engine loaded: "+engine.getClass().toString());
        engine.setDiagram(diagram);
        engine.setTimeIncrement(0.1);
        engine.setCompletionTime(10);
        Model model = engine.createModel();
        assertNotNull(model);
        engine.simulate(model);
        System.out.println(diagram.getName() + " tested");
    }

    private String repositoryPath = "../data";
    private String collectionPath = "databases/Virtual Human/";
    private String[] diagramsToTest = new String[] {"Solodyannikov 1994/Solodyannikov 1994", "Solodyannikov 2006/Solodyannkiov 2006",
            "Karaaslan 2005/Karaaslan 2005", "Complex model/Complex model 2012"};
}
