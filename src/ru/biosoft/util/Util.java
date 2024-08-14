package ru.biosoft.util;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import one.util.streamex.DoubleCollector;

public class Util
{
    public static final Predicate<Object> TRUE = a -> true;
    public static final Predicate<Object> FALSE = a -> false;

    public static double max(double a, double b)
    {
        return a > b ? a : b;
    }

    public static double max(double a, double b, double c)
    {
        return a > b ? ( a > c ? a : c ) : ( b > c ? b : c );
    }

    private static final AtomicLong lastId = new AtomicLong( 0 );
    public static long getUniqueId()
    {
        return lastId.incrementAndGet();
    }

    public static <T> Predicate<T> safePredicate(Predicate<T> predicate)
    {
        return safePredicate( predicate, (t, e) -> {
        } );
    }

    public static <T> Predicate<T> safePredicate(Predicate<T> predicate, BiConsumer<T, Throwable> onError)
    {
        return val -> {
            try
            {
                return predicate.test( val );
            }
            catch( Throwable t )
            {
                onError.accept( val, t );
                return false;
            }
        };
    }

    public static <T, R> Function<T, R> safeFunction(Function<T, R> function, R defaultValue, BiConsumer<T, Throwable> onError)
    {
        return val -> {
            try
            {
                return function.apply( val );
            }
            catch( Throwable t )
            {
                onError.accept( val, t );
                return defaultValue;
            }
        };
    }

    public static DoubleCollector<?, OptionalDouble> median()
    {
        return DoubleCollector.toArray().andThen( arr -> {
            Arrays.sort( arr );
            if( arr.length == 0 )
                return OptionalDouble.empty();
            if( arr.length % 2 == 0 )
                return OptionalDouble.of( ( arr[arr.length / 2] + arr[arr.length / 2 - 1] ) / 2 );
            return OptionalDouble.of( arr[arr.length / 2] );
        } );
    }
}
