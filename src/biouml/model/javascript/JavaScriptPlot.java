package biouml.model.javascript;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;

import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.StrokeTextEditor;
import ru.biosoft.plugins.javascript.Global;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * JavaScript host object to create customizable plots.
 * Contains single plot instance which is edited currently. 
 * Plot have all methods to work with it. However all of them are duplicated in the host object. 
 * Thus there are two ways to work with plot:
 * 1. Creating plot object:
 * var p = plotManager.createPlot();
 * p.addCurve();
 *  
 * 2. Working only with host object. 
 * plotManager.createPlot();
 * plotManager.addCurve();
 * 
 * TODO: appropriate error messages 
 * @author axec
 *
 */
public class JavaScriptPlot extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger( JavaScriptPlot.class.getName() );
    protected static final Shape circle = new Ellipse2D.Float( 0, 0, 3, 3 );

    public static String AXIS_STANDARD = "Standard";
    public static String AXIS_DATE = "Date";

    /** Plot instance with which user is working. Default title is empty*/
    private Plot instance = new Plot("");

    /**Creates new empty plot*/
    public Plot createPlot()
    {
        return createPlot( "" );
    }

    /**Creates new empty plot with given title*/
    public Plot createPlot(String title)
    {
        instance = new Plot( title );
        return instance;
    }

    /**Creates new empty plot with given general title and titles of axis*/
    public Plot createPlot(String title, String xTitle, String yTitle)
    {
        instance = new Plot( title );
        instance.xTitle = xTitle;
        instance.yTitle = yTitle;
        return instance;
    }

    /**
     * Method to create plot from java script associative array
     * @param obj - array containing all data to initialize plot
     * @return plot
     */
    public Plot createPlot(Object obj) throws Exception
    {
        if( ! ( obj instanceof NativeObject ) )
            log.info( "ERROR" );

        NativeObject njo = (NativeObject)obj;

        String title = "";
        Object titleObj = njo.get( "title", scope );
        if( titleObj instanceof String )
            title = (String)titleObj;

        this.createPlot( title );

        Object xTitleObj = njo.get( "xTitle", scope );
        if( xTitleObj instanceof String )
            instance.setXTitle( (String)xTitleObj );

        Object yTitleObj = njo.get( "yTitle", scope );
        if( yTitleObj instanceof String )
            instance.setYTitle( (String)yTitleObj );

        Object backgroundObj = njo.get( "backgound", scope );
        if( backgroundObj instanceof String )
            instance.setBackgroundColor( (String)backgroundObj );

        Object curvesObj = njo.get( "curves", scope );
        if( curvesObj instanceof NativeArray )
        {
            NativeArray ar = (NativeArray)curvesObj;
            for( int i = 0; i < ar.size(); i++ )
                instance.addCurve( ar.get( i ) );
        }

        Object expObj = njo.get( "experiments", scope );
        if( expObj instanceof NativeArray )
        {
            NativeArray ar = (NativeArray)expObj;
            for( int i = 0; i < ar.size(); i++ )
                instance.addExperiment( ar.get( i ) );
        }
        return instance;
    }

    /**
     * Method to create and show plot from java script associative array
     * @param obj - array containing all data to initialize plot
     * @return plot
     */
    public Plot showPlot(Object obj) throws Exception
    {
        Plot result = createPlot( obj );
        
        ScriptEnvironment environment = Global.getEnvironment();
        if( environment != null )
            environment.showGraphics( getImage() );
        return result;
    }

    public void showPlot()
    {
        ScriptEnvironment environment = Global.getEnvironment();
        if( environment != null )
            environment.showGraphics( getImage() );        
    }
    
    /** Sets background color for plot */
    public void setBackgroundColor(String color)
    {
        instance.setBackgroundColor( color );
    }

    /** Java class to work with inside java-script code. Mostly intended to be represented as image.*/
    public class Plot
    {
        private String title = "";
        private String xTitle = "Time";
        private String yTitle = "Quantity or concentration";
        private Color background = Color.white;
        private List<Variable> yVariables = new ArrayList<>();
        private double xFrom = Double.NaN;
        private double xTo = Double.NaN;
        private double yFrom = Double.NaN;
        private double yTo = Double.NaN;
        private String xType = AXIS_STANDARD;

        //Date axis issues
        private String dateFormat = "MMMM";
        private int startingYear = 2020;
        private int labelFontSize = 18;
        private int tickFontSize = 16;
        private boolean verticalTicks = false;

        public Plot(String title)
        {
            this.title = title;
        }

        public void setBackgroundColor(String color)
        {
            this.background = getColor( color, Color.white );
        }

        public void setYTitle(String yTitle)
        {
            this.yTitle = yTitle;
        }

        public void setXTitle(String xTitle)
        {
            this.xTitle = xTitle;
        }

        public void setXType(String xType)
        {
            this.xType = xType;
        }

        public void setDateFormat(String format)
        {
            this.dateFormat = format;
        }

        public void setStartingYear(int year)
        {
            this.startingYear = year;
        }

        public void setLabelFontSize(int size)
        {
            this.labelFontSize = size;
        }

        public void setTickFontSize(int size)
        {
            this.tickFontSize = size;
        }

        public void setVerticalTick(boolean verticalTicks)
        {
            this.verticalTicks = verticalTicks;
        }

        public void addCurve(String title, double[] vals, double[] xVals)
        {
            addCurve( title, vals, xVals, null );
        }

        public void addCurve(String title, double[] vals, double[] xVals, Pen pen)
        {
            yVariables.add( new Variable( title, vals, xVals, VarType.SIMULATION, pen ) );
        }

        public void addCurve(String title, SimulationResult res, String yPath, String xPath) throws Exception
        {
            addCurve( title, res, yPath, xPath, null );
        }

        public void addCurve(String title, SimulationResult res, String yPath, String xPath, Pen pen) throws Exception
        {
            if( !res.getVariableMap().containsKey( yPath ) )
                throw new Exception( "Can not find variable " + yPath + " in simulation result" );
            if( !res.getVariableMap().containsKey( yPath ) )
                throw new Exception( "Can not find variable " + xPath + " in simulation result" );
            addCurve( title, res.getValues( yPath ), res.getValues( xPath ), pen );
        }

        public void addExperiment(String title, double[] yVals, double[] xVals)
        {
            addExperiment( title, yVals, xVals, null );
        }

        public void addExperiment(String title, double[] yVals, double[] xVals, Pen pen)
        {
            yVariables.add( new Variable( title, yVals, xVals, VarType.EXPERIMENT, pen ) );
        }

        public void addExperiment(String title, TableDataCollection tdc, String column, String xColumn)
        {
            addExperiment( title, tdc, column, xColumn, null );
        }

        public void addExperiment(String title, TableDataCollection tdc, String yColumn, String xColumn, Pen pen)
        {
            addExperiment( title, TableDataCollectionUtils.getColumn( tdc, yColumn ), TableDataCollectionUtils.getColumn( tdc, xColumn ),
                    pen );
        }

        public BufferedImage getImage()
        {
            return getImage( 550, 350 );
        }
        
        public void setXFrom(double xFrom)
        {
            this.xFrom = xFrom;
        }

        public void setXTo(double xTo)
        {
            this.xTo = xTo;
        }

        public void setYFrom(double yFrom)
        {
            this.yFrom = yFrom;
        }

        public void setYTo(double yTo)
        {
            this.yTo = yTo;
        }


        private void addXYSeries(XYSeriesCollection dataset, String name, double[] xData, double[] yData)
        {
            XYSeries series = new XYSeries(name, false, true);
            for( int j = 0; j < Math.min(yData.length, xData.length); j++ )
                series.add(xData[j], yData[j]);
            dataset.addSeries(series);
        }

        private void addTimeSeries(TimeSeriesCollection dataset, String name, double[] xData, double[] yData)
        {
            TimeSeries series = new TimeSeries(name);
            for( int j = 0; j < Math.min(yData.length, xData.length); j++ )
            {
                int xVal = (int)xData[j];
                int year = startingYear;
                int length = Year.of(year).length();
                while( xVal > length )
                {
                    year++;
                    length = Year.of(year).length();
                    xVal -= length;
                }

                if (xVal == 0)
                {
                    year--;
                    xVal = Year.of(year).length();
                }
                LocalDate dayExp = Year.of(year).atDay(xVal);
                Day day = new Day(dayExp.getDayOfMonth(), dayExp.getMonthValue(), dayExp.getYear());
                series.add(day, yData[j]);
            }
            dataset.addSeries(series);
        }

        private AbstractXYDataset fillDataSet(XYLineAndShapeRenderer renderer)
        {
            boolean dateType = xType.equals(AXIS_DATE);
            AbstractXYDataset dataset = dateType ? new TimeSeriesCollection() : new XYSeriesCollection();

            int counter = 0;
            for( Variable var : this.yVariables )
            {
                if( !dateType )
                    addXYSeries((XYSeriesCollection)dataset, var.title, var.xData, var.yData);
                else
                    addTimeSeries((TimeSeriesCollection)dataset, var.title, var.xData, var.yData);

                if( var.type.equals( VarType.EXPERIMENT ) )
                {
                    renderer.setSeriesLinesVisible( counter, false );
                    renderer.setSeriesShape( counter, circle );
                    if( var.pen != null )
                    {
                        renderer.setSeriesFillPaint( counter, var.pen.getColor() );
                        renderer.setSeriesPaint( counter, var.pen.getColor() );
                    }
                }
                else
                {
                    renderer.setSeriesShapesVisible( counter, false );
                    if( var.pen != null )
                    {
                        renderer.setSeriesPaint( counter, var.pen.getColor() );
                        renderer.setSeriesStroke( counter, var.pen.getStroke() );
                    }
                }
                counter++;
            }
            return dataset;
        }

        public BufferedImage getImage(int width, int height)
        {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setDrawSeriesLineAsPath(true);
            AbstractXYDataset xyDataset = fillDataSet(renderer);
            JFreeChart chart = ChartFactory.createXYLineChart( title, xTitle, yTitle, xyDataset, //dataset,
                    PlotOrientation.VERTICAL, true, // legend
                    true, // tool tips
                    false // URLs
            );            
            chart.setBackgroundPaint( background );
            
            if( !Double.isNaN( xTo ) )
                chart.getXYPlot().getDomainAxis().setUpperBound( xTo );
            if( !Double.isNaN( xFrom ) )
                chart.getXYPlot().getDomainAxis().setLowerBound( xFrom );
            if( !Double.isNaN( yTo ) )
                chart.getXYPlot().getRangeAxis().setUpperBound( yTo );
            if( !Double.isNaN( yFrom ) )
                chart.getXYPlot().getRangeAxis().setLowerBound( yFrom );

            chart.getXYPlot().setBackgroundPaint( background );
            chart.getXYPlot().setRenderer( renderer );

            if( xType.equals(AXIS_DATE) )
            {
                DateAxis dateAxis = new DateAxis();
                dateAxis.setDateFormatOverride(new SimpleDateFormat(dateFormat));
                chart.getXYPlot().setDomainAxis(dateAxis);
            }

            chart.getXYPlot().getDomainAxis().setTickMarkPaint(Color.black);
            chart.getXYPlot().getRangeAxis().setTickMarkPaint(Color.black);
            chart.getXYPlot().getDomainAxis().setVerticalTickLabels(verticalTicks);
            chart.getXYPlot().getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, labelFontSize));
            chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, tickFontSize));
            chart.getXYPlot().getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, labelFontSize));
            chart.getXYPlot().getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, tickFontSize));
            
            return chart.createBufferedImage( width, height );
        }

        public void addCurve(Object obj) throws Exception
        {
            if( ! ( obj instanceof NativeObject ) )
                log.info( "ERROR" );

            NativeObject njo = (NativeObject)obj;
            String title = njo.get( "title", scope ).toString();
            addCurve( title, obj );
        }

        public void addCurve(String title, Object obj) throws Exception
        {
            if( ! ( obj instanceof NativeObject ) )
                log.info( "ERROR" );

            NativeObject njo = (NativeObject)obj;

            Object penObj = njo.get( "pen", scope );
            Pen pen = null;
            if( penObj instanceof NativeJavaObject )
                pen = (Pen) ( (NativeJavaObject)penObj ).unwrap();

            Object yValObj = njo.get( "yValues", scope );
            Object xValObj = njo.get( "xValues", scope );
            if( ( yValObj instanceof NativeArray || yValObj instanceof NativeJavaArray )
                    && ( xValObj instanceof NativeArray || xValObj instanceof NativeJavaArray ) )
            {
                double[] yValues = Global.getDoubleArrayFromObject( scope, yValObj );
                double[] xValues = Global.getDoubleArrayFromObject( scope, xValObj );
                addCurve( title, yValues, xValues, pen );
                return;
            }

            Object sourceObj = njo.get( "source", scope );
            if( sourceObj instanceof NativeJavaObject )
            {
                SimulationResult result = (SimulationResult) ( (NativeJavaObject)sourceObj ).unwrap();
                Object yVarObj = njo.get( "yVariable", scope );
                Object xVarObj = njo.get( "xVariable", scope );
                addCurve( title, result, yVarObj.toString(), xVarObj.toString(), pen );
                return;
            }
        }

        public void addExperiment(Object obj)
        {
            if( ! ( obj instanceof NativeObject ) )
                log.info( "ERROR" );

            NativeObject njo = (NativeObject)obj;

            String title = njo.get( "title", scope ).toString();

            Object penObj = njo.get( "pen", scope );
            Pen pen = null;
            if( penObj instanceof NativeJavaObject )
                pen = (Pen) ( (NativeJavaObject)penObj ).unwrap();

            Object yValObj = njo.get( "yValues", scope );
            Object xValObj = njo.get( "xValues", scope );
            if( yValObj instanceof NativeArray && xValObj instanceof NativeArray )
            {
                double[] yValues = Global.getDoubleArrayFromObject( scope, yValObj );
                double[] xValues = Global.getDoubleArrayFromObject( scope, xValObj );
                addExperiment( title, yValues, xValues, pen );
                return;
            }

            Object sourceObj = njo.get( "source", scope );
            if( sourceObj instanceof NativeJavaObject )
            {
                Object javaObject = ( (NativeJavaObject)sourceObj ).unwrap();
                TableDataCollection tdc = null;
                if( javaObject instanceof TableDataCollection )
                    tdc = (TableDataCollection)javaObject;
                else if( javaObject instanceof String )
                    tdc = DataElementPath.create( javaObject.toString() ).getDataElement( TableDataCollection.class );

                Object yVarObj = njo.get( "yColumn", scope );
                Object xVarObj = njo.get( "xColumn", scope );
                addExperiment( title, tdc, yVarObj.toString(), xVarObj.toString(), pen );
            }
            return;
        }
    }

    public Pen createPen(String color)
    {
        return new Pen( 1.0f, getColor( color, Color.black ) );
    }

    public Pen createPen(float width, String color)
    {
        return new Pen( width, getColor( color, Color.black ) );
    }

    public Pen createPen(float width, String color, String stroke)
    {
        BasicStroke basicStroke = Pen.createBasicStroke( StrokeTextEditor.getArrayByPattern( stroke ) );
        Pen pen = new Pen( basicStroke, getColor( color, Color.black ) );
        pen.setWidth( width ); //TODO: create method with width in Pen
        return pen;
    }

    /**Simplest way to add curve from two arrays and title*/
    public void addCurve(String title, double[] yVals, double[] xVals)
    {
        instance.addCurve( title, yVals, xVals );
    }

    /**Simplest way to add curve from two arrays and title. Additionally pen is set.*/
    public void addCurve(String title, double[] yVals, double[] xVals, Pen pen)
    {
        instance.addCurve( title, yVals, xVals, pen );
    }

    /**Adds curve derived from simulation result*/
    public void addCurve(String title, SimulationResult res, String yPath, String xPath) throws Exception
    {
        instance.addCurve( title, res, yPath, xPath );
    }

    /**Adds curve derived from simulation result. Additionally pen is set.*/
    public void addCurve(String title, SimulationResult res, String yPath, String xPath, Pen pen) throws Exception
    {
        instance.addCurve( title, res, yPath, xPath, pen );
    }

    public void addCurve(String title, Object obj) throws Exception
    {
        instance.addCurve( title, obj );
    }

    public void addCurve(Object obj) throws Exception
    {
        instance.addCurve( obj );
    }
    
    public void addExperiment(Object obj)
    {
        instance.addExperiment( obj );
    }

    public void addExperiment(String title, double[] yVals, double[] xVals)
    {
        instance.addExperiment( title, yVals, xVals );
    }

    public void addExperiment(String title, double[] yVals, double[] xVals, Pen pen)
    {
        instance.addExperiment( title, yVals, xVals, pen );
    }

    public void addExperiment(String title, TableDataCollection tdc, String yColumn, String xColumn)
    {
        instance.addExperiment( title, tdc, yColumn, xColumn );
    }

    public void addExperiment(String title, TableDataCollection tdc, String yColumn, String xColumn, Pen pen)
    {
        instance.addExperiment( title, tdc, yColumn, xColumn, pen );
    }

    public BufferedImage getImage()
    {
        return instance.getImage();
    }

    public BufferedImage getImage(int width, int height)
    {
        return instance.getImage( width, height );
    }

    /**Set title of X Axis*/
    public void setXTitle(String xTitle)
    {
        instance.setXTitle( xTitle );
    }

    /**Set title of Y Axis*/
    public void setYTitle(String yTitle)
    {
        instance.setYTitle( yTitle );
    }

    /**Set minimum value for X Axis*/
    public void setXFrom(double xFrom)
    {
        instance.setXFrom( xFrom );
    }

    /**Set maximum value for X Axis*/
    public void setXTo(double xTo)
    {
        instance.setXTo( xTo );
    }

    /**Set minimum value for Y Axis*/
    public void setYFrom(double yFrom)
    {
        instance.setYFrom( yFrom );
    }

    /**Set maximum value for Y Axis*/
    public void setYTo(double yTo)
    {
        instance.setYTo( yTo );
    }
    
    /**
     * Translates string representation of color to Color java object.
     * If given string can not be translated to Color returns defaultColor.
     * TODO: maybe implement RGB translation ([255, 128, 128] -> Color object)
     */
    public static Color getColor(String str, Color defaultColor)
    {
        switch( str )
        {
            case "red":
                return Color.red;
            case "blue":
                return Color.blue;
            case "green":
                return Color.green;
            case "black":
                return Color.black;
            case "cyan":
                return Color.cyan;
            case "darkGray":
                return Color.darkGray;
            case "magenta":
                return Color.magenta;
            case "orange":
                return Color.orange;
            case "pink":
                return Color.pink;
            case "gray":
                return Color.gray;
            default:
                return defaultColor;
        }
    }

    public enum VarType
    {
        EXPERIMENT, SIMULATION
    }

    public static class Variable
    {
        private String title;
        private double[] yData;
        private double[] xData;
        private VarType type;
        private Pen pen;

        public Variable(String title, double[] data, VarType type, Pen pen)
        {
            this.title = title;
            this.yData = data;
            this.type = type;
            this.pen = pen;
        }

        public Variable(String title, double[] data, double[] xData, VarType type, Pen pen)
        {
            this.title = title;
            this.yData = data;
            this.xData = xData;
            this.type = type;
            this.pen = pen;
        }
    }
}
