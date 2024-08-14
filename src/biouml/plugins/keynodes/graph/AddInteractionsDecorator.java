package biouml.plugins.keynodes.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.MemoryHubGraph.HubRelation;

public class AddInteractionsDecorator implements GraphDecorator<AddInteractionsDecorator.AddInteractionsDecoratorParameters>
{
    @Override
    public boolean isAcceptable(KeyNodesHub<?> hub)
    {
        return true;
    }

    private float toWeight(Object value)
    {
        float val = ((Number)DataType.Float.convertValue( value )).floatValue();
        return val <= 0 ? Float.POSITIVE_INFINITY : val;
    }

    @Override
    public <N> HubGraph<N> decorate(HubGraph<N> graph, ElementConverter<N> converter,
            AddInteractionsDecoratorParameters parameters)
    {
        TableDataCollection table = parameters.getTable().getDataElement( TableDataCollection.class );
        int fromIndex = table.getColumnModel().getColumnIndex( parameters.getFromColumn() );
        int toIndex = table.getColumnModel().getColumnIndex( parameters.getToColumn() );
        int weightIndex = table.getColumnModel().optColumnIndex( parameters.getWeightColumn() );
        table.sortTable( -1, true );
        MemoryHubGraph<N> addHub = table.stream().flatMap( row -> {
            Object[] values = row.getValues();
            List<N> fromNodes = TableDataCollectionUtils.splitToStream( String.valueOf( values[fromIndex] ) )
                    .map( Element::new ).map( converter::toNode ).toList();
            List<N> toNodes = TableDataCollectionUtils.splitToStream( String.valueOf( values[toIndex] ) )
                    .map( Element::new ).map( converter::toNode ).toList();
            UserHubEdge edge = new UserHubEdge( UserHubEdge.USER_REACTION_PREFIX + row.getName(), String.valueOf( values[fromIndex] ),
                    String.valueOf( values[toIndex] ) );
            float weight = weightIndex == -1 ? 1.0f : toWeight( values[weightIndex] );
            return StreamEx.of( fromNodes ).cross( toNodes )
                    .<HubRelation<N>>mapKeyValue( (fromNode, toNode) -> new HubRelation<>( fromNode, toNode, edge, weight ) );
        } ).collect( MemoryHubGraph.toMemoryHub() );
        return new DelegatingGraph<N>(graph)
        {
            @Override
            public StreamEx<N> nodes()
            {
                return origin.nodes().append( addHub.nodes() ).distinct();
            }

            @Override
            public StreamEx<N> startingNodes()
            {
                return origin.startingNodes().append( addHub.startingNodes() ).distinct();
            }

            @Override
            public boolean hasNode(N node)
            {
                return origin.hasNode( node ) || addHub.hasNode( node );
            }

            @Override
            public void visitEdges(N start, boolean upstream, HubEdgeVisitor<N> visitor)
            {
                Set<N> redefinedEnds = new HashSet<>();
                addHub.visitEdges( start, upstream, (edge, otherEnd, weight) -> {
                    redefinedEnds.add( otherEnd );
                });
                origin.visitEdges( start, upstream, (edge, otherEnd, weight) -> {
                    if(!redefinedEnds.contains( otherEnd ))
                        visitor.accept( edge, otherEnd, weight );
                });
                addHub.visitEdges( start, upstream, visitor );
            }
        };
    }

    @Override
    public Class<AddInteractionsDecoratorParameters> getParametersClass()
    {
        return AddInteractionsDecoratorParameters.class;
    }

    @SuppressWarnings ( "serial" )
    public static class AddInteractionsDecoratorParameters extends GraphDecoratorParameters
    {
        private DataElementPath table;
        private String fromColumn = ColumnNameSelector.NONE_COLUMN, toColumn = ColumnNameSelector.NONE_COLUMN,
                weightColumn = ColumnNameSelector.NONE_COLUMN;

        @PropertyName ( "Interations table" )
        public DataElementPath getTable()
        {
            return table;
        }

        public void setTable(DataElementPath table)
        {
            Object oldValue = this.table;
            this.table = table;
            firePropertyChange( "table", oldValue, this.table );
            ColumnNameSelector.updateColumns( this );
        }

        @PropertyName ( "Reactant column" )
        @PropertyDescription ( "Column containing IDs of input molecules" )
        public String getFromColumn()
        {
            return fromColumn;
        }

        public void setFromColumn(String fromColumn)
        {
            Object oldValue = this.fromColumn;
            this.fromColumn = fromColumn;
            firePropertyChange( "fromColumn", oldValue, this.fromColumn );
        }

        @PropertyName ( "Product column" )
        @PropertyDescription ( "Column containing IDs of output molecules" )
        public String getToColumn()
        {
            return toColumn;
        }

        public void setToColumn(String toColumn)
        {
            Object oldValue = this.toColumn;
            this.toColumn = toColumn;
            firePropertyChange( "toColumn", oldValue, this.toColumn );
        }

        @PropertyName ( "Weight column" )
        @PropertyDescription ( "Column containing weights. Will be set to 1.0 if not specified." )
        public String getWeightColumn()
        {
            return weightColumn;
        }

        public void setWeightColumn(String weightColumn)
        {
            Object oldValue = this.weightColumn;
            this.weightColumn = weightColumn;
            firePropertyChange( "weightColumn", oldValue, this.weightColumn );
        }
    }

    public static class AddInteractionsDecoratorParametersBeanInfo extends BeanInfoEx2<AddInteractionsDecoratorParameters>
    {
        public AddInteractionsDecoratorParametersBeanInfo()
        {
            super( AddInteractionsDecoratorParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "table" ).inputElement( TableDataCollection.class ).add();
            add( ColumnNameSelector.registerSelector( "fromColumn", beanClass, "table", false ) );
            add( ColumnNameSelector.registerSelector( "toColumn", beanClass, "table", false ) );
            add( ColumnNameSelector.registerNumericSelector( "weightColumn", beanClass, "table", true ) );
        }
    }
}
