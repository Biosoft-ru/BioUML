package biouml.plugins.agentmodeling._test;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class ScriptAgentTest extends TestCase
{
    public ScriptAgentTest(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ScriptAgentTest.class.getName());
        suite.addTest(new ScriptAgentTest("test"));
        return suite;
    }
    
    public void test() throws Exception
    {
        CollectionFactory.createRepository( repositoryPath );
        DataCollection collection = CollectionFactory.getDataCollection( collectionPath );
        assertNotNull( collection );
        System.out.println( collection.getSize() );

        Diagram diagram = (Diagram)collection.get( diagramName );
        assertNotNull( diagram );

        System.out.println( "Diagram was read successfully" );
        AgentModelSimulationEngine engine = new AgentModelSimulationEngine();
        engine.setDiagram( diagram );
        engine.setTimeIncrement( 100 );
//        engine.writeUtilityFiles( true );
        
//        engine.generateModel( true );
        AgentBasedModel model = engine.createModel();

        assertNotNull( model );
        assert(model.getAgents().size() == 2);
        engine.simulate( model );
    }


    private String repositoryPath = "../data";
    private String collectionPath = "databases/Virtual Human/Diagrams/";
    private String diagramName = "AA";
}
