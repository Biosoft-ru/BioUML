package biouml.plugins.agentmodeling.covid19.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import biouml.plugins.agentmodeling.MortalAgent;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.standard.simulation.ResultListener;

public class MortalProducerAgent extends MortalAgent
{
    private Population population;
    String chanceProduceName;
    String numberProduceName;
    private List<MortalProducerAgent> createdAgents = new ArrayList<>();
    private MortalProducerAgent creator;
    private Map<SimulationAgent, Set<String>> globalAgents = new HashMap<>();
        
    public MortalProducerAgent(Population population, Model model, int deathIndex, Simulator simulator, Span span, String name, ResultListener ... listeners)
            throws Exception
    {
        super( model, deathIndex, simulator, span, name, listeners );
        this.population = population;
    }
    
    public Span getSpan()
    {
        return this.span;
    }
    
    @Override
    public void iterate()
    {
        try
        {
            for( Entry<SimulationAgent, Set<String>> e : globalAgents.entrySet() )
            {
                for( String variable : e.getValue() )
                    setCurrentValue( variable, e.getKey().getCurrentValue( variable ) );
            }
            
            super.iterate();

            for( Entry<SimulationAgent, Set<String>> e : globalAgents.entrySet() )
            {
                for( String variable : e.getValue() )
                    e.getKey().setCurrentValue( variable, getCurrentValue( variable ) );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
    
    public void addGlobalContext(SimulationAgent agent, Set<String>sharedVariables) throws Exception
    {
        globalAgents.put( agent, sharedVariables );
    }
        
    @Override
    public boolean shouldDivide()
    {
        try
        {
            return getCurrentValue(chanceProduceName) != 0;
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public MortalProducerAgent[] divide() throws Exception
    {
        int numberProduced = (int)getCurrentValue(numberProduceName); //TODO use indices instead
//        log.info( currentTime+ ": "+ name + " wants to infected " + numberProduced );

        double chance = getCurrentValue(chanceProduceName);
        
        List<Person> infected = population.tryInfectPersons( chance, numberProduced );

//        if (numberProduced > 0)
//        {
//        System.out.println( currentTime+ ": "+ name + " wants to infected " + numberProduced +" chance "+ chance+  "Population permits "+infected.size());
//        }
//        log.info("Population permits "+infected.size());

        MortalProducerAgent[] result = new MortalProducerAgent[infected.size()];
        for( int i = 0; i < infected.size(); i++ )
        {
            Person person = infected.get( i );
            MortalProducerAgent copy = createCopy( person );
            createdAgents.add( copy );
            result[i] = copy;
        }
//        if( result.length > 0 )
//            log.info( currentTime+ ": "+ name + " infected " + result.length + " ( "+StreamEx.of( result ).map(agent->agent.getName()).joining( "," )+")" );
        return result;
    }
    
    public void setChanceProduceName(String name)
    {
        this.chanceProduceName = name;
    }

    public void setNumberProduceName(String name)
    {
        this.numberProduceName = name;
    }

    public MortalProducerAgent createCopy(Person person) throws Exception
    {
        Model newModel = this.model.clone();
        newModel.init();
        
        //TODO: better way
        double[] values = newModel.getCurrentValues();
        values[this.variableToIndex.get( "age" )] = person.age;
        newModel.setCurrentValues( values );
        
        Simulator newSimulator = this.simulator.getClass().newInstance();
        
        if (simulator instanceof EventLoopSimulator)
        {
            Simulator innerSolver = ( (EventLoopSimulator)simulator ).getSolver().getClass().newInstance();
            innerSolver.setOptions(  ( (EventLoopSimulator)simulator ).getSolver().getOptions());
            ((EventLoopSimulator)newSimulator).setSolver( innerSolver );
        }
        
        ( (SimulatorSupport)newSimulator ).setFireInitialValues( false );
        Span newSpan = span.getRestrictedSpan( currentTime, completionTime );

        MortalProducerAgent agent = new MortalProducerAgent( population, newModel, deathIndex, newSimulator, newSpan, String.valueOf(person.index),
                this.listeners.toArray( new ResultListener[listeners.size()] ) );

        for( Entry<String, Integer> port : variableToIndex.entrySet() )
            agent.addPort( port.getKey(), port.getValue() );

        for( Entry<SimulationAgent, Set<String>> e : globalAgents.entrySet() )
            agent.addGlobalContext( e.getKey(), e.getValue() );

//        agent.setCurrentValue( "age", person.age );
        
        agent.creator = this;
        agent.setChanceProduceName( chanceProduceName );
        agent.setNumberProduceName( numberProduceName );
        return agent;
    }
    
    public MortalProducerAgent getCreator()
    {
        return creator;
    }
    
    public List<MortalProducerAgent> getCreated()
    {
        return createdAgents;
    }
}