package biouml.plugins.agentmodeling._test;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.JFrame;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling._test.CellAgentTest.Var;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;

import com.developmentontheedge.application.ApplicationUtils;

public class CellAgentTestDif extends TestCase implements ResultListener
{
    static final String repositoryPath = "../data";
    static final String COLLECTION_NAME = "databases/agentmodel_test/Diagrams/";
    static final String DIAGRAM_NAME = "ProteinModelDifSim4";
    
    public final static double TIME_INCREMENT = 1;
    public final static double COMPLETION_TIME = 108;
    public double h = 1.0;
    public double arrestProbability = 0.12;
    public double tcc = 27.5;
        
    
    public double proteinDegradation = 0;
    public double rnaDegradation = 0;
    public double proteinCopies = 0;
    public double rnaCopies = 0;
    public double proteinSynthesis = 0;
    public double rnaSynthesis = 0;

    public boolean showPlot = true;
    public boolean saveToFile = false;
    
    int rnaIndex;
    int proteinIndex;
    
    Map<String, Integer> varIndexmapping;
    HashSet<Var> varsToView = new HashSet<>();
    HashMap<Integer, XYSeries> indexToVar = new HashMap<>();
    
    public CellAgentTestDif(String name)
    {
        super(name);

        varsToView.add(new Var("$Protein", "fig-PRT"));
        varsToView.add(new Var("$RNA", "fig-RNA"));
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(CellAgentTestDif.class.getName());
        suite.addTest(new CellAgentTestDif("test"));
        return suite;
    }
    
    JavaSimulationEngine engine;
    Model model;
    
    public void test() throws Exception
    {
        prepare();
        simulate();
    }
    
    public void prepare() throws Exception
    {
        Diagram diagram = AgentTestingUtils.loadDiagram(DIAGRAM_NAME, COLLECTION_NAME);
        engine = new JavaSimulationEngine();
        engine.setSpan(new ArraySpan(engine.getInitialTime(), COMPLETION_TIME, TIME_INCREMENT));
        model = generateModel(diagram, engine);
    }
    
    public void simulate() throws Exception
    {
        init(model);
        generateIndexMapping();
        
        if (showPlot)
        generatePlots();
        engine.simulate(model, new ResultListener[] {this});
        
        if( saveToFile )
            saveToFile();
        
        if(showPlot)
        {
            try
            {
                Thread.sleep(10000);
                // any action
            }
            catch( InterruptedException e )
            {
                e.printStackTrace();
            }
        }
    }
    private void saveToFile() throws IOException
    {
        double meanValue;
        int sampleSize;
        double sdValue;

        for( Var var : varsToView )
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

            String result = StringUtils.join(new Object[] {title, meanValue, sdValue, proteinCopies, rnaCopies,
                    Math.log(2) / proteinDegradation, Math.log(2) / rnaDegradation}, "\t")
                    + "\n";
            ApplicationUtils.writeString(new FileOutputStream(var.file, true), result);
        }
    }

    public Model generateModel(Diagram diagram, JavaSimulationEngine engine) throws Exception
    {
        engine.setOutputDir("../out");
        engine.setDiagram(diagram);
        Model model = engine.createModel();
        varIndexmapping = engine.getVarIndexMapping();
        rnaIndex = engine.getVariableRateIndex("$RNA");
        proteinIndex= engine.getVariableRateIndex("$Protein");
        model.init();
        return model;
    }

    private void init(Model model) throws Exception
    {
        HashMap<String, Double> map = new HashMap<>();
        map.put("arrestProbability", arrestProbability);
        map.put("h", h);
        map.put("tcc", tcc);
        map.put("kdp", proteinDegradation);
        map.put("kdr", rnaDegradation);
        map.put("ksp", proteinSynthesis);
        map.put("vsr", rnaSynthesis);
         
        double[] values = new double[2];
        values[proteinIndex] = proteinCopies;
        values[rnaIndex] = rnaCopies;
        
        model.init(values, map);
    }
    
    public void generatePlots()
    {
        HashSet<String> plotNames = new HashSet<>();
        for( Var var : varsToView )
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
        for( Var var : varsToView )
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
    
    @Override
    public void start(Object model)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        for( int i = 0; i < y.length; i++ )
        {
            XYSeries series = findSeries(i);
            if( series != null )
                series.add(t, y[i]);
        }
    }
    
    private void generateIndexMapping()
    {
        indexToVar = new HashMap<>();
        for( Var var : varsToView )
        {
            if( !varIndexmapping.containsKey(var.name) )
            {
                System.out.println("WARNING:  variable " + var.name + " not found");
            }
            else
            {
                Integer index = varIndexmapping.get(var.name);
                indexToVar.put(index, var.series);
            }
        }
    }
    
    private XYSeries findSeries(int i)
    {
        return indexToVar.get(i);
    }
    
    public void clearVars()
    {
        varsToView.clear();
    }
    
    public void addVarToFile(String varName, String title, File file)
    {
        varsToView.add(new Var(varName, title, file));
    }

}
