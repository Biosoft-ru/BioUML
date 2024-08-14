package biouml.model.javascript;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

public class StatisticalBarChart extends BarChart
{
    public StatisticalBarChart()
    {
        dataset = new DefaultStatisticalCategoryDataset();

        renderer = new StatisticalBarRenderer();
        ((StatisticalBarRenderer) renderer).setErrorIndicatorPaint( Color.black );
        renderer.setItemMargin( 0.0 );
    }

    public void addBars(String legend, String tablePath, String mean, String sd)
    {
        TableDataCollection data = getData( tablePath );
        addBars(legend, data, mean, sd);
    }

    public void addBars(String legend, TableDataCollection data, String mean, String sd)
    {
        if( checkColumn( data, mean ) && checkColumn( data, sd ) )
        {
            for( RowDataElement row : data )
            {
                ( (DefaultStatisticalCategoryDataset)dataset ).add( (double)row.getValue( mean ), (double)row.getValue( sd ), legend,
                        row.getName() );
            }
        }
    }

    @Override
    public BufferedImage getImage(int width, int height)
    {
        JFreeChart chart = generateChart();
        ( (CategoryPlot)chart.getPlot() ).setRenderer( renderer );
        return chart.createBufferedImage( width, height );
    }
}
