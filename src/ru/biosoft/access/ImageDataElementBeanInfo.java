package ru.biosoft.access;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;

/**
 * @author lan
 *
 */
public class ImageDataElementBeanInfo extends BeanInfoEx
{
    public ImageDataElementBeanInfo()
    {
        super( ImageDataElement.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptor("name", beanClass, "getName", null));
        add(new PropertyDescriptor("format", beanClass, "getFormat", null));
    }
}
