package biouml.plugins.hemodynamics._test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.dynamics.EModel;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsEModel;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.Vessel;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.SuppressHuntBugsWarning;

@SuppressHuntBugsWarning({"Field*"})
public class PressureTest extends TestCase
{
    private static final String REPOSITORY_PATH = "../data";
    private static final String L_BRACHIAL = "L. Brachial";
    private static final String R_BRACHIAL = "R. Brachial";
    private static final String R_POSTERIOR_TIBIAL = "R. posterior Tibial";
    private static final String L_POSTERIOR_TIBIAL = "L. posterior Tibial";
    private static final String R_ANTERIOR_TIBIAL = "R. anterior Tibial";
    private static final String L_ANTERIOR_TIBIAL = "L. anterior Tibial";
    private static final String HEART_RATE = "Heart_Rate";
    private static final String AREA_FACTOR = "factorArea";
    private static final String AREA_FACTOR_2 = "factorArea2";
    private static final String BETA_FACTOR = "factorBeta";
    private static final String BETA_FACTOR_2 = "factorBeta2";
    private static final DataElementPath DIAGRAM_PATH = DataElementPath.create("databases/Virtual Human/Diagrams/Arterial Brachial new");
//    private static final DataElementPath DIAGRAM_PATH = DataElementPath.create("databases/Virtual Human/Diagrams/Arterial Tree Equal RH");
    
    private static final File TEST_DIR = new File("C:/VP/New/");
    private static final File DEFAULT_INPUT_FILE = new File(TEST_DIR+"/VPData BSA_HR.txt");

    private static final int THREAD_NUMBER = 4;

    private static final int COMPLETION_TIME = 6;
    private static final int START_SEARCH_TIME = 4;

    public PressureTest()
    {
        super("test");
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(PressureTest.class.getName());
        suite.addTest(new PressureTest());
        return suite;
    }

    public void test() throws Exception
    {
//        Options options = new Options("A+B+HR+SV", true, true, true, true);
//        options.comment = "test results";
//        doTest(options);
    	
//    	Options options = new Options("BSA_HR", false, false, false, true);
//        options.comment = "SV = BSA / HR";
//        doTest(options);

//    	Options options = new Options("RH_A_B_HR_SV", true, true, true, false);
//      options.comment = "SV = BSA / HR";
//      doTest(options);
    	
//    	Options options = new Options("Allometric A B Hr SV", true, true, true, true);
////    	options.capillaryConductivity = true;
//    	options.useAllometricR = true;
//        doTest(options);
    	
    	Options options = new Options("R A B updown SV HR", true, true, true, true);
    	options.useResistance = true;
//    	options.comment= "keep original vessel parameters";
//	options.capillaryConductivity = true;
//	options.useAllometricR = true;
//    	options.comment = "HR = 90 for all";
    doTest(options);
    }


    private ConcurrentLinkedQueue<PatientInfo> infos;

    // private double referencedRate;
    // 1396
    private static double referencedA02 = 0.147319;
    private static double referencedA0 = 0.153153;
    private final double referencedBeta2 = 1101855;
    private final double referencedBeta = 2218347;
    private final double referencedHeight = 181;
    private final double referencedPS = 129;
    private final double referencedPD = 81;
    private static double referencedWeight = 86;
    private final double referencedResistance = 113.983;

    public void readData(File f) throws IOException
    {
        infos = new ConcurrentLinkedQueue<>();
        try (BufferedReader br = ApplicationUtils.utfReader(f))
        {
            String line = br.readLine();
            line = br.readLine();

            while( line != null )
            {
                infos.add(new PatientInfo(line));
                line = br.readLine();
            }
        }
    }

    public void doTest(Options options) throws Exception
    {

        readData(options.inputFile);

        new File(options.resultFolder).mkdirs();
        File resultFile = new File(options.resultFolder + "//"+options.name+".txt");
        File optionsFile = new File(options.resultFolder + "//"+options.name+"_info.txt");

        try (BufferedWriter bw = ApplicationUtils.utfAppender(resultFile);
                BufferedWriter bwOptions = ApplicationUtils.utfAppender(optionsFile);)
        {
            bw.write(PatientInfo.getDescription() + "\n");
            bwOptions.write(options.toString());
        }

        SimulationThread[] threads = new SimulationThread[THREAD_NUMBER];
        for( int i = 0; i < THREAD_NUMBER; i++ )
        {
            threads[i] = new SimulationThread(options, resultFile);
            threads[i].start();
        }

        for( int i = 0; i < THREAD_NUMBER; i++ )
            threads[i].join();
    }

    public synchronized ArterialBinaryTreeModel getModel(HemodynamicsSimulationEngine engine) throws Exception
    {
        return engine.createModel();
    }

    public synchronized HemodynamicsSimulationEngine createEngine() throws Exception
    {
        return new HemodynamicsSimulationEngine();
    }

    private class SimulationThread extends Thread implements ResultListener
    {
        Options options;
        File resultFile;
        public SimulationThread(Options options, File resultFile)
        {
            this.options = options;
            this.resultFile = resultFile;
        }

        @Override
        public void run()
        {
            PatientInfo info = infos.poll();
            System.out.println("Simulate: " + info.patId);
            while( info != null )
            {
                try
                {
                    if( info.isValid )
                        simulate(info);
                    writeResult(info, resultFile);
                }
                catch( Exception ex )
                {
                    System.out.println("Error on patient: " + info.patId);
                    ex.printStackTrace();
                }
                info = infos.poll();
            }
            System.out.println("Finished");
        }

        private void prepareDiagram(Diagram diagram, PatientInfo info)
        {
            HemodynamicsEModel emodel = diagram.getRole( HemodynamicsEModel.class );
            if( options.useBeta )
            {
                emodel.getVariable(BETA_FACTOR).setInitialValue(info.beta / referencedBeta);
                emodel.getVariable(BETA_FACTOR_2).setInitialValue(info.beta2 / referencedBeta2);
            }

            if( options.useA0 )
            {
                emodel.getVariable(AREA_FACTOR).setInitialValue(info.a0 / referencedA0);
                emodel.getVariable(AREA_FACTOR_2).setInitialValue(info.a02 / referencedA02);
            }
            if( options.useHR )
                emodel.getVariable("T_C").setInitialValue(60.0 / info.heartRate);

            if( options.useSV )
                emodel.getVariable("SV").setInitialValue(info.strokeVolume);
            
            if (options.useAllometricR)
                emodel.getVariable("capillaryResistance").setInitialValue(Math.pow(info.weight/70, 0.75));
            
            if (options.useResistance)
            	emodel.getVariable("capillaryResistance").setInitialValue(info.resistance);
            
            if (options.capillaryConductivity)
            	emodel.getVariable("capillaryConductivityFactor").setInitialValue(2);
            //           setWeightFactor(info.weight / referencedWeight, emodel);
            //           setAreaFactor(info.a0 / referencedA0, emodel);
            //           setAreaFactor2(info.a02 / referencedA02, emodel);
            //                      setHeartRate(info.heartRate);
            // setLengthFactor(length[i]);
            //                      setWeightFactor(info.weight);

            //default values
            //            hands
            Edge e = (Edge)diagram.get("Vessel_37");
            Vessel v = (Vessel)e.getAttributes().getValue("vessel");
            v.setBeta(referencedBeta);
            v.setInitialArea(referencedA0);

            e = (Edge)diagram.get("Vessel_42");
            v = (Vessel)e.getAttributes().getValue("vessel");
            v.setBeta(referencedBeta);
            v.setInitialArea(referencedA0);

            e = (Edge)diagram.get("Vessel_50");
            v = (Vessel)e.getAttributes().getValue("vessel");
            v.setBeta(referencedBeta);
            v.setInitialArea(referencedA0);

            e = (Edge)diagram.get("Vessel_55");
            v = (Vessel)e.getAttributes().getValue("vessel");
            v.setBeta(referencedBeta);
            v.setInitialArea(referencedA0);

            //legs
            e = (Edge)diagram.get("Vessel_18");
            v = (Vessel)e.getAttributes().getValue("vessel");
            v.setBeta(referencedBeta2);
            v.setInitialArea(2 * referencedA02 / 3);

            e = (Edge)diagram.get("Vessel_25");
            v = (Vessel)e.getAttributes().getValue("vessel");
            v.setBeta(referencedBeta2);
            v.setInitialArea(2 * referencedA02 / 3);

            e = (Edge)diagram.get("Vessel_19");
            v = (Vessel)e.getAttributes().getValue("vessel");
            v.setBeta(referencedBeta2);
            v.setInitialArea(referencedA02 / 3);

            e = (Edge)diagram.get("Vessel_26");
            v = (Vessel)e.getAttributes().getValue("vessel");
            v.setBeta(referencedBeta2);
            v.setInitialArea(referencedA02 / 3);
        }

        public void simulate(PatientInfo info) throws Exception
        {
            double startTime = System.currentTimeMillis();
            HemodynamicsSimulationEngine engine = createEngine();//new HemodynamicsSimulationEngine();
            Diagram diagram = getDiagram();
            prepareDiagram(diagram, info);

            engine.setOutputDir(options.resultFolder + info.patId);
            engine.setDiagram(diagram);
            engine.setInitialTime(0);
            engine.setTimeIncrement(0.001);
            engine.setCompletionTime(COMPLETION_TIME);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                    .setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setUseFullPressureConservation(true);

            diagram.getRole( EModel.class ).getVariable( "integrationSegments" ).setInitialValue( 10 );
            diagram.getRole( EModel.class ).getVariable( "vesselSegments" ).setInitialValue( 10 );
            ArterialBinaryTreeModel model = getModel(engine);

            engine.setResultAllVessels(true);

            System.out.println("Prepare: " + ( System.currentTimeMillis() - startTime ));
            startTime = System.currentTimeMillis();
            engine.simulate(model, new ResultListener[] {this});
            System.out.println("Simulate: " + ( System.currentTimeMillis() - startTime ));
            startTime = System.currentTimeMillis();
            engine = null;
            double[] maxminLeft = getMaxMinPressure(brachialLeftValues);
            double[] maxminRight = getMaxMinPressure(brachialRightValues);
            double[] maxminPost = getMaxMinPressure(pTibialValues);
            double[] maxminAnt = getMaxMinPressure(aTibialValues);
            info.ps = maxminLeft[1];
            info.pd = maxminLeft[0];
            info.ps_right = maxminRight[1];
            info.pd_right = maxminRight[0];
            info.ps_ptibial_left = maxminPost[1];
            info.pd_ptibial_left = maxminPost[0];
            info.ps_atibial_left = maxminAnt[1];
            info.pd_atibial_left = maxminAnt[0];
            
            new File(options.resultFolder + info.patId).delete();
            System.out.println("Postprocess: " + ( System.currentTimeMillis() - startTime ));

        }

        public void writeResult(PatientInfo info, File f) throws IOException
        {
            try (BufferedWriter bw = ApplicationUtils.utfAppender(f))
            {
                bw.write(info.toString() + "\n");
            }
        }

        List<Double> brachialLeftValues;
        List<Double> brachialRightValues;
        List<Double> pTibialValues;
        List<Double> aTibialValues;

        ArterialBinaryTreeModel model;

        @Override
        public void start(Object model)
        {
            brachialLeftValues = new ArrayList<>();
            brachialRightValues = new ArrayList<>();
            pTibialValues = new ArrayList<>();
            aTibialValues = new ArrayList<>();
            this.model = (ArterialBinaryTreeModel)model;
        }

        @Override
        public void add(double t, double[] y) throws Exception
        {
            if( t > START_SEARCH_TIME && Math.abs(t / 0.01 - Math.round(t / 0.01)) < 0.0001 && t < COMPLETION_TIME ) // t is 4.01, 4.02, 4.03,....
            {
                brachialLeftValues.add(model.vesselMap.get(L_BRACHIAL).pressure[1]);
                brachialRightValues.add(model.vesselMap.get(R_BRACHIAL).pressure[1]);
                pTibialValues.add(model.vesselMap.get(L_POSTERIOR_TIBIAL).pressure[1]);
                aTibialValues.add(model.vesselMap.get(L_ANTERIOR_TIBIAL).pressure[1]);
            }
        }

        public Diagram getDiagram() throws Exception
        {
            CollectionFactory.createRepository(REPOSITORY_PATH);
            return DIAGRAM_PATH.getDataElement(Diagram.class);
        }

    }

    public static double[] getMaxMinPressure(List<Double> pressure)
    {
        Collections.sort(pressure);
        return new double[] {pressure.get(0), pressure.get(pressure.size() - 1)};
    }

    /**
     * Container class which describes patient parameters.
     * @author Ilya
     *
     */
    public static class PatientInfo
   {

       public PatientInfo(String str)
       {
           String[] data = str.split("\t");
           int i = 0;
           patId = read(data[i++]);
           age = read(data[i++]);
           height = read(data[i++]);
           weight = read(data[i++]);
           heartRate = read(data[i++]);
           strokeVolume = read(data[i++]);
           resistance = read(data[i++]);
           a0 = read(data[i++]);
           beta = read(data[i++]);
           a02 = read(data[i++]);
           beta2 = read(data[i++]);

           bmi = weight / ( height * height / 10000 );
           
           originalPS = read(data[i++]);
           originalPD = read(data[i++]);
           originalPP = originalPS - originalPD;
           
           originalPS_leg = read(data[i++]);
           originalPD_leg = read(data[i++]);
           originalPP_leg = originalPS_leg - originalPD_leg;

           if( data.length <= i )
               return;

           ps = read(data[i++]);
           pd = read(data[i++]);

           pp = ps - pd;
           
           ps_right = read(data[i++]);
           pd_right = read(data[i++]);
           
           
           pp_right = ps_right - pd_right;
           
           ps_ptibial_left = read(data[i++]);
           pd_ptibial_left = read(data[i++]);
           pp_ptibial_left = ps_ptibial_left - pd_ptibial_left;
           
           ps_atibial_left = read(data[i++]);
           pd_atibial_left = read(data[i++]);
           pp_atibial_left = ps_atibial_left - pd_atibial_left;

//           if( data.length <= i ) //left hand added
//               return;
//
//           originalPS_right = read(data[i++]);
//           originalPD_right = read(data[i++]);
//           originalPP_right = originalPS_right - originalPD_right;
           
       }
       
       public static String getDescription()
       {
           return String.join("\t", "patID", "age", "height", "weight", "heart rate", "stroke volume", "resistance", "a0",
                   "beta", "a0 legs", "beta legs", "originalPS", "originalPD", "originalPS_leg", "originalPD_leg", "ps", "pd", "rightPS",
                   "rightPD", "postPS", "postPD", "antPS", "antPD");
       }

       @Override
    public String toString()
       {
           return StreamEx.of(patId, age, height, weight, heartRate, strokeVolume, resistance, a0, beta, a02, beta2,
                   originalPS, originalPD, originalPS_leg, originalPD_leg, ps, pd, ps_right, pd_right, ps_ptibial_left, pd_ptibial_left,
                   ps_atibial_left, pd_atibial_left).joining("\t");
       }

       private double read(String str)
       {
           try
           {
               str = str.replaceAll(",", ".");
               double val = Double.parseDouble(str);
               if( Double.isNaN(val) )
                   isValid = false;
               return val;
           }
           catch( Exception ex )
           {
               isValid = false;
               return Double.NaN;
           }

       }

       boolean isValid = true;

       double age;
       double patId;
       double heartRate;
       double a0;
       double beta;
       double a02;
       double beta2;
       double strokeVolume;
       double resistance;
       double originalPS;
       double originalPD;
       double originalPP;
       double originalPS_right;
       double originalPD_right;
       double originalPP_right;
       double originalPS_leg;
       double originalPD_leg;
       double originalPP_leg;
       double ps;
       double pd;
       double pp;
       double ps_right;
       double pd_right;
       double pp_right;
       double ps_ptibial_left;
       double pd_ptibial_left;
       double pp_ptibial_left;
       double ps_atibial_left;
       double pd_atibial_left;
       double pp_atibial_left;
       double height;
       double weight;
       double bmi;

   }

    private static class Options
    {
        public boolean useResistance;
		public Options(String resultFolder, boolean useA, boolean useBeta, boolean useHR, boolean useSV)
        {
			this.name = resultFolder;
            this.resultFolder = TEST_DIR+"/"+resultFolder+"/";
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
            return String.join( "\n", "Input file \t" + inputFile.getAbsolutePath(), "diagramPath \t" + diagramPath, "useA0 \t" + useA0,
                    "useBeta \t" + useBeta, "useHR \t" + useHR, "diagramPath \t" + useAllometricR, "separateUpDown \t" + separateUpDown,
                    "capillaryConductivity \t" + capillaryConductivity, "useSV \t" + useSV, "useAllometricR\t" + useAllometricR,
                    "comment \t" + comment );
        }
    }
}
