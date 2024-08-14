package ru.biosoft.table;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertyBuilder;
import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * One line values for TableDataCollection
 */
@SuppressWarnings ( "serial" )
@PropertyName("table row")
public class RowDataElement implements DataElement, DynamicPropertySet
{
    protected static final Logger log = Logger.getLogger(RowDataElement.class.getName());

    private final String name;
    private DynamicProperty nameDP;
    private final TableDataCollection origin;
    private Object[] values;
    private boolean evaluated = false;

    public RowDataElement(String name, TableDataCollection origin)
    {
        this.name = name;
        this.origin = origin;
        this.values = new Object[0];
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return origin.getCompletePath().hashCode() * 31 + name.hashCode();
    }

    @Override
    public TableDataCollection getOrigin()
    {
        return origin;
    }

    /**
     * Add new element to values array to the end
     */
    public void addNewColumn()
    {
        Object[] newValues = new Object[values.length + 1];
        System.arraycopy(values, 0, newValues, 0, values.length);
        values = newValues;
    }

    /**
     * Get row values
     */
    public @Nonnull Object[] getValues()
    {
        return getValues(true);
    }

    /**
     * Get row values
     * @param calculate indicates when calculated columns should be recalculated
     */
    public @Nonnull Object[] getValues(boolean calculate)
    {
        if( calculate && origin.getColumnModel().hasExpressions() && !evaluated )
        {
            origin.evaluateRowCalculation(this);
            evaluated = true;
        }
        return values;
    }

    /**
     * Set row values
     */
    public void setValues(@Nonnull Object[] valuesArg)
    {
        values = valuesArg;
        evaluated = false;
    }

    @Override
    public void add(DynamicProperty property)
    {
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
    }

    private DynamicProperty getNameProperty()
    {
        if( nameDP == null )
        {
            nameDP = new DynamicProperty( DataCollectionConfigConstants.NAME_PROPERTY, String.class, name );
            ReferenceTypeRegistry.registerTypeForDescriptor(nameDP.getDescriptor(), origin.getReferenceType());
        }
        return nameDP;
    }

    @Override
    public Map<String, Object> asMap()
    {
        Map<String, Object> map = new HashMap<>();

        map.put( DataCollectionConfigConstants.NAME_PROPERTY, name );

        for( int i = 0; i < origin.getColumnModel().getColumnCount(); i++ )
        {
            map.put(origin.getColumnModel().getColumn(i).getName(), getProperty(i).getValue());
        }

        return map;
    }

    @Override
    public boolean contains(DynamicProperty property)
    {
        return property.getName().equals( DataCollectionConfigConstants.NAME_PROPERTY )
                || this.origin.getColumnModel().hasColumn( property.getName() );
    }

    @Override
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
    }

    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        this.origin.firePropertyChange(new PropertyChangeEvent(this, propertyName, oldValue, newValue));
    }

    @Override
    public DynamicProperty getProperty(String name)
    {
        if( DataCollectionConfigConstants.NAME_PROPERTY.equals( name ) )
        {
            return getNameProperty();
        }

        int index = this.origin.getColumnModel().optColumnIndex(name);
        if( index < 0 )
            return null;

        return getProperty(index);
    }
    
    private static final DecimalFormat SCIENTIFIC_FORMAT = new DecimalFormat( "0.###E0" );
    private static final DecimalFormat FIXED_POINT_FORMAT = new DecimalFormat( "0.0##" );
    static
    {
        DecimalFormatSymbols formatSymbols = SCIENTIFIC_FORMAT.getDecimalFormatSymbols();
        formatSymbols.setNaN( "NaN" );
        SCIENTIFIC_FORMAT.setDecimalFormatSymbols( formatSymbols );
        
        formatSymbols = FIXED_POINT_FORMAT.getDecimalFormatSymbols();
        formatSymbols.setNaN( "NaN" );
        FIXED_POINT_FORMAT.setDecimalFormatSymbols( formatSymbols );
    }

    private DynamicProperty getProperty(int index)
    {
        TableColumn columnInfo = this.origin.getColumnModel().getColumn(index);
        DynamicProperty prop = null;

        Object value = getValues()[index];
        value = columnInfo.getType().convertValue( value );

        Class<?> type = columnInfo.getValueClass();
        prop = new DynamicProperty(columnInfo.getName(), type, value);

        if( type == Double.class && value instanceof Number )
        {
            if( Math.abs(( (Number)value ).doubleValue()) < 0.001 && !value.equals(0.0) )
                prop.getDescriptor().setValue(BeanInfoConstants.NUMBER_FORMAT, SCIENTIFIC_FORMAT);
            else
                prop.getDescriptor().setValue(BeanInfoConstants.NUMBER_FORMAT, FIXED_POINT_FORMAT);
        }

        ReferenceTypeRegistry.registerTypeForDescriptor(prop.getDescriptor(), columnInfo
                .getValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY));

        return prop;
    }
    @Override
    public PropertyDescriptor getPropertyDescriptor(String name)
    {
        DynamicProperty prop = getProperty(name);
        if( prop == null )
            return null;

        return prop.getDescriptor();
    }

    @Override
    public Class<?> getType(String name)
    {
        DynamicProperty prop = getProperty(name);
        if( prop == null )
            return null;

        return prop.getType();
    }

    @Override
    public Object getValue(String name)
    {
        if( DataCollectionConfigConstants.NAME_PROPERTY.equals( name ) )
        {
            return getName();
        }
        int index = this.origin.getColumnModel().optColumnIndex(name);
        if( index < 0 )
            return null;
        return getValues()[index];
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
    public boolean hasListeners(String propertyName)
    {
        return false;
    }

    @Override
    public boolean moveTo(String name, int index)
    {
        return false;
    }

    @Override
    public Iterator<String> nameIterator()
    {
        return new Iterator<String>()
        {
            int i = 0;

            @Override
            public boolean hasNext()
            {
                return i < origin.getColumnModel().getColumnCount() + 1;
            }

            @Override
            public String next()
            {
                if(!hasNext())
                    throw new NoSuchElementException();
                int k = i++;
                if( k == 0 )
                {
                    return DataCollectionConfigConstants.NAME_PROPERTY;
                }

                return origin.getColumnModel().getColumn(k - 1).getName();
            }
        };
    }

    @Override
    public Iterator<DynamicProperty> propertyIterator()
    {
        return new Iterator<DynamicProperty>()
        {
            int i = 0;

            @Override
            public boolean hasNext()
            {
                return i < origin.getColumnModel().getColumnCount() + 1;
            }

            @Override
            public DynamicProperty next()
            {
                if(!hasNext())
                    throw new NoSuchElementException();
                int k = i++;
                if( k == 0 )
                {
                    return getNameProperty();
                }

                return getProperty(k - 1);
            }
        };
    }

    @Override
    public Iterator<DynamicProperty> iterator()
    {
        return propertyIterator();
    }

    @Override
    public Object remove(String name)
    {
        return null;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
    }

    @Override
    public void renameProperty(String from, String to)
    {
    }

    @Override
    public boolean replaceWith(String name, DynamicProperty prop)
    {
        return false;
    }

    @Override
    public void setValue(String name, Object value)
    {
        int index = this.origin.getColumnModel().optColumnIndex(name);
        if( index < 0 )
            return;

        Object oldValue = values[index];
        if( oldValue == value || ( oldValue != null && oldValue.equals(value) ) )
            return;

        TableColumn col = this.origin.getColumnModel().getColumn(index);
        value = col.getType().convertValue( value );

        values[index] = value;
        evaluated = false;
        firePropertyChange(name, oldValue, value);
    }

    protected void markNonEvaluated()
    {
        evaluated = false;
    }

    @Override
    public int size()
    {
        return values.length;
    }

    @Override
    public boolean isEmpty()
    {
        return ( values.length == 0 );
    }

    @Override
    public RowDataElement clone()
    {
        RowDataElement rde = new RowDataElement(name, origin);
        Object[] values = new Object[this.values.length];
        System.arraycopy(this.values, 0, values, 0, this.values.length);
        rde.setValues(values);
        return rde;
    }

    //new methods in DynamicPropertySet interface
    //TODO: correctly support of this functions\
    @Override
    public boolean addAfter(String propName, DynamicProperty property)
    {
        throw new UnsupportedOperationException("addAfter is not supported");
    }
    @Override
    public boolean addBefore(String propName, DynamicProperty property)
    {
        throw new UnsupportedOperationException("addAfter is not supported");
    }

    @Override
    public void setPropertyAttribute( String propName, String attrName, Object attrValue )
    {
        throw new UnsupportedOperationException();
    }

	@Override
	public Long getValueAsLong(String name) {
        Object val = getValue( name );
        if( val == null )
            return null;
        return Long.parseLong( val.toString() );
	}

	@Override
	public DynamicPropertyBuilder getAsBuilder(String name) {
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