package ru.biosoft.util;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import ru.biosoft.exception.ExceptionRegistry;

/**
 * Additional methods missing in Class class useful for streams processing
 * @author lan
 *
 * @param <T>
 */
public class Clazz<T>
{
    private final Class<T> clazz;

    public Clazz(Class<T> clazz)
    {
        this.clazz = clazz;
    }

    @SuppressWarnings ( "unchecked" )
    public Stream<Class<? extends T>> selectClass(Class<?> clazz)
    {
        if(this.clazz.isAssignableFrom( clazz ))
        {
            return Stream.of( (Class<? extends T>)clazz );
        }
        return Stream.empty();
    }

    @SuppressWarnings ( "unchecked" )
    public <TT> ToIntFunction<TT> toInt(ToIntFunction<T> f, int defValue)
    {
        return obj -> {
            if(clazz.isInstance( obj ))
            {
                return f.applyAsInt( (T)obj );
            }
            return defValue;
        };
    }

    @SuppressWarnings ( "unchecked" )
    public <TT> ToDoubleFunction<TT> toDouble(ToDoubleFunction<T> f, double defValue)
    {
        return obj -> {
            if(clazz.isInstance( obj ))
            {
                return f.applyAsDouble( (T)obj );
            }
            return defValue;
        };
    }

    public <TT,R> Function<TT,R> toObj(Function<T,R> f, Function<TT, R> defValue)
    {
        return obj -> {
            if(clazz.isInstance( obj ))
            {
                return f.apply( (T)obj );
            }
            return defValue.apply( obj );
        };
    }

    public T create(Class<? extends T> clazz)
    {
        try
        {
            return clazz.newInstance();
        }
        catch( InstantiationException | IllegalAccessException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    public T createOrLog(Class<? extends T> clazz)
    {
        try
        {
            return clazz.newInstance();
        }
        catch( InstantiationException | IllegalAccessException e )
        {
            ExceptionRegistry.log(e);
            return null;
        }
    }

    public static <T> Clazz<T> of(Class<T> clazz)
    {
        return new Clazz<>(clazz);
    }
}