package biouml.plugins.agentmodeling._test;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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


public class CellAgentTestGD extends TestCase implements ResultListener
{
    final static String COLLECTION_NAME = "databases/agentmodel_test/Diagrams/";
    final static String DIAGRAM_NAME = "ProteinModel";
    public final static int POPULATION_SIZE = 200;

    public final static double TIME_INCREMENT = 1;
    public final static double COMPLETION_TIME = 108;

    public double proteinCopies = 0;
    public double rnaCopies = 0;
    public double rnaHL = 0;
    public double proteinHL = 0;


    HashSet<Var> varsToPlot = new HashSet<>();

    HashMap<Integer, XYSeries> indexToVar = new HashMap<>();

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

    public CellAgentTestGD(String name)
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
        TestSuite suite = new TestSuite(CellAgentTestGD.class.getName());
        suite.addTest(new CellAgentTestGD("test"));
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

    public void addVarToFile(String varName, File file)
    {
        varsToPlot.add(new Var(varName, file));
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

        engine.setCompletionTime(span.getTimeFinal());
        engine.setTimeIncrement(1);

        engine.simulate(model, new ResultListener[] {});

        if( saveToFile )
            saveToFile();
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

            String name = var.name;
            XYSeries series = var.series;

            sampleSize = 0;
            meanValue = 0;
            sdValue = 0;

            for( int i = 0; i < series.getItemCount(); i++ )
            {
                if( series.getX(i).doubleValue() > COMPLETION_TIME - 27.5 )
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
            sb.append(name);
            sb.append("\t");

            sb.append(meanValue);
            sb.append("\t");

            sb.append(sdValue);
            sb.append("\t");

            if( name.contains("_m") )
            {
                sb.append(rnaCopies);
                sb.append("\t");

                sb.append(rnaHL);
                sb.append("\t");
            }
            else
            {
                sb.append(proteinCopies);
                sb.append("\t");

                sb.append(proteinHL);
                sb.append("\t");
            }
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
        //        Random r = new Random();

        for( int i = 0; i < size; i++ )
        {
            Model model = prototypeModel.clone();
            ModelAgent agent = new CellAgent(model, new EventLoopSimulator(), span, baseName + "_" + i, this);
            //            agent.setInitialValues(model.getInitialValues());
            result.add(agent);
        }
        return result;
    }

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
        Set<Double> keyset = new HashSet<>(sumValues.keySet());
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
