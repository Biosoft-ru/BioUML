package ru.biosoft.access.task;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.Iteration;

@FunctionalInterface
public interface ExceptionalConsumer<T>
{
    void accept(T t) throws Exception;

    static <T> Iteration<T> iteration(ExceptionalConsumer<T> c)
    {
        return t -> {
            try
            {
                c.accept( t );
            }
            catch( Throwable e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return true;
        };
    }
}
