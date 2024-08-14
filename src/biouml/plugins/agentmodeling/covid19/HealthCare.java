package biouml.plugins.agentmodeling.covid19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Span;

/**
 * Agent representing health care system
 * @author axec
 *
 */
public class HealthCare extends SimulationAgent
{
	public Population population; //update 16 march
    private double[] values;
    public Map<String, Integer> varIndeces = new HashMap<>();
    
    private AgentPopulationModel model;
    private Queue<InfectiousAgent> qeue = new PriorityQueue<>();
  
    private int currentDay;
    
    public int vaccineNumber = 50;
    
    //calculated variables
    public int testsToday = 0;
    public int tests = 0;
    private int inQeue = 0;
    
    private int[] secondaryInfected; //infected by people who was infected at certain day
    private int[] infectedByDay; //infected total by days
    
    //calculated statistics
    public int infected = 0;
    public int infectedToday = 0;
    public int dead = 0;
    public int deadToday = 0; 
    public int deadRegistered = 0; 
    public int deadRegisteredToday = 0; 
    public int recovered = 0;
    public int recoveredRegistered = 0;
    public int recoveredToday = 0;
    public int recoveredRegisteredToday = 0;
    public int infectedFromEndedToday = 0;    
    public int infectedFromRegisteredToday = 0;
    public int registeredToday = 0;
    public int registered = 0;
    public double Rt = 0;
    public double Rt_Registered = 0;
    
    int currentVariant = 0;
    
    public double Rt_Registered_2 = 0;
    
    public double hospitalizedToday = 0;
    public double onICU = 0;
        
    public int available_beds = 0;
    public int available_ICU = 0;
    
    //control variables
    public double mobilityLimit = 0;
    public double limit_mass_gathering = 0;
    public double contactLevels = 3;
    public double testsLimit = 3;
    public double hospitalizationPeriod = 14;
    public double newInfected = 0;
    public double newVaccined = 0; //update 25 march
    public double p_tracing = 1;
    public double avgContacts = 10;
        
    public double p_death_severe_hospital = 0;
    public double p_death_severe_home = 0.2;
    public double p_death_critical_ICU = 0.4;
    public double p_death_critical_home = 1;    
    public double p_ignore_lockdown = 0;
    public double p_infect = 0.3;
    public double p_hospitalization = 1;
    
    public static int TEST_NO_TESTING = 0;
    public static int TEST_ONLY_SEVERE_SYMPTOMS = 1;
    public static int TEST_ALL_WITH_SYMPTOMS = 2;
    public static int TEST_ALL_WITH_SYMPTOMS_CT = 3;
    public static int TEST_ONLY_CRITICAL_SYMPTOMS = 4;
    
    public int testingMode = TEST_ONLY_SEVERE_SYMPTOMS;
    
    public int testingModeContact = TEST_ONLY_SEVERE_SYMPTOMS;

    private RandomGenerator randomGenerator;

    public double getPriority()
    {
        return OBSERVER_AGENT_PRIORITY;
    }

    public HealthCare(AgentPopulationModel model, String name, Span span, RandomGenerator randomGenerator)
    {
        super( name, span );
        this.model = model;
        this.currentDay = 0;
        this.randomGenerator = randomGenerator;
        this.infectedByDay =new int[span.getLength()];
        this.secondaryInfected = new int[span.getLength()];

        initVarInideces();
        //initial
        updateFromFields();
        
    }
    
    @Override
    public void iterate()
    {
        //reset new day
        this.currentTime += 1;// = this.span.getTime( nextSpanIndex );
        currentDay++;
        
        updateFields();

        model.importInfected((int)newInfected, currentVariant); 
        
        
        //perform routine
        performTesting();
        
        //calculate statistics
        dead += deadToday;
        deadRegistered += deadRegisteredToday;
        recovered += recoveredToday;
        recoveredRegistered += recoveredRegisteredToday; 
        registered += registeredToday;
        tests += testsToday;
        infected += infectedToday;
        
//        System.out.println( currentTime +" Recovered registered " +this.recoveredRegistered );
//        System.out.println( currentTime +" Available ICU " +this.available_ICU );
        
        
        
        
        double endedToday = (deadToday + recoveredToday);
        double registeredEndedToday = (deadRegisteredToday + recoveredRegisteredToday);
        Rt = endedToday == 0? 0: infectedFromEndedToday / endedToday;
        Rt_Registered = registeredEndedToday == 0? 0: infectedFromRegisteredToday / registeredEndedToday;
        
        //List<Person> persons1 = population.tryInfectPersons(p_infect, testsToday , currentDay); //remove
        population.tryVaccinePersons((int)newVaccined , currentDay);
        //instead of curDay write currentDay
        
        inQeue = qeue.size();
        qeue.clear();

        //update values array
        updateFromFields();

        //debug
//        System.out.println( currentTime + " Tests today: "+ testsToday+" Registered today: "+ registered+ " Registered total: "+ registered );
//        System.out.println( currentTime + " Rt: "+ Rt );
    }

    private void performTesting()
    {
        try
        {
            for( SimulationAgent agent : model.getAgents() )
            {
                if( ! ( agent instanceof InfectiousAgent ) )
                    continue;
                InfectiousAgent infectiousAgent = (InfectiousAgent)agent;
                if( agent.isAlive() && infectiousAgent.Seek_Testing > 0 && infectiousAgent.Detected == 0 && infectiousAgent.Qeued == 0 )
                {
                    if( shouldBeTested( infectiousAgent, testingMode ) )
                        qeueForTesting( infectiousAgent );
                }
            }
            runTests();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    /**
     * Add agent in qeue for testing
     */
    private void qeueForTesting(InfectiousAgent agent) throws Exception
    {
        agent.Qeued = 1.0;
        qeue.add( (InfectiousAgent)agent );
    }

    /**
     * Check if this agent who seeks testing should be tested
     */
    private boolean shouldBeTested(InfectiousAgent agent, int testingMode) throws Exception
    {
        if( testingMode == TEST_ALL_WITH_SYMPTOMS || testingMode == TEST_ALL_WITH_SYMPTOMS_CT )
            return true;
        else if( testingMode == TEST_ONLY_SEVERE_SYMPTOMS )
            return (int)agent.Symptoms >= InfectiousAgent.SYMPTOMS_SEVERE;
        else if( testingMode == TEST_ONLY_CRITICAL_SYMPTOMS )
            return (int)agent.Symptoms >= InfectiousAgent.SYMPTOMS_CRITICAL;
        else
            return randomGenerator.sampleUniform() < 0.02; //some people gets testing anyway
    }

    private void performContactTracing(InfectiousAgent agent) throws Exception
    {
        //find who infected this agent and who was infected by him
        //TODO: introduce some type of error here
        Set<InfectiousAgent> contacts = new HashSet<>();
        contacts.addAll( agent.getCreated() );
        if( agent.getCreator() != null )
            contacts.add( agent.getCreator() );

        //trace three levels
        for( int i = 0; i < contactLevels; i++ )
        {
            Set<InfectiousAgent> nextContacts = new HashSet<>();
            for( InfectiousAgent contact : contacts )
            {
                if( !contact.isAlive() || contact.Detected != 0.0 || contact.Qeued == 1.0 )
                    continue;

                if (randomGenerator.sampleUniform() >= p_tracing)
                    continue;

                if( !shouldBeTested( contact, testingModeContact ) )
                    continue;

                //add probability
                qeueForTesting( contact );

                nextContacts.addAll( contact.getCreated() );
                if( contact.getCreator() != null )
                    nextContacts.add( contact.getCreator() );
            }
            contacts = nextContacts;
        }
    }

    private void runTests() throws Exception
    {
//        testTodayList = new ArrayList<>();
        List<InfectiousAgent> detected = new ArrayList<>();
        int todayLimit = (int)testsLimit;// + (int)((1 - 2*Math.random())*(testsLimit/10.0));
        while( !qeue.isEmpty() && testsToday < todayLimit )
        {
            InfectiousAgent agent = qeue.poll();
            boolean isDetected = detect( agent );
            if( isDetected )
            {
                detected.add( agent );
                //                    
                //                    if( testingMode == TEST_ALL_WITH_SYMPTOMS_CT )
                //                    {
                //                        for( MortalProducerAgent detectedAgent : detected )
                //                            performContactTracing( detectedAgent );
                //                    }
            }
        }

        if( this.contactLevels > 0 )
        {
            for( InfectiousAgent agent : detected )
                performContactTracing( agent );


            while( !qeue.isEmpty() && testsToday < todayLimit )
            {
                InfectiousAgent agent = qeue.poll();
                boolean isDetected = detect( agent );
                if( isDetected )                
                    performContactTracing( agent );               
            }
        }
        detected.clear();
    }

    private boolean detect(InfectiousAgent agent) throws Exception
    {
        testsToday++;
        boolean isDetected = true;//agent.Symptoms >= 1;//agent.Infectious > 0;
        if( isDetected )
        {
            agent.Detected = 1.0;
            agent.Seek_Testing = 0.0;
            registeredToday++;
        }
        //        testTodayList.add( agent.getName() + " [" + ( isDetected ? "+" : "-" ) + "]" );
        return isDetected;
    }
    
    @Override
    public void addVariable(String name) throws Exception
    {
        int i = varIndeces.size();
        varIndeces.put( name, i );        
    }
    
    public String[] getInputNames()
    {
        return new String[] {"testsLimit", "mobilityLimit", "limit_mass_gathering", "testingMode", "hospitalizationPeriod",
                "newInfected", "contactLevels", "p_tracing", "testingModeContact", "avgContacts", "p_death_severe_hospital",
                "p_death_severe_home", "p_death_critical_ICU", "p_death_critical_home", "p_ignore_lockdown", "p_infect",
                "p_hospitalization", "newVaccined", "currentVariant"}; //update 25 March
    }

    public String[] getOutputNames()
    {
        return new String[] {"testsToday", "tests", "registeredToday", "registered", "Rt", "Rt_Registered", "inQeue", "dead", "deadToday",
                "recovered", "recoveredToday", "deadRegistered", "deadRegisteredToday", "recoveredRegistered", "recoveredRegisteredToday",
                "infected", "infectedToday", "hospitalizedToday", "available_beds", "available_ICU"};
    }

    private void initVarInideces()
    {

        int index = 0;
        for( String input : getInputNames() )
            varIndeces.put( input, index++ );

        //outputs
        for( String input : getOutputNames() )
            varIndeces.put( input, index++ );
    }

    public void setInput(String name, double value)
    {
        int index = varIndeces.get( name );
        values[index] = value;
    }
    
    public double getOutput(String name)
    {
        int index = varIndeces.get( name );
        return values[index];
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        return new double[] {testsToday, tests, registeredToday, registered, Rt, Rt_Registered, inQeue, available_beds, available_ICU, dead,
                deadToday, recovered, recoveredToday, deadRegistered, deadRegisteredToday, recoveredRegistered, recoveredRegisteredToday,
                infected, infectedToday, hospitalizedToday};
    }

    @Override
    public String[] getVariableNames()
    {
        return new String[] {"tests today", "tests total", "registered today", "registered", "Rt", "Rt Registered", "inQeue",
                "available beds", "available ICU", "dead", "dead today", "recovered", "recovered today", "dead registered",
                "dead registered today", "recovered registered", "recovered registered today", "infected", "infected today",
                "hospitalized today"};
    }

	private void updateFromFields() {
		values = new double[] { testsLimit, mobilityLimit, limit_mass_gathering, testingMode, hospitalizationPeriod,
				newInfected, contactLevels, p_tracing, testingModeContact, avgContacts, p_death_severe_hospital,
				p_death_severe_home, p_death_critical_ICU, p_death_critical_home, p_ignore_lockdown, p_infect,
				p_hospitalization, newVaccined, currentVariant, /*currentVariant*/ /* <- update 25 march /* NEXT ARE OUTPUTS */ testsToday, tests,
				registeredToday, registered, Rt, Rt_Registered, inQeue, dead, deadToday, recovered, recoveredToday,
				deadRegistered, deadRegisteredToday, recoveredRegistered, recoveredRegisteredToday, infected,
				infectedToday, hospitalizedToday, available_beds, available_ICU };
	}

    public void setPopulation(Population population)// update 16 march. setter
    {
        this.population = population;
    }
    
    /*public Population (int size)
    {
        this.size = size;
        this.alreadyCreated = new HashMap<>();
    }*/  // taken from Population.java

    private void updateFields()
    {
        testsLimit = values[0];
        mobilityLimit = values[1];
        limit_mass_gathering = values[2]; 
        testingMode = (int)values[3];
        hospitalizationPeriod =values[4];
        newInfected = values[5];
        contactLevels = values[6];
        p_tracing = values[7];
        testingModeContact = (int)values[8];
        avgContacts = values[9];
        p_death_severe_hospital = values[10];
        p_death_severe_home = values[11];
        p_death_critical_ICU = values[12];
        p_death_critical_home = values[13];
        p_ignore_lockdown = values[14];
        p_infect = values[15];
        p_hospitalization = values[16];
        newVaccined = values[17]; //update 25 march 2022
        currentVariant = (int)values[18]; //UPDATE 8 APRIL 2022
    }

    @Override
    public double[] getUpdatedValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setUpdated()
    {
        // TODO Auto-generated method stub
    }

    public void registerDeath(InfectiousAgent agent)
    {
        if( agent.Detected == 1.0 )
        {
            this.deadRegisteredToday++;
            this.infectedFromRegisteredToday += agent.getCreated().size();
        }
        this.deadToday++;
        this.secondaryInfected[agent.infectedDay] += agent.getCreated().size();
        this.infectedFromEndedToday += agent.getCreated().size();
    }

    public void registerRecovered(InfectiousAgent agent)
    {
        if( agent.Detected == 1.0 )
        {
            this.recoveredRegisteredToday++;
            this.infectedFromRegisteredToday += agent.getCreated().size();
        }
        this.recoveredToday++;
        this.secondaryInfected[agent.infectedDay] += agent.getCreated().size();
        this.infectedFromEndedToday += agent.getCreated().size();
    }
    
    public void registerInfected(InfectiousAgent agent)
    {
        this.infectedToday++;
        this.infectedByDay[(int)this.currentDay]++;
    }
    
    public void resetStatistics()
    {
        recoveredToday = 0;
        recoveredRegisteredToday = 0;
        deadToday = 0;
        deadRegisteredToday = 0;
        registeredToday = 0;
        testsToday = 0;
        infectedFromRegisteredToday = 0;
        infectedFromEndedToday = 0;
        infectedToday = 0;
        hospitalizedToday = 0;
    }
    
    public double[] calcRt()
    {
        double[] Rt = new double[this.infectedByDay.length];
        for (int i=0; i<infectedByDay.length; i++)
            Rt[i] = this.secondaryInfected[i] / infectedByDay[i];
        return Rt;
    }
}