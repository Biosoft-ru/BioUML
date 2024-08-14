package biouml.plugins.hemodynamics._test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsEModel;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics._test.PulseWavePatientInfo.PressureProfile;
import biouml.plugins.sbgn.Type;
import biouml.standard.simulation.ResultListener;
import biouml.standard.type.Stub;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class PulseWaveValidationTest extends TestCase
{
    private static final String REPOSITORY_PATH = "../data";

    private static final Object AORTAL = "Ascending Aorta";
    private static final Object L_RADIAL = "L. Radial";

    private static final DataElementPath DIAGRAM_PATH = DataElementPath.create("databases/Virtual Human/Diagrams/Arterial Brachial new");
        private static final File TEST_DIR = new File("C:/VP/Pulse wave/");
    private static final File DEFAULT_INPUT_FILE = new File(TEST_DIR + "/Data.txt");

    private static final int THREAD_NUMBER = 1;

    private static final int COMPLETION_TIME = 6;
    private static final int START_SEARCH_TIME = 4;

    public PulseWaveValidationTest()
    {
        super("test");
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(PulseWaveValidationTest.class.getName());
        suite.addTest(new PulseWaveValidationTest());
        return suite;
    }

    public void test() throws Exception
    {
        Options options = new Options("input pressure", false, false, true, true);
        options.useResistance = true;
        doTest(options);
    }

    private ConcurrentLinkedQueue<PulseWavePatientInfo> infos;

    public void readData(File f) throws IOException
    {
        infos = new ConcurrentLinkedQueue<>();
        try (BufferedReader br = ApplicationUtils.utfReader(f))
        {
            String line = br.readLine();
            String[] propertyNames = PulseWavePatientInfo.readHeader(line);
            while( ( line = br.readLine() ) != null )
                infos.add(new PulseWavePatientInfo(line, propertyNames));
        }
    }

    public void doTest(Options options) throws Exception
    {
        readData(options.inputFile);
        new File(options.resultFolder).mkdirs();
        File seriesResult = new File(options.resultFolder + "//" + options.name + "_series.txt");
        File staticResult = new File(options.resultFolder + "//" + options.name + "_static.txt");
        File optionsFile = new File(options.resultFolder + "//" + options.name + "_info.txt");

        try (BufferedWriter bwOptions = ApplicationUtils.utfAppender(optionsFile))
        {
            bwOptions.write(options.toString());
        }

        SimulationThread[] threads = new SimulationThread[THREAD_NUMBER];
        for( int i = 0; i < THREAD_NUMBER; i++ )
        {
            threads[i] = new SimulationThread(options, seriesResult, staticResult);
            threads[i].start();
        }

        for( int i = 0; i < THREAD_NUMBER; i++ )
            threads[i].join();
    }

    public synchronized ArterialBinaryTreeModel getModel(HemodynamicsSimulationEngine engine) throws Exception
    {
        return engine.createModel();
    }

    public static HemodynamicsSimulationEngine createEngine() throws Exception
    {
        return new HemodynamicsSimulationEngine();
    }

    private class SimulationThread extends Thread implements ResultListener
    {
        Options options;
        File seriesResult;
        File staticResult;
        public SimulationThread(Options options, File seriesResult, File staticResult)
        {
            this.options = options;
            this.staticResult = staticResult;
            this.seriesResult = seriesResult;
        }

        @Override
        public void run()
        {
            PulseWavePatientInfo info = infos.poll();
            System.out.println("Simulate: " + info.getId());
            while( info != null )
            {
                try
                {
//                    if( info.isValid() )
                        executeTest2(info);

                }
                catch( Exception ex )
                {
                    System.out.println("Error on patient: " + info.getId());
                    ex.printStackTrace();
                }
                info = infos.poll();
            }
            System.out.println("Finished");
        }

        private void setGlobalParameters(Diagram diagram, PulseWavePatientInfo info)
        {
            HemodynamicsEModel emodel = diagram.getRole( HemodynamicsEModel.class );
            
//            emodel.getVariable("T_C").setShowInPlot(true);
//            emodel.getVariable("inputPressure").setShowInPlot(true);
            
            double sv = info.getDoubleValue("sv");
            double ps = info.getDoubleValue("a_sp");
            double pd = info.getDoubleValue("a_dp");
//            double hr = info.getDoubleValue("hr");
            double tf = info.getDoubleValue("tf")/1000;
            double hr = 60 / tf;
            double co = sv * hr;

            double resistance = ( ps / 3 + 2 * pd / 3 ) * 60 / co;
            if( options.useResistance )
                emodel.getVariable("capillaryResistance").setInitialValue(resistance);

            if( options.useHR )
                emodel.getVariable("HR").setInitialValue( hr );//60.0 / hr);

            if( options.useSV )
                emodel.getVariable("SV").setInitialValue(sv);
        }
        
        private void setInitialPressure(Diagram diagram, PressureProfile profile)
        {
            String[] formulas = profile.getFormulas("cycle_Time");
            Equation eq = new Equation(null, Equation.TYPE_SCALAR, "inputPressure");
            eq.setFormula(formulas[1]);
            Node node = new Node(diagram , new Stub(null, "inputPressureEquation2",  Type.TYPE_EQUATION) );
            node.setRole(eq);
            diagram.put(node);
            
            eq = new Equation(null, Equation.TYPE_SCALAR, "cycle_Time");
            eq.setFormula(formulas[0]);
            node = new Node(diagram , new Stub(null, "inputPressureEquation1",  Type.TYPE_EQUATION) );
            node.setRole(eq);
            diagram.put(node);
        }

        public void executeTest(PulseWavePatientInfo info) throws Exception
        {
            Diagram diagram = getDiagram();
            setGlobalParameters(diagram, info);
            simulate(diagram, info);
            writePressureProfiles(info, seriesResult);
            writeStaticData(info, staticResult);
        }
        
        public void executeTest2(PulseWavePatientInfo info) throws Exception
        {
            Diagram diagram = getDiagram();
            this.setInitialPressure(diagram, info.aortalProfile);
            simulate(diagram, info);
            writePressureProfiles(info, seriesResult);
            writeStaticData(info, staticResult);
        }


        public void simulate(Diagram diagram, PulseWavePatientInfo info) throws Exception
        {
            double startTime = System.currentTimeMillis();
            HemodynamicsSimulationEngine engine = createEngine();

            engine.setOutputDir(options.resultFolder + info.getId());
            engine.setDiagram(diagram);
            engine.setInitialTime(0);
            engine.setTimeIncrement(0.001);
            engine.setCompletionTime(COMPLETION_TIME);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                    .setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setUseFullPressureConservation(true);

            diagram.getRole( EModel.class ).getVariable( "integrationSegments" ).setInitialValue( 10 );
            diagram.getRole( EModel.class ).getVariable( "vesselSegments" ).setInitialValue( 10 );
            ArterialBinaryTreeModel model = engine.createModel();
            System.out.println("Prepare: " + ( System.currentTimeMillis() - startTime ) / 1000);
            startTime = System.currentTimeMillis();
            engine.simulate(model, new ResultListener[]{this});
            System.out.println("Simulate: " + ( System.currentTimeMillis() - startTime ) / 1000);
            startTime = System.currentTimeMillis();
            engine = null;
            System.out.println("Postprocess: " + ( System.currentTimeMillis() - startTime ) / 1000);
        }

        public Diagram getDiagram() throws Exception
        {
            CollectionFactory.createRepository(REPOSITORY_PATH);
            return DIAGRAM_PATH.getDataElement(Diagram.class);
        }
        
        public void writePressureProfiles(PulseWavePatientInfo info, File resultFile) throws Exception
        {
            try (BufferedWriter bw = ApplicationUtils.utfAppender( resultFile ))
            {
                double[] times = ArrayUtils.toPrimitive(timeValues.toArray(new Double[timeValues.size()]));
                PressureProfile aorta = info.aortalProfile;
                PressureProfile radial = info.radialProfile;
                double[] aortaOriginal = aorta.getPressures(times);
                double[] radialOriginal = radial.getPressures(times);
                bw.write("\n" +info.getId() + "\t" + "times" + "\t");
                bw.write(DoubleStreamEx.of(times).joining("\t"));
                bw.write("\n" + info.getId() + "\t" + "aorta_original" + "\t");
                bw.write(DoubleStreamEx.of(aortaOriginal).joining("\t"));
                bw.write("\n" + info.getId() + "\t" + "aorta_simulated" + "\t");
                bw.write(DoubleStreamEx.of(aortaValues).joining("\t"));
                bw.write("\n" + info.getId() + "\t" + "radial_original" + "\t");
                bw.write(DoubleStreamEx.of(radialOriginal).joining("\t"));
                bw.write("\n" + info.getId() + "\t" + "radial_simulated" + "\t");
                bw.write(DoubleStreamEx.of(radialValues).joining("\t"));
            }
        }
        
        public void writeStaticData(PulseWavePatientInfo info, File resultFile) throws Exception
        {
            try (BufferedWriter bw = ApplicationUtils.utfAppender(resultFile))
            {
                double age = info.getDoubleValue("age");
                double h = info.getDoubleValue("height");
                double w = info.getDoubleValue("weight");
                double hr = 60_000/info.getDoubleValue("tf");
                double sv = info.getDoubleValue("sv");
                double a_sp = info.getDoubleValue("a_sp");
                double a_dp = info.getDoubleValue("a_dp");
                double r_sp = info.getDoubleValue("r_sp");
                double r_dp = info.getDoubleValue("r_dp");
                double[] a_estimated = PressureTest.getMaxMinPressure(aortaValues);
                double[] r_estimated = PressureTest.getMaxMinPressure(this.radialValues);
                bw.write(info.getId() + "\t"+DoubleStreamEx
                        .of(age, h, w, hr, sv, a_sp, a_dp, r_sp, r_dp, a_estimated[1], a_estimated[0], r_estimated[1], r_estimated[0])
                        .joining("\t") + "\n");
            }
        }

        @Override
        public void start(Object model)
        {
            // TODO Auto-generated method stub
            this.atm = (ArterialBinaryTreeModel)model;
            aortaValues = new ArrayList<>();
            radialValues = new ArrayList<>();
            timeValues = new ArrayList<>();
        }

        ArterialBinaryTreeModel atm;
        @Override
        public void add(double t, double[] y) throws Exception
        {
            if( t > START_SEARCH_TIME && Math.abs(t / 0.01 - Math.round(t / 0.01)) < 0.0001 && t < COMPLETION_TIME )
            {
                aortaValues.add(atm.vesselMap.get(AORTAL).pressure[1]);
                radialValues.add(atm.vesselMap.get(L_RADIAL).pressure[1]);
                timeValues.add(t);
            }
            // TODO Auto-generated method stub
            
        }
        
        List<Double> aortaValues;
        List<Double> radialValues;
        List<Double> timeValues;
    }

    /**
     * Container class which describes patient parameters.
     * @author Ilya
     *
     */
    private static class Options
    {
        public boolean useResistance;
        public Options(String resultFolder, boolean useA, boolean useBeta, boolean useHR, boolean useSV)
        {
            this.name = resultFolder;
            this.resultFolder = TEST_DIR + "/" + resultFolder + "/";
            this.useA0 = useA;
            this.useBeta = useBeta;
            this.useHR = useHR;
            this.useSV = useSV;
        }

        File inputFile = DEFAULT_INPUT_FILE;
        String resultFolder;
        String diagramPath = DIAGRAM_PATH.toString();
        String name;
        boolean useA0 = false;
        boolean useBeta = false;
        boolean useSV = false;
        boolean useHR = false;
        boolean useAllometricR = false;
        boolean separateUpDown = false;
        boolean capillaryConductivity = false;
        String comment = "";
        @Override
        public String toString()
        {
            return String.join("\n", "Input file \t" + inputFile.getAbsolutePath(), "diagramPath \t" + diagramPath, "useA0 \t" + useA0,
                    "useBeta \t" + useBeta, "useHR \t" + useHR, "diagramPath \t" + useAllometricR, "separateUpDown \t" + separateUpDown,
                    "capillaryConductivity \t" + capillaryConductivity, "useSV \t" + useSV, "useAllometricR\t" + useAllometricR,
                    "comment \t" + comment);
        }
    }


}
