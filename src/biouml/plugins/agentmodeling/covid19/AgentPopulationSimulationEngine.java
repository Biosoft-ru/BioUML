package biouml.plugins.agentmodeling.covid19;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jfree.data.general.Series;
import org.jfree.data.xy.YIntervalSeries;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.Variable;
import biouml.plugins.agentmodeling.Classification;
import biouml.plugins.agentmodeling.ClassificationStatCollector;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.agentmodeling.Stepper;
import biouml.plugins.simulation.CycledResultListener;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class AgentPopulationSimulationEngine extends SimulationEngine
{
    private static final String INITIAL_ICU = "initial_ICU";
    private static final String INITIAL_BEDS = "initial_beds";
    private static final String P_IMMUNE = "p_immune";
    private static final String POPULATION_SIZE = "populationSize";
	private static final String IMMUNITY_PERIOD = "immunityPeriod";

    private JavaSimulationEngine innerEngine;
    
    private ClassificationStatCollector collector = new ClassificationStatCollector();
    private Set<Classification> classifications = new HashSet<Classification>();
    
    private int seed = 0;
    private boolean manualSeed = false;
    private boolean customPopulation = false;
    private boolean externalScenario = false;
    private int repeats = 10;

    private DataElementPath populationData;
    private DataElementPath scenarioTable;
    
    public void setDiagram(Diagram diagram)
    {
        try
        {
            super.setDiagram( diagram );
        }
        catch( Exception ex )
        {
            log.error( "Could not set diagram, error: " + ex.getMessage() );
        }
    }
    
    @Override
    public String getEngineDescription()
    {       
        return null;
    }

    @Override
    public String getVariableCodeName(String varName)
    {
       return null;
    }

    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
       return null;
    }

    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        return innerEngine.getVarIndexMapping();
    }

    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        return innerEngine.getVarPathIndexMapping();
    }
    
    public AgentPopulationSimulationEngine()
    {
        innerEngine = new JavaSimulationEngine();
        simulator = new Stepper();
    }      
    
    @Override
    public AgentPopulationModel createModel() throws Exception
    {

        AgentPopulationModel model = null;
        try
        {
            if( diagram == null )
                throw new Exception( "Diagram is not set" );
            
            int populationSize = (int)diagram.getRole( EModel.class ).getVariable( POPULATION_SIZE ).getInitialValue();
            double p_immune = diagram.getRole( EModel.class ).getVariable( P_IMMUNE ).getInitialValue();
            int available_beds = (int)diagram.getRole( EModel.class ).getVariable( INITIAL_BEDS ).getInitialValue();
            int available_ICU = (int)diagram.getRole( EModel.class ).getVariable( INITIAL_ICU ).getInitialValue();
            model = new AgentPopulationModel( generateStatistics(), populationSize, p_immune, available_beds, available_ICU );
            model.setImmunityPeriod((int)diagram.getRole( EModel.class ).getVariable( IMMUNITY_PERIOD ).getInitialValue());
            model.setControllerAgent( generateControllerAgent( diagram, model ) );
        }
        catch( Exception ex )
        {
            throw new Exception( "Could not create model: " + ex.getMessage() );
        }
        
        if( manualSeed )
            model.setSeed( seed );
 
        model.setSpan( new UniformSpan(0, getCompletionTime(), getTimeIncrement()));
        return model;
    }
    
    private PopulationStatistics generateStatistics()
    {
        PopulationStatistics statistics = new PopulationStatistics();
        try
        {
            if( customPopulation )                
            {
                TableDataCollection table = this.getPopulationData().getDataElement( TableDataCollection.class );
                statistics.setAgeProportion( TableDataCollectionUtils.getColumn( table, "Proportion" ) );
                statistics.ageSevere =TableDataCollectionUtils.getColumn( table, "Severe" );
                statistics.ageCritical = TableDataCollectionUtils.getColumn( table, "Critical" );           
            }
        }
        catch( Exception ex )
        {
            log.error( "Invalid statistics data set: " + ex.getMessage() );
        }
        return statistics;
    }
    
    private SimulationAgent generateControllerAgent(Diagram diagram, AgentPopulationModel model) throws Exception
    {
        this.innerEngine.setDiagram( diagram );
        innerEngine.setInitialTime( this.getInitialTime() );
        innerEngine.setCompletionTime( this.getCompletionTime() );
        innerEngine.setTimeIncrement( this.getTimeIncrement() );
        ModelAgent controllerAgent = new ModelAgent( innerEngine );

        //those are inputs to the model
        for( Variable var : diagram.getRole( EModel.class ).getVariables() )
        {
            controllerAgent.addPort( var.getName(), innerEngine.getVarPathIndexMapping().get( var.getName() ) );
            model.addExternalVariable( var.getName(), var.getName() );
        }        

        //those calculated inside module are outputs
        List<SubDiagram> subDiagrams = Util.getSubDiagrams( diagram );
        for( SubDiagram s : subDiagrams )
        {
            String path = Util.getPath( s );
            for( Variable var : s.getDiagram().getRole( EModel.class ).getVariables() )
            {
                String varPath = path + "/" + var.getName();
                controllerAgent.addPort( varPath, innerEngine.getVarPathIndexMapping().get( varPath ) );
                model.addExternalVariable( var.getName(), varPath );
            }
        }
        return controllerAgent;
    }

    @Override
    public String simulate() throws Exception
    {        
        if (result != null)
        {
            int varSize = IntStreamEx.of( getVarPathIndexMapping().values()).max().orElse( 0 ) + 1;
            ((StochasticSimulationResult)result).setVars(varSize);
            initSimulationResult(result);
        }
        
        AgentPopulationModel model = createModel();
        
        simulator = new Stepper();
        UniformSpan span = new UniformSpan(0, 1, getCompletionTime());
        
        log.info( "Model " + diagram.getName() + ": simulation started." );
        
        List<CycledResultListener> cycledListeners = new ArrayList<>();

        if( listeners != null )
        {
            for( ResultListener listener : listeners )
            {
                listener.start(model);

                if( listener instanceof CycledResultListener )
                    cycledListeners.add((CycledResultListener)listener);
            }
        }
        
        collector = new ClassificationStatCollector();
        collector.setStepUpdate( true );
        ((Stepper)simulator).addStatisticsCollector( collector );
        
        for( int i = 0; i < repeats; i++ )
        {
            simulator.start( model, span, listeners.toArray( new ResultListener[listeners.size()] ), jobControl );

            if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                return null;

            
            if( i < repeats - 1 )
            {
                for( CycledResultListener listener : cycledListeners )
                    listener.startCycle();
            }
        }

        for( CycledResultListener listener : cycledListeners )
            listener.finish();


        log.info( "Simulation finished." );
        return null;
    }
    
    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {        
        if (result != null)
        {
            int varSize = IntStreamEx.of( getVarPathIndexMapping().values()).max().orElse( 0 ) + 1;
            ((StochasticSimulationResult)result).setVars(varSize);
            initSimulationResult(result);
        }

        simulator = new Stepper();
        UniformSpan span = new UniformSpan(0, getCompletionTime(), 1);
        
        log.info( "Model " + diagram.getName() + ": simulation started." );
        List<CycledResultListener> cycledListeners = new ArrayList<>();

        if( resultListeners != null )
        {
            for( ResultListener listener : resultListeners )
            {
                listener.start(model);

                if( listener instanceof CycledResultListener )
                    cycledListeners.add((CycledResultListener)listener);
            }
        }
        
        collector = new ClassificationStatCollector();
        for( Classification c : classifications )
            collector.addClassification( c );
        collector.setStepUpdate( true );
        ((Stepper)simulator).addStatisticsCollector( collector );

        if( !model.isInit() )
            model.init();
        double[] initialValues = model.getCurrentValues();
        for( int i = 0; i < repeats; i++ )
        {
            
            simulator.start( model, span, resultListeners, jobControl );

            if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                return null;

            if( i < repeats - 1 )
            {
                for( CycledResultListener listener : cycledListeners )
                    listener.startCycle();
            }
            model.setCurrentValues( initialValues );
        }

        for( CycledResultListener listener : cycledListeners )
            listener.finish();
        
        log.info( "Simulation finished." );
        
        return null;
    }

    @Override
    public Object getSolver()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSolver(Object solver)
    {
        // TODO Auto-generated method stub
    }

    @PropertyName ( "Random seed" )
    public int getSeed()
    {
        return seed;
    }
    public void setSeed(int seed)
    {
        this.seed = seed;
    }

    @PropertyName ( "Set seed manually" )
    public boolean isManualSeed()
    {
        return manualSeed;
    }
    public void setManualSeed(boolean manualSeed)
    {
        boolean oldValue = this.manualSeed;
        this.manualSeed = manualSeed;
        firePropertyChange( "manualSeed", oldValue, manualSeed );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Set custom population" )
    public boolean isCustomPopulation()
    {
        return customPopulation;
    }
    public void setCustomPopulation(boolean customPopulation)
    {
        boolean oldValue = this.customPopulation;
        this.customPopulation = customPopulation;
        firePropertyChange( "customPopulation", oldValue, customPopulation );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Path to custom population" )
    public DataElementPath getPopulationData()
    {
        return populationData;
    }
    public void setPopulationData(DataElementPath populationData)
    {
        this.populationData = populationData;
    }

    @PropertyName ( "Set external scenario" )
    public boolean isExternalScenario()
    {
        return externalScenario;
    }
    public void setExternalScenario(boolean externalScenario)
    {
        boolean oldValue = this.externalScenario;
        this.externalScenario = externalScenario;
        firePropertyChange( "externalScenario", oldValue, externalScenario );
    }

    @PropertyName ( "Path to external scenario" )
    public DataElementPath getScenarioTable()
    {
        return scenarioTable;
    }
    public void setScenarioTable(DataElementPath scenarioTable)
    {
        this.scenarioTable = scenarioTable;
    }
    
    public boolean isScenarioTableHidden()
    {
        return !isExternalScenario();
    }
    
    public boolean isPopulationTableHidden()
    {
        return !isCustomPopulation();
    }
    
    public boolean isSeedHidden()
    {
        return !isManualSeed();
    }
    
    @PropertyName("Repeats")
    public int getRepeats()
    {
        return repeats;
    }

    public void setRepeats(int repeats)
    {
        this.repeats = repeats;
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
            SubDiagram subDiagram = Util.getSubDiagram(originalDiagram, path);
            EModel emodel = (subDiagram != null)? subDiagram.getDiagram().getRole(EModel.class): originalDiagram.getRole(EModel.class);
            double initialValue = emodel.getVariable(name).getInitialValue();
            Integer index = getVarPathIndexMapping().get(path.isEmpty() ? name : path + VAR_PATH_DELIMITER + name);
            List<Series> list = new ArrayList<>();   
            list.add(new YIntervalSeries(title));
            if( !variablesToPlot.keySet().stream().anyMatch( k -> k.name.equals( name ) ) )
                variablesToPlot.put( new Var( name, title, initialValue, index, curve.getPen() ), list );
        }
        return variablesToPlot;
    }
    
    @Override
    public SimulationResult generateSimulationResult()
    {
        int length = new UniformSpan( getInitialTime(), getCompletionTime(), getTimeIncrement() ).getLength();
        result = new StochasticSimulationResult(null, "tmp", this.getRepeats(), length);
        return result;
    }
    
    @Override
    public int getSimulationType()
    {
        return STOCHASTIC_TYPE;
    }
    
    public void clearClassifications()
    {
        classifications.clear();
    }
    
    public Classification createClassification(String variableName, int index)
    {
        
        Classification classification = new Classification( variableName, index );
        classification.setClass( InfectiousAgent.class );
        classifications.add( classification );        
        return classification;
    }
}
