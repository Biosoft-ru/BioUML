package ru.biosoft.access.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

public class BiosoftNullException extends LoggedException
{
    public static final ExceptionDescriptor ED_NPE = new ExceptionDescriptor( "NPE", LoggingLevel.Summary,
            "Internal error occured (null pointer exception).");

    public BiosoftNullException(Throwable t)
    {
        super(t, ED_NPE);
    }
}