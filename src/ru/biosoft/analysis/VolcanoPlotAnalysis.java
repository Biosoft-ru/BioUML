package ru.biosoft.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.font.LineMetrics;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.Pair;

@ClassIcon("resources/volcano.png")
public class VolcanoPlotAnalysis extends AnalysisMethodSupport<ru.biosoft.analysis.VolcanoPlotAnalysis.Parameters>
{

    private static final int pointSize = 5;
    private static final Shape circle = new Ellipse2D.Float( 0, 0, pointSize, pointSize );
    JFreeChart chart;
    XYDataset dataset;

    public VolcanoPlotAnalysis(DataCollection<?> origin, String name)
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
        chart = ChartFactory.createScatterPlot( null, "log2 Fold Change", "-log10 P-value", dataset, PlotOrientation.VERTICAL, true, false, false );
        makePlot();
        drawCutoffLines();
        return imageChart();
    }


    private List<XYAnnotation> annotations = new ArrayList<>();
    List<DataPoint> points = new ArrayList<>();

    private XYDataset createXYDataset()
    {

        TableDataCollection inputTable = parameters.getInputTable().getDataElement( TableDataCollection.class );
        if( inputTable.getSize() == 0 )
        {
            return null;
        }

        int pvalIdx = inputTable.getColumnModel().getColumnIndex( parameters.getPvalueColumn() );
        int logfcIdx = inputTable.getColumnModel().getColumnIndex( parameters.getLogfcColumn() );
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries seriesUp = new XYSeries( "Upregulated" );
        XYSeries seriesDown = new XYSeries( "Downregulated" );
        XYSeries seriesOut = new XYSeries( "Not significant" );
        
        List<Pair<Double, String>> up2Id = new ArrayList<>();
        List<Pair<Double, String>> down2Id = new ArrayList<>();
        List<Pair<Double, String>> ns2Id = new ArrayList<>();
        for ( RowDataElement row : inputTable )
        {
            Object[] values = row.getValues();
            Object xValueObj = values[logfcIdx];
            Object yValueObj = values[pvalIdx];
            if( xValueObj instanceof Number && yValueObj instanceof Number )
            {
                Number xValue = (Number) xValueObj;
                Number yValue = (Number) yValueObj;
                if( (isNaN( xValue ) || isNaN( yValue )) )
                    continue;
                Double x = xValue.doubleValue();
                Double y = -Math.log10( yValue.doubleValue() );
                if( yValue.doubleValue() < parameters.getPvalueCutoff() )
                {
                    if( x < parameters.getDownCutoff() )
                    {
                        down2Id.add( new Pair( y, row.getName() ) );
                        seriesDown.add( x, y );
                    }
                    else if( x > parameters.getUpCutoff() )
                    {
                        up2Id.add( new Pair( y, row.getName() ) );
                        seriesUp.add( x, y );
                    }
                    else
                    {
                        ns2Id.add( new Pair( y, row.getName() ) );
                        seriesOut.add( x, y );
                    }
                }
                else
                {
                    ns2Id.add( new Pair( y, row.getName() ) );
                    seriesOut.add( x, y );
                }

            }
        }
        XYSeries annotSeries = fillAnnotations( inputTable, up2Id, down2Id, ns2Id, pvalIdx, logfcIdx );
        dataset.addSeries( annotSeries );
        dataset.addSeries( seriesUp );
        dataset.addSeries( seriesDown );
        dataset.addSeries( seriesOut );

        return dataset;
    }

    private boolean isNaN(Number value)
    {
        return Double.isNaN( value.doubleValue() );
    }

    private XYSeries fillAnnotations(TableDataCollection inputTable, List<Pair<Double, String>> up2Id, List<Pair<Double, String>> down2Id, List<Pair<Double, String>> ns2Id,
            int pvalIdx, int logfcIdx)
    {
        XYSeries annotationSeries = new XYSeries( "annotations" );
        String lblColumn = parameters.getLabelsColumn();
        int idx = inputTable.getColumnModel().getColumnIndex( lblColumn );
        DataElementPath labeledPath = parameters.getTopGenes();
        List<String> idsToShow = new ArrayList<>();
        if( labeledPath != null && labeledPath.exists() && labeledPath.getDataElement() instanceof TableDataCollection )
        {
            //Add labels for genes from the specified list
            TableDataCollection labeledTable = labeledPath.getDataElement( TableDataCollection.class );
            labeledTable.getNameList().stream().forEach( id -> {
                if( inputTable.contains( id ) )
                    idsToShow.add( id );
            } );

        }
        else
        {
            int numToLabel = parameters.getNumTopGenes();
            if( numToLabel <= 0 )
                return annotationSeries;
            up2Id.stream().sorted( (p1, p2) -> {
                return p2.getFirst().compareTo( p1.getFirst() );
            } ).limit( numToLabel ).forEach( p -> {
                idsToShow.add( p.getSecond() );

            } );

            down2Id.stream().sorted( (p1, p2) -> {
                return p2.getFirst().compareTo( p1.getFirst() ); //reverse sorted
            } ).limit( numToLabel ).forEach( p -> {
                idsToShow.add( p.getSecond() );

            } );
        }



        for ( String id : idsToShow )
        {
            try
            {
                Object[] values = inputTable.get( id ).getValues();
                String label = (String) values[idx];
                Double x = ((Number) values[logfcIdx]).doubleValue();
                Double y = -Math.log10( ((Number) values[pvalIdx]).doubleValue()  );
                points.add( new DataPoint( label, x, y ) );

                //LineMetrics lineMetrics = fontMetrics.getLineMetrics(label, gr);
                XYTextAnnotation annot = new XYTextAnnotation( label, x, y + 0.01 );
                annot.setPaint( Color.RED );

                //annotations.add( annot );//TODO: scale 0.01
                annotationSeries.add( x, y );
            }
            catch (Exception e)
            {
            }

        }

        return annotationSeries;

    }

    private void makePlot()
    {
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint( Color.WHITE );
        plot.setOutlineVisible( false );
        chart.setBackgroundPaint( Color.WHITE );
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Configure scatter plot series (show shapes, no lines)
        //Annotation small circles
        renderer.setSeriesLinesVisible( 0, false );
        renderer.setSeriesShapesVisible( 0, true );
        renderer.setSeriesShape( 0, new Ellipse2D.Float( 2, 2, 1, 1 ) );
        renderer.setSeriesPaint( 0, new Color( 0, 0, 0 ) ); //Black 

        //Up-regulated
        renderer.setSeriesLinesVisible( 1, false );
        renderer.setSeriesShapesVisible( 1, true );
        renderer.setSeriesShape( 1, circle );
        renderer.setSeriesPaint( 1, new Color( 184, 28, 0 ) ); //Red

        //Down-regulated
        renderer.setSeriesLinesVisible( 2, false );
        renderer.setSeriesShapesVisible( 2, true );
        renderer.setSeriesShape( 2, circle );
        renderer.setSeriesPaint( 2, new Color( 44, 107, 156 ) ); //Blue

        //Not-significant
        renderer.setSeriesLinesVisible( 3, false );
        renderer.setSeriesShapesVisible( 3, true );
        renderer.setSeriesShape( 3, circle );
        renderer.setSeriesPaint( 3, Color.LIGHT_GRAY );


        plot.setRenderer( renderer );
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
        BufferedImage image = chart.createBufferedImage( 900, 900 );
        Graphics gr = image.createGraphics();
        placeAnnotations( gr, chart.getXYPlot() );
        BufferedImage image2 = chart.createBufferedImage( 900, 900 );
        ImageDataElement imageDE = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), image2 );
        imagePath.save( imageDE );
        return imageDE;
    }

    private void placeAnnotations(Graphics gr, XYPlot plot)
    {
        ValueAxis ya = plot.getRangeAxis();
        Rectangle2D area = new Rectangle2D.Float( 0, 0, 900, 800 );
        ValueAxis xa = plot.getDomainAxis();

        XYTextAnnotation annotation = new XYTextAnnotation( "test", 1, 1 );
        Font font = annotation.getFont();
        FontMetrics fontMetrics = gr.getFontMetrics( font );
        points.sort( (p1, p2) -> {
            if( p1.x <= 0 && p2.x <= 0 )
                return p1.compareTo( p2 );
            else if( p1.x <= 0 && p2.x > 0 )
                return -1;
            else if( p1.x > 0 && p2.x <= 0 )
                return 1;
            else
                return p1.compareTo( p2 );
        } );
        double prevYB = Double.MAX_VALUE, prevXL = 0, prevXR = 0;
        double delta = fontMetrics.getDescent();
        //process negative X first, then start comparing Y from default
        //TODO: refactor - cluster values and place nicely in clusters
        boolean flag = true;
        for ( DataPoint pt : points )
        {
            boolean isPointer = false;
            double ptrx, ptry;
            if( flag && pt.x > 0 )
            {
                prevYB = Double.MAX_VALUE;
                flag = false;
            }
            try
            {
                String label = pt.label;
                Double x = xa.valueToJava2D( pt.x, area, plot.getDomainAxisEdge() );//pt.x;
                Double y = ya.valueToJava2D( pt.y, area, plot.getRangeAxisEdge() ); //pt.y
                LineMetrics lineMetrics = fontMetrics.getLineMetrics( label, gr );
                int width = fontMetrics.stringWidth( label );
                float height = lineMetrics.getAscent();
                double nY = y - pointSize;
                double nX = x;
                double lineX2D = nX;
                double curYT = nY - height;
                double prevX = (prevXR + prevXL) / 2;

                if( nY < prevYB )
                {
                    if( x + width / 2 < prevXL || x - width / 2 > prevXR )
                    {
                        //do nothing
                        nY = y - pointSize;
                    }
                    else
                    {
                        //previous was moved down and current can intersect pre-previous
                        //try to move it right or left, if can not, move far right for x>0 and far left for x < 0 and make line pointer
                        nY = y + height;
                        if( x <= prevXL && x + width / 2 >= prevXL )
                        {
                            //move to left
                            nX = prevXL - width / 2;
                        }
                        else if( x > prevXR && x - width / 2 <= prevXR )
                        {
                            //move To Rigth
                            nX = prevXR + width / 2;
                        }
                        else
                        {
                            if( nX > prevX )
                            {
                                nY = y - pointSize;
                                nX = prevXR + width / 2 + 7;
                                lineX2D = nX - width / 2;
                                isPointer = true;
                            }
                            else
                            {
                                nY = y - pointSize;
                                nX = prevXL - width / 2 + 7;
                                lineX2D = nX + width / 2;
                                isPointer = true;
                            }
                        }
                    }

                }
                else if( curYT > prevYB || x + width / 2 < prevXL || x - width / 2 > prevXR )
                {
                    //ok, label below previous, place as is and store label position
                }
                else
                {

                    //move left, right or below the point
                    if( x <= prevXL && x + width / 2 >= prevXL )
                    {
                        //move to left
                        nX = prevXL - width / 2;
                    }
                    else if( x >= prevXR && x - width / 2 <= prevXR )
                    {
                        //move to right
                        nX = prevXR + width / 2;
                    }
                    else
                    {
                        //move down, can intersect
                        nY = y + height;
                    }
                }
                prevXL = nX - width / 2;
                prevXR = nX + width / 2;
                prevYB = nY;
                double lblx = xa.java2DToValue( nX, area, plot.getDomainAxisEdge() );
                double lbly = ya.java2DToValue( nY, area, plot.getRangeAxisEdge() );
                XYTextAnnotation annot;
                if( isPointer )
                {
                    annot = new XYTextAnnotation( label, lblx, lbly );
                    annotations.add( annot );
                    double lineX = xa.java2DToValue( lineX2D, area, plot.getDomainAxisEdge() );
                    double lineY = ya.java2DToValue( nY, area, plot.getRangeAxisEdge() );
                    XYShapeAnnotation shape = new XYShapeAnnotation( new Line2D.Double( pt.x, pt.y, lineX, lineY ), new BasicStroke( 1.0f ), Color.DARK_GRAY );
                    annotations.add( shape );
                }
                else
                {
                    annot = new XYTextAnnotation( label, lblx, lbly );
                    annotations.add( annot );
                }
            }
            catch (Exception e)
            {
            }

        }

        for ( XYAnnotation label : annotations )
        {
            plot.addAnnotation( label );
        }
    }

    private static class DataPoint implements Comparable<DataPoint>
    {
        String label;
        double x, y;

        DataPoint(String label, double x, double y)
        {
            this.label = label;
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(DataPoint o)
        {
            int valueCmp = -Double.compare( y, o.y );
            if( valueCmp != 0 )
            {
                return valueCmp;
            }
            return Double.compare( x, o.x );
        }
    }

    @SuppressWarnings("serial")
    public static class Parameters extends ChartAnalysisParameters
    {

        private String pvalueColumn, logfcColumn;
        private Double downCutoff = -0.6, upCutoff = 0.6, pvalueCutoff = 0.05;
        private int numTopGenes = 5;
        private DataElementPath topGenes;

        @PropertyName("P-value column")
        public String getPvalueColumn()
        {
            return pvalueColumn;
        }

        public void setPvalueColumn(String pvalueColumn)
        {
            String oldValue = this.pvalueColumn;
            this.pvalueColumn = pvalueColumn;
            firePropertyChange( "pvalueColumn", oldValue, this.pvalueColumn );
        }

        @PropertyName("LogFC column")
        public String getLogfcColumn()
        {
            return logfcColumn;
        }

        public void setLogfcColumn(String logfcColumn)
        {
            String oldValue = this.logfcColumn;
            this.logfcColumn = logfcColumn;
            firePropertyChange( "logfcColumn", oldValue, this.logfcColumn );
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

        @PropertyName("P-value cutoff")
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

        public int getNumTopGenes()
        {
            return numTopGenes;
        }

        @PropertyName("Number of labels")
        @PropertyDescription("Number of genes (up- and downregulated) for which labels will be shown on plot")
        public void setNumTopGenes(int numTopGenes)
        {
            int oldValue = this.numTopGenes;
            this.numTopGenes = numTopGenes;
            firePropertyChange( "numTopGenes", oldValue, this.numTopGenes );
        }

        @PropertyName("Labeled list")
        @PropertyDescription("Labels for genes from this list will be shown on plot")
        public DataElementPath getTopGenes()
        {
            return topGenes;
        }

        public void setTopGenes(DataElementPath topGenes)
        {
            DataElementPath oldValue = this.topGenes;
            this.topGenes = topGenes;
            firePropertyChange( "topGenes", oldValue, this.topGenes );
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
            add( ColumnNameSelector.registerNumericSelector( "pvalueColumn", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerNumericSelector( "logfcColumn", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerSelector( "labelsColumn", beanClass, "inputTable", true ) );
            addExpert( "downCutoff" );
            addExpert( "upCutoff" );
            addExpert( "pvalueCutoff" );
            addExpert( "numTopGenes" );
            property( "topGenes" ).inputElement( TableDataCollection.class ).canBeNull().add();
            property( "outputChart" ).outputElement( ImageDataElement.class ).add();
        }
    }
}
