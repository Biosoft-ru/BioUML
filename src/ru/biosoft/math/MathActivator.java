package ru.biosoft.math;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import ru.biosoft.math.view.Handler;

/**
 * @author lan
 *
 */
public class MathActivator implements BundleActivator
{
    private ServiceRegistration formulaURLStreamHandlerService = null;

    @Override
    public void start(BundleContext bc) throws Exception
    {
        Hashtable properties = new Hashtable();
        properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] {"formula"});
        formulaURLStreamHandlerService = bc.registerService(URLStreamHandlerService.class.getName(), new Handler(), properties);
    }

    @Override
    public void stop(BundleContext arg0) throws Exception
    {
        if( formulaURLStreamHandlerService != null )
        {
            formulaURLStreamHandlerService.unregister();
            formulaURLStreamHandlerService = null;
        }
    }
}
