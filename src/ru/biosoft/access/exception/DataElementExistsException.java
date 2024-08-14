package ru.biosoft.access.exception;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionDescriptor;

/**
 * @author lan
 *
 */
public class DataElementExistsException extends RepositoryException
{
    public static final ExceptionDescriptor ED_EXISTS = new ExceptionDescriptor( "Exists",
            LoggingLevel.Summary, "Element already exists: $path$");

    public DataElementExistsException(DataElementPath path)
    {
        super(null, ED_EXISTS, path);
    }
}
