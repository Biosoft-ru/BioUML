package biouml.plugins.agentmodeling;

public class ConstantAgent extends SimulationAgent
{
    private double value;
    private SimulationAgent parent;

    public ConstantAgent(SimulationAgent parent, String name, double value)
    {
        super(name, parent.span);
        this.value = value;
        this.parent = parent;
    }

    @Override
    public double getScaledCurrentTime()
    {
        return Double.MAX_VALUE;
    }

    @Override
    public double getScaledPreviousTime()
    {
        return initialTime;
    }

    @Override
    public double getCurrentValue(String name) throws Exception
    {
        return value;
    }

    @Override
    public void addVariable(String name) throws Exception
    {

    }

    @Override
    public double getPriority()
    {
        return INITIAL_AGENT_PRIORITY;
    }

    @Override
    public boolean containsVariable(String name) throws Exception
    {
        return true;
    }


    @Override
    public void iterate()
    {
        isAlive = parent.isAlive;
        //do nothing as agent does not change at all
    }

    @Override
    public double[] getCurrentValues()
    {
        return new double[] {value};
    }

    @Override
    public String[] getVariableNames()
    {
        return new String[] {name};
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
}
