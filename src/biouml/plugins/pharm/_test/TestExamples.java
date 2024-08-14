package biouml.plugins.pharm._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Diagram;
import biouml.plugins.pharm.nlme.PopulationModelSimulationEngine;
import biouml.plugins.simulation.Model;

public class TestExamples extends AbstractBioUMLTest
{
    public TestExamples(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestExamples.class.getName() );
        suite.addTest( new TestExamples( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        testDiagram("Indometh");
        testDiagram("Theoph");
        testDiagram("SteadyState");
    }
    
    
    public void testDiagram(String name) throws Exception
    {
        Diagram diagram = TestUtil.getExampleDiagram(name);

        assertNotNull(diagram);
        System.out.println( "Diagram was read successfully" );
        PopulationModelSimulationEngine engine = new PopulationModelSimulationEngine();
        engine.setDiagram( diagram );
       
        Model model = engine.createModel();

        assertNotNull( model );
        engine.setNeedToShowPlot(false);
        engine.simulate( model );
    }
}
