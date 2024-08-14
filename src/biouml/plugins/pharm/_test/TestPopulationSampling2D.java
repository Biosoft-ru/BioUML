package biouml.plugins.pharm._test;

import java.io.File;
import java.util.List;
import java.util.Random;

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
import ru.biosoft.analysis.Util;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestPopulationSampling2D extends AbstractBioUMLTest
{
    public TestPopulationSampling2D(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestPopulationSampling2D.class.getName());
        suite.addTest(new TestPopulationSampling2D("simpleTest"));
        return suite;
    }

    //    private double mean = 10;
    //    private double variance = 2.5;
    private String fileName = "C:/My/sample2d.txt";
    //    private String historyFileName = "C:/My/history.txt";
    //    private String inputFileName = "C:/My/input.txt";
    private int inputSize = 200;
    private boolean debug = false;
    private String[] parameterNames = {"p1", "p2"};
    private String[] observedNames = {"x1", "x2"};
    private int dim = 2;
    private boolean saveHistory = false;


    /**
     * Simple 2d test
     */
    public void simpleTest() throws Exception
    {
        //        double[][] experimentalData =  generateNormalData(inputSize, dim);
        double[][] experimentalData = readSample(Stat.class, "_test/resources/sample.txt");
        TableDataCollection experimentalTable = TableDataCollectionUtils.createTable("t", experimentalData, observedNames);

        double[][] initialData = {{0.0, 0.5}, {0.0, 0.5}};
        TableDataCollection initialDataTable = TableDataCollectionUtils.createTable("init", initialData, new String[] {"Mean", "Variance"},
                parameterNames);
        Diagram diagram = getDiagram();

        PopulationSampling analysis = new PopulationSampling(null, "");
        analysis.setDebug(debug);
        //        analysis.setSaveHistory(saveHistory);
        PopulationSamplingParameters parameters = analysis.getParameters();
        parameters.setExperimentalData(experimentalTable);
        parameters.setInitialData(initialDataTable);
        parameters.setDiagram(getDiagram());
        parameters.setObservedVariables(new VariableSet(diagram, observedNames));
        parameters.setEstimatedVariables(new VariableSet(diagram, parameterNames));
        parameters.setAcceptanceRate(20);
        parameters.setPopulationSize(2000);
        analysis.setCalculator(new TestCalculator());

        double startTime = System.currentTimeMillis();
        List<Patient> result = analysis.justAnalyze();
        System.out.println("Elapsed time: " + ( System.currentTimeMillis() - startTime ) / 1000);
        writeData(analysis, result);
        checkResult(result, experimentalData, analysis.getAcceptanceRate());
    }

    private Diagram getDiagram() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("diagram");
        generator.createEquation("x1", "2*p1", Equation.TYPE_SCALAR);
        generator.createEquation("x2", "2*p2", Equation.TYPE_SCALAR);
        generator.createEquation("t", "0", Equation.TYPE_RATE);
        Diagram diagram = generator.getDiagram();
        diagram.getRole(EModel.class).declareVariable("x1", 0.0);
        diagram.getRole(EModel.class).declareVariable("x2", 0.0);
        diagram.getRole(EModel.class).declareVariable("p1", 0.0);
        diagram.getRole(EModel.class).declareVariable("p2", 0.0);
        diagram.getRole(EModel.class).declareVariable("t", 0.0);
        return diagram;
    }

    private static class TestCalculator implements PatientCalculator
    {
        @Override
        public Patient calculate(double[] input) throws Exception
        {
            return new Patient(input, new double[] {input[0] * input[1], input[0] + input[1]});
            //            return DoubleStreamEx.of(input).map(d -> 2 * d).toArray();
        }
    }

    private void checkResult(List<Patient> result, double[][] sample, double rate)
    {
        sample = Util.matrixConjugate(sample);
        int length = sample.length;
        double[][] data = StreamEx.of(result).map(p -> p.getObserved()).toArray(double[][]::new);

        data = Util.matrixConjugate(data);

        for( int i = 0; i < length; i++ )
        {
            double mean = Stat.mean(sample[i]);
            double variance = Stat.variance(sample[i]);
            double expectedMean = Stat.mean(data[i]);
            double expectedVariance = Stat.variance(data[i]);
            System.out.println("Acceptance rate:" + rate);
            System.out.println("Mean:" + mean + " | " + expectedMean + " | " + ( mean - expectedMean ) / expectedMean);
            System.out.println(
                    "Variance:" + variance + " | " + expectedVariance + " | " + ( variance - expectedVariance ) / expectedVariance);
            //            double[] values = ArrayUtils.toPrimitive(StreamEx.of(result).map(p -> p.getObserved()[i]).toArray(Double[]::new));
            //            assertEquals(Stat.mean(data[i]), Stat.mean(sample[i]), 0.1);
            //            assertEquals(Stat.variance(data[i]), Stat.variance(sample[i]), 0.5);
        }
        double expectedCovariance = Stat.covariance(data[0], data[1]);
        double covariance = Stat.covariance(sample[0], sample[1]);
        System.out.println(
                "Covariance:" + covariance + " | " + expectedCovariance + " | " + ( covariance - expectedCovariance ) / expectedCovariance);
    }

    private void writeData(PopulationSampling analysis, List<Patient> result) throws Exception
    {
        File f = new File(fileName);
        f.delete();
        //            double[][] data = StreamEx.of(result).map(p -> p.getObserved()).toArray(double[][]::new);
        ApplicationUtils.writeString(f, StreamEx.of(result).map(p -> DoubleStreamEx.of(p.getObserved()).joining("\t")).joining("\n"));

        //            File fInput = new File(inputFileName);
        //            fInput.delete();
        //            ApplicationUtils.writeString(fInput, DoubleStreamEx.of(inputValues).joining("\n"));

        //            File history = new File(historyFileName);
        //            history.delete();
        //            ApplicationUtils.writeString(history, StreamEx.of(analysis.getHistory()).joining("\n"));

        //            double[] values = ArrayUtils.toPrimitive(StreamEx.of(result).map(p -> p.getObserved()[0]).toArray(Double[]::new));

        //                    double mean = Stat.mean(values);
        //                    double expectedMean = Stat.mean(inputValues);
        //                    double variance = Stat.variance(values);
        //                    double expectedVariance = Stat.variance(inputValues);
        //                    System.out.println("Acceptance rate:" + analysis.getAcceptanceRate());
        //                    System.out.println("Mean:" + mean + " | " + expectedMean + " | " + ( mean - expectedMean ) / expectedMean);
        //                    System.out.println("Variance:" + variance + " | " + expectedVariance + " | " + ( variance - expectedVariance ) / expectedVariance);
    }

    public double[][] readSample(Class root, String fileName) throws Exception
    {
        File f = new File(root.getResource(fileName).getFile());
        List<String> list = ApplicationUtils.readAsList(f);
        double[][] result = new double[list.size()][];

        for( int i = 0; i < list.size(); i++ )
            result[i] = ArrayUtils.toPrimitive(StreamEx.of(list.get(i).split("\t")).map(s -> Double.valueOf(s)).toArray(Double[]::new));

        return result;
    }

    private double[][] generateNormalData(int inputSize, int dim)
    {
        Normal[] dist = new Normal[dim];
        double[][] experimentalData = new double[inputSize][dim];

        for( int j = 0; j < dim; j++ )
        {
            dist[j] = new Normal(4 * j + 1, 3 + j, new MersenneTwister(new Random().nextInt()));
            for( int i = 0; i < inputSize; i++ )
                experimentalData[i][j] = dist[j].nextDouble();
        }
        return experimentalData;
    }
}
