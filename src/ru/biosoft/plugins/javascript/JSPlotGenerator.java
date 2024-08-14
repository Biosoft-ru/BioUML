package ru.biosoft.plugins.javascript;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.graphics.chart.ChartSeries;


/**
 * Special class for generating plot for JavaScript function "plot()"
 */
public class JSPlotGenerator
{
    public static final String TYPE_LINE = "line";
    public static final String TYPE_CONSTANT = "constant";
    public static final String TYPE_EXPERIMENT = "experiment";
    
    protected static final Shape circle = new Ellipse2D.Float(0, 0, 3, 3);

    /**
     * Create BufferedImage with plot
     */
    public static BufferedImage generatePlot(String xAxisTitle, String yAxisTitle, List<XYSeries> series, List<String> seriesTypes)
    {
        XYSeriesCollection xyDataset = new XYSeriesCollection();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        int counter = 0;
        for( XYSeries line : series )
        {
            xyDataset.addSeries(line);
            String type = TYPE_LINE;
            if(seriesTypes != null)
                type = seriesTypes.get(counter);
            if(type.equals(TYPE_EXPERIMENT))
            {
                renderer.setSeriesLinesVisible(counter, false);
                renderer.setSeriesShape(counter, circle);
            }
            else
            {
                renderer.setSeriesShapesVisible(counter, false);
            }
            counter++;
        }
        
        JFreeChart chart = ChartFactory.createXYLineChart("", xAxisTitle, yAxisTitle, xyDataset, //dataset,
                PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
                );
        chart.setBackgroundPaint(Color.white);
        chart.getXYPlot().setRenderer(renderer);
        if( areDomainValuesInteger(xyDataset.getSeries(0)) )
            chart.getXYPlot().getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        int width = 550;
        int height = 350;
        if( series.size() > 10 )
        {
            height += getSeriesLabelsHeight(series, chart.getXYPlot().getRenderer(), width - 50);
        }
        return chart.createBufferedImage(width, height);
    }

    public static BufferedImage generateLogScalePlot(String xAxisTitle, String yAxisTitle, double minX, double maxX, double minY,
            double maxY, List<XYSeries> series)
    {
        XYSeriesCollection xyDataset = new XYSeriesCollection();
        for( XYSeries line : series )
        {
            xyDataset.addSeries(line);
        }

        //        NumberAxis xAxis = new NumberAxis(xAxisTitle);
        //        xAxis.setAutoRangeIncludesZero(false);

        LogarithmicAxis xAxis = new LogarithmicAxis(yAxisTitle);
        xAxis.setExpTickLabelsFlag(true);
        xAxis.setStrictValuesFlag(false);
        xAxis.setRange(minX, maxX);

        LogarithmicAxis yAxis = new LogarithmicAxis(yAxisTitle);
        yAxis.setExpTickLabelsFlag(true);
        yAxis.setStrictValuesFlag(false);
        yAxis.setRange(minY, maxY);

        XYItemRenderer renderer = new StandardXYItemRenderer();
        XYPlot plot = new XYPlot(xyDataset, xAxis, yAxis, renderer);

        JFreeChart chart = new JFreeChart("", new Font("SansSerif", Font.BOLD, 14), plot, true);

        chart.setBackgroundPaint(Color.white);
        return chart.createBufferedImage(550, 350);
    }
    
    public static ChartSeries createSimpleChartSeries(String title, double[] values)
    {
        double[] xValues = new double[values.length];
        for( int i = 0; i < xValues.length; i++ )
            xValues[i] = i + 1;
        ChartSeries series = new ChartSeries(xValues, values);
        series.setLabel(title);
        return series;
    }

    public static ChartSeries createLineChartSeries(String title, double[] xValues, double[] yValues)
    {
        ChartSeries series = new ChartSeries(xValues, yValues);
        series.setLabel(title);
        return series;
    }
    
    public static ChartSeries createConstantChartSeries(String title, double[] xValues, double yValue)
    {
        double[] yValues = new double[xValues.length];
        Arrays.fill(yValues, yValue);
        ChartSeries series = new ChartSeries(xValues, yValues);
        series.setLabel(title);
        return series;
    }

    public static XYSeries createLineSeries(String title, double[] xValues, double[] yValues)
    {
        XYSeries series = new XYSeries(title);
        if( xValues.length == yValues.length )
        {
            for( int j = 0; j < xValues.length; j++ )
            {
                series.add(xValues[j], yValues[j]);
            }
        }
        return series;
    }

    /**
     * Create BufferedImage with box and whisker chart
     */
    public static BufferedImage generateBoxAndWhisker(String[] columns, List<double[]> values)
    {
        int columnsCount = Math.min(columns.length, values.size());

        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        for( int j = 0; j < columnsCount; j++ )
        {
            List<Double> list = new ArrayList<>();
            double[] val = values.get(j);
            for( double element : val )
            {
                list.add(Double.valueOf(element));
            }
            dataset.add(list, "series 0", columns[j]);
        }

        CategoryAxis xAxis = new CategoryAxis("Column");
        NumberAxis yAxis = new NumberAxis("Value");
        yAxis.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(false);
        renderer.setDefaultToolTipGenerator( new BoxAndWhiskerToolTipGenerator() );
        CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

        JFreeChart chart = new JFreeChart("", new Font("SansSerif", Font.BOLD, 14), plot, true);

        chart.setBackgroundPaint(Color.white);

        return chart.createBufferedImage(550, 350);
    }

    private static boolean areDomainValuesInteger(XYSeries series)
    {
        int numPoints = series.getItemCount();
        int numToCheck = numPoints > 10 ? 10 : numPoints;
        for( int i = 0; i < numToCheck; i++ )
        {
            double xValue = series.getX(i).doubleValue();
            if( Math.floor(xValue) != xValue )
            {
                return false;
            }
        }
        return true;
    }

    private static int getSeriesLabelsHeight(List<XYSeries> series, XYItemRenderer renderer, int width)
    {
        Font font = renderer.getSeriesItemLabelFont(0);
        if( font == null )
            font = renderer.getDefaultItemLabelFont();
        int height = 0;
        Graphics2D graphics = ApplicationUtils.getGraphics();
        FontMetrics fm = graphics.getFontMetrics(font);
        int curLineWidth = 0;
        int numLines = 1;
        for( XYSeries line : series )
        {
            String title = line.getKey().toString();
            if( curLineWidth + fm.stringWidth(title) + 20 > width )
            {
                numLines++;
                curLineWidth = fm.stringWidth(title) + 20;
            }
            else
                curLineWidth += fm.stringWidth(title) + 20;
        }
        height += fm.getHeight() * numLines * 3 / 2;
        return height;
    }
}
