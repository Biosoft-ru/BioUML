package biouml.plugins.simulation.web;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Series;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngine.Var;
import biouml.plugins.simulation.plot.PlotPane;
import biouml.standard.simulation.plot.Plot;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.Pen;

public class WebResultPlot
{
    protected JFreeChart chart;
    protected Map<SimulationEngine.Var, List<Series>> variableIndeces;
    protected AbstractXYDataset dataset;
    protected Var xVariable;
    protected PlotInfo plotInfo; 
    
    public WebResultPlot(PlotInfo plotInfo)
    {
        this.plotInfo = plotInfo;
    }
    
    public void init(SimulationEngine engine)
    {
        dataset = new XYSeriesCollection();
        xVariable = engine.getXVariable(plotInfo);
        variableIndeces = engine.getVariablesToPlot(plotInfo);
        String yAxisLabel = variableIndeces.size() == 1? StreamEx.of(variableIndeces.keySet()).findFirst().map(v->v.title).get(): "Quantity or concentration";
        
        chart = ChartFactory.createXYLineChart(plotInfo.getTitle(), xVariable.title, yAxisLabel, null, //dataset,
                PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
        );
        chart.setBackgroundPaint(Color.white);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDrawSeriesLineAsPath(true);
        // plot properties
        XYPlot plot = chart.getXYPlot();

        //X axis
        ValueAxis axis = PlotPane.generateAxis( Plot.AxisType.getAxisType( plotInfo.getXAxisType() ) );
        if( axis != null )
            plot.setDomainAxis( axis );

        if( plotInfo.getXFrom() != plotInfo.getXTo() )
        {
            plot.getDomainAxis().setLowerBound( plotInfo.getXFrom() );
            plot.getDomainAxis().setUpperBound( plotInfo.getXTo() );
        }
        else
        {
            plot.getDomainAxis().setLowerBound( engine.getInitialTime() );
            plot.getDomainAxis().setUpperBound( engine.getCompletionTime() );
        }
        plot.getDomainAxis().setAutoRange( plotInfo.isXAutoRange() );

        //Y axis
        axis = PlotPane.generateAxis( Plot.AxisType.getAxisType( plotInfo.getYAxisType() ) );
        if( axis != null )
            plot.setRangeAxis( axis );
        if( plotInfo.getYFrom() != plotInfo.getYTo() )
        {
            plot.getRangeAxis().setLowerBound( plotInfo.getYFrom() );
            plot.getRangeAxis().setUpperBound( plotInfo.getYTo() );
        }
        plot.getRangeAxis().setAutoRange( plotInfo.isYAutoRange() );

        //Possible to adjust
        //          plot.getDomainAxis().setLabel(plotInfo.getXTitle());
        //            plot.getRangeAxis().setLabel(plotInfo.getYTitle());
        //
        //            plot.getDomainAxis().setLabelFont( plotInfo.getXTitleFont().getFont() );
        //            plot.getRangeAxis().setLabelFont( plotInfo.getYTitleFont().getFont() );
        //
        //            plot.getDomainAxis().setLabelPaint( plotInfo.getXTitleFont().getColor() );
        //            plot.getRangeAxis().setLabelPaint( plotInfo.getYTitleFont().getColor() );
        //
        //            plot.getDomainAxis().setTickLabelFont( plotInfo.getXTickLabelFont().getFont() );
        //            plot.getRangeAxis().setTickLabelFont( plotInfo.getYTickLabelFont().getFont() );
        //
        //            plot.getDomainAxis().setTickLabelPaint( plotInfo.getXTickLabelFont().getColor() );
        //            plot.getRangeAxis().setTickLabelPaint( plotInfo.getYTickLabelFont().getColor() );


        int counter = 0;
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            SimulationEngine.Var var = e.getKey();
            XYSeries series = (XYSeries)e.getValue().get(0);
            if( series != null )
            {
                Pen spec = var.pen;
                if( spec != null )
                {
                    renderer.setSeriesShapesVisible(counter, false);
                    renderer.setSeriesPaint(counter, spec.getColor());
                    renderer.setSeriesStroke(counter, spec.getStroke());
                }
                counter++;
                ((XYSeriesCollection)dataset).addSeries(series);
            }
        }
        
        if( plotInfo.getExperiments() != null )
            ResultPlotPane.addExperiments( plotInfo.getExperiments(), renderer, dataset, engine.getCompletionTime()  );

        chart.getXYPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setRenderer(renderer);
        chart.getXYPlot().setDataset(dataset);
    }
    
    public BufferedImage getImage()
    {
        return chart.createBufferedImage(550, 350);
    }
    
    public void add(double t, double[] y)
    {
        int size = variableIndeces.size();
        int count = 0;
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            SimulationEngine.Var var = e.getKey();
            if (var.index == null)
                continue;
            
            XYSeries series = (XYSeries)e.getValue().get(0);
            double yV =  y[var.index];
            double xV =  y[xVariable.index];
            series.add(xV, yV, count == size - 1);
            count++;
        }
    }
    
    public void start(Object model)
    {
    }

    public void startCycle()
    {
    }
}