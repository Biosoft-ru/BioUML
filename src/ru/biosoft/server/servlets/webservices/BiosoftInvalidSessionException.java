package ru.biosoft.server.servlets.webservices;

import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionDescriptor;

public class BiosoftInvalidSessionException extends LoggedException
{
    private static final String KEY_SESSION_ID = "sessionId";

    public static final ExceptionDescriptor ED_INVALID_SESSION = new ExceptionDescriptor( "InvalidSession", LoggingLevel.Summary,
            "Invalid session $sessionId$");

    public BiosoftInvalidSessionException(String sessionId)
    {
        super(ED_INVALID_SESSION);
        properties.put( KEY_SESSION_ID, sessionId );
    }
}
