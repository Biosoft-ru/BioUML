package ru.biosoft.access.exception;

import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionDescriptor;
import biouml.standard.type.access.TitleIndex;

/**
 * @author lan
 */
@SuppressWarnings ( "serial" )
public class IndexCreateException extends RepositoryException
{
    private static final String KEY_INDEX = "index";

    public static final ExceptionDescriptor ED_CANNOT_CREATE_INDEX = new ExceptionDescriptor( "CannotCreateIndex",
            LoggingLevel.Summary, "Cannot create index '$index$' for $path$");

    /**
     * @param t - cause
     * @param path - full path to the element which cannot be created
     * @param clazz - class of element you tried to create
     */
    public IndexCreateException(Throwable t, TitleIndex index)
    {
        super(t, ED_CANNOT_CREATE_INDEX, index.getOwner().getCompletePath());
        properties.put( KEY_INDEX, index.getName() );
    }
}
