package biouml.plugins.agentmodeling.covid19.old;

import java.lang.reflect.Field;
import java.util.List;

import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.SimulationEngine;

public class AgentSource extends ModelAgent
{
    private Population population;
    private String numberProduceName;
    private Field numberProduceField;
    private MortalProducerAgent prototype;
    
    public AgentSource(Population population, SimulationEngine engine)
            throws Exception
    {
        super(engine );
        this.population = population;
    }
    
    public void setPrototypeAgent(SimulationAgent agent)
    {
        this.prototype = (MortalProducerAgent)agent;
    }
    
    @Override
    public void iterate()
    {
        super.iterate();
    }

    @Override
    public boolean shouldDivide()
    {
        try
        {
            return numberProduceField.getDouble( model ) != 0;
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public MortalProducerAgent[] divide() throws Exception //TODO: clone
    {
        int numberProduced = (int)numberProduceField.getDouble( model ); //TODO use indices instead
        List<Person> infected = population.ImportPersons(this , numberProduced );
        MortalProducerAgent[] result = new MortalProducerAgent[infected.size()];
        for( int i = 0; i < infected.size(); i++ )
        {
            Person person = infected.get( i);            
            result[i] = prototype.createCopy( person );
        }
        
//        if (result.length > 0)
//        /log.info( getScaledCurrentTime()+ " new agents arrived: "+result.length );
        return result;
    }
    
    public void setNumberProduceName(String name)
    {
        try
        {
            this.numberProduceName = name;
            this.numberProduceField = model.getClass().getDeclaredField( numberProduceName );
        }
        catch( Exception ex )
        {
            this.numberProduceField = null;
        }
    }
}
