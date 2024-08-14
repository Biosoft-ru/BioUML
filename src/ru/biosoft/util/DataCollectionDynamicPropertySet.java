package ru.biosoft.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertyBuilder;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Option;

/**
 * @author lan
 * @todo Support changing of ru.biosoft.access.core.DataCollection
 */
public class DataCollectionDynamicPropertySet extends Option implements DynamicPropertySet
{
    private static final long serialVersionUID = 1L;
    private DataCollection<?> dc;
    
    public DataCollectionDynamicPropertySet(DataCollection<?> dc)
    {
        this.dc = dc;
    }

    @Override
    public Class<?> getType(String name)
    {
        try
        {
            return dc.get(name).getClass();
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public Object getValue(String name)
    {
        try
        {
            return dc.get(name);
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public String getValueAsString( String name )
    {
        Object val = getValue( name );
        if( val == null )
            return null;
        return val.toString();
    }

    @Override
    public void setValue(String name, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String name)
    {
        DynamicProperty property = getProperty(name);
        if(property != null)
             return property.getDescriptor();

        return null;
    }

    @Override
    public DynamicProperty getProperty(String name)
    {
        try
        {
            DataElement value = dc.get(name);
            return new DynamicProperty(name, value.getClass(), value);
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public void renameProperty(String from, String to)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(DynamicProperty property)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addBefore(String propName, DynamicProperty property)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAfter(String propName, DynamicProperty property)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(DynamicProperty property)
    {
        return dc.contains(property.getName());
    }

    @Override
    public Object remove(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean moveTo(String name, int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replaceWith(String name, DynamicProperty prop)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> nameIterator()
    {
        return dc.getNameList().iterator();
    }
    
    private class PropertyIterator extends TransformedIterator<String, DynamicProperty>
    {
        public PropertyIterator()
        {
            super(nameIterator());
        }
        
        @Override
        protected DynamicProperty transform(String name)
        {
            return getProperty(name);
        }
    }

    @Override
    public Iterator<DynamicProperty> propertyIterator()
    {
        return new PropertyIterator();
    }

    @Override
    public Iterator<DynamicProperty> iterator()
    {
        return propertyIterator();
    }

    @Override
    public Map<String, Object> asMap()
    {
        // TODO Fast implementation if necessary
        Map<String, Object> map = new HashMap<>();
        for(DataElement de : dc)
        {
            map.put( de.getName(), de );
        }
        return map;
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        // TODO Use propertyName
        addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        // TODO Use propertyName
        removePropertyChangeListener(listener);
    }

    @Override
    public boolean hasListeners(String propertyName)
    {
        // TODO Use propertyName
        return listenerList != null && listenerList.getListenerCount() > 0;
    }

    @Override
    public int size()
    {
        return dc.getSize();
    }

    @Override
    public boolean isEmpty()
    {
        return dc.isEmpty();
    }

    @Override
    public Object clone()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        super.firePropertyChange(evt);
    }

    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void setPropertyAttribute( String propName, String attrName, Object attrValue )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getValueAsLong(String name)
    {
        Object val = getValue( name );
        if( val == null )
            return null;
        return Long.parseLong( val.toString() );
    }

    @Override
    public DynamicPropertyBuilder getAsBuilder(String name)
    {
        throw new UnsupportedOperationException( "getAsBuilder is not supported" );
    }

    @Override
    public <T> T cast(String name, Class<T> clazz)
    {
        return clazz.cast( getValue( name ) );
    }

    @Override
    public String serializeAsXml(String beanName, String offset)
    {
        throw new UnsupportedOperationException( "serializeAsXml is not supported" );
    }
}
