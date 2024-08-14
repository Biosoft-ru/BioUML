package ru.biosoft.table;

import java.util.List;
import java.util.stream.Collector;

import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.QueryFilter;

/**
 * @author lan
 *
 */
public class RowCountFilter implements QueryFilter<DataElement>
{
    private final RowJSExpression expression;
    private final int count;
    private final boolean lowest;

    public RowCountFilter(DataCollection<?> dc, String filterStr, int count, boolean lowest) throws IllegalArgumentException
    {
        this.expression = new RowJSExpression(filterStr, dc);
        this.count = count;
        this.lowest = lowest;
    }
    
    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public boolean isAcceptable(DataElement de)
    {
        return true;
    }
    
    private static class Pair implements Comparable<Pair>
    {
        private final String key;
        private final Object value;

        public Pair(String key, Object value)
        {
            super();
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        @SuppressWarnings ( {"rawtypes", "unchecked"} )
        @Override
        public int compareTo(Pair o)
        {
            if(!(o.value instanceof Comparable))
            {
                return (value instanceof Comparable)?-1:0;
            }
            if(!(value instanceof Comparable)) return 1;
            try
            {
                return ((Comparable<Comparable>)value).compareTo((Comparable<Comparable>)o.value);
            }
            catch( Exception e )
            {
                return 0;
            }
        }
    }

    @Override
    public List<String> doQuery(DataCollection<? extends DataElement> dc)
    {
        if(dc.getSize() <= count) return dc.getNameList();
        Collector<Pair, ?, List<Pair>> collector = lowest ? MoreCollectors.least( count ) : MoreCollectors.greatest( count );
        List<Pair> filterVals = dc.stream().map( de -> new Pair( de.getName(), expression.evaluate( de ) ) ).collect( collector );
        return StreamEx.of( filterVals ).map( Pair::getKey ).toList();
    }
}
