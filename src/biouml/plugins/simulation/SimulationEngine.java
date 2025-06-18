package biouml.plugins.simulation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;

import one.util.streamex.StreamEx;

import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.WeakPropertyChangeForwarder;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.access.core.PluginEntry;
import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.java.RunTimeCompiler;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;

import com.developmentontheedge.beans.Option;
import ru.biosoft.jobcontrol.FunctionJobControl;


/**
 * General class for simulation engine. It defines common contract
 * (properties and methods) for any simulation engine.
 */
abstract public class SimulationEngine extends Option implements PropertyChangeListener, JSONBean
{
    public static int DETERMINISTIC_TYPE = 0;
    public static int STOCHASTIC_TYPE = 1;
    
    public static String VAR_PATH_DELIMITER = "/";
    
    protected SimulationEngineLogger log = new SimulationEngineLogger();
    
    /**Directory where generated files will be stored*/
    private String srcDir = ".";
    
    /**
     * Flag indicating that diagram was modified (elements added\removed, new diagram set) and we should reinit engine
     * For now it is used only to avoid double work during getModel - getModel2 execution
     * TODO: actually listen to the diagram and update this flag
     */
    protected boolean diagramModified = false;
    
    protected WeakPropertyChangeForwarder diagramListener;
    
    /**Preprocessors added by user that should be fired before default preprocessors*/
    private final List<Preprocessor> additionalPreprocessors = new ArrayList<>();
    
    /**Preprocessors added by user that should be fired after default preprocessors*/
    private final List<Preprocessor> additionalPreprocessors2 = new ArrayList<>();
    
    /** Span characterizing result output */
    private double initialTime = 0;
    private double timeIncrement = 1;
    private double completionTime = 100;
        
    protected FunctionJobControl jobControl;

    /**Simulator type: "JAVA", "JAVA_STOCHASTIC", etc. Defines list of available simulators for current engine*/
    protected String simulatorType = "NONE";
    protected String solverName;
    protected Simulator simulator;
    protected Diagram diagram;

    protected Model model;

    /**Diagram before any preprocessing*/
    protected Diagram originalDiagram;
    protected EModel executableModel;

    protected boolean terminated = false;

    /**If true than simulation engine should show results on plot while simulating*/
    public boolean needToShowPlot = true;

    /**Type of the model: STATIC, ODE, etc.*/
    protected int modelType;

    protected SimulationResult result;
    
    public String getSimulatorType()
    {
        return simulatorType;
    }
    public FunctionJobControl getJobControl()
    {
        return jobControl;
    }
    public void setJobControl(FunctionJobControl jobControl)
    {
        this.jobControl = jobControl;
    }
    public int getModelType()
    {
        return modelType;
    }

    public void setNeedToShowPlot(boolean value)
    {
        needToShowPlot = value;
    }

    @PropertyName("Show plot")
    public boolean getNeedToShowPlot()
    {
        return needToShowPlot;
    }

    public boolean hasVariablesToPlot()
    {
        Object plotObj = executableModel.getParent().getAttributes().getValue("Plots");

        if( ! ( plotObj instanceof PlotsInfo ) )
            return false;

        PlotsInfo plots = (PlotsInfo)plotObj;

        for( PlotInfo plot : plots.getActivePlots() )
        {
            if( plot.getYVariables().length > 0 )
                return true;
        }
        return false;
    }

    public List<String> getIncorrectPlotVariables() throws Exception
    {
        List<String> incorrect = new ArrayList<>();
        Object plotObj = executableModel.getParent().getAttributes().getValue( "Plots" );
        if( plotObj == null )
            return incorrect;
        PlotsInfo plots = (PlotsInfo)plotObj;
        for( PlotInfo plot : plots.getActivePlots() )
        {
            PlotVariable xVar = plot.getXVariable();
            String path = xVar.getPath();
            EModel emodel = path.isEmpty() ? executableModel
                    : Util.getSubDiagram( originalDiagram, path ).getDiagram().getRole( EModel.class );

            if( !emodel.containsVariable( xVar.getName() ) )
                incorrect.add( xVar.getTitle() );

            for( Curve curve : plot.getYVariables() )
            {
                path = curve.getPath();
                if( path.isEmpty() )
                    emodel = executableModel;
                else
                {
                    SubDiagram subDiagram = Util.getSubDiagram( originalDiagram, path );
                    if( subDiagram == null )                    
                        throw new Exception( "Can not find subdiagram" + path + " in diagram " + originalDiagram );                    
                    emodel = subDiagram.getDiagram().getRole( EModel.class );
                }

                if( !emodel.containsVariable( curve.getName() ) )
                    incorrect.add( curve.getTitle() );
            }
        }
        return incorrect;
    }

    @Override
    public SimulationEngine clone()
    {
        SimulationEngine simulationEngine = null;
        try
        {
            simulationEngine = getClass().newInstance();

            Object solver = getSolver().getClass().newInstance();

            if( solver instanceof Simulator )
            {
                ( (Simulator)solver ).setOptions( ( (Simulator)getSolver() ).getOptions() );
                simulationEngine.setSolver( solver );
            }

            simulationEngine.setCompletionTime( completionTime );
            simulationEngine.setTimeIncrement( timeIncrement );
            simulationEngine.setDiagram( getDiagram() );
        }
        catch( Exception e )
        {
            log.error( "Can not clone simulation engine.", e );
        }
        return simulationEngine;
    }

    //Abstract methods
    public abstract String getEngineDescription();

    /**
     * Get variable name used in mathematical model<br>
     * Engine must guarantee that generated model will have field with name equal to returned by this method String
     */
    public abstract String getVariableCodeName(String varName);

    /**
     * Get variable name used in plain mathematical model of composite diagrams<br>
     */
    public abstract String getVariableCodeName(String diagramName, String varName);

    /**
     * Mapping between variable name and its index in results array.
     */
    public abstract Map<String, Integer> getVarIndexMapping();
    
    /**
     * Mapping between variable path and its index in results array.
     */
    public abstract Map<String, Integer> getVarPathIndexMapping();

    /**
     * Creates mathematical model for further simulation
     */
    public abstract Model createModel() throws Exception;

    /**
     * The method is required for the optimization process, where model is the same for all threads.
     */
    public void setModel(Model model)
    {
        this.model = model;
    }

    /**
     *
     * @param varName
     * @param value
     * @throws IllegalArgumentException
     */
    public void setInitialValue(String varName, double value) throws IllegalArgumentException
    {
        if( !executableModel.containsVariable( varName ) )
            throw new IllegalArgumentException( "Variable " + varName + " not found" );

        executableModel.getVariable( varName ).setInitialValue( value );
    }

    public String[] getVariableNames()
    {
        try
        {
            DataCollection<Variable> collection = executableModel.getVariables();
            return collection.names().toArray( String[]::new );
        }
        catch( Exception ex )
        {
            return new String[] {};
        }
    }

    //override this method if necessary
    public void setOutputDir(String outputDir)
    {
        //do nothing by default
    }

    //override this method if necessary
    public String getOutputDir()
    {
        return null;
    }

    /**
     * Some strange method... not recommended<br>
     *  (it can return String instead of simulator or it can return inner solver without EventLoopSimulator around it which can be inappropriate).
     */
    abstract public Object getSolver();
    abstract public void setSolver(Object solver);

    public File[] generateModel(boolean forceRewrite) throws Exception
    {
        throw new Exception( "Current engine does not support generating model into file" );
    }

    /**
     * Creates mathematical model from file
     * @param files
     * @param compile
     * @param outputDir
     * @return
     * @throws Exception
     */
    public Object[] compileModel(File[] files, boolean compile, String outputDir) throws Exception
    {
        log.info( "Model " + diagram.getName() + ": Java code compilation..." );
        Object[] result = new Object[files.length];
        for( int i = 0; i < files.length; i++ )
        {
            String className = files[i].getName();
            className = className.substring( 0, className.length() - 5 );

            if( compile )
            {
                files[i] = new File( outputDir, className + ".java" );
                StringBuilder classPath = new StringBuilder(outputDir);
                for( PluginEntry f : getClassPathEntries() )
                    classPath.append(File.pathSeparator).append(f.extract().getAbsolutePath());

                RunTimeCompiler comp = new RunTimeCompiler( classPath.toString(), outputDir, new String[] {outputDir + File.separator
                        + files[i].getName()} );
                if( !comp.execute() )
                    throw new BiosoftParseException( new Exception(comp.getMessages()), files[i].getName() );
            }

            File outDir = new File( outputDir );
            URL[] url = new URL[] {outDir.toURI().toURL()};
            try(URLClassLoader cl = new URLClassLoader( url, getClass().getClassLoader() ))
            {
                result[i] = cl.loadClass( className ).newInstance();
            }
            catch( Throwable t )
            {
                throw new Exception( "Can not load model, name=" + className + ", error=" + t );
            }
        }
        return result;
    }

    public Object[] compileModel(File[] files, String classPath) throws Exception
    {
        log.info( "Model " + diagram.getName() + ": Java code compilation..." );
        Object[] result = new Object[files.length];

        String outDirName = files[0].getParentFile().getAbsolutePath();
        String[] fileNames = new String[files.length];
        for( int i = 0; i < files.length; i++ )
            fileNames[i] = files[i].getAbsolutePath();

        RunTimeCompiler comp = new RunTimeCompiler( classPath, outDirName, fileNames );
        if( !comp.execute() )
            throw new BiosoftParseException( new Exception( comp.getMessages() ), files[0].getName() );

        URL[] url = new URL[] {new File( outDirName ).toURI().toURL()};
        for( int i = 0; i < files.length; i++ )
        {
            String fileName = files[i].getName();
            String className = fileName.substring( 0, fileName.lastIndexOf( "." ) );
            try (URLClassLoader cl = new URLClassLoader( url, getClass().getClassLoader() ))
            {
                result[i] = cl.loadClass( className ).newInstance();
            }
            catch( Throwable t )
            {
                throw new Exception( "Can not load model, name=" + className + ", error=" + t );
            }
        }
        return result;
    }

    protected List<PluginEntry> getClassPathEntries()
    {
        try
        {
            return Arrays.asList( ApplicationUtils.resolvePluginPath( "biouml.plugins.simulation:src.jar" ) );
        }
        catch( Exception e )
        {
            return Collections.emptyList();
        }
    }

    public String getSrcDir()
    {
        return srcDir;
    }


    public void setSrcDir(String dir)
    {
        this.srcDir = dir;
    }

    /**
     * Utility method to write needed source files from the specified source
     * to the specified directory.
     */
    protected void writeFile(String source, boolean rewrite) throws Exception
    {
        File out = new File( getOutputDir(), source );
        if( out.exists() && !rewrite )
            return;
        out.getParentFile().mkdirs();

        // load from srcDir
        File file = new File( source );
        if( !file.exists() )
            file = new File( getSrcDir(), source );

        InputStream is;
        if( file.exists() )
            is = new FileInputStream( file );
        else
        {
            // try to load through plugin class loader
            ClassLoader cl = getClass().getClassLoader();
            URL url = cl.getResource( source );

            if( url == null ) // try to get resources as file
                throw new FileNotFoundException( "Can not find file: " + source );

            is = url.openStream();
        }

        com.developmentontheedge.application.ApplicationUtils.copyStream( new FileOutputStream( out, false ), is );
    }

    public String[] getAvailableSolvers()
    {
        return SimulatorRegistry.registry( getSimulatorType() ).keys().toArray( String[]::new );
    }

    /**
     * Simulate mathematical model <b>model</b>
     * @param model
     * @param resultListeners
     * @return
     * @throws Exception
     */
    public abstract String simulate(Model model, ResultListener[] resultListeners) throws Exception;

    /**
     * Method to simulate model generated from currently set diagram
     * @param resultListeners
     * @return
     * @throws Exception
     */
    public String simulate(ResultListener[] resultListeners) throws Exception //again only in tests
    {
        return simulate( createModel(), resultListeners );
    }

    public String simulate(Model model) throws Exception //used only in tests (except TreatmentSystem)
    {
        return simulate( model, getListeners() );
    }

    public String simulate() throws Exception //used only in tests
    {
        return simulate( createModel() );
    }

    protected List<ResultListener> listeners = new ArrayList<>();
    public void addListeners(ResultListener ... resultListeners)
    {
        listeners.addAll( Arrays.asList( resultListeners ) );
    }

    public ResultListener[] getListeners()
    {
        return listeners.toArray( new ResultListener[listeners.size()] );
    }

    public void removeAllListeners()
    {
        listeners.clear();
    }

    /**
     * Method to simulate model and setting results to SimulationResult <b>result</b>
     * @param model
     * @param result
     * @return
     * @throws Exception
     */
    public String simulate(Model model, SimulationResult result) throws Exception
    {
        initSimulationResult( result );
        return simulate( model, new ResultListener[] {new ResultWriter( result )} );
    }

    /**
     * Method to simulate model and setting results to SimulationResult <b>result</b>
     * @param model
     * @param result
     * @return
     * @throws Exception
     */
    public String simulate(SimulationResult result) throws Exception
    {
        return simulate( createModel(), result );
    }


    /**
     * Common method to stop simulation by stopping simulator work
     */
    public void stopSimulation()
    {
        setTerminated(true);
        if( simulator != null )
            simulator.stop();
    }

    /**
     * Returns options for simulator
     * @return
     */
    @PropertyName("Simulator options")
    @PropertyDescription("Simulator options.")
    public Options getSimulatorOptions()
    {
        return simulator.getOptions();
    }

    public void setSimulatorOptions(Options options)
    {
        Options oldValue = this.simulator.getOptions();
        options.setParent( this );
        simulator.setOptions( options );
        firePropertyChange( "simulatorOptions", oldValue, options );
        firePropertyChange( "*", null, null );
    }


    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /**
     * Method to get real simulator for further using independently from this engine.<br>
     * @return
     */
    public Simulator getSimulator()
    {
        return simulator;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    /**
     * returns unprocessed diagram
     * @return
     */
    public Diagram getOriginalDiagram()
    {
        return originalDiagram;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( doNotAffectSimulation( evt))
           return;
        diagramModified = true;
        restoreOriginalDiagram();
    }

    protected boolean doNotAffectSimulation(PropertyChangeEvent evt)
    {
        if( evt != null )
        {
            String propertyName = evt.getPropertyName();
            if( "path".equals( propertyName ) || "location".equals( propertyName ) )
                return true;
        }
        return false;
    }

    public void releaseDiagram()
    {
        if( this.diagram != null )
            this.diagram.removePropertyChangeListener( diagramListener );
    }

    public void setDiagram(Diagram diagram)
    {
        Diagram oldValue = this.diagram;
        this.originalDiagram = diagram;
        this.diagram = diagram;
        if( diagram != null && diagram.getRole() != null && diagram.getRole() instanceof EModel )
        {
            executableModel = diagram.getRole( EModel.class );
        }
        firePropertyChange( "diagram", oldValue, diagram );

        diagramModified = true;

        if( oldValue != null )
            oldValue.removePropertyChangeListener( diagramListener );

        diagramListener = diagram != null ? new WeakPropertyChangeForwarder( this, diagram ) : null;
    }

    public EModel getExecutableModel()
    {
        return executableModel;
    }

    @PropertyName("Simulator name")
    @PropertyDescription("Simulator name.")
    public String getSolverName()
    {
        return SimulatorRegistry.getSolverName( getSolver().getClass() );
    }

    public void setSolverName(String solverName)
    {
        if( Objects.equals( this.solverName, solverName ) )
            return;

        try
        {
            Simulator simulator = SimulatorRegistry.getSimulator( solverName );
            simulator.getOptions().setParent( this );
            setSolver( simulator );
            this.solverName = solverName;
        }
        catch( Exception e )
        {
            log.error( "Can set solver name by name, error: " + e, e );
            return;
        }
    }
    
    /**
     * Sets initial, completion and time step from span
     */
    public void setSpan(Span span)
    {
        setInitialTime(span.getTimeStart());
        setCompletionTime(span.getTimeFinal());
        if( span instanceof UniformSpan )
        {
            setTimeIncrement( ( (UniformSpan)span ).getTimeIncrement());
        }
        else
        {
            setTimeIncrement(span.getTime(1) - span.getTimeStart());
        }
    }

    @PropertyName("Initial time")
    @PropertyDescription("Initial time.")
    public double getInitialTime()
    {
        return this.initialTime;
    }

    public void setInitialTime(double initialTime)
    {
        double oldValue = getInitialTime();
        this.initialTime = initialTime;
        firePropertyChange( "initialTime", oldValue, initialTime );
    }
   
    @PropertyName("Completion time")
    @PropertyDescription("Completion time.")
    public double getCompletionTime()
    {
        return this.completionTime;
    }

    public void setCompletionTime(double completionTime)
    {
        double oldValue = getCompletionTime();
        this.completionTime = completionTime;
        firePropertyChange( "completionTime", oldValue, completionTime );
    }
    
    @PropertyName("Time increment")
    @PropertyDescription("Time increment.")
    public double getTimeIncrement()
    {
        return this.timeIncrement;
    }

    public void setTimeIncrement(double timeIncrement)
    {
        double oldValue = getTimeIncrement();
        this.timeIncrement = timeIncrement;
        firePropertyChange( "timeIncrement", oldValue, timeIncrement );
    }

    public void initSimulationResult(SimulationResult simuationResult)
    {
        simuationResult.setInitialTime( getInitialTime() );
        simuationResult.setCompletionTime( getCompletionTime() );
        //        simuationResult.setSimulatorName( getSolverName() );
        if( diagram != null )
            simuationResult.setDiagramPath( diagram.getCompletePath() );

        // fill initial values
        for( Variable var : executableModel.getVariables() )
        {
            if( Boolean.TRUE.equals( var.getAttributes().getValue( Preprocessor.AUTOGENERATED_VAR ) ) )
                continue;
            simuationResult.addInitialValue( var );
        }
        simuationResult.setVariableMap( getVarPathIndexMapping()  );
        simuationResult.setVariablePathMap( getVarPathIndexMapping() );
        this.result = simuationResult;
    }

    /** Returns information about x variable for this particular plot */
    public SimulationEngine.Var getXVariable(PlotInfo plot)
    {
        PlotVariable xVariable = plot.getXVariable();
        String name = xVariable.getName();
        String title = xVariable.getTitle();
        String path = xVariable.getPath();
        SubDiagram subDiagram = Util.getSubDiagram(originalDiagram, path);
        EModel emodel = (subDiagram != null)? subDiagram.getDiagram().getRole(EModel.class): originalDiagram.getRole(EModel.class);
        Variable var = emodel.getVariable( name );
        double initialValue = var.getInitialValue();
        Integer index = getVarPathIndexMapping().get(path.isEmpty() ? name : path + VAR_PATH_DELIMITER + name);
        return new Var( name, title, initialValue, index, null );
    }
    
    /** Returns information about all plots that should be demonstrated */
    public PlotInfo[] getPlots()
    {
        Object plotObj = executableModel.getParent().getAttributes().getValue("Plots");
        if( plotObj instanceof PlotsInfo )
            return ( (PlotsInfo)plotObj ).getActivePlots();
        return new PlotInfo[0];
    }
    
    /** Returns all variables which should be demonstrated on this particular plot */
    public Map<SimulationEngine.Var, List<Series>> getVariablesToPlot(PlotInfo plot)
    {
        Map<Var, List<Series>> variablesToPlot = new TreeMap<>();

        for( Curve curve : plot.getYVariables() )
        {
            String name = curve.getName();
            String title = curve.getTitle();
            String path = curve.getPath();
            SubDiagram subDiagram = Util.getSubDiagram(originalDiagram, path);
            EModel emodel = (subDiagram != null)? subDiagram.getDiagram().getRole(EModel.class): originalDiagram.getRole(EModel.class);
            double initialValue = emodel.getVariable(name).getInitialValue();
            Integer index = getVarPathIndexMapping().get(path.isEmpty() ? name : path + VAR_PATH_DELIMITER + name);
            List<Series> list = new ArrayList<>();   
            list.add(new XYSeries(title, false, true));
            if( !variablesToPlot.keySet().stream().anyMatch( k -> k.title.equals( title ) ) )
                variablesToPlot.put( new Var( name, title, initialValue, index, curve.getPen() ), list );
        }
        return variablesToPlot;
    }

    public void setStandalone(boolean standalone)
    {
        //do nothing by default
    }
    
    public boolean isTerminated()
    {
        return terminated;
    }
    public void setTerminated(boolean terminated)
    {
        this.terminated = terminated;
    }


    public void restoreOriginalDiagram()
    {
        this.diagram = originalDiagram;
        this.executableModel = (EModel)originalDiagram.getRole();
    }

    protected List<Preprocessor> getDiagramPreprocessors()
    {
        return new ArrayList<>();
    }

    public void preprocess(Diagram originalDiagram) throws Exception
    {
        executableModel = diagram.getRole( EModel.class );

        boolean notify = executableModel.isNotificationEnabled();
        boolean notifyDiagram = diagram.isNotificationEnabled();
        executableModel.setNotificationEnabled( false );
        diagram.setNotificationEnabled( false );

        for( Preprocessor p : StreamEx.of(additionalPreprocessors).append(getDiagramPreprocessors()).append(additionalPreprocessors2)
                .filter(p -> p.accept(diagram)) )
            diagram = p.preprocess(diagram);

        executableModel.setNotificationEnabled( notify );
        diagram.setNotificationEnabled( notifyDiagram );
        executableModel = diagram.getRole( EModel.class );
    }

    /**
     * Add preprocessor which will be used before all other preprocessors
     */
    public void addPreprocessor(Preprocessor preprocessor)
    {
        additionalPreprocessors.add( preprocessor );
    }

    /**
     * Add preprocessor which will be used after all other preprocessors
     */
    public void addPreprocessor2(Preprocessor preprocessor)
    {
        additionalPreprocessors2.add( preprocessor );
    }

    public void resetPreprocessors()
    {
        additionalPreprocessors.clear();
        additionalPreprocessors2.clear();
    }

    public static String escapeWrongSymbols(String str)
    {
        String wrongLetters = ": .,;|()[]{}+-*/%^!~&|$'\"";
        char[] data = str.toCharArray();
        for( int i = 0; i < data.length; i++ )
        {
            if( wrongLetters.indexOf( data[i] ) >= 0 )
                data[i] = '_';
        }

        str = new String( data );
        if( Character.isDigit( str.charAt( 0 ) ) )
            str = "_" + str;

        return str;
    }

    /**
     * Normalize name to be valid simulation script name.
     */
    public String normalize(String name)
    {
        // cut $ prefix
        while( name.charAt( 0 ) == '$' )
        {
            name = name.substring( 1 );
        }

        // cut '.xml' suffix
        int offset = name.lastIndexOf( "." );
        if( offset > 0 && name.length() > offset + 3 && name.charAt( offset + 1 ) == 'x' && name.charAt( offset + 2 ) == 'm'
                && name.charAt( offset + 3 ) == 'l' )
        {
            name = name.substring( 0, offset );
            // replaces most common error characters
        }

        name = escapeWrongSymbols( name );

        return name;
    }

    public void clearContext()
    {
        //nothing by default
    }
    
    public SimulationEngineLogger getLogger()
    {
        return log;
    }
    
    public void setLogLevel(Level level)
    {
        log.getLogger().setLevel( level );
    }
    
    public static class Var implements Comparable<Var>
    {
        public String name;
        public double initialValue;
        public Integer index;
        public Pen pen;
        public Var mainVar = null;
        public String title;

        public Var(Var mainVar)
        {
            this(mainVar.name, mainVar.initialValue, mainVar.index, mainVar.pen);
            this.mainVar = mainVar;
        }

        public Var(String name, double initialValue, Integer index, Pen pen)
        {
            this(name, name, initialValue, index, pen);
        }
        
        public Var(String name, String title, double initialValue, Integer index, Pen pen)
        {
            this.name = name;
            this.title = title;
            this.initialValue = initialValue;
            this.index = index;
            this.pen = pen;
        }

        @Override
        public int compareTo(Var o)
        {
            int compareNames = name.compareTo(o.name);
            return compareNames == 0 ? title.compareTo(o.title) : 1;
        }
    }

    /**Utility method. Finds NANs and Infinity in model parameters and logs message*/
    public void checkVariables(double[] values, Map<Integer, String> names)
    {
        List<String> nan = new ArrayList<>();
        List<String> inf = new ArrayList<>();

        for( int i = 0; i < values.length; i++ )
        {
            double val = values[i];
            if( Double.isNaN(val) )
                nan.add(names.get(i));
            else if( Double.isInfinite(val) )
                inf.add(names.get(i));
        }
        if( nan.isEmpty() && inf.isEmpty() )
            return;

        log.info("At time = " + getSimulator().getProfile().getTime() + ":");
        if( !nan.isEmpty() )
            log.info("Next variables turned NaN: " + StreamEx.of(nan).joining(",\t"));

        if( !inf.isEmpty() )
            log.info("Next variables turned INFINITE: " + StreamEx.of(inf).joining(",\t"));
    }
    
    public SimulationResult generateSimulationResult()
    {
        SimulationResult result = new SimulationResult(null, "tmp");
        initSimulationResult(result);
        return result;
    }
    
    public int getSimulationType()
    {
        return DETERMINISTIC_TYPE;
    }

    public ResultListener generateResultPlot(FunctionJobControl jobControl, PlotInfo plotInfo)
    {
        return new ResultPlotPane( this, jobControl, plotInfo );
    }
    
    public Object getPlotsBean(Diagram diagram)
    {
        return null;
    }
}