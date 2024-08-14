package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * @author lan
 *
 */
public class WebProviderFactory
{
    private static ObjectExtensionRegistry<WebProvider> providers = new ObjectExtensionRegistry<>("ru.biosoft.server.servlets.webProvider", "prefix", WebProvider.class);
    
    public static WebProvider getProvider(String name)
    {
        return providers.getExtension(name);
    }
}
