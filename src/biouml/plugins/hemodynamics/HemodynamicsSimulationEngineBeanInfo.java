package biouml.plugins.hemodynamics;

import biouml.plugins.simulation.SimulationEngineBeanInfo;

public class HemodynamicsSimulationEngineBeanInfo extends SimulationEngineBeanInfo
{
    public HemodynamicsSimulationEngineBeanInfo()
    {
        super( HemodynamicsSimulationEngine.class );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add("resultAllVessels");
    }
}
