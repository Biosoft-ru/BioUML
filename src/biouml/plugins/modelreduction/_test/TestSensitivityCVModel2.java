package biouml.plugins.modelreduction._test;

import biouml.model.Diagram;
import biouml.plugins.modelreduction.SensitivityAnalysis;
import biouml.plugins.modelreduction.SensitivityAnalysis.SensitivityAnalysisResults;
import biouml.plugins.modelreduction.VariableSet;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestSensitivityCVModel2 extends AbstractBioUMLTest
{
    public TestSensitivityCVModel2(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestSensitivityCVModel2.class);
        return suite;
    }

    public void test() throws Exception
    {
        Diagram diagram = getDiagram("data/Collaboration (git)/Cardiovascular system/Complex model/", "Complex model flatted");

        SensitivityAnalysis analysis = new SensitivityAnalysis(null, "");

        analysis.getParameters().setStartSearchTime(1E2);
        analysis.getParameters().setValidationSize(100);
        analysis.getParameters().setAbsoluteTolerance(0.00001);
        analysis.getParameters().setDiagram(diagram);
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime(1E4);
        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(0.1);

        analysis.getParameters().getEngineWrapper().getEngine();
        
        VariableSet inputSet1 = new VariableSet();
        VariableSet inputSet2 = new VariableSet();
        VariableSet targetSet1 = new VariableSet();
        analysis.getParameters().setInputVariables(new VariableSet[] {inputSet1, inputSet2});
        analysis.getParameters().setTargetVariables(new VariableSet[] {targetSet1});
//        analysis.getParameters().setValidationSize(10);
        inputSet1.setSubdiagramName("Complex model Hallow_plain");
        inputSet1.setVariableNames(new String[] {VariableSet.RATE_VARIABLES, VariableSet.CONSTANT_PARAMETERS});//VariableSet.CONSTANT_PARAMETERS});

        inputSet2.setSubdiagramName("Solodyannkiov 2006 Composite_plain");
        inputSet2.setVariableNames(new String[]{VariableSet.RATE_VARIABLES, VariableSet.CONSTANT_PARAMETERS});//.CONSTANT_PARAMETERS});

        targetSet1.setSubdiagramName("Solodyannkiov 2006 Composite_plain");
        targetSet1.setVariableNames(new String[] {"P_S", "P_D"});
        VariableSet steadySet = new VariableSet();
        analysis.getParameters().setVariableNames(new VariableSet[] {steadySet});
        steadySet.setSubdiagramName("Solodyannkiov 2006 Composite_plain");
        steadySet.setVariableNames(new String[] {"P_S", "P_D"});
        SensitivityAnalysisResults pResults = analysis.performAnalysis();

        double[][] unscaled = pResults.unscaledSensitivities;
        String[] parameters = pResults.parameters;
        String[] targets = pResults.targets;

        
        System.out.println("Target" + "\t" + StreamEx.of(parameters).map(s->s.substring(s.lastIndexOf("\\")+1, s.length())).joining("\t"));
        for( int i = 0; i < targets.length; i++ )
        {
            String shortName = targets[i].substring(targets[i].lastIndexOf("\\")+1, targets[i].length());
            System.out.println(shortName + "\t" + DoubleStreamEx.of(unscaled[i]).joining("\t"));
        }
    }

    private Diagram getDiagram(String path, String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(path);
        DataElement de = collection.get(name);
        return (Diagram)de;
    }
}