package biouml.plugins.simulation.java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.SimulationEngineLogger;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.ae.AeConjugateGradientSolver;
import biouml.plugins.simulation.ae.AeLevenbergMarquardSolver;
import biouml.plugins.simulation.ae.AeNelderMeadSolver;
import biouml.plugins.simulation.ae.AeSolver;
import biouml.plugins.simulation.ae.KinSolverWrapper;
import biouml.plugins.simulation.ae.NewtonSolverWrapperEx;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.EntryStream;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.math.model.Formatter;
import ru.biosoft.math.model.JavaFormatter;
import ru.biosoft.util.DPSUtils;

/**
 *Ode Java Simulation Engine (generates JavaBaseModel and runs its simulation by java simulators)
 */
@PropertyName ( "Java simulation engine" )
@PropertyDescription ( "Deterministic simulation engine to solve system of odinary differential equations." )
public class JavaSimulationEngine extends OdeSimulationEngine
{
    private static final long serialVersionUID = -8518921040812722254L;

    public static final String STIFF_PROBLEM = "Problem is too stiff for this solver";
    public static final String UNSTABLE_PROBLEM = "Problem is unstable";

    public final static int DIAGRAM_SIZE_LIMIT = 1000;

    public static final String TEMPLATE_AUTO = "Auto";
    public static final String TEMPLATE_NORMAL_ONLY = "Normal model";
    public static final String TEMPLATE_LARGE_ONLY = "Large model";
    public static final String TEMPLATE_PARALLEL = "Parallel";

    protected String templateType = TEMPLATE_AUTO;

    protected AeSolver algebraicSolver = new NewtonSolverWrapperEx();
    protected String algebraicSolverName = NEWTON_SOLVER;
    protected Template velocityTemplate;
    private File modelFile;

    private String customClassPath = null;
    protected static String NORMAL_TEMPLATE = "resources/odeModelTemplate.vm";
    protected static String LARGE_TEMPLATE = "resources/odeLargeModelTemplate.vm";
    protected static String LARGE_DELAY_TEMPLATE = "resources/odeDelayLargeModelTemplate.vm";
    protected static String PARALLEL_LARGE_TEMPLATE = "resources/odeParallelLargeModelTemplate.vm";

    //Algebraic solver issues, TODO: use registry
    public static final String KIN_SOLVER = "KinSolver";
    public static final String NEWTON_SOLVER = "NewtonSolver";
    public static final String CONJUGATE_GRADIENT = "ConjugateGradient";
    public static final String NELDER_MEAD = "NelderMead";
    public static final String LEVENBERG_MARQUARD = "LevenbergMarquard";
    static final String[] availableSolvers = new String[] {LEVENBERG_MARQUARD, CONJUGATE_GRADIENT, NELDER_MEAD, NEWTON_SOLVER, KIN_SOLVER};

    private boolean largeTemplate = false;
    private boolean hasDelays = false;
    private boolean parallel = false;
    private int threads = 4;

    public JavaSimulationEngine()
    {
        simulatorType = "JAVA";
        log = new SimulationEngineLogger(biouml.plugins.simulation.java.MessageBundle.class.getName(), getClass());
        EventLoopSimulator javaSimulator = new EventLoopSimulator();
        javaSimulator.setSolver(new JVodeSolver());
        simulator = javaSimulator;
    }
    @Override
    public Formatter getFormatter()
    {
        return new JavaFormatter(varHistoricalIndexMapping);
    }

    @Override
    public Object getSolver()
    {
        return ( simulator instanceof EventLoopSimulator ) ? ( (EventLoopSimulator)simulator ).getSolver() : simulator;
    }


    @Override
    public void setSolver(Object solver)
    {
        Object oldValue = getSolver();
        Object oldOptions = getSimulatorOptions();
        SimulatorSupport newSimulator = (SimulatorSupport)solver;

        if( newSimulator instanceof EventLoopSimulator ) //if try to set EventLoopSimulator as solver
        {
            solverName = ( (EventLoopSimulator)newSimulator ).getSolver().getInfo().name;
            simulator = newSimulator;
        }
        else if( newSimulator.getInfo().eventsSupport ) //if try to set solver with ability to check and process events
        {
            solverName = newSimulator.getInfo().name;
            EventLoopSimulator eventSimulator = new EventLoopSimulator();
            eventSimulator.setSolver(newSimulator);
            simulator = eventSimulator;
        }
        else
        {
            solverName = newSimulator.getInfo().name;
            simulator = newSimulator;
        }
        firePropertyChange("simulatorOptions", oldOptions, simulator.getDefaultOptions());
        firePropertyChange("solver", oldValue, solver);
    }

    //Tolerance issues
    @Override
    public double getAbsTolerance()
    {
        return ( (OdeSimulatorOptions)getSimulatorOptions() ).getAtol();
    }

    @Override
    public void setAbsTolerance(double absTolerance)
    {
        ( (OdeSimulatorOptions)getSimulatorOptions() ).setAtol(absTolerance);
    }

    @Override
    public double getRelTolerance()
    {
        return ( (OdeSimulatorOptions)getSimulatorOptions() ).getRtol();
    }

    @Override
    public void setRelTolerance(double relTolerance)
    {
        ( (OdeSimulatorOptions)getSimulatorOptions() ).setRtol(relTolerance);
    }

    @Override
    public void clearContext()
    {
        setAbsTolerance(DEFAULT_ABSOLUTE_TOLERANCE);
        setRelTolerance(DEFAULT_RELATIVE_TOLERANCE);
    }

    //Time span issues
    protected Span span = null;

    public Span getSpan()
    {
        return span;
    }

    public void setSpan(Span span)
    {
        super.setSpan( span );
        this.span = span;
    }

    public void resetSpan()
    {
        double t0 = getInitialTime();
        double tf = getCompletionTime();
        double inc = getTimeIncrement();
        span = ( inc != 0.0 ) ? new UniformSpan(t0, tf, inc) : new ArraySpan(t0, tf);
    }

    @Override
    public void setInitialTime(double initialTime)
    {
        super.setInitialTime(initialTime);
        resetSpan();
    }

    @Override
    public void setCompletionTime(double completionTime)
    {
        super.setCompletionTime(completionTime);
        resetSpan();
    }

    @Override
    public void setTimeIncrement(double timeIncrement)
    {
        super.setTimeIncrement(timeIncrement);
        resetSpan();
    }

    @Override
    public String getEngineDescription()
    {
        return "Java-" + simulator.getInfo().name;
    }


    @PropertyName ( "Code generation type" )
    @PropertyDescription ( "Type of code generation: if auto then BioUML will try to choose by itself." )
    public String getTemplateType()
    {
        return templateType;
    }

    public void setTemplateType(String templateType)
    {
        if( !templateType.equals(this.templateType) )
        {
            this.templateType = templateType;
            this.diagramModified = true; //template was changed therefore model code should be regenerated
        }
    }

    ///////Simulation issues
    ///
    @Override
    public String simulate(File[] files, ResultListener[] listeners) throws Exception
    {
        return simulate(files, true, listeners);
    }

    public String simulate(File[] files, boolean compile, ResultListener[] listeners) throws Exception
    {
        Object[] objs = compileModel(files, compile, outputDir);
        if( objs.length == 0 || ! ( objs[0] instanceof Model ) )
            throw new Exception("Can not find model");
        return simulate((Model)objs[0], listeners);
    }

    public void disableLog()
    {
        setLogLevel( Level.OFF );
    }

    public SimulationResult simulateSimple(Model model) throws Exception
    {
        SimulationResult result = new SimulationResult( null, "" );
        initSimulationResult( result );
        simulate( model, new ResultListener[] {new ResultWriter( result )} );
        return result;
    }

    @Override
    public String simulate(Model model, ResultListener[] listeners) throws Exception
    {
        if( model == null )
            return "Can not simulate, model is null!";

        if( ! ( model instanceof JavaBaseModel ) )
            throw new IllegalArgumentException(
                    "Incorrect model class for JavaSimulationEngine " + model.getClass() + ". Only JavaBaseModel allowed" );

        simulator.setLogLevel( log.getLogger().getLevel() );

        log.info("Model " + diagram.getName() + ": simulation started.");

        if( listeners != null )
            for( ResultListener listener : listeners )
                listener.start(model);

        if( span == null )
            resetSpan();

        if (simulator instanceof SimulatorSupport)
            ((SimulatorSupport)simulator).setPresimulateFastReactions(fastReactions.equals(ODE_SYSTEM) && Util.hasFastReactions(diagram));
            
        ( (JavaBaseModel)model ).setAeSolver(getAlgebraicSolver());
        try
        {
            simulator.start(model, span, listeners, jobControl);
        }
        catch( Throwable t )
        {
            log.info("Model " + diagram.getName() + ": simulation terminated with error.");
            log.info(t.getMessage());
            log.info("");
            return "Simulation error: " + t.getMessage();
        }
        finally
        {
            //if we do not do this then engine have changed diagram before next simulation and when user specify span properties (e.g. time step)
            //then this properties goes to changed diagram, and original is not updated. Maybe we should detach these properties from the diagram
            //and make them just properties of simulation engine
            restoreOriginalDiagram();
        }
        
        checkVariables( ( (JavaBaseModel)model ).getCurrentState(), EntryStream.of(varIndexMapping).invert().toMap());

        log.info("Model " + diagram.getName() + ": simulation finished.");
        log.info("");

        if( simulator.getProfile().isStiff() )
            return STIFF_PROBLEM; //Special text for indicating test result as "stiffness detected"
        else if( simulator.getProfile().isUnstable() )
            return UNSTABLE_PROBLEM;
        return simulator.getProfile().getErrorMessage();
    }
    
    @Override
    public void stopSimulation()
    {
        //if model is static we did not start solver so we do not need to stop it
        if( modelType == EModel.STATIC_TYPE || modelType == EModel.STATIC_EQUATIONS_TYPE )
        {
            log.info("Model " + diagram.getName() + ": simulation finished.");
            return;
        }
        super.stopSimulation();
    }

    //////Code Generation issues
    @Override
    public Model createModel() throws Exception
    {
        try
        {
            if( TEMPLATE_NORMAL_ONLY.equals(getTemplateType()) )
            {
                return doGetModel();
            }
            else if( TEMPLATE_LARGE_ONLY.equals(getTemplateType()) )
            {
                setLargeTemplate();
                JavaLargeModel model = (JavaLargeModel)doGetModel();
                model.setNameToIndex(getGlobalIndexMap());
                return model;
            }
            else if( TEMPLATE_PARALLEL.equals(getTemplateType()) )
            {
                setParallelTemplate();
                JavaLargeModel model = (JavaLargeModel)doGetModel();
                model.setNameToIndex(getGlobalIndexMap());
                return model;
            }
            else //auto detect that diagram is too large
            {
                if( isLarge(diagram) )
                {
                    log.info("Model " + diagram.getName() + " is too large, Template for large models will be used.");
                    setLargeTemplate();
                    JavaLargeModel model = (JavaLargeModel)doGetModel();
                    model.setNameToIndex(getGlobalIndexMap());
                    return model;
                }
                else
                {
                    try
                    {
                        return doGetModel();
                    }
                    catch( Exception ex )
                    {
                        log.error("Error while model generating, will try to use template for large models");
                        restoreOriginalDiagram();
                        setLargeTemplate();
                        modelFile = null;
                        JavaLargeModel model = (JavaLargeModel)doGetModel();
                        model.setNameToIndex(getGlobalIndexMap());
                        return model;
                    }
                }
            }
        }
        catch( Exception ex )
        {
            log.error("Attempt to generate model failed: " + ExceptionRegistry.log(ex));
            modelFile = null;
            return null;
        }
        finally
        {
            resetTemplate();
        }
    }

    public void setClassPath(String customPath)
    {
        this.customClassPath = customPath;
    }

    protected Model doGetModel() throws Exception
    {
        File[] files = generateModel(false);

        if( model != null )
            return model;

        if( files == null )
            throw new Exception("Model " + diagram.getName() + ": Error during code generation.");

        Object[] objs = ( customClassPath != null ) ? compileModel( files, customClassPath ) : compileModel( files, true, outputDir );

        if( objs == null || objs.length == 0 || ! ( objs[0] instanceof Model ) )
            throw new Exception("Model " + diagram.getName() + ": Error during code compilation.");

        return (Model)objs[0];
    }

    public void setLargeTemplate()
    {
        largeTemplate = true;
        velocityTemplate = null; //reset template
    }

    public void setParallelTemplate()
    {
        largeTemplate = true;
        parallel = true;
        velocityTemplate = null; //reset template
    }

    protected void resetTemplate()
    {
        largeTemplate = false;
        parallel = false;
        velocityTemplate = null; //reset template
    }

    /**
     * returns input stream to template
     * @return
     */
    protected InputStream getTemplateInputStream()
    {
        String templatePath = NORMAL_TEMPLATE;

        if( largeTemplate )
            templatePath = parallel ? PARALLEL_LARGE_TEMPLATE : hasDelays ? LARGE_DELAY_TEMPLATE : LARGE_TEMPLATE;

        log.info("Generating code with template " + templatePath);
        return OdeSimulationEngine.class.getResourceAsStream(templatePath);
    }

    private void doGenerateModel() throws Exception
    {
        String name = executableModel.getDiagramElement().getName();
        System.out.println("Generating model" + diagram.getName()+" in folder "+outputDir);
        log.info("Generating model" + diagram.getName()+" in folder "+outputDir);
        File dir = new File(outputDir);
        if( !dir.exists() && !dir.mkdirs() )
            throw new Exception("Failed to create directory '" + outputDir + "'.");
        modelFile = new File(dir, normalize(name) + ".java");

        if( velocityTemplate == null )
        {
            RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
            SimpleNode node = runtimeServices.parse(new InputStreamReader(getTemplateInputStream()), "ODE template");
            velocityTemplate = new Template();
            velocityTemplate.setRuntimeServices(runtimeServices);
            velocityTemplate.setData(node);
            velocityTemplate.initDocument();

            // experimental, possble fix for 
            // Runtime : ran out of parsers. Creating a new one.  Please increment the parser.pool.size property. The current value is too small.
            //Properties props = new Properties();
            //props.setProperty( "parser.pool.size", "50" );
            //Velocity.init( props );

            Velocity.init();
        }
        VelocityContext context = new VelocityContext();

        context.put( "sew", new SimulationEngineWrapper( this ) );
        context.put( "engine", this );

        //Creation time
        String pattern = "yyyy.MM.dd HH:mm:ss";
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        String creationTime = format.format(date);

        context.put("creationTime", creationTime);

        try (BufferedWriter bw = ApplicationUtils.utfWriter(modelFile))
        {
            velocityTemplate.merge(context, bw);
        }
    }

    @Override
    public @Nonnull File[] generateModel(boolean forceRewrite) throws Exception
    {
        if( !diagramModified && !forceRewrite && modelFile != null )
            return new File[] {modelFile};
        
        restoreOriginalDiagram();

        setGenerateVariableAsArray(largeTemplate ? ARRAY_MODE_ON : ARRAY_MODE_OFF);

        init();
        hasDelays = ( modelType & EModel.ODE_DELAY_TYPE ) != 0;

        log.info("Model " + diagram.getName() + ": Java code generating...");

        if( model == null )
            doGenerateModel();
        diagramModified = false;
        return new File[] {modelFile};
    }

    @Override
    public String generateVariableCodeName(int n)
    {
        return "x_values[" + n + "]";
    }

    @Override
    protected String getAsArrayName(int index)
    {
        return "var[" + index + "]";
    }

    public String createFunctionBody(String declarations, String result)
    {
        int splitIndex = result.lastIndexOf("return");
        return result.substring(0, splitIndex) + declarations + result.substring(splitIndex);
    }

    public String getCompartmentRoleName(Compartment compartment)
    {
        final Role role = compartment.getRole();
        return role instanceof VariableRole ? ( (VariableRole)role ).getName() : "";
    }

    public boolean checkShouldBeFind(String variableName)
    {
        return isAlgebraic(variableName);
    }

    private static final Set<String> javaKeywords = new HashSet<>(Arrays.asList("abstract", "continue", "for", "new", "switch", "assert",
            "default", "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements",
            "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long",
            "strictfp", "volatile", "const", "float", "native", "super", "while"));

    /**
     * Generate Java-correct variable name
     */
    @Override
    public String normalize(String name)
    {
        name = super.normalize(name);
        while( javaKeywords.contains(name) || varNames.contains(name) )
            name = name + "_";
        return name;
    }

    @Override
    public boolean isForbidden(String name)
    {
        return javaKeywords.contains( name );
    }
    
    @Override
    public String normalizeFunction(String name)
    {
        while( javaKeywords.contains(name) || functionNames.contains(name) )
            name = name + "_";
        return name;
    }


    private boolean isLarge(Diagram diagram)
    {
        return diagram.getSize() > DIAGRAM_SIZE_LIMIT;
    }
    
    @PropertyName ( "Fast reactions handling" )
    @PropertyDescription ( "Method to handle fast reactions: either as separate ODE system with very fast rate or as algebraic system." )
    public String getFastReactions()
    {
        return fastReactions;
    }
    public void setFastReactions(String fastReactions)
    {
        this.diagramModified = true;
        this.fastReactions = fastReactions;
    }

    @PropertyName ( "Algebraic solver name" )
    @PropertyDescription ( "Name of algebraic solver which will be used" )
    public String getAlgebraicSolverName()
    {
        return algebraicSolverName;
    }
    public void setAlgebraicSolverName(String solverName)
    {
        Object oldValue = this.algebraicSolverName;
        this.algebraicSolverName = solverName;
        switch( solverName )
        {
            case LEVENBERG_MARQUARD:
                setAlgebraicSolver(new AeLevenbergMarquardSolver());
                break;
            case CONJUGATE_GRADIENT:
                setAlgebraicSolver(new AeConjugateGradientSolver());
                break;
            case NELDER_MEAD:
                setAlgebraicSolver(new AeNelderMeadSolver());
                break;
            case NEWTON_SOLVER:
                setAlgebraicSolver(new NewtonSolverWrapperEx());
                break;
            case KIN_SOLVER:
                setAlgebraicSolver(new KinSolverWrapper());
                break;
            default:
                break;
        }
        firePropertyChange("algebraicSolverName", oldValue, solverName);
    }

    @PropertyName ( "Algebraic Solver parameters" )
    @PropertyDescription ( "Parameters of algebraic solver which will be used" )
    public AeSolver getAlgebraicSolver()
    {
        return algebraicSolver;
    }
    public void setAlgebraicSolver(AeSolver solver)
    {
        Object oldValue = this.algebraicSolver;
        this.algebraicSolver = solver;
        firePropertyChange("algebraicSolver", oldValue, solver);
        firePropertyChange( "*", null, null );
    }

    /**
     * Is used for model generated by large template. It needs mapping between variable name and index in global array ("var")
     */
    private Map<String, Integer> getGlobalIndexMap()
    {
        return EntryStream.of(varNameMapping).filter(e -> e.getValue().startsWith("var"))
                .mapValues(val -> Integer.parseInt(val.substring(4, val.length() - 1))).toMap();
    }

    public static String[] getTemplateMethods()
    {
        return new String[] {TEMPLATE_AUTO, TEMPLATE_NORMAL_ONLY, TEMPLATE_LARGE_ONLY, TEMPLATE_PARALLEL};
    }



    @PropertyName ( "Threads number" )
    public int getThreads()
    {
        return threads;
    }

    public void setThreads(int threads)
    {
        this.threads = threads;
    }

    /**
     * Utility method to get all ODE species 
     */
    public String[] getFloatingSpecies()
    {
        return varNameRateIndexMapping.keySet().stream().filter( n -> n.startsWith( "$" ) ).map( s -> {
            s = s.substring( s.lastIndexOf( "." ) + 1 );
            if( s.startsWith( "$" ) )
                s = s.substring( 1 );
            return s;
        } ).toArray( String[]::new );
    }
    
    public static final String PLOTS = "Plots";
    
    @Override
    public Object getPlotsBean(Diagram diagram)
    {
        Role role = diagram.getRole();
        if (!(role instanceof EModel))
            return null;
        Object plotsObj = diagram.getAttributes().getValue( PLOTS );
        PlotsInfo result = null;
        if( ! ( plotsObj instanceof PlotsInfo ) )
        {
            result = new PlotsInfo( (EModel)role );
            diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( PLOTS, PlotsInfo.class, result ) );
        }
        else
            result = (PlotsInfo)plotsObj;
        return result;
    }
}
