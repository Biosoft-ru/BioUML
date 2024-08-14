package biouml.plugins.simulation.java._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Diagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.SimulationResult;

/**
 *
 */
public class TestEventModel extends TestCase
{
    public TestEventModel(String name)
    {
        super(name);
        File configFile = new File( "./biouml/plugins/simulation/java/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestEventModel.class.getName());
        suite.addTest(new TestEventModel("testCreateDiagram"));
        suite.addTest(new TestEventModel("testSimulate"));
        return suite;
    }

    private Diagram diagram;
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        DiagramGenerator generator = new DiagramGenerator( "diagram_event" );

        // create nodes
        generator.createSpecies("x", 0);
        generator.createSpecies("y", 100);
        generator.createSpecies("vX", 10);
        generator.createSpecies("vY", 0);

        // create equations
        generator.createEquation("$x", "$vX", Equation.TYPE_RATE_BY_RULE);
        generator.createEquation("$y", "$vY", Equation.TYPE_RATE_BY_RULE);
        generator.createEquation("$vY", "-0.9", Equation.TYPE_RATE);

        // create events
        generator.createEvent("floor", "$y <= 0", new Assignment("$vY", "-$vY*0.9"));
        generator.createEvent("right_wall", "$x >= 300", new Assignment("$vX", "-$vX*0.9"));
        generator.createEvent("left_wall", "$x < 0", new Assignment("$vX", "-$vX*0.9"));

        diagram = generator.getDiagram();
    }

    public void testCreateDiagram() throws Exception
    {
        //prepare map of expected events
        String endl = System.getProperty( "line.separator" );
        Map<String, String> expectedEvents = new HashMap<>();
        expectedEvents.put( "floor",
                "Event " + endl + "  trigger: $y <= 0" + endl + "  delay  : 0" + endl + "  assignments: " + endl + "    $vY = -$vY*0.9" );
        expectedEvents.put( "right_wall",
                "Event " + endl + "  trigger: $x >= 300" + endl + "  delay  : 0" + endl + "  assignments: " + endl + "    $vX = -$vX*0.9" );
        expectedEvents.put( "left_wall",
                "Event " + endl + "  trigger: $x < 0" + endl + "  delay  : 0" + endl + "  assignments: " + endl + "    $vX = -$vX*0.9" );

        // print events
        Event[] events = diagram.getRole( EModel.class ).getEvents();
        assertEquals( "Incorrect size of events array", expectedEvents.size(), events.length );
        for( Event event : events )
        {
            String name = event.getDiagramElement().getName();
            String expectedEvent = expectedEvents.get( name );
            assertNotNull( "Not expected event " + name, expectedEvent );
            assertEquals( "Incorrect event " + name, expectedEvent, event.toString() );
        }
    }

    public void testSimulate() throws Exception
    {
        File outDir = AbstractBioUMLTest.getTestDir();
        OdeSimulationEngine jse = new JavaSimulationEngine();
        jse.setDiagram( diagram );
        jse.setOutputDir( outDir.getPath() );
        File[] modelFiles = jse.generateModel( true );
        assertNotNull( "Model generating failed", modelFiles );

        SimulationResult sr = new SimulationResult(null, "tmp");
        String result = jse.simulate( modelFiles, sr );
        System.out.println("Result: " + result);
    }

}
