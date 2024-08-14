package ru.biosoft.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Map;

import com.developmentontheedge.beans.AbstractDynamicPropertySet;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * not thread-safe!
 * @author lan
 */
public class LazyDynamicPropertySet extends AbstractDynamicPropertySet
{
    private DynamicPropertySet original;
    private String serialized;
    
    public LazyDynamicPropertySet(String serialized)
    {
        this.serialized = serialized;
    }
    
    private void init()
    {
        if(this.original == null)
        {
            try
            {
                this.original = TextUtil.readDPSFromJSON(serialized);
            }
            catch( Exception e )
            {
                throw new RuntimeException("Unable to initialize lazy DynamicPropertySet", e);
            }
            this.serialized = null;
        }
    }

    @Override
    public void renameProperty(String from, String to)
    {
        init();
        original.renameProperty(from, to);
    }

    @Override
    public void add(DynamicProperty property)
    {
        init();
        original.add(property);
    }

    @Override
    public boolean addBefore(String propName, DynamicProperty property)
    {
        init();
        return original.addBefore(propName, property);
    }

    @Override
    public boolean addAfter(String propName, DynamicProperty property)
    {
        init();
        return original.addAfter(propName, property);
    }

    @Override
    public boolean contains(DynamicProperty property)
    {
        init();
        return original.contains(property);
    }

    @Override
    public Object remove(String name)
    {
        init();
        return original.remove(name);
    }

    @Override
    public boolean moveTo(String name, int index)
    {
        init();
        return original.moveTo(name, index);
    }

    @Override
    public boolean replaceWith(String name, DynamicProperty prop)
    {
        init();
        return original.replaceWith(name, prop);
    }

    @Override
    public Iterator<String> nameIterator()
    {
        init();
        return original.nameIterator();
    }

    @Override
    public Iterator<DynamicProperty> propertyIterator()
    {
        init();
        return original.propertyIterator();
    }

    @Override
    public Map<String, Object> asMap()
    {
        init();
        return original.asMap();
    }

    @Override
    public int size()
    {
        init();
        return original.size();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        init();
        original.addPropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        init();
        original.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        init();
        original.removePropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        init();
        original.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        init();
        original.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        init();
        original.firePropertyChange(evt);
    }

    @Override
    public boolean hasListeners(String propertyName)
    {
        return original != null && original.hasListeners(propertyName);
    }

    @Override
    protected DynamicProperty findProperty(String name)
    {
        init();
        return original.getProperty(name);
    }
}
