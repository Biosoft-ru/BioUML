package biouml.plugins.keynodes.graph;

import java.util.HashSet;
import java.util.Set;

import one.util.streamex.StreamEx;

import biouml.plugins.keynodes.biohub.KeyNodesHub;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CustomDomainDecorator implements GraphDecorator<CustomDomainDecorator.Parameters>
{
    @SuppressWarnings ( "serial" )
    public static class Parameters extends GraphDecoratorParameters
    {
        DataElementPath domainCollectionPath;

        public DataElementPath getDomainCollectionPath()
        {
            return domainCollectionPath;
        }

        public void setDomainCollectionPath(DataElementPath domainCollectionPath)
        {
            DataElementPath oldValue = this.domainCollectionPath;
            this.domainCollectionPath = domainCollectionPath;
            firePropertyChange( "domainCollectionPath", oldValue, domainCollectionPath );
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "domainCollectionPath" ).inputElement( ru.biosoft.access.core.DataCollection.class ).add();
        }
    }

    @Override
    public boolean isAcceptable(KeyNodesHub<?> hub)
    {
        return true;
    }

    @Override
    public <N> HubGraph<N> decorate(HubGraph<N> graph, ElementConverter<N> converter, Parameters parameters)
    {
        Set<String> domain = new HashSet<>( parameters.getDomainCollectionPath().getDataCollection().getNameList() );
        return new DelegatingGraph<N>( graph)
        {
            @Override
            public StreamEx<N> startingNodes()
            {
                return graph.nodes().filter( node -> domain.contains( node.toString() ) );
            }
        };
    }

    @Override
    public Class<Parameters> getParametersClass()
    {
        return Parameters.class;
    }
}
