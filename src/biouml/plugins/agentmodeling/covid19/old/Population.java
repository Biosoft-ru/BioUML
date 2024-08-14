package biouml.plugins.agentmodeling.covid19.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ru.biosoft.table.TableDataCollection;

public class Population
{
    private int size;
    Map<Integer, Person> alreadyCreated;
    private Random r;
    private double[] ageProportion;
    private double[] ageSevere;
    private double[] ageCritical;
    private double[] ageContacts;
    private double[] ageImmunity;
    private double p_immunity;

    public int getInfectedSize()
    {
        return alreadyCreated.size();
    }
    public Population(int size, double p_immunity, TableDataCollection statistics)
    {
        r = new Random();
        this.size = size;
        this.p_immunity = p_immunity;
        this.alreadyCreated = new HashMap<>();
        ageProportion = AgentPopulationModelOld.fillAgeProportion( statistics );
        ageSevere = AgentPopulationModelOld.fillAgeSeverity( statistics );
        ageCritical = AgentPopulationModelOld.fillAgeCritical( statistics );
    }

    public void generateInitial(int infectedSize)
    {
        for( int i = 0; i < infectedSize; i++ )
            createPerson( i );
    }

    private Person createPerson(int i)
    {
        Person person = new Person( i );
        person.infected = true;
        int ageGroup = getRandomAgeGroup();
        person.immune = r.nextDouble() < p_immunity ? 1: 0;
        person.age = ( ageGroup + 1 ) * 10;
        person.p_critical = ageCritical[ageGroup];
        person.p_severe = ageSevere[ageGroup];
        alreadyCreated.put( i, person );
        return person;
    }

    private int getRandomAgeGroup()
    {
        double val = r.nextDouble();
        int i = 0;
        for( ; i < ageProportion.length; i++ )
        {
            if( val < ageProportion[i] )
                break;
        }
        return i;
    }

    public List<Person> ImportPersons(AgentSource producer, int number)
    {
        List<Person> result = new ArrayList<Person>();
        for( int i = 0; i < number; i++ )
        {
            int index = size + i;
            result.add( createPerson( index ) );
            size++;
        }
        return result;
    }

    public List<Person> tryInfectPersons(double chance, int number)
    {
        List<Person> result = new ArrayList<Person>();
        for( int i = 0; i < number; i++ )
        {
            int personIndex = r.nextInt( size );
            if( alreadyCreated.containsKey( personIndex ) ) //already infected - skip
                continue;

            if( r.nextDouble() < chance )
                result.add( createPerson( personIndex ) );
        }
        return result;
    }
}