package ru.biosoft.util;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import ru.biosoft.exception.InternalException;

public class Cache<K, V>
{
    private final Function<V, Reference<V>> refSupplier;
    private final Function<K, V> fn;
    private final Map<K, Holder<V>> map = new ConcurrentHashMap<>();
    
    private static class Holder<V>
    {
        private volatile Reference<V> ref;
        
        public V fetch(Function<V, Reference<V>> refSupplier, Supplier<V> supplier)
        {
            V result = ref == null ? null : ref.get();
            if(result != null)
                return result;
            synchronized(this)
            {
                result = ref == null ? null : ref.get();
                if(result == null)
                {
                    result = supplier.get();
                    ref = refSupplier.apply( result );
                }
                return result;
            }
        }
    }

    private Cache(Function<K, V> fn, Function<V, Reference<V>> refSupplier)
    {
        this.refSupplier = refSupplier;
        this.fn = fn;
    }
    
    public V get(K key)
    {
        V res = map.computeIfAbsent( key, k -> new Holder<>() ).fetch( refSupplier, () -> fn.apply( key ) );
        if(res == null)
            throw new InternalException( "null object is returned for key "+key );
        return res;
    }
    
    public static <K, V> Function<K, V> weak(Function<K, V> fn)
    {
        return new Cache<>( fn, WeakReference<V>::new )::get;
    }
    
    public static <K, V> Function<K, V> soft(Function<K, V> fn)
    {
        return new Cache<>( fn, SoftReference<V>::new )::get;
    }
    
    public static <K, V> Function<K, V> hard(Function<K, V> fn)
    {
        ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
        return k -> map.computeIfAbsent( k, fn );
    }
    
    public static <V> Supplier<V> weak(Supplier<V> supplier)
    {
        return single(supplier, WeakReference<V>::new);
    }
    
    public static <V> Supplier<V> soft(Supplier<V> supplier)
    {
        return single(supplier, SoftReference<V>::new);
    }
    
    private static <V> Supplier<V> single(Supplier<V> supplier, Function<V, Reference<V>> refSupplier)
    {
        Holder<V> holder = new Holder<>();
        return () -> holder.fetch( refSupplier, supplier );
    }
}
