package ru.biosoft.util;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Simple extension registry which collects classes.
 * Extensions should be written like this:
 * <extension name="..." class="...">
 * If nameAttribute is not specified, class name is used.
 * getExtensions() will return all classes collected in this registry
 * getExtension(name) will return class associated with given name (if nameAttribute was not specified, then name is class name) 
 * @author lan
 */
public class ClassExtensionRegistry<T> extends ExtensionRegistrySupport<Class<? extends T>>
{
    private Class<T> clazz;
    private String classAttributeName = "class";

    /**
     * @param extensionPointId fully qualified extension point ID like "ru.biosoft.access.commonClass"
     * @param nameAttribute name of the attribute used as key
     * @param clazz common parent class of all classes (Class cast check will be performed) 
     */
    public ClassExtensionRegistry(String extensionPointId, String nameAttribute, Class<T> clazz)
    {
        super(extensionPointId, nameAttribute);
        this.clazz = clazz;
    }

    public ClassExtensionRegistry(String extensionPointId, Class<T> clazz)
    {
        this(extensionPointId, "class", clazz);
    }

    @Override
    protected Class<? extends T> loadElement(IConfigurationElement element, String elementName) throws Exception
    {
        return getClassAttribute(element, classAttributeName, clazz);
    }
}
