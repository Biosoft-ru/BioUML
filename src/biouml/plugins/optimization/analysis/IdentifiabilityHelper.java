package biouml.plugins.optimization.analysis;

import java.awt.Color;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.annotation.Nonnull;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.AxisOptions.Transform;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;

public class IdentifiabilityHelper
{
    public static final @Nonnull String PLOT_TYPE_PNG = "Image (png)";
    public static final @Nonnull String PLOT_TYPE_CHART = "Chart";

    private static final String Y_AXIS_LABEL = "Objective function value";

    static class Point
    {
        final double x;
        final double y;
        public Point(double x, double y)
        {
            this.x = x;
            this.y = y;
        }
        public double getX()
        {
            return x;
        }
        public double getY()
        {
            return y;
        }
    }

    public static String[] getAvailablePlotTypes()
    {
        return new String[] {PLOT_TYPE_PNG, PLOT_TYPE_CHART};
    }

    public static DataElementPath savePlotAsPNG(DataCollection<DataElement> resultsFolder, List<Point> points, String selectedName,
            Point estimatedPoint, double delta, boolean xLog, boolean yLog)
    {
        Chart chart = createChart(selectedName, points, estimatedPoint, delta, xLog, yLog);
        JFreeChart jfc = chart.getChart();
        jfc.getXYPlot().getRangeAxis().setAutoRange(true);
        jfc.getXYPlot().getDomainAxis().setAutoRange(true);

        if( jfc.getXYPlot().getRangeAxis() instanceof NumberAxis )
            ( (NumberAxis)jfc.getXYPlot().getRangeAxis() ).setAutoRangeIncludesZero(false);

        if( jfc.getXYPlot().getDomainAxis() instanceof NumberAxis )
            ( (NumberAxis)jfc.getXYPlot().getDomainAxis() ).setAutoRangeIncludesZero(false);

        BufferedImage chartImage = jfc.createBufferedImage(600, 400);
        ImageDataElement image = new ImageDataElement(selectedName + " chart.png", resultsFolder, chartImage);
        resultsFolder.put(image);
        return image.getCompletePath();
    }

    public static DataElementPath savePlotAsChart(DataCollection<DataElement> resultsFolder, List<Point> points, String selectedName,
            Point estimatedPoint, double delta, boolean xLog, boolean yLog)
    {
        Chart chart = createChart(selectedName, points, estimatedPoint, delta, xLog, yLog);
        ChartDataElement image = new ChartDataElement(selectedName + " chart", resultsFolder, chart);
        resultsFolder.put(image);
        return image.getCompletePath();
    }

    private static Chart createChart(String selectedName, List<Point> points, Point estimatedPoint, double delta, boolean xLog,
            boolean yLog)
    {
        Chart chart = new Chart();

        //init profile likelihood line
        int size = points.size();
        double[][] data = new double[size][2];
        for( int i = 0; i < size; i++ )
        {
            Point point = points.get(i);
            data[i][0] = point.getX();
            data[i][1] = point.getY();
        }
        chart.addSeries(createSeries(selectedName, data, true, true, Color.blue, Chart.CIRCLE));
        //init estimation line
        double objFuncEstimation = estimatedPoint.getY();
        double upperBound = objFuncEstimation + delta;

        data = new double[][] {{points.get(0).getX(), upperBound}, {points.get(points.size() - 1).getX(), upperBound}};
        chart.addSeries(createSeries("upper bound estimation", data, true, false, Color.red, null));

        //add estimated point
        data = new double[][] {{estimatedPoint.getX(), objFuncEstimation}};
        chart.addSeries(createSeries(selectedName + " estimated", data, false, true, Color.black, Chart.RECTANGLE));

        ChartOptions options = new ChartOptions();
        if( xLog )
            options.getXAxis().setTransform(Transform.LOGARITHM);
        if( yLog )
            options.getYAxis().setTransform(Transform.LOGARITHM);
        options.getXAxis().setLabel(selectedName);
        options.getYAxis().setLabel(Y_AXIS_LABEL);
        chart.setOptions(options);
        return chart;
    }

    private static ChartSeries createSeries(String name, double[][] data, boolean showLines, boolean showShapes, Color color, Shape shape)
    {
        ChartSeries series = new ChartSeries();
        series.getLines().setShow(showLines);
        series.getLines().setShapesVisible(showShapes);
        series.getBars().setShow(false);
        series.setData(data);
        series.setLabel(name);
        series.setColor(color);
        if( shape != null )
            series.setShape(shape);
        return series;
    }

}
