package ru.biosoft.fs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import one.util.streamex.EntryStream;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.exception.Assert;

/**
 * Immutable class which stores element information
 * @author lan
 *
 */
class ElementInfo implements Comparable<ElementInfo>
{
    private final String name;
    private Class<? extends DataElement> clazz;
    private boolean changed = false;
    private final Map<String, String> properties;
    
    private static final Set<String> excludedPropertyNames = new HashSet<>(Arrays.asList(DataCollectionConfigConstants.NAME_PROPERTY,
            DataCollectionConfigConstants.CONFIG_FILE_PROPERTY, DataCollectionConfigConstants.CONFIG_PATH_PROPERTY));

    public ElementInfo(String name, Class<? extends DataElement> clazz)
    {
        Assert.notNull( "ElementInfo name", name );
        Assert.notNull( "ElementInfo class for "+name, clazz );
        this.name = name;
        this.clazz = clazz;
        if(clazz != TextDataElement.class && clazz != FileSystemCollection.class)
        {
            changed = true;
        }
        this.properties = new HashMap<>();
    }

    public ElementInfo(String name, boolean folder)
    {
        Assert.notNull( "ElementInfo name", name );
        this.name = name;
        this.properties = new HashMap<>();
        FileSystemElementDriver driver = FileSystemCollection.getDriverByClass( folder ? FileSystemCollection.class : TextDataElement.class );
        this.clazz = driver.detectClass(this);
    }

    public ElementInfo(String name, Class<? extends DataElement> clazz, Map<String, ?> map)
    {
        this( name, clazz );
        read( map );
    }
    
    public ElementInfo(DataElement de)
    {
        this(de.getName(), de.getClass());
        if(de instanceof DataCollection)
        {
            readCollectionProperties( (DataCollection<?>)de );
        }
    }

    public ElementInfo(DataElement de, Map<String, String> addProperties)
    {
        this(de);
        EntryStream.of(addProperties).forKeyValue(this::setProperty);
    }

    public DataElementDescriptor getDescriptor()
    {
        return new DataElementDescriptor( clazz, isLeaf(), properties );
    }

    public Class<? extends DataElement> getClazz()
    {
        return clazz;
    }

    public String getName()
    {
        return name;
    }

    public FileSystemElementDriver getDriver()
    {
        return FileSystemCollection.getDriverByClass( clazz );
    }

    public String getProperty(String key)
    {
        return properties.get( key );
    }
    
    public Map<String, String> getProperties()
    {
        return new HashMap<>(properties);
    }

    public boolean isLeaf()
    {
        return getDriver().isLeaf( this );
    }

    private void changeType(Class<? extends DataElement> clazz)
    {
        this.clazz = clazz;
        this.changed = true;
    }

    private void read(Map<String, ?> map)
    {
        if( map == null )
            return;
        for( Entry<String, ?> entry : map.entrySet() )
        {
            Object valueObj = entry.getValue();
            if( ! ( valueObj instanceof String ) )
                continue;
            if( entry.getKey().equals( DataCollectionConfigConstants.CLASS_PROPERTY ) )
            {
                changeType( ClassLoading.loadSubClass( (String)valueObj, DataElement.class ) );
            }
            else if( !excludedPropertyNames.contains( entry.getKey() ) )
            {
                setProperty( entry.getKey(), (String)valueObj );
            }
        }
    }

    public Map<String, Object> write()
    {
        if( !changed )
            return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put( DataCollectionConfigConstants.CLASS_PROPERTY, clazz.getName() );
        for( Entry<String, String> entry : properties.entrySet() )
        {
            if( !DataCollectionConfigConstants.CLASS_PROPERTY.equals( entry.getKey() ) )
            {
                map.put( entry.getKey(), entry.getValue() );
            }
        }
        return map;
    }

    private void setProperty(String key, String value)
    {
        properties.put( key, value );
        this.changed = true;
    }

    private void removeProperty(String key)
    {
        if(properties.remove( key ) != null)
            this.changed = true;
    }

    private void readCollectionProperties(DataCollection<?> dc)
    {
        DataCollectionInfo dcInfo = dc.getInfo();
        Properties dcProperties = dcInfo.getProperties();
        for( Map.Entry<Object, Object> entry : dcProperties.entrySet() )
        {
            Object propertyName = entry.getKey();
            if( excludedPropertyNames.contains( propertyName ) )
                continue;
            Object value = entry.getValue();
            if(value != null)
                setProperty(propertyName.toString(), value.toString());
        }
        if(!dcInfo.getDisplayName().equals(dc.getName()))
        {
            setProperty(DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY, dcInfo.getDisplayName());
        } else
        {
            removeProperty(DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY);
        }
    }

    public void initCollectionProperties(DataCollection<?> dc)
    {
        Properties props = dc.getInfo().getProperties();
        EntryStream.of(properties).forKeyValue( props::setProperty );
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( clazz == null ) ? 0 : clazz.hashCode() );
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( properties == null ) ? 0 : properties.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        ElementInfo other = (ElementInfo)obj;
        if( !clazz.equals( other.clazz ) )
            return false;
        if( !name.equals( other.name ) )
            return false;
        if( !properties.equals( other.properties ) )
            return false;
        return true;
    }

    @Override
    public int compareTo(ElementInfo o)
    {
        boolean folder = FolderCollection.class.isAssignableFrom( clazz );
        boolean otherFolder = FolderCollection.class.isAssignableFrom( o.clazz );
        if(folder && !otherFolder)
            return -1;
        if(!folder && otherFolder)
            return 1;
        return name.compareToIgnoreCase( o.name );
    }

}