package biouml.plugins.agentmodeling._test;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.standard.simulation.SimulationResult;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class TestCVS extends AbstractBioUMLTest
{

    public TestCVS(String name)
    {
        super( name );
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( TestCVS.class.getName() );
        suite.addTest( new TestCVS( "test" ) );
        return suite;
    }

    String resultDest = "data/Collaboration/Cardiovascular/Data/";
    public void test() throws Exception
    {
        Diagram diagram = AgentTestingUtils.loadDiagram( "Complex CVS Model with Kidney", "databases/Cardiovascular student/Diagrams/" );
        
        assert(diagram != null);
        CollectionFactory.unregisterAllRoot();
        String repositoryPath = "../data_resources";
        CollectionFactory.createRepository(repositoryPath);
        DataCollection collection = AgentTestingUtils.loadCollection( resultDest );
        assert(collection != null);
        AgentModelSimulationEngine engine = new AgentModelSimulationEngine();
        engine.setDiagram( diagram );
        engine.setTimeIncrement( 100 );
        SimulationResult result = new SimulationResult(collection, "result_simple");
        
        double time = System.currentTimeMillis();
        engine.simulate( result );
        System.out.println("Simulation time: "+ (System.currentTimeMillis() - time));
        engine.initSimulationResult( result );
        
        collection.put(result);
        System.out.println("Total time: "+ (System.currentTimeMillis() - time));
        
       
    }
}