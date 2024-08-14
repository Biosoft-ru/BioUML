package ru.biosoft.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Maps
{
    public static <K, V> Map<K, V> filterKeys(Map<K, V> src, Predicate<K> predicate)
    {
        return filterKeys( src, predicate, HashMap::new );
    }
    
    public static <K, V> Map<K, V> filterKeys(Map<K, V> src, Predicate<K> predicate, Supplier<? extends Map<K, V>> mapSupplier)
    {
        Map<K, V> result = mapSupplier.get();
        for( Entry<K, V> entry : src.entrySet() )
            if( predicate.test( entry.getKey() ) )
                result.put( entry.getKey(), entry.getValue() );
        return result;
    }
    
    public static <K, V> Map<K, V> filterValues(Map<K, V> src, Predicate<V> predicate)
    {
        return filterValues( src, predicate, HashMap::new );
    }
    
    public static <K, V> Map<K, V> filterValues(Map<K, V> src, Predicate<V> predicate, Supplier<? extends Map<K, V>> mapSupplier)
    {
        Map<K, V> result = mapSupplier.get();
        for( Entry<K, V> entry : src.entrySet() )
            if( predicate.test( entry.getValue() ) )
                result.put( entry.getKey(), entry.getValue() );
        return result;
    }
    
    public static <K, V, KK, VV> Map<KK, VV> transform(Map<K, V> src, Function<K, KK> keyMapper, Function<V, VV> valueMapper, Supplier<? extends Map<KK, VV>> mapSupplier)
    {
        Map<KK, VV> result = mapSupplier.get();
        for(Entry<K, V> entry : src.entrySet())
        {
            result.put( keyMapper.apply( entry.getKey() ), valueMapper.apply( entry.getValue() ) );
        }
        return result;
    }
    
    public static <K, V, KK, VV> Map<KK, VV> transform(Map<K, V> src, Function<K, KK> keyMapper, Function<V, VV> valueMapper)
    {
        return transform( src, keyMapper, valueMapper, HashMap::new );
    }
    
    public static <K, V, KK> Map<KK, V> transformKeys(Map<K, V> src, Function<K, KK> keyMapper)
    {
        return transform( src, keyMapper, Function.identity(), HashMap::new );
    }
    
    public static <K, V, VV> Map<K, VV> transformValues(Map<K, V> src, Function<V, VV> valueMapper)
    {
        return transform( src, Function.identity(), valueMapper, HashMap::new );
    }
}
