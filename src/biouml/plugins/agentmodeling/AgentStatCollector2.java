package biouml.plugins.agentmodeling;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.agentmodeling.StatCollector;
import biouml.plugins.agentmodeling.covid19.HealthCare;

/**
 * Collector intended to collect data from one given agent
 * @author axec
 */
public class AgentStatCollector2 extends StatCollector
{
    private HashMap<String, Integer> nameToIndex;
    private String[] names;
    private double[][] values;
    private int timeIndex = 0;
    private SimulationAgent agent;
    private boolean allVariables = true;

    public double[] getDynamic(String name)
    {
        int index = nameToIndex.get( name );
        return values[index];
    }
    
    private Map<Integer, XYSeries> variablesToPlot = new HashMap<>();

    public void createPlot(String plotName, String[] varNames) throws Exception
    {     
        XYSeries[] series = new XYSeries[varNames.length];
        for( int i = 0; i < varNames.length; i++ )
        {
            if (!nameToIndex.containsKey( varNames[i]))
                    throw new Exception("Unknown variable "+varNames[i]+" in agent "+agent.getName());
            int index = nameToIndex.get( varNames[i] );
            series[i] = new XYSeries( varNames[i] );
            variablesToPlot.put( index, series[i] );
        }
        generatePlot( plotName, series );
    }

    public AgentStatCollector2(SimulationAgent agent)
    {
        nameToIndex = new HashMap<>();
        int n = agent.span.getLength();
        this.agent = agent;
        names = agent.getVariableNames();
        this.values = new double[names.length][n];
        for( int i = 0; i < names.length; i++ )
            nameToIndex.put( names[i], i );
        allVariables = true;
    }

    public AgentStatCollector2(SimulationAgent agent, String[] names)
    {
        nameToIndex = new HashMap<>();
        int n = agent.span.getLength();
        this.agent = agent;
        String[] allVariableNames = agent.getVariableNames();
        this.values = new double[names.length][n];
        for( String name : names )
        {
            for( int i = 0; i < allVariableNames.length; i++ )
                if( allVariableNames[i].equals( name ) )
                    nameToIndex.put( name, i );
        }
        allVariables = false;
    }

    @Override
    public void finish()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void update(double time) throws Exception
    {
        double[] fullValues = agent.getCurrentValues();
        for( int i = 0; i < names.length; i++ )
        {
            int index = allVariables ? i : nameToIndex.get( names[i] );
            values[i][timeIndex] = fullValues[index];
            
            XYSeries series = variablesToPlot.get( i );
            if (series != null)
                series.add( time, fullValues[index] );
        }
        timeIndex++;
        
        //TODO: dirty hack, needs some other way to collect statistics
        if (agent instanceof HealthCare)
            ( (HealthCare)agent ).resetStatistics();
    }

    @Override
    public void update(double time, SimulationAgent agent) throws Exception
    {
        //do nothing as this collector collects only data from one agent;
    }

    public void generatePlot(String name, XYSeries[] series)
    {
        JFrame frame = new JFrame( name );
        Container content = frame.getContentPane();
        XYSeriesCollection collection = new XYSeriesCollection();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        for( XYSeries xySeries : series )
            collection.addSeries( xySeries );

        JFreeChart chart = ChartFactory.createXYLineChart( "", "Time, day", "", collection, PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
        );

        chart.getXYPlot().setDomainGridlinePaint( Color.black );
        chart.getXYPlot().setRangeGridlinePaint( Color.black );

        chart.getXYPlot().setRenderer( renderer );
        chart.getXYPlot().setBackgroundPaint( Color.white );
        chart.setBackgroundPaint( Color.white );
        content.add( new ChartPanel( chart ) );
        frame.setSize( 800, 600 );
        frame.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                System.exit( 0 );
            }
        } );
        frame.setVisible( true );
    }
}