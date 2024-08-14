package biouml.plugins.hemodynamics._test;

import java.util.stream.DoubleStream;

import biouml.model.Diagram;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.simulation.Model;
import biouml.standard.simulation.SimulationResult;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
public class TestHemodynamicsModel extends AbstractBioUMLTest
{
    public TestHemodynamicsModel(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestHemodynamicsModel.class.getName());
        suite.addTest(new TestHemodynamicsModel("test"));
        return suite;
    }

    public void test() throws Exception
    {
        Diagram diagram = getDiagram("Arterial Tree");
        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        SimulationResult result = new SimulationResult(null, "result");
        engine.setDiagram(diagram);
        engine.setCompletionTime(2);
        engine.setTimeIncrement( 0.01 );
        Model model = engine.createModel();
        assertNotNull("Model was not generated", model);
        engine.simulate(model, result);
        double[] values = result.getValues(new String[]{"inputPressure"})[0];
        
        assertTrue(values.length > 0);
        
        double max = DoubleStream.of(values).max().getAsDouble();
        double min = DoubleStream.of(values).min().getAsDouble();
        assertTrue("Maxium pressure "+max+" is greater than 140 mmHg", max < 140);
        assertTrue("Minimum pressure "+min+" is lower than 60 mmHg", min > 60);
    }
    
    public Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository("../data");
        return DataElementPath.create("databases/Virtual Human/Arterial Tree/"+name).getDataElement(Diagram.class);
    }
}
