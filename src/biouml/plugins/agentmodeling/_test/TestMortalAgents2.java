package biouml.plugins.agentmodeling._test;

import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.agentmodeling.AgentPopulation2;
import biouml.plugins.agentmodeling.Classification;

public class TestMortalAgents2 extends AbstractBioUMLTest
{
    final static String COLLECTION_NAME = "data/Collaboration (git)/Myeloma/Data/Agent model/";
    final static String DIAGRAM_NAME = "MM composite with POM";
    public final static int POPULATION_SIZE = 100;
    public final static double TIME_INCREMENT = 100;
    public final static double COMPLETION_TIME = 24*365*5;

    public TestMortalAgents2(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestMortalAgents.class.getName());
        suite.addTest(new TestMortalAgents2("test"));
        return suite;
    }

    public void test() throws Exception
    {
        double start = System.currentTimeMillis();
        Diagram diagram = AgentTestingUtils.loadDiagram(DIAGRAM_NAME, COLLECTION_NAME);
        diagram.getRole(EModel.class).getVariable("applyTreatment").setInitialValue(0);
        AgentPopulation2 population = new AgentPopulation2(diagram, POPULATION_SIZE);
        population.generatePopulation();
        Classification classification = new Classification("Status");
        classification.setTitle(0.0, "No status");
        classification.setTitle(1.0, "Dead of age");
        classification.setTitle(2.0, "Dead of cancer");
        classification.setTitle(3.0, "Resistant");
        population.addClassification(classification);
        population.setCompletionTime(COMPLETION_TIME);
        population.setTimeIncrement(TIME_INCREMENT);
        population.simulate();
        System.out.println( ( System.currentTimeMillis() - start ) / 1000);
        for(;;);
        //        System.out.println(IntStreamEx.of(population.getDynamic("Other")).joining(" "));
        //        System.out.println(IntStreamEx.of(population.getDynamic("No status")).joining(" "));
        //        System.out.println(IntStreamEx.of(population.getDynamic("Dead of age")).joining(" "));
        //        System.out.println(IntStreamEx.of(population.getDynamic("Dead of cancer")).joining(" "));
        //        System.out.println(IntStreamEx.of(population.getDynamic("Resistant")).joining(" "));
    }
    
   
}
