package biouml.plugins.agentmodeling.covid19;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Span;
import one.util.streamex.StreamEx;

public class InfectiousAgent extends SimulationAgent
{
    //Symptom types
    public static final int SYMPTOMS_CRITICAL = 3;
    public static final int SYMPTOMS_SEVERE = 2;
    public static final int SYMPTOMS_MILD = 1;
    public static final int SYMPTOMS_ASYMPTOMATIC = 0;

    private int reinfected = 0;
    int variant; //update 5 April 2022
    
    //Possible statuses    
    public static final int STATUS_INFECTED = 1;
    public static final int STATUS_ILL = 2;
    public static final int STATUS_RECOVERED = 3;
    public static final int STATUS_HOSPITALIZED = 4;
    public static final int STATUS_ICU = 5;
    public static final int STATUS_DEAD = 6;
    public static final int STATUS_RECOVERED_DETECTED = 7;
    public static final int STATUS_HOME_TREATMENT = 8;
       
    private List<InfectiousAgent> createdAgents = new ArrayList<>(); //Agents infected by this one    
    private InfectiousAgent creator; //Agent which infected this one (can be null)
    
    private Population population; //Population in which agent lives

    public int infectedDay; //day at which agent was infected
    
    //variables
    public double Infection_Day = 0; //days since agent was infected
    public double Detected = 0; //1 means that this agent was detected by health care system as infected, 0 - otherwise
    public double Next_Stage; //time point at which agent will transit to next stage of infection
    public double Infectious;
    public double Others_exposed; //number of persons exposed to current agent (and possibly infected)
    public double Qeued; //if 1 then agent is queued for testing in health care system
    public double Seek_Testing;
    public int Status; //current agent status
    public int Symptoms; //symptom type of given agent
    public int age; 

    //global parameters
    public double illness_period; //period of illness
    public double p_critical; //probability of critical symptoms (depends on age)
    public double p_severe; //probability of severe symptoms (depends on age)
    public double p_mild; //probability of mild symptoms (depends on age)
    private double p_asymptomatic = 0.5; //probability of asymptomatic infection 
    public double Incubation_period; //duration of incubation period
    public double ICU_period; //duration of treatment on ICU
    
    public double symptomatic_mobility = 0.5;

    private RandomGenerator randomGenerator;
    private HealthCare healthCare;
    
    private Person person;

    //dummy agent
    public InfectiousAgent(String name, Span span, Population population, HealthCare healthCare, RandomGenerator randomGenerator)
    {
        super(  name, span );
       
        this.span = span;
        this.healthCare = healthCare;
        this.population = population;
        this.randomGenerator = randomGenerator;
        this.infectedDay = (int)span.getTimeStart();
        
        init();
        if( healthCare != null )
            this.healthCare.registerInfected( this );
    }
    
    /**
     * Agent corresponding to given person in population
     */
    //public InfectiousAgent(Person person, Span span, Population population, HealthCare healthCare, RandomGenerator randomGenerator) //before 5 April 2022
    public InfectiousAgent(Person person, Span span, Population population, HealthCare healthCare, RandomGenerator randomGenerator, int variant) //before 5 April 2022
    {
        super( String.valueOf( person.index ), span );
        
        age = person.age;
        p_severe = person.p_severe;
		p_critical = person.p_critical;
		this.person = person;
		
		if (person.recovered < Integer.MAX_VALUE)
			this.reinfected = 1;
		this.span = span;
        this.healthCare = healthCare;
        this.population = population;
        this.randomGenerator = randomGenerator;
        this.infectedDay = (int)span.getTimeStart();
        
        init();
        if( healthCare != null )
            this.healthCare.registerInfected( this );
    }

    public void setRandomGenerator(RandomGenerator randomGenerator)
    {
        this.randomGenerator = randomGenerator;
    }

    public void setHealthCare(HealthCare healthCare)
    {
        this.healthCare = healthCare;
    }

    public void init()// throws Exception
    {
        Qeued = 0;
        Status = STATUS_INFECTED;
        Incubation_period = Math.min( 14, Math.floor( 1 + 4 * randomGenerator.sampleLogNormal() ) );//MathRoutines.logNormal( 1, 0.5 ) ) );
        ICU_period = 21.0; // initial value of ICU_End        
//        Symptoms = -1; // initial value of Symptoms
        age = 60; // initial value of age
//        avgContacts = 10.0; // initial value of avgContacts
        illness_period = 7.0; // initial value of illness_period
//        p_Hospital_Death_No_Beds = 0.2; // initial value of p_Hospital_Death_No_Beds
//        p_infect = 0.28; // initial value of p_infect
        Next_Stage = Incubation_period;
        Symptoms = Calc_Symptoms( randomGenerator.sampleUniform(), age );
    }

    @Override
    public String[] getVariableNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void iterate()
    {
        //advance time
        Infection_Day += 1;
        currentTime += 1;

        //transit to next stage if neccessary
        if( Infection_Day > Next_Stage )
        {
            switch( Status )
            {
                case STATUS_INFECTED:
                {
                    becomeIll();
                    break;
                }
                case STATUS_ILL:
                {
                    if( Symptoms < SYMPTOMS_SEVERE )
                    {
                        recover();
                    }
                    else if( Symptoms == SYMPTOMS_SEVERE )
                    {
                        hospitalize();
                    }
                    else if( Symptoms == SYMPTOMS_CRITICAL )
                    {
                        toICU();
                    }
                    break;
                }
                case STATUS_HOSPITALIZED:
                {
                    releaseFromHospital();
                    break;
                }
                case STATUS_ICU:
                {
                    releaseFromICU();
                    break;
                }
                case STATUS_HOME_TREATMENT:
                {
                    releaseFromHomeTreatment();
                    break;
                }
            }
        }

        //check if agent is alive        
        checkAlive();

        //perform routine
        routine();
    }

    /*public void checkAlive()
    {
        if( Status == STATUS_DEAD )
        {
            healthCare.registerDeath( this );
            isAlive = false;
        }
        else if( Status == STATUS_RECOVERED || Status == STATUS_RECOVERED_DETECTED )
        {
            healthCare.registerRecovered( this );
//            isAlive = false;
        }
    }*/
    
    public void checkAlive()
    {
        if( Status == STATUS_DEAD )
        {
            healthCare.registerDeath( this );
            isAlive = false;
            this.variant = -1;
        }
        else if( Status == STATUS_RECOVERED || Status == STATUS_RECOVERED_DETECTED )
        {
            healthCare.registerRecovered( this );
            this.variant = -1;
//            isAlive = false;
        }
    }

    protected double calcVariantInfectious (int variant)
    {
    	double a = 0;
    	if (this.variant == 0) a = 1.0;
    	if (this.variant == 1) a = 1.0;
    	if (this.variant == 2) a = 1.5;
    	if (this.variant == 3) a = 3.0;
		return a;
    }
    
    public void routine()
    {
        if( !isAlive )
            return;
        Infectious = healthCare.p_infect * Infectious( Status, Infection_Day - Incubation_period )*calcVariantInfectious(this.variant);
        
        if (Infectious == 0)
            return;

        if( randomGenerator.sampleUniform() < healthCare.p_ignore_lockdown )
        {
            Others_exposed = Calc_Infected_day( Status, Infectious, healthCare.avgContacts, 1, Double.MAX_VALUE, Symptoms, Detected );
        }
        else
        {
            Others_exposed = Calc_Infected_day( Status, Infectious, healthCare.avgContacts, healthCare.mobilityLimit,
                    healthCare.limit_mass_gathering, Symptoms, Detected );
        }
    }

    public void becomeIll()
    {
        Status = STATUS_ILL;
        Next_Stage = Infection_Day + illness_period; //make random
        Seek_Testing = Symptoms > SYMPTOMS_MILD ? 1 : 0; //try another way
    }
    

    public void recover()
	{
		Status = Detected == 1 ? STATUS_RECOVERED_DETECTED : STATUS_RECOVERED;
		this.person.recovered = (int) this.currentTime;
	}
    
    public void treatAtHome()
    {
        Status = STATUS_HOME_TREATMENT;
        Next_Stage = Infection_Day + healthCare.hospitalizationPeriod;
    }

    public void hospitalize()
    {
        double available_beds = healthCare.available_beds;
        if( available_beds <= 0 || randomGenerator.sampleUniform() < (1-healthCare.p_hospitalization))
        {
            treatAtHome();
            return;
        }
        else
        {
            Status = STATUS_HOSPITALIZED;
            healthCare.available_beds = healthCare.available_beds - 1; //TODO: use methods
            Next_Stage = Infection_Day + healthCare.hospitalizationPeriod;
            healthCare.hospitalizedToday++;
        }
        if( Detected == 0.0 )
        {
            Detected = 1.0;
            healthCare.registeredToday++;
        }
    }

    public void toICU()
    {
        double available_ICU = healthCare.available_ICU;
        if( available_ICU == 0 )
        {
            if( randomGenerator.sampleUniform() < healthCare.p_death_critical_home )
                Status = STATUS_DEAD;
            else
                Status = STATUS_RECOVERED;
        }
        else
        {
            Status = STATUS_ICU;
            healthCare.available_ICU = healthCare.available_ICU - 1; //TODO: use methods
            Next_Stage = Infection_Day + ICU_period;
        }
        if( Detected == 0.0 )
        {
            Detected = 1.0;
            healthCare.registeredToday++;
        }
    }

    private void releaseFromHospital()
    {
        healthCare.available_beds = healthCare.available_beds + 1;//TODO: use methods
        if( randomGenerator.sampleUniform() < healthCare.p_death_severe_hospital )
            Status = STATUS_DEAD;
        else
            Status = STATUS_RECOVERED_DETECTED;
    }

    private void releaseFromICU()
    {
        healthCare.available_ICU = healthCare.available_ICU + 1;//TODO: use methods
        if( randomGenerator.sampleUniform() < healthCare.p_death_critical_ICU )
            Status = STATUS_DEAD;
        else
            Status = STATUS_RECOVERED_DETECTED;
    }
    
    private void releaseFromHomeTreatment()
    {
        if( randomGenerator.sampleUniform() < healthCare.p_death_severe_home )
            Status = STATUS_DEAD;
        else
            Status = STATUS_RECOVERED;
    }

    @Override
    public boolean shouldDivide()
    {
        return Infectious != 0;
    }

    public void setVariant(Person person, int variant)// update 7 April of 2022. setter
    {
        this.variant = variant;
    }
    
    @Override
    public InfectiousAgent[] divide() throws Exception
    {

    	int index_0 = 0; //update 8 April 2022 
        int index_1 = 0; //update 8 April 2022
        int index_2 = 0;
    	
        if( population.getInfectedSize() == population.getSize() )
            return null;

        List<Person> infected = population.tryInfectPersons( Infectious, (int)Others_exposed, (int)currentTime, variant); //added variant 
        
        if( infected.isEmpty() )
            return null;
        
        List<InfectiousAgent> result = new ArrayList<InfectiousAgent>();
        for( int i = 0; i < infected.size(); i++ )
        {
            Person person = infected.get( i );
            if( person.immune )
                continue;
            InfectiousAgent copy = createCopy( person ); //update 8 of April 2022
            createdAgents.add( copy );
            result.add( copy );
            if(person.index == 0) index_0 += 1;
            if(person.index == 1) index_1 += 1;
            if(person.index == 2) index_2 += 1;
            //System.out.println("agent "+ i + " Variant " + variant + "\n"); //update 7 of April 2022 (extra)
        }
        
        return StreamEx.of( result ).toArray( InfectiousAgent[]::new );
    }
    
    public InfectiousAgent createCopy(Person person) //UPDATE 8 April 2022. Succesfully (I hope so)
    {
        Span newSpan = span.getRestrictedSpan( currentTime, span.getTimeFinal() );
        //InfectiousAgent agent = new InfectiousAgent(  person, newSpan, population, healthCare, randomGenerator); //before 5 April 2022
        InfectiousAgent agent = new InfectiousAgent(  person, newSpan, population, healthCare, randomGenerator, variant); //after 5 April 2022
        agent.currentTime = currentTime;
        agent.completionTime = completionTime;
        agent.age = person.age;
//        agent.avgContacts = this.avgContacts;
//        agent.p_infect = this.p_infect;
        agent.p_severe = person.p_severe;
        agent.p_critical = person.p_critical;
        agent.variant = person.variant;//UPDATE 8 April 2022. Variant is copied succesfully (I hope so)
        agent.creator = this;
        return agent;
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
    protected double Calc_Infected_day(double Status, double Infectious, double avgContacts, double mobilityFactor, double limit,
            double Symptoms, double Detected)
    {
        if( Infectious == 0 )
            return 0;

        if( Status == STATUS_ILL && Symptoms != SYMPTOMS_ASYMPTOMATIC )
            return Calc_Contacts_day( symptomatic_mobility, avgContacts, mobilityFactor, 5, limit, Detected );

        return Calc_Contacts_day( 1, avgContacts, mobilityFactor, 100, limit, Detected );
    }

    protected double Calc_Contacts_day(double Restriction, double average, double mobility, double max, double limit, double Detected)
    {
        if( Detected == 1 ) //detected are completely isolated
            return 0;
        double contacts = Restriction * randomGenerator.sampleLogNormal() * average * mobility;
        if( contacts > max )
            contacts = max;
        if( contacts > limit )
            contacts = limit;

        return contacts;
    }

    @Override
    public final void setCurrentValues(double[] values) throws Exception
    {
        throw new Exception( "Method is unsupported" );
    }

    @Override
    public double[] getCurrentValues()
    {
        return new double[] {Status, Symptoms, Detected, Seek_Testing, Infectious, Others_exposed, Qeued, Infection_Day, age, reinfected, variant};
    }

    public InfectiousAgent getCreator()
    {
        return creator;
    }

    public List<InfectiousAgent> getCreated()
    {
        return this.createdAgents;
    }

//    protected static double p_ICU_death(double age)
//    {
//        if( age < 20 )
//            return 40;
//        return 50;
//    }

    protected static double Infectious(double Status, double Infectious_Day)
    {
        if( Status == STATUS_RECOVERED || Status == STATUS_DEAD )
            return 0;

        else if( Infectious_Day < -2 )
            return 0;

        else if( Infectious_Day == -2 )
            return 0.12;

        else if( Infectious_Day == -1 )
            return 0.29;

        else if( Infectious_Day == 0 )
            return 0.27;

        else if( Infectious_Day == 1 )
            return 0.07;

        else if( Infectious_Day == 2 )
            return 0.05;

        else if( Infectious_Day == 3 )
            return 0.04;

        else if( Infectious_Day == 4 )
            return 0.03;

        else if( Infectious_Day == 5 )
            return 0.02;

        else if( Infectious_Day == 6 )
            return 0.02;

        else if( Infectious_Day == 7 )
            return 0.01;

        return 0;
    }

    protected int Calc_Symptoms(double v, int age)
    {
        if( v < p_severe * p_critical)
            return SYMPTOMS_CRITICAL;
        else if( v < p_severe )
            return SYMPTOMS_SEVERE;
        else if( v > 1 - p_asymptomatic )
            return SYMPTOMS_MILD;
        return SYMPTOMS_ASYMPTOMATIC;
    }
}