package biouml.plugins.keynodes.graph;

import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.keynodes.KeyNodeAnalysisParameters;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ApplyContextDecorator implements GraphDecorator<ApplyContextDecorator.ApplyContextDecoratorParameters>
{
    @Override
    public <N> HubGraph<N> decorate(HubGraph<N> graph, ElementConverter<N> converter, ApplyContextDecoratorParameters properties)
    {
        TableDataCollection secondarySet = Assert.notNull( "Table name", properties.getTableName() ).getDataElement(
                TableDataCollection.class );
        String weightColumn = properties.getTableColumn();
        double decayFactor = properties.getDecayFactor();
        int direction = properties.getDirection();
        int columnIndex = -1;
        if( weightColumn != null && !weightColumn.equals("") && !weightColumn.equals(ColumnNameSelector.NONE_COLUMN) )
        {
            columnIndex = secondarySet.getColumnModel().optColumnIndex(weightColumn);
        }
        TObjectDoubleMap<N> weights = new TObjectDoubleHashMap<>();
        for(RowDataElement row : secondarySet)
        {
            double weight = 1.0;
            if( columnIndex != -1 )
                weight = (Double)DataType.Float.convertValue(row.getValues()[columnIndex]);
            weights.put( converter.toNode( new Element(row.getName()) ), weight );
        }
        return applyContext( graph, weights, decayFactor, direction );
    }

    public static <N> HubGraph<N> applyContext(HubGraph<N> m, TObjectDoubleMap<N> weights, double decayFactor, int direction)
    {
        normalizeWeights( weights );
        Map<N, Map<N, Double>> mOut = propagate( m, weights, decayFactor, direction );
        double max = Math.max( 1.0, mOut.values().stream()
                .mapToDouble( map -> map.values().stream().mapToDouble( Double::doubleValue ).max().orElse( 0.0 ) ).max().orElse( 0.0 ) );
        return new DelegatingGraph<N>(m)
        {
            @Override
            public void visitEdges(N from, boolean upstream, HubEdgeVisitor<N> visitor)
            {
                origin.visitEdges( from, upstream, (edge, to, v) -> {
                    Double w = upstream ?
                            mOut.getOrDefault( to, Collections.emptyMap() ).get(from)
                            :
                            mOut.getOrDefault( from, Collections.emptyMap() ).get(to);
                    visitor.accept( edge, to, w == null ? v : (float)(v * ( 1.0 - w / max )) );
                });
            }
        };
    }
    
    private static <N> void propagateUpstream(Map<N, Map<N, Double>> mOut, HubGraph<N> m, TObjectDoubleMap<N> weights, double decay)
    {
        Map<N, Set<N>> mProp = new HashMap<>();
        //init
        weights.forEachEntry( (from, weight) -> {
            Set<N> propSet = mProp.computeIfAbsent( from, k -> new HashSet<>() );
            m.visitEdges( from, true, (edge, to, w) -> {
                mOut.computeIfAbsent( to, k -> new HashMap<>() ).put( from, weight );
                propSet.add( to );
            });
            return true;
        });
        Map<N, Set<N>> visited = new HashMap<>();
        //iterate forward
        int maxiter = 4;
        double d = 0.1;
        for( int j = 0; j < maxiter; j++ )
        {
            d *= decay;
            double curD = d;
            Map<N, Set<N>> mProp2 = new HashMap<>();
            
            for(Set<N> targets : mProp.values())
            {
                for(N to : targets)
                {
                    Set<N> propSet = mProp2.computeIfAbsent( to, k -> new HashSet<>() );
                    Set<N> visitedByNode = visited.computeIfAbsent( to, k -> new HashSet<>() );
                    m.visitEdges( to, true, (edge, tt, w) ->
                    {
                        if( !visitedByNode.contains( tt ) )
                        {
                            mOut.computeIfAbsent( tt, k -> new HashMap<>() ).compute( to, (k, v) -> v == null ? curD : v + curD );
                            propSet.add( tt );
                            visitedByNode.add( tt );
                        }
                    });
                }
            }

            transfer( mProp, mProp2 );
            visited.clear();
        }
    }

    private static <N> void propagateDownstream(Map<N, Map<N, Double>> mOut, HubGraph<N> m, TObjectDoubleMap<N> weights, double decay)
    {
        Map<N, Set<N>> mProp = new HashMap<>();

        //init
        weights.forEachEntry( (from, weight) -> {
            Set<N> propSet = mProp.computeIfAbsent( from, k -> new HashSet<>() );
            Map<N, Double> outMap = mOut.computeIfAbsent( from, k -> new HashMap<>() );
            m.visitEdges( from, false, (edge, to, w) -> {
                outMap.put( to, weight );
                propSet.add( to );
            });
            return true;
        });
        //iterate forward
        int maxiter = 4;
        double d = 0.1;
        Map<N, Set<N>> visited = new HashMap<>();
        for( int j = 0; j < maxiter; j++ )
        {
            d *= decay;
            double curD = d;
            Map<N, Set<N>> mProp2 = new HashMap<>();
            
            for(Set<N> targets : mProp.values())
            {
                for(N to : targets)
                {
                    Map<N, Double> outMap = mOut.computeIfAbsent( to, k -> new HashMap<>() );
                    Set<N> visitedByNode = visited.computeIfAbsent( to, k -> new HashSet<>() );
                    Set<N> propSet = mProp2.computeIfAbsent( to, k -> new HashSet<>() );
                    m.visitEdges( to, false, (edge, tt, w) ->
                    {
                        if( !visitedByNode.contains( tt ) )
                        {
                            outMap.compute( tt, (k, v) -> v == null ? curD : v + curD );
                            propSet.add( tt );
                            visitedByNode.add( tt );
                        }
                    });
                }
            }
            transfer( mProp, mProp2 );
            visited.clear();
        }
    }

    private static <N> void transfer(Map<N, Set<N>> mProp, Map<N, Set<N>> mProp2)
    {
        for(Entry<N, Set<N>> entry : mProp2.entrySet())
        {
            N from = entry.getKey();
            Set<N> propSet = null;
            for(N to : entry.getValue())
            {
                if(propSet == null)
                    propSet = mProp.computeIfAbsent( from, k -> new HashSet<>() );
                propSet.add( to );
            }
        }
    }

    public static <N> Map<N, Map<N, Double>> propagate(HubGraph<N> m, TObjectDoubleMap<N> weights, double decay, int direction)
    {
        Map<N, Map<N, Double>> mOut = new HashMap<>();
        switch(direction)
        {
            case BioHub.DIRECTION_UP:
                propagateUpstream(mOut, m, weights, decay);
                break;
            case BioHub.DIRECTION_DOWN:
                propagateDownstream(mOut, m, weights, decay);
                break;
            case BioHub.DIRECTION_BOTH:
                propagateUpstream(mOut, m, weights, decay);
                propagateDownstream(mOut, m, weights, decay);
                break;
            default:
                throw new ParameterNotAcceptableException("direction", String.valueOf(direction));
        }
        return mOut;
    }

    private static void normalizeWeights(TObjectDoubleMap<?> weights)
    {
        ApplyContextDecorator.WeightNormalizer normalizer = new ApplyContextDecorator.WeightNormalizer( weights.values() );
        weights.transformValues( normalizer::normalize );
    }

    @Override
    public boolean isAcceptable(KeyNodesHub<?> hub)
    {
        return true;
    }

    @Override
    public Class<ApplyContextDecoratorParameters> getParametersClass()
    {
        return ApplyContextDecoratorParameters.class;
    }
    
    @SuppressWarnings ( "serial" )
    public static class ApplyContextDecoratorParameters extends GraphDecoratorParameters
    {
        private int direction;
        private DataElementPath tableName;
        private String tableColumn = ColumnNameSelector.NONE_COLUMN;
        private double decayFactor = 0.1;

        @Override
        public void setKeyNodeParameters(KeyNodeAnalysisParameters params)
        {
            this.direction = params.getReverseDirection();
        }
        
        public void setDirection(int direction)
        {
            Object oldValue = this.direction;
            this.direction = direction;
            firePropertyChange( "direction", oldValue, this.direction );
        }

        public int getDirection()
        {
            return direction;
        }

        @PropertyName("Context table")
        public DataElementPath getTableName()
        {
            return tableName;
        }
        public void setTableName(DataElementPath tableName)
        {
            Object oldValue = this.tableName;
            this.tableName = tableName;
            firePropertyChange( "tableName", oldValue, this.tableName );
            setTableColumn( ColumnNameSelector.NONE_COLUMN );
        }
        
        @PropertyName("Weights column")
        public String getTableColumn()
        {
            return tableColumn;
        }
        public void setTableColumn(String tableColumn)
        {
            Object oldValue = this.tableColumn;
            this.tableColumn = tableColumn;
            firePropertyChange( "tableColumn", oldValue, this.tableColumn );
        }
        
        @PropertyName("Decay factor")
        public double getDecayFactor()
        {
            return decayFactor;
        }
        public void setDecayFactor(double decayFactor)
        {
            Object oldValue = this.decayFactor;
            this.decayFactor = decayFactor;
            firePropertyChange( "decayFactor", oldValue, this.decayFactor );
        }
    }
    
    public static class ApplyContextDecoratorParametersBeanInfo extends BeanInfoEx2<ApplyContextDecoratorParameters>
    {
        public ApplyContextDecoratorParametersBeanInfo()
        {
            super(ApplyContextDecoratorParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput( "tableName", beanClass, TableDataCollection.class ));
            add(ColumnNameSelector.registerNumericSelector( "tableColumn", beanClass, "tableName", true ));
            add("decayFactor");
            addHidden("direction");
            findPropertyDescriptor( "direction" ).setValue( TextUtil2.SERIALIZABLE_PROPERTY, true );
        }
    }

    public static class WeightNormalizer
    {
        private double vmin;
        private final double delta;
    
        public WeightNormalizer(double[] weights)
        {
            DoubleSummaryStatistics stats = Arrays.stream(weights).filter( Double::isFinite ).summaryStatistics();
            double vmax = stats.getMax();
            vmin = stats.getMin();
            if( Double.isInfinite( vmax ) || Double.isInfinite( vmin ) )
                vmax = vmin = 0;
            else if( vmin == vmax || vmin > 0 )
                vmin = 0;
            delta = vmax - vmin;
        }
        
        public double normalize(double weight)
        {
            return Double.isFinite( weight ) && delta >= 0 ? ( weight - vmin ) / delta : 1.0;
        }
    }
}
