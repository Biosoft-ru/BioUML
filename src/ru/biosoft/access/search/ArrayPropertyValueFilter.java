package ru.biosoft.access.search;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.MutableFilter;

public class ArrayPropertyValueFilter extends MutableFilter<DataElement>
{
    public ArrayPropertyValueFilter( PropertyDescriptor pd )
    {
        this.pd = pd;
        try
        {
            Class<?> c =  pd.getPropertyType().getComponentType();
            if( c == int.class )
            {
                value = 0;
            }
            else if( c == byte.class )
            {
                value = (byte)0;
            }
            else if( c == short.class )
            {
                value = (short)0;
            }
            else if( c == long.class )
            {
                value = (long)0;
            }
            else if( c == char.class )
            {
                value = (char)0;
            }
            else if( c == boolean.class )
            {
                value = false;
            }
            else if( c == float.class )
            {
                value = (float)0;
            }
            else if( c == double.class )
            {
                value = (double)0;
            }
            else
            {
                value = c.newInstance();
            }
            initValue = value;
        }
        catch (InstantiationException ex)
        {
            ex.printStackTrace();
        }
        catch (IllegalAccessException ex)
        {
            ex.printStackTrace();
        }
    }

    public String getDisplayName()
    {
        if( pd==null )
            return "unknown property";
        else
            return pd.getDisplayName();
    }

    private Object initValue;
    private Object value;
    public void setValue( Object value )
    {
        this.value = value;
    }
    public Object getValue()
    {
        return value;
    }

    @Override
    public boolean isAcceptable( ru.biosoft.access.core.DataElement de )
    {
        if( !isEnabled() )
            return true;
        if( pd==null )
            return false;

        try
        {
            Method getter = pd.getReadMethod();
            Object[] result = (Object[])getter.invoke( de );
            Object value = getValue();
            if( value==null )
                return true;
            if( value.equals(initValue) )
                return true;
            boolean invalidPattern = false;
            for( Object element : result )
            {
                if( element instanceof String && value instanceof String && !invalidPattern)
                {
                    try
                    {
                        boolean matched = Pattern.matches((String)value,(String)element);
                        if( matched )
                            return matched;
                    }
                    catch( Throwable t )
                    {
                        invalidPattern = true;
                        if( element.equals(value) )
                            return true;
                    }
                }
                else
                    if( element.equals(value) )
                        return true;
            }
            return false;
        }
        catch( Exception exc )
        {
            return false;
        }
    }

    public PropertyDescriptor getDescriptor()
    {
        return pd;
    }

    private final PropertyDescriptor pd;
}