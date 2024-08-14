package biouml.plugins.agentmodeling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.agentmodeling.StatCollector;

/**
 * Collector intended to collect data from one given agent
 * @author axec
 */
public class AgentStatCollector extends StatCollector
{
    private SimulationAgent agent;
    private Map<String, List<Double>> values;

    public List<Double> getDynamic(String name)
    {
        return values.get( name );
    }

    public void addVariable(String varName)
    {
        values.put( varName, new ArrayList<>() );
    }
    
    public AgentStatCollector(SimulationAgent agent, String[] names)
    {
        this.agent = agent;
        this.values = new HashMap<>();
        for( String name : names )
            values.put( name, new ArrayList<>() );
    }

    @Override
    public void finish()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void update(double time) throws Exception
    {
        for( Entry<String, List<Double>> entry : values.entrySet() )
        {
            entry.getValue().add( agent.getCurrentValue( entry.getKey() ) );
        }
    }

    @Override
    public void update(double time, SimulationAgent agent) throws Exception
    {
    }
}