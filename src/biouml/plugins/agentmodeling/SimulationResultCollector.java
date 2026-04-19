package biouml.plugins.agentmodeling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import biouml.plugins.simulation.Span;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;

/**
 * Collector intended to collect data from one given agent to simulation result
 * @author axec
 */
public class SimulationResultCollector extends StatCollector
{
    private static final String TIME = "time";
    private SimulationResult result;
    private String agentName;
    private ModelAgent agent;
    private List<String> varNames = new ArrayList<>();
    private int timeIndex = -1;
    private boolean hasTime = false;
    
    public SimulationResultCollector(String subdiagram, String resultPath)
    {
        this(subdiagram, resultPath, new String[] {});
    }
    
    public SimulationResultCollector(String subdiagram, String resultPath, String ... names)
    {
        for( String name : names )
        {
            this.varNames.add( name );
            if (name.equals( TIME ))
                hasTime = true;
        }
   
        this.agentName = subdiagram;
        DataElementPath path = DataElementPath.create( resultPath );
        this.result = new SimulationResult( path.getParentCollection(), path.getName() );
        result.setDiagramName( "Whatever" );
        result.setVariableMap( new HashMap<String, Integer>() );
    }

    @Override
    public void init(Span span, AgentBasedModel model) throws Exception
    {
        for( SimulationAgent agent : model.getAgents() )
        {
            if( agent.getName().equals( agentName ) )
            {
                if (!(agent instanceof ModelAgent))
                {
                    throw new Exception( "Only agents with mathematical models allowed for result collector" );
                }
                this.agent = (ModelAgent)agent;

                if( varNames.size() == 0 )
                    varNames = StreamEx.of( this.agent.getEngine().getVarIndexMapping().keySet() ).toList();               
                else if (!hasTime)
                    varNames.add( TIME );
                
                for( int index = 0; index < varNames.size(); index ++)
                {
                    String name = varNames.get( index );
                    if (name.equals( TIME ))
                            timeIndex = index;
                    addVariable( name );
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
        result.add( values[timeIndex], values );
    }

    @Override
    public void update(double time, SimulationAgent agent) throws Exception
    {
    }
}