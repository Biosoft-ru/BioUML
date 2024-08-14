package biouml.plugins.agentmodeling._test;

import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentPopulation;

public class TestMortalAgents extends AbstractBioUMLTest
{
        final static String COLLECTION_NAME = "data/Collaboration (git)/Myeloma/Data/Agent model/";
        final static String DIAGRAM_NAME = "MM composite with POM";
    public final static int POPULATION_SIZE = 100;
        public final static double TIME_INCREMENT = 1;
        public final static double COMPLETION_TIME = 43825;

    public TestMortalAgents(String name)
    {
        super(name);
    }

    public TestMortalAgents()
    {
        super("test");
    }

    
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestMortalAgents.class.getName());
        suite.addTest(new TestMortalAgents("test"));
        return suite;
    }


    public void test() throws Exception
    {
        Diagram diagram = AgentTestingUtils.loadDiagram(DIAGRAM_NAME, COLLECTION_NAME);
        AgentPopulation population = new AgentPopulation(diagram, POPULATION_SIZE);
        population.setCompletionTime(COMPLETION_TIME);
        population.setTimeIncrement(TIME_INCREMENT);
        population.generatePopulation();
        population.simulate();
        double[] times = population.getTimes();
        double[] size = population.getSizeDynamic();
    }
}
