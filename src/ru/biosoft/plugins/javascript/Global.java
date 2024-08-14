package ru.biosoft.plugins.javascript;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.json.JSONException;
import org.mozilla.javascript.ClassDefinitionException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.PropertyException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.tools.ToolErrorReporter;

import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.LazyStringBuilder;
import ru.biosoft.util.RhinoUtils;

@SuppressWarnings ( {"deprecation", "unused", "serial"} )
public class Global extends ImporterTopLevel
{
    public static final String QUIT = "quit.";
    private boolean init = false;
    public static final String ENVIRONMENT_OBJECT = "js_environment_object";

    public Global()
    {
    }
    
    public Global(Context context)
    {
        super(context);
    }
    
    static RuntimeException reportRuntimeError(String msgId)
    {
        String message = ToolErrorReporter.getMessage(msgId);
        return Context.reportRuntimeError(message);
    }

    static RuntimeException reportRuntimeError(String msgId, String msgArg)
    {
        String message = ToolErrorReporter.getMessage(msgId, msgArg);
        return Context.reportRuntimeError(message);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Global functions
    //

    /**
     * Returns description for the specified JavaScript function or host object.
     *
     * This description should be loaded using 'ru.biosoft.pligins.javascript.function' or
     * 'ru.biosoft.pligins.javascript.hostObject' extension point.
     */
    public static void help(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        ScriptEnvironment environment = getEnvironment(thisObj);
        if(environment == null)
            throw new RuntimeException("Unable to find environment");
        Object obj = null;
        if( args == null || args.length == 0 )
            obj = funObj;
        else
            obj = args[0];


        if( obj.toString().equals("functions") )
        {
            environment.showHtml(JScriptHelp.getFunctionDescriptionList());
        }
        else if( obj.toString().equals("objects") )
        {
            environment.showHtml(JScriptHelp.getObjectDescriptionList());
        }
        else
        {
            String help = obj instanceof Scriptable ? JScriptHelp.getHelpValue((Scriptable)obj) : JScriptHelp.getJavaDoc(obj);
            if( help != null )
            {
                environment.showHtml(help);
                return;
            }
            try
            {
                environment.print(ScriptRuntime.toString(obj));
            }
            catch( Exception e )
            {
                environment.print(obj.toString());
            }
        }
    }

    /**
     * Print the string values of its arguments.
     *
     * Note that its arguments are of the "varargs" form, which
     * allows it to handle an arbitrary number of arguments
     * supplied to the JavaScript function.
     */
    public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        StringBuffer buf = new StringBuffer();
        for( int i = 0; i < args.length; i++ )
        {
            if( i > 0 )
                buf.append(" ");

            // Convert the arbitrary JavaScript value into a string form.
            buf.append(Context.toString(args[i]));
        }

        printMessage(buf.toString(), thisObj);
    }

    /**
     * Draw plot in new window
     */
    public static void plot(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        ScriptEnvironment environment = getEnvironment(thisObj);
        if(environment == null)
        {
            throw new IllegalStateException("plot: cannot detect environment");
        }
        environment.showGraphics(createPlot(cx, thisObj, args, funObj));
        infoMessage("Opening plot...", thisObj);
    }

    public static ImageElement createPlot(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        if( args.length < 1 )
        {
            throw new IllegalArgumentException("plot: at least one parameter is required");
        }

        String xTitle = "Axis (X)";
        String yTitle = "Axis (Y)";
        List<ChartSeries> series = new ArrayList<ChartSeries>();

        if( args.length == 1 )
        {
            ImageElement elem = plotChart( args[0], thisObj );
            if(elem != null)
                return elem;
        }
        if( ( args[0] instanceof NativeArray ) || ( args[0] instanceof NativeJavaArray ) )
        {
            //parse plot(Double[], Double[], ...);
            double[] xValues = getDoubleArrayFromObject(thisObj, Context.toObject(args[0], thisObj));
            if( xValues == null )
            {
                throw new IllegalArgumentException("plot: all parameters must be arrays of double");
            }
            if( args.length == 1 )
            {
                series.add(JSPlotGenerator.createSimpleChartSeries("", xValues));
            }
            else
            {
                for( int i = 1; i < args.length; i++ )
                {
                    double[] yValues = getDoubleArrayFromObject(thisObj, Context.toObject(args[i], thisObj));
                    if( yValues != null )
                    {
                        series.add(JSPlotGenerator.createLineChartSeries(Integer.toString(i), xValues, yValues));
                    }
                    else
                    {
                        throw new IllegalArgumentException("plot: all parameters must be arrays of double");
                    }
                }
            }
        }
        else if( ( args.length >= 2 ) && ( args[0] instanceof NativeJavaObject ) )
        {
            if( ( (NativeJavaObject)args[0] ).unwrap() instanceof TableDataCollection )
            {
                //parse plot(TableDataCollection, String, String, ...);
                TableDataCollection tdc = (TableDataCollection) ( (NativeJavaObject)args[0] ).unwrap();
                double[] xValues;
                String columnName = Context.toString(args[1]);
                try
                {
                    xValues = TableDataCollectionUtils.getColumn(tdc, columnName);
                }
                catch( RepositoryException e )
                {
                    throw new IllegalArgumentException("plot: column '" + columnName + "' can not be the values of plot");
                }
                if( args.length == 2 )
                {
                    series.add(JSPlotGenerator.createSimpleChartSeries(columnName, xValues));
                }
                else
                {
                    for( int i = 2; i < args.length; i++ )
                    {
                        columnName = Context.toString(args[i]);
                        try
                        {
                            double[] yValues = TableDataCollectionUtils.getColumn(tdc, columnName);
                            series.add(JSPlotGenerator.createLineChartSeries(columnName, xValues, yValues));
                        }
                        catch( RepositoryException e )
                        {
                            throw new IllegalArgumentException("plot: column '" + columnName + "' can not be the values of plot");
                        }
                    }
                }
            }
        }
        else if( args.length > 3 )
        {
            //parse plot(xTitle, yTitle, xValues, {name: yName, type: yType, values: yValues}, ...)
            xTitle = Context.toString(args[0]);
            yTitle = Context.toString(args[1]);
            if( args[2] instanceof NativeArray || args[2] instanceof NativeJavaArray )
            {
                double[] xValues = getDoubleArrayFromObject(thisObj, Context.toObject(args[2], thisObj));
                for( int i = 3; i < args.length; i++ )
                {
                    NativeObject yObject = (NativeObject)args[i];
                    Object yNameObj = yObject.get("name", thisObj);
                    String yName = yNameObj == UniqueTag.NOT_FOUND ? null : yNameObj.toString();
                    String yType;
                    Object yTypeObj = yObject.get("type", thisObj);
                    if( yTypeObj == UniqueTag.NOT_FOUND )
                        yType = JSPlotGenerator.TYPE_LINE;
                    else
                        yType = yTypeObj.toString();
                    Object yValuesObj = yObject.get("values", thisObj);

                    if( yType.equals(JSPlotGenerator.TYPE_LINE) )
                    {
                        Object yArrayObject = Context.toObject(yValuesObj, thisObj);
                        if( yArrayObject instanceof NativeArray || yArrayObject instanceof NativeJavaArray)
                        {
                            double[] yValues = getDoubleArrayFromObject(thisObj, Context.toObject(yArrayObject, thisObj));//getDoubleArray(thisObj, (NativeArray)yArrayObject);
                            series.add(JSPlotGenerator.createLineChartSeries(yName, xValues, yValues));
                        }
                    }
                    else if( yType.equals(JSPlotGenerator.TYPE_CONSTANT) )
                    {
                        try
                        {
                            double yValue = Double.parseDouble(yValuesObj.toString());
                            series.add(JSPlotGenerator.createConstantChartSeries(yName, xValues, yValue));
                        }
                        catch( NumberFormatException e )
                        {
                        }
                    }
                    else if( yType.equals(JSPlotGenerator.TYPE_EXPERIMENT) )
                    {
                        Object pointsObject = Context.toObject(yValuesObj, thisObj);
                        if( pointsObject instanceof NativeObject )
                        {
                            Object xArrayObject = ( (NativeObject)pointsObject ).get("x", thisObj);
                            Object yArrayObject = ( (NativeObject)pointsObject ).get("y", thisObj);
                            if( ( xArrayObject instanceof NativeArray ) && ( yArrayObject instanceof NativeArray ) )
                            {
                                double[] xPoints = getDoubleArray(thisObj, (NativeArray)xArrayObject);
                                double[] yPoints = getDoubleArray(thisObj, (NativeArray)yArrayObject);
                                if( xPoints.length == yPoints.length )
                                {
                                    ChartSeries xySeries = new ChartSeries(xPoints, yPoints);
                                    xySeries.setLabel(yName);
                                    xySeries.getLines().setShow(false);
                                    series.add(xySeries);
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                throw new IllegalArgumentException("plot: incorrect xValues");
            }
        }
        else
        {
            throw new IllegalArgumentException("plot: incorrect parameters");
        }

        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        for(int i=0; i<series.size(); i++)
        {
            ChartSeries chartSeries = series.get(i);
            chartSeries.setColor(ColorUtils.getDefaultColor(i));
            chart.addSeries(chartSeries);
        }
        options.getXAxis().setLabel(xTitle);
        options.getYAxis().setLabel(yTitle);
        chart.setOptions(options);
        return new ChartDataElement("", null, chart);
    }

    private static ImageElement plotChart(Object arg, Scriptable thisObj)
    {
        Chart chart;
        Object obj = ( arg instanceof NativeJavaObject ) ? ( (NativeJavaObject)arg ).unwrap() : arg;
        if( obj instanceof Chart )
        {
            chart = (Chart)obj;
        } else if(obj instanceof ChartDataElement)
        {
            chart = ((ChartDataElement)obj).getChart();
        } else if(obj instanceof NativeArray)
        {
            if(getDoubleArray( thisObj, (NativeArray)obj ) != null)
                return null;
            try
            {
                chart = new Chart(RhinoUtils.toJSONArray( (NativeArray)obj ));
            }
            catch( JSONException e )
            {
                throw new IllegalArgumentException( "plot: invalid plot specification", e );
            }
        } else
        {
            throw new IllegalArgumentException( "plot: argument type is unsupported" );
        }
        return new ChartDataElement("", null, chart);
    }
    /**
     * Draw box and whisker chart in new window
     */
    public static void boxPlot(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        if( args.length < 1 )
        {
            infoMessage("boxPlot error: incorrect parameters", thisObj);
            return;
        }

        String[] columns = null;
        List<double[]> parameters = new ArrayList<double[]>();

        if( args[0] instanceof NativeArray )
        {
            //parse boxAndWhisker(String[], Double[], ...);
            Object arrayObject = Context.toObject(args[0], thisObj);
            if( ! ( arrayObject instanceof NativeArray ) )
            {
                infoMessage("boxPlot error: all parameters must be arrays of double", thisObj);
                return;
            }
            NativeArray nativeArray = (NativeArray)arrayObject;
            columns = new String[(int)nativeArray.getLength()];
            for( int j = 0; j < columns.length; j++ )
            {
                columns[j] = nativeArray.get(j, thisObj).toString();
            }

            for( int i = 1; i < args.length; i++ )
            {
                arrayObject = Context.toObject(args[i], thisObj);
                if( ! ( arrayObject instanceof NativeArray ) )
                {
                    infoMessage("boxPlot error: all parameters must be arrays of double", thisObj);
                    return;
                }
                double[] doubleArray = getDoubleArray(thisObj, (NativeArray)arrayObject);
                if( doubleArray != null )
                    parameters.add(doubleArray);
            }
        }
        else if( ( args[0] instanceof NativeJavaObject ) && ( ( (NativeJavaObject)args[0] ).unwrap() instanceof TableDataCollection ) )
        {
            //parse boxAndWhisker(TableDataCollection, String, String, ...);
            TableDataCollection tdc = (TableDataCollection) ( (NativeJavaObject)args[0] ).unwrap();

            if( args.length < 2 )
            {
                infoMessage("boxPlot error: need more parameters", thisObj);
                return;
            }

            columns = new String[args.length];

            for( int i = 1; i < args.length; i++ )
            {
                String columnName = Context.toString(args[i]);
                try
                {
                    double[] values = TableDataCollectionUtils.getColumn(tdc, columnName);
                    parameters.add(values);
                    columns[i - 1] = columnName;
                }
                catch( Exception e )
                {
                    infoMessage("boxPlot error: column '" + columnName + "' can not be the values of plot", thisObj);
                    continue;
                }
            }
        }
        else
        {
            infoMessage("boxPlot: incorrect parameters", thisObj);
            return;
        }

        ScriptEnvironment environment = getEnvironment(thisObj);
        if( environment != null )
        {
            BufferedImage plotImage = JSPlotGenerator.generateBoxAndWhisker(columns, parameters);
            environment.showGraphics(plotImage);
        }

        infoMessage("Opening boxPlot...", thisObj);
    }

    /**
     * Open TableDataCollection view in new window
     */
    public static void view(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        if( args.length < 1 )
        {
            infoMessage("view error: incorrect parameters", thisObj);
            return;
        }

        Object tableObject = Context.toObject(args[0], thisObj);
        if( ! ( tableObject instanceof NativeJavaObject ) )
        {
            infoMessage("view error: parameter should implement TableDataCollection", thisObj);
            return;
        }

        tableObject = ( (NativeJavaObject)tableObject ).unwrap();
        if( ! ( tableObject instanceof TableDataCollection ) )
        {
            infoMessage("view error: parameter should implement TableDataCollection", thisObj);
            return;
        }

        TableDataCollection dataCollection = (TableDataCollection)tableObject;
        ScriptEnvironment environment = getEnvironment(thisObj);
        if( environment != null )
        {
            environment.showTable(dataCollection);
        }

        infoMessage("Opening view...", thisObj);
    }

    /**
     * Quit the shell.
     *
     * This only affects the interactive mode.
     */
    public static String quit()
    {
        return QUIT;
    }

    /**
     * Get and set the language version.
     */
    public static double version(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        double result = cx.getLanguageVersion();
        if( args.length > 0 )
        {
            double d = Context.toNumber(args[0]);
            cx.setLanguageVersion((int)d);
        }
        return result;
    }

    /**
     * Load and execute a set of JavaScript source files.
     */
    public static void load(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        for( Object arg : args )
            JScriptContext.processFile(Context.toString(arg));
    }

    /**
     * Load a Java class that defines a JavaScript object using the
     * conventions outlined in ScriptableObject.defineClass.
     *
     * @exception IllegalAccessException if access is not available
     *            to a reflected class member
     * @exception InstantiationException if unable to instantiate
     *            the named class
     * @exception InvocationTargetException if an exception is thrown
     *            during execution of methods of the named class
     * @exception ClassDefinitionException if the format of the
     *            class causes this exception in ScriptableObject.defineClass
     * @exception PropertyException if the format of the
     *            class causes this exception in ScriptableObject.defineClass
     * @see org.mozilla.javascript.ScriptableObject#defineClass
     */
    public static void defineClass(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IllegalAccessException,
            InstantiationException, InvocationTargetException, ClassDefinitionException, PropertyException
    {
        Class<? extends Scriptable> clazz = getClass(args, Scriptable.class);
        ScriptableObject.defineClass(thisObj, clazz);
    }

    /**
     * Load and execute a script compiled to a class file.
     * <p>
     * When called as a JavaScript function, a single argument is
     * expected. This argument should be the name of a class that
     * implements the Script interface, as will any script
     * compiled by jsc.
     *
     * @exception IllegalAccessException if access is not available
     *            to the class
     * @exception InstantiationException if unable to instantiate
     *            the named class
     * @exception InvocationTargetException if an exception is thrown
     *            during execution of methods of the named class
     * @exception JavaScriptException if a JavaScript exception is thrown
     *            during execution of the compiled script
     * @see org.mozilla.javascript.ScriptableObject#defineClass
     */
    public static void loadClass(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IllegalAccessException,
            InstantiationException, InvocationTargetException, JavaScriptException
    {
        Script script = getClass(args, Script.class).newInstance();
        script.exec(cx, thisObj);
    }

    private static <T> Class<? extends T> getClass(Object[] args, Class<T> clazz) throws IllegalAccessException, InstantiationException, InvocationTargetException
    {
        if( args.length == 0 )
            throw reportRuntimeError("msg.expected.string.arg");

        String className = Context.toString(args[0]);
        try
        {
            return Class.forName(className).asSubclass( clazz );
        }
        catch( ClassNotFoundException | ClassCastException ex )
        {
            throw reportRuntimeError("msg.class.not.found", className);
        }
    }

    public static void printMessage(String message, Scriptable scope)
    {
        ScriptEnvironment jsEnvironment = getEnvironment(scope);
        if( jsEnvironment != null )
        {
            jsEnvironment.print(message);
        }
        else
        {
            System.out.println(message);
        }
    }

    public static void infoMessage(String message, Scriptable scope)
    {
        ScriptEnvironment jsEnvironment = getEnvironment(scope);
        if( jsEnvironment != null )
        {
            jsEnvironment.info(message);
        }
        else
        {
            System.out.println(message);
        }
    }

    /**
     * Get JSEnvironment object
     */
    public static ScriptEnvironment getEnvironment(Scriptable scope)
    {
        Object environment = scope.get(Global.ENVIRONMENT_OBJECT, scope);
        if( environment instanceof ScriptEnvironment )
        {
            return (ScriptEnvironment)environment;
        }
        return null;
    }

    private static WeakHashMap<Thread, ScriptEnvironment> environmentMap = new WeakHashMap<Thread, ScriptEnvironment>();
    public static void storeEnvironmentForThread(Thread t, ScriptEnvironment environment)
    {
        environmentMap.put(t, environment);
    }

    public static ScriptEnvironment getEnvironment()
    {
        return environmentMap.get(Thread.currentThread());
    }

    public static double[] getDoubleArrayFromObject(Scriptable thisObj, Object arrayObject)
    {
        if( arrayObject instanceof NativeArray )
        {
            double[] doubleArray = getDoubleArray(thisObj, (NativeArray)arrayObject);
            if( doubleArray != null )
                return doubleArray;
        }
        else if( arrayObject instanceof NativeJavaArray )
        {
            NativeJavaArray nativeArray = (NativeJavaArray)arrayObject;
            Object objectArray = nativeArray.unwrap();
            double[] doubleArray = (double[])objectArray;
            return doubleArray;
        }
        return null;
    }

    protected static double[] getDoubleArray(Scriptable thisObj, NativeArray nativeArray)
    {
        double[] doubleArray = new double[(int)nativeArray.getLength()];
        for( int j = 0; j < doubleArray.length; j++ )
        {
            try
            {
                doubleArray[j] = Double.parseDouble(nativeArray.get(j, thisObj).toString());
            }
            catch( NumberFormatException e )
            {
                printMessage("error: element of arrays must be double", thisObj);
                return null;
            }
        }
        return doubleArray;
    }
    
    public static CharSequence concat(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        LazyStringBuilder builder = new LazyStringBuilder();
        for(Object arg: args)
        {
            if(arg instanceof NativeJavaObject) arg = ((NativeJavaObject)arg).unwrap();
            builder.append(arg instanceof CharSequence?(CharSequence)arg:arg.toString());
        }
        return builder;
    }

    @Override
    public void initStandardObjects(Context cx, boolean sealed)
    {
        if(!init)
        {
            super.initStandardObjects(cx, sealed);
            init = true;
        }
    }

    public static void printToFile(Context cx, Scriptable thisObj, Object[] args, Function funObj)
    {
        File file = new File(args[1].toString());

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)))
        {
            bw.append(args[0].toString());
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }
}
