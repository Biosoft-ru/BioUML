package biouml.plugins.agentmodeling;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.apache.commons.lang.ArrayUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import biouml.plugins.simulation.Span;

public class ClassificationStatCollector extends StatCollector
{
    private double[] times;
    private int[] size;
    private int[][] values;
    private int[][] values_day;
    private Map<String, Integer> nameToIndex;
    private List<Classification> classifications = new ArrayList<>();
    private Map<Classification, Map<String, XYSeries>> series = new HashMap<>();
    private boolean drawGraphics = true;
    private int step;
    private boolean stepUpdate = true; 
    
    public void setStepUpdate(boolean stepUpdate)
    {
        this.stepUpdate = stepUpdate;
    }

    public void setDrawGraphics(boolean draw)
    {
        this.drawGraphics = draw;
    }
    
    public void addClassification(Classification classification)
    {
        classifications.add(classification);
    }

    @Override
    public void init(Span span, AgentBasedModel model) throws Exception
    {
        super.init(span, model);
        int length = span.getLength();
        times = new double[length];
        size = new int[length];
        step = 0;
        nameToIndex = new HashMap<>();
        nameToIndex.put("Other", 0);
        int j = 1;

        for( Classification classification : classifications )
        {
            Map<String, XYSeries> classificationSeries = new HashMap<>();
            for( String name : classification.titles.values() )
            {
                nameToIndex.put(name, j);
                classificationSeries.put( name, new XYSeries(name) );
                j++;
            }
            series.put( classification, classificationSeries );
            generatePlot(classification.getVariableName(), classificationSeries);
        }
        values = new int[nameToIndex.size()][length];
        values_day = new int[nameToIndex.size()][length];
    }

    @Override
    public void update(double time)
    {
        for( int i=0; i<classifications.size(); i++)
        {
            Classification classificaton = classifications.get( i ); 
            
            if( !stepUpdate )
            {
                for( SimulationAgent agent : model.getAgents() )
                    classificaton.classify( agent );
            }
            
            Map<String, XYSeries> classificationSeries = series.get( classificaton );
            
            for( Double value : classificaton.counts.keySet() )
            {
                String title = classificaton.titles.get(value);
                if (title == null)
                    continue;
                int index = nameToIndex.get(title);
                values_day[index][step] = classificaton.counts.get(value) - values[index][step];
                values[index][step] = classificaton.counts.get(value);
                classificationSeries.get( title ).add( time,  values[index][step] );
            }
            
            classificaton.reset();
        }
        times[step] = time;
        size[step] = model.getAgents().size();
        step++;
    }
    
    @Override
    public void update(double time, SimulationAgent agent) throws Exception
    {
        for( int i=0; i<classifications.size(); i++)
        {
            Classification classificaton = classifications.get( i ); 
                classificaton.classify(agent);
        }
    }

    @Override
    public void finish()
    {
        times = ArrayUtils.subarray(times, 0, step);
        size = ArrayUtils.subarray(size, 0, step);

        for( int i = 0; i < values.length; i++ )
            values[i] = ArrayUtils.subarray(values[i], 0, step);
    }

    public double[] getTimes()
    {
        return times;
    }

    public int[] getDynamic(String value)
    {
        return values[nameToIndex.get(value)];
    }
    
    public int[] getSizeDynamic()
    {
        return size;
    }
    
    public void generatePlot(String name, Map<String, XYSeries> seriesList)
    {
        JFrame frame = new JFrame(name);
        Container content = frame.getContentPane();
        XYSeriesCollection collection = new XYSeriesCollection();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for( XYSeries xySeries : seriesList.values() )
            collection.addSeries( xySeries );

        JFreeChart chart = ChartFactory.createXYLineChart( "", "Time", "", collection, PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
        );

        chart.getXYPlot().setDomainGridlinePaint(Color.black);
        chart.getXYPlot().setRangeGridlinePaint(Color.black);
        
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


}
