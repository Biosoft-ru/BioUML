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

public class TestSensitivitySolodyannikov extends AbstractBioUMLTest
{
    public TestSensitivitySolodyannikov(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestSensitivitySolodyannikov.class);
        return suite;
    }

	String[] input = { "STRESS" };//G_HL", "G_HR", "K_VRHL", "K_VLHR"};//, "AO_2", "Y_ALVL0",
			//"K_HRAR", "A_1", "A_4" };

    String resultPath = "C:/Users/axec/Sensitivity results";
//    String resultName = "Sol_R_0.1_A_1";
	String[] targets = { "P_S", "P_D", "Cycle_Length" };

	public void test() throws Exception
    {
		test("Sol_A0_R1_500", 0, 1, 500, 1E4, 1 );
		test("Sol_A0_R1_5000", 0, 1, 5000, 1E4, 1 );
		test("Sol_A0_R0.2_5000", 0, 0.2, 5000, 1E4, 1 );
		test("Sol_A0.1_R1_500", 0.1, 1, 500, 1E4, 1 );
		test("Sol_A0.1_R1_5000", 0.1, 1, 5000, 1E4, 1 );
    }
	
    public void test(String resultName, double aStep, double rStep, double start, double aTol, int size) throws Exception
    {
        Diagram diagram = getDiagram("data/Collaboration (git)/Cardiovascular system/Solodyannikov 2006/", "Solodyannikov 2006 _2");

        SensitivityAnalysis analysis = new SensitivityAnalysis(null, "");

        analysis.getParameters().setStartSearchTime(start);
        analysis.getParameters().setValidationSize(size);
        analysis.getParameters().setAbsoluteTolerance(aTol);
        analysis.getParameters().setDiagram(diagram);
        
        VariableSet inputSet1 = new VariableSet();
        VariableSet targetSet1 = new VariableSet();
        analysis.getParameters().setInputVariables(new VariableSet[] {inputSet1});
        analysis.getParameters().setTargetVariables(new VariableSet[] {targetSet1});
        
        analysis.getParameters().setVariableNames(new VariableSet[]{targetSet1});

        analysis.getParameters().setAbsoluteStep(aStep);
        analysis.getParameters().setRelativeStep(rStep);//0.3);
        
//        inputSet1.setVariableNames(new String[]{VariableSet.CONSTANT_PARAMETERS, VariableSet.RATE_VARIABLES});
        inputSet1.setVariableNames(input);//new String[]{"AO_2"});//VariableSet.CONSTANT_PARAMETERS});//, VariableSet.RATE_VARIABLES});
        targetSet1.setVariableNames(targets);
//        double start = System.currentTimeMillis();
        
        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(61);
        SensitivityAnalysisResults pResults = analysis.performAnalysis();

        double[][] unscaled = pResults.unscaledSensitivities;
        String[] parameters = pResults.parameters;
        String[] targets = pResults.targets;
        
//    	System.out.println("Target\t"+StreamEx.of(parameters).map(s -> s.substring(s.lastIndexOf("\\") + 1, s.length())).joining("\t"));
//		for (int i = 0; i < targets.length; i++)
//		{
//			String shortName = targets[i].substring(targets[i].lastIndexOf("\\") + 1, targets[i].length());
//			System.out.println(shortName + "\t" + DoubleStreamEx.of(unscaled[i]).joining("\t"));
//		}
//		System.out.println("Ended: " + (System.currentTimeMillis() - start) / 1000);
//		
        unscaled = Util.transpose(unscaled);
//        
		File f = new File(resultPath, resultName);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(f)))
		{
			bw.write("Parameter\t"+StreamEx.of(targets).map(s -> s.substring(s.lastIndexOf("\\") + 1, s.length())).joining("\t"));
			bw.write("\n");
			for (int i=0; i<parameters.length; i++)
			{
				String shortName = parameters[i].substring(parameters[i].lastIndexOf("\\") + 1, parameters[i].length());
				bw.write(shortName + "\t" + DoubleStreamEx.of(unscaled[i]).joining("\t"));
				bw.write("\n");
			}
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
