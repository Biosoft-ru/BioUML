package ru.biosoft.graphics.chart;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.graphics.chart.AxisOptions.Transform;


/**
 * Container-object to represent charts, (de)serialize them into JSON and plot them using JFreeChart
 * See "2010-06-02 - JSON Graph serialization.doc" for format details
 * @author lan
 */
public class Chart implements Iterable<ChartSeries>
{
    private ChartOptions options = null;
    private final List<ChartSeries> series = new ArrayList<>();
    private CharSequence initString = null;
    public static final Shape CIRCLE = new Ellipse2D.Float( -2.5f, -2.5f, 5, 5);
    public static final Shape RECTANGLE = new Rectangle2D.Float( -3f, -3f, 6, 6);
    
    private void initFromJSON(JSONArray from)
    {
        if(from == null || from.length() < 1) return;
        JSONArray data = null;
        if(from.optJSONObject(0) == null)   // full format
        {
            data = from.optJSONArray(0);
            if(from.optJSONObject(1) != null)
                options = new ChartOptions(from.optJSONObject(1));
        } else  // simplified format
        {
            data = from;
        }
        if(data != null)
        {
            for(int i=0; i<data.length(); i++)
            {
                if(data.optJSONObject(i) != null)
                    series.add(new ChartSeries(data.optJSONObject(i)));
            }
        }
    }
    
    private void init()
    {
        if(initString != null)
        {
            try
            {
                initFromJSON(new JSONArray(initString.toString()));
            }
            catch( JSONException e )
            {
            }
            initString = null;
        }
    }
    
    public Chart()
    {
    }
    
    /**
     * Construct from JSON-string
     */
    public Chart(CharSequence from)
    {
        initString = from;
    }
    
    public Chart(JSONArray from)
    {
        initFromJSON(from);
    }

    public ChartOptions getOptions()
    {
        init();
        return options;
    }
    
    public boolean isEmpty()
    {
        return initString == null && series.isEmpty();
    }

    public void setOptions(ChartOptions options)
    {
        init();
        this.options = options;
    }
    
    public int addSeries(ChartSeries series)
    {
        init();
        this.series.add(series);
        return this.series.size();
    }
    
    public int addSeries(double[][] data)
    {
        init();
        return this.addSeries(new ChartSeries(data));
    }
    
    public ChartSeries getSeries(int index)
    {
        init();
        return this.series.get(index);
    }
    
    public int getSeriesCount()
    {
        init();
        return this.series.size();
    }
    
    public JSONArray toJSON() throws JSONException
    {
        init();
        JSONObject jsonOptions = options == null?null:options.toJSON();
        JSONArray data = new JSONArray();
        for(int i=0; i<series.size(); i++)
        {
            JSONObject jsonSeries = series.get(i).toJSON();
            if(jsonSeries != null) data.put(jsonSeries);
        }
        if(jsonOptions == null) return data;
        JSONArray result = new JSONArray();
        result.put(data);
        result.put(jsonOptions);
        return result;
    }
    
    @Override
    public String toString()
    {
        return getData().toString();
    }
    
    public CharSequence getData()
    {
        if(initString != null) return initString;
        if(isEmpty()) return "";
        try
        {
            return toJSON().toString();
        }
        catch(Exception e)
        {
            return "";
        }
    }
    
    protected XYSeries createSeries(ChartSeries curSeries, String label, ChartOptions options)
    {
        XYSeries xySeries = new XYSeries( label, false );
        double[][] data = curSeries.getData();
        for( double[] element : data )
        {
            if( element == null || element.length < 2
                    || ( options.getXAxis(curSeries.getXAxis()).getTransform() == Transform.LOGARITHM && element[0] <= 0 )
                    || ( options.getYAxis(curSeries.getYAxis()).getTransform() == Transform.LOGARITHM && element[1] <= 0 ) )
                continue;
            xySeries.add(element[0], element[1]);
        }
        return xySeries;
    }
    
    protected String getSeriesId(ChartSeries series)
    {
        return series.getXAxis()+"."+series.getYAxis()+"."+(series.getBars().isShow()?"bars":"lines");
    }

    protected void initXAxes(ChartOptions options, XYPlot xyPlot)
    {
        for(int i=0; i<options.getXAxisCount(); i++)
        {
            AxisOptions xAxis = options.getXAxis(i+1);
            String label = xAxis.getLabel() == null || xAxis.getLabel().isEmpty()?"Axis (X)":xAxis.getLabel();
            LogAxis logAxis = new LogAxis(label);
            xyPlot.setDomainAxis(i, xAxis.getTransform() == Transform.LOGARITHM?logAxis:new NumberAxis(label));
            xyPlot.setDomainAxisLocation(i, xAxis.isRightOrTop()?AxisLocation.TOP_OR_RIGHT:AxisLocation.BOTTOM_OR_LEFT);
            ValueAxis axis = xyPlot.getDomainAxis();
            if(xAxis.getMin() != null) axis.setLowerBound(xAxis.getMin());
            if(xAxis.getMax() != null) axis.setUpperBound(xAxis.getMax());
        }
    }

    protected void initYAxes(ChartOptions options, XYPlot xyPlot)
    {
        for(int i=0; i<options.getYAxisCount(); i++)
        {
            AxisOptions yAxis = options.getYAxis(i+1);
            String label = yAxis.getLabel() == null || yAxis.getLabel().isEmpty()?"Axis (Y)":yAxis.getLabel();
            LogAxis logAxis = new LogAxis(label);
            xyPlot.setRangeAxis(i, yAxis.getTransform() == Transform.LOGARITHM?logAxis:new NumberAxis(label));
            xyPlot.setRangeAxisLocation(i, yAxis.isRightOrTop()?AxisLocation.TOP_OR_RIGHT:AxisLocation.BOTTOM_OR_LEFT);
            ValueAxis axis = xyPlot.getRangeAxis();
            if(yAxis.getMin() != null) axis.setLowerBound(yAxis.getMin());
            if(yAxis.getMax() != null) axis.setUpperBound(yAxis.getMax());
        }
    }

    public JFreeChart getChart()
    {
        init();
        ChartOptions options = this.options == null?new ChartOptions():this.options;
        List<XYSeriesCollection> dataSetMap = new ArrayList<>();
        List<ChartSeries> seriesMap = new ArrayList<>();
        boolean legend = false;
        for( int i = 0; i < series.size(); i++ )
        {
            ChartSeries curSeries = series.get(i);
            if( curSeries.getData() == null )
                continue;
            if( curSeries.getLabel() != null ) legend = true;
            String label = curSeries.getLabel() == null?String.valueOf(i):curSeries.getLabel();
            XYSeries xySeries = createSeries(curSeries, label, options);
            dataSetMap.add(new XYSeriesCollection(xySeries));
            seriesMap.add(curSeries);
        }
        
        JFreeChart chart = ChartFactory.createXYLineChart("", "", "", dataSetMap.get(0), //dataset,
                PlotOrientation.VERTICAL, legend, // legend
                true, // tool tips
                false // URLs
                );
        XYPlot xyPlot = chart.getXYPlot();
        xyPlot.setBackgroundPaint(Color.WHITE);
        xyPlot.setDomainGridlinePaint(Color.GRAY);
        xyPlot.setRangeGridlinePaint(Color.GRAY);

        initXAxes(options, xyPlot);
        initYAxes(options, xyPlot);
        for(int dataSetNum = 0; dataSetNum < dataSetMap.size(); dataSetNum++)
        {
            XYSeriesCollection dataSet = dataSetMap.get(dataSetNum);
            ChartSeries curSeries = seriesMap.get(dataSetNum);
            xyPlot.mapDatasetToDomainAxis(dataSetNum, curSeries.getXAxis()-1);
            xyPlot.mapDatasetToRangeAxis(dataSetNum, curSeries.getYAxis()-1);
            xyPlot.setDataset(dataSetNum, dataSet);
            XYItemRenderer renderer;
            if(curSeries.getBars().isShow())
            {
                renderer = new XYBarRenderer(1-curSeries.getBars().getWidth());
                ((XYBarRenderer)renderer).setBarPainter(new StandardXYBarPainter());
                ((XYBarRenderer)renderer).setShadowVisible(false);
            }
            else
            {
                renderer = new XYLineAndShapeRenderer(curSeries.getLines().isShow(), curSeries.getLines().isShapesVisible());
                if(curSeries.getLines().isShapesVisible())
                    renderer.setSeriesShape(0, curSeries.getShape());
            }
            renderer.setSeriesPaint(0, curSeries.getColor());
            xyPlot.setRenderer(dataSetNum, renderer);
        }
        return chart;
    }

    public BufferedImage getImage(int width, int height)
    {
        JFreeChart chart = getChart();
        chart.setBackgroundPaint(Color.white);
        return chart.createBufferedImage(width, height);
    }

    @Override
    public Iterator<ChartSeries> iterator()
    {
        return series.iterator();
    }
}
