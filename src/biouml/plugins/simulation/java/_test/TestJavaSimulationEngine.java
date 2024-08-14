package biouml.plugins.simulation.java._test;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;

public class TestJavaSimulationEngine
    extends TestCase
{
    public TestJavaSimulationEngine(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestJavaSimulationEngine.class.getName());
        suite.addTest ( new TestJavaSimulationEngine("testSimulation"));
        return suite;
    }

    public void testSimulation() throws Exception
    {
        File file = new File("./biouml/plugins/simulation/java/_test/TYSON2.XML");
        assertTrue("Can not find file: " + file.getCanonicalPath(), file.exists());

        Diagram diagram = SbmlModelFactory.readDiagram(file, null, null);

        OdeSimulationEngine jse = new JavaSimulationEngine();
        jse.setDiagram(diagram);
        jse.setOutputDir( AbstractBioUMLTest.getTestDir().getPath() );
        jse.generateModel(true);
    }
}
