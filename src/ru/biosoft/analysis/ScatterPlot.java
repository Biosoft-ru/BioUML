package ru.biosoft.analysis;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

public class ScatterPlot extends AnalysisMethodSupport<ScatterPlotParameters>
{
    JFreeChart chart;
    XYDataset dataset;
    double xMin, xMax;

    public ScatterPlot(DataCollection<?> origin, String name)
    {
        super( origin, name, new ScatterPlotParameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        dataset = createXYDataset();
        if(dataset == null)
        {
            return null;
        }
        chart = ChartFactory.createScatterPlot( null, parameters.getXColumnLabel(), parameters.getYColumnLabel(), dataset,
                PlotOrientation.VERTICAL, parameters.isShowLegend(), false, false );
        makePlot();
        drawRegressionLine();
        return imageChart();
    }

    private XYDataset createXYDataset()
    {
        TableDataCollection inputTable = parameters.getInputTable().getDataElement( TableDataCollection.class );
        if(inputTable.getSize() == 0)
        {
            return null;
        }
        int yValIdx = findColIdx(inputTable,() -> parameters.getYColumn());
        int xValIdx = findColIdx(inputTable,() -> parameters.getXColumn());
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries(parameters.getYColumn() + " vs." + parameters.getXColumn() );
        List<Double> xVals = new ArrayList<>();
        for( RowDataElement row : inputTable )
        {
            Object[] values = row.getValues();
            Object xValueObj = values[xValIdx];
            Object yValueObj = values[yValIdx];
            if( xValueObj instanceof Number && yValueObj instanceof Number )
            {
                Number xValue = (Number)xValueObj;
                Number yValue = (Number)yValueObj;
                if( parameters.isSkipNaN() && ( isNaN( xValue ) || isNaN( yValue ) ) )
                    continue;
                series.add( xValue, yValue );
                xVals.add( xValue.doubleValue() );
            }
        }
        if( series.isEmpty() )
            return null;
        xMin = Collections.min( xVals );
        xMax = Collections.max( xVals );
        dataset.addSeries( series );
        return dataset;
    }

    private boolean isNaN(Number value)
    {
        return Double.isNaN( value.doubleValue() );
    }
    
    int findColIdx(TableDataCollection table, Supplier<String>findCol) {
        int idx = 0;
        if(findCol.get() != null) {
            idx = table.getColumnModel().getColumnIndex( findCol.get() );
        }
        return idx;
    }

    private void makePlot()
    {
        Plot plot = chart.getPlot();
        plot.setBackgroundPaint( Color.WHITE );
        plot.setOutlineVisible( false );
        chart.setBackgroundPaint( Color.WHITE );
    }

    private ImageDataElement imageChart()
    {
        DataElementPath imagePath = parameters.getOutputChart();
        BufferedImage image = chart.createBufferedImage( 600, 600 );
        ImageDataElement imageDE = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), image );
        imagePath.save( imageDE );
        return imageDE;
    }

    private void drawRegressionLine()
    {
        double regressionParameters[] = Regression.getOLSRegression( dataset, 0 );
        LineFunction2D linefunction2d = new LineFunction2D( regressionParameters[0], regressionParameters[1] );
        XYDataset regressionDataSet = DatasetUtils.sampleFunction2D( linefunction2d, xMin, xMax, 50, "Fitted Regression Line" );
        XYPlot xyplot = chart.getXYPlot();
        xyplot.setDataset( 1, regressionDataSet );
        XYLineAndShapeRenderer xylineandshaperenderer = new XYLineAndShapeRenderer( true, false );
        xylineandshaperenderer.setSeriesPaint( 0, Color.BLUE );
        xyplot.setRenderer( 1, xylineandshaperenderer );
    }
}
