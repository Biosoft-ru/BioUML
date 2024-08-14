package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;

/**
 * @author lan
 *
 */
public class RevertRequestProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        DataElementPath dePath = arguments.getDataElementPath();
        SessionCache sessionCache = WebServicesServlet.getSessionCache();
        Object object = sessionCache.getObject(dePath.toString());
        if(object != null)
        {
            SecurityManager.removeObjectFromUserCaches(object, dePath.toString());
            if(object instanceof DataElement)
            {
                try
                {
                    ( (DataElement)object ).getOrigin().getCompletePath().getDataCollection().release( ( (DataElement)object ).getName());
                }
                catch( Exception e )
                {
                }
            }
            synchronized(object)
            {
                object.notifyAll();
            }
        }
        sessionCache.removeObject("DiagramView/"+dePath);
        //TODO: make more correct removal of cached beans 
        sessionCache.removeObject( "diagram/plot/" + dePath );
        response.sendString("ok");
    }
}
