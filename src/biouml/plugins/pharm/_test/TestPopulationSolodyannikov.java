package biouml.plugins.pharm._test;

import java.io.File;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.pharm.analysis.Patient;
import biouml.plugins.pharm.analysis.PopulationSampling;
import biouml.plugins.pharm.analysis.PopulationSamplingParameters;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
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

public class TestPopulationSolodyannikov extends AbstractBioUMLTest
{
    public TestPopulationSolodyannikov(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestPopulationSolodyannikov.class.getName());
        suite.addTest(new TestPopulationSolodyannikov("simpleTest"));
        return suite;
    }

    private String resultFileName = "ExpData_solod2.txt";
    private boolean debug = true;
    private String[] additionalList = {"V_AL0", "V_HL0", "Y_ALVL0", "Y_HLAL", "w_AL0", "w_HL0", "w_VL0", "G_AL0", "G_HL", "G_VL0"};
    private String[] observedNames = {"P_S", "P_D", "Cycle_Length"};
//    private boolean saveHistory = false;

    public void simpleTest() throws Exception
    {
        TableDataCollection table = getTable();
        Diagram d = getDiagram();
        EModel emodel = d.getRole(EModel.class);
        List<Variable> vars = StreamEx.of(additionalList).map(s->emodel.getVariable(s)).toList();
        
        double[][] initialData = new double[vars.size()][];
        
        for (int i=0; i<vars.size(); i++)
        {
            initialData[i] = new double[]{vars.get(i).getInitialValue(), vars.get(i).getInitialValue()/20, 0, vars.get(i).getInitialValue()*2};
        }
        String[] parameterNames = vars.stream().map(var->var.getName()).toArray(String[]::new);
        
        TableDataCollection initialDataTable = TableDataCollectionUtils.createTable("init", initialData, new String[] {"Mean", "Variance", "Min", "Max"},
                parameterNames);

        PopulationSampling analysis = new PopulationSampling(null, "");
        analysis.setDebug(debug);
        analysis.setObservedDistribution(new double[]{150, 100, 60.0/90.0}, new double[][]{{1, 0, 0}, {0,1,0}, {0,0,1}});
        analysis.setUseExperimentalTable(false);
        
        //        analysis.setSaveHistory(saveHistory);
        PopulationSamplingParameters parameters = analysis.getParameters();
//        parameters.setExperimentalData(table);
        parameters.setInitialData(initialDataTable);
        parameters.setDiagram(getDiagram()); 
        parameters.setObservedVariables(new VariableSet(d, observedNames));
        parameters.setEstimatedVariables(new VariableSet(d, parameterNames));
        parameters.setAcceptanceRate(1);
        parameters.setPopulationSize(20);
        parameters.setPreliminarySteps(1);
        ((JVodeOptions)parameters.getEngineWrapper().getEngine().getSimulatorOptions()).setAtol(1E-8);
        double startTime = System.currentTimeMillis();
        List<Patient> result = analysis.justAnalyze();
        System.out.println("Elapsed time: " + ( System.currentTimeMillis() - startTime ) / 1000);
        writeData(analysis, result);
        checkResult(result, TableDataCollectionUtils.getMatrix(table));
    }

    private Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = CollectionFactory.getDataCollection( "data/Collaboration (git)/Cardiovascular system/Solodyannikov 2006/" );
        DataElement de = collection.get( "Solodyannikov 2006 _2" );
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
            System.out.println("Mean:" + mean + " | expected: " + expectedMean + " | error: " + ( mean - expectedMean ) / expectedMean);
            System.out.println(
                    "Variance:" + variance + " | expected: " + expectedVariance + " | error: " + ( variance - expectedVariance ) / expectedVariance);
            //            double[] values = ArrayUtils.toPrimitive(StreamEx.of(result).map(p -> p.getObserved()[i]).toArray(Double[]::new));
            //            assertEquals(Stat.mean(data[i]), Stat.mean(sample[i]), 0.1);
            //            assertEquals(Stat.variance(data[i]), Stat.variance(sample[i]), 0.5);
        }
        double expectedCovariance = Stat.covariance(data[0], data[1]);
        double covariance = Stat.covariance(sample[0], sample[1]);
        System.out.println(
                "Covariance:" + covariance + " | expected: " + expectedCovariance + " | error: " + ( covariance - expectedCovariance ) / expectedCovariance);
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
