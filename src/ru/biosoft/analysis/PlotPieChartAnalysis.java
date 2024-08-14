package ru.biosoft.analysis;

import java.awt.Color;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.TreeSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.DefaultPieDataset;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

@ClassIcon ( "resources/PlotPieChart.gif" )
public class PlotPieChartAnalysis extends AnalysisMethodSupport<ChartAnalysisParameters>
{
    public PlotPieChartAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new ChartAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection inputTable = parameters.getInputTable().getDataElement( TableDataCollection.class );
        if( inputTable.getSize() == 0 )
        {
            return null;
        }

        int valIdx = inputTable.getColumnModel().getColumnIndex( parameters.getColumn() );
        int labelIdx = 0;
        if(parameters.getLabelsColumn() != null)
        {
            labelIdx = inputTable.getColumnModel().getColumnIndex( parameters.getLabelsColumn() );
        }

        TreeSet<DataPoint> head = new TreeSet<>();
        double removedSum = 0; boolean wasRemoved = false;
        for( RowDataElement row : inputTable )
        {
            Object[] values = row.getValues();
            String label = String.valueOf( values[labelIdx] );
            Number value = (Number)values[valIdx];
            DataPoint dataPoint = new DataPoint( label, Math.abs( value.doubleValue() ) );
            head.add( dataPoint );
            if(head.size() > parameters.getMaxPieces())
            {
                Iterator<DataPoint> it = head.descendingIterator();
                DataPoint removed = it.next();
                removedSum += removed.value;
                it.remove();
                wasRemoved = true;
            }
        }
        if(wasRemoved)
        {
            Iterator<DataPoint> it = head.descendingIterator();
            DataPoint removed = it.next();
            removedSum += removed.value;
            it.remove();
            if(parameters.isAddRemaininig())
            {
                head.add( new DataPoint( "Other", removedSum ) );
            }
        }

        DataElementPath imagePath = parameters.getOutputChart();
        DefaultPieDataset dataset = new DefaultPieDataset();
        for( DataPoint data : head )
        {
            dataset.setValue( data.label, data.value );
        }

        JFreeChart chart = ChartFactory.createPieChart( null, dataset, false, true, false );
        Plot plot = chart.getPlot();

        if( plot instanceof PiePlot )
        {
            ( (PiePlot)plot ).setStartAngle( 0 );
        }
        plot.setDrawingSupplier( getDrawingSupplier( parameters.getPaletteName() ));
        plot.setBackgroundPaint( Color.WHITE );
        plot.setOutlineVisible( false );
        chart.setBackgroundPaint( Color.WHITE );

        BufferedImage image = chart.createBufferedImage( 600, 600 );
        ImageDataElement imageDE = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), image );
        imagePath.save( imageDE );
        return imageDE;
    }

    protected static DrawingSupplier getDrawingSupplier(String palette)
    {
        if( palette.equals( ChartAnalysisParameters.ADVANCED_PALETTE ) )
        {
            return new CustomColorDrawingSupplier( niceColors ) ;
        }
        else if( palette.equals( ChartAnalysisParameters.PASTEL_PALETTE ))
        {
            return new PastelPaletteDrawingSupplier( 0.5f ) ;
        }
        else
        {
            return new DefaultDrawingSupplier();
        }
    }

    public JFreeChart getChart(DefaultPieDataset dataset)
    {
        JFreeChart chart = ChartFactory.createPieChart( null, dataset, false, true, false );
        Plot plot = chart.getPlot();
        plot.setDrawingSupplier( new PastelPaletteDrawingSupplier( 0.5f ) );
        chart.setBackgroundPaint( Color.WHITE );
        return chart;
    }

    private static class DataPoint implements Comparable<DataPoint>
    {
        String label;
        double value;

        DataPoint(String label, double value)
        {
            this.label = label;
            this.value = value;
        }

        @Override
        public int compareTo(DataPoint o)
        {
            int valueCmp = -Double.compare( value, o.value );
            if( valueCmp != 0 )
            {
                return valueCmp;
            }
            return label.compareTo( o.label );
        }
    }

    /*
     * Generate color sequence for chart. Use HSV color space (Hue, Saturation, Value).
     * Hue for the next color is calculated using the golden ratio as the spacing,
     * allowing nice difference between neighbor colors. First and last color could be similar though.
     * Saturation and value are the same for all colors and can be set via constructor.
     */
    public static class PastelPaletteDrawingSupplier extends DefaultDrawingSupplier
    {
        float hue = 0.5f;
        float saturation = 0.3f;
        float hsvalue = 0.99f;
        float golden_ratio_conjugate = 0.618033988749895f;

        public PastelPaletteDrawingSupplier(float hue)
        {
            this.hue = hue;
        }

        public PastelPaletteDrawingSupplier(float hue, float saturation, float hsvalue)
        {
            this.hue = hue;
            this.saturation = saturation;
            this.hsvalue = hsvalue;
        }

        @Override
        public Paint getNextPaint()
        {
            float hue = ( this.hue + golden_ratio_conjugate ) % 1;
            this.hue = hue;
            Color c = Color.getHSBColor( hue, saturation, hsvalue );
            return c;
        }
    }

    private static Color[] niceColors = new Color[] {new Color( 176, 21, 19 ), new Color( 234, 99, 18 ), new Color( 230, 183, 41 ),
            new Color( 106, 172, 144 ), new Color( 84, 132, 154 ), new Color( 158, 94, 155 ), new Color( 107, 13, 11 ),
            new Color( 140, 60, 11 ), new Color( 147, 114, 17 ), new Color( 36, 92, 67 ), new Color( 41, 67, 94 ), new Color( 94, 56, 93 )};


    /*
     * Generate color sequence for chart. Use predefined array of colors.
     * Number of colors is limited by array size. Colors will be repeated in cycle.
     */
    public static class CustomColorDrawingSupplier extends DefaultDrawingSupplier
    {
        private Color[] colors;
        private int nextColorNumber = 0;

        public CustomColorDrawingSupplier(Color[] colors)
        {
            this.colors = colors;
        }

        @Override
        public Paint getNextPaint()
        {
            return colors[ ( nextColorNumber++ ) % colors.length];
        }
    }
}
