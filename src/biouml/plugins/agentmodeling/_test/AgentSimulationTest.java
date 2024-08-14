package biouml.plugins.agentmodeling._test;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class AgentSimulationTest extends AbstractBioUMLTest
{
    public AgentSimulationTest(String name)
    {
        super( name );
    }
    
    public static final String REPOSITORY_PATH = "../data";

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( AgentSimulationTest.class.getName() );
        suite.addTest( new AgentSimulationTest( "test" ) );
        return suite;
    }
    public void test() throws Exception
    {
        AgentModelSimulationEngine simulationEngine = new AgentModelSimulationEngine();
        
        simulationEngine.setDiagram( getDiagram() );
        simulationEngine.simulate();
    }
    
    public Diagram getDiagram() throws Exception
    {
        DataCollection repository = CollectionFactory.createRepository(REPOSITORY_PATH);
        assertNotNull("Wrong repository", repository);
        Diagram diagram = (Diagram)CollectionFactory.getDataCollection("databases/agentmodel_test/Diagrams/AgentModel");
        assertNotNull("Diagram not loaded", diagram);
        return diagram;
    }
}
