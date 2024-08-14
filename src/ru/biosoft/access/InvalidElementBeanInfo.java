package ru.biosoft.access;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.access.core.InvalidElement;

public class InvalidElementBeanInfo extends BeanInfoEx
{
    public InvalidElementBeanInfo()
    {
        super( InvalidElement.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptor("name", beanClass, "getName", null));
        add(new PropertyDescriptor("description", beanClass, "getDescription", null));
    }
}
