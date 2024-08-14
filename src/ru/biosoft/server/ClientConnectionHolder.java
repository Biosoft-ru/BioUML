package ru.biosoft.server;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.LoggedException;

/**
 * Data element which holds a client connection
 */
public interface ClientConnectionHolder extends DataElement
{
    public @Nonnull ClientConnection getClientConnection() throws LoggedException;
}
