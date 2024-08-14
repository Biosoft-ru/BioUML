package ru.biosoft.access.exception;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import ru.biosoft.exception.ExceptionTranslator;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.exception.LoggedException;

public class BiosoftExceptionTranslator implements ExceptionTranslator
{

    @Override
    public LoggedException translateException(Throwable t)
    {
        if( t == null )
            return null;

        if( t instanceof InvocationTargetException || t instanceof ExecutionException )
            t = t.getCause();

        if( t instanceof LoggedException )
            return (LoggedException)t;

        if( t instanceof ClassNotFoundException || t instanceof NoClassDefFoundError )
            return new LoggedClassNotFoundException( t );

        if( t instanceof NullPointerException )
            return new BiosoftNullException( t );

        if( t instanceof OutOfMemoryError )
            return new BiosoftMemoryException( t );

        if( t instanceof SQLException )
            return new BiosoftSQLException( (SQLException)t );

        if( t instanceof FileNotFoundException )
            return new BiosoftFileNotFoundException( (FileNotFoundException)t );

        if( t instanceof IOException && "No space left on device".equals( t.getMessage() ) )
            return new BiosoftCustomException( t, "Not enough disk space" );

        if( t.getClass().equals( Exception.class ) || t instanceof SecurityException || t instanceof IllegalArgumentException )
            return new BiosoftCustomException( t );

        return new InternalException( t );
    }

}
