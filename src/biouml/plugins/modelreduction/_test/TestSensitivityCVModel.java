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

public class TestSensitivityCVModel extends AbstractBioUMLTest
{
    public TestSensitivityCVModel(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestSensitivityCVModel.class);
        return suite;
    }

    public void test() throws Exception
    {
        Diagram diagram = getDiagram("data/Collaboration (git)/Cardiovascular system/Complex model/", "Complex model simple");

        SensitivityAnalysis analysis = new SensitivityAnalysis(null, "");

        analysis.getParameters().setAbsoluteTolerance(0.005);
//        analysis.getParameters().setRelativeTolerance(1E-9);
        analysis.getParameters().setDiagram(diagram);
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime(1E6);
        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(0.1);

        VariableSet inputSet1 = new VariableSet();
        VariableSet targetSet1 = new VariableSet();
        analysis.getParameters().setInputVariables(new VariableSet[] {inputSet1});//, inputSet2});
        analysis.getParameters().setTargetVariables(new VariableSet[] {targetSet1});//, targetSet2});
        analysis.getParameters().setValidationSize(10);
        inputSet1.setSubdiagramName("Sol06 Left Ventricle");
        inputSet1.setVariableNames(new String[] {"P_HL0"});//VariableSet.CONSTANT_PARAMETERS});
        
        targetSet1.setSubdiagramName("Hallow Nervous System");
        targetSet1.setVariableNames(new String[] {"P_ma"});
        VariableSet steadySet = new VariableSet();
        analysis.getParameters().setVariableNames(new VariableSet[] {steadySet});
        steadySet.setSubdiagramName("Hallow Nervous System");
        steadySet.setVariableNames(new String[] {"P_ma"});
        SensitivityAnalysisResults pResults = analysis.performAnalysis();
//        System.out.println(DoubleStreamEx.of(pResults.unscaledSensitivities[0]).joining(" "));

        double[][] unscaled = pResults.unscaledSensitivities;

        for( double[] val : unscaled )
            System.out.println(DoubleStreamEx.of(val).joining(" "));
    }

    private Diagram getDiagram(String path, String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(path);
        DataElement de = collection.get(name);
        return (Diagram)de;
    }
}
