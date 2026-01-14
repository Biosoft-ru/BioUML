package ru.biosoft.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.DotPlotAnalysis.Parameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

public class DotPlotAnalysis extends AnalysisMethodSupport<Parameters>
{
    private JFreeChart chart;
    private Color circleColor = new Color( 199, 2, 58 );

    public DotPlotAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        chart = ChartFactory.createScatterPlot( null, "log10 P-value", "", null, PlotOrientation.VERTICAL, true, false, false );
        makePlot();
        drawCutoffLines();
        return imageChart();
    }

    private void drawCutoffLines()
    {
        XYPlot xyplot = chart.getXYPlot();


    }

    private Object imageChart()
    {
        DataElementPath imagePath = parameters.getOutputChart();
        BufferedImage image = chart.createBufferedImage( parameters.getImageWidth(), parameters.getImageHeight() );
        ImageDataElement imageDE = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), image );
        imagePath.save( imageDE );
        return imageDE;
    }


    private void makePlot()
    {
        TableDataCollection inputTable = parameters.getInputTable().getDataElement( TableDataCollection.class );
        if( inputTable.getSize() == 0 )
        {
            return;
        }

        int pvalIdx = inputTable.getColumnModel().getColumnIndex( parameters.getLogPvalueColumn() );
        int sizeIdx = inputTable.getColumnModel().getColumnIndex( parameters.getGeneCountColumn() );
        int lblIdx = inputTable.getColumnModel().getColumnIndex( parameters.getCategoryColumn() );
        boolean needLog = parameters.isNeedLog();
        List<DotPlotItem> items = new ArrayList<>();
        for ( RowDataElement row : inputTable )
        {
            Object[] values = row.getValues();
            Object xValueObj = values[pvalIdx];
            if( xValueObj instanceof Number )
            {
                Number xValue = (Number) xValueObj;
                Double x = xValue.doubleValue();
                if( Double.isFinite( x ) )
                {
                    if( needLog )
                        x = -Math.log10( x );
                    DotPlotItem item = new DotPlotItem( x, ((Number) (values[sizeIdx])).intValue(), (String) (values[lblIdx]) );
                    items.add( item );
                }
            }
        }

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint( Color.WHITE );
        plot.setOutlineVisible( false );
        chart.setBackgroundPaint( Color.WHITE );

        double cutoff = parameters.getPvalueCutoff();
        if( parameters.isNeedLog() )
            cutoff = -Math.log10( cutoff );

        int num = parameters.getNumTopCategories();
        items.sort( (p1, p2) -> {
            return p1.compareTo( p2 );
        } );
        int maxSize = items.stream().limit( num ).mapToInt( item -> item.size ).max().orElse( 0 );
        double maxPval = items.stream().limit( num ).mapToDouble( item -> item.val ).max().orElse( 0 );
        maxPval = Math.max( maxPval, cutoff );

        double minPval = items.stream().limit( num ).mapToDouble( item -> item.val ).min().orElse( 0 );
        minPval = Math.min( minPval, cutoff );
        //??
        float maxXVal = (float) (maxPval * 1.1);
        float minXval = (float) (minPval * 0.9);
        float rangeX = maxXVal - minXval;
        float rangeY = num;
        float maxYVal = (num + 1) * maxSize;
        String[] labels = new String[num + 1];

        labels[0] = "";
        for ( int i = 0; i < num; i++ )
        {
            DotPlotItem item = items.get( i );

            labels[i + 1] = item.label;
        }
        SymbolAxis yaxis = new SymbolAxis( null, labels );
        //TODO: code below was copied to support multi-line tick labels from https://www.jfree.org/forum/viewtopic.php?t=24926
        // but the plot will be shifted for vertical multi-lines, as if they are one line
        // fix the code to remove extra shift for all - ticks, axes and DataArea
        //        {
        //
        //            @Override
        //            protected AxisState drawTickMarksAndLabels(Graphics2D g2, double cursor, Rectangle2D plotArea, Rectangle2D dataArea, RectangleEdge edge)
        //            {
        //                AxisState state = new AxisState( cursor );
        //
        //                if( isAxisLineVisible() )
        //                {
        //                    drawAxisLine( g2, cursor, dataArea, edge );
        //                }
        //
        //                double ol = getTickMarkOutsideLength();
        //                double il = getTickMarkInsideLength();
        //
        //                List ticks = refreshTicks( g2, state, dataArea, edge );
        //                state.setTicks( ticks );
        //                g2.setFont( getTickLabelFont() );
        //                Iterator iterator = ticks.iterator();
        //                List<ValueTick> shortenedTicks = new ArrayList<>();
        //
        //                // remember the max number of lines used in any label
        //                int maxLinesUsed = 0;
        //
        //                while ( iterator.hasNext() )
        //                {
        //                    ValueTick tick = (ValueTick) iterator.next();
        //                    if( isTickLabelsVisible() )
        //                    {
        //                        g2.setPaint( getTickLabelPaint() );
        //                        float[] anchorPoint = calculateAnchorPoint( tick, cursor, dataArea, edge );
        //
        //                        g2.draw( plotArea );
        //                        g2.setPaint( Color.green );
        //                        g2.draw( dataArea );
        //                        g2.setPaint( getTickLabelPaint() );
        //                        // split by "\n" and draw text in a new line for each result
        //                        String tickText = getMultiLineLabel( tick.getText() );
        //                        int line = 1;
        //                        for ( String tickTextLine : tickText.split( "\n" ) )
        //                        {
        //                            float x = anchorPoint[0];
        //                            // one row down...
        //                            float y = anchorPoint[1] + line * g2.getFont().getSize();
        //                            TextUtils.drawRotatedString( tickTextLine, g2, x, y, tick.getTextAnchor(), tick.getAngle(), tick.getRotationAnchor() );
        //                            if( tick instanceof NumberTick )
        //                            {
        //                                NumberTick t = new NumberTick( tick.getValue(), tickTextLine, tick.getTextAnchor(), tick.getRotationAnchor(), tick.getAngle() );
        //                                shortenedTicks.add( t );
        //                            }
        //                            line++;
        //                        }
        //                        // if we used more lines than any time before remember it
        //                        if( line > maxLinesUsed )
        //                        {
        //                            maxLinesUsed = line;
        //                        }
        //                        if( !(tick instanceof NumberTick) )
        //                        {
        //                            shortenedTicks.add( tick );
        //                        }
        //                    }
        //
        //                    if( isTickMarksVisible() && tick.getTickType().equals( TickType.MAJOR ) )
        //                    {
        //                        float xx = (float) valueToJava2D( tick.getValue(), dataArea, edge );
        //                        Line2D mark = null;
        //                        g2.setStroke( getTickMarkStroke() );
        //                        g2.setPaint( getTickMarkPaint() );
        //                        if( edge == RectangleEdge.LEFT )
        //                        {
        //                            mark = new Line2D.Double( cursor - ol, xx, cursor + il, xx );
        //                        }
        //                        else if( edge == RectangleEdge.RIGHT )
        //                        {
        //                            mark = new Line2D.Double( cursor + ol, xx, cursor - il, xx );
        //                        }
        //                        else if( edge == RectangleEdge.TOP )
        //                        {
        //                            mark = new Line2D.Double( xx, cursor - ol, xx, cursor + il );
        //                        }
        //                        else if( edge == RectangleEdge.BOTTOM )
        //                        {
        //                            mark = new Line2D.Double( xx, cursor + ol, xx, cursor - il );
        //                        }
        //                        g2.draw( mark );
        //                    }
        //                }
        //
        //                // need to work out the space used by the tick labels...
        //                // so we can update the cursor...
        //                // patched using maxLinesUsed => we need more space because of multiple lines
        //                double used = 0.0;
        //                if( isTickLabelsVisible() )
        //                {
        //                    if( edge == RectangleEdge.LEFT )
        //                    {
        //                        used += findMaximumTickLabelWidth( shortenedTicks, g2, plotArea, isVerticalTickLabels() );
        //                        state.cursorLeft( used );
        //                    }
        //                    else if( edge == RectangleEdge.RIGHT )
        //                    {
        //                        used = findMaximumTickLabelWidth( shortenedTicks, g2, plotArea, isVerticalTickLabels() );
        //                        state.cursorRight( used );
        //                    }
        //                    else if( edge == RectangleEdge.TOP )
        //                    {
        //                        used = findMaximumTickLabelHeight( ticks, g2, plotArea, isVerticalTickLabels() ) * maxLinesUsed;
        //                        state.cursorUp( used );
        //                    }
        //                    else if( edge == RectangleEdge.BOTTOM )
        //                    {
        //                        used = findMaximumTickLabelHeight( ticks, g2, plotArea, isVerticalTickLabels() ) * maxLinesUsed;
        //                        state.cursorDown( used );
        //                    }
        //                }
        //
        //                return state;
        //            }
        //
        //        };
        yaxis.setGridBandsVisible( true );
        yaxis.setAutoTickUnitSelection( false, false );
        yaxis.setTickMarkOutsideLength( 5.0f );
        yaxis.setTickMarkInsideLength( 5.0f );
        yaxis.setRange( 0.5, num + 0.5 );
        //yaxis.setVerticalTickLabels( true );
        plot.setRangeAxis( yaxis );
        
        NumberAxis xaxis = new NumberAxis( (needLog ? "- log10 " : "") + parameters.getLogPvalueColumn() );
        xaxis.setRange( minXval, maxXVal );
        plot.setDomainAxis( xaxis );

        ChartRenderingInfo chartInfo = new ChartRenderingInfo();
        chart.createBufferedImage( parameters.getImageWidth(), parameters.getImageHeight(), chartInfo );
        Rectangle2D dataArea = chartInfo.getPlotInfo().getDataArea();

        //cutoff line
        ValueMarker marker = new ValueMarker( cutoff );
        marker.setPaint( Color.GRAY );
        marker.setStroke( new BasicStroke( 1.0f ) );
        plot.addDomainMarker( marker );

        //now place circles calculating x size by Java2D coordinates
        for ( int i = 0; i < num; i++ )
        {
            DotPlotItem item = items.get( i );
            float yrad = ((float) item.size) / (2.0f * maxSize);
            float xrad = (float) (yrad * rangeX * dataArea.getHeight() / (rangeY * dataArea.getWidth()));
            XYShapeAnnotation shape = new XYShapeAnnotation( new Ellipse2D.Float( (float) item.val - xrad, i + 1 - yrad, xrad * 2, yrad * 2 ), new BasicStroke( 1.0f ),
                    circleColor, circleColor );
            plot.addAnnotation( shape );
        }
    }

    private String getMultiLineLabel(String label)
    {
        int maxLength = 10;
        if( label.length() < maxLength )
            return label;
        return label.replaceAll( " ", "\n" );
    }

    private static class DotPlotItem implements Comparable<DotPlotItem>
    {
        public double val;
        public int size;
        public String label;

        public DotPlotItem(double v, int s, String l)
        {
            val = v;
            size = s;
            label = l;
        }

        @Override
        public int compareTo(DotPlotItem o)
        {
            int valueCmp = -Double.compare( val, o.val );
            if( valueCmp != 0 )
            {
                return valueCmp;
            }
            return Integer.compare( size, o.size );
        }
    }

    @SuppressWarnings("serial")
    public static class Parameters extends ChartAnalysisParameters
    {
        //TODO: rename variables
        private String geneCountColumn, logPvalueColumn, categoryColumn;
        private boolean needLog = true;
        private Double pvalueCutoff = 0.1;
        private int imageWidth = 1200, imageHeight = 1200;
        private int numTopCategories = 10;

        @PropertyName("Dot size column")
        @PropertyDescription("Value to be used as dot size. For example, number of hits in functional classification")
        public String getGeneCountColumn()
        {
            return geneCountColumn;
        }

        public void setGeneCountColumn(String geneCountColumn)
        {
            String oldValue = this.geneCountColumn;
            this.geneCountColumn = geneCountColumn;
            firePropertyChange( "geneCountColumn", oldValue, this.geneCountColumn );
        }

        @PropertyName("Values column")
        public String getLogPvalueColumn()
        {
            return logPvalueColumn;
        }

        public void setLogPvalueColumn(String logPvalueColumn)
        {
            String oldValue = this.logPvalueColumn;
            this.logPvalueColumn = logPvalueColumn;
            firePropertyChange( "logPvalueColumn", oldValue, this.logPvalueColumn );
        }

        @PropertyName("Need log10")
        @PropertyDescription("If checked, -log10 of values will be used for plotting")
        public boolean isNeedLog()
        {
            return needLog;
        }

        public void setNeedLog(boolean needLog)
        {
            boolean oldValue = this.needLog;
            this.needLog = needLog;
            firePropertyChange( "needLog", oldValue, this.needLog );
        }


        @PropertyName("Cutoff value")
        @PropertyDescription("Cutoff line will be drawn on plot, -log10 will be used if checked as for the values")
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


        @PropertyName("Width")
        @PropertyDescription("Image width")
        public int getImageWidth()
        {
            return imageWidth;
        }

        public void setImageWidth(int imageWidth)
        {
            int oldValue = this.imageWidth;
            this.imageWidth = imageWidth;
            firePropertyChange( "imageWidth", oldValue, this.imageWidth );
        }

        @PropertyName("Height")
        @PropertyDescription("Image height")
        public int getImageHeight()
        {
            return imageHeight;
        }

        public void setImageHeight(int imageHeight)
        {
            int oldValue = this.imageHeight;
            this.imageHeight = imageHeight;
            firePropertyChange( "imageHeight", oldValue, this.imageHeight );
        }

        @PropertyName("Titles column")
        @PropertyDescription("Titles of dots.")
        public String getCategoryColumn()
        {
            return categoryColumn;
        }

        public void setCategoryColumn(String categoryColumn)
        {
            String oldValue = this.categoryColumn;
            this.categoryColumn = categoryColumn;
            firePropertyChange( "categoryColumn", oldValue, this.categoryColumn );
        }

        @PropertyName("Number of top categories")
        public int getNumTopCategories()
        {
            return numTopCategories;
        }

        public void setNumTopCategories(int numTopCategories)
        {
            int oldValue = this.numTopCategories;
            this.numTopCategories = numTopCategories;
            firePropertyChange( "numTopCategories", oldValue, this.numTopCategories );
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
            add( ColumnNameSelector.registerNumericSelector( "geneCountColumn", beanClass, "inputTable", false ) );

            add( ColumnNameSelector.registerSelector( "categoryColumn", beanClass, "inputTable", true ) );
            add( ColumnNameSelector.registerNumericSelector( "logPvalueColumn", beanClass, "inputTable", false ) );
            add( "needLog" );
            add( "pvalueCutoff" );
            add( "numTopCategories" );
            addExpert( "imageWidth" );
            addExpert( "imageHeight" );
            property( "outputChart" ).outputElement( ImageDataElement.class ).add();
        }
    }
}
