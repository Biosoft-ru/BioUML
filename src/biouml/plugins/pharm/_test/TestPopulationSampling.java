package biouml.plugins.pharm._test;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.pharm.analysis.Patient;
import biouml.plugins.pharm.analysis.PatientCalculator;
import biouml.plugins.pharm.analysis.PopulationSampling;
import biouml.plugins.pharm.analysis.PopulationSamplingParameters;
import biouml.standard.diagram.DiagramGenerator;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.analysis.Stat;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestPopulationSampling extends AbstractBioUMLTest
{
    public TestPopulationSampling(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestPopulationSampling.class.getName());
        suite.addTest(new TestPopulationSampling("simpleTest"));
        return suite;
    }

    private double mean = 10;
    private double variance = 2.5;
    private String fileName = "C:/My/sampled.txt";
    private String historyFileName = "C:/My/history.txt";
    private String inputFileName = "C:/My/input.txt";
    private int inputSize = 200;
    private boolean debug = false;
    private boolean saveHistory = true;

    /**
     * Simple 1d test
     */
    //TODO: set seed for random generator
    public void simpleTest() throws Exception
    {
        Normal dist = new Normal(mean, variance, new MersenneTwister(1234567));
        double[][] experimentalData = new double[inputSize][1];
        double[] inputValues = new double[inputSize];
        for( int i = 0; i < inputSize; i++ )
        {
            experimentalData[i][0] = dist.nextDouble();
            inputValues[i] = experimentalData[i][0];
        }

        TableDataCollection experimentalTable = TableDataCollectionUtils.createTable("exp", experimentalData, new String[] {"Observed"});

        double mean = Stat.mean(inputValues);
        double[][] initialData = {{mean, mean / 10, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}};
        TableDataCollection initialDataTable = TableDataCollectionUtils.createTable("init", initialData, new String[] {"Mean", "Variance", "Min", "Max"},
                new String[] {"Parameter"});

        Diagram diagram = getDiagram();
        
        PopulationSampling analysis = new PopulationSampling(null, "");
        analysis.getParameters().setSeed(667);
        analysis.setDebug(debug);
        analysis.setSaveHistory(saveHistory);
        PopulationSamplingParameters parameters = analysis.getParameters();
        parameters.setExperimentalData(experimentalTable);
        parameters.setInitialData(initialDataTable);
        parameters.setDiagram(diagram);
        parameters.setObservedVariables(new VariableSet(diagram, new String[] {"Observed"}));
        parameters.setEstimatedVariables(new VariableSet(diagram, new String[] {"Parameter"}));
        parameters.setAcceptanceRate(30);
        parameters.setPopulationSize(1000);
        parameters.setPreliminarySteps(10);
        analysis.setCalculator(new TestCalculator());
        analysis.setUseExperimentalTable(true);

        double startTime = System.currentTimeMillis();
        List<Patient> result = analysis.justAnalyze();
        System.out.println("Elapsed time: " + ( System.currentTimeMillis() - startTime ) / 1000);
//        writeData(analysis, result, inputValues);
        checkResult(result, inputValues);
    }

    private Diagram getDiagram() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("diagram");
        generator.createEquation("Observed", "Parameter*3+2", Equation.TYPE_SCALAR);
        generator.createEquation("Parameter", "0", Equation.TYPE_RATE);
        Diagram diagram = generator.getDiagram();
        diagram.getRole(EModel.class).declareVariable("Observed", 0.0);
        diagram.getRole(EModel.class).declareVariable("Parameter", 0.0);
        return diagram;
    }

    private static class TestCalculator implements PatientCalculator
    {
        @Override
        public Patient calculate(double[] input) throws Exception
        {
            return new Patient(input, DoubleStreamEx.of(input).map(d -> 3 * d + 2).toArray());
        }
    }

    private void checkResult(List<Patient> result, double[] inputValues)
    {
        double[] values = ArrayUtils.toPrimitive(StreamEx.of(result).map(p -> p.getObserved()[0]).toArray(Double[]::new));
        double mean = Stat.mean(values);
        double expectedMean = Stat.mean(inputValues);
        assertEquals(0, ( mean - expectedMean ) / expectedMean, 0.1);
        System.out.println(Math.abs(mean - expectedMean) / expectedMean);

        double var = Stat.variance(values);
        double expectedVar = Stat.variance(inputValues);
        System.out.println(Math.abs(var - expectedVar) / expectedVar);
        assertEquals(0, ( var - expectedVar)/expectedVar, 0.1);
    }

    private void writeData(PopulationSampling analysis, List<Patient> result, double[] inputValues) throws Exception
    {
        File f = new File(fileName);
        f.delete();
        ApplicationUtils.writeString(f, StreamEx.of(result).map(p -> p.getObserved()[0]).joining("\n"));

        File fInput = new File(inputFileName);
        fInput.delete();
        ApplicationUtils.writeString(fInput, DoubleStreamEx.of(inputValues).joining("\n"));

        File history = new File(historyFileName);
        history.delete();
        ApplicationUtils.writeString(history, StreamEx.of(analysis.getHistory()).joining("\n"));

        double[] values = ArrayUtils.toPrimitive(StreamEx.of(result).map(p -> p.getObserved()[0]).toArray(Double[]::new));

        double mean = Stat.mean(values);
        double expectedMean = Stat.mean(inputValues);
        double variance = Stat.variance(values);
        double expectedVariance = Stat.variance(inputValues);
        System.out.println("Acceptance rate:" + analysis.getAcceptanceRate());
        System.out.println("Mean:" + mean + " | " + expectedMean + " | " + ( mean - expectedMean ) / expectedMean);
        System.out.println("Variance:" + variance + " | " + expectedVariance + " | " + ( variance - expectedVariance ) / expectedVariance);
    }
}
