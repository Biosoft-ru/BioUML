package ru.biosoft.util.bean;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import com.developmentontheedge.beans.BeanInfoConstants;

/**
 * PropertyDescriptor useful for mass DynamicProperty creation
 * it's read-only after creation, it ignores methods and it doesn't throw IntrospectionException
 * @author lan
 */
public class StaticDescriptor extends PropertyDescriptor
{
    private boolean initialized = false;
    
    private StaticDescriptor(String name, String displayName, String description, Class<?> editor, boolean hidden, boolean readOnly) throws IntrospectionException
    {
        super(name, null, null);
        if(displayName != null)
            super.setDisplayName(displayName);
        if(description != null)
            super.setShortDescription(description);
        if(editor != null)
            super.setPropertyEditorClass(editor);
        if(hidden)
            super.setHidden(true);
        if(readOnly)
            setValue( BeanInfoConstants.READ_ONLY, true);
        initialized = true;
    }
    
    public static StaticDescriptor create(String name, String displayName, String description, Class<?> editor, boolean hidden, boolean readOnly)
    {
        try
        {
            return new StaticDescriptor(name, displayName, description, editor, hidden, readOnly);
        }
        catch( IntrospectionException e )
        {
            throw new InternalError();
        }
    }
    
    public static StaticDescriptor create(String name, String displayName, Class<?> editor)
    {
        return create(name, displayName, null, editor, false, false);
    }
    
    public static StaticDescriptor create(String name, String displayName)
    {
        return create(name, displayName, null, null, false, false);
    }
    
    public static StaticDescriptor create(String name, Class<?> editor)
    {
        return create(name, null, null, editor, false, false);
    }

    public static StaticDescriptor create(String name)
    {
        return create(name, null, null, null, false, false);
    }

    public static StaticDescriptor createHidden(String name)
    {
        return create(name, null, null, null, true, false);
    }

    public static StaticDescriptor createReadOnly(String name, String displayName)
    {
        return create(name, displayName, null, null, false, true);
    }

    public static StaticDescriptor createReadOnly(String name)
    {
        return create(name, null, null, null, false, true);
    }

    @Override
    public synchronized void setReadMethod(Method readMethod) throws IntrospectionException
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setReadMethod(readMethod);
    }

    @Override
    public synchronized void setWriteMethod(Method writeMethod) throws IntrospectionException
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setWriteMethod(writeMethod);
    }

    @Override
    public void setBound(boolean bound)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setBound(bound);
    }

    @Override
    public void setConstrained(boolean constrained)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setConstrained(constrained);
    }

    @Override
    public void setPropertyEditorClass(Class<?> propertyEditorClass)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setPropertyEditorClass(propertyEditorClass);
    }

    @Override
    public void setName(String name)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setName(name);
    }

    @Override
    public void setDisplayName(String displayName)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setDisplayName(displayName);
    }

    @Override
    public void setExpert(boolean expert)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setExpert(expert);
    }

    @Override
    public void setHidden(boolean hidden)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setHidden(hidden);
    }

    @Override
    public void setPreferred(boolean preferred)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setPreferred(preferred);
    }

    @Override
    public void setShortDescription(String text)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setShortDescription(text);
    }

    @Override
    public void setValue(String attributeName, Object value)
    {
        if(initialized)
            throw new UnsupportedOperationException();
        super.setValue(attributeName, value);
    }
}
