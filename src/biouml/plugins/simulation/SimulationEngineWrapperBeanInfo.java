package biouml.plugins.simulation;

import one.util.streamex.StreamEx;

import ru.biosoft.util.bean.BeanInfoEx2;


public class SimulationEngineWrapperBeanInfo extends BeanInfoEx2<SimulationEngineWrapper>
{
    public SimulationEngineWrapperBeanInfo()
    {
        super( SimulationEngineWrapper.class, biouml.plugins.simulation.resources.MessageBundle.class.getName() );
    }

    @Override
    public void initProperties() throws Exception
    {
        //addWithTags( "engineName", bean -> StreamEx.of( bean.getAvailableEngines()));
        property("engineName").tags(bean -> StreamEx.of(bean.getAvailableEngines())).structureChanging().add();
        addHidden( "engine", "isEngineHidden" );
    }
}
