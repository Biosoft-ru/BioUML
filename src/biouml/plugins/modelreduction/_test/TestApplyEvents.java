package biouml.plugins.modelreduction._test;

import biouml.model.Diagram;
import biouml.plugins.modelreduction.ApplyEvents;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.SimulationResult;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class TestApplyEvents extends AbstractBioUMLTest
{

    public TestApplyEvents(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestApplyEvents.class);
        return suite;
    }

    public void test() throws Exception
    {
        TableDataCollection table = new StandardTableDataCollection(null, "test");
        table.getColumnModel().addColumn("Start", DataType.Text);
        table.getColumnModel().addColumn("Duration", DataType.Text);
        table.getColumnModel().addColumn("Intensity", DataType.Text);

        TableDataCollectionUtils.addRow(table, "1", new Object[] {"1", "5", "500"});
        TableDataCollectionUtils.addRow(table, "2", new Object[] {"1", "3", "100"});
        TableDataCollectionUtils.addRow(table, "3", new Object[] {"1", "2", "1000"});

        DiagramGenerator generator = new DiagramGenerator("d");
        generator.getEModel().declareVariable("W", 0.0);
        Diagram d = generator.getDiagram();

        ApplyEvents analysis = new ApplyEvents(null, "");

        d = analysis.addEvents(d, table);

        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram(d);
        engine.setCompletionTime(30);
        engine.setTimeIncrement(1);
        SimulationResult result = new SimulationResult(null, "");
        engine.simulate(result);

        double[] values = result.getValues(new String[] {"W"})[0];

        for( int i = 0; i < values.length; i++ )
            assertEquals(expectedValues[i], values[i]);
    }

    static double[] expectedValues = new double[31];
    static
    {
        expectedValues[0] = 500;
        expectedValues[1] = 500;
        expectedValues[2] = 500;
        expectedValues[3] = 500;
        expectedValues[4] = 500;
        expectedValues[5] = 100;
        expectedValues[6] = 100;
        expectedValues[7] = 100;
        expectedValues[8] = 1000;
        expectedValues[9] = 1000;
    }
}
