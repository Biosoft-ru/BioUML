package biouml.plugins.pharm._test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import biouml.model.Diagram;
import biouml.plugins.hemodynamics.Util;
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

public class TestSensitivityComplex extends AbstractBioUMLTest
{
    public TestSensitivityComplex(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestSensitivityComplex.class);
        return suite;
    }
    String resultPath = "C:/Users/axec/Sensitivity results";

    public void test() throws Exception
    {
        test("Comp_A0_R1_500", 0, 1, 500, 1E4, 1);
        test("Comp_A0_R1_5000", 0, 1, 5000, 1E4, 1);
        test("Comp_A0_R1_10000", 0, 1, 10000, 1E4, 1);
        test("Comp_A0_R0.2_5000", 0, 0.2, 5000, 1E4, 1);
        test("Comp_A0.1_R1_500", 0.1, 1, 500, 1E4, 1);
        test("Comp_A0.1_R1_5000", 0.1, 1, 5000, 1E4, 1);
        test("Comp_A0.1_R1_10000", 0.1, 1, 10000, 1E4, 1);
    }

    public void test(String resultName, double aStep, double rStep, double start, double aTol, int size) throws Exception
    {
        Diagram diagram = getDiagram("data/Collaboration (git)/Cardiovascular system/Complex model/", "Complex model new");

        SensitivityAnalysis analysis = new SensitivityAnalysis(null, "");

        analysis.getParameters().setStartSearchTime(start);
        analysis.getParameters().setAbsoluteStep(aStep);
        analysis.getParameters().setRelativeStep(rStep);
        analysis.getParameters().setValidationSize(size);
        analysis.getParameters().setAbsoluteTolerance(aTol);
        analysis.getParameters().setDiagram(diagram);
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime(1E5);
        //        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(0.1);

        analysis.getParameters().getEngineWrapper().getEngine();

        VariableSet inputSet1 = new VariableSet();
        VariableSet inputSet2 = new VariableSet();
        VariableSet targetSet1 = new VariableSet();
        analysis.getParameters().setInputVariables(new VariableSet[] {inputSet1, inputSet2});
        analysis.getParameters().setTargetVariables(new VariableSet[] {targetSet1});

        inputSet1.setSubdiagramName("Kidney");
        inputSet1.setVariableNames(new String[] {});//VariableSet.RATE_VARIABLES, VariableSet.CONSTANT_PARAMETERS});//VariableSet.CONSTANT_PARAMETERS});

        inputSet2.setSubdiagramName("Heart");
        inputSet2.setVariableNames(new String[] {"STRESS"});//VariableSet.RATE_VARIABLES, VariableSet.CONSTANT_PARAMETERS});//.CONSTANT_PARAMETERS});

        targetSet1.setSubdiagramName("Heart");
        targetSet1.setVariableNames(new String[] {"P_S", "P_D", "Cycle_Length"});
        VariableSet steadySet = new VariableSet();
        analysis.getParameters().setVariableNames(new VariableSet[] {steadySet});
        steadySet.setSubdiagramName("Heart");
        steadySet.setVariableNames(new String[] {"P_S", "P_D"});

        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(61);
        SensitivityAnalysisResults pResults = analysis.performAnalysis();

        double[][] unscaled = pResults.unscaledSensitivities;
        String[] parameters = pResults.parameters;
        String[] targets = pResults.targets;

        unscaled = Util.transpose(unscaled);
        //      
        File f = new File(resultPath, resultName + ".txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f)))
        {
            bw.write("Parameter\t" + StreamEx.of(targets).map(s -> s.substring(s.lastIndexOf("\\") + 1, s.length())).joining("\t"));
            bw.write("\n");
            for( int i = 0; i < parameters.length; i++ )
            {
                String shortName = parameters[i].substring(parameters[i].lastIndexOf("\\") + 1, parameters[i].length());
                bw.write(shortName + "\t" + DoubleStreamEx.of(unscaled[i]).joining("\t"));
                bw.write("\n");
            }
        }
        //        System.out.println("Target" + "\t" + StreamEx.of(parameters).map(s->s.substring(s.lastIndexOf("\\")+1, s.length())).joining("\t"));
        //        for( int i = 0; i < targets.length; i++ )
        //        {
        //            String shortName = targets[i].substring(targets[i].lastIndexOf("\\")+1, targets[i].length());
        //            System.out.println(shortName + "\t" + DoubleStreamEx.of(unscaled[i]).joining("\t"));
        //        }
    }

    private Diagram getDiagram(String path, String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection collection = CollectionFactory.getDataCollection(path);
        DataElement de = collection.get(name);
        return (Diagram)de;
    }
}
