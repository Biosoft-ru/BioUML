package biouml.plugins.agentmodeling;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import biouml.plugins.simulation.Span;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataElementPath;

/**
 * Collector intended to collect data from one given agent to simulation result
 * @author axec
 */
public class SimulationResultCollector extends StatCollector
{
    private SimulationResult result;
    private String agentName;
    private SimulationAgent agent;

    public SimulationResultCollector(String subdiagram, String resultPath, String ... names)
    {
        this.agentName = subdiagram;
        DataElementPath path =  DataElementPath.create( resultPath );
        this.result = new SimulationResult( path.getParentCollection(), path.getName() );
        result.setVariableMap( new HashMap<String, Integer>() );
        for( String name : names )
            addVariable( name );
    }

    @Override
    public void init(Span span, AgentBasedModel model) throws Exception
    {
        for( SimulationAgent agent : model.getAgents() )
        {
            if( agent.getName().equals( agentName ) )
            {
                this.agent = agent;
                agent.addVariable("time");
                for (String name: result.getVariables())
                {
                    agent.addVariable( name );
                }
            }
        }
    }

    public void addVariable(String varPath)
    {
        Map<String, Integer> map = result.getVariableMap();
        int oldSize = map.size();
        map.put( varPath, oldSize );
        result.setVariableMap( map );
        result.setVariablePathMap( map );
    }

    @Override
    public void finish()
    {
        result.getOrigin().put( result );
    }

    @Override
    public void update(double time) throws Exception
    {
        double[] values = new double[result.getVariableMap().size()];
        for( Entry<String, Integer> e : result.getVariableMap().entrySet() )
        {
            Integer index = e.getValue();
            String varName = e.getKey();
            double value = agent.getCurrentValue( varName );
            values[index] = value;
        }
        result.add( time, values );
    }

    @Override
    public void update(double time, SimulationAgent agent) throws Exception
    {
    }
}