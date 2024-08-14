package biouml.plugins.agentmodeling.covid19;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentStatCollector2;
import biouml.plugins.agentmodeling.Classification;
import biouml.plugins.agentmodeling.ClassificationStatCollector;
import biouml.plugins.agentmodeling.StatCollector;
import biouml.plugins.agentmodeling.Stepper;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * Spike solution agent-based model for covid-19 spread simulation  
 * @author axec
 */
public class AgentPopulationModel_OLD
{
    private double timeIncrement = 1;
    private double completionTime = 100;
    private AgentBasedModel agentmodel;    
    Map<String, AgentStatCollector2> globalCollectors = new HashMap<>();
    private ClassificationStatCollector collector;
    private Population population;    
    private InfectiousAgent prototypeAgent;
    private Stepper stepper;
    private int initialInfected = 1;
    
    RandomGenerator randomGenerator;

    public AgentPopulationModel_OLD(int size, double completionTime) throws Exception
    {
        randomGenerator = new RandomGenerator();
        population = new Population( size );
        population.setRandomGenerator( randomGenerator );
        collector = new ClassificationStatCollector();
        collector.setStepUpdate( true );
        agentmodel = new AgentBasedModel();
        Span span = new UniformSpan( 0, completionTime, timeIncrement );
        prototypeAgent = new InfectiousAgent( "Person", span.clone(), population, null, randomGenerator );
        stepper = new Stepper();
    }
    
    public void setSeed(long seed)
    {
        this.randomGenerator = new RandomGenerator(seed);
        this.population.setRandomGenerator( randomGenerator );
        this.prototypeAgent.setRandomGenerator( randomGenerator );     
    }
    
    public void setPopulationStatistics(TableDataCollection table)
    {
//        population.setStatistics( table );
    }
    
    public void setInitialInfected(int initialInfected)
    {
        this.initialInfected = initialInfected;
    }
    
    public AgentPopulationModel_OLD(TableDataCollection statistics, double p_immunity, int size) throws Exception
    {
        randomGenerator = new RandomGenerator();
//        this.population = new Population( size, p_immunity, statistics, randomGenerator );
        collector = new ClassificationStatCollector();
        collector.setStepUpdate( true );
        agentmodel = new AgentBasedModel();
        Span span = new UniformSpan( 0, completionTime, timeIncrement );
        prototypeAgent = new InfectiousAgent( "Person", span.clone(), population, null, randomGenerator );
        stepper = new Stepper();
    }

    
    public AgentPopulationModel_OLD(long seed, TableDataCollection statistics, double p_immunity, int size) throws Exception
    {
        randomGenerator = new RandomGenerator(seed);
//        this.population = new Population( size, p_immunity, statistics, randomGenerator );
        collector = new ClassificationStatCollector();
        collector.setStepUpdate( true );
        agentmodel = new AgentBasedModel();
        Span span = new UniformSpan( 0, completionTime, timeIncrement );
        prototypeAgent = new InfectiousAgent( "Person", span.clone(), population, null, randomGenerator );
        stepper = new Stepper();
    }

    public void addPlot(String agentName, String plotName, String[] variableNames) throws Exception
    {
        globalCollectors.get( agentName ).createPlot( plotName, variableNames );
    }

    public HealthCare addHealthCare() throws Exception
    {
        return null;
//        HealthCare healthCareAgent = new HealthCare( agentmodel, "HealthCare", new UniformSpan( 0, completionTime, timeIncrement ), randomGenerator );
//        stepper.addObserverAgent( healthCareAgent );
//        globalCollectors.put( "HealthCare", new AgentStatCollector2( healthCareAgent ) );
//        prototypeAgent.setHealthCare( healthCareAgent );
//        return healthCareAgent;
    }

    public void simulate() throws Exception
    {
        double time = System.nanoTime();

        population.generateInitial( initialInfected );
        //create initial population
        for( Entry<Integer, Person> infectedPerson : population.alreadyCreated.entrySet() )
            agentmodel.addAgent( getPrototypeAgent().createCopy( infectedPerson.getValue() ) );
        
        stepper.addStatisticsCollector( collector );
        for( StatCollector collector : globalCollectors.values() )
            stepper.addStatisticsCollector( collector );
        stepper.start( agentmodel, new UniformSpan( 0, completionTime, timeIncrement ), null, null );
        System.out.println( "Simulation finished: " + ( System.nanoTime() - time ) / 1E9 + " seconds" );
    }

    public Classification createClassification(String variableName, int index) throws Exception
    {
        Classification classification = new Classification( variableName, index );
        classification.setClass( InfectiousAgent.class );
        collector.addClassification( classification );
        return classification;
    }

    public void setCompletionTime(double t)
    {
        this.completionTime = t;
    }

    public double[] getGlobalDynamic(String agentName, String value)
    {
        return DoubleStreamEx.of( globalCollectors.get( agentName ).getDynamic( value ) ).toArray();
    }

    public double[] getDynamic(String value)
    {
        return IntStreamEx.of( collector.getDynamic( value ) ).asDoubleStream().toArray();
    }

    public double[] getTimes()
    {
        return collector.getTimes();
    }
//
//    public Scenario loadScenario(HealthCare healthCare, TableDataCollection table)
//    {
//        Scenario scenario = new Scenario();
//        scenario.times = TableDataCollectionUtils.getColumn( table, "Time" );
//        scenario.mobilityLimit = TableDataCollectionUtils.getColumn( table, "Mobility limit" );
//        scenario.newBeds = TableDataCollectionUtils.getColumn( table, "New beds" );
//        scenario.newICU = TableDataCollectionUtils.getColumn( table, "New ICU" );
//        scenario.testMode = TableDataCollectionUtils.getColumn( table, "Testing Mode" );
//        healthCare.setScenario( scenario );
//        return scenario;
//    }

    public InfectiousAgent getPrototypeAgent()
    {
        return prototypeAgent;
    }

    public void setPrototypeAgent(InfectiousAgent prototypeAgent)
    {
        this.prototypeAgent = prototypeAgent;
    }
}