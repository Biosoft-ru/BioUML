package ru.biosoft.access.support;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.Entry;

import com.developmentontheedge.beans.DynamicProperty;

public class DatabaseField extends DynamicProperty
{

    public DatabaseField(PropertyDescriptor descriptor, Class type, String fieldTag)
    {
        super(descriptor, type);
        this.fieldTag = fieldTag;
    }

    ////////////////////////////////////////////////////////////////////////////

    protected Entry getEntry()
    {
        return ((DatabaseEntry)getParent()).getEntry();
    }

    protected String  fieldTag;
    public String getFieldTag()
    {
        return fieldTag;
    }
}
