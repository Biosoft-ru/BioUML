package biouml.plugins.pharm._test;

import java.io.File;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.pharm.analysis.Patient;
import biouml.plugins.pharm.analysis.PopulationSampling;
import biouml.plugins.pharm.analysis.PopulationSamplingParameters;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysis.Util;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestPopulationSamplingPruett extends AbstractBioUMLTest
{
    public TestPopulationSamplingPruett(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestPopulationSamplingPruett.class.getName());
        suite.addTest(new TestPopulationSamplingPruett("simpleTest"));
        return suite;
    }

    private String resultFileName = "ExpData_sampled2.txt";
    //    private String historyFileName = "C:/My/history.txt";
    //    private String inputFileName = "C:/My/input.txt";
    private boolean debug = false;
    private String[] parameterNames = {"k_baro", "A_fluid", "m_fluid", "S_auto", "A_Afferent", "m_Afferent", "A_Symps", "m_symps",
            "S_symps", "B_symps"};
    private String[] observedNames = {"TPR", "CO"};
//    private boolean saveHistory = false;

    public void simpleTest() throws Exception
    {
        TableDataCollection table = getTable();
        double[][] experimentalData =  TableDataCollectionUtils.getMatrix(table);
//        TableDataCollection experimentalTable = TableDataCollectionUtils.createTable("t", experimentalData, observedNames);

        double[][] initialData = {{7.0E-4, 0.000035}, {7900.0, 395}, {3.58, 0.179}, {5126, 256.3}, {2.1, 0.105}, {3.63, 0.1815},
                {1.02, 0.051}, {3.6046, 1.745}, {0.53, 0.0265}, {1.14, 0.057}};

        TableDataCollection initialDataTable = TableDataCollectionUtils.createTable("init", initialData, new String[] {"Mean", "Variance"},
                parameterNames);

        Diagram diagram = getDiagram();
        PopulationSampling analysis = new PopulationSampling(null, "");
        analysis.setDebug(debug);
        //        analysis.setSaveHistory(saveHistory);
        PopulationSamplingParameters parameters = analysis.getParameters();
        parameters.setExperimentalData(table);
        parameters.setInitialData(initialDataTable);
        parameters.setDiagram(getDiagram());
        parameters.setObservedVariables(new VariableSet(diagram, observedNames));
        parameters.setEstimatedVariables(new VariableSet(diagram, parameterNames));
        parameters.setAcceptanceRate(1);
        parameters.setPopulationSize(200);

        double startTime = System.currentTimeMillis();
        List<Patient> result = analysis.justAnalyze();
        System.out.println("Elapsed time: " + ( System.currentTimeMillis() - startTime ) / 1000);
//        writeData(analysis, result);
        checkResult(result, experimentalData);
    }

    private Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = CollectionFactory.getDataCollection( "data/Collaboration (git)/Cardiovascular system/Pruett 2013/" );
        DataElement de = collection.get( "Pruett 2013" );
        return (Diagram)de;
    }
    
    private TableDataCollection getTable() throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = CollectionFactory.getDataCollection( "data/Collaboration (git)/Cardiovascular system/Data/" );
        DataElement de = collection.get( "ExpData_m" );
        return (TableDataCollection)de;
    }

    private void checkResult(List<Patient> result, double[][] sample)
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
            //            System.out.println("Acceptance rate:" + analysis.getAcceptanceRate());
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
        File f = this.getTestFile(resultFileName);
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

        //            double mean = Stat.mean(values);
        //            double expectedMean = Stat.mean(inputValues);
        //            double variance = Stat.variance(values);
        //            double expectedVariance = Stat.variance(inputValues);
        //            System.out.println("Acceptance rate:" + analysis.getAcceptanceRate());
        //            System.out.println("Mean:" + mean + " | " + expectedMean + " | " + ( mean - expectedMean ) / expectedMean);
        //            System.out.println("Variance:" + variance + " | " + expectedVariance + " | " + ( variance - expectedVariance ) / expectedVariance);
    }

    public double[][] readSample(String fileName) throws Exception
    {
        File f = new File(fileName);
        List<String> list = ApplicationUtils.readAsList(f);
        double[][] result = new double[list.size()][];

        for( int i = 1; i < list.size(); i++ )
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
