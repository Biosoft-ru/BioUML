package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;

/**
 * @author lan
 */
public interface WebProvider
{
    /**
     * Handle request
     * @param arguments - web arguments
     * @param header - response object (to modify headers, get output stream, etc.)
     */
    public void process(BiosoftWebRequest req, BiosoftWebResponse resp) throws Exception;
}
