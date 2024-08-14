package biouml.plugins.simulation.java._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Equation;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.SimulationResult;

/**
 *  Test state/transition simulation
 */
public class TestStateTransitionModel extends TestCase
{
    public TestStateTransitionModel(String name)
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
        TestSuite suite = new TestSuite(TestStateTransitionModel.class.getName());
        suite.addTest(new TestStateTransitionModel("testSimulate"));
        return suite;
    }

    public void testSimulate() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator( "diagram" );

        // create nodes
        generator.createSpecies( "A", 10 );

        // create equation
        generator.createEquation( "$A", "0.1", Equation.TYPE_RATE_BY_RULE );

        // create states/transitions
        Node state0 = generator.createState( "state0", null, new Assignment( "$A", "$A-2" ) );
        Node state1 = generator.createState( "state1", new Assignment( "$A", "$A-2" ), null );
        generator.createTransition( "transition: 0->1", state0, state1, null, "10" );

        Diagram diagram = generator.getDiagram();

        File outDir = AbstractBioUMLTest.getTestDir();
        OdeSimulationEngine jse = new JavaSimulationEngine();
        jse.setDiagram( diagram );
        jse.setOutputDir( outDir.getPath() );
        File[] modelFiles = jse.generateModel( true );
        assertNotNull( "Model generating failed", modelFiles );

        SimulationResult sr = new SimulationResult(null, "tmp");
        jse.simulate( modelFiles, sr );

        int aInd = sr.getVariableMap().get("$A");
        assertEquals("incorrect start value", 10.0, sr.getValues()[0][aInd]);
        assertEquals("incorrect before state value", 11.0, sr.getValues()[10][aInd], 0.01);
        assertEquals("incorrect after state value", 9.1, sr.getValues()[11][aInd], 0.01);
    }
}
