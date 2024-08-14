package biouml.plugins.agentmodeling;

import java.util.Map;
import java.util.logging.Logger;

import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

/**
 * Base class for all agents in agent-based simulation
 * Part of AgentBasedModel
 * @author Ilya
 * @see AgentBasedModel
 *
 */
public abstract class SimulationAgent implements Comparable<SimulationAgent>, Cloneable
{
    Logger log = Logger.getLogger(SimulationAgent.class.getName());

    //final agent time (when this age is achieved agent dies)
    protected double completionTime;

    //flag indicating whether agent is alive
    protected boolean isAlive = true;

    private double deathTime = Double.POSITIVE_INFINITY;
    
    //initial agent time (agent start its' stepping from that time)
    protected double initialTime;

    //current agent time (current agent age)
    protected double currentTime;

    /**agent time at previous step*/
    protected double previousTime;

    /**
     * Defines how agent inner time corresponds to global model time. 
     * For example timeScale = 10 then one unit of time of this agent corresponds to 10 units of global time.
     * It is used to conform different time units (e.g. seconds and minutes) of agents.
     * By default is set to unity.
     */
    protected double timeScale = 1;

    protected String name;
    
    protected String status;
    
    protected Span span;
    
    protected static final double INITIAL_AGENT_PRIORITY = 10; //default priority for simulation agents
    protected static final double DEFAULT_AGENT_PRIORITY = 1; //default priority for simulation agents
    protected static final double SECOND_AGENT_PRIORITY = 2; //default priority for simulation agents
    protected static final double STEADYSTATE_AGENT_PRIORITY = 0; //priority for steady state agents should be run last 
    protected static final double OBSERVER_AGENT_PRIORITY = -1; //priority for observer agents which should iterate after simulation step is completed
       
    public SimulationAgent(String name, Span span)
    {
        this.name = name;
        this.span = span;
        initialTime = span.getTimeStart();
        completionTime = span.getTimeFinal();
        currentTime = initialTime;
        isAlive = true;
    }
    
    /**
     * Method is used to reset agent so simulation of the model may be started again (or 
     * @throws Exception
     */
    public void init() throws Exception
    {
        initialTime = span.getTimeStart();
        completionTime = span.getTimeFinal();
        currentTime = initialTime;
        previousTime = initialTime;
        deathTime = Double.POSITIVE_INFINITY;
        isAlive = true;
    }
    
    public String getName()
    {
        return name;
    }
    
    @Override
    public int compareTo(SimulationAgent agent)
    {
        int result = Double.compare( agent.getScaledCurrentTime(), this.getScaledCurrentTime() );
        if(result != 0)
            return result;
        return Double.compare( this.getPriority(), agent.getPriority() );
    }
    
    /**
     * Override this method if you want change your agent type execution priority
     */
    public double getPriority()
    {
        return DEFAULT_AGENT_PRIORITY;
    }

    public void setTimeScale(double scale)
    {
        this.timeScale = scale;
    }
    
    public double getScaledCurrentTime()
    {
        return currentTime * timeScale;
    }

    public double getScaledPreviousTime()
    {
        return previousTime * timeScale;
    }

    public void setupPreviousTime()
    {
        previousTime = currentTime;
    }

    /**
     * Get current value of variable with given name.
     * method must be overridden to allow sending messages
     * @param name variable name to send
     * @return value of this variable stored in agent
     * @throws Exception if method is not supported or variable with this name does not exist
     */
    public double getCurrentValue(String name) throws Exception
    {
        throw new Exception("Agent " + getName() + " does not support sending variables");
    }

    /**
     * Set current value of variable with given name.
     * Method must be overridden to allow receiving messages
     * @param name variable name to receive
     * @param value new value for variable
     * @throws Exception if method is not supported or variable with this name does not exist
     */
    public void setCurrentValue(String name, double value) throws Exception
    {
        throw new Exception("Agent " + getName() + " does not support receiveing variables");
    }

    /**
     * Update value of variable with given name.
     * Method must be overridden to allow sharing variables
     * @param name variable name to receive
     * @param update update to be added to variable value
     * @throws Exception if method is not supported or variable with this name does not exist
     */
    public void setCurrentValueUpdate(String name, double update) throws Exception
    {
        throw new Exception("Agent " + getName() + " does not support receiving variables update");
    }

    /**
     * Creates variable with given name in the agent for following sending or receiving value
     * Method must be overridden to allow sending or receiving variables
     * @param name of variable. Agent must be able to get or set values of that variable
     * @throws Exception if method is not supported or variable with this name does not exist
     */
    public void addVariable(String name) throws Exception
    {
        throw new Exception("Agent " + getName() + " does not support sending or recieving variables");
    }
    
    public void addVariable(String name, double value) throws Exception
    {
        addVariable(name);
    }

    /**
     * Return true if agent already contains variable with given name
     * Method must be overridden to allow sending or receiving variables
     * @param name of variable. Agent must be able to get or set values of that variable
     * @throws Exception if method is not supported or variable with this name does not exist
     */
    public boolean containsVariable(String name) throws Exception
    {
        throw new Exception("Agent " + getName() + " does not support sending or recieving variables");
    }

    /**
     * @return array containing all variable values
     * @throws Exception 
     */
    public abstract double[] getCurrentValues() throws Exception;
 
    public void setCurrentValues(double[] values) throws Exception
    {
        
    }
    
    /**
     * @return array of variable names
     */
    public abstract String[] getVariableNames();
    
    /**
     * Agent initialization (optional)
     */
    public void applyChanges() throws Exception
    {
        //do nothing by default
    }
    
    /**
     * Agent routine (e.g. integrating differential equation)
     */
    public abstract void iterate();

    public void iterate(Simulator simulator)
    {
    	
    }

    /**
     * Death condition for agent. When it became true agent dies.
     * Returns true if agent reaches it's death age
     */
    public boolean deathCondition()
    {
        return ( currentTime > completionTime || !isAlive );
    }
    
    public void die()
    {
        deathTime = this.currentTime;
        isAlive = false;
    }
    
    public boolean isAlive()
    {
        return isAlive;
    }
    
    public boolean isAlive(double time)
    {
        return this.timeScale*deathTime >= time;
    }
    
    public boolean shouldDivide()
    {
       return false;
    }
    
    public SimulationAgent[] divide() throws Exception
    {
       return null;
    }
    
    @Override
    public String toString()
    {
        return this.getName();
    }
    
    /**
     * @return array containing all variable values
     * @throws Exception 
     */
    public abstract double[] getUpdatedValues() throws Exception;
    
    /**
     * This method is only for internal usage by scheduler.
     * it tells agent that process of its update is finished and now agent has valid values of variables
     * it is called before each step is taken by agent
     */
    protected abstract void setUpdated() throws Exception;
    
    //TODO: more elaborate status
    public String getErrorMessage()
    {
        return null;
    }

    public Map<String, Integer> getMapping()
    {
        return null;
    }

    public SimulationAgent clone(String name) throws Exception
    {
        return null;
    }
    
}
