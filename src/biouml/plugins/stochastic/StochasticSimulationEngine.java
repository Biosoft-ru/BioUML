package biouml.plugins.stochastic;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jfree.data.general.Series;
import org.jfree.data.xy.YIntervalSeries;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.BooleanPreprocessor;
import biouml.plugins.simulation.ConstraintPreprocessor;
import biouml.plugins.simulation.CycledResultListener;
import biouml.plugins.simulation.DelayPreprocessor;
import biouml.plugins.simulation.EmptyMathPreprocessor;
import biouml.plugins.simulation.EquationTypePreprocessor;
import biouml.plugins.simulation.EventPreprocessor;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Preprocessor;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.ScalarCyclesPreprocessor;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.StateTransitionPreprocessor;
import biouml.plugins.simulation.StochasticResultPlotPane;
import biouml.plugins.simulation.TableElementPreprocessor;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.stochastic.solvers.GillespieSolver;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Unit;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.PluginEntry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.ApplicationUtils;

@PropertyName ( "Stochastic simulation engine" )
@PropertyDescription ( "Stochastic java simulation engine to generate and solve stochastic models." )
public class StochasticSimulationEngine extends JavaSimulationEngine
{
    public static final String RESULT_TYPE_MEAN = "Mean values";
    public static final String RESULT_TYPE_REPEATS = "Repeats";

    private Map<String, Integer> reactionNameIndexMapping;
    private int simulationNumber = 100;
    private String resultType = RESULT_TYPE_MEAN;
    private boolean transformRates = true;
    private int reactionNumber;
    private Map<String, Node> reactionNodes;
    private int[][] reactionDependencies;
    private String templatePath;
    private boolean outputMolecules = false;
    private int seed = 0;
    private boolean customSeed = false;
    private boolean averageRegime = false;
    private int threadsRepeats = 1;

    public StochasticSimulationEngine()
    {
        setSolver( new GillespieSolver() );
        simulatorType = "JAVA_STOCHASTIC";
        templatePath = "resources/stochasticModelTemplate.vm";
    }

    @Override
    public List<Preprocessor> getDiagramPreprocessors()
    {
        List<Preprocessor> preprocessors = new ArrayList<>();
        preprocessors.add( new EmptyMathPreprocessor() );
        preprocessors.add( new ConstraintPreprocessor( constraintsViolation ) );
        preprocessors.add( new BooleanPreprocessor() );
        preprocessors.add( new StateTransitionPreprocessor() );
        preprocessors.add( new EventPreprocessor() );
        preprocessors.add( new EquationTypePreprocessor() );
        preprocessors.add( new DelayPreprocessor() );
        preprocessors.add( new TableElementPreprocessor() );
        preprocessors.add( new StochasticAssignmentPreprocessor() );
        preprocessors.add( new ScalarCyclesPreprocessor() );

        if( transformRates )
            preprocessors.add( new StochasticPreprocessor() );
        return preprocessors;
    }

    @Override
    public String getEngineDescription()
    {
        return "Stochastic-" + simulator.getInfo().name;
    }

    @PropertyName ( "Number of simulations" )
    @PropertyDescription ( "Number of simulations." )
    public int getSimulationNumber()
    {
        return simulationNumber;
    }
    public void setSimulationNumber(int number)
    {
        if( simulationNumber > 0 )
            this.simulationNumber = number;
    }

    @PropertyName ( "Transform kinetik laws" )
    @PropertyDescription ( "Transform kinetik laws, e.g.: X^2 => X*(X-1)." )
    public boolean getTransformRates()
    {
        return transformRates;
    }
    public void setTransformRates(boolean transformRates)
    {
        this.transformRates = transformRates;
    }

    @PropertyName ( "Result type" )
    @PropertyDescription ( "Result type." )
    public String getResultType()
    {
        return resultType;
    }
    public void setResultType(String resultType)
    {
        this.resultType = resultType;
    }

    @Override
    public String simulate(Model model, SimulationResult result) throws Exception
    {
        if( threadsRepeats > 1 )
            return simulateParallel( model, result );
        initSimulationResult( result );

        ResultWriter writer = new ResultWriter( result );

        ResultListener[] listeners = getListeners();
        ResultListener[] fullListeners;

        if( listeners == null )
            fullListeners = new ResultListener[] {writer};
        else
        {
            fullListeners = new ResultListener[listeners.length + 1];
            System.arraycopy( listeners, 0, fullListeners, 0, listeners.length );
            fullListeners[listeners.length] = writer;
        }

        return simulate( model, fullListeners );
    }

    public String simulateParallel(Model model, SimulationResult result) throws Exception
    {
        initSimulationResult( result );

        ResultListener listener;
        if( result instanceof ResultListener )
            listener = (ResultListener)result;
        else
            listener = new ResultWriter( result );

        ResultListener[] listeners = getListeners();
        ResultListener[] fullListeners;

        if( listeners == null )
            fullListeners = new ResultListener[] {listener};
        else
        {
            fullListeners = new ResultListener[listeners.length + 1];
            System.arraycopy( listeners, 0, fullListeners, 0, listeners.length );
            fullListeners[listeners.length] = listener;
        }

        return simulateParallel( model, fullListeners );
    }

    @Override
    public String simulate(Model model, ResultListener[] listeners) throws Exception
    {
        if( ! ( model instanceof StochasticModel ) )
            throw new IllegalArgumentException( "Incorrect model class for StochasticSimulationEngine "
                    + ( model == null ? null : model.getClass() ) + ". Only StochasticModel allowed" );

        log.info( "Model " + diagram.getName() + ": simulation started." );

        if( span == null )
        {
            double t0 = getInitialTime();
            double tf = getCompletionTime();
            span = ( getTimeIncrement() != 0.0 ) ? new UniformSpan( t0, tf, getTimeIncrement() ) : new ArraySpan( t0, tf );
        }

        List<CycledResultListener> cycledListeners = new ArrayList<>();

        if( listeners != null )
        {
            for( ResultListener listener : listeners )
            {
                if( listener instanceof CycledResultListener )
                    cycledListeners.add( (CycledResultListener)listener );

                if( listener instanceof ResultWriter )
                    initSimulationResult( ( (ResultWriter)listener ).getResults() );

                listener.start( model );
            }
        }

        try
        {
            resetSeed();
            if( !model.isInit() )
                model.init();
            double[] initialValues = model.getCurrentValues();
            for( int i = 0; i < simulationNumber; i++ )
            {
                simulator.start( model, span, listeners, jobControl );

                if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                    return null;

                if( i < simulationNumber - 1 )
                {
                    for( CycledResultListener listener : cycledListeners )
                        listener.startCycle();
                }
                model.setCurrentValues( initialValues );
            }

            for( CycledResultListener listener : cycledListeners )
                listener.finish();
        }
        catch( Throwable t )
        {
            return "Simulation error: " + t.getMessage();
        }

        log.info( "Model " + diagram.getName() + ": simulation finished." );
        log.info( "" );

        if( jobControl != null )
        {
            if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST || jobControl.getStatus() == JobControl.TERMINATED_BY_ERROR )
                return simulator.getProfile().getErrorMessage();
        }
        return null;
    }



    @Override
    public Model createModel() throws Exception
    {
        try
        {
            Model model = super.createModel();
            if( model instanceof StochasticModel )
                ( (StochasticModel)model ).setReactionDependencies( reactionDependencies );
            return model;
        }
        finally
        {
            restoreOriginalDiagram();
        }
    }

    @Override
    public void setLargeTemplate()
    {
        //        setGenerateVariableAsArray(ARRAY_MODE_ON);
        //        templatePath = "resources/stochasticLargeModelTemplate.vm";
        //        velocityTemplate = null; //reset template
    }
    @Override
    protected void resetTemplate()
    {
        //        setGenerateVariableAsArray(ARRAY_MODE_OFF);
        //        templatePath = "resources/stochasticModelTemplate.vm";
        //        velocityTemplate = null; //reset template
    }

    @Override
    protected InputStream getTemplateInputStream()
    {
        return StochasticSimulationEngine.class.getResourceAsStream( templatePath );
    }

    @Override
    public void init() throws Exception
    {
        super.init();

        reactionNumber = 0;
        reactionNameIndexMapping = new HashMap<>();
        for( Variable v : executableModel.getVariables() )
        {
            if( isReactionVariable( v ) )
                reactionNameIndexMapping.put( v.getName(), reactionNumber++ );
        }
        fillReactionDependencies();
    }

    @Override
    public void initSimulationResult(SimulationResult simuationResult)
    {
        super.initSimulationResult( simuationResult );
        int varSize = getVarPathIndexMapping() != null ? IntStreamEx.of( getVarPathIndexMapping().values() ).max().orElse( 0 ) + 1 : 0;
        if( result instanceof ParallelStochasticSimulationResult )
        {
            ( (StochasticSimulationResult)result ).setVars( varSize );
            ( (StochasticSimulationResult)result ).setCycles( this.simulationNumber );
            ( (StochasticSimulationResult)result ).setSpanSize( this.getSpan().getLength() );
            for( StochasticSimulationResult innerResult : ( (ParallelStochasticSimulationResult)result ).results )
            {
                initSimulationResult( innerResult );
            }
        }
        if( result instanceof StochasticSimulationResult )
        {
            ( (StochasticSimulationResult)result ).setVars( varSize );
            ( (StochasticSimulationResult)result ).setCycles( this.simulationNumber );
            ( (StochasticSimulationResult)result ).setSpanSize( this.getSpan().getLength() );
        }
    }

    public Map<String, Integer> getReactionNameIndexMapping()
    {
        return reactionNameIndexMapping;
    }
    public int getVariableReactionIndex(String name)
    {
        return reactionNameIndexMapping.containsKey( name ) ? reactionNameIndexMapping.get( name ) : -1;
    }

    public int getReactionNumber()
    {
        return reactionNumber;
    }

    public Node getReactionNode(String name)
    {
        return ( reactionNodes == null || !reactionNodes.containsKey( name ) ) ? null : reactionNodes.get( name );
    }

    public boolean isReactionVariable(Variable v)
    {
        return v.getName().startsWith( "$$" );
    }

    private void fillReactionDependencies()
    {
        reactionNodes = new HashMap<>();

        reactionDependencies = new int[reactionNameIndexMapping.size()][];
        Set<Node> reactions = getReactions( diagram );
        for( Node reactionNode1 : reactions )
        {
            Equation eq = reactionNode1.getRole( Equation.class );
            String var1 = eq.getVariable();
            reactionNodes.put( var1, reactionNode1 );
            Set<Integer> dependentReactions = new HashSet<>();
            for( Node reactionNode2 : reactions )
            {
                if( areDependent( reactionNode1, reactionNode2 ) )
                {
                    eq = reactionNode2.getRole( Equation.class );
                    String var2 = eq.getVariable();
                    dependentReactions.add( reactionNameIndexMapping.get( var2 ) );
                }
            }
            reactionDependencies[reactionNameIndexMapping.get( var1 )] = IntStreamEx.of( dependentReactions ).toArray();
        }
    }

    public boolean isValueTransformed(Variable var)
    {
        if( var instanceof VariableRole )
        {
            DiagramElement de = ( (VariableRole)var ).getDiagramElement();
            if( de.getKernel() instanceof biouml.standard.type.Compartment )
                return false;
            return true;
        }
        return isMolar( var );
    }

    public double getMoleculeScale(Variable var)
    {
        if( isOutputMolecules() )
            return 1.0;
        try
        {
            return StochasticAssignmentPreprocessor.transformToMolar( executableModel, var );
        }
        catch( Exception ex )
        {
            return 1.0;
        }
    }


    public double getMolecules(Variable var)
    {
        //        return var.getInitialValue();
        double multiplier = var.getInitialValue();
        if( var instanceof VariableRole )
        {
            DiagramElement de = ( (VariableRole)var ).getDiagramElement();
            if( de.getKernel() instanceof biouml.standard.type.Compartment )
                return 1.0;
            Compartment comp = de.getCompartment();
            if( Util.isVariable( comp ) && comp.getRole( VariableRole.class ).getInitialValue() != 1.0 )
            {
                if( ( (VariableRole)var ).getInitialQuantityType() == VariableRole.CONCENTRATION_TYPE )
                {
                    multiplier *= comp.getRole( VariableRole.class ).getInitialValue();
                }
            }
        }
        try
        {
            multiplier *= StochasticAssignmentPreprocessor.transformToMolar( executableModel, var );
        }
        catch( Exception ex )
        {
            return -1.0;
        }
        return Math.rint( multiplier );
    }

    public boolean isMolar(Variable var)
    {
        Unit unit = executableModel.getUnits().get( var.getUnits() );
        if( unit == null )
            return false;
        if( unit.getName().equals( "mole" ) )
            return true;
        for( BaseUnit baseUnit : unit.getBaseUnits() )
            if( baseUnit.getType().equals( "mole" ) || baseUnit.getType().equals( "items" ) )
                return true;
        return false;
    }

    /**
     * Returns true if reaction representing by reactioNode1 is dependent from reaction in reactioNode1
     * @param reactionNode1
     * @param reactionNode2
     * @return
     */
    private boolean areDependent(Node reactionNode1, Node reactionNode2)
    {
        try
        {
            String expression = reactionNode2.getRole( Equation.class ).getFormula();
            for( SpecieReference specieReference : ( (Reaction)reactionNode1.getKernel() ).getSpecieReferences() )
            {
                if( expression.contains( specieReference.getSpecieVariable() ) )
                    return true;
            }
            return false;
        }
        catch( Exception ex )
        {
            return false;
        }
    }

    private Set<Node> getReactions(Node de)
    {
        return de.recursiveStream().select( Node.class ).filter( node -> node.getKernel() instanceof Reaction ).toSet();
    }

    @Override
    protected List<PluginEntry> getClassPathEntries()
    {
        List<PluginEntry> result = new ArrayList<>( super.getClassPathEntries() );
        try
        {
            result.add( ApplicationUtils.resolvePluginPath( "biouml.plugins.stochastic:src.jar" ) );
        }
        catch( Exception e )
        {
        }
        return result;
    }

    @Override
    public Map<SimulationEngine.Var, List<Series>> getVariablesToPlot(PlotInfo plot)
    {
        Map<Var, List<Series>> variablesToPlot = new TreeMap<>();

        for( Curve curve : plot.getYVariables() )
        {
            String name = curve.getName();
            String title = curve.getTitle();
            String path = curve.getPath();
            SubDiagram subDiagram = Util.getSubDiagram( originalDiagram, path );
            EModel emodel = ( subDiagram != null ) ? subDiagram.getDiagram().getRole( EModel.class )
                    : originalDiagram.getRole( EModel.class );
            double initialValue = emodel.getVariable( name ).getInitialValue();
            Integer index = getVarPathIndexMapping().get( path.isEmpty() ? name : path + VAR_PATH_DELIMITER + name );
            List<Series> list = new ArrayList<>();
            list.add( new YIntervalSeries( title ) );
            if( !variablesToPlot.keySet().stream().anyMatch( k -> k.name.equals( name ) ) )
                variablesToPlot.put( new Var( name, title, initialValue, index, curve.getPen() ), list );
        }

        return variablesToPlot;
    }

    @Override
    public SimulationResult generateSimulationResult()
    {
        return generateSimulationResult(null, "tmp");
    }

    public SimulationResult generateSimulationResult(DataCollection<?> origin, String name)
    {
        int length = new UniformSpan( getInitialTime(), getCompletionTime(), getTimeIncrement() ).getLength();
        if( threadsRepeats == 1 )
            result = new StochasticSimulationResult( origin, name, this.getSimulationNumber(), length );
        else
            result = new ParallelStochasticSimulationResult( origin, name, threadsRepeats );
        return result;
    }


    @Override
    public int getSimulationType()
    {
        return STOCHASTIC_TYPE;
    }


    @PropertyName ( "Output result in molecules count" )
    public boolean isOutputMolecules()
    {
        return outputMolecules;
    }

    private void resetSeed()
    {
        StochasticSimulator simulator = getStochasticSimulator();
        if( simulator == null )
            return;
        if( isCustomSeed() )
        {
            simulator.setSeed( getSeed() );
        }
        else
        {
            simulator.setSeed( new Date() );
        }
    }

    private StochasticSimulator getStochasticSimulator()
    {
        if( simulator instanceof EventLoopSimulator )
        {
            Simulator solver = ( (EventLoopSimulator)simulator ).getSolver();
            if( solver instanceof StochasticSimulator )
                return (StochasticSimulator)solver;
        }
        else if( simulator instanceof StochasticSimulator )
        {
            return (StochasticSimulator)simulator;
        }
        return null;
    }

    public void setOutputMolecules(boolean outputMolecules)
    {
        this.outputMolecules = outputMolecules;
        this.diagramModified = true;
    }

    @PropertyName ( "Seed" )
    public int getSeed()
    {
        return seed;
    }
    public void setSeed(int seed)
    {
        this.seed = seed;
    }

    @PropertyName ( "Use custom seed" )
    public boolean isCustomSeed()
    {
        return customSeed;
    }
    public void setCustomSeed(boolean customSeed)
    {
        boolean oldValue = this.customSeed;
        this.customSeed = customSeed;
        firePropertyChange( "customSeed", oldValue, customSeed );
        firePropertyChange( "*", null, null );
    }
    public boolean isAutoSeed()
    {
        return !isCustomSeed();
    }

    @PropertyName ( "Output average +- SD" )
    public boolean isAverageRegime()
    {
        return averageRegime;
    }

    public void setAverageRegime(boolean averageRegime)
    {
        this.averageRegime = averageRegime;
    }

    @Override
    public ResultListener generateResultPlot(FunctionJobControl jobControl, PlotInfo plotInfo)
    {
        StochasticResultPlotPane plotPane = new StochasticResultPlotPane( this, jobControl, plotInfo );
        plotPane.setAverageRegime( averageRegime );
        return plotPane;
    }

    @PropertyName ( "Number of parallel threads" )
    public int getThreads()
    {
        return threadsRepeats;
    }
    public void setThreads(int threads)
    {
        this.threadsRepeats = threads;
    }

    private class SimulationTask implements Callable<Void>
    {
        protected Simulator simulator;
        private int repeats;
        private ResultListener[] listeners;
        private double[] initialValues;
        private Model model;

        public SimulationTask(Simulator simulator, Model model, Span span, double[] initialValues, int repeats, ResultListener[] listeners)
        {
            this.simulator = simulator;
            this.repeats = repeats;
            this.initialValues = initialValues;
            this.listeners = listeners;
            this.model = model;
        }

        @Override
        public Void call() throws Exception
        {
            for( ResultListener listener : listeners )
                listener.start( model );

            for( int i = 0; i < repeats; i++ )
            {
                simulator.start( model, span, listeners, jobControl );

                if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                    return null;

                if( i < simulationNumber - 1 )
                {
                    for( ResultListener listener : listeners )
                    {
                        if( listener instanceof CycledResultListener )
                            ( (CycledResultListener)listener ).startCycle();
                    }
                }
                model.setCurrentValues( initialValues );
            }
            return null;
        }
    }

    public String simulateParallel(Model model, ResultListener[] listeners) throws Exception
    {
        if( ! ( model instanceof StochasticModel ) )
            throw new IllegalArgumentException( "Incorrect model class for StochasticSimulationEngine "
                    + ( model == null ? null : model.getClass() ) + ". Only StochasticModel allowed" );

        log.info( "Model " + diagram.getName() + ": simulation started." );

        if( span == null )
        {
            double t0 = getInitialTime();
            double tf = getCompletionTime();
            span = ( getTimeIncrement() != 0.0 ) ? new UniformSpan( t0, tf, getTimeIncrement() ) : new ArraySpan( t0, tf );
        }

        //        List<CycledResultListener> cycledListeners = new ArrayList<>();

        //        if( listeners != null )
        //        {
        //            for( ResultListener listener : listeners )
        //            {
        //                if( listener instanceof CycledResultListener )
        //                    cycledListeners.add( (CycledResultListener)listener );
        //
        //                if( listener instanceof ResultWriter )
        //                    initSimulationResult( ( (ResultWriter)listener ).getResults() );
        //
        //                listener.start( model );
        //            }
        //        }
        ParallelStochasticSimulationResult parallelResult = null;
        if( listeners != null )
        {
            for( ResultListener listener : listeners )
            {
                if( listener instanceof ParallelStochasticSimulationResult )
                    parallelResult = (ParallelStochasticSimulationResult)listener;
            }
        }
        parallelResult.start( model );
        try
        {
            resetSeed();
            if( !model.isInit() )
                model.init();
            double[] initialValues = model.getCurrentValues();

            List<SimulationTask> tasks = new ArrayList<>();
            int cycles = simulationNumber;
            int actualThreads = Math.min( cycles, threadsRepeats );
            int cyclesPerThread = simulationNumber / actualThreads;
            int reminder = cycles - cyclesPerThread * actualThreads;
            for( int i = 0; i < actualThreads - 1; i++ )
            {
                StochasticSimulationResult partialResult = parallelResult.get( i );
                partialResult.setCycles( cyclesPerThread );
                tasks.add( createSimulationTask( model, simulator, span, initialValues, cyclesPerThread,
                        new ResultListener[] {partialResult} ) );
            }
            StochasticSimulationResult partialResult = parallelResult.get( actualThreads - 1 );
            partialResult.setCycles( cyclesPerThread + reminder );
            tasks.add( createSimulationTask( model, simulator, span, initialValues, cyclesPerThread + reminder,
                    new ResultListener[] {partialResult} ) );

            ExecutorService executor = (ExecutorService)Executors.newFixedThreadPool( actualThreads );
            executor.invokeAll( tasks );

            parallelResult.finish();
        }
        catch( Throwable t )
        {
            return "Simulation error: " + t.getMessage();
        }

        log.info( "Model " + diagram.getName() + ": simulation finished." );
        log.info( "" );

        if( jobControl != null )
        {
            if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST || jobControl.getStatus() == JobControl.TERMINATED_BY_ERROR )
                return simulator.getProfile().getErrorMessage();
        }
        return null;
    }

    private SimulationTask createSimulationTask(Model model, Simulator simulator, Span span, double[] initialValues, int cycles,
            ResultListener[] listeners) throws Exception
    {
        Model modelCloned = model.clone();
        Simulator simCloned = simulator.getClass().newInstance();
        if( simulator instanceof EventLoopSimulator )
        {
            Simulator innerSolver = ( (EventLoopSimulator)simulator ).getSolver();
            Simulator innerSolverCloned = innerSolver.getClass().newInstance();
            ( (StochasticSimulator)innerSolverCloned ).setSeed( (int) ( Math.random() * 10000 ) );
            //            ( (StochasticSimulator)innerSolverCloned ).setStochastic( ( (StochasticSimulator)innerSolver ).getStochastic() );
            ( (EventLoopSimulator)simCloned ).setSolver( innerSolverCloned );
        }
        simCloned.setOptions( simulator.getOptions() );
        Span spanCloned = span.clone();
        return new SimulationTask( simCloned, modelCloned, spanCloned, initialValues, cycles, listeners );
    }
}