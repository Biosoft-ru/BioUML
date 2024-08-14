package biouml.plugins.keynodes.graph;

import biouml.plugins.keynodes.biohub.KeyNodesHub;

// Must be stateless!
public interface GraphDecorator<T extends GraphDecoratorParameters>
{
    boolean isAcceptable(KeyNodesHub<?> hub);
    
    <N> HubGraph<N> decorate(HubGraph<N> graph, ElementConverter<N> converter, T parameters);

    Class<T> getParametersClass();
}
