package ru.biosoft.analysis;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

@ClassIcon ( "resources/PlotBarChart.gif" )
public class PlotBarChartAnalysis extends AnalysisMethodSupport<PlotBarChartAnalysis.Parameters>
{
    public PlotBarChartAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
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
        int labelIdx = -1;
        if( !Parameters.noLabelsSelection( parameters.getLabelsColumn() ) )
        {
            labelIdx = inputTable.getColumnModel().getColumnIndex( parameters.getLabelsColumn() );
        }

        TreeSet<DataPoint> head = new TreeSet<>();
        double removedSum = 0; boolean wasRemoved = false;
        for( RowDataElement row : inputTable )
        {
            Object[] values = row.getValues();
            String label = labelIdx == -1 ? row.getName() : String.valueOf( values[labelIdx] );
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
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for( DataPoint data : head )
        {
            dataset.addValue( data.value, data.label, "" );
        }

        String xAxisLabel = parameters.getXAxisLabel();
        String yAxisLabel = parameters.getYAxisLabel();

        JFreeChart chart = ChartFactory.createBarChart( null, xAxisLabel.isEmpty() ? null : xAxisLabel,
                yAxisLabel.isEmpty() ? null : yAxisLabel, dataset );
        Plot plot = chart.getPlot();

        plot.setDrawingSupplier( PlotPieChartAnalysis.getDrawingSupplier( parameters.getPaletteName() ));
        plot.setBackgroundPaint( Color.WHITE );
        plot.setOutlineVisible( false );
        chart.setBackgroundPaint( Color.WHITE );
        ( (BarRenderer)chart.getCategoryPlot().getRenderer() ).setBarPainter( new StandardBarPainter() );

        BufferedImage image = chart.createBufferedImage( 600, 500 );
        ImageDataElement imageDE = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), image );
        imagePath.save( imageDE );
        return imageDE;
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

    @SuppressWarnings ( "serial" )
    public static class Parameters extends ChartAnalysisParameters
    {
        @Override
        public void setColumn(String column)
        {
            super.setColumn( column );
            if( isAutoFillLabels() )
                setYAxisLabel( column );
        }

        @Override
        public void setLabelsColumn(String labelsColumn)
        {
            super.setLabelsColumn( labelsColumn );
            if( isAutoFillLabels() )
            {
                if( noLabelsSelection( labelsColumn ) )
                    labelsColumn = "ID";
                setXAxisLabel( labelsColumn );
            }
        }

        private boolean autoFillLabels = true;
        @PropertyName ( "Use selected columns as axis labels" )
        public boolean isAutoFillLabels()
        {
            return autoFillLabels;
        }
        public void setAutoFillLabels(boolean autoFillLabels)
        {
            this.autoFillLabels = autoFillLabels;
            if( autoFillLabels )
            {
                setYAxisLabel( getColumn() );
                String labelsColumn = getLabelsColumn();
                if( noLabelsSelection( labelsColumn ) )
                    labelsColumn = "ID";
                setXAxisLabel( labelsColumn );
            }
        }

        private String xAxisLabel = "ID";
        @PropertyName ( "Category axis label (x-axis)" )
        public @Nonnull String getXAxisLabel()
        {
            if( xAxisLabel != null )
                return xAxisLabel;
            return "";
        }
        public void setXAxisLabel(String xAxisLabel)
        {
            this.xAxisLabel = xAxisLabel;
        }

        private String yAxisLabel = "";
        @PropertyName ( "Values axis label (y-axis)" )
        public @Nonnull String getYAxisLabel()
        {
            if( yAxisLabel != null )
                return yAxisLabel;
            return "";
        }
        public void setYAxisLabel(String yAxisLabel)
        {
            this.yAxisLabel = yAxisLabel;
        }

        static boolean noLabelsSelection(String labelsColumn)
        {
            return labelsColumn == null || labelsColumn.isEmpty() || ColumnNameSelector.NONE_COLUMN.equals( labelsColumn );
        }
    }

    public static class ParametersBeanInfo extends ChartAnalysisParametersBeanInfo
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            int index = findPropertyIndex( "outputChart" );
            if( index != properties.size() )
            {
                property( "yAxisLabel" ).readOnly( "isAutoFillLabels" ).add( index );
                property( "xAxisLabel" ).readOnly( "isAutoFillLabels" ).add( index );
                property( "autoFillLabels" ).add( index );
            }
            else
            {
                add( "autoFillLabels" );
                addReadOnly( "xAxisLabel", "isAutoFillLabels" );
                addReadOnly( "yAxisLabel", "isAutoFillLabels" );
            }
        }
    }
}
