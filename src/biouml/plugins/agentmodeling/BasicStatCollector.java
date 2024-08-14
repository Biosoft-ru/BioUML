package biouml.plugins.agentmodeling;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import biouml.plugins.simulation.Span;

public class BasicStatCollector extends StatCollector
{
    XYSeries series;

    XYSeries agentsIncreaseSeries;

    TreeMap<Double, Integer> timeToAgents = new TreeMap<>();

    @Override
    public void init(Span span, AgentBasedModel model) throws Exception
    {
        super.init(span, model);
        if( isShowPlot() )
        {
            series = new XYSeries("Agents number");
            agentsIncreaseSeries = new XYSeries("Agents increase rate");
            generatePlot();
        }

        setAgentsNumber(span.getTimeStart(), model.getAgents().size());
    }

    @Override
    public void update(double time)
    {
        setAgentsNumber(time, model.getAgents().size());

    }
    
    public Map<Double, Integer> getAgentsNumber()
    {
        return timeToAgents;
    }

    @Override
    public void finish()
    {
        if( isShowPlot() )
            calcAgentIncreaseRate();
    }

    public void setAgentsNumber(double time, int agentsNumber)
    {
        if( isShowPlot() )
        {
            if( timeToAgents.keySet().contains(time) )
            {
                series.update(time, agentsNumber);
            }
            else
            {
                series.add(time, agentsNumber);
            }
        }
        timeToAgents.put(time, agentsNumber);
    }

    private void calcAgentIncreaseRate()
    {
        Iterator<Map.Entry<Double, Integer>> iter = timeToAgents.entrySet().iterator();
        Map.Entry<Double, Integer> entry = iter.next();
        double initialTime = entry.getKey();
        agentsIncreaseSeries.add(initialTime, 0);
        int previousAgents = timeToAgents.get(0.0);
        while( iter.hasNext() )
        {
            entry = iter.next();
            Integer agentsIncrease = entry.getValue() - previousAgents;
            agentsIncreaseSeries.add(entry.getKey(), agentsIncrease);
            previousAgents = entry.getValue();
        }
    }


    public void generatePlot()
    {
        JFrame frame = new JFrame("");
        Container content = frame.getContentPane();
        XYSeriesCollection collection = new XYSeriesCollection();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        collection.addSeries(series);
        collection.addSeries(agentsIncreaseSeries);

        JFreeChart chart = ChartFactory.createXYLineChart("", "Time", "", collection, PlotOrientation.VERTICAL, true, // legend
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
    public void update(double time, SimulationAgent agent) throws Exception
    {
        // TODO Auto-generated method stub
        
    }


}
