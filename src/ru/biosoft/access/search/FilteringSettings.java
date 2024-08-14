package ru.biosoft.access.search;

import java.beans.IntrospectionException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import ru.biosoft.access.core.filter.Filter;

public class FilteringSettings
{
    private String collectionName;

    public void setCollectionName(String collectionName)
    {
        String oldCollectionName = this.collectionName;
        this.collectionName = collectionName;
        propertyChangeListeners.firePropertyChange("collectionName", oldCollectionName, collectionName);
    }

    public String getCollectionName()
    {
        return collectionName;
    }

    private Class<?> nodeType;
    public void setType(Class<?> nodeType)
    {
        Class<?> oldNodeType = this.nodeType;
        this.nodeType = nodeType;
        try
        {
            setFilter(new BeanValueFilter(nodeType));
        }
        catch( IntrospectionException ex )
        {
            ex.printStackTrace();
        }
        propertyChangeListeners.firePropertyChange("type", oldNodeType, nodeType);
    }

    public Class<?> getType()
    {
        return nodeType;
    }

    private Filter filter;
    public void setFilter(Filter filter)
    {
        Filter oldFilter = this.filter;
        this.filter = filter;
        propertyChangeListeners.firePropertyChange("filter", oldFilter, filter);
    }
    public Filter getFilter()
    {
        return filter;
    }

    private transient PropertyChangeSupport propertyChangeListeners = new PropertyChangeSupport(this);
    public synchronized void removePropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeListeners.removePropertyChangeListener(l);
    }
    public synchronized void addPropertyChangeListener(PropertyChangeListener l)
    {
        propertyChangeListeners.addPropertyChangeListener(l);
    }
}