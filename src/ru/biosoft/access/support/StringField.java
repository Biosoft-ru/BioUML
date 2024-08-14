package ru.biosoft.access.support;

import java.beans.PropertyDescriptor;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StringField extends DatabaseField
{
    protected static final Logger log = Logger.getLogger( StringField.class.getName() );

    public StringField(PropertyDescriptor descriptor, String fieldTag)
    {
        super(descriptor, String.class, fieldTag);
    }

    @Override
    public Object getValue()
    {
        if(value==null && getEntry() != null)
        {
            try { value= EntryParser.parseStringValue(getEntry(), fieldTag); }
            catch(Exception e)
            {
                log.log(Level.SEVERE, "String field error", e);
                value = "error";
            }
        }

        return value;
    }

}
