package ru.biosoft.access;

import java.util.Properties;

import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ExtensionRegistrySupport;

/**
 * Common methods for DataElementExportRegistry and DataElementImportRegistry
 */
public abstract class DataElementRegistry<T extends DataElementRegistry.RegistryElementInfo> extends ExtensionRegistrySupport<T>
{
    // Common property IDs
    public static final String PLUGIN_ID = "pluginId";
    public static final String FORMAT = "format";
    public static final String DESCRIPTION = "description";
    public static final String PRIORITY = "priority";

    /** Utility class that stores information about <code>DiagramImporter</code>. */
    public static class RegistryElementInfo implements Comparable<RegistryElementInfo>
    {
        public RegistryElementInfo(Properties prop)
        {
            this.properties = prop;
        }

        protected Properties properties;
        public String getFormat()
        {
            return properties.getProperty(FORMAT);
        }

        public String getDescription()
        {
            return properties.getProperty(DESCRIPTION);
        }

        public int getPriority()
        {
            String priorityStr = properties.getProperty(PRIORITY);
            if( priorityStr != null )
            {
                try
                {
                    return Integer.parseInt(priorityStr);
                }
                catch( NumberFormatException e )
                {
                }
            }
            return -1;
        }

        @Override
        public String toString()
        {
            return getFormat();
        }

        @Override
        public int compareTo(RegistryElementInfo element)
        {
            return getFormat().compareTo(element.getFormat());
        }
    }

    public DataElementRegistry(String extensionPointId)
    {
        super(extensionPointId, FORMAT);
    }

    protected static Properties getExtensionProperties(IConfigurationElement e)
    {
        Properties prop = new Properties();
        prop.put(PLUGIN_ID, e.getNamespaceIdentifier());
        for( String name : e.getAttributeNames() )
        {
            prop.put(name, e.getAttribute(name));
        }
        return prop;
    }

}
