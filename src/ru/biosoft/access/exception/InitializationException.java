package ru.biosoft.access.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class InitializationException extends LoggedException
{
    private static final String KEY_CAUSE = "cause";

    public static final ExceptionDescriptor ED_INIT = new ExceptionDescriptor( "Common", LoggingLevel.TraceIfNoCause,
            "Error during the initialization of $cause$");

    public InitializationException(Throwable t, String cause)
    {
        super(ExceptionRegistry.translateException(t), ED_INIT);
        properties.put( KEY_CAUSE, cause );
    }

    public InitializationException(String cause)
    {
        this(null, cause);
    }
}
