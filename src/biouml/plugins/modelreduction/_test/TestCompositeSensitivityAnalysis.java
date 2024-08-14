package biouml.plugins.modelreduction._test;

import biouml.model.Diagram;
import biouml.plugins.modelreduction.SensitivityAnalysis;
import biouml.plugins.modelreduction.SensitivityAnalysis.SensitivityAnalysisResults;
import biouml.plugins.modelreduction.VariableSet;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestCompositeSensitivityAnalysis extends AbstractBioUMLTest
{
    public TestCompositeSensitivityAnalysis(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestCompositeSensitivityAnalysis.class);
        return suite;
    }

    public void testParamSensitivityAnalysis() throws Exception
    {
        Diagram diagram = getDiagram("a_agent");

        SensitivityAnalysis analysis = new SensitivityAnalysis(null, "");

        analysis.getParameters().setAbsoluteTolerance(1E-8);
        analysis.getParameters().setRelativeTolerance(1E-9);
        analysis.getParameters().setDiagram(diagram);
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime(1E4);
//        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(0.1);

        VariableSet inputSet1 = new VariableSet();
        VariableSet inputSet2 = new VariableSet();
        VariableSet targetSet1 = new VariableSet();
        VariableSet targetSet2 = new VariableSet();
        analysis.getParameters().setInputVariables(new VariableSet[] {inputSet1, inputSet2});
        analysis.getParameters().setTargetVariables(new VariableSet[] {targetSet1, targetSet2});
        analysis.getParameters().setValidationSize(10);
        inputSet1.setSubdiagramName("a1");
        inputSet2.setSubdiagramName("a2");
        inputSet1.setVariableNames(new String[] {"k0", "k1"});
        inputSet2.setVariableNames(new String[] {"k2", "k_2", "k3", "k_3", "k4", "k5"});

        targetSet1.setSubdiagramName("a1");
        targetSet1.setVariableNames(new String[] {"$A", "$B"});

        targetSet2.setSubdiagramName("a2");
        targetSet2.setVariableNames(new String[] {"$B", "$C", "$D"});

        VariableSet steadySet = new VariableSet();
        analysis.getParameters().setVariableNames(new VariableSet[] {steadySet});
        steadySet.setSubdiagramName("a1");
        steadySet.setVariableNames(new String[] {"$A", "$B"});
        SensitivityAnalysisResults pResults = analysis.performAnalysis();
        System.out.println(DoubleStreamEx.of(pResults.unscaledSensitivities[0]).joining(" "));

        double[][] unscaled = pResults.unscaledSensitivities;

        for( double[] val : unscaled )
        {
            System.out.println(DoubleStreamEx.of(val).joining(" "));
        }
    }

    private double[][] unscaledParamSensitivities_d2 = new double[][] {{10, -9.99001, 0, 0, 0, 0, 0, 0},
            {20, 0, -19.99, -19.99, -9.99251, -9.99251, 9.9975, 9.9975}, {10, 0, 9.995, -9.995, -14.9888, -4.99625, -4.99875, 4.99875},
            {10, 0, -9.995, 9.995, -4.99625, -14.9888, 4.99875, -4.99875}};

    private Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection("data/Collaboration/test/Data/");
        DataElement de = collection.get(name);
        return (Diagram)de;
    }
}
