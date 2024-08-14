package biouml.plugins.simulation._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.FileCollection;
import ru.biosoft.access.FileDataElement;
import biouml.model.dynamics.Variable;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.access.SimulationResultTransformer;

public class ResultTransformerTest
    extends TestCase
{
    static SimulationResult result = new SimulationResult(null, "result");
    static SimulationResultTransformer transformer;
    static FileDataElement out;

    public ResultTransformerTest(String name)
    {
        super(name);
        File configFile = new File( "./biouml/plugins/simulation/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(ResultTransformerTest.class.getName());
        suite.addTest(new ResultTransformerTest("testCreateSimulationResult"));
        suite.addTest(new ResultTransformerTest("testWriteSimulationResult"));
        suite.addTest(new ResultTransformerTest("testReadSimulationResult"));

        return suite;
    }

    public void testCreateSimulationResult() throws Exception
    {
        result = new SimulationResult(null, "result");
        double[] times = new double[]
            {
            1, 2, 3};
        double[][] values = new double[][]
            {
            {
            10, 20}
            ,
            {
            30, 40}
            ,
            {
            50, 60}
        };

        Map<String, Integer> var_map = new HashMap<>();
        var_map.put("var0", 0);
        var_map.put("var1", 1);

        Variable v0 = new Variable("v0", null, null);
        v0.setInitialValue(111);
        v0.setUnits("unit0");

        Variable v1 = new Variable("v1", null, null);
        v1.setInitialValue(222);
        v1.setUnits("unit1");

        Variable v2 = new Variable("v2", null, null);
        v2.setInitialValue(333);
        v2.setUnits("unit2");

        result.addInitialValue(v0);
        result.addInitialValue(v1);
        result.addInitialValue(v2);

        result.setDiagramName("diagram_name");
        result.setTitle("title");
        result.setInitialTime(1);
        result.setCompletionTime(2);

        result.setTimes(times);
        result.setValues(values);
        result.setVariableMap(var_map);
    }

    public void testWriteSimulationResult() throws Exception
    {
        // create FileDataCollection
        Properties props = new Properties();
        props.put(DataCollectionConfigConstants.NAME_PROPERTY, "testFileDC");
        props.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, "./biouml/plugins/simulation/_test");
        props.put(FileCollection.FILE_FILTER, "");
        FileCollection fdc = new FileCollection(null, props);

        transformer = new SimulationResultTransformer();
        transformer.init(fdc, null);

        out = transformer.transformOutput(result);
    }

    public void testReadSimulationResult() throws Exception
    {
        SimulationResult res = transformer.transformInput(out);

        assertEquals(result.getName(), res.getName());
        assertEquals(result.getDiagramPath(), res.getDiagramPath());
        assertEquals(result.getTitle(), res.getTitle());
        assertEquals(result.getInitialTime(), res.getInitialTime(), 1e-8);
        assertEquals(result.getCompletionTime(), res.getCompletionTime(), 1e-8);

        // check result set - reuse method from ResultTransformerTest
        for (int i = 0; i < res.getTimes().length; i++)
            assertEquals(res.getTimes()[i], result.getTimes()[i], 1e-8);

        Map<String, Integer> new_variables = res.getVariableMap();
        Map<String, Integer> old_variables = result.getVariableMap();

        {
            assertEquals(new_variables.size(), old_variables.size());
            for( Map.Entry<String, Integer> newVariablesEntry : new_variables.entrySet() )
            {
                for( Map.Entry<String, Integer> oldVariablesEntry : old_variables.entrySet() )
                {
                    String new_name = newVariablesEntry.getKey();
                    String old_name = oldVariablesEntry.getKey();
                    if( old_name.equals(new_name) )
                    {
                        int new_index = newVariablesEntry.getValue();
                        int old_index = oldVariablesEntry.getValue();
                        for (int i = 0; i < res.getValues().length; i++)
                        {
                            assertEquals(res.getValues()[i][new_index], result.getValues()[i][old_index], 1e-8);
                        }
                        break;
                    }
                }
            }
        }

        // verify initialValues
        ArrayList<Variable> old_list = result.getInitialValues();
        ArrayList<Variable> new_list = res.getInitialValues();

        {
            Iterator<Variable> old_iter = old_list.iterator();
            Iterator<Variable> new_iter = new_list.iterator();

            while (new_iter.hasNext())
            {
                while (old_iter.hasNext())
                {
                    Variable new_var = new_iter.next();
                    Variable old_var = old_iter.next();

                    if (old_var.getName().equals(new_var.getName()))
                    {
                        assertEquals(old_var.getInitialValue(), new_var.getInitialValue(), 1e-8);
                        assertEquals(old_var.getUnits(), new_var.getUnits());
                    }
                    break;
                }
            }
        }
    }
}
