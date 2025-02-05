package biouml.plugins.agentmodeling;

import java.util.HashMap;
import java.util.Map;

public class Classification
{
    public HashMap<Double, String> titles= new HashMap<>();
    Map<Double, Integer> counts = new HashMap<>();
    private String variableName;
    private int index = -1;
    private Class agentClass = SimulationAgent.class;

    public Classification(String variableName)
    {
        this.variableName = variableName;
    }
    
    public Classification(String variableName, int index)
    {
        this.variableName = variableName;
        this.index = index;
    }
    
    public void setClass(Class<? extends SimulationAgent> agentClass)
    {
        this.agentClass = agentClass;
    }
    
    public boolean accept(SimulationAgent agent)
    {
        return agentClass == null? true: agent.getClass().isAssignableFrom( agentClass );
    }
        
    public void setTitle(Double value, String title)
    {
        titles.put(value, title);
    }

    public void reset()
    {
        counts.clear();
    }

    public String getVariableName()
    {
        return variableName;
    }

    public void classify(SimulationAgent agent)
    {
//        if (!accept(agent))
//            return;
       
        try
        {
            if( index != -1 )
                counts.compute( agent.getCurrentValues()[index], (k, v) -> v == null ? 1 : v + 1 );
            else
                counts.compute( agent.getCurrentValue( variableName ), (k, v) -> v == null ? 1 : v + 1 ); //Not every agent can return value by name
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}