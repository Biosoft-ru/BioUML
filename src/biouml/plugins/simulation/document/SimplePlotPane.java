package biouml.plugins.simulation.document;

import java.awt.Color;
import java.util.logging.Level;
import java.util.Map;

import javax.swing.JPanel;

import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.ResultPlotPane;

/**
 * @author axec
 */
@SuppressWarnings ( "serial" )
public class SimplePlotPane extends JPanel
{
    protected static final Logger log = Logger.getLogger( SimplePlotPane.class.getName() );
    protected JFreeChart chart;
    private PlotInfo plotInfo;
    private String xVariable;

    public SimplePlotPane(int ix, int iy, PlotInfo plotInfo, double timeLimit)
    {
        super( new java.awt.BorderLayout() );

        try
        {
            chart = ChartFactory.createXYLineChart( "", "Axis (X)", "Axis (Y)", null, //dataset,
                    PlotOrientation.VERTICAL, true, // legend
                    true, // tool tips
                    false // URLs
            );

            this.plotInfo = plotInfo;
            xVariable = this.plotInfo.getXVariable().getName();
            chart.getXYPlot().setBackgroundPaint( Color.white );
            chart.setBackgroundPaint( Color.white );
            XYSeriesCollection dataset = new XYSeriesCollection();
            chart.getXYPlot().setDataset( dataset );
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setDrawSeriesLineAsPath( true );
            ChartPanel chartPanel = new ChartPanel( chart );
            chartPanel.setPreferredSize( new java.awt.Dimension( ix, iy ) );
            add( chartPanel );
            if( plotInfo.getExperiments() != null )
                ResultPlotPane.addExperiments( plotInfo.getExperiments(), renderer, dataset, timeLimit );
            int counter = chart.getXYPlot().getDataset().getSeriesCount();
            for( Curve c : plotInfo.getYVariables() )
            {
                renderer.setSeriesPaint( counter, c.getPen().getColor() );
                renderer.setSeriesStroke( counter, c.getPen().getStroke() );
                renderer.setSeriesShapesVisible( counter, false );
                renderer.setSeriesLinesVisible( counter, true );
                renderer.setSeriesShape( counter, null );
                dataset.addSeries( new XYSeries( c.getCompleteName(), false, true ) );
                counter++;
            }
            chart.getXYPlot().setRenderer( renderer );
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Error occured while creating chart panel: " + ex );
        }
    }

    public void redrawChart(Map<String, double[]> values)
    {
        double[] x = values.get( xVariable );
        XYSeriesCollection xyDataset = (XYSeriesCollection)chart.getXYPlot().getDataset();
        for( Curve c : plotInfo.getYVariables() )
        {
            double[] y = values.get( c.getCompleteName() );
            XYSeries series = xyDataset.getSeries( c.getCompleteName() );
            series.clear();           

            for( int i = 0; i < x.length; i++ )
                series.add( x[i], y[i] );
        }
    }
}
