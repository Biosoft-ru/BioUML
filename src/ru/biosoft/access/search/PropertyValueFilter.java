package ru.biosoft.access.search;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.MutableFilter;

public class PropertyValueFilter extends MutableFilter
{
    public PropertyValueFilter( PropertyDescriptor pd )
    {
        this.pd = pd;
        try
        {
            Class<?> c =  pd.getPropertyType();
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
        }catch (IllegalAccessException ex)
        {
        }
    }

    private Object initValue;
    private Object value;
    public void setValue( Object value )
    {
        Object oldValue = this.value;
    	this.value = value;
        this.firePropertyChange("value", oldValue, value);
    }
    public Object getValue()
    {
        return value;
    }

    public String getDisplayName()
    {
        if( pd==null )
            return "unknown property";
        else
            return pd.getDisplayName();
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
            Object result = getter.invoke( de );
            Object value = getValue();
            if( value==null )
                return true;
            if( value.equals(initValue) )
                return true;

            if( result instanceof String && value instanceof String )
            {
                try
                {
                    //boolean matched = perl.match((String)value,(String)result);
                    boolean matched = Pattern.matches((String)value,(String)result);
                    return matched;
                }
                catch( Throwable t )
                {}
            }
            return result.equals(value);
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