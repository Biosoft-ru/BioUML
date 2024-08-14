package biouml.plugins.keynodes.graph;

import java.util.Set;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.keynodes.biohub.KeyNodesHub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RemoveNodesDecorator implements GraphDecorator<RemoveNodesDecorator.Parameters>
{
    @Override
    public boolean isAcceptable(KeyNodesHub<?> hub)
    {
        return true;
    }

    @Override
    public <N> HubGraph<N> decorate(HubGraph<N> graph, ElementConverter<N> converter, Parameters parameters)
    {
        return new DelegatingGraph<N>( graph )
        {
            Set<String> namesToRemove = parameters.getInputTable().getDataCollection().names().collect( Collectors.toSet() );
            @Override
            public StreamEx<N> nodes()
            {
                return super.nodes().filter( n -> !namesToRemove.contains( n.toString() ) );
            }
            @Override
            public StreamEx<N> startingNodes()
            {
                return super.startingNodes().filter( n -> !namesToRemove.contains( n.toString() ) );
            }
            @Override
            public boolean hasNode(N node)
            {
                return super.hasNode( node ) && !namesToRemove.contains( node.toString() );
            }
            @Override
            public void visitEdges(N start, boolean upstream, HubEdgeVisitor<N> visitor)
            {
                super.visitEdges( start, upstream, (HubEdge edge, N otherEnd, float weight) -> {
                    if( namesToRemove.contains( start.toString() ) || namesToRemove.contains( otherEnd.toString() ) )
                        return;
                    visitor.accept( edge, otherEnd, weight );
                } );
            }
        };
    }

    @Override
    public Class<Parameters> getParametersClass()
    {
        return Parameters.class;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends GraphDecoratorParameters
    {
        private DataElementPath inputTable;

        @PropertyName ( "Input table" )
        public DataElementPath getInputTable()
        {
            return inputTable;
        }
        public void setInputTable(DataElementPath inputTable)
        {
            Object oldValue = this.inputTable;
            this.inputTable = inputTable;
            firePropertyChange( "inputTable", oldValue, inputTable );
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
            property( "inputTable" ).inputElement( TableDataCollection.class ).add();
        }
    }

}
