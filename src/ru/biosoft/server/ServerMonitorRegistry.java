package ru.biosoft.server;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.ClassLoading;

/**
 * Registry for server monitor plugins.
 * Loads services from the biouml.plugins.servermonitor extension point
 * and calls their init(Properties) method at server startup.
 */
public class ServerMonitorRegistry
{
    private static Logger log = Logger.getLogger(ServerMonitorRegistry.class.getName());
    private static final Set<String> initialized = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /**
     * Initialize all server monitor plugins registered via the extension point.
     * Idempotent — safe to call multiple times; each service is initialized only once.
     */
    public static void init()
    {
        IExtensionRegistry registry = Application.getExtensionRegistry();
        if (registry == null)
        {
            log.log(Level.WARNING, "Extension registry not available, skipping server monitor initialization");
            return;
        }

        IConfigurationElement[] extensions = registry.getConfigurationElementsFor("biouml.plugins.servermonitor.serverMonitor");

        for (int i = 0; i < extensions.length; i++)
        {
            String pluginId = extensions[i].getNamespaceIdentifier();
            String className = extensions[i].getAttribute("class");
            if (className == null)
            {
                log.log(Level.WARNING, "class attribute not specified in extension " + extensions[i].getName());
                continue;
            }

            // Idempotent: skip if already initialized
            if (initialized.contains(className))
            {
                log.fine("ServerMonitorRegistry: already initialized " + className + ", skipping");
                continue;
            }

            try
            {
                Class<?> type = ClassLoading.loadClass(className, pluginId);
                if (type != null)
                {
                    Object service = type.newInstance();
                    Method m = type.getMethod("init", new Class[] { Properties.class });
                    m.invoke(service, new Object[] { new Properties() });

                    initialized.add(className);
                    log.info("ServerMonitorRegistry: initialized " + className);
                }
            }
            catch (Throwable t)
            {
                log.log(Level.SEVERE, "Cannot load server monitor service, extension=" + extensions[i].getName()
                        + ", plugin: " + pluginId + ", error: " + t, t);
            }
        }
    }
}
