package biouml.plugins.virtualcell.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Model;

public class VirtualCellModel implements Model
{
    protected List<SimulationAgent> agents = new ArrayList<>();
    protected List<MapPool> pools = new ArrayList<>();

    public void addAgent(SimulationAgent agent)
    {
        agents.add( agent );
    }

    public void addPool(MapPool pool)
    {
        pools.add( pool );
    }

    public List<SimulationAgent> getAgents()
    {
        return agents;
    }

    public List<MapPool> getPools()
    {
        return pools;
    }


    @Override
    public void init() throws Exception
    {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isInit()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public double[] getCurrentValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public double[] getCurrentState() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Model clone()
    {
        return new VirtualCellModel();
    }

    @Override
    public double[] getInitialValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(double[] initialValues, Map<String, Double> parameters) throws Exception
    {
        // TODO Auto-generated method stub
        
    }
}