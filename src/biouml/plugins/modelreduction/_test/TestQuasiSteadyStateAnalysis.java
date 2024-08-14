package biouml.plugins.modelreduction._test;

import java.util.List;
import java.util.Map;

import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;

import biouml.model.Diagram;
import biouml.plugins.modelreduction.QuasiSteadyStateAnalysis;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class TestQuasiSteadyStateAnalysis extends TestCase
{
    public TestQuasiSteadyStateAnalysis(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestQuasiSteadyStateAnalysis.class);
        return suite;
    }

    public void testQuasiSteadyStateAnalysis() throws Exception
    {
        Diagram diagram = TestUtils.createTestDiagram_1();

        OdeSimulationEngine jse = new JavaSimulationEngine();
        jse.setDiagram(diagram);
        jse.setOutputDir("../out");
        jse.setInitialTime(0.0);
        jse.setTimeIncrement(1.0);
        jse.setCompletionTime(1000.0);

        QuasiSteadyStateAnalysis qssa = new QuasiSteadyStateAnalysis(null, "testQSSA");
        TableDataCollection tdc = new StandardTableDataCollection(null, "");
        Map<String, List<double[]>> result = qssa.performAnalysis(jse, tdc, 1e-5, 0.01, 0.01);

        assertEquals(true, isResultOK(result));
    }

    private boolean isResultOK(Map<String, List<double[]>> result)
    {
        if( result == null )
            return false;

        List<double[]> list = result.get("$e");
        //result list for e: [0.0,679.0]; [747.0,1000.0]

        if( list.size() != 2 || list.get(0)[0] != 0.0 || list.get(0)[1] != 679.0 || list.get(1)[0] != 747.0 || list.get(1)[1] != 1000.0 )
            return false;

        list = result.get("$s");
        //result list for s: [0.0, 0.0]; [682.0, 1000.0]
        if( list.size() != 2 || list.get(0)[0] != 0.0 || list.get(0)[1] != 0.0 || list.get(1)[0] != 682.0 || list.get(1)[1] != 1000.0 )
            return false;

        list = result.get("$c1");
        //result list for c1: [0.0, 680.0]; [731.0, 1000.0]
        if( list.size() != 2 || list.get(0)[0] != 0.0 || list.get(0)[1] != 680.0 || list.get(1)[0] != 731.0 || list.get(1)[1] != 1000.0 )
            return false;

        list = result.get("$c2");
        //result list for c2: [0.0, 679.0]; [746.0, 1000.0]
        if( list.size() != 2 || list.get(0)[0] != 0.0 || list.get(0)[1] != 679.0 || list.get(1)[0] != 746.0 || list.get(1)[1] != 1000.0 )
            return false;

        list = result.get("$p");
        //result list for p: [747.0, 1000.0]
        if( list.size() != 1 || list.get(0)[0] != 747.0 || list.get(0)[1] != 1000.0 )
            return false;

        return true;
    }
}