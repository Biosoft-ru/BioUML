package biouml.model.javascript;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;

public class BarChart
{
    private String title = "";
    private String xTitle = "";
    private String yTitle = "";

    private double yFrom = Double.NaN;
    private double yTo = Double.NaN;
    private CategoryLabelPositions lablePosition = CategoryLabelPositions.createUpRotationLabelPositions( Math.PI / 6.0 );

    protected static final Logger log = Logger.getLogger( BarChart.class.getName() );

    private Font lableFont = new Font( "Arial", Font.PLAIN, 12 );

    protected CategoryDataset dataset;

    protected BarRenderer renderer;

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setYTitle(String yTitle)
    {
        this.yTitle = yTitle;
    }

    public void setXTitle(String xTitle)
    {
        this.xTitle = xTitle;
    }

    public void setYFrom(double yFrom)
    {
        this.yFrom = yFrom;
    }

    public void setYTo(double yTo)
    {
        this.yTo = yTo;
    }

    public void setLablePosition(CategoryLabelPositions lablePosition)
    {
        this.lablePosition = lablePosition;
    }

    public void setLableFont(Font lableFont)
    {
        this.lableFont = lableFont;
    }

    public BarRenderer getRenderer()
    {
        return this.renderer;
    }

    public void addBars(String legend, String tablePath, String column)
    {
        //TODO: to implement the general case if necessary
    }

    protected TableDataCollection getData(String tablePath)
    {
        return DataElementPath.create( tablePath ).getDataElement( TableDataCollection.class );
    }

    protected boolean checkColumn(TableDataCollection data, String column)
    {
        if( !data.getColumnModel().hasColumn( column ) )
        {
            log.log( Level.SEVERE, "Table data collection " + data.getName() + " does not contain the column " + column );
            return false;
        }
        if( !data.getColumnModel().getColumn( column ).getType().equals( DataType.Float ) )
        {
            log.log( Level.SEVERE, "Incorrect type of data in the column '" + column + "'" );
            return false;
        }
        return true;
    }

    protected JFreeChart generateChart()
    {
        JFreeChart chart = ChartFactory.createLineChart( title, xTitle, yTitle, dataset, PlotOrientation.VERTICAL, true, true, false );

        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        plot.setBackgroundPaint( Color.white );
        plot.setRangeGridlinePaint( Color.black );

        NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();
        rangeAxis.setStandardTickUnits( NumberAxis.createStandardTickUnits() );
        if( !Double.isNaN( yFrom ) )
            rangeAxis.setLowerBound( yFrom );
        if( !Double.isNaN( yTo ) )
            rangeAxis.setUpperBound( yTo );
        rangeAxis.setLabelFont( lableFont );
        rangeAxis.setTickLabelFont( lableFont );

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryMargin( 0.2 );
        domainAxis.setCategoryLabelPositions( lablePosition );
        domainAxis.setMaximumCategoryLabelLines( 2 );
        domainAxis.setLabelFont( lableFont );
        domainAxis.setTickLabelFont( lableFont );

        return chart;
    }

    public BufferedImage getImage()
    {
        return getImage( 550, 350 );
    }

    public BufferedImage getImage(int width, int height)
    {
        JFreeChart chart = generateChart();
        return chart.createBufferedImage( width, height );
    }
}
