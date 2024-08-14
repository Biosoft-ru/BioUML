package biouml.plugins.optimization._test;

import java.util.HashMap;
import java.util.Map;

import biouml.model.Diagram;
import biouml.plugins.optimization.analysis.ParameterFitting;
import biouml.plugins.optimization.analysis.ParameterGroup;
import biouml.plugins.optimization.analysis.ParameterGroup.EstimatedParameter;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.table.TableDataCollection;

public class TestParameterFitting extends AbstractBioUMLTest
{
    public TestParameterFitting(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestParameterFitting.class);
        //        suite.addTest(new TestParameterFitting("test"));
        return suite;
    }

    Map<String, Double> expectedResult = new HashMap<String, Double>()
    {
        {
            put("k1", 0.08);
            put("k2", 0.04);
            put("k3", 0.07);
        }
    };

    //TODO: finish test
    public void test() throws Exception
    {
        ParameterFitting analysis = new ParameterFitting(null, "fitting");
        Diagram d = TestUtils.createTestDiagram();
        TableDataCollection experiment = TestUtils.createTimeCourseExperiment_1();
//        analysis.getParameters().setDiagram(d);
//        analysis.getParameters().setExperiment(experiment);
        analysis.getParameters().setOutputDiagram(DataElementPath.create(d));
        ParameterGroup parametersGroup = new ParameterGroup();
        EstimatedParameter[] fittingParameters = new EstimatedParameter[3];
        fittingParameters[0] = new EstimatedParameter("k1", 0, 0, 0.1);
        fittingParameters[1] = new EstimatedParameter("k2", 0, 0, 0.1);
        fittingParameters[2] = new EstimatedParameter("k3", 0, 0, 0.1);
        parametersGroup.setParameters(fittingParameters);
//        analysis.getParameters().setParameters(parametersGroup);
        analysis.getParameters().getEngineWrapper().setEngine(new JavaSimulationEngine());
        analysis.getParameters().getEngineWrapper().getEngine().setInitialTime(0.0);
        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(1.0);
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime(200.0);
//        Map<String, Double> result = analysis.doFitting(d);

//        for (String key: result.keySet())
//            assertEquals(expectedResult.get(key), result.get(key), 1.E-6);
    }

}
