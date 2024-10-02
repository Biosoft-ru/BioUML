package biouml.plugins.simulation.plot;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import java.util.logging.Logger;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;

@SuppressWarnings ( "serial" )
public class PlotPane extends JPanel implements PropertyChangeListener, PlotChangeListener
{
    protected static final Logger log = Logger.getLogger(PlotPane.class.getName());
    protected JFreeChart chart;

    private ChartPanel chartPanel;
    private static final Shape circle = new Ellipse2D.Float(-3, -3, 6, 6);

    private static final Paint[] POSSIBLE_COLORS = new Paint[] {new Color(0xFF, 0x55, 0x55), new Color(0x55, 0x55, 0xFF),
            new Color(0x00, 0xFF, 0x00), Color.pink, ChartColor.DARK_RED, ChartColor.DARK_BLUE, ChartColor.DARK_GREEN,
            ChartColor.DARK_YELLOW, ChartColor.DARK_MAGENTA, ChartColor.DARK_CYAN, Color.darkGray, ChartColor.VERY_DARK_RED,
            ChartColor.VERY_DARK_BLUE, ChartColor.VERY_DARK_GREEN, ChartColor.VERY_DARK_YELLOW, ChartColor.VERY_DARK_MAGENTA,
            ChartColor.VERY_DARK_CYAN};

    public PlotPane(int ix, int iy)
    {
        super(new java.awt.BorderLayout());

        try
        {
            chart = ChartFactory.createXYLineChart("", "Axis (X)", "Axis (Y)", null, //dataset,
                    PlotOrientation.VERTICAL, true, // legend
                    true, // tool tips
                    false // URLs
                    );

            chart.getXYPlot().setBackgroundPaint(Color.white);
            chart.setBackgroundPaint(Color.white);

            chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(ix, iy));
            add(chartPanel);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error occured while creating chart panel: " + ex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    protected Plot plot;
    public Plot getPlot()
    {
        return plot;
    }

    public void setPlot(Plot plot)
    {
        Plot oldPlot = this.plot;
        this.plot = plot;
        if( oldPlot != null )
        {
            oldPlot.removePropertyChangeListener(this);
        }
        if( this.plot != null )
        {
            this.plot.addPropertyChangeListener(this);
        }

        seriesChanged = true;

        setChartProperties();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Series issues
    //

    // indicates if the row model was changed
    boolean seriesChanged = true;

    public static class XYSeriesEx extends XYSeries
    {
        protected Series series;
        public XYSeriesEx(String name, Series series, boolean sortAndRemoveDuplicates)
        {
            super(name, sortAndRemoveDuplicates, true);
            this.series = series;
        }

        public Series getSeries()
        {
            return series;
        }
    }

    protected XYSeriesEx findXYSeries(Series s)
    {
        XYSeriesCollection xyDataset = (XYSeriesCollection)chart.getXYPlot().getDataset();
        int xySeriesCount = xyDataset.getSeriesCount();
        for( int j = 0; j < xySeriesCount; j++ )
        {
            XYSeriesEx xyS = (XYSeriesEx)xyDataset.getSeries(j);
            if( xyS.getSeries() == s )
            {
                return xyS;
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // PropertyChange processing
    //

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        // plot events
        if( pce.getPropertyName().equals("plot") )
        {
            setPlot((Plot)pce.getNewValue());
            clearChart();
        }
        else if( pce.getPropertyName().equals("title") )
        {
            chart.setTitle((String)pce.getNewValue());
        }
        else if( pce.getPropertyName().equals("xTitle") )
        {
            chart.getXYPlot().getDomainAxis().setLabel((String)pce.getNewValue());
        }
        else if( pce.getPropertyName().equals("xTitleFont") )
        {
            chart.getXYPlot().getDomainAxis().setLabelFont(((ColorFont)pce.getNewValue()).getFont());
            chart.getXYPlot().getDomainAxis().setLabelPaint(((ColorFont)pce.getNewValue()).getColor());
        }
        else if( pce.getPropertyName().equals("xTickLabelFont") )
        {
            chart.getXYPlot().getDomainAxis().setTickLabelFont(((ColorFont)pce.getNewValue()).getFont());
        }
        else if( pce.getPropertyName().equals("xTo") )
        {
            chart.getXYPlot().getDomainAxis().setUpperBound( ( (Double)pce.getNewValue() ).doubleValue());
        }
        else if( pce.getPropertyName().equals("xFrom") )
        {
            chart.getXYPlot().getDomainAxis().setLowerBound( ( (Double)pce.getNewValue() ).doubleValue());
        }
        else if( pce.getPropertyName().equals("yTitle") )
        {
            chart.getXYPlot().getRangeAxis().setLabel((String)pce.getNewValue());
        }
        else if( pce.getPropertyName().equals("yTitleFont") )
        {
            chart.getXYPlot().getRangeAxis().setLabelFont(((ColorFont)pce.getNewValue()).getFont());
            chart.getXYPlot().getRangeAxis().setLabelPaint(((ColorFont)pce.getNewValue()).getColor());
        }
        else if( pce.getPropertyName().equals("xTickLabelFont") )
        {
            chart.getXYPlot().getRangeAxis().setTickLabelFont(((ColorFont)pce.getNewValue()).getFont());
        }
        else if( pce.getPropertyName().equals("yTo") )
        {
            chart.getXYPlot().getRangeAxis().setUpperBound( ( (Double)pce.getNewValue() ).doubleValue());
        }
        else if( pce.getPropertyName().equals("yFrom") )
        {
            chart.getXYPlot().getRangeAxis().setLowerBound( ( (Double)pce.getNewValue() ).doubleValue());

            // series propagated events
        }
        else if( pce.getPropertyName().equals("legend") )
        {
            Series s = (Series)pce.getSource();
            try
            {
                XYSeriesEx xyEx = findXYSeries(s);
                xyEx.setKey((String)pce.getNewValue());
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Error occured while updating legend to value " + pce.getNewValue() + ": " + ex);
            }
        }
        else if( pce.getPropertyName().equals("spec") )
        {
        }
        else if( pce.getPropertyName().equals("plotData") )
        {
            scheduleRedraw();
        }
    }

    @Override
    public void plotChanged(PlotChangeEvent event)
    {
        if( plot == null )
            return;

        try
        {
            XYPlot xyPlot = (XYPlot)event.getSource();

            //avoid infinite cycle
            boolean isNotificatioNEnabled = plot.isNotificationEnabled();
            plot.setNotificationEnabled( false );
            
            if( chart.getTitle() != null )
                plot.setTitle(chart.getTitle().getText());

            plot.setXTitle(xyPlot.getDomainAxis().getLabel());
            plot.setYTitle(xyPlot.getRangeAxis().getLabel());
            Paint paintX = xyPlot.getDomainAxis().getLabelPaint();
            plot.setXTitleFont(new ColorFont(xyPlot.getDomainAxis().getLabelFont(), paintX instanceof Color? (Color)paintX: Color.black));
            Paint paintY = xyPlot.getRangeAxis().getLabelPaint();
            plot.setYTitleFont(new ColorFont(xyPlot.getRangeAxis().getLabelFont(), paintY instanceof Color? (Color)paintY: Color.black));
            plot.setXTickLabelFont(new ColorFont(xyPlot.getDomainAxis().getTickLabelFont(), Color.black));
            plot.setYTickLabelFont(new ColorFont(xyPlot.getRangeAxis().getTickLabelFont(), Color.black));
            plot.setXFrom(xyPlot.getDomainAxis().getLowerBound());
            plot.setXTo(xyPlot.getDomainAxis().getUpperBound());
            plot.setYFrom(xyPlot.getRangeAxis().getLowerBound());
            plot.setYTo(xyPlot.getRangeAxis().getUpperBound());
            plot.setXAutoRange( false );
            plot.setYAutoRange( false );
            plot.setNotificationEnabled( isNotificatioNEnabled );
            
            if( chart.getXYPlot() == null )
            {
                return;
            }

            XYSeriesCollection xyDataset = (XYSeriesCollection)chart.getXYPlot().getDataset();

            for( int i = 0; i < xyDataset.getSeriesCount(); i++ )
            {
                XYSeriesEx xyS = (XYSeriesEx)xyDataset.getSeries(i);
                Series s = xyS.getSeries();
                if( s != null )
                    s.setLegend((String)xyS.getKey());
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Error occured while updating series: " + t, t);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // utilities
    //
    public void setChartProperties()
    {
        chart.getPlot().removeChangeListener(this);
        // unplug it temporary
        setChartProperties(plot, chart);
        //plug back
        chart.getPlot().addChangeListener(this);
    }

    public static void setChartProperties(Plot plot, JFreeChart chart)
    {
        if( plot != null )
        {
            chart.setTitle(plot.getTitle());

            ValueAxis axis = generateAxis(Plot.AxisType.getAxisType(plot.getXAxisType()));
            if( axis != null )
                chart.getXYPlot().setDomainAxis(axis);

            axis = generateAxis(Plot.AxisType.getAxisType(plot.getYAxisType()));
            if( axis != null )
                chart.getXYPlot().setRangeAxis(axis);

            chart.getXYPlot().getDomainAxis().setLabel(plot.getXTitle());
            chart.getXYPlot().getRangeAxis().setLabel(plot.getYTitle());
 
            chart.getXYPlot().getDomainAxis().setLabelFont(plot.getXTitleFont().getFont());
            chart.getXYPlot().getRangeAxis().setLabelFont(plot.getYTitleFont().getFont());
            
            chart.getXYPlot().getDomainAxis().setLabelPaint(plot.getXTitleFont().getColor());
            chart.getXYPlot().getRangeAxis().setLabelPaint(plot.getYTitleFont().getColor());
            
            chart.getXYPlot().getDomainAxis().setTickLabelFont(plot.getXTickLabelFont().getFont());
            chart.getXYPlot().getRangeAxis().setTickLabelFont(plot.getYTickLabelFont().getFont());
            
            chart.getXYPlot().getDomainAxis().setTickLabelPaint( plot.getXTickLabelFont().getColor() );
            chart.getXYPlot().getRangeAxis().setTickLabelPaint( plot.getYTickLabelFont().getColor() );

            if( plot.getXFrom() != plot.getXTo() )
            {
                chart.getXYPlot().getDomainAxis().setLowerBound(plot.getXFrom());
                chart.getXYPlot().getDomainAxis().setUpperBound(plot.getXTo());
            }
            chart.getXYPlot().getDomainAxis().setAutoRange(plot.isXAutoRange());

            if( plot.getYFrom() != plot.getYTo() )
            {
                chart.getXYPlot().getRangeAxis().setLowerBound(plot.getYFrom());
                chart.getXYPlot().getRangeAxis().setUpperBound(plot.getYTo());
            }
            chart.getXYPlot().getRangeAxis().setAutoRange(plot.isYAutoRange());

            chart.getXYPlot().setDrawingSupplier(
                    new DefaultDrawingSupplier(ResultPlotPane.POSSIBLE_COLORS, DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                            DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE, DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                            DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        }
    }

    public static ValueAxis generateAxis(Plot.AxisType axisType)
    {
        switch( axisType )
        {
            case LOGARITHMIC:
            {
                LogarithmicAxis logAxis = new LogarithmicAxis("");
                logAxis.setExpTickLabelsFlag(true);
                logAxis.setStrictValuesFlag(false);
                return logAxis;
            }
            case LOG10:
            {
                LogarithmicAxis logAxis = new LogarithmicAxis("");
                logAxis.setLog10TickLabelsFlag(true);
                logAxis.setStrictValuesFlag(false);
                return logAxis;
            }
            case NUMBER:
            {
                NumberAxis numAxis = new NumberAxis("");
                numAxis.setAutoRangeIncludesZero(false);
                return numAxis;
            }
        }
        return null;
    }

    public void clearChart()
    {
        XYSeriesCollection xyDataset = (XYSeriesCollection)chart.getXYPlot().getDataset();
        if( xyDataset != null )
            xyDataset.removeAllSeries();
    }

    public static final int MAX_DELAY = 200; // ms -- delay between redraws
    private Thread redrawThread = null;
    public synchronized void scheduleRedraw()
    {
        seriesChanged = true;
        if( redrawThread != null )
            return;
        redrawThread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(MAX_DELAY);
                    redrawChart();
                }
                catch( Throwable e )
                {
                }
                redrawThread = null;
            }
        };
        redrawThread.start();
    }

    public synchronized void redrawChart()
    {
        if( !seriesChanged )
            return;

        chart.getPlot().removeChangeListener(this);
        redrawChart(plot, chart);
        chart.getPlot().addChangeListener(this);

        if( seriesChanged )
        {
            seriesChanged = false;
        }
    }

    public static void redrawChart(Plot plot, JFreeChart chart)
    {
        chart.getXYPlot().setDataset(new XYSeriesCollection());
        if( plot == null )
        {
            log.log(Level.SEVERE, "Could not redraw chart: the \"plot\" structure is null.");
            return;
        }

        List<Series> series = plot.getSeries();
        if( series == null )
            return;

        Iterator<Series> iter = series.iterator();
        if( iter != null )
        {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setDrawSeriesLineAsPath(true);
            int counter = 0;
            while( iter.hasNext() )
            {
                Series nextSeries = iter.next();

                if( nextSeries.getSourceNature().equals(Series.SourceNature.SIMULATION_RESULT) )
                {
                    renderer.setSeriesShapesVisible(counter, false);
                }
                else if( nextSeries.getSourceNature().equals(Series.SourceNature.EXPERIMENTAL_DATA) )
                {
                    renderer.setSeriesLinesVisible(counter, false);
                    renderer.setSeriesShapesVisible(counter, true);
                    renderer.setSeriesShape(counter, circle);
                }
                Pen spec = nextSeries.getSpec();
                if( spec != null )
                {
                    renderer.setSeriesPaint(counter, spec.getColor());
                    renderer.setSeriesStroke(counter, spec.getStroke());
                }
                else if( counter < POSSIBLE_COLORS.length )
                {
                    Color color = (Color)POSSIBLE_COLORS[counter];
                    renderer.setSeriesPaint(counter, POSSIBLE_COLORS[counter]);
                    nextSeries.setSpec(new Pen(1.0f, color));
                }
                redrawSeries(nextSeries, chart);
                counter++;
            }
            chart.getXYPlot().setRenderer(renderer);

            setChartProperties(plot, chart);
        }
    }
    protected static void redrawSeries(Series s, JFreeChart chart)
    {
        try
        {
            XYSeriesCollection xyDataset = (XYSeriesCollection)chart.getXYPlot().getDataset();

            double[] xValues = s.getXValues();
            double[] yValues = s.getYValues();

            String name = ( s.getLegend() != null && !s.getLegend().equals("") ) ? s.getLegend() : s.getYVar();

            XYSeriesEx series = new XYSeriesEx(name, s, s.getXVar().equals("time"));

            for( int i = 0; i < xValues.length; i++ )
            {
                series.add(xValues[i], yValues[i]);
            }

            // trick for reducing space occupied by legends on the plot
            if( xyDataset.getSeriesCount() > 15 )
                return;
            xyDataset.addSeries(series);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error occured while filling in chart data for series : \n" + ex);
        }
    }
}
