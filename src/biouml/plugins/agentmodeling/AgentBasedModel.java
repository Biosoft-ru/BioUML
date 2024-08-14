package biouml.plugins.agentmodeling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import biouml.plugins.simulation.Model;

public class AgentBasedModel implements Model
{
    //All links leading to agent from scheduler
    HashMap<SimulationAgent, HashSet<Link>> agentToInputLinks = new HashMap<>();
   
    //All links leading from agent to scheduler
    HashMap<SimulationAgent, HashSet<Link>> agentToOutputLinks = new HashMap<>();

    //set of agents in model
    protected ArrayList<SimulationAgent> agents = new ArrayList<>();
    
    private Map<SimulationAgent, Integer> agentToFirstIndex;
    private int resultSize;
    protected boolean isInit = false;

    public boolean addAgent(SimulationAgent a)
    {
        return agents.add(a);
    }

    public boolean removeAgent(SimulationAgent a)
    {
        return agents.remove(a);
    }
    
    public boolean containsAgent(Object a)
    {
        return agents.contains(a);
    }  
    
    public List<SimulationAgent> getAgents()
    {
        return agents;
    }

    /**
     * Add undirected link: agent1 < - > agent2. Link transfers changes made by each agent to another.
     * @param agent1, agent2 - interacting agents
     * @param name1 variable local name in agent1
     * @param name2 variable local name in agent2
     * @throws Exception if wrong agents or agents does not contain variables with corresponding names
     */
    public void addUndirectedLink(SimulationAgent agent1, String name1, SimulationAgent agent2, String name2, boolean senderIsMain, String conversionFactor) throws Exception
    {
        if( !containsAgent(agent1) )
            throw new Exception("Unknown agent " + agent1.getName());
        if( !containsAgent(agent2) )
            throw new Exception("Unknown agent " + agent2.getName());

        if( !agent1.containsVariable( name1 ) )
            agent1.addVariable( name1 );

        if( !agent2.containsVariable( name2 ) )
            agent2.addVariable( name2 );

        UpdateValueLink forwardLink = new UpdateValueLink(agent1, agent2, name1, name2);
        UpdateValueLink backwardLink = new UpdateValueLink(agent2, agent1, name2, name1);

        forwardLink.setConversion(senderIsMain, conversionFactor);
        backwardLink.setConversion( !senderIsMain, conversionFactor);

        if( !conversionFactor.isEmpty() )
        {
            if( senderIsMain )
                agent1.addVariable(conversionFactor);
            else
                agent2.addVariable(conversionFactor);
        }

        registerLink(forwardLink, agent2);
        registerLink(backwardLink, agent1);
    }
    
    public void addUndirectedLink(SimulationAgent agent1, String name1, SimulationAgent agent2, String name2) throws Exception
    {
        addUndirectedLink(agent1, name1, agent2, name2, false, "");
    }

    /**
     * Add directed link: agent sender -> agent receiver. Link transfers full variable value from one agent to another
     * @param sender agent which sends the message (variable value )
     * @param receiver agent which receives the message (variable value)
     * @param nameAtSender variable local name in sender agent
     * @param nameAtReceiver variable local name in receiver agent
     * @throws Exception if wrong agents or agents does not contain variables with corresponding names
     */
    public void addDirectedLink(SimulationAgent sender, String nameAtSender, SimulationAgent receiver, String nameAtReceiver)
            throws Exception
    {
        if( !containsAgent(sender) )
            throw new Exception("Unknown agent " + sender.getName());
        if( !containsAgent(receiver) )
            throw new Exception("Unknown agent " + receiver.getName());

        if( !sender.containsVariable(nameAtSender) )
            sender.addVariable(nameAtSender);

        if( !receiver.containsVariable( nameAtReceiver ) )
            receiver.addVariable( nameAtReceiver, sender.getCurrentValue( nameAtSender ) );
        registerLink( new FullValueLink( sender, receiver, nameAtSender, nameAtReceiver), receiver);
    }

    private void registerLink(Link link, SimulationAgent reciever)
    {
        SimulationAgent sender = link.getSender();

        agentToOutputLinks.computeIfAbsent( sender, s -> new HashSet<>() ).add( link );
        agentToInputLinks.computeIfAbsent( reciever, r -> new HashSet<>() ).add( link );
    }

    @Override
    public double[] getInitialValues()
    {
        // TODO Auto-generated method stub
        return new double[0];
    }

    @Override
    public void init() throws Exception
    {       
        for (Link link: agentToInputLinks.values().stream().flatMap(Set::stream).collect(Collectors.toSet()))
        {
//            System.out.println(link.sender.getName()+" ( "+link.nameAtSender+" )");
            link.init();
        }
        
        agentToFirstIndex = new HashMap<>();
        resultSize = 0;
        for( SimulationAgent agent : agents )
        {
            if( agent instanceof ModelAgent )
            {
                agentToFirstIndex.put(agent, resultSize);
                resultSize += agent.getCurrentValues().length;
                ((ModelAgent) agent).getModel().init();
                ((ModelAgent) agent).init();
            }
        }
        isInit = true;
    }

    @Override
    public void init(double[] initialValues, Map<String, Double> parameters)
    {
        // TODO Auto-generated method stub

    }
    
    @Override
    public AgentBasedModel clone()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInit()
    {
        return isInit;
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        double[] result = new double[resultSize];
        for( SimulationAgent agent : this.agents )
        {
            if( agent instanceof ModelAgent )
            {
                double[] agentValues = agent.getUpdatedValues();
                if( agentValues == null )
                    continue;
                System.arraycopy( agentValues, 0, result, agentToFirstIndex.get( agent ), agentValues.length );
            }
        }
        return result;
    }
    
    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        for( SimulationAgent agent : this.agents )
        {
            if( agent instanceof ModelAgent )
            {
                double[] agentValues = agent.getCurrentValues();
                System.arraycopy( values,  agentToFirstIndex.get( agent ), agentValues, 0, agentValues.length );
                ( (ModelAgent)agent ).updatedFromOutside = true;
                agent.setCurrentValues(agentValues);
                agent.setUpdated();
            }
        }
    }

	@Override
	public double[] getCurrentState() throws Exception 
	{
		return getCurrentValues(); //implement properly
	}
}