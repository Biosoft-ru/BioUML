package biouml.plugins.virtualcell.simulation;

import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Span;
import biouml.plugins.virtualcell.diagram.TableCollectionPoolProperties;

public class ProcessAgent extends SimulationAgent
{

    private TableCollectionPoolProperties[] inputs;
    private TableCollectionPoolProperties[] outputs;
    
    public ProcessAgent(String name, Span span)
    {
        super( name, span );
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getVariableNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void iterate()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public double[] getUpdatedValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setUpdated() throws Exception
    {
        // TODO Auto-generated method stub
        
    }

}
