package ru.biosoft.access.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

public class BiosoftCustomException extends LoggedException
{
    private static final String KEY_MESSAGE = "message";
    public static final ExceptionDescriptor ED_CUSTOM = new ExceptionDescriptor( "Custom", LoggingLevel.Summary,
            "$message$");

    public BiosoftCustomException(Throwable t)
    {
        this(t, t.getMessage());
    }

    public BiosoftCustomException(Throwable t, String message)
    {
        super(t, ED_CUSTOM);
        properties.put( KEY_MESSAGE, message );
    }
}