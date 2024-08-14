package biouml.plugins.agentmodeling.covid19;

import ru.biosoft.util.bean.BeanInfoEx2;

public class AgentPopulationSimulationEngineBeanInfo extends BeanInfoEx2<AgentPopulationSimulationEngine>
{
    public AgentPopulationSimulationEngineBeanInfo()
    {
        super(AgentPopulationSimulationEngine.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("completionTime");
        add("repeats");
        add("manualSeed");
        addHidden( "seed", "isSeedHidden");
        add("customPopulation");
        addHidden("populationData", "isPopulationTableHidden");
        add("externalScenario");
        addHidden("scenarioTable", "isScenarioTableHidden");
    }
}
