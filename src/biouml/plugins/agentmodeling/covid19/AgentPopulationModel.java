package biouml.plugins.agentmodeling.covid19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.Classification;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Span;

/**
 * Spike solution agent-based model for covid-19 spread simulation  
 * @author axec
 */
public class AgentPopulationModel extends AgentBasedModel
{    
    private PopulationStatistics statistics;
    private Span span; 
    private Population population;   
    private InfectiousAgent prototypeAgent; //prototype object for all infectious agents in population
    private HealthCare healthCareAgent; //agent representing healthcare system
    private SimulationAgent controllerAgent; //is defined by simulatable diagram. Introduces into the model events and parameters set by user
    private RandomGenerator randomGenerator;
    private long seed; //custom seed for random generator
    private boolean useSeed = false; //if true then custom seed is used
    private Map<String, String> externalVariables = new HashMap<>();
    
    //initial global parameters
    private int populationSize;
    private int immunityPeriod; //period of immunity
    private double p_immune;  //percentage of individuals in population with preexisting immunity     
    private int initial_beds; //inital number of free beds in hospital
    private int initial_ICU; //initial number of free ICU in hospital
    private double time; //model time
    private int vaccineImmunityPeriod = 90;
    
    

    public void addExternalVariable(String insideName, String externalName)
    {
        externalVariables.put( insideName, externalName );
    }

    /**
     * Parameters of the model
     */
    public String[] getInputNames()
    {
        return  healthCareAgent.getInputNames();
    }

    /**
     * Observed variables
     */
    public String[] getOutputNames()
    {
        return healthCareAgent.getOutputNames();
    }
    
    public void setControllerAgent(SimulationAgent agent)
    {
        this.controllerAgent = agent;
    }

    public SimulationAgent getControllerAgent()
    {
        return this.controllerAgent;
    }
    
    
    public AgentPopulationModel(PopulationStatistics statistics, int populationSize, double p_immunity, int initial_beds, int initial_ICU)
    {     
        this.useSeed = false;
        this.populationSize = populationSize;
        this.p_immune = p_immunity;
        this.initial_beds = initial_beds;
        this.initial_ICU = initial_ICU;
        this.statistics = statistics;
    }
    
    public void setImmunityPeriod(int immunityPeriod)
    {
    	this.immunityPeriod = immunityPeriod;
    }
    
    public void setVaccineImmunityPeriod(int immunityPeriod)
    {
        this.vaccineImmunityPeriod = immunityPeriod;
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        return controllerAgent.getCurrentValues();
    }
    
    /**
     * Import infected persons into the population
     */
    public void importInfected(int arrived, int variant)
    {
        List<Person> persons = population.ImportPersons( (int)arrived , variant);
        for (Person person: persons)
        {
            InfectiousAgent agent = prototypeAgent.createCopy( person ); //Update 8 April 2022. Variant added
            agents.add( agent );
        }
    }
    
    public void initNewStep() throws Exception
    {
        healthCareAgent.resetStatistics();
        updateFromControl();
    }
    
    /**
     * Update model from user diagram, all external variables are updated
     */    
    public void updateFromControl() throws Exception
    {
        for( String name : getInputNames() )
        {
            try
            {
                if( ! ( externalVariables.containsKey( name ) ) )
                    continue;
                String externalName = this.externalVariables.get( name );
                healthCareAgent.setInput( name, controllerAgent.getCurrentValue( externalName ) );
            }
            catch( Exception ex )
            {
                //                Log.info( "Missing parameter in agent population diagram: " + name );
            }

        }
    }

    /**
     * Update user diagram from the model after simulation step 
     */
    public void updateToControl() throws Exception
    {
        for( String name : getOutputNames() )
        {
            if( ! ( externalVariables.containsKey( name ) ) )
                continue;
            String externalName = this.externalVariables.get( name );
            controllerAgent.setCurrentValue( externalName, healthCareAgent.getOutput( name ) );
        }               
    }

    public void updateValues(double time) throws Exception
    {
        updateToControl();        
    }

    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        controllerAgent.setCurrentValues( values );
        this.updateFromControl();

        if( time == 0 )
        {
            try
            {
                controllerAgent.init();
                this.initial_beds = (int)controllerAgent.getCurrentValue( "initial_beds" );
                this.initial_ICU = (int)controllerAgent.getCurrentValue( "initial_ICU" );
                this.populationSize = (int)controllerAgent.getCurrentValue( "populationSize" );
                this.immunityPeriod = (int)controllerAgent.getCurrentValue( "immunityPeriod" );
                this.p_immune = controllerAgent.getCurrentValue( "p_immune" );
                
                randomGenerator = useSeed? new RandomGenerator(seed): new RandomGenerator();
                population = new Population(this.populationSize, this.p_immune, statistics, randomGenerator );    
                population.setImmunityPeriod(immunityPeriod);
                prototypeAgent = new InfectiousAgent( "Person", span.clone(), population, null, randomGenerator );
                healthCareAgent = new HealthCare( this, "HealthCare", span.clone(), randomGenerator );
                healthCareAgent.setPopulation(population);
                healthCareAgent.available_beds = this.initial_beds;
                healthCareAgent.available_ICU = this.initial_ICU;
                prototypeAgent.setHealthCare( healthCareAgent );
            }
            catch( Exception ex )
            {

            }
        }
    }

    @Override
    public void init() throws Exception
    {
        if (controllerAgent instanceof ModelAgent)            
        ((ModelAgent)controllerAgent).getModel().init();
        controllerAgent.init();
        
        randomGenerator = useSeed? new RandomGenerator(seed): new RandomGenerator();         
        population = new Population(this.populationSize, this.p_immune, statistics, randomGenerator );
        population.setImmunityPeriod(immunityPeriod);
        prototypeAgent = new InfectiousAgent( "Person", span.clone(), population, null, randomGenerator );
        healthCareAgent = new HealthCare( this, "HealthCare", span.clone(), randomGenerator );
        
        healthCareAgent.setPopulation(population); 
        
        healthCareAgent.available_beds = this.initial_beds;
        healthCareAgent.available_ICU = this.initial_ICU;
        prototypeAgent.setHealthCare( healthCareAgent );
        this.time = 0;
        isInit = true;
    }

    public void setSpan(Span span)
    {
        this.span = span;
    }
    
    public void setSeed(long seed)
    {
        this.seed = seed;
        this.useSeed = true;
    }
    
//    public void setPopulationStatistics(TableDataCollection table)
//    {
//        this.population.setStatistics( table );
//    }

    public void init(double[] initialValues, Map<String, Double> parameters)
    {
        super.init(initialValues, parameters);
    }
          
    /**Returns current values of all model variables and parameters without any additional calculations. Method must be overridden by subclasses */
    public double[] getCurrentState() throws Exception
    {
        return getCurrentValues();
    }
    
    public AgentPopulationModel clone()
    {
        AgentPopulationModel result = new AgentPopulationModel(this.statistics, populationSize, p_immune, initial_beds, initial_ICU);
        result.setSpan( span );       
        return result;
    }    

    public List<SimulationAgent> getObserverAgents()
    {
        List<SimulationAgent> result = new ArrayList<>();
        result.add( this.healthCareAgent );
        return result;
    }
}