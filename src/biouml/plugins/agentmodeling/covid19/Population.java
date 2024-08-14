package biouml.plugins.agentmodeling.covid19;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Population for infection spread
 * @author axec
 *
 */
public class Population
{
    private int size;
    Map<Integer, Person> alreadyCreated;
    
    //default values
    private PopulationStatistics statistics;
    private double p_immunity = 0; //probability of preexisting immunity
    private RandomGenerator randomGenerator;
    private int immunity_period;
    private int vaccineImmunityPeriod = 90;  //update
    
    /**
     * @returns number of infected persons in population 
     */
    public int getInfectedSize()
    {
        return alreadyCreated.size();
    }

    public int getSize()
    {
        return size;
    }

    public Population(int size)
    {
        this.size = size;
        this.alreadyCreated = new HashMap<>();
    }

    public void setRandomGenerator(RandomGenerator randomGenerator)
    {
        this.randomGenerator = randomGenerator;
    }

    public void setP_immunity(double p_immunity)
    {
        this.p_immunity = p_immunity;
    }
    
    public void setImmunityPeriod(int immunityPeriod)
    {
        this.immunity_period = immunityPeriod;
    }
    
    public void setVaccineImmunityPeriod(int immunityPeriod)
    {
        this.vaccineImmunityPeriod = immunityPeriod;
    }
    
    public void setSize(int size)
    {
        this.size = size;
    }

    public Population(PopulationStatistics statistics, RandomGenerator randomGenerator)
    {
        this( 0, 0, statistics, randomGenerator);
    }
    
    public Population(int size, double p_immunity, PopulationStatistics statistics, RandomGenerator randomGenerator)
    {
        this.randomGenerator = randomGenerator;
        this.size = size;
        this.p_immunity = p_immunity;
        this.alreadyCreated = new HashMap<>();
        this.statistics = statistics;       
    }

    /**
     * Creates initialy infected persons
     * @param infectedSize
     */
    public void generateInitial(int infectedSize)
    {
    	int j = 1;
        for( int i = 0; i < infectedSize; i++ )
        	//randomize??
            createPerson( i , j ); //+ VARIANT ???
    }

    /**
     * Method creates person in the population from given index: 0 < i < size of population.
     * Can be treated as selecting ith person from population
     * @param i - persons index.
     * @return person
     */
    //private Person createPerson(int i) //before 5 April 2022
    private Person createPerson(int i, int j)//after 5 April 2022
    {
        //Person person = new Person( i ); //before 5 April 2022
        Person person = new Person( i, j ); //before 5 April 2022
        person.infected = true;
        int ageGroup = getRandomAgeGroup();
        person.immune = randomGenerator.sampleUniform() < p_immunity;
        person.age = ( ageGroup + 1 ) * 10;
        person.p_critical = statistics.ageCritical[ageGroup];
        person.p_severe = statistics.ageSevere[ageGroup];
        alreadyCreated.put( i, person ); //and what about j ???
        return person;
    }

    private int getRandomAgeGroup()
    {
        double val = randomGenerator.sampleUniform();
        int i = 0;
        for( ; i < statistics.ageProbability.length; i++ )
        {
            if( val < statistics.ageProbability[i] )
                break;
        }
        return i;
    }

    /**
     * Method imports number of infected persons to the population from 
     * @param number - number of imported persons
     * @return list of imported persons
     */
    public List<Person> ImportPersons(int number, int variant)
    {
        List<Person> result = new ArrayList<Person>();
        
        
        for( int i = 0; i < number; i++ )
        {
            int index = size + i;
            result.add( createPerson( index, variant) ); //before 5 April 2022 // + VARIANT???
            //result.add( createPerson( index , variant) ); //after before 5 April 2022
            // sad. I don't know where I can get the word variant. Just like an index
            size++;
        }
        return result;
    }

    /**
     * Method tries to infect given number of persons in population with given chance of infection
     * @param chance - chance of infection
     * @param number - number of persons to be infected, each with given chance
     * @return list of persons who was actually infected
     */    
    public List<Person> tryInfectPersons(double chance, int number, int curDay, int variant)
    {
        List<Person> result = new ArrayList<Person>();
        for( int i = 0; i < number; i++ )
        {
			int personIndex = randomGenerator.sampleInteger(size);
			Person person = null;
			if (alreadyCreated.containsKey(personIndex)) // already infected - skip
			{
				person = alreadyCreated.get(personIndex);
            
				if (person.recovered <= Integer.MAX_VALUE && person.recovered >= curDay - immunity_period)
					continue;
            }
			
            if( randomGenerator.sampleUniform() < chance )
            {
            	if (person == null)
            		person = createPerson( personIndex , variant); // problem 1
               result.add( person );
            }
        }
        
        return result;
    }
    
    public List<Person> tryVaccinePersons(int number, int curDay)
    {
        List<Person> result = new ArrayList<Person>();
        int j = 1;
        int totalNumber = 0;
        while(totalNumber < number) //after
        {
			int personIndex = randomGenerator.sampleInteger(size);
			Person person = null;
			if (alreadyCreated.containsKey(personIndex)) //something happened before
			{
				person = alreadyCreated.get(personIndex);
            
				//person was infected, check immunity
				if (person.recovered <= Integer.MAX_VALUE && person.recovered >= curDay - immunity_period)
				continue;
				
				//person was vaccinated, check immunity
				if (person.vaccinated <= Integer.MAX_VALUE && person.vaccinated >= curDay - vaccineImmunityPeriod)
				continue;
				
				//immunity expired
				person.vaccinated = curDay;
				totalNumber += 1;
			}
			else
			{
				// nothing happened before
				person = createPerson(personIndex, j); // + VARINAT???
				person.vaccinated = curDay;
				totalNumber += 1;
			}
        }
         //after
        //System.out.println("Day "+ curDay + " Vaccinated " + totalNumber + "\n"); //to be commented for a while
        return result;
    }
    
   
}