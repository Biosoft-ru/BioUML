package biouml.plugins.agentmodeling;

import one.util.streamex.StreamEx;
import biouml.plugins.simulation.SimulationEngineBeanInfo;

/**
 * Definition of common properties for simulation engine
 */
public class AgentSimulationEngineWrapperBeanInfo extends SimulationEngineBeanInfo
{
    public AgentSimulationEngineWrapperBeanInfo()
    {
        super(AgentSimulationEngineWrapper.class);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        property("engineName").tags(bean -> StreamEx.of(((AgentSimulationEngineWrapper)bean).getAvailableSimulationEngines())).add();
        addReadOnly( "subDiagramPath" );
        add("initialTime");
        add("completionTime");
        add("timeIncrement");
        add("timeScale");
        property("stepType").tags( AgentSimulationEngineWrapper.TYPE_STANDARD, AgentSimulationEngineWrapper.TYPE_STEADY_STATE).structureChanging().add();
        addHidden( "timeBeforeSteadyState" , "isNotSteadyState");
        addHidden( "timeStepBeforeSteadyState", "isNotSteadyState" );
        addHidden( "controlTimeStart", "isNotSteadyState" );
        addHidden( "controlTimeStep", "isNotSteadyState" );
        property("solverName").tags(bean ->  StreamEx.of(bean.getAvailableSolvers())).add();
        add("simulatorOptions");

    }

}
