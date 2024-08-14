package ru.biosoft.access.exception;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * @author lan
 *
 */
public class SearchException extends RepositoryException
{
    private static final String KEY_QUERY = "query";

    public static final ExceptionDescriptor ED_SEARCH = new ExceptionDescriptor( "Search", LoggingLevel.TraceIfNoCause,
            "Error searching for '$query$' in $path$");

    public SearchException(Throwable t, DataElementPath path, String query)
    {
        super(ExceptionRegistry.translateException(t), ED_SEARCH, path);
        properties.put( KEY_QUERY, query );
    }
}
