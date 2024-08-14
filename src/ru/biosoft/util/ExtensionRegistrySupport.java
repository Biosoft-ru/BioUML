package ru.biosoft.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.exception.ExtensionInitException;
import ru.biosoft.access.exception.InitializationException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.exception.MissingParameterException;

import com.developmentontheedge.application.Application;

/**
 * @author lan
 *
 */
public abstract class ExtensionRegistrySupport<T> implements Iterable<T>
{
    private static final Logger log = Logger.getLogger(ExtensionRegistrySupport.class.getName());
    private final String extensionPointId;
    private final String nameAttribute;
    protected volatile List<T> extensions;
    protected volatile Map<String, T> nameToExtension;
    private volatile boolean initialized = false;
    private volatile boolean initializing = false;

    /**
     * @param extensionPointId - id of extension point like "ru.biosoft.access.import"
     * @param nameAttribute - required attribute which identifies the extension (particularly will be used for error messages and duplicates check)
     */
    public ExtensionRegistrySupport(String extensionPointId, String nameAttribute)
    {
        this.extensionPointId = extensionPointId;
        this.nameAttribute = nameAttribute;
    }

    public ExtensionRegistrySupport(String extensionPointId)
    {
        this(extensionPointId, "name");
    }

    /**
     * @return collection of registered extensions
     */
    protected List<T> doGetExtensions()
    {
        init();
        return extensions;
    }

    /**
     * Returns extension by given name
     * @param name
     * @return extension or null if no such extension exists
     */
    public T getExtension(String name)
    {
        if( name == null )
            return null;
        init();
        return nameToExtension.get(name);
    }

    @Override
    public Iterator<T> iterator()
    {
        init();
        return Collections.unmodifiableList(extensions).iterator();
    }

    protected final void init()
    {
        if(!initialized)
        {
            synchronized(this)
            {
                if(!initialized)
                {
                    if(initializing)
                        throw new InitializationException("Concurrent initialization of extension point "+extensionPointId);
                    initializing = true;
                    try
                    {
                        loadExtensions();
                        postInit();
                    }
                    finally
                    {
                        initializing = false;
                        initialized = true;
                    }
                }
            }
        }
    }

    /**
     * Subclass this method to define additional initialization steps
     */
    protected void postInit()
    {
    }

    protected @Nonnull String getStringAttribute(IConfigurationElement element, String name)
    {
        String value = element.getAttribute(name);
        if(value == null)
        {
            throw new MissingParameterException( name );
        }
        return value;
    }

    protected boolean getBooleanAttribute(IConfigurationElement element, String name)
    {
        return getBooleanAttribute(element, name, false);
    }

    protected boolean getBooleanAttribute(IConfigurationElement element, String name, boolean defaultValue)
    {
        String value = element.getAttribute(name);
        return value == null ? defaultValue : (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
    }

    protected int getIntAttribute(IConfigurationElement element, String name, int defaultValue)
    {
        String value = element.getAttribute(name);
        if(value == null)
            return defaultValue;
        try
        {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException ex)
        {
            return defaultValue;
        }
    }

    protected int getIntAttribute(IConfigurationElement element, String name)
    {
        return getIntAttribute(element, name, 0);
    }

    protected double getDoubleAttribute(IConfigurationElement element, String name, double defaultValue)
    {
        String value = element.getAttribute(name);
        if(value == null)
            return defaultValue;
        try
        {
            return Double.parseDouble(value);
        }
        catch(NumberFormatException ex)
        {
            return defaultValue;
        }
    }

    protected double getDoubleAttribute(IConfigurationElement element, String name)
    {
        return getDoubleAttribute(element, name, 0);
    }

    @Nonnull
    protected <K> Class<? extends K> getClassAttribute(IConfigurationElement element, String name, @Nonnull Class<K> parentClass)
    {
        String className = getStringAttribute(element, name);
        Class<? extends K> clazz;
        try
        {
            clazz = ClassLoading.loadSubClass( className, element.getNamespaceIdentifier(), parentClass );
        }
        catch( Exception e )
        {
            throw new ParameterNotAcceptableException(e, name, className);
        }
        return clazz;
    }

    private void loadExtensions()
    {
        List<T> extensions = new ArrayList<>();
        Map<String, T> nameToExtension = new HashMap<>();
        IExtensionRegistry extensionRegistry = Application.getExtensionRegistry();
        if(extensionRegistry == null)
        {
            new ExtensionInitException(extensionPointId).log();
        } else
        {
            IExtensionPoint point = extensionRegistry.getExtensionPoint(extensionPointId);
            if(point == null)
            {
                new ExtensionInitException(extensionPointId).log();
            } else
            {
                Map<String, List<String>> debug = new LinkedHashMap<>();
                int total = 0, success = 0;
                for(IExtension extension: point.getExtensions())
                {
                    String pluginId = extension.getNamespaceIdentifier();
                    for(IConfigurationElement element: extension.getConfigurationElements())
                    {
                        String name = element.getName();
                        total++;
                        try
                        {
                            name = getStringAttribute(element, nameAttribute);
                            if(nameToExtension.containsKey(name))
                                throw new Exception("Duplicate extension "+name);
                            T loadedElement = loadElement(element, name);
                            if(loadedElement == null)
                                throw new NullPointerException();
                            extensions.add(loadedElement);
                            nameToExtension.put(name, loadedElement);
                            debug.computeIfAbsent( pluginId, k -> new ArrayList<>() ).add(name);
                            success++;
                        }
                        catch(Throwable t)
                        {
                            new ExtensionInitException(t, extensionPointId, pluginId, name, null).log();
                        }
                    }
                }
                StringBuilder debugInfo = new StringBuilder("Registry initialized ("+success+"/"+total+" extensions loaded):\n");
                debugInfo.append(extensionPointId).append(" -- ");
                Iterator<Entry<String, List<String>>> pluginIterator = debug.entrySet().iterator();
                boolean firstPlugin = true;
                while(pluginIterator.hasNext())
                {
                    Entry<String, List<String>> pluginEntry = pluginIterator.next();
                    Iterator<String> extensionIterator = pluginEntry.getValue().iterator();
                    boolean firstPoint = true;
                    while(extensionIterator.hasNext())
                    {
                        String extension = extensionIterator.next();
                        if(firstPoint)
                        {
                            firstPoint = false;
                            if(firstPlugin)
                            {
                                firstPlugin = false;
                            } else
                            {
                                debugInfo.append(TextUtil.whiteSpace(extensionPointId.length())).append(
                                        pluginIterator.hasNext() ? " |- " : " \\- ");
                            }
                            debugInfo.append(pluginEntry.getKey()).append(" -- ");
                        } else
                        {
                            debugInfo.append(TextUtil.whiteSpace(extensionPointId.length()))
                                    .append(pluginIterator.hasNext() ? " |  " : "    ")
                                    .append(TextUtil.whiteSpace(pluginEntry.getKey().length())).append(extensionIterator.hasNext() ? " |- " : " \\- ");
                        }
                        debugInfo.append(extension).append("\n");
                    }
                }
                log.config(debugInfo.toString());
            }
        }
        this.extensions = extensions;
        this.nameToExtension = nameToExtension;
    }

    public StreamEx<T> stream()
    {
        init();
        return StreamEx.of( extensions );
    }

    public StreamEx<String> names()
    {
        init();
        return StreamEx.ofKeys( nameToExtension );
    }

    public EntryStream<String, T> entries()
    {
        init();
        return EntryStream.of( nameToExtension );
    }

    protected abstract T loadElement(IConfigurationElement element, String elementName) throws Exception;
}
