package biouml.plugins.simulation;

import ru.biosoft.util.bean.BeanInfoEx2;
import one.util.streamex.StreamEx;

/**
 * Definition of common properties for simulation engine
 */
public class SimulationEngineBeanInfo extends BeanInfoEx2<SimulationEngine>
{
    protected SimulationEngineBeanInfo(Class<? extends SimulationEngine> simulationEngine)
    {
        super(simulationEngine);
    }
    
    protected SimulationEngineBeanInfo(Class<? extends SimulationEngine> simulationEngine, String messageBundle)
    {
        super(simulationEngine, messageBundle);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("initialTime");
        add("completionTime");
        add("timeIncrement");
        property("solverName").structureChanging().tags(bean ->  StreamEx.of(bean.getAvailableSolvers())).add();
        add("simulatorOptions");
    }
}
