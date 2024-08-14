package biouml.plugins.proteinmodel._test;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import com.developmentontheedge.log.PatternFormatter;

import biouml.model.Diagram;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.IterationType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.JacobianType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.Method;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class ProteinModelTest extends AbstractBioUMLTest
{
    //private WriterAppender appender;
    private StreamHandler logHandler;

    public ProteinModelTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(ProteinModelTest.class.getName());
        suite.addTest(new ProteinModelTest("testSimulation10"));
        suite.addTest(new ProteinModelTest("testSimulation100"));
        suite.addTest(new ProteinModelTest("testSimulation"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        logHandler = new StreamHandler( System.out, new PatternFormatter( "%4$-7s :  %5$s%n" ) );
        logHandler.setLevel( Level.INFO );
        Logger.getGlobal().addHandler( logHandler );

        CollectionFactory.createRepository("../data");
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        Logger.getGlobal().removeHandler( logHandler );
    }

    public void testSimulation10() throws Exception
    {
        DataElement de = CollectionFactory.getDataElement("databases/Protein model/Diagrams/Simulation-10");
        assertTrue("Diagram is loaded", de instanceof Diagram);
        Diagram diagram = (Diagram)de;
        simulation(diagram);
    }

    public void testSimulation100() throws Exception
    {
        DataElement de = CollectionFactory.getDataElement("databases/Protein model/Diagrams/Simulation-100");
        assertTrue("Diagram is loaded", de instanceof Diagram);
        Diagram diagram = (Diagram)de;
        simulation(diagram);
    }

    public void testSimulation() throws Exception
    {
        DataElement de = CollectionFactory.getDataElement("databases/Protein model/Diagrams/Simulation");
        assertTrue("Diagram is loaded", de instanceof Diagram);
        Diagram diagram = (Diagram)de;
        simulation(diagram);
    }

    protected void simulation(Diagram diagram) throws Exception
    {
        JavaSimulationEngine simulationEngine = new JavaSimulationEngine();
        JVodeOptions options = new JVodeOptions(Method.BDF, IterationType.FUNCTIONAL, JacobianType.DENSE);
        simulationEngine.setSimulatorOptions(options);
        File outDir = new File("../out");
        outDir.mkdirs();
        simulationEngine.setCompletionTime(15);
        simulationEngine.setOutputDir(outDir.getAbsolutePath());
        simulationEngine.setDiagram(diagram);
        simulationEngine.setJobControl(new FunctionJobControl(null));
        File[] files = simulationEngine.generateModel(true);
        assertNotNull("Generate model error: " + diagram.getName(), files);

        SimulationResult result = new SimulationResult(null, "tmp");
        simulationEngine.initSimulationResult(result);
        ResultWriter currentResults = new ResultWriter(result);

        String msg = simulationEngine.simulate(files, new ResultListener[] {currentResults});
        if( msg != null && msg.length() > 1 )
        {
            fail("Simulation error: " + diagram.getName() + ": " + msg);
        }
        assertEquals("$pm_3 at time=15", 2.144035692981443E7, result.getValues(new String[] {"$pm_3"})[0][15], 0.0001);
    }
}
