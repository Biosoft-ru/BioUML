package ru.biosoft.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.annotation.Nonnull;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

public class MAPlotAnalysis extends AnalysisMethodSupport<ru.biosoft.analysis.MAPlotAnalysis.Parameters>
{

    private static final Shape circle = new Ellipse2D.Float( 0, 0, 5, 5 );
    JFreeChart chart;
    XYDataset dataset;
    private Color upColor = new Color( 129, 128, 129 );
    private Color downColor = new Color( 98, 146, 199 );
    private Color middleColor = new Color( 229, 228, 229 );

    private double minXValue = -10, maxXValue = 10;

    public MAPlotAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }


    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        dataset = createXYDataset();
        if( dataset == null )
        {
            return null;
        }
        chart = ChartFactory.createScatterPlot( null, parameters.getColumnA(), parameters.getColumnM(), dataset, PlotOrientation.VERTICAL, true, false, false );
        makePlot();
        //drawCutoffLines();
        return imageChart();
    }



    private XYDataset createXYDataset()
    {
        TableDataCollection inputTable = parameters.getInputTable().getDataElement( TableDataCollection.class );
        if( inputTable.getSize() == 0 )
        {
            return null;
        }

        int aIdx = inputTable.getColumnModel().getColumnIndex( parameters.getColumnA() );
        int mIdx = inputTable.getColumnModel().getColumnIndex( parameters.getColumnM() );
        boolean useAdditionalCutoff = parameters.isUseCutoffs() && parameters.getCutoffColumn() != null;
        int pvalIdx = useAdditionalCutoff ? inputTable.getColumnModel().getColumnIndex( parameters.getCutoffColumn() ) : -1;
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries seriesUp = new XYSeries( "Upregulated" );
        XYSeries seriesDown = new XYSeries( "Downregulated" );
        XYSeries seriesMiddle = new XYSeries( "Middle" );
        Double pValCutoff = useAdditionalCutoff ? parameters.getPvalueCutoff() : Double.MAX_VALUE;
        for ( RowDataElement row : inputTable )
        {
            Object[] values = row.getValues();
            Object xValueObj = values[aIdx];
            Object yValueObj = values[mIdx];
            if( xValueObj instanceof Number && yValueObj instanceof Number )
            {
                Number xValue = (Number) xValueObj;
                Number yValue = (Number) yValueObj;
                if( (isNaN( xValue ) || isNaN( yValue )) )
                    continue;
                Double x = xValue.doubleValue();
                Double y = yValue.doubleValue();
                if( parameters.isUseCutoffs() )
                {
                    if( useAdditionalCutoff )
                    {
                        Double pval = ((Number) values[pvalIdx]).doubleValue();
                        if( pval > pValCutoff )
                            seriesMiddle.add( x, y );
                        else if( y < parameters.getDownCutoff() )
                            seriesDown.add( x, y );
                        else if( y > parameters.getUpCutoff() )
                            seriesUp.add( x, y );
                        else
                            seriesMiddle.add( x, y );
                    }
                    else
                    {
                        if( y < parameters.getDownCutoff() )
                            seriesDown.add( x, y );
                        else if( y > parameters.getUpCutoff() )
                            seriesUp.add( x, y );
                        else
                            seriesMiddle.add( x, y );
                    }
                }
                else
                {
                    seriesUp.add( x, y );
                }

            }
        }
        dataset.addSeries( seriesUp );
        dataset.addSeries( seriesDown );
        dataset.addSeries( seriesMiddle );
        return dataset;
    }

    private boolean isNaN(Number value)
    {
        return Double.isNaN( value.doubleValue() );
    }

    private void makePlot()
    {
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint( Color.WHITE );
        plot.setOutlineVisible( false );
        chart.setBackgroundPaint( Color.WHITE );
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Configure scatter plot series (show shapes, no lines)
        //Up-regulated
        renderer.setSeriesLinesVisible( 0, false );
        renderer.setSeriesShapesVisible( 0, true );
        renderer.setSeriesShape( 0, circle );
        renderer.setSeriesPaint( 0, upColor );

        //Down-regulated
        renderer.setSeriesLinesVisible( 1, false );
        renderer.setSeriesShapesVisible( 1, true );
        renderer.setSeriesShape( 1, circle );
        renderer.setSeriesPaint( 1, downColor );

        //Not-significant
        renderer.setSeriesLinesVisible( 2, false );
        renderer.setSeriesShapesVisible( 2, true );
        renderer.setSeriesShape( 2, circle );
        renderer.setSeriesPaint( 2, middleColor );

        plot.setRenderer( renderer );

        chart.removeLegend();

        //NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        //domainAxis.setRange( minXValue, maxXValue );
    }

    private void drawCutoffLines()
    {
        XYPlot xyplot = chart.getXYPlot();

        ValueMarker marker = new ValueMarker( parameters.getDownCutoff() );
        marker.setPaint( Color.GRAY );
        marker.setStroke( new BasicStroke( 1.0f ) );
        //marker.setLabel( "Important Event" ); // Optional label
        xyplot.addDomainMarker( marker );

        marker = new ValueMarker( parameters.getUpCutoff() );
        marker.setPaint( Color.GRAY );
        marker.setStroke( new BasicStroke( 1.0f ) );
        xyplot.addDomainMarker( marker );

        marker = new ValueMarker( -Math.log10( parameters.getPvalueCutoff() ) );
        marker.setPaint( Color.GRAY );
        marker.setStroke( new BasicStroke( 1.0f ) );
        xyplot.addRangeMarker( marker );

    }

    private ImageDataElement imageChart()
    {
        DataElementPath imagePath = parameters.getOutputChart();
        BufferedImage image = chart.createBufferedImage( 600, 600 );
        ImageDataElement imageDE = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), image );
        imagePath.save( imageDE );
        return imageDE;
    }

    @SuppressWarnings("serial")
    public static class Parameters extends ChartAnalysisParameters
    {

        private String columnA, columnM;
        private String cutoffColumn;
        private Double downCutoff = -0.59, upCutoff = 0.59, pvalueCutoff = 0.1;
        private boolean useCutoffs = true;
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

        @PropertyName("Use selected columns as axis labels")
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

        @PropertyName("Category axis label (x-axis)")
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

        @PropertyName("Values axis label (y-axis)")
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

        @PropertyName("A column")
        @PropertyDescription("X axis values - logCPM for EdgeR output")
        public String getColumnA()
        {
            return columnA;
        }

        public void setColumnA(String aColumn)
        {
            String oldValue = this.columnA;
            this.columnA = aColumn;
            firePropertyChange( "columnA", oldValue, this.columnA );
        }

        @PropertyName("M column")
        @PropertyDescription("Y axis values - logFC for EdgeR output")
        public String getColumnM()
        {
            return columnM;
        }

        public void setColumnM(String mColumn)
        {
            String oldValue = this.columnM;
            this.columnM = mColumn;
            firePropertyChange( "columnM", oldValue, this.columnM );
        }

        @PropertyName("Downregulated logFC cutoff")
        public Double getDownCutoff()
        {
            return downCutoff;
        }

        public void setDownCutoff(Double downCutoff)
        {
            Double oldValue = this.downCutoff;
            this.downCutoff = downCutoff;
            firePropertyChange( "downCutoff", oldValue, this.downCutoff );
        }

        @PropertyName("Upregulated logFC cutoff")
        public Double getUpCutoff()
        {
            return upCutoff;
        }

        public void setUpCutoff(Double upCutoff)
        {
            Double oldValue = this.upCutoff;
            this.upCutoff = upCutoff;
            firePropertyChange( "upCutoff", oldValue, this.upCutoff );
        }

        @PropertyName("Additional cutoff (p-value)")
        public Double getPvalueCutoff()
        {
            return pvalueCutoff;
        }

        public void setPvalueCutoff(Double pvalueCutoff)
        {
            Double oldValue = this.pvalueCutoff;
            this.pvalueCutoff = pvalueCutoff;
            firePropertyChange( "pvalueCutoff", oldValue, this.pvalueCutoff );
        }

        @PropertyName("Cutoff column")
        @PropertyDescription("If specified, only values lower than additional cutoff will be coloured")
        public String getCutoffColumn()
        {
            return cutoffColumn;
        }

        public void setCutoffColumn(String cutoffColumn)
        {
            String oldValue = this.cutoffColumn;
            this.cutoffColumn = cutoffColumn;
            firePropertyChange( "cutoffColumn", oldValue, this.cutoffColumn );
        }

        @PropertyName("Use cutoffs")
        @PropertyDescription("If set, M values higher than Up cutoff and lower than Down cutoff will be coloured in different colours")
        public boolean isUseCutoffs()
        {
            return useCutoffs;
        }

        public void setUseCutoffs(boolean useCutoffs)
        {
            boolean oldValue = this.useCutoffs;
            this.useCutoffs = useCutoffs;
            firePropertyChange( "useCutoffs", oldValue, this.useCutoffs );
        }

        public boolean isCutoffHidden()
        {
            return !useCutoffs;
        }

        public boolean isPvalueCutoffHidden()
        {
            return isCutoffHidden() && cutoffColumn == null;
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
            property( "inputTable" ).inputElement( TableDataCollection.class ).add();
            add( ColumnNameSelector.registerNumericSelector( "columnA", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerNumericSelector( "columnM", beanClass, "inputTable", true ) );
            add( "useCutoffs" );
            addHidden( "downCutoff", "isCutoffHidden" );
            addHidden( "upCutoff", "isCutoffHidden" );
            PropertyDescriptorEx pde = ColumnNameSelector.registerNumericSelector( "cutoffColumn", beanClass, "inputTable", true );
            pde.setHidden( beanClass.getMethod( "isCutoffHidden" ) );
            add( pde );
            //property( "cutoffColumn").hidden("isCutoffHidden" );
            property( "pvalueCutoff" ).hidden( "isPvalueCutoffHidden" ).add();
            property( "outputChart" ).outputElement( ImageDataElement.class ).add();
        }
    }
}
