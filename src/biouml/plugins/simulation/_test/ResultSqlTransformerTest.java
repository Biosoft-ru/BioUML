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
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.SqlConnectionPool;
import biouml.model.dynamics.Variable;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.access.SimulationResultSqlTransformer;

public class ResultSqlTransformerTest extends TestCase
{
    private SqlDataCollection<SimulationResult> resultDC;
    private SimulationResult result;

    public ResultSqlTransformerTest(String name)
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
        suite.addTest(new ResultSqlTransformerTest("testWriteSimulationResult"));
        suite.addTest(new ResultSqlTransformerTest("testReadSimulationResult"));
        suite.addTest(new ResultSqlTransformerTest("testRemoveSimulationResult"));

        return suite;
    }

    @Override
    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.put( DataCollectionConfigConstants.NAME_PROPERTY, "Simulatiob results (test)" );
        props.put( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, SimulationResult.class.getName() );
        props.put( SqlDataCollection.SQL_TRANSFORMER_CLASS, SimulationResultSqlTransformer.class.getName() );
        props.put( SqlDataCollection.JDBC_URL_PROPERTY, "cyclonet" );
        resultDC = new SqlDataCollection<>( null, props );

        createSimulationResult();
    }

    @Override
    public void tearDown() throws Exception
    {
        resultDC.close();
        SqlConnectionPool.closeMyConnections();
    }

    private void createSimulationResult()
    {
        result = new SimulationResult(null, "result");
        double[] times = new double[] {1, 2, 3};
        double[][] values = new double[][] {{10, 20}, {30, 40}, {50, 60}};

        Map<String, Integer> var_map = new HashMap<>();
        var_map.put("time", 0);
        var_map.put("var0", 1);
        var_map.put("var1", 2);

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
        System.out.println("testWriteSimulationResult");
        resultDC.remove("result");
        resultDC.put(result);
    }

    public void testRemoveSimulationResult() throws Exception
    {
        System.out.println("tesRemoveSimulationResult");
        resultDC.remove(result.getName());
        assertFalse(resultDC.contains(result.getName()));
    }

    public void testReadSimulationResult() throws Exception
    {
        System.out.println("testReadSimulationResult");

        SimulationResult res = resultDC.get(result.getName());

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

        assertEquals(new_variables.size(), old_variables.size());
        for( Map.Entry<String, Integer> newVarsEntry : new_variables.entrySet() )
        {
            String name = newVarsEntry.getKey();
            assertTrue("Variable " + name + "not found", old_variables.containsKey(name));
            int new_index = newVarsEntry.getValue();
            int old_index = old_variables.get(name);

            for (int i = 0; i < res.getValues().length; i++)
            {
                assertEquals(res.getValues()[i][new_index], result.getValues()[i][old_index], 1e-8);
            }
        }

        // verify intiaslValues
        ArrayList<Variable> old_list = result.getInitialValues();
        ArrayList<Variable> new_list = res.getInitialValues();

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
