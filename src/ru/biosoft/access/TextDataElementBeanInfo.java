package ru.biosoft.access;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;

public class TextDataElementBeanInfo extends BeanInfoEx
{
    public TextDataElementBeanInfo()
    {
        super( TextDataElement.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptor("name", beanClass, "getName", null));
    }
}
