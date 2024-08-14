package ru.biosoft.server;

import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * Service registry
 */
public class ServiceRegistry
{
    public static final String NAME_ATTRIBUTE = "name";

    private static ObjectExtensionRegistry<Service> instance = new ObjectExtensionRegistry<>( "ru.biosoft.server.service",
            NAME_ATTRIBUTE, Service.class);
    
    /**
     * Return service by name
     */
    public static Service getService(String name)
    {
        if( name == null )
            return null;
        return instance.getExtension(name);
    }
}
