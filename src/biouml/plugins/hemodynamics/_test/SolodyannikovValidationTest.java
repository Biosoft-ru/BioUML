package biouml.plugins.hemodynamics._test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class SolodyannikovValidationTest extends TestCase
{
    private static final String REPOSITORY_PATH = "../data";
//    private static final DataElementPath DIAGRAM_PATH = DataElementPath.create("databases/Virtual Human/Diagrams/Solodyannikov 1994");
    private static final DataElementPath DIAGRAM_PATH = DataElementPath.create("databases/Virtual Human/Diagrams/Solodyannikov 2006");
    private static final File TEST_DIR = new File("C:/VP/Solodyannikov/");
    private static final File DEFAULT_INPUT_FILE = new File(TEST_DIR + "/VPData BSA_HR.txt");

    private static final int THREAD_NUMBER = 1;

    private static final int COMPLETION_TIME = 6;
    private static final int START_SEARCH_TIME = 4;
    
    public SolodyannikovValidationTest()
    {
        super("test");
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(SolodyannikovValidationTest.class.getName());
        suite.addTest(new SolodyannikovValidationTest());
        return suite;
    }

    public void test() throws Exception
    {
        Options options = new Options("M", true, true, true, true);
        doTest(options);
    }

    private ConcurrentLinkedQueue<GenericPatientInfo> infos;
    private String[] header;

    public void readData(File f) throws IOException
    {
        infos = new ConcurrentLinkedQueue<>();
        try (BufferedReader br = ApplicationUtils.utfReader(f))
        {
            String line = br.readLine();
            header = line.split(TAB_DELIMITER);
            while( (line = br.readLine()) != null )
                infos.add(new GenericPatientInfo(line, header));
        }
    }

    public void doTest(Options options) throws Exception
    {
        readData(options.inputFile);

        new File(options.resultFolder).mkdirs();
        File resultFile = new File(options.resultFolder + "//" + options.name + ".txt");
        File optionsFile = new File(options.resultFolder + "//" + options.name + "_info.txt");

        try (BufferedWriter bw = ApplicationUtils.utfAppender(resultFile);
                BufferedWriter bwOptions = ApplicationUtils.utfAppender(optionsFile);)
        {
            bw.write(StringUtils.join(header, TAB_DELIMITER) + LINE_BREAK);
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

    public synchronized Model getModel(SimulationEngine engine) throws Exception
    {
        return engine.createModel();
    }

    public synchronized SimulationEngine createEngine() throws Exception
    {
        return new JavaSimulationEngine();
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
            GenericPatientInfo info = infos.poll();
            while( info != null )
            {
                try
                {
                    if( info.isValid() )
                        simulate(info);
                    writeResult(info, resultFile);
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

        private void prepareDiagram(Diagram diagram, GenericPatientInfo info)
        {
//            Double value = (Double)info.getValue("R (BSA)");
            EModel emodel = diagram.getRole( EModel.class );
//            emodel.getVariable("Resistance_Capillary").setInitialValue(value);
//            emodel.getVariable("Y_ALVL0").setInitialValue(1/value);
            
            Double mass = (Double)info.getValue("wei");
            emodel.getVariable("m").setInitialValue(mass);
        }

        public void simulate(GenericPatientInfo info) throws Exception
        {
            System.out.println("Simulation for patient: " + info.getId());
            double startTime = System.currentTimeMillis();
            SimulationEngine engine = createEngine();
            engine.setLogLevel( Level.SEVERE );
            Diagram diagram = getDiagram();
            
            System.out.println("Number of events: "+StreamEx.of(diagram.getNodes()).map(n->n.getRole()).select(Event.class).count());
            
            prepareDiagram(diagram, info);

            engine.setDiagram(diagram);
            engine.setInitialTime(0);
            engine.setTimeIncrement(0.001);
            engine.setCompletionTime(COMPLETION_TIME);
            JVodeSolver solver = new JVodeSolver();
            solver.getOptions().setAtol(1E-8);
            engine.setSolver(solver);
            
            Model model = engine.createModel();
//            index = engine.getVarIndexMapping().get("Pressure_Arterial");
            index = engine.getVarIndexMapping().get("P_AL");
            System.out.println("Model prepared, elapsed time: " + ( System.currentTimeMillis() - startTime )/1000+" s");
            startTime = System.currentTimeMillis();
            engine.simulate(model, new ResultListener[] {this});
            System.out.println("Simulation finished, elapsed time" + ( System.currentTimeMillis() - startTime )/1000 +" s");
            engine = null;
            
            double[] result = getMaxMinPressure(pressure);
            info.addProoperty("PS_calc", result[1]);
            info.addProoperty("PD_calc", result[0]);
        }

        public void writeResult(GenericPatientInfo info, File f) throws IOException
        {
            try (BufferedWriter bw = ApplicationUtils.utfAppender(f))
            {
                bw.write(info.toString() + "\n");
            }
        }
        
        @Override
        public void start(Object model)
        {
            pressure = new ArrayList<>();
        }

        private int index;
        private List<Double> pressure;
        @Override
        public void add(double t, double[] y) throws Exception
        {
            if( t > START_SEARCH_TIME && Math.abs(t / 0.01 - Math.round(t / 0.01)) < 0.0001 && t < COMPLETION_TIME ) // t is 4.01, 4.02, 4.03,....
            {
                pressure.add(y[index]);
            }
        }

        public Diagram getDiagram() throws Exception
        {
            CollectionFactory.createRepository(REPOSITORY_PATH);
            return DIAGRAM_PATH.getDataElement(Diagram.class);
        }

    }

    private double[] getMaxMinPressure(List<Double> pressure)
    {
        Collections.sort(pressure);
        return new double[] {pressure.get(0), pressure.get(pressure.size() - 1)};
    }


    public static final String TAB_DELIMITER = "\t";
    public static final String LINE_BREAK = "\n";

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
