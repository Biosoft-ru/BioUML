package biouml.plugins.keynodes.graph;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.keynodes.biohub.KeyNodesHub;

public class StubDecorator implements GraphDecorator<StubDecorator.StubDecoratorParameters>
{
    @Override
    public boolean isAcceptable(KeyNodesHub<?> hub)
    {
        return true;
    }

    @Override
    public <N> HubGraph<N> decorate(HubGraph<N> graph, ElementConverter<N> converter, StubDecoratorParameters parameters)
    {
        return graph;
    }

    @Override
    public Class<StubDecoratorParameters> getParametersClass()
    {
        return StubDecoratorParameters.class;
    }

    @SuppressWarnings ( "serial" )
    public static class StubDecoratorParameters extends GraphDecoratorParameters
    {
    }

    public static class StubDecoratorParametersBeanInfo extends BeanInfoEx2<StubDecoratorParameters>
    {
        public StubDecoratorParametersBeanInfo()
        {
            super(StubDecoratorParameters.class);
        }
    }
}