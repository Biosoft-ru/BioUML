package ru.biosoft.util;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Simple extension registry which instantiates objects of given class
 * Registry for extensions in the following format:
 * <extension name="..." class="...">
 * If nameAttribute is not specified, class name is used.
 * getExtensions() will return all objects collected in this registry
 * getExtension(name) will return object associated with given name (if nameAttribute was not specified, then name is class name) 
 * @author lan
 */
public class ObjectExtensionRegistry<T> extends ExtensionRegistrySupport<T>
{
    private Class<T> clazz;
    private String classAttributeName = "class";

    public ObjectExtensionRegistry(String extensionPointId, String nameAttribute, Class<T> clazz)
    {
        super(extensionPointId, nameAttribute);
        this.clazz = clazz;
    }

    public ObjectExtensionRegistry(String extensionPointId, Class<T> clazz)
    {
        this(extensionPointId, "class", clazz);
    }

    @Override
    protected T loadElement(IConfigurationElement element, String elementName) throws Exception
    {
        return getClassAttribute(element, classAttributeName, clazz).newInstance();
    }
}
