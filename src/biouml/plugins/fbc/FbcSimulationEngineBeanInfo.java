package biouml.plugins.fbc;

import one.util.streamex.StreamEx;

import biouml.plugins.simulation.SimulationEngineBeanInfo;

public class FbcSimulationEngineBeanInfo extends SimulationEngineBeanInfo
{
    public FbcSimulationEngineBeanInfo()
    {
        super( FbcSimulationEngine.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property("engineName").tags(bean -> StreamEx.of(((FbcSimulationEngine)bean).getAvailableSimulationEngines())).add();
        add("engine");
    }
}
