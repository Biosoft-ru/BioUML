package biouml.plugins.agentmodeling;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PopulationAgentSimulationEngineBeanInfo extends BeanInfoEx2<PopulationAgentSimulationEngine>
{
    public PopulationAgentSimulationEngineBeanInfo()
    {
        super(PopulationAgentSimulationEngine.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("initialTime");
        add("completionTime");
        add("timeIncrement");
        add( "agentsNumber" );
        add( "threads" );
        add("mainEngine");
    }

}
