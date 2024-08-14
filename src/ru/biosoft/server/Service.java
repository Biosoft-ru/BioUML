package ru.biosoft.server;

import java.util.Map;

/**
 * Defines public interface to process requests in the server side.
 */
public interface Service
{
    /**
     * Process request
     * @param command - command id
     * @param data - send arguments
     * @param out - special output stream wrapper
     */
    public void processRequest(Integer command, Map data, Response out);
}
