package ru.biosoft.access.exception;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ExceptionRegistry;

public class DataElementNotAcceptableException extends RepositoryException
{
    public static final ExceptionDescriptor ED_ELEMENT_NOT_ACCEPTABLE = new ExceptionDescriptor( "InvalidElement", LoggingLevel.Summary,
            "$path$ is not acceptable: $reason$");

    private static String KEY_REASON = "reason";

    public DataElementNotAcceptableException(DataElementPath path, String reason)
    {
        super(ED_ELEMENT_NOT_ACCEPTABLE, path);
        properties.put( KEY_REASON, reason );
    }

    public DataElementNotAcceptableException(Throwable t, DataElementPath path, String reason)
    {
        super(ExceptionRegistry.translateException(t), ED_ELEMENT_NOT_ACCEPTABLE, path);
        properties.put( KEY_REASON, reason );
    }
}
