package biouml.plugins.simulation_test._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import biouml.model.Diagram;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineRegistry;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.SimulationResult;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class ApoptosisTest extends AbstractBioUMLTest
{
    /** Standart JUnit constructor */
    public ApoptosisTest(String name)
    {
        super(name);
        File configFile = new File( "./biouml/plugins/simulation_test/_test/log.lcf" );
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
        TestSuite suite = new TestSuite(ApoptosisTest.class.getName());
        suite.addTest(new ApoptosisTest("test"));
        return suite;
    }

    public void test() throws Exception
    {
        String repositoryPath = "../data";
        CollectionFactory.createRepository(repositoryPath);
        String collectionName = "databases/Apoptosis model/Composite model";
        String diagramName = "Composite apoptosis model";
        DataCollection<?> collection = CollectionFactory.getDataCollection(collectionName);
        assertNotNull(collection);
        DataElement de = collection.get(diagramName);
        assertNotNull(de);
        assert ( de instanceof Diagram );

        Diagram diagram = (Diagram)de;

        SimulationEngine engine = SimulationEngineRegistry.getSimulationEngine(diagram);

        assertNotNull(engine);

        assert ( engine instanceof JavaSimulationEngine );

        engine.setDiagram(diagram);

        Model model = engine.createModel();

        assertNotNull(model);

        assert ( model instanceof JavaBaseModel );

        SimulationResult result = new SimulationResult(null, "result");
        engine.simulate(model, result);
    }

}
