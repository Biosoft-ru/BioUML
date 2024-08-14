package ru.biosoft.access.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

public class BiosoftMemoryException extends LoggedException
{
    public static final ExceptionDescriptor ED_MEMORY = new ExceptionDescriptor( "Memory", LoggingLevel.Summary,
            "Operation cannot be performed due to lack of memory.");

    public BiosoftMemoryException(Throwable t)
    {
        super(t, ED_MEMORY);
    }
}