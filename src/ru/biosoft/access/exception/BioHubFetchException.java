package ru.biosoft.access.exception;

import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class BioHubFetchException extends LoggedException
{
    private static final String KEY_BIOHUB = "biohub";

    public static final ExceptionDescriptor ED_BIOHUB = new ExceptionDescriptor( "Fetch", LoggingLevel.Trace,
            "Cannot fetch references via BioHub '$biohub$'");

    public BioHubFetchException(Throwable t, BioHub hub)
    {
        super(ExceptionRegistry.translateException(t), ED_BIOHUB);
        properties.put( KEY_BIOHUB, hub.getName() );
    }
}
