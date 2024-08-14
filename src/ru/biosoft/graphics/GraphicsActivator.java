package ru.biosoft.graphics;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import ru.biosoft.graphics.chart.ChartConnection;

/**
 * @author lan
 *
 */
public class GraphicsActivator implements BundleActivator
{
    private ServiceRegistration<?> matrixURLStreamHandlerService = null;

    @Override
    public void start(BundleContext bc) throws Exception
    {
        Hashtable<String,String[]> properties = new Hashtable<>();
        properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] {"chart"});
        matrixURLStreamHandlerService = bc.registerService(URLStreamHandlerService.class.getName(), new AbstractURLStreamHandlerService()
        {
            @Override
            public URLConnection openConnection(URL u) throws IOException
            {
                return new ChartConnection(u);
            }
        }, properties);
    }

    @Override
    public void stop(BundleContext arg0) throws Exception
    {
        if( matrixURLStreamHandlerService != null )
        {
            matrixURLStreamHandlerService.unregister();
            matrixURLStreamHandlerService = null;
        }
    }
}
