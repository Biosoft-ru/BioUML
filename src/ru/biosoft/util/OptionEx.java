package ru.biosoft.util;

import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.model.Property;

/**
 * Option class with support of auto-updated options
 * @author lan
 */
public class OptionEx extends Option implements BeanWithAutoProperties
{
    public static final String TEMPLATE_PROPERTY = "template";
    transient private Map<String, Property> autoPropertiesMap;
    transient private Set<String> activeAutoProperties;
    private final PropertyChangeListener propagate = e ->
    {
        if(e.getPropertyName().equals("*"))
            firePropertyChange("*", null, null);
    };

    public static <T extends FeatureDescriptor> T makeAutoProperty(T fd, String template)
    {
        fd.setValue(TEMPLATE_PROPERTY, template);
        return fd;
    }

    public static boolean isAutoProperty(FeatureDescriptor fd)
    {
        return fd.getValue(TEMPLATE_PROPERTY) != null;
    }

    @Override
    public AutoPropertyStatus getAutoPropertyStatus(String name)
    {
        if( autoPropertiesMap.get(name) == null )
            return AutoPropertyStatus.NOT_AUTO_PROPERTY;
        if( activeAutoProperties.contains(name) )
            return AutoPropertyStatus.AUTO_MODE_ON;
        return AutoPropertyStatus.AUTO_MODE_OFF;
    }
    
    protected OptionEx(boolean lateInit)
    {
        super();
        if(!lateInit) initAutoProperties();
    }

    public OptionEx()
    {
        super();
        initAutoProperties();
    }

    public OptionEx(Option parent)
    {
        super(parent);
        initAutoProperties();
    }
    
    protected <T extends Option> T withPropagation(T oldValue, T newValue)
    {
        if( oldValue != null )
        {
            oldValue.removePropertyChangeListener(propagate);
        }
        if( newValue != null )
        {
            newValue.setParent(this);
            newValue.addPropertyChangeListener(propagate);
        }
        return newValue;
    }
    
    protected <T extends OptionEx> T[] withPropagation(T[] oldValue, T[] newValue)
    {
        Set<T> oldValues = new HashSet<>();
        if( oldValue != null )
            oldValues.addAll(Arrays.asList(oldValue));
        Set<T> newValues = new HashSet<>( Arrays.asList( newValue ) );
        Set<T> added = new HashSet<>(newValues);
        added.removeAll(oldValues);
        Set<T> removed = new HashSet<>(oldValues);
        removed.removeAll(newValues);
        for( T val : removed )
        {
            if( val != null )
            {
                val.removePropertyChangeListener(propagate);
            }
        }
        for( T val : added )
        {
            if( val != null )
            {
                val.addPropertyChangeListener(propagate);
            }
        }
        return newValue;
    }

    @Override
    protected void firePropertyChange(PropertyChangeEvent evt)
    {
        super.firePropertyChange(evt);
        if( Objects.equals( evt.getOldValue(), evt.getNewValue() ) )
            return;
        if( ( evt.getNewValue() == null && evt.getOldValue() instanceof String && evt.getOldValue().equals("") )
                || ( evt.getOldValue() == null && evt.getNewValue() instanceof String && evt.getNewValue().equals("") ) )
            return;
        if( autoPropertiesMap.containsKey(evt.getPropertyName()) )
        {
            try
            {
                if( evt.getNewValue() == null )
                    activeAutoProperties.add(evt.getPropertyName());
                else
                {
                    Property property = autoPropertiesMap.get(evt.getPropertyName());
                    Object calcValue = TextUtil.fromString(property.getValueClass(),
                            TextUtil.calculateTemplate(property.getDescriptor().getValue(TEMPLATE_PROPERTY).toString(), this));
                    if( calcValue != null && calcValue.equals(evt.getNewValue()) )
                        activeAutoProperties.add(evt.getPropertyName());
                    else
                        activeAutoProperties.remove(evt.getPropertyName());
                }
            }
            catch( Exception e )
            {
            }
        }
        for( String propertyName : activeAutoProperties )
        {
            try
            {
                Property property = autoPropertiesMap.get(propertyName);
                Object oldValue = property.getValue();
                Object newValue = TextUtil.fromString(property.getValueClass(),
                        TextUtil.calculateTemplate(property.getDescriptor().getValue(TEMPLATE_PROPERTY).toString(), this));
                if( !Objects.equals( oldValue, newValue ) )
                {
                    property.setValue(newValue);
                }
            }
            catch( Exception e )
            {
            }
        }
    }

    protected void initAutoProperties()
    {
        autoPropertiesMap = BeanUtil.properties( this ).filter( property -> property.getDescriptor().getValue( TEMPLATE_PROPERTY ) != null )
                .toMap( Property::getName, Function.identity() );
        activeAutoProperties = new HashSet<>(autoPropertiesMap.keySet());
    }
}
