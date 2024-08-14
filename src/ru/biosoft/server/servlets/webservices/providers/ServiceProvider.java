package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.server.Request;
import ru.biosoft.server.Service;
import ru.biosoft.server.ServiceRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;

/**
 * @author lan
 *
 */
public class ServiceProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String serviceName = Request.getService(arguments.getArguments());
        Service service = ServiceRegistry.getService(serviceName);
        Integer command = Request.getCommand(arguments.getArguments());

        if( service != null && command != null )
        {
            service.processRequest(command, arguments.getArguments(), response);
        }
        else
        {
            if(serviceName == null)
                throw new WebException("EX_QUERY_PARAM_NO_SERVICE", "null");
            else
                throw new WebException("EX_QUERY_PARAM_NO_SERVICE", serviceName+"->"+command);
        }
    }
}
