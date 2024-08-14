package ru.biosoft.table.datatype;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.exception.InternalException;
import ru.biosoft.table.MessageBundle;
import ru.biosoft.util.ObjectExtensionRegistry;

public abstract class DataType
{
    private static final ObjectExtensionRegistry<DataType> DATA_TYPE_REGISTRY = new ObjectExtensionRegistry<>( "ru.biosoft.table.dataType", DataType.class );

    public static class BooleanType
    {
        private Boolean value = false;

        public Boolean getValue()
        {
            return value;
        }

        public void setValue(Boolean value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return value.toString();
        }
    }

    public static class BooleanTypeBeanInfo extends BeanInfoEx
    {
        protected BooleanTypeBeanInfo()
        {
            super(BooleanType.class, MessageBundle.class.getName());

            initResources(MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_BOOLEAN_TYPE"));
            beanDescriptor.setShortDescription(getResourceString("CD_BOOLEAN_TYPE"));
        }

        @Override
        public void initProperties() throws Exception
        {
            initResources(MessageBundle.class.getName());

            add(new PropertyDescriptorEx("value", beanClass), getResourceString("PN_VALUE"), getResourceString("PD_VALUE"));
        }
    }

    public static final DataType Text = new DataType(String.class, "Text", "")
    {
        @Override
        public Object convertValue(Object value)
        {
            if(value == null)
                return value;
            return value.toString();
        }
    };
    public static final DataType Integer = new DataType(Integer.class, "Integer", 0)
    {

        @Override
        public Object convertValue(Object value)
        {
            try
            {
                return value == null ? null : value instanceof Number ? ( (Number)value ).intValue() : java.lang.Integer
                        .parseInt(value.toString());
            }
            catch( NumberFormatException e )
            {
                return null;
            }
        }

    };
    public static final DataType Float = new DataType(Double.class, "Float", 0.0)
    {
        @Override
        public Object convertValue(Object value)
        {
            if(value == null)
                return Double.NaN;
            try
            {
                double result = value instanceof Number ? ( (Number)value ).doubleValue() : Double.parseDouble( value.toString() );
                if( result == Double.MAX_VALUE )
                    return Double.POSITIVE_INFINITY;
                if( result == -Double.MAX_VALUE)
                    return Double.NEGATIVE_INFINITY;
                return result;
            }
            catch( NumberFormatException e )
            {
                return null;
            }
        }
    };
    public static final DataType Boolean = new DataType(DataType.BooleanType.class, "Boolean", null)
    {

        @Override
        public Object convertValue(Object value)
        {
            if(value instanceof DataType.BooleanType)
                return value;
            DataType.BooleanType result = new DataType.BooleanType();
            if(value instanceof Boolean)
            {
                result.setValue( (java.lang.Boolean)value );
            }
            result.setValue( java.lang.Boolean.valueOf( String.valueOf( value ) ) );
            return result;
        }

    };

    protected DataType(Class<?> mainType, String name, Object defaultValue)
    {
        this.mainType = mainType;
        this.name = name;
        this.defaultValue = defaultValue;
        this.isNumeric = Number.class.isAssignableFrom( mainType );
    }

    private final Class<?> mainType;
    private final boolean isNumeric;

    /** Can be null, but must be immutable */
    private final Object defaultValue;
    private final String name;

    public Class<?> getType()
    {
        return mainType;
    }

    public String name()
    {
        return name;
    }

    public boolean isNumeric()
    {
        return isNumeric;
    }

    @Override
    public String toString()
    {
        return name();
    }

    /**
     * @return true if the value object of this data type supports lazy initialization from the CharSequence
     */
    public boolean supportsLazyCharSequenceInit()
    {
        return false;
    }

    public Object getDefaultValue()
    {
        return defaultValue;
    }

    public abstract Object convertValue(Object value);

    private static final Map<String, DataType> registry = new TreeMap<>();
    private static final Map<Class<?>, DataType> classMap = new HashMap<>();

    static
    {
        for(Field field : DataType.class.getFields())
        {
            if(field.getType() == DataType.class)
            {
                try
                {
                    DataType value = (DataType)field.get( null );
                    registry.put( value.name(), value );
                }
                catch( IllegalArgumentException | IllegalAccessException e )
                {
                    throw new InternalException( e );
                }
            }
        }
        for(DataType type : DATA_TYPE_REGISTRY.stream())
        {
            registry.put( type.name(), type );
        }
        for(DataType type : registry.values())
        {
            classMap.put( type.getType(), type );
        }
        classMap.put( java.lang.Float.class, Float );
    }

    public static DataType fromClass(Class<?> type)
    {
        DataType dataType = classMap.get( type );
        if(dataType != null)
            return dataType;
        return DataType.Text;
    }

    public static DataType fromString(String type)
    {
        DataType dataType = registry.get( type );
        if(dataType != null)
            return dataType;
        return DataType.Text;
    }

    public static String[] names()
    {
        return registry.keySet().toArray( new String[registry.size()] );
    }
}