package biouml.plugins.gtrd.master.index;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

import ru.biosoft.bsa.Site;
import ru.biosoft.rtree.RTree;

public class OverlapUtils
{
    //ys - must be sorted by from
    //interval nesting allowed
    //all sites on the same chr
    public static <S1 extends Site, S2 extends Site> void mapOverlapping(List<S1> xs, List<S2> ys, int distance, BiConsumer<S1, S2> f)
    {
        RTree index = new RTree();
        index.build( new ListOfSitesWrapper( ys ) );
        for(S1 x : xs)
            index.findOverlapping( x.getFrom()-distance, x.getTo()+distance, idx->f.accept( x, ys.get( idx ) ) );
    }
    
    //same as above, but with more general result reporting 
    public static <S1 extends Site, S2 extends Site, R,A> void mapOverlapping(List<S1> xs, List<S2> ys, int distance,
            Function<S1, Collector<? super S2,A,R>> collectorProvider,
            BiConsumer<S1, R> f)
    {
        RTree index = new RTree();
        index.build( new ListOfSitesWrapper( ys ) );
        
        class Consumer
        {
            Collector<? super S2,A,R> collector;
            A cur;
            BiConsumer<A, ? super S2> acc;
            
            void init(S1 x)
            {
                collector = collectorProvider.apply( x );
                acc = collector.accumulator();
                cur = collector.supplier().get();
            }
            boolean isInitialized()
            {
                return collector != null;
            }
            void reset()
            {
                collector = null;
            }
            
            void accept(S2 y)
            {
                acc.accept( cur, y);
            }
            
            R finish()
            {
                return collector.finisher().apply( cur );
            }
            
        }
        Consumer consumer = new Consumer();
        for(S1 x : xs)
        {
            index.findOverlapping( x.getFrom()-distance, x.getTo()+distance, idx -> {
                if(!consumer.isInitialized())
                    consumer.init( x );
                consumer.accept( ys.get( idx ) );
            } );
            if(consumer.isInitialized())
            {
                R res = consumer.finish();
                f.accept( x, res );
            }
            consumer.reset();
        }
    }
    
    //xs - sorted by from
    //ys - sorted by from
    //no interval contains another interval (means that order induced by Interval.from is the same as order induced by Interval.to)
    //all on the same chr
    public static <S1 extends Site, S2 extends Site> void mapOverlappingNestingForbidden(List<S1> xs, List<S2> ys, int distance, BiConsumer<S1, List<S2>> f)
    {
        checkSortedNoContainment(xs);
        checkSortedNoContainment(ys);
        int i = 0, j = 0;
        for(S1 x : xs)
        {
            while(i < ys.size() && ys.get( i ).getTo() < x.getFrom() - distance)
                i++;
            if(i >= ys.size())
                return;
            
            if(i > j)
                j = i;
            
            while(j < ys.size() && ys.get( j ).getFrom() <= x.getTo() + distance)
                j++;
            
            if(j > i)
                f.accept( x, ys.subList( i, j ) );
        }
    }
    
    private static void checkSortedNoContainment(List<? extends Site> xs)
    {
        if(xs.size() < 2)
            return;
        Site prev = xs.get( 0 );
        for(int i = 1; i < xs.size(); i++)
        {
            Site cur = xs.get( i );
            if(prev.getFrom() > cur.getFrom() || prev.getTo() > cur.getTo())
                throw new IllegalArgumentException();
            prev = cur;
        }
    }
}
