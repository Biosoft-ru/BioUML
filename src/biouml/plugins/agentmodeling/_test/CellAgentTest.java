package biouml.plugins.agentmodeling._test;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ru.biosoft.graphics.Pen;
import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.BasicStatCollector;
import biouml.plugins.agentmodeling.CellAgent;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.Scheduler;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;

import com.developmentontheedge.application.ApplicationUtils;


public class CellAgentTest extends TestCase implements ResultListener
{
    final static String COLLECTION_NAME = "databases/agentmodel_test/Diagrams/";
    final static String DIAGRAM_NAME = "ProteinModel_3";
    public final static int POPULATION_SIZE = 200;

    public final static double TIME_INCREMENT = 1;
    public final static double COMPLETION_TIME = 108;

    HashSet<Var> varsToPlot = new HashSet<>();

    HashMap<Integer, XYSeries> indexToVar = new HashMap<>();

    public double proteinDegradation = 0;
    public double rnaDegradation = 0;
    public double proteinCopies = 0;
    public double rnaCopies = 0;
    public double proteinSynthesis = 0;
    public double rnaSynthesis = 0;
    public String nameProtein = "";


    public double arrestProbability = 0.0;
    public double g1Length = 15.5;
    public double g2Length = 4;
    public double sLength = 7;
    public double mLength = 1;



    Span span;

    public Scheduler scheduler;

    JavaSimulationEngine engine = new JavaSimulationEngine();

    private boolean showPlot = true;
    private boolean saveToFile = false;

    public void setShowPlot(boolean showPlot)
    {
        this.showPlot = showPlot;
    }

    public void setSaveToFile(boolean saveToFile)
    {
        this.saveToFile = saveToFile;
    }

    public CellAgentTest(String name)
    {
        super(name);

        varsToPlot.add(new Var("$pm_3", "p1"));
        varsToPlot.add(new Var("$pm_3_m", "p2"));

        varsToPlot.add(new Var("s_stage", "p3"));
        varsToPlot.add(new Var("g1_stage", "p3"));
        varsToPlot.add(new Var("g2_stage", "p3"));
        varsToPlot.add(new Var("m_stage", "p3"));
        varsToPlot.add(new Var("g0_stage", "p3"));
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(CellAgentTest.class.getName());
        suite.addTest(new CellAgentTest("test"));
        return suite;
    }



    private AgentBasedModel generateAgentModel(List<SimulationAgent> agents)
    {
        AgentBasedModel model = new AgentBasedModel();

        for( SimulationAgent agent : agents )
            model.addAgent(agent);

        return model;
    }


    public void test() throws Exception
    {
        Diagram diagram = AgentTestingUtils.loadDiagram(DIAGRAM_NAME, COLLECTION_NAME);
        simulate(diagram);
    }

    public void clearVars()
    {
        varsToPlot.clear();
    }

    public void addVarToFile(String varName, String title, File file)
    {
        varsToPlot.add(new Var(varName, title, file));
    }

    public void addVarToPlot(String varName, String plotName)
    {
        varsToPlot.add(new Var(varName, plotName));
    }

    public void simulate(Diagram diagram) throws Exception
    {
        Model prototypeModel = initModel(diagram);
        AgentBasedModel model = generateAgentModel(generatePopulation(diagram.getName(), prototypeModel, POPULATION_SIZE));

        generateIndexMapping();

        if( showPlot )
            generatePlots();

        AgentModelSimulationEngine engine = new AgentModelSimulationEngine();
        scheduler = (Scheduler)engine.getSimulator();

        //        scheduler.setDebugMode(true);

        if( showPlot )
            scheduler.addStatisticsCollector(new BasicStatCollector());

        engine.setCompletionTime(span.getTimeFinal());
        engine.setTimeIncrement(1);

        engine.simulate(model, new ResultListener[] {});

        if( saveToFile )
            saveToFile();

        //        System.out.println("finisehd");
    }

    private void saveToFile() throws IOException
    {
        double meanValue;
        int sampleSize;
        double sdValue;

        for( Var var : varsToPlot )
        {
            if( var.file == null )
                continue;

            XYSeries series = var.series;
            String title = var.title;

            sampleSize = 0;
            meanValue = 0;
            sdValue = 0;

            for( int i = 0; i < series.getItemCount(); i++ )
            {
                if( series.getX(i).doubleValue() > 84 )
                {
                    double value = series.getY(i).doubleValue();
                    sdValue += value * value;
                    meanValue += value;
                    sampleSize++;
                }
            }

            meanValue /= sampleSize;

            sdValue = sdValue / sampleSize - meanValue * meanValue;

            StringBuilder sb = new StringBuilder();
            sb.append(title);
            sb.append("\t");

            sb.append(meanValue);
            sb.append("\t");

            sb.append(sdValue);
            sb.append("\t");

            //            sb.append(series.getY(0).doubleValue());
            //            sb.append("\t");
            //
            //            double ratio = meanValue / ( series.getY(0).doubleValue() );
            //            sb.append(ratio);
            //            sb.append("\t");

            sb.append(proteinCopies);
            sb.append("\t");

            sb.append(rnaCopies);
            sb.append("\t");

            sb.append(Math.log(2) / proteinDegradation);
            sb.append("\t");

            sb.append(Math.log(2) / rnaDegradation);
            sb.append("\t");

            //            for( int i = 0; i < series.getItemCount(); i++ )
            //            {
            //                if( series.getX(i).doubleValue() > 100 )
            //                {
            //                    sb.append(series.getY(i));
            //                    sb.append("\t");
            //                }
            //            }

            sb.append("\n");
            ApplicationUtils.writeString(new FileOutputStream(var.file, true), sb.toString());
        }
    }
    private Model initModel(Diagram diagram) throws Exception
    {
        engine.setOutputDir("../out");

        span = new ArraySpan(engine.getInitialTime(), COMPLETION_TIME, TIME_INCREMENT);
        for( int i = 1; i < span.getLength(); i++ )
            timePoints.add(span.getTime(i));

        engine.setDiagram(diagram);
        Model prototypeModel = engine.createModel();

        return prototypeModel;
    }

    private List<SimulationAgent> generatePopulation(String baseName, Model prototypeModel, int size) throws Exception
    {
        List<SimulationAgent> result = new ArrayList<>();
        Span span = new ArraySpan(engine.getInitialTime(), COMPLETION_TIME, TIME_INCREMENT);
        for( int i = 0; i < size; i++ )
        {
            Model model = prototypeModel.clone();
            ModelAgent agent = new CellAgent(model, new EventLoopSimulator(), span, baseName + "_" + i, this);
            
            init(model);
            agent.setInitialValues(model.getInitialValues());
            result.add(agent);
        }
        return result;
    }

    private void init(Model model) throws Exception
    {
        Field arrestProbabilityField = model.getClass().getDeclaredField("arrestProbability");
        arrestProbabilityField.setAccessible(true);
        arrestProbabilityField.setDouble(model, arrestProbability);

        Field g1_lengthField = model.getClass().getDeclaredField("g1_length");
        g1_lengthField.setAccessible(true);
        g1_lengthField.setDouble(model, g1Length);


        Field g2_lengthField = model.getClass().getDeclaredField("g2_length");
        g2_lengthField.setAccessible(true);
        g2_lengthField.setDouble(model, g2Length);


        Field s_lengthField = model.getClass().getDeclaredField("s_length");
        s_lengthField.setAccessible(true);
        s_lengthField.setDouble(model, sLength);

        Field proteinDegradationField = model.getClass().getDeclaredField("kdp");
        proteinDegradationField.setAccessible(true);
        proteinDegradationField.setDouble(model, proteinDegradation);

        Field rnaDegradationField = model.getClass().getDeclaredField("kdr");
        rnaDegradationField.setAccessible(true);
        rnaDegradationField.setDouble(model, rnaDegradation);

        Field proteinSynthesisField = model.getClass().getDeclaredField("ksp");
        proteinSynthesisField.setAccessible(true);
        proteinSynthesisField.setDouble(model, proteinSynthesis);

        Field rnaSynthesisField = model.getClass().getDeclaredField("vsr");
        rnaSynthesisField.setAccessible(true);
        rnaSynthesisField.setDouble(model, rnaSynthesis);

        int proteinIndex = engine.getVariableRateIndex("$pm_3");
        int rnaIndex = engine.getVariableRateIndex("$pm_3_m");

        model.getInitialValues()[proteinIndex] = proteinCopies;
        model.getInitialValues()[rnaIndex] = rnaCopies;
    }

//    private void randomize(Random r, Model model) throws Exception
//    {
//        time = 27.5 * r.nextDouble();
//
//        double g1_stage = 0;
//        double g2_stage = 0;
//        double m_stage = 0;
//        double s_stage = 0;
//        double g0_stage = 0;
//        if( time < g1Length )
//        {
//            state = engine.getStateIndex("math-state_4");
//            stateTime = -time;
//            h = 1;
//            g1_stage = 1;
//        }
//        else if( time < g1Length + sLength )
//        {
//            state = engine.getStateIndex("math-state_2");
//            stateTime = g1Length - time;
//            h = 0.7;
//            s_stage = 1;
//        }
//        else if( time < g1Length + sLength + g2Length )
//        {
//            state = engine.getStateIndex("math-state_3");
//            stateTime = g1Length + sLength - time;
//            h = 2;
//            g2_stage = 1;
//        }
//        else if( time < g1Length + sLength + g2Length + mLength )
//        {
//            state = engine.getStateIndex("math-state_0");
//            stateTime = g1Length + sLength + g2Length - time;
//            h = 0;
//            m_stage = 1;
//        }
//        else
//        {
//            state = engine.getStateIndex("math-state_4");
//            stateTime = 0;
//            h = 1;
//            g1_stage = 1;
//        }
//
//        if( r.nextDouble() < arrestProbability )
//        {
//            g1_stage = 0;
//            g2_stage = 0;
//            m_stage = 0;
//            s_stage = 0;
//            g0_stage = 1;
//            state = engine.getStateIndex("math-state_1");
//        }
//
//        Field hField = model.getClass().getDeclaredField("h");
//        hField.setAccessible(true);
//        hField.setDouble(model, h);
//
//        Field currentStateField = model.getClass().getDeclaredField("currentState");
//        currentStateField.setAccessible(true);
//        currentStateField.setDouble(model, state);
//
//        Field currentStateTimeField = model.getClass().getDeclaredField("currentStateTime");
//        currentStateTimeField.setAccessible(true);
//        currentStateTimeField.setDouble(model, stateTime);
//
//        Field s_stageField = model.getClass().getDeclaredField("s_stage");
//        s_stageField.setAccessible(true);
//        s_stageField.setDouble(model, s_stage);
//
//
//        Field g1_stageField = model.getClass().getDeclaredField("g1_stage");
//        g1_stageField.setAccessible(true);
//        g1_stageField.setDouble(model, g1_stage);
//
//
//        Field g2_stage_Field = model.getClass().getDeclaredField("g2_stage");
//        g2_stage_Field.setAccessible(true);
//        g2_stage_Field.setDouble(model, g2_stage);
//
//
//        Field m_stageField = model.getClass().getDeclaredField("m_stage");
//        m_stageField.setAccessible(true);
//        m_stageField.setDouble(model, m_stage);
//
//        Field g0_stageField = model.getClass().getDeclaredField("g0_stage");
//        g0_stageField.setAccessible(true);
//        g0_stageField.setDouble(model, g0_stage);
//    }
    HashMap<Double, double[]> sumValues = new HashMap<>();
    HashMap<Double, Integer> sumSize = new HashMap<>();

    HashSet<Double> timePoints = new HashSet<>();

    @Override
    public void add(double t, double[] y) throws Exception
    {

        if( !timePoints.contains(t) )
            return;

        if( sumValues.containsKey(t) )
        {
            int size = sumSize.get(t) + 1;
            double[] sum = sumValues.get(t);
            for( int i = 0; i < sum.length; i++ )
            {
                sum[i] += y[i];
                XYSeries series = findSeries(i);
                if( series != null )
                    series.update(t, sum[i] / size);
            }
            sumSize.put(t, size);
        }
        else
        {
            sumValues.put(t, y);
            sumSize.put(t, 1);
            for( int i = 0; i < y.length; i++ )
            {
                XYSeries series = findSeries(i);
                if( series != null )
                    series.add(t, y[i]);
            }
        }
        clearHistory();
    }

    private XYSeries findSeries(int i)
    {
        return indexToVar.get(i);
    }

    private void generateIndexMapping()
    {
        indexToVar = new HashMap<>();
        for( Var var : varsToPlot )
        {
            if( !engine.getVarIndexMapping().containsKey(var.name) )
            {
                System.out.println("WARNING:  variable " + var.name + " not found");
            }
            else
            {
                Integer index = engine.getVarIndexMapping().get(var.name);
                indexToVar.put(index, var.series);
            }
        }
    }

    private void clearHistory()
    {
        Set<Double> keyset = new HashSet<>( sumValues.keySet() );
        for( double val : keyset )
        {
            if( val < scheduler.memorizedTime )
            {
                sumValues.remove(val);
                sumSize.remove(val);
            }
        }
    }

    @Override
    public void start(Object model)
    {

    }


    public void generatePlots()
    {
        HashSet<String> plotNames = new HashSet<>();
        for( Var var : varsToPlot )
        {
            if( var.plotName != null )
                plotNames.add(var.plotName);
        }

        for( String plotName : plotNames )
            generatePlot(plotName);
    }

    public void generatePlot(String plotName)
    {
        JFrame frame = new JFrame(plotName);
        Container content = frame.getContentPane();
        XYSeriesCollection collection = new XYSeriesCollection();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        int i = 0;
        for( Var var : varsToPlot )
        {
            if( plotName.equals(var.plotName) )
            {
                collection.addSeries(var.series);
                renderer.setSeriesShapesVisible(i, false);
                i++;
            }
        }
        JFreeChart chart = ChartFactory.createXYLineChart(plotName, "Time", "", collection, PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
                );

        chart.getXYPlot().setRenderer(renderer);
        chart.getXYPlot().setBackgroundPaint(Color.white);
        chart.setBackgroundPaint(Color.white);
        content.add(new ChartPanel(chart));
        frame.setSize(800, 600);
        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }

    public static class Var
    {
        String name;
        String title;
        Pen pen;
        XYSeries series;
        File file;
        String plotName;

        public Var(String name, File file)
        {
            this.name = name;
            this.file = file;
            series = new XYSeries(name);
        }

        public Var(String name, String title, File file)
        {
            this.name = name;
            this.title = title;
            this.file = file;
            series = new XYSeries(name);
        }
        public Var(String name, String plotName)
        {
            this.name = name;
            this.plotName = plotName;
            series = new XYSeries(name);
        }

        public Var(String name, String plotName, Pen pen)
        {
            this(name, plotName);
            this.pen = pen;
        }
    }
}
