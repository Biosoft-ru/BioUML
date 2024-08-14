package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;

/**
 * @author lan
 *
 */
public abstract class WebJSONProviderSupport extends WebProviderSupport
{
    public abstract void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception;

    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        process(arguments, new JSONResponse(resp));
    }
}
