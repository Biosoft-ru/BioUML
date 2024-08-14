package biouml.plugins.agentmodeling.covid19.old;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.BasicStatCollector;
import biouml.plugins.agentmodeling.Classification;
import biouml.plugins.agentmodeling.ClassificationStatCollector;
import biouml.plugins.agentmodeling.Scheduler;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.DiscreteSimulator;
import biouml.plugins.simulation.ode.EulerSimple;
import biouml.standard.simulation.ResultListener;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * Spike solution agent-based model for covid-19 spread simulation  
 * @author axec
 */
public class AgentPopulationModelOld
{
    private double timeIncrement = 1;
    private double completionTime = 100;
    private AgentBasedModel agentmodel;
    private ClassificationStatCollector collector;
    private BasicStatCollector collector2;
    private Map<String, Integer> varToIndex;
    private Population population;
    Map<String, AgentStatCollector> globalCollectors = new HashMap<>();
    MortalProducerAgent prototypeAgent;

    private int contactLevels = 3;
    
    public Classification createClassification(String variableName) throws Exception
    {
        int index = varToIndex.get( variableName );
        Classification classification = new Classification( variableName, index );
        classification.setClass( MortalProducerAgent.class );
        collector.addClassification( classification );
        return classification;
    }

    public void setTimeIncrement(double t)
    {
        this.timeIncrement = t;
    }

    public void setCompletionTime(double t)
    {
        this.completionTime = t;
    }

    public void addGlobalAgentSource(Diagram diagram, String sourceVar) throws Exception
    {
        SimulationEngine engine = new JavaSimulationEngine();
        engine.setSolver( new EulerSimple() );
        engine.setCompletionTime( completionTime );
        engine.setTimeIncrement( timeIncrement );
        engine.setDiagram( diagram );

        AgentSource source = new AgentSource( population, engine );
        source.setPrototypeAgent( prototypeAgent );
        source.setNumberProduceName( sourceVar );
        agentmodel.addAgent( source );
    }

    public void addVariable(HealthCare agent, String name, double value) throws Exception
    {
        agent.addGlobalVariable( name, value, false );
        if( globalCollectors.containsKey( agent.getName() ) )
            globalCollectors.get( agent.getName() ).addVariable( name );
    }
    
    public void addSharedVariable(HealthCare agent, String name, double value) throws Exception
    {
        agent.addGlobalVariable( name, value, true );
        prototypeAgent.addPort( name, varToIndex.get( name ) );
        if( globalCollectors.containsKey( agent.getName() ) )
            globalCollectors.get( agent.getName() ).addVariable( name );
    }

    public AgentPopulationModelOld(Diagram diagram, TableDataCollection statistics, double p_immunity,  boolean discrete, int size) throws Exception
    {
        this.population = new Population( size, p_immunity, statistics );
        collector = new ClassificationStatCollector();
        collector2 = new BasicStatCollector();
        collector2.setShowPlot( true );

        agentmodel = new AgentBasedModel();
        SimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram( diagram );
        Model model = engine.createModel();
        varToIndex = engine.getVarIndexMapping();
        int deathIndex = varToIndex.get( "Death" );

        Span span = new UniformSpan( 0, completionTime, timeIncrement );

        EventLoopSimulator simulator = new EventLoopSimulator();
        simulator.setSolver( discrete ? new DiscreteSimulator() : new EulerSimple() );
        prototypeAgent = new MortalProducerAgent( population, model.getClass().newInstance(), deathIndex, simulator, span.clone(), "Person",
                new ResultListener[0] );

        prototypeAgent.addPort( "limit_mass_gathering", varToIndex.get( "limit_mass_gathering" ) );
        prototypeAgent.addPort( "mobility_limit", varToIndex.get( "mobility_limit" ) );
        prototypeAgent.addPort( "age", varToIndex.get( "age" ) );
        prototypeAgent.addPort( "Others_exposed", varToIndex.get( "Others_exposed" ) );
        prototypeAgent.addPort( "Seek_Testing", varToIndex.get( "Seek_Testing" ) );
        prototypeAgent.addPort( "Infectious", varToIndex.get( "Infectious" ) );
        prototypeAgent.addPort( "Detected", varToIndex.get( "Detected" ) );
        prototypeAgent.addPort( "Symptoms", varToIndex.get( "Symptoms" ) );
        prototypeAgent.addPort( "Qeued", varToIndex.get( "Qeued" ) );

        prototypeAgent.setChanceProduceName( "Infectious" );
        prototypeAgent.setNumberProduceName( "Others_exposed" );
    }

    public HealthCare addHealthCare() throws Exception
    {
        HealthCare healthCareAgent = new HealthCare( agentmodel, "HealthCare", prototypeAgent.getSpan().clone() );
        agentmodel.addAgent( healthCareAgent );
        prototypeAgent.addGlobalContext( healthCareAgent, healthCareAgent.getSharedVariables() );
        for( String variable : healthCareAgent.getSharedVariables() )
            prototypeAgent.addPort( variable, varToIndex.get( variable ) );
        globalCollectors.put( "HealthCare", new AgentStatCollector( healthCareAgent, healthCareAgent.getVariableNames() ) );
        return healthCareAgent;
    }

    public void simulate(int initialSize) throws Exception
    {
        double time = System.nanoTime();

        population.generateInitial( initialSize );
        //create initial population
        for( Entry<Integer, Person> infectedPerson : population.alreadyCreated.entrySet() )
            agentmodel.addAgent( prototypeAgent.createCopy( infectedPerson.getValue() ) );

        Scheduler scheduler = new Scheduler();
        scheduler.setSaveResult( false );
        scheduler.addStatisticsCollector( collector );
        
        for( AgentStatCollector collector : globalCollectors.values() )
            scheduler.addStatisticsCollector( collector );
        scheduler.start( agentmodel, new UniformSpan( 0, completionTime, timeIncrement ), null, null );
        System.out.println( "Simulation finished: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );
    }

    public double[] getGlobalDynamic(String agentName, String value)
    {
        return DoubleStreamEx.of( globalCollectors.get( agentName ).getDynamic( value ) ).toArray();
    }

    public double[] getDynamicDouble(String value)
    {
        return IntStreamEx.of( collector.getDynamic( value ) ).asDoubleStream().toArray();
    }

    public double[] getTimes()
    {
        return collector.getTimes();
    }

    public double[] getSizeDynamicFullDouble()
    {
        return Arrays.copyOf( IntStreamEx.of( collector.getSizeDynamic() ).asDoubleStream().toArray(),
                (int) ( completionTime / timeIncrement ) );
    }

    public double[] getTimesFull()
    {
        return Arrays.copyOf( collector.getTimes(), (int) ( completionTime / timeIncrement ) );
    }

    public void setParameterValue(String name, double value)
    {
        int index = this.varToIndex.get( name );
    }

    public static double[] fillAgeProportion(TableDataCollection table)
    {
        double[] proportions = TableDataCollectionUtils.getColumn( table, "Proportion" );
        double[] ageProportion = new double[proportions.length];
        ageProportion[0] = proportions[0] / 100;
        for( int i = 1; i < proportions.length - 1; i++ )
            ageProportion[i] = proportions[i] / 100 + ageProportion[i - 1];
        ageProportion[proportions.length - 1] = 1;
        return ageProportion;
    }

    public static double[] fillAgeSeverity(TableDataCollection table)
    {
        return TableDataCollectionUtils.getColumn( table, "Severe" );
    }

    public static double[] fillAgeCritical(TableDataCollection table)
    {
        return TableDataCollectionUtils.getColumn( table, "Critical" );
    }

    public Scenario loadScenario(HealthCare healthCare, TableDataCollection table)
    {
        Scenario scenario = new Scenario();        
        scenario.times = TableDataCollectionUtils.getColumn( table, "Time" );
        scenario.mobilityLimit = TableDataCollectionUtils.getColumn( table, "Mobility limit" );
        scenario.newBeds = TableDataCollectionUtils.getColumn( table, "New beds" );
        scenario.newICU = TableDataCollectionUtils.getColumn( table, "New ICU" );
        scenario.testMode = TableDataCollectionUtils.getColumn( table, "Testing Mode" );
        healthCare.setScenario( scenario );
        return scenario;
    }

}