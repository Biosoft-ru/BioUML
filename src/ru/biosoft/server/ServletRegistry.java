package ru.biosoft.server;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.ClassLoading;

/**
 * Service registry
 */
public class ServletRegistry
{
    private static Map map = null;
    private static Logger log = Logger.getLogger(ServletRegistry.class.getName());

    /**
     * Should be called by server
     */
    public static void initServlets(String[] args)
    {
        if( map != null )
            return;

        loadExtensions("ru.biosoft.server.servlet", args);
    }

    /**
     * Return all servlets
     */
    public synchronized static Map getRegistry()
    {
        validate();
        return map;
    }

    // /////////////////////////////////////////////////////////////////
    // Protected functions
    //

    protected static void validate()
    {
        if( map == null )
            throw new RuntimeException("ServletRegistry was not initialised.");
    }

    /**
     * Load all executable extensions for the specified extension point.
     */
    protected static void loadExtensions(String extensionPointId, String[] args)
    {
        map = new HashMap();

        IExtensionRegistry registry = Application.getExtensionRegistry();
        if( registry == null )
        {
            return;
        }
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor(extensionPointId);

        for( int i = 0; i < extensions.length; i++ )
        {
            String pluginId = extensions[i].getNamespaceIdentifier();
            try
            {
                String prefix = extensions[i].getAttribute("prefix");
                if( prefix == null )
                    throw new Exception("prefix attribute is not specified");

                String className = extensions[i].getAttribute("class");
                if( className == null )
                    throw new Exception("class attribute is not specified");

                Class<?> type = ClassLoading.loadClass( className, pluginId );
                if( type != null )
                {
                    Object servlet = type.newInstance();
                    Method m = type.getMethod("init", new Class[] {String[].class});
                    m.invoke(servlet, new Object[] {args});
                    map.put(prefix, servlet);
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not load servlet, extension=" + extensions[i].getName() + ", plugin: " + pluginId + ", error: " + t + ".", t);
            }
        }
    }

}
