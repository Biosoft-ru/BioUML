package ru.biosoft.access.generic;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.DataElementReadException;

/**
 * ru.biosoft.access.core.DataElement which is reference to another ru.biosoft.access.core.DataElement.
 * Information about base element saved in properties.
 * Designed for use in {@link GenericDotaCollection}
 */
public class DataElementInfo extends DataElementSupport
{
    public static final String DRIVER_CLASS = "driver";
    public static final String ELEMENT_CLASS = "class";
    
    protected Properties properties;
    protected String description;

    public DataElementInfo(String name, DataCollection<?> origin)
    {
        super(name, origin);
        this.properties = new Properties();
    }
    
    public DataElementInfo(String name, DataCollection<?> origin, Properties properties)
    {
        super(name, origin);
        this.properties = properties;
    }
    
    public String getProperty(String key)
    {
        return properties.getProperty(key);
    }
    
    public @Nonnull String getStrictProperty(String key)
    {
        String result = properties.getProperty(key);
        if(result == null)
        {
            System.out.println( "Missing properties " + key );
            throw new DataElementReadException(this, key);
        }
            return result;
    }
    
    public void setProperty(String key, String value)
    {
        properties.setProperty(key, value);
    }

    protected Properties getProperties()
    {
        return properties;
    }
}
